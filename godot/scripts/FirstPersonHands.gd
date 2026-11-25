extends Node3D
## Presentation only: item ownership and consumption remain server-authoritative.
var enabled := true
var held_item := ""
var _left: Node3D
var _right: Node3D
var _props: Dictionary = {}
var _reach := 0.0

func _ready() -> void:
    name = "FirstPersonHands"
    var sleeve := Color("#536570")
    var skin := Color("#d0a17e")
    _left = _arm(-1.0,sleeve,skin)
    _right = _arm(1.0,sleeve,skin)
    for id in ["bread","water","grape-juice"]:
        var prop := Node3D.new()
        prop.name = id
        _right.add_child(prop)
        prop.position = Vector3(-0.015,0.065,-0.055)
        if id=="bread":
            _shape(prop,Vector3(0.11,0.07,0.17),Color("#b17c3f"),Vector3.ZERO,true)
            _shape(prop,Vector3(0.095,0.015,0.13),Color("#e9c791"),Vector3(0,0.036,0),true)
        else:
            _shape(prop,Vector3(0.065,0.18,0.065),Color("#7ea9ba") if id=="water" else Color("#773c64"),Vector3(0,0.065,0),true)
            _shape(prop,Vector3(0.04,0.025,0.04),Color("#e7e3d9"),Vector3(0,0.16,0),false)
            _shape(prop,Vector3(0.069,0.045,0.069),Color("#dedbca"),Vector3(0,0.07,0),false)
        prop.hide()
        _props[id] = prop

func _shape(parent: Node3D,size: Vector3,color: Color,at: Vector3,rounded: bool) -> void:
    var instance := MeshInstance3D.new()
    if rounded:
        var mesh := SphereMesh.new()
        mesh.radius = 0.5
        mesh.height = 1.0
        mesh.radial_segments = 12
        mesh.rings = 6
        instance.mesh = mesh
        instance.scale = size
    else:
        var mesh := BoxMesh.new()
        mesh.size = size
        instance.mesh = mesh
    var material := StandardMaterial3D.new()
    material.albedo_color = color
    material.roughness = 0.85
    instance.material_override = material
    instance.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
    instance.position = at
    parent.add_child(instance)

func _arm(side: float,sleeve: Color,skin: Color) -> Node3D:
    var arm := Node3D.new()
    arm.name = "LeftHand" if side<0.0 else "RightHand"
    add_child(arm)
    _shape(arm,Vector3(0.095,0.32,0.10),sleeve,Vector3(0,-0.16,0.06),true)
    _shape(arm,Vector3(0.085,0.12,0.09),skin,Vector3.ZERO,true)
    _shape(arm,Vector3(0.035,0.055,0.055),skin,Vector3(-side*0.045,0.015,-0.012),true)
    return arm

func set_item(id: String) -> void:
    held_item = id if _props.has(id) else ""
    for key in _props:
        _props[key].visible = key==held_item

func reach() -> void:
    _reach = 0.45

func update_pose(delta: float,bob: float,available: bool) -> void:
    visible = enabled and available
    _reach = maxf(0.0,_reach-maxf(delta,0.0))
    var reach_amount := sin(PI*_reach/0.45)
    _left.position = Vector3(-0.26,-0.34+bob*0.55,-0.44)
    _right.position = Vector3(0.26-0.12*reach_amount,-0.34+bob*0.55+0.14*reach_amount,-0.44-0.12*reach_amount)
    _left.rotation = Vector3(-0.62,0,-0.12)
    _right.rotation = Vector3(-0.62-0.25*reach_amount,0,0.12)
    if not held_item.is_empty():
        _right.position += Vector3(-0.07,0.075,-0.035)
