extends Node3D
## Living Alibi vertical slice: move around a local public scene; travel and
## careers are validated by the real Java/Spring server, never by a fake client wallet.
## WASD/arrow keys: walk relative to the camera. Click the world for mouse look.
## First-person only. Escape: release mouse. E: gate. F: local kiosk.

const GATE_POSITION := Vector3(0.0, 0.0, -12.0)
const KIOSK_POSITION := Vector3(7.0, 0.0, 3.0)
const INTERACTION_RADIUS := 3.5
const WALK_SPEED := 4.5
const CYCLE_SPEED := 8.0
const RACK_POSITION := Vector3(6.0,0,-10.0)
const CAR_PARK := Vector3(11.0,0,-10.0)
const CAR_ARRIVAL := Vector3(11.0,0,-7.5)
const TRUCK_PARK := Vector3(14.5,0,-10.0)
const TRUCK_EXIT := Vector3(14.5,0,-7.0)
const FREIGHT_DISTRICTS := ["QUARRY","WORKSHOP","FACTORY"]
const RACK_DISTRICTS := ["PARK","WORKSHOP","NORTH_RESIDENCES"]
const WALKER_RADIUS := 0.32
const COLLISION_STEP := 0.12
const ROAD_EDGE := 24.0
const ROAD_HALF_WIDTH := 2.0
const ROAD_TRIGGER := 23.8
const TownLayout = preload("res://scripts/TownMap.gd")
const MOUSE_SENSITIVITY := 0.0025
const EYE_HEIGHT := 1.65
const ROOM_BOUNDS := Rect2(-13.0,4.0,9.0,10.0)
const ROOM_EXIT := Vector3(-3.0,0,8.5)
const BED_POSITION := Vector3(-11.3,0,6.1)
const KITCHEN_POSITION := Vector3(-5.05,0,12.3)
const BATH_POSITION := Vector3(-12.1,0,12.95)
const LiveClock = preload("res://scripts/LiveClock.gd")
const DriveControls = preload("res://scripts/DriveControls.gd")
const DRIVER_EYE := Vector3(-0.33,1.24,0.08)
const WeatherEffects = preload("res://scripts/WeatherEffects.gd")

# Local presentation only: true movement between districts is Java-authorized.
var _view_yaw := 0.0
var _view_pitch := 0.0

var _avatar: Node3D
var _camera: Camera3D
var _scenery: Node3D
var _chunks: Dictionary = {}
var _residents_root: Node3D
var _obstacles: Array[Rect2] = []
var _park_lamps: Array[OmniLight3D] = []
var _fountain_water: MeshInstance3D
var _sun: DirectionalLight3D
var _ground_material: StandardMaterial3D
var _environment: Environment
var _weather_fx: Node3D
var _api
var _current_place := ""
var _road_links: Dictionary = {}
var _traffic_root: Node3D
var _traffic_edges: Dictionary = {}
var _traffic_seconds := 0.0
var _road_pending := ""
var _road_arrival := Vector3.ZERO
var _road_blocked := ""
var _last_snapshot: Dictionary = {}
var _connected := false
var _clock := LiveClock.new()
var _clock_pending := false
var _clock_selector: OptionButton
var _locations: Array = []
var _job_offers: Array = []
var _work_orders: Array = []
var _business_report: Dictionary = {}
var _business_candidates: Array = []

var _heading: Label
var _status: Label
var _weather: Label
var _needs: Label
var _message: Label
var _hint: Label
var _transport_button: Button
var _parking_button: Button
var _car_label: Label
var _main_panel: PanelContainer
var _drive_hud: Label
var _cockpit: Node3D
var _steering_wheel: Node3D
var _drive := DriveControls.new()
var _drive_target := ""
var _drive_direction := Vector3.ZERO
var _drive_look := 0.0
var _drive_pending := false
var _drive_parked := false
var _drive_freight := false
var _bike_model: Node3D
var _bike_wheel_angle := 0.0
var _jobs: Label
var _bag_label: Label
var _housing_label: Label
var _eat_button: Button
var _water_button: Button
var _map_panel: PanelContainer
var _town_map
var _travel_panel: PanelContainer
var _travel_choices: GridContainer
var _action_panel: PanelContainer
var _dialog_panel: PanelContainer
var _dialog_title: Label
var _dialog_reply: Label
var _dialog_attitude: Label
var _dialog_input: LineEdit
var _dialog_target := ""
var _dialog_pending := false
var _action_title: Label
var _action_choices: VBoxContainer

func _ready() -> void:
    _build_world()
    _create_interface()
    _api = preload("res://examples/WorldApi.gd").new()
    add_child(_api)
    _api.world_updated.connect(_on_world_updated)
    _api.locations_updated.connect(_on_locations_updated)
    _api.job_offers_updated.connect(_on_job_offers_updated)
    _api.work_orders_updated.connect(_on_work_orders_updated)
    _api.industry_updated.connect(_on_industry_updated)
    _api.business_updated.connect(_on_business_updated)
    _api.business_candidates_updated.connect(_on_business_candidates_updated)
    _api.action_feedback.connect(_on_action_feedback)
    _api.request_failed.connect(_on_request_failed)
    _api.request_started.connect(_on_request_started)
    _api.request_finished.connect(_on_request_finished)
    _api.refresh_world()

func _process(delta: float) -> void:
    if _drive_active():
        if not _drive_parked:
            _process_driving(delta)
        else:
            _update_drive_hud()
        _advance_traffic(delta)
        _animate_residents(delta)
        _weather_fx.follow(_avatar.position)
        return
    var direction := Vector3.ZERO
    # Do not move away from an open clickable interaction menu.
    if _road_pending.is_empty() and not _travel_panel.visible and not _action_panel.visible and not _map_panel.visible and not _dialog_panel.visible:
        if Input.is_key_pressed(KEY_W) or Input.is_key_pressed(KEY_UP):
            direction.z -= 1.0
        if Input.is_key_pressed(KEY_S) or Input.is_key_pressed(KEY_DOWN):
            direction.z += 1.0
        if Input.is_key_pressed(KEY_A) or Input.is_key_pressed(KEY_LEFT):
            direction.x -= 1.0
        if Input.is_key_pressed(KEY_D) or Input.is_key_pressed(KEY_RIGHT):
            direction.x += 1.0
    if direction.length_squared() > 0.0:
        # The Java world controls district transitions; this is local preview walking.
        var relative_motion := Basis(Vector3.UP, _view_yaw) * direction.normalized()
        var before := _avatar.position
        _avatar.position = _move_on_ground(_avatar.position,
                relative_motion * delta * _movement_speed(), WALKER_RADIUS, true)
        if _movement_speed() == CYCLE_SPEED:
            _bike_wheel_angle += before.distance_to(_avatar.position)/0.35
        _follow_player()
        _try_road_crossing(relative_motion)

    _sync_bicycle()
    _advance_traffic(delta)
    _animate_residents(delta)
    _weather_fx.follow(_avatar.position)
    _weather_fx.set_sheltered(_inside_guest_room(),_environment)
    _tick_live_clock(delta)
    if not _near_gate():
        _travel_panel.visible = false
    if not _near_kiosk() and not _near_home_station() and not _near_car() and not _near_truck():
        _action_panel.visible = false
    if not _road_pending.is_empty():
        _hint.text = "Waiting for Java to confirm arrival..."
    elif not _road_destination(_avatar.position, ROAD_TRIGGER).is_empty():
        _hint.text = "District crossing · walk forward to travel"
    elif _near_truck():
        _hint.text = "F: assigned freight truck."
    elif _near_car():
        _hint.text = "F: choose a driving route."
    elif _near_rack() and bool(_last_snapshot.get("player",{}).get("ownsBicycle",false)):
        _hint.text = "P: park or retrieve your bicycle · B: ride/walk."
    elif _near_home_station():
        _hint.text = "F: use your bed, kitchen or bathroom."
    elif _current_place == "NORTH_RESIDENCES" and _avatar.position.distance_to(ROOM_EXIT) < 2.3:
        _hint.text = "Your room is open." if _has_room_lease() else "Rent a room at reception (blue kiosk)."
    elif not _nearest_resident().is_empty():
        _hint.text = "T: talk to nearby resident · Esc: cursor."
    elif _near_gate():
        _hint.text = "E: travel gate (game time). Esc: cursor."
    elif _near_kiosk():
        _hint.text = "F: local interactions. Esc: cursor."
    else:
        _hint.text = "WASD: walk · click scene: mouse look · Esc: cursor."

func _unhandled_input(event: InputEvent) -> void:
    if event is InputEventMouseMotion and Input.mouse_mode == Input.MOUSE_MODE_CAPTURED:
        if _drive_active():
            _drive_look = clampf(_drive_look-event.relative.x*MOUSE_SENSITIVITY,-1.05,1.05)
            _view_pitch = clampf(_view_pitch-event.relative.y*MOUSE_SENSITIVITY,-0.45,0.45)
        else:
            _view_yaw -= event.relative.x * MOUSE_SENSITIVITY
            _view_pitch = clampf(_view_pitch - event.relative.y * MOUSE_SENSITIVITY, -1.15, 1.15)
        _follow_player()
    elif event is InputEventMouseButton and event.button_index == MOUSE_BUTTON_LEFT and event.pressed:
        # UI consumes its own clicks before they reach this handler.
        if not _travel_panel.visible and not _action_panel.visible and not _map_panel.visible and not _dialog_panel.visible:
            Input.set_mouse_mode(Input.MOUSE_MODE_CAPTURED)

func _unhandled_key_input(event: InputEvent) -> void:
    if event is InputEventKey and event.pressed and not event.echo:
        if _drive_active():
            if _drive_parked:
                if event.keycode in [KEY_F,KEY_ESCAPE]:
                    _end_driving(true)
                    _message.text = "Car parked. You are on foot."
                elif event.keycode in [KEY_1,KEY_2,KEY_3]:
                    var choices: Array = _last_snapshot.get("player",{}).get("carDestinations",[])
                    var index: int = event.keycode-KEY_1
                    if index<choices.size() and _connected and not _api.is_busy():
                        _begin_driving(str(choices[index]))
            elif event.keycode == KEY_ESCAPE and not _drive_pending:
                _end_driving(true)
                _message.text = "Trip cancelled; car remains parked."
            get_viewport().set_input_as_handled()
            return
        if event.keycode == KEY_ESCAPE:
            Input.set_mouse_mode(Input.MOUSE_MODE_VISIBLE)
            _travel_panel.visible = false
            _action_panel.visible = false
            _map_panel.visible = false
            _close_dialog()
            get_viewport().set_input_as_handled()
        elif event.keycode == KEY_T and not _dialog_panel.visible:
            _open_dialog()
            get_viewport().set_input_as_handled()
        elif event.keycode == KEY_M:
            _toggle_map()
            get_viewport().set_input_as_handled()
        elif event.keycode == KEY_P and not _action_panel.visible and not _travel_panel.visible and not _map_panel.visible and not _dialog_panel.visible:
            _toggle_parking()
            get_viewport().set_input_as_handled()
        elif event.keycode == KEY_B and not _action_panel.visible and not _travel_panel.visible and not _map_panel.visible and not _dialog_panel.visible:
            _toggle_transport()
            get_viewport().set_input_as_handled()
        elif event.keycode == KEY_E and _near_gate() and not _map_panel.visible:
            _toggle_travel()
            get_viewport().set_input_as_handled()
        elif event.keycode == KEY_F and (_near_kiosk() or _near_home_station() or _near_car() or _near_truck()) and not _map_panel.visible:
            _toggle_actions()
            get_viewport().set_input_as_handled()

func _near_gate() -> bool:
    return _avatar.position.distance_to(GATE_POSITION) < INTERACTION_RADIUS

func _near_kiosk() -> bool:
    return _avatar.position.distance_to(KIOSK_POSITION) < INTERACTION_RADIUS

func _has_room_lease() -> bool:
    return bool(_last_snapshot.get("player",{}).get("roomActive",false))

func _inside_guest_room() -> bool:
    return _current_place == "NORTH_RESIDENCES" and ROOM_BOUNDS.has_point(
            Vector2(_avatar.position.x,_avatar.position.z))

func _near_room_bed() -> bool:
    return _inside_guest_room() and _has_room_lease() and _avatar.position.distance_to(BED_POSITION) < 2.5

func _near_kitchen() -> bool:
    return _inside_guest_room() and _has_room_lease() and _avatar.position.x > -7.9 and _avatar.position.distance_to(KITCHEN_POSITION)<2.5

func _near_bath() -> bool:
    return _inside_guest_room() and _has_room_lease() and _avatar.position.z > 11.0 and _avatar.position.x < -8.0 and _avatar.position.distance_to(BATH_POSITION)<2.5

func _near_home_station() -> bool:
    return _near_room_bed() or _near_kitchen() or _near_bath()

func _sync_guest_room() -> void:
    if _chunks.has("NORTH_RESIDENCES"):
        var root: Node3D = _chunks["NORTH_RESIDENCES"]["root"]
        var door := root.get_node_or_null("CasaOspiti/LeaseDoor") as Node3D
        if door != null:
            door.visible = not _has_room_lease()
    if _inside_guest_room() and not _has_room_lease():
        _avatar.position = ROOM_EXIT
        _action_panel.visible = false
        _follow_player()
        _message.text = "Your lease ended. Renew at reception to enter again."

func _cycling() -> bool:
    var p: Dictionary = _last_snapshot.get("player",{})
    return bool(p.get("ownsBicycle",false)) and bool(p.get("cycling",false)) and _parked_district().is_empty()

func _movement_speed() -> float:
    # A portable bicycle is carried at walking speed in the rented studio
    # and grocery interior. No vehicle parking state is simulated yet.
    var at := _avatar.position
    var in_shop := _current_place == "PORTICI" and Rect2(4.4,2.15,6.1,5.7).has_point(Vector2(at.x,at.z))
    return CYCLE_SPEED if _cycling() and not _inside_guest_room() and not in_shop else WALK_SPEED

func _truck_place() -> String:
    var value = _last_snapshot.get("freight",{}).get("truckPlace")
    return "" if value==null else str(value)

func _near_truck() -> bool:
    return _truck_place()==_current_place and _current_place in FREIGHT_DISTRICTS and _avatar.position.distance_to(TRUCK_PARK)<3.5

func _freight_action(action: String) -> void:
    if not _connected or _api.is_busy():
        return
    if (action=="load" and _current_place=="QUARRY" and _near_kiosk()) or (action=="unload" and _near_truck()):
        _api.freight_action(action)

func _drive_truck(destination: String) -> void:
    if _connected and not _api.is_busy():
        _begin_driving(destination,true)

func _car_place() -> String:
    var value = _last_snapshot.get("player",{}).get("carPlace")
    return "" if value==null else str(value)

func _near_car() -> bool:
    return _car_place()==_current_place and _current_place in TownLayout.MOTOR_DISTRICTS and _avatar.position.distance_to(CAR_PARK)<3.3

func _car_action(action: String) -> void:
    if (_current_place=="WORKSHOP" or (_current_place=="FUEL_STATION" and action=="refuel")) and _near_kiosk() and _connected and not _api.is_busy():
        _api.car_action(action)

func _drive_active() -> bool:
    return _drive_parked or not _drive_target.is_empty()

func _drive_to(destination: String) -> void:
    if not _near_car() or not _connected or _api.is_busy():
        return
    _begin_driving(destination)

func _drive_position(point: Vector2) -> Vector3:
    var right := Vector3(-_drive_direction.z,0,_drive_direction.x)
    return _drive_direction*point.x+right*point.y

func _drive_clear(at: Vector3) -> bool:
    # Sample centre and both ends of the vehicle to avoid clipping scenery.
    for offset in [-0.85,0.0,0.85]:
        var sample: Vector3 = at+_drive_direction*float(offset)
        if sample.dot(_drive_direction)>ROAD_EDGE:
            sample -= _drive_direction*(sample.dot(_drive_direction)-ROAD_EDGE)
        if not _can_stand(sample,0.82,true):
            return false
    return true

func _begin_driving(destination: String,freight: bool = false) -> bool:
    var player: Dictionary = _last_snapshot.get("player",{})
    if not TownLayout.motor_road(_current_place,destination):
        return false
    if freight:
        if not _near_truck() or str(_last_snapshot.get("freight",{}).get("nextDestination",""))!=destination:
            return false
    elif not _near_car() or not destination in player.get("carDestinations",[]):
        return false
    if not freight and (int(player.get("carFuel",0))<1 or int(player.get("carCondition",0))<3):
        _message.text = "The car needs fuel or service."
        return false
    if bool(player.get("ownsBicycle",false)) and _parked_district().is_empty():
        _message.text = "Park your bicycle before driving."
        return false
    var candidate := DriveControls.new()
    _drive_direction = (TownLayout.world_center(destination)-TownLayout.world_center(_current_place)).normalized()
    var start := _drive_position(Vector2(candidate.distance,candidate.lateral))
    if not _drive_clear(start):
        _message.text = "Road occupied. Let traffic pass before departing."
        return false
    _drive = candidate
    _drive_freight = freight
    _drive_target = destination
    _drive_parked = false
    _drive_pending = false
    _drive_look = 0.0
    _view_pitch = 0.0
    _view_yaw = atan2(-_drive_direction.x,-_drive_direction.z)
    _avatar.position = start
    _action_panel.visible = false
    _travel_panel.visible = false
    _map_panel.visible = false
    _close_dialog()
    _main_panel.visible = false
    _drive_hud.visible = true
    _cockpit.visible = true
    _sync_owned_car()
    _sync_bicycle()
    Input.set_mouse_mode(Input.MOUSE_MODE_CAPTURED)
    _follow_player()
    _update_drive_hud()
    return true

