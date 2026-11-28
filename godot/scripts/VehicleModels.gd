extends RefCounted
## Optional CC BY vehicle pack. Existing CC0 cars remain a fully offline fallback.
const PATHS: Array[String]=[
    "res://assets/vehicles/realistic/sport-sedan.glb",
    "res://assets/vehicles/realistic/hot-hatchback.glb",
    "res://assets/vehicles/realistic/city-van.glb",
    "res://assets/vehicles/realistic/luxury-suv.glb"
]
# Source dimensions in meters (including mirrors). Fit the current road/parking
# envelope uniformly until collisions and parking use per-model dimensions.
const WIDTHS: Array[float]=[2.10,2.10,2.33,2.35]
const LENGTHS: Array[float]=[4.81,4.22,4.32,4.96]

static func build(index: int, fallback: PackedScene, paint: Color) -> Node3D:
    var model:=_realistic(PATHS[index],WIDTHS[index],LENGTHS[index],1.85,3.62,paint)
    if model!=null:return model
    model=fallback.instantiate() as Node3D
    model.name="ImportedVehicle"
    model.rotation.y=PI
    model.set_meta("realistic_vehicle",false)
    model.scale=Vector3(1.18,1.20,1.32)
    return model

static func minibus(max_width: float=2.4,max_length: float=7.5) -> Node3D:
    return _realistic("res://assets/vehicles/realistic/minibus.glb",2.51,6.96,max_width,max_length,Color.TRANSPARENT)

static func city_bus() -> Node3D:
    return _realistic("res://assets/vehicles/realistic/city-bus.glb",2.98,12.17,2.90,12.17,Color.TRANSPARENT)

static func box_truck(max_width: float=2.45,max_length: float=7.2,paint: Color=Color.TRANSPARENT) -> Node3D:
    return _realistic("res://assets/vehicles/realistic/box-truck.glb",2.80,6.65,max_width,max_length,paint)

static func _realistic(path: String,width: float,length: float,max_width: float,max_length: float,paint: Color) -> Node3D:
    if not ResourceLoader.exists(path,"PackedScene"):return null
    var scene:=ResourceLoader.load(path,"PackedScene") as PackedScene
    if scene==null:return null
    var model:=scene.instantiate() as Node3D
    if model==null:return null
    model.name="ImportedVehicle"
    model.rotation.y=PI
    var fit:=minf(1.0,minf(max_width/width,max_length/length))
    model.scale=Vector3.ONE*fit
    model.set_meta("realistic_vehicle",true)
    model.set_meta("rear_z",length*fit*0.5)
    model.set_meta("vehicle_width",width*fit)
    mark_wheels(model)
    if paint.a>0.0:_paint(model,paint,{})
    configure_lod(model)
    return model

# Godot generates mesh LODs during the normal GLB import. Lower bias
# selects simpler imported meshes earlier, keeping silhouettes intact.
static func configure_lod(node: Node) -> void:
    if node is MeshInstance3D:
        var mesh:=node as MeshInstance3D
        mesh.lod_bias=0.65
        # Only clearly named cabin parts; never hide glazing or body panels.
        var n:=str(mesh.name).to_lower()
        if n.begins_with("seat_") or n.begins_with("headrest_") or n.begins_with("pedal_"):
            mesh.visibility_range_end=32.0
            mesh.visibility_range_end_margin=4.0
    for child in node.get_children():configure_lod(child)

static func set_bus_door(bus: Node3D,closed: bool) -> void:
    var proxy:=bus.get_node_or_null("PassengerDoor") as Node3D
    if proxy!=null:proxy.visible=closed
    var model:=bus.get_node_or_null("ImportedVehicle") as Node3D
    if model==null:return
    var door:=model.find_child("Sliding_Door",true,false) as Node3D
    if door==null:
        for name in ["Door_1a","Door_1b","Door_2a","Door_2b"]:
            var leaf:=model.find_child(name,true,false) as Node3D
            if leaf==null:continue
            if not leaf.has_meta("closed_y"):leaf.set_meta("closed_y",leaf.rotation.y)
            var offset:=0.0 if closed else (-1.4 if name.ends_with("a") else 1.4)
            leaf.rotation.y=float(leaf.get_meta("closed_y"))+offset
        return
    if not door.has_meta("closed_position"):door.set_meta("closed_position",door.position)
    var origin: Vector3=door.get_meta("closed_position")
    door.position=origin if closed else origin+Vector3(-0.12,0,-0.75)

static func mark_wheels(node: Node3D) -> void:
    var n:=str(node.name).to_lower().replace("_","-")
    if n.begins_with("wheel-") and (n.ends_with("-l") or n.ends_with("-r") or n.ends_with("-left") or n.ends_with("-right") or n in ["wheel-fl","wheel-fr","wheel-rl","wheel-rr"]):
        node.set_meta("rolling_wheel",true)
        node.set_meta("wheel_rest",node.basis)
        node.set_meta("wheel_right",n.ends_with("-r") or n.ends_with("-right") or n in ["wheel-fr","wheel-rr"])
        return
    for child in node.get_children():
        if child is Node3D:mark_wheels(child)

static func roll_wheel(wheel: Node3D,distance: float,radius: float=0.36) -> void:
    if not wheel.has_meta("wheel_rest"):
        wheel.rotation.x=fposmod(wheel.rotation.x+distance/radius,TAU)
        return
    var direction: float=-1.0 if wheel.get_meta("wheel_right",false) else 1.0
    var angle:=fposmod(float(wheel.get_meta("wheel_angle",0.0))+direction*distance/radius,TAU)
    wheel.set_meta("wheel_angle",angle)
    var rest: Basis=wheel.get_meta("wheel_rest")
    wheel.basis=rest*Basis(Vector3.RIGHT,angle)

static func roll_tree(node: Node3D,distance: float,radius: float=0.36) -> void:
    if node.has_meta("rolling_wheel"):
        roll_wheel(node,distance,radius)
        return
    for child in node.get_children():
        if child is Node3D:roll_tree(child,distance,radius)

static func _paint(node: Node, color: Color, cache: Dictionary) -> void:
    if node is MeshInstance3D:
        var part:=node as MeshInstance3D
        if part.mesh!=null:
            for i in range(part.mesh.get_surface_count()):
                var material:=part.get_active_material(i) as StandardMaterial3D
                if material==null or not material.resource_name.to_lower().begins_with("paint"):
                    continue
                var id:=material.get_instance_id()
                if not cache.has(id):
                    var copy:=material.duplicate() as StandardMaterial3D
                    copy.albedo_color=color
                    cache[id]=copy
                part.set_surface_override_material(i,cache[id])
    for child in node.get_children():
        _paint(child,color,cache)
