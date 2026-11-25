extends Node3D
## Local first-person interior. Java owns the private scene and all home actions.
const SPAWN := Vector3(0,0,3.5)
const KITCHEN := Vector3(3.3,0,-2.6)
const BED := Vector3(-4.0,0,-2.2)
const LIVING := Vector3(1.1,0,1.5)
const ANCHORS := [Vector3(-1.2,0,1.5),Vector3(2.5,0,2.3),Vector3(-3.3,0,-0.6),Vector3(3.8,0,-0.7),Vector3(-2.2,0,3.4)]
var obstacles: Array[Rect2] = []
var residents: Node3D

func _ready() -> void:
    name = "FamilyApartment"
    var warm := Color("#eee0ca")
    var wood := Color("#987653")
    var floor_color := Color("#c8a581")
    _box("Floor",Vector3(12,0.18,9),floor_color,Vector3(0,-0.10,0))
    _box("Ceiling",Vector3(12,0.15,9),warm,Vector3(0,2.85,0))
    # Internal openings at x=-3 and x=3 connect both rooms to the living room.
    _wall("NorthWall",Vector3(0,0,-4.5),Vector2(12,0.20),warm)
    _wall("SouthWall",Vector3(0,0,4.5),Vector2(12,0.20),warm)
    _wall("WestWall",Vector3(-6,0,0),Vector2(0.20,9),warm)
    _wall("EastWall",Vector3(6,0,0),Vector2(0.20,9),warm)
    _wall("BedroomDivider",Vector3(0,0,-2.25),Vector2(0.16,4.5),warm)
    for x in [-5.15,0.0,5.15]:
        _wall("Partition",Vector3(x,0,0),Vector2(1.7 if absf(x)>1 else 3.6,0.18),warm)
    _box("BedroomDoorHeader",Vector3(2.6,0.6,0.18),wood,Vector3(-3.05,2.5,0))
    _box("KitchenDoorHeader",Vector3(2.6,0.6,0.18),wood,Vector3(3.05,2.5,0))
    _furniture("BedFrame",Vector3(-4.3,0,-3.3),Vector3(2.6,0.45,1.7),wood)
    _box("Mattress",Vector3(2.45,0.22,1.56),Color("#f5f1e7"),Vector3(-4.3,0.56,-3.3))
    _box("Blanket",Vector3(1.9,0.10,1.48),Color("#547e89"),Vector3(-4.0,0.70,-3.3))
    _box("Pillow",Vector3(0.48,0.14,0.65),Color("#fff4d8"),Vector3(-5.2,0.72,-3.3))
    _furniture("Wardrobe",Vector3(-5.4,0,-1.25),Vector3(0.8,2.2,1.1),wood)
    _furniture("KitchenCounter",Vector3(4.7,0,-3.9),Vector3(2.2,0.92,0.9),Color("#8a9a85"))
    _box("Worktop",Vector3(2.3,0.1,1.0),Color("#494c4b"),Vector3(4.7,0.97,-3.9))
    _box("Cooktop",Vector3(0.65,0.05,0.6),Color("#212629"),Vector3(5.1,1.045,-3.9))
    _box("Sink",Vector3(0.65,0.06,0.62),Color("#aab5ba"),Vector3(4.1,1.045,-3.9))
    _furniture("Fridge",Vector3(5.35,0,-1.6),Vector3(0.95,1.9,0.95),Color("#e4e7e5"))
    _furniture("DiningTable",Vector3(3.3,0,-1.4),Vector3(1.3,0.76,0.85),wood)
    _furniture("Sofa",Vector3(-3.6,0,2.6),Vector3(2.65,0.65,0.95),Color("#657f73"))
    _box("SofaBack",Vector3(2.65,0.65,0.2),Color("#657f73"),Vector3(-3.6,0.9,2.95))
    _furniture("CoffeeTable",Vector3(-3.3,0,1.25),Vector3(1.45,0.45,0.65),wood)
    _box("Rug",Vector3(3.0,0.015,2.1),Color("#b8a68b"),Vector3(-3.3,0.01,1.8))
    _furniture("Bookcase",Vector3(5.3,0,2.5),Vector3(0.85,2.0,1.8),wood)
    _box("LivingWindow",Vector3(2.6,1.1,0.035),Color("#8bb5c6"),Vector3(1.7,1.7,4.37))
    _box("WindowSill",Vector3(2.8,0.10,0.22),wood,Vector3(1.7,1.1,4.27))
    _sign("Bedroom",Vector3(-3.0,2.05,0.25))
    _sign("Kitchen",Vector3(3.0,2.05,0.25))
    _sign("Living room",Vector3(0,2.4,4.30))
    for pos in [Vector3(-3.0,2.6,-2.2),Vector3(3.0,2.6,-2.2),Vector3(0,2.6,2.25)]:
        var light := OmniLight3D.new()
        light.position=pos
        light.light_color=Color("#fff1d5")
        light.light_energy=1.7
        light.omni_range=6.0
        add_child(light)
    residents=Node3D.new()
    residents.name="Residents"
    add_child(residents)

func _box(label: String,size: Vector3,color: Color,at: Vector3) -> void:
    var item := MeshInstance3D.new()
    item.name=label
    var mesh := BoxMesh.new()
    mesh.size=size
    item.mesh=mesh
    var mat := StandardMaterial3D.new()
    mat.albedo_color=color
    item.material_override=mat
    item.position=at
    add_child(item)

func _wall(label: String,at: Vector3,size: Vector2,color: Color) -> void:
    _furniture(label,at,Vector3(size.x,2.8,size.y),color)

func _furniture(label: String,at: Vector3,size: Vector3,color: Color) -> void:
    obstacles.append(Rect2(Vector2(at.x-size.x/2,at.z-size.z/2),Vector2(size.x,size.z)))
    _box(label,size,color,at+Vector3(0,size.y/2,0))

func _sign(caption: String,at: Vector3) -> void:
    var sign_node := Label3D.new()
    sign_node.text=caption
    sign_node.font_size=32
    sign_node.pixel_size=0.006
    sign_node.position=at
    add_child(sign_node)

func can_stand(at: Vector3,radius: float) -> bool:
    var p := Vector2(at.x,at.z)
    if absf(p.x)>5.8-radius or absf(p.y)>4.3-radius:
        return false
    for obstacle in obstacles:
        if p.distance_squared_to(p.clamp(obstacle.position,obstacle.end))<radius*radius:
            return false
    return true

func nearby_action(at: Vector3,actions: Array) -> String:
    if at.distance_to(KITCHEN)<1.8:
        if "meal" in actions:return "meal"
        if "drink" in actions:return "drink"
    if at.distance_to(BED)<1.8 and "rest" in actions:return "rest"
    if at.distance_to(LIVING)<1.8 and "family" in actions:return "family"
    return ""