func _update_drive_hud() -> void:
    var player: Dictionary = _last_snapshot.get("player",{})
    if _drive_freight:
        var cargo: Dictionary = _last_snapshot.get("freight",{})
        _drive_hud.text = "FREIGHT | %s km/h | Iron %s · Copper %s\nTo %s · W/S gas/reverse · A/D steer · Space brake\n%s" % [roundi(absf(_drive.speed)*3.6),cargo.get("iron",0),cargo.get("copper",0),_drive_target.capitalize(),"Awaiting arrival..." if _drive_pending else "Esc: cancel this leg"]
        return
    if _drive_parked:
        var lines := PackedStringArray()
        var choices: Array = player.get("carDestinations",[])
        for i in range(choices.size()):
            lines.append("%s: %s" % [i+1,str(choices[i]).capitalize()])
        _drive_hud.text = "Parked at %s | Fuel %s/12 | Condition %s%%\n%s\nF / Esc: get out · Mouse: look around" % [
            _current_place.capitalize(),player.get("carFuel",0),player.get("carCondition",0),"   ".join(lines)]
        if int(player.get("carFuel",0))<1 or int(player.get("carCondition",0))<3:
            _drive_hud.text += "\nCar needs fuel or service."
        return
    _drive_hud.text = "%s km/h  |  Fuel %s/12  |  Condition %s%%\nTo %s · W gas / S reverse / A D steer / Space brake\n%s" % [
        roundi(absf(_drive.speed)*3.6),player.get("carFuel",0),player.get("carCondition",0),
        _drive_target.capitalize(),"Awaiting confirmed arrival..." if _drive_pending else "Esc: cancel before arrival · Mouse: look around"]

func _process_driving(delta: float) -> void:
    if not _drive_pending:
        var throttle := float(Input.is_key_pressed(KEY_W))-float(Input.is_key_pressed(KEY_S))
        var turn := float(Input.is_key_pressed(KEY_D))-float(Input.is_key_pressed(KEY_A))
        var next := _drive.propose(delta,throttle,turn,Input.is_key_pressed(KEY_SPACE),_traffic_phase(_traffic_seconds))
        var at := _drive_position(next)
        if _drive_clear(at):
            _drive.accept(next)
            _avatar.position = at
        else:
            _drive.blocked()
        if _drive.distance>=DriveControls.FINISH-0.001 and _connected and not _api.is_busy():
            _drive_pending = true
            _drive.speed = 0.0
            if _drive_freight:
                _api.freight_action("drive",_drive_target)
            else:
                _api.drive_to(_drive_target)
    _follow_player()
    _update_drive_hud()

func _end_driving(return_to_car: bool) -> void:
    var was_freight := _drive_freight
    _drive_freight = false
    _drive_parked = false
    _drive_target = ""
    _drive_pending = false
    _drive.speed = 0.0
    _drive_look = 0.0
    _view_pitch = 0.0
    _cockpit.visible = false
    _drive_hud.visible = false
    _main_panel.visible = true
    if return_to_car:
        _avatar.position = TRUCK_EXIT if was_freight else CAR_ARRIVAL
    _sync_owned_car()
    _follow_player()
    Input.set_mouse_mode(Input.MOUSE_MODE_VISIBLE)

func _park_in_cabin() -> void:
    _drive_target = ""
    _drive_pending = false
    _drive_parked = true
    _drive = DriveControls.new()
    _drive_look = 0.0
    _view_pitch = 0.0
    _view_yaw = 0.0
    _avatar.position = CAR_PARK
    _cockpit.visible = true
    _drive_hud.visible = true
    _main_panel.visible = false
    _sync_owned_car()
    _follow_player()
    _update_drive_hud()

func _sync_owned_car() -> void:
    for id in TownLayout.MOTOR_DISTRICTS:
        if not _chunks.has(id):
            continue
        var root: Node3D = _chunks[id]["root"]
        var car := root.get_node_or_null("PublicCarPark/OwnedCar") as Node3D
        if car!=null:
            car.visible = _car_place()==id and (not _drive_active() or _drive_freight or id!=_current_place)
        var truck := root.get_node_or_null("FreightParking/AssignedTruck") as Node3D
        if truck!=null:
            truck.visible = _truck_place()==id and (not _drive_freight or not _drive_active())

func _parked_district() -> String:
    var value = _last_snapshot.get("player",{}).get("bicycleParked")
    return "" if value == null else str(value)

func _near_rack() -> bool:
    return _current_place in RACK_DISTRICTS and _avatar.position.distance_to(RACK_POSITION)<2.8

func _toggle_parking() -> void:
    if not _connected or _api.is_busy() or not _near_rack():
        return
    if not bool(_last_snapshot.get("player",{}).get("ownsBicycle",false)):
        return
    if _parked_district().is_empty():
        _api.transport_mode("park")
    elif _parked_district() == _current_place:
        _api.transport_mode("retrieve")
    else:
        _message.text = "Your bicycle is parked at "+_parked_district().capitalize()

func _sync_bicycle() -> void:
    if not is_instance_valid(_bike_model):
        return
    var riding := not _drive_active() and _movement_speed() == CYCLE_SPEED
    _bike_model.visible = riding
    _bike_model.position = _avatar.position
    _bike_model.rotation.y = _view_yaw
    for wheel in _bike_model.get_children():
        if wheel.has_meta("wheel"):
            wheel.rotation.x = _bike_wheel_angle
    for id in RACK_DISTRICTS:
        if not _chunks.has(id):
            continue
        var root: Node3D = _chunks[id]["root"]
        var parked := root.get_node_or_null("BicycleRack/ParkedBicycle") as Node3D
        if parked != null:
            parked.visible = _parked_district() == id

func _toggle_transport() -> void:
    if _connected and not _api.is_busy() and _parked_district().is_empty() and bool(_last_snapshot.get("player",{}).get("ownsBicycle",false)):
        _api.transport_mode("walk" if _cycling() else "cycle")

func _buy_bicycle() -> void:
    if _current_place == "WORKSHOP" and _near_kiosk() and _connected and not _api.is_busy():
        _api.buy_bicycle()

func _follow_player() -> void:
    # Fixed first-person presentation in every district and transport mode.
    _avatar.visible = false
    _avatar.rotation.y = _view_yaw
    if _drive_active():
        _cockpit.position = _avatar.position
        _cockpit.rotation.y = _view_yaw
        _steering_wheel.rotation.z = -_drive.steering*0.7
        _camera.position = _avatar.position+Basis(Vector3.UP,_view_yaw)*DRIVER_EYE
        _camera.rotation = Vector3(_view_pitch,_view_yaw+_drive_look,0.0)
    else:
        _camera.position = _avatar.position + Vector3(0.0,EYE_HEIGHT,0.0)
        _camera.rotation = Vector3(_view_pitch,_view_yaw,0.0)

func _build_world() -> void:
    _environment = Environment.new()
    _environment.background_mode = Environment.BG_COLOR
    _environment.background_color = Color(0.63, 0.80, 0.96)
    _environment.ambient_light_source = Environment.AMBIENT_SOURCE_COLOR
    _environment.ambient_light_color = Color(0.80, 0.86, 0.93)
    var lighting := WorldEnvironment.new()
    lighting.environment = _environment
    add_child(lighting)

    _sun = DirectionalLight3D.new()
    _sun.rotation_degrees = Vector3(-55.0, -25.0, 0.0)
    _sun.light_energy = 1.1
    _sun.shadow_enabled = true
    add_child(_sun)

    _avatar = Node3D.new()
    _avatar.name = "OrdinaryVisitor"
    # Position-only anchor. No third-person player mesh is needed.
    add_child(_avatar)

    _traffic_root = Node3D.new()
    _traffic_root.name = "MotorTraffic"
    add_child(_traffic_root)
    _bike_model = _make_bicycle(self,"PlayerBicycle",Color("#496e50"))
    _bike_model.visible = false
    _weather_fx = WeatherEffects.new()
    add_child(_weather_fx)
    _weather_fx.follow(_avatar.position)
    _build_cockpit()
    _camera = Camera3D.new()
    _camera.current = true
    add_child(_camera)
    _follow_player()
    _render_place("PARK")
    _current_place = "PARK"

func _build_cockpit() -> void:
    _cockpit = Node3D.new()
    _cockpit.name = "DriverCabin"
    add_child(_cockpit)
    var dark := Color("#303538")
    var trim := Color("#50575a")
    _box(_cockpit,Vector3(1.65,0.28,0.45),dark,Vector3(0,0.76,-0.55))
    _box(_cockpit,Vector3(1.72,0.15,0.9),Color("#496e50"),Vector3(0,0.55,-1.2))
    _box(_cockpit,Vector3(1.72,0.10,1.7),trim,Vector3(0,1.57,0.15))
    for x in [-0.79,0.79]:
        _box(_cockpit,Vector3(0.12,0.8,0.13),trim,Vector3(x,1.20,-0.66))
        _box(_cockpit,Vector3(0.14,0.46,1.2),dark,Vector3(x,0.80,0.02))
        _box(_cockpit,Vector3(0.23,0.15,0.19),dark,Vector3(x*1.14,1.09,-0.48))
        _box(_cockpit,Vector3(0.19,0.11,0.03),Color("#a0b7c1"),Vector3(x*1.14,1.09,-0.37))
    _box(_cockpit,Vector3(0.44,0.18,0.10),Color("#20282a"),Vector3(-0.33,0.93,-0.57))
    _box(_cockpit,Vector3(0.28,0.095,0.045),Color("#99ad95"),Vector3(-0.33,0.95,-0.51))
    _steering_wheel = Node3D.new()
    _steering_wheel.position = Vector3(-0.33,0.94,-0.31)
    _cockpit.add_child(_steering_wheel)
    var ring := TorusMesh.new()
    ring.inner_radius = 0.145
    ring.outer_radius = 0.18
    var wheel := MeshInstance3D.new()
    wheel.mesh = ring
    var material := StandardMaterial3D.new()
    material.albedo_color = dark
    wheel.material_override = material
    wheel.rotation_degrees.x = 90
    _steering_wheel.add_child(wheel)
    _box(_steering_wheel,Vector3(0.30,0.035,0.045),dark,Vector3.ZERO)
    _box(_steering_wheel,Vector3(0.035,0.15,0.045),dark,Vector3(0,-0.06,0))
    _box(_steering_wheel,Vector3(0.09,0.08,0.06),trim,Vector3.ZERO)
    _cockpit.visible = false

func _render_place(place_id: String) -> void:
    # Keep the current and nearby chunks. Positions are rebased around the
    # active district; the Java simulation remains district-authoritative.
    if is_instance_valid(_residents_root):
        for resident in _residents_root.get_children():
            _residents_root.remove_child(resident)
            resident.queue_free()
    var wanted: Dictionary = {}
    var origin := TownLayout.world_center(place_id)
    for id in TownLayout.DISTRICTS:
        var offset := TownLayout.world_center(str(id)) - origin
        if absf(offset.x) <= TownLayout.TILE_SPACING and absf(offset.z) <= TownLayout.TILE_SPACING:
            wanted[id] = offset
    # Keep even an unexpected public place renderable, rather than failing
    # to show a valid server snapshot.
    wanted[place_id] = Vector3.ZERO
    for id in _chunks.keys():
        if not wanted.has(id):
            var old_root: Node3D = _chunks[id]["root"]
            remove_child(old_root)
            old_root.queue_free()
            _chunks.erase(id)
    for id in wanted:
        if not _chunks.has(id):
            _generate_chunk(str(id))
        var chunk_root: Node3D = _chunks[id]["root"]
        chunk_root.position = wanted[id]
    _activate_chunk(place_id)
    _sync_traffic_edges(place_id)
    _apply_weather()

func _activate_chunk(place_id: String) -> void:
    var chunk: Dictionary = _chunks[place_id]
    _scenery = chunk["root"]
    _residents_root = chunk["residents"]
    _ground_material = chunk["ground"]
    _obstacles = chunk["obstacles"]
    _road_links = chunk["roads"]
    _park_lamps = chunk["lamps"]
    _fountain_water = chunk["water"]

func _generate_chunk(place_id: String) -> void:
    # Allocate fresh per-chunk state: never clear arrays owned by a cached
    # neighbour when creating another district.
    _obstacles = []
    _park_lamps = []
    _road_links = {}
    _fountain_water = null
    _scenery = Node3D.new()
    _scenery.name = "PublicScene_" + place_id
    add_child(_scenery)

    _ground_material = StandardMaterial3D.new()
    _ground_material.albedo_color = Color(0.27, 0.49, 0.26)
    var ground := BoxMesh.new()
    ground.size = Vector3(48.0, 0.3, 48.0)
    var floor := MeshInstance3D.new()
    floor.mesh = ground
    floor.material_override = _ground_material
    floor.position.y = -0.2
    _scenery.add_child(floor)

    _box(_scenery, Vector3(3.0, 0.05, 35.0), Color(0.63, 0.57, 0.45),
            Vector3(0.0, -0.015, 0.0))
    _create_gate_and_kiosk()
    _residents_root = Node3D.new()
    _residents_root.name = "VisibleResidents"
    _scenery.add_child(_residents_root)

    match place_id:
        "FUEL_STATION":
            _build_fuel_station()
        "FARM":
            _build_farm()
        "NORTH_RESIDENCES":
            _build_guesthouse()
        "PORTICI":
            _build_portici()
        "CENTRO":
            _build_centro()
        "PARK":
            _build_park()
        "MARKET":
            _building("Market stalls", Color(0.68, 0.39, 0.22))
            _box(_scenery, Vector3(6.0, 0.18, 3.0), Color(0.83, 0.20, 0.18),
                    Vector3(-7.0, 2.35, 3.0))
            _box(_scenery, Vector3(6.0, 0.18, 3.0), Color(0.90, 0.76, 0.31),
                    Vector3(4.0, 2.35, 8.0))
        "SCHOOL":
            _building("Public school", Color(0.79, 0.68, 0.43))
            _box(_scenery, Vector3(4.0, 1.3, 0.2), Color(0.14, 0.36, 0.38),
                    Vector3(-7.0, 1.5, 5.0))
        "LIBRARY":
            _building("Public library", Color(0.59, 0.48, 0.33))
            for offset in [-10.0, -7.0, -4.0]:
                _box(_scenery, Vector3(0.55, 2.0, 2.0), Color(0.40, 0.28, 0.21),
                        Vector3(offset, 1.1, 5.0))
        "WORKSHOP":
            _building("Mechanical workshop", Color(0.41, 0.51, 0.59))
            _build_motor_court()
            _box(_scenery, Vector3(4.0, 1.2, 2.0), Color(0.27, 0.30, 0.32),
                    Vector3(-8.0, 0.7, 7.0))
        "OFFICE":
            _building("Offices", Color(0.40, 0.55, 0.72))
            for offset in [-10.0, -7.0, -4.0]:
                _box(_scenery, Vector3(1.6, 1.8, 0.12), Color(0.61, 0.83, 0.94),
                        Vector3(offset, 2.3, 4.9))
        "QUARRY":
            _add_obstacle(Vector3(-8.0, 0.0, 9.0), Vector2(11.0, 3.0))
            for x in [-11.0, -8.0, -5.0]:
                _add_obstacle(Vector3(x, 0.0, 3.0), Vector2(1.8, 2.0))
            # Placeholder rock face and ore heaps; the finite seam is Java-owned.
            _box(_scenery, Vector3(11.0, 7.0, 3.0), Color(0.42, 0.43, 0.46),
                    Vector3(-8.0, 3.4, 9.0))
            for offset in [-11.0, -8.0, -5.0]:
                _box(_scenery, Vector3(1.8, 1.1, 2.0), Color(0.57, 0.38, 0.28),
                        Vector3(offset, 0.55, 3.0))
        "FACTORY":
            for x in [-8.0, -5.0, -2.0]:
                _add_obstacle(Vector3(x, 0.0, 2.2), Vector2(2.0, 1.6))
            _building("Parts factory", Color(0.50, 0.53, 0.56))
            _box(_scenery, Vector3(1.5, 8.0, 1.5), Color(0.36, 0.37, 0.40),
                    Vector3(-10.0, 8.0, 7.0))
            for offset in [-8.0, -5.0, -2.0]:
                _box(_scenery, Vector3(2.0, 1.0, 1.6), Color(0.32, 0.38, 0.42),
                        Vector3(offset, 0.5, 2.2))
        "RIVER":
            # A bridge keeps the east-west public road dry.
            for z in [-10.0, 10.0]:
                _add_obstacle(Vector3(-11.0, 0.0, z), Vector2(10.0, 15.0))
            _box(_scenery, Vector3(10.0, 0.11, 35.0), Color(0.12, 0.40, 0.67),
                    Vector3(-11.0, 0.02, 0.0))
            _box(_scenery, Vector3(10.5, 0.12, 4.8), Color(0.45,0.34,0.23),
                    Vector3(-11.0, 0.10, 0.0))
            _tree(Vector3(12.0, 0.0, 7.0))
        "WOODS":
            for p in [
                Vector2(-11.0, -9.0), Vector2(-10.0, 7.0),
                Vector2(10.0, -10.0), Vector2(11.0, 9.0),
                Vector2(-15.0, 14.0), Vector2(14.0, 14.0)
            ]:
                _tree(Vector3(p.x, 0.0, p.y))
        _:
            _building("Neighborhood", Color(0.57, 0.61, 0.57))
            _tree(Vector3(11.0, 0.0, 7.0))

    if place_id in RACK_DISTRICTS:
        _build_bicycle_rack()
    if place_id in TownLayout.MOTOR_DISTRICTS:
        _build_public_car_park()
    if place_id in FREIGHT_DISTRICTS:
        _build_freight_parking()
    _build_road_links(place_id)
    _chunks[place_id] = {
        "root": _scenery, "residents": _residents_root,
        "ground": _ground_material, "obstacles": _obstacles,
        "roads": _road_links, "lamps": _park_lamps, "water": _fountain_water
    }

