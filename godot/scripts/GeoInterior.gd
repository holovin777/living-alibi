extends Node3D
## Modular fictional interiors, streamed separately from the OSM exterior.
const LEVEL=150.0
var materials: Dictionary={}

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
    for x in [-3.5,3.5]:
        var lamp:=OmniLight3D.new()
        lamp.position=Vector3(x,2.9,0)
        lamp.light_energy=1.5
        lamp.omni_range=12
        add_child(lamp)
        _box(Vector3(x,3.12,0),Vector3(1.7,0.12,0.65),Color("#f5ecd5"),false)
    _sign(str(record.get("label","")),Vector3(0,2.9,-6.72))
