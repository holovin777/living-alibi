extends Node3D
## Bounded local traffic preview. Actors follow connected, directed OSM edges.
## No authoritative identities, schedules, or collision physics yet.
const RoadGraph=preload("res://scripts/GeoRoadGraph.gd")
const VehicleModels=preload("res://scripts/VehicleModels.gd")
const CAR_MODELS: Array[PackedScene]=[
    preload("res://assets/vehicles/sedan.glb"),
    preload("res://assets/vehicles/hatchback_sports.glb"),
    preload("res://assets/vehicles/van.glb"),
    preload("res://assets/vehicles/suv.glb")
]
const Signals=preload("res://scripts/GeoSignals.gd")
var signal_nodes: Dictionary={}
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
    signal_nodes.clear()
    for raw_id in graph.get("signals",{}):
        if motor.ids.has(raw_id):signal_nodes[motor.ids[raw_id]]=int(graph["signals"][raw_id])
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
        var model:=VehicleModels.build(index,CAR_MODELS[index],paint)
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

func _fire_engine() -> Node3D:
    var root:=_truck()
    for child in root.get_children():
        if child is MeshInstance3D and child.mesh is BoxMesh and child.mesh.size.y>2.0:
            child.material_override=_mat(Color("#c72929"))
    for side in [-1.0,1.0]:
        var x: float=float(side)*1.24
        _box(root,Vector3(0.035,1.25,3.65),Vector3(x,1.90,1.12),Color("#a2abb0"))
        for i in range(6):
            _box(root,Vector3(0.043,0.025,3.62),Vector3(x,1.35+i*0.19,1.12),Color("#667078"))
        var beacon:=_box(root,Vector3(0.47,0.18,0.28),Vector3(float(side)*0.55,3.04,-2.30),Color("#2987f4"))
        beacon.name="BeaconL" if side<0 else "BeaconR"
        var mat:=StandardMaterial3D.new()
        mat.albedo_color=Color("#2987f4")
        mat.emission_enabled=true
        mat.emission=Color("#2987f4")
        mat.emission_energy_multiplier=2.5
        beacon.material_override=mat
    for x in [-0.34,0.34]:
        _box(root,Vector3(0.08,0.09,3.8),Vector3(float(x),3.48,1.10),Color("#b9c4c9"))
    for i in range(10):
        _box(root,Vector3(0.75,0.09,0.07),Vector3(0,3.48,-0.65+i*0.38),Color("#b9c4c9"))
    var light:=OmniLight3D.new()
    light.name="EmergencyLight"
    light.position=Vector3(0,3.2,-2.3)
    light.light_color=Color("#398bff")
    light.light_energy=2.0
    light.omni_range=8.0
    light.shadow_enabled=false
    root.add_child(light)
    var sign:=Label3D.new()
    sign.text="VIGILI DEL FUOCO"
    sign.font_size=40
    sign.pixel_size=0.0024
    sign.modulate=Color.WHITE
    sign.position=Vector3(0,3.0,3.49)
    root.add_child(sign)
    return root