func _build_road_links(place_id: String) -> void:
    _road_links.clear()
    var roads := Node3D.new()
    roads.name = "DistrictRoads"
    _scenery.add_child(roads)
    for id in TownLayout.DISTRICTS:
        if not TownLayout.adjacent(place_id, str(id)):
            continue
        var direction := (TownLayout.world_center(str(id)) -
                TownLayout.world_center(place_id)).normalized()
        _road_links[direction] = str(id)
        var vertical := absf(direction.z) > 0.5
        var motor := TownLayout.motor_road(place_id,str(id))
        var width := 6.4 if motor else 4.0
        var dimensions := Vector3(width,0.05,15.0) if vertical else Vector3(15.0,0.05,width)
        _box(roads, dimensions, Color("#42484b") if motor else Color(0.49,0.47,0.42),
                direction * 16.5 + Vector3(0,0.025,0))
        if motor:
            _mark_motor_road(roads,direction)
        _sign(roads, str(TownLayout.LABELS.get(id,str(id).capitalize())), direction * 19.0 + Vector3(0,2.8,0))

func _mark_motor_road(parent: Node3D,direction: Vector3) -> void:
    var lane := Node3D.new()
    lane.name = "MotorLane"
    lane.rotation.y = atan2(direction.x,direction.z)
    parent.add_child(lane)
    var white := Color("#ddd9c7")
    # Standardise on z-forward in lane-local coordinates. Geometry is
    # symmetric on both sides of a chunk seam.
    for x in [-3.0,3.0]:
        _box(lane,Vector3(0.10,0.018,14.9),white,Vector3(x,0.06,16.5))
    for z in [13.5,16.5,19.5,22.5]:
        _box(lane,Vector3(0.10,0.018,1.25),white,Vector3(0,0.06,z))
    for i in range(9):
        _box(lane,Vector3(0.34,0.022,1.5),white,Vector3(-2.6+float(i)*0.65,0.065,18.0))
    # Small roadside crossing markers, outside the road corridor.
    _box(lane,Vector3(2.9,0.022,0.15),white,Vector3(-1.5,0.065,16.7))
    _box(lane,Vector3(2.9,0.022,0.15),white,Vector3(1.5,0.065,19.3))
    _build_traffic_signal(lane)
    for x in [-3.7,3.7]:
        _box(lane,Vector3(0.12,1.8,0.12),Color("#686c70"),Vector3(x,0.9,18.0))
        _box(lane,Vector3(0.45,0.45,0.12),Color("#416d9c"),Vector3(x,1.8,18.0))
        _box(lane,Vector3(0.25,0.25,0.14),white,Vector3(x,1.8,11.0))

func _build_traffic_signal(lane: Node3D) -> void:
    var signal_root := Node3D.new()
    signal_root.name = "TrafficSignal"
    signal_root.position = Vector3(-3.9,0,16.7)
    lane.add_child(signal_root)
    _box(signal_root,Vector3(0.12,2.6,0.12),Color("#62686a"),Vector3(0,1.3,0))
    _box(signal_root,Vector3(0.45,1.04,0.34),Color("#303639"),Vector3(0,2.8,0))
    var colors := [Color("#f74c43"),Color("#ffbd3c"),Color("#51d879")]
    for i in range(3):
        var dark: Color = colors[i].darkened(0.80)
        var at := Vector3(0,3.12-float(i)*0.32,0)
        _sphere(signal_root,0.115,dark,at+Vector3(0,0,-0.18))
        _sphere(signal_root,0.115,dark,at+Vector3(0,0,0.18))
        var lamp := Node3D.new()
        lamp.name = "Lamp"+str(i)
        signal_root.add_child(lamp)
        for face in [-1.0,1.0]:
            var mesh := SphereMesh.new()
            mesh.radius = 0.121
            mesh.height = 0.242
            var bulb := MeshInstance3D.new()
            bulb.mesh = mesh
            var material := StandardMaterial3D.new()
            material.albedo_color = colors[i]
            material.shading_mode = BaseMaterial3D.SHADING_MODE_UNSHADED
            bulb.material_override = material
            bulb.position = at+Vector3(0,0,face*0.19)
            lamp.add_child(bulb)
        lamp.visible = false

func _traffic_phase(seconds: float) -> int:
    # Local visual traffic clock, not Java world time. Both ends of each
    # link share a phase. -1 represents failed street electricity.
    if not bool(_last_snapshot.get("services",{}).get("lightsWorking",true)):
        return -1
    var phase := fposmod(seconds,24.0)
    if phase<10.0:
        return 2 # green
    if phase<13.0:
        return 1 # amber
    return 0 # red / pedestrian interval

func _sync_traffic_edges(origin_id: String) -> void:
    var wanted: Dictionary = {}
    for first in _chunks:
        for second in _chunks:
            if str(first)>=str(second) or not TownLayout.motor_road(str(first),str(second)):
                continue
            var key := str(first)+":"+str(second)
            wanted[key] = true
            if not _traffic_edges.has(key):
                var cars: Array[Dictionary] = []
                for i in range(2):
                    var car := _make_city_car(_traffic_root,Color("#7894aa") if i==0 else Color("#ad7965"))
                    car.visible = false
                    cars.append({"node":car,"distance":14.2,"delay":float(i)*3.0})
                _traffic_edges[key] = {"first":str(first),"second":str(second),"cars":cars}
    for key in _traffic_edges.keys():
        if not wanted.has(key):
            for record in _traffic_edges[key]["cars"]:
                var car: Node3D = record["node"]
                car.visible = false
                car.queue_free()
            _traffic_edges.erase(key)
    for edge in _traffic_edges.values():
        _position_traffic_edge(edge,origin_id)

func _position_traffic_edge(edge: Dictionary,origin_id: String) -> void:
    var origin := TownLayout.world_center(origin_id)
    var first := TownLayout.world_center(edge["first"])-origin
    var second := TownLayout.world_center(edge["second"])-origin
    for i in range(2):
        var record: Dictionary = edge["cars"][i]
        var start := first if i==0 else second
        var direction := (second-first).normalized() if i==0 else (first-second).normalized()
        var right := Vector3(-direction.z,0,direction.x)
        var car: Node3D = record["node"]
        car.position = start+direction*float(record["distance"])+right*1.6
        car.rotation.y = atan2(-direction.x,-direction.z)

func _car_footprint(car: Node3D) -> Rect2:
    var dimensions := Vector2(1.88,3.62) if absf(cos(car.rotation.y))>0.5 else Vector2(3.62,1.88)
    return Rect2(Vector2(car.position.x,car.position.z)-dimensions*0.5,dimensions)

func _traffic_people() -> Array[Vector3]:
    var people: Array[Vector3] = [_avatar.position]
    if is_instance_valid(_residents_root):
        for person in _residents_root.get_children():
            if person is Node3D:
                people.append(person.position)
    return people

func _advance_traffic(delta: float) -> void:
    if not is_instance_valid(_traffic_root):
        return
    var dt := clampf(delta,0.0,0.1)
    _traffic_seconds += dt
    var phase := _traffic_phase(_traffic_seconds)
    var origin := TownLayout.world_center(_current_place)
    var people := _traffic_people()
    for edge in _traffic_edges.values():
        var first := TownLayout.world_center(edge["first"])-origin
        var second := TownLayout.world_center(edge["second"])-origin
        for i in range(2):
            var record: Dictionary = edge["cars"][i]
            var car: Node3D = record["node"]
            var start := first if i==0 else second
            var direction := (second-first).normalized() if i==0 else (first-second).normalized()
            var right := Vector3(-direction.z,0,direction.x)
            var distance := float(record["distance"])
            var blocked := false
            # Yield to pedestrians anywhere immediately ahead, including
            # a pedestrian already on either crossing.
            for person in people:
                var relative := person-(start+direction*distance+right*1.6)
                var ahead := relative.dot(direction)
                if ahead>=-2.2 and ahead<=4.3 and absf(relative.dot(right))<1.45:
                    blocked = true
                    break
            if float(record["delay"])>0.0:
                record["delay"] = maxf(0.0,float(record["delay"])-dt)
                car.visible = false
                continue
            if not car.visible and (blocked or (not _drive_target.is_empty() and direction.dot(_drive_direction)>0.9 and start.distance_to(Vector3.ZERO)<0.1 and (edge["second"]==_drive_target or edge["first"]==_drive_target))):
                continue
            car.visible = true
            if blocked:
                continue
            var next := distance+dt*(2.0 if phase==-1 else 4.0)
            if phase in [0,1]:
                for stop in [15.0,27.0]:
                    if distance<=stop and next>stop:
                        next = stop
                        break
            if next>=33.8:
                record["distance"] = 14.2
                record["delay"] = 4.0
                car.visible = false
            else:
                record["distance"] = next
        _position_traffic_edge(edge,_current_place)
    for chunk in _chunks.values():
        var root: Node3D = chunk["root"]
        var roads := root.get_node_or_null("DistrictRoads")
        if roads==null:
            continue
        for lane in roads.get_children():
            var signal_root := lane.get_node_or_null("TrafficSignal")
            if signal_root==null:
                continue
            for i in range(3):
                signal_root.get_node("Lamp"+str(i)).visible = phase==i

func _road_destination(at: Vector3, minimum: float) -> String:
    for key in _road_links:
        var direction: Vector3 = key
        var along := at.dot(direction)
        var lateral := (at - direction * along).length()
        if along >= minimum and along <= ROAD_EDGE + 0.001 and lateral <= ROAD_HALF_WIDTH:
            return str(_road_links[key])
    return ""

func _try_road_crossing(motion: Vector3) -> void:
    var destination := _road_destination(_avatar.position, ROAD_TRIGGER)
    if destination.is_empty():
        _road_blocked = ""
        return
    var direction := (TownLayout.world_center(destination) -
            TownLayout.world_center(_current_place)).normalized()
    if motion.dot(direction) <= 0.0 or destination == _road_blocked:
        return
    if not _connected or _api.is_busy() or not _road_pending.is_empty():
        return
    _road_pending = destination
    var lateral := _avatar.position - direction * _avatar.position.dot(direction)
    _road_arrival = lateral - direction * 23.0
    _api.go_to(destination)

# A flat, client-side collision layer. The Java server owns district travel
# and never trusts these local visual positions as proof of an action.
func _add_obstacle(center: Vector3, size: Vector2) -> void:
    _obstacles.append(Rect2(Vector2(center.x, center.z) - size * 0.5, size))

func _can_stand(at: Vector3, radius: float, allow_roads: bool = false) -> bool:
    if absf(at.x) > 17.0 or absf(at.z) > 17.0:
        if not allow_roads or _road_destination(at, 17.0).is_empty():
            return false
    var p := Vector2(at.x, at.z)
    if _current_place == "NORTH_RESIDENCES" and not _has_room_lease() and ROOM_BOUNDS.grow(radius).has_point(p):
        return false
    for obstacle in _obstacles:
        var nearest := p.clamp(obstacle.position, obstacle.end)
        if p.distance_squared_to(nearest) < radius * radius:
            return false
    if _truck_place()==_current_place and _current_place in FREIGHT_DISTRICTS:
        if Rect2(Vector2(TRUCK_PARK.x-1.1,TRUCK_PARK.z-2.4),Vector2(2.2,4.8)).grow(radius).has_point(p):
            return false
    if _car_place()==_current_place and _current_place in TownLayout.MOTOR_DISTRICTS:
        if Rect2(Vector2(CAR_PARK.x-0.94,CAR_PARK.z-1.81),Vector2(1.88,3.62)).grow(radius).has_point(p):
            return false
    for edge in _traffic_edges.values():
        for record in edge["cars"]:
            var car: Node3D = record["node"]
            if car.visible and _car_footprint(car).grow(radius).has_point(p):
                return false
    return true

func _move_on_ground(from: Vector3, displacement: Vector3, radius: float,
        allow_roads: bool = false) -> Vector3:
    # Substeps prevent tunnelling even when a frame takes longer than usual.
    # Independent axes permit sliding along walls instead of sticking to them.
    var count := maxi(1, ceili(Vector2(displacement.x, displacement.z).length() / COLLISION_STEP))
    var step := Vector3(displacement.x, 0.0, displacement.z) / float(count)
    var at := from
    for i in range(count):
        var candidate := at + step
        if _can_stand(candidate, radius, allow_roads):
            at = candidate
        else:
            candidate = at + Vector3(step.x, 0.0, 0.0)
            if _can_stand(candidate, radius, allow_roads):
                at = candidate
            candidate = at + Vector3(0.0, 0.0, step.z)
            if _can_stand(candidate, radius, allow_roads):
                at = candidate
    return at

func _open_anchor(preferred: Vector3) -> Vector3:
    if _can_stand(preferred, WALKER_RADIUS):
        return preferred
    for distance in range(1, 7):
        for side in [Vector3.RIGHT, Vector3.LEFT, Vector3.FORWARD, Vector3.BACK]:
            var candidate: Vector3 = preferred + side * float(distance)
            if _can_stand(candidate, WALKER_RADIUS):
                return candidate
    return Vector3.ZERO

# Piazza Centrale: fictional Lombard piazza, reusable portico modules.
# The four axial lanes stay clear for the district road connections.
func _room_wall(parent: Node3D,at: Vector3,footprint: Vector2,color: Color) -> void:
    _add_obstacle(at,footprint)
    _box(parent,Vector3(footprint.x,3.0,footprint.y),color,at+Vector3(0,1.5,0))

func _room_furniture(parent: Node3D,at: Vector3,size: Vector3,color: Color) -> void:
    _add_obstacle(at,Vector2(size.x,size.z))
    _box(parent,size,color,at+Vector3(0,size.y*0.5,0))

func _bike_tube(parent: Node3D,a: Vector3,b: Vector3,color: Color,radius: float = 0.025) -> void:
    var tube := _cylinder(parent,radius,a.distance_to(b),color,(a+b)*0.5)
    tube.quaternion = Quaternion(Vector3.UP,(b-a).normalized())

func _make_bicycle(parent: Node3D,caption: String,paint: Color) -> Node3D:
    var bike := Node3D.new()
    bike.name = caption
    parent.add_child(bike)
    var metal := Color("#969a97")
    var tire := Color("#242625")
    for z in [-0.62,0.62]:
        var wheel := Node3D.new()
        wheel.position = Vector3(0,0.35,z)
        wheel.set_meta("wheel",true)
        bike.add_child(wheel)
        var torus := TorusMesh.new()
        torus.inner_radius = 0.31
        torus.outer_radius = 0.37
        var ring := MeshInstance3D.new()
        ring.mesh = torus
        var material := StandardMaterial3D.new()
        material.albedo_color = tire
        ring.material_override = material
        ring.rotation_degrees.z = 90
        wheel.add_child(ring)
        for i in range(6):
            var angle := float(i)*PI/6.0
            var end := Vector3(0,sin(angle)*0.31,cos(angle)*0.31)
            _bike_tube(wheel,-end,end,metal,0.008)
    var rear := Vector3(0,0.35,0.62)
    var crank := Vector3(0,0.38,0.05)
    var saddle := Vector3(0,0.88,0.20)
    var head := Vector3(0,0.90,-0.44)
    var front := Vector3(0,0.35,-0.62)
    for endpoints in [[rear,crank],[rear,saddle],[crank,saddle],[saddle,head],[crank,head]]:
        _bike_tube(bike,endpoints[0],endpoints[1],paint,0.035)
    _bike_tube(bike,head,front,metal,0.035)
    _bike_tube(bike,head,Vector3(0,1.12,-0.52),metal)
    _bike_tube(bike,Vector3(-0.29,1.12,-0.52),Vector3(0.29,1.12,-0.52),metal)
    _box(bike,Vector3(0.22,0.075,0.30),tire,saddle+Vector3(0,0.06,0))
    _box(bike,Vector3(0.35,0.06,0.42),metal,Vector3(0,0.78,0.60))
    _box(bike,Vector3(0.1,0.1,0.12),Color("#efe1a1"),Vector3(0,0.99,-0.58))
    _box(bike,Vector3(0.1,0.08,0.09),Color("#be4440"),Vector3(0,0.8,0.84))
    return bike

