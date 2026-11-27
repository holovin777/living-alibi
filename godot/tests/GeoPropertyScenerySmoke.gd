extends SceneTree
const Scenery=preload("res://scripts/GeoPropertyScenery.gd")
func _initialize() -> void:
    _check.call_deferred()
func _check() -> void:
    var scenery=Scenery.new()
    root.add_child(scenery)
    var city={"chunks":{"0,0":{"buildings":[{"points":[[-2,-2],[2,-2],[2,2],[-2,2]]}],"roads":[]}}}
    scenery.configure(city)
    if Scenery.clear_at(city,Vector2.ZERO,1.0) or not Scenery.clear_at(city,Vector2(20,0),1.0):
        push_error("Plot clearance failure")
        quit(1)
        return
    var yard={"anchor":{"x":0,"z":0},"garden":{"planted":true,"growth":2,"required":3,"greenhouse":true},"hens":{"coop":true,"hens":3},"orchard":{"plants":[{"planted":true,"age":5,"fruit":2}]}}
    var site={"anchor":{"x":100,"z":0},"stage":1,"active":true}
    scenery.sync({"yard":yard,"site":site})
    if scenery.hens.size()!=3 or scenery.models.size()!=2:
        push_error("Property visuals missing")
        quit(1)
        return
    var old=scenery.models["site"]
    site["stage"]=3
    scenery.sync({"yard":yard,"site":site})
    if old==scenery.models["site"]:
        push_error("Construction did not update")
        quit(1)
        return
    scenery.animate(0.5)
    scenery.sync({})
    if not scenery.models.is_empty() or not scenery.hens.is_empty():
        push_error("Property visuals not removed")
        quit(1)
        return
    print("GeoPropertyScenerySmoke: ALL CHECKS PASSED")
    quit(0)
