extends Node3D
## Java owns the roster, schedule and route position; this node only renders nearby results.
var people: Dictionary={}
var http: HTTPRequest
var elapsed:=1.0
var connected:=false
var population:=0
var commuting:=0
var day:=1
var minute:=0
var traffic: Node3D
var enabled:=false

func configure(source: Node3D, use_server: bool) -> void:
    traffic=source
    enabled=use_server
    if not enabled:return
    http=HTTPRequest.new()
    http.timeout=3.0
    add_child(http)
    http.request_completed.connect(_received)

func update_residents(pos: Vector2,delta: float) -> void:
    if not enabled:return
    elapsed+=delta
    if elapsed>=1.0 and http.get_http_client_status()==HTTPClient.STATUS_DISCONNECTED:
        elapsed=0
        http.request("http://127.0.0.1:8091/geo/nearby?x=%.3f&z=%.3f" % [pos.x,pos.y])
    for id in people:
        var record: Dictionary=people[id]
        var node: Node3D=record["node"]
        var goal: Vector3=record["goal"]
        var direction:=goal-node.position
        if direction.length()>0.03:
            node.position=node.position.move_toward(goal,delta*12.0)
            node.rotation.y=atan2(-direction.x,-direction.z)
            record["phase"]=float(record["phase"])+delta*7
            node.get_node("LegL").rotation.x=sin(float(record["phase"]))*0.32
            node.get_node("LegR").rotation.x=-sin(float(record["phase"]))*0.32

func _received(result: int,code: int,_headers: PackedStringArray,body: PackedByteArray) -> void:
    if result!=HTTPRequest.RESULT_SUCCESS or code!=200:
        _offline()
        return
    var parsed=JSON.parse_string(body.get_string_from_utf8())
    if not parsed is Dictionary or not parsed.get("residents") is Array:
        _offline()
        return
    connected=true
    traffic.set_java_population(true)
    population=int(parsed.get("population",0))
    commuting=int(parsed.get("commuting",0))
    day=int(parsed.get("day",1))
    minute=int(parsed.get("minute",0))
    var seen: Dictionary={}
    for person in parsed["residents"]:
        var id:=str(person["id"])
        var goal:=Vector3(float(person["x"]),0.05,float(person["z"]))
        seen[id]=true
        if not people.has(id):
            var node: Node3D=traffic._person()
            add_child(node)
            node.position=goal
            people[id]={"node":node,"goal":goal,"phase":0.0}
        else:
            people[id]["goal"]=goal
    for id in people.keys():
        if not seen.has(id):
            people[id]["node"].queue_free()
            people.erase(id)

func _offline() -> void:
    connected=false
    traffic.set_java_population(false)
    for record in people.values():record["node"].queue_free()
    people.clear()
