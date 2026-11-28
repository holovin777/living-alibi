extends Node3D
## Lightweight first-person cabin, independent of authoritative road simulation.
var steering: Node3D
var speed_display: Label3D
var materials: Dictionary={}

func _box(at: Vector3,size: Vector3,color: Color) -> MeshInstance3D:
    var mesh:=BoxMesh.new()
    mesh.size=size
    var part:=MeshInstance3D.new()
    part.mesh=mesh
    if not materials.has(color):
        var mat:=StandardMaterial3D.new()
        mat.albedo_color=color
        mat.roughness=0.76
        materials[color]=mat
    part.material_override=materials[color]
    part.position=at
    add_child(part)
    return part

func _ready() -> void:
    var dark:=Color("#282e32")
    var trim:=Color("#4b555a")
    _box(Vector3(0,0.77,-0.65),Vector3(1.68,0.23,0.50),dark)
    _box(Vector3(0,0.56,-1.25),Vector3(1.70,0.12,0.95),Color("#55585a"))
    _box(Vector3(0,1.65,0.12),Vector3(1.67,0.08,1.48),trim)
    for side in [-1.0,1.0]:
        var x: float=float(side)*0.78
        var pillar:=_box(Vector3(x,1.23,-0.66),Vector3(0.095,0.84,0.12),trim)
        pillar.rotation.x=-0.24
        _box(Vector3(x,0.79,0.06),Vector3(0.14,0.44,1.37),dark)
        _box(Vector3(x*1.23,1.10,-0.43),Vector3(0.26,0.14,0.16),dark)
        _box(Vector3(x*1.23,1.10,-0.34),Vector3(0.21,0.10,0.018),Color("#81949f"))
    _box(Vector3(-0.33,0.95,-0.58),Vector3(0.43,0.16,0.10),Color("#151e22"))
    speed_display=Label3D.new()
    speed_display.name="SpeedDisplay"
    speed_display.position=Vector3(-0.33,0.96,-0.52)
    speed_display.font_size=42
    speed_display.pixel_size=0.0018
    speed_display.modulate=Color("#b6e7cf")
    speed_display.outline_size=0
    speed_display.no_depth_test=false
    add_child(speed_display)
    _box(Vector3(0.19,0.93,-0.55),Vector3(0.28,0.16,0.08),Color("#26363a"))
    for y in [0.80,0.84]:
        _box(Vector3(0.19,float(y),-0.39),Vector3(0.24,0.018,0.03),trim)
    _box(Vector3(0.13,0.43,0.04),Vector3(0.25,0.30,0.58),trim)
    steering=Node3D.new()
    steering.name="SteeringWheel"
    steering.position=Vector3(-0.33,0.97,-0.29)
    add_child(steering)
    var ring:=TorusMesh.new()
    ring.inner_radius=0.145
    ring.outer_radius=0.18
    var wheel:=MeshInstance3D.new()
    wheel.mesh=ring
    wheel.material_override=materials[dark]
    wheel.rotation.x=PI/2.0
    steering.add_child(wheel)
    var spoke:=_box(Vector3.ZERO,Vector3(0.31,0.045,0.035),dark)
    spoke.reparent(steering,false)
    var hub:=_box(Vector3.ZERO,Vector3(0.12,0.085,0.055),trim)
    hub.reparent(steering,false)
    update_instruments(0,0,1)

func update_instruments(speed_kmh: float,turn: float,delta: float) -> void:
    if not is_instance_valid(steering):return
    steering.rotation.z=lerpf(steering.rotation.z,-turn*0.75,minf(delta*8.0,1.0))
    speed_display.text="%02d km/h" % roundi(maxf(0,speed_kmh))
