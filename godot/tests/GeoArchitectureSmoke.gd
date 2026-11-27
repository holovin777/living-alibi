extends SceneTree
const Architecture=preload("res://scripts/GeoArchitecture.gd")
func _initialize() -> void:
    var detail=Architecture.new()
    detail.facade({"id":"1","kind":"apartments","height":9.6,"points":[[0,0],[12,0],[12,10],[0,10]]})
    detail.road({"a":[0,20],"b":[30,20],"width":7,"kind":"residential","sidewalk":"both"})
    detail.tree({"point":[12,20]})
    for kind in ["glass","door","pavement","mark","lamp","leaves"]:
        if not detail.batches.has(kind) or detail.batches[kind].is_empty() or detail.batches[kind].size()%3!=0:
            push_error("Invalid detail geometry: "+kind)
            quit(1)
            return
        if Architecture.material(kind)==null:
            quit(1)
            return
    print("GeoArchitectureSmoke: ALL CHECKS PASSED")
    quit(0)
