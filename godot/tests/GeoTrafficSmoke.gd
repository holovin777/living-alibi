extends SceneTree
const Traffic=preload("res://scripts/GeoTraffic.gd")
const Cockpit=preload("res://scripts/GeoCockpit.gd")
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
    var signals=preload("res://scripts/GeoSignals.gd")
    if signals.phase(0,0,Vector2(0,1))!=2 or signals.phase(0,0,Vector2(1,0))!=0 or signals.phase(0,17,Vector2(1,0))!=2 or signals.phase(0,12,Vector2(0,1))!=1:
        push_error("Signal phase mismatch");quit(1);return
    var motion=preload("res://scripts/GeoCommuteMotion.gd").new()
    motion.configure({"trip":"test","distance":0,"route":[{"x":0,"z":0},{"x":10,"z":0},{"x":10,"z":10}]},{Vector2i(1000,0):0})
    motion.set_target(20)
    motion.advance(20,0)
    if not motion.waiting or motion.at().distance_to(Vector2(7,0))>0.001:
        push_error("Commute signal stop failed");quit(1);return
    motion.advance(8,17)
    if motion.at().distance_to(Vector2(10,5))>0.001:
        push_error("Commute corner following failed");quit(1);return
    var cabin=Cockpit.new()
    root.add_child(cabin)
    cabin.update_instruments(32.0,1.0,0.2)
    if cabin.speed_display.text!="32 km/h" or cabin.steering.rotation.z>=0.0:
        cabin.queue_free();push_error("Cabin instruments failed");quit(1);return
    cabin.queue_free()
    traffic.configure(graph)
    for scooter in [true,false]:
        var bike: Node3D=traffic._two_wheeler(scooter)
        if bike.get_child_count()<12:
            bike.free();push_error("Two-wheeler model incomplete");quit(1);return
        bike.free()
    var ambulance: Node3D=traffic._ambulance()
    traffic.set_plate(ambulance,"LA EMS1","AMBULANCE")
    if not ambulance.has_node("BeaconL") or not ambulance.has_node("BeaconR") or not ambulance.has_node("EmergencyLight") or ambulance.get_node("RegistrationPlate").text!="LA EMS1":
        ambulance.free();push_error("Ambulance model incomplete");quit(1);return
    var sirens=preload("res://scripts/EmergencyAudio.gd")
    var sound: AudioStreamPlayer3D=sirens.attach(ambulance,"AMBULANCE")
    var wav: AudioStreamWAV=sound.stream
    if wav.data.size()!=76800 or wav.loop_end!=38400 or wav.loop_mode!=AudioStreamWAV.LOOP_FORWARD or sirens.attach(ambulance,"AMBULANCE")!=sound or sirens.tone("POLICE").data==wav.data:
        ambulance.free();push_error("Emergency audio synthesis failed");quit(1);return
    ambulance.free()
    var effect=preload("res://scripts/GeoFireVisual.gd").new()
    root.add_child(effect)
    effect.sync({"id":"test","x":10,"z":12,"intensity":0.8},Vector3.ZERO,true)
    effect.animate(1.0)
    if not effect.visible or effect.position.x!=10 or effect.flames.size()!=7 or effect.smoke.size()!=12 or effect.intensity<=0:
        effect.free();push_error("Fire visual failed");quit(1);return
    effect.sync({"id":"test","x":10,"z":12,"intensity":0.4,"fighting":true},Vector3.ZERO,true)
    effect.animate(0.2)
    if effect.spray.size()!=24 or not effect.spray[0].visible:
        effect.free();push_error("Suppression spray missing");quit(1);return
    effect.sync({"id":"test","x":10,"z":12,"intensity":0.4,"fighting":false},Vector3.ZERO,true)
    if effect.spray[0].visible:
        effect.free();push_error("Suppression spray did not stop");quit(1);return
    effect.sync({},Vector3.ZERO,true)
    if effect.visible:
        effect.free();push_error("Fire visual did not clear");quit(1);return
    effect.free()
    var fire: Node3D=traffic._fire_engine()
    traffic.set_plate(fire,"LA FIRE1","FIRE")
    if not fire.has_node("BeaconL") or not fire.has_node("BeaconR") or not fire.has_node("EmergencyLight"):
        fire.free();push_error("Fire engine model incomplete");quit(1);return
    fire.free()
    var truck: Node3D=traffic._truck()
    if truck.get_child_count()<14:
        truck.free();push_error("Truck exterior incomplete");quit(1);return
    truck.free()
    var car: Node3D=traffic._car()
    if not car.has_node("ImportedVehicle"):
        car.free();push_error("Geographic car model missing");quit(1);return
    var model:=car.get_node("ImportedVehicle") as Node3D
    if not model.has_meta("realistic_vehicle") or model.scale.x<=0.0:
        car.free();push_error("Vehicle pack integration missing");quit(1);return
    traffic.set_plate(car,"LA R3")
    traffic.set_plate(car,"LA R4")
    if car.get_node("RegistrationPlate").text!="LA R4" or not car.has_node("RegistrationBacking"):
        car.free();push_error("Registration plate failed");quit(1);return
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