func _build_bicycle_rack() -> void:
    var rack := Node3D.new()
    rack.name = "BicycleRack"
    rack.position = RACK_POSITION
    _scenery.add_child(rack)
    for x in [-0.85,0.85]:
        _bike_tube(rack,Vector3(x,0,0),Vector3(x,0.8,0),Color("#727876"),0.055)
    _bike_tube(rack,Vector3(-0.85,0.8,0),Vector3(0.85,0.8,0),Color("#727876"),0.055)
    _add_obstacle(RACK_POSITION,Vector2(2.0,0.45))
    var parked := _make_bicycle(rack,"ParkedBicycle",Color("#496e50"))
    parked.position = Vector3(0,0,0.6)
    parked.rotation.y = PI/2.0
    parked.visible = false
    _sign(rack,"Bici · P",Vector3(0,1.5,0))

func _build_freight_parking() -> void:
    var lot := Node3D.new()
    lot.name = "FreightParking"
    lot.position = TRUCK_PARK
    _scenery.add_child(lot)
    _box(lot,Vector3(3,0.045,5.8),Color("#505659"),Vector3.ZERO)
    _sign(lot,"Merci · F",Vector3(0,2.7,-2.8))
    var truck := Node3D.new()
    truck.name = "AssignedTruck"
    lot.add_child(truck)
    var paint := Color("#b29b69")
    _box(truck,Vector3(2.1,0.55,4.7),Color("#48494a"),Vector3(0,0.67,0))
    _box(truck,Vector3(2.1,1.65,1.65),paint,Vector3(0,1.51,-1.43))
    _box(truck,Vector3(1.78,0.65,0.06),Color("#577782"),Vector3(0,1.85,-2.27))
    _box(truck,Vector3(2.1,0.45,2.7),paint,Vector3(0,1.05,0.75))
    for x in [-1.0,1.0]:
        _box(truck,Vector3(0.12,0.6,2.7),paint,Vector3(x,1.47,0.75))
        for z in [-1.5,1.45]:
            var wheel := _cylinder(truck,0.43,0.27,Color("#2b3030"),Vector3(x,0.43,z))
            wheel.rotation_degrees.z = 90
    for z in [0.1,0.8,1.5]:
        _box(truck,Vector3(1.65,0.46,0.62),Color("#846d58"),Vector3(0,1.5,z))
    truck.visible = false

func _build_public_car_park() -> void:
    var lot := Node3D.new()
    lot.name = "PublicCarPark"
    lot.position = CAR_PARK
    _scenery.add_child(lot)
    _box(lot,Vector3(3.2,0.05,5.0),Color("#4b5153"),Vector3.ZERO)
    for x in [-1.5,1.5]:
        _box(lot,Vector3(0.08,0.018,4.8),Color("#e7e0cd"),Vector3(x,0.04,0))
    _box(lot,Vector3(3,0.018,0.08),Color("#e7e0cd"),Vector3(0,0.04,-2.4))
    _sign(lot,"Parcheggio · F",Vector3(0,2.1,-2.4))
    var car := _make_city_car(lot,Color("#496e50"))
    car.name = "OwnedCar"
    car.visible = false

func _build_fuel_station() -> void:
    var station := Node3D.new()
    station.name = "StazioneDiServizio"
    _scenery.add_child(station)
    var white := Color("#e2dfd3")
    var red := Color("#a34a39")
    _box(station,Vector3(12,0.055,14),Color("#52595b"),Vector3(-9,0.02,-6))
    _box(station,Vector3(11.6,0.32,9.5),white,Vector3(-9,4.0,-6))
    _box(station,Vector3(11.7,0.27,0.2),red,Vector3(-9,4.02,-1.2))
    for x in [-14.2,-3.8]:
        _room_wall(station,Vector3(x,0,-6),Vector2(0.32,0.32),white)
        _box(station,Vector3(0.32,1.0,0.32),white,Vector3(x,3.5,-6))
    for x in [-12.0,-6.0]:
        var pump := Node3D.new()
        pump.name = "FuelPump"
        pump.position = Vector3(x,0,-6)
        station.add_child(pump)
        _add_obstacle(pump.position,Vector2(0.85,0.7))
        _box(pump,Vector3(0.85,1.65,0.7),white,Vector3(0,0.825,0))
        _box(pump,Vector3(0.65,0.35,0.07),Color("#394b4c"),Vector3(0,1.25,0.39))
        _box(pump,Vector3(0.85,0.20,0.72),red,Vector3(0,1.55,0))
        _bike_tube(pump,Vector3(0.48,1.25,0),Vector3(0.63,0.25,0),Color("#303434"),0.035)
        _bike_tube(pump,Vector3(0.63,0.25,0),Vector3(0.51,0.9,0.23),Color("#303434"),0.035)
    _sign(station,"Stazione · carburante",Vector3(-9,4.5,-1.1))
    # Walk-in shop with a doorway toward the central path.
    var shop := Node3D.new()
    shop.name = "StationShop"
    station.add_child(shop)
    _box(shop,Vector3(9,0.06,7),Color("#c3baaa"),Vector3(-9,0.02,10))
    for side in [-1.0,1.0]:
        _room_wall(shop,Vector3(-9+side*4.5,0,10),Vector2(0.25,7),white)
    _room_wall(shop,Vector3(-9,0,13.5),Vector2(9,0.25),white)
    for x in [-12.2,-5.8]:
        _room_wall(shop,Vector3(x,0,6.5),Vector2(2.6,0.25),white)
    _box(shop,Vector3(9.3,0.3,7.3),red,Vector3(-9,3.15,10))
    _sign(shop,"Mini market",Vector3(-9,3.6,6.3))
    _room_furniture(shop,Vector3(-12.1,0,11.8),Vector3(1.1,1.6,2),Color("#856a4d"))
    _room_furniture(shop,Vector3(-6,0,11.8),Vector3(1.1,1.0,2),Color("#a8b7b8"))
    _sign(station,"Fuel / shop / jobs · F",KIOSK_POSITION+Vector3(0,2.2,0))
    _lamp(station,Vector3(-3,0,8))

func _build_motor_court() -> void:
    var court := Node3D.new()
    court.name = "OfficinaMotorCourt"
    _scenery.add_child(court)
    # Keep the rack, kiosk, main paths and district seams clear.
    var asphalt := Color("#52585b")
    var white := Color("#ddd9c7")
    _box(court,Vector3(11,0.05,8),asphalt,Vector3(-9,0.015,-9))
    for x in [-14.5,-11.75,-9.0,-6.25,-3.5]:
        _box(court,Vector3(0.09,0.018,5.3),white,Vector3(x,0.055,-10.0))
    _box(court,Vector3(11,0.018,0.1),white,Vector3(-9,0.055,-12.65))
    _sign(court,"Parcheggio",Vector3(-9,2.2,-13.2))
    var car := _make_city_car(court,Color("#7191a4"))
    car.position = Vector3(-13.1,0,-10)
    _add_obstacle(car.position,Vector2(1.85,3.6))
    # Ground-level two-bay service garage, front open toward z=7.
    var garage := Node3D.new()
    garage.name = "ServiceGarage"
    court.add_child(garage)
    var plaster := Color("#c3b6a0")
    _box(garage,Vector3(10,0.08,8),Color("#92918a"),Vector3(10,0.025,11.5))
    for x in [5.0,10.0,15.0]:
        _room_wall(garage,Vector3(x,0,11.5),Vector2(0.32,8),plaster)
    _room_wall(garage,Vector3(10,0,15.5),Vector2(10,0.32),plaster)
    _box(garage,Vector3(10.5,0.3,8.4),Color("#6f7173"),Vector3(10,3.15,11.5))
    _box(garage,Vector3(10.5,0.5,0.35),plaster,Vector3(10,2.85,7.45))
    _sign(garage,"Autorimessa · Officina",Vector3(10,3.7,7.3))
    _sign(garage,"Bay 1",Vector3(7.5,2.45,7.35))
    _sign(garage,"Bay 2",Vector3(12.5,2.45,7.35))
    for at in [Vector3(7.5,2.8,11.5),Vector3(12.5,2.8,11.5)]:
        var light := OmniLight3D.new()
        light.position = at
        light.light_color = Color("#fff0d3")
        light.light_energy = 1.1
        light.omni_range = 6.0
        garage.add_child(light)
    var service_car := _make_city_car(garage,Color("#ae8771"))
    service_car.position = Vector3(12.5,0,12)
    _add_obstacle(service_car.position,Vector2(1.85,3.6))

func _make_city_car(parent: Node3D,paint: Color) -> Node3D:
    var car := Node3D.new()
    car.name = "CityCar"
    parent.add_child(car)
    var dark := Color("#303638")
    var glass := Color("#536d79")
    _box(car,Vector3(1.78,0.60,3.5),paint,Vector3(0,0.62,0))
    _box(car,Vector3(1.64,0.65,1.9),paint,Vector3(0,1.23,0.22))
    _box(car,Vector3(1.45,0.45,0.07),glass,Vector3(0,1.23,-0.76))
    _box(car,Vector3(1.45,0.45,0.07),glass,Vector3(0,1.23,1.2))
    for side in [-1.0,1.0]:
        for z in [-0.25,0.69]:
            _box(car,Vector3(0.07,0.44,0.76),glass,Vector3(side*0.835,1.27,z))
        for z in [-1.08,1.08]:
            var wheel := _cylinder(car,0.32,0.20,dark,Vector3(side*0.82,0.34,z))
            wheel.rotation_degrees.z = 90
            var hub := _cylinder(car,0.16,0.04,Color("#a6aaa7"),Vector3(side*0.94,0.34,z))
            hub.rotation_degrees.z = 90
        _box(car,Vector3(0.34,0.19,0.08),Color("#eee5bb"),Vector3(side*0.60,0.70,-1.79))
        _box(car,Vector3(0.28,0.18,0.08),Color("#b33b36"),Vector3(side*0.65,0.7,1.79))
    for z in [-1.77,1.77]:
        _box(car,Vector3(1.75,0.16,0.13),dark,Vector3(0,0.43,z))
        _box(car,Vector3(0.40,0.12,0.03),Color("#e7e6de"),Vector3(0,0.60,z*1.035))
    return car

func _build_farm() -> void:
    var farm := Node3D.new()
    farm.name = "CascinaDelleRisaie"
    _scenery.add_child(farm)
    # Field beds and shallow rice paddy, outside the north/south road.
    _box(farm,Vector3(12,0.06,14),Color("#66503a"),Vector3(-10,0.015,-8))
    for x in [-15.0,-13.0,-11.0,-9.0,-7.0,-5.0]:
        _box(farm,Vector3(1.1,0.17,13),Color("#796043"),Vector3(x,0.1,-8))
        for z in range(-14,-1,2):
            _box(farm,Vector3(0.48,0.24,0.45),Color("#668740"),Vector3(x,0.31,z))
    _box(farm,Vector3(12,0.09,14),Color("#608e80"),Vector3(10,0.04,-8))
    for x in range(5,16,2):
        for z in range(-14,-1,2):
            _box(farm,Vector3(0.14,0.5,0.14),Color("#a9b75d"),Vector3(x,0.3,z))
    _add_obstacle(Vector3(10,0,-8),Vector2(12,14))
    _sign(farm,"Risaia",Vector3(8,1.6,-1))
    _sign(farm,"Orto",Vector3(-8,1.6,-1))
    # Lombard farmhouse / barn and stationary tractor.
    _add_obstacle(Vector3(-10,0,10),Vector2(10,8))
    _box(farm,Vector3(10,4.2,8),Color("#b99a74"),Vector3(-10,2.1,10))
    _box(farm,Vector3(10.6,0.5,8.6),Color("#8b4e38"),Vector3(-10,4.45,10))
    _box(farm,Vector3(3,2.8,0.12),Color("#694a34"),Vector3(-10,1.4,5.96))
    _sign(farm,"Cascina delle Risaie",Vector3(-10,3.4,5.8))
    var tractor := Vector3(10,0,10)
    _add_obstacle(tractor,Vector2(3.5,4.6))
    _box(farm,Vector3(2.0,0.85,3.8),Color("#527c47"),tractor+Vector3(0,1.0,0))
    _box(farm,Vector3(1.6,1.4,1.6),Color("#759b9b"),tractor+Vector3(0,2.0,0.8))
    _box(farm,Vector3(1.9,0.16,1.9),Color("#426746"),tractor+Vector3(0,2.8,0.8))
    for side in [-1.0,1.0]:
        for z in [-1.3,1.3]:
            var wheel := _cylinder(farm,0.65 if z>0 else 0.43,0.52,Color("#343431"),tractor+Vector3(side*1.25,0.65 if z>0 else 0.43,z))
            wheel.rotation_degrees.z = 90
    _sign(farm,"Farm office · F",KIOSK_POSITION+Vector3(0,2.2,0))
    _lamp(farm,Vector3(-3,0,8))

func _build_guesthouse() -> void:
    var house := Node3D.new()
    house.name = "CasaOspiti"
    _scenery.add_child(house)
    var plaster := Color("#d6c4a3")
    var interior := Color("#e7d9bd")
    _box(house,Vector3(9,0.08,10),Color("#bd9c79"),Vector3(-8.5,0.025,9))
    _room_wall(house,Vector3(-13,0,9),Vector2(0.24,10),plaster)
    _room_wall(house,Vector3(-8.5,0,4),Vector2(9,0.24),plaster)
    _room_wall(house,Vector3(-8.5,0,14),Vector2(9,0.24),plaster)
    _room_wall(house,Vector3(-4,0,5.5),Vector2(0.24,3),plaster)
    _room_wall(house,Vector3(-4,0,12),Vector2(0.24,4),plaster)
    _box(house,Vector3(0.35,0.45,3.1),Color("#a7805e"),Vector3(-4,2.78,8.5))
    var door := Node3D.new()
    door.name = "LeaseDoor"
    house.add_child(door)
    _box(door,Vector3(0.16,2.55,2.95),Color("#715039"),Vector3(-4,1.275,8.5))
    _sphere(door,0.08,Color("#d5b16e"),Vector3(-3.88,1.1,7.5))
    door.visible = not _has_room_lease()
    _box(house,Vector3(9.25,0.22,10.3),Color("#ccb99c"),Vector3(-8.5,3.1,9))
    # Decorative upper storey; the actual rented studio is on ground level.
    _box(house,Vector3(9,3.2,10),Color("#c4a382"),Vector3(-8.5,4.8,9))
    _box(house,Vector3(9.5,0.35,10.5),Color("#865747"),Vector3(-8.5,6.55,9))
    for z in [6.0,9.0,12.0]:
        _box(house,Vector3(0.08,1.3,1.0),Color("#3b5051"),Vector3(-3.96,4.7,z))
        for offset in [-0.72,0.72]:
            _box(house,Vector3(0.14,1.4,0.35),Color("#486045"),Vector3(-3.86,4.7,z+offset))
    _sign(house,"Casa Ospiti · Camera 1",Vector3(-3.7,3.35,8.5))
    _sign(house,"Reception · F",KIOSK_POSITION+Vector3(0,2.2,0))
    # Bedroom opening from z=7 to z=10.
    _room_wall(house,Vector3(-8,0,5.5),Vector2(0.17,3),interior)
    _room_wall(house,Vector3(-8,0,12),Vector2(0.17,4),interior)
    # Bathroom opening centred at x=-10.
    _room_wall(house,Vector3(-12,0,11),Vector2(2,0.17),interior)
    _room_wall(house,Vector3(-8.5,0,11),Vector2(1,0.17),interior)
    _room_furniture(house,BED_POSITION,Vector3(2.1,0.46,2.0),Color("#806449"))
    _box(house,Vector3(2.0,0.22,1.93),Color("#dce1db"),BED_POSITION+Vector3(0,0.57,0))
    _box(house,Vector3(1.95,0.12,1.25),Color("#6e8494"),BED_POSITION+Vector3(0,0.73,0.3))
    _box(house,Vector3(0.72,0.16,0.45),Color("#f0e9d8"),BED_POSITION+Vector3(0,0.76,-0.69))
    _sign(house,"Bed · F",BED_POSITION+Vector3(0,1.6,0))
    _room_furniture(house,Vector3(-12.35,0,9.35),Vector3(0.85,2.15,1.5),Color("#815d41"))
    # Kitchen counter, sink, small dining table; visually usable floor plan.
    _room_furniture(house,Vector3(-5.05,0,12.3),Vector3(1.0,0.92,2.25),Color("#8b7558"))
    _box(house,Vector3(1.05,0.09,2.3),Color("#c4c5bc"),Vector3(-5.05,0.96,12.3))
    _box(house,Vector3(0.6,0.08,0.62),Color("#71868a"),Vector3(-5.05,1.02,12.65))
    _room_furniture(house,Vector3(-6.25,0,10.5),Vector3(1.05,0.76,0.95),Color("#ae8863"))
    _sign(house,"Kitchen / cupboard · F",Vector3(-5.05,1.9,12.3))
    # Bathroom shower tray and toilet, not simulated plumbing yet.
    _room_furniture(house,Vector3(-12.1,0,12.95),Vector3(1.2,0.15,1.4),Color("#e5e9e4"))
    _box(house,Vector3(0.06,1.9,1.4),Color("#9ebcc1"),Vector3(-11.5,1.02,12.95))
    _room_furniture(house,Vector3(-8.8,0,13.0),Vector3(0.65,0.5,0.8),Color("#e6e7dd"))
    _box(house,Vector3(0.65,0.85,0.25),Color("#e6e7dd"),Vector3(-8.8,0.43,13.35))
    _sign(house,"Wash · F",Vector3(-12.1,2.25,12.95))
    for at in [Vector3(-10.5,2.8,8),Vector3(-6,2.8,9),Vector3(-10.5,2.8,12.5)]:
        var light := OmniLight3D.new()
        light.position = at
        light.light_color = Color("#ffe2b8")
        light.light_energy = 1.1
        light.omni_range = 5.5
        house.add_child(light)
    _lamp(house,Vector3(-2.8,0,12))

