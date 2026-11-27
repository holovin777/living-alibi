extends Node3D
## Small camera-following visual volume; no independent weather decisions.
const COUNT=240
const RADIUS=13.0
var drops: Array[Vector3]=[]
var rng:=RandomNumberGenerator.new()
var instance: MultiMeshInstance3D
var mesh: BoxMesh
var mat: StandardMaterial3D
var mode:=""
var accumulator:=0.0

func _ready() -> void:
    rng.seed=8073
    mesh=BoxMesh.new()
    mesh.size=Vector3(0.018,0.46,0.018)
    mat=StandardMaterial3D.new()
    mat.shading_mode=BaseMaterial3D.SHADING_MODE_UNSHADED
    mat.transparency=BaseMaterial3D.TRANSPARENCY_ALPHA
    mesh.material=mat
    var multi:=MultiMesh.new()
    multi.transform_format=MultiMesh.TRANSFORM_3D
    multi.mesh=mesh
    multi.instance_count=COUNT
    instance=MultiMeshInstance3D.new()
    instance.multimesh=multi
    instance.custom_aabb=AABB(Vector3(-RADIUS,-1,-RADIUS),Vector3(RADIUS*2,17,RADIUS*2))
    add_child(instance)
    for i in range(COUNT):
        drops.append(Vector3(rng.randf_range(-RADIUS,RADIUS),rng.randf_range(0.1,14),rng.randf_range(-RADIUS,RADIUS)))
    hide()

func update_weather(data: Dictionary,player: Vector3,indoors: bool,delta: float) -> void:
    var next_mode:=str(data.get("sky","CLEAR"))
    visible=not indoors and next_mode in ["RAIN","SNOW"]
    if not visible:return
    position=Vector3(player.x,0,player.z)
    if mode!=next_mode:
        mode=next_mode
        mesh.size=Vector3(0.035,0.035,0.035) if mode=="SNOW" else Vector3(0.018,0.46,0.018)
        mat.albedo_color=Color(0.94,0.96,1.0,0.85) if mode=="SNOW" else Color(0.68,0.82,0.95,0.55)
    accumulator+=delta
    if accumulator<0.04:return
    var dt:=minf(accumulator,0.12)
    accumulator=0
    var velocity:=Vector3(0.7,-1.4,0.3) if mode=="SNOW" else Vector3(0.5,-13.0,0.3)
    for i in range(COUNT):
        var point: Vector3=drops[i]+velocity*dt
        if point.y<0.1:
            point=Vector3(rng.randf_range(-RADIUS,RADIUS),14,rng.randf_range(-RADIUS,RADIUS))
        point.x=wrapf(point.x,-RADIUS,RADIUS)
        point.z=wrapf(point.z,-RADIUS,RADIUS)
        drops[i]=point
        instance.multimesh.set_instance_transform(i,Transform3D(Basis.IDENTITY,point))
