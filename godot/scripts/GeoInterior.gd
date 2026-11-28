extends Node3D
## Modular fictional interiors, streamed separately from the OSM exterior.
const LEVEL=150.0
var materials: Dictionary={}
var task_board: Label3D
var task_board_text:=""
var fire_board: Label3D

func _mat(color: Color) -> StandardMaterial3D:
    if not materials.has(color):
        var mat:=StandardMaterial3D.new()
        mat.albedo_color=color
        mat.roughness=0.88
        materials[color]=mat
    return materials[color]

func _box(at: Vector3,dimensions: Vector3,color: Color,solid: bool=true) -> void:
    var mesh:=BoxMesh.new()
    mesh.size=dimensions
    var visual:=MeshInstance3D.new()
    visual.mesh=mesh
    visual.material_override=_mat(color)
    visual.position=at
    add_child(visual)
    if solid:
        var body:=StaticBody3D.new()
        var shape:=CollisionShape3D.new()
        var bounds:=BoxShape3D.new()
        bounds.size=dimensions
        shape.shape=bounds
        body.position=at
        body.add_child(shape)
        add_child(body)

func _sign(words: String,at: Vector3) -> void:
    var sign_label:=Label3D.new()
    sign_label.text=words
    sign_label.font_size=48
    sign_label.pixel_size=0.012
    sign_label.position=at
    sign_label.modulate=Color("#f5e2bf")
    add_child(sign_label)

func build(record: Dictionary) -> void:
    position=Vector3(float(record["x"]),LEVEL,float(record["z"]))
    var kind:=str(record["id"])
    var accent:=Color("#8d9f9b")
    match kind:
        "MARKET","BAKERY":accent=Color("#a58862")
        "SCHOOL","LIBRARY":accent=Color("#95a97c")
        "CLINIC":accent=Color("#8eb6bb")
        "WORKSHOP","FACTORY":accent=Color("#788592")
        "CAFE":accent=Color("#a27b62")
    _box(Vector3(0,-0.15,0),Vector3(14.0,0.3,14.0),Color("#b0a89b"))
    _box(Vector3(0,3.4,0),Vector3(14.0,0.25,14.0),Color("#dad7ce"))
    _box(Vector3(0,1.65,-7),Vector3(14,3.3,0.25),accent)
    _box(Vector3(-7,1.65,0),Vector3(0.25,3.3,14),accent)
    _box(Vector3(7,1.65,0),Vector3(0.25,3.3,14),accent)
    _box(Vector3(0,1.65,7),Vector3(14,3.3,0.25),accent)
    _box(Vector3(0,1.05,6.83),Vector3(1.8,2.1,0.12),Color("#415b56"),false)
    _sign("USCITA / EXIT · E",Vector3(0,2.5,6.7))
    if kind=="HOME":
        _home()
        return
    # Back-wall furnishings leave the resident grid and central aisle clear.
    match kind:
        "MARKET","BAKERY","LIBRARY":
            for x in [-4.8,0.0,4.8]:
                _box(Vector3(x,0.95,-5.85),Vector3(3.3,1.9,0.75),Color("#795e47"))
                for y in [0.55,1.15,1.75]:
                    _box(Vector3(x,y,-5.38),Vector3(3.15,0.09,0.28),Color("#ded4ba"),false)
                    for i in range(6):
                        _box(Vector3(x-1.3+i*0.5,y+0.18,-5.35),Vector3(0.28,0.3,0.24),accent,false)
        "SCHOOL":
            _box(Vector3(0,1.85,-6.8),Vector3(6.8,1.55,0.12),Color("#29443b"),false)
            for x in [-4.5,4.5]:
                _box(Vector3(x,0.45,-5.8),Vector3(2.7,0.9,0.8),Color("#997852"))
        "CLINIC":
            for x in [-4.5,4.5]:
                _box(Vector3(x,0.43,-5.6),Vector3(2.4,0.86,1.5),Color("#dde6e7"))
                _box(Vector3(x,0.94,-5.8),Vector3(1.95,0.17,1.05),Color("#eef1ec"))
            _box(Vector3(0,0.55,-5.8),Vector3(2.0,1.1,0.8),Color("#e4e7e2"))
        "WORKSHOP","FACTORY":
            for x in [-4.5,0.0,4.5]:
                _box(Vector3(x,0.6,-5.8),Vector3(2.8,1.2,1.05),Color("#4c606c"))
                _box(Vector3(x,1.35,-5.8),Vector3(2.9,0.2,1.2),Color("#b1b4b5"))
        _:
            for x in [-4.5,0.0,4.5]:
                _box(Vector3(x,0.55,-5.8),Vector3(2.7,1.1,1),Color("#9c7853"))
    # Match the authoritative Java station grid: staff behind counters,
    # and children seated at individual classroom desks.
    for i in range(6):
        var x: float=-5.0+i*2.0
        var color:=Color("#b89b76")
        if kind in ["WORKSHOP","FACTORY"]:color=Color("#536873")
        elif kind=="CLINIC":color=Color("#dce5e2")
        _box(Vector3(x,0.45,-3.65),Vector3(1.5,0.9,0.62),color)
        _box(Vector3(x,0.94,-3.65),Vector3(1.58,0.08,0.7),Color("#d8c9ad"))
        if kind in ["OFFICE","LIBRARY"]:
            _box(Vector3(x,1.17,-3.65),Vector3(0.58,0.43,0.12),Color("#353f45"),false)
        elif kind in ["WORKSHOP","FACTORY"]:
            _box(Vector3(x,1.02,-3.65),Vector3(0.35,0.14,0.25),Color("#7b8387"),false)
        elif kind in ["MARKET","BAKERY","CAFE"]:
            _box(Vector3(x,1.08,-3.65),Vector3(0.4,0.25,0.28),Color("#5d676b"),false)
    if kind=="SCHOOL":
        for i in range(12):
            var x: float=-4.5+(i%4)*3
            var z: float=-1.2+floori(i/4.0)*2.2
            _box(Vector3(x,0.55,z-0.65),Vector3(1.45,0.10,0.58),Color("#b99b76"))
            for dx in [-0.58,0.58]:
                _box(Vector3(x+dx,0.27,z-0.65),Vector3(0.10,0.54,0.45),Color("#59686a"))
            _box(Vector3(x,0.34,z+0.05),Vector3(0.55,0.12,0.52),Color("#72887a"))
            _box(Vector3(x,0.65,z+0.29),Vector3(0.55,0.52,0.10),Color("#72887a"))
            _box(Vector3(x,0.62,z-0.65),Vector3(0.34,0.03,0.24),Color("#ebe0c7"),false)
    if kind=="OFFICE":_office()
    if kind=="WORKSHOP":_fire_station()
    for x in [-3.5,3.5]:
        var lamp:=OmniLight3D.new()
        lamp.position=Vector3(x,2.9,0)
        lamp.light_energy=1.5
        lamp.omni_range=12
        add_child(lamp)
        _box(Vector3(x,3.12,0),Vector3(1.7,0.12,0.65),Color("#f5ecd5"),false)
    _sign(str(record.get("label","")),Vector3(0,2.9,-6.72))