func _build_portici() -> void:
    var street := Node3D.new()
    street.name = "ViaDeiPortici"
    _scenery.add_child(street)
    _box(street,Vector3(7.4,0.06,34),Color("#b7aa93"),Vector3(0,0.025,0))
    _box(street,Vector3(34,0.06,5.1),Color("#b7aa93"),Vector3(0,0.025,0))
    for side in [-1,1]:
        _box(street,Vector3(1.1,0.12,34),Color("#d1c4a9"),Vector3(side*3.65,0.06,0))
    # The four road axes are open; blocks occupy the corners only.
    _portici_house(street,-1.0,-9.5,12.0,8.0,Color("#c8ae89"),"Casa 12")
    _portici_house(street,-1.0,10.0,12.0,9.2,Color("#bc8b73"),"Casa 14")
    _portici_house(street,1.0,-10.0,12.0,9.0,Color("#d5c09c"),"Casa 15")
    _portici_house(street,1.0,12.6,8.0,7.2,Color("#c29b73"),"Casa 17")
    _portici_grocery(street)
    for at in [Vector3(-3,0,-8),Vector3(3,0,-4),Vector3(-3,0,9),Vector3(3,0,12)]:
        _lamp(street,at)
    _sign(street,"Via dei Portici",Vector3(0,3.6,-8))
    _sign(street,"Piazza Centrale",Vector3(0,3.4,-16))

func _portici_house(parent: Node3D,side: float,z: float,length: float,height: float,
        wall: Color,caption: String) -> void:
    var front := side*4.4
    var center := side*9.2
    var trim := Color("#dfc9a5")
    _add_obstacle(Vector3(center,0,z),Vector2(9.6,length))
    _box(parent,Vector3(9.6,height,length),wall,Vector3(center,height*0.5,z))
    _box(parent,Vector3(9.85,0.32,length+0.2),Color("#744536"),Vector3(center,height+0.12,z))
    _box(parent,Vector3(0.12,0.18,length),trim,Vector3(front-side*0.06,3.3,z))
    for offset in [-length/3.0,0.0,length/3.0]:
        for y in [4.5,6.5]:
            var face := front-side*0.055
            _box(parent,Vector3(0.1,1.3,0.88),Color("#35454a"),Vector3(face,y,z+offset))
            _box(parent,Vector3(0.22,0.12,1.65),trim,Vector3(face,y-0.72,z+offset))
            for shutter in [-0.65,0.65]:
                _box(parent,Vector3(0.12,1.4,0.36),Color("#455644"),Vector3(face-side*0.07,y,z+offset+shutter))
        if offset == 0.0:
            _box(parent,Vector3(0.16,2.55,1.5),Color("#503c2e"),Vector3(front-side*0.08,1.275,z))
            _box(parent,Vector3(0.18,0.25,1.85),trim,Vector3(front-side*0.12,2.65,z))
        else:
            _box(parent,Vector3(0.14,1.25,1.2),Color("#5d6460"),Vector3(front-side*0.08,1.55,z+offset))
    _sign(parent,caption,Vector3(front-side*0.28,2.95,z))
    _centro_flowers(parent,Vector3(front-side*0.22,3.78,z+length/3.0))

func _portici_grocery(parent: Node3D) -> void:
    var shop := Node3D.new()
    shop.name = "BottegaAlimentare"
    parent.add_child(shop)
    var wall := Color("#d3b995")
    # Open doorway on the west face (x=4.4), z=4.3..6.3.
    # The existing local action kiosk at (7,3) sits INSIDE the shop.
    var walls := [
        [Vector3(10.5,0,5.0),Vector2(0.3,6.0)],
        [Vector3(7.45,0,2.15),Vector2(6.4,0.3)],
        [Vector3(7.45,0,7.85),Vector2(6.4,0.3)],
        [Vector3(4.4,0,3.2),Vector2(0.3,2.2)],
        [Vector3(4.4,0,7.1),Vector2(0.3,1.6)]]
    for part in walls:
        var at: Vector3 = part[0]
        var footprint: Vector2 = part[1]
        _add_obstacle(at,footprint)
        _box(shop,Vector3(footprint.x,3.4,footprint.y),wall,at+Vector3(0,1.7,0))
    _box(shop,Vector3(6.4,0.065,6),Color("#b69b78"),Vector3(7.45,0.03,5))
    _box(shop,Vector3(6.4,3.8,6),wall,Vector3(7.45,5.3,5))
    _box(shop,Vector3(6.65,0.3,6.25),Color("#794b36"),Vector3(7.45,7.35,5))
    _box(shop,Vector3(0.24,0.45,2.3),Color("#6f4931"),Vector3(4.4,2.85,5.3))
    _sign(shop,"Alimentari · F",Vector3(4.05,3.1,5.3))
    for z in [4.5,6.8]:
        var shelf := Vector3(9.8,0,z)
        _add_obstacle(shelf,Vector2(0.6,1.3))
        _box(shop,Vector3(0.6,1.8,1.3),Color("#765c40"),shelf+Vector3(0,0.9,0))
        for y in [0.75,1.25]:
            _box(shop,Vector3(0.25,0.24,0.9),Color("#d9bb76"),shelf+Vector3(-0.35,y,0))
    var light := OmniLight3D.new()
    light.position = Vector3(7.5,2.95,5)
    light.light_color = Color("#ffe0aa")
    light.light_energy = 1.2
    light.omni_range = 5.5
    shop.add_child(light)
    _park_lamps.append(light)

func _build_centro() -> void:
    var piazza := Node3D.new()
    piazza.name = "PiazzaCentrale"
    _scenery.add_child(piazza)
    var stone := Color(0.68,0.64,0.57)
    _box(piazza,Vector3(34,0.035,34),stone,Vector3(0,0.006,0))
    _centro_paving(piazza)
    var colors := [Color("#bf956e"),Color("#d6b185"),Color("#d4c1a0"),Color("#ba8274")]
    var variant := 0
    for side in [-1,1]:
        for row in [-1,1]:
            _centro_portico(piazza,float(side),float(row)*9.0,colors[variant],variant)
            variant += 1
    _centro_courtyard(piazza)
    # Landmark offset from the northern exit; no fictional church access.
    var tower := Vector3(6.8,0,-14.1)
    _add_obstacle(tower,Vector2(3.0,3.0))
    _box(piazza,Vector3(3,10,3),Color(0.59,0.34,0.25),tower+Vector3(0,5,0))
    _box(piazza,Vector3(3.5,0.45,3.5),Color(0.73,0.61,0.45),tower+Vector3(0,9.8,0))
    _box(piazza,Vector3(2.7,2.3,2.7),Color(0.68,0.42,0.30),tower+Vector3(0,11.15,0))
    _box(piazza,Vector3(3.2,0.35,3.2),Color(0.29,0.20,0.17),tower+Vector3(0,12.5,0))
    _sign(piazza,"Torre Civica",tower+Vector3(0,8,1.6))
    for at in [Vector3(-5,0,-9),Vector3(5,0,-6),Vector3(-5,0,8),Vector3(5,0,10)]:
        _lamp(piazza,at)
    _bench(piazza,Vector3(-7,0,12))
    _bench(piazza,Vector3(7,0,12))
    # Café furniture is scenery, with solid footprints, not yet a paid service.
    for z in [-10.0,-6.0]:
        var at := Vector3(-8,0,z)
        _add_obstacle(at,Vector2(1.4,1.4))
        _cylinder(piazza,0.08,0.9,Color(0.25,0.22,0.19),at+Vector3(0,0.45,0))
        _cylinder(piazza,0.65,0.09,Color(0.45,0.25,0.14),at+Vector3(0,0.92,0))
        _cylinder(piazza,1.1,0.1,Color(0.65,0.19,0.14),at+Vector3(0,2.15,0))
        _cylinder(piazza,0.035,1.2,Color(0.35,0.27,0.2),at+Vector3(0,1.55,0))
    _sign(piazza,"Caffè Ducale",Vector3(-11,3.55,-9))
    _sign(piazza,"Botteghe",Vector3(11,3.55,-9))
    _sign(piazza,"Piazza Centrale",Vector3(0,3.9,-12))
    _sign(piazza,"Bacheca lavoro · F",KIOSK_POSITION+Vector3(0,2.4,0))

func _centro_paving(parent: Node3D) -> void:
    # One batched mesh for 1,536 stone blocks, not 1,536 scene nodes.
    var stone := BoxMesh.new()
    stone.size = Vector3(0.96,0.025,0.65)
    var material := StandardMaterial3D.new()
    material.vertex_color_use_as_albedo = true
    material.roughness = 0.96
    stone.material = material
    var batch := MultiMesh.new()
    batch.transform_format = MultiMesh.TRANSFORM_3D
    batch.use_colors = true
    batch.mesh = stone
    batch.instance_count = 32*48
    var palette := [Color("#b3aa99"),Color("#c4b9a4"),Color("#a99f91"),Color("#c9c1b2"),Color("#bcb2a2")]
    for row in range(48):
        for col in range(32):
            var index := row*32+col
            var x := float(col)-15.75 + float(row%2)*0.5
            var z := (float(row)-23.5)*0.69
            batch.set_instance_transform(index,Transform3D(Basis.IDENTITY,Vector3(x,0.038,z)))
            batch.set_instance_color(index,palette[(row*13+col*7+row*col)%palette.size()])
    var paving := MultiMeshInstance3D.new()
    paving.name = "StonePaving"
    paving.multimesh = batch
    parent.add_child(paving)

func _centro_courtyard(parent: Node3D) -> void:
    # A small open garden court off the piazza, entered from its south side.
    var court := Node3D.new()
    court.name = "CortileDeiFiori"
    court.position = Vector3(-6.4,0,-14.3)
    parent.add_child(court)
    _box(court,Vector3(4.2,0.06,3.5),Color("#b99d78"),Vector3(0,0.05,0))
    for x in [-2.0,2.0]:
        _box(court,Vector3(0.25,1.0,3.5),Color("#c6a47f"),Vector3(x,0.5,0))
        _add_obstacle(court.position+Vector3(x,0,0),Vector2(0.25,3.5))
    _box(court,Vector3(4.2,1.2,0.25),Color("#c6a47f"),Vector3(0,0.6,-1.65))
    _add_obstacle(court.position+Vector3(0,0,-1.65),Vector2(4.2,0.25))
    for x in [-1.25,1.25]:
        _box(court,Vector3(0.7,0.42,0.85),Color("#9f6545"),Vector3(x,0.21,-0.95))
        _add_obstacle(court.position+Vector3(x,0,-0.95),Vector2(0.7,0.85))
        _sphere(court,0.32,Color("#577443"),Vector3(x,0.58,-0.95))
        for offset in [-0.17,0.0,0.17]:
            _sphere(court,0.095,Color("#b94c51"),Vector3(x+offset,0.83,-0.95))
    _sign(court,"Cortile dei Fiori",Vector3(0,1.7,-1.55))

func _centro_portico(root: Node3D, side: float, z: float, wall: Color, variant: int) -> void:
    var parent := Node3D.new()
    parent.name = "Facade_%s" % variant
    root.add_child(parent)
    # Building rear at x=15.8; open covered arcade between rear and pillars.
    var back := side*15.8
    var front := side*11.7
    var trim := Color(0.89,0.78,0.60)
    _add_obstacle(Vector3(back,0,z),Vector2(2.2,12.0))
    _box(parent,Vector3(2.2,7.1,12),wall,Vector3(back,3.55,z))
    _box(parent,Vector3(5.2,3.9,12),wall,Vector3(side*14,5.15,z))
    _box(parent,Vector3(5.5,0.32,12.2),Color(0.48,0.26,0.20),Vector3(side*14,7.25,z))
    _box(parent,Vector3(5.7,0.14,12.3),trim,Vector3(side*14,7.03,z))
    _box(parent,Vector3(5.2,0.16,12),trim,Vector3(side*14,3.25,z))
    _box(parent,Vector3(0.16,0.16,12),trim,Vector3(side*11.35,5.35,z))
    for offset in [-6.0,-2.0,2.0,6.0]:
        var at := Vector3(front,0,z+offset)
        _add_obstacle(at,Vector2(0.55,0.55))
        _box(parent,Vector3(0.55,1.95,0.55),trim,at+Vector3(0,0.975,0))
    for offset in [-4.0,0.0,4.0]:
        # Segmented semicircular stone arches; the open space below is walkable.
        for i in range(9):
            var angle := float(i)*PI/8.0
            var arch := Node3D.new()
            arch.position = Vector3(front,1.95+sin(angle)*1.72,z+offset+cos(angle)*1.72)
            arch.rotation.x = -angle
            parent.add_child(arch)
            _box(arch,Vector3(0.58,0.74,0.36),trim,Vector3.ZERO)
        for y in [4.4,6.15]:
            var face := side*11.36
            _box(parent,Vector3(0.08,1.1,0.85),Color(0.19,0.28,0.30),Vector3(face,y,z+offset))
            var shutters := Color("#415943") if variant%2 == 0 else Color("#64513f")
            _box(parent,Vector3(0.15,0.12,1.6),trim,Vector3(face-side*0.06,y-0.61,z+offset))
            for shutter in [-0.64,0.64]:
                _box(parent,Vector3(0.13,1.18,0.35),shutters,Vector3(face-side*0.06,y,z+offset+shutter))
            if y < 5.0 and (variant == 1 or variant == 3) and offset == 0.0:
                _centro_balcony(parent,side,face,y,z+offset)
            elif y > 5.0 and variant%2 == 0:
                _centro_flowers(parent,Vector3(face-side*0.22,y-0.60,z+offset))
        var rear := side*14.64
        # Recessed shop fronts have a frame, glass panes and timber door.
        _box(parent,Vector3(0.12,2.35,2.55),Color("#6d5140"),Vector3(rear,1.175,z+offset))
        for pane in [-0.83,0.83]:
            _box(parent,Vector3(0.08,1.52,0.72),Color("#496064"),Vector3(rear-side*0.09,1.29,z+offset+pane))
            _box(parent,Vector3(0.09,0.045,0.72),trim,Vector3(rear-side*0.14,1.29,z+offset+pane))
        _box(parent,Vector3(0.09,1.94,0.65),Color("#3c3028"),Vector3(rear-side*0.1,0.97,z+offset))
        _box(parent,Vector3(0.14,0.28,2.55),Color("#9c6b45"),Vector3(rear-side*0.11,2.45,z+offset))

func _centro_balcony(parent: Node3D,side: float,face: float,y: float,z: float) -> void:
    var edge := face-side*0.92
    var iron := Color("#353331")
    _box(parent,Vector3(1.1,0.18,2.1),Color("#a89c88"),Vector3(face-side*0.48,y-0.65,z))
    _box(parent,Vector3(0.065,0.065,2.1),iron,Vector3(edge,y+0.12,z))
    for i in range(9):
        _box(parent,Vector3(0.045,0.72,0.045),iron,Vector3(edge,y-0.24,z-1.0+float(i)*0.25))
    for end in [-1.0,1.0]:
        _box(parent,Vector3(0.95,0.065,0.065),iron,Vector3(face-side*0.45,y+0.12,z+end))
    _centro_flowers(parent,Vector3(edge,y-0.4,z))

func _centro_flowers(parent: Node3D,at: Vector3) -> void:
    _box(parent,Vector3(0.32,0.20,0.90),Color("#986647"),at)
    _box(parent,Vector3(0.28,0.18,0.84),Color("#47633c"),at+Vector3(0,0.16,0))
    for offset in [-0.32,-0.10,0.10,0.32]:
        _sphere(parent,0.10,Color("#c85b67"),at+Vector3(0,0.29,offset))

