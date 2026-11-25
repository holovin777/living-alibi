extends Node3D
## Local first-person interior. Java owns the private scene and all home actions.
const SPAWN := Vector3(0,0,3.5)
const KITCHEN := Vector3(3.3,0,-2.6)
const BED := Vector3(-4.0,0,-2.2)
const LIVING := Vector3(1.1,0,1.5)
const BATH := Vector3(4.4,0,2.2)
const ENTRY := Vector3(0,0,4.5)
const STREET_DOOR := Vector3(0,0,8.7)
var entrance_open := false
const ANCHORS := [Vector3(-1.2,0,1.5),Vector3(1.7,0,3.0),Vector3(-3.3,0,-0.6),Vector3(3.8,0,-0.7),Vector3(-2.2,0,3.4)]
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
    _wall("SouthWallLeft",Vector3(-3.45,0,4.5),Vector2(5.1,0.20),warm)
    _wall("SouthWallRight",Vector3(3.45,0,4.5),Vector2(5.1,0.20),warm)
    _box("EntryHeader",Vector3(1.8,0.65,0.2),wood,Vector3(0,2.48,4.5))
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
    _furniture("Bookcase",Vector3(-5.4,0,1.7),Vector3(0.75,2.0,1.1),wood)
    _box("BathroomTiles",Vector3(2.1,0.018,4.1),Color("#b8c9cd"),Vector3(4.85,0.012,2.25))
    _wall("BathNorth",Vector3(4.85,0,0.65),Vector2(2.1,0.16),warm)
    _wall("BathDividerNorth",Vector3(3.8,0,1.0),Vector2(0.16,0.7),warm)
    _wall("BathDividerSouth",Vector3(3.8,0,3.65),Vector2(0.16,1.5),warm)
    _box("BathDoorHeader",Vector3(0.16,0.6,1.55),wood,Vector3(3.8,2.5,2.2))
    _furniture("Shower",Vector3(5.15,0,3.65),Vector3(1.05,0.12,1.05),Color("#e2e8e8"))
    _box("ShowerGlass",Vector3(0.055,1.9,1.0),Color("#9fc4cc"),Vector3(5.72,1.05,3.65))
    _furniture("Washbasin",Vector3(5.35,0,1.15),Vector3(0.75,0.88,0.65),Color("#edf0ed"))
    _box("Mirror",Vector3(0.035,0.7,0.65),Color("#acc4cc"),Vector3(5.86,1.65,1.15))
    _furniture("Toilet",Vector3(5.35,0,2.25),Vector3(0.6,0.65,0.7),Color("#edf0ed"))
    _sign("Bathroom",Vector3(3.65,2.05,2.2))
    _box("EntranceDoor",Vector3(1.1,2.15,0.06),wood,Vector3(0,1.08,4.36))
    _box("DoorHandle",Vector3(0.12,0.07,0.12),Color("#bba66a"),Vector3(0.42,1.05,4.29))
    _box("LobbyFloor",Vector3(4.2,0.18,4.5),Color("#b6b1a1"),Vector3(0,-0.10,6.75))
    _box("LobbyCeiling",Vector3(4.2,0.15,4.5),warm,Vector3(0,2.85,6.75))
    _wall("LobbyLeft",Vector3(-2.1,0,6.75),Vector2(0.18,4.5),warm)
    _wall("LobbyRight",Vector3(2.1,0,6.75),Vector2(0.18,4.5),warm)
    _wall("LobbyEnd",Vector3(0,0,9),Vector2(4.2,0.18),warm)
    _box("BuildingExit",Vector3(1.35,2.25,0.07),Color("#496976"),Vector3(0,1.13,8.87))
    _box("NeighborDoor",Vector3(0.07,2.12,0.95),wood,Vector3(-2.0,1.06,6.4))
    _box("Mailboxes",Vector3(0.25,0.75,1.0),Color("#8a9699"),Vector3(1.9,1.45,6.1))
    _sign("Building exit",Vector3(0,2.45,8.83))
    var lobby_light := OmniLight3D.new()
    lobby_light.position=Vector3(0,2.55,6.7)
    lobby_light.omni_range=5.0
    lobby_light.light_energy=1.6
    add_child(lobby_light)
    _box("WallArt",Vector3(1.0,0.65,0.04),Color("#527c82"),Vector3(-2.0,1.7,4.36))
    _box("DecorCushion",Vector3(0.55,0.25,0.40),Color("#d4a86c"),Vector3(-3.6,0.82,2.85))
    _box("PremiumHeadboard",Vector3(0.10,1.25,1.7),Color("#815b45"),Vector3(-5.65,0.65,-3.3))
    _box("KitchenMicrowave",Vector3(0.58,0.33,0.42),Color("#5d6568"),Vector3(4.7,1.22,-3.9))
    _box("KitchenMicrowaveGlass",Vector3(0.44,0.23,0.025),Color("#272d33"),Vector3(4.67,1.22,-3.68))
    _box("KitchenUpperCabinet",Vector3(2.0,0.65,0.36),Color("#ded3be"),Vector3(4.7,2.2,-4.24))
    _box("BedroomThrow",Vector3(1.2,0.07,1.44),Color("#ba865b"),Vector3(-3.85,0.78,-3.3))
    _box("BedroomArtwork",Vector3(0.05,0.75,1.0),Color("#8a9c9a"),Vector3(-5.86,1.9,-2.95))
    _box("ChildBooks",Vector3(0.37,0.32,0.7),Color("#d8ae56"),Vector3(-5.38,1.25,1.7))
    _box("ChildDrawing",Vector3(0.65,0.48,0.035),Color("#f0c883"),Vector3(-0.9,1.5,4.35))
    _box("ChildPlayMat",Vector3(1.15,0.018,0.95),Color("#8caea4"),Vector3(1.8,0.025,2.9))
    set_furniture_level(0)
    _box("WashingMachine",Vector3(0.66,0.85,0.65),Color("#e5e8e8"),Vector3(2.95,0.425,3.95))
    _box("WasherDoor",Vector3(0.44,0.44,0.035),Color("#687e8b"),Vector3(2.95,0.43,3.61))
    set_room_packages([])
    _box("LivingWindow",Vector3(2.6,1.1,0.035),Color("#8bb5c6"),Vector3(1.7,1.7,4.37))
    _box("WindowSill",Vector3(2.8,0.10,0.22),wood,Vector3(1.7,1.1,4.27))
    _sign("Bedroom",Vector3(-3.0,2.05,0.25))
    _sign("Kitchen",Vector3(3.0,2.05,0.25))
    _sign("Living room",Vector3(0,2.4,4.30))
    for pos in [Vector3(-3.0,2.6,-2.2),Vector3(3.0,2.6,-2.2),Vector3(0,2.6,2.25),Vector3(4.4,2.6,2.2)]:
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
    if p.y>4.5:
        if absf(p.x)>1.9-radius or p.y>8.8-radius:return false
    elif absf(p.x)>5.8-radius or p.y< -4.3+radius:
        return false
    if not entrance_open and Rect2(-0.9,4.38,1.8,0.24).grow(radius).has_point(p):return false
    if get_node("WashingMachine").visible and Rect2(2.62,3.625,0.66,0.65).grow(radius).has_point(p):return false
    for obstacle in obstacles:
        if p.distance_squared_to(p.clamp(obstacle.position,obstacle.end))<radius*radius:
            return false
    return true