func _fire_station() -> void:
    # Shared workshop wing; keep the existing exit and resident grid clear.
    var red:=Color("#a92b2c")
    var steel:=Color("#63717b")
    for z in [-0.8,0.25,1.3,2.35]:
        _box(Vector3(-6.42,1.03,z),Vector3(0.72,2.06,0.94),steel)
        _box(Vector3(-6.02,1.03,z),Vector3(0.045,1.94,0.86),red,false)
        _box(Vector3(-5.98,1.05,z+0.29),Vector3(0.07,0.13,0.07),Color("#c5c9ca"),false)
    _box(Vector3(-6.4,0.28,4.15),Vector3(0.62,0.56,1.4),Color("#594b3b"))
    for i in range(3):
        var reel:=MeshInstance3D.new()
        var mesh:=TorusMesh.new()
        mesh.inner_radius=0.11
        mesh.outer_radius=0.25
        reel.mesh=mesh
        reel.rotation.z=PI/2
        reel.position=Vector3(-6.45,1.55,3.0+i*0.8)
        reel.material_override=_mat(Color("#e1be66"))
        add_child(reel)
    _box(Vector3(6.54,1.45,2.1),Vector3(0.12,1.85,3.6),Color("#303940"),false)
    fire_board=Label3D.new()
    fire_board.rotation.y=-PI/2
    fire_board.position=Vector3(6.45,1.5,2.1)
    fire_board.font_size=40
    fire_board.pixel_size=0.008
    fire_board.modulate=Color("#f5ead5")
    add_child(fire_board)
    update_fire_board({})

func update_fire_board(state: Dictionary) -> void:
    if not is_instance_valid(fire_board):return
    fire_board.text="VIGILI DEL FUOCO\nEquipment / Attrezzatura: %s%%\nCrew / Squadra: %s available · %s assigned\n%s\nF · Maintenance / Manutenzione" % [state.get("equipment",100),state.get("available",0),state.get("assigned",0),state.get("status","NONE")]

func _office() -> void:
    # Real Java office workers occupy the existing back-row stations. The
    # front workbenches are reserved for player interaction, not fake employees.
    for x in [-4.0,4.0]:
        _box(Vector3(x,0.83,2.8),Vector3(2.3,0.12,1.1),Color("#b6a68d"))
        for dx in [-1.02,1.02]:
            _box(Vector3(x+dx,0.4,2.8),Vector3(0.12,0.8,1.0),Color("#43515b"))
        _box(Vector3(x,1.13,2.4),Vector3(0.12,0.48,0.12),Color("#303b45"),false)
        _box(Vector3(x,1.43,2.39),Vector3(1.12,0.68,0.12),Color("#26343e"),false)
        _box(Vector3(x,1.43,2.46),Vector3(1.04,0.59,0.02),Color("#477c91"),false)
        _box(Vector3(x,0.92,3.02),Vector3(0.72,0.055,0.27),Color("#36444c"),false)
        _box(Vector3(x+0.58,0.92,3.04),Vector3(0.13,0.065,0.22),Color("#36444c"),false)
        _box(Vector3(x-0.9,0.27,2.4),Vector3(0.35,0.54,0.55),Color("#26333d"))
        _box(Vector3(x+1.35,0.23,4.1),Vector3(0.5,0.46,0.5),Color("#607e82"))
        var prompt:=Label3D.new()
        prompt.text="PC · E"
        prompt.font_size=36
        prompt.pixel_size=0.008
        prompt.position=Vector3(x,1.95,2.46)
        prompt.modulate=Color("#e1edf0")
        add_child(prompt)
    _box(Vector3(0,2.0,-6.7),Vector3(5.8,1.4,0.13),Color("#293e4a"),false)
    task_board=Label3D.new()
    task_board.font_size=34
    task_board.pixel_size=0.007
    task_board.position=Vector3(0,2.0,-6.60)
    task_board.modulate=Color("#dce9e8")
    add_child(task_board)
    update_work_board({})

