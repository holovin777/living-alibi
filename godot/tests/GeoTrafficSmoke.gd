extends SceneTree
const Traffic=preload("res://scripts/GeoTraffic.gd")
func _initialize() -> void:
    var graph={"nodes":{"1":[35,0],"2":[65,0],"3":[65,35],"4":[35,35]},"edges":[
        {"a":"1","b":"2","walk":true,"drive_ab":true,"drive_ba":false},
        {"a":"2","b":"3","walk":true,"drive_ab":true,"drive_ba":false},
        {"a":"3","b":"4","walk":true,"drive_ab":true,"drive_ba":false},
        {"a":"4","b":"1","walk":true,"drive_ab":true,"drive_ba":false}]}
    var traffic=Traffic.new()
    root.add_child.call_deferred(traffic)
    _check.call_deferred(traffic,graph)

func _check(traffic,graph: Dictionary) -> void:
    traffic.configure(graph)
    var car: Node3D=traffic._car()
    if not car.has_node("ImportedVehicle"):
        car.free();push_error("Geographic car model missing");quit(1);return
    car.free()
    for i in range(120):traffic.update_traffic(Vector2.ZERO,0.1)
    if traffic.count("drive")<1 or traffic.count("walk")<1 or traffic.actors.size()>38:
        push_error("traffic population failed");quit(1);return
    for actor in traffic.actors:
        if actor["mode"]=="drive" and not traffic.motor.network.are_points_connected(actor["from"],actor["to"],false):
            push_error("traffic used invalid motor edge");quit(1);return
    traffic.update_traffic(Vector2(500,500),0.1)
    if not traffic.actors.is_empty():
        push_error("traffic failed to unload distant actors");quit(1);return
    print("GeoTrafficSmoke: ALL CHECKS PASSED")
    quit(0)
