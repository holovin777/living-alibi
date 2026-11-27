extends SceneTree
const Room=preload("res://scripts/GeoInterior.gd")
func _initialize() -> void:
    _check.call_deferred()
func _check() -> void:
    for kind in ["MARKET","BAKERY","CAFE","SCHOOL","CLINIC","WORKSHOP","LIBRARY","OFFICE","FACTORY"]:
        var room=Room.new()
        root.add_child(room)
        room.build({"id":kind,"label":kind,"x":10.0,"z":20.0})
        if room.get_child_count()<12 or room.position!=Vector3(10,150,20):
            push_error("Interior build failed: "+kind)
            quit(1)
            return
        root.remove_child(room)
        room.queue_free()
    print("GeoInteriorSmoke: ALL CHECKS PASSED")
    quit(0)
