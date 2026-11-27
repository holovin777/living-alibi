extends Node3D
## Schematic common stairwell, not a private apartment or new Java district.
const SPAWN := Vector3(0,0,1.0)
const RISE := 3.1
const RUN := 5.4
const STEP_COUNT := 18
const PITCH := 7.4
const MIN_Z := -14.8
const MAX_Z := 2.0

func _ready() -> void:
    name = "PublicStairwell"
    var stone := Color("#b5ada0")
    var wall := Color("#ded6c6")
    for floor in range(3):
        var z := -float(floor)*PITCH
        var y := float(floor)*RISE
        _box(Vector3(3.4,0.18,2.0),stone,Vector3(0,y-0.09,z+1.0))
        for side in [-1.0,1.0]:
            var door := Vector3(side*1.62,y+1.1,z+1.0)
            _box(Vector3(0.12,2.2,1.0),Color("#6c503c"),door)
            _box(Vector3(0.12,0.08,0.08),Color("#cfb46e"),door+Vector3(-side*0.08,-0.05,0.37))
            _label("%s" % (floor*2+(1 if side<0 else 2)),door+Vector3(-side*0.085,0.78,0),-side*PI/2.0)
        _label("PIANO %s" % floor,Vector3(-1.64,y+2.35,z+1.0),PI/2.0)
        # An elevator call panel on each landing. It is separate from apartment doors.
        var panel := Vector3(1.64,y+1.2,z+0.35)
        _box(Vector3(0.09,0.52,0.22),Color("#48565c"),panel)
        for offset in [-0.12,0.12]:
            _box(Vector3(0.035,0.11,0.11),Color("#d8bd78"),panel+Vector3(-0.06,offset,0))
        _label("ASCENSORE",Vector3(1.62,y+1.65,z+0.35),-PI/2.0)
        var lamp := OmniLight3D.new()
        lamp.position=Vector3(0,y+2.55,z+0.9)
        lamp.light_color=Color("#ffe5bd")
        lamp.light_energy=1.4
        lamp.omni_range=6.5
        add_child(lamp)
        if floor==2:continue
        for n in range(STEP_COUNT):
            var step_depth := RUN/STEP_COUNT
            var h := float(n+1)*RISE/STEP_COUNT
            _box(Vector3(3.4,h+0.18,step_depth+0.015),stone,Vector3(0,y+(h-0.18)/2.0,z-(float(n)+0.5)*step_depth))
            for side in [-1.0,1.0]:
                if n%3==0:
                    _box(Vector3(0.07,0.8,0.07),Color("#505b5c"),Vector3(side*1.42,y+h+0.4,z-(float(n)+0.5)*step_depth))
                _box(Vector3(0.07,0.07,step_depth+0.015),Color("#785a40"),Vector3(side*1.42,y+h+0.82,z-(float(n)+0.5)*step_depth))
    for side in [-1.0,1.0]:
        _box(Vector3(0.15,9.4,19.0),wall,Vector3(side*1.75,4.5,-7.0))
    _box(Vector3(3.65,9.4,0.15),wall,Vector3(0,4.5,-14.9))
    _box(Vector3(3.65,9.4,0.15),wall,Vector3(0,4.5,2.1))
    _box(Vector3(3.65,0.2,19.0),wall,Vector3(0,9.25,-7.0))
    _box(Vector3(1.3,2.2,0.12),Color("#77563e"),Vector3(0,1.1,1.99))
    _label("USCITA",Vector3(0,2.35,1.9),PI)
    # Number-only mailboxes preserve the privacy of the occupants.
    _label("POSTA",Vector3(-1.64,1.85,0.15),PI/2.0)
    for unit in range(1,7):
        var row := int((unit-1)/2.0)
        var column := (unit-1)%2
        var at := Vector3(-1.61,1.53-float(row)*0.25,0.0+float(column)*0.32)
        _box(Vector3(0.12,0.22,0.29),Color("#68757a"),at)
        _box(Vector3(0.015,0.035,0.19),Color("#252e31"),at+Vector3(0.07,0.05,0))
        _label(str(unit),at+Vector3(0.075,-0.055,0),PI/2.0)

func _box(size: Vector3,color: Color,at: Vector3) -> void:
    var mesh := BoxMesh.new()
    mesh.size=size
    var node := MeshInstance3D.new()
    node.mesh=mesh
    var material := StandardMaterial3D.new()
    material.albedo_color=color
    material.roughness=0.88
    node.material_override=material
    node.position=at
    add_child(node)

func _label(caption: String,at: Vector3,yaw: float) -> void:
    var node := Label3D.new()
    node.text=caption
    node.position=at
    node.rotation.y=yaw
    node.font_size=40
    node.pixel_size=0.004
    node.modulate=Color("#324650")
    node.outline_size=0
    add_child(node)

# Keep emergency light so broken common lighting never makes the stairs invisible.
func set_lighting_working(working: bool) -> void:
    for child in get_children():
        if child is OmniLight3D:
            child.light_energy=1.4 if working else 0.22

func can_stand(at: Vector3,radius: float) -> bool:
    return absf(at.x)<=1.35-radius and at.z>=MIN_Z+radius and at.z<=MAX_Z-radius

func ground_height(at: Vector3) -> float:
    var distance := maxf(0.0,-at.z)
    var floor := mini(2,int(floorf(distance/PITCH)))
    var run := distance-float(floor)*PITCH
    if floor==2:return 2.0*RISE
    return float(floor)*RISE+ceilf(clampf(run/RUN,0.0,1.0)*STEP_COUNT)*RISE/STEP_COUNT

func floor_number(at: Vector3) -> int:
    return clampi(int(floorf((at.y+0.01)/RISE)),0,2)

func near_exit(at: Vector3) -> bool:
    return at.z>0.3 and at.y<0.1

func near_unit(at: Vector3) -> int:
    if absf(at.x)<0.72:return 0
    for floor in range(3):
        var landing := Vector3(0,float(floor)*RISE,-float(floor)*PITCH+1.0)
        if absf(at.z-landing.z)<0.65 and absf(at.y-landing.y)<0.08:
            return floor*2+(1 if at.x<0 else 2)
    return 0

# At the edge of the landing, not on the stairs or at an apartment door.
func near_elevator(at: Vector3) -> bool:
    if at.x<0.72:return false
    for floor in range(3):
        if absf(at.y-float(floor)*RISE)<0.08 and absf(at.z-(-float(floor)*PITCH+0.4))<0.18:
            return true
    return false

func elevator_destination(at: Vector3,direction: int) -> Vector3:
    if not near_elevator(at) or absi(direction)!=1:return at
    var current := floor_number(at)
    var target := clampi(current+direction,0,2)
    if target==current:return at
    # Arrive in the middle of the landing with clearance from the wall and stairs.
    return Vector3(0,float(target)*RISE,-float(target)*PITCH+1.0)