func _build_park() -> void:
    var park := Node3D.new()
    park.name = "ParkDetails"
    _scenery.add_child(park)
    var paving := Color(0.65, 0.61, 0.54)
    _box(park, Vector3(32.0, 0.06, 2.6), paving, Vector3(0, 0.01, 5))
    _box(park, Vector3(3.2, 0.06, 16.0), paving, Vector3(-11, 0.01, -3))
    for p in [Vector2(-15,-13), Vector2(-8,-14), Vector2(8,-13),
            Vector2(14,-8), Vector2(-15,9), Vector2(-7,11),
            Vector2(10,11), Vector2(15,14), Vector2(-8,-5)]:
        _tree(Vector3(p.x, 0.0, p.y))
    for p in [Vector3(-5,0,3), Vector3(-5,0,7), Vector3(11,0,7)]:
        _bench(park, p)
    for p in [Vector3(-3,0,-9), Vector3(3,0,-3), Vector3(-3,0,8), Vector3(13,0,3)]:
        _lamp(park, p)
    # The pool is decorative; drinking still requires the validated Java action.
    var fountain := Node3D.new()
    fountain.name = "PublicFountain"
    fountain.position = Vector3(8, 0, -5)
    park.add_child(fountain)
    _add_obstacle(fountain.position, Vector2(4.0, 4.0))
    _cylinder(fountain, 2.0, 0.35, Color(0.52,0.53,0.54), Vector3(0,0.18,0))
    _cylinder(fountain, 1.85, 0.08, Color(0.24,0.28,0.29), Vector3(0,0.39,0))
    _fountain_water = _cylinder(fountain, 1.78, 0.04, Color(0.24,0.63,0.76), Vector3(0,0.45,0))
    _cylinder(fountain, 0.25, 1.1, Color(0.60,0.61,0.62), Vector3(0,0.85,0))
    _sign(fountain, "Public fountain", Vector3(0,1.8,0))
    _sign(park, "Town centre · E", GATE_POSITION + Vector3(0,3.7,0))
    _sign(park, "Public services · F", KIOSK_POSITION + Vector3(0,2.2,0))
    # A street along the edge provides visual context, not a new travel route.
    _box(park, Vector3(38,0.08,3.2), Color(0.23,0.25,0.27), Vector3(0,0.02,-16))
    for x in range(-16,17,4):
        _box(park, Vector3(1.5,0.02,0.11), Color(0.92,0.85,0.61), Vector3(x,0.07,-16))
    for x in [-14.0,-8.0,8.0,14.0]:
        _add_obstacle(Vector3(x,0,-19), Vector2(4.7,2.5))
        _box(park, Vector3(4.7,4.0,2.5), Color(0.69,0.59,0.47), Vector3(x,2,-19))
        for offset in [-1.2,1.2]:
            _box(park, Vector3(0.8,1.1,0.08), Color(0.32,0.49,0.57), Vector3(x+offset,2.8,-17.7))

func _bench(parent: Node3D, at: Vector3) -> void:
    _add_obstacle(at, Vector2(2.0, 0.66))
    var wood := Color(0.43,0.27,0.14)
    _box(parent, Vector3(2.0,0.12,0.58), wood, at + Vector3(0,0.52,0))
    _box(parent, Vector3(2.0,0.42,0.10), wood, at + Vector3(0,0.85,0.25))
    for x in [-0.75,0.75]:
        _box(parent, Vector3(0.11,0.50,0.47), Color(0.20,0.23,0.24), at+Vector3(x,0.25,0))

func _lamp(parent: Node3D, at: Vector3) -> void:
    _add_obstacle(at, Vector2(0.18, 0.18))
    _cylinder(parent, 0.09, 3.4, Color(0.19,0.22,0.24), at+Vector3(0,1.7,0))
    _sphere(parent, 0.23, Color(0.91,0.87,0.65), at+Vector3(0,3.4,0))
    var lamp := OmniLight3D.new()
    lamp.position = at+Vector3(0,3.2,0)
    lamp.light_color = Color(1.0,0.81,0.53)
    lamp.light_energy = 2.0
    lamp.omni_range = 8.0
    lamp.visible = false
    parent.add_child(lamp)
    _park_lamps.append(lamp)

func _sign(parent: Node3D, caption: String, at: Vector3) -> void:
    var tag := Label3D.new()
    tag.text = caption
    tag.position = at
    tag.billboard = BaseMaterial3D.BILLBOARD_ENABLED
    tag.font_size = 30
    tag.pixel_size = 0.008
    parent.add_child(tag)

func _cylinder(parent: Node3D, radius: float, height: float, color: Color,
        at: Vector3) -> MeshInstance3D:
    var mesh := CylinderMesh.new()
    mesh.top_radius = radius
    mesh.bottom_radius = radius
    mesh.height = height
    mesh.radial_segments = 16
    var node := MeshInstance3D.new()
    node.mesh = mesh
    var material := StandardMaterial3D.new()
    material.albedo_color = color
    node.material_override = material
    node.position = at
    parent.add_child(node)
    return node

func _render_residents() -> void:
    # Stable visual identities. Java still owns presence and activity.
    if _residents_root == null:
        return
    var residents: Array = _last_snapshot.get("residents", [])
    var present: Dictionary = {}
    for i in range(residents.size()):
        if typeof(residents[i]) != TYPE_DICTIONARY:
            continue
        var resident: Dictionary = residents[i]
        var id := str(resident.get("id", ""))
        if id.is_empty() or present.has(id):
            continue
        present[id] = true
        var person := _residents_root.get_node_or_null("Resident_" + id) as Node3D
        if person == null:
            person = _make_resident(id, i, str(resident.get("name", "Resident")))
        person.set_meta("activity", str(resident.get("activity", "")).to_upper())
        person.set_meta("display_name",str(resident.get("name","Resident")))
        person.set_meta("known_name",str(resident.get("knownName","")) if resident.get("knownName") != null else "")
        person.set_meta("conversations",int(resident.get("conversations",0)))
        person.set_meta("relationship",str(resident.get("relationship","STRANGER")))
    for child in _residents_root.get_children():
        if not present.has(str(child.get_meta("resident_id", ""))):
            _residents_root.remove_child(child)
            child.queue_free()

func _make_resident(id: String, index: int, display_name: String) -> Node3D:
    var person := Node3D.new()
    person.name = "Resident_" + id
    person.set_meta("resident_id", id)
    var anchor := Vector3((float(index % 8)-3.5)*3.0, 0, -4.0-float(index / 8)*2.5)
    anchor.z = maxf(anchor.z, -13.0)
    anchor = _open_anchor(anchor)
    person.position = anchor
    person.set_meta("anchor", anchor)
    person.set_meta("phase", float(absi(id.hash()) % 628) / 100.0)
    _residents_root.add_child(person)
    var seed := absi(id.hash())
    var jacket := Color.from_hsv(float(seed % 360)/360.0, 0.48, 0.63)
    var skin := Color(0.82,0.64,0.49)
    _cylinder(person, 0.24, 0.57, jacket, Vector3(0,1.14,0))
    _sphere(person, 0.22, skin, Vector3(0,1.66,0))
    _sphere(person, 0.215, Color(0.22,0.16,0.12), Vector3(0,1.78,0.035))
    for side in [-1,1]:
        var leg := Node3D.new()
        leg.name = "LegLeft" if side < 0 else "LegRight"
        leg.position = Vector3(side*0.13,0.84,0)
        person.add_child(leg)
        _box(leg,Vector3(0.20,0.70,0.22),Color(0.20,0.25,0.30),Vector3(0,-0.35,0))
        _box(leg,Vector3(0.22,0.13,0.34),Color(0.14,0.15,0.17),Vector3(0,-0.77,-0.06))
        var arm := Node3D.new()
        arm.name = "ArmLeft" if side < 0 else "ArmRight"
        arm.position = Vector3(side*0.32,1.37,0)
        person.add_child(arm)
        _box(arm,Vector3(0.16,0.44,0.17),jacket,Vector3(0,-0.22,0))
        _sphere(arm,0.085,skin,Vector3(0,-0.48,0))
    _sign(person,display_name,Vector3(0,2.1,0))
    return person

func _animate_residents(delta: float) -> void:
    if _residents_root == null:
        return
    for child in _residents_root.get_children():
        var person := child as Node3D
        var activity := str(person.get_meta("activity", ""))
        var walking := activity in ["WALKING", "WANDERING", "STROLLING", "FREE_TIME", "SHOPPING", "JOB_SEARCH"]
        var phase := float(person.get_meta("phase", 0.0))
        if _dialog_panel.visible and str(person.get_meta("resident_id","")) == _dialog_target:
            walking = false
        if walking:
            phase += delta * 0.65
            person.set_meta("phase", phase)
            var anchor: Vector3 = person.get_meta("anchor")
            var target := anchor + Vector3(sin(phase)*1.1,0,cos(phase)*0.8)
            var motion := target-person.position
            if motion.length_squared() > 0.0001:
                person.rotation.y = atan2(-motion.x,-motion.z)
            var next := person.position.move_toward(target,delta*1.5)
            var old := person.position
            person.position = _move_on_ground(old, next - old, WALKER_RADIUS)
            walking = person.position.distance_squared_to(old) > 0.000001
        var swing := sin(phase*12.0)*0.42 if walking else 0.0
        for limb in ["LegLeft","ArmRight","LegRight","ArmLeft"]:
            var part := person.get_node(limb) as Node3D
            var angle := swing if limb in ["LegLeft","ArmRight"] else -swing
            part.rotation.x = lerpf(part.rotation.x,angle,minf(delta*10.0,1.0))

func _building(_title: String, wall_color: Color) -> void:
    _add_obstacle(Vector3(-7.0, 0.0, 7.5), Vector2(11.0, 7.0))
    _box(_scenery, Vector3(11.0, 4.5, 7.0), wall_color,
            Vector3(-7.0, 2.25, 7.5))
    _box(_scenery, Vector3(12.0, 0.7, 8.0), Color(0.27, 0.30, 0.33),
            Vector3(-7.0, 4.85, 7.5))
    _box(_scenery, Vector3(2.1, 3.0, 0.1), Color(0.20, 0.28, 0.32),
            Vector3(-7.0, 1.5, 3.95))

func _create_gate_and_kiosk() -> void:
    # A yellow gate is a map transition — actual travel is authorized by Java.
    for offset in [-1.6, 1.6]:
        _add_obstacle(GATE_POSITION + Vector3(offset, 0.0, 0.0), Vector2(0.4, 0.4))
    _add_obstacle(KIOSK_POSITION, Vector2(2.4, 1.6))
    _box(_scenery, Vector3(0.4, 3.1, 0.4), Color(0.94, 0.69, 0.12),
            GATE_POSITION + Vector3(-1.6, 1.55, 0.0))
    _box(_scenery, Vector3(0.4, 3.1, 0.4), Color(0.94, 0.69, 0.12),
            GATE_POSITION + Vector3(1.6, 1.55, 0.0))
    _box(_scenery, Vector3(3.6, 0.38, 0.45), Color(0.94, 0.69, 0.12),
            GATE_POSITION + Vector3(0.0, 3.2, 0.0))
    # Blue kiosk: only offers actions physically available in this district.
    _box(_scenery, Vector3(2.4, 1.4, 1.6), Color(0.13, 0.39, 0.69),
            KIOSK_POSITION + Vector3(0.0, 0.7, 0.0))
    _box(_scenery, Vector3(3.2, 0.22, 2.2), Color(0.88, 0.89, 0.89),
            KIOSK_POSITION + Vector3(0.0, 1.6, 0.0))

func _tree(at: Vector3) -> void:
    _add_obstacle(at, Vector2(0.45, 0.45))
    _box(_scenery, Vector3(0.45, 1.8, 0.45), Color(0.33, 0.19, 0.11),
            at + Vector3(0.0, 0.9, 0.0))
    _sphere(_scenery, 1.15, Color(0.16, 0.37, 0.16),
            at + Vector3(0.0, 2.4, 0.0))

func _box(parent: Node3D, size: Vector3, color: Color,
        where: Vector3) -> void:
    var mesh := BoxMesh.new()
    mesh.size = size
    var node := MeshInstance3D.new()
    node.mesh = mesh
    var mat := StandardMaterial3D.new()
    mat.albedo_color = color
    node.material_override = mat
    node.position = where
    parent.add_child(node)

func _sphere(parent: Node3D, radius: float, color: Color,
        where: Vector3) -> void:
    var mesh := SphereMesh.new()
    mesh.radius = radius
    mesh.height = radius * 2.0
    var node := MeshInstance3D.new()
    node.mesh = mesh
    var mat := StandardMaterial3D.new()
    mat.albedo_color = color
    node.material_override = mat
    node.position = where
    parent.add_child(node)

func _create_interface() -> void:
    var canvas := CanvasLayer.new()
    add_child(canvas)

    var panel := PanelContainer.new()
    _main_panel = panel
    _drive_hud = Label.new()
    _drive_hud.position = Vector2(20,20)
    _drive_hud.add_theme_color_override("font_color",Color.WHITE)
    _drive_hud.add_theme_color_override("font_shadow_color",Color.BLACK)
    _drive_hud.add_theme_constant_override("shadow_offset_x",2)
    _drive_hud.add_theme_constant_override("shadow_offset_y",2)
    _drive_hud.visible = false
    canvas.add_child(_drive_hud)
    panel.position = Vector2(15.0, 15.0)
    panel.custom_minimum_size = Vector2(435.0, 0.0)
    canvas.add_child(panel)
    var column := VBoxContainer.new()
    panel.add_child(column)

    _heading = Label.new()
    _heading.text = "Living Alibi | public park"
    column.add_child(_heading)
    _status = Label.new()
    _status.text = "A visitor with 50 EUR and no assigned profession"
    column.add_child(_status)
    _weather = Label.new()
    _weather.text = "Connecting to the local Java world..."
    column.add_child(_weather)
    _needs = Label.new()
    _needs.text = "Jobs, study and travel use the real game world."
    column.add_child(_needs)
    _bag_label = Label.new()
    _bag_label.text = "Bag: empty"
    column.add_child(_bag_label)
    _housing_label = Label.new()
    _housing_label.text = "Housing: no room"
    column.add_child(_housing_label)
    _car_label = Label.new()
    _car_label.text = "Car: none"
    column.add_child(_car_label)
    var bag_row := HBoxContainer.new()
    column.add_child(bag_row)
    _eat_button = _button("Eat bread",_consume_product.bind("bread"))
    _water_button = _button("Drink bottle",_consume_product.bind("water"))
    bag_row.add_child(_eat_button)
    bag_row.add_child(_water_button)
    _eat_button.disabled = true
    _water_button.disabled = true
    _message = Label.new()
    _message.text = "No forced mission. Find your own direction."
    _message.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
    _message.custom_minimum_size = Vector2(420.0, 0.0)
    column.add_child(_message)

    var buttons := HBoxContainer.new()
    column.add_child(buttons)
    buttons.add_child(_button("Refresh", _refresh))
    buttons.add_child(_button("Jobs", _find_jobs))
    buttons.add_child(_button("Industry", _view_industry))
    buttons.add_child(_button("Business", _view_business))
    buttons.add_child(_button("Wait 60m", _wait_one_hour))
    _transport_button = _button("Transport: walking (B)",_toggle_transport)
    column.add_child(_transport_button)
    _transport_button.disabled = true
    _parking_button = _button("Bicycle rack (P)",_toggle_parking)
    column.add_child(_parking_button)
    _parking_button.disabled = true
    var map_button := Button.new()
    map_button.text = "Town map (M)"
    map_button.pressed.connect(_toggle_map)
    column.add_child(map_button)
    var clock_row := HBoxContainer.new()
    column.add_child(clock_row)
    var clock_label := Label.new()
    clock_label.text = "World time:"
    clock_row.add_child(clock_label)
    _clock_selector = OptionButton.new()
    for rate in LiveClock.SPEEDS:
        var caption := "Paused" if rate == 0 else "1:%s" % rate
        _clock_selector.add_item(caption, rate)
    _clock_selector.select(3)
    _clock_selector.item_selected.connect(_on_clock_speed_selected)
    clock_row.add_child(_clock_selector)
    var atmosphere_row := HBoxContainer.new()
    column.add_child(atmosphere_row)
    var effects_toggle := CheckButton.new()
    effects_toggle.text = "Weather FX"
    effects_toggle.button_pressed = true
    effects_toggle.toggled.connect(_set_weather_visuals)
    atmosphere_row.add_child(effects_toggle)
    var audio_toggle := CheckButton.new()
    audio_toggle.text = "Weather sound"
    audio_toggle.button_pressed = true
    audio_toggle.toggled.connect(_set_weather_audio)
    atmosphere_row.add_child(audio_toggle)
    _hint = Label.new()
    _hint.text = "WASD: walk · click scene: mouse look · Esc: cursor."
    column.add_child(_hint)
    _jobs = Label.new()
    _jobs.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
    _jobs.text = ""
    column.add_child(_jobs)

    _map_panel = PanelContainer.new()
    _map_panel.position = Vector2(15, 15)
    _map_panel.visible = false
    canvas.add_child(_map_panel)
    var map_column := VBoxContainer.new()
    _map_panel.add_child(map_column)
    var map_header := Label.new()
    map_header.text = "Living Alibi — public town map (schematic)"
    map_column.add_child(map_header)
    _town_map = preload("res://scripts/TownMap.gd").new()
    map_column.add_child(_town_map)
    var close_map := Button.new()
    close_map.text = "Close map (M)"
    close_map.pressed.connect(_toggle_map)
    map_column.add_child(close_map)

    _travel_panel = PanelContainer.new()
    _travel_panel.position = Vector2(480.0, 15.0)
    _travel_panel.custom_minimum_size = Vector2(460.0, 0.0)
    _travel_panel.visible = false
    canvas.add_child(_travel_panel)
    var travel_col := VBoxContainer.new()
    _travel_panel.add_child(travel_col)
    var travel_label := Label.new()
    travel_label.text = "Choose a destination (Java decides travel time):"
    travel_col.add_child(travel_label)
    _travel_choices = GridContainer.new()
    _travel_choices.columns = 2
    travel_col.add_child(_travel_choices)

    _dialog_panel = PanelContainer.new()
    _dialog_panel.position = Vector2(480,15)
    _dialog_panel.custom_minimum_size = Vector2(410,0)
    _dialog_panel.visible = false
    canvas.add_child(_dialog_panel)
    var dialog_col := VBoxContainer.new()
    _dialog_panel.add_child(dialog_col)
    _dialog_title = Label.new()
    dialog_col.add_child(_dialog_title)
    _dialog_attitude = Label.new()
    dialog_col.add_child(_dialog_attitude)
    _dialog_reply = Label.new()
    _dialog_reply.custom_minimum_size = Vector2(400,0)
    _dialog_reply.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
    dialog_col.add_child(_dialog_reply)
    dialog_col.add_child(_button("Say hello",_send_dialog.bind("Hello")))
    dialog_col.add_child(_button("Ask about occupation",_send_dialog.bind("What do you do?")))
    dialog_col.add_child(_button("Allow sharing my name",_send_dialog.bind("You can tell people my name.")))
    dialog_col.add_child(_button("Ask for visitor news",_send_dialog.bind("Any news about visitors?")))
    dialog_col.add_child(_button("Apologize",_send_dialog.bind("I'm sorry.")))
    dialog_col.add_child(_button("Leave me alone!",_send_dialog.bind("Leave me alone!")))
    _dialog_input = LineEdit.new()
    _dialog_input.placeholder_text = "My name is ... / Say something"
    _dialog_input.max_length = 1000
    _dialog_input.text_submitted.connect(_send_dialog)
    dialog_col.add_child(_dialog_input)
    dialog_col.add_child(_button("Speak",_send_typed_dialog))
    var close := Button.new()
    close.text = "Close conversation"
    close.pressed.connect(_close_dialog)
    dialog_col.add_child(close)

    _action_panel = PanelContainer.new()
    _action_panel.position = Vector2(480.0, 15.0)
    _action_panel.custom_minimum_size = Vector2(430.0, 0.0)
    _action_panel.visible = false
    canvas.add_child(_action_panel)
    var action_scroll := ScrollContainer.new()
    action_scroll.custom_minimum_size = Vector2(420,420)
    action_scroll.horizontal_scroll_mode = ScrollContainer.SCROLL_MODE_DISABLED
    _action_panel.add_child(action_scroll)
    _action_choices = VBoxContainer.new()
    _action_choices.size_flags_horizontal = Control.SIZE_EXPAND_FILL
    action_scroll.add_child(_action_choices)
    _action_title = Label.new()
    _action_choices.add_child(_action_title)

