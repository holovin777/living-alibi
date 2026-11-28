extends RefCounted
## Optional CC BY vehicle pack. Existing CC0 cars remain a fully offline fallback.
const PATHS: Array[String]=[
    "res://assets/vehicles/realistic/sport-sedan.glb",
    "res://assets/vehicles/realistic/hot-hatchback.glb",
    "",
    "res://assets/vehicles/realistic/luxury-suv.glb"
]
# Source dimensions in meters (including mirrors). Fit the current road/parking
# envelope uniformly until collisions and parking use per-model dimensions.
const WIDTHS: Array[float]=[2.10,2.10,1.0,2.35]
const LENGTHS: Array[float]=[4.81,4.22,1.0,4.96]

static func build(index: int, fallback: PackedScene, paint: Color) -> Node3D:
    var path: String=PATHS[index]
    var scene: PackedScene
    if not path.is_empty() and ResourceLoader.exists(path,"PackedScene"):
        scene=ResourceLoader.load(path,"PackedScene") as PackedScene
    var realistic: bool=scene!=null
    if not realistic:scene=fallback
    var model:=scene.instantiate() as Node3D
    model.name="ImportedVehicle"
    model.rotation.y=PI
    model.set_meta("realistic_vehicle",realistic)
    if realistic:
        var fit:=minf(1.85/WIDTHS[index],3.9/LENGTHS[index])
        model.scale=Vector3.ONE*fit
        model.set_meta("rear_z",LENGTHS[index]*fit*0.5)
        if paint.a>0.0:
            _paint(model,paint,{})
    else:
        model.scale=Vector3(1.18,1.20,1.32)
    return model

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
