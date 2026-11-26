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
    var dinner := Node3D.new()
    dinner.name="FamilyDinner"
    add_child(dinner)
    for n in range(4):
        var plate := MeshInstance3D.new()
        var mesh := CylinderMesh.new()
        mesh.top_radius=0.18
        mesh.bottom_radius=0.16
        mesh.height=0.025
        plate.mesh=mesh
        plate.position=Vector3(-2.5*side+(-0.30 if n%2==0 else 0.30),0.77,-0.8+(-0.22 if n<2 else 0.22))
        var material := StandardMaterial3D.new()
        material.albedo_color=Color("#eee6d7")
        plate.material_override=material
        dinner.add_child(plate)
    dinner.hide()
    _furniture("Sofa",Vector3(2.9*side,0,1.8),Vector3(1.2,0.65,1.9),fabric)
    _box("SofaBack",Vector3(0.18,0.65,1.9),fabric,Vector3(3.38*side,0.9,1.8))
    # Small shared tray, visible only after the server accepts coffee.
    var tray := Node3D.new()
    tray.name="CoffeeTray"
    add_child(tray)
    tray.position=Vector3(1.5*side,0.48,1.8)
    for z in [-0.22,0.22]:
        var cup := MeshInstance3D.new()
        var mesh := CylinderMesh.new()
        mesh.top_radius=0.065
        mesh.bottom_radius=0.05
        mesh.height=0.10
        cup.mesh=mesh
        cup.position=Vector3(0,0.05,z)
        var material := StandardMaterial3D.new()
        material.albedo_color=Color("#f3eee2")
        cup.material_override=material
        tray.add_child(cup)
    tray.hide()
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
    _box("Television",Vector3(1.0,0.63,0.08),Color("#292d32"),Vector3(-2.2*side,1.6,3.86))
    _box("Screen",Vector3(0.91,0.53,0.02),Color("#536d7c"),Vector3(-2.2*side,1.6,3.81))
    _furniture("HostChair",Vector3(-2.0*side,0,2.8),Vector3(0.62,0.48,0.60),wood)
    _box("ChairBack",Vector3(0.62,0.65,0.10),wood,Vector3(-2.0*side,0.72,2.53))
    var clutter := Node3D.new()
    clutter.name="Clutter"
    add_child(clutter)
    for n in range(3):
        var paper := MeshInstance3D.new()
        var mesh := BoxMesh.new()
        mesh.size=Vector3(0.18,0.012,0.23)
        paper.mesh=mesh
        paper.position=Vector3(float(n-1)*0.24,0.02,0.8)
        paper.rotation.y=float(n)*0.5
        clutter.add_child(paper)
    clutter.hide()
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

func host_anchor() -> Vector3:
    return Vector3(0.65 if unit%2==1 else -0.65,0,0.3)

func sofa_anchor() -> Vector3:
    return Vector3(-2.9 if unit%2==1 else 2.9,0,1.8)

func near_sofa(at: Vector3) -> bool:
    return at.distance_to(sofa_anchor())<2.1

func near_table(at: Vector3) -> bool:
    var side := -1.0 if unit%2==1 else 1.0
    return at.distance_to(Vector3(-2.5*side,0,-0.8))<2.1

func show_coffee() -> void:
    get_node("CoffeeTray").show()

func routine_anchor(activity: String) -> Vector3:
    var side := -1.0 if unit%2==1 else 1.0
    match activity:
        "COOKING","DRINKING":return Vector3(-2.7*side,0,-2.25)
        "READING","WATCHING_TV","RESTING":return Vector3(-1.15*side,0,2.8)
        "CLEANING":return Vector3(0,0,-1.25)
        "FAMILY_DINNER":return Vector3(-2.5*side,0,0.15)
    return host_anchor()

func routine_seat() -> Vector3:
    return Vector3(2.0 if unit%2==1 else -2.0,0,2.8)

func update_cleanliness(value: int) -> void:
    get_node("Clutter").visible = value<75

func host_path(from: Vector3,to: Vector3) -> Array[Vector3]:
    var grid := AStarGrid2D.new()
    grid.region=Rect2i(-16,-14,33,29)
    grid.cell_size=Vector2(0.25,0.25)
    grid.diagonal_mode=AStarGrid2D.DIAGONAL_MODE_ONLY_IF_NO_OBSTACLES
    grid.update()
    for x in range(-16,17):
        for z in range(-14,15):
            if not can_stand(Vector3(x*0.25,0,z*0.25),0.25):
                grid.set_point_solid(Vector2i(x,z))
    var start := Vector2i(roundi(from.x*4),roundi(from.z*4))
    var finish := Vector2i(roundi(to.x*4),roundi(to.z*4))
    var result: Array[Vector3] = []
    if not grid.region.has_point(start) or not grid.region.has_point(finish):return result
    if grid.is_point_solid(start) or grid.is_point_solid(finish):return result
    for id in grid.get_id_path(start,finish):
        result.append(Vector3(id.x*0.25,0,id.y*0.25))
    if not result.is_empty():result.append(to)
    return result

func update_household(household: Dictionary) -> void:
    var dinner := get_node("FamilyDinner") as Node3D
    dinner.visible=bool(household.get("dinnerNow",false))
    for i in range(dinner.get_child_count()):
        dinner.get_child(i).visible=i<mini(4,int(household.get("present",0)))