func _button(caption: String, callback: Callable) -> Button:
    var button := Button.new()
    button.text = caption
    button.pressed.connect(callback)
    button.add_to_group("world_api_buttons")
    return button

func _replace_choices(parent: Control, keep_header: bool = false) -> void:
    var children := parent.get_children()
    for i in range(children.size()):
        if keep_header and i == 0:
            continue
        parent.remove_child(children[i])
        children[i].queue_free()

func _nearest_resident() -> String:
    if not is_instance_valid(_residents_root):
        return ""
    var nearest := ""
    var best := 2.5
    for child in _residents_root.get_children():
        var person := child as Node3D
        if person == null or str(person.get_meta("activity","")) in ["SLEEPING","TRAVELING"]:
            continue
        var distance := _avatar.position.distance_to(person.position)
        if distance < best:
            best = distance
            nearest = str(person.get_meta("resident_id",""))
    return nearest

func _open_dialog() -> void:
    if not _connected or _api.is_busy() or _dialog_pending:
        return
    var id := _nearest_resident()
    if id.is_empty():
        _message.text = "Approach a resident to talk."
        return
    var person := _residents_root.get_node_or_null("Resident_"+id) as Node3D
    _dialog_target = id
    _travel_panel.visible = false
    _action_panel.visible = false
    _map_panel.visible = false
    _dialog_title.text = str(person.get_meta("display_name","Resident"))
    _dialog_attitude.text = "Attitude: "+str(person.get_meta("relationship","STRANGER")).capitalize()
    var known := str(person.get_meta("known_name",""))
    _dialog_reply.text = "They know you as "+known if not known.is_empty() else (
            "You have spoken before." if int(person.get_meta("conversations",0)) > 0 else "You have not met.")
    _dialog_input.clear()
    _dialog_panel.visible = true
    Input.set_mouse_mode(Input.MOUSE_MODE_VISIBLE)

func _close_dialog() -> void:
    _dialog_panel.visible = false
    _dialog_target = ""
    _dialog_input.release_focus()

func _send_typed_dialog() -> void:
    _send_dialog(_dialog_input.text)

func _send_dialog(message: String) -> void:
    if not _dialog_panel.visible or _dialog_pending or _api.is_busy() or not _connected:
        return
    var person := _residents_root.get_node_or_null("Resident_"+_dialog_target) as Node3D
    if person == null or _avatar.position.distance_to(person.position) >= 2.5:
        _close_dialog()
        return
    if message.strip_edges().is_empty():
        return
    _dialog_pending = true
    _api.speak_to(_dialog_target,message)

func _toggle_map() -> void:
    _close_dialog()
    _map_panel.visible = not _map_panel.visible
    if _map_panel.visible:
        _travel_panel.visible = false
        _action_panel.visible = false
        Input.set_mouse_mode(Input.MOUSE_MODE_VISIBLE)
        _town_map.update_world(_current_place, _locations)
        if _connected and not _api.is_busy():
            _api.list_locations()

func _toggle_travel() -> void:
    if not _near_gate() or not _connected:
        _message.text = "Approach the yellow gate with the Java server running."
        return
    _close_dialog()
    _action_panel.visible = false
    _travel_panel.visible = not _travel_panel.visible
    if _travel_panel.visible:
        Input.set_mouse_mode(Input.MOUSE_MODE_VISIBLE)
        _render_destinations()
        if not _api.is_busy():
            _api.list_locations()

func _render_destinations() -> void:
    _replace_choices(_travel_choices)
    for place in _locations:
        var id := str(place.get("id", ""))
        var minutes := int(place.get("travelMinutes", 0))
        var caption := "%s (%sm)" % [str(place.get("label", id)), str(minutes)]
        var choice := _button(caption, _travel_to.bind(id))
        choice.set_meta("destination", id)
        choice.disabled = id == _current_place or _api.is_busy()
        _travel_choices.add_child(choice)

func _travel_to(destination: String) -> void:
    if not _near_gate() or _api.is_busy() or not _connected:
        return
    _road_blocked = ""
    _travel_panel.visible = false
    _api.go_to(destination)

func _toggle_actions() -> void:
    if (not _near_kiosk() and not _near_home_station() and not _near_car() and not _near_truck()) or not _connected:
        _message.text = "Approach a local kiosk or your rented bed."
        return
    _close_dialog()
    _travel_panel.visible = false
    _action_panel.visible = not _action_panel.visible
    if _action_panel.visible:
        Input.set_mouse_mode(Input.MOUSE_MODE_VISIBLE)
        _render_actions()
        if _current_place == "WORKSHOP" and not _api.is_busy():
            _api.view_business()
        elif _current_place == "OFFICE" and not _api.is_busy():
            _api.list_work_orders()

func _render_actions() -> void:
    _replace_choices(_action_choices, true)
    _action_title.text = "Local actions — " + _current_place.capitalize()
    var player: Dictionary = _last_snapshot.get("player", {})
    var employed := str(player.get("jobId", ""))
    if _near_truck():
        var cargo: Dictionary = _last_snapshot.get("freight",{})
        _action_title.text = "Freight truck · iron %s / copper %s" % [cargo.get("iron",0),cargo.get("copper",0)]
        var next = cargo.get("nextDestination")
        if next!=null:
            _action_choices.add_child(_button("Drive truck to "+str(next).capitalize(),_drive_truck.bind(str(next))))
        else:
            _action_choices.add_child(_button("Unload ore · earn 32 EUR",_freight_action.bind("unload")))
        return
    if _near_car():
        _action_title.text = "Your car · choose adjacent destination"
        for id in player.get("carDestinations",[]):
            _action_choices.add_child(_button("Drive to %s · 3m / 1 fuel" % str(id).capitalize(),_drive_to.bind(str(id))))
        return
    match _current_place:
        "FARM":
            var report: Dictionary = _last_snapshot.get("farm",{})
            var label := Label.new()
            label.text = "Fields: vegetables %s · rice %s\nBarn: vegetables %s · rice %s\nFarm funds: %s EUR" % [
                report.get("vegetablesInField",0),report.get("riceInField",0),
                report.get("vegetablesInBarn",0),report.get("riceInBarn",0),report.get("payroll",0)]
            _action_choices.add_child(label)
            _action_choices.add_child(_button("Apply: farm worker",_apply_job.bind("farm-worker")))
        "QUARRY":
            _action_choices.add_child(_button("Apply: freight driver",_apply_job.bind("freight-driver")))
            if employed=="freight-driver" and not bool(_last_snapshot.get("freight",{}).get("active",false)):
                _action_choices.add_child(_button("Load ore truck · paid delivery",_freight_action.bind("load")))
            _action_choices.add_child(_button("Apply: quarry miner",
                    _apply_job.bind("miner")))
            if employed == "miner":
                _action_choices.add_child(_button("Survey western mineral deposit · 2h",
                        _prospect))
        "FACTORY":
            _action_choices.add_child(_button("Apply: factory machinist",
                    _apply_job.bind("machinist")))
        "CENTRO":
            _action_choices.add_child(_button("Job noticeboard",_find_jobs))
        "NORTH_RESIDENCES":
            if _near_room_bed():
                _action_choices.add_child(_button("Sleep in guest room · 2h",_sleep_room.bind(2)))
                _action_choices.add_child(_button("Sleep in guest room · 8h",_sleep_room.bind(8)))
            elif _near_kitchen():
                var bag: Dictionary = player.get("bag",{"bread":player.get("bread",0),"water":player.get("bottledWater",0)})
                var pantry: Dictionary = player.get("pantry",{"bread":player.get("storedBread",0),"water":player.get("storedWater",0)})
                var stock := Label.new()
                stock.text = "Cupboard %s/24" % _inventory_count(pantry)
                _action_choices.add_child(stock)
                for item in ["bread","water","flour","eggs","pasta","vegetables","milk","rice"]:
                    if item not in ["bread","water"] and int(bag.get(item,0))+int(pantry.get(item,0)) == 0:
                        continue
                    var line := HBoxContainer.new()
                    _action_choices.add_child(line)
                    line.add_child(_button("Store %s (%s)" % [item,bag.get(item,0)],_room_storage.bind(item,true)))
                    line.add_child(_button("Take %s (%s)" % [item,pantry.get(item,0)],_room_storage.bind(item,false)))
                _action_choices.add_child(_button("Cook bread soup · 15m (bread + water)",_room_cook.bind("bread-soup")))
                _action_choices.add_child(_button("Pasta verdure · 25m (pasta + veg + water)",_room_cook.bind("pasta-verdure")))
                _action_choices.add_child(_button("Frittata · 20m (eggs + veg)",_room_cook.bind("frittata")))
                _action_choices.add_child(_button("Pancakes · 25m (flour + eggs + milk)",_room_cook.bind("pancakes")))
                _action_choices.add_child(_button("Rice verdure · 25m (rice + veg + water)",_room_cook.bind("rice-verdure")))
            elif _near_bath():
                _action_choices.add_child(_button("Wash · 10m",_room_wash))
            elif _near_kiosk():
                _action_choices.add_child(_button("Rent / renew guest room · 35 EUR / 7 days",_rent_room))
                if _has_room_lease():
                    var instruction := Label.new()
                    instruction.text = "Your room is across the street. Walk to the bed."
                    _action_choices.add_child(instruction)
        "MARKET", "PORTICI", "FUEL_STATION":
            for item in _last_snapshot.get("shop",[]):
                var id := str(item.get("id",""))
                var stock := int(item.get("stock",0))
                var choice := _button("Buy %s · %s EUR (%s left)" % [
                    str(item.get("label",id)),str(item.get("price",0)),stock],_buy_product.bind(id))
                choice.disabled = stock <= 0
                choice.set_meta("sold_out",stock <= 0)
                _action_choices.add_child(choice)
            if _current_place == "MARKET":
                _action_choices.add_child(_button("Apply: market assistant",
                        _apply_job.bind("shop-assistant")))
            elif _current_place=="FUEL_STATION":
                var fuel: Dictionary = _last_snapshot.get("fuel",{})
                var supply := Label.new()
                supply.text = "Fuel: %s/%s · %s EUR/unit" % [fuel.get("stock",0),fuel.get("capacity",48),fuel.get("price",2)]
                _action_choices.add_child(supply)
                if _car_place()==_current_place:
                    _action_choices.add_child(_button("Refuel · up to 3 units",_car_action.bind("refuel")))
                _action_choices.add_child(_button("Apply: fuel station attendant",_apply_job.bind("fuel-attendant")))
        "WORKSHOP":
            if _car_place().is_empty():
                _action_choices.add_child(_button("Buy used car · 180 EUR",_car_action.bind("buy")))
            elif _car_place()=="WORKSHOP":
                _action_choices.add_child(_button("Refuel · up to 3 units / 2 EUR each",_car_action.bind("refuel")))
                _action_choices.add_child(_button("Service car · 15 EUR",_car_action.bind("repair")))
            if not bool(player.get("ownsBicycle",false)):
                _action_choices.add_child(_button("Buy used bicycle · 45 EUR",_buy_bicycle))
            _action_choices.add_child(_button("Apply: technician",
                    _apply_job.bind("technician")))
            var business: Dictionary = _business_report
            if not bool(business.get("registered", false)) or not bool(business.get("yours", false)):
                _action_choices.add_child(_button("Register / acquire workshop · 15 EUR",
                        _business_open))
            else:
                var cash := str(business.get("cash", 0))
                var machine := bool(business.get("machine", false))
                var label := Label.new()
                var machine_health := int(business.get("machineCondition", 0))
                label.text = "YOUR COMPANY | Bank: " + cash + " EUR | machine: " + str(machine) + " " + str(machine_health) + "/100"
                _action_choices.add_child(label)
                _action_choices.add_child(_button("Invest 10 EUR", _business_invest.bind(10)))
                _action_choices.add_child(_button("Invest 25 EUR", _business_invest.bind(25)))
                if not machine:
                    _action_choices.add_child(_button("Buy fabrication machine · 25 EUR",
                            _business_machine))
                elif machine_health < 100:
                    _action_choices.add_child(_button("Service machine · 2 parts + 10 EUR",
                            _business_service))
                if machine and str(business.get("employeeId", "")) in ["", "<null>"]:
                    for candidate in _business_candidates:
                        var id := str(candidate.get("id", ""))
                        var name := str(candidate.get("name", id))
                        _action_choices.add_child(_button("Hire " + name + " · 10 EUR bonus",
                                _business_hire.bind(id)))
                _action_choices.add_child(_button("Withdraw 10 EUR", _business_withdraw.bind(10)))
        "OFFICE":
            _action_choices.add_child(_button("Apply: engineer",
                    _apply_job.bind("engineer")))
            _action_choices.add_child(_button("Apply: investigator",
                    _apply_job.bind("investigator")))
        "SCHOOL":
            _action_choices.add_child(_button("Study engineering · 10 EUR",
                    _study.bind("engineering")))
        "LIBRARY":
            _action_choices.add_child(_button("Study investigation · 10 EUR",
                    _study.bind("investigation")))
        "RIVER":
            _action_choices.add_child(_button("Drink river water (unsafe)",
                    _drink.bind("river-water")))
        "WOODS":
            _action_choices.add_child(_button("Gather wild berries", _gather))
        "PARK":
            var utilities: Dictionary = _last_snapshot.get("services", {})
            var clean := str(utilities.get("cleanWater", 0))
            _action_choices.add_child(_button("Drink from park fountain (clean: " + clean + ")",
                    _drink.bind("park-fountain")))
            _action_choices.add_child(_button("Drink puddle water (unsafe)",
                    _drink.bind("puddle-water")))
        _:
            var no_actions := Label.new()
            no_actions.text = "No career actions here yet."
            _action_choices.add_child(no_actions)

    var work_site := ""
    match employed:
        "fuel-attendant":
            work_site = "FUEL_STATION"
        "farm-worker":
            work_site = "FARM"
        "shop-assistant":
            work_site = "MARKET"
        "miner":
            work_site = "QUARRY"
        "machinist":
            work_site = "FACTORY"
        "technician":
            work_site = "WORKSHOP"
        "engineer", "investigator":
            work_site = "OFFICE"
    if _current_place in ["WORKSHOP", "OFFICE"]:
        for order in _work_orders:
            if str(order.get("place", "")) != _current_place:
                continue
            var id := str(order.get("id", ""))
            var phase := str(order.get("phase", "OPEN"))
            var description := Label.new()
            description.text = "%s | %s | condition %s/100" % [
                str(order.get("title", id)), phase,
                str(order.get("condition", 0))
            ]
            _action_choices.add_child(description)
            if phase == "OPEN":
                _action_choices.add_child(_button("Diagnose: " + id, _diagnose.bind(id)))
            elif phase == "INSPECTED":
                _action_choices.add_child(_button("Repair / implement: " + id, _repair.bind(id)))
    if work_site == _current_place:
        _action_choices.add_child(_button("Work a paid 2-hour shift", _work))

func _business_open() -> void:
    if _near_kiosk() and not _api.is_busy():
        _action_panel.visible = false
        _api.business_open()

func _business_invest(amount: int) -> void:
    if _near_kiosk() and not _api.is_busy():
        _action_panel.visible = false
        _api.business_invest(amount)

