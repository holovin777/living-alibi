extends Node3D
## Living Alibi vertical slice: move around a local public scene; travel and
## careers are validated by the real Java/Spring server, never by a fake client wallet.
## WASD/arrow keys: walk relative to the camera. Click the world for mouse look.
## First-person only. Escape: release mouse. E: gate. F: local kiosk.

const GATE_POSITION := Vector3(0.0, 0.0, -12.0)
const KIOSK_POSITION := Vector3(7.0, 0.0, 3.0)
const DEPOT_DESK := Vector3(-9.0,0.0,10.0)
const DEPOT_BAYS := [Vector3(6.5,0,12.0),Vector3(12.5,0,12.0)]
const INTERACTION_RADIUS := 3.5
const WALK_SPEED := 4.5
const CYCLE_SPEED := 8.0
const RACK_POSITION := Vector3(6.0,0,-10.0)
const CAR_PARK := Vector3(11.0,0,-10.0)
const CAR_ARRIVAL := Vector3(11.0,0,-7.5)
const TRUCK_PARK := Vector3(14.5,0,-10.0)
const TRUCK_EXIT := Vector3(14.5,0,-7.0)
const FREIGHT_DISTRICTS := ["QUARRY","WORKSHOP","FACTORY","FARM","BAKERY","CAFE","SOUTH_RESIDENCES","MARKET"]
const RACK_DISTRICTS := ["PARK","WORKSHOP","NORTH_RESIDENCES"]
const WALKER_RADIUS := 0.32
const COLLISION_STEP := 0.12
const ROAD_EDGE := 24.0
const ROAD_HALF_WIDTH := 2.0
const ROAD_TRIGGER := 23.8
const TownLayout = preload("res://scripts/TownMap.gd")
const MOUSE_SENSITIVITY := 0.0025
const EYE_HEIGHT := 1.65
const GARDEN_CENTER := Vector3(11,0,11)
const COOP_CENTER := Vector3(-10,0,-10)
const ORCHARD_CENTER := Vector3(7.5,0,-9.5)
const ORCHARD_PLOTS := [Vector3(5.5,0,-12),Vector3(9.5,0,-12),Vector3(5.5,0,-7),Vector3(9.5,0,-7)]
const ROOM_BOUNDS := Rect2(-13.0,4.0,9.0,10.0)
const ROOM_EXIT := Vector3(-3.0,0,8.5)
const BED_POSITION := Vector3(-11.3,0,6.1)
const KITCHEN_POSITION := Vector3(-5.05,0,12.3)
const BATH_POSITION := Vector3(-12.1,0,12.95)
const LiveClock = preload("res://scripts/LiveClock.gd")
const DriveControls = preload("res://scripts/DriveControls.gd")
const DRIVER_EYE := Vector3(-0.33,1.24,0.08)
const WeatherEffects = preload("res://scripts/WeatherEffects.gd")
const TownVisuals = preload("res://scripts/TownVisuals.gd")
var _sky_material: ProceduralSkyMaterial
var _surface_textures: Dictionary = {}
const FamilyApartment = preload("res://scripts/FamilyApartment.gd")
const GameLanguage = preload("res://scripts/GameLanguage.gd")
const ResidentPersonality = preload("res://scripts/ResidentPersonality.gd")
var _guide_label: Label
var _guide_enabled := true
var _apartment: Node3D
var _apartment_active := false
var _home_entrance: Node3D
var _home_transition := false
var _grocery_menu := false
const GROCERY_COUNTER := Vector3(7.3,0,3.8)
const BAKERY_COUNTER := Vector3(-6.3,0,7.0)
const CAFE_COUNTER := Vector3(-7.0,0,5.9)
const CAFE_TABLE := Vector3(-7.0,0,9.0)
var _cafe_menu := false
var _medical_menu := false
var _college_menu := false
var _engineering_menu := false
var _software_menu := false
var _office_app := "work"
var _office_drafts: Dictionary = {}
# Presentation only: a real co-present worker gets a stable comic personality.
var _office_joker_id := ""
var _office_joke_index := 0
var _office_joke_wait := 8.0
var _office_joke_hold := 0.0
const OFFICE_JOKE_LINES := [
    "WHO MERGED ON A FRIDAY?!",
    "It works on my machine. My machine is a genius!",
    "Coffee first. Then we debug reality.",
    "I fixed one bug and adopted three more!",
    "Attention! The build is green! Nobody move!",
    "The server needs a holiday. So do I."
]
const JAVA_DESK := Vector3(-10,0,4)
const DEVOPS_DESK := Vector3(-10,0,8)
const COMPANY_DESK := Vector3(-10,0,12)
const OFFICE_STAFF_ANCHOR := Vector3(-11.6,0,4.9)
const COLLEGE_DESK := Vector3(-6.3,0,7)
const CLINIC_COUNTER := Vector3(-6.3,0,7)
const PHARMACY_COUNTER := Vector3(6.3,0,-7)

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
var _aid_label: Label
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
var _dialog_style: Label
var _language_selector: OptionButton
var _dialog_input: LineEdit
var _dialog_target := ""
var _dialog_pending := false
var _action_title: Label
var _action_choices: VBoxContainer

func _ready() -> void:
    GameLanguage.install()
    GameLanguage.select_locale(GameLanguage.saved_locale(),false)
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
                relative_motion * delta * _movement_speed(), WALKER_RADIUS, _can_walk_roads())
        if _movement_speed() == CYCLE_SPEED:
            _bike_wheel_angle += before.distance_to(_avatar.position)/0.35
        _follow_player()
        if _can_walk_roads():
            _try_road_crossing(relative_motion)

    if _apartment_active:
        _update_first_steps()
        _animate_residents(delta)
        _weather_fx.follow(_avatar.position)
        _weather_fx.set_sheltered(true,_environment)
        var action := _apartment_action()
        _hint.text = "E: "+action.capitalize()+" · F: home / life choices" if not action.is_empty() else "WASD: walk indoors · F: life choices"
        return
    _sync_bicycle()
    _advance_traffic(delta)
    _animate_residents(delta)
    _tick_office_joker(delta)
    _weather_fx.follow(_avatar.position)
    _weather_fx.set_sheltered(_apartment_active or _inside_guest_room(),_environment)
    if _rebirth_active():
        _update_first_steps()
        if _near_software():
            _hint.text="E: Borgo Digital · "+_office_station()+" · F: life choices"
        elif _near_engineering():
            _hint.text="E: engineering work orders · F: life choices"
        elif _near_college():
            _hint.text="E: Istituto Leonardo · F: life choices"
        elif not _medical_station().is_empty():
            _hint.text="E: "+("Farmacia" if _medical_station()=="first-aid" else "Ambulatorio")+" · F: life choices"
        elif not _cafe_station().is_empty():
            _hint.text="E: Bar Centrale · "+_cafe_station()+" · F: life choices"
        elif _near_grocery_counter():
            _hint.text="E: "+str(_last_snapshot["rebirth"]["grocery"].get("name","Shop"))+" · F: life choices"
        elif _near_family_entrance():
            _hint.text="E: enter your home · F: life choices"
        elif _can_walk_roads() and not _road_destination(_avatar.position,ROAD_TRIGGER).is_empty():
            _hint.text="Walk forward to the next district"
        else:
            _hint.text = "F: life choices · WASD: local exploration" if _child_can_walk() else "Childhood · follow the family schedule"
        return
    _tick_live_clock(delta)
    _update_first_steps()
    if not _near_gate():
        _travel_panel.visible = false
    if not _rebirth_active() and not _near_kiosk() and not _near_home_station() and not _near_car() and not _near_truck():
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
    elif _near_orchard():
        _hint.text = "F: care for the fruit orchard."
    elif _near_coop():
        _hint.text = "F: care for chickens or collect eggs."
    elif _near_garden():
        _hint.text = "F: plant, water or harvest your garden."
    elif _near_home_station():
        _hint.text = "F: use your bed, kitchen or bathroom."
    elif _current_place == "NORTH_RESIDENCES" and _avatar.position.distance_to(ROOM_EXIT) < 2.3:
        _hint.text = "Your room is open." if _has_room_lease() else "Rent a room at reception (blue kiosk)."
    elif _near_gate():
        _hint.text = tr("E: travel gate (game time). Esc: cursor.")
    elif _near_kiosk():
        _hint.text = tr("F: local interactions. Esc: cursor.")
    elif not _nearest_resident().is_empty():
        _hint.text = "T: talk to nearby resident · Esc: cursor."
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
    if _rebirth_active():
        if event is InputEventKey and event.pressed and not event.echo and _child_can_walk():
            if event.keycode == KEY_E and _apartment_active and not _action_panel.visible:
                var action := _apartment_action()
                if action=="door":
                    _apartment.toggle_entrance(_avatar.position)
                elif action=="exit":
                    _request_home_transition("rebirth-home-exit")
                elif not action.is_empty():
                    _garden_action("rebirth-home-"+action)
                get_viewport().set_input_as_handled()
            elif event.keycode == KEY_E and _near_software() and not _action_panel.visible:
                _software_menu=true
                _render_actions()
                _action_panel.visible=true
                Input.set_mouse_mode(Input.MOUSE_MODE_VISIBLE)
                get_viewport().set_input_as_handled()
            elif event.keycode == KEY_E and _near_engineering() and not _action_panel.visible:
                _engineering_menu=true
                _render_actions()
                _action_panel.visible=true
                Input.set_mouse_mode(Input.MOUSE_MODE_VISIBLE)
                get_viewport().set_input_as_handled()
            elif event.keycode == KEY_E and _near_college() and not _action_panel.visible:
                _college_menu=true
                _render_actions()
                _action_panel.visible=true
                Input.set_mouse_mode(Input.MOUSE_MODE_VISIBLE)
                get_viewport().set_input_as_handled()
            elif event.keycode == KEY_E and not _medical_station().is_empty() and not _action_panel.visible:
                _medical_menu=true
                _render_actions()
                _action_panel.visible=true
                Input.set_mouse_mode(Input.MOUSE_MODE_VISIBLE)
                get_viewport().set_input_as_handled()
            elif event.keycode == KEY_E and not _cafe_station().is_empty() and not _action_panel.visible:
                _cafe_menu=true
                _render_actions()
                _action_panel.visible=true
                Input.set_mouse_mode(Input.MOUSE_MODE_VISIBLE)
                get_viewport().set_input_as_handled()
            elif event.keycode == KEY_E and _near_grocery_counter() and not _action_panel.visible:
                _grocery_menu=true
                _render_actions()
                _action_panel.visible=true
                Input.set_mouse_mode(Input.MOUSE_MODE_VISIBLE)
                get_viewport().set_input_as_handled()
            elif event.keycode == KEY_E and _near_family_entrance() and not _action_panel.visible:
                _request_home_transition("rebirth-home-go")
                get_viewport().set_input_as_handled()
            elif event.keycode == KEY_F:
                _grocery_menu=false
                _cafe_menu=false
                _medical_menu=false
                _college_menu=false
                _engineering_menu=false
                _software_menu=false
                _render_actions()
                _action_panel.visible = not _action_panel.visible
                Input.set_mouse_mode(Input.MOUSE_MODE_VISIBLE if _action_panel.visible else Input.MOUSE_MODE_CAPTURED)
                get_viewport().set_input_as_handled()
            elif event.keycode == KEY_ESCAPE:
                _action_panel.visible = true
                Input.set_mouse_mode(Input.MOUSE_MODE_VISIBLE)
                get_viewport().set_input_as_handled()
        return
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
        elif event.keycode == KEY_E and not _map_panel.visible:
            if _near_gate():
                _toggle_travel()
            else:
                _message.text=tr("Find the yellow TRAVEL gate. Follow the distance and direction in First steps.")
            get_viewport().set_input_as_handled()
        elif event.keycode == KEY_F and not _map_panel.visible:
            _toggle_actions()
            get_viewport().set_input_as_handled()

func _set_guide_enabled(enabled: bool) -> void:
    _guide_enabled=enabled
    _update_first_steps()

func _guide_direction(target: Vector3) -> String:
    var offset := target-_avatar.position
    var distance := Vector2(offset.x,offset.z).length()
    if distance<INTERACTION_RADIUS:
        return tr("nearby")
    var local := Basis(Vector3.UP,-_view_yaw)*offset
    var angle := atan2(local.x,-local.z)
    var sector := posmod(roundi(angle/(PI/4.0)),8)
    var arrows := ["↑","↗","→","↘","↓","↙","←","↖"]
    return "%s %s m" % [arrows[sector],roundi(distance)]

func _update_first_steps() -> void:
    if not is_instance_valid(_guide_label):return
    _guide_label.visible=_guide_enabled and not _drive_active()
    if not _guide_enabled:return
    if not _connected:
        _guide_label.text=tr("First steps: connecting to the world...")
        return
    if _rebirth_active():
        _guide_label.text=tr("Your life: F opens choices. Follow signs to shops, work and home.")
        return
    var p: Dictionary=_last_snapshot.get("player",{})
    var job = p.get("jobId")
    var guide := ""
    if job==null or str(job).is_empty():
        if _current_place=="MARKET":
            guide=tr("Optional goal: find the blue SERVICES kiosk %s. Press F and choose Apply: market assistant.") % _guide_direction(KIOSK_POSITION)
        else:
            guide=tr("Optional goal: earn your first money. Walk to yellow TRAVEL gate %s, press E and choose Market.") % _guide_direction(GATE_POSITION)
    else:
        var workplaces := {"shop-assistant":"MARKET","technician":"WORKSHOP","engineer":"OFFICE","investigator":"OFFICE","miner":"QUARRY","machinist":"FACTORY","journalist":"LIBRARY","farm-worker":"FARM","fuel-attendant":"FUEL_STATION"}
        var site := str(workplaces.get(str(job),""))
        if site==_current_place:
            guide=tr("Your job: blue SERVICES kiosk %s. Press F for work actions during working hours.") % _guide_direction(KIOSK_POSITION)
        elif not site.is_empty():
            guide=tr("Your job: %s. Travel gate %s (E).") % [str(TownLayout.LABELS.get(site,site)),_guide_direction(GATE_POSITION)]
        else:
            guide=tr("Explore freely: use the blue SERVICES kiosk for local actions.")
    _guide_label.text=guide+"\n"+(tr("Travel E: %s · Services F: %s · Map M · Talk T") % [_guide_direction(GATE_POSITION),_guide_direction(DEPOT_DESK if _current_place=="LOGISTICS_DEPOT" else KIOSK_POSITION)])

func _near_gate() -> bool:
    return _avatar.position.distance_to(GATE_POSITION) < INTERACTION_RADIUS

func _near_kiosk() -> bool:
    var target := DEPOT_DESK if _current_place=="LOGISTICS_DEPOT" else KIOSK_POSITION
    return _avatar.position.distance_to(target) < INTERACTION_RADIUS

func _owns_house() -> bool:
    return bool(_last_snapshot.get("player",{}).get("ownsHouse",false))

func _has_room_lease() -> bool:
    return _owns_house() if _current_place=="SOUTH_RESIDENCES" else bool(_last_snapshot.get("player",{}).get("roomActive",false))

func _inside_guest_room() -> bool:
    return not _apartment_active and _current_place in ["NORTH_RESIDENCES","SOUTH_RESIDENCES"] and ROOM_BOUNDS.has_point(
            Vector2(_avatar.position.x,_avatar.position.z))

func _near_room_bed() -> bool:
    return _inside_guest_room() and _has_room_lease() and _avatar.position.distance_to(BED_POSITION) < 2.5

func _near_kitchen() -> bool:
    return _inside_guest_room() and _has_room_lease() and _avatar.position.x > -7.9 and _avatar.position.distance_to(KITCHEN_POSITION)<2.5

func _near_bath() -> bool:
    return _inside_guest_room() and _has_room_lease() and _avatar.position.z > 11.0 and _avatar.position.x < -8.0 and _avatar.position.distance_to(BATH_POSITION)<2.5

func _near_garden() -> bool:
    return _current_place=="SOUTH_RESIDENCES" and _owns_house() and _avatar.position.distance_to(GARDEN_CENTER)<3.3

func _near_coop() -> bool:
    return _current_place=="SOUTH_RESIDENCES" and _owns_house() and _avatar.position.distance_to(COOP_CENTER)<4.5

func _near_orchard() -> bool:
    return _current_place=="SOUTH_RESIDENCES" and _owns_house() and _avatar.position.distance_to(ORCHARD_CENTER)<4.5

func _near_home_station() -> bool:
    return _near_room_bed() or _near_kitchen() or _near_bath() or _near_garden() or _near_coop() or _near_orchard()

func _sync_guest_room() -> void:
    for district in ["NORTH_RESIDENCES","SOUTH_RESIDENCES"]:
        if not _chunks.has(district):
            continue
        var root: Node3D = _chunks[district]["root"]
        var door := root.get_node_or_null("CasaOspiti/LeaseDoor" if district=="NORTH_RESIDENCES" else "CasaDelGlicine/LeaseDoor") as Node3D
        if door != null:
            door.visible = not (_owns_house() if district=="SOUTH_RESIDENCES" else bool(_last_snapshot.get("player",{}).get("roomActive",false)))
    if _inside_guest_room() and not _has_room_lease():
        _avatar.position = ROOM_EXIT
        _action_panel.visible = false
        _follow_player()
        _message.text = "This home is locked. Visit the local property office or reception."

func _cycling() -> bool:
    var p: Dictionary = _last_snapshot.get("player",{})
    return bool(p.get("ownsBicycle",false)) and bool(p.get("cycling",false)) and _parked_district().is_empty()

func _movement_speed() -> float:
    # A portable bicycle is carried at walking speed in the rented studio
    # and grocery interior. No vehicle parking state is simulated yet.
    var at := _avatar.position
    var in_shop := _current_place == "PORTICI" and Rect2(4.4,2.15,6.1,5.7).has_point(Vector2(at.x,at.z))
    if _rebirth_active() and str(_last_snapshot.get("rebirth",{}).get("phase","")) != "LINKED_ADULT":
        return WALK_SPEED * 0.65
    return CYCLE_SPEED if _cycling() and not _inside_guest_room() and not in_shop else WALK_SPEED

func _truck_place() -> String:
    var value = _last_snapshot.get("freight",{}).get("truckPlace")
    return "" if value==null else str(value)

func _truck_parking(id: String) -> Vector3:
    return Vector3(15,0,2.5) if id=="FARM" else TRUCK_PARK

func _truck_exit(id: String) -> Vector3:
    return _truck_parking(id)+Vector3(0,0,3.0)

func _near_truck() -> bool:
    return not bool(_last_snapshot.get("freight",{}).get("automated",false)) and _truck_place()==_current_place and _current_place in FREIGHT_DISTRICTS and _avatar.position.distance_to(_truck_parking(_current_place))<3.5

func _hire_driver(id: String) -> void:
    if _connected and not _api.is_busy() and _current_place in ["WORKSHOP","LOGISTICS_DEPOT"] and _near_kiosk():
        _api.freight_action("hire-driver",id)

func _hire_second_driver(id: String) -> void:
    if _connected and not _api.is_busy() and _current_place in ["WORKSHOP","LOGISTICS_DEPOT"] and _near_kiosk():
        _api.freight_action("hire-driver-2",id)

func _freight_action(action: String) -> void:
    if not _connected or _api.is_busy():
        return
    if (action in ["load","load-company"] and _current_place in ["QUARRY","FACTORY","FARM"] and _near_kiosk()) or (action=="unload" and _near_truck()) or (action in ["register","invest-25","invest-100","withdraw-25","buy-truck","auto-on","auto-off","service-truck","buy-truck-2","auto-on-2","auto-off-2","service-truck-2"] and _current_place in ["WORKSHOP","LOGISTICS_DEPOT"] and _near_kiosk()):
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

func _cargo_caption(cargo: Dictionary) -> String:
    return "%s %s · %s %s" % [cargo.get("firstLabel","Iron"),cargo.get("firstUnits",cargo.get("iron",0)),cargo.get("secondLabel","Copper"),cargo.get("secondUnits",cargo.get("copper",0))]

func _update_drive_hud() -> void:
    var player: Dictionary = _last_snapshot.get("player",{})
    if _drive_freight:
        var cargo: Dictionary = _last_snapshot.get("freight",{})
        _drive_hud.text = "FREIGHT | %s km/h | %s\nTo %s · W/S gas/reverse · A/D steer · Space brake\n%s" % [roundi(absf(_drive.speed)*3.6),_cargo_caption(cargo),_drive_target.capitalize(),"Awaiting arrival..." if _drive_pending else "Esc: cancel this leg"]
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
        _avatar.position = _truck_exit(_current_place) if was_freight else CAR_ARRIVAL
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

func _depot_truck_present(number: int) -> bool:
    var logistics: Dictionary = _last_snapshot.get("logistics",{})
    if number==0:
        return bool(logistics.get("truck",false)) and str(logistics.get("truckPlace",""))=="LOGISTICS_DEPOT"
    var second: Dictionary = logistics.get("secondTruck",{})
    return bool(second.get("purchased",false)) and str(second.get("place",""))=="LOGISTICS_DEPOT"

func _sync_owned_car() -> void:
    if _chunks.has("LOGISTICS_DEPOT"):
        var depot: Node3D = _chunks["LOGISTICS_DEPOT"]["root"]
        for n in range(2):
            var model := depot.get_node_or_null("DepositoLogistico/Garage/Truck"+str(n+1)) as Node3D
            if model!=null:
                model.visible = _depot_truck_present(n)
    for id in TownLayout.MOTOR_DISTRICTS:
        if not _chunks.has(id):
            continue
        var root: Node3D = _chunks[id]["root"]
        var car := root.get_node_or_null("PublicCarPark/OwnedCar") as Node3D
        if car!=null:
            car.visible = _car_place()==id and (not _drive_active() or _drive_freight or id!=_current_place)
    for id in FREIGHT_DISTRICTS:
        if not _chunks.has(id):
            continue
        var root: Node3D = _chunks[id]["root"]
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
        var eye := EYE_HEIGHT
        if str(_last_snapshot.get("rebirth",{}).get("phase","ADULT")) == "CHILDHOOD":
            eye = lerpf(0.65,EYE_HEIGHT,clampf(float(_last_snapshot.get("rebirth",{}).get("age",0))/18.0,0.0,1.0))
        _camera.position = _avatar.position + Vector3(0.0,eye,0.0)
        _camera.rotation = Vector3(_view_pitch,_view_yaw,0.0)