func _ambulance() -> Node3D:
    var root:=Node3D.new()
    var white:=Color("#f0eee7")
    var orange:=Color("#e36e31")
    var glass:=Color("#415968")
    _box(root,Vector3(2.08,0.74,4.75),Vector3(0,0.82,0),white)
    _box(root,Vector3(2.0,1.02,1.68),Vector3(0,1.61,-1.48),white)
    _box(root,Vector3(2.03,1.39,2.96),Vector3(0,1.76,0.91),white)
    _box(root,Vector3(1.76,0.54,0.045),Vector3(0,1.79,-2.34),glass)
    for side in [-1.0,1.0]:
        var x: float=float(side)*1.045
        _box(root,Vector3(0.035,0.26,4.65),Vector3(x,1.10,0),orange)
        _box(root,Vector3(0.035,0.52,0.88),Vector3(x,1.81,-1.49),glass)
        _box(root,Vector3(0.035,0.42,1.08),Vector3(x,1.92,0.91),glass)
        _box(root,Vector3(0.037,0.44,0.13),Vector3(x,1.65,1.78),Color("#2871ad"))
        _box(root,Vector3(0.037,0.13,0.44),Vector3(x,1.65,1.78),Color("#2871ad"))
        for z in [-1.43,1.48]:
            var wheel:=MeshInstance3D.new()
            var tire:=CylinderMesh.new()
            tire.top_radius=0.34
            tire.bottom_radius=0.34
            tire.height=0.23
            tire.radial_segments=16
            wheel.mesh=tire
            wheel.rotation.z=PI/2
            wheel.position=Vector3(x*0.88,0.34,float(z))
            wheel.material_override=_mat(Color("#252729"))
            root.add_child(wheel)
        _box(root,Vector3(0.28,0.17,0.05),Vector3(x*0.74,0.91,-2.40),Color("#fff1d1"))
        _box(root,Vector3(0.24,0.19,0.05),Vector3(x*0.78,0.95,2.40),Color("#b82927"))
        var lamp:=_box(root,Vector3(0.52,0.16,0.26),Vector3(float(side)*0.45,2.53,-0.68),Color("#2785ef"))
        lamp.name="BeaconL" if side<0 else "BeaconR"
        var material:=StandardMaterial3D.new()
        material.albedo_color=Color("#2785ef")
        material.emission_enabled=true
        material.emission=Color("#2785ef")
        material.emission_energy_multiplier=2.5
        lamp.material_override=material
    _box(root,Vector3(1.60,0.24,0.04),Vector3(0,1.12,2.41),orange)
    var sign:=Label3D.new()
    sign.text="AMBULANZA"
    sign.font_size=38
    sign.pixel_size=0.0028
    sign.outline_size=0
    sign.modulate=Color("#1f5790")
    sign.position=Vector3(0,2.24,2.415)
    root.add_child(sign)
    var light:=OmniLight3D.new()
    light.name="EmergencyLight"
    light.position=Vector3(0,2.58,-0.68)
    light.light_color=Color("#398bff")
    light.light_energy=2.0
    light.omni_range=8.0
    light.shadow_enabled=false
    root.add_child(light)
    return root

func set_plate(vehicle: Node3D,number: String,kind: String="CAR") -> void:
    if number.is_empty():return
    var label: Label3D=vehicle.get_node_or_null("RegistrationPlate") as Label3D
    if label==null:
        var rear:=Vector3(0,0.62,2.0)
        var model:=vehicle.get_node_or_null("ImportedVehicle")
        if model!=null and model.has_meta("rear_z"):
            rear.z=float(model.get_meta("rear_z"))+0.02
        if kind=="AMBULANCE":rear=Vector3(0,0.68,2.43)
        elif kind=="BUS":
            rear=Vector3(0,0.92,float(model.get_meta("rear_z"))+0.02 if model!=null and model.has_meta("rear_z") else 3.84)
        elif kind in ["TRUCK","FIRE"]:rear=Vector3(0,0.94,3.57)
        elif kind in ["SCOOTER","MOTORCYCLE"]:rear=Vector3(0,0.62,0.94)
        var plate:=_box(vehicle,Vector3(0.38,0.13,0.025),rear,Color("#f1f0e5"))
        plate.name="RegistrationBacking"
        label=Label3D.new()
        label.name="RegistrationPlate"
        label.position=rear+Vector3(0,0,0.020)
        label.font_size=36
        label.pixel_size=0.00145
        label.outline_size=0
        label.modulate=Color("#17202a")
        vehicle.add_child(label)
    label.text=number

func _two_wheeler(scooter: bool) -> Node3D:
    var root:=Node3D.new()
    var paint:=Color("#8bb6ad") if scooter else Color("#9e3e35")
    var dark:=Color("#272c31")
    var metal:=Color("#a6a9ab")
    for z in [-0.72,0.72]:
        var wheel:=MeshInstance3D.new()
        var tire:=CylinderMesh.new()
        tire.top_radius=0.27 if scooter else 0.32
        tire.bottom_radius=tire.top_radius
        tire.height=0.15
        tire.radial_segments=16
        wheel.mesh=tire
        wheel.rotation.z=PI/2
        wheel.position=Vector3(0,tire.top_radius,float(z))
        wheel.material_override=_mat(dark)
        root.add_child(wheel)
    _box(root,Vector3(0.36,0.25,1.15),Vector3(0,0.48,0.08),paint)
    _box(root,Vector3(0.38,0.15,0.63),Vector3(0,0.87,0.30),dark)
    _box(root,Vector3(0.36,0.30,0.48),Vector3(0,0.76,-0.19),paint)
    _box(root,Vector3(0.12,0.66,0.15),Vector3(0,0.72,-0.62),metal)
    _box(root,Vector3(0.62,0.055,0.055),Vector3(0,1.13,-0.53),metal)
    for x in [-0.29,0.29]:
        _box(root,Vector3(0.17,0.065,0.08),Vector3(float(x),1.13,-0.53),dark)
        _box(root,Vector3(0.05,0.22,0.05),Vector3(float(x),1.24,-0.55),metal)
        _box(root,Vector3(0.13,0.08,0.035),Vector3(float(x),1.36,-0.55),Color("#82949d"))
    _box(root,Vector3(0.19,0.12,0.07),Vector3(0,1.02,-0.68),Color("#fff2cb"))
    _box(root,Vector3(0.20,0.09,0.07),Vector3(0,0.68,0.80),Color("#9c2928"))
    if scooter:_box(root,Vector3(0.46,0.58,0.20),Vector3(0,0.76,-0.48),paint)
    return root

