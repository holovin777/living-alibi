extends Node3D
## Artistic alpine silhouettes, not surveyed elevations or traversable terrain.
## The background follows horizontal player movement but never camera yaw.
var layers: Array[StandardMaterial3D]=[]
const COLORS: Array[Color]=[Color("#a1b6c7"),Color("#849eae"),Color("#6d8998")]

static func ridge(radius: float,layer: int) -> PackedVector3Array:
    var out:=PackedVector3Array()
    var previous:=Vector3.ZERO
    for i in range(161):
        var angle:=lerpf(-1.55,1.35,float(i)/160.0)
        var profile:=absf(sin(angle*9.2+layer*1.3))*0.42+absf(sin(angle*19.0+layer))*0.20+absf(sin(angle*37.0+layer*0.7))*0.11
        var fade:=sin(PI*float(i)/160.0)
        var height: float=fade*(9.0+profile*(46.0-float(layer)*7.0))
        var point:=Vector3(sin(angle)*radius,height,-cos(angle)*radius)
        if i>0:
            var a:=Vector3(previous.x,-3.0,previous.z)
            var b:=Vector3(point.x,-3.0,point.z)
            out.append_array(PackedVector3Array([a,b,point,a,point,previous]))
        previous=point
    return out

func _ready() -> void:
    for i in range(3):
        var builder:=SurfaceTool.new()
        builder.begin(Mesh.PRIMITIVE_TRIANGLES)
        for v in ridge(470.0-float(i)*12.0,i):builder.add_vertex(v)
        var instance:=MeshInstance3D.new()
        instance.mesh=builder.commit()
        var mat:=StandardMaterial3D.new()
        mat.shading_mode=BaseMaterial3D.SHADING_MODE_UNSHADED
        mat.cull_mode=BaseMaterial3D.CULL_DISABLED
        mat.albedo_color=COLORS[i]
        instance.material_override=mat
        instance.cast_shadow=GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
        add_child(instance)
        layers.append(mat)

func update_view(at: Vector3,daylight: float,overcast: bool,horizon_color: Color,indoors: bool) -> void:
    position=Vector3(at.x,0,at.z)
    visible=not indoors and daylight>0.06
    for i in range(layers.size()):
        var color:=Color("#182737").lerp(COLORS[i],clampf(daylight*2.0,0.0,1.0))
        layers[i].albedo_color=color.lerp(horizon_color,0.76 if overcast else 0.18)