func _build_world() -> void:
    _environment = Environment.new()
    _surface_textures={"grass":TownVisuals.texture("grass"),"paving":TownVisuals.texture("paving"),"asphalt":TownVisuals.texture("asphalt")}
    _sky_material=ProceduralSkyMaterial.new()
    var sky := Sky.new()
    sky.sky_material=_sky_material
    _environment.sky=sky
    _environment.background_mode = Environment.BG_SKY
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
    _sun.directional_shadow_max_distance=65.0
    _sun.shadow_blur=1.2
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
    _ground_material.albedo_color = Color(0.37, 0.50, 0.29)
    TownVisuals.surface(_ground_material,_surface_textures["grass"],0.32)
    var ground := BoxMesh.new()
    ground.size = Vector3(48.0, 0.3, 48.0)
    var floor := MeshInstance3D.new()
    floor.mesh = ground
    floor.material_override = _ground_material
    floor.position.y = -0.2
    _scenery.add_child(floor)

    _box(_scenery, Vector3(3.0, 0.05, 35.0), Color(0.63, 0.57, 0.45),
            Vector3(0.0, -0.015, 0.0),"paving")
    _create_gate_and_kiosk()
    _residents_root = Node3D.new()
    _residents_root.name = "VisibleResidents"
    _scenery.add_child(_residents_root)

    match place_id:
        "LOGISTICS_DEPOT":
            _build_logistics_depot()
        "FUEL_STATION":
            _build_fuel_station()
        "FARM":
            _build_farm()
        "NORTH_RESIDENCES":
            _build_guesthouse()
            _build_construction_site()
            _build_lombardy_neighborhood()
        "SOUTH_RESIDENCES":
            _build_guesthouse(true)
            _build_home_garden()
            _build_home_coop()
            _build_orchard()
        "PORTICI":
            _build_portici()
        "CENTRO":
            _build_centro()
        "PARK":
            _build_park()
        "CLINIC":
            _build_medical_building(false)
            _build_medical_building(true)
        "CAFE":
            _build_cafe()
        "BAKERY":
            _build_bakery()
        "MARKET":
            _building("Market stalls", Color(0.68, 0.39, 0.22))
            _box(_scenery, Vector3(6.0, 0.18, 3.0), Color(0.83, 0.20, 0.18),
                    Vector3(-7.0, 2.35, 3.0))
            _box(_scenery, Vector3(6.0, 0.18, 3.0), Color(0.90, 0.76, 0.31),
                    Vector3(4.0, 2.35, 8.0))
        "SCHOOL":
            _build_college()
            _build_dojo()
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
            _build_digital_office()
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
        _build_freight_parking(place_id)
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
                direction * 16.5 + Vector3(0,0.025,0),"asphalt" if motor else "paving")
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
    for x in [-3.35,3.35]:
        _box(lane,Vector3(0.28,0.10,15),Color("#b5b0a3"),Vector3(x,0.05,16.5))
    for x in [-4.0,4.0]:
        _box(lane,Vector3(1.0,0.08,15),Color("#aaa294"),Vector3(x,0.04,16.5),"paving")
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
    if _rebirth_active():
        _api.garden_action("rebirth-road-"+destination)
    else:
        _api.go_to(destination)

# A flat, client-side collision layer. The Java server owns district travel
# and never trusts these local visual positions as proof of an action.
func _add_obstacle(center: Vector3, size: Vector2) -> void:
    _obstacles.append(Rect2(Vector2(center.x, center.z) - size * 0.5, size))

func _can_stand(at: Vector3, radius: float, allow_roads: bool = false) -> bool:
    if _apartment_active:
        return _apartment.can_stand(at,radius)
    if absf(at.x) > 17.0 or absf(at.z) > 17.0:
        if not allow_roads or _road_destination(at, 17.0).is_empty():
            return false
    var p := Vector2(at.x, at.z)
    if _current_place=="SOUTH_RESIDENCES" and _owns_house():
        var plants: Array = _last_snapshot.get("orchard",{}).get("plants",[])
        for n in range(mini(plants.size(),4)):
            var plot: Vector3 = ORCHARD_PLOTS[n]
            if bool(plants[n].get("planted",false)) and p.distance_to(Vector2(plot.x,plot.z))<radius+0.30:
                return false
    if _current_place=="SOUTH_RESIDENCES" and _owns_house() and bool(_last_snapshot.get("hens",{}).get("coop",false)):
        if Rect2(Vector2(COOP_CENTER.x-2.6,COOP_CENTER.z-2.4),Vector2(5.2,4.8)).grow(radius).has_point(p):
            return false
    if _current_place=="SOUTH_RESIDENCES" and _owns_house() and bool(_last_snapshot.get("garden",{}).get("barrel",false)):
        var barrel_at := Vector2(GARDEN_CENTER.x+2.85,GARDEN_CENTER.z)
        if p.distance_to(barrel_at)<0.43+radius:
            return false
    if _current_place in ["NORTH_RESIDENCES","SOUTH_RESIDENCES"] and not _has_room_lease() and ROOM_BOUNDS.grow(radius).has_point(p):
        return false
    for obstacle in _obstacles:
        var nearest := p.clamp(obstacle.position, obstacle.end)
        if p.distance_squared_to(nearest) < radius * radius:
            return false
    if _current_place=="LOGISTICS_DEPOT":
        for n in range(2):
            var bay: Vector3 = DEPOT_BAYS[n]
            if _depot_truck_present(n) and Rect2(Vector2(bay.x-1.1,bay.z-2.4),Vector2(2.2,4.8)).grow(radius).has_point(p):
                return false
    if _truck_place()==_current_place and _current_place in FREIGHT_DISTRICTS:
        var truck_at := _truck_parking(_current_place)
        if Rect2(Vector2(truck_at.x-1.1,truck_at.z-2.4),Vector2(2.2,4.8)).grow(radius).has_point(p):
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

func _build_freight_parking(place_id: String) -> void:
    var lot := Node3D.new()
    lot.name = "FreightParking"
    lot.position = _truck_parking(place_id)
    _scenery.add_child(lot)
    _box(lot,Vector3(3,0.045,5.8),Color("#505659"),Vector3.ZERO)
    _sign(lot,"Merci · F",Vector3(0,2.7,-2.8))
    var truck := _make_freight_truck(lot,"AssignedTruck",Color("#b29b69"))
    truck.visible = false

func _make_freight_truck(parent: Node3D,caption: String,paint: Color) -> Node3D:
    var truck := Node3D.new()
    truck.name = caption
    parent.add_child(truck)
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
    return truck

func _build_logistics_depot() -> void:
    var depot := Node3D.new()
    depot.name = "DepositoLogistico"
    _scenery.add_child(depot)
    var plaster := Color("#d5d5ca")
    var steel := Color("#64777e")
    # Administrative office: doorway facing the central yard, not a solid shell.
    var office := Node3D.new()
    office.name = "Office"
    depot.add_child(office)
    _box(office,Vector3(10,0.08,8),Color("#b0ada0"),Vector3(-9,0.02,10))
    _room_wall(office,Vector3(-14,0,10),Vector2(0.25,8),plaster)
    _room_wall(office,Vector3(-9,0,14),Vector2(10,0.25),plaster)
    _room_wall(office,Vector3(-4,0,10),Vector2(0.25,8),plaster)
    for x in [-12.25,-5.75]:
        _room_wall(office,Vector3(x,0,6),Vector2(3.5,0.25),plaster)
    _box(office,Vector3(10.3,0.25,8.3),steel,Vector3(-9,3.13,10))
    _sign(office,"Deposito Logistico · Ufficio",Vector3(-9,3.6,5.8))
    _room_furniture(office,DEPOT_DESK,Vector3(2.5,0.85,0.85),Color("#8b7158"))
    _box(office,Vector3(0.66,0.4,0.09),Color("#323c40"),DEPOT_DESK+Vector3(-0.5,1.1,0))
    _room_furniture(office,Vector3(-12.4,0,12.1),Vector3(1.0,2.0,1.2),steel)
    _sign(office,"Fleet management · F",DEPOT_DESK+Vector3(0,2.1,0))
    # Two walk-in garage bays, shared maintenance floor and marked parking.
    var garage := Node3D.new()
    garage.name = "Garage"
    depot.add_child(garage)
    _box(garage,Vector3(12,0.07,8),Color("#555b5b"),Vector3(9.5,0.03,12))
    for x in [3.5,9.5,15.5]:
        _room_wall(garage,Vector3(x,0,12),Vector2(0.24,8),steel)
        _box(garage,Vector3(0.24,1.0,8),steel,Vector3(x,3.5,12))
    _room_wall(garage,Vector3(9.5,0,16),Vector2(12,0.25),steel)
    _box(garage,Vector3(12.3,0.3,8.3),Color("#526369"),Vector3(9.5,4.15,12))
    for n in range(2):
        var bay: Vector3 = DEPOT_BAYS[n]
        for x in [-1.45,1.45]:
            _box(garage,Vector3(0.08,0.02,6),Color("#e7d9ac"),bay+Vector3(x,0.08,0))
        _sign(garage,"Bay %s" % (n+1),Vector3(bay.x,3.65,7.8))
        var truck := _make_freight_truck(garage,"Truck"+str(n+1),Color("#b29b69") if n==0 else Color("#668696"))
        truck.position = bay
        truck.visible = false
    # Storage racks are scenery until a separate depot inventory is implemented.
    var store := Node3D.new()
    store.name = "Warehouse"
    depot.add_child(store)
    _box(store,Vector3(10,0.05,5),Color("#7c807d"),Vector3(-9,0.02,-11))
    for x in [-13.0,-9.0,-5.0]:
        _room_furniture(store,Vector3(x,0,-12),Vector3(2.4,1.7,1.4),Color("#967956"))
    _sign(store,"Magazzino · storage",Vector3(-9,2.65,-9))
    _lamp(depot,Vector3(-3,0,5))
    _lamp(depot,Vector3(3,0,-8))

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

func _build_construction_site() -> void:
    var site := Node3D.new()
    site.name = "CantiereVerdi"
    _scenery.add_child(site)
    site.position = Vector3(12,0,12)
    _box(site,Vector3(6,0.06,6),Color("#aa9475"),Vector3.ZERO)
    _sign(site,"Cantiere Verdi",Vector3(0,1.5,-3.5))
    _add_obstacle(Vector3(12,0,12),Vector2(6,6))
    var foundation := Node3D.new()
    foundation.name = "Foundation"
    site.add_child(foundation)
    _box(foundation,Vector3(5.4,0.35,5.4),Color("#a4a29a"),Vector3(0,0.18,0))
    var walls := Node3D.new()
    walls.name = "Walls"
    site.add_child(walls)
    _box(walls,Vector3(5.0,2.8,5.0),Color("#d4b997"),Vector3(0,1.75,0))
    var roof := Node3D.new()
    roof.name = "Roof"
    site.add_child(roof)
    _box(roof,Vector3(5.5,0.35,5.5),Color("#92563d"),Vector3(0,3.3,0))
    _box(roof,Vector3(1.0,2.0,0.08),Color("#72523e"),Vector3(0,1.2,-2.53))
    for x in [-1.5,1.5]:
        _box(roof,Vector3(0.85,0.9,0.08),Color("#7199aa"),Vector3(x,1.9,-2.53))
    foundation.visible = false
    walls.visible = false
    roof.visible = false

func _sync_construction_site() -> void:
    if not _chunks.has("NORTH_RESIDENCES"):
        return
    var root: Node3D = _chunks["NORTH_RESIDENCES"]["root"]
    var site := root.get_node_or_null("CantiereVerdi") as Node3D
    if site==null:
        return
    var stage := int(_last_snapshot.get("construction",{}).get("stage",0))
    site.get_node("Foundation").visible = stage>=1
    site.get_node("Walls").visible = stage>=2
    site.get_node("Roof").visible = stage>=3

# Fictional Lombard residential block, with a real street-facing home entrance.
# Keep the established guesthouse, rental room, construction site and road lanes intact.
func _build_lombardy_neighborhood() -> void:
    var street := Node3D.new()
    street.name="ViaDelleMagnolie"
    _scenery.add_child(street)
    var paving := Color("#b8b2a5")
    _box(street,Vector3(34,0.045,3.2),Color("#777b78"),Vector3(0,0.025,-0.1))
    for z in [-2.35,2.15]:
        _box(street,Vector3(32,0.10,1.5),paving,Vector3(0,0.04,z))
        for x in range(-15,16):
            _box(street,Vector3(0.035,0.018,1.45),Color("#918e85"),Vector3(x,0.1,z))
    _lombardy_facade(street,"CasaMagnolia",Vector3(11,0,-6.8),Color("#d3b895"),3)
    _lombardy_facade(street,"CasaTiglio",Vector3(-10,0,-7.5),Color("#c88f7b"),2)
    _sign(street,"Via delle Magnolie",Vector3(-3,2.5,-2.4))
    for x in [-15.0,15.0]:
        _lamp(street,Vector3(x,0,-2.3))
    _box(street,Vector3(4.0,0.055,5.3),paving,Vector3(-4.6,0.025,-8.0))
    _tree(Vector3(-4.5,0,-9.5))
    _bench(street,Vector3(-4.5,0,-6.2))
    _sign(street,"Piazzetta del Tiglio",Vector3(-4.5,2.2,-10.5))

func _lombardy_facade(parent: Node3D,id: String,at: Vector3,wall: Color,floors: int) -> void:
    var building := Node3D.new()
    building.name=id
    parent.add_child(building)
    var width := 7.4
    var depth := 5.6
    var height := 3.1*float(floors)
    var face := at.z+depth/2.0
    var stone := Color("#d7cbb7")
    var shutter := Color("#4c6255")
    var dark := Color("#394c52")
    _add_obstacle(at,Vector2(width,depth))
    _box(building,Vector3(width,height,depth),wall,at+Vector3(0,height/2,0))
    _box(building,Vector3(width+0.4,0.35,depth+0.4),Color("#8e503a"),at+Vector3(0,height+0.12,0))
    _box(building,Vector3(width+0.16,0.45,0.12),stone,Vector3(at.x,0.23,face+0.07))
    for level in range(floors):
        var y := 1.6+float(level)*3.1
        if level>0:
            _box(building,Vector3(width+0.18,0.12,0.16),stone,Vector3(at.x,y-1.35,face+0.08))
        for offset in [-2.35,0.0,2.35]:
            var x: float = at.x+offset
            if level==0 and offset==0.0:
                _box(building,Vector3(1.8,2.55,0.14),stone,Vector3(x,1.27,face+0.09))
                _box(building,Vector3(1.38,2.2,0.16),Color("#72513a"),Vector3(x,1.10,face+0.18))
                _box(building,Vector3(0.08,0.12,0.1),Color("#c5a45e"),Vector3(x+0.43,1.03,face+0.28))
                continue
            _box(building,Vector3(1.17,1.5,0.12),stone,Vector3(x,y,face+0.06))
            _box(building,Vector3(0.85,1.24,0.14),dark,Vector3(x,y,face+0.14))
            _box(building,Vector3(0.055,1.23,0.16),stone,Vector3(x,y,face+0.21))
            for sx in [-0.71,0.71]:
                _box(building,Vector3(0.32,1.35,0.15),shutter,Vector3(x+sx,y,face+0.13))
            _box(building,Vector3(1.6,0.12,0.35),stone,Vector3(x,y-0.75,face+0.17))
            if level>0 and offset==0.0:
                _box(building,Vector3(1.9,0.14,0.75),stone,Vector3(x,y-0.87,face+0.42))
                for bar in range(7):
                    _box(building,Vector3(0.045,0.65,0.045),Color("#444442"),Vector3(x-0.84+float(bar)*0.28,y-0.48,face+0.77))
                _box(building,Vector3(1.85,0.055,0.055),Color("#444442"),Vector3(x,y-0.14,face+0.77))
    _sign(building,"Casa Magnolia" if id=="CasaMagnolia" else "Casa Tiglio",Vector3(at.x,2.8,face+0.35))
    if id=="CasaMagnolia":
        var entrance := Marker3D.new()
        entrance.name="HomeApproach"
        entrance.position=Vector3(at.x,0,face+1.2)
        building.add_child(entrance)

func _build_guesthouse(owned: bool = false) -> void:
    var house := Node3D.new()
    house.name = "CasaDelGlicine" if owned else "CasaOspiti"
    _scenery.add_child(house)
    var plaster := Color("#c6b5a6") if owned else Color("#d6c4a3")
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
    door.visible = not (_owns_house() if owned else bool(_last_snapshot.get("player",{}).get("roomActive",false)))
    _box(house,Vector3(9.25,0.22,10.3),Color("#ccb99c"),Vector3(-8.5,3.1,9))
    # Decorative upper storey; the actual rented studio is on ground level.
    _box(house,Vector3(9,3.2,10),Color("#c4a382"),Vector3(-8.5,4.8,9))
    _box(house,Vector3(9.5,0.35,10.5),Color("#865747"),Vector3(-8.5,6.55,9))
    for z in [6.0,9.0,12.0]:
        _box(house,Vector3(0.08,1.3,1.0),Color("#3b5051"),Vector3(-3.96,4.7,z))
        for offset in [-0.72,0.72]:
            _box(house,Vector3(0.14,1.4,0.35),Color("#486045"),Vector3(-3.86,4.7,z+offset))
    _sign(house,"Casa del Glicine" if owned else "Casa Ospiti · Camera 1",Vector3(-3.7,3.35,8.5))
    _sign(house,"Property office · F" if owned else "Reception · F",KIOSK_POSITION+Vector3(0,2.2,0))
    if owned:
        _box(house,Vector3(2.8,0.10,1.2),Color("#a58d71"),Vector3(-2.5,0.04,8.5))
        for z in [5.0,12.0]:
            _room_furniture(house,Vector3(-2.7,0,z),Vector3(0.5,0.45,0.55),Color("#8d6853"))
            _sphere(house,0.35,Color("#7664a3"),Vector3(-2.7,0.8,z))
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

func _build_home_garden() -> void:
    var garden := Node3D.new()
    garden.name = "OrtoPrivato"
    _scenery.add_child(garden)
    _box(garden,Vector3(4,0.16,4.8),Color("#765039"),GARDEN_CENTER+Vector3(0,0.04,0))
    _add_obstacle(GARDEN_CENTER,Vector2(4,4.8))
    for x in [-2.05,2.05]:
        _box(garden,Vector3(0.16,0.34,5.0),Color("#a28a66"),GARDEN_CENTER+Vector3(x,0.15,0))
    for z in [-2.45,2.45]:
        _box(garden,Vector3(4.2,0.34,0.16),Color("#a28a66"),GARDEN_CENTER+Vector3(0,0.15,z))
    _sign(garden,"Orto privato · F",GARDEN_CENTER+Vector3(0,1.5,2.65))
    var barrel := Node3D.new()
    barrel.name = "RainBarrel"
    garden.add_child(barrel)
    barrel.position = GARDEN_CENTER+Vector3(2.85,0,0)
    _cylinder(barrel,0.43,1.05,Color("#486a76"),Vector3(0,0.55,0))
    _cylinder(barrel,0.39,0.04,Color("#718f9c"),Vector3(0,1.10,0))
    _box(barrel,Vector3(0.11,0.12,0.20),Color("#a6aaa0"),Vector3(0,0.28,0.46))
    barrel.visible = false
    var greenhouse := Node3D.new()
    greenhouse.name = "Greenhouse"
    greenhouse.position = GARDEN_CENTER
    garden.add_child(greenhouse)
    for x in [-2.05,2.05]:
        for z in [-2.45,2.45]:
            _box(greenhouse,Vector3(0.10,2.25,0.10),Color("#839e8b"),Vector3(x,1.15,z))
        _box(greenhouse,Vector3(0.10,0.10,4.9),Color("#839e8b"),Vector3(x,2.25,0))
    for z in [-2.45,0.0,2.45]:
        _box(greenhouse,Vector3(4.2,0.10,0.10),Color("#839e8b"),Vector3(0,2.25,z))
    var roof := MeshInstance3D.new()
    var roof_mesh := BoxMesh.new()
    roof_mesh.size = Vector3(4.2,0.06,5.0)
    roof.mesh = roof_mesh
    var glass := StandardMaterial3D.new()
    glass.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA
    glass.albedo_color = Color(0.69,0.89,0.83,0.28)
    roof.material_override = glass
    roof.position = Vector3(0,2.3,0)
    greenhouse.add_child(roof)
    greenhouse.visible = false
    var composter := Node3D.new()
    composter.name = "Composter"
    composter.position = GARDEN_CENTER+Vector3(-2.7,0,0)
    garden.add_child(composter)
    _box(composter,Vector3(0.7,0.85,0.7),Color("#66503b"),Vector3(0,0.425,0))
    _box(composter,Vector3(0.80,0.08,0.80),Color("#4d603d"),Vector3(0,0.89,0))
    composter.visible = false
    for stage in range(4):
        var crops := Node3D.new()
        crops.name = "Stage"+str(stage)
        garden.add_child(crops)
        for x in [-1.25,0.0,1.25]:
            for z in [-1.5,0.0,1.5]:
                var height := 0.10+0.13*stage
                var at := GARDEN_CENTER+Vector3(x,0.15+height,z)
                _sphere(crops,0.08+0.07*stage,Color("#558849"),at)
                if stage==3:
                    _sphere(crops,0.10,Color("#be553d"),at+Vector3(0.15,-0.08,0))
        crops.visible = false

func _sync_garden() -> void:
    if not _chunks.has("SOUTH_RESIDENCES"):
        return
    var root: Node3D = _chunks["SOUTH_RESIDENCES"]["root"]
    var report: Dictionary = _last_snapshot.get("garden",{})
    var composter := root.get_node_or_null("OrtoPrivato/Composter") as Node3D
    if composter!=null:
        composter.visible = _owns_house() and bool(report.get("composter",false))
    var greenhouse := root.get_node_or_null("OrtoPrivato/Greenhouse") as Node3D
    if greenhouse!=null:
        greenhouse.visible = _owns_house() and bool(report.get("greenhouse",false))
    var barrel := root.get_node_or_null("OrtoPrivato/RainBarrel") as Node3D
    if barrel!=null:
        barrel.visible = _owns_house() and bool(report.get("barrel",false))
    for stage in range(4):
        var crops := root.get_node_or_null("OrtoPrivato/Stage"+str(stage)) as Node3D
        if crops!=null:
            crops.visible = _owns_house() and bool(report.get("planted",false)) and int(report.get("growth",0))==stage

