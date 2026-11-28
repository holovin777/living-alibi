extends SceneTree
const RoadGraph=preload("res://scripts/GeoRoadGraph.gd")
func _initialize() -> void:
    var movement=preload("res://scripts/GeoMovement.gd")
    if not movement.block_reason(false,false,false,false,true,true,{}).is_empty() or movement.block_reason(false,false,false,false,true,false,{}).is_empty():
        push_error("Geographic movement connection policy failed");quit(1);return
    if movement.block_reason(false,false,false,false,true,true,{"confined":true}).is_empty():
        push_error("Custody must restrict movement");quit(1);return
    if movement.outside_limit({"phase":"LINKED_ADULT","radius":24},Vector2.ZERO,Vector2(24,0),Vector2(25,0)) or not movement.outside_limit({"phase":"CHILDHOOD","radius":24},Vector2.ZERO,Vector2(24,0),Vector2(25,0)):
        push_error("Geographic life movement radius failed");quit(1);return
    var graph={"nodes":{"1":[0,0],"2":[10,0],"3":[10,10],"4":[0,10]},"edges":[
        {"a":"1","b":"2","walk":true,"drive_ab":true,"drive_ba":false,"name":"Via Uno"},
        {"a":"2","b":"3","walk":true,"drive_ab":false,"drive_ba":false,"name":"Via Due"},
        {"a":"3","b":"4","walk":false,"drive_ab":true,"drive_ba":true,"name":"Via Tre"}]}
    var roads=RoadGraph.new()
    roads.build(graph)
    var path: PackedVector2Array=roads.route(Vector2.ZERO,Vector2(10,10))
    if path.size()!=3 or not is_equal_approx(RoadGraph.length(path),20.0) or roads.street(Vector2(5,1))!="Via Uno":
        push_error("walking graph failed")
        quit(1)
        return
    roads.build(graph,"drive")
    if roads.route(Vector2(10,0),Vector2.ZERO,"drive").size()!=0 or roads.route(Vector2.ZERO,Vector2(10,0),"drive").size()!=2:
        push_error("one-way road failed")
        quit(1)
        return
    print("GeoRoadGraphSmoke: ALL CHECKS PASSED")
    quit(0)
