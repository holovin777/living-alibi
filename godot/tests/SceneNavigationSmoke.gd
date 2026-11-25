extends SceneTree
## Offline scene test: visual districts, gate UI and career kiosk can render
## even if the local Java server is not running. Backend HTTP paths are
## independently exercised by Java CI.

func _initialize() -> void:
    call_deferred("_exercise")

func _exercise() -> void:
    var packed: PackedScene = load("res://scenes/park.tscn")
    if packed == null:
        push_error("Cannot load the initial park scene.")
        quit(1)
        return
    var world = packed.instantiate()
    root.add_child(world)
    await process_frame

    var clock = preload("res://scripts/LiveClock.gd").new()
    for i in range(5):
        if not _check(not clock.advance(1.0,true), "1:10 waits six real seconds"):
            return
    if not _check(clock.advance(1.0,true), "1:10 advances one game minute"):
        return
    if not _check(not clock.advance(1.0,false) and is_zero_approx(clock.elapsed),
            "busy or menu time does not accumulate"):
        return
    clock.set_speed(60)
    if not _check(clock.advance(1.0,true), "1:60 advances every real second"):
        return
    clock.set_speed(0)
    if not _check(not clock.advance(1.0,true), "paused clock sends no request"):
        return
    world._clock_pending = true
    world._on_request_failed("uncertain response")
    if not _check(not world._clock_pending and world._clock.speed == 0
            and world._clock_selector.get_selected_id() == 0,
            "automatic time pauses after uncertain server response"):
        return
    world._clock_selector.select(3)
    world._on_clock_speed_selected(3)
    if not _check(world._clock.speed == 10, "speed selection restores default"):
        return

    # First person is the only camera mode, including after old V input.
    if not _check(not world._avatar.visible and world._avatar.get_child_count()==0
            and world._camera.position.is_equal_approx(
                    world._avatar.position+Vector3(0,world.EYE_HEIGHT,0)),
            "visitor starts at eye level without a third-person mesh"):
        return
    var legacy_key := InputEventKey.new()
    legacy_key.keycode = KEY_V
    legacy_key.pressed = true
    world._unhandled_key_input(legacy_key)
    world._view_yaw = PI/2.0
    world._view_pitch = 0.25
    world._avatar.position = Vector3(1,0,2)
    world._follow_player()
    if not _check(not world._avatar.visible
            and world._camera.position.is_equal_approx(
                    world._avatar.position+Vector3(0,world.EYE_HEIGHT,0))
            and is_equal_approx(world._camera.rotation.y,world._view_yaw)
            and is_equal_approx(world._camera.rotation.x,world._view_pitch),
            "old V key cannot change first-person viewpoint"):
        return
    world._avatar.position = Vector3.ZERO
    world._view_yaw = 0.0
    world._view_pitch = 0.0
    world._follow_player()

    var layout = world._town_map
    if not _check(layout.DISTRICTS.size() == 19
            and layout.world_center("PARK") == Vector3.ZERO
            and layout.adjacent("PARK", "SCHOOL")
            and not layout.adjacent("PARK", "QUARRY")
            and not layout.DISTRICTS.has("MARCO_HOME"),
            "public map defines all districts, neighbours and no private residences"):
        return
    world._toggle_map()
    if not _check(world._map_panel.visible and not world._travel_panel.visible
            and not world._action_panel.visible and layout.current_place == "PARK",
            "map opens independently of the gate"):
        return
    world._toggle_map()
    if not _check(not world._map_panel.visible, "map closes without travelling"):
        return

    # The nine surrounding chunks are real scenery, reused across arrivals.
    if not _check(world._chunks.size() == 9 and world._chunks.has("SCHOOL")
            and world._chunks.has("CENTRO") and not world._chunks.has("QUARRY"),
            "park streams its eight nearest neighbours only"):
        return
    var school_root: Node3D = world._chunks["SCHOOL"]["root"]
    var park_root: Node3D = world._chunks["PARK"]["root"]
    if not _check(school_root.position == Vector3(0,0,-48)
            and park_root.position == Vector3.ZERO,
            "neighbour geometry aligns with the public map coordinates"):
        return
    world._on_world_updated(_world("SCHOOL", "School"))
    if not _check(world._scenery == school_root
            and school_root.position == Vector3.ZERO
            and park_root.position == Vector3(0,0,48)
            and world._chunks.size() == 6,
            "arriving at school reuses visible geometry and unloads distant rows"):
        return
    world._on_world_updated(_world("PARK", "Park"))
    if not _check(world._scenery == park_root and world._chunks.size() == 9,
            "returning to park keeps its cached geometry"):
        return

    var centro_root: Node3D = world._chunks["CENTRO"]["root"]
    world._on_world_updated(_world("CENTRO","Centro Storico"))
    if not _check(world._scenery == centro_root and centro_root.has_node("PiazzaCentrale")
            and world._road_links.size() == 4 and world._park_lamps.size() == 4,
            "historic square has cached scenery, four roads and lamps"):
        return
    if not _check(world._can_stand(Vector3(-13,0,-9),world.WALKER_RADIUS)
            and not world._can_stand(Vector3(-15.8,0,-9),world.WALKER_RADIUS),
            "portico is walkable but building back wall is solid"):
        return
    var square := centro_root.get_node("PiazzaCentrale")
    var paving := square.get_node("StonePaving") as MultiMeshInstance3D
    if not _check(paving.multimesh.instance_count == 1536 and square.has_node("Facade_0")
            and square.has_node("Facade_3") and square.has_node("CortileDeiFiori"),
            "historic square has batched paving, distinct facades and a garden court"):
        return
    if not _check(world._can_stand(Vector3(-6.4,0,-14.3),world.WALKER_RADIUS)
            and not world._can_stand(Vector3(-8.4,0,-14.3),world.WALKER_RADIUS),
            "courtyard centre is accessible and boundary wall blocks walking"):
        return
    for at in [Vector3(0,0,-23.9),Vector3(23.9,0,0),Vector3(0,0,23.9),Vector3(-23.9,0,0)]:
        if not _check(world._can_stand(at,world.WALKER_RADIUS,true),
                "all four Centro exits remain walkable"):
            return
    world._on_world_updated(_world("PARK","Park"))

    world._on_world_updated(_world("PORTICI","Via dei Portici"))
    if not _check(world._scenery.has_node("ViaDeiPortici/BottegaAlimentare")
            and world._road_links.size() == 4
            and world._road_destination(Vector3(0,0,-23.9),23.8) == "CENTRO",
            "historic street connects to the square and has a real shop doorway"):
        return
    var entered: Vector3 = world._move_on_ground(Vector3(3,0,5.4),Vector3(4,0,0),world.WALKER_RADIUS)
    if not _check(entered.x > 6.8 and not world._can_stand(Vector3(4.4,0,3.2),world.WALKER_RADIUS),
            "walk through open grocery door, but not through its wall"):
        return
    world._on_world_updated(_world("PARK","Park"))

    var guest := _world("NORTH_RESIDENCES","North residences")
    world._on_world_updated(guest)
    if not _check(world._scenery.has_node("CasaOspiti/LeaseDoor")
            and not world._can_stand(Vector3(-5,0,8.5),world.WALKER_RADIUS),
            "unrented guest room is locked"):
        return
    guest["player"]["roomActive"] = true
    guest["player"]["roomUntil"] = 10080
    world._on_world_updated(guest)
    var door := world._scenery.get_node("CasaOspiti/LeaseDoor") as Node3D
    var room_path: Vector3 = world._move_on_ground(Vector3(-3,0,8.5),Vector3(-6.7,0,0),world.WALKER_RADIUS,true)
    if not _check(not door.visible and room_path.x < -9.5,
            "paid room opens and bedroom can be reached through both doorways"):
        return
    world._avatar.position = Vector3(-9.8,0,7.7)
    world._render_actions()
    if not _check(world._near_room_bed() and _has_button(world._action_choices,"Sleep in guest room"),
            "bed gives sheltered sleep actions instead of reception"):
        return
    world._avatar.position = Vector3(-5.8,0,11.1)
    world._render_actions()
    if not _check(world._near_kitchen() and _has_button(world._action_choices,"Cook bread soup")
            and _has_button(world._action_choices,"Store bread"),"kitchen exposes cooking and storage"):
        return
    world._avatar.position = Vector3(-10.6,0,12.2)
    world._render_actions()
    if not _check(world._near_bath() and _has_button(world._action_choices,"Wash"),"bathroom exposes hygiene"):
        return
    guest["weather"] = {"sky":"RAIN","raining":true}
    world._on_world_updated(guest)
    if not _check(world._weather_fx.sheltered and not world._weather_fx.rain.emitting,
            "room suppresses local precipitation"):
        return
    guest["player"]["roomActive"] = false
    world._on_world_updated(guest)
    if not _check(door.visible and world._avatar.position == world.ROOM_EXIT
            and not world._weather_fx.sheltered,
            "expired lease moves visitor safely outside and closes entrance"):
        return
    world._on_world_updated(_world("PARK","Park"))

    world._on_world_updated(_world("FARM","Cascina delle Risaie"))
    if not _check(world._scenery.has_node("CascinaDelleRisaie")
            and world._road_destination(Vector3(0,0,-23.9),23.8) == "RIVER"
            and not world._can_stand(Vector3(10,0,-8),world.WALKER_RADIUS)
            and _has_button(world._action_choices,"Apply: farm worker"),
            "farm district is connected with solid paddy and available job"):
        return
    world._on_world_updated(_world("PARK","Park"))

    var cyclist := _world("PARK","Park")
    cyclist["player"]["ownsBicycle"] = true
    cyclist["player"]["cycling"] = true
    world._on_world_updated(cyclist)
    if not _check(world._movement_speed() == world.CYCLE_SPEED
            and world._transport_button.text.contains("cycling"),
            "confirmed bicycle mode speeds up local outdoor travel"):
        return
    if not _check(world._bike_model.visible and world._scenery.has_node("BicycleRack/ParkedBicycle"),
            "cycling has a visible bike model and public rack"):
        return
    cyclist["player"]["cycling"] = false
    cyclist["player"]["bicycleParked"] = "PARK"
    world._on_world_updated(cyclist)
    var parked_model := world._scenery.get_node("BicycleRack/ParkedBicycle") as Node3D
    if not _check(parked_model.visible and not world._bike_model.visible and world._transport_button.disabled,
            "parked bicycle remains at rack and cannot be ridden"):
        return
    cyclist["player"]["bicycleParked"] = null
    world._on_world_updated(cyclist)
    if not _check(world._movement_speed() == world.WALK_SPEED,
            "walking mode restores ordinary movement"):
        return

    if not _check(layout.motor_road("WORKSHOP","FACTORY")
            and layout.motor_road("FACTORY","WORKSHOP")
            and not layout.motor_road("PARK","CENTRO"),
            "motor links are symmetric and historic square remains pedestrian"):
        return
    world._on_world_updated(_world("WORKSHOP","Workshop"))
    if not _check(world._scenery.has_node("OfficinaMotorCourt/ServiceGarage/CityCar")
            and world._scenery.has_node("DistrictRoads/MotorLane")
            and not world._can_stand(Vector3(-13.1,0,-10),world.WALKER_RADIUS),
            "parking court has a solid car and marked motor links"):
        return
    if not _check(world._traffic_edges.has("FACTORY:WORKSHOP")
            and world._scenery.has_node("DistrictRoads/MotorLane/TrafficSignal")
            and world._traffic_phase(0.0)==2 and world._traffic_phase(11.0)==1
            and world._traffic_phase(16.0)==0,
            "loaded motor link has cars and a three-phase crossing signal"):
        return
    var traffic: Dictionary = world._traffic_edges["FACTORY:WORKSHOP"]
    var inbound: Dictionary = traffic["cars"][1]
    inbound["delay"] = 0.0
    inbound["distance"] = 14.9
    world._traffic_seconds = 16.0
    world._position_traffic_edge(traffic,"WORKSHOP")
    world._advance_traffic(0.1)
    if not _check(is_equal_approx(float(inbound["distance"]),15.0),
            "car stops before crossing on red"):
        return
    world._traffic_seconds = 0.0
    world._advance_traffic(0.1)
    if not _check(float(inbound["distance"])>15.0,
            "car proceeds on green"):
        return
    var inbound_car: Node3D = inbound["node"]
    world._avatar.position = inbound_car.position+Vector3(0,0,-3)
    var stopped := float(inbound["distance"])
    world._advance_traffic(0.1)
    if not _check(is_equal_approx(float(inbound["distance"]),stopped)
            and not world._can_stand(inbound_car.position,world.WALKER_RADIUS,true),
            "car yields to pedestrian ahead and has a collision footprint"):
        return
    world._avatar.position = Vector3.ZERO
    var garage_entry: Vector3 = world._move_on_ground(Vector3(7.5,0,5),Vector3(0,0,7),world.WALKER_RADIUS)
    if not _check(garage_entry.z > 11.9
            and not world._can_stand(Vector3(10,0,12),world.WALKER_RADIUS)
            and world._road_destination(Vector3(0,0,-23.9),23.8) == "FACTORY",
            "open garage bay remains walkable, walls and district seam intact"):
        return
    world._on_world_updated(_world("PARK","Park"))

    var driver := _world("WORKSHOP","Workshop")
    driver["player"]["carPlace"] = "WORKSHOP"
    driver["player"]["carFuel"] = 6
    driver["player"]["carCondition"] = 80
    driver["player"]["carDestinations"] = ["LIBRARY","FACTORY","QUARRY"]
    world._on_world_updated(driver)
    world._avatar.position = world.CAR_ARRIVAL
    world._render_actions()
    var owned_car := world._scenery.get_node("PublicCarPark/OwnedCar") as Node3D
    if not _check(owned_car.visible and world._near_car()
            and _has_button(world._action_choices,"Drive to Library")
            and not world._can_stand(world.CAR_PARK,world.WALKER_RADIUS),
            "owned car is parked physically and offers nearby motor routes"):
        return
    var controls = preload("res://scripts/DriveControls.gd").new()
    for i in range(100):
        controls.accept(controls.propose(0.1,1.0,0.0,false,0))
    if not _check(is_equal_approx(controls.distance,15.0) and is_zero_approx(controls.speed),
            "manual drive stops at red crossing"):
        return
    for i in range(15):
        controls.accept(controls.propose(0.1,1.0,1.0,false,2))
    if not _check(controls.distance>15.0 and controls.lateral<=1.851 and controls.speed>0,
            "gas and limited steering move car on green"):
        return
    controls.accept(controls.propose(0.1,0.0,0.0,true,2))
    if not _check(controls.speed<4.5,"brake slows car"):
        return
    for edge in world._traffic_edges.values():
        for record in edge["cars"]:
            record["node"].visible = false
    if not _check(world._begin_driving("LIBRARY") and world._cockpit.visible
            and world._camera.position.is_equal_approx(world._avatar.position+Basis(Vector3.UP,world._view_yaw)*world.DRIVER_EYE),
            "manual driving starts in first-person cabin"):
        return
    world._end_driving(true)
    if not _check(not world._cockpit.visible and world._avatar.position==world.CAR_ARRIVAL
            and world._camera.position.is_equal_approx(world.CAR_ARRIVAL+Vector3(0,world.EYE_HEIGHT,0)),
            "cancelling restores first-person walking at original car"):
        return
    driver = driver.duplicate(true) # A fresh server response, not mutation of the previous snapshot.
    driver["player"]["carPlace"] = "LIBRARY"
    driver["player"]["placeId"] = "LIBRARY"
    driver["player"]["place"] = "Library"
    world._on_world_updated(driver)
    if not _check(world._avatar.position==world.CAR_ARRIVAL and world._near_car()
            and world._camera.position.is_equal_approx(world.CAR_ARRIVAL+Vector3(0,world.EYE_HEIGHT,0)),
            "confirmed car trip arrives at destination parking"):
        return
    driver = driver.duplicate(true)
    driver["player"]["carDestinations"] = ["WORKSHOP","OFFICE"]
    world._on_world_updated(driver)
    for edge in world._traffic_edges.values():
        for record in edge["cars"]:
            record["node"].visible = false
    if not _check(world._begin_driving("OFFICE"),"second motor route can start"):
        return
    world._drive_pending = true
    driver = driver.duplicate(true)
    driver["player"]["placeId"] = "OFFICE"
    driver["player"]["place"] = "Office"
    driver["player"]["carPlace"] = "OFFICE"
    driver["player"]["carDestinations"] = ["FACTORY","LIBRARY"]
    driver["player"]["carFuel"] = 5
    world._on_world_updated(driver)
    if not _check(world._drive_parked and world._drive_target.is_empty()
            and world._cockpit.visible and world._avatar.position==world.CAR_PARK
            and world._camera.position.is_equal_approx(world.CAR_PARK+world.DRIVER_EYE)
            and world._drive_hud.text.contains("1: Factory"),
            "confirmed trip keeps the driver seated at the new parking space"):
        return
    for edge in world._traffic_edges.values():
        for record in edge["cars"]:
            record["node"].visible = false
    if not _check(world._begin_driving("FACTORY") and not world._drive_parked and world._cockpit.visible,
            "choose another adjacent road without leaving the cabin"):
        return
    world._end_driving(true)
    world._on_world_updated(_world("PARK","Park"))

    var station := _world("FUEL_STATION","Stazione di servizio")
    station["player"]["carPlace"] = "FUEL_STATION"
    station["fuel"] = {"stock":48,"capacity":48,"price":2}
    world._on_world_updated(station)
    if not _check(layout.motor_road("QUARRY","FUEL_STATION")
            and world._scenery.has_node("StazioneDiServizio/StationShop")
            and not world._can_stand(Vector3(-12,0,-6),world.WALKER_RADIUS)
            and _has_button(world._action_choices,"Refuel")
            and _has_button(world._action_choices,"Apply: fuel station attendant"),
            "station has solid pumps, a connected motor road and local services"):
        return
    world._on_world_updated(_world("PARK","Park"))

    var cargo := _world("QUARRY","Quarry")
    cargo["player"]["jobId"] = "freight-driver"
    cargo["freight"] = {"active":true,"truckPlace":"QUARRY","nextDestination":"WORKSHOP","iron":2,"copper":1}
    world._on_world_updated(cargo)
    world._avatar.position = world.TRUCK_EXIT
    world._render_actions()
    if not _check(world._near_truck()
            and world._scenery.get_node("FreightParking/AssignedTruck").visible
            and _has_button(world._action_choices,"Drive truck to Workshop"),
            "assigned truck has visible cargo and a legal route"):
        return
    for edge in world._traffic_edges.values():
        for record in edge["cars"]:
            record["node"].visible = false
    if not _check(world._begin_driving("WORKSHOP",true) and world._drive_freight
            and world._cockpit.visible and world._drive_hud.text.contains("FREIGHT"),
            "truck uses the first-person driving controls"):
        return
    world._end_driving(true)
    world._on_world_updated(_world("PARK","Park"))

    var produce := _world("FARM","Cascina delle Risaie")
    produce["player"]["jobId"] = "freight-driver"
    produce["freight"] = {"active":true,"truckPlace":"FARM","nextDestination":"BAKERY","firstLabel":"Vegetables","secondLabel":"Rice","firstUnits":2,"secondUnits":1}
    world._on_world_updated(produce)
    world._avatar.position = world._truck_exit("FARM")
    world._render_actions()
    if not _check(layout.motor_road("FARM","BAKERY") and layout.motor_road("CAFE","SOUTH_RESIDENCES")
            and layout.motor_road("SOUTH_RESIDENCES","MARKET") and not layout.motor_road("PORTICI","CENTRO")
            and world._near_truck() and world._scenery.get_node("FreightParking/AssignedTruck").visible
            and world._action_title.text.contains("Vegetables 2") and _has_button(world._action_choices,"Drive truck to Bakery"),
            "farm freight route and cargo display preserve pedestrian centre"):
        return
    world._on_world_updated(_world("PARK","Park"))

    # Physical footprints: long steps cannot tunnel through solid objects.
    var kiosk_stop: Vector3 = world._move_on_ground(Vector3(7,0,6),
            Vector3(0,0,-6), world.WALKER_RADIUS)
    if not _check(kiosk_stop.z > 4.1 and kiosk_stop.z < 4.3
            and world._can_stand(kiosk_stop, world.WALKER_RADIUS),
            "kiosk blocks even a long movement step"):
        return
    var fountain_stop: Vector3 = world._move_on_ground(Vector3(8,0,-1),
            Vector3(0,0,-8), world.WALKER_RADIUS)
    if not _check(fountain_stop.z > -2.8 and fountain_stop.z < -2.5,
            "visitor cannot walk through the fountain"):
        return
    var tree_stop: Vector3 = world._move_on_ground(Vector3(-8,0,-3),
            Vector3(0,0,-4), world.WALKER_RADIUS)
    if not _check(tree_stop.z > -4.5 and tree_stop.z < -4.3,
            "tree trunk blocks movement"):
        return
    var gate_pass: Vector3 = world._move_on_ground(Vector3(0,0,-9),
            Vector3(0,0,-5), world.WALKER_RADIUS)
    if not _check(gate_pass.is_equal_approx(Vector3(0,0,-14)),
            "the open gate remains passable"):
        return
    var slid: Vector3 = world._move_on_ground(Vector3(7,0,4.3),
            Vector3(0.6,0,-1.0), world.WALKER_RADIUS)
    if not _check(slid.x > 7.5 and slid.z >= 4.1,
            "diagonal movement slides along an obstacle"):
        return
    var edge: Vector3 = world._move_on_ground(Vector3(16,0,0),
            Vector3(4,0,0), world.WALKER_RADIUS)
    if not _check(edge.x <= 17.0 and edge.x > 16.8,
            "district boundary remains enforced"):
        return

    if not _check(world._road_links.size() == 4
            and world._road_destination(Vector3(0,0,-23.9),23.8) == "SCHOOL"
            and world._road_destination(Vector3(23.9,0,0),23.8) == "LIBRARY"
            and world._road_destination(Vector3(-23.9,0,0),23.8) == "CENTRO"
            and world._road_destination(Vector3(0,0,23.9),23.8) == "RIVER",
            "park has four walkable connections matching the town layout"):
        return
    var road_end: Vector3 = world._move_on_ground(Vector3(0,0,16),
            Vector3(0,0,12),world.WALKER_RADIUS,true)
    if not _check(road_end.z >= 23.8 and road_end.z <= 24.001,
            "visitor can walk to a road seam"):
        return
    var offroad: Vector3 = world._move_on_ground(Vector3(8,0,16),
            Vector3(0,0,12),world.WALKER_RADIUS,true)
    if not _check(offroad.z <= 17.0, "off-road district boundary remains solid"):
        return
    # Simulate server rejection and committed arrival without a live Java process.
    world._road_pending = "SCHOOL"
    world._road_arrival = Vector3(0,0,23)
    world._on_request_failed("offline test")
    if not _check(world._road_pending.is_empty() and world._road_blocked == "SCHOOL"
            and world._current_place == "PARK", "failed crossing keeps the original district"):
        return
    world._road_pending = "SCHOOL"
    world._view_yaw = 0.25
    world._on_world_updated(_world("SCHOOL", "School"))
    if not _check(world._current_place == "SCHOOL"
            and world._avatar.position == Vector3(0,0,23)
            and is_equal_approx(world._view_yaw,0.25)
            and world._road_destination(Vector3(0,0,23.9),23.8) == "PARK",
            "confirmed road arrival preserves orientation and enters opposite edge"):
        return
    world._road_pending = "PARK"
    world._road_arrival = Vector3(0,0,-23)
    world._on_world_updated(_world("PARK", "Park"))
    world._avatar.position = Vector3.ZERO
    world._view_yaw = 0.0
    world._follow_player()
    world._on_world_updated(_world("RIVER", "River"))
    var bridge: Vector3 = world._move_on_ground(Vector3(-5,0,0),
            Vector3(-12,0,0),world.WALKER_RADIUS,true)
    if not _check(bridge.x < -16.8 and world._can_stand(bridge,world.WALKER_RADIUS,true),
            "river crossing has a walkable bridge"):
        return
    world._on_world_updated(_world("PARK", "Park"))

    var populated_park := _world("PARK", "Park")
    populated_park["residents"] = [
        {"id": "marco", "name": "Marco Bianchi", "activity": "WALKING"},
        {"id": "elena", "name": "Elena Rossi", "activity": "SHOPPING"}
    ]
    world._on_world_updated(populated_park)
    if not _check(world._residents_root.get_child_count() == 2
            and world._residents_root.get_child(0).name == "Resident_marco",
            "only actually co-present residents appear in the park"):
        return
    var marco = world._residents_root.get_node("Resident_marco")
    var start_position: Vector3 = marco.position
    world._animate_residents(0.5)
    if not _check(marco.position.distance_to(start_position) > 0.01,
            "walking residents move within the local visual scene"):
        return
    world._on_world_updated(populated_park)
    if not _check(world._chunks["SCHOOL"]["residents"].get_child_count() == 0,
            "neighbour scenery never fabricates unseen residents"):
        return
    if not _check(world._residents_root.get_node("Resident_marco") == marco,
            "refresh retains the same resident model and animation state"):
        return
    var rainy := _world("PARK", "Park")
    rainy["weather"] = {"sky":"RAIN","raining":true,"snowing":false}
    world._on_world_updated(rainy)
    if not _check(world._weather_fx.rain.emitting and not world._weather_fx.snow.emitting
            and world._environment.fog_enabled and world._weather_fx.ambience.stream != null,
            "rain starts particles, visibility fog and synthetic rain ambience"):
        return
    world._set_weather_visuals(false)
    if not _check(not world._weather_fx.rain.emitting and not world._environment.fog_enabled
            and world._last_snapshot["weather"]["raining"],
            "visual toggle does not change authoritative weather"):
        return
    world._set_weather_visuals(true)
    world._set_weather_audio(false)
    if not _check(world._weather_fx.rain.emitting and world._weather_fx.ambience.stream == null,
            "weather audio can be muted independently"):
        return
    var snowy := _world("PARK", "Park")
    snowy["weather"] = {"sky":"SNOW","raining":false,"snowing":true}
    world._on_world_updated(snowy)
    if not _check(world._weather_fx.snow.emitting and not world._weather_fx.rain.emitting,
            "snow switches precipitation without overlapping rain"):
        return
    world._weather_fx.follow(Vector3(3,0,4))
    if not _check(world._weather_fx.position == Vector3(3,9,4),
            "one precipitation emitter follows the player across chunks"):
        return
    world._on_world_updated(_world("PARK", "Park"))
    if not _check(not world._weather_fx.rain.emitting and not world._weather_fx.snow.emitting
            and not world._environment.fog_enabled,
            "clear weather removes precipitation and fog"):
        return
    world._set_weather_audio(true)

    var social := _world("PARK", "Park")
    social["residents"] = [{"id":"marco","name":"Marco","activity":"FREE_TIME",
            "knownName":"Alex","conversations":2,"relationship":"FAMILIAR"}]
    world._on_world_updated(social)
    var social_person = world._residents_root.get_node("Resident_marco")
    world._avatar.position = social_person.position
    if not _check(world._nearest_resident() == "marco"
            and social_person.get_meta("known_name") == "Alex"
            and social_person.get_meta("relationship") == "FAMILIAR",
            "nearby resident carries server-owned conversation memory"):
        return
    world._avatar.position = Vector3(16,0,16)
    if not _check(world._nearest_resident().is_empty(),
            "distant residents cannot be selected for conversation"):
        return
    world._avatar.position = Vector3.ZERO

    var noon := _world("PARK", "Park")
    noon["minute"] = 720
    world._on_world_updated(noon)
    var noon_light: float = world._sun.light_energy
    var dawn := _world("PARK", "Park")
    dawn["minute"] = 360
    world._on_world_updated(dawn)
    if not _check(noon_light > world._sun.light_energy,
            "solar lighting follows committed world minutes"):
        return
    var night := _world("PARK", "Park")
    night["minute"] = 1260
    night["services"] = {"lightsWorking": true, "fountainWorking": true, "cleanWater": 5}
    world._on_world_updated(night)
    if not _check(world._park_lamps.size() == 4 and world._park_lamps[0].visible
            and world._fountain_water.visible,
            "night park lights and water reflect working municipal services"):
        return
    night["services"] = {"lightsWorking": false, "fountainWorking": false, "cleanWater": 0}
    world._on_world_updated(night)
    if not _check(not world._park_lamps[0].visible and not world._fountain_water.visible,
            "broken services turn off real visual indicators"):
        return
    world._on_world_updated(_world("PARK", "Park"))
    if not _check(world._residents_root.get_child_count() == 0,
            "residents disappear when the authoritative snapshot reports nobody"):
        return

    world._on_world_updated(_world("PARK", "Park"))
    if not _check(world._current_place == "PARK",
            "first scene is the park"):
        return
    if not _check(world._scenery.name == "PublicScene_PARK",
            "park scenery was generated"):
        return
    world._avatar.position = world.KIOSK_POSITION + Vector3(0,0,2)
    world._toggle_actions()
    if not _check(_has_button(world._action_choices, "Drink from park fountain"),
            "public park offers the limited fountain separately from unsafe puddles"):
        return
    world._action_panel.visible = false


    world._on_locations_updated([
        {"id": "PARK", "label": "Park", "travelMinutes": 0},
        {"id": "MARKET", "label": "Market", "travelMinutes": 10},
        {"id": "SCHOOL", "label": "School", "travelMinutes": 10}
    ])
    if not _check(layout.travel_minutes.get("SCHOOL") == 10,
            "map reads travel minutes from the Java location response"):
        return
    world._avatar.position = world.GATE_POSITION
    world._toggle_travel()
    if not _check(world._travel_panel.visible,
            "gate opens only when the visitor approaches"):
        return
    if not _check(world._travel_choices.get_child_count() == 3,
            "destinations show server-supplied travel times"):
        return

    world._on_world_updated(_world("MARKET", "Market"))
    if not _check(world._chunks.size() == 6 and not world._chunks.has("PARK")
            and world._chunks["CENTRO"]["root"].position == Vector3(48,0,0),
            "long-distance travel unloads distant chunks and rebases the grid"):
        return
    if not _check(layout.current_place == "MARKET" and layout.travel_minutes.is_empty(),
            "map follows committed arrival and clears stale travel estimates"):
        return
    var wall_stop: Vector3 = world._move_on_ground(Vector3(-7,0,0),
            Vector3(0,0,9), world.WALKER_RADIUS)
    if not _check(wall_stop.z > 3.5 and wall_stop.z < 3.8,
            "market building has a solid exterior"):
        return
    if not _check(world._scenery.name == "PublicScene_MARKET" and
            world._avatar.position == Vector3.ZERO,
            "authoritative market arrival regenerates scenery and spawns visitor"):
        return
    world._avatar.position = world.KIOSK_POSITION + Vector3(0,0,2)
    world._toggle_actions()
    if not _check(world._action_panel.visible and
            _has_button(world._action_choices, "Apply: market assistant"),
            "market has its actual employment action"):
        return

    world._on_world_updated(_world("SCHOOL", "School"))
    world._avatar.position = world.KIOSK_POSITION + Vector3(0,0,2)
    world._toggle_actions()
    if not _check(world._action_panel.visible and
            _has_button(world._action_choices, "Study engineering"),
            "school provides engineering classes rather than arbitrary jobs"):
        return

    world._on_world_updated(_world("WORKSHOP", "Workshop"))
    world._avatar.position = world.KIOSK_POSITION + Vector3(0,0,2)
    world._toggle_actions()
    if not _check(_has_button(world._action_choices, "Apply: technician"),
            "workshop provides technician applications"):
        return
    world._on_work_orders_updated([{
        "id": "water-pump", "place": "WORKSHOP",
        "title": "Park water-pump maintenance",
        "phase": "OPEN", "condition": 45
    }])
    if not _check(_has_button(world._action_choices, "Diagnose: water-pump"),
            "actual workshop fault offers diagnosis rather than free wages"):
        return
    world._on_work_orders_updated([{
        "id": "water-pump", "place": "WORKSHOP",
        "title": "Park water-pump maintenance",
        "phase": "INSPECTED", "condition": 45
    }])
    if not _check(_has_button(world._action_choices, "Repair / implement: water-pump"),
            "inspected workshop fault offers repair"):
        return

    world._on_world_updated(_world("OFFICE", "Office"))
    world._avatar.position = world.KIOSK_POSITION + Vector3(0,0,2)
    world._toggle_actions()
    world._on_work_orders_updated([{
        "id": "street-lights", "place": "OFFICE",
        "title": "Public lighting efficiency upgrade",
        "phase": "OPEN", "condition": 50
    }])
    if not _check(_has_button(world._action_choices, "Diagnose: street-lights"),
            "office offers engineering task rather than magically paying salary"):
        return

    world._on_world_updated(_world("QUARRY", "Quarry"))
    if not _check(world._scenery.name == "PublicScene_QUARRY",
            "quarry has its own physical district"):
        return
    world._avatar.position = world.KIOSK_POSITION + Vector3(0,0,2)
    world._toggle_actions()
    if not _check(_has_button(world._action_choices, "Apply: quarry miner"),
            "quarry offers a real paid mining career"):
        return
    var employed_miner = _world("QUARRY", "Quarry")
    employed_miner["player"]["jobId"] = "miner"
    world._on_world_updated(employed_miner)
    if not _check(_has_button(world._action_choices, "Survey western mineral deposit"),
            "employed quarry miner can explore a second finite deposit"):
        return
    world._on_world_updated(_world("FACTORY", "Factory"))
    if not _check(world._scenery.name == "PublicScene_FACTORY",
            "manufacturing factory has its own physical district"):
        return
    world._avatar.position = world.KIOSK_POSITION + Vector3(0,0,2)
    world._toggle_actions()
    if not _check(_has_button(world._action_choices, "Apply: factory machinist"),
            "factory lets the visitor become a worker rather than only watch NPCs"):
        return
    world._on_industry_updated({
        "ironRemaining": 78, "copperRemaining": 39,
        "oreAtQuarry": 0, "oreAtFactory": 0,
        "factorySteel": 0, "factoryCopper": 0, "factoryParts": 0,
        "workshopSteel": 5, "workshopCopper": 4, "workshopParts": 8,
        "minersAtWork": 1, "factoryWorkersAtWork": 1
    })
    if not _check(world._jobs.text.contains("iron ore 78") and
            world._jobs.text.contains("spare parts 8"),
            "Godot displays the authoritative finite industrial inventories"):
        return

    world._on_world_updated(_world("WORKSHOP", "Workshop"))
    world._avatar.position = world.KIOSK_POSITION + Vector3(0,0,2)
    world._action_panel.visible = true
    world._on_business_updated({
        "registered": false, "yours": false, "ownerIncarnation": 0,
        "machine": false, "cash": 0, "employeeId": null,
        "employeeName": null, "spareParts": 0, "paidHoursToday": 0
    })
    if not _check(_has_button(world._action_choices, "Register / acquire workshop"),
            "the visitor can register a first business at the actual Workshop"):
        return
    world._on_business_updated({
        "registered": true, "yours": true, "ownerIncarnation": 1,
        "machine": true, "machineCondition": 85, "cash": 70,
        "employeeId": null, "employeeName": null, "spareParts": 2,
        "paidHoursToday": 2
    })
    world._on_business_candidates_updated([{
        "id": "resident-0002", "name": "A local mechanic",
        "currentOccupation": "Mechanic"
    }])
    if not _check(_has_button(world._action_choices, "Invest 25 EUR") and
            _has_button(world._action_choices, "Hire A local mechanic") and
            _has_button(world._action_choices, "Service machine") and
            _has_button(world._action_choices, "Withdraw 10 EUR"),
            "owner sees actual investment, local hiring and available profit controls"):
        return
    if not _check(world._jobs.text.contains("bank 70 EUR") and
            world._jobs.text.contains("85/100"),
            "business dashboard exposes real funds and machine wear"):
        return

    print("SceneNavigationSmoke: ALL CHECKS PASSED")
    quit(0)

func _world(place_id: String, label: String) -> Dictionary:
    return {
        "day": 1,
        "minute": 450,
        "season": "SPRING",
        "weather": {
            "sky": "CLEAR", "temperatureC": 15,
            "raining": false, "snowing": false
        },
        "player": {
            "incarnation": 1, "placeId": place_id, "place": label,
            "money": 50, "jobId": null, "health": 100,
            "hunger": 0, "thirst": 0, "fatigue": 0,
            "engineeringSkill": 0, "investigationSkill": 0
        }
    }

func _has_button(parent: Node, prefix: String) -> bool:
    for node in parent.get_children():
        if node is Button and node.text.begins_with(prefix):
            return true
        if _has_button(node,prefix):
            return true
    return false

func _check(ok: bool, label: String) -> bool:
    if not ok:
        push_error("Scene navigation failed: " + label)
        quit(1)
        return false
    return true