func _build_home_coop() -> void:
    var yard := Node3D.new()
    yard.name = "PollaioPrivato"
    yard.position = COOP_CENTER
    _scenery.add_child(yard)
    _sign(yard,"Pollaio · F",Vector3(0,2.4,2.7))
    var coop := Node3D.new()
    coop.name = "Coop"
    yard.add_child(coop)
    _box(coop,Vector3(5.2,0.05,4.8),Color("#a58e66"),Vector3.ZERO)
    _box(coop,Vector3(2.5,1.6,1.6),Color("#a07951"),Vector3(0,1.1,-1.35))
    _box(coop,Vector3(2.8,0.18,1.9),Color("#795043"),Vector3(0,2.0,-1.35))
    _box(coop,Vector3(0.65,0.65,0.06),Color("#4c3829"),Vector3(0,0.9,-0.52))
    _box(coop,Vector3(0.66,0.12,1.3),Color("#bc9a6c"),Vector3(0,0.3,0.1))
    for x in [-2.5,2.5]:
        for z in [-2.3,0.0,2.3]:
            _box(coop,Vector3(0.11,1.2,0.11),Color("#84684a"),Vector3(x,0.6,z))
        for y in [0.25,0.65,1.05]:
            _box(coop,Vector3(0.045,0.045,4.6),Color("#9caaa0"),Vector3(x,y,0))
    for z in [-2.3,2.3]:
        for y in [0.25,0.65,1.05]:
            _box(coop,Vector3(5,0.045,0.045),Color("#9caaa0"),Vector3(0,y,z))
    for n in range(3):
        var bird := Node3D.new()
        bird.name = "Hen"+str(n+1)
        bird.position = Vector3(-1.5+1.5*n,0,1.0)
        coop.add_child(bird)
        var feathers := Color("#c3a17a") if n==1 else Color("#e4ddcd")
        _sphere(bird,0.26,feathers,Vector3(0,0.37,0))
        _sphere(bird,0.15,feathers,Vector3(0,0.62,-0.19))
        _box(bird,Vector3(0.11,0.08,0.15),Color("#d1a146"),Vector3(0,0.6,-0.35))
        _sphere(bird,0.075,Color("#ae493b"),Vector3(0,0.78,-0.19))
        for x in [-0.10,0.10]:
            _box(bird,Vector3(0.045,0.19,0.045),Color("#b28b45"),Vector3(x,0.10,0))
            _sphere(bird,0.028,Color("#333333"),Vector3(x,0.65,-0.26))
        bird.visible = false
    for n in range(6):
        var egg := Node3D.new()
        egg.name = "Egg"+str(n+1)
        coop.add_child(egg)
        egg.position = Vector3(-0.8+0.32*n,0.14,-0.2)
        _sphere(egg,0.085,Color("#e7dfcd"),Vector3.ZERO)
        egg.scale.y = 1.25
        egg.visible = false
    coop.visible = false

func _sync_coop() -> void:
    if not _chunks.has("SOUTH_RESIDENCES"):
        return
    var root: Node3D = _chunks["SOUTH_RESIDENCES"]["root"]
    var coop := root.get_node_or_null("PollaioPrivato/Coop") as Node3D
    if coop==null:
        return
    var report: Dictionary = _last_snapshot.get("hens",{})
    coop.visible = _owns_house() and bool(report.get("coop",false))
    for n in range(3):
        var bird := coop.get_node("Hen"+str(n+1)) as Node3D
        bird.visible = n<int(report.get("hens",0))
    for n in range(6):
        var egg := coop.get_node("Egg"+str(n+1)) as Node3D
        egg.visible = n<int(report.get("eggs",0))

func _build_orchard() -> void:
    var orchard := Node3D.new()
    orchard.name = "FruttetoPrivato"
    _scenery.add_child(orchard)
    _sign(orchard,"Frutteto · F",ORCHARD_CENTER+Vector3(0,2.4,3.5))
    var hive := Node3D.new()
    hive.name = "Beehive"
    hive.position = ORCHARD_CENTER
    orchard.add_child(hive)
    for h in range(3):
        _box(hive,Vector3(0.65,0.24,0.65),Color("#cfaa5c"),Vector3(0,0.25+0.24*h,0))
    _box(hive,Vector3(0.78,0.12,0.78),Color("#85633f"),Vector3(0,0.91,0))
    hive.visible = false
    var cellar := Node3D.new()
    cellar.name = "Cantina"
    cellar.position = ORCHARD_CENTER+Vector3(3.4,0,0)
    orchard.add_child(cellar)
    _box(cellar,Vector3(1.7,0.12,1.6),Color("#817665"),Vector3(0,0.06,0))
    for x in [-0.48,0.48]:
        _cylinder(cellar,0.33,0.90,Color("#896040"),Vector3(x,0.56,0))
        _cylinder(cellar,0.35,0.08,Color("#514c47"),Vector3(x,0.35,0))
        _cylinder(cellar,0.35,0.08,Color("#514c47"),Vector3(x,0.78,0))
    _sign(cellar,"Cantina",Vector3(0,1.6,0))
    cellar.visible = false
    for n in range(4):
        var plot: Vector3 = ORCHARD_PLOTS[n]
        _box(orchard,Vector3(1.8,0.05,1.8),Color("#866449"),plot+Vector3(0,0.02,0))
        var plant := Node3D.new()
        plant.name = "Plant"+str(n)
        plant.position = plot
        orchard.add_child(plant)
        var trunk := Node3D.new()
        trunk.name = "Tree"
        plant.add_child(trunk)
        _cylinder(trunk,0.15,1.7,Color("#73523b"),Vector3(0,0.85,0))
        _sphere(trunk,0.8,Color("#4c783d"),Vector3(0,1.9,0))
        if n==3:
            _box(trunk,Vector3(1.7,0.12,0.12),Color("#997b59"),Vector3(0,1.25,0))
            _box(trunk,Vector3(0.12,1.5,0.12),Color("#997b59"),Vector3(-0.8,0.75,0))
            _box(trunk,Vector3(0.12,1.5,0.12),Color("#997b59"),Vector3(0.8,0.75,0))
        var fruit := Node3D.new()
        fruit.name = "Fruit"
        trunk.add_child(fruit)
        var colors := [Color("#c64b40"),Color("#c5ac49"),Color("#69518b"),Color("#60537e")]
        for x in [-0.55,0.0,0.55]:
            _sphere(fruit,0.13,colors[n],Vector3(x,1.7,0.48))
        fruit.visible = false
        plant.visible = false

func _sync_orchard() -> void:
    if not _chunks.has("SOUTH_RESIDENCES"):
        return
    var root: Node3D = _chunks["SOUTH_RESIDENCES"]["root"]
    var cellar := root.get_node_or_null("FruttetoPrivato/Cantina") as Node3D
    if cellar!=null:
        cellar.visible = _owns_house() and bool(_last_snapshot.get("orchard",{}).get("cellar",false))
    var hive := root.get_node_or_null("FruttetoPrivato/Beehive") as Node3D
    if hive!=null:
        hive.visible = _owns_house() and bool(_last_snapshot.get("orchard",{}).get("hive",false))
    var plants: Array = _last_snapshot.get("orchard",{}).get("plants",[])
    for n in range(4):
        var model := root.get_node_or_null("FruttetoPrivato/Plant"+str(n)) as Node3D
        if model==null:
            continue
        var data: Dictionary = plants[n] if n<plants.size() else {}
        model.visible = _owns_house() and bool(data.get("planted",false))
        var tree := model.get_node("Tree") as Node3D
        var size := 0.30+0.14*float(data.get("age",0))
        tree.scale = Vector3.ONE*size
        var fruit := tree.get_node("Fruit") as Node3D
        fruit.visible = bool(data.get("ready",false))

func _build_digital_office() -> void:
    var office := Node3D.new()
    office.name="BorgoDigital"
    _scenery.add_child(office)
    var wall := Color("#e3d9c7")
    var partition := Color("#c3ccc9")
    var wood := Color("#a58a69")
    var dark := Color("#333e44")
    for part in [[Vector3(-15,0,8),Vector2(0.24,12)],
            [Vector3(-9.5,0,2),Vector2(11,0.24)],
            [Vector3(-9.5,0,14),Vector2(11,0.24)],
            [Vector3(-4,0,4.4),Vector2(0.24,4.8)],
            [Vector3(-4,0,11.6),Vector2(0.24,4.8)]]:
        _room_wall(office,part[0],part[1],wall)
    for z in [6.0,10.0]:
        _room_wall(office,Vector3(-11.75,0,z),Vector2(6.5,0.20),partition)
    _box(office,Vector3(11,0.10,12),Color("#b9b3a5"),Vector3(-9.5,0.005,8))
    _box(office,Vector3(11.15,0.23,12.15),Color("#435359"),Vector3(-9.5,3.12,8))
    _box(office,Vector3(11.35,0.30,12.35),Color("#93684e"),Vector3(-9.5,3.39,8))
    _box(office,Vector3(0.22,0.40,2.35),dark,Vector3(-4,2.8,8))
    _sign(office,"Borgo Digital · Informatica",Vector3(-3.65,3.65,8))
    _box(office,Vector3(2.3,0.07,2.7),Color("#aaa99f"),Vector3(-2.95,0.045,8))
    for z in [3.5,5.0,11.0,12.5]:
        _box(office,Vector3(0.14,1.55,1.15),Color("#7999a4"),Vector3(-3.84,1.67,z))
    for z in [4.0,8.0,12.0]:
        var light := OmniLight3D.new()
        light.position=Vector3(-10.8,2.72,z)
        light.omni_range=6.5
        light.light_energy=1.6
        office.add_child(light)
    var corridor_light := OmniLight3D.new()
    corridor_light.position=Vector3(-6.0,2.7,8.0)
    corridor_light.omni_range=7
    corridor_light.light_energy=1.3
    office.add_child(corridor_light)
    var dev := Node3D.new()
    dev.name="DevelopmentRoom"
    office.add_child(dev)
    _sign(dev,"Java development · E",Vector3(-10,2.35,4))
    for z in [3.25,4.9]:
        _room_furniture(dev,Vector3(-13.4,0,z),Vector3(1.0,0.78,1.25),wood)
        _box(dev,Vector3(0.10,0.46,0.72),dark,Vector3(-13.6,1.12,z))
        _box(dev,Vector3(0.045,0.38,0.62),Color("#6795a0"),Vector3(-13.53,1.12,z))
        _box(dev,Vector3(0.30,0.04,0.59),dark,Vector3(-13.1,0.81,z))
        _room_furniture(dev,Vector3(-12.35,0,z),Vector3(0.46,0.46,0.46),dark)
    _box(dev,Vector3(0.12,1.12,1.9),Color("#eef0e9"),Vector3(-14.82,1.85,4))
    _sign(dev,"Tickets · Tests · Git",Vector3(-14.65,2.0,4))
    var server := Node3D.new()
    server.name="ServerRoom"
    office.add_child(server)
    _sign(server,"Operations · DevOps · E",Vector3(-10,2.35,8))
    for z in [7.0,8.65]:
        _room_furniture(server,Vector3(-13.85,0,z),Vector3(1.05,2.2,1.15),dark)
        for y in [0.48,0.83,1.18,1.53,1.88]:
            _box(server,Vector3(0.055,0.22,0.88),Color("#596469"),Vector3(-13.30,y,z))
            _box(server,Vector3(0.06,0.075,0.10),Color("#75bd83"),Vector3(-13.26,y,z+0.31))
    _room_furniture(server,Vector3(-12.0,0,8),Vector3(1.0,0.82,1.6),wood)
    _box(server,Vector3(0.11,0.51,0.82),dark,Vector3(-12.25,1.18,8))
    _box(server,Vector3(0.05,0.42,0.72),Color("#588873"),Vector3(-12.17,1.18,8))
    _room_furniture(server,Vector3(-11.25,0,7.1),Vector3(0.45,0.46,0.45),dark)
    var meeting := Node3D.new()
    meeting.name="MeetingRoom"
    office.add_child(meeting)
    _sign(meeting,"Clients & company · E",Vector3(-10,2.35,12))
    _room_furniture(meeting,Vector3(-12.8,0,12),Vector3(2.25,0.82,1.2),wood)
    for x in [-13.4,-12.1]:
        for z in [11.02,12.98]:
            _room_furniture(meeting,Vector3(x,0,z),Vector3(0.48,0.47,0.48),dark)
    _box(meeting,Vector3(0.12,1.1,2.25),Color("#e9ebe6"),Vector3(-14.82,1.85,12))
    _sign(meeting,"Contracts · Payroll · Finance",Vector3(-14.6,1.9,12))
    for station in [JAVA_DESK,DEVOPS_DESK,COMPANY_DESK]:
        _box(office,Vector3(1.45,0.025,1.45),Color("#6f8c85"),station+Vector3(0,0.07,0))

func _build_college() -> void:
    var campus := Node3D.new()
    campus.name="IstitutoLeonardo"
    _scenery.add_child(campus)
    var wall := Color("#d9c9a5")
    var wood := Color("#8f7455")
    for part in [[Vector3(-13,0,7),Vector2(0.24,8)],
            [Vector3(-8.5,0,3),Vector2(9,0.24)],
            [Vector3(-8.5,0,11),Vector2(9,0.24)],
            [Vector3(-4,0,4.45),Vector2(0.24,2.9)],
            [Vector3(-4,0,9.55),Vector2(0.24,2.9)]]:
        _room_wall(campus,part[0],part[1],wall)
    _box(campus,Vector3(9,0.08,8),Color("#c3bba8"),Vector3(-8.5,0.025,7))
    _box(campus,Vector3(9,3.3,8),wall,Vector3(-8.5,4.65,7))
    _box(campus,Vector3(9.4,0.3,8.4),Color("#9b6045"),Vector3(-8.5,6.45,7))
    _box(campus,Vector3(0.24,0.5,2.2),wood,Vector3(-4,2.75,7))
    _sign(campus,"Istituto Leonardo · Scuola e formazione",Vector3(-3.6,3.4,7))
    for z in [4.5,9.5]:
        _box(campus,Vector3(0.16,1.35,1.65),Color("#536e75"),Vector3(-3.84,1.55,z))
        _box(campus,Vector3(0.16,1.35,1.65),Color("#536e75"),Vector3(-3.84,4.65,z))
    var desk := Node3D.new()
    desk.name="CollegeDesk"
    campus.add_child(desk)
    _room_furniture(desk,Vector3(-8,0,7),Vector3(1.15,0.85,2.0),wood)
    _box(desk,Vector3(0.55,0.45,0.62),Color("#42494d"),Vector3(-8,1.1,7.3))
    _sign(desk,"Segreteria · E",Vector3(-7.15,2.0,7))
    var classroom := Node3D.new()
    classroom.name="Classroom"
    campus.add_child(classroom)
    for x in [-11.1,-9.4]:
        for z in [4.35,9.55]:
            _room_furniture(classroom,Vector3(x,0,z),Vector3(0.95,0.72,0.65),wood)
            _room_furniture(classroom,Vector3(x,0,z+0.7),Vector3(0.45,0.42,0.45),Color("#65786e"))
            _box(classroom,Vector3(0.45,0.3,0.06),Color("#3a454a"),Vector3(x,0.9,z-0.2))
    _box(classroom,Vector3(0.12,1.25,3.1),Color("#415d53"),Vector3(-12.82,1.85,7))
    _box(classroom,Vector3(0.17,0.1,3.2),wood,Vector3(-12.75,1.2,7))
    var light := OmniLight3D.new()
    light.position=Vector3(-8.5,2.6,7)
    light.omni_range=8
    light.light_energy=1.5
    campus.add_child(light)

func _build_dojo() -> void:
    var dojo := Node3D.new()
    dojo.name="DojoShotokan"
    _scenery.add_child(dojo)
    var wall := Color("#e2dbca")
    var wood := Color("#8d6b48")
    for part in [[Vector3(13,0,7),Vector2(0.24,8)],
            [Vector3(8.5,0,3),Vector2(9,0.24)],
            [Vector3(8.5,0,11),Vector2(9,0.24)],
            [Vector3(4,0,4.5),Vector2(0.24,3)],
            [Vector3(4,0,9.5),Vector2(0.24,3)]]:
        _room_wall(dojo,part[0],part[1],wall)
    _box(dojo,Vector3(9,0.08,8),wood,Vector3(8.5,0.025,7))
    _box(dojo,Vector3(9.4,0.24,8.4),Color("#555c58"),Vector3(8.5,3.12,7))
    _box(dojo,Vector3(0.24,0.55,2),wood,Vector3(4,2.75,7))
    _sign(dojo,"Dojo Shotokan",Vector3(3.8,3.3,7))
    var mat := Node3D.new()
    mat.name="Tatami"
    dojo.add_child(mat)
    for x in range(5):
        for z in range(4):
            var edge := x==0 or x==4 or z==0 or z==3
            _box(mat,Vector3(1.1,0.035,1.1),Color("#b16d61") if edge else Color("#78948a"),Vector3(6.7+x*1.1,0.095,4.65+z*1.1))
    _room_furniture(dojo,Vector3(11.9,0,9.6),Vector3(1.5,0.45,0.45),wood)
    for z in [4.1,4.8,5.5]:
        _room_furniture(dojo,Vector3(12.5,0,z),Vector3(0.65,1.8,0.58),Color("#8a9696"))
    _sign(dojo,"Rei · Kihon · Kata · Kumite",Vector3(12.7,2.25,7))
    _sign(dojo,"Training · F",Vector3(6,1.8,7))
    var light := OmniLight3D.new()
    light.position=Vector3(8.5,2.65,7)
    light.omni_range=9
    light.light_energy=1.6
    dojo.add_child(light)

func _near_dojo() -> bool:
    return not _apartment_active and _current_place=="SCHOOL" and Rect2(5.9,4.0,5.9,5.2).has_point(Vector2(_avatar.position.x,_avatar.position.z))

func _build_medical_building(pharmacy: bool) -> void:
    var building := Node3D.new()
    building.name="FarmaciaDelBorgo" if pharmacy else "AmbulatorioCivico"
    _scenery.add_child(building)
    var side := 1.0 if pharmacy else -1.0
    var z := -7.0 if pharmacy else 7.0
    var wall := Color("#d5d6c1") if pharmacy else Color("#e1d5bf")
    var trim := Color("#557c68") if pharmacy else Color("#618699")
    for part in [[Vector3(side*12,0,z),Vector2(0.24,7)],
            [Vector3(side*8,0,z-3.5),Vector2(8,0.24)],
            [Vector3(side*8,0,z+3.5),Vector2(8,0.24)],
            [Vector3(side*4,0,z-2.3),Vector2(0.24,2.4)],
            [Vector3(side*4,0,z+2.3),Vector2(0.24,2.4)]]:
        _room_wall(building,part[0],part[1],wall)
    _box(building,Vector3(8,0.08,7),Color("#c4c7bd"),Vector3(side*8,0.025,z))
    _box(building,Vector3(8,3.1,7),wall,Vector3(side*8,4.55,z))
    _box(building,Vector3(8.3,0.3,7.3),Color("#93664c"),Vector3(side*8,6.25,z))
    _box(building,Vector3(0.24,0.48,2.2),trim,Vector3(side*4,2.76,z))
    _sign(building,"Farmacia del Borgo" if pharmacy else "Ambulatorio civico",Vector3(side*3.4,3.4,z))
    _box(building,Vector3(2.1,0.07,3.8),Color("#c4b9a5"),Vector3(side*2.95,0.045,z))
    for offset in [-2.2,2.2]:
        _box(building,Vector3(0.13,1.35,1.35),Color("#71929a"),Vector3(side*3.85,1.55,z+offset))
    _box(building,Vector3(0.12,0.68,0.22),trim,Vector3(side*3.77,3.6,z+2.2))
    _box(building,Vector3(0.12,0.22,0.68),trim,Vector3(side*3.77,3.6,z+2.2))
    var counter := Node3D.new()
    counter.name="ServiceCounter"
    building.add_child(counter)
    _room_furniture(counter,Vector3(side*8.1,0,z),Vector3(1.05,0.94,2.6),trim)
    _box(counter,Vector3(1.15,0.10,2.7),Color("#ece9dd"),Vector3(side*8.1,0.99,z))
    _sign(counter,"Assistenza · E" if pharmacy else "Visita · E",Vector3(side*7.25,2.0,z))
    if pharmacy:
        _room_furniture(building,Vector3(side*11.3,0,z),Vector3(0.65,2.0,4.8),Color("#deded0"))
        for y in [0.7,1.25,1.8]:
            for offset in [-1.6,-0.8,0.0,0.8,1.6]:
                _box(building,Vector3(0.34,0.22,0.42),Color("#809e9e") if int(y*10)%2==0 else Color("#e1c78a"),Vector3(side*11.0,y,z+offset))
    else:
        _room_furniture(building,Vector3(side*11.0,0,z-1.7),Vector3(1.0,0.72,2.1),Color("#a7b9b8"))
        _box(building,Vector3(0.92,0.15,2.0),Color("#f1eee4"),Vector3(side*11.0,0.79,z-1.7))
        _room_furniture(building,Vector3(side*10.4,0,z+2.0),Vector3(1.5,0.78,0.8),Color("#927e69"))
        for offset in [-2.1,2.1]:
            _room_furniture(building,Vector3(side*5.7,0,z+offset),Vector3(0.5,0.45,0.55),trim)
    var light := OmniLight3D.new()
    light.position=Vector3(side*7.5,2.6,z)
    light.light_energy=1.5
    light.omni_range=7
    building.add_child(light)

func _build_cafe() -> void:
    var cafe := Node3D.new()
    cafe.name="BarCentrale"
    _scenery.add_child(cafe)
    var wall := Color("#d9c2a2")
    var stone := Color("#d6cbb8")
    var wood := Color("#715342")
    for part in [[Vector3(-13,0,7),Vector2(0.24,8.0)],
            [Vector3(-8.5,0,3),Vector2(9.0,0.24)],
            [Vector3(-8.5,0,11),Vector2(9.0,0.24)],
            [Vector3(-4,0,4.45),Vector2(0.24,2.9)],
            [Vector3(-4,0,9.55),Vector2(0.24,2.9)]]:
        _room_wall(cafe,part[0],part[1],wall)
    _box(cafe,Vector3(9,0.07,8),Color("#bca994"),Vector3(-8.5,0.025,7))
    _box(cafe,Vector3(9,3.2,8),wall,Vector3(-8.5,4.6,7))
    _box(cafe,Vector3(9.35,0.3,8.35),Color("#91563e"),Vector3(-8.5,6.35,7))
    _box(cafe,Vector3(0.22,0.50,2.1),wood,Vector3(-4,2.75,7))
    _box(cafe,Vector3(1.45,0.14,2.4),Color("#537569"),Vector3(-3.4,3.0,7))
    _sign(cafe,"Bar Centrale",Vector3(-3.0,3.4,7))
    for z in [4.5,9.5]:
        _box(cafe,Vector3(0.12,1.55,1.85),Color("#536e75"),Vector3(-3.82,1.45,z))
        _box(cafe,Vector3(0.16,1.3,1.1),Color("#4e6352"),Vector3(-3.84,4.6,z))
    var bar := Node3D.new()
    bar.name="CafeCounter"
    cafe.add_child(bar)
    _room_furniture(bar,Vector3(-8.6,0,5.4),Vector3(1.2,0.98,3.4),wood)
    _box(bar,Vector3(1.32,0.10,3.5),stone,Vector3(-8.6,1.02,5.4))
    _box(bar,Vector3(0.65,0.55,0.95),Color("#676f70"),Vector3(-8.6,1.36,4.5))
    _box(bar,Vector3(0.07,0.22,0.85),Color("#282f32"),Vector3(-8.24,1.31,4.5))
    for z in [4.25,4.75,5.6]:
        _cylinder(bar,0.085,0.13,Color("#f3efe4"),Vector3(-8.0,1.14,z))
    _sign(bar,"Caffè · E",Vector3(-7.9,2.0,5.4))
    var tables := Node3D.new()
    tables.name="CafeTables"
    cafe.add_child(tables)
    for x in [-10.8,-5.8]:
        _room_furniture(tables,Vector3(x,0,9.0),Vector3(0.94,0.76,0.94),wood)
        for z in [7.95,10.05]:
            _room_furniture(tables,Vector3(x,0,z),Vector3(0.48,0.45,0.48),Color("#698379"))
            _box(tables,Vector3(0.48,0.43,0.07),wood,Vector3(x,0.66,z+0.20*signf(z-9.0)))
    _sign(tables,"Take a break · E",Vector3(-7,2.1,9))
    for at in [Vector3(-7,2.6,5.4),Vector3(-8.3,2.6,9)]:
        var light := OmniLight3D.new()
        light.position=at
        light.light_color=Color("#ffe1b5")
        light.light_energy=1.4
        light.omni_range=6
        cafe.add_child(light)

