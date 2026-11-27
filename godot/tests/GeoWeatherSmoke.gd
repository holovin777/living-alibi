extends SceneTree
const Weather=preload("res://scripts/GeoWeather.gd")
func _initialize() -> void:
    _check.call_deferred()
func _check() -> void:
    var effect=Weather.new()
    root.add_child(effect)
    effect.update_weather({"sky":"RAIN"},Vector3(1,1,2),false,0.05)
    if not effect.visible or effect.mode!="RAIN" or effect.drops.size()!=Weather.COUNT:
        push_error("Rain not initialized")
        quit(1)
        return
    effect.update_weather({"sky":"SNOW"},Vector3.ZERO,false,0.05)
    if not effect.visible or effect.mode!="SNOW":
        push_error("Snow not initialized")
        quit(1)
        return
    effect.update_weather({"sky":"SNOW"},Vector3.ZERO,true,0.05)
    if effect.visible:
        push_error("Precipitation indoors")
        quit(1)
        return
    print("GeoWeatherSmoke: ALL CHECKS PASSED")
    quit(0)