func update_work_board(work: Dictionary) -> void:
    if not is_instance_valid(task_board):return
    var lines: PackedStringArray=["BORGO DIGITAL · PROJECT BOARD"]
    for project in work.get("projects",[]):
        lines.append("%s · %s" % [project.get("title",""),project.get("phase","")])
    if lines.size()==1:lines.append("IT workspace · access requires qualification")
    var words: String="\n".join(lines)
    if words!=task_board_text:
        task_board_text=words
        task_board.text=words

func _home() -> void:
    # Four functional zones around an unobstructed central hallway.
    var wood:=Color("#b38c62")
    var linen:=Color("#e3dfd1")
    var tiles:=Color("#b7c7c4")
    _box(Vector3(-3.5,0.025,-3.5),Vector3(6.5,0.05,6.5),Color("#b8a083"),false)
    _box(Vector3(3.5,0.025,-3.5),Vector3(6.5,0.05,6.5),tiles,false)
    _box(Vector3(-3.5,0.025,3.5),Vector3(6.5,0.05,6.5),Color("#c3ad91"),false)
    _box(Vector3(3.5,0.025,3.5),Vector3(6.5,0.05,6.5),Color("#d1d6d3"),false)
    # Bedroom: bed, pillows and wardrobe.
    _box(Vector3(-4.8,0.32,-5.4),Vector3(2.1,0.64,2.5),wood)
    _box(Vector3(-4.8,0.73,-5.4),Vector3(2,0.20,2.4),linen)
    _box(Vector3(-4.8,0.86,-6.2),Vector3(1.65,0.15,0.5),Color("#f8f3e6"),false)
    _box(Vector3(-2.2,1.0,-6.2),Vector3(1.5,2.0,0.8),wood)
    _sign("CAMERA / BEDROOM",Vector3(-4.5,2.75,-6.7))
    # Kitchen: counter, sink, hob and refrigerator.
    _box(Vector3(3.3,0.5,-6.15),Vector3(4.0,1.0,0.85),wood)
    _box(Vector3(3.3,1.04,-6.15),Vector3(4.1,0.10,0.95),Color("#9faaa9"))
    _box(Vector3(2.2,1.11,-6.1),Vector3(0.8,0.06,0.55),Color("#67797d"),false)
    _box(Vector3(4.4,1.11,-6.1),Vector3(0.85,0.05,0.55),Color("#343739"),false)
    _box(Vector3(6.15,1.0,-5.9),Vector3(0.9,2.0,0.95),linen)
    _sign("CUCINA / KITCHEN",Vector3(3.6,2.75,-6.7))
    # Living room and dining table.
    _box(Vector3(-5.6,0.40,4.8),Vector3(1.5,0.8,2.6),Color("#788a7c"))
    _box(Vector3(-6.12,1.03,4.8),Vector3(0.38,0.85,2.6),Color("#788a7c"))
    _box(Vector3(-3.0,0.47,4.5),Vector3(1.7,0.94,1.1),wood)
    _box(Vector3(-3,1.22,6.55),Vector3(1.8,1.0,0.15),Color("#30373b"),false)
    _sign("SOGGIORNO / LIVING",Vector3(-4,2.75,6.65))
    # Bathroom behind a partition; functional washing is handled by Java.
    _box(Vector3(3.8,1.4,2.7),Vector3(4.5,2.8,0.13),Color("#c4cecc"))
    _box(Vector3(5.6,0.35,5.6),Vector3(1.7,0.7,1.2),linen)
    _box(Vector3(2.2,0.43,6),Vector3(0.7,0.86,0.6),linen)
    _box(Vector3(2.2,1.35,6.72),Vector3(0.7,0.8,0.06),Color("#aec8cb"),false)
    _box(Vector3(6.4,0.45,3.5),Vector3(0.65,0.9,0.65),linen)
    _sign("BAGNO / BATHROOM",Vector3(4.5,2.75,6.65))
    for x in [-3.5,3.5]:
        var light:=OmniLight3D.new()
        light.position=Vector3(x,2.95,0)
        light.light_energy=1.7
        light.omni_range=13
        add_child(light)