func _build_bakery() -> void:
    var bakery := Node3D.new()
    bakery.name="PanetteriaDelBorgo"
    _scenery.add_child(bakery)
    var plaster := Color("#d4b598")
    var wood := Color("#845f42")
    var stone := Color("#ded1ba")
    # 8 x 7 m shop; east-facing doorway at z=7.0 remains unobstructed.
    for part in [[Vector3(-12,0,7),Vector2(0.24,7.0)],
            [Vector3(-8,0,3.5),Vector2(8.0,0.24)],
            [Vector3(-8,0,10.5),Vector2(8.0,0.24)],
            [Vector3(-4,0,4.7),Vector2(0.24,2.4)],
            [Vector3(-4,0,9.3),Vector2(0.24,2.4)]]:
        _room_wall(bakery,part[0],part[1],plaster)
    _box(bakery,Vector3(8,0.08,7),Color("#b69c7a"),Vector3(-8,0.025,7))
    _box(bakery,Vector3(8,3.15,7),plaster,Vector3(-8,4.57,7))
    _box(bakery,Vector3(8.3,0.28,7.3),Color("#92543c"),Vector3(-8,6.28,7))
    _box(bakery,Vector3(0.25,0.48,2.25),wood,Vector3(-4,2.76,7))
    _box(bakery,Vector3(1.35,0.12,2.45),Color("#b45b43"),Vector3(-3.4,2.98,7))
    for z in [4.85,9.15]:
        _box(bakery,Vector3(0.12,1.25,1.15),Color("#5b7278"),Vector3(-3.82,1.55,z))
        _box(bakery,Vector3(0.24,0.12,1.4),stone,Vector3(-3.72,0.88,z))
        _box(bakery,Vector3(0.16,1.25,1.10),Color("#526554"),Vector3(-3.85,4.55,z))
    _sign(bakery,"Panetteria del Borgo",Vector3(-3.1,3.36,7))
    _box(bakery,Vector3(2.1,0.07,4.0),stone,Vector3(-2.95,0.045,7))
    var counter := Node3D.new()
    counter.name="BakeryCounter"
    bakery.add_child(counter)
    _room_furniture(counter,Vector3(-8.1,0,7.1),Vector3(1.05,0.95,3.0),wood)
    _box(counter,Vector3(1.12,0.1,3.1),stone,Vector3(-8.1,1.0,7.1))
    _box(counter,Vector3(0.06,0.46,2.1),Color("#a4b7bd"),Vector3(-7.53,1.24,7.0))
    for z in [6.35,6.75,7.15,7.55]:
        _box(counter,Vector3(0.53,0.16,0.24),Color("#bc8a4d"),Vector3(-8.08,1.13,z))
    _box(counter,Vector3(0.33,0.28,0.42),Color("#444b4c"),Vector3(-8.1,1.18,8.25))
    _sign(counter,"Pane fresco · E",Vector3(-7.3,2.0,7))
    var oven := Node3D.new()
    oven.name="BreadOven"
    bakery.add_child(oven)
    _room_furniture(oven,Vector3(-10.75,0,5.1),Vector3(1.85,1.9,1.9),Color("#9a7256"))
    _box(oven,Vector3(0.08,0.64,1.1),Color("#36302c"),Vector3(-9.78,1.03,5.1))
    _box(oven,Vector3(0.1,0.08,0.63),Color("#b0a598"),Vector3(-9.71,1.38,5.1))
    _room_furniture(bakery,Vector3(-11.3,0,8.6),Vector3(0.65,2.0,2.4),wood)
    for y in [0.75,1.3,1.85]:
        _box(bakery,Vector3(0.72,0.08,2.4),stone,Vector3(-11.3,y,8.6))
        for z in [7.9,8.5,9.1]:
            _box(bakery,Vector3(0.38,0.15,0.36),Color("#c79b59"),Vector3(-11.1,y+0.14,z))
    var light := OmniLight3D.new()
    light.position=Vector3(-7,2.65,7)
    light.light_color=Color("#ffe1b4")
    light.light_energy=1.5
    light.omni_range=7
    bakery.add_child(light)

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
    var counter := Node3D.new()
    counter.name="GroceryCounter"
    shop.add_child(counter)
    _add_obstacle(Vector3(8.7,0,3.5),Vector2(1.0,1.9))
    _box(counter,Vector3(1.0,0.94,1.9),Color("#83674c"),Vector3(8.7,0.47,3.5))
    _box(counter,Vector3(1.1,0.09,2.0),Color("#d3c2a5"),Vector3(8.7,0.98,3.5))
    _box(counter,Vector3(0.32,0.25,0.38),Color("#353d42"),Vector3(8.7,1.16,3.0))
    _sign(counter,"Cassa · E",Vector3(8.0,2.0,3.5))
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
    _box(park, Vector3(32.0, 0.06, 2.6), paving, Vector3(0, 0.01, 5),"paving")
    _box(park, Vector3(3.2, 0.06, 16.0), paving, Vector3(-11, 0.01, -3),"paving")
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
    # Shared gate and kiosk already have labelled signs.
    # A street along the edge provides visual context, not a new travel route.
    _box(park, Vector3(38,0.08,3.2), Color(0.23,0.25,0.27), Vector3(0,0.02,-16),"asphalt")
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
        person.set_meta("mood",str(resident.get("mood","CALM")))
        person.set_meta("personal_debt",int(resident.get("personalDebt",0)))
        person.set_meta("personal_debt_due",int(resident.get("personalDebtDueDay",0)))
        _sync_office_employee(person,id)
        _sync_dojo_member(person,id)
        person.set_meta("display_name",str(resident.get("name","Resident")))
        person.set_meta("known_name",str(resident.get("knownName","")) if resident.get("knownName") != null else "")
        person.set_meta("conversations",int(resident.get("conversations",0)))
        person.set_meta("relationship",str(resident.get("relationship","STRANGER")))
        var activity_label := person.get_node_or_null("ActivityLabel") as Label3D
        if activity_label!=null:
            activity_label.text=tr(str(person.get_meta("activity","")).replace("_"," ").capitalize())
    for child in _residents_root.get_children():
        if not present.has(str(child.get_meta("resident_id", ""))):
            _residents_root.remove_child(child)
            child.queue_free()

    _sync_office_joker()

func _sync_office_joker() -> void:
    if _current_place!="OFFICE" or _apartment_active:return
    if _office_joker_id.is_empty():
        var candidates: Array[String] = []
        for person in _residents_root.get_children():
            if str(person.get_meta("activity",""))=="WORKING":
                candidates.append(str(person.get_meta("resident_id","")))
        if candidates.is_empty():return
        candidates.sort()
        _office_joker_id=candidates[0]
    var joker := _residents_root.get_node_or_null("Resident_"+_office_joker_id) as Node3D
    if joker==null:return
    if str(joker.get_meta("activity",""))!="WORKING":return
    joker.set_meta("personality","joker")
    if not Rect2(-15,2,11,12).has_point(Vector2(joker.position.x,joker.position.z)):
        var anchor := Vector3(-10.8,0,3.15)
        if _can_stand(anchor,WALKER_RADIUS):
            joker.position=anchor
            joker.set_meta("anchor",anchor)

func _tick_office_joker(delta: float) -> void:
    if _current_place!="OFFICE" or _apartment_active or not is_instance_valid(_residents_root):return
    if _office_joker_id.is_empty():return
    var joker := _residents_root.get_node_or_null("Resident_"+_office_joker_id) as Node3D
    if joker==null:
        _office_joke_hold=0.0
        return
    var bubble := joker.get_node_or_null("OfficeJoke") as Label3D
    var inside := Rect2(-15,2,11,12).has_point(Vector2(_avatar.position.x,_avatar.position.z))
    var can_hear := inside and _avatar.position.distance_to(joker.position)<7.0 and str(joker.get_meta("activity",""))=="WORKING"
    if not can_hear or str(joker.get_meta("mood","CALM")) in ["TIRED","SAD","STRESSED","IRRITATED","LONELY"]:
        if bubble!=null:bubble.hide()
        _office_joke_hold=0.0
        return
    if _office_joke_hold>0.0:
        _office_joke_hold=maxf(0.0,_office_joke_hold-delta)
        if bubble!=null:bubble.visible=_office_joke_hold>0.0
        if _office_joke_hold>0.0:
            var arm := joker.get_node_or_null("ArmRight") as Node3D
            if arm!=null:arm.rotation.x=-0.65
        return
    _office_joke_wait=maxf(0.0,_office_joke_wait-delta)
    if _office_joke_wait>0.0:return
    if bubble==null:
        bubble=Label3D.new()
        bubble.name="OfficeJoke"
        bubble.position=Vector3(0,2.65,0)
        bubble.billboard=BaseMaterial3D.BILLBOARD_ENABLED
        bubble.font_size=34
        bubble.pixel_size=0.004
        bubble.modulate=Color("#ffe6a0")
        bubble.outline_size=9
        joker.add_child(bubble)
    bubble.text=OFFICE_JOKE_LINES[_office_joke_index % OFFICE_JOKE_LINES.size()]
    bubble.show()
    _office_joke_index+=1
    _office_joke_hold=4.0
    _office_joke_wait=28.0+float(_office_joke_index%3)*7.0

func _sync_dojo_member(person: Node3D,id: String) -> void:
    if _current_place!="SCHOOL" or _apartment_active:return
    var community = _last_snapshot.get("rebirth",{}).get("dojoCommunity")
    var members: Array = community.get("present",[]) if community is Dictionary else []
    var index := 0
    for member in members:
        if str(member.get("id",""))==id:
            var anchor := Vector3(7.2+float(index%3)*1.6,0,5.0+float((index/3)%3)*1.3)
            if _can_stand(anchor,WALKER_RADIUS):
                person.position=anchor
                person.set_meta("anchor",anchor)
                var match_view = _last_snapshot.get("rebirth",{}).get("kumite")
                var sparring: bool = match_view is Dictionary and match_view.get("active",false) and str(match_view.get("partnerId",""))==id
                person.set_meta("activity","KUMITE" if sparring else "DOJO")
                if sparring:
                    person.rotation.y=atan2(person.position.x-_avatar.position.x,person.position.z-_avatar.position.z)
                person.set_meta("dojo_member",true)
                return
        index+=1
    if person.get_meta("dojo_member",false):
        var anchor := _open_anchor(Vector3(0,0,-4))
        person.position=anchor
        person.set_meta("anchor",anchor)
        person.set_meta("dojo_member",false)

func _sync_office_employee(person: Node3D,id: String) -> void:
    if _current_place!="OFFICE" or _apartment_active:return
    var workspace = _last_snapshot.get("rebirth",{}).get("javaWorkspace")
    if not workspace is Dictionary:return
    var firm = workspace.get("companyStatus")
    if not firm is Dictionary:return
    var staff = firm.get("staff")
    if not staff is Dictionary:return
    var employee = staff.get("employee")
    if not employee is Dictionary or str(employee.get("id",""))!=id:return
    if str(person.get_meta("activity",""))=="WORKING" and _can_stand(OFFICE_STAFF_ANCHOR,WALKER_RADIUS):
        person.position=OFFICE_STAFF_ANCHOR
        person.set_meta("anchor",OFFICE_STAFF_ANCHOR)
        person.rotation.y=PI/2.0

func _make_resident(id: String, index: int, display_name: String) -> Node3D:
    var person := Node3D.new()
    person.name = "Resident_" + id
    person.set_meta("resident_id", id)
    person.set_meta("personality",ResidentPersonality.style(id))
    var anchor := Vector3((float(index % 8)-3.5)*3.0, 0, -4.0-float(index / 8)*2.5)
    if _apartment_active:
        anchor = FamilyApartment.ANCHORS[index % FamilyApartment.ANCHORS.size()]
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
    var activity_label := Label3D.new()
    activity_label.name="ActivityLabel"
    activity_label.position=Vector3(0,1.94,0)
    activity_label.billboard=BaseMaterial3D.BILLBOARD_ENABLED
    activity_label.font_size=24
    activity_label.pixel_size=0.006
    activity_label.modulate=Color("#f4db9e")
    person.add_child(activity_label)
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
            if activity=="KUMITE" and limb in ["ArmLeft","ArmRight"]:
                angle=-0.7
            elif not walking and activity in ["WORKING","STUDYING"] and limb in ["ArmRight","ArmLeft"]:
                angle=-0.22+sin(float(Time.get_ticks_msec())*0.003+phase)*0.12
            elif not walking and activity!="SLEEPING" and limb=="ArmRight":
                var style := str(person.get_meta("personality","quiet"))
                if style in ["talkative","joker"] and str(person.get_meta("mood","CALM")) in ["CALM","HAPPY","ENTHUSIASTIC"]:
                    angle=sin(float(Time.get_ticks_msec())*0.003+phase)*0.16
                elif style=="relaxed":
                    angle=-0.10
            part.rotation.x = lerpf(part.rotation.x,angle,minf(delta*10.0,1.0))

func _building(_title: String, wall_color: Color) -> void:
    _add_obstacle(Vector3(-7.0, 0.0, 7.5), Vector2(11.0, 7.0))
    _box(_scenery, Vector3(11.0, 4.5, 7.0), wall_color,
            Vector3(-7.0, 2.25, 7.5))
    _box(_scenery, Vector3(12.0, 0.7, 8.0), Color(0.27, 0.30, 0.33),
            Vector3(-7.0, 4.85, 7.5))
    _box(_scenery, Vector3(2.1, 3.0, 0.1), Color(0.20, 0.28, 0.32),
            Vector3(-7.0, 1.5, 3.95))
    _sign(_scenery,_title,Vector3(-7,3.9,3.82))
    var trim := wall_color.lightened(0.25)
    for x in [-10.7,-3.3]:
        for y in [1.5,3.25]:
            _box(_scenery,Vector3(1.55,1.3,0.12),trim,Vector3(x,y,3.91))
            _box(_scenery,Vector3(1.28,1.03,0.14),Color("#4a6068"),Vector3(x,y,3.83))
            _box(_scenery,Vector3(0.065,1.02,0.16),trim,Vector3(x,y,3.74))
            _box(_scenery,Vector3(1.28,0.065,0.16),trim,Vector3(x,y,3.74))
            _box(_scenery,Vector3(1.65,0.13,0.24),trim,Vector3(x,y-0.68,3.82))
    _box(_scenery,Vector3(11.0,0.18,0.14),trim,Vector3(-7,2.42,3.9))

func _create_gate_and_kiosk() -> void:
    # A yellow gate is a map transition — actual travel is authorized by Java.
    for offset in [-1.6, 1.6]:
        _add_obstacle(GATE_POSITION + Vector3(offset, 0.0, 0.0), Vector2(0.4, 0.4))
    if _scenery.name != "PublicScene_LOGISTICS_DEPOT":
        _add_obstacle(KIOSK_POSITION, Vector2(2.4, 1.6))
    _box(_scenery, Vector3(0.4, 3.1, 0.4), Color(0.94, 0.69, 0.12),
            GATE_POSITION + Vector3(-1.6, 1.55, 0.0))
    _box(_scenery, Vector3(0.4, 3.1, 0.4), Color(0.94, 0.69, 0.12),
            GATE_POSITION + Vector3(1.6, 1.55, 0.0))
    _box(_scenery, Vector3(3.6, 0.38, 0.45), Color(0.94, 0.69, 0.12),
            GATE_POSITION + Vector3(0.0, 3.2, 0.0))
    _sign(_scenery,tr("TRAVEL · E"),GATE_POSITION+Vector3(0,3.65,0))
    if _scenery.name != "PublicScene_LOGISTICS_DEPOT":
        # Blue kiosk: only offers actions physically available in this district.
        _sign(_scenery,tr("SERVICES · F"),KIOSK_POSITION+Vector3(0,2.25,0))
        _box(_scenery, Vector3(2.4, 1.4, 1.6), Color(0.13, 0.39, 0.69),
                KIOSK_POSITION + Vector3(0.0, 0.7, 0.0))
        _box(_scenery, Vector3(3.2, 0.22, 2.2), Color(0.88, 0.89, 0.89),
                KIOSK_POSITION + Vector3(0.0, 1.6, 0.0))

func _tree(at: Vector3) -> void:
    _add_obstacle(at, Vector2(0.45, 0.45))
    _box(_scenery, Vector3(0.45, 1.8, 0.45), Color(0.33, 0.19, 0.11),
            at + Vector3(0.0, 0.9, 0.0))
    var crown := Node3D.new()
    crown.name="TreeCrown"
    crown.position=at
    _scenery.add_child(crown)
    var leaves := Color("#476f39")
    for offset in [Vector3(-0.55,2.5,0),Vector3(0.55,2.6,0.2),Vector3(0,3.25,-0.1),Vector3(0,2.55,-0.65)]:
        var mesh := SphereMesh.new()
        mesh.radius=0.88
        mesh.height=1.65
        mesh.radial_segments=10
        mesh.rings=5
        var foliage := MeshInstance3D.new()
        foliage.mesh=mesh
        var mat := StandardMaterial3D.new()
        mat.albedo_color=leaves.lightened(0.06*float(crown.get_child_count()))
        mat.roughness=1.0
        foliage.material_override=mat
        foliage.position=offset
        crown.add_child(foliage)

func _box(parent: Node3D, size: Vector3, color: Color,
        where: Vector3, surface: String = "") -> void:
    var mesh := BoxMesh.new()
    mesh.size = size
    var node := MeshInstance3D.new()
    node.mesh = mesh
    var mat := StandardMaterial3D.new()
    mat.albedo_color = color
    if _surface_textures.has(surface):
        TownVisuals.surface(mat,_surface_textures[surface],0.65 if surface=="paving" else 0.32)
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
    var guide_toggle := CheckButton.new()
    guide_toggle.text = "First steps / navigation"
    guide_toggle.button_pressed = true
    guide_toggle.toggled.connect(_set_guide_enabled)
    column.add_child(guide_toggle)
    _guide_label=Label.new()
    _guide_label.autowrap_mode=TextServer.AUTOWRAP_WORD_SMART
    _guide_label.custom_minimum_size=Vector2(420,0)
    _guide_label.text=tr("First steps: connecting to the world...")
    column.add_child(_guide_label)
    _weather = Label.new()
    _weather.text = "Connecting to the local Java world..."
    column.add_child(_weather)
    _needs = Label.new()
    _needs.text = "Jobs, study and travel use the real game world."
    column.add_child(_needs)
    _bag_label = Label.new()
    _bag_label.text = "Bag: empty"
    column.add_child(_bag_label)
    _aid_label = Label.new()
    _aid_label.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
    _aid_label.visible = false
    column.add_child(_aid_label)
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
    bag_row.add_child(_button("Drink juice",_consume_product.bind("grape-juice")))
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
    _language_selector = OptionButton.new()
    _language_selector.add_item("English",0)
    _language_selector.add_item("Italiano",1)
    _language_selector.select(1 if TranslationServer.get_locale()=="it" else 0)
    _language_selector.item_selected.connect(_on_language_selected)
    column.add_child(_language_selector)
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
    _hint.autowrap_mode=TextServer.AUTOWRAP_WORD_SMART
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
    _dialog_style = Label.new()
    _dialog_style.autowrap_mode=TextServer.AUTOWRAP_WORD_SMART
    dialog_col.add_child(_dialog_style)
    _dialog_reply = Label.new()
    _dialog_reply.custom_minimum_size = Vector2(400,0)
    _dialog_reply.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
    dialog_col.add_child(_dialog_reply)
    dialog_col.add_child(_button("Say hello",_send_dialog.bind("Hello")))
    dialog_col.add_child(_button("How are you?",_send_dialog.bind("How are you?")))
    dialog_col.add_child(_button("Offer support",_send_dialog.bind("I'm here for you.")))
    dialog_col.add_child(_button("Ask about shared memories",_send_dialog.bind("What do you remember about me?")))
    dialog_col.add_child(_button("Offer bread",_send_dialog.bind("Offer bread")))
    dialog_col.add_child(_button("Offer water",_send_dialog.bind("Offer water")))
    dialog_col.add_child(_button("Ask for bread",_send_dialog.bind("Ask for bread")))
    dialog_col.add_child(_button("Ask for water",_send_dialog.bind("Ask for water")))
    dialog_col.add_child(_button("Ask for a 10 EUR loan",_send_dialog.bind("Ask for a 10 EUR loan")))
    dialog_col.add_child(_button("Repay personal loan",_send_dialog.bind("Repay personal loan")))
    dialog_col.add_child(_button("Ask about occupation",_send_dialog.bind("What do you do?")))
    dialog_col.add_child(_button("Allow sharing my name",_send_dialog.bind("You can tell people my name.")))
    dialog_col.add_child(_button("Ask for visitor news",_send_dialog.bind("Any news about visitors?")))
    dialog_col.add_child(_button("Press interview",_send_dialog.bind("Any public news for the newspaper?")))
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

func _on_language_selected(index: int) -> void:
    GameLanguage.select_locale("it" if index==1 else "en")
    _refresh_localized_hud()
    if _software_menu:
        _render_actions()

func _refresh_localized_hud() -> void:
    if _last_snapshot.is_empty():return
    _render_hud_summary(_last_snapshot)

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
    _dialog_attitude.text = tr("Attitude: %s") % tr(str(person.get_meta("relationship","STRANGER")).capitalize())
    _render_dialog_mood(person)
    var known := str(person.get_meta("known_name",""))
    _dialog_reply.text = tr("They know you as %s") % known if not known.is_empty() else (
            "You have spoken before." if int(person.get_meta("conversations",0)) > 0 else "You have not met.")
    _dialog_input.clear()
    _dialog_panel.visible = true
    Input.set_mouse_mode(Input.MOUSE_MODE_VISIBLE)

func _render_dialog_mood(person: Node3D) -> void:
    var mood := str(person.get_meta("mood","CALM"))
    _dialog_style.text = tr("Seems: %s") % tr(mood.capitalize())
    var debt := int(person.get_meta("personal_debt",0))
    var due := int(person.get_meta("personal_debt_due",0))
    if debt>0:
        _dialog_style.text += "\n"+(tr("Personal debt: %s EUR · due day %s") % [debt,due])
        if int(_last_snapshot.get("day",1))>due:
            _dialog_style.text += " · "+tr("Overdue")

func _localized_resident_reply(message: String) -> String:
    var person := _residents_root.get_node_or_null("Resident_"+_dialog_target) as Node3D
    if person==null:return message
    var prefix := str(person.get_meta("display_name",""))+": "
    if not message.begins_with(prefix):return message
    return prefix+tr(message.substr(prefix.length()))

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

