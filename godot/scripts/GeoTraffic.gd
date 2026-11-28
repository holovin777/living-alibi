extends Node3D
## Bounded local traffic preview. Actors follow connected, directed OSM edges.
## No authoritative identities, schedules, or collision physics yet.
const RoadGraph=preload("res://scripts/GeoRoadGraph.gd")
const CAR_MODELS: Array[PackedScene]=[
    preload("res://assets/vehicles/sedan.glb"),
    preload("res://assets/vehicles/hatchback_sports.glb"),
    preload("res://assets/vehicles/van.glb"),
    preload("res://assets/vehicles/suv.glb")
]
const MAX_CARS=14
const MAX_PEOPLE=24
const SPAWN_MIN=25.0
const SPAWN_MAX=100.0
const DESPAWN_DISTANCE=180.0
var motor=RoadGraph.new()
var foot=RoadGraph.new()
var actors: Array[Dictionary]=[]
var rng:=RandomNumberGenerator.new()
var clock:=0.0
var cursor:=0
var java_population:=false

func set_java_population(enabled: bool) -> void:
    if java_population==enabled:return
    java_population=enabled
    if enabled:
        for i in range(actors.size()-1,-1,-1):
            if actors[i]["mode"] in ["walk","drive"]:
                actors[i]["node"].queue_free()
                actors.remove_at(i)
var car_colors: Array[Color]=[Color("#a63938"),Color("#3e6580"),Color("#d0ccc0"),Color("#55585a"),Color("#777f59")]
var shirts: Array[Color]=[Color("#9e6d50"),Color("#4e747d"),Color("#84708d"),Color("#576e51"),Color("#c8b89e")]
var materials: Dictionary={}

func configure(graph: Dictionary) -> void:
    motor.build(graph,"drive")
    foot.build(graph,"walk")
    rng.seed=7217

func _mat(color: Color) -> StandardMaterial3D:
    if not materials.has(color):
        var mat:=StandardMaterial3D.new()
        mat.albedo_color=color
        materials[color]=mat
    return materials[color]

func _box(parent: Node3D,dimensions: Vector3,at: Vector3,color: Color) -> MeshInstance3D:
    var mesh:=BoxMesh.new()
    mesh.size=dimensions
    var part:=MeshInstance3D.new()
    part.mesh=mesh
    part.position=at
    part.material_override=_mat(color)
    parent.add_child(part)
    return part

func _car(paint: Color=Color.TRANSPARENT, imported: bool=true) -> Node3D:
    var root:=Node3D.new()
    if imported:
        var index:=rng.randi_range(0,CAR_MODELS.size()-1) if paint.a==0 else posmod(str(paint).hash(),CAR_MODELS.size())
        var model:=CAR_MODELS[index].instantiate() as Node3D
        model.name="ImportedVehicle"
        model.rotation.y=PI
        model.scale=Vector3(1.18,1.20,1.32)
        root.add_child(model)
        return root
    var color: Color=car_colors[rng.randi_range(0,car_colors.size()-1)] if paint.a==0 else paint
    _box(root,Vector3(1.65,0.62,3.55),Vector3(0,0.65,0),color)
    _box(root,Vector3(1.42,0.53,1.83),Vector3(0,1.16,0.15),Color("#394e59"))
    _box(root,Vector3(1.43,0.10,1.83),Vector3(0,1.47,0.15),color)
    for x in [-0.74,0.74]:
        for z in [-1.16,1.16]:
            var wheel:=MeshInstance3D.new()
            var cylinder:=CylinderMesh.new()
            cylinder.top_radius=0.32
            cylinder.bottom_radius=0.32
            cylinder.height=0.23
            cylinder.radial_segments=10
            wheel.mesh=cylinder
            wheel.rotation.z=PI/2
            wheel.position=Vector3(x,0.34,z)
            wheel.material_override=_mat(Color("#252729"))
            root.add_child(wheel)
        _box(root,Vector3(0.35,0.15,0.06),Vector3(x*0.83,0.72,-1.8),Color("#fff3c3"))
        _box(root,Vector3(0.35,0.15,0.06),Vector3(x*0.83,0.72,1.8),Color("#9b2726"))
    return root

