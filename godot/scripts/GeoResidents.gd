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
var world: Dictionary={}
var action_http: HTTPRequest
var last_message:=""
signal first_snapshot(point: Vector2)
var first:=true
var action_pending:=false

func configure(source: Node3D, use_server: bool) -> void:
    traffic=source
    enabled=use_server
    if not enabled:return
    http=HTTPRequest.new()
    http.timeout=3.0
    add_child(http)
    http.request_completed.connect(_received)
    action_http=HTTPRequest.new()
    action_http.timeout=30.0
    add_child(action_http)
    action_http.request_completed.connect(_action_received)

func update_residents(pos: Vector2,delta: float) -> void:
    if not enabled:return
    elapsed+=delta
    if elapsed>=1.0 and not action_pending and http.get_http_client_status()==HTTPClient.STATUS_DISCONNECTED:
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
    world=parsed
    if first and parsed.get("playerDistrict") is Dictionary:
        first=false
        var place: Dictionary=parsed["playerDistrict"]
        first_snapshot.emit(Vector2(float(place["x"]),float(place["z"])))
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
            people[id]={"node":node,"goal":goal,"phase":0.0,"talkable":bool(person.get("talkable",false))}
        else:
            people[id]["goal"]=goal
            people[id]["talkable"]=bool(person.get("talkable",false))
    for id in people.keys():
        if not seen.has(id):
            people[id]["node"].queue_free()
            people.erase(id)

func enter_district(pos: Vector2) -> void:
    if not connected or not bool(world.get("canEnter",false)):return
    var destination: Dictionary=world.get("nearestDistrict",{})
    _action("enter",{"destination":destination.get("id",""),"x":pos.x,"z":pos.y})

func nearby_talker(pos: Vector2) -> String:
    var best:=4.0
    var id:=""
    for key in people:
        var record: Dictionary=people[key]
        if not bool(record.get("talkable",false)):continue
        var node: Node3D=record["node"]
        var distance:=pos.distance_to(Vector2(node.position.x,node.position.z))
        if distance<best:
            best=distance
            id=str(key)
    return id

func speak(id: String,message: String,pos: Vector2) -> void:
    _action("speak",{"id":id,"message":message,"language":"it","x":pos.x,"z":pos.y})

func perform(id: String,pos: Vector2) -> void:
    _action("action",{"id":id,"x":pos.x,"z":pos.y})

func _action(endpoint: String,data: Dictionary) -> void:
    if not connected or action_pending or action_http.get_http_client_status()!=HTTPClient.STATUS_DISCONNECTED:return
    action_pending=action_http.request("http://127.0.0.1:8091/geo/"+endpoint,["Content-Type: application/json"],HTTPClient.METHOD_POST,JSON.stringify(data))==OK

func _action_received(result: int,code: int,_headers: PackedStringArray,body: PackedByteArray) -> void:
    action_pending=false
    var parsed=JSON.parse_string(body.get_string_from_utf8())
    if result==HTTPRequest.RESULT_SUCCESS and code==200 and parsed is Dictionary and parsed.get("arrival") is Dictionary:
        var arrival: Dictionary=parsed["arrival"]
        first_snapshot.emit(Vector2(float(arrival["x"]),float(arrival["z"])))
    last_message=str(parsed.get("message","Action failed")) if result==HTTPRequest.RESULT_SUCCESS and parsed is Dictionary else "Java action failed"
    elapsed=1.0

func _offline() -> void:
    connected=false
    world.clear()
    traffic.set_java_population(enabled)
    for record in people.values():record["node"].queue_free()
    people.clear()