func _render_logistics(logistics: Dictionary) -> void:
    if not bool(logistics.get("yours",false)):
        _action_choices.add_child(_button("Register / acquire logistics company · 20 EUR",_freight_action.bind("register")))
    else:
        var carrier := Label.new()
        carrier.text = "LOGISTICS | Bank: %s EUR | Truck: %s" % [logistics.get("cash",0),str(logistics.get("truckPlace","none"))]
        _action_choices.add_child(carrier)
        _action_choices.add_child(_button("Logistics: invest 25 EUR",_freight_action.bind("invest-25")))
        _action_choices.add_child(_button("Logistics: invest 100 EUR",_freight_action.bind("invest-100")))
        _action_choices.add_child(_button("Logistics: withdraw 25 EUR",_freight_action.bind("withdraw-25")))
        if not bool(logistics.get("truck",false)):
            _action_choices.add_child(_button("Buy company truck · 220 EUR",_freight_action.bind("buy-truck")))
        else:
            var condition := int(logistics.get("condition",100))
            var finances: Dictionary = logistics.get("financials",{})
            var accounts := Label.new()
            accounts.text = "Truck condition: %s/100\nRevenue %s | Fuel %s | Relocation %s EUR\nWages %s | Hiring %s | Repairs %s EUR\nOperating net %s EUR | Deliveries %s\nTruck purchases %s | Invested %s | Withdrawn %s EUR" % [
                condition,finances.get("revenue",0),finances.get("fuel",0),finances.get("relocation",0),
                finances.get("wages",0),finances.get("hiring",0),finances.get("maintenance",0),
                finances.get("operatingNet",0),finances.get("deliveries",0),finances.get("truckPurchases",0),
                finances.get("invested",0),finances.get("withdrawn",0)]
            _action_choices.add_child(accounts)
            if condition<100:
                var service_note := " + transport" if str(logistics.get("truckPlace",""))!=_current_place else ""
                _action_choices.add_child(_button("Service company truck · 15 EUR + 1 part"+service_note,_freight_action.bind("service-truck")))
            var driver_name = logistics.get("driverName")
            var details := Label.new()
            details.text = "Driver: %s | Automated deliveries: %s" % ["none" if driver_name==null else str(driver_name),logistics.get("completed",0)]
            _action_choices.add_child(details)
            if driver_name==null:
                for candidate in logistics.get("candidates",[]):
                    _action_choices.add_child(_button("Hire driver: "+str(candidate.get("name",""))+" · 10 EUR",_hire_driver.bind(str(candidate.get("id","")))))
            else:
                var enabled := bool(logistics.get("automatic",false))
                _action_choices.add_child(_button("Disable automatic freight" if enabled else "Enable automatic freight",_freight_action.bind("auto-off" if enabled else "auto-on")))
            var second: Dictionary = logistics.get("secondTruck",{})
            if not bool(second.get("purchased",false)):
                _action_choices.add_child(_button("Buy second truck · 220 EUR",_freight_action.bind("buy-truck-2")))
            else:
                var second_driver = second.get("driverName")
                var second_status := Label.new()
                second_status.text = "TRUCK 2 | %s | %s/100\nDriver: %s | Deliveries: %s\n%s" % [
                    str(second.get("place","")),second.get("condition",0),"none" if second_driver==null else str(second_driver),
                    second.get("completed",0),("Cargo: %s (%s)" % [second.get("cargo",""),second.get("units",0)]) if bool(second.get("active",false)) else "Idle"]
                _action_choices.add_child(second_status)
                if second_driver==null:
                    for candidate in logistics.get("candidates",[]):
                        _action_choices.add_child(_button("Hire driver 2: "+str(candidate.get("name",""))+" · 10 EUR",_hire_second_driver.bind(str(candidate.get("id","")))))
                else:
                    var second_enabled := bool(second.get("automatic",false))
                    _action_choices.add_child(_button("Disable truck 2 dispatch" if second_enabled else "Enable truck 2 dispatch",_freight_action.bind("auto-off-2" if second_enabled else "auto-on-2")))
                if int(second.get("condition",100))<100 and not bool(second.get("active",false)):
                    _action_choices.add_child(_button("Service truck 2 · 15 EUR + 1 part (transport extra)",_freight_action.bind("service-truck-2")))

func _rebirth_active() -> bool:
    return str(_last_snapshot.get("rebirth",{}).get("phase","ADULT"))!="ADULT"

func _child_can_walk() -> bool:
    var r: Dictionary = _last_snapshot.get("rebirth",{})
    return str(r.get("phase","")) == "LINKED_ADULT" or (str(r.get("phase","")) == "CHILDHOOD" and int(r.get("age",0)) >= 3 and int(r.get("age",0)) < 18)

func _office_station() -> String:
    if _apartment_active or _current_place!="OFFICE" or _last_snapshot.get("rebirth",{}).get("softwareProjects",[]).is_empty():
        return ""
    if _avatar.position.distance_to(JAVA_DESK)<1.25:return "java"
    if _avatar.position.distance_to(DEVOPS_DESK)<1.25:return "devops"
    if _avatar.position.distance_to(COMPANY_DESK)<1.25:return "company"
    return ""

func _near_software() -> bool:
    return not _office_station().is_empty()

func _office_action_allowed(action: String) -> bool:
    var station := _office_station()
    if action.begins_with("rebirth-devops-"):return station=="devops"
    if action.begins_with("rebirth-it:"):return station=="company"
    if action.begins_with("rebirth-software-") or action.begins_with("rebirth-java-") or action.begins_with("rebirth-git:") or action.begins_with("rebirth-developer-"):
        return station=="java"
    return true

func _close_software() -> void:
    _office_app="work"
    _software_menu=false
    _action_panel.visible=false
    _render_actions()
    Input.set_mouse_mode(Input.MOUSE_MODE_CAPTURED)

func _office_label(value: String) -> void:
    var label := Label.new()
    label.autowrap_mode=TextServer.AUTOWRAP_WORD_SMART
    label.text=value
    _action_choices.add_child(label)

func _switch_office_app(app: String) -> void:
    if not _near_software():return
    _office_app=app
    _render_actions()

func _office_draft_key(id: String) -> String:
    return str(_last_snapshot.get("player",{}).get("incarnation",0))+":"+str(_last_snapshot.get("rebirth",{}).get("childName",""))+":"+id

func _remember_office_draft(value: String,id: String) -> void:
    _office_drafts[_office_draft_key(id)]=value.left(180)

func _render_office_apps(station: String) -> void:
    _office_label("BorgoOS · "+station.capitalize()+" workstation · Day "+str(_last_snapshot.get("day",1)))
    var dock := HFlowContainer.new()
    _action_choices.add_child(dock)
    var work_title := "Code editor" if station=="java" else ("Terminal" if station=="devops" else "Company ledger")
    for entry in [["work",work_title],["tasks","Issue tracker"],["mail","Inbox"],["browser","Intranet"],["help","Handbook"]]:
        var app := _button(str(entry[1]),_switch_office_app.bind(str(entry[0])))
        app.disabled=_office_app==str(entry[0])
        dock.add_child(app)

func _render_office_app(workspace: Dictionary) -> void:
    var projects: Array = _last_snapshot.get("rebirth",{}).get("softwareProjects",[])
    var ops = workspace.get("devops")
    match _office_app:
        "tasks":
            _office_label("Issue tracker · Team backlog")
            for project in projects:
                _office_label("%s | %s | Next: %s | %s EUR" % [project.get("title",""),project.get("phase",""),project.get("nextAction",""),project.get("wage",0)])
                if project.get("branch","")!="":
                    _office_label(str(project.get("branch","")))
            if projects.is_empty():_office_label("No assigned projects.")
            _office_label("Use the Java code editor to advance a ticket.")
        "mail":
            _office_label("Inbox · Generated work notices")
            _office_label("From: Team lead\nSubject: Welcome to Borgo Digital\nRead acceptance criteria, run tests and open a pull request before releasing your work.")
            for project in projects:
                if project.get("deployed",false):
                    _office_label("From: Release automation\nSubject: %s deployed\nThe client service is released." % project.get("title",""))
                elif str(project.get("phase",""))=="TESTED":
                    _office_label("From: Code review\nSubject: %s reviewed\nNext step: %s" % [project.get("title",""),project.get("nextAction","")])
            if ops is Dictionary and ops.get("active",false):
                _office_label("From: Monitoring\nSubject: Retail incident\n"+str(ops.get("status",""))+" · Visit the operations workstation.")
            var firm = workspace.get("companyStatus")
            if firm is Dictionary and firm.get("registered",false):
                _office_label("From: Company accounts\nBalance: %s EUR · Lease through day %s" % [firm.get("cash",0),firm.get("rentUntil",0)])
        "browser":
            _office_label("Intranet · /api/v1/retail/catalog")
            var catalog := false
            var low_stock := false
            for project in projects:
                if project.get("id","")=="retail-api":catalog=bool(project.get("deployed",false))
                if project.get("id","")=="retail-sql":low_stock=bool(project.get("deployed",false))
            if not catalog:
                _office_label("Catalog not deployed. Finish BD-103 to publish this service.")
            elif ops is Dictionary and str(ops.get("status",""))=="OUTAGE":
                _office_label("503 Service Unavailable · Visit the server room.")
            else:
                _office_label("200 OK · Live inventory from world snapshot (read-only preview)")
                var rows: Array = _last_snapshot.get("shop",[]).duplicate()
                rows.sort_custom(func(a,b):return str(a.get("id",""))<str(b.get("id","")))
                for item in rows:
                    _office_label("%s · %s EUR · %s units" % [item.get("label",item.get("id","")),item.get("price",0),item.get("stock",0)])
                if low_stock:
                    _office_label("/api/v1/retail/low-stock · Stock below 3")
                    for item in rows:
                        if int(item.get("stock",0))<3:_office_label(str(item.get("id",""))+": "+str(item.get("stock",0)))
        "help":
            _office_label("Handbook · Java / Spring / SQL")
            _office_label("Java: return an integer expression using variables, arithmetic, Math.min and Math.max. Test boundary cases such as zero stock.")
            _office_label("REST: @GetMapping defines a read-only method route below the class-level @RequestMapping. SQL: SELECT columns FROM table WHERE condition ORDER BY column.")
            _office_label("Git: feature branch → implementation → commit → pull request → CI/review → merge → deployment.")
            _office_label("Operations: check health, inspect evidence, apply the guided recovery and verify. Terminal commands are simulated, never executed on a host.")
    _action_choices.add_child(_button("Leave workstation",_close_software))

func _render_software() -> void:
    var station := _office_station()
    _action_title.text="Borgo Digital · "+(tr("Java team") if station=="java" else (tr("Server room") if station=="devops" else tr("Meeting room")))
    var workspace = _last_snapshot.get("rebirth",{}).get("javaWorkspace")
    _render_office_apps(station)
    if _office_app!="work":
        _render_office_app(workspace if workspace is Dictionary else {})
        return
    if workspace is Dictionary:
        var intro := Label.new()
        intro.autowrap_mode=TextServer.AUTOWRAP_WORD_SMART
        intro.text=str(workspace.get("intro","")) if station=="java" else "Borgo Digital · "+station.capitalize()
        _action_choices.add_child(intro)
        if not workspace.get("onboarded",false):
            var start := _button("Complete onboarding · 15 min",_garden_action.bind("rebirth-java-onboard"))
            start.disabled=not bool(workspace.get("canOnboard",false))
            if station=="java":
                _action_choices.add_child(start)
            else:
                var direction := Label.new()
                direction.text="Complete onboarding at the Java desk first."
                _action_choices.add_child(direction)
            _action_choices.add_child(_button("Leave",_close_software))
            return
    if station=="java" and workspace is Dictionary and workspace.get("career") is Dictionary:
        var career: Dictionary = workspace["career"]
        var profile := Label.new()
        profile.autowrap_mode=TextServer.AUTOWRAP_WORD_SMART
        profile.text="%s · XP %s · Technical %s/100\nReleases %s · Audits %s" % [career.get("rank","Junior"),career.get("xp",0),career.get("technical",0),career.get("releases",0),career.get("audits",0)]
        _action_choices.add_child(profile)
        if career.get("nextRank")!=null:
            var promotion := _button("Promotion: %s · XP %s · Skill %s · Audits %s" % [career.get("nextRank",""),career.get("requiredXp",0),career.get("requiredTechnical",0),career.get("requiredAudits",0)],_garden_action.bind("rebirth-developer-promote"))
            promotion.disabled=not bool(career.get("canPromote",false))
            _action_choices.add_child(promotion)
        var audit: Dictionary = career.get("audit",{})
        var assignment := Label.new()
        assignment.autowrap_mode=TextServer.AUTOWRAP_WORD_SMART
        assignment.text="%s · %s EUR · 60 min\n%s" % [audit.get("title","Daily operations"),audit.get("wage",0),audit.get("prompt","")]
        _action_choices.add_child(assignment)
        var answer := LineEdit.new()
        answer.name="DeveloperAuditAnswer"
        answer.placeholder_text="Enter numeric result"
        answer.max_length=10
        answer.size_flags_horizontal=Control.SIZE_EXPAND_FILL
        _action_choices.add_child(answer)
        var submit_audit := _button("Submit daily audit",_submit_developer_audit.bind(answer))
        submit_audit.disabled=not bool(audit.get("available",false))
        _action_choices.add_child(submit_audit)
    if station=="devops" and workspace is Dictionary and workspace.get("devops") is Dictionary:
        var ops: Dictionary = workspace["devops"]
        var ops_info := Label.new()
        ops_info.autowrap_mode=TextServer.AUTOWRAP_WORD_SMART
        ops_info.text="DevOps · %s · Resolved %s\n%s\n%s" % [ops.get("status",""),ops.get("resolved",0),ops.get("guide",""),ops.get("output","")]
        _action_choices.add_child(ops_info)
        if not ops.get("trained",false):
            var training := _button("DevOps onboarding · 30 min",_garden_action.bind("rebirth-devops-train"))
            training.disabled=not bool(ops.get("canTrain",false))
            _action_choices.add_child(training)
        elif ops.get("active",false):
            var terminal := LineEdit.new()
            terminal.name="DevOpsCommand"
            terminal.placeholder_text="Enter simulated shell command"
            terminal.max_length=120
            terminal.size_flags_horizontal=Control.SIZE_EXPAND_FILL
            _action_choices.add_child(terminal)
            var run := _button("Run command · 5 min",_submit_devops.bind(terminal))
            run.disabled=not bool(ops.get("canRun",false))
            _action_choices.add_child(run)
    if station=="company" and workspace is Dictionary and workspace.get("companyStatus") is Dictionary:
        var firm: Dictionary = workspace["companyStatus"]
        var details := Label.new()
        details.autowrap_mode=TextServer.AUTOWRAP_WORD_SMART
        details.text="Your IT consultancy · %s\nCompany cash %s EUR · Rent through day %s · Contracts %s · Tax paid %s EUR\n%s" % ["Registered" if firm.get("registered",false) else "Not registered",firm.get("cash",0),firm.get("rentUntil",0),firm.get("contracts",0),firm.get("taxPaid",0),firm.get("guide","")]
        _action_choices.add_child(details)
        if not firm.get("registered",false):
            var register := _button("Register IT company · 50 EUR",_garden_action.bind("rebirth-it:register"))
            register.disabled=not bool(firm.get("canRegister",false))
            _action_choices.add_child(register)
        else:
            for choice in [{"label":"Invest 20 EUR","id":"invest","enabled":firm.get("canInvest",false)},{"label":"Withdraw 20 EUR","id":"withdraw","enabled":firm.get("canWithdraw",false)},{"label":"Rent office · 20 EUR / 7 days","id":"rent","enabled":firm.get("canRent",false)}]:
                var operation := _button(str(choice["label"]),_garden_action.bind("rebirth-it:"+str(choice["id"])))
                operation.disabled=not bool(choice["enabled"])
                _action_choices.add_child(operation)
            var staff = firm.get("staff")
            if staff is Dictionary:
                var staffing := Label.new()
                staffing.autowrap_mode=TextServer.AUTOWRAP_WORD_SMART
                staffing.text="IT assistant · 10 EUR signing bonus · 12 EUR per delegated report · No idle wages"
                _action_choices.add_child(staffing)
                var employee = staff.get("employee")
                if employee is Dictionary:
                    var cv := Label.new()
                    cv.text="%s · Technical %s/100 · %s · Delegated %s" % [employee.get("name",""),employee.get("technical",0),"Sick leave" if employee.get("sick",false) else employee.get("activity",""),staff.get("delegated",0)]
                    cv.autowrap_mode=TextServer.AUTOWRAP_WORD_SMART
                    _action_choices.add_child(cv)
                    var dismiss := _button("End support contract",_garden_action.bind("rebirth-it:dismiss"))
                    dismiss.disabled=not bool(staff.get("canDismiss",false))
                    _action_choices.add_child(dismiss)
                else:
                    for candidate in staff.get("candidates",[]):
                        var resume := Label.new()
                        resume.text="%s · Age %s · Technical %s/100%s" % [candidate.get("name",""),candidate.get("age",0),candidate.get("technical",0)," · programming diploma" if candidate.get("diploma",false) else ""]
                        resume.autowrap_mode=TextServer.AUTOWRAP_WORD_SMART
                        _action_choices.add_child(resume)
                        _action_choices.add_child(_button("Hire "+str(candidate.get("name","")),_garden_action.bind("rebirth-it:hire:"+str(candidate.get("id","")))))
            for contract in firm.get("offers",[]):
                var terms := Label.new()
                terms.autowrap_mode=TextServer.AUTOWRAP_WORD_SMART
                terms.text="%s · %s EUR gross · %s EUR tax\n%s" % [contract.get("title",""),contract.get("gross",0),contract.get("tax",0),contract.get("prompt","")]
                _action_choices.add_child(terms)
                var response := LineEdit.new()
                response.name="IT_"+str(contract.get("id",""))
                response.placeholder_text="Enter verified numeric result"
                response.max_length=10
                _action_choices.add_child(response)
                var deliver := _button("Deliver contract · 60 min",_submit_it_contract.bind(str(contract.get("id","")),response))
                deliver.disabled=not bool(contract.get("available",false))
                _action_choices.add_child(deliver)
                if staff is Dictionary and staff.get("employee") is Dictionary:
                    var delegate := _button("Delegate · 12 EUR wage",_garden_action.bind("rebirth-it:delegate:"+str(contract.get("id",""))))
                    delegate.disabled=not str(contract.get("id","")) in staff.get("delegatable",[])
                    _action_choices.add_child(delegate)
    var projects: Array = _last_snapshot.get("rebirth",{}).get("softwareProjects",[]) if station=="java" else []
    for project in projects:
        var info := Label.new()
        info.autowrap_mode=TextServer.AUTOWRAP_WORD_SMART
        info.text="%s · %s\n%s · Contract %s EUR" % [project.get("title",""),project.get("phase",""),project.get("effect",""),project.get("wage",0)]
        _action_choices.add_child(info)
        var command := str(project.get("command","code" if str(project.get("phase",""))=="OPEN" else ""))
        if project.has("explanation"):
            var guide := Label.new()
            guide.autowrap_mode=TextServer.AUTOWRAP_WORD_SMART
            guide.text=str(project.get("explanation",""))
            _action_choices.add_child(guide)
        if command=="code" and workspace is Dictionary:
            for ticket in workspace.get("tickets",[]):
                if str(ticket.get("id",""))!=str(project.get("id","")):continue
                var spec := Label.new()
                spec.autowrap_mode=TextServer.AUTOWRAP_WORD_SMART
                spec.text="%s · %s\n%s\n%s\n%s" % [ticket.get("number",""),ticket.get("language","Java"),ticket.get("requirement",""),ticket.get("signature",""),ticket.get("examples","")]
                _action_choices.add_child(spec)
                var editor := LineEdit.new()
                editor.name="Java_"+str(ticket.get("id",""))
                editor.placeholder_text=str(ticket.get("language","Java"))+" solution"
                var ticket_id := str(ticket.get("id",""))
                editor.text=str(_office_drafts.get(_office_draft_key(ticket_id),ticket.get("starter","return 0;")))
                editor.max_length=180
                editor.text_changed.connect(_remember_office_draft.bind(ticket_id))
                editor.size_flags_horizontal=Control.SIZE_EXPAND_FILL
                _action_choices.add_child(editor)
                var submit := _button("Run tests and submit · 60 min",_submit_java.bind(str(ticket.get("id","")),editor))
                submit.disabled=not bool(project.get("canAdvance",false))
                _action_choices.add_child(submit)
        elif not project.get("deployed",false):
            var request := "rebirth-git:"+command+":"+str(project.get("id","")) if command in ["branch","commit","pr","merge"] else "rebirth-software-"+str(project.get("id",""))
            var advance := _button("%s · %s min" % [project.get("nextAction",""),project.get("minutes",0)],_garden_action.bind(request))
            advance.disabled=not bool(project.get("canAdvance",false))
            _action_choices.add_child(advance)
    _action_choices.add_child(_button("Leave",_close_software))

func _submit_it_contract(contract: String,editor: LineEdit) -> void:
    if is_instance_valid(editor):
        _garden_action("rebirth-it:deliver:"+contract+":"+editor.text)

func _submit_devops(editor: LineEdit) -> void:
    if is_instance_valid(editor):
        _garden_action("rebirth-devops-command:"+editor.text)

func _submit_developer_audit(editor: LineEdit) -> void:
    if is_instance_valid(editor):
        _garden_action("rebirth-developer-audit:"+editor.text)

func _submit_java(ticket: String,editor: LineEdit) -> void:
    if is_instance_valid(editor):
        _garden_action("rebirth-java-submit:"+ticket+":"+editor.text)

func _near_engineering() -> bool:
    if _apartment_active or not _near_kiosk():return false
    for order in _last_snapshot.get("rebirth",{}).get("engineeringOrders",[]):
        if str(order.get("district",""))==_current_place:return true
    return false

func _close_engineering() -> void:
    _engineering_menu=false
    _action_panel.visible=false
    _render_actions()
    Input.set_mouse_mode(Input.MOUSE_MODE_CAPTURED)

func _render_engineering() -> void:
    _action_title.text="Engineering work orders"
    for order in _last_snapshot.get("rebirth",{}).get("engineeringOrders",[]):
        if str(order.get("district",""))!=_current_place:continue
        var info := Label.new()
        info.autowrap_mode=TextServer.AUTOWRAP_WORD_SMART
        info.text="%s · %s · Condition %s/100\n%s · Wage %s EUR" % [order.get("title",""),order.get("phase",""),order.get("condition",0),order.get("materials",""),order.get("wage",0)]
        _action_choices.add_child(info)
        var inspect := _button("Inspect · 30 min",_garden_action.bind("rebirth-engineer-inspect-"+str(order.get("id",""))))
        inspect.disabled=not bool(order.get("canInspect",false))
        _action_choices.add_child(inspect)
        var repair := _button("Repair · 90 min",_garden_action.bind("rebirth-engineer-repair-"+str(order.get("id",""))))
        repair.disabled=not bool(order.get("canComplete",false))
        _action_choices.add_child(repair)
    _action_choices.add_child(_button("Leave",_close_engineering))

