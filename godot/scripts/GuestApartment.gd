extends Node3D
const SPAWN := Vector3(0,0,3.0)
var obstacles: Array[Rect2] = []
var resident_name := ""
var unit := 1
var level := 0
var style := "olmo"

func _ready() -> void:
    name="GuestApartment"
    var wall := Color("#e5dfd0") if style=="tiglio" else Color("#eeded9") if style=="rosa" else Color("#d9e3d8")
    var wood := Color("#a28565") if level<2 else Color("#795640")
    var fabric := Color("#a36e73") if style=="rosa" else Color("#647e79")
    _box("Floor",Vector3(9,0.15,8),wood,Vector3(0,-0.085,0))
    _box("Ceiling",Vector3(9,0.15,8),wall,Vector3(0,2.8,0))
    for x in [-4.5,4.5]:
        _box("Wall",Vector3(0.18,2.8,8),wall,Vector3(x,1.4,0))
    for z in [-4.0,4.0]:
        _box("Wall",Vector3(9,2.8,0.18),wall,Vector3(0,1.4,z))
    _box("Door",Vector3(1.2,2.2,0.07),Color("#755139"),Vector3(0,1.1,3.87))
    _box("Handle",Vector3(0.10,0.08,0.1),Color("#dbbd78"),Vector3(0.44,1.05,3.78))
    # Even units have a mirrored bedroom/kitchen layout.
    var side := -1.0 if unit%2==1 else 1.0
    _furniture("BedroomWall",Vector3(1.7*side,0,-1.7),Vector3(0.15,2.7,4.4),wall)
    _furniture("Bed",Vector3(3.1*side,0,-2.6),Vector3(1.65,0.55,2.1),wood)
    _box("Blanket",Vector3(1.55,0.12,1.85),fabric,Vector3(3.1*side,0.61,-2.5))
    _box("Pillow",Vector3(1.1,0.15,0.42),Color("#f7f3df"),Vector3(3.1*side,0.70,-3.3))
    _furniture("Kitchen",Vector3(-2.7*side,0,-3.35),Vector3(2.6,0.95,0.75),wood)
    _box("Worktop",Vector3(2.7,0.09,0.82),Color("#777f80"),Vector3(-2.7*side,1.0,-3.35))
    _box("Sink",Vector3(0.55,0.06,0.50),Color("#b4c1c5"),Vector3(-2.7*side,1.06,-3.35))
    _furniture("Table",Vector3(-2.5*side,0,-0.8),Vector3(1.25,0.75,0.9),wood)
    _furniture("Sofa",Vector3(2.9*side,0,1.8),Vector3(1.2,0.65,1.9),fabric)
    _box("SofaBack",Vector3(0.18,0.65,1.9),fabric,Vector3(3.38*side,0.9,1.8))
    _furniture("CoffeeTable",Vector3(1.5*side,0,1.8),Vector3(0.65,0.43,1.05),wood)
    _box("Window",Vector3(2.1,1.1,0.08),Color("#91b2c0"),Vector3(-1.3,1.7,-3.87))
    if level>=1:
        _box("Rug",Vector3(2.3,0.02,2.3),fabric.lightened(0.25),Vector3(-1.5*side,0.012,1.7))
        _furniture("Bookshelf",Vector3(-3.9*side,0,2.1),Vector3(0.62,1.85,1.15),wood)
        for n in range(5):
            _box("Book",Vector3(0.18,0.30,0.12),fabric.lightened(0.06*n),Vector3(-3.75*side,1.25,1.65+0.18*n))
    if level>=2:
        _box("Art",Vector3(1.35,0.8,0.07),fabric,Vector3(-2.4*side,1.9,3.85))
        _box("CoffeeMachine",Vector3(0.35,0.40,0.35),Color("#414b4f"),Vector3(-3.5*side,1.25,-3.35))
        _furniture("Sideboard",Vector3(-3.6*side,0,0.7),Vector3(0.65,0.95,1.0),wood)
    for at in [Vector3(0,2.5,1.7),Vector3(0,2.5,-2.3)]:
        var lamp := OmniLight3D.new()
        lamp.position=at
        lamp.light_color=Color("#ffe9cf")
        lamp.light_energy=1.7
        lamp.omni_range=7.0
        add_child(lamp)

func _box(label: String,size: Vector3,color: Color,at: Vector3) -> void:
    var node := MeshInstance3D.new()
    node.name=label
    var mesh := BoxMesh.new()
    mesh.size=size
    node.mesh=mesh
    var material := StandardMaterial3D.new()
    material.albedo_color=color
    material.roughness=0.85
    node.material_override=material
    node.position=at
    add_child(node)

func _furniture(label: String,at: Vector3,size: Vector3,color: Color) -> void:
    obstacles.append(Rect2(Vector2(at.x-size.x/2,at.z-size.z/2),Vector2(size.x,size.z)))
    _box(label,size,color,at+Vector3(0,size.y/2,0))

func can_stand(at: Vector3,radius: float) -> bool:
    if absf(at.x)>4.35-radius or absf(at.z)>3.85-radius:return false
    var p := Vector2(at.x,at.z)
    for obstacle in obstacles:
        if p.distance_squared_to(p.clamp(obstacle.position,obstacle.end))<radius*radius:return false
    return true

func near_exit(at: Vector3) -> bool:
    return absf(at.x)<1.2 and at.z>2.3
