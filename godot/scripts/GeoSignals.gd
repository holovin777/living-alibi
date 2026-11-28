extends RefCounted
## Shared two-axis prototype phases. Local server/client use Unix seconds.
static func phase(offset: int,seconds: int,approach: Vector2) -> int:
    var t:=posmod(seconds+offset,34)
    if absf(approach.x)>absf(approach.y):t=posmod(t+17,34)
    if t<12:return 2 # green
    if t<15:return 1 # amber
    return 0 # red, including all-red clearance

static func seconds() -> int:
    return int(Time.get_unix_time_from_system())

static func create(parent: Node3D,record: Dictionary) -> void:
    var at:=Vector2(float(record["point"][0]),float(record["point"][1]))
    var origin:=Vector2(float(record["from"][0]),float(record["from"][1]))
    var dir: Vector2=(at-origin).normalized()
    if dir.length_squared()<0.01:return
    var side:=Vector2(-dir.y,dir.x)
    var width:=float(record["width"])
    if str(record["kind"])=="crossing":
        for i in range(maxi(1,floori(width/0.7))):
            var lateral: float=(float(i)+0.5)*0.7-width*0.5
            var pos:=at+side*lateral
            var stripe:=box(parent,Vector3(pos.x,0.065,pos.y),Vector3(0.35,0.025,2.2),Color("#e5e2d9"))
            stripe.rotation.y=atan2(-dir.x,-dir.y)
        return
    var pos:=at-dir*3.0+side*(width*0.5+0.40)
    var pole:=Node3D.new()
    pole.position=Vector3(pos.x,0,pos.y)
    pole.rotation.y=atan2(-dir.x,-dir.y)
    parent.add_child(pole)
    box(pole,Vector3(0,1.65,0),Vector3(0.10,3.30,0.10),Color("#697072"))
    box(pole,Vector3(0,3.02,0),Vector3(0.30,0.76,0.20),Color("#282d2e"))
    var bulbs: Array[StandardMaterial3D]=[]
    var colors: Array[Color]=[Color("#fa3535"),Color("#ffb52a"),Color("#34df74")]
    for i in range(3):
        var bulb:=MeshInstance3D.new()
        var mesh:=SphereMesh.new()
        mesh.radius=0.105
        mesh.height=0.21
        mesh.radial_segments=8
        mesh.rings=4
        bulb.mesh=mesh
        bulb.scale.z=0.35
        bulb.position=Vector3(0,3.25-float(i)*0.23,0.12)
        var mat:=StandardMaterial3D.new()
        mat.albedo_color=colors[i].darkened(0.8)
        mat.emission_enabled=true
        mat.emission=colors[i]
        mat.emission_energy_multiplier=0.0
        bulb.material_override=mat
        pole.add_child(bulb)
        bulbs.append(mat)
    pole.set_meta("signal",{"offset":int(record["offset"]),"dir":dir,"bulbs":bulbs})
    update(pole,seconds())

static func update(node: Node3D,now: int) -> void:
    var data: Dictionary=node.get_meta("signal",{})
    if data.is_empty():return
    var active:=phase(data["offset"],now,data["dir"])
    for i in range(3):
        var mat: StandardMaterial3D=data["bulbs"][i]
        mat.emission_energy_multiplier=1.5 if i==active else 0.0

static func box(parent: Node3D,at: Vector3,size: Vector3,color: Color) -> MeshInstance3D:
    var part:=MeshInstance3D.new()
    var mesh:=BoxMesh.new()
    mesh.size=size
    part.mesh=mesh
    var mat:=StandardMaterial3D.new()
    mat.albedo_color=color
    part.material_override=mat
    part.position=at
    parent.add_child(part)
    return part