func _near_college() -> bool:
    var college = _last_snapshot.get("rebirth",{}).get("college")
    return not _apartment_active and _current_place=="SCHOOL" and college is Dictionary and college.get("atCampus",false) and _avatar.position.distance_to(COLLEGE_DESK)<1.5

func _close_college() -> void:
    _college_menu=false
    _action_panel.visible=false
    _render_actions()
    Input.set_mouse_mode(Input.MOUSE_MODE_CAPTURED)

func _render_college() -> void:
    var college: Dictionary = _last_snapshot["rebirth"]["college"]
    _action_title.text="Istituto Leonardo"
    var info := Label.new()
    info.text="Mon–Fri 08:00–18:00 · 2 EUR per lesson"
    _action_choices.add_child(info)
    for course in college.get("courses",[]):
        var label := Label.new()
        label.text="%s · %s/%s lessons%s" % [course.get("label",""),course.get("completed",0),course.get("required",0)," · Diploma" if course.get("diploma",false) else ""]
        _action_choices.add_child(label)
        if course.get("canEnroll",false):
            _action_choices.add_child(_button("Enroll: "+str(course.get("label","")),_garden_action.bind("rebirth-enroll-"+str(course.get("id","")))))
    if college.get("enrolled")!=null:
        var lesson := _button("Attend lesson · 60 min · 2 EUR",_garden_action.bind("rebirth-study"))
        lesson.disabled=not bool(college.get("canStudy",false))
        _action_choices.add_child(lesson)
    _action_choices.add_child(_button("Leave",_close_college))

func _medical_station() -> String:
    var medical = _last_snapshot.get("rebirth",{}).get("medical")
    if _apartment_active or _current_place!="CLINIC" or not medical is Dictionary or not medical.get("atClinic",false):
        return ""
    if _avatar.position.distance_to(CLINIC_COUNTER)<1.4:return "consult"
    if _avatar.position.distance_to(PHARMACY_COUNTER)<1.4:return "first-aid"
    return ""

func _close_medical() -> void:
    _medical_menu=false
    _action_panel.visible=false
    _render_actions()
    Input.set_mouse_mode(Input.MOUSE_MODE_CAPTURED)

func _render_medical() -> void:
    var medical: Dictionary = _last_snapshot["rebirth"]["medical"]
    var clinic := _medical_station()=="consult"
    _action_title.text="Ambulatorio civico" if clinic else "Farmacia del Borgo"
    var info := Label.new()
    info.text="08:00–19:00 · Health %s/100 · %s" % [medical.get("health",100),"Ill" if medical.get("ill",false) else "Not ill"]
    _action_choices.add_child(info)
    var service := _button("Consultation · 15 EUR · 30 min" if clinic else "Basic pharmacy care · 5 EUR · 10 min",_garden_action.bind("rebirth-medical-"+_medical_station()))
    service.disabled=not bool(medical.get("canConsult" if clinic else "canFirstAid",false))
    _action_choices.add_child(service)
    var followup = medical.get("followup")
    if followup is Dictionary:
        var followup_label := Label.new()
        followup_label.text = tr("Recovery kit: %s/3 · completed days: %s") % [followup.get("remainingSupplies",0),followup.get("completedDays",0)]
        _action_choices.add_child(followup_label)
        if clinic:
            var exam := _button("Private assessment · 12 EUR · 20 min",_garden_action.bind("rebirth-medical-exam"))
            exam.disabled=not bool(followup.get("canExam",false))
            _action_choices.add_child(exam)
        else:
            var collect := _button("Collect recovery kit · 9 EUR · 10 min",_garden_action.bind("rebirth-medical-collect"))
            collect.disabled=not bool(followup.get("canCollect",false))
            _action_choices.add_child(collect)
    _action_choices.add_child(_button("Leave",_close_medical))

func _cafe_station() -> String:
    if _apartment_active or _current_place!="CAFE" or str(_last_snapshot.get("rebirth",{}).get("phase",""))!="LINKED_ADULT" or not _last_snapshot.get("rebirth",{}).get("cafe") is Dictionary:
        return ""
    if _avatar.position.distance_to(CAFE_COUNTER)<1.4:return "counter"
    if _avatar.position.distance_to(CAFE_TABLE)<1.4:return "table"
    return ""

func _close_cafe() -> void:
    _cafe_menu=false
    _action_panel.visible=false
    _render_actions()
    Input.set_mouse_mode(Input.MOUSE_MODE_CAPTURED)

func _render_cafe() -> void:
    var cafe: Dictionary = _last_snapshot["rebirth"]["cafe"]
    _action_title.text=str(cafe.get("name","Bar Centrale"))
    var info := Label.new()
    info.text="%s · %s" % [cafe.get("hours","07:00–21:00"),"Open" if cafe.get("open",false) else "Closed"]
    _action_choices.add_child(info)
    if _cafe_station()=="counter":
        for order in cafe.get("orders",[]):
            var button := _button("%s · %s EUR" % [order.get("label",""),order.get("price",0)],_garden_action.bind("rebirth-cafe-"+str(order.get("id",""))))
            button.disabled=not bool(order.get("available",false))
            _action_choices.add_child(button)
    else:
        var rest := _button("Rest at table · 20 min",_garden_action.bind("rebirth-cafe-rest"))
        rest.disabled=not bool(cafe.get("canRest",false))
        _action_choices.add_child(rest)
        for guest in cafe.get("guests",[]):
            _action_choices.add_child(_button("Talk with "+str(guest.get("name","")),_garden_action.bind("rebirth-cafe-chat-"+str(guest.get("id","")))))
    _action_choices.add_child(_button("Leave",_close_cafe))

func _near_grocery_counter() -> bool:
    var counter := BAKERY_COUNTER if _current_place=="BAKERY" else GROCERY_COUNTER
    return not _apartment_active and _current_place in ["PORTICI","BAKERY"] and str(_last_snapshot.get("rebirth",{}).get("phase",""))=="LINKED_ADULT" and _avatar.position.distance_to(counter)<1.5 and _last_snapshot.get("rebirth",{}).get("grocery") is Dictionary

func _close_grocery() -> void:
    _grocery_menu=false
    _action_panel.visible=false
    _render_actions()
    Input.set_mouse_mode(Input.MOUSE_MODE_CAPTURED)