func set_furniture_level(level: int) -> void:
    get_node("WallArt").visible=level>=1
    get_node("DecorCushion").visible=level>=1
    get_node("PremiumHeadboard").visible=level>=2
    var sofa := get_node("Sofa") as MeshInstance3D
    var material := sofa.material_override as StandardMaterial3D
    material.albedo_color=Color("#657f73") if level==0 else Color("#496976") if level==1 else Color("#9b684d")

func _paint(node_name: String,color: Color) -> void:
    var mesh := get_node(node_name) as MeshInstance3D
    (mesh.material_override as StandardMaterial3D).albedo_color=color

func set_home_style(style: String) -> void:
    var floor_color := Color("#c8a581")
    var wall_color := Color("#eee0ca")
    match style:
        "tiglio":
            floor_color=Color("#b6946e")
            wall_color=Color("#eee7d8")
        "rosa":
            floor_color=Color("#c18f81")
            wall_color=Color("#f4e2df")
        "olmo":
            floor_color=Color("#a58f7a")
            wall_color=Color("#dfe5dc")
    _paint("Floor",floor_color)
    for wall in ["NorthWall","SouthWallLeft","SouthWallRight","WestWall","EastWall","BedroomDivider"]:
        _paint(wall,wall_color)

func set_room_packages(packages: Array) -> void:
    var installed := []
    for item in packages:
        if item is Dictionary and item.get("installed",false):
            installed.append(str(item.get("id","")))
    for node_name in ["KitchenMicrowave","KitchenMicrowaveGlass","KitchenUpperCabinet"]:
        get_node(node_name).visible="kitchen" in installed
    for node_name in ["BedroomThrow","BedroomArtwork"]:
        get_node(node_name).visible="bedroom" in installed
    for node_name in ["ChildBooks","ChildDrawing","ChildPlayMat"]:
        get_node(node_name).visible="children" in installed
    for node_name in ["WashingMachine","WasherDoor"]:
        get_node(node_name).visible="laundry" in installed

func toggle_entrance(at: Vector3) -> void:
    if entrance_open and absf(at.z-ENTRY.z)<0.65:return
    entrance_open=not entrance_open
    get_node("EntranceDoor").visible=not entrance_open
    get_node("DoorHandle").visible=not entrance_open

func nearby_action(at: Vector3,actions: Array) -> String:
    if at.distance_to(ENTRY)<1.3:return "door"
    if at.distance_to(STREET_DOOR)<1.3:return "exit"
    if at.z>4.5:return ""
    if at.distance_to(BATH)<1.3 and "wash" in actions:return "wash"
    if at.distance_to(Vector3(2.95,0,2.9))<0.8 and "laundry" in actions:return "laundry"
    if at.distance_to(KITCHEN)<1.8:
        if "microwave" in actions:return "microwave"
        if "meal" in actions:return "meal"
        if "drink" in actions:return "drink"
    if at.distance_to(BED)<1.8 and "rest" in actions:return "rest"
    if at.distance_to(LIVING)<1.8 and "family" in actions:return "family"
    if at.distance_to(LIVING)<1.8 and "clean" in actions:return "clean"
    return ""
