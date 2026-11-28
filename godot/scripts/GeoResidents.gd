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
signal interior_changed(room: Dictionary)
var room: Dictionary={}
var first:=true
var life_key:=""
var office_remark: Dictionary={}
var remark_seen:=""
var remark_time:=0.0
var remark_label: Label3D
var action_pending:=false
var driving: Dictionary={}
var driving_request:=false
signal drive_pose(pose: Dictionary)

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
    _update_remark(pos,delta)
    if elapsed>=1.0 and not action_pending and http.get_http_client_status()==HTTPClient.STATUS_DISCONNECTED:
        elapsed=0
        http.request("http://127.0.0.1:8091/geo/nearby?x=%.3f&z=%.3f" % [pos.x,pos.y])
    for id in people:
        var record: Dictionary=people[id]
        var node: Node3D=record["node"]
        var goal: Vector3=record["goal"]
        var direction:=goal-node.position
        var pose:=str(record.get("pose","STAND"))
        var left: Node3D=node.get_node("ArmL")
        var right: Node3D=node.get_node("ArmR")
        var leg_l: Node3D=node.get_node("LegL")
        var leg_r: Node3D=node.get_node("LegR")
        record["phase"]=float(record["phase"])+delta*3.0
        var wave:=sin(float(record["phase"]))
        if not room.is_empty() and pose!="STAND":
            node.position=node.position.move_toward(goal,delta*5)
            node.rotation.y=float(record.get("heading",0))
            leg_l.rotation.x=-1.15 if pose=="STUDY" else 0.0
            leg_r.rotation.x=-1.15 if pose=="STUDY" else 0.0
            left.rotation.x=-0.8+wave*0.15
            right.rotation.x=(-1.4+wave*0.45) if pose=="TEACH" else (-0.8-wave*0.28)
        elif direction.length()>0.03:
            node.position=node.position.move_toward(goal,delta*12.0)
            node.rotation.y=atan2(-direction.x,-direction.z)
            record["phase"]=float(record["phase"])+delta*7
            node.get_node("LegL").rotation.x=sin(float(record["phase"]))*0.32
            node.get_node("LegR").rotation.x=-sin(float(record["phase"]))*0.32
            left.rotation.x=-wave*0.25
            right.rotation.x=wave*0.25
        else:
            leg_l.rotation.x=0
            leg_r.rotation.x=0
            left.rotation.x=0
            right.rotation.x=0

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
    for remark in parsed.get("officeRemarks",[]):
        var key:=str(remark.get("id",""))
        if not key.is_empty() and key!=remark_seen:
            remark_seen=key
            office_remark=remark
            remark_time=6.0
    var next_room: Dictionary=parsed["interior"] if parsed.get("interior") is Dictionary else {}
    if str(next_room.get("id",""))!=str(room.get("id","")) or next_room.get("x")!=room.get("x") or next_room.get("z")!=room.get("z"):
        room=next_room
        for record in people.values():record["node"].queue_free()
        people.clear()
        interior_changed.emit(room)
    if parsed.get("playerDistrict") is Dictionary:
        var place: Dictionary=parsed["playerDistrict"]
        var life: Dictionary=parsed.get("life",{})
        var next_key:=str(life.get("phase","ADULT"))+":"+str(life.get("name",""))+":"+str(place.get("id",""))
        if room.is_empty() and (first or (next_key!=life_key and (bool(parsed.get("rebirth",false)) or not life_key.begins_with("ADULT:")))):
            first_snapshot.emit(Vector2(float(place["x"]),float(place["z"])))
        first=false
        life_key=next_key
    population=int(parsed.get("population",0))
    commuting=int(parsed.get("commuting",0))
    day=int(parsed.get("day",1))
    minute=int(parsed.get("minute",0))
    var seen: Dictionary={}
    for person in parsed["residents"]:
        var id:=str(person["id"])
        var pose:=str(person.get("pose","STAND"))
        var goal:=Vector3(float(person["x"]),0.05 if room.is_empty() else (149.84 if pose=="STUDY" else 150.05),float(person["z"]))
        seen[id]=true
        if not people.has(id):
            var profile: Dictionary=person.get("appearance",{}) if person.get("appearance") is Dictionary else {}
            var node: Node3D=traffic._person(profile,id)
            add_child(node)
            node.position=goal
            people[id]={"node":node,"goal":goal,"phase":0.0,"talkable":bool(person.get("talkable",false))}
        else:
            people[id]["goal"]=goal
            people[id]["talkable"]=bool(person.get("talkable",false))
        people[id]["pose"]=pose
        people[id]["heading"]=float(person.get("heading",0))
        _appearance(people[id],person)
    for id in people.keys():
        if not seen.has(id):
            people[id]["node"].queue_free()
            people.erase(id)

func _update_remark(pos: Vector2,delta: float) -> void:
    remark_time=maxf(0.0,remark_time-delta)
    var id:=str(office_remark.get("speakerId",""))
    if remark_time<=0 or str(room.get("id",""))!="OFFICE" or not people.has(id):
        if is_instance_valid(remark_label):remark_label.hide()
        return
    var actor: Node3D=people[id]["node"]
    if not is_instance_valid(remark_label):
        remark_label=Label3D.new()
        remark_label.billboard=BaseMaterial3D.BILLBOARD_ENABLED
        remark_label.font_size=36
        remark_label.pixel_size=0.0045
        remark_label.outline_size=8
        add_child(remark_label)
    remark_label.visible=pos.distance_to(Vector2(actor.position.x,actor.position.z))<=7
    remark_label.position=actor.position+Vector3.UP*2.05
    remark_label.text=str(office_remark.get("italian",""))

