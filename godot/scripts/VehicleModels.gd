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
    if door==null:return
    if not door.has_meta("closed_position"):door.set_meta("closed_position",door.position)
    var origin: Vector3=door.get_meta("closed_position")
    door.position=origin if closed else origin+Vector3(-0.12,0,-0.75)

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