func _business_machine() -> void:
    if _near_kiosk() and not _api.is_busy():
        _action_panel.visible = false
        _api.business_machine()

func _business_service() -> void:
    if _near_kiosk() and not _api.is_busy():
        _action_panel.visible = false
        _api.business_service()

func _business_hire(resident_id: String) -> void:
    if _near_kiosk() and not _api.is_busy():
        _action_panel.visible = false
        _api.business_hire(resident_id)

func _business_withdraw(amount: int) -> void:
    if _near_kiosk() and not _api.is_busy():
        _action_panel.visible = false
        _api.business_withdraw(amount)

func _apply_job(job_id: String) -> void:
    if _near_kiosk() and not _api.is_busy():
        _action_panel.visible = false
        _api.apply_for_job(job_id)

func _study(subject: String) -> void:
    if _near_kiosk() and not _api.is_busy():
        _action_panel.visible = false
        _api.study_subject(subject)

func _diagnose(order_id: String) -> void:
    if _near_kiosk() and not _api.is_busy():
        _action_panel.visible = false
        _api.diagnose(order_id)

func _repair(order_id: String) -> void:
    if _near_kiosk() and not _api.is_busy():
        _action_panel.visible = false
        _api.repair(order_id)

func _prospect() -> void:
    if _near_kiosk() and not _api.is_busy():
        _action_panel.visible = false
        _api.prospect()

func _work() -> void:
    if _near_kiosk() and not _api.is_busy():
        _action_panel.visible = false
        _api.work_shift()

func _room_storage(item: String,deposit: bool) -> void:
    if _near_kitchen() and _connected and not _api.is_busy():
        _api.room_storage(item,deposit)

func _room_cook(recipe_id: String) -> void:
    if _near_kitchen() and _connected and not _api.is_busy():
        _api.room_cook(recipe_id)

func _room_wash() -> void:
    if _near_bath() and _connected and not _api.is_busy():
        _api.room_wash()

func _rent_room() -> void:
    if _near_kiosk() and _connected and not _api.is_busy():
        _api.rent_room()

func _sleep_room(hours: int) -> void:
    if _near_room_bed() and _connected and not _api.is_busy():
        _api.sleep_in_room(hours)

func _buy_product(id: String) -> void:
    if _near_kiosk() and not _api.is_busy() and _connected:
        _api.buy_product(id)

func _consume_product(id: String) -> void:
    if _connected and not _api.is_busy():
        _api.consume_product(id)

func _inventory_count(items: Dictionary) -> int:
    var total := 0
    for amount in items.values():
        total += int(amount)
    return total

func _update_bag() -> void:
    var player: Dictionary = _last_snapshot.get("player",{})
    var bread := int(player.get("bread",0))
    var water := int(player.get("bottledWater",0))
    var bag: Dictionary = player.get("bag",{"bread":bread,"water":water})
    var lines := PackedStringArray()
    for id in bag:
        if int(bag[id])>0:
            lines.append("%s %s" % [str(id).capitalize(),bag[id]])
    _bag_label.text = "Bag %s/12 | %s" % [_inventory_count(bag),", ".join(lines)]
    _bag_label.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
    var remaining := maxi(0,int(player.get("roomUntil",0)) - ((int(_last_snapshot.get("day",1))-1)*1440+int(_last_snapshot.get("minute",0))))
    _housing_label.text = "Housing: North guesthouse · %s h left" % ceili(remaining/60.0) if bool(player.get("roomActive",false)) else "Housing: no room"
    _eat_button.disabled = bread == 0 or int(player.get("hunger",0)) == 0
    _water_button.disabled = water == 0 or int(player.get("thirst",0)) == 0

func _drink(source_id: String) -> void:
    if _near_kiosk() and not _api.is_busy():
        _action_panel.visible = false
        _api.drink_from(source_id)

func _gather() -> void:
    if _near_kiosk() and not _api.is_busy():
        _action_panel.visible = false
        _api.forage("wild-berries")

func _refresh() -> void:
    if not _api.is_busy():
        _api.refresh_world()

func _find_jobs() -> void:
    if not _api.is_busy():
        _api.list_jobs()

func _view_industry() -> void:
    if not _api.is_busy():
        _api.view_industry()

func _view_business() -> void:
    if not _api.is_busy():
        _api.view_business()

func _on_clock_speed_selected(index: int) -> void:
    _clock.set_speed(_clock_selector.get_item_id(index))

func _tick_live_clock(delta: float) -> void:
    if _api == null:
        return
    var eligible: bool = _connected and not _api.is_busy() and not _clock_pending \
            and _road_pending.is_empty() and not _travel_panel.visible \
            and not _action_panel.visible and not _map_panel.visible and not _dialog_panel.visible
    if _clock.advance(delta, eligible):
        _clock_pending = true
        # The existing atomic Java action advances needs, NPCs and SQLite.
        _api.wait_for(1)

func _wait_one_hour() -> void:
    if not _api.is_busy():
        _api.wait_for(60)

func _on_world_updated(snapshot: Dictionary) -> void:
    var clock_update := _clock_pending
    _clock_pending = false
    _connected = true
    var old_car_place := _car_place()
    var was_freight := _drive_freight
    _last_snapshot = snapshot
    var player: Dictionary = snapshot.get("player", {})
    var new_place := str(player.get("placeId", "PARK"))
    var truck_arrival := was_freight and _drive_pending and new_place==_drive_target and _truck_place()==new_place
    var continue_in_cabin := not was_freight and _drive_pending and not _drive_target.is_empty() and new_place==_drive_target and _car_place()==new_place
    if _drive_active() and not continue_in_cabin and (_drive_pending or new_place!=_current_place or _car_place()!=_current_place):
        _end_driving(new_place==_current_place)
    var road_arrival := new_place == _road_pending and not _road_pending.is_empty()
    if not _road_pending.is_empty():
        _road_blocked = "" if road_arrival else _road_pending
        _road_pending = ""
    if new_place != _current_place:
        _close_dialog()
        _current_place = new_place
        _render_place(new_place)
        var car_arrival := not old_car_place.is_empty() and old_car_place!=_car_place() and _car_place()==new_place
        _avatar.position = TRUCK_EXIT if truck_arrival else (CAR_ARRIVAL if car_arrival else (_road_arrival if road_arrival else Vector3.ZERO))
        if not _can_stand(_avatar.position, WALKER_RADIUS, true):
            _avatar.position = Vector3.ZERO
        _locations.clear() # Previous-origin travel estimates are now stale.
        _follow_player()
        _action_panel.visible = false
        _travel_panel.visible = false
    if continue_in_cabin:
        _park_in_cabin()

    var job = player.get("jobId")
    var profession := "unemployed" if job == null else str(job)
    _heading.text = "Living Alibi | " + str(player.get("place", "Park"))
    _status.text = "Visitor #%s | %s EUR | %s" % [
        str(player.get("incarnation", 1)), str(player.get("money", 50)), profession
    ]
    var climate: Dictionary = snapshot.get("weather", {})
    _weather.text = "Day %s  %s:%02d | %s | %s  %s °C" % [
        str(snapshot.get("day", 1)), int(snapshot.get("minute", 450))/60,
        int(snapshot.get("minute", 450))%60, str(snapshot.get("season", "SPRING")),
        str(climate.get("sky", "CLEAR")), str(climate.get("temperatureC", 0))
    ]
    _needs.text = "Hunger %s  Thirst %s  Fatigue %s  Health %s  Eng %s  Inv %s" % [
        str(player.get("hunger", 0)), str(player.get("thirst", 0)),
        str(player.get("fatigue", 0)), str(player.get("health", 100)),
        str(player.get("engineeringSkill", 0)), str(player.get("investigationSkill", 0))
    ]
    _needs.text += " | Hygiene %s" % str(player.get("hygiene",100))
    var utilities: Dictionary = snapshot.get("services", {})
    var fountain := "working" if utilities.get("fountainWorking", false) else "closed"
    var lighting := "lit" if utilities.get("lightsWorking", false) else "dark"
    _needs.text += "\nPark fountain: %s (%s clean units) | Street lights: %s" % [
        fountain, str(utilities.get("cleanWater", 0)), lighting
    ]
    _needs.text += "\nResidents present in this district: %s" % [
        str(snapshot.get("residents", []).size())
    ]
    _sync_guest_room()
    _sync_owned_car()
    _car_label.text = "Car: none" if _car_place().is_empty() else "Car: %s | Fuel %s/12 | Condition %s%%" % [_car_place().capitalize(),player.get("carFuel",0),player.get("carCondition",0)]
    _refresh_transport_buttons(false)
    _sync_bicycle()
    _follow_player()
    if _drive_active():
        _update_drive_hud()
    _update_bag()
    _town_map.update_world(_current_place, _locations)
    _render_residents()
    if _dialog_panel.visible:
        var person := _residents_root.get_node_or_null("Resident_"+_dialog_target) as Node3D
        if person == null:
            _close_dialog()
        else:
            _dialog_attitude.text = "Attitude: "+str(person.get_meta("relationship","STRANGER")).capitalize()
    _apply_weather()
    _render_actions()
    if _api != null and not _api.is_busy() and not clock_update:
        _api.list_locations()

func _set_weather_visuals(enabled: bool) -> void:
    _weather_fx.visual_enabled = enabled
    _apply_weather()

func _set_weather_audio(enabled: bool) -> void:
    _weather_fx.audio_enabled = enabled
    _apply_weather()

func _apply_weather() -> void:
    if _ground_material == null:
        return
    var climate: Dictionary = _last_snapshot.get("weather", {})
    if climate.get("snowing", false):
        _ground_material.albedo_color = Color(0.80, 0.86, 0.88)
        _environment.background_color = Color(0.72, 0.76, 0.83)
    elif climate.get("raining", false):
        _ground_material.albedo_color = Color(0.22, 0.37, 0.23)
        _environment.background_color = Color(0.44, 0.50, 0.57)
    elif climate.get("sky", "CLEAR") == "CLOUDY":
        _ground_material.albedo_color = Color(0.27,0.45,0.25)
        _environment.background_color = Color(0.60,0.65,0.71)
    else:
        _ground_material.albedo_color = Color(0.27, 0.49, 0.26)
        _environment.background_color = Color(0.63, 0.80, 0.96)
    var minute := int(_last_snapshot.get("minute", 450))
    var night := minute < 360 or minute >= 1200
    # Approximate solar arc from the committed world minute, never wall time.
    var day_progress := clampf((float(minute)-360.0)/840.0,0.0,1.0)
    var daylight := sin(day_progress * PI)
    _sun.rotation_degrees = Vector3(-8.0-65.0*daylight,
            -90.0+180.0*day_progress,0.0)
    _sun.light_energy = lerpf(0.12,1.1,daylight)
    _environment.ambient_light_energy = lerpf(0.25,1.0,daylight)
    _environment.background_color = Color(0.07,0.10,0.18).lerp(
            _environment.background_color, smoothstep(0.0,0.35,daylight))
    if climate.get("sky","CLEAR") != "CLEAR":
        _sun.light_energy *= 0.65
    _weather_fx.follow(_avatar.position)
    _weather_fx.set_sheltered(_inside_guest_room(),_environment)
    _weather_fx.apply(climate,_environment)
    var services: Dictionary = _last_snapshot.get("services", {})
    # Weather and city services affect every loaded chunk, not just the
    # one containing the visitor. Only the active chunk gets NPC data.
    for chunk in _chunks.values():
        var ground: StandardMaterial3D = chunk["ground"]
        ground.albedo_color = _ground_material.albedo_color
        for lamp in chunk["lamps"]:
            lamp.visible = night and bool(services.get("lightsWorking", false))
        var water: MeshInstance3D = chunk["water"]
        if is_instance_valid(water):
            water.visible = bool(services.get("fountainWorking", false)) and int(services.get("cleanWater", 0)) > 0

func _on_locations_updated(locations: Array) -> void:
    _locations = locations
    _town_map.update_world(_current_place, _locations)
    if _travel_panel.visible:
        _render_destinations()

func _on_job_offers_updated(jobs: Array) -> void:
    _job_offers = jobs
    var lines := PackedStringArray(["Available careers (travel to workplace to apply):"])
    for offer in jobs:
        lines.append("%s | %s EUR/shift | %s level %s" % [
            str(offer.get("title", "?")), str(offer.get("wage", 0)),
            str(offer.get("subject", "none")), str(offer.get("requiredSkill", 0))
        ])
    _jobs.text = "\n".join(lines)

func _on_business_updated(report: Dictionary) -> void:
    _business_report = report
    var employee := str(report.get("employeeName", "unemployed"))
    var state := "NOT REGISTERED"
    if bool(report.get("registered", false)):
        state = "OWNED" if bool(report.get("yours", false)) else "SOMEONE ELSE'S BUSINESS"
    _jobs.text = "SMALL WORKSHOP: %s | bank %s EUR | machine %s (%s/100)\n" % [
        state, str(report.get("cash", 0)), str(report.get("machine", false)),
        str(report.get("machineCondition", 0))
    ]
    _jobs.text += "Employee %s | parts in stock %s | paid hours today %s" % [
        employee, str(report.get("spareParts", 0)), str(report.get("paidHoursToday", 0))
    ]
    if _action_panel.visible and _current_place == "WORKSHOP":
        _render_actions()
        if not _api.is_busy():
            _api.business_candidates()

func _on_business_candidates_updated(candidates: Array) -> void:
    _business_candidates = candidates
    if _action_panel.visible and _current_place == "WORKSHOP":
        _render_actions()
        if not _api.is_busy():
            _api.list_work_orders()

func _on_industry_updated(report: Dictionary) -> void:
    _jobs.text = "QUARRY: iron ore %s, copper ore %s | mined cargo %s\n" % [
        str(report.get("ironRemaining", 0)), str(report.get("copperRemaining", 0)),
        str(report.get("oreAtQuarry", 0))
    ]
    _jobs.text += "FACTORY: ore %s, steel %s, copper %s, spare parts %s\n" % [
        str(report.get("oreAtFactory", 0)), str(report.get("factorySteel", 0)),
        str(report.get("factoryCopper", 0)), str(report.get("factoryParts", 0))
    ]
    _jobs.text += "REPAIRS: steel %s, copper %s, spare parts %s\n" % [
        str(report.get("workshopSteel", 0)), str(report.get("workshopCopper", 0)),
        str(report.get("workshopParts", 0))
    ]
    _jobs.text += "Working now: miners %s, machinists %s\n" % [
        str(report.get("minersAtWork", 0)), str(report.get("factoryWorkersAtWork", 0))
    ]
    _jobs.text += "Freight on truck %s | Scrap %s | West seam surveyed: %s\n" % [
        str(report.get("oreOnTruck", 0)), str(report.get("scrapRemaining", 0)),
        str(report.get("westDepositDiscovered", false))
    ]
    _jobs.text += "Enterprise budgets: Quarry %s EUR | Factory %s EUR | City %s EUR" % [
        str(report.get("quarryPayroll", 0)), str(report.get("factoryPayroll", 0)),
        str(report.get("cityBudget", 0))
    ]

func _on_work_orders_updated(orders: Array) -> void:
    _work_orders = orders
    if _action_panel.visible:
        _render_actions()

func _on_action_feedback(message: String) -> void:
    if _dialog_pending:
        _dialog_pending = false
        if _dialog_panel.visible:
            _dialog_reply.text = message
            _dialog_input.clear()
    if not _clock_pending:
        _message.text = message

func _on_request_started(route: String) -> void:
    if route.begins_with("/actions/") and not _clock_pending:
        _clock.reset()
    _set_request_busy(true)

func _on_request_finished(_route: String) -> void:
    _set_request_busy(false)

func _refresh_transport_buttons(busy: bool) -> void:
    var owns := bool(_last_snapshot.get("player",{}).get("ownsBicycle",false))
    var parked := _parked_district()
    _transport_button.text = "Bicycle parked: "+parked.capitalize() if not parked.is_empty() else (
        "Transport: cycling (B)" if _cycling() else "Transport: walking (B)")
    _transport_button.disabled = busy or not owns or not parked.is_empty()
    _parking_button.text = "Retrieve bicycle (P)" if not parked.is_empty() else "Park bicycle (P)"
    _parking_button.disabled = busy or not owns or (not parked.is_empty() and parked != _current_place)

func _set_request_busy(busy: bool) -> void:
    for node in get_tree().get_nodes_in_group("world_api_buttons"):
        if node is Button:
            var same_place := node.has_meta("destination") and (
                    str(node.get_meta("destination")) == _current_place)
            node.disabled = busy or same_place or bool(node.get_meta("sold_out",false))
    _refresh_transport_buttons(busy)
    if not busy:
        _update_bag()

func _on_request_failed(message: String) -> void:
    if _drive_pending:
        _end_driving(true)
        _connected = false
        _message.text = "Trip response uncertain. Refresh world before retrying: "+message
        return
    if _dialog_pending:
        _dialog_pending = false
        if _dialog_panel.visible:
            _dialog_reply.text = message
    if _clock_pending:
        # The server might have committed before a connection failure.
        # Pause rather than automatically retry a potentially committed write.
        _clock_pending = false
        _clock.set_speed(0)
        _clock_selector.select(0)
        _message.text = "Live time paused. Refresh world before resuming: " + message
        return
    if not _road_pending.is_empty():
        _road_blocked = _road_pending
        _road_pending = ""
    _message.text = "Java server: " + message