func _appearance(record: Dictionary,person: Dictionary) -> void:
    var node: Node3D=record["node"]
    var stage:=str(person.get("appearanceStage","ADULT"))
    var height:=1.0
    if stage=="PRESCHOOL":height=0.57
    elif stage=="CHILD":height=0.72
    elif stage=="TEEN":height=0.9
    var profile: Dictionary=person.get("appearance",{}) if person.get("appearance") is Dictionary else {}
    var build:=float(profile.get("build",100))/100.0
    var adult_height:=float(profile.get("height",100))/100.0
    var width:=0.62 if stage=="PRESCHOOL" else (0.75 if stage=="CHILD" else (0.88 if stage=="TEEN" else 1.0))
    if stage=="SENIOR":height=0.95
    node.scale=Vector3(build*width,adult_height*height,build*width)
    var hair:=node.get_node_or_null("Hair") as MeshInstance3D
    if hair!=null:
        var base: Color=hair.get_meta("base_color")
        var shade:=base.lerp(Color("#d2d0c8"),0.75 if stage=="SENIOR" else (0.27 if stage=="MATURE" else 0.0))
        hair.material_override=traffic._mat(shade)
    var outfit:=str(person.get("outfitVisual",""))
    var work:=str(person.get("workVisual",""))
    var signature:=outfit+":"+work
    if str(record.get("appearance",""))==signature:return
    record["appearance"]=signature
    var old:=node.get_node_or_null("Equipment")
    if old!=null:
        node.remove_child(old)
        old.queue_free()
    var equipment:=Node3D.new()
    equipment.name="Equipment"
    node.add_child(equipment)
    var cloth:=Color.TRANSPARENT
    match outfit:
        "DOCTOR_COAT":cloth=Color("#eeeae0")
        "SCRUBS":cloth=Color("#77b7ad")
        "TECHNICIAN":cloth=Color("#4b6681")
        "HIGH_VIS":cloth=Color("#e0ad42")
        "BAKER_APRON","CAFE_APRON":cloth=Color("#b9aa91")
        "FIREFIGHTER":cloth=Color("#373c43")
        "POLICE":cloth=Color("#294569")
    if cloth.a>0:traffic._box(equipment,Vector3(0.43,0.50,0.26),Vector3(0,1.04,0),cloth)
    if outfit=="BACKPACK":traffic._box(equipment,Vector3(0.29,0.35,0.16),Vector3(0,1.03,0.19),Color("#5d727c"))
    if work in ["PAPERS","TEACHING"]:traffic._box(equipment,Vector3(0.25,0.035,0.20),Vector3(0.23,0.88,-0.32),Color("#f4ead6"))
    elif work=="WRENCH":traffic._box(equipment,Vector3(0.07,0.05,0.25),Vector3(0.27,0.91,-0.32),Color("#9ca5ac"))
    elif work=="BREAD":traffic._box(equipment,Vector3(0.24,0.12,0.16),Vector3(0.22,0.92,-0.32),Color("#b58443"))
    elif work=="COFFEE":traffic._box(equipment,Vector3(0.13,0.14,0.13),Vector3(0.22,0.94,-0.32),Color("#e3ded0"))
    elif work=="CRATE":traffic._box(equipment,Vector3(0.36,0.22,0.25),Vector3(0,0.85,-0.32),Color("#ac875a"))

func enter_district(pos: Vector2) -> void:
    if not connected or (room.is_empty() and not bool(world.get("canEnter",false))):return
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

func drive_command(command: String,pos: Vector2,turn: int=0) -> void:
    if not connected or action_pending:return
    driving_request=true
    _action("drive",{"command":command,"x":pos.x,"z":pos.y,"turn":turn})
    if not action_pending:driving_request=false

func perform(id: String,pos: Vector2) -> void:
    _action("action",{"id":id,"x":pos.x,"z":pos.y})

func _action(endpoint: String,data: Dictionary) -> void:
    if not connected or action_pending or action_http.get_http_client_status()!=HTTPClient.STATUS_DISCONNECTED:return
    # Long childhood batches still simulate every minute; do not abandon a
    # running request after the ordinary interaction timeout.
    action_http.timeout=0.0 if str(data.get("id",""))=="rebirth-wait-extended" else 30.0
    action_pending=action_http.request("http://127.0.0.1:8091/geo/"+endpoint,["Content-Type: application/json"],HTTPClient.METHOD_POST,JSON.stringify(data))==OK
    if action_pending and str(data.get("id",""))=="rebirth-wait-extended":
        last_message="Simulating childhood / Simulazione infanzia..."

func _action_received(result: int,code: int,_headers: PackedStringArray,body: PackedByteArray) -> void:
    action_pending=false
    var parsed=JSON.parse_string(body.get_string_from_utf8())
    if driving_request:
        driving_request=false
        if result==HTTPRequest.RESULT_SUCCESS and code==200 and parsed is Dictionary:
            driving=parsed
            drive_pose.emit(parsed)
        elif bool(world.get("confined",false)):
            driving.clear()
    if result==HTTPRequest.RESULT_SUCCESS and code==200 and parsed is Dictionary and parsed.get("arrival") is Dictionary:
        var arrival: Dictionary=parsed["arrival"]
        first_snapshot.emit(Vector2(float(arrival["x"]),float(arrival["z"])))
    last_message=str(parsed.get("message","Action failed")) if result==HTTPRequest.RESULT_SUCCESS and parsed is Dictionary else "Java action failed"
    elapsed=1.0

func _offline() -> void:
    connected=false
    remark_time=0
    if is_instance_valid(remark_label):remark_label.hide()
    world.clear()
    traffic.set_java_population(enabled)
    for record in people.values():record["node"].queue_free()
    people.clear()
