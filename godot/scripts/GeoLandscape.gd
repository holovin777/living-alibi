extends RefCounted
## Shared polygon records, clipped to each streamed tile before triangulation.
const LAND_COLORS={"farmland":Color("#8a8852"),"meadow":Color("#83995c"),"forest":Color("#526c42"),"orchard":Color("#748348"),"vineyard":Color("#7b8050"),"grass":Color("#8fa469"),"park":Color("#859e63"),"water":Color("#527e87")}
static func polygon(points: Array) -> PackedVector2Array:
    var out:=PackedVector2Array()
    for point in points:out.append(Vector2(float(point[0]),float(point[1])))
    return out

static func pieces(points: Array,cell: Vector2i,size: float) -> Array[PackedVector2Array]:
    var corner:=Vector2(cell)*size
    var clip:=PackedVector2Array([corner,corner+Vector2(size,0),corner+Vector2(size,size),corner+Vector2(0,size)])
    return Geometry2D.intersect_polygons(polygon(points),clip)

static func material(kind: String) -> Material:
    if kind=="water":
        var water:=StandardMaterial3D.new()
        water.albedo_color=LAND_COLORS[kind]
        water.roughness=0.25
        water.cull_mode=BaseMaterial3D.CULL_DISABLED
        return water
    var mat:=ShaderMaterial.new()
    mat.shader=preload("res://shaders/GeoLand.gdshader")
    var color: Color=LAND_COLORS.get(kind,Color("#83995c"))
    mat.set_shader_parameter("soil",color.darkened(0.24))
    mat.set_shader_parameter("crop",color.lightened(0.12))
    mat.set_shader_parameter("rows",1.0 if kind in ["farmland","orchard","vineyard"] else 0.0)
    return mat

static func clear_of_paths(point: Vector2,nearby: Array) -> bool:
    for data in nearby:
        for kind in ["roads","rail","water"]:
            for road in data.get(kind,[]):
                var a:=Vector2(float(road["a"][0]),float(road["a"][1]))
                var b:=Vector2(float(road["b"][0]),float(road["b"][1]))
                if point.distance_to(Geometry2D.get_closest_point_to_segment(point,a,b))<float(road["width"])*0.5+3.0:return false
        for building in data.get("buildings",[]):
            if Geometry2D.is_point_in_polygon(point,polygon(building["points"])):return false
    return true