func _render_actions() -> void:
    _replace_choices(_action_choices, true)
    if _software_menu and _near_software():
        _render_software()
        return
    _software_menu=false
    if _engineering_menu and _near_engineering():
        _render_engineering()
        return
    _engineering_menu=false
    if _college_menu and _near_college():
        _render_college()
        return
    _college_menu=false
    if _medical_menu and not _medical_station().is_empty():
        _render_medical()
        return
    _medical_menu=false
    if _cafe_menu and not _cafe_station().is_empty():
        _render_cafe()
        return
    _cafe_menu=false
    if _grocery_menu and _near_grocery_counter():
        var grocery: Dictionary = _last_snapshot["rebirth"]["grocery"]
        _action_title.text = str(grocery.get("name","Bottega Alimentare"))
        var info := Label.new()
        info.autowrap_mode=TextServer.AUTOWRAP_WORD_SMART
        info.text="%s · %s\nFood %s · Water %s · Capacity %s" % [grocery.get("hours","08:00–20:00"),"Open" if grocery.get("open",false) else "Closed",grocery.get("food",0),grocery.get("water",0),grocery.get("capacity",24)]
        _action_choices.add_child(info)
        for item in grocery.get("items",[]):
            var caption := "%s · %s EUR · %s left" % [item.get("label",""),item.get("price",0),item.get("stock",0)]
            var buy := _button(caption,_garden_action.bind("rebirth-grocery-"+str(item.get("id",""))))
            buy.disabled=not bool(item.get("available",false))
            _action_choices.add_child(buy)
        _action_choices.add_child(_button("Leave counter",_close_grocery))
        return
    _grocery_menu=false
    if _rebirth_active():
        var rebirth: Dictionary = _last_snapshot.get("rebirth",{})
        var info := Label.new()
        info.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
        if str(rebirth.get("phase",""))=="CHOICE":
            _action_title.text = "A new life · Choose your parents"
            var families: Array = rebirth.get("families",[])
            info.text = "The town continues. Choose an eligible family." if not families.is_empty() else "No eligible family today. Wait or start an adult life."
            _action_choices.add_child(info)
            var history: Array = rebirth.get("pastLives",[])
            if not history.is_empty():
                var recent: Dictionary = history[0]
                var memorial := Label.new()
                memorial.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
                var life_name = recent.get("name")
                memorial.text = "Previous life #%s · %s · day %s" % [recent.get("incarnation",0),"Unnamed visitor" if life_name == null else str(life_name),recent.get("day",0)]
                if int(recent.get("age",-1)) >= 0:
                    memorial.text += " · age %s" % recent.get("age",0)
                _action_choices.add_child(memorial)
            for family in families:
                var label_text := "%s (%s) + %s (%s) · %s" % [family.get("firstName",""),family.get("firstJob",""),family.get("secondName",""),family.get("secondJob",""),family.get("district","")]
                _action_choices.add_child(_button(label_text,_garden_action.bind("rebirth-family-%s-%s" % [family.get("first",0),family.get("second",0)])))
            _action_choices.add_child(_button("Start adult life instead",_garden_action.bind("rebirth-adult")))
        else:
            _action_title.text = "Childhood · family life"
            info.text = "%s · Age %s\nParents: %s and %s\nHome: %s\nF: choices / local first-person exploration from age 3. School and home routines continue; afternoon outings unlock at age 6." % [rebirth.get("childName",""),rebirth.get("age",0),rebirth.get("parentA",""),rebirth.get("parentB",""),rebirth.get("district","")]
            _action_choices.add_child(info)
            var age := int(rebirth.get("age",0))
            var identity = rebirth.get("identity")
            if identity is Dictionary:
                var personal := Label.new()
                personal.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
                personal.text = "Personal savings: %s EUR · Born on day %s\nFitness %s · Hobby days %s · Pocket money received %s EUR" % [identity.get("savings",0),identity.get("bornDay",0),identity.get("fitness",0),identity.get("hobbyDays",0),identity.get("allowanceTotal",0)]
                if age >= 18:
                    _action_title.text = "Coming of age · personal history"
                    if str(rebirth.get("phase","")) == "LINKED_ADULT":
                        _action_title.text = "Adult life · personal history"
                    else:
                        personal.text += "\nGraduation recorded. Continue as the same person."
                        _action_choices.add_child(_button("Continue adult life",_garden_action.bind("rebirth-continue")))
                _action_choices.add_child(personal)
                var personality = identity.get("personality")
                if personality is Dictionary:
                    var traits := Label.new()
                    traits.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
                    traits.text = "Sociability %s · Carefulness %s · Curiosity %s" % [personality.get("sociability",0),personality.get("carefulness",0),personality.get("curiosity",0)]
                    var ambition = personality.get("ambition")
                    if ambition is Dictionary:
                        traits.text += "\nAmbition: %s · %s/%s %s" % [ambition.get("description",""),ambition.get("progress",0),ambition.get("target",0),ambition.get("unit","")]
                    _action_choices.add_child(traits)
                    for goal in personality.get("choices",[]):
                        _action_choices.add_child(_button("Aim: "+str(goal).replace("_"," ").capitalize(),_garden_action.bind("rebirth-ambition-"+str(goal))))
                for person in identity.get("relationships",[]):
                    var relation := Label.new()
                    relation.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
                    relation.text = "%s · %s%s · Trust %s · Affinity %s" % [person.get("name",""),person.get("bond","Acquaintance")," (deceased)" if not person.get("alive",true) else "",person.get("trust",0),person.get("affinity",0)]
                    _action_choices.add_child(relation)
            if age >= 6:
                var skills: Dictionary = rebirth.get("skills",{})
                var school := Label.new()
                school.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
                var focus = rebirth.get("schoolFocus")
                school.text = "School days: %s · Focus: %s\nTechnical %s | Social %s | Academic %s | Commerce %s" % [rebirth.get("schoolDays",0),"balanced" if focus == null else str(focus),skills.get("technical",0),skills.get("social",0),skills.get("academic",0),skills.get("commerce",0)]
                _action_choices.add_child(school)
                if age < 18:
                    for subject in ["balanced","technical","social","academic","commerce"]:
                        _action_choices.add_child(_button("Focus: "+subject.capitalize(),_garden_action.bind("rebirth-focus-"+subject)))
            var child_options = rebirth.get("childOptions")
            if child_options is Dictionary:
                var outing := Label.new()
                outing.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
                var return_minute := int(child_options.get("returnMinute",1080))
                outing.text = "Afternoon outings · return starts at %02d:%02d" % [return_minute/60,return_minute%60]
                _action_choices.add_child(outing)
                _action_choices.add_child(_button("Advance one hour",_garden_action.bind("rebirth-child-hour")))
                for destination in child_options.get("destinations",[]):
                    _action_choices.add_child(_button("Visit "+str(destination).capitalize(),_garden_action.bind("rebirth-child-travel-"+str(destination))))
                for hobby in child_options.get("hobbies",[]):
                    _action_choices.add_child(_button("%s · %s min" % [hobby.get("label",""),hobby.get("minutes",30)],_garden_action.bind("rebirth-hobby-"+str(hobby.get("id","")))))
                var allowance := int(child_options.get("allowanceAvailable",0))
                if allowance > 0:
                    _action_choices.add_child(_button("Ask for weekly pocket money · %s EUR" % allowance,_garden_action.bind("rebirth-allowance")))
                if child_options.get("outing",false):
                    _action_choices.add_child(_button("Return home",_garden_action.bind("rebirth-child-home")))
            var options = rebirth.get("adultOptions")
            if options is Dictionary:
                var career := Label.new()
                career.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
                career.text = "%s · %s" % [options.get("occupation",""),options.get("activity","")]
                _action_choices.add_child(career)
                for job in options.get("jobs",[]):
                    _action_choices.add_child(_button("Apply: "+str(job),_garden_action.bind("rebirth-job-"+str(job))))
                _action_choices.add_child(_button("Resume work and daily routine",_garden_action.bind("rebirth-routine")))
                _action_choices.add_child(_button("Advance one hour",_garden_action.bind("rebirth-wait-hour")))
                _action_choices.add_child(_button("Advance one day",_garden_action.bind("rebirth-wait-adult-day")))
                for destination in options.get("destinations",[]):
                    if str(destination) != _current_place:
                        _action_choices.add_child(_button("Walk to "+str(destination).capitalize(),_garden_action.bind("rebirth-travel-"+str(destination))))
            for order in rebirth.get("engineeringOrders",[]):
                var task := Label.new()
                task.autowrap_mode=TextServer.AUTOWRAP_WORD_SMART
                task.text="Engineering · %s · %s · %s/100" % [order.get("title",""),order.get("district",""),order.get("condition",0)]
                _action_choices.add_child(task)
            var college = rebirth.get("college")
            if college is Dictionary and college.get("enrolled")!=null:
                var study_info := Label.new()
                study_info.text="Education · %s · %s/%s lessons" % [college.get("enrolled",""),college.get("completed",0),college.get("required",0)]
                _action_choices.add_child(study_info)
            var dojo = rebirth.get("dojo")
            if dojo is Dictionary:
                var dojo_label := Label.new()
                dojo_label.autowrap_mode=TextServer.AUTOWRAP_WORD_SMART
                dojo_label.text=tr("Shotokan · belt %s · Mon/Wed/Fri 18:00–21:00") % tr(str(dojo.get("belt","White")))
                dojo_label.text+="\n"+(tr("Kihon %s · Kata %s · Kumite %s · next target %s each") % [dojo.get("kihon",0),dojo.get("kata",0),dojo.get("kumite",0),dojo.get("required",3)])
                _action_choices.add_child(dojo_label)
                var community = rebirth.get("dojoCommunity")
                if community is Dictionary and _near_dojo():
                    for member in community.get("present",[]):
                        var member_label := Label.new()
                        member_label.text = "%s · %s" % [member.get("name",""),tr("Sensei" if member.get("role","")=="instructor" else "Student")]
                        _action_choices.add_child(member_label)
                    if community.get("instructorPresent",false):
                        _action_choices.add_child(_button("Rei · Greet sensei",_garden_action.bind("rebirth-dojo-rei")))
                if _near_dojo() and dojo.get("canTrain",false):
                    for mode in ["kihon","kata","kumite"]:
                        _action_choices.add_child(_button(tr("Practice %s · 60 min · 5 EUR") % tr(mode.capitalize()),_garden_action.bind("rebirth-dojo-"+mode)))
                if _near_dojo() and dojo.get("canExam",false):
                    _action_choices.add_child(_button(tr("Belt exam · %s · 15 EUR") % tr(str(dojo.get("nextBelt",""))),_garden_action.bind("rebirth-dojo-exam")))
            var match_view = rebirth.get("kumite")
            if match_view is Dictionary and _near_dojo():
                var match_label := Label.new()
                match_label.autowrap_mode=TextServer.AUTOWRAP_WORD_SMART
                match_label.text=tr("Controlled kumite · %s : %s") % [match_view.get("score",0),match_view.get("partnerScore",0)]
                _action_choices.add_child(match_label)
                if match_view.get("active",false):
                    var cue_names := {"close":"Partner approaches","far":"Partner keeps distance","opening":"Scoring opportunity"}
                    var cue_label := Label.new()
                    cue_label.text=tr("Exchange %s/5 · %s") % [match_view.get("round",1),tr(str(cue_names.get(str(match_view.get("cue","")),"")))]
                    _action_choices.add_child(cue_label)
                    for choice in ["guard","step","touch"]:
                        _action_choices.add_child(_button(tr(str({"guard":"Guard","step":"Adjust distance","touch":"Controlled point"}.get(choice))),_garden_action.bind("rebirth-dojo-match-"+choice)))
                    _action_choices.add_child(_button("End practice",_garden_action.bind("rebirth-dojo-match-stop")))
                elif match_view.get("canStart",false):
                    _action_choices.add_child(_button(tr("Practice with %s · 5 EUR") % str(match_view.get("partnerName","")),_garden_action.bind("rebirth-dojo-match-start")))
                elif match_view.get("completed",false):
                    var done := Label.new()
                    done.text=tr("Practice completed")
                    _action_choices.add_child(done)
            var sports = rebirth.get("sports")
            if sports is Dictionary:
                var sport_label := Label.new()
                sport_label.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
                sport_label.text = tr("Fitness %s · Endurance %s · Flexibility %s") % [sports.get("fitness",0),sports.get("endurance",0),sports.get("flexibility",0)]
                sport_label.text += "\n"+(tr("Strength %s · Karate %s · Sessions %s") % [sports.get("strength",0),sports.get("karate",0),sports.get("sessions",0)])
                _action_choices.add_child(sport_label)
                var sport_names := {"run":"Running","yoga":"Yoga","karate":"Karate","gym":"Gym training","cycling":"Cycling rental"}
                for option in sports.get("options",[]):
                    var sport_id := str(option.get("id",""))
                    var sport_name := tr(str(sport_names.get(sport_id,sport_id)))
                    if option.get("available",false):
                        _action_choices.add_child(_button(tr("%s · %s min · %s EUR") % [sport_name,option.get("minutes",0),option.get("price",0)],_garden_action.bind("rebirth-sport-"+sport_id)))
                    else:
                        var sport_hint := Label.new()
                        sport_hint.text = "%s · %s" % [sport_name,tr(str(option.get("district","")))]
                        _action_choices.add_child(sport_hint)
            var medical = rebirth.get("medical")
            if medical is Dictionary:
                var health_label := Label.new()
                health_label.text="Health %s/100 · %s" % [medical.get("health",100),"Ill until day "+str(medical.get("sickUntilDay",0)) if medical.get("ill",false) else "Not ill"]
                _action_choices.add_child(health_label)
                var followup = medical.get("followup")
                if followup is Dictionary:
                    var recovery_label := Label.new()
                    recovery_label.text = tr("Recovery kit: %s/3 · completed days: %s") % [followup.get("remainingSupplies",0),followup.get("completedDays",0)]
                    if int(followup.get("lastExamDay",0))>0:
                        recovery_label.text += "\n"+(tr("Last assessment: day %s · health %s/100") % [followup.get("lastExamDay",0),followup.get("assessedHealth",0)])
                    _action_choices.add_child(recovery_label)
                    if followup.get("canRecover",false):
                        _action_choices.add_child(_button("Supported recovery · 60 min",_garden_action.bind("rebirth-medical-recover")))
                if medical.get("canVisit",false):
                    _action_choices.add_child(_button("Visit medical district",_garden_action.bind("rebirth-medical-visit")))
            var home_options = rebirth.get("homeOptions")
            if home_options is Dictionary:
                var home_label := Label.new()
                home_label.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
                home_label.text = "Home · %s · %s\nFood portions %s · Water portions %s" % [home_options.get("district",""),"at home" if home_options.get("atHome",false) else "away",home_options.get("food",0),home_options.get("water",0)]
                home_label.text += "\nHygiene %s · Furnishings %s/2" % [home_options.get("hygiene",100),home_options.get("furnitureLevel",0)]
                for relative in home_options.get("family",[]):
                    home_label.text += "\nAt home: "+str(relative)
                var domestic = home_options.get("domestic")
                if domestic is Dictionary:
                    home_label.text += "\n"+(tr("Cleanliness %s · Clothes %s") % [domestic.get("cleanliness",100),domestic.get("clothes",100)])
                    home_label.text += "\n"+(tr("Electricity credits used %s · Water units used %s") % [domestic.get("electricityUses",0),domestic.get("waterUsed",0)])
                _action_choices.add_child(home_label)
                var home_labels := {"go":"Go to your home","meal":"Prepare meal · 15 min","drink":"Drink water · 5 min","rest":"Rest · 60 min","family":"Spend time with family · 30 min","wash":"Wash · 10 min","microwave":"Heat meal · 5 min · 1 EUR","clean":"Clean home · 30 min · 1 water","laundry":"Wash clothes · 45 min · 1 water · 1 EUR","upgrade":"Upgrade furnishings · %s EUR" % home_options.get("upgradePrice",0)}
                for action in home_options.get("actions",[]):
                    _action_choices.add_child(_button(str(home_labels.get(action,action)),_garden_action.bind("rebirth-home-"+str(action))))
                var wardrobe = home_options.get("wardrobe")
                if wardrobe is Dictionary:
                    var wardrobe_label := Label.new()
                    wardrobe_label.autowrap_mode=TextServer.AUTOWRAP_WORD_SMART
                    wardrobe_label.text=tr("Wardrobe · %s · weather comfort %s/100") % [tr(str(wardrobe.get("worn","casual")).capitalize()),wardrobe.get("comfort",100)]
                    _action_choices.add_child(wardrobe_label)
                    for outfit in wardrobe.get("items",[]):
                        var outfit_id := str(outfit.get("id",""))
                        var outfit_name := tr(outfit_id.capitalize())
                        if outfit.get("owned",false):
                            var condition_label := Label.new()
                            condition_label.text=tr("%s · condition %s/100") % [outfit_name,outfit.get("condition",100)]
                            _action_choices.add_child(condition_label)
                        if outfit.get("canBuy",false):
                            _action_choices.add_child(_button(tr("Buy outfit: %s · %s EUR") % [outfit_name,outfit.get("price",0)],_garden_action.bind("rebirth-wardrobe-buy-"+outfit_id)))
                        if outfit.get("canWear",false):
                            _action_choices.add_child(_button(tr("Wear %s") % outfit_name,_garden_action.bind("rebirth-wardrobe-wear-"+outfit_id)))
                var furnishing_names := {"kitchen":"Kitchen furnishings","bedroom":"Bedroom furnishings","children":"Children’s furnishings","laundry":"Washing machine"}
                for offer in home_options.get("furnishings",[]):
                    var item_id := str(offer.get("id",""))
                    var item_name := tr(str(furnishing_names.get(item_id,item_id)))
                    if offer.get("installed",false):
                        var installed_label := Label.new()
                        installed_label.text = tr("%s · installed") % item_name
                        _action_choices.add_child(installed_label)
                    elif offer.get("canBuy",false):
                        _action_choices.add_child(_button(tr("%s · %s EUR") % [item_name,offer.get("price",0)],_garden_action.bind("rebirth-home-furnish-"+item_id)))
                var properties = home_options.get("properties",[])
                if not properties.is_empty():
                    var market_label := Label.new()
                    market_label.text = "Property market · Office 09:00–17:00"
                    _action_choices.add_child(market_label)
                    for property in properties:
                        var property_label := Label.new()
                        property_label.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
                        property_label.text = "%s · %s · %s EUR" % [property.get("name",""),tr(str(property.get("district",""))),property.get("price",0)]
                        property_label.text += " · "+tr("Owned" if property.get("owned",false) else ("Available" if property.get("available",false) else "Unavailable"))
                        _action_choices.add_child(property_label)
                        if property.get("canBuy",false):
                            _action_choices.add_child(_button(tr("Buy home · %s EUR") % property.get("price",0),_garden_action.bind("rebirth-property-buy-"+str(property.get("id","")))))
                        if property.get("canSell",false):
                            _action_choices.add_child(_button(tr("Sell home · %s EUR") % property.get("resale",0),_garden_action.bind("rebirth-property-sell-"+str(property.get("id","")))))
            var family_options = rebirth.get("familyOptions")
            if family_options is Dictionary:
                var household := Label.new()
                household.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
                var spouse = family_options.get("partner")
                household.text = "Family · Single" if spouse == null else "Family · Partner: %s · %s" % [spouse.get("name",""),"shared home" if family_options.get("livingTogether",false) else "separate homes"]
                for kid in family_options.get("children",[]):
                    household.text += "\nChild: %s · age %s%s" % [kid.get("name",""),kid.get("age",0)," (deceased)" if not kid.get("alive",true) else ""]
                _action_choices.add_child(household)
                var budget = family_options.get("budget")
                if budget is Dictionary:
                    var budget_label := Label.new()
                    budget_label.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
                    budget_label.text = tr("Family savings: %s EUR · yours %s · partner %s") % [budget.get("total",0),budget.get("personalSavings",0),budget.get("partnerSavings",0)]
                    budget_label.text += "\n"+(tr("Outstanding bills: %s EUR · dependent children: %s") % [budget.get("unpaidBills",0),budget.get("children",0)])
                    _action_choices.add_child(budget_label)
                    if budget.get("canDeposit",false):
                        _action_choices.add_child(_button("Save 10 EUR for the family",_garden_action.bind("rebirth-budget-deposit")))
                    if budget.get("canWithdraw",false):
                        _action_choices.add_child(_button("Withdraw your 10 EUR savings",_garden_action.bind("rebirth-budget-withdraw")))
                    if budget.get("canPayBills",false):
                        _action_choices.add_child(_button("Pay bills from family savings",_garden_action.bind("rebirth-budget-bills")))
                for candidate in family_options.get("proposals",[]):
                    _action_choices.add_child(_button("Propose partnership to "+str(candidate.get("name","")),_garden_action.bind("rebirth-propose-"+str(candidate.get("id","")))))
                if family_options.get("canMoveTogether",false):
                    _action_choices.add_child(_button("Agree to share a home",_garden_action.bind("rebirth-move-together")))
                if family_options.get("canHaveChild",false):
                    _action_choices.add_child(_button("Welcome a child together",_garden_action.bind("rebirth-have-child")))
                if family_options.get("canSupport",false):
                    _action_choices.add_child(_button("Support partner · 10 EUR",_garden_action.bind("rebirth-support-partner")))
            for partner in rebirth.get("conversations",[]):
                _action_choices.add_child(_button("Talk with "+str(partner.get("name","")),_garden_action.bind("rebirth-chat-"+str(partner.get("id","")))))
        var max_days := int(rebirth.get("maxWaitDays",0))
        if max_days > 0:
            _action_choices.add_child(_button("Advance one shared day",_garden_action.bind("rebirth-wait")))
        if max_days > 1:
            _action_choices.add_child(_button("Advance up to %s shared days" % max_days,_garden_action.bind("rebirth-wait-fast")))
        return
    _action_title.text = "Local actions — " + _current_place.capitalize()
    var player: Dictionary = _last_snapshot.get("player", {})
    var employed := str(player.get("jobId", ""))
    if _near_truck():
        var cargo: Dictionary = _last_snapshot.get("freight",{})
        _action_title.text = "Freight truck · "+_cargo_caption(cargo)
        var next = cargo.get("nextDestination")
        if next!=null:
            _action_choices.add_child(_button("Drive truck to "+str(next).capitalize(),_drive_truck.bind(str(next))))
        else:
            var payout := "Company contract · 40 EUR" if bool(cargo.get("companyRun",false)) else "Earn 32 EUR"
            _action_choices.add_child(_button("Unload cargo · "+payout,_freight_action.bind("unload")))
        return
    if _near_car():
        _action_title.text = "Your car · choose adjacent destination"
        for id in player.get("carDestinations",[]):
            _action_choices.add_child(_button("Drive to %s · 3m / 1 fuel" % str(id).capitalize(),_drive_to.bind(str(id))))
        return
    var logistics: Dictionary = _last_snapshot.get("logistics",{})
    if _current_place in ["FARM","QUARRY","FACTORY"] and bool(logistics.get("yours",false)) and bool(logistics.get("truck",false)) and not bool(_last_snapshot.get("freight",{}).get("active",false)):
        var relocation := " · relocation 8 EUR + fuel" if str(logistics.get("truckPlace",""))!=_current_place else ""
        _action_choices.add_child(_button("Company freight contract · 40 EUR"+relocation,_freight_action.bind("load-company")))
    match _current_place:
        "FARM":
            if employed=="freight-driver" and not bool(_last_snapshot.get("freight",{}).get("active",false)):
                _action_choices.add_child(_button("Load produce truck · paid delivery",_freight_action.bind("load")))
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
            if employed=="freight-driver" and not bool(_last_snapshot.get("freight",{}).get("active",false)):
                _action_choices.add_child(_button("Load parts truck · paid delivery",_freight_action.bind("load")))
            _action_choices.add_child(_button("Apply: factory machinist",
                    _apply_job.bind("machinist")))
        "CENTRO":
            _action_choices.add_child(_button("Job noticeboard",_find_jobs))
        "NORTH_RESIDENCES", "SOUTH_RESIDENCES":
            if _near_orchard():
                var orchard: Dictionary = _last_snapshot.get("orchard",{})
                if not bool(orchard.get("cellar",false)):
                    _action_choices.add_child(_button("Install cellar · %s EUR" % orchard.get("cellarPrice",35),_garden_action.bind("orchard-build-cellar")))
                else:
                    var grapes := int(player.get("pantry",{}).get("grapes",0))
                    var cellar_info := Label.new()
                    cellar_info.text = "Cantina · Grapes in cupboard: %s" % grapes
                    if bool(orchard.get("wineReady",false)):
                        cellar_info.text += " | Wine ready"
                    elif int(orchard.get("wineDueDay",0))>0:
                        cellar_info.text += " | Wine ready day %s" % orchard.get("wineDueDay",0)
                    _action_choices.add_child(cellar_info)
                    if grapes>=2:
                        _action_choices.add_child(_button("Press grape juice · 2 grapes",_garden_action.bind("orchard-press-juice")))
                    if grapes>=3 and int(orchard.get("wineDueDay",0))==0 and not bool(orchard.get("wineReady",false)):
                        _action_choices.add_child(_button("Start wine batch · 3 grapes",_garden_action.bind("orchard-start-wine")))
                    if bool(orchard.get("wineReady",false)):
                        _action_choices.add_child(_button("Collect wine into cupboard",_garden_action.bind("orchard-collect-wine")))
                if not bool(orchard.get("hive",false)):
                    _action_choices.add_child(_button("Install beehive · 30 EUR",_garden_action.bind("orchard-build-hive")))
                else:
                    var bee_info := Label.new()
                    bee_info.text = "Honey %s/4 | %s" % [orchard.get("honey",0),"Winter rest" if bool(orchard.get("winter",false)) else ("Watered today" if bool(orchard.get("beesWateredToday",false)) else "Needs drinking water")]
                    _action_choices.add_child(bee_info)
                    if not bool(orchard.get("winter",false)) and not bool(orchard.get("beesWateredToday",false)) and int(_last_snapshot.get("minute",0))<1080:
                        _action_choices.add_child(_button("Water bees · 1 water",_garden_action.bind("orchard-water-bees")))
                    if int(orchard.get("honey",0))>0:
                        _action_choices.add_child(_button("Collect honey into cupboard",_garden_action.bind("orchard-collect-honey")))
                for plant in orchard.get("plants",[]):
                    var id := str(plant.get("id",""))
                    var label := Label.new()
                    label.text = "%s · %s | Growth %s/5 | Fruit %s/3" % [plant.get("label",id),plant.get("season",""),plant.get("age",0),plant.get("fruit",0)]
                    _action_choices.add_child(label)
                    if not bool(plant.get("planted",false)) and not bool(orchard.get("winter",false)) and not bool(orchard.get("wateredToday",false)):
                        _action_choices.add_child(_button("Plant %s · %s EUR" % [plant.get("label",id),plant.get("price",0)],_garden_action.bind("orchard-plant-"+id)))
                    elif bool(plant.get("ready",false)):
                        _action_choices.add_child(_button("Harvest "+id,_garden_action.bind("orchard-harvest-"+id)))
                if not bool(orchard.get("winter",false)) and not bool(orchard.get("rainy",false)) and not bool(orchard.get("wateredToday",false)) and int(_last_snapshot.get("minute",0))<1080:
                    _action_choices.add_child(_button("Water orchard · 1 water",_garden_action.bind("orchard-water")))
                    if int(_last_snapshot.get("garden",{}).get("rainwater",0))>0:
                        _action_choices.add_child(_button("Water orchard from barrel",_garden_action.bind("orchard-water-rain")))
            elif _near_coop():
                var hens: Dictionary = _last_snapshot.get("hens",{})
                if not bool(hens.get("coop",false)):
                    _action_choices.add_child(_button("Build chicken coop · 20 EUR",_garden_action.bind("build-coop")))
                else:
                    var status := Label.new()
                    status.text = "Hens %s/3 | Condition %s/100 | Nest %s/6\nFeed: %s | Water: %s" % [hens.get("hens",0),hens.get("health",0),hens.get("eggs",0),"done" if bool(hens.get("fedToday",false)) else "needed","done" if bool(hens.get("wateredToday",false)) else "needed"]
                    _action_choices.add_child(status)
                    if int(hens.get("hens",0))<3 and not bool(hens.get("fedToday",false)) and not bool(hens.get("wateredToday",false)):
                        _action_choices.add_child(_button("Buy hen · 12 EUR",_garden_action.bind("buy-hen")))
                    if int(hens.get("hens",0))>0 and int(_last_snapshot.get("minute",0))<1080:
                        if not bool(hens.get("fedToday",false)):
                            _action_choices.add_child(_button("Feed hens · 1 rice",_garden_action.bind("feed-hens")))
                        if not bool(hens.get("wateredToday",false)):
                            _action_choices.add_child(_button("Water hens · 1 drinking water",_garden_action.bind("water-hens")))
                    if int(hens.get("eggs",0))>0:
                        _action_choices.add_child(_button("Collect egg into cupboard",_garden_action.bind("collect-eggs")))
            elif _near_garden():
                var garden: Dictionary = _last_snapshot.get("garden",{})
                var info := Label.new()
                var covered := bool(garden.get("greenhouse",false))
                info.text = "%s · growth %s/3 | %s" % ["Serra privata" if covered else "Garden",garden.get("growth",0),"Watered today" if bool(garden.get("wateredToday",false)) else ("Manual watering required" if covered else ("Winter: dormant" if bool(garden.get("winter",false)) else ("Rain today" if bool(garden.get("rainy",false)) else "Dry today")))]
                if not covered:
                    _action_choices.add_child(_button("Build greenhouse · %s EUR" % garden.get("greenhousePrice",25),_garden_action.bind("build-greenhouse")))
                _action_choices.add_child(info)
                if not bool(garden.get("composter",false)):
                    _action_choices.add_child(_button("Build composter · %s EUR" % garden.get("composterPrice",10),_garden_action.bind("build-composter")))
                else:
                    var compost_info := Label.new()
                    compost_info.text = "Plant scraps %s/6 · Compost %s" % [garden.get("scraps",0),garden.get("compost",0)]
                    if int(garden.get("compostDueDay",0))>0:
                        compost_info.text += " · Ready day %s at 18:00" % garden.get("compostDueDay",0)
                    if bool(garden.get("fertilized",false)):
                        compost_info.text += " · Bed fertilized"
                    _action_choices.add_child(compost_info)
                    if int(garden.get("scraps",0))>=3 and int(garden.get("compostDueDay",0))==0 and int(garden.get("compost",0))==0:
                        _action_choices.add_child(_button("Start compost · 3 scraps",_garden_action.bind("start-compost")))
                    if int(garden.get("compost",0))>0 and bool(garden.get("planted",false)) and int(garden.get("growth",0))<3 and not bool(garden.get("fertilized",false)):
                        _action_choices.add_child(_button("Fertilize bed · +2 vegetables",_garden_action.bind("fertilize")))
                if bool(garden.get("barrel",false)):
                    var water_stock := Label.new()
                    water_stock.text = "Rain barrel: %s/8 (irrigation only)" % garden.get("rainwater",0)
                    _action_choices.add_child(water_stock)
                else:
                    _action_choices.add_child(_button("Install rain barrel · 8 EUR",_garden_action.bind("buy-barrel")))
                if not bool(garden.get("planted",false)):
                    if covered or not bool(garden.get("winter",false)):
                        _action_choices.add_child(_button("Plant vegetables · 3 EUR",_garden_action.bind("plant")))
                elif int(garden.get("growth",0))>=3:
                    _action_choices.add_child(_button("Harvest %s vegetables into cupboard" % garden.get("harvestUnits",4),_garden_action.bind("harvest")))
                elif (covered or (not bool(garden.get("winter",false)) and not bool(garden.get("rainy",false)))) and not bool(garden.get("wateredToday",false)) and int(_last_snapshot.get("minute",0))<1080:
                    _action_choices.add_child(_button("Water garden · 1 water",_garden_action.bind("water")))
                    if int(garden.get("rainwater",0))>0:
                        _action_choices.add_child(_button("Water from rain barrel · 1 unit",_garden_action.bind("water-rain")))
            elif _near_room_bed():
                _action_choices.add_child(_button("Sleep in your house · 2h" if _current_place=="SOUTH_RESIDENCES" else "Sleep in guest room · 2h",_sleep_room.bind(2)))
                _action_choices.add_child(_button("Sleep in your house · 8h" if _current_place=="SOUTH_RESIDENCES" else "Sleep in guest room · 8h",_sleep_room.bind(8)))
            elif _near_kitchen():
                var bag: Dictionary = player.get("bag",{"bread":player.get("bread",0),"water":player.get("bottledWater",0)})
                var pantry: Dictionary = player.get("pantry",{"bread":player.get("storedBread",0),"water":player.get("storedWater",0)})
                var stock := Label.new()
                stock.text = "Cupboard %s/24" % _inventory_count(pantry)
                _action_choices.add_child(stock)
                for item in ["bread","water","flour","eggs","pasta","vegetables","milk","rice","apples","pears","plums","grapes","honey","grape-juice","wine"]:
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
                _action_choices.add_child(_button("Honey bread · 5m (bread + honey)",_room_cook.bind("honey-bread")))
                _action_choices.add_child(_button("Garden soup · 20m (bread + veg + water)",_room_cook.bind("garden-soup")))
                _action_choices.add_child(_button("Omelette · 15m (eggs + milk)",_room_cook.bind("omelette")))
                for fruit in ["apple","pear","plum","grape"]:
                    if int(pantry.get(fruit+"s",0))>0:
                        _action_choices.add_child(_button("Prepare "+fruit+" plate · 5m",_room_cook.bind(fruit+"-plate")))
            elif _near_bath():
                _action_choices.add_child(_button("Wash · 10m",_room_wash))
            elif _near_kiosk():
                if _current_place=="SOUTH_RESIDENCES" and not _owns_house():
                    _action_choices.add_child(_button("Buy Casa del Glicine · 320 EUR",_buy_house))
                elif _current_place=="NORTH_RESIDENCES" and not _owns_house():
                    _action_choices.add_child(_button("Rent / renew guest room · 35 EUR / 7 days",_rent_room))
                if _has_room_lease():
                    var instruction := Label.new()
                    instruction.text = "Your room is across the street. Walk to the bed."
                    _action_choices.add_child(instruction)
        "MARKET", "PORTICI", "FUEL_STATION":
            if _current_place=="MARKET" and employed!="shop-assistant":
                _action_choices.add_child(_button("Apply: market assistant",_apply_job.bind("shop-assistant")))
            for item in _last_snapshot.get("shop",[]):
                var id := str(item.get("id",""))
                var stock := int(item.get("stock",0))
                var choice := _button("Buy %s · %s EUR (%s left)" % [
                    str(item.get("label",id)),str(item.get("price",0)),stock],_buy_product.bind(id))
                choice.disabled = stock <= 0
                choice.set_meta("sold_out",stock <= 0)
                _action_choices.add_child(choice)
            if _current_place == "MARKET":
                var stall: Dictionary = _last_snapshot.get("stall",{})
                if not bool(stall.get("registered",false)):
                    _action_choices.add_child(_button("Register private stall · 20 EUR",_garden_action.bind("stall-register")))
                else:
                    var stall_info := Label.new()
                    stall_info.text = "Bottega · Stock %s/12 | Sold %s | Revenue %s EUR" % [stall.get("stocked",0),stall.get("sales",0),stall.get("revenue",0)]
                    stall_info.text += "\nCustomers %s | Regulars %s | Referred %s" % [stall.get("customers",0),stall.get("regularCustomers",0),stall.get("recommendedCustomers",0)]
                    stall_info.text += "\nPayroll %s EUR | Wages paid %s EUR" % [stall.get("reserve",0),stall.get("wages",0)]
                    var seller = stall.get("sellerName")
                    if seller!=null:
                        stall_info.text += "\nSeller: %s | %s" % [str(seller),"Paid shift" if bool(stall.get("staffed",false)) else "Off duty"]
                    var supply: Dictionary = stall.get("supply",{})
                    stall_info.text += "\nFarm supply %s | Reserve %s EUR | Cargo %s | Delivered %s" % ["ON" if bool(supply.get("enabled",false)) else "OFF",supply.get("reserve",0),supply.get("cargo",0),supply.get("delivered",0)]
                    _action_choices.add_child(stall_info)
                    _action_choices.add_child(_button("Fund supply · 10 EUR",_garden_action.bind("stall-supply-fund")))
                    _action_choices.add_child(_button("Withdraw supply · 10 EUR",_garden_action.bind("stall-supply-withdraw")))
                    if bool(supply.get("enabled",false)):
                        _action_choices.add_child(_button("Pause farm orders",_garden_action.bind("stall-supply-disable")))
                    else:
                        _action_choices.add_child(_button("Farm contract · %s veg / %s EUR" % [supply.get("batchUnits",4),supply.get("batchCost",11)],_garden_action.bind("stall-supply-enable")))
                    _action_choices.add_child(_button("Fund payroll · 10 EUR",_garden_action.bind("stall-fund")))
                    _action_choices.add_child(_button("Withdraw payroll · 10 EUR",_garden_action.bind("stall-withdraw")))
                    if seller!=null:
                        _action_choices.add_child(_button("Dismiss seller",_garden_action.bind("stall-dismiss")))
                    else:
                        for candidate in stall.get("candidates",[]):
                            _action_choices.add_child(_button("Hire "+str(candidate.get("name",""))+" · 4 EUR/hour",_garden_action.bind("stall-hire-"+str(candidate.get("id","")))))
                    if bool(stall.get("open",false)):
                        _action_choices.add_child(_button("Close stall",_garden_action.bind("stall-close")))
                    else:
                        _action_choices.add_child(_button("Open stall",_garden_action.bind("stall-open")))
                    for item in stall.get("items",[]):
                        var item_id := str(item.get("id",""))
                        var shelf := int(item.get("stock",0))
                        if shelf==0 and int(player.get("bag",{}).get(item_id,0))==0:
                            continue
                        var price := int(item.get("price",3))
                        var shelf_label := Label.new()
                        shelf_label.text = "%s · %s in stock · %s EUR" % [item_id,shelf,price]
                        _action_choices.add_child(shelf_label)
                        var shelf_row := HBoxContainer.new()
                        _action_choices.add_child(shelf_row)
                        shelf_row.add_child(_button("Stock",_garden_action.bind("stall-stock-"+item_id)))
                        shelf_row.add_child(_button("Take",_garden_action.bind("stall-take-"+item_id)))
                        shelf_row.add_child(_button("-1 EUR",_garden_action.bind("stall-price-down-"+item_id)))
                        shelf_row.add_child(_button("+1 EUR",_garden_action.bind("stall-price-up-"+item_id)))
                var trade: Dictionary = _last_snapshot.get("garden",{})
                if int(player.get("bag",{}).get("vegetables",0))>0:
                    var sale := _button("Sell vegetables · 2 EUR / unit",_garden_action.bind("sell-vegetables"))
                    sale.disabled = int(trade.get("marketFree",0))<=0 or int(trade.get("marketBudget",0))<2
                    _action_choices.add_child(sale)
                var orchard: Dictionary = _last_snapshot.get("orchard",{})
                for fruit in orchard.get("plants",[]):
                    var fruit_id := str(fruit.get("id",""))
                    if int(player.get("bag",{}).get(fruit_id,0))>0:
                        var fruit_sale := _button("Sell "+fruit_id+" · 2 EUR / unit",_garden_action.bind("sell-fruit-"+fruit_id))
                        fruit_sale.disabled = int(fruit.get("marketFree",0))<=0 or int(orchard.get("marketBudget",0))<2
                        _action_choices.add_child(fruit_sale)
                if int(player.get("bag",{}).get("honey",0))>0:
                    var honey_sale := _button("Sell honey · 3 EUR / unit",_garden_action.bind("sell-honey"))
                    honey_sale.disabled = int(orchard.get("honeyMarketFree",0))<=0 or int(orchard.get("marketBudget",0))<3
                    _action_choices.add_child(honey_sale)
                for product_id in ["grape-juice","wine"]:
                    if int(player.get("bag",{}).get(product_id,0))>0:
                        var price := int(orchard.get("winePrice",8) if product_id=="wine" else orchard.get("juicePrice",5))
                        var free := int(orchard.get("wineMarketFree",0) if product_id=="wine" else orchard.get("juiceMarketFree",0))
                        var cellar_sale := _button("Sell %s · %s EUR" % [product_id,price],_garden_action.bind("sell-"+product_id))
                        cellar_sale.disabled = free<=0 or int(orchard.get("marketBudget",0))<price
                        _action_choices.add_child(cellar_sale)
                var eggs: Dictionary = _last_snapshot.get("hens",{})
                if int(player.get("bag",{}).get("eggs",0))>0:
                    var egg_sale := _button("Sell eggs · 2 EUR / unit",_garden_action.bind("sell-eggs"))
                    egg_sale.disabled = int(eggs.get("marketFree",0))<=0 or int(eggs.get("marketBudget",0))<2
                    _action_choices.add_child(egg_sale)

            elif _current_place=="FUEL_STATION":
                var fuel: Dictionary = _last_snapshot.get("fuel",{})
                var supply := Label.new()
                supply.text = "Fuel: %s/%s · %s EUR/unit" % [fuel.get("stock",0),fuel.get("capacity",48),fuel.get("price",2)]
                _action_choices.add_child(supply)
                if _car_place()==_current_place:
                    _action_choices.add_child(_button("Refuel · up to 3 units",_car_action.bind("refuel")))
                _action_choices.add_child(_button("Apply: fuel station attendant",_apply_job.bind("fuel-attendant")))
        "LOGISTICS_DEPOT":
            _render_logistics(logistics)
        "WORKSHOP":
            _render_logistics(logistics)
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
            var population: Dictionary = _last_snapshot.get("demography",{})
            var census := Label.new()
            census.text = "Anagrafe · Residents %s/%s | Children %s | Retired %s" % [population.get("alive",0),population.get("capacity",40),population.get("children",0),population.get("retired",0)]
            census.text += "\nBirths %s | Deaths %s | Pensions %s EUR" % [population.get("births",0),population.get("deaths",0),population.get("pensions",0)]
            _action_choices.add_child(census)
            var education: Dictionary = _last_snapshot.get("education",{})
            var school_info := Label.new()
            school_info.text = "Scuola · Pupils %s | Lessons %s | Graduates %s" % [education.get("pupils",0),education.get("lessons",0),education.get("graduates",0)]
            school_info.text += "\nSchool reserve %s EUR | Work experience days %s" % [education.get("schoolFunds",0),education.get("workDays",0)]
            _action_choices.add_child(school_info)
            var election: Dictionary = _last_snapshot.get("election",{})
            var mayor := Label.new()
            mayor.text = "Sindaco · %s | %s | Next election: day %s" % [election.get("mayor","Caretaker"),election.get("programme","Balanced"),election.get("nextDay",0)]
            mayor.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
            _action_choices.add_child(mayor)
            var plans := ["Balanced","Education","Healthcare","Infrastructure"]
            if bool(election.get("campaigning",false)):
                if not bool(election.get("registered",false)):
                    for i in range(1,4):
                        _action_choices.add_child(_button("Run for mayor · "+plans[i],_garden_action.bind("election-register-%s" % i)))
                for candidate in election.get("candidates",[]):
                    var candidate_label := Label.new()
                    candidate_label.text = "%s · %s" % [candidate.get("name",""),candidate.get("programme","")]
                    _action_choices.add_child(candidate_label)
                    if not bool(election.get("voted",false)):
                        _action_choices.add_child(_button("Vote for "+str(candidate.get("name","")),_garden_action.bind("election-vote-%s" % candidate.get("slot",0))))
            if bool(election.get("playerMayor",false)):
                for i in range(4):
                    _action_choices.add_child(_button("Budget programme · "+plans[i],_garden_action.bind("election-policy-%s" % i)))
            var opinion: Dictionary = _last_snapshot.get("opinion",{})
            var poll := Label.new()
            poll.text = "Opinione pubblica · Comune trust %s/100 | Stall %s/100" % [opinion.get("cityTrust",50),opinion.get("stallTrust",50)]
            poll.text += "\nAdult residents %s | Newspaper readers %s" % [opinion.get("respondents",0),opinion.get("readers",0)]
            _action_choices.add_child(poll)
            var comune: Dictionary = _last_snapshot.get("comune",{})
            var budget_info := Label.new()
            budget_info.text = "Comune · Budget %s EUR | Taxes %s EUR" % [comune.get("budget",0),comune.get("taxes",0)]
            budget_info.text += "\nEducation %s | Healthcare %s | Maintenance %s EUR" % [comune.get("education",0),comune.get("healthcare",0),comune.get("maintenance",0)]
            budget_info.text += "\nClinic reserve %s EUR | Medical aid %s EUR" % [comune.get("clinicFunds",0),comune.get("medicalAid",0)]
            _action_choices.add_child(budget_info)
            var construction: Dictionary = _last_snapshot.get("construction",{})
            var stage := int(construction.get("stage",0))
            var construction_info := Label.new()
            construction_info.text = "Cantiere Verdi · %s/3 | Steel %s | Parts %s | Crew EUR %s" % [stage,construction.get("steel",0),construction.get("parts",0),construction.get("reserve",0)]
            _action_choices.add_child(construction_info)
            if not bool(construction.get("owned",false)) and stage<3:
                _action_choices.add_child(_button("Buy plot · 60 EUR",_garden_action.bind("construction-buy")))
            elif bool(construction.get("owned",false)):
                if stage<3:
                    _action_choices.add_child(_button("Buy steel · 4 EUR (factory %s)" % construction.get("factorySteel",0),_garden_action.bind("construction-steel")))
                    _action_choices.add_child(_button("Buy parts · 5 EUR (factory %s)" % construction.get("factoryParts",0),_garden_action.bind("construction-parts")))
                    _action_choices.add_child(_button("Fund crew · 10 EUR",_garden_action.bind("construction-fund")))
                    if bool(construction.get("enabled",false)):
                        _action_choices.add_child(_button("Pause construction",_garden_action.bind("construction-pause")))
                    else:
                        _action_choices.add_child(_button("Authorize crew",_garden_action.bind("construction-start")))
                else:
                    _action_choices.add_child(_button("Sell finished house · 150 EUR",_garden_action.bind("construction-sell")))
                _action_choices.add_child(_button("Withdraw unused crew funds",_garden_action.bind("construction-withdraw")))
            var rental: Dictionary = _last_snapshot.get("rental",{})
            var property_info := Label.new()
            property_info.text = "Appartamento Verdi · North residences | %s" % ["Occupied" if bool(rental.get("occupied",false)) else "Vacant"]
            if bool(rental.get("owned",false)):
                property_info.text += "\nRent %s EUR / week | Revenue %s EUR" % [rental.get("weeklyRent",55),rental.get("revenue",0)]
                if bool(rental.get("occupied",false)):
                    property_info.text += "\nTenant: %s | Paid until day %s" % [str(rental.get("tenantName","")),rental.get("paidUntilDay",0)]
            _action_choices.add_child(property_info)
            if not bool(rental.get("owned",false)):
                _action_choices.add_child(_button("Buy apartment · 180 EUR",_garden_action.bind("rental-buy")))
            else:
                if bool(rental.get("listed",false)):
                    _action_choices.add_child(_button("Stop lease renewals",_garden_action.bind("rental-unlist")))
                else:
                    _action_choices.add_child(_button("List apartment · 55 EUR/week",_garden_action.bind("rental-list")))
                if not bool(rental.get("occupied",false)):
                    _action_choices.add_child(_button("Sell vacant apartment · 140 EUR",_garden_action.bind("rental-sell")))
            var bank: Dictionary = _last_snapshot.get("bank",{})
            var bank_info := Label.new()
            bank_info.text = "Banca · Personal %s EUR | Business %s EUR | Debt %s EUR" % [bank.get("personal",0),bank.get("business",0),bank.get("debt",0)]
            _action_choices.add_child(bank_info)
            _action_choices.add_child(_button("Deposit personal · 10 EUR",_garden_action.bind("bank-deposit-personal")))
            _action_choices.add_child(_button("Withdraw personal · up to 10 EUR",_garden_action.bind("bank-withdraw-personal")))
            if bool(bank.get("businessAvailable",false)):
                _action_choices.add_child(_button("Deposit business · 10 EUR",_garden_action.bind("bank-deposit-business")))
                _action_choices.add_child(_button("Withdraw business · up to 10 EUR",_garden_action.bind("bank-withdraw-business")))
            if int(bank.get("debt",0))>0:
                _action_choices.add_child(_button("Repay loan · up to 10 EUR",_garden_action.bind("bank-repay")))
            elif bool(bank.get("eligible",false)):
                _action_choices.add_child(_button("Borrow 50 EUR · weekly interest",_garden_action.bind("bank-borrow")))
            _action_choices.add_child(_button("Apply: engineer",
                    _apply_job.bind("engineer")))
            _action_choices.add_child(_button("Apply: investigator",
                    _apply_job.bind("investigator")))
        "SCHOOL":
            _action_choices.add_child(_button("Study engineering · 10 EUR",
                    _study.bind("engineering")))
        "LIBRARY":
            var newspaper = _last_snapshot.get("newspaper")
            if newspaper is Dictionary:
                var press := Label.new()
                press.text = "Il Giornale · Editorial funds %s EUR" % newspaper.get("funds",0)
                press.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
                _action_choices.add_child(press)
                _action_choices.add_child(_button("Apply: journalist · Investigation 2",_apply_job.bind("journalist")))
                if employed=="journalist":
                    var hint := Label.new()
                    hint.text = "Dispatch filed today." if bool(newspaper.get("filedToday",false)) else "Interview a resident in town, then return to the library for your shift."
                    hint.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
                    _action_choices.add_child(hint)
                var editions: Array = newspaper.get("editions",[])
                if not editions.is_empty():
                    var latest := Label.new()
                    latest.text = str(editions[0].get("text",""))
                    latest.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
                    _action_choices.add_child(latest)
            var archive = _last_snapshot.get("archive")
            if archive is Dictionary:
                var year := int(archive.get("year",1))
                var page := int(archive.get("page",1))
                var pages := int(archive.get("pages",1))
                var header := Label.new()
                header.text = "Archivio storico · Year %s · Page %s/%s" % [year,page,pages]
                _action_choices.add_child(header)
                if year>int(archive.get("firstYear",year)):
                    _action_choices.add_child(_button("Previous year",_garden_action.bind("archive-year-%s-1" % (year-1))))
                if year<int(archive.get("currentYear",year)):
                    _action_choices.add_child(_button("Next year",_garden_action.bind("archive-year-%s-1" % (year+1))))
                    _action_choices.add_child(_button("Current year",_garden_action.bind("archive-year-%s-1" % archive.get("currentYear",year))))
                for entry in archive.get("entries",[]):
                    var line := Label.new()
                    line.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
                    line.text = "Day %s · %s\n%s" % [entry.get("day",0),entry.get("category",""),entry.get("text","")]
                    _action_choices.add_child(line)
                if int(archive.get("total",0))==0:
                    var empty := Label.new()
                    empty.text = "No recorded milestones for this year."
                    _action_choices.add_child(empty)
                if page>1:
                    _action_choices.add_child(_button("Previous page",_garden_action.bind("archive-year-%s-%s" % [year,page-1])))
                if page<pages:
                    _action_choices.add_child(_button("Next page",_garden_action.bind("archive-year-%s-%s" % [year,page+1])))
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
        "journalist":
            work_site = "LIBRARY"
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