func _person(profile: Dictionary={},identity: String="") -> Node3D:
    var root:=Node3D.new()
    var skins := [Color("#8e5a41"),Color("#b9845f"),Color("#c69574"),Color("#d8aa87"),Color("#edc4a6")]
    var hairs := [Color("#262322"),Color("#382b25"),Color("#594437"),Color("#79513a"),Color("#b58c54")]
    var eyes := [Color("#413427"),Color("#50694c"),Color("#527387"),Color("#807154")]
    var skin: Color=skins[clampi(int(profile.get("skin",2)),0,4)]
    var hair: Color=hairs[clampi(int(profile.get("hair",1)),0,4)]
    var eye: Color=eyes[clampi(int(profile.get("eye",0)),0,3)]
    var face:=float(profile.get("face",100))/100.0
    var nose:=float(profile.get("nose",100))/100.0
    var shirt_index:=rng.randi_range(0,shirts.size()-1) if identity.is_empty() else posmod(identity.hash(),shirts.size())
    _box(root,Vector3(0.40,0.58,0.24),Vector3(0,1.06,0),shirts[shirt_index])
    _box(root,Vector3(0.25*face,0.27,0.23),Vector3(0,1.49,0),skin)
    if not profile.is_empty():
        var top:=_box(root,Vector3(0.26*face,0.075,0.24),Vector3(0,1.63,0),hair)
        top.name="Hair"
        top.set_meta("base_color",hair)
        for side in [-1.0,1.0]:
            _box(root,Vector3(0.027,0.021,0.012),Vector3(side*0.059*face,1.52,-0.119),eye)
        _box(root,Vector3(0.034,0.048,0.035*nose),Vector3(0,1.478,-0.127),skin)
    for x in [-0.14,0.14]:
        var leg:=_box(root,Vector3(0.15,0.67,0.19),Vector3(x,0.43,0),Color("#3d454c"))
        leg.name="LegL" if x<0 else "LegR"
        var arm:=Node3D.new()
        arm.name="ArmL" if x<0 else "ArmR"
        arm.position=Vector3(x*2.0,1.29,0)
        root.add_child(arm)
        _box(arm,Vector3(0.13,0.52,0.16),Vector3(0,-0.26,0),skin)
    return root

func count(mode: String) -> int:
    var n:=0
    for actor in actors:
        if actor["mode"]==mode:n+=1
    return n

func _graph(mode: String):
    return motor if mode=="drive" else foot

func _choose_next(graph,at: int,previous: int) -> int:
    var choices: PackedInt64Array=graph.network.get_point_connections(at)
    if choices.is_empty():return -1
    var onward: Array[int]=[]
    for id in choices:
        if id!=previous:onward.append(id)
    if onward.is_empty():return int(choices[0])
    return onward[rng.randi_range(0,onward.size()-1)]

func _spawn(mode: String,player: Vector2) -> bool:
    var graph=_graph(mode)
    var candidates: Array[int]=[]
    var ids: Dictionary=graph.drive_nodes if mode=="drive" else graph.walk_nodes
    for id in ids:
        var p: Vector2=graph.coordinates[id]
        var distance:=p.distance_to(player)
        if distance>=SPAWN_MIN and distance<=SPAWN_MAX and not graph.network.get_point_connections(id).is_empty():
            candidates.append(id)
    if candidates.is_empty():return false
    for attempt in range(8):
        var from: int=candidates[rng.randi_range(0,candidates.size()-1)]
        var to:=_choose_next(graph,from,-1)
        if to<0:continue
        var start: Vector2=graph.coordinates[from]
        var crowded:=false
        for actor in actors:
            if actor["mode"]!=mode:continue
            var existing: Node3D=actor["node"]
            if start.distance_to(Vector2(existing.position.x,existing.position.z))<5.0:crowded=true;break
        if crowded:continue
        var node:=_car() if mode=="drive" else _person()
        add_child(node)
        var speed:=rng.randf_range(5.0,9.0) if mode=="drive" else rng.randf_range(0.85,1.65)
        var actor: Dictionary={"node":node,"mode":mode,"from":from,"to":to,"progress":0.0,"speed":speed,"phase":rng.randf_range(0,TAU),"waiting":0.0}
        actors.append(actor)
        _pose(actor,graph,0.0)
        return true
    return false