func _truck() -> Node3D:
    var root:=Node3D.new()
    var paint:=Color("#c5c9cb")
    var glass:=Color("#435a67")
    _box(root,Vector3(2.36,0.32,7.20),Vector3(0,0.72,0),Color("#41464b"))
    _box(root,Vector3(2.32,2.22,2.20),Vector3(0,1.83,-2.35),paint)
    _box(root,Vector3(2.15,0.82,0.06),Vector3(0,2.25,-3.47),glass)
    _box(root,Vector3(2.45,2.55,4.70),Vector3(0,2.14,1.12),Color("#88939b"))
    for side in [-1.0,1.0]:
        var x: float=float(side)*1.18
        _box(root,Vector3(0.06,0.82,1.05),Vector3(x,2.25,-2.40),glass)
        for z in [-2.30,1.70,2.75]:
            var wheel:=MeshInstance3D.new()
            var tire:=CylinderMesh.new()
            tire.top_radius=0.49
            tire.bottom_radius=0.49
            tire.height=0.32
            wheel.mesh=tire
            wheel.rotation.z=PI/2
            wheel.position=Vector3(x*0.90,0.49,float(z))
            wheel.material_override=_mat(Color("#252729"))
            root.add_child(wheel)
        _box(root,Vector3(0.30,0.18,0.06),Vector3(x*0.75,0.90,-3.48),Color("#fff1d1"))
        _box(root,Vector3(0.30,0.18,0.06),Vector3(x*0.75,0.91,3.50),Color("#ac342d"))
    return root

func _bus() -> Node3D:
    var root:=Node3D.new()
    var model:=VehicleModels.minibus()
    if model!=null:
        root.add_child(model)
        return root
    var paint:=Color("#d7a53c")
    var glass:=Color("#435a67")
    _box(root,Vector3(2.38,2.38,7.5),Vector3(0,1.58,0),paint)
    _box(root,Vector3(2.27,1.05,0.06),Vector3(0,2.15,-3.78),glass)
    _box(root,Vector3(2.27,0.85,0.06),Vector3(0,2.25,3.78),glass)
    _box(root,Vector3(2.40,0.18,7.52),Vector3(0,2.82,0),Color("#e4ded0"))
    for side in [-1.0,1.0]:
        var x: float=float(side)*1.20
        for i in range(5):
            _box(root,Vector3(0.05,0.93,1.17),Vector3(x,2.13,-2.88+float(i)*1.32),glass)
        for z in [-2.45,2.45]:
            var wheel:=MeshInstance3D.new()
            var tire:=CylinderMesh.new()
            tire.top_radius=0.46
            tire.bottom_radius=0.46
            tire.height=0.30
            wheel.mesh=tire
            wheel.rotation.z=PI/2
            wheel.position=Vector3(x*0.90,0.46,float(z))
            wheel.material_override=_mat(Color("#252729"))
            root.add_child(wheel)
        _box(root,Vector3(0.32,0.15,0.07),Vector3(x*0.74,0.83,-3.78),Color("#fff1d1"))
        _box(root,Vector3(0.32,0.18,0.07),Vector3(x*0.74,0.83,3.78),Color("#ac342d"))
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
        if actor["mode"]=="drive" and signal_nodes.has(actor["to"]):
            var stop_distance:=minf(3.0,length*0.8)
            if available>=stop_distance-0.001 and Signals.phase(signal_nodes[actor["to"]],Signals.seconds(),b-a)!=2:
                var move:=minf(remaining,maxf(0.0,available-stop_distance))
                actor["progress"]=float(actor["progress"])+move/length
                break
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
