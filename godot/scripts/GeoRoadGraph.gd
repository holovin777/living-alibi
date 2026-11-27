extends RefCounted
## Routing along shared OSM node IDs, not visual line intersections.
var network := AStar2D.new()
var coordinates: Dictionary={}
var ids: Dictionary={}
var road_segments: Array=[]
var walk_nodes: Dictionary={}
var drive_nodes: Dictionary={}

func build(graph: Dictionary,mode: String="walk") -> void:
    network.clear()
    coordinates.clear()
    ids.clear()
    road_segments.clear()
    walk_nodes.clear()
    drive_nodes.clear()
    var points: Dictionary=graph.get("nodes",{})
    for raw_id in points:
        var p: Array=points[raw_id]
        var id: int=ids.size()+1
        var pos := Vector2(float(p[0]),float(p[1]))
        ids[raw_id]=id
        coordinates[id]=pos
        network.add_point(id,pos)
    for edge in graph.get("edges",[]):
        if not ids.has(edge["a"]) or not ids.has(edge["b"]):continue
        var a: int=ids[edge["a"]]
        var b: int=ids[edge["b"]]
        var walk := bool(edge.get("walk",false))
        var ab := bool(edge.get("drive_ab",false))
        var ba := bool(edge.get("drive_ba",false))
        if walk:
            walk_nodes[a]=true
            walk_nodes[b]=true
        if ab or ba:
            drive_nodes[a]=true
            drive_nodes[b]=true
        var start: Vector2=coordinates[a]
        var finish: Vector2=coordinates[b]
        road_segments.append({"a":start,"b":finish,"name":str(edge.get("name",""))})
        if mode=="walk" and walk:
            network.connect_points(a,b,true)
        elif mode=="drive":
            if ab:network.connect_points(a,b,false)
            if ba:network.connect_points(b,a,false)

func nearest(point: Vector2,mode: String="walk") -> int:
    var nodes := drive_nodes if mode=="drive" else walk_nodes
    var best := INF
    var result := -1
    for id in nodes:
        var pos: Vector2=coordinates[id]
        var dist := point.distance_squared_to(pos)
        if dist<best:
            best=dist
            result=id
    return result

func route(from: Vector2,to: Vector2,mode: String="walk") -> PackedVector2Array:
    var a := nearest(from,mode)
    var b := nearest(to,mode)
    if a<0 or b<0:return PackedVector2Array()
    return network.get_point_path(a,b)

func street(point: Vector2) -> String:
    var best := 30.0*30.0
    var result := ""
    for segment in road_segments:
        if str(segment["name"]).is_empty():continue
        var closest := Geometry2D.get_closest_point_to_segment(point,segment["a"],segment["b"])
        var dist := point.distance_squared_to(closest)
        if dist<best:
            best=dist
            result=segment["name"]
    return result

static func length(path: PackedVector2Array) -> float:
    var meters := 0.0
    for i in range(1,path.size()):meters+=path[i-1].distance_to(path[i])
    return meters