func _pose(actor: Dictionary,graph,delta: float) -> void:
    var node: Node3D=actor["node"]
    var a: Vector2=graph.coordinates[actor["from"]]
    var b: Vector2=graph.coordinates[actor["to"]]
    var dir: Vector2=(b-a).normalized()
    var side:=Vector2(-dir.y,dir.x)
    var lane:=0.8 if actor["mode"]=="drive" else 1.25
    var pos:=a.lerp(b,clampf(float(actor["progress"]),0.0,1.0))+side*lane
    node.position=Vector3(pos.x,0.05,pos.y)
    node.rotation.y=atan2(-dir.x,-dir.y)
    if actor["mode"]=="walk":
        actor["phase"]=float(actor["phase"])+delta*7.0
        node.get_node("LegL").rotation.x=sin(float(actor["phase"]))*0.32
        node.get_node("LegR").rotation.x=-sin(float(actor["phase"]))*0.32

func _advance(actor: Dictionary,player: Vector2,delta: float) -> void:
    var graph=_graph(actor["mode"])
    var node: Node3D=actor["node"]
    var pos:=Vector2(node.position.x,node.position.z)
    var a: Vector2=graph.coordinates[actor["from"]]
    var b: Vector2=graph.coordinates[actor["to"]]
    var dir: Vector2=(b-a).normalized()
    var gap:=3.8 if actor["mode"]=="drive" else 0.9
    var ahead:=player-pos
    if ahead.length()<gap and ahead.dot(dir)>-0.5:return
    for other in actors:
        if other["node"]==node:continue
        var other_node: Node3D=other["node"]
        var offset:=Vector2(other_node.position.x,other_node.position.z)-pos
        if offset.length()<gap and offset.dot(dir)>0.1 and absf(offset.cross(dir))<1.4:return
    var remaining:=float(actor["speed"])*delta
    # A finite loop also handles very short OSM segments.
    for step in range(12):
        a=graph.coordinates[actor["from"]]
        b=graph.coordinates[actor["to"]]
        var length:=maxf(a.distance_to(b),0.05)
        var available: float=length*(1.0-float(actor["progress"]))
        if remaining<available:
            actor["progress"]=float(actor["progress"])+remaining/length
            break
        remaining-=available
        var next:=_choose_next(graph,int(actor["to"]),int(actor["from"]))
        if next<0:
            actor["progress"]=1.0
            actor["waiting"]=float(actor["waiting"])+delta
            break
        actor["from"]=actor["to"]
        actor["to"]=next
        actor["progress"]=0.0
    _pose(actor,graph,delta)

func update_traffic(player: Vector2,delta: float) -> void:
    for i in range(actors.size()-1,-1,-1):
        var actor: Dictionary=actors[i]
        var node: Node3D=actor["node"]
        if Vector2(node.position.x,node.position.z).distance_to(player)>DESPAWN_DISTANCE:
            node.queue_free()
            actors.remove_at(i)
        else:
            _advance(actor,player,delta)
    clock+=delta
    if clock<0.4:return
    clock=0.0
    # Gradual spawn keeps the frame cost stable. Dead ends stop rather than reverse illegally.
    if not java_population and count("drive")<MAX_CARS:_spawn("drive",player)
    if not java_population and count("walk")<MAX_PEOPLE:_spawn("walk",player)
