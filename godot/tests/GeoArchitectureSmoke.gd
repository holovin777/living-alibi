extends SceneTree
const Architecture=preload("res://scripts/GeoArchitecture.gd")
func _initialize() -> void:
    var detail=Architecture.new()
    detail.facade({"id":"1","kind":"apartments","height":9.6,"points":[[0,0],[12,0],[12,10],[0,10]]})
    detail.road({"a":[0,20],"b":[30,20],"width":7,"kind":"residential","sidewalk":"both"})
    detail.tree({"point":[12,20]})
    detail.bench({"id":"bench","point":[14,20]})
    if detail.lamp_positions.size()!=1 or detail.batches.get("bark",PackedVector3Array()).size()<=36:
        push_error("Missing light anchor or bench geometry")
        quit(1)
        return
    for kind in ["glass","door","pavement","mark","lamp","leaves"]:
        if not detail.batches.has(kind) or detail.batches[kind].is_empty() or detail.batches[kind].size()%3!=0:
            push_error("Invalid detail geometry: "+kind)
            quit(1)
            return
        if Architecture.material(kind)==null:
            quit(1)
            return
    var highway=Architecture.new()
    highway.road({"a":[0,0],"b":[20,0],"width":8,"kind":"trunk"})
    if highway.batches.get("mark",PackedVector3Array()).size()!=12 or highway.batches.has("pavement"):
        push_error("Highway edge marking geometry invalid")
        quit(1)
        return
    var bridge=Architecture.new()
    bridge.road({"a":[0,0],"b":[20,0],"width":8,"kind":"primary","bridge":true,"sidewalk":"both"})
    if not bridge.batches.has("bridge_concrete") or bridge.batches.get("iron",PackedVector3Array()).is_empty():
        push_error("Bridge railing geometry missing")
        quit(1)
        return
    var horizon=preload("res://scripts/GeoHorizon.gd")
    if horizon.ridge(470,0).size()!=960:
        push_error("Invalid mountain silhouette geometry")
        quit(1)
        return
    if Architecture.roof_kind("warehouse")!="flat" or Architecture.roof_kind("house")!="clay" or Architecture.texture_kind("wall_0")!="stucco" or Architecture.texture_kind("glass")!="":
        push_error("Invalid building surface selection");quit(1);return
    var sign_road={"a":[0,0],"b":[12,0],"kind":"residential"}
    if not Architecture.street_sign_candidate(sign_road):
        push_error("Named local street cannot host a plate");quit(1);return
    sign_road["bridge"]=true
    if Architecture.street_sign_candidate(sign_road):
        push_error("Street plate placed on a bridge");quit(1);return
    var capped=Architecture.new()
    capped.windows_count=Architecture.MAX_WINDOWS
    capped.facade({"id":"1","kind":"apartments","height":9.6,"points":[[0,0],[12,0],[12,10],[0,10]]})
    var wall_key: String="wall_%d" % posmod("1".hash(),Architecture.PALETTE.size())
    if capped.batches.get(wall_key,PackedVector3Array()).size()!=24:
        push_error("Window cap must not remove facade walls")
        quit(1)
        return
    print("GeoArchitectureSmoke: ALL CHECKS PASSED")
    quit(0)