func _garden_action(action: String) -> void:
    if not _office_action_allowed(action):return
    if action.begins_with("rebirth-dojo-") and not _near_dojo():return
    if action.begins_with("rebirth-engineer-") and not _near_engineering():return
    if (action=="rebirth-study" or action.begins_with("rebirth-enroll-")) and not _near_college():return
    if action.begins_with("rebirth-medical-") and action!="rebirth-medical-visit":
        if action=="rebirth-medical-recover":
            if not _apartment_active:return
        elif action=="rebirth-medical-exam":
            if _medical_station()!="consult":return
        elif action=="rebirth-medical-collect":
            if _medical_station()!="first-aid":return
        elif action!="rebirth-medical-"+_medical_station():return
    if action.begins_with("rebirth-cafe-"):
        var station := _cafe_station()
        var table_action := action=="rebirth-cafe-rest" or action.begins_with("rebirth-cafe-chat-")
        if station!=("table" if table_action else "counter"):return
    if action.begins_with("rebirth-grocery-") and not _near_grocery_counter():
        return
    if action.begins_with("rebirth-"):
        if _connected and not _api.is_busy():
            _api.garden_action(action)
        return
    if ((action.begins_with("archive-year-") and _current_place=="LIBRARY" and _near_kiosk()) or ((action.begins_with("bank-") or action.begins_with("rental-") or action.begins_with("construction-") or action.begins_with("election-")) and _current_place=="OFFICE" and _near_kiosk()) or _near_garden() or _near_coop() or _near_orchard() or ((action in ["sell-vegetables","sell-eggs","sell-honey","sell-grape-juice","sell-wine"] or action.begins_with("sell-fruit-") or action.begins_with("stall-")) and _current_place=="MARKET" and _near_kiosk())) and _connected and not _api.is_busy():
        _api.garden_action(action)

func _buy_house() -> void:
    if _current_place=="SOUTH_RESIDENCES" and _near_kiosk() and _connected and not _api.is_busy():
        _api.buy_house()

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

func _render_aid_notices() -> void:
    var lines := PackedStringArray()
    for notice in _last_snapshot.get("aidNotices",[]):
        var item := tr("Water") if str(notice.get("item",""))=="water" else tr("Bread")
        lines.append(tr("%s shared %s with you today.") % [str(notice.get("name","")),item])
    _aid_label.text = "\n".join(lines)
    _aid_label.visible = not lines.is_empty()

func _update_bag() -> void:
    _render_aid_notices()
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
    _housing_label.text = "Housing: Casa del Glicine (owned)" if _owns_house() else ("Housing: North guesthouse · %s h left" % ceili(remaining/60.0) if bool(player.get("roomActive",false)) else "Housing: no room")
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

func _can_walk_roads() -> bool:
    return not _apartment_active and (not _rebirth_active() or str(_last_snapshot.get("rebirth",{}).get("phase",""))=="LINKED_ADULT")

func _near_family_entrance() -> bool:
    return is_instance_valid(_home_entrance) and _home_entrance.visible and not _apartment_active and _avatar.position.distance_to(_home_entrance.position)<2.0

func _request_home_transition(action: String) -> void:
    if _connected and not _api.is_busy():
        _home_transition=true
        _api.garden_action(action)

func _sync_family_entrance(home) -> void:
    if is_instance_valid(_home_entrance):
        _home_entrance.queue_free()
        _home_entrance=null
    if not home is Dictionary or not home.get("outsideHome",false) or _apartment_active:
        return
    _home_entrance=Node3D.new()
    _home_entrance.name="FamilyHomeEntrance"
    add_child(_home_entrance)
    var approach := _scenery.get_node_or_null("ViaDelleMagnolie/CasaMagnolia/HomeApproach") as Marker3D
    if approach != null:
        _home_entrance.position=approach.global_position
        _sign(_home_entrance,"Your home · E",Vector3(0,2.3,-0.6))
    else:
        _home_entrance.position=_open_anchor(Vector3(0,0,6))
        _box(_home_entrance,Vector3(1.25,2.25,0.16),Color("#987653"),Vector3(0,1.125,-0.65))
        _sign(_home_entrance,"Your home",Vector3(0,2.45,-0.65))

func _apartment_action() -> String:
    if not _apartment_active:
        return ""
    var home = _last_snapshot.get("rebirth",{}).get("homeOptions")
    return _apartment.nearby_action(_avatar.position,home.get("actions",[])) if home is Dictionary else ""

func _set_apartment(active: bool) -> void:
    if active == _apartment_active:
        return
    _apartment_active=active
    if active:
        if not is_instance_valid(_apartment):
            _apartment=FamilyApartment.new()
            add_child(_apartment)
        _apartment.show()
        for chunk in _chunks.values():
            chunk["root"].hide()
        _traffic_root.hide()
        _bike_model.hide()
        _residents_root=_apartment.get("residents")
        _apartment.set("entrance_open",false)
        _apartment.get_node("EntranceDoor").show()
        _apartment.get_node("DoorHandle").show()
        _avatar.position=FamilyApartment.SPAWN
        _view_yaw=0.0
        _view_pitch=0.0
    else:
        _apartment.hide()
        for resident in _apartment.get("residents").get_children():
            _apartment.get("residents").remove_child(resident)
            resident.queue_free()
        for chunk in _chunks.values():
            chunk["root"].show()
        _traffic_root.show()
        _activate_chunk(_current_place)
        _avatar.position=Vector3.ZERO
    _follow_player()

func _render_hud_summary(snapshot: Dictionary) -> void:
    var player: Dictionary = snapshot.get("player", {})
    var job = player.get("jobId")
    var profession := tr("unemployed") if job == null else str(job)
    _heading.text = "Living Alibi | " + str(player.get("place", "Park"))
    _status.text = tr("Visitor #%s | %s EUR | %s") % [
        str(player.get("incarnation", 1)), str(player.get("money", 50)), profession
    ]
    if _rebirth_active():
        var child: Dictionary = snapshot.get("rebirth",{})
        _status.text = tr("Choose a new family") if str(child.get("phase","")) == "CHOICE" else tr("%s | Age %s | %s") % [child.get("childName",""),child.get("age",0),tr("Adult") if str(child.get("phase","")) == "LINKED_ADULT" else tr("Childhood")]
    var climate: Dictionary = snapshot.get("weather", {})
    _weather.text = tr("Day %s  %s:%02d | %s | %s  %s °C") % [
        str(snapshot.get("day", 1)), int(snapshot.get("minute", 450))/60,
        int(snapshot.get("minute", 450))%60, tr(str(snapshot.get("season", "SPRING"))),
        tr(str(climate.get("sky", "CLEAR"))), str(climate.get("temperatureC", 0))
    ]
    _needs.text = tr("Hunger %s  Thirst %s  Fatigue %s  Health %s  Eng %s  Inv %s") % [
        str(player.get("hunger", 0)), str(player.get("thirst", 0)),
        str(player.get("fatigue", 0)), str(player.get("health", 100)),
        str(player.get("engineeringSkill", 0)), str(player.get("investigationSkill", 0))
    ]
    _needs.text += tr(" | Hygiene %s") % str(player.get("hygiene",100))
    var utilities: Dictionary = snapshot.get("services", {})
    var fountain := "working" if utilities.get("fountainWorking", false) else "closed"
    var lighting := "lit" if utilities.get("lightsWorking", false) else "dark"
    _needs.text += tr("\nPark fountain: %s (%s clean units) | Street lights: %s") % [
        tr(fountain), str(utilities.get("cleanWater", 0)), tr(lighting)
    ]
    _needs.text += tr("\nResidents present in this district: %s") % [
        str(snapshot.get("residents", []).size())
    ]

func _on_world_updated(snapshot: Dictionary) -> void:
    var clock_update := _clock_pending
    var walking_transition := _home_transition or not _road_pending.is_empty()
    _home_transition=false
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
        _office_app="work"
        _office_joke_hold=0.0
        _office_joke_wait=8.0
        _software_menu=false
        _engineering_menu=false
        _college_menu=false
        _medical_menu=false
        _cafe_menu=false
        _grocery_menu=false
        if _apartment_active:
            _set_apartment(false)
        _close_dialog()
        _current_place = new_place
        _render_place(new_place)
        var car_arrival := not old_car_place.is_empty() and old_car_place!=_car_place() and _car_place()==new_place
        _avatar.position = _truck_exit(new_place) if truck_arrival else (CAR_ARRIVAL if car_arrival else (_road_arrival if road_arrival else Vector3.ZERO))
        if not _can_stand(_avatar.position, WALKER_RADIUS, true):
            _avatar.position = Vector3.ZERO
        _locations.clear() # Previous-origin travel estimates are now stale.
        _follow_player()
        _action_panel.visible = false
        _travel_panel.visible = false
    if continue_in_cabin:
        _park_in_cabin()

    _render_hud_summary(snapshot)
    var home = snapshot.get("rebirth",{}).get("homeOptions")
    _set_apartment(home is Dictionary and bool(home.get("atHome",false)))
    if _apartment_active:
        _apartment.set_furniture_level(int(home.get("furnitureLevel",0)))
        _apartment.set_home_style(str(home.get("interiorStyle","standard")))
        _apartment.set_room_packages(home.get("furnishings",[]))
    _sync_family_entrance(home)
    if walking_transition and is_instance_valid(_home_entrance) and not road_arrival:
        _avatar.position=_home_entrance.position
    _sync_guest_room()
    _sync_garden()
    _sync_coop()
    _sync_orchard()
    _sync_construction_site()
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
            _dialog_attitude.text = tr("Attitude: %s") % tr(str(person.get_meta("relationship","STRANGER")).capitalize())
            _render_dialog_mood(person)
    _apply_weather()
    _render_actions()
    if _rebirth_active():
        _travel_panel.visible = false
        _map_panel.visible = false
        _close_dialog()
        _action_panel.visible = not walking_transition
        Input.set_mouse_mode(Input.MOUSE_MODE_VISIBLE if _action_panel.visible else Input.MOUSE_MODE_CAPTURED)
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
    _sun.light_energy = lerpf(0.08,1.18,daylight)
    _sun.light_color=Color("#f1bd83").lerp(Color("#fff5df"),smoothstep(0.0,0.65,daylight))
    _environment.ambient_light_energy = lerpf(0.28,0.72,daylight)
    _environment.background_color = Color(0.07,0.10,0.18).lerp(
            _environment.background_color, smoothstep(0.0,0.35,daylight))
    if climate.get("sky","CLEAR") != "CLEAR":
        _sun.light_energy *= 0.65
    var horizon := Color("#e9cba8").lerp(Color("#d9e7ef"),smoothstep(0.0,0.7,daylight))
    var top := Color("#456fa5").lerp(Color("#548fc9"),daylight)
    if climate.get("sky","CLEAR")!="CLEAR":
        horizon=Color("#b7bdc2")
        top=Color("#75818d")
    _sky_material.sky_top_color=Color("#101a31").lerp(top,smoothstep(0.0,0.35,daylight))
    _sky_material.sky_horizon_color=Color("#253044").lerp(horizon,smoothstep(0.0,0.35,daylight))
    _sky_material.ground_horizon_color=_sky_material.sky_horizon_color
    _sky_material.ground_bottom_color=Color("#272e28").lerp(Color("#777f68"),daylight)
    _weather_fx.follow(_avatar.position)
    _weather_fx.set_sheltered(_apartment_active or _inside_guest_room(),_environment)
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
            _dialog_reply.text = _localized_resident_reply(message)
            _dialog_input.clear()
    if not _clock_pending:
        _message.text = tr(message)

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
    _home_transition=false
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
