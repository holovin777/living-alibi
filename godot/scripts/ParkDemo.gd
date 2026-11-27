extends Node3D
## Living Alibi vertical slice: move around a local public scene; travel and
## careers are validated by the real Java/Spring server, never by a fake client wallet.
## WASD/arrow keys: walk relative to the camera. Click the world for mouse look.
## First-person only. Escape: release mouse. E: gate. F: local kiosk.

const GATE_POSITION := Vector3(0.0, 0.0, -12.0)
const KIOSK_POSITION := Vector3(7.0, 0.0, 3.0)
const SPORT_COUNTER := Vector3(12.5,0,2.0)
const LIBRARY_DESK := Vector3(-7,0,6.1)
const DEPOT_DESK := Vector3(-9.0,0.0,10.0)
const DEPOT_BAYS := [Vector3(6.5,0,12.0),Vector3(12.5,0,12.0)]
const INTERACTION_RADIUS := 3.5
const WALK_SPEED := 4.5
const RUN_SPEED := 6.5
const CROUCH_SPEED := 2.2
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
const JUMP_GRAVITY := 9.8
const JUMP_SPEED := 4.0
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
const TownAudio = preload("res://scripts/TownAudio.gd")
const FirstPersonHands = preload("res://scripts/FirstPersonHands.gd")
const TownPhone = preload("res://scripts/TownPhone.gd")
const TownVisuals = preload("res://scripts/TownVisuals.gd")
var _sky_material: ProceduralSkyMaterial
var _surface_textures: Dictionary = {}
var _graphics_quality := 1
var _leaf_mesh: SphereMesh
var _leaf_materials: Array[StandardMaterial3D] = []
var _foliage_season := ""
var _water_material: ShaderMaterial
var _lantern_glass: StandardMaterial3D
var _shop_glass: StandardMaterial3D
var _graphics_details: Array[Node3D] = []
const FamilyApartment = preload("res://scripts/FamilyApartment.gd")
const PublicStairwell = preload("res://scripts/PublicStairwell.gd")
const GuestApartment = preload("res://scripts/GuestApartment.gd")
const GameLanguage = preload("res://scripts/GameLanguage.gd")
const ResidentPersonality = preload("res://scripts/ResidentPersonality.gd")
const ResidentAppearance = preload("res://scripts/ResidentAppearance.gd")
var _guide_label: Label
var _guide_enabled := true
var _apartment: Node3D
var _apartment_active := false
var _stairwell: Node3D
var _stairwell_active := false
var _stairwell_return := Vector3.ZERO
var _stairwell_hint: Label
var _guest: Node3D
var _guest_active := false
var _guest_return := Vector3.ZERO
var _guest_pending := false
var _guest_host: Node3D
var _host_activity := ""
var _host_path: Array[Vector3] = []
var _host_seated := false
var _guest_sitting := false
var _guest_stand_at := Vector3.ZERO
var _guest_feedback := ""
var _guest_feedback_time := 0.0
var _guest_requested_action := ""
var _home_entrance: Node3D
var _home_transition := false
var _grocery_menu := false
const GROCERY_COUNTER := Vector3(7.3,0,3.8)
const BAKERY_COUNTER := Vector3(-6.3,0,7.0)
const CAFE_COUNTER := Vector3(-7.0,0,5.9)
const BARBER_COUNTER := Vector3(-7.8,0,6.4)
const PIZZA_COUNTER := Vector3(-6.0,0,18.0)
const GELATO_COUNTER := Vector3(6.0,0,18.0)
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
# Vertical presentation is separate from the existing planar collision anchor.
var _jump_elapsed := -1.0
var _jump_height := 0.0
var _crouch_amount := 0.0
var _step_phase := 0.0
var _step_bob := 0.0
var _head_bob_enabled := true

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
var _town_audio: Node3D
var _hands: Node3D
var _held_label: Label
var _phone: PanelContainer
var _intercom: PanelContainer
var _intercom_title: Label
var _intercom_reply: Label
var _intercom_contacts: OptionButton
var _intercom_code := 0
var _intercom_pending := false
var _phone_alert: Label
var _phone_alert_remaining := 0.0
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
var _destination_filter: LineEdit
var _action_panel: PanelContainer
var _dialog_panel: PanelContainer
var _dialog_title: Label
var _dialog_reply: Label
var _dialog_attitude: Label
var _dialog_style: Label
var _language_selector: OptionButton
var _dialog_input: LineEdit
var _dialog_language: OptionButton
var _dialog_history: RichTextLabel
var _dialog_more_scroll: ScrollContainer
var _spoken_dialog_enabled := true
var _hud_details: VBoxContainer
var _hud_scroll: ScrollContainer
var _interface_theme: Theme
var _music: AudioStreamPlayer
var _music_value: Label
var _dialog_target := ""
var _dialog_pending := false
var _custody_panel: PanelContainer
var _custody_label: Label
var _custody_cell: Node3D
var _custody_active := false
var _dialog_started_at := 0
var _dialog_end_remaining := 0.0
var _dialog_farewell := ""
var _action_title: Label
var _action_choices: VBoxContainer

func _ready() -> void:
    GameLanguage.install()
    GameLanguage.select_locale(GameLanguage.saved_locale(),false)
    _build_world()
    _music = preload("res://scripts/TownMusic.gd").new()
    add_child(_music)
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
    if _custody_active:
        return
    _tick_dialog_autonomy(delta)
    _music.update_mix(delta,_dialog_panel.visible and _spoken_dialog_enabled)
    _tick_public_door(delta)
    if _phone_alert_remaining>0.0:
        _phone_alert_remaining = maxf(0.0,_phone_alert_remaining-delta)
        if _phone_alert_remaining<=0.0:
            _phone_alert.hide()
    if _drive_active():
        _reset_jump()
        _reset_locomotion()
        if not _drive_parked:
            _process_driving(delta)
        else:
            _update_drive_hud()
        _advance_traffic(delta)
        _animate_residents(delta)
        _weather_fx.follow(_avatar.position)
        _update_town_audio(delta,0.0)
        _hands.update_pose(delta,0.0,false)
        return
    var direction := Vector3.ZERO
    var moved := 0.0
    # Do not move away from an open clickable interaction menu.
    if not _guest_sitting and not _guest_pending and _road_pending.is_empty() and not _travel_panel.visible and not _action_panel.visible and not _map_panel.visible and not _dialog_panel.visible and not _phone.visible and not _intercom.visible:
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
        var speed := _movement_speed()
        _avatar.position = _move_among_people(_avatar.position,
                relative_motion * delta * speed, WALKER_RADIUS, null, _can_walk_roads())
        moved = before.distance_to(_avatar.position)
        if speed == CYCLE_SPEED:
            _bike_wheel_angle += moved/0.35
        if _can_walk_roads() and _jump_elapsed < 0.0:
            _try_road_crossing(relative_motion)
    _advance_jump(delta)
    _advance_locomotion(delta,moved)
    _follow_player()
    _update_town_audio(delta,moved)
    _hands.update_pose(delta,_step_bob,not _cycling() and (not _rebirth_active() or _child_can_walk()))
    if _guest_active:
        _weather_fx.follow(_avatar.position)
        _weather_fx.set_sheltered(true,_environment)
        _animate_guest_host(delta)
        var hints := tr("Visiting %s") % _guest.resident_name
        if not _host_activity.is_empty():hints += "\n"+tr("Host: %s") % tr(_host_activity.capitalize().replace("_"," "))
        var visit = _last_snapshot.get("apartmentVisit",{})
        if visit is Dictionary:
            var household = visit.get("household",{})
            if household is Dictionary and int(household.get("members",0))>1:
                hints += "\n"+tr("Household: %s · at home: %s") % [household.get("members",0),household.get("present",0)]
        if _guest_sitting:
            hints += "\n"+tr("E: stand up · R: relax")
        else:
            if _guest.near_exit(_avatar.position):hints += "\n"+tr("G: leave apartment")
            if _guest.near_sofa(_avatar.position):hints += "\n"+tr("E: sit on sofa")
            if _guest.near_table(_avatar.position):hints += "\n"+tr("C: coffee · F: snack · V: dinner")
        if not _nearest_resident().is_empty():
            hints += "\n"+tr("T or click: talk to a person")
        if is_instance_valid(_guest_host) and _avatar.position.distance_to(_guest_host.position)<2.5:
            hints += "\n"+tr("Ask the host to introduce their family.")
        _guest_feedback_time = maxf(0.0,_guest_feedback_time-delta)
        if _guest_feedback_time>0.0:hints += "\n"+_guest_feedback
        _stairwell_hint.text = hints
        _tick_live_clock(delta)
        return
    if _stairwell_active:
        _weather_fx.follow(_avatar.position)
        _weather_fx.set_sheltered(true,_environment)
        var unit: int = _stairwell.near_unit(_avatar.position)
        var invitation = _last_snapshot.get("apartmentVisit")
        if _stairwell.near_elevator(_avatar.position):
            var services: Dictionary = _last_snapshot.get("magnoliaServices",{})
            _stairwell_hint.text=(tr("Elevator · floor %s · E: up · R: down") % _stairwell.floor_number(_avatar.position)) if bool(services.get("liftWorking",true)) else tr("Elevator out of service · use stairs")
        elif unit>0 and invitation is Dictionary and int(invitation.get("unit",0))==unit:
            _stairwell_hint.text=tr("E: enter apartment %s") % unit
        elif unit>0:
            _stairwell_hint.text=tr("Apartment %s · locked") % unit
        else:
            _stairwell_hint.text = tr("G: leave stairwell") if _stairwell.near_exit(_avatar.position) else tr("Stairwell · floor %s · apartment doors locked") % _stairwell.floor_number(_avatar.position)
        if _stairwell.near_exit(_avatar.position):
            var condo: Dictionary = _last_snapshot.get("condominium",{})
            if not condo.is_empty():
                _stairwell_hint.text+="\n"+(tr("Building reserve: %s EUR · unpaid contributions: %s EUR") % [condo.get("reserve",0),condo.get("arrears",0)])
                var status := str(condo.get("maintenanceStatus",""))
                if not status.is_empty():
                    _stairwell_hint.text+="\n"+tr("Maintenance: %s") % tr(status)
                if condo.has("dirt"):
                    _stairwell_hint.text+="\n"+(tr("Dirt: %s/100 · %s") % [condo.get("dirt",0),tr(str(condo.get("cleaningStatus","")))])

        if _stairwell.near_exit(_avatar.position):
            var postal: Dictionary = _last_snapshot.get("postal",{})
            if not postal.is_empty():
                _stairwell_hint.text+="\n"+(tr("Mail service: %s") % tr(str(postal.get("status","POSTAL_IDLE"))))
        if not _nearest_resident().is_empty():
            _stairwell_hint.text+="\n"+tr("T or click: talk to a person")
        _tick_live_clock(delta)
        return

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
    _weather_fx.set_sheltered(_stairwell_active or _apartment_active or _inside_guest_room(),_environment)
    if _rebirth_active():
        _update_first_steps()
        if not _nearest_resident().is_empty():
            _hint.text=tr("Talk: approach someone and press T or click on them.")
        elif _near_software():
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
    elif _near_stairwell():
        _hint.text = tr("G: enter stairwell · E: intercom")
    elif _near_intercom()>0:
        _hint.text = tr("E: ring the intercom")
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
    elif _near_public_counter():
        _hint.text=tr("E / F: use the counter · Esc: close")
    elif _near_kiosk():
        _hint.text = tr("F: local interactions. Esc: cursor.")
    elif not _nearest_resident().is_empty():
        _hint.text = "T: talk to nearby resident · Esc: cursor."
    else:
        _hint.text = tr("WASD: walk · Shift: run · Ctrl: crouch · Space: jump · Esc: cursor.")

func _unhandled_input(event: InputEvent) -> void:
    if _custody_active:return
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
        if not (_travel_panel.visible or _action_panel.visible or _map_panel.visible or _dialog_panel.visible or _phone.visible or _intercom.visible):
            if Input.mouse_mode == Input.MOUSE_MODE_CAPTURED and (not _rebirth_active() or _child_can_walk()):
                if _hands.held_item=="sport":
                    _try_shoot()
                    get_viewport().set_input_as_handled()
                    return
                var looked_at := _looked_at_resident()
                if not looked_at.is_empty():
                    _open_dialog(looked_at)
                    get_viewport().set_input_as_handled()
                    return
            Input.set_mouse_mode(Input.MOUSE_MODE_CAPTURED)

func _unhandled_key_input(event: InputEvent) -> void:
    if _custody_active:return
    if _dialog_panel.visible and event is InputEventKey and event.pressed and not event.echo:
        if event.keycode == KEY_ESCAPE:
            _close_dialog()
        get_viewport().set_input_as_handled()
        return
    if event is InputEventKey and event.pressed and not event.echo:
        if _guest_active:
            if event.keycode==KEY_G and not _guest_sitting and _guest.near_exit(_avatar.position):
                _guest_action("leave")
            elif event.keycode==KEY_E and not _guest_pending:
                _toggle_guest_seat()
            elif event.keycode==KEY_T:
                _open_dialog()
            elif event.keycode==KEY_C:
                _guest_action("coffee")
            elif event.keycode==KEY_F:
                _guest_action("snack")
            elif event.keycode==KEY_V:
                _guest_action("dinner")
            elif event.keycode==KEY_R:
                _guest_action("rest")
            get_viewport().set_input_as_handled()
            return
        if _stairwell_active:
            if event.keycode==KEY_T:
                _open_dialog()
                get_viewport().set_input_as_handled()
                return
            if event.keycode in [KEY_E,KEY_R] and _stairwell.near_elevator(_avatar.position):
                var services: Dictionary = _last_snapshot.get("magnoliaServices",{})
                if not bool(services.get("liftWorking",true)):
                    get_viewport().set_input_as_handled()
                    return
                var destination: Vector3 = _stairwell.elevator_destination(_avatar.position,1 if event.keycode==KEY_E else -1)
                if destination!=_avatar.position:
                    _avatar.position=destination
                    _reset_locomotion()
                    _follow_player()
                    _town_audio.play_effect("door")
            elif event.keycode==KEY_E:
                _guest_action("enter")
            if event.keycode==KEY_G and _stairwell.near_exit(_avatar.position):
                _set_stairwell(false)
            get_viewport().set_input_as_handled()
            return
        if event.keycode==KEY_G and _near_stairwell() and _foot_controls_available():
            _set_stairwell(true)
            get_viewport().set_input_as_handled()
            return
    if event is InputEventKey and event.pressed and not event.echo and _intercom.visible:
        if event.keycode in [KEY_E,KEY_ESCAPE]:
            _close_intercom()
        get_viewport().set_input_as_handled()
        return
    if event is InputEventKey and event.pressed and not event.echo:
        if event.keycode==KEY_H:
            _toggle_phone()
            get_viewport().set_input_as_handled()
            return
        if _phone.visible:
            if event.keycode==KEY_ESCAPE:
                _close_phone()
            get_viewport().set_input_as_handled()
            return
    if event is InputEventKey and event.pressed and not event.echo and event.keycode == KEY_SPACE:
        if _can_jump():
            _jump_elapsed = 0.0
            get_viewport().set_input_as_handled()
        return
    if event is InputEventKey and event.pressed and not event.echo and event.keycode == KEY_X:
        if _hands.held_item=="sport":
            _try_shoot()
        else:
            _try_strike()
        get_viewport().set_input_as_handled()
        return
    if event is InputEventKey and event.pressed and not event.echo and event.keycode == KEY_Q:
        if _foot_controls_available():
            _cycle_held_item()
            get_viewport().set_input_as_handled()
        return
    if _rebirth_active():
        if event is InputEventKey and event.pressed and not event.echo and _child_can_walk():
            if event.keycode == KEY_T and not _dialog_panel.visible:
                _open_dialog()
                get_viewport().set_input_as_handled()
                return
            if event.keycode == KEY_E and _apartment_active and not _action_panel.visible:
                var action := _apartment_action()
                if action=="door":
                    var was_open: bool = _apartment.get("entrance_open")
                    _apartment.toggle_entrance(_avatar.position)
                    if was_open != bool(_apartment.get("entrance_open")):
                        _town_audio.play_effect("door")
                        _hands.reach()
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
                _action_panel.visible = false
                Input.set_mouse_mode(Input.MOUSE_MODE_CAPTURED)
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
            if _near_intercom()>0 and _foot_controls_available():
                _open_intercom()
            elif _near_public_counter():
                _toggle_actions()
            elif _near_gate():
                _toggle_travel()
            else:
                _message.text=tr("Find the yellow TRAVEL gate. Follow the distance and direction in First steps.")
            get_viewport().set_input_as_handled()
        elif event.keycode == KEY_F and not _map_panel.visible:
            _toggle_actions()
            get_viewport().set_input_as_handled()

func _toggle_hud_details(enabled: bool) -> void:
    _hud_details.visible = enabled
    _hud_scroll.visible = enabled
    if not enabled:
        _main_panel.reset_size()

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
        if _child_can_walk():
            _guide_label.text+="\n"+tr("Talk: approach someone and press T or click on them.")
        if not _entrance_hint().is_empty():_guide_label.text+="\n"+_entrance_hint()
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
    var service_target := GROCERY_COUNTER if _current_place=="PORTICI" else (DEPOT_DESK if _current_place=="LOGISTICS_DEPOT" else KIOSK_POSITION)
    _guide_label.text=guide+"\n"+(tr("Travel E: %s · Services F: %s · Map M · Talk T · Space: jump · Shift: run · Ctrl: crouch") % [_guide_direction(GATE_POSITION),_guide_direction(service_target)])
    if not _entrance_hint().is_empty():_guide_label.text+="\n"+_entrance_hint()
    if _current_place in ["MARKET","WORKSHOP"] and not _rebirth_active():
        _guide_label.text+="\n"+("%s · F: %s" % ["Armeria" if _current_place=="MARKET" else "Poligono di tiro",_guide_direction(SPORT_COUNTER)])
    var safety: Dictionary = _last_snapshot.get("publicSafety",{})
    if bool(safety.get("localAlert",false)):
        _guide_label.text+="\n"+tr("Public safety alert in this district.")

func _near_gate() -> bool:
    return _avatar.position.distance_to(GATE_POSITION) < INTERACTION_RADIUS

func _near_sport_counter() -> bool:
    return _current_place in ["MARKET","WORKSHOP"] and not _apartment_active and not _guest_active and not _stairwell_active and _avatar.position.distance_to(SPORT_COUNTER)<1.8

func _near_charity() -> bool:
    return _current_place=="LIBRARY" and not _apartment_active and not _guest_active and not _stairwell_active and _avatar.position.distance_to(Vector3(8,0,7.4))<1.8

func _near_kiosk() -> bool:
    if _near_charity():return true
    if _near_sport_counter():return true
    if _current_place=="PORTICI":return _avatar.position.distance_to(GROCERY_COUNTER)<1.5
    if _near_public_counter():return true
    var target := DEPOT_DESK if _current_place=="LOGISTICS_DEPOT" else KIOSK_POSITION
    return _avatar.position.distance_to(target) < INTERACTION_RADIUS

func _near_public_counter() -> bool:
    if _apartment_active or _guest_active or _stairwell_active:return false
    if _current_place=="CENTRO":return _near_barber() or not _centro_food_counter().is_empty()
    var counters := {"PORTICI":GROCERY_COUNTER,"BAKERY":BAKERY_COUNTER,"LIBRARY":LIBRARY_DESK}
    return counters.has(_current_place) and _avatar.position.distance_to(counters[_current_place])<1.5

func _near_barber() -> bool:
    return _current_place=="CENTRO" and not _apartment_active and not _guest_active and not _stairwell_active and _avatar.position.distance_to(BARBER_COUNTER)<1.15

func _centro_food_counter() -> String:
    if _current_place!="CENTRO" or _apartment_active or _guest_active or _stairwell_active:return ""
    if _avatar.position.distance_to(PIZZA_COUNTER)<1.3:return "pizza"
    if _avatar.position.distance_to(GELATO_COUNTER)<1.3:return "gelato"
    return ""

func _public_entrances() -> Array:
    # Local positions are registered only for genuinely open geometry.
    return {
        "CENTRO":[["Pizzeria",Vector3(-6,0,15)],["Gelateria",Vector3(6,0,15)],["Barbiere",Vector3(-7.8,0,8.5)]],
        "PORTICI":[["Alimentari",Vector3(3.2,0,5.3)]],
        "BAKERY":[["Panetteria",Vector3(-2.8,0,7)]],
        "CAFE":[["Bar Centrale",Vector3(-2.8,0,7)]],
        "LIBRARY":[["Biblioteca comunale",Vector3(-7,0,2.8)]],
        "SCHOOL":[["Istituto Leonardo",Vector3(-2.8,0,7)],["Dojo Shotokan",Vector3(2.8,0,7)]],
        "OFFICE":[["Borgo Digital",Vector3(-2.8,0,8)]],
        "CLINIC":[["Ambulatorio",Vector3(-2.8,0,7)],["Farmacia",Vector3(2.8,0,-7)]]
    }.get(_current_place,[])

func _entrance_hint() -> String:
    var entries := _public_entrances()
    if entries.is_empty():return ""
    var lines := PackedStringArray()
    for entry in entries:
        lines.append("%s · %s" % [entry[0],_guide_direction(entry[1])])
    var door := _public_door()
    if door!=null and not _door_open_hours(door):return tr("Closed · %s") % " / ".join(lines)
    return tr("Walk through the open entrance: %s") % " / ".join(lines)

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
        _reset_jump()
        _reset_locomotion()
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
    var child := _rebirth_active() and str(_last_snapshot.get("rebirth",{}).get("phase","")) != "LINKED_ADULT"
    if not child and _cycling() and not _inside_guest_room() and not in_shop and not _apartment_active:
        return CYCLE_SPEED
    var speed := WALK_SPEED
    if _foot_controls_available():
        if _crouch_requested():
            speed = CROUCH_SPEED
        elif Input.is_key_pressed(KEY_SHIFT):
            speed = RUN_SPEED
    return speed * (0.65 if child else 1.0)

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
    return _car_place()==_current_place and TownLayout.motor_district(_current_place) and _avatar.position.distance_to(CAR_PARK)<3.3

func _car_action(action: String) -> void:
    if (_current_place=="WORKSHOP" or (_current_place=="FUEL_STATION" and action=="refuel") or (TownLayout.service_hub(_current_place) and action in ["refuel","repair"])) and (_near_kiosk() or _near_car()) and _connected and not _api.is_busy():
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
    _reset_jump()
    _reset_locomotion()
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
    for id in _chunks:
        if not TownLayout.motor_district(str(id)):
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
    return (_current_place in RACK_DISTRICTS or TownLayout.outer(_current_place)) and _avatar.position.distance_to(RACK_POSITION)<2.8

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
    for id in _chunks:
        if not (id in RACK_DISTRICTS or TownLayout.outer(str(id))):
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

func _foot_controls_available() -> bool:
    return not _drive_active() and not _cycling() \
        and (not _rebirth_active() or _child_can_walk()) \
        and _road_pending.is_empty() and not _home_transition \
        and Input.mouse_mode == Input.MOUSE_MODE_CAPTURED \
        and not (_travel_panel.visible or _action_panel.visible or _map_panel.visible or _dialog_panel.visible or _phone.visible or _intercom.visible)

func _crouch_requested() -> bool:
    return Input.is_key_pressed(KEY_CTRL) and _jump_elapsed < 0.0

func _can_jump() -> bool:
    return not _stairwell_active and _jump_elapsed < 0.0 and not _crouch_requested() and _crouch_amount < 0.1 and _foot_controls_available()

func _reset_locomotion() -> void:
    _crouch_amount = 0.0
    _step_phase = 0.0
    _step_bob = 0.0

func _advance_locomotion(delta: float, moved: float) -> void:
    var on_foot := _foot_controls_available()
    var blend := 1.0 - exp(-12.0 * maxf(delta,0.0))
    _crouch_amount = lerpf(_crouch_amount,1.0 if on_foot and _crouch_requested() else 0.0,blend)
    var target_bob := 0.0
    if on_foot and _head_bob_enabled and _jump_elapsed < 0.0 and moved > 0.0001:
        _step_phase = fposmod(_step_phase + moved * TAU / 1.7,TAU)
        var amplitude := 0.025 if _crouch_requested() else (0.045 if Input.is_key_pressed(KEY_SHIFT) else 0.03)
        target_bob = sin(_step_phase) * amplitude
    _step_bob = lerpf(_step_bob,target_bob,blend)

func _set_head_bob(enabled: bool) -> void:
    _head_bob_enabled = enabled
    if not enabled:
        _step_bob = 0.0
        _follow_player()

func _reset_jump() -> void:
    _jump_elapsed = -1.0
    _jump_height = 0.0

func _advance_jump(delta: float) -> void:
    if _jump_elapsed < 0.0:
        return
    if _cycling() or not _road_pending.is_empty() or _home_transition:
        _reset_jump()
    else:
        _jump_elapsed += maxf(0.0, delta)
        # Analytic trajectory: the same height at the same elapsed time regardless of FPS.
        _jump_height = maxf(0.0, JUMP_SPEED * _jump_elapsed - 0.5 * JUMP_GRAVITY * _jump_elapsed * _jump_elapsed)
        if _jump_elapsed >= 2.0 * JUMP_SPEED / JUMP_GRAVITY:
            _reset_jump()
            _town_audio.play_effect("landing")
    _follow_player()

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
        _camera.position = _avatar.position + Vector3(0.0,1.12 if _guest_sitting else eye * (1.0 - 0.35 * _crouch_amount) + _jump_height + _step_bob,0.0)
        _camera.rotation = Vector3(_view_pitch,_view_yaw,0.0)

func _set_graphics_quality(index: int) -> void:
    _graphics_quality = clampi(index,0,3)
    # Godot Compatibility supports these MSAA levels; no renderer restart needed.
    match _graphics_quality:
        0:
            get_viewport().msaa_3d = Viewport.MSAA_DISABLED
        1:
            get_viewport().msaa_3d = Viewport.MSAA_2X
        2:
            get_viewport().msaa_3d = Viewport.MSAA_4X
        3:
            get_viewport().msaa_3d = Viewport.MSAA_8X
    _sun.shadow_enabled = _graphics_quality > 0
    _sun.directional_shadow_max_distance = [25.0,55.0,80.0,105.0][_graphics_quality]
    for detail in _graphics_details:
        if is_instance_valid(detail):
            detail.visible = _graphics_quality > 0
    for chunk in _chunks.values():
        var water: MeshInstance3D = chunk["water"]
        if is_instance_valid(water):
            var spray := water.get_node_or_null("FountainSpray") as CPUParticles3D
            if spray != null:
                spray.emitting = water.visible and _graphics_quality > 0

func _register_graphics_detail(detail: Node3D) -> void:
    # Visual-only decoration never alters an obstacle or interaction.
    _graphics_details.append(detail)
    detail.visible = _graphics_quality > 0

func _update_foliage(season: String, snowy: bool) -> void:
    var key := season + str(snowy)
    if key == _foliage_season:
        return
    _foliage_season = key
    var color := Color("#537f3b")
    match season:
        "SPRING": color = Color("#659747")
        "AUTUMN": color = Color("#b17a38")
        "WINTER": color = Color("#66705a")
    if snowy:
        color = color.lerp(Color("#dce4df"),0.50)
    for i in range(_leaf_materials.size()):
        _leaf_materials[i].albedo_color = color.lightened(float(i)*0.07)

func _build_world() -> void:
    _environment = Environment.new()
    for kind in ["grass","paving","asphalt","stucco","terracotta","wood"]:
        _surface_textures[kind] = TownVisuals.texture(kind)
    _leaf_mesh = SphereMesh.new()
    _leaf_mesh.radius = 1.0
    _leaf_mesh.height = 2.0
    _leaf_mesh.radial_segments = 12
    _leaf_mesh.rings = 6
    for i in range(4):
        var leaf := StandardMaterial3D.new()
        leaf.roughness = 1.0
        _leaf_materials.append(leaf)
    _update_foliage("SPRING",false)
    _water_material = ShaderMaterial.new()
    _water_material.shader = preload("res://shaders/TownWater.gdshader")
    _lantern_glass = StandardMaterial3D.new()
    _lantern_glass.albedo_color = Color("#e4d4ac")
    _lantern_glass.emission = Color("#ffca78")
    _lantern_glass.emission_energy_multiplier = 1.2
    _shop_glass = StandardMaterial3D.new()
    _shop_glass.albedo_color = Color("#496064")
    _shop_glass.roughness = 0.26
    _shop_glass.emission = Color("#e4b77a")
    _shop_glass.emission_energy_multiplier = 0.55
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
    _set_graphics_quality(_graphics_quality)

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
    _town_audio = TownAudio.new()
    add_child(_town_audio)
    _build_cockpit()
    _camera = Camera3D.new()
    _camera.current = true
    add_child(_camera)
    _hands = FirstPersonHands.new()
    _camera.add_child(_hands)
    _hands.update_pose(0.0,0.0,true)
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
    # Prune freed scenery references before new chunks are generated.
    var retained: Array[Node3D] = []
    for detail in _graphics_details:
        if is_instance_valid(detail) and detail.is_inside_tree():
            retained.append(detail)
    _graphics_details = retained
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
            _lombardy_facade(_scenery,"CasaOlmo",Vector3(-10,0,-3),Color("#c4bb9a"),3)
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
            _build_market_counters()
            _build_sport_facility(false)
        "SCHOOL":
            _build_college()
            _build_dojo()
        "LIBRARY":
            _build_library()
            _build_caritas()
        "WORKSHOP":
            _building("Mechanical workshop", Color(0.41, 0.51, 0.59))
            _build_motor_court()
            _build_sport_facility(true)
            _box(_scenery, Vector3(4.0, 1.2, 2.0), Color(0.27, 0.30, 0.32),
                    Vector3(-8.0, 0.7, 7.0))
        "OFFICE":
            _build_digital_office()
            _build_postal_counter()
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
            var river_mesh := BoxMesh.new()
            river_mesh.size = Vector3(10.0,0.11,35.0)
            var river_water := MeshInstance3D.new()
            river_water.name = "RiverWater"
            river_water.mesh = river_mesh
            river_water.material_override = _water_material
            river_water.position = Vector3(-11.0,0.02,0.0)
            _scenery.add_child(river_water)
            _box(_scenery, Vector3(10.5, 0.12, 4.8), Color(0.45,0.34,0.23),
                    Vector3(-11.0, 0.10, 0.0))
            _tree(Vector3(12.0, 0.0, 7.0))
        "BORGO_SUD", "PIAZZA_FONTANA", "GIARDINI", "STAZIONE", "VIA_TIGLI":
            _build_southern_district(place_id)
        "WOODS":
            for p in [
                Vector2(-11.0, -9.0), Vector2(-10.0, 7.0),
                Vector2(10.0, -10.0), Vector2(11.0, 9.0),
                Vector2(-15.0, 14.0), Vector2(14.0, 14.0)
            ]:
                _tree(Vector3(p.x, 0.0, p.y))
        _:
            if TownLayout.outer(place_id):
                _build_outer_block(place_id)
            else:
                _building("Neighborhood", Color(0.57, 0.61, 0.57))
                _tree(Vector3(11.0, 0.0, 7.0))

    if place_id in RACK_DISTRICTS or TownLayout.outer(place_id):
        _build_bicycle_rack()
    if TownLayout.motor_district(place_id):
        _build_public_car_park()
    if place_id in FREIGHT_DISTRICTS:
        _build_freight_parking(place_id)
    if TownLayout.bus_stop(place_id):
        _sign(_scenery,"BUS · Fermata · F",KIOSK_POSITION+Vector3(0,3.0,0))
    _build_road_links(place_id)
    _street_marker(place_id)
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
        var roadside := direction*19.0+Vector3(-direction.z,0,direction.x)*(width/2.0+0.65)
        var yaw := atan2(-direction.x,-direction.z)
        _box(roads,Vector3(0.09,2.0,0.09),Color("#686e70"),roadside+Vector3(0,1,0))
        _physical_sign(roads,_district_sign_name(str(id)),roadside+Vector3(0,2.05,0),yaw,2.5)

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
                    var colors := [Color("#7894aa"),Color("#ad7965"),Color("#c4c1b7"),Color("#687c64"),Color("#806b7f"),Color("#414d59")]
                    var palette_index := absi((key+str(i)).hash())%colors.size()
                    var car := _make_city_car(_traffic_root,colors[palette_index],palette_index%2)
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
    if _guest_active:
        return _guest.can_stand(at,radius)
    if _stairwell_active:
        return _stairwell.can_stand(at,radius)
    if _apartment_active:
        return _apartment.can_stand(at,radius)
    # Extra walkable garden space inside the existing 48 m park tile.
    # Keep the road axes and inter-district triggers unchanged.
    var extended_park := _current_place=="PARK" and absf(at.x)<=20.0-radius and at.z>=12.0 and at.z<=21.0-radius
    var extended_centro := _current_place=="CENTRO" and absf(at.x)<=10.5-radius and at.z>=15.0 and at.z<=21.0-radius
    if (absf(at.x) > 17.0 or absf(at.z) > 17.0) and not extended_park and not extended_centro:
        if not allow_roads or _road_destination(at, 17.0).is_empty():
            return false
    var p := Vector2(at.x, at.z)
    if _public_door_blocks(at,radius):return false
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
    if _car_place()==_current_place and TownLayout.motor_district(_current_place):
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
    if _stairwell_active and not _guest_active:
        at.y = _stairwell.ground_height(at)
    return at

# Dynamic local bodies are separate from static geometry and never affect
# spawn/seat/path validation. A pre-existing overlap can be escaped, not deepened.
func _person_clear(from: Vector3,at: Vector3,radius: float,other: Vector3,other_radius: float) -> bool:
    if absf(at.y-other.y)>1.1:return true
    var limit := radius+other_radius
    var next := Vector2(at.x-other.x,at.z-other.z).length_squared()
    if next>=limit*limit:return true
    var before := Vector2(from.x-other.x,from.z-other.z).length_squared()
    return next>before+0.000001

func _people_clear(from: Vector3,at: Vector3,radius: float,ignore: Node3D) -> bool:
    if ignore!=null and not _drive_active() and not _person_clear(from,at,radius,_avatar.position,WALKER_RADIUS):
        return false
    if _guest_active or _stairwell_active:return true
    if not is_instance_valid(_residents_root):return true
    for child in _residents_root.get_children():
        var person := child as Node3D
        if person==null or person==ignore:continue
        var body_radius := clampf(0.32*person.scale.x,0.18,0.42)
        if not _person_clear(from,at,radius,person.position,body_radius):return false
    return true

func _move_among_people(from: Vector3,motion: Vector3,radius: float,ignore: Node3D,allow_roads: bool=false) -> Vector3:
    if _guest_active or _stairwell_active:
        return _move_on_ground(from,motion,radius,allow_roads)
    var count := maxi(1,ceili(Vector2(motion.x,motion.z).length()/COLLISION_STEP))
    var step := Vector3(motion.x,0,motion.z)/float(count)
    var at := from
    for i in range(count):
        var candidate := _move_on_ground(at,step,radius,allow_roads)
        if _people_clear(at,candidate,radius,ignore):
            at=candidate
        else:
            # Consistent right-side passing, with a left fallback; never cross a wall.
            for angle in [PI/3.0,-PI/3.0,PI/2.0,-PI/2.0]:
                var detour := _move_on_ground(at,step.rotated(Vector3.UP,angle),radius,allow_roads)
                if detour.distance_squared_to(at)>0.000001 and _people_clear(at,detour,radius,ignore):
                    at=detour
                    break
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

func _make_city_car(parent: Node3D,paint: Color,style: int = 0) -> Node3D:
    var car := Node3D.new()
    car.name = "CityCar"
    parent.add_child(car)
    car.set_meta("body_style",style)
    var dark := Color("#303638")
    var glass := Color("#536d79")
    _box(car,Vector3(1.78,0.50,3.5),paint,Vector3(0,0.62,0))
    var body := car.get_child(car.get_child_count()-1) as MeshInstance3D
    var paint_material := body.material_override as StandardMaterial3D
    paint_material.roughness = 0.29
    paint_material.metallic = 0.18
    var roof_height := 1.45 if style==0 else 1.58
    _box(car,Vector3(1.52,0.10,1.40),paint,Vector3(0,roof_height,0.20))
    _box(car,Vector3(1.53,0.12,0.95),paint,Vector3(0,0.91,-1.23))
    _box(car,Vector3(1.53,0.12,0.65),paint,Vector3(0,0.91,1.39))
    # Tilt the front and rear glass between hood and roof instead of a square cabin.
    for side in [-1.0,1.0]:
        var glass_height := roof_height-0.91
        var pane := Node3D.new()
        pane.position = Vector3(0,(roof_height+0.91)*0.5,0.20+side*0.88)
        pane.rotation.x = side*0.51
        car.add_child(pane)
        _box(pane,Vector3(1.44,glass_height/0.87,0.065),glass,Vector3.ZERO)
    for side in [-1.0,1.0]:
        for z in [-0.17,0.60]:
            _box(car,Vector3(0.06,roof_height-1.01,0.67),glass,Vector3(side*0.76,(roof_height+0.98)*0.5,z))
        _box(car,Vector3(0.06,roof_height-0.91,0.08),dark,Vector3(side*0.79,(roof_height+0.91)*0.5,0.21))
        _box(car,Vector3(0.06,0.06,0.22),dark,Vector3(side*0.906,0.78,0.31))
        _box(car,Vector3(0.06,0.06,0.22),dark,Vector3(side*0.906,0.78,-0.48))
        _box(car,Vector3(0.13,0.10,0.22),paint,Vector3(side*0.87,1.00,-0.74))
        for z in [-1.08,1.08]:
            var wheel := _cylinder(car,0.32,0.20,dark,Vector3(side*0.82,0.34,z))
            wheel.rotation_degrees.z = 90
            var hub := _cylinder(car,0.19,0.04,Color("#a6aaa7"),Vector3(side*0.93,0.34,z))
            hub.rotation_degrees.z = 90
            var center := _cylinder(car,0.075,0.04,Color("#4b5151"),Vector3(side*0.953,0.34,z))
            center.rotation_degrees.z = 90
            for n in range(5):
                var spoke := Node3D.new()
                spoke.position = Vector3(side*0.954,0.34,z)
                spoke.rotation.x = float(n)*TAU/5.0
                car.add_child(spoke)
                _box(spoke,Vector3(0.018,0.14,0.035),Color("#666d6d"),Vector3(0,0.095,0))
        _box(car,Vector3(0.38,0.18,0.08),Color("#eee5bb"),Vector3(side*0.60,0.70,-1.79))
        _box(car,Vector3(0.28,0.18,0.08),Color("#b33b36"),Vector3(side*0.65,0.7,1.79))
    _box(car,Vector3(0.69,0.15,0.07),dark,Vector3(0,0.73,-1.78))
    for y in [0.69,0.75]:
        _box(car,Vector3(0.62,0.022,0.07),Color("#858b88"),Vector3(0,y,-1.82))
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
# Deterministic scenery, generated only when the streaming window needs this block.
func _build_outer_block(id: String) -> void:
    var block := Node3D.new()
    block.name="ProceduralBlock"
    _scenery.add_child(block)
    var cell: Vector2i = TownLayout.DISTRICTS[id]
    var variant := posmod(cell.x*7+cell.y*3,5)
    var label := TownLayout.label_for(id)
    var rural := label.begins_with("Campagna")
    var green := label.begins_with("Giardino")
    var square := label.begins_with("Piazzetta")
    var positions := [Vector3(-10,0,-8),Vector3(-10,0,9),Vector3(10,0,10),Vector3(14,0,5)]
    _box(block,Vector3(34,0.04,3.4),Color("#777c78"),Vector3(0,0.025,0),"paving")
    for z in [-2.8,2.8]:
        _box(block,Vector3(34,0.07,1.2),Color("#beb7a6"),Vector3(0,0.035,z),"paving")
    if rural:
        for x in [-10.0,10.0]:
            _box(block,Vector3(9,0.04,9),Color("#8e9860"),Vector3(x,0.015,9))
            for stripe in range(5):
                _box(block,Vector3(0.13,0.035,8.5),Color("#77794c"),Vector3(x-3.6+stripe*1.7,0.04,9))
        if variant%2==0:
            _lombardy_facade(block,"Cascina",positions[0],Color("#c7b18c"),1)
        _tree(positions[1])
    elif green or square:
        if square:
            _box(block,Vector3(22,0.06,9),Color("#c5b9a6"),Vector3(0,0.04,7),"paving")
            _lombardy_facade(block,"Palazzo",positions[0],Color("#d6bc98"),2)
            _lombardy_facade(block,"Loggia",positions[1],Color("#cba08a"),3)
        else:
            for at in positions:
                _tree(at)
        _bench(block,Vector3(-6,0,5))
        _bench(block,Vector3(6,0,-5))
    else:
        var walls := [Color("#d2b89a"),Color("#c38f80"),Color("#d6c9af"),Color("#bcb9a5"),Color("#ddc5a0")]
        for i in range(3 if variant%2==0 else 2):
            _lombardy_facade(block,"Casa"+str(i+1),positions[i],walls[(i+variant)%walls.size()],2+(i+variant)%2)
        _tree(positions[3])
    _lamp(block,Vector3(-5,0,-4))
    _lamp(block,Vector3(5,0,4))
    if TownLayout.service_hub(id):
        _sign(block,"Servizi auto · 06:00–22:00",CAR_PARK+Vector3(0,2.4,2.5))
        _box(block,Vector3(2.6,0.15,1.5),Color("#d2bd82"),CAR_PARK+Vector3(0,2.7,0))

func _build_southern_district(id: String) -> void:
    var district := Node3D.new()
    district.name = "SouthernDistrict_"+id
    _scenery.add_child(district)
    # All four road axes stay open. Buildings are actual collision obstacles.
    var stone := Color("#b8b0a0")
    _box(district,Vector3(32,0.05,3.2),Color("#777d7a"),Vector3(0,0.02,0),"asphalt")
    for z in [-2.8,2.8]:
        _box(district,Vector3(32,0.06,1.5),stone,Vector3(0,0.035,z),"paving")
    match id:
        "BORGO_SUD","VIA_TIGLI":
            var palettes := [Color("#c9a88d"),Color("#dbc8a4"),Color("#c1b7a5"),Color("#ce9682")]
            var positions := [Vector3(-10,0,-8),Vector3(10,0,-8),Vector3(-10,0,9),Vector3(10,0,10)]
            for i in range(4):
                _lombardy_facade(district,"Casa"+str(i+1),positions[i],palettes[(i+1)%4] if id=="VIA_TIGLI" else palettes[i],2 if i%2==0 else 3)
            for p in [Vector3(-5,0,5),Vector3(5,0,-5),Vector3(-14,0,3.5)]:
                _tree(p)
            _bench(district,Vector3(5,0,5.5))
        "PIAZZA_FONTANA":
            _box(district,Vector3(24,0.08,10),stone,Vector3(0,0.05,6),"paving")
            _lombardy_facade(district,"PalazzoFontana",Vector3(-10,0,-8),Color("#c7a991"),3)
            _lombardy_facade(district,"PalazzoLoggia",Vector3(10,0,-8),Color("#dfc6a1"),2)
            _add_obstacle(Vector3(-7,0,7),Vector2(3.2,3.2))
            _cylinder(district,1.6,0.5,Color("#a7a59a"),Vector3(-7,0.25,7))
            _cylinder(district,1.3,0.08,Color("#609da7"),Vector3(-7,0.52,7))
            _cylinder(district,0.22,1.1,Color("#a7a59a"),Vector3(-7,1,7))
            _bench(district,Vector3(7,0,7))
            _sign(district,"Piazza Fontana",Vector3(-7,2.4,7))
        "GIARDINI":
            for p in [Vector3(-10,0,-9),Vector3(-14,0,8),Vector3(11,0,-8),Vector3(11,0,9),Vector3(-7,0,12),Vector3(6,0,12)]:
                _tree(p)
            for p in [Vector3(-6,0,-5),Vector3(6,0,5),Vector3(-6,0,5)]:
                _bench(district,p)
            _box(district,Vector3(8,0.05,7),Color("#b7ad8e"),Vector3(8,0.02,9),"paving")
            _sign(district,"Giardini Comunali",Vector3(-7,2.2,-6))
        "STAZIONE":
            _lombardy_facade(district,"StazioneFerroviaria",Vector3(-10,0,8),Color("#e0c79f"),2)
            _box(district,Vector3(14,0.10,2),stone,Vector3(8,0.05,9),"paving")
            for z in [11.0,12.1]:
                _box(district,Vector3(13,0.10,0.08),Color("#646767"),Vector3(8,0.09,z))
            for x in range(2,15):
                _box(district,Vector3(0.20,0.06,1.7),Color("#756654"),Vector3(x,0.04,11.55))
            _bench(district,Vector3(8,0,8))
            _sign(district,"Stazione · Piazza",Vector3(-10,2.4,4.8))
    for p in [Vector3(-4,0,-3.8),Vector3(4,0,3.8),Vector3(-13,0,-3.8)]:
        _lamp(district,p)

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
    _physical_sign(street,"Via delle Magnolie",Vector3(7.28,2.8,-4.0),0,2.8)
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
    _box(building,Vector3(width,height,depth),wall,at+Vector3(0,height/2,0),"stucco")
    _historic_roof(building,at+Vector3(0,height,0),width+0.4,depth+0.4,1.15)
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
    var number := "12" if id=="CasaMagnolia" else "14" if id=="CasaTiglio" else "8"
    var building_name := "Casa Magnolia" if id=="CasaMagnolia" else "Casa Tiglio" if id=="CasaTiglio" else "Casa Olmo"
    _physical_sign(building,building_name,Vector3(at.x,2.8,face+0.18),0,2.1)
    _physical_sign(building,number,Vector3(at.x+1.18,1.9,face+0.13),0,0.52)
    var mailbox := Node3D.new()
    mailbox.name="Mailboxes"
    building.add_child(mailbox)
    for n in range(4):
        var pos := Vector3(at.x-1.15,1.05+0.20*float(n),face+0.13)
        _box(mailbox,Vector3(0.35,0.18,0.16),Color("#647078"),pos)
        _box(mailbox,Vector3(0.24,0.025,0.01),Color("#252e31"),pos+Vector3(0,0.035,0.086))
    var bell := Node3D.new()
    bell.name="Intercom"
    bell.position=Vector3(at.x+1.2,1.35,face+0.17)
    building.add_child(bell)
    _box(bell,Vector3(0.22,0.36,0.08),Color("#8b969a"),Vector3.ZERO)
    for n in range(3):
        _box(bell,Vector3(0.13,0.018,0.012),Color("#343b40"),Vector3(0,0.12-float(n)*0.035,0.045))
    _box(bell,Vector3(0.065,0.065,0.018),Color("#d6c6a0"),Vector3(0,-0.095,0.05))
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
    _physical_sign(house,"Casa del Glicine" if owned else "Casa Ospiti",Vector3(-3.78,3.35,8.5),PI/2.0,2.8)
    _physical_sign(house,"6" if owned else "10",Vector3(-3.78,2.0,6.7),PI/2.0,0.52)
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

# A small walk-in postal counter within the existing Offices district.
func _build_postal_counter() -> void:
    var post := Node3D.new()
    post.name="UfficioPostale"
    _scenery.add_child(post)
    var wall := Color("#e8dfca")
    for part in [[Vector3(8,0,11),Vector2(7,0.24)],
            [Vector3(4.5,0,8),Vector2(0.24,6)],
            [Vector3(11.5,0,8),Vector2(0.24,6)],
            [Vector3(5.65,0,5),Vector2(2.3,0.24)],
            [Vector3(10.35,0,5),Vector2(2.3,0.24)]]:
        _room_wall(post,part[0],part[1],wall)
    _box(post,Vector3(7,0.06,6),Color("#bdb5a5"),Vector3(8,0.03,8))
    _box(post,Vector3(7.2,0.23,6.2),Color("#526876"),Vector3(8,3.12,8))
    _box(post,Vector3(3.1,0.40,0.22),Color("#d6ae43"),Vector3(8,2.7,5))
    _sign(post,"POSTE · Ufficio postale",Vector3(8,3.38,4.95))
    _room_furniture(post,Vector3(8,0,9.25),Vector3(4.0,0.95,0.65),Color("#c9ae6e"))
    for i in range(4):
        _box(post,Vector3(0.50,0.24,0.35),Color("#af9c7b"),Vector3(6.5+float(i),1.10,9.2))
    _room_furniture(post,Vector3(10.4,0,10.3),Vector3(1.0,1.8,0.6),Color("#687b83"))
    var light := OmniLight3D.new()
    light.position=Vector3(8,2.7,8)
    light.omni_range=6
    light.light_energy=1.4
    post.add_child(light)

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
    # Desks face the west-wall board. The central entrance/secretary aisle stays clear.
    for x in [-11.15,-9.65,-8.15]:
        for z in [3.9,5.3,8.7,10.1]:
            _room_furniture(classroom,Vector3(x,0,z),Vector3(0.62,0.69,0.78),wood)
            var chair := Vector3(x+0.70,0,z)
            _room_furniture(classroom,chair,Vector3(0.42,0.36,0.43),Color("#65786e"))
            _box(classroom,Vector3(0.07,0.36,0.43),Color("#65786e"),chair+Vector3(0.20,0.50,0))
            _box(classroom,Vector3(0.26,0.035,0.38),Color("#e4dfcb"),Vector3(x,0.72,z))
            _register_resident_seat(chair+Vector3(0,0.36,0),PI/2.0,"classroom")
    _physical_sign(classroom,"AULA",Vector3(-6.2,2.4,5.7),PI/2.0,1.2)
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
    _physical_sign(cafe,"Bar Centrale",Vector3(-3.8,3.4,7),PI/2.0,2.6)
    _build_public_door(cafe,Vector3(-4,0,7),2.1,420,1260,Rect2(-13,3,9,8))
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
            _register_resident_seat(Vector3(x,0.48,z),0.0 if z>9.0 else PI,"cafe")
            _box(tables,Vector3(0.48,0.43,0.07),wood,Vector3(x,0.66,z+0.20*signf(z-9.0)))
    _sign(tables,"Take a break · E",Vector3(-7,2.1,9))
    for at in [Vector3(-7,2.6,5.4),Vector3(-8.3,2.6,9)]:
        var light := OmniLight3D.new()
        light.position=at
        light.light_color=Color("#ffe1b5")
        light.light_energy=1.4
        light.omni_range=6
        cafe.add_child(light)

# Animated sliding entrance, registered in the actual district scene.
func _build_public_door(parent: Node3D, at: Vector3, width: float, opens: int, closes: int, inside: Rect2) -> void:
    var door := Node3D.new()
    door.name="PublicDoor"
    parent.add_child(door)
    door.position=at
    door.set_meta("width",width)
    door.set_meta("opens",opens)
    door.set_meta("closes",closes)
    door.set_meta("inside",inside)
    door.set_meta("fraction",0.0)
    for side in [-1,1]:
        var leaf := Node3D.new()
        leaf.name="Left" if side<0 else "Right"
        door.add_child(leaf)
        leaf.position.z=side*width/4.0
        var frame := Color("#6a5949")
        _box(leaf,Vector3(0.09,2.35,width/2.0-0.025),Color("#78999b"),Vector3(0,1.175,0))
        for z in [-width/4.0+0.04,width/4.0-0.04]:
            _box(leaf,Vector3(0.12,2.35,0.08),frame,Vector3(0,1.175,z))
        for y in [0.04,1.0,2.31]:
            _box(leaf,Vector3(0.12,0.08,width/2.0),frame,Vector3(0,y,0))
    var signboard := Label3D.new()
    signboard.name="Hours"
    signboard.position=Vector3(-0.18 if at.x>0 else 0.18,2.55,0)
    signboard.rotation.y=-PI/2.0 if at.x>0 else PI/2.0
    signboard.font_size=32
    signboard.pixel_size=0.006
    signboard.outline_size=5
    door.add_child(signboard)

func _public_door() -> Node3D:
    if not is_instance_valid(_scenery):return null
    var path := str({"PORTICI":"ViaDeiPortici/BottegaAlimentare/PublicDoor","BAKERY":"PanetteriaDelBorgo/PublicDoor","CAFE":"BarCentrale/PublicDoor"}.get(_current_place,""))
    return null if path.is_empty() else _scenery.get_node_or_null(path) as Node3D

func _door_open_hours(door: Node3D) -> bool:
    var minute := int(_last_snapshot.get("minute",0))
    return minute>=int(door.get_meta("opens")) and minute<int(door.get_meta("closes"))

func _tick_public_door(delta: float) -> void:
    var door := _public_door()
    if door==null:return
    var inside: Rect2=door.get_meta("inside")
    var player_at := Vector2(_avatar.position.x,_avatar.position.z)
    var width := float(door.get_meta("width"))
    var at := door.position
    var in_public := not _apartment_active and not _guest_active and not _stairwell_active
    var threshold := Rect2(at.x-0.45,at.z-width/2.0-0.35,0.90,width+0.70)
    var leaving := in_public and (inside.has_point(player_at) or (threshold.has_point(player_at) and float(door.get_meta("fraction"))>0.0))
    var open := _door_open_hours(door)
    var fraction := move_toward(float(door.get_meta("fraction")),1.0 if open or leaving else 0.0,delta*2.5)
    door.set_meta("fraction",fraction)
    for side in [-1,1]:
        var leaf := door.get_node("Left" if side<0 else "Right") as Node3D
        leaf.position.z=side*(width/4.0+fraction*width/2.0)
    var start := int(door.get_meta("opens"))
    var end := int(door.get_meta("closes"))
    var hours := "%02d:%02d–%02d:%02d" % [floori(start/60.0),start%60,floori(end/60.0),end%60]
    door.get_node("Hours").text="%s · %s" % [tr("Open") if open else tr("Closed"),hours]

func _public_door_blocks(at: Vector3,radius: float) -> bool:
    var door := _public_door()
    if door==null or float(door.get_meta("fraction"))>=0.95:return false
    var width := float(door.get_meta("width"))
    var rect := Rect2(door.position.x-0.06,door.position.z-width/2.0,0.12,width)
    return rect.grow(radius).has_point(Vector2(at.x,at.z))

func _build_library() -> void:
    var library := Node3D.new()
    library.name="BibliotecaComunale"
    _scenery.add_child(library)
    var wall := Color("#d4c3a4")
    var wood := Color("#87694e")
    for part in [[Vector3(-12.5,0,7.5),Vector2(0.24,7.0)],
            [Vector3(-1.5,0,7.5),Vector2(0.24,7.0)],
            [Vector3(-7,0,11),Vector2(11.0,0.24)],
            [Vector3(-10.25,0,4),Vector2(4.5,0.24)],
            [Vector3(-3.75,0,4),Vector2(4.5,0.24)]]:
        _room_wall(library,part[0],part[1],wall)
    _box(library,Vector3(11,0.08,7),Color("#b5ab94"),Vector3(-7,0.025,7.5))
    _box(library,Vector3(11.3,0.35,7.3),Color("#795e49"),Vector3(-7,3.2,7.5))
    _physical_sign(library,"Biblioteca comunale",Vector3(-7,2.75,3.80),PI,3.6)
    _room_furniture(library,Vector3(-7,0,7.6),Vector3(2.0,0.9,0.9),wood)
    _box(library,Vector3(2.1,0.08,1),Color("#d2bf9e"),Vector3(-7,0.94,7.6))
    _physical_sign(library,"Informazioni · E",Vector3(-7,1.25,7.1),PI,1.5)
    for x in [-11.6,-2.4]:
        for z in [6.1,9.1]:
            _room_furniture(library,Vector3(x,0,z),Vector3(0.55,2.0,2.1),wood)
            for y in [0.5,1.1,1.7]:
                for i in range(7):
                    _box(library,Vector3(0.30,0.28,0.20),[Color("#7c9480"),Color("#a16c5c"),Color("#647b99")][i%3],Vector3(x,y,z-0.84+i*0.27))
    for x in [-9.5,-4.5]:
        _room_furniture(library,Vector3(x,0,9.2),Vector3(1.3,0.74,0.9),wood)
        _room_furniture(library,Vector3(x,0,8.15),Vector3(0.44,0.45,0.44),Color("#67817c"))
    var light := OmniLight3D.new()
    light.position=Vector3(-7,2.7,7.5)
    light.omni_range=10
    light.light_energy=1.8
    library.add_child(light)

func _build_caritas() -> void:
    var hall := Node3D.new()
    hall.name="Caritas"
    _scenery.add_child(hall)
    var plaster := Color("#e6dfcf")
    for part in [[Vector3(4,0,8),Vector2(0.24,6)],
            [Vector3(12,0,8),Vector2(0.24,6)],
            [Vector3(8,0,11),Vector2(8,0.24)],
            [Vector3(5.5,0,5),Vector2(3,0.24)],
            [Vector3(10.5,0,5),Vector2(3,0.24)]]:
        _room_wall(hall,part[0],part[1],plaster)
    _box(hall,Vector3(8,0.08,6),Color("#bdb4a0"),Vector3(8,0.025,8))
    _box(hall,Vector3(8.3,0.30,6.3),Color("#856d59"),Vector3(8,3.16,8))
    _physical_sign(hall,"CARITAS · Mensa solidale",Vector3(8,2.65,4.82),PI,4.2)
    _room_furniture(hall,Vector3(8,0,8.5),Vector3(3.2,0.9,0.7),Color("#967957"))
    _physical_sign(hall,"Pasti gratuiti · 12–14 · F",Vector3(8,1.3,8.06),PI,2.6)
    for x in [5.3,10.7]:
        _room_furniture(hall,Vector3(x,0,9.2),Vector3(1.1,0.74,1.4),Color("#9f8768"))
        _room_furniture(hall,Vector3(x,0,7.95),Vector3(0.42,0.45,0.42),Color("#718478"))
    var light := OmniLight3D.new()
    light.position=Vector3(8,2.7,8)
    light.omni_range=8
    light.light_energy=1.5
    hall.add_child(light)

# The market has real walkable counters, with separate vendor and customer sides.
func _build_market_counters() -> void:
    var stalls := Node3D.new()
    stalls.name="MarketCounters"
    _scenery.add_child(stalls)
    for x in [-9.0,-7.0,-5.0]:
        _room_furniture(stalls,Vector3(x,0,1.25),Vector3(1.65,0.92,0.58),Color("#87694d"))
        _box(stalls,Vector3(1.72,0.08,0.64),Color("#d2bc96"),Vector3(x,0.96,1.25))
        for i in range(3):
            _sphere(stalls,0.09,[Color("#af4938"),Color("#7c964d"),Color("#dfaa4a")][i],Vector3(x-0.55+i*0.24,1.1,1.25))
        _box(stalls,Vector3(0.24,0.17,0.24),Color("#42494a"),Vector3(x+0.53,1.1,1.25))

func _build_sport_facility(range_site: bool) -> void:
    var site := Node3D.new()
    site.name = "PoligonoDiTiro" if range_site else "Armeria"
    _scenery.add_child(site)
    var wall := Color("#c9baa4")
    _box(site,Vector3(6.0,0.08,5.6),Color("#9d9b93"),Vector3(12.5,0.02,-0.5))
    for x in [9.5,15.5]:
        _room_wall(site,Vector3(x,0,-0.5),Vector2(0.24,5.6),wall)
    _room_wall(site,Vector3(12.5,0,-3.3),Vector2(6.0,0.24),wall)
    _box(site,Vector3(6.4,0.24,5.9),Color("#626a68"),Vector3(12.5,3.15,-0.5))
    _room_furniture(site,Vector3(12.5,0,0.6),Vector3(2.3,1.0,0.65),Color("#6e6456"))
    _sign(site,"Poligono di tiro · F" if range_site else "Armeria · F",Vector3(12.5,2.65,2.5))
    _sign(site,"08:00–18:00" if range_site else "08:00–20:00",Vector3(12.5,1.5,0.97))
    if range_site:
        for x in [11.0,14.0]:
            _box(site,Vector3(0.9,0.9,0.10),Color("#d9d9cf"),Vector3(x,1.45,-2.7))
            _box(site,Vector3(0.4,0.4,0.11),Color("#696c67"),Vector3(x,1.45,-2.62))
    else:
        _box(site,Vector3(2.0,1.4,0.35),Color("#7e8888"),Vector3(12.5,1.0,-2.8))

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
    _physical_sign(bakery,"Panetteria del Borgo",Vector3(-3.1,3.36,7),PI/2.0,3.0)
    _build_public_door(bakery,Vector3(-4,0,7),2.1,420,1080,Rect2(-12,3.5,8,7))
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

func _build_centro_south_walk(parent: Node3D) -> void:
    var lane := Node3D.new()
    lane.name="PasseggiataDeiTigli"
    parent.add_child(lane)
    var stone := Color("#bcb19b")
    for side in [-1.0,1.0]:
        _box(lane,Vector3(7.6,0.045,6.0),stone,Vector3(side*6.3,0.018,18.0),"paving")
        _bench(lane,Vector3(side*3.3,0,20.0))
        for z in [16.5,19.5]:
            var at := Vector3(side*10,0,z)
            _add_obstacle(at,Vector2(0.85,0.85))
            _box(lane,Vector3(0.85,0.42,0.85),Color("#a36e4a"),at+Vector3(0,0.21,0))
            _sphere(lane,0.48,Color("#5f7547"),at+Vector3(0,0.75,0))
    _physical_sign(lane,"Passeggiata dei Tigli",Vector3(3.3,2.2,20.4),PI,3.0)
    _build_centro_food_shop(lane,-6.0,true)
    _build_centro_food_shop(lane,6.0,false)

func _build_centro_food_shop(parent: Node3D,x: float,pizza: bool) -> void:
    var shop := Node3D.new()
    shop.name="Pizzeria" if pizza else "Gelateria"
    parent.add_child(shop)
    var wall := Color("#c98b6a") if pizza else Color("#b0c8b2")
    var trim := Color("#f4e3c8")
    for part in [[Vector3(x-2.1,0,18.15),Vector2(0.22,5.3)],
            [Vector3(x+2.1,0,18.15),Vector2(0.22,5.3)],
            [Vector3(x,0,20.8),Vector2(4.4,0.22)],
            [Vector3(x-1.6,0,15.5),Vector2(1.0,0.22)],
            [Vector3(x+1.6,0,15.5),Vector2(1.0,0.22)]]:
        _room_wall(shop,part[0],part[1],wall)
    _box(shop,Vector3(4.2,0.08,5.3),trim,Vector3(x,0.04,18.15),"paving")
    _box(shop,Vector3(4.5,0.22,5.5),Color("#96513d"),Vector3(x,3.1,18.15),"terracotta")
    _box(shop,Vector3(4.4,0.55,0.23),wall.darkened(0.15),Vector3(x,2.75,15.5))
    _physical_sign(shop,"Pizzeria del Borgo" if pizza else "Gelateria dei Tigli",Vector3(x,2.72,15.32),PI,3.9)
    _box(shop,Vector3(4.4,0.09,0.8),wall.darkened(0.22),Vector3(x,2.43,15.1))
    _room_furniture(shop,Vector3(x,0,19.1),Vector3(3.0,0.9,0.65),wall.darkened(0.2))
    _box(shop,Vector3(3.05,0.08,0.7),trim,Vector3(x,0.94,19.1))
    if pizza:
        for dx in [-0.9,0.0,0.9]:
            _cylinder(shop,0.30,0.06,Color("#c68b43"),Vector3(x+dx,1.02,19.1))
            _cylinder(shop,0.24,0.025,Color("#b9543c"),Vector3(x+dx,1.06,19.1))
            for offset in [-0.09,0.09]:
                _sphere(shop,0.055,Color("#eee1b7"),Vector3(x+dx+offset,1.07,19.12))
    else:
        for dx in [-0.9,-0.3,0.3,0.9]:
            _box(shop,Vector3(0.48,0.15,0.48),Color("#e9e5db"),Vector3(x+dx,1.05,19.1))
            _sphere(shop,0.17,Color("#c98b99") if dx<0 else Color("#aebd83"),Vector3(x+dx,1.18,19.1))
        _box(shop,Vector3(2.7,0.38,0.045),Color("#9eb6bf"),Vector3(x,1.2,18.77))
    _physical_sign(shop,"11:00–22:00 · F",Vector3(x,1.7,19.65),PI,2.0)

func _build_portici() -> void:
    var street := Node3D.new()
    street.name = "ViaDeiPortici"
    _scenery.add_child(street)
    _box(street,Vector3(7.4,0.06,34),Color("#a29b8c"),Vector3(0,0.025,0),"paving")
    _box(street,Vector3(34,0.06,5.1),Color("#a29b8c"),Vector3(0,0.025,0),"paving")
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
    _box(street,Vector3(0.10,2.0,0.10),Color("#6a6c67"),Vector3(4,1,-8))
    _physical_sign(street,"Via dei Portici",Vector3(4,2.05,-8),0,2.4)
    _physical_sign(street,"Piazza Ducale",Vector3(-3.1,2.55,-16),0,2.4)

func _portici_house(parent: Node3D,side: float,z: float,length: float,height: float,
        wall: Color,caption: String) -> void:
    var front := side*4.4
    var center := side*9.2
    var trim := Color("#dfc9a5")
    _add_obstacle(Vector3(center,0,z),Vector2(9.6,length))
    _box(parent,Vector3(9.6,height,length),wall,Vector3(center,height*0.5,z),"stucco")
    _historic_roof(parent,Vector3(center,height,z),9.85,length,1.45)
    _box(parent,Vector3(9.85,0.20,length+0.2),trim,Vector3(center,height-0.05,z))
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
    _physical_sign(shop,"Alimentari",Vector3(4.05,3.1,5.3),-PI/2.0,2.3)
    _build_public_door(shop,Vector3(4.4,0,5.3),1.95,480,1200,Rect2(4.4,2.15,6.1,5.7))
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

func _build_barber(parent: Node3D) -> void:
    var shop := Node3D.new()
    shop.name="BarbiereDelBorgo"
    parent.add_child(shop)
    var wall := Color("#b7b09d")
    var wood := Color("#684d3c")
    for part in [[Vector3(-10.1,0,6),Vector2(0.22,4.6)],
            [Vector3(-5.5,0,6),Vector2(0.22,4.6)],
            [Vector3(-7.8,0,3.7),Vector2(4.8,0.22)],
            [Vector3(-9.5,0,8.3),Vector2(1.35,0.22)],
            [Vector3(-6.1,0,8.3),Vector2(1.35,0.22)]]:
        _room_wall(shop,part[0],part[1],wall)
    _box(shop,Vector3(4.6,0.08,4.6),Color("#d5c8b4"),Vector3(-7.8,0.035,6),"paving")
    _box(shop,Vector3(4.9,0.22,4.9),Color("#65554b"),Vector3(-7.8,3.1,6))
    _physical_sign(shop,"Barbiere del Borgo",Vector3(-7.8,2.75,8.45),0,4.3)
    _box(shop,Vector3(4.6,0.48,0.15),wood,Vector3(-7.8,2.42,8.35))
    var pole := Node3D.new()
    pole.name="BarberPole"
    shop.add_child(pole)
    pole.position=Vector3(-9.8,1.8,8.48)
    for i in range(8):
        var stripe := _cylinder(pole,0.115,0.12,[Color("#ece8dd"),Color("#ad4949"),Color("#ece8dd"),Color("#416b8a")][i%4],Vector3(0,float(i)*0.12-0.42,0))
        stripe.rotation.z=0.0
    _room_furniture(shop,Vector3(-7.8,0,4.25),Vector3(2.8,0.8,0.65),wood)
    _box(shop,Vector3(2.8,0.08,0.7),Color("#e6e2d8"),Vector3(-7.8,0.84,4.25))
    # Mirror is decorative; no third-person camera or reflection is enabled.
    _box(shop,Vector3(2.0,1.15,0.09),wood,Vector3(-7.8,1.8,3.85))
    _box(shop,Vector3(1.85,1.0,0.04),Color("#9eb2bb"),Vector3(-7.8,1.8,3.92))
    var chair := Node3D.new()
    chair.name="BarberChair"
    shop.add_child(chair)
    var at := Vector3(-7.8,0,5.15)
    _add_obstacle(at,Vector2(0.85,0.85))
    _cylinder(chair,0.36,0.09,Color("#64696b"),at+Vector3(0,0.05,0))
    _cylinder(chair,0.11,0.47,Color("#848b8e"),at+Vector3(0,0.30,0))
    _box(chair,Vector3(0.64,0.14,0.64),Color("#873c3b"),at+Vector3(0,0.58,0))
    _box(chair,Vector3(0.64,0.65,0.14),Color("#873c3b"),at+Vector3(0,0.95,0.28))
    for side in [-0.36,0.36]:
        _box(chair,Vector3(0.12,0.12,0.52),Color("#4b4240"),at+Vector3(side,0.82,0))
    _room_furniture(shop,Vector3(-9.52,0,6.9),Vector3(0.66,0.44,1.4),Color("#516d65"))
    for x in [-8.6,-8.2,-7.1]:
        _box(shop,Vector3(0.12,0.22,0.12),Color("#687b79"),Vector3(x,0.99,4.25))
    _physical_sign(shop,"09:00–19:00 · F",Vector3(-7.8,2.52,3.95),0,2.5)
    var light := OmniLight3D.new()
    light.position=Vector3(-7.8,2.65,6)
    light.light_energy=1.2
    light.omni_range=5.0
    shop.add_child(light)

func _build_centro() -> void:
    var piazza := Node3D.new()
    piazza.name = "PiazzaCentrale"
    _scenery.add_child(piazza)
    var stone := Color(0.68,0.64,0.57)
    _box(piazza,Vector3(34,0.035,34),stone,Vector3(0,0.006,0),"paving")
    _centro_paving(piazza)
    var colors := [Color("#c4a47a"),Color("#c9ad86"),Color("#c7a780"),Color("#ceb38b")]
    var variant := 0
    for side in [-1,1]:
        for row in [-1,1]:
            _centro_portico(piazza,float(side),float(row)*9.0,colors[variant],variant)
            variant += 1
    _centro_courtyard(piazza)
    _historic_cathedral(piazza)
    _build_barber(piazza)
    _build_centro_south_walk(piazza)
    # Landmark offset from the northern exit; no fictional church access.
    var tower := Vector3(8.0,0,-19.0)
    _add_obstacle(tower,Vector2(3.0,3.0))
    _box(piazza,Vector3(3,10,3),Color(0.59,0.34,0.25),tower+Vector3(0,5,0))
    _box(piazza,Vector3(3.5,0.45,3.5),Color(0.73,0.61,0.45),tower+Vector3(0,9.8,0))
    _box(piazza,Vector3(2.7,2.3,2.7),Color(0.68,0.42,0.30),tower+Vector3(0,11.15,0))
    _box(piazza,Vector3(3.2,0.35,3.2),Color(0.29,0.20,0.17),tower+Vector3(0,12.5,0))
    # Layered brick silhouette, crenellated cornice and clock face.
    for x in [-1.25,0.0,1.25]:
        for z in [-1.25,1.25]:
            _box(piazza,Vector3(0.42,0.65,0.42),Color("#956044"),tower+Vector3(x,13.0,z),"stucco")
    for x in [-1.25,1.25]:
        _box(piazza,Vector3(0.42,0.65,0.42),Color("#956044"),tower+Vector3(x,13.0,0),"stucco")
    _box(piazza,Vector3(1.85,2.6,1.85),Color("#b17e59"),tower+Vector3(0,14.0,0),"stucco")
    _historic_roof(piazza,tower+Vector3(0,15.3,0),2.1,2.1,0.85)
    var clock_face := Node3D.new()
    piazza.add_child(clock_face)
    clock_face.position=tower+Vector3(0,8.35,1.52)
    clock_face.rotation.x=PI*0.5
    _cylinder(clock_face,0.73,0.11,Color("#d6c8a9"),Vector3.ZERO)
    _cylinder(clock_face,0.61,0.13,Color("#ede0c3"),Vector3(0,0.035,0))
    for angle in range(12):
        var r := float(angle)*TAU/12.0
        _box(clock_face,Vector3(0.045,0.018,0.065),Color("#554a3b"),Vector3(sin(r)*0.49,0.11,cos(r)*0.49))
    _box(clock_face,Vector3(0.045,0.025,0.38),Color("#554a3b"),Vector3(0,0.12,0.13))
    _box(clock_face,Vector3(0.29,0.025,0.055),Color("#554a3b"),Vector3(-0.10,0.12,0))
    _physical_sign(piazza,"Torre Civica",tower+Vector3(0,2.2,1.62),0,2.0)
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
    _physical_sign(piazza,"Caffè Ducale",Vector3(-11.25,3.6,-9),PI*0.5,2.4)
    _physical_sign(piazza,"Botteghe",Vector3(11.25,3.6,-9),-PI*0.5,2.0)
    _box(piazza,Vector3(0.10,2.0,0.10),Color("#6a6c67"),Vector3(4,1,-12))
    _physical_sign(piazza,"Piazza Ducale",Vector3(4,2.05,-12),0,2.5)
    _physical_sign(piazza,"Bacheca lavoro · F",KIOSK_POSITION+Vector3(0,1.04,-0.85),PI,2.1)

func _centro_paving(parent: Node3D) -> void:
    var slab := PlaneMesh.new()
    slab.size=Vector2(24,34)
    var material := StandardMaterial3D.new()
    if not _surface_textures.has("ducale"):
        _surface_textures["ducale"]=TownVisuals.ducale_paving()
    material.albedo_texture=_surface_textures["ducale"]
    material.roughness=0.96
    slab.material=material
    var paving := MeshInstance3D.new()
    paving.name="DucalePebbleMosaic"
    paving.mesh=slab
    paving.position=Vector3(0,0.046,0)
    parent.add_child(paving)

# Sloped clay tiles, eaves and ridge; roof geometry does not alter walking collision.
func _historic_roof(parent: Node3D,at: Vector3,width: float,depth: float,rise: float) -> void:
    var half := width*0.5
    var slope := atan2(rise,half)
    var length := sqrt(half*half+rise*rise)
    for side in [-1.0,1.0]:
        var roof := Node3D.new()
        roof.position=at+Vector3(side*half*0.5,rise*0.5,0)
        roof.rotation.z=-side*slope
        parent.add_child(roof)
        _box(roof,Vector3(length+0.26,0.18,depth+0.32),Color("#975b43"),Vector3.ZERO,"terracotta")
    _box(parent,Vector3(0.18,0.20,depth+0.38),Color("#a46b4d"),at+Vector3(0,rise+0.04,0),"terracotta")
    var gable := SurfaceTool.new()
    gable.begin(Mesh.PRIMITIVE_TRIANGLES)
    for side in [-1.0,1.0]:
        var z: float=side*depth*0.5
        gable.add_vertex(Vector3(-half,0,z))
        gable.add_vertex(Vector3(half,0,z))
        gable.add_vertex(Vector3(0,rise,z))
    gable.generate_normals()
    var gable_mesh := MeshInstance3D.new()
    gable_mesh.mesh=gable.commit()
    var material := StandardMaterial3D.new()
    material.albedo_color=Color("#b49b7d")
    material.cull_mode=BaseMaterial3D.CULL_DISABLED
    TownVisuals.surface(material,_surface_textures["stucco"],0.32)
    gable_mesh.material_override=material
    gable_mesh.position=at
    parent.add_child(gable_mesh)

func _historic_roundel(parent: Node3D,at: Vector3,side: float,radius: float) -> void:
    var ring := Node3D.new()
    ring.position=at
    ring.rotation.z=PI*0.5
    parent.add_child(ring)
    _cylinder(ring,radius,0.10,Color("#cab69a"),Vector3.ZERO)
    _cylinder(ring,radius*0.72,0.12,Color("#423e37"),Vector3(0,side*0.055,0))

func _historic_cathedral(parent: Node3D) -> void:
    # Compressed landmark off the north pedestrian axis, with no inaccessible fake interaction.
    var church := Node3D.new()
    church.name="DuomoInspiredFacade"
    church.position=Vector3(-7,0,-20.1)
    parent.add_child(church)
    _add_obstacle(church.position,Vector2(7.4,5.2))
    var plaster := Color("#c8c1b2")
    var trim := Color("#ddd4c1")
    _box(church,Vector3(7.4,7.4,5.2),plaster,Vector3(0,3.7,0),"stucco")
    _historic_roof(church,Vector3(0,7.4,0),7.4,5.2,1.65)
    _box(church,Vector3(5.0,2.1,0.34),plaster,Vector3(0,8.15,2.66),"stucco")
    for x in [-3.35,-1.65,1.65,3.35]:
        _box(church,Vector3(0.34,6.5,0.38),trim,Vector3(x,3.55,2.78))
        _box(church,Vector3(0.58,0.22,0.52),trim,Vector3(x,6.77,2.82))
    for y in [0.25,3.9,6.9,9.15]:
        _box(church,Vector3(7.7 if y<8 else 5.25,0.19,0.48),trim,Vector3(0,y,2.8))
    for x in [-2.5,0.0,2.5]:
        var height := 3.0 if x==0.0 else 2.1
        _box(church,Vector3(1.35,height,0.13),Color("#514236"),Vector3(x,height*0.5,2.79),"wood")
        for sx in [-0.76,0.76]:
            _box(church,Vector3(0.16,height+0.17,0.22),trim,Vector3(x+sx,height*0.5,2.85))
        _box(church,Vector3(1.85,0.22,0.3),trim,Vector3(x,height+0.12,2.88))
    _box(church,Vector3(1.65,1.7,0.14),Color("#646e6b"),Vector3(0,5.45,2.79))
    for x in [-0.58,0.0,0.58]:
        _box(church,Vector3(0.08,1.72,0.17),trim,Vector3(x,5.45,2.89))
    _box(church,Vector3(0.12,1.0,0.12),Color("#53504b"),Vector3(0,9.7,2.72))
    _box(church,Vector3(0.62,0.12,0.12),Color("#53504b"),Vector3(0,9.88,2.72))

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
    _box(parent,Vector3(2.2,7.1,12),wall,Vector3(back,3.55,z),"stucco")
    _box(parent,Vector3(5.2,3.6,12),wall,Vector3(side*14,5.45,z),"stucco")
    _historic_roof(parent,Vector3(side*14,7.25,z),5.5,12.0,1.15)
    _box(parent,Vector3(5.7,0.14,12.3),trim,Vector3(side*14,7.03,z))
    _box(parent,Vector3(5.2,0.16,12),trim,Vector3(side*14,3.82,z))
    _box(parent,Vector3(0.15,0.29,12),Color("#a98363"),Vector3(side*11.32,6.75,z))
    _box(parent,Vector3(0.18,0.11,12),trim,Vector3(side*11.25,6.96,z))
    _box(parent,Vector3(0.16,0.16,12),trim,Vector3(side*11.35,5.35,z))
    for offset in [-6.0,-2.0,2.0,6.0]:
        var at := Vector3(front,0,z+offset)
        _add_obstacle(at,Vector2(0.55,0.55))
        _cylinder(parent,0.22,1.95,Color("#a8a29a"),at+Vector3(0,0.975,0))
        _box(parent,Vector3(0.70,0.15,0.70),trim.darkened(0.12),at+Vector3(0,0.075,0))
        _box(parent,Vector3(0.71,0.17,0.71),trim,at+Vector3(0,1.88,0))
    for offset in [-4.0,0.0,4.0]:
        # Segmented semicircular stone arches; the open space below is walkable.
        for i in range(9):
            var angle := float(i)*PI/8.0
            var arch := Node3D.new()
            arch.position = Vector3(front,1.95+sin(angle)*1.72,z+offset+cos(angle)*1.72)
            arch.rotation.x = -angle
            parent.add_child(arch)
            _box(arch,Vector3(0.58,0.74,0.36),trim,Vector3.ZERO)
        for y in [4.65]:
            var face := side*11.36
            _box(parent,Vector3(0.08,1.1,0.85),Color(0.19,0.28,0.30),Vector3(face,y,z+offset))
            _box(parent,Vector3(0.09,1.08,0.055),trim,Vector3(face-side*0.035,y,z+offset))
            _box(parent,Vector3(0.09,0.05,0.83),trim,Vector3(face-side*0.035,y,z+offset))
            var shutters := Color("#415943") if variant%2 == 0 else Color("#64513f")
            _box(parent,Vector3(0.15,0.12,1.6),trim,Vector3(face-side*0.06,y-0.61,z+offset))
            for shutter in [-0.64,0.64]:
                _box(parent,Vector3(0.13,1.18,0.35),shutters,Vector3(face-side*0.06,y,z+offset+shutter),"wood")
            if y < 5.0 and (variant == 1 or variant == 3) and offset == 0.0:
                _centro_balcony(parent,side,face,y,z+offset)
            elif y > 5.0 and variant%2 == 0:
                _centro_flowers(parent,Vector3(face-side*0.22,y-0.60,z+offset))
        _historic_roundel(parent,Vector3(side*11.30,5.14,z+offset),side,0.43)
        _historic_roundel(parent,Vector3(side*11.30,6.18,z+offset),side,0.33)
        for dz in [-1.65,1.65]:
            _box(parent,Vector3(0.09,1.6,0.11),trim,Vector3(side*11.30,4.9,z+offset+dz))
        var rear := side*14.64
        # Recessed shop fronts have a frame, glass panes and timber door.
        _box(parent,Vector3(0.12,2.35,2.55),Color("#6d5140"),Vector3(rear,1.175,z+offset))
        for pane in [-0.83,0.83]:
            _box(parent,Vector3(0.08,1.52,0.72),Color("#496064"),Vector3(rear-side*0.09,1.29,z+offset+pane))
            var pane_node := parent.get_child(parent.get_child_count()-1) as MeshInstance3D
            pane_node.material_override = _shop_glass
            _box(parent,Vector3(0.09,0.055,0.72),Color("#b99b74"),Vector3(rear-side*0.145,0.91,z+offset+pane))
            _box(parent,Vector3(0.09,0.055,0.72),Color("#b99b74"),Vector3(rear-side*0.145,1.67,z+offset+pane))
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
    for at in [Vector3(-8,0,-5),Vector3(-15,0,9),Vector3(10,0,11)]:
        _park_flowers(park,at)
    # Southern garden promenade occupies previously unused space in the tile.
    var garden := Node3D.new()
    garden.name="SouthGardenPromenade"
    park.add_child(garden)
    _box(garden,Vector3(36,0.06,2.4),paving,Vector3(0,0.01,18.6),"paving")
    for x in [-12.0,12.0]:
        _box(garden,Vector3(2.4,0.06,11.0),paving,Vector3(x,0.01,12.0),"paving")
        _bench(garden,Vector3(x,0,20.1))
        _lamp(garden,Vector3(x*1.35,0,16.8))
        _park_flowers(garden,Vector3(x*1.4,0,19.9))
    _tree(Vector3(-18.5,0,13.0))
    _tree(Vector3(18.5,0,13.0))
    _build_playground(park)
    # The pool is decorative; drinking still requires the validated Java action.
    var fountain := Node3D.new()
    fountain.name = "PublicFountain"
    fountain.position = Vector3(8, 0, -5)
    park.add_child(fountain)
    _add_obstacle(fountain.position, Vector2(4.0, 4.0))
    _cylinder(fountain, 2.0, 0.35, Color(0.52,0.53,0.54), Vector3(0,0.18,0))
    _cylinder(fountain, 1.85, 0.08, Color(0.24,0.28,0.29), Vector3(0,0.39,0))
    _fountain_water = _cylinder(fountain, 1.78, 0.04, Color(0.24,0.63,0.76), Vector3(0,0.45,0))
    _fountain_water.name = "FountainWater"
    _fountain_water.material_override = _water_material
    var rim := TorusMesh.new()
    rim.inner_radius = 1.77
    rim.outer_radius = 2.0
    rim.rings = 32
    rim.ring_segments = 12
    var rim_node := MeshInstance3D.new()
    rim_node.mesh = rim
    var stone := StandardMaterial3D.new()
    stone.albedo_color = Color("#b6ad99")
    rim_node.material_override = stone
    rim_node.position = Vector3(0,0.43,0)
    fountain.add_child(rim_node)
    _cylinder(fountain,0.38,0.16,Color("#b6ad99"),Vector3(0,0.55,0))
    _cylinder(fountain, 0.25, 1.1, Color(0.60,0.61,0.62), Vector3(0,0.85,0))
    _cylinder(fountain,0.36,0.12,Color("#b6ad99"),Vector3(0,1.39,0))
    _add_fountain_spray(_fountain_water)
    _sign(fountain, "Public fountain", Vector3(0,1.8,0))
    # Shared gate and kiosk already have labelled signs.
    # A street along the edge provides visual context, not a new travel route.
    _box(park, Vector3(38,0.08,3.2), Color(0.23,0.25,0.27), Vector3(0,0.02,-16),"asphalt")
    for x in range(-16,17,4):
        _box(park, Vector3(1.5,0.02,0.11), Color(0.92,0.85,0.61), Vector3(x,0.07,-16))
    for x in [-14.0,-8.0,8.0,14.0]:
        _add_obstacle(Vector3(x,0,-19), Vector2(4.7,2.5))
        _box(park, Vector3(4.7,4.0,2.5), Color(0.76,0.64,0.49), Vector3(x,2,-19),"stucco")
        _box(park,Vector3(5.0,0.22,2.8),Color("#95533e"),Vector3(x,4.07,-19),"terracotta")
        _box(park,Vector3(4.85,0.12,0.18),Color("#e0c6a0"),Vector3(x,2.05,-17.68))
        _box(park,Vector3(1.05,1.85,0.12),Color("#71513b"),Vector3(x,0.925,-17.68),"wood")
        for offset in [-1.2,1.2]:
            _box(park,Vector3(0.98,1.28,0.10),Color("#ddc7a6"),Vector3(x+offset,2.8,-17.70))
            _box(park, Vector3(0.8,1.1,0.08), Color(0.32,0.49,0.57), Vector3(x+offset,2.8,-17.62))
            _box(park,Vector3(0.055,1.08,0.08),Color("#e0c6a0"),Vector3(x+offset,2.8,-17.56))

func _build_playground(parent: Node3D) -> void:
    var play := Node3D.new()
    play.name="ParcoGiochi"
    play.position=Vector3(-7.8,0,14.5)
    parent.add_child(play)
    _box(play,Vector3(6.8,0.06,5.2),Color("#c2a17b"),Vector3(0,0.025,0))
    var blue := Color("#527f9b")
    var yellow := Color("#e6b54c")
    # Swing frame, with a collision footprint; no fake ride interaction.
    var swing := Node3D.new()
    swing.name="SwingFrame"
    swing.position=Vector3(-1.7,0,-0.6)
    play.add_child(swing)
    for x in [-1.15,1.15]:
        for z in [-0.65,0.65]:
            var leg := _cylinder(swing,0.055,2.35,blue,Vector3(x,1.08,z*0.5))
            leg.rotation.x=-z*0.42
    _box(swing,Vector3(2.5,0.11,0.11),blue,Vector3(0,2.2,0))
    for x in [-0.28,0.28]:
        _cylinder(swing,0.018,1.65,Color("#777b79"),Vector3(x,1.32,0))
    _box(swing,Vector3(0.65,0.10,0.38),yellow,Vector3(0,0.49,0))
    _add_obstacle(play.position+swing.position,Vector2(2.65,1.7))
    # Small slide and climbing steps, reserved as physical scenery.
    var slide := Node3D.new()
    slide.name="Slide"
    slide.position=Vector3(1.5,0,0)
    play.add_child(slide)
    for x in [-0.42,0.42]:
        _cylinder(slide,0.055,1.5,blue,Vector3(x,0.75,-0.65))
    _box(slide,Vector3(1.0,0.12,0.85),yellow,Vector3(0,1.45,-0.65))
    for i in range(4):
        _box(slide,Vector3(0.82,0.08,0.27),blue,Vector3(0,0.33+float(i)*0.30,-1.4+float(i)*0.16))
    var chute := _box_node(slide,Vector3(0.88,0.10,2.2),yellow,Vector3(0,0.78,0.45))
    chute.rotation.x=0.59
    _add_obstacle(play.position+slide.position,Vector2(1.15,3.1))
    _physical_sign(play,"Parco giochi",Vector3(0,1.65,2.55),0,2.0)
    # Leave the south edge open for access from the promenade.

func _add_fountain_spray(water: MeshInstance3D) -> void:
    var spray := CPUParticles3D.new()
    spray.name = "FountainSpray"
    spray.position = Vector3(0,1.03,0)
    spray.amount = 48
    spray.lifetime = 0.95
    spray.local_coords = true
    spray.direction = Vector3.UP
    spray.spread = 28.0
    spray.initial_velocity_min = 1.65
    spray.initial_velocity_max = 2.5
    spray.gravity = Vector3(0,-8.5,0)
    spray.emitting = false
    var droplet := SphereMesh.new()
    droplet.radius = 0.023
    droplet.height = 0.046
    droplet.radial_segments = 6
    droplet.rings = 3
    var mat := StandardMaterial3D.new()
    mat.albedo_color = Color("#bbdfdf")
    mat.roughness = 0.23
    droplet.material = mat
    spray.mesh = droplet
    water.add_child(spray)

func _park_flowers(parent: Node3D, at: Vector3) -> void:
    # Small decorative flowers hug the tree bases, never create obstacles.
    var blossom := SphereMesh.new()
    blossom.radius = 0.065
    blossom.height = 0.10
    blossom.radial_segments = 6
    blossom.rings = 3
    var mat := StandardMaterial3D.new()
    mat.vertex_color_use_as_albedo = true
    mat.roughness = 0.95
    blossom.material = mat
    var batch := MultiMesh.new()
    batch.transform_format = MultiMesh.TRANSFORM_3D
    batch.use_colors = true
    batch.mesh = blossom
    batch.instance_count = 40
    var rng := RandomNumberGenerator.new()
    rng.seed = absi(roundi(at.x*127.0+at.z*53.0))
    var colors := [Color("#f0c250"),Color("#e0a5b7"),Color("#f5ede0"),Color("#ae727d")]
    for i in range(batch.instance_count):
        var angle := rng.randf_range(0.0,TAU)
        var radius := rng.randf_range(0.38,0.85)
        var pos := at+Vector3(cos(angle)*radius,rng.randf_range(0.035,0.09),sin(angle)*radius)
        batch.set_instance_transform(i,Transform3D(Basis.IDENTITY,pos))
        batch.set_instance_color(i,colors[i%colors.size()])
    var bed := MultiMeshInstance3D.new()
    bed.name = "Wildflowers"
    bed.multimesh = batch
    parent.add_child(bed)
    _register_graphics_detail(bed)

func _bench(parent: Node3D, at: Vector3) -> void:
    _add_obstacle(at, Vector2(2.0, 0.66))
    for x in [-0.50,0.50]:
        _register_resident_seat(at+Vector3(x,0.58,0),0.0,"bench")
    var wood := Color(0.43,0.27,0.14)
    for z in [-0.20,0.0,0.20]:
        _box(parent,Vector3(2.0,0.09,0.17),wood,at+Vector3(0,0.52,z),"wood")
    for y in [0.70,0.86,1.02]:
        _box(parent,Vector3(2.0,0.13,0.09),wood,at+Vector3(0,y,0.25),"wood")
    for x in [-0.75,0.75]:
        _box(parent, Vector3(0.11,0.50,0.47), Color(0.20,0.23,0.24), at+Vector3(x,0.25,0))

func _lamp(parent: Node3D, at: Vector3) -> void:
    _add_obstacle(at, Vector2(0.18, 0.18))
    _cylinder(parent, 0.09, 3.4, Color(0.19,0.22,0.24), at+Vector3(0,1.7,0))
    _cylinder(parent,0.18,0.3,Color("#3c4140"),at+Vector3(0,0.15,0))
    _cylinder(parent,0.14,0.10,Color("#3c4140"),at+Vector3(0,3.17,0))
    _box(parent,Vector3(0.32,0.39,0.32),Color("#e4d4ac"),at+Vector3(0,3.4,0))
    var glass := parent.get_child(parent.get_child_count()-1) as MeshInstance3D
    glass.material_override = _lantern_glass
    for x in [-0.17,0.17]:
        for z in [-0.17,0.17]:
            _box(parent,Vector3(0.035,0.46,0.035),Color("#3c4140"),at+Vector3(x,3.4,z))
    _cylinder(parent,0.24,0.08,Color("#343b39"),at+Vector3(0,3.64,0))
    _sphere(parent,0.07,Color("#343b39"),at+Vector3(0,3.72,0))
    var lamp := OmniLight3D.new()
    lamp.position = at+Vector3(0,3.2,0)
    lamp.light_color = Color(1.0,0.81,0.53)
    lamp.light_energy = 2.0
    lamp.omni_range = 8.0
    lamp.visible = false
    parent.add_child(lamp)
    _park_lamps.append(lamp)

func _district_sign_name(id: String) -> String:
    if TownLayout.outer(id):return TownLayout.label_for(id)
    return str({"PARK":"Parco","MARKET":"Mercato","CAFE":"Bar Centrale","BAKERY":"Panetteria","LIBRARY":"Biblioteca","OFFICE":"Uffici","CLINIC":"Ambulatorio","SCHOOL":"Scuola","WORKSHOP":"Officina","NORTH_RESIDENCES":"Quartiere Nord","SOUTH_RESIDENCES":"Quartiere Sud","RIVER":"Fiume","WOODS":"Bosco","QUARRY":"Cava","FACTORY":"Fabbrica","CENTRO":"Piazza Centrale","PORTICI":"Via dei Portici","FARM":"Cascina","FUEL_STATION":"Stazione di servizio","LOGISTICS_DEPOT":"Deposito logistico","BORGO_SUD":"Borgo Sud","PIAZZA_FONTANA":"Piazza Fontana","GIARDINI":"Giardini Comunali","STAZIONE":"Piazza Stazione","VIA_TIGLI":"Via dei Tigli"}.get(id,id))

func _street_marker(place_id: String) -> void:
    var street := str({"PARK":"Viale del Parco","MARKET":"Piazza del Mercato","CAFE":"Via del Caffè","BAKERY":"Via del Pane","LIBRARY":"Via dei Libri","OFFICE":"Via dell'Industria","CLINIC":"Via della Salute","SCHOOL":"Via Leonardo","WORKSHOP":"Via degli Artigiani","NORTH_RESIDENCES":"Via delle Magnolie","SOUTH_RESIDENCES":"Via del Glicine","RIVER":"Lungofiume","WOODS":"Via del Bosco","QUARRY":"Via della Cava","FACTORY":"Via delle Fabbriche","CENTRO":"Piazza Centrale","PORTICI":"Via dei Portici","FARM":"Strada delle Risaie","FUEL_STATION":"Via della Stazione","LOGISTICS_DEPOT":"Via del Deposito","BORGO_SUD":"Via del Borgo Sud","PIAZZA_FONTANA":"Piazza Fontana","GIARDINI":"Viale dei Giardini","STAZIONE":"Piazza Stazione","VIA_TIGLI":"Via dei Tigli"}.get(place_id,"Via del Borgo"))
    if TownLayout.outer(place_id):street=TownLayout.label_for(place_id)
    var marker := Node3D.new()
    marker.name = "StreetMarker"
    _scenery.add_child(marker)
    marker.position = Vector3(3.8,0,-11.5)
    _box(marker,Vector3(0.10,2.1,0.10),Color("#686e70"),Vector3(0,1.05,0))
    _physical_sign(marker,street,Vector3(0,2.12,0),0,3.0)

func _physical_sign(parent: Node3D, caption: String, at: Vector3, yaw: float, width: float) -> void:
    var board := Node3D.new()
    board.name = "PhysicalSign"
    parent.add_child(board)
    board.position = at
    board.rotation.y = yaw
    _box(board,Vector3(width,0.46,0.075),Color("#e4daca"),Vector3.ZERO)
    var tag := Label3D.new()
    tag.text = caption
    tag.position = Vector3(0,0,0.045)
    tag.billboard = BaseMaterial3D.BILLBOARD_DISABLED
    tag.font_size = 32
    tag.pixel_size = 0.005
    tag.modulate = Color("#233d49")
    tag.outline_size = 0
    board.add_child(tag)

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
        person.set_meta("conversation_minutes",int(resident.get("conversationMinutes",10)))
        person.set_meta("conversation_reason",str(resident.get("conversationReason","PERSONAL")))
        person.set_meta("mood",str(resident.get("mood","CALM")))
        person.set_meta("walking_companion",str(resident.get("walkingCompanion","")))
        _sync_work_prop(person,str(resident.get("workVisual","")))
        _sync_resident_uniform(person,str(resident.get("outfitVisual","")))
        _sync_resident_appearance(person,str(resident.get("appearanceStage","ADULT")))
        person.set_meta("personal_debt",int(resident.get("personalDebt",0)))
        person.set_meta("personal_debt_due",int(resident.get("personalDebtDueDay",0)))
        _sync_office_employee(person,id)
        _sync_dojo_member(person,id)
        person.set_meta("display_name",str(resident.get("name","Unknown resident")))
        person.get_node("ResidentName").text = tr(str(person.get_meta("display_name")))
        person.get_node("ResidentName").hide()
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

    _assign_resident_seats()
    _assign_work_stations()
    _assign_market_queues()
    _assign_street_aid_spots()
    _assign_caritas_spots()
    _sync_office_joker()
    _sync_hosted_guest()

func _sync_hosted_guest() -> void:
    var active_id := ""
    var phone_data = _last_snapshot.get("phone")
    if _current_place=="SOUTH_RESIDENCES" and _owns_house() and phone_data is Dictionary:
        for meeting in phone_data.get("meetings",[]):
            if bool(meeting.get("guestPresent",false)):
                active_id=str(meeting.get("residentId",""))
                break
    for person in _residents_root.get_children():
        var visiting: bool = str(person.get_meta("resident_id",""))==active_id
        var was_visiting: bool = bool(person.get_meta("hosted_guest",false))
        if visiting and not was_visiting:
            person.set_meta("original_anchor",person.get_meta("anchor",person.position))
            var at := _open_anchor(Vector3(-6.5,0,8.5))
            person.set_meta("anchor",at)
            person.position=at
        elif was_visiting and not visiting:
            var at: Vector3 = person.get_meta("original_anchor",Vector3.ZERO)
            person.set_meta("anchor",at)
            person.position=_open_anchor(at)
        person.set_meta("hosted_guest",visiting)
        if visiting:
            if int(person.get_meta("seat_index",-1))>=0:
                person.position=person.get_meta("anchor",person.position)
            person.set_meta("seat_index",-1)
            person.set_meta("activity","SOCIALIZING")
            person.set_meta("walking_companion","")

# Presentation follows the server's current visible work equipment, never a guessed identity.
func _sync_work_prop(person: Node3D, kind: String) -> void:
    var hand := person.get_node("ArmRight") as Node3D
    var old := hand.get_node_or_null("WorkProp") as Node3D
    if old!=null and str(old.get_meta("kind",""))==kind:return
    if old!=null:
        hand.remove_child(old)
        old.queue_free()
    person.set_meta("work_visual",kind)
    if kind.is_empty():return
    var prop := Node3D.new()
    prop.name="WorkProp"
    prop.set_meta("kind",kind)
    prop.position=Vector3(0,-0.48,-0.12)
    hand.add_child(prop)
    match kind:
        "PICK":
            _box(prop,Vector3(0.045,0.70,0.045),Color("#95704d"),Vector3(0,-0.12,0))
            _box(prop,Vector3(0.37,0.07,0.07),Color("#686e70"),Vector3(0,-0.45,0))
        "WRENCH":
            _box(prop,Vector3(0.055,0.24,0.035),Color("#aeb6ba"),Vector3(0,-0.08,0))
            for side in [-1,1]:
                _box(prop,Vector3(0.035,0.08,0.035),Color("#aeb6ba"),Vector3(side*0.045,-0.22,0))
            _box(prop,Vector3(0.12,0.045,0.035),Color("#aeb6ba"),Vector3(0,-0.17,0))
        "BREAD":
            _sphere(prop,0.12,Color("#c38c47"),Vector3.ZERO)
            var loaf := prop.get_child(prop.get_child_count()-1) as Node3D
            loaf.scale=Vector3(1.6,0.65,0.8)
        "PIZZA":
            _cylinder(prop,0.15,0.04,Color("#c68b43"),Vector3.ZERO)
            _cylinder(prop,0.12,0.02,Color("#b9543c"),Vector3(0,0.025,0))
        "GELATO":
            _cylinder(prop,0.055,0.09,Color("#e7dfcb"),Vector3.ZERO)
            _sphere(prop,0.06,Color("#c98b99"),Vector3(0,0.065,0))
        "COFFEE":
            _cylinder(prop,0.075,0.10,Color("#f4ebdb"),Vector3.ZERO)
        "CRATE":
            _box(prop,Vector3(0.29,0.17,0.23),Color("#99724a"),Vector3.ZERO)
            for x in [-0.08,0.08]:
                _sphere(prop,0.055,Color("#b94c38"),Vector3(x,0.10,0))
        "MAIL_BAG":
            _box(prop,Vector3(0.25,0.30,0.13),Color("#525f72"),Vector3(0,-0.09,0))
            _box(prop,Vector3(0.24,0.055,0.14),Color("#d8b74f"),Vector3(0,0.06,0))
            _box(prop,Vector3(0.15,0.09,0.02),Color("#f0eee5"),Vector3(0,0.10,0))
        "PAPERS","TEACHING":
            _box(prop,Vector3(0.22,0.035,0.30),Color("#a17e53"),Vector3.ZERO)
            _box(prop,Vector3(0.19,0.012,0.26),Color("#f1eee4"),Vector3(0,0.025,0))
    prop.show()

# Outfit meshes are presentation only; they do not grant equipment or clothing ownership.
func _sync_resident_uniform(person: Node3D,kind: String) -> void:
    if str(person.get_meta("outfit_visual",""))==kind:return
    var previous := person.get_node_or_null("Uniform")
    if previous!=null:
        person.remove_child(previous)
        previous.queue_free()
    person.set_meta("outfit_visual",kind)
    if kind.is_empty():return
    var uniform := Node3D.new()
    uniform.name="Uniform"
    person.add_child(uniform)
    match kind:
        "BAKER_APRON","CAFE_APRON":
            var cloth := Color("#f3eee3") if kind=="BAKER_APRON" else Color("#4c645c")
            _box(uniform,Vector3(0.37,0.62,0.055),cloth,Vector3(0,1.03,-0.19))
            _box(uniform,Vector3(0.18,0.14,0.03),cloth.darkened(0.13),Vector3(0,0.91,-0.225))
            for x in [-0.12,0.12]:
                _box(uniform,Vector3(0.045,0.25,0.04),cloth,Vector3(x,1.37,-0.16))
            if kind=="BAKER_APRON":
                _cylinder(uniform,0.19,0.14,cloth,Vector3(0,1.92,0))
        "DOCTOR_COAT","SCRUBS":
            var cloth := Color("#f4f1e9") if kind=="DOCTOR_COAT" else Color("#528c91")
            var coat := _cylinder(uniform,0.255,0.68,cloth,Vector3(0,1.08,0))
            coat.scale.z=0.76
            _box(uniform,Vector3(0.045,0.53,0.03),cloth.darkened(0.2),Vector3(0,1.09,-0.204))
            _box(uniform,Vector3(0.105,0.06,0.025),Color("#78949d"),Vector3(0.13,1.29,-0.205))
        "TECHNICIAN","HIGH_VIS":
            var cloth := Color("#384f65") if kind=="TECHNICIAN" else Color("#d5a63c")
            var vest := _cylinder(uniform,0.255,0.54,cloth,Vector3(0,1.15,0))
            vest.scale.z=0.77
            if kind=="HIGH_VIS":
                for y in [1.04,1.24]:
                    var stripe := _cylinder(uniform,0.259,0.055,Color("#e7e9cf"),Vector3(0,y,0))
                    stripe.scale.z=0.78
                _cylinder(uniform,0.235,0.045,Color("#d6ac39"),Vector3(0,1.83,0))
                var helmet := _sphere(uniform,0.22,Color("#e7bc42"),Vector3(0,1.88,0))
                helmet.scale.y=0.52
            else:
                _box(uniform,Vector3(0.24,0.24,0.18),Color("#443a30"),Vector3(0.26,0.91,0.04))
                _box(uniform,Vector3(0.055,0.14,0.06),Color("#b1b8b9"),Vector3(0.26,1.07,0.04))
        "POSTAL":
            var vest := _cylinder(uniform,0.255,0.54,Color("#405c78"),Vector3(0,1.15,0))
            vest.scale.z=0.77
            _box(uniform,Vector3(0.12,0.055,0.025),Color("#e0bb4a"),Vector3(0.12,1.30,-0.205))
        "POLICE","DETECTIVE":
            var uniform_color := Color("#233b5b") if kind=="POLICE" else Color("#4a4844")
            var jacket := _cylinder(uniform,0.27,0.63,uniform_color,Vector3(0,1.12,0))
            jacket.scale.z=0.79
            _box(uniform,Vector3(0.045,0.49,0.032),uniform_color.darkened(0.18),Vector3(0,1.13,-0.215))
            _box(uniform,Vector3(0.068,0.065,0.02),Color("#d9bb77"),Vector3(-0.125,1.3,-0.225))
            if kind=="POLICE":
                _cylinder(uniform,0.225,0.105,uniform_color,Vector3(0,1.88,0))
                _box(uniform,Vector3(0.30,0.025,0.17),Color("#18242f"),Vector3(0,1.84,-0.15))
                _box(uniform,Vector3(0.05,0.14,0.055),Color("#18242f"),Vector3(0.18,1.3,-0.20))
                var label := Label3D.new()
                label.text="POLIZIA"
                label.font_size=26
                label.pixel_size=0.003
                label.position=Vector3(0,1.23,0.216)
                label.outline_size=0
                uniform.add_child(label)
            else:
                _box(uniform,Vector3(0.20,0.28,0.04),Color("#77573d"),Vector3(0.19,1.0,-0.23))
        "BACKPACK":
            _box(uniform,Vector3(0.37,0.43,0.21),Color("#526c90"),Vector3(0,1.15,0.25))
            _box(uniform,Vector3(0.25,0.18,0.08),Color("#384c6c"),Vector3(0,1.08,0.39))
            for x in [-0.17,0.17]:
                _box(uniform,Vector3(0.05,0.45,0.06),Color("#526c90"),Vector3(x,1.15,-0.15))

func _resident_outdoors(person: Node3D) -> bool:
    if _apartment_active or _guest_active or _stairwell_active:return false
    # Conservative outdoor districts only; avoid guessing shelter in mixed office/home scenes.
    if _current_place not in ["PARK","CENTRO","RIVER","WOODS","MARKET","QUARRY","BAKERY","CAFE","PORTICI"]:return false
    var door := _public_door()
    if door!=null:
        var inside: Rect2=door.get_meta("inside")
        if inside.has_point(Vector2(person.position.x,person.position.z)):return false
    return true

func _sync_resident_weather(person: Node3D,talking: bool,seated: bool) -> bool:
    var weather: Dictionary=_last_snapshot.get("weather",{})
    var outdoors := _resident_outdoors(person)
    var activity := str(person.get_meta("activity",""))
    var cold := outdoors and int(weather.get("temperatureC",18))<=8 and activity!="WORKING" and str(person.get_meta("outfit_visual","")).is_empty()
    var coat := person.get_node_or_null("WinterClothes") as Node3D
    if cold and coat==null:
        coat=Node3D.new()
        coat.name="WinterClothes"
        person.add_child(coat)
        var profile: Dictionary=person.get_meta("appearance",{})
        var paint: Color=profile.get("jacket",Color("#52636c"))
        var jacket := _cylinder(coat,0.27,0.63,paint.darkened(0.12),Vector3(0,1.12,0))
        jacket.scale.z=0.83
        _cylinder(coat,0.15,0.12,Color("#aa775d"),Vector3(0,1.48,0))
        _box(coat,Vector3(0.11,0.29,0.045),Color("#aa775d"),Vector3(0.10,1.35,-0.21))
        var hat := _sphere(coat,0.215,paint.darkened(0.25),Vector3(0,1.79,0))
        hat.scale.y=0.60
    if coat!=null:coat.visible=cold
    var use_umbrella := outdoors and bool(weather.get("raining",false)) and not seated and not talking and activity!="WORKING" and activity in ["WALKING","WANDERING","STROLLING","FREE_TIME","SHOPPING","JOB_SEARCH"] and absi(str(person.get_meta("resident_id","")).hash())%3!=0
    var umbrella := person.get_node_or_null("Umbrella") as Node3D
    if use_umbrella and umbrella==null:
        umbrella=Node3D.new()
        umbrella.name="Umbrella"
        person.add_child(umbrella)
        var profile: Dictionary=person.get_meta("appearance",{})
        var paint: Color=profile.get("jacket",Color("#52636c"))
        _cylinder(umbrella,0.016,1.18,Color("#555a5b"),Vector3(-0.29,1.60,-0.34))
        var canopy := _cylinder(umbrella,0.67,0.22,paint,Vector3(-0.29,2.25,-0.34))
        var mesh := canopy.mesh as CylinderMesh
        mesh.top_radius=0.06
        mesh.bottom_radius=0.67
        _sphere(umbrella,0.035,Color("#555a5b"),Vector3(-0.29,2.39,-0.34))
    if umbrella!=null:umbrella.visible=use_umbrella
    return use_umbrella

func _assign_food_staff() -> void:
    var used: Dictionary={}
    for person in _residents_root.get_children():
        var kind := str(person.get_meta("work_visual",""))
        var working: bool = str(person.get_meta("activity",""))=="WORKING" and kind in ["PIZZA","GELATO"]
        if working and not used.has(kind):
            var at := Vector3(-6.0 if kind=="PIZZA" else 6.0,0,19.9)
            if not _can_stand(at,WALKER_RADIUS):continue
            used[kind]=true
            if not person.has_meta("pre_work_anchor"):person.set_meta("pre_work_anchor",person.get_meta("anchor",person.position))
            person.position=at
            person.set_meta("anchor",at)
            person.rotation.y=0.0
        elif person.has_meta("pre_work_anchor"):
            var previous: Vector3=person.get_meta("pre_work_anchor")
            person.set_meta("anchor",previous)
            person.position=_open_anchor(previous)
            person.remove_meta("pre_work_anchor")

func _assign_work_stations() -> void:
    if _apartment_active:return
    if _current_place=="CENTRO":
        _assign_food_staff()
        return
    if _current_place=="SCHOOL":
        _assign_school_staff()
        return
    var stations: Array[Vector3] = []
    var yaw := 0.0
    match _current_place:
        "BAKERY":
            stations=[Vector3(-9.1,0,6.6),Vector3(-9.1,0,7.8),Vector3(-9.1,0,8.8)]
            yaw=-PI/2.0
        "CAFE":
            stations=[Vector3(-9.65,0,4.4),Vector3(-9.65,0,5.6),Vector3(-9.65,0,6.8)]
            yaw=-PI/2.0
        "QUARRY":
            stations=[Vector3(-11,0,4.7),Vector3(-8,0,4.7),Vector3(-5,0,4.7)]
        "FACTORY":
            stations=[Vector3(-8,0,0.8),Vector3(-5,0,0.8),Vector3(-2,0,0.8)]
            yaw=PI
        "WORKSHOP":
            stations=[Vector3(12.5,0,9.5),Vector3(7.5,0,10.0)]
            yaw=PI
        "MARKET":
            stations=[Vector3(-9,0,2.3),Vector3(-7,0,2.3),Vector3(-5,0,2.3)]
            yaw=0.0
        _:
            return
    var people := _residents_root.get_children()
    people.sort_custom(func(a,b): return str(a.get_meta("resident_id","")) < str(b.get_meta("resident_id","")))
    for child in people:
        var person := child as Node3D
        var kind := str(person.get_meta("work_visual",""))
        var eligible := str(person.get_meta("activity",""))=="WORKING" and not kind.is_empty()
        if eligible:
            while not stations.is_empty():
                var at: Vector3 = stations.pop_front()
                if not _can_stand(at,WALKER_RADIUS):continue
                if not person.has_meta("pre_work_anchor"):
                    person.set_meta("pre_work_anchor",person.get_meta("anchor",person.position))
                person.position=at
                person.set_meta("anchor",at)
                person.rotation.y=yaw
                break
        elif person.has_meta("pre_work_anchor"):
            var at: Vector3 = person.get_meta("pre_work_anchor")
            person.set_meta("anchor",at)
            if int(person.get_meta("seat_index",-1))<0:
                person.position=_open_anchor(at)
            person.remove_meta("pre_work_anchor")

# Teachers use real working snapshots, not arbitrary bystanders or dojo members.
func _assign_school_staff() -> void:
    var spots: Array[Vector3] = [Vector3(-12.15,0,7),Vector3(-11.0,0,6.6),Vector3(-10.0,0,7.4),Vector3(-11.0,0,7.7),Vector3(-9.25,0,6.6)]
    var people := _residents_root.get_children()
    people.sort_custom(func(a,b): return str(a.get_meta("resident_id","")) < str(b.get_meta("resident_id","")))
    for child in people:
        var person := child as Node3D
        var teaching: bool = str(person.get_meta("activity",""))=="WORKING" and str(person.get_meta("work_visual",""))=="TEACHING" and not person.get_meta("dojo_member",false)
        if teaching:
            while not spots.is_empty():
                var at: Vector3 = spots.pop_front()
                if not _can_stand(at,WALKER_RADIUS):continue
                if not person.has_meta("pre_school_anchor"):
                    person.set_meta("pre_school_anchor",person.get_meta("anchor",person.position))
                person.position=at
                person.set_meta("anchor",at)
                person.rotation.y=-PI/2.0
                break
        elif person.has_meta("pre_school_anchor"):
            var previous: Vector3=person.get_meta("pre_school_anchor")
            if not person.get_meta("dojo_member",false):
                person.set_meta("anchor",previous)
                person.position=_open_anchor(previous)
            person.remove_meta("pre_school_anchor")

# Presentation only: show genuine SHOPPING residents waiting by occupied counters.
# The authoritative simulation remains responsible for purchases, stock and cash.
func _assign_market_queues() -> void:
    if _current_place!="MARKET" or _apartment_active or _guest_active or _stairwell_active:return
    var people := _residents_root.get_children()
    people.sort_custom(func(a,b): return str(a.get_meta("resident_id","")) < str(b.get_meta("resident_id","")))
    var lanes: Array[float]=[]
    for child in people:
        var vendor := child as Node3D
        if str(vendor.get_meta("activity",""))=="WORKING" and str(vendor.get_meta("work_visual",""))=="CRATE" and vendor.has_meta("pre_work_anchor"):
            if absf(vendor.position.z-2.3)<0.1 and not lanes.has(vendor.position.x):
                lanes.append(vendor.position.x)
    lanes.sort()
    var slots: Array[Vector3]=[]
    for depth in range(3):
        for x in lanes:
            var at := Vector3(x,0,0.12-float(depth)*1.1)
            if _can_stand(at,WALKER_RADIUS):slots.append(at)
    for child in people:
        var person := child as Node3D
        if str(person.get_meta("activity",""))=="SHOPPING" and not slots.is_empty():
            person.set_meta("market_queue_target",slots.pop_front())
        else:
            person.remove_meta("market_queue_target")

# Asking for help is a real server activity, not a new decorative resident.
func _assign_street_aid_spots() -> void:
    if not _current_place in ["MARKET","BAKERY"] or _apartment_active:return
    var spots: Array[Vector3]=[]
    if _current_place=="MARKET":
        spots=[Vector3(-11.0,0,0.0),Vector3(-3.4,0,0.0),Vector3(-11.0,0,-1.5)]
    else:
        spots=[Vector3(-2.7,0,5.7),Vector3(-2.7,0,8.3),Vector3(-1.6,0,9.0)]
    var people := _residents_root.get_children()
    people.sort_custom(func(a,b): return str(a.get_meta("resident_id","")) < str(b.get_meta("resident_id","")))
    for child in people:
        var person := child as Node3D
        if str(person.get_meta("activity",""))=="ASKING_FOR_HELP":
            while not spots.is_empty():
                var at: Vector3=spots.pop_front()
                if not _can_stand(at,WALKER_RADIUS):continue
                if not person.has_meta("pre_aid_anchor"):
                    person.set_meta("pre_aid_anchor",person.get_meta("anchor",person.position))
                if not (_dialog_panel.visible and _dialog_target==str(person.get_meta("resident_id",""))):
                    person.position=at
                    person.set_meta("anchor",at)
                break
        elif person.has_meta("pre_aid_anchor"):
            person.set_meta("anchor",person.get_meta("pre_aid_anchor"))
            person.remove_meta("pre_aid_anchor")

func _assign_caritas_spots() -> void:
    if _current_place!="LIBRARY" or _apartment_active:return
    var spots: Array[Vector3]=[Vector3(7.2,0,7.35),Vector3(8.6,0,6.65),Vector3(6.3,0,6.25)]
    for child in _residents_root.get_children():
        var person := child as Node3D
        var activity := str(person.get_meta("activity",""))
        var at := Vector3(8,0,9.5)
        if activity=="SEEKING_MEAL" and not spots.is_empty():at=spots.pop_front()
        elif activity!="VOLUNTEERING":
            if person.has_meta("pre_caritas_anchor"):
                person.set_meta("anchor",person.get_meta("pre_caritas_anchor"))
                person.remove_meta("pre_caritas_anchor")
            continue
        if not _can_stand(at,WALKER_RADIUS):continue
        if not person.has_meta("pre_caritas_anchor"):person.set_meta("pre_caritas_anchor",person.get_meta("anchor",person.position))
        if not (_dialog_panel.visible and _dialog_target==str(person.get_meta("resident_id",""))):
            person.position=at
            person.set_meta("anchor",at)

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

func _resident_spawn_anchor(index: int) -> Vector3:
    # Preserve the original first row, then distribute additional residents
    # over alternating north/south rows instead of piling up at z=-13.
    var rows := [-4.0,3.0,-9.0,8.0,-13.0,13.0,18.0,0.0]
    var preferred := Vector3((float(index%8)-3.5)*3.0,0,rows[posmod(floori(float(index)/8.0),rows.size())])
    var start := _open_anchor(preferred)
    if _resident_spawn_clear(start):return start
    for row in rows:
        for column in range(8):
            var at := Vector3((float((column+index)%8)-3.5)*3.0,0,row)
            if _resident_spawn_clear(at):return at
    return start

func _resident_spawn_clear(at: Vector3) -> bool:
    if not _can_stand(at,WALKER_RADIUS):return false
    if not is_instance_valid(_residents_root):return true
    for child in _residents_root.get_children():
        var other := child as Node3D
        if other!=null and other.position.distance_squared_to(at)<1.44:return false
    return true

func _make_resident(id: String, index: int, display_name: String) -> Node3D:
    var person := Node3D.new()
    person.name = "Resident_" + id
    person.set_meta("resident_id", id)
    person.set_meta("personality",ResidentPersonality.style(id))
    var anchor := _resident_spawn_anchor(index)
    if _apartment_active:
        anchor = _open_anchor(FamilyApartment.ANCHORS[index % FamilyApartment.ANCHORS.size()])
    person.position = anchor
    person.set_meta("anchor", anchor)
    person.set_meta("phase", float(absi(id.hash()) % 628) / 100.0)
    _residents_root.add_child(person)
    var profile := ResidentAppearance.profile(id)
    person.set_meta("appearance",profile)
    person.scale = Vector3(float(profile["build"]),float(profile["height"]),float(profile["build"]))
    var jacket: Color = profile["jacket"]
    var skin: Color = profile["skin"]
    var hair: Color = profile["hair"]
    var pants: Color = profile["pants"]
    var torso := _cylinder(person,0.23,0.57,jacket,Vector3(0,1.14,0))
    torso.name="Torso"
    torso.scale=Vector3(float(profile["waist"]),1.0,0.72)
    var torso_mesh := torso.mesh as CylinderMesh
    torso_mesh.top_radius=0.245
    torso_mesh.bottom_radius=0.205
    var hips := _sphere(person,0.22,pants,Vector3(0,0.86,0))
    hips.scale=Vector3(1.0,0.46,0.72)
    var shoulders := _cylinder(person,0.255,0.14,jacket,Vector3(0,1.36,0))
    shoulders.name="Shoulders"
    shoulders.scale=Vector3(float(profile["shoulders"]),1.0,0.68)
    var neck := _cylinder(person,0.087,0.17,skin,Vector3(0,1.49,0))
    var neck_mesh := neck.mesh as CylinderMesh
    neck_mesh.top_radius=0.072
    neck_mesh.bottom_radius=0.097
    var head := Node3D.new()
    head.name="Head"
    person.add_child(head)
    var face := _sphere(head,0.20,skin,Vector3(0,1.66,0))
    face.scale=Vector3(float(profile["face_width"]),1.06,0.94)
    var jaw := _sphere(head,0.132,skin,Vector3(0,1.565,-0.025))
    jaw.scale=Vector3(float(profile["face_width"]),0.62,0.88)
    var hair_root := Node3D.new()
    hair_root.name="Hair"
    head.add_child(hair_root)
    var style: int = profile["style"]
    if style == 0:
        _sphere(hair_root,0.195,hair,Vector3(0,1.75,0.04))
    elif style == 1:
        _sphere(hair_root,0.21,hair,Vector3(0,1.76,0.055))
        _sphere(hair_root,0.12,hair,Vector3(0,1.60,0.17))
    elif style == 2:
        _sphere(hair_root,0.17,hair,Vector3(0,1.78,0.025))
    else:
        _sphere(hair_root,0.20,hair,Vector3(0,1.77,0.05))
        _sphere(hair_root,0.12,hair,Vector3(0.11,1.79,0.02))
    match int(profile["hair_detail"]):
        0:
            var fringe := _sphere(hair_root,0.105,hair,Vector3(-0.08,1.77,-0.125))
            fringe.scale=Vector3(1.2,0.42,0.62)
        1:
            for x in [-0.13,0.13]:
                var lock := _sphere(hair_root,0.075,hair,Vector3(x,1.57,0.08))
                lock.scale.y=1.7
        2:
            _sphere(hair_root,0.085,hair,Vector3(0,1.88,0.10))
    match int(profile["hair_extra"]):
        0:
            for x in [-0.12,0.0,0.12]:
                _sphere(hair_root,0.075,hair,Vector3(x,1.84,0.035))
        1:
            var ponytail := _sphere(hair_root,0.085,hair,Vector3(0,1.56,0.20))
            ponytail.scale.y=2.1
        2:
            var swept := _sphere(hair_root,0.12,hair,Vector3(0.06,1.80,-0.09))
            swept.scale=Vector3(1.25,0.55,0.65)
        3:
            for x in [-0.17,0.17]:
                var sideburn := _sphere(hair_root,0.045,hair,Vector3(x,1.65,0.02))
                sideburn.scale.y=1.45
    for side in [-1.0,1.0]:
        var ear := _sphere(head,0.045,skin,Vector3(side*0.18,1.64,0))
        ear.scale=Vector3(0.64,1.25,0.78)
        var inner_ear := _sphere(head,0.021,skin.darkened(0.16),Vector3(side*0.195,1.64,-0.012))
        inner_ear.scale=Vector3(0.45,1.2,0.8)
        var eye := Node3D.new()
        eye.name="EyeLeft" if side<0 else "EyeRight"
        head.add_child(eye)
        eye.position=Vector3(side*0.072,1.69,-0.177)
        _sphere(eye,0.025,Color("#e9e3d8"),Vector3.ZERO)
        _sphere(eye,0.013,profile["eye"],Vector3(0,0,-0.022))
        _sphere(eye,0.006,Color("#202225"),Vector3(0,0,-0.034))
        _box(hair_root,Vector3(0.071,0.014,0.014),hair,Vector3(side*0.072,1.735,-0.187))
        var brow := hair_root.get_child(hair_root.get_child_count()-1) as Node3D
        brow.name="BrowLeft" if side<0 else "BrowRight"
    _sphere(head,float(profile["nose"]),skin,Vector3(0,1.635,-0.193))
    var mouth := Node3D.new()
    mouth.name="Mouth"
    mouth.position=Vector3(0,1.57,-0.184)
    head.add_child(mouth)
    var lip := skin.darkened(0.48)
    _box(mouth,Vector3(0.048,0.012,0.018),lip,Vector3.ZERO)
    mouth.get_child(0).name="Center"
    for side in [-1.0,1.0]:
        _box(mouth,Vector3(0.025,0.012,0.018),lip,Vector3(side*0.034,0,0.005))
        mouth.get_child(mouth.get_child_count()-1).name="Left" if side<0 else "Right"
    var opening := _sphere(mouth,0.025,Color("#36282b"),Vector3(0,-0.004,-0.008))
    opening.name="Opening"
    opening.scale=Vector3(0.95,0.12,0.32)
    opening.hide()
    var casual := Node3D.new()
    casual.name="CasualDetails"
    person.add_child(casual)
    _build_resident_clothes(casual,profile)
    match int(profile["face_detail"]):
        0:
            for x in [-0.115,0.115]:
                for n in range(3):
                    _sphere(head,0.005,skin.darkened(0.30),Vector3(x+float(n-1)*0.012,1.63+float(n%2)*0.008,-0.165))
        1:
            _sphere(head,0.007,skin.darkened(0.55),Vector3(0.10,1.59,-0.163))
        2:
            for x in [-0.19,0.19]:
                _sphere(head,0.018,Color("#d2b16c"),Vector3(x,1.61,-0.018))
    var beard := Node3D.new()
    beard.name="FacialHair"
    head.add_child(beard)
    if int(profile["beard"])==0:
        var chin := _sphere(beard,0.115,hair,Vector3(0,1.535,-0.128))
        chin.scale=Vector3(1.03,0.61,0.55)
    elif int(profile["beard"])==1:
        _box(beard,Vector3(0.11,0.022,0.024),hair,Vector3(0,1.602,-0.186))
    elif int(profile["beard"])==2:
        var short_beard := _sphere(beard,0.075,hair,Vector3(0,1.52,-0.15))
        short_beard.scale=Vector3(1.0,0.66,0.45)
    beard.hide()
    var accessory: int = profile["accessory"]
    if accessory == 0:
        for side in [-1.0,1.0]:
            # Open frames keep the animated eyes visible.
            for y in [-0.033,0.033]:
                _box(head,Vector3(0.105,0.009,0.018),Color("#414b51"),Vector3(side*0.068,1.69+y,-0.225))
            for x in [-0.048,0.048]:
                _box(head,Vector3(0.009,0.06,0.018),Color("#414b51"),Vector3(side*0.068+x,1.69,-0.225))
        _box(head,Vector3(0.045,0.014,0.02),Color("#414b51"),Vector3(0,1.69,-0.214))
    elif accessory == 1:
        var bag := _cylinder(casual,0.19,0.43,jacket.darkened(0.35),Vector3(0,1.14,0.22))
        bag.scale = Vector3(1.0,1.0,0.62)
    elif accessory == 2:
        var cap := _cylinder(head,0.205,0.095,jacket.darkened(0.23),Vector3(0,1.85,0))
        cap.scale.z = 1.02
        _box(head,Vector3(0.32,0.035,0.18),jacket.darkened(0.23),Vector3(0,1.82,-0.19))
    for side in [-1,1]:
        var leg := Node3D.new()
        leg.name = "LegLeft" if side < 0 else "LegRight"
        leg.position = Vector3(side*0.13,0.84,0)
        person.add_child(leg)
        var trouser := _cylinder(leg,0.105,0.36,pants,Vector3(0,-0.18,0))
        trouser.scale.z = 1.08
        var thigh_mesh := trouser.mesh as CylinderMesh
        thigh_mesh.top_radius=0.116
        thigh_mesh.bottom_radius=0.090
        var knee := Node3D.new()
        knee.name = "Knee"
        knee.position = Vector3(0,-0.35,0)
        leg.add_child(knee)
        _sphere(knee,0.092,pants,Vector3.ZERO)
        var calf := _cylinder(knee,0.09,0.35,pants,Vector3(0,-0.175,0))
        var calf_mesh := calf.mesh as CylinderMesh
        calf_mesh.top_radius=0.093
        calf_mesh.bottom_radius=0.060
        var foot := Node3D.new()
        foot.name="Foot"
        knee.add_child(foot)
        foot.position=Vector3(0,-0.35,0)
        var shoe := _sphere(foot,0.15,profile["shoe_color"],Vector3(0,-0.065,-0.055))
        shoe.scale=Vector3(0.72,0.48,1.12)
        _box(foot,Vector3(0.20,0.035,0.31),Color("#665f55"),Vector3(0,-0.122,-0.055))
        for z in [-0.05,-0.09]:
            _box(foot,Vector3(0.10,0.012,0.018),profile["shoe_color"].lightened(0.32),Vector3(0,-0.012,z))
        var arm := Node3D.new()
        arm.name = "ArmLeft" if side < 0 else "ArmRight"
        arm.position = Vector3(side*0.29*float(profile["shoulders"]),1.37,0)
        person.add_child(arm)
        var shoulder_cap := _sphere(arm,0.091,jacket,Vector3(0,-0.035,0))
        shoulder_cap.scale=Vector3(0.95,1.15,0.93)
        var upper := _cylinder(arm,0.080,0.23,jacket,Vector3(0,-0.115,0))
        var upper_mesh := upper.mesh as CylinderMesh
        upper_mesh.top_radius=0.083
        upper_mesh.bottom_radius=0.064
        var elbow := Node3D.new()
        elbow.name="Elbow"
        arm.add_child(elbow)
        elbow.position=Vector3(0,-0.23,0)
        _sphere(elbow,0.067,jacket,Vector3.ZERO)
        var forearm := _cylinder(elbow,0.063,0.20,jacket,Vector3(0,-0.10,0))
        var forearm_mesh := forearm.mesh as CylinderMesh
        forearm_mesh.top_radius=0.067
        forearm_mesh.bottom_radius=0.048
        _cylinder(elbow,0.053,0.040,jacket.darkened(0.18),Vector3(0,-0.205,0))
        var palm := _sphere(elbow,0.062,skin,Vector3(0,-0.256,0))
        palm.name="Palm"
        palm.scale=Vector3(0.72,1.05,0.62)
        var fingers := Node3D.new()
        fingers.name="Fingers"
        elbow.add_child(fingers)
        fingers.position=Vector3(0,-0.29,0)
        for n in range(4):
            var length: float = [0.046,0.057,0.053,0.042][n]
            var digit := _sphere(fingers,0.014,skin,Vector3((float(n)-1.5)*0.022,-length*0.45,-0.002))
            digit.scale.y=length/0.028
        var thumb := _sphere(fingers,0.017,skin,Vector3(-float(side)*0.041,0.025,-0.019))
        thumb.scale.y=1.65
        thumb.rotation.z=float(side)*0.45
        if side<0 and str(person.get_meta("personality",""))=="perfectionist":
            var watch := Node3D.new()
            watch.name="Wristwatch"
            elbow.add_child(watch)
            _cylinder(watch,0.057,0.034,Color("#34383c"),Vector3(0,-0.21,0))
            _box(watch,Vector3(0.065,0.055,0.019),Color("#a6a8ab"),Vector3(0,-0.21,-0.055))
            _box(watch,Vector3(0.048,0.04,0.009),Color("#273d43"),Vector3(0,-0.21,-0.069))
    var hand := person.get_node("ArmRight") as Node3D
    var phone := Node3D.new()
    phone.name = "Phone"
    hand.add_child(phone)
    phone.position = Vector3(0,-0.49,-0.065)
    _box(phone,Vector3(0.075,0.13,0.025),Color("#303638"),Vector3.ZERO)
    _box(phone,Vector3(0.062,0.108,0.008),Color("#8aaeb6"),Vector3(0,0,-0.017))
    phone.hide()
    var cup := _cylinder(hand,0.055,0.095,Color("#eee6d5"),Vector3(0,-0.49,-0.07))
    cup.name = "CoffeeCup"
    cup.hide()
    var name_label := Label3D.new()
    name_label.name = "ResidentName"
    name_label.text = display_name
    name_label.position = Vector3(0,2.1,0)
    name_label.hide()
    person.add_child(name_label)
    var activity_label := Label3D.new()
    activity_label.name="ActivityLabel"
    activity_label.position=Vector3(0,1.94,0)
    activity_label.billboard=BaseMaterial3D.BILLBOARD_ENABLED
    activity_label.font_size=24
    activity_label.pixel_size=0.006
    activity_label.modulate=Color("#f4db9e")
    activity_label.hide()
    person.add_child(activity_label)
    return person

# Updates the existing model when a birthday crosses a visible life stage.
func _build_resident_clothes(root: Node3D,profile: Dictionary) -> void:
    var paint: Color=profile["jacket"]
    var accent: Color=profile["accent"]
    match int(profile["clothing_style"]):
        0: # Zip-up hoodie
            var hood := _sphere(root,0.17,paint.darkened(0.10),Vector3(0,1.43,0.12))
            hood.scale=Vector3(1.0,0.70,0.65)
            _box(root,Vector3(0.025,0.48,0.025),accent,Vector3(0,1.15,-0.185))
            _box(root,Vector3(0.24,0.11,0.035),paint.lightened(0.10),Vector3(0,0.98,-0.18))
        1: # Button shirt
            for x in [-0.08,0.08]:
                var collar := _cylinder(root,0.06,0.025,accent,Vector3(x,1.40,-0.16))
                collar.rotation.x=PI/2.0
            for y in [1.10,1.22,1.34]:
                _sphere(root,0.012,accent,Vector3(0,y,-0.185))
        2: # Knit stripes
            for y in [1.00,1.15,1.30]:
                var band := _cylinder(root,0.235,0.045,accent,Vector3(0,y,0))
                band.scale=Vector3(float(profile["waist"]),1.0,0.76)
        3: # Long coat
            var hem := _cylinder(root,0.245,0.35,paint,Vector3(0,0.81,0))
            hem.scale.z=0.74
            for x in [-0.075,0.075]:
                _box(root,Vector3(0.07,0.30,0.035),paint.lightened(0.18),Vector3(x,1.24,-0.17))
        4: # Casual vest
            var vest := _cylinder(root,0.248,0.49,accent.darkened(0.15),Vector3(0,1.15,0))
            vest.scale.z=0.75
            _box(root,Vector3(0.025,0.44,0.03),paint,Vector3(0,1.15,-0.19))
        5: # Graphic shirt
            _box(root,Vector3(0.19,0.10,0.018),accent,Vector3(0,1.23,-0.185))
            _box(root,Vector3(0.12,0.025,0.018),accent.lightened(0.25),Vector3(0,1.16,-0.185))
        6: # Smart jacket
            for x in [-0.085,0.085]:
                var lapel := _box_node(root,Vector3(0.07,0.29,0.035),paint.darkened(0.25),Vector3(x,1.28,-0.18))
                lapel.rotation.z=x*2.5
            _box(root,Vector3(0.045,0.25,0.03),accent,Vector3(0,1.29,-0.198))

func _box_node(parent: Node3D,size: Vector3,color: Color,at: Vector3) -> Node3D:
    _box(parent,size,color,at)
    return parent.get_child(parent.get_child_count()-1) as Node3D

func _sync_resident_appearance(person: Node3D,stage: String) -> void:
    if str(person.get_meta("appearance_stage",""))==stage:return
    person.set_meta("appearance_stage",stage)
    var profile: Dictionary=person.get_meta("appearance",{})
    person.scale=ResidentAppearance.body_scale(profile,stage)
    var tint := ResidentAppearance.hair_color(profile.get("hair",Color("#594437")),stage)
    for group in ["Hair","FacialHair"]:
        var root := person.get_node_or_null("Head/"+group) as Node3D
        if root==null:continue
        for child in root.get_children():
            if child is MeshInstance3D and child.material_override is StandardMaterial3D:
                child.material_override.albedo_color=tint
        if group=="FacialHair":
            root.visible=stage in ["ADULT","MATURE","SENIOR"]

# Client-only expression parameters, driven by the authoritative mood snapshot.
# Blink timing is stable per resident and speech is bounded to delivered replies.
func _animate_resident_face(person: Node3D,delta: float,now: float,speaking: bool) -> void:
    var mouth := person.get_node_or_null("Head/Mouth") as Node3D
    if mouth==null:return
    var mood := str(person.get_meta("peer_expression",person.get_meta("mood","CALM")))
    var smile := 0.0
    var eye_open := 1.0
    var brow_height := 0.0
    var brow_slant := 0.0
    match mood:
        "HAPPY":smile=0.012;eye_open=0.86;brow_height=0.003
        "ENTHUSIASTIC":smile=0.017;eye_open=1.12;brow_height=0.012
        "TIRED":smile=-0.004;eye_open=0.48;brow_height=-0.006
        "SAD","LONELY":smile=-0.012;eye_open=0.76;brow_slant=-0.30
        "IRRITATED":smile=-0.009;eye_open=0.65;brow_height=-0.010;brow_slant=0.44
        "STRESSED":smile=-0.006;eye_open=1.06;brow_height=0.008;brow_slant=0.24
    var seed := absi(str(person.get_meta("resident_id","")).hash())
    var cycle := 3.2+float(seed%19)*0.11
    var blink_phase := fposmod(now+float(seed%97)*0.073,cycle)
    var blink := 1.0
    if blink_phase<0.18:
        blink=0.08+0.92*absf(blink_phase-0.09)/0.09
    var blend := minf(delta*18.0,1.0)
    for side in [-1.0,1.0]:
        var eye := person.get_node("Head/EyeLeft" if side<0 else "Head/EyeRight") as Node3D
        eye.scale.y=lerpf(eye.scale.y,eye_open*blink,blend)
        var brow := person.get_node("Head/Hair/BrowLeft" if side<0 else "Head/Hair/BrowRight") as Node3D
        brow.position.y=lerpf(brow.position.y,1.735+brow_height,blend)
        brow.rotation.z=lerpf(brow.rotation.z,side*brow_slant,blend)
        var corner := mouth.get_node("Left" if side<0 else "Right") as Node3D
        corner.position.y=lerpf(corner.position.y,smile*0.5,blend)
        corner.rotation.z=lerpf(corner.rotation.z,side*atan2(smile,0.027),blend)
    var yawning := not speaking and mood=="TIRED" and fposmod(now+float(seed%31),19.0)<1.25
    var opening := mouth.get_node("Opening") as Node3D
    var laughing := str(person.get_meta("peer_reaction",""))=="LAUGH"
    opening.visible=speaking or yawning or laughing
    var syllable := 0.18+0.62*absf(sin(now*13.0+float(seed%31))) if speaking else 0.12
    opening.scale.y=lerpf(opening.scale.y,0.95 if yawning else (0.45+0.35*absf(sin(now*9.0)) if laughing else syllable),blend)
    var center := mouth.get_node("Center") as Node3D
    center.visible=not speaking and not yawning and not laughing

func _animate_resident_head(person: Node3D,delta: float,now: float,engaged: bool,walking: bool) -> void:
    var head := person.get_node_or_null("Head") as Node3D
    if head==null:return
    var phase := float(person.get_meta("phase",0.0))
    var mood := str(person.get_meta("mood","CALM"))
    var nod := 0.0
    var turn := 0.0
    if engaged:
        nod=0.045*sin(now*3.0+phase)
        if mood=="IRRITATED":turn=0.12*sin(now*4.0+phase)
    elif not walking:
        turn=0.20*sin(now*0.56+phase)
        if mood=="TIRED":nod=0.10
    else:
        turn=0.08*sin(now*0.72+phase)
    if not engaged and person.has_meta("attention_yaw"):
        turn=lerpf(turn,float(person.get_meta("attention_yaw")),0.72)
    # Rotation is around the neck; existing facial meshes share one pivot.
    # Limit movement when headgear is anchored to a uniform or winter outfit.
    var winter := person.get_node_or_null("WinterClothes") as Node3D
    if (winter!=null and winter.visible) or str(person.get_meta("outfit_visual","")) in ["HIGH_VIS","BAKER_APRON"]:
        nod*=0.25
        turn*=0.25
    head.rotation.x=lerpf(head.rotation.x,nod,minf(delta*6.0,1.0))
    head.rotation.y=lerpf(head.rotation.y,turn,minf(delta*6.0,1.0))
    var pivot := Vector3(0,1.49,0)
    head.position=pivot-head.basis*pivot

func _resident_wave(person: Node3D,delta: float,eligible: bool,nearby: bool) -> bool:
    var remaining := maxf(0.0,float(person.get_meta("wave_remaining",0.0))-delta)
    var was_near := bool(person.get_meta("wave_near",false))
    var cooldown := maxf(0.0,float(person.get_meta("wave_cooldown",0.0))-delta)
    if nearby and not was_near and eligible and cooldown<=0.0:
        remaining=1.6
        cooldown=30.0
    if not eligible or not nearby:remaining=0.0
    person.set_meta("wave_remaining",remaining)
    person.set_meta("wave_cooldown",cooldown)
    person.set_meta("wave_near",nearby)
    return remaining>0.0

func _begin_resident_reply(person: Node3D,reply: String) -> void:
    if person==null or reply.strip_edges().is_empty():return
    # Approximate text-driven mouth motion, not audio or phoneme synchronization.
    person.set_meta("reply_remaining",clampf(float(reply.length())*0.055,0.6,5.0))

func _register_resident_seat(at: Vector3, yaw: float, kind: String) -> void:
    var seats: Array = _scenery.get_meta("resident_seats",[])
    seats.append({"at":at,"yaw":yaw,"kind":kind})
    _scenery.set_meta("resident_seats",seats)

func _assign_resident_seats() -> void:
    # Only residents present in the authoritative snapshot may sit.
    var seats: Array = [] if _apartment_active else _scenery.get_meta("resident_seats",[])
    var used: Dictionary = {}
    var people := _residents_root.get_children()
    people.sort_custom(func(a,b): return str(a.get_meta("resident_id","")) < str(b.get_meta("resident_id","")))
    for child in people:
        var person := child as Node3D
        var activity := str(person.get_meta("activity",""))
        var school := _current_place=="SCHOOL" and not _apartment_active
        var in_class: bool = school and activity=="STUDYING" and not person.get_meta("dojo_member",false)
        var eligible: bool = in_class if school else activity in ["RELAXING","RESTING","FREE_TIME","EATING","DRINKING"]
        var old_index := int(person.get_meta("seat_index",-1))
        var chosen := -1
        if eligible:
            if old_index>=0 and old_index<seats.size() and not used.has(old_index):
                chosen = old_index
            else:
                for i in range(seats.size()):
                    if not used.has(i):
                        chosen = i
                        break
        person.set_meta("seat_index",chosen)
        if chosen>=0:
            used[chosen] = true
            var seat: Dictionary = seats[chosen]
            var at: Vector3 = seat["at"]
            # Seat the hips at chair height, including scaled children.
            person.position = at-Vector3(0,0.84*person.scale.y,0)
            person.rotation.y = float(seat["yaw"])
            person.set_meta("seat_kind",seat["kind"])
        elif in_class:
            # If chairs fill up, place extra pupils in the classroom aisle,
            # not at the generic outdoor spawn points.
            var overflow: Array[Vector3]=[Vector3(-6.0,0,4.0),Vector3(-6.0,0,5.0),Vector3(-6.0,0,9.0),Vector3(-6.0,0,10.0),Vector3(-7.0,0,4.0),Vector3(-7.0,0,5.0),Vector3(-7.0,0,9.0),Vector3(-7.0,0,10.0)]
            for at in overflow:
                if not _can_stand(at,WALKER_RADIUS):continue
                var occupied := false
                for other in people:
                    if other!=person and other.position.distance_to(at)<0.7:
                        occupied=true
                        break
                if occupied:continue
                person.position=at
                person.rotation.y=PI/2.0
                break
            person.set_meta("seat_kind","")
            person.set_meta("school_overflow",true)
        elif old_index>=0 or person.get_meta("school_overflow",false):
            var anchor: Vector3 = person.get_meta("anchor")
            person.position = _open_anchor(anchor)
            person.set_meta("seat_kind","")
            person.remove_meta("school_overflow")

# Advance gait only by distance actually covered, not by the wall clock.
func _resident_attention(person: Node3D) -> void:
    person.remove_meta("attention_yaw")
    var target := _avatar.position
    var offset := target-person.position
    offset.y=0.0
    if offset.length_squared()>16.0 or offset.length_squared()<0.12:
        return
    var local := wrapf(atan2(-offset.x,-offset.z)-person.rotation.y,-PI,PI)
    # Only notice someone in the forward field of view.
    if absf(local)<1.0:
        person.set_meta("attention_yaw",clampf(local,-0.38,0.38))

func _animate_resident_posture(person: Node3D,delta: float,now: float,gait: float,walking: bool,seated: bool) -> void:
    var torso := person.get_node_or_null("Torso") as Node3D
    var shoulders := person.get_node_or_null("Shoulders") as Node3D
    if torso==null or shoulders==null:return
    var profile: Dictionary=person.get_meta("appearance",{})
    var phase := float(person.get_meta("phase",0.0))
    var breathing := 0.009*sin(now*2.1+phase)
    var clothed := not str(person.get_meta("outfit_visual","")).is_empty()
    var winter := person.get_node_or_null("WinterClothes") as Node3D
    if clothed or (winter!=null and winter.visible):breathing*=0.25
    var blend := minf(delta*5.0,1.0)
    torso.scale.z=lerpf(torso.scale.z,0.72+breathing,blend)
    var shift := 0.009*sin(now*0.75+phase) if not seated and not walking else 0.0
    shoulders.position.x=lerpf(shoulders.position.x,shift,blend)
    shoulders.rotation.y=lerpf(shoulders.rotation.y,0.055*sin(gait) if walking else 0.0,blend)
    shoulders.rotation.z=lerpf(shoulders.rotation.z,shift*2.0,blend)
    # Small asymmetric arm offsets; no ground position changes.
    if not seated and not walking:
        torso.scale.x=lerpf(torso.scale.x,float(profile.get("waist",1.0))+breathing*0.35,blend)
    else:
        torso.scale.x=lerpf(torso.scale.x,float(profile.get("waist",1.0)),blend)

func _resident_gait(person: Node3D,distance: float) -> float:
    var phase := float(person.get_meta("gait_phase",person.get_meta("phase",0.0)))
    var step_length := maxf(0.27,0.68*person.scale.y)
    phase=fposmod(phase+maxf(distance,0.0)*PI/step_length,TAU)
    person.set_meta("gait_phase",phase)
    return phase

func _animate_resident_leg(leg: Node3D,delta: float,phase: float,walking: bool,seated: bool) -> void:
    var knee := leg.get_node("Knee") as Node3D
    var foot := knee.get_node("Foot") as Node3D
    var flex := -0.58*maxf(0.0,sin(phase-0.65)) if walking else 0.0
    if seated:flex=-PI/2.0
    knee.rotation.x=lerpf(knee.rotation.x,flex,minf(delta*10.0,1.0))
    var ankle := 0.13*sin(phase+0.5) if walking else 0.0
    foot.rotation.x=lerpf(foot.rotation.x,ankle,minf(delta*10.0,1.0))

# Preserve existing prop paths while making held props follow the elbow joint.
func _animate_resident_elbow(arm: Node3D,delta: float,bend: float,grip: bool) -> void:
    var elbow := arm.get_node("Elbow") as Node3D
    elbow.rotation.x=lerpf(elbow.rotation.x,bend,minf(delta*12.0,1.0))
    var fingers := elbow.get_node("Fingers") as Node3D
    fingers.rotation.x=lerpf(fingers.rotation.x,-0.45 if grip else 0.0,minf(delta*12.0,1.0))
    for child in arm.get_children():
        if not child is Node3D or child==elbow or str(child.name) not in ["Phone","CoffeeCup","WorkProp","ReadingBook","FamilyBook","FamilyToy"]:continue
        if not child.has_meta("rest_transform"):
            child.set_meta("rest_transform",child.transform)
        var rest: Transform3D=child.get_meta("rest_transform")
        var pivot := elbow.position
        child.transform=Transform3D(elbow.basis*rest.basis,pivot+elbow.basis*(rest.origin-pivot))

# Presentation-only habits: no invented transactions, needs or social outcomes.
func _idle_gesture_choice(personality: String,mood: String,stage: String,index: int) -> String:
    var choices: Array[String]=["ADJUST_COLLAR","LOOK_HANDS","SCRATCH_HEAD"]
    match mood:
        "TIRED":choices=["STRETCH","RUB_NECK"]
        "STRESSED","IRRITATED":choices=["TAP_FOOT","RUB_NECK","SCRATCH_HEAD"]
        "HAPPY","ENTHUSIASTIC":
            choices=["ADJUST_COLLAR","STRETCH","HAIR_FIX"]
    if mood in ["CALM","HAPPY","ENTHUSIASTIC"]:
        match personality:
            "perfectionist":choices.append("CHECK_WATCH")
            "relaxed":choices.append("STRETCH")
            "quiet":choices.append("LOOK_HANDS")
            "joker","talkative":choices.append("HAIR_FIX")
    if stage in ["PRESCHOOL","CHILD"]:
        choices=["SCRATCH_HEAD","LOOK_HANDS","HAIR_FIX"]
    return choices[posmod(index,choices.size())]

func _tick_idle_gesture(person: Node3D,delta: float,eligible: bool) -> String:
    var seed := absi(str(person.get_meta("resident_id","")).hash())
    var wait := float(person.get_meta("idle_wait",5.0+float(seed%15)))
    var left := float(person.get_meta("idle_left",0.0))
    var gesture := str(person.get_meta("idle_gesture",""))
    if not eligible:
        if not gesture.is_empty():wait=8.0+float(seed%8)
        gesture=""
        left=0.0
    elif left>0.0:
        left=maxf(0.0,left-delta)
        if left<=0.0:gesture=""
    else:
        wait-=delta
        if wait<=0.0:
            var serial := int(person.get_meta("idle_serial",0))
            gesture=_idle_gesture_choice(str(person.get_meta("personality","quiet")),str(person.get_meta("mood","CALM")),str(person.get_meta("appearance_stage","ADULT")),seed+serial)
            person.set_meta("idle_serial",serial+1)
            left=2.4 if gesture=="STRETCH" else 1.8
            wait=12.0+float((seed+serial*7)%19)
    person.set_meta("idle_wait",wait)
    person.set_meta("idle_left",left)
    person.set_meta("idle_gesture",gesture)
    return gesture

# Shoulder pitch, shoulder roll and elbow pitch in radians.
func _idle_arm_pose(gesture: String,right: bool,now: float) -> Vector3:
    match gesture:
        "SCRATCH_HEAD":
            if right:return Vector3(1.50,0.55,1.55+0.12*sin(now*12.0))
        "HAIR_FIX":
            if right:return Vector3(1.65,0.42,1.40+0.10*sin(now*5.0))
        "RUB_NECK":
            if right:return Vector3(1.30,0.60,1.70)
        "CHECK_WATCH":
            if not right:return Vector3(0.72,-0.35,0.85)
        "LOOK_HANDS":
            return Vector3(0.45,0.16 if right else -0.16,0.78)
        "ADJUST_COLLAR":
            if right:return Vector3(0.85,-0.22,1.22)
        "STRETCH":
            return Vector3(2.40,0.50 if right else -0.50,0.25)
    return Vector3.ZERO

# A cue describes a real recent server interaction, never a new relationship.
# Keep visual encounters bounded and do not replay a cue on every snapshot.
var _peer_seen: Dictionary={}
var _peer_active: Dictionary={}

func _peer_available(person: Node3D) -> bool:
    return is_instance_valid(person) and str(person.get_meta("activity","")) in ["WALKING","WANDERING","STROLLING","FREE_TIME","RELAXING","RESTING"] and int(person.get_meta("seat_index",-1))<0 and not person.has_meta("market_queue_target") and not (_dialog_panel.visible and str(person.get_meta("resident_id",""))==_dialog_target)

# A brief visual flourish for a recorded conversation, not an extra social event.
func _peer_reaction_phase(kind: String,left: float) -> String:
    match kind:
        "LAUGH":
            if left<=5.5 and left>=3.5:return "LAUGH"
        "ARGUMENT":return "ARGUMENT"
        "RECONCILE":
            return "PEACE_GESTURE" if left<2.5 else "RECONCILE"
    return "CHAT"

func _tick_peer_encounters(delta: float) -> void:
    if not is_instance_valid(_residents_root):return
    for child in _residents_root.get_children():
        child.remove_meta("peer_target")
        child.remove_meta("peer_face")
        child.remove_meta("peer_speaking")
        child.remove_meta("peer_tone")
        child.remove_meta("peer_reaction")
        child.remove_meta("peer_expression")
    var valid: Dictionary={}
    for cue in _last_snapshot.get("peerEncounters",[]):
        if not cue is Dictionary:continue
        var a_id := str(cue.get("firstId",""))
        var b_id := str(cue.get("secondId",""))
        if a_id.is_empty() or b_id.is_empty() or a_id==b_id:continue
        var key := a_id+"|"+b_id+"|"+str(cue.get("tick",-1))
        valid[key]=true
        var a := _residents_root.get_node_or_null("Resident_"+a_id) as Node3D
        var b := _residents_root.get_node_or_null("Resident_"+b_id) as Node3D
        if not _peer_available(a) or not _peer_available(b):
            if _peer_active.has(key):_peer_active.erase(key)
            continue
        var gap := a.position.distance_to(b.position)
        if not _peer_seen.has(key) and gap<=4.0 and gap>0.05:
            _peer_seen[key]=true
            _peer_active[key]=9.0
        if not _peer_active.has(key):continue
        var left := float(_peer_active[key])-delta
        if left<=0.0 or gap>5.0:
            _peer_active.erase(key)
            continue
        _peer_active[key]=left
        var tone := str(cue.get("tone","CASUAL"))
        var direction := (b.position-a.position).normalized()
        if gap>1.35:
            a.set_meta("peer_target",b.position-direction*1.28)
            b.set_meta("peer_target",a.position+direction*1.28)
        else:
            a.set_meta("peer_face",b.position)
            b.set_meta("peer_face",a.position)
            a.set_meta("peer_speaking",fposmod(left,3.0)<1.5)
            b.set_meta("peer_speaking",fposmod(left,3.0)>=1.5)
            a.set_meta("peer_tone",tone)
            b.set_meta("peer_tone",tone)
            var reaction := _peer_reaction_phase(str(cue.get("reaction","CHAT")),left)
            a.set_meta("peer_reaction",reaction)
            b.set_meta("peer_reaction",reaction)
            if reaction=="LAUGH":
                a.set_meta("peer_expression","ENTHUSIASTIC")
                b.set_meta("peer_expression","ENTHUSIASTIC")
            elif reaction=="ARGUMENT":
                a.set_meta("peer_expression","IRRITATED")
                b.set_meta("peer_expression","STRESSED")
            elif reaction=="RECONCILE" or reaction=="PEACE_GESTURE":
                a.set_meta("peer_expression","HAPPY")
                b.set_meta("peer_expression","HAPPY")
    for key in _peer_active.keys():
        if not valid.has(key):_peer_active.erase(key)
    for key in _peer_seen.keys():
        if not valid.has(key):_peer_seen.erase(key)

# Mutual server-selected pairs only. Keep the faster person from leaving a child behind.
func _companion_available(person: Node3D) -> bool:
    return _peer_available(person) and not person.has_meta("peer_face") and not person.has_meta("peer_target")

func _tick_walking_companions() -> void:
    if not is_instance_valid(_residents_root):return
    for child in _residents_root.get_children():
        child.remove_meta("companion_target")
        child.remove_meta("companion_wait")
        child.remove_meta("companion_pace")
    for child in _residents_root.get_children():
        var a := child as Node3D
        var id := str(a.get_meta("resident_id",""))
        var other := str(a.get_meta("walking_companion",""))
        if other.is_empty() or id>=other:continue
        var b := _residents_root.get_node_or_null("Resident_"+other) as Node3D
        if not _companion_available(a) or not _companion_available(b):continue
        if str(b.get_meta("walking_companion",""))!=id:continue
        if a.position.distance_to(b.position)>5.0:continue
        # Adult leads a child, otherwise use stable ID order.
        var leader := a
        var follower := b
        if a.scale.y<b.scale.y*0.78:
            leader=b
            follower=a
        var gap := leader.position.distance_to(follower.position)
        var phase := float(leader.get_meta("phase",0.0))
        var anchor: Vector3=leader.get_meta("anchor",leader.position)
        var target := anchor+Vector3(sin(phase)*1.1,0,cos(phase)*0.8)
        var direction := target-leader.position
        direction.y=0.0
        if direction.length_squared()<0.01:direction=-leader.basis.z
        direction=direction.normalized()
        var side := direction.cross(Vector3.UP).normalized()
        var alongside := leader.position+side*0.88-direction*0.25
        if not _can_stand(alongside,WALKER_RADIUS):
            alongside=leader.position-side*0.88-direction*0.25
        if not _can_stand(alongside,WALKER_RADIUS):
            alongside=leader.position-direction*0.95
        if not _can_stand(alongside,WALKER_RADIUS):continue
        var speed := minf(ResidentAppearance.pace_factor(str(leader.get_meta("appearance_stage","ADULT"))),ResidentAppearance.pace_factor(str(follower.get_meta("appearance_stage","ADULT"))))
        leader.set_meta("companion_pace",speed)
        follower.set_meta("companion_pace",speed)
        leader.set_meta("companion_wait",gap>1.65)
        leader.set_meta("companion_target",target)
        follower.set_meta("companion_target",alongside)
        follower.set_meta("companion_wait",follower.position.distance_to(alongside)<0.10)

func _animate_residents(delta: float) -> void:
    if _residents_root == null:
        return
    _tick_peer_encounters(delta)
    _tick_walking_companions()
    for child in _residents_root.get_children():
        var person := child as Node3D
        var activity := str(person.get_meta("activity", ""))
        var patrol := activity=="WORKING" and str(person.get_meta("outfit_visual",""))=="POLICE"
        var walking := patrol or activity in ["WALKING", "WANDERING", "STROLLING", "FREE_TIME", "SHOPPING", "JOB_SEARCH"]
        var phase := float(person.get_meta("phase", 0.0))
        var appearance: Dictionary = person.get_meta("appearance",{})
        var pace := float(appearance.get("pace",1.0))*ResidentAppearance.pace_factor(str(person.get_meta("appearance_stage","ADULT")))
        var stride := float(appearance.get("stride",0.42))
        var seated := int(person.get_meta("seat_index",-1)) >= 0
        var talking := _dialog_panel.visible and _dialog_end_remaining<=0.0 and str(person.get_meta("resident_id","")) == _dialog_target
        var queued := person.has_meta("market_queue_target")
        var queue_target: Vector3 = person.get_meta("market_queue_target",person.position)
        if queued and person.position.distance_to(queue_target)<0.12:
            walking=false
            if not talking:person.rotation.y=lerp_angle(person.rotation.y,PI,minf(delta*6.0,1.0))
        var peer_chat := person.has_meta("peer_face")
        var peer_approach := person.has_meta("peer_target")
        var companion := person.has_meta("companion_target")
        if companion:
            pace=minf(pace,float(person.get_meta("companion_pace",pace)))
            walking=not bool(person.get_meta("companion_wait",false))
        if peer_approach:walking=true
        if peer_chat:
            walking=false
            var facing: Vector3=person.get_meta("peer_face")-person.position
            if facing.length_squared()>0.001:
                person.rotation.y=lerp_angle(person.rotation.y,atan2(-facing.x,-facing.z),minf(delta*6.0,1.0))
        if talking or seated or bool(person.get_meta("hosted_guest",false)):
            walking = false
        if talking and not seated:
            var toward := _avatar.position-person.position
            if toward.length_squared()>0.001:
                person.rotation.y = lerp_angle(person.rotation.y,atan2(-toward.x,-toward.z),minf(delta*6.0,1.0))
        var walked := 0.0
        if companion and bool(person.get_meta("companion_wait",false)):
            phase+=delta*0.4*pace
            person.set_meta("phase",phase)
        if walking:
            phase += delta * 0.65 * pace
            person.set_meta("phase", phase)
            var anchor: Vector3 = person.get_meta("anchor")
            var target: Vector3 = person.get_meta("peer_target") if peer_approach else (person.get_meta("companion_target") if companion else (queue_target if queued else anchor + Vector3(sin(phase)* (3.3 if patrol else 1.1),0,cos(phase)* (2.7 if patrol else 0.8))))
            var motion := target-person.position
            if motion.length_squared() > 0.0001:
                person.rotation.y = atan2(-motion.x,-motion.z)
            var next := person.position.move_toward(target,delta*1.5*pace)
            var old := person.position
            person.position = _move_among_people(old,next-old,WALKER_RADIUS,person)
            walked=person.position.distance_to(old)
            if walked>0.001:
                var actual := person.position-old
                person.rotation.y=atan2(-actual.x,-actual.z)
            walking = walked > 0.001
        var umbrella := _sync_resident_weather(person,talking,seated)
        var gait := _resident_gait(person,walked)
        var swing := sin(gait)*stride if walking else 0.0
        var now := float(Time.get_ticks_msec())*0.001
        var reply_remaining := maxf(0.0,float(person.get_meta("reply_remaining",0.0))-delta)
        person.set_meta("reply_remaining",reply_remaining)
        var joke := _current_place=="OFFICE" and str(person.get_meta("resident_id",""))==_office_joker_id and _office_joke_hold>0.0
        var peer_speaking := bool(person.get_meta("peer_speaking",false))
        var peer_reaction := str(person.get_meta("peer_reaction",""))
        _animate_resident_face(person,delta,now,(talking and reply_remaining>0.0) or joke or peer_speaking)
        _resident_attention(person)
        _animate_resident_head(person,delta,now,talking or peer_chat,walking)
        _animate_resident_posture(person,delta,now,gait,walking,seated)
        var casual := person.get_node_or_null("CasualDetails") as Node3D
        var winter := person.get_node_or_null("WinterClothes") as Node3D
        if casual!=null:
            casual.visible=str(person.get_meta("outfit_visual","")).is_empty() and (winter==null or not winter.visible)
        var gesture_time := now+phase
        var coffee := seated and str(person.get_meta("seat_kind",""))=="cafe" and not talking
        var phone := not peer_chat and not seated and not talking and not walking and activity in ["RELAXING","RESTING","FREE_TIME"] and sin(gesture_time*0.45)>0.45
        var familiar := int(person.get_meta("conversations",0))>0 and str(person.get_meta("relationship","STRANGER"))!="STRANGER"
        var friendly := str(person.get_meta("mood","CALM")) in ["CALM","HAPPY","ENTHUSIASTIC"]
        var wave := _resident_wave(person,delta,familiar and friendly and not peer_chat and not seated and not walking and not talking and not phone and activity!="WORKING" and not umbrella,person.position.distance_to(_avatar.position)<3.3)
        if wave:
            var to_visitor := _avatar.position-person.position
            if to_visitor.length_squared()>0.001:
                person.rotation.y=lerp_angle(person.rotation.y,atan2(-to_visitor.x,-to_visitor.z),minf(delta*5.0,1.0))
        var idle_ok := not companion and not peer_chat and not walking and not seated and not talking and not phone and not coffee and not umbrella and not wave and not joke and not queued and activity in ["RELAXING","RESTING","FREE_TIME"]
        var idle_gesture := _tick_idle_gesture(person,delta,idle_ok)
        var guarded := idle_ok and idle_gesture.is_empty() and str(person.get_meta("mood","")) in ["IRRITATED","STRESSED"]
        if idle_gesture in ["CHECK_WATCH","LOOK_HANDS"]:
            var head := person.get_node("Head") as Node3D
            head.rotation.x=lerpf(head.rotation.x,0.24,minf(delta*7.0,1.0))
            var pivot := Vector3(0,1.49,0)
            head.position=pivot-head.basis*pivot
        person.get_node("ArmRight/Phone").visible = phone
        person.get_node("ArmRight/CoffeeCup").visible = coffee
        var work_prop := person.get_node_or_null("ArmRight/WorkProp") as Node3D
        if work_prop!=null:work_prop.visible=activity=="WORKING" and not talking and not walking
        for limb in ["LegLeft","ArmRight","LegRight","ArmLeft"]:
            var part := person.get_node(limb) as Node3D
            var angle := swing if limb in ["LegLeft","ArmRight"] else -swing
            if limb in ["LegLeft","LegRight"]:
                _animate_resident_leg(part,delta,gait+(0.0 if limb=="LegLeft" else PI),walking,seated)
                if seated:
                    angle = PI/2.0
                elif idle_gesture=="TAP_FOOT" and limb=="LegRight":
                    var foot := part.get_node("Knee/Foot") as Node3D
                    foot.rotation.x=0.12*maxf(0.0,sin(now*12.0))
            var idle_pose := Vector3.ZERO
            if limb in ["ArmLeft","ArmRight"]:
                idle_pose=_idle_arm_pose(idle_gesture,limb=="ArmRight",now)
            if idle_pose!=Vector3.ZERO:
                angle=idle_pose.x
            elif wave and limb=="ArmRight":
                angle=-2.25+sin(gesture_time*11.0)*0.22
            elif guarded and limb in ["ArmLeft","ArmRight"]:
                angle=-0.95
            elif umbrella and limb=="ArmLeft":
                angle=0.85
            elif peer_reaction=="LAUGH" and limb=="ArmRight":
                angle=0.65+sin(gesture_time*8.0)*0.10
            elif peer_reaction=="PEACE_GESTURE" and limb=="ArmRight":
                angle=0.85
            elif peer_reaction=="ARGUMENT" and peer_speaking and limb=="ArmRight":
                angle=0.55+sin(gesture_time*6.0)*0.38
            elif peer_speaking and limb=="ArmRight":
                var emphasis := 0.48 if str(person.get_meta("peer_tone",""))=="TENSE" else 0.22
                angle=0.35+sin(gesture_time*4.0)*emphasis
            elif talking and limb=="ArmRight":
                angle=0.30+sin(gesture_time*3.3)*0.28
            elif coffee and limb=="ArmRight":
                angle=0.85+0.50*maxf(0.0,sin(gesture_time*1.4))
            elif phone and limb=="ArmRight":
                angle=1.02
            elif activity=="KUMITE" and limb in ["ArmLeft","ArmRight"]:
                angle=-0.7
            elif not talking and not walking and activity=="WORKING" and limb in ["ArmRight","ArmLeft"] and work_prop!=null:
                var kind := str(person.get_meta("work_visual",""))
                if kind=="PICK":
                    angle=0.9+sin(gesture_time*3.0)*0.8 if limb=="ArmRight" else 0.55
                elif kind=="WRENCH":
                    angle=0.65+sin(gesture_time*4.0)*0.25 if limb=="ArmRight" else 0.35
                else:
                    angle=0.8+sin(gesture_time*1.7)*0.18 if limb=="ArmRight" else 0.25
            elif not walking and activity in ["WORKING","STUDYING"] and limb in ["ArmRight","ArmLeft"]:
                angle=-0.22+sin(float(Time.get_ticks_msec())*0.003+phase)*0.12
            elif not walking and activity!="SLEEPING" and limb=="ArmRight":
                var style := str(person.get_meta("personality","quiet"))
                if style in ["talkative","joker"] and str(person.get_meta("mood","CALM")) in ["CALM","HAPPY","ENTHUSIASTIC"]:
                    angle=sin(float(Time.get_ticks_msec())*0.003+phase)*0.16
                elif style=="relaxed":
                    angle=-0.10
            part.rotation.x = lerpf(part.rotation.x,angle,minf(delta*10.0,1.0))
            if limb in ["ArmLeft","ArmRight"]:
                var roll := (-0.42 if limb=="ArmRight" else 0.42) if guarded else (0.30 if wave and limb=="ArmRight" else 0.0)
                var shoulder_sway := 0.035*sin(gait+(0.0 if limb=="ArmLeft" else PI)) if walking else 0.0
                part.rotation.z=lerpf(part.rotation.z,idle_pose.y if idle_pose!=Vector3.ZERO else roll+shoulder_sway,minf(delta*8.0,1.0))
                var grip: bool = limb=="ArmRight" and (phone or coffee or (work_prop!=null and work_prop.visible))
                var elbow_bend := -0.12-0.18*maxf(0.0,sin(gait+(PI if limb=="ArmLeft" else 0.0))) if walking else 0.0
                if grip:elbow_bend=-0.20
                elif wave and limb=="ArmRight":elbow_bend=-0.36
                elif guarded:elbow_bend=-0.55
                if limb=="ArmRight" and peer_reaction=="PEACE_GESTURE":elbow_bend=0.40
                elif limb=="ArmRight" and peer_reaction=="LAUGH":elbow_bend=0.65
                if idle_pose!=Vector3.ZERO:elbow_bend=idle_pose.z
                _animate_resident_elbow(part,delta,elbow_bend,grip)

func _building(_title: String, wall_color: Color) -> void:
    _add_obstacle(Vector3(-7.0, 0.0, 7.5), Vector2(11.0, 7.0))
    _box(_scenery, Vector3(11.0, 4.5, 7.0), wall_color,
            Vector3(-7.0, 2.25, 7.5))
    _box(_scenery, Vector3(12.0, 0.7, 8.0), Color(0.27, 0.30, 0.33),
            Vector3(-7.0, 4.85, 7.5))
    _box(_scenery, Vector3(2.1, 3.0, 0.1), Color(0.20, 0.28, 0.32),
            Vector3(-7.0, 1.5, 3.95))
    var title := str({"Market stalls":"Mercato","Public library":"Biblioteca comunale","Mechanical workshop":"Officina meccanica","Parts factory":"Fabbrica","Neighborhood":"Quartiere"}.get(_title,_title))
    _physical_sign(_scenery,title,Vector3(-7,3.9,3.81),PI,3.5)
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
    # In historic districts use restrained physical wayfinding, not a bright game portal.
    if _scenery.name in ["PublicScene_CENTRO","PublicScene_PORTICI"]:
        var iron := Color("#56574f")
        for offset in [-1.6,1.6]:
            var at := GATE_POSITION+Vector3(offset,0,0)
            _add_obstacle(at,Vector2(0.4,0.4))
            _cylinder(_scenery,0.10,1.05,iron,at+Vector3(0,0.525,0))
            _sphere(_scenery,0.12,iron,at+Vector3(0,1.08,0))
        _box(_scenery,Vector3(0.09,2.35,0.09),iron,GATE_POSITION+Vector3(1.6,1.175,0))
        _physical_sign(_scenery,tr("TRAVEL · E"),GATE_POSITION+Vector3(1.6,2.4,0),0,1.7)
        _physical_sign(_scenery,tr("TRAVEL · E"),GATE_POSITION+Vector3(1.6,2.4,-0.10),PI,1.7)
        if _scenery.name=="PublicScene_CENTRO":
            _add_obstacle(KIOSK_POSITION,Vector2(2.4,1.6))
            _box(_scenery,Vector3(2.4,1.4,1.6),Color("#727c69"),KIOSK_POSITION+Vector3(0,0.7,0),"wood")
            _box(_scenery,Vector3(2.5,0.14,1.7),Color("#b3a38a"),KIOSK_POSITION+Vector3(0,1.43,0))
            _historic_roof(_scenery,KIOSK_POSITION+Vector3(0,1.54,0),2.65,1.85,0.35)
            _physical_sign(_scenery,tr("SERVICES · F"),KIOSK_POSITION+Vector3(0,1.04,0.85),0,1.9)
        return
    # A yellow gate is a map transition — actual travel is authorized by Java.
    for offset in [-1.6, 1.6]:
        _add_obstacle(GATE_POSITION + Vector3(offset, 0.0, 0.0), Vector2(0.4, 0.4))
    if _scenery.name not in ["PublicScene_LOGISTICS_DEPOT","PublicScene_PORTICI"]:
        _add_obstacle(KIOSK_POSITION, Vector2(2.4, 1.6))
    _box(_scenery, Vector3(0.4, 3.1, 0.4), Color(0.94, 0.69, 0.12),
            GATE_POSITION + Vector3(-1.6, 1.55, 0.0))
    _box(_scenery, Vector3(0.4, 3.1, 0.4), Color(0.94, 0.69, 0.12),
            GATE_POSITION + Vector3(1.6, 1.55, 0.0))
    _box(_scenery, Vector3(3.6, 0.38, 0.45), Color(0.94, 0.69, 0.12),
            GATE_POSITION + Vector3(0.0, 3.2, 0.0))
    _sign(_scenery,tr("TRAVEL · E"),GATE_POSITION+Vector3(0,3.65,0))
    if _scenery.name not in ["PublicScene_LOGISTICS_DEPOT","PublicScene_PORTICI"]:
        # Blue kiosk: only offers actions physically available in this district.
        _sign(_scenery,tr("SERVICES · F"),KIOSK_POSITION+Vector3(0,2.25,0))
        _box(_scenery, Vector3(2.4, 1.4, 1.6), Color(0.13, 0.39, 0.69),
                KIOSK_POSITION + Vector3(0.0, 0.7, 0.0))
        _box(_scenery, Vector3(3.2, 0.22, 2.2), Color(0.88, 0.89, 0.89),
                KIOSK_POSITION + Vector3(0.0, 1.6, 0.0))

func _tree(at: Vector3) -> void:
    _add_obstacle(at, Vector2(0.45, 0.45))
    var tree_seed := absi(roundi(at.x*43.0+at.z*79.0))
    var rng := RandomNumberGenerator.new()
    rng.seed = tree_seed
    var trunk_height := rng.randf_range(2.0,2.6)
    var trunk := _cylinder(_scenery,0.22,trunk_height,Color("#70513b"),at+Vector3(0,trunk_height*0.5,0))
    TownVisuals.surface(trunk.material_override as StandardMaterial3D,_surface_textures["wood"],0.75)
    var crown := Node3D.new()
    crown.name="TreeCrown"
    crown.position=at+Vector3(0,trunk_height+0.55,0)
    _scenery.add_child(crown)
    for i in range(8):
        var angle := float(i)*TAU/8.0
        var foliage := MeshInstance3D.new()
        foliage.mesh = _leaf_mesh
        foliage.material_override = _leaf_materials[i%4]
        var spread := rng.randf_range(0.55,0.95)
        foliage.position = Vector3(cos(angle)*spread,rng.randf_range(-0.1,0.5),sin(angle)*spread)
        foliage.scale = Vector3(rng.randf_range(0.77,1.12),rng.randf_range(0.88,1.35),rng.randf_range(0.77,1.12))
        crown.add_child(foliage)
        if i%2 == 1:
            _register_graphics_detail(foliage)
    var top := MeshInstance3D.new()
    top.mesh = _leaf_mesh
    top.material_override = _leaf_materials[2]
    top.position = Vector3(0,1.0,0)
    top.scale = Vector3(1.08,1.14,1.08)
    crown.add_child(top)

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
        where: Vector3) -> MeshInstance3D:
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
    return node

func _create_interface() -> void:
    var canvas := CanvasLayer.new()
    add_child(canvas)
    _interface_theme = preload("res://scripts/TownInterface.gd").build()

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
    panel.custom_minimum_size = Vector2(350.0, 0.0)
    panel.theme = _interface_theme
    canvas.add_child(panel)
    var column := VBoxContainer.new()
    panel.add_child(column)

    _heading = Label.new()
    _heading.text = "Living Alibi | public park"
    preload("res://scripts/TownInterface.gd").title(_heading)
    column.add_child(_heading)
    _status = Label.new()
    _status.text = "A visitor with 50 EUR and no assigned profession"
    _status.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
    column.add_child(_status)
    var primary_actions := GridContainer.new()
    primary_actions.columns = 2
    column.add_child(primary_actions)
    primary_actions.add_child(_button(tr("Talk (T)"),_open_dialog))
    primary_actions.add_child(_button(tr("Actions (F)"),_toggle_actions))
    primary_actions.add_child(_button(tr("Map (M)"),_toggle_map))
    primary_actions.add_child(_button(tr("Phone (H)"),_toggle_phone))
    for primary in primary_actions.get_children():
        primary.size_flags_horizontal = Control.SIZE_EXPAND_FILL
    var details_toggle := CheckButton.new()
    details_toggle.text = tr("More / settings")
    details_toggle.toggled.connect(_toggle_hud_details)
    column.add_child(details_toggle)
    _hud_scroll = ScrollContainer.new()
    _hud_scroll.custom_minimum_size = Vector2(320,270)
    _hud_scroll.horizontal_scroll_mode = ScrollContainer.SCROLL_MODE_DISABLED
    _hud_scroll.hide()
    column.add_child(_hud_scroll)
    _hud_details = VBoxContainer.new()
    _hud_details.size_flags_horizontal = Control.SIZE_EXPAND_FILL
    _hud_details.visible = false
    _hud_scroll.add_child(_hud_details)
    var guide_toggle := CheckButton.new()
    guide_toggle.text = "First steps / navigation"
    guide_toggle.button_pressed = true
    guide_toggle.toggled.connect(_set_guide_enabled)
    _hud_details.add_child(guide_toggle)
    _guide_label=Label.new()
    _guide_label.autowrap_mode=TextServer.AUTOWRAP_WORD_SMART
    _guide_label.custom_minimum_size=Vector2(320,0)
    preload("res://scripts/TownInterface.gd").secondary(_guide_label)
    _guide_label.text=tr("First steps: connecting to the world...")
    column.add_child(_guide_label)
    _weather = Label.new()
    _weather.text = "Connecting to the local Java world..."
    _weather.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
    _hud_details.add_child(_weather)
    _needs = Label.new()
    _needs.text = "Jobs, study and travel use the real game world."
    _needs.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
    _hud_details.add_child(_needs)
    _bag_label = Label.new()
    _bag_label.text = "Bag: empty"
    _hud_details.add_child(_bag_label)
    _aid_label = Label.new()
    _aid_label.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
    _aid_label.visible = false
    _hud_details.add_child(_aid_label)
    _housing_label = Label.new()
    _housing_label.text = "Housing: no room"
    _hud_details.add_child(_housing_label)
    _car_label = Label.new()
    _car_label.text = "Car: none"
    _hud_details.add_child(_car_label)
    var bag_row := GridContainer.new()
    bag_row.columns = 2
    _hud_details.add_child(bag_row)
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
    _message.custom_minimum_size = Vector2(320.0, 0.0)
    preload("res://scripts/TownInterface.gd").secondary(_message)
    column.add_child(_message)

    var buttons := GridContainer.new()
    buttons.columns = 2
    _hud_details.add_child(buttons)
    buttons.add_child(_button("Refresh", _refresh))
    buttons.add_child(_button("Jobs", _find_jobs))
    buttons.add_child(_button("Industry", _view_industry))
    buttons.add_child(_button("Business", _view_business))
    buttons.add_child(_button("Wait 60m", _wait_one_hour))
    _transport_button = _button("Transport: walking (B)",_toggle_transport)
    _hud_details.add_child(_transport_button)
    _transport_button.disabled = true
    _parking_button = _button("Bicycle rack (P)",_toggle_parking)
    _hud_details.add_child(_parking_button)
    _parking_button.disabled = true
    var map_button := Button.new()
    map_button.text = "Town map (M)"
    map_button.pressed.connect(_toggle_map)
    _hud_details.add_child(map_button)
    _language_selector = OptionButton.new()
    _language_selector.add_item("English",0)
    _language_selector.add_item("Italiano",1)
    _language_selector.select(1 if TranslationServer.get_locale()=="it" else 0)
    _language_selector.item_selected.connect(_on_language_selected)
    _hud_details.add_child(_language_selector)
    var clock_row := HBoxContainer.new()
    _hud_details.add_child(clock_row)
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
    var graphics_row := HBoxContainer.new()
    _hud_details.add_child(graphics_row)
    var graphics_label := Label.new()
    graphics_label.text = "Graphics quality"
    graphics_row.add_child(graphics_label)
    var quality := OptionButton.new()
    for caption in ["Low","Medium","High","Ultra"]:
        quality.add_item(caption)
    quality.select(_graphics_quality)
    quality.item_selected.connect(_set_graphics_quality)
    graphics_row.add_child(quality)
    var atmosphere_row := HBoxContainer.new()
    _hud_details.add_child(atmosphere_row)
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
    var sound_toggle := CheckButton.new()
    sound_toggle.text = "Town sound"
    sound_toggle.button_pressed = true
    sound_toggle.toggled.connect(_set_town_audio)
    _hud_details.add_child(sound_toggle)
    var music_row := HBoxContainer.new()
    _hud_details.add_child(music_row)
    var music_label := Label.new()
    music_label.text = "Background music"
    music_row.add_child(music_label)
    _music_value = Label.new()
    music_row.add_child(_music_value)
    _music_value.text = "%d%%" % roundi(_music.level)
    var music_slider := HSlider.new()
    music_slider.min_value = 0
    music_slider.max_value = 100
    music_slider.step = 5
    music_slider.value = _music.level
    music_slider.custom_minimum_size = Vector2(180,24)
    music_slider.value_changed.connect(_set_music_level)
    _hud_details.add_child(music_slider)
    var head_bob_toggle := CheckButton.new()
    head_bob_toggle.text = tr("Camera motion")
    head_bob_toggle.button_pressed = _head_bob_enabled
    head_bob_toggle.toggled.connect(_set_head_bob)
    _hud_details.add_child(head_bob_toggle)
    var hands_toggle := CheckButton.new()
    hands_toggle.text = "First-person hands"
    hands_toggle.button_pressed = true
    hands_toggle.toggled.connect(_set_hands_enabled)
    _hud_details.add_child(hands_toggle)
    _held_label = Label.new()
    _hud_details.add_child(_held_label)
    _hud_details.add_child(_button("Open phone (H)",_toggle_phone))
    _refresh_held_item()
    _hint = Label.new()
    _hint.autowrap_mode=TextServer.AUTOWRAP_WORD_SMART
    preload("res://scripts/TownInterface.gd").secondary(_hint)
    _hint.text = tr("WASD: walk · Shift: run · Ctrl: crouch · Space: jump · Esc: cursor.")
    column.add_child(_hint)
    _jobs = Label.new()
    _jobs.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
    _jobs.text = ""
    _hud_details.add_child(_jobs)

    _map_panel = PanelContainer.new()
    _map_panel.position = Vector2(15, 15)
    _map_panel.visible = false
    _map_panel.theme = _interface_theme
    canvas.add_child(_map_panel)
    var map_column := VBoxContainer.new()
    _map_panel.add_child(map_column)
    var map_header := Label.new()
    map_header.text = "Living Alibi — public town map (schematic)"
    map_column.add_child(map_header)
    _town_map = preload("res://scripts/TownMap.gd").new()
    map_column.add_child(_town_map)
    var overview_toggle := CheckButton.new()
    overview_toggle.text = tr("Whole city overview")
    overview_toggle.toggled.connect(_town_map.set_overview)
    map_column.add_child(overview_toggle)
    var close_map := Button.new()
    close_map.text = "Close map (M)"
    close_map.pressed.connect(_toggle_map)
    map_column.add_child(close_map)

    _travel_panel = PanelContainer.new()
    _travel_panel.position = Vector2(480.0, 15.0)
    _travel_panel.custom_minimum_size = Vector2(460.0, 0.0)
    _travel_panel.visible = false
    _travel_panel.theme = _interface_theme
    canvas.add_child(_travel_panel)
    _dock_right(_travel_panel,490.0)
    var travel_col := VBoxContainer.new()
    _travel_panel.add_child(travel_col)
    var travel_label := Label.new()
    travel_label.text = "Choose a destination (Java decides travel time):"
    travel_col.add_child(travel_label)
    _destination_filter=LineEdit.new()
    _destination_filter.placeholder_text=tr("Search destinations")
    _destination_filter.text_changed.connect(func(_value):_render_destinations())
    travel_col.add_child(_destination_filter)
    _travel_choices = GridContainer.new()
    _travel_choices.columns = 2
    var travel_scroll := ScrollContainer.new()
    travel_scroll.custom_minimum_size=Vector2(440,minf(510.0,maxf(220.0,get_viewport().get_visible_rect().size.y-100.0)))
    travel_scroll.horizontal_scroll_mode=ScrollContainer.SCROLL_MODE_DISABLED
    travel_col.add_child(travel_scroll)
    _travel_choices.size_flags_horizontal=Control.SIZE_EXPAND_FILL
    travel_scroll.add_child(_travel_choices)

    _dialog_panel = PanelContainer.new()
    _dialog_panel.position = Vector2(480,15)
    _dialog_panel.custom_minimum_size = Vector2(410,0)
    _dialog_panel.visible = false
    _dialog_panel.theme = _interface_theme
    canvas.add_child(_dialog_panel)
    _dock_right(_dialog_panel,450.0)
    var dialog_scroll := ScrollContainer.new()
    dialog_scroll.custom_minimum_size = Vector2(410,minf(470.0,maxf(250.0,get_viewport().get_visible_rect().size.y-54.0)))
    dialog_scroll.horizontal_scroll_mode = ScrollContainer.SCROLL_MODE_DISABLED
    _dialog_panel.add_child(dialog_scroll)
    var dialog_col := VBoxContainer.new()
    dialog_col.size_flags_horizontal = Control.SIZE_EXPAND_FILL
    dialog_scroll.add_child(dialog_col)
    _dialog_title = Label.new()
    preload("res://scripts/TownInterface.gd").title(_dialog_title)
    dialog_col.add_child(_dialog_title)
    _dialog_attitude = Label.new()
    _dialog_attitude.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
    preload("res://scripts/TownInterface.gd").secondary(_dialog_attitude)
    dialog_col.add_child(_dialog_attitude)
    _dialog_style = Label.new()
    _dialog_style.autowrap_mode=TextServer.AUTOWRAP_WORD_SMART
    dialog_col.add_child(_dialog_style)
    _dialog_reply = Label.new()
    _dialog_reply.custom_minimum_size = Vector2(370,0)
    _dialog_reply.add_theme_color_override("font_color",Color("#f2cc87"))
    _dialog_reply.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
    dialog_col.add_child(_dialog_reply)
    _dialog_history = RichTextLabel.new()
    _dialog_history.bbcode_enabled = false
    _dialog_history.custom_minimum_size = Vector2(370,140)
    _dialog_history.scroll_following = true
    dialog_col.add_child(_dialog_history)
    var spoken_label := Label.new()
    spoken_label.text = "Parlo / I speak"
    dialog_col.add_child(spoken_label)
    _dialog_language = OptionButton.new()
    _dialog_language.add_item("Italiano")
    _dialog_language.add_item("English")
    _dialog_language.select(0)
    dialog_col.add_child(_dialog_language)
    _dialog_input = LineEdit.new()
    _dialog_input.placeholder_text = tr("Write to this person...")
    _dialog_input.max_length = 1000
    _dialog_input.text_submitted.connect(_send_dialog)
    dialog_col.add_child(_dialog_input)
    dialog_col.add_child(_button(tr("Send message (Enter)"),_send_typed_dialog))
    var quick := GridContainer.new()
    quick.columns = 2
    dialog_col.add_child(quick)
    quick.add_child(_button(tr("Say hello"),_send_dialog.bind("Hello")))
    quick.add_child(_button(tr("Ask their name"),_send_dialog.bind("What is your name?")))
    quick.add_child(_button(tr("How are you?"),_send_dialog.bind("How are you?")))
    var more_toggle := CheckButton.new()
    more_toggle.text = tr("More questions")
    more_toggle.toggled.connect(_toggle_dialog_more)
    dialog_col.add_child(more_toggle)
    _dialog_more_scroll = ScrollContainer.new()
    _dialog_more_scroll.custom_minimum_size = Vector2(370,170)
    _dialog_more_scroll.horizontal_scroll_mode = ScrollContainer.SCROLL_MODE_DISABLED
    _dialog_more_scroll.visible = false
    dialog_col.add_child(_dialog_more_scroll)
    var more_choices := VBoxContainer.new()
    _dialog_more_scroll.add_child(more_choices)
    more_choices.add_child(_button(tr("Ask their address"),_send_dialog.bind("Where do you live?")))
    more_choices.add_child(_button(tr("Ask about occupation"),_send_dialog.bind("What do you do?")))
    more_choices.add_child(_button(tr("Offer support"),_send_dialog.bind("I'm here for you.")))
    more_choices.add_child(_button(tr("Ask about shared memories"),_send_dialog.bind("What do you remember about me?")))
    more_choices.add_child(_button(tr("Offer bread"),_send_dialog.bind("Offer bread")))
    more_choices.add_child(_button(tr("Give 2 EUR"),_send_dialog.bind("Give 2 EUR")))
    more_choices.add_child(_button(tr("Decline request"),_send_dialog.bind("Decline request")))
    more_choices.add_child(_button(tr("Offer water"),_send_dialog.bind("Offer water")))
    more_choices.add_child(_button(tr("Ask for bread"),_send_dialog.bind("Ask for bread")))
    more_choices.add_child(_button(tr("Ask for water"),_send_dialog.bind("Ask for water")))
    more_choices.add_child(_button(tr("Ask for a 10 EUR loan"),_send_dialog.bind("Ask for a 10 EUR loan")))
    more_choices.add_child(_button(tr("Repay personal loan"),_send_dialog.bind("Repay personal loan")))
    more_choices.add_child(_button(tr("Allow sharing my name"),_send_dialog.bind("You can tell people my name.")))
    more_choices.add_child(_button(tr("Ask for visitor news"),_send_dialog.bind("Any news about visitors?")))
    more_choices.add_child(_button(tr("Press interview"),_send_dialog.bind("Any public news for the newspaper?")))
    more_choices.add_child(_button(tr("Apologize"),_send_dialog.bind("I'm sorry.")))
    more_choices.add_child(_button(tr("Leave me alone!"),_send_dialog.bind("Leave me alone!")))
    var voice_toggle := CheckButton.new()
    voice_toggle.text = tr("Read replies aloud")
    voice_toggle.button_pressed = true
    voice_toggle.toggled.connect(_set_dialog_voice)
    dialog_col.add_child(voice_toggle)
    var close := Button.new()
    close.text = tr("Close conversation")
    close.pressed.connect(_close_dialog)
    dialog_col.add_child(close)

    _action_panel = PanelContainer.new()
    _action_panel.position = Vector2(480.0, 15.0)
    _action_panel.custom_minimum_size = Vector2(430.0, 0.0)
    _action_panel.visible = false
    _action_panel.theme = _interface_theme
    canvas.add_child(_action_panel)
    _dock_right(_action_panel,460.0)
    var action_scroll := ScrollContainer.new()
    action_scroll.custom_minimum_size = Vector2(420,420)
    action_scroll.horizontal_scroll_mode = ScrollContainer.SCROLL_MODE_DISABLED
    _action_panel.add_child(action_scroll)
    _action_choices = VBoxContainer.new()
    _action_choices.size_flags_horizontal = Control.SIZE_EXPAND_FILL
    action_scroll.add_child(_action_choices)
    _action_title = Label.new()
    _action_title.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
    preload("res://scripts/TownInterface.gd").title(_action_title)
    _action_choices.add_child(_action_title)
    _phone = TownPhone.new()
    _phone.theme = _interface_theme
    _phone.hide()
    canvas.add_child(_phone)
    _phone.set_anchors_and_offsets_preset(Control.PRESET_CENTER_RIGHT)
    _phone.offset_left = -355.0
    _phone.offset_right = -15.0
    _phone.offset_top = -235.0
    _phone.offset_bottom = 235.0
    _phone.close_requested.connect(_close_phone)
    _phone.jobs_requested.connect(_phone_request_jobs)
    _phone.map_requested.connect(_phone_open_map)
    _phone.alarm_rang.connect(_on_phone_alarm)
    _phone.sms_received.connect(_on_phone_alarm)
    _phone.sms_reply_requested.connect(_reply_phone_sms)
    _phone.call_received.connect(_on_phone_alarm)
    _phone.call_action_requested.connect(_reply_phone_call)
    _phone_alert = Label.new()
    _phone_alert.position = Vector2(480,12)
    _phone_alert.add_theme_font_size_override("font_size",24)
    _phone_alert.add_theme_color_override("font_color",Color("#ffe4a0"))
    _phone_alert.add_theme_color_override("font_shadow_color",Color.BLACK)
    _phone_alert.add_theme_constant_override("shadow_offset_x",2)
    _phone_alert.add_theme_constant_override("shadow_offset_y",2)
    _phone_alert.hide()
    canvas.add_child(_phone_alert)
    _stairwell_hint = Label.new()
    _stairwell_hint.position = Vector2(20,20)
    _stairwell_hint.add_theme_color_override("font_color",Color.WHITE)
    _stairwell_hint.add_theme_color_override("font_shadow_color",Color.BLACK)
    _stairwell_hint.add_theme_constant_override("shadow_offset_x",2)
    _stairwell_hint.add_theme_constant_override("shadow_offset_y",2)
    _stairwell_hint.hide()
    canvas.add_child(_stairwell_hint)
    _intercom = PanelContainer.new()
    _intercom.position = Vector2(480,15)
    _intercom.custom_minimum_size = Vector2(350,0)
    _intercom.theme = _interface_theme
    canvas.add_child(_intercom)
    _dock_right(_intercom,375.0)
    var intercom_col := VBoxContainer.new()
    _intercom.add_child(intercom_col)
    _intercom_title = Label.new()
    intercom_col.add_child(_intercom_title)
    _intercom_contacts = OptionButton.new()
    intercom_col.add_child(_intercom_contacts)
    _intercom_reply = Label.new()
    _intercom_reply.custom_minimum_size = Vector2(340,0)
    _intercom_reply.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
    intercom_col.add_child(_intercom_reply)
    intercom_col.add_child(_button("Ring bell",_intercom_action.bind("ring")))
    intercom_col.add_child(_button("Chat at entrance",_intercom_action.bind("visit")))
    intercom_col.add_child(_button("Ask to come inside",_intercom_action.bind("invite")))
    intercom_col.add_child(_button("Leave entrance",_close_intercom))
    _intercom.hide()

func _dock_right(panel: Control, width: float) -> void:
    panel.set_anchors_and_offsets_preset(Control.PRESET_TOP_RIGHT)
    panel.grow_horizontal = Control.GROW_DIRECTION_BEGIN
    panel.offset_left = -width-15.0
    panel.offset_right = -15.0
    panel.offset_top = 15.0

func _set_music_level(value: float) -> void:
    _music.set_level(value)
    _music_value.text = "%d%%" % roundi(value)

func _on_language_selected(index: int) -> void:
    GameLanguage.select_locale("it" if index==1 else "en")
    _refresh_localized_hud()
    _refresh_held_item()
    _phone.refresh()
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

func _dialog_candidates() -> Array:
    if _guest_active and is_instance_valid(_guest):
        var people: Array = []
        if is_instance_valid(_guest_host):
            people.append(_guest_host)
        var family := _guest.get_node_or_null("GuestFamily") as Node3D
        if family!=null:
            people.append_array(family.get_children())
        return people
    if _stairwell_active and is_instance_valid(_stairwell):
        var neighbors := _stairwell.get_node_or_null("ArrivingNeighbors")
        return neighbors.get_children() if neighbors!=null else []
    return _residents_root.get_children() if is_instance_valid(_residents_root) else []

func _dialog_person(id: String) -> Node3D:
    for child in _dialog_candidates():
        if str(child.get_meta("resident_id",""))==id:
            return child as Node3D
    return null

func _nearest_resident() -> String:
    if not is_instance_valid(_residents_root):
        return ""
    var nearest := ""
    var best := 2.5
    for child in _dialog_candidates():
        var person := child as Node3D
        if person == null or str(person.get_meta("activity","")) in ["SLEEPING","TRAVELING"]:
            continue
        var distance := _avatar.position.distance_to(person.position)
        if distance < best:
            best = distance
            nearest = str(person.get_meta("resident_id",""))
    return nearest

func _looked_at_resident() -> String:
    if not is_instance_valid(_residents_root) or not is_instance_valid(_camera):
        return ""
    var center := get_viewport().get_visible_rect().size*0.5
    var selected := ""
    var best := 90.0
    for child in _dialog_candidates():
        var person := child as Node3D
        if person == null or str(person.get_meta("activity","")) in ["SLEEPING","TRAVELING"]:
            continue
        if _avatar.position.distance_to(person.position) >= 2.5:
            continue
        var head := person.global_position + Vector3(0,1.35*person.scale.y,0)
        if _camera.is_position_behind(head):
            continue
        var distance := _camera.unproject_position(head).distance_to(center)
        if distance < best:
            best = distance
            selected = str(person.get_meta("resident_id",""))
    return selected

func _resident_caption(person: Node3D) -> String:
    var label := str(person.get_meta("display_name","Unknown resident"))
    if label=="Unknown resident" and int(person.get_meta("conversations",0))>0:
        return tr("Familiar face")
    return tr(label)

func _open_dialog(preferred_id: String = "") -> void:
    # Reading the town or fetching location choices must not hide the chat.
    # Only sending requires a free API channel.
    if not _connected or _dialog_pending:
        return
    var id := preferred_id if not preferred_id.is_empty() else _nearest_resident()
    if id.is_empty():
        _message.text = tr("Approach a resident to talk.")
        return
    var person := _dialog_person(id)
    if person == null or _avatar.position.distance_to(person.position) >= 2.5:
        _message.text = tr("Approach a resident to talk.")
        return
    _dialog_target = id
    person.set_meta("reply_remaining",0.0)
    _travel_panel.visible = false
    _action_panel.visible = false
    _map_panel.visible = false
    _dialog_title.text = _resident_caption(person)
    _dialog_attitude.text = tr("Attitude: %s") % tr(str(person.get_meta("relationship","STRANGER")).capitalize())
    _render_dialog_mood(person)
    var known := str(person.get_meta("known_name",""))
    _dialog_reply.text = tr("They know you as %s") % known if not known.is_empty() else (
            "You have spoken before." if int(person.get_meta("conversations",0)) > 0 else "You have not met.")
    if str(person.get_meta("activity",""))=="ASKING_FOR_HELP":
        _dialog_reply.text=_reply_translate("Excuse me, could you spare a little change?")
    _dialog_input.clear()
    _dialog_history.clear()
    _dialog_history.append_text(_dialog_reply.text+"\n")
    _dialog_started_at = _dialog_world_minute()
    _dialog_end_remaining = 0.0
    _dialog_farewell = ""
    _dialog_panel.visible = true
    Input.set_mouse_mode(Input.MOUSE_MODE_VISIBLE)
    _dialog_input.grab_focus()

func _render_dialog_mood(person: Node3D) -> void:
    var mood := str(person.get_meta("mood","CALM"))
    _dialog_style.text = tr("Seems: %s") % tr(mood.capitalize())
    var debt := int(person.get_meta("personal_debt",0))
    var due := int(person.get_meta("personal_debt_due",0))
    if debt>0:
        _dialog_style.text += "\n"+(tr("Personal debt: %s EUR · due day %s") % [debt,due])
        if int(_last_snapshot.get("day",1))>due:
            _dialog_style.text += " · "+tr("Overdue")

# The spoken language is independent of TranslationServer's interface locale.
func _reply_translate(source: String) -> String:
    if _api.dialogue_language == "it":
        return str(GameLanguage.IT.get(source,source))
    if _api.dialogue_language == "en":
        return source
    return tr(source)

func _localized_resident_reply(message: String) -> String:
    var person := _dialog_person(_dialog_target)
    if person==null:return message
    var separator := message.find(": ")
    if separator<0:return _reply_translate(message)
    var spoken := message.substr(separator+2)
    if _api.dialogue_rendered:
        return spoken
    if spoken.begins_with("I live at ") and spoken.ends_with("."):
        return _reply_translate("I live at %s.") % spoken.substr(10,spoken.length()-11)
    if spoken.begins_with("My name is ") and spoken.ends_with("."):
        return _reply_translate("My name is %s.") % spoken.substr(11,spoken.length()-12)
    if spoken.begins_with("I work as ") and spoken.ends_with("."):
        return _reply_translate("I work as %s.") % _reply_translate(spoken.substr(10,spoken.length()-11))
    if spoken.begins_with("My workplace is ") and spoken.ends_with("."):
        return _reply_translate("My workplace is %s.") % _reply_translate(spoken.substr(16,spoken.length()-17))
    if spoken.begins_with("Look for ") and spoken.ends_with(" on your town map."):
        return _reply_translate("Look for %s on your town map.") % _reply_translate(spoken.substr(9,spoken.length()-27))
    if spoken.begins_with("Nice to meet you, ") and spoken.ends_with("."):
        return _reply_translate("Nice to meet you, %s.") % spoken.substr(18,spoken.length()-19)
    if spoken.begins_with("It's ") and spoken.ends_with(".") and spoken.contains(":"):
        return _reply_translate("It's %s.") % spoken.substr(5,spoken.length()-6)
    if spoken.begins_with("Hello, ") and spoken.ends_with(". Nice to meet someone around here."):
        return _reply_translate("Hello, %s. Nice to meet someone around here.") % _reply_translate(spoken.substr(7,spoken.length()-42))
    if spoken.begins_with("Good to see you again, ") and spoken.ends_with("."):
        return _reply_translate("Good to see you again, %s.") % _reply_translate(spoken.substr(23,spoken.length()-24))
    return _reply_translate(spoken)

func _dialog_world_minute() -> int:
    return (int(_last_snapshot.get("day",1))-1)*1440+int(_last_snapshot.get("minute",0))

func _dialog_patience(person: Node3D) -> int:
    if person.has_meta("conversation_minutes"):
        return int(person.get_meta("conversation_minutes"))
    var activity := str(person.get_meta("activity",""))
    if activity in ["SLEEPING","TRAVELING","SICK_LEAVE","RESTING_WHILE_SICK"]:
        return 1
    if activity in ["WORKING","STUDYING","JOB_SEARCH","SHOPPING"]:
        return 4
    if str(person.get_meta("relationship",""))=="FRIENDLY":
        return 18
    return 10

func _finish_dialog_autonomously(farewell: String) -> void:
    _close_dialog()
    _message.text = farewell
    _phone.add_notification(farewell)

func _tick_dialog_autonomy(delta: float) -> void:
    if not _dialog_panel.visible or _dialog_pending:return
    if _dialog_end_remaining>0.0:
        _dialog_end_remaining=maxf(0.0,_dialog_end_remaining-delta)
        if _dialog_end_remaining<=0.0:
            _finish_dialog_autonomously(_dialog_farewell)
        return
    var person := _dialog_person(_dialog_target)
    if person==null:
        _finish_dialog_autonomously(tr("They had to leave."))
        return
    if _dialog_world_minute()-_dialog_started_at>=_dialog_patience(person):
        var reason := str(person.get_meta("conversation_reason","PERSONAL"))
        var farewell := tr("They have other things to do. Talk later.")
        if reason=="SHIFT":farewell=tr("They have to get back to their shift.")
        elif reason=="NEEDS":farewell=tr("They need to take care of themselves.")
        _finish_dialog_autonomously(farewell)

func _close_dialog() -> void:
    var was_open := _dialog_panel.visible
    _dialog_panel.visible = false
    _dialog_target = ""
    _dialog_end_remaining = 0.0
    _dialog_input.release_focus()
    if was_open:
        DisplayServer.tts_stop()
        if not (_travel_panel.visible or _action_panel.visible or _map_panel.visible or _phone.visible or _intercom.visible):
            Input.set_mouse_mode(Input.MOUSE_MODE_CAPTURED)

func _toggle_dialog_more(enabled: bool) -> void:
    _dialog_more_scroll.visible = enabled

func _set_dialog_voice(enabled: bool) -> void:
    _spoken_dialog_enabled = enabled
    if not enabled:
        DisplayServer.tts_stop()

func _speak_resident_reply(reply: String) -> void:
    if not _spoken_dialog_enabled or reply.is_empty():
        return
    var language: String = _api.dialogue_language
    if language not in ["it","en"]:
        language = "it" if TranslationServer.get_locale().begins_with("it") else "en"
    var locale: String = _api.dialogue_locale
    var voices := PackedStringArray()
    if not locale.is_empty() and locale != "<null>":
        # Prefer the actual region when the platform has a matching voice.
        for voice in DisplayServer.tts_get_voices():
            if str(voice.get("language","")).replace("_","-").to_lower()==locale.to_lower():
                voices.append(str(voice.get("id","")))
    if voices.is_empty():
        voices = DisplayServer.tts_get_voices_for_language(language)
    if voices.is_empty():
        _message.text = tr("No system speech voice is available. Text chat still works.")
        return
    # Stable voice and subtle pitch/rate variation for each resident.
    var voice_seed := absi(_dialog_target.hash())
    var pitch := 0.90+float(voice_seed%21)/100.0
    var rate := 0.92+float((voice_seed/21)%17)/100.0
    DisplayServer.tts_speak(reply,str(voices[voice_seed%voices.size()]),75,pitch,rate,0,true)

func _send_typed_dialog() -> void:
    _send_dialog(_dialog_input.text)

func _send_dialog(message: String) -> void:
    if not _dialog_panel.visible or _dialog_pending or _dialog_end_remaining>0.0 or not _connected:
        return
    if _api.is_busy():
        _dialog_reply.text = tr("The world is updating. Try sending again.")
        return
    var person := _dialog_person(_dialog_target)
    if person == null or _avatar.position.distance_to(person.position) >= 2.5:
        _close_dialog()
        return
    if message.strip_edges().is_empty():
        return
    _dialog_pending = true
    _dialog_history.append_text(tr("You")+": "+message.strip_edges()+"\n")
    _api.speak_to(_dialog_target,message,"en" if _dialog_language.selected==1 else "it",_stairwell_active and not _guest_active)

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
        var destination_name := str(place.get("label",id))
        var query := _destination_filter.text.strip_edges().to_lower()
        if not query.is_empty() and not destination_name.to_lower().contains(query) and not id.to_lower().contains(query):continue
        var caption := "%s (%sm)" % [destination_name, str(minutes)]
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
    if _rebirth_active():
        if not _connected:
            return
        _close_dialog()
        _travel_panel.visible = false
        _action_panel.visible = not _action_panel.visible
        if _action_panel.visible:
            _render_actions()
        Input.set_mouse_mode(Input.MOUSE_MODE_VISIBLE if _action_panel.visible else Input.MOUSE_MODE_CAPTURED)
        return
    if (not _near_kiosk() and not _near_home_station() and not _near_car() and not _near_truck()) or not _connected:
        _message.text = "Approach a local kiosk or your rented bed."
        return
    _close_dialog()
    _travel_panel.visible = false
    _action_panel.visible = not _action_panel.visible
    if _action_panel.visible:
        Input.set_mouse_mode(Input.MOUSE_MODE_VISIBLE)
        _render_actions()
        if _current_place == "WORKSHOP" and not _near_sport_counter() and not _api.is_busy():
            _api.view_business()
        elif _current_place in ["OFFICE","NORTH_RESIDENCES"] and not _api.is_busy():
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

func _render_barber() -> void:
    _action_title.text="Barbiere del Borgo"
    var barber: Dictionary=_last_snapshot.get("barber",{})
    var info := Label.new()
    info.autowrap_mode=TextServer.AUTOWRAP_WORD_SMART
    info.text=tr("Open") if barber.get("open",false) else tr("Closed")
    info.text+=" · 09:00–19:00\n"+tr("Hair")+": "+tr(str(barber.get("hair","NATURAL")).capitalize())+" · "+tr("Beard")+": "+tr(str(barber.get("beard","NATURAL")).capitalize())
    _action_choices.add_child(info)
    var money := int(_last_snapshot.get("player",{}).get("money",0))
    var minute := int(_last_snapshot.get("minute",0))
    for service in barber.get("services",[]):
        var duration := int(service.get("minutes",0))
        var price := int(service.get("price",0))
        var btn := _button("%s · %s EUR · %s min" % [tr(str(service.get("label",""))),price,duration],_garden_action.bind("barber-"+str(service.get("id",""))))
        btn.disabled=not bool(barber.get("open",false)) or money<price or minute+duration>1140
        _action_choices.add_child(btn)

func _render_actions() -> void:
    _replace_choices(_action_choices, true)
    if _custody_active:return
    if _near_charity() and not _rebirth_active():
        _action_title.text=tr("Caritas · Community kitchen")
        var charity: Dictionary=_last_snapshot.get("caritas",{})
        var summary := Label.new()
        summary.autowrap_mode=TextServer.AUTOWRAP_WORD_SMART
        summary.text=tr("Open 12:00–14:00 when a volunteer is present.")
        summary.text+="\n"+tr("Bread %s · Water %s · Parcels served %s") % [charity.get("bread",0),charity.get("water",0),charity.get("served",0)]
        _action_choices.add_child(summary)
        var receive := _button(tr("Receive free food and water"),_garden_action.bind("caritas-receive"))
        receive.disabled=not bool(charity.get("canReceive",false))
        _action_choices.add_child(receive)
        for item in ["bread","water"]:
            var donation := _button(tr("Donate bread") if item=="bread" else tr("Donate water"),_garden_action.bind("caritas-donate-"+item))
            donation.disabled=not bool(charity.get("canDonateBread" if item=="bread" else "canDonateWater",false))
            _action_choices.add_child(donation)
        return
    if _near_kiosk() and not _near_sport_counter() and not _near_car() and not _rebirth_active():
        _render_bus()
    var justice: Dictionary = _last_snapshot.get("player",{}).get("justice",{})
    var permit: Dictionary = _last_snapshot.get("player",{}).get("weaponLicense",{})
    if _near_sport_counter() and not _rebirth_active() and _current_place=="MARKET":
        _render_armeria(permit)
        return
    if _near_sport_counter() and not _rebirth_active() and _current_place=="WORKSHOP":
        var practice_info := Label.new()
        practice_info.text=tr("Training counter · 08:00–18:00")+"\n"+tr("Practice shots: %s · Sessions: %s") % [permit.get("practiceShots",0),permit.get("practiceSessions",0)]
        _action_choices.add_child(practice_info)
        var practice_btn := _button(tr("Practice shot"),_weapon_license_action.bind("weapon-practice"))
        var practice_minute := int(_last_snapshot.get("minute",0))
        practice_btn.disabled = not bool(permit.get("ownsSportWeapon",false)) or not bool(permit.get("valid",false)) or practice_minute<480 or practice_minute+2>1080 or int(permit.get("rounds",0))<=0
        practice_btn.set_meta("sold_out",practice_btn.disabled)
        _action_choices.add_child(practice_btn)
        var refill_btn := _button(tr("Replenish training supply · 3 EUR"),_weapon_license_action.bind("weapon-refill"))
        refill_btn.disabled=not bool(permit.get("canRefill",false))
        refill_btn.set_meta("sold_out",refill_btn.disabled)
        _action_choices.add_child(refill_btn)
        if not bool(permit.get("ownsSportWeapon",false)):
            var training_hint := Label.new()
            training_hint.text=tr("A registered sporting weapon is required.")
            _action_choices.add_child(training_hint)
        return
    if _current_place=="OFFICE" and not _rebirth_active():
        var permit_info := Label.new()
        permit_info.text = tr("Weapon license: active")+" · "+tr("%s days left") % permit.get("daysRemaining",0) if bool(permit.get("valid",false)) else tr("Weapon license: inactive")
        _action_choices.add_child(permit_info)
        if not bool(permit.get("valid",false)):
            var request_permit := _button(tr("Apply for weapon license · 10 EUR"),_weapon_license_action.bind("weapon-license-apply"))
            request_permit.disabled = not bool(permit.get("canApply",false))
            request_permit.set_meta("sold_out",request_permit.disabled)
            _action_choices.add_child(request_permit)
    if _current_place=="OFFICE" and int(justice.get("fineOutstanding",0))>0 and not _rebirth_active():
        _action_choices.add_child(_button(tr("Pay outstanding fine"),_pay_justice_fine))
    if _near_barber() and not _rebirth_active():
        _render_barber()
        return
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
    var contract: Dictionary = _last_snapshot.get("employmentContract",{})
    if bool(contract.get("active",false)) and _near_kiosk() and not _near_car() and not _near_truck():
        var contract_label := Label.new()
        contract_label.autowrap_mode=TextServer.AUTOWRAP_WORD_SMART
        contract_label.text=tr("Employment contract: %s") % tr(str(contract.get("status","PERMANENT")))
        if int(contract.get("endDay",0))>0:
            contract_label.text+="\n"+tr("Expires on day %s") % contract.get("endDay",0)
        contract_label.text+="\n"+tr("Paid shifts: %s · Total earnings: %s EUR") % [contract.get("paidShifts",0),contract.get("totalPaid",0)]
        if bool(contract.get("fiscalPayroll",false)):
            contract_label.text+="\n"+tr("Payslip · gross %s / net %s EUR") % [contract.get("lastPay",0),contract.get("lastNet",0)]
            contract_label.text+="\n"+tr("Tax %s · Pension %s · TFR %s EUR") % [contract.get("lastTax",0),contract.get("lastPension",0),contract.get("lastTfr",0)]
            contract_label.text+="\n"+tr("TFR reserve %s · Pension contributions %s EUR") % [contract.get("tfrBalance",0),contract.get("pensionTotal",0)]
        else:
            contract_label.text+="\n"+tr("Register a fiscal code to activate payroll deductions.")
        _action_choices.add_child(contract_label)
        if bool(contract.get("canResign",false)):
            _action_choices.add_child(_button(tr("Resign from job"),_garden_action.bind("employment-resign")))
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
            var name := TownLayout.label_for(str(id)) if TownLayout.outer(str(id)) else str(id).capitalize()
            _action_choices.add_child(_button("Drive to %s · 3m / 1 fuel" % name,_drive_to.bind(str(id))))
        if TownLayout.service_hub(_current_place):
            _action_choices.add_child(_button(tr("Refuel · up to 3 units / 2 EUR each"),_car_action.bind("refuel")))
            _action_choices.add_child(_button(tr("Service car · 15 EUR"),_car_action.bind("repair")))
        return
    var logistics: Dictionary = _last_snapshot.get("logistics",{})
    if _current_place in ["FARM","QUARRY","FACTORY"] and bool(logistics.get("yours",false)) and bool(logistics.get("truck",false)) and not bool(_last_snapshot.get("freight",{}).get("active",false)):
        var relocation := " · relocation 8 EUR + fuel" if str(logistics.get("truckPlace",""))!=_current_place else ""
        _action_choices.add_child(_button("Company freight contract · 40 EUR"+relocation,_freight_action.bind("load-company")))
    match _current_place:
        "CLINIC":
            var registry: Dictionary = _last_snapshot.get("registry",{})
            var consultation := Label.new()
            consultation.text=tr("Medical consultation · %s EUR · 30 min") % registry.get("consultationPrice",15)
            _action_choices.add_child(consultation)
            var visit := _button(tr("Request consultation"),_garden_action.bind("registry-consult"))
            visit.disabled=not bool(registry.get("canConsult",false))
            _action_choices.add_child(visit)
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
            var food_id := _centro_food_counter()
            if food_id.is_empty():
                _action_choices.add_child(_button("Job noticeboard",_find_jobs))
            else:
                var minute := int(_last_snapshot.get("minute",0))
                var shop_open := minute>=660 and minute+2<=1320
                var status := Label.new()
                status.text=("Open" if shop_open else "Closed")+" · 11:00–22:00"
                _action_choices.add_child(status)
                for item in _last_snapshot.get("shop",[]):
                    if str(item.get("id",""))!=food_id:continue
                    var buy := _button("Buy %s · %s EUR (%s left)" % [item.get("label",food_id),item.get("price",0),item.get("stock",0)],_buy_product.bind(food_id))
                    buy.disabled=not shop_open or int(item.get("stock",0))<=0 or int(player.get("money",0))<int(item.get("price",0))
                    _action_choices.add_child(buy)
                var food_bag: Dictionary=player.get("bag",{})
                if int(food_bag.get(food_id,0))>0:
                    _action_choices.add_child(_button("Eat "+food_id,_consume_product.bind(food_id)))
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
                var phone_data = _last_snapshot.get("phone")
                if _owns_house() and phone_data is Dictionary:
                    for meeting in phone_data.get("meetings",[]):
                        if bool(meeting.get("guestPresent",false)) and not bool(meeting.get("mealServed",false)) and int(_last_snapshot.get("minute",0))<1145:
                            _action_choices.add_child(_button(tr("Cook pasta together · 25m"),_garden_action.bind("host-cook:"+str(meeting.get("residentId","")))))
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
        "MARKET", "PORTICI", "FUEL_STATION", "BAKERY":
            var minute := int(_last_snapshot.get("minute",0))
            var start := 420 if _current_place=="BAKERY" else 480
            var finish := 1080 if _current_place=="BAKERY" else 1200
            var shop_open := minute>=start and minute+2<=finish
            var shop_status := Label.new()
            shop_status.text=tr("Open") if shop_open else tr("Closed")
            shop_status.text+=" · "+("%02d:%02d–%02d:%02d" % [floori(start/60.0),start%60,floori(finish/60.0),finish%60])
            _action_choices.add_child(shop_status)
            if _current_place=="MARKET" and employed!="shop-assistant":
                _action_choices.add_child(_button("Apply: market assistant",_apply_job.bind("shop-assistant")))
            for item in _last_snapshot.get("shop",[]):
                var id := str(item.get("id",""))
                var stock := int(item.get("stock",0))
                var choice := _button("Buy %s · %s EUR (%s left)" % [
                    str(item.get("label",id)),str(item.get("price",0)),stock],_buy_product.bind(id))
                choice.disabled = stock <= 0 or not shop_open or int(player.get("money",0))<int(item.get("price",0))
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
            var pension: Dictionary = _last_snapshot.get("pension",{})
            if not pension.is_empty():
                var pension_label := Label.new()
                pension_label.autowrap_mode=TextServer.AUTOWRAP_WORD_SMART
                pension_label.text=tr("Pension fund · balance %s EUR · arrears %s EUR") % [pension.get("fund",0),pension.get("arrears",0)]
                pension_label.text+="\n"+tr("Pensioners %s · weekly entitlement %s EUR") % [pension.get("retirees",0),pension.get("weeklyPension",35)]
                _action_choices.add_child(pension_label)
            var social: Dictionary = _last_snapshot.get("socialProtection",{})
            if not social.is_empty():
                var welfare := Label.new()
                welfare.autowrap_mode=TextServer.AUTOWRAP_WORD_SMART
                welfare.text=tr("Social fund · balance %s EUR") % social.get("fund",0)
                welfare.text+="\n"+tr("Unemployment paid %s · Sick leave paid %s EUR") % [social.get("unemploymentPaid",0),social.get("sicknessPaid",0)]
                welfare.text+="\n"+tr("Eligible job seekers %s · Eligible sick workers %s") % [social.get("eligibleUnemployed",0),social.get("eligibleSick",0)]
                _action_choices.add_child(welfare)
            var registry: Dictionary = _last_snapshot.get("registry",{})
            if not registry.is_empty():
                var identity := Label.new()
                identity.autowrap_mode=TextServer.AUTOWRAP_WORD_SMART
                identity.text=tr("Civil registry · your documents")
                if bool(registry.get("identityValid",false)):
                    identity.text+="\n"+str(registry.get("legalName",""))+" · "+str(registry.get("identityNumber",""))
                    identity.text+="\n"+(tr("Valid until day %s") % registry.get("expiresDay",0))
                else:
                    identity.text+="\n"+tr("No valid identity card")
                    _action_choices.add_child(_button(tr("Request identity card · 5 EUR"),_garden_action.bind("registry-identity")))
                identity.text+="\n"+(tr("Registered residence: %s") % (tr("Owned home") if bool(registry.get("residenceRegistered",false)) else tr("Not registered")))
                var fiscal = registry.get("fiscalCode")
                if fiscal!=null:
                    identity.text+="\n"+tr("Fiscal code: %s") % str(fiscal)
                if bool(registry.get("healthCardValid",false)):
                    identity.text+="\n"+tr("Health card valid until day %s") % registry.get("healthCardExpiresDay",0)
                else:
                    identity.text+="\n"+tr("No valid health card")
                _action_choices.add_child(identity)
                if bool(registry.get("canRequestHealthCard",false)):
                    _action_choices.add_child(_button(tr("Request health card · free"),_garden_action.bind("registry-health")))
                if bool(registry.get("canRegisterResidence",false)):
                    _action_choices.add_child(_button(tr("Register residence · free"),_garden_action.bind("registry-residence")))
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
    if _current_place in ["WORKSHOP", "OFFICE","NORTH_RESIDENCES"]:
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
    if action.begins_with("caritas-"):
        if _near_charity() and _connected and not _api.is_busy():_api.garden_action(action)
        return
    if action=="employment-resign":
        if _near_kiosk() and _connected and not _api.is_busy():
            _api.garden_action(action)
        return
    if action=="registry-consult":
        if _current_place=="CLINIC" and _near_kiosk() and _connected and not _api.is_busy():
            _api.garden_action(action)
        return
    if action.begins_with("barber-"):
        if _near_barber() and _connected and not _api.is_busy():_api.garden_action(action)
        return
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
    if ((action.begins_with("archive-year-") and _current_place=="LIBRARY" and _near_kiosk()) or ((action.begins_with("bank-") or action.begins_with("rental-") or action.begins_with("construction-") or action.begins_with("election-") or action.begins_with("registry-")) and _current_place=="OFFICE" and _near_kiosk()) or _near_garden() or _near_coop() or _near_orchard() or ((action in ["sell-vegetables","sell-eggs","sell-honey","sell-grape-juice","sell-wine"] or action.begins_with("sell-fruit-") or action.begins_with("stall-")) and _current_place=="MARKET" and _near_kiosk())) and _connected and not _api.is_busy():
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
    if id in ["pizza","gelato"] and _centro_food_counter()!=id:return
    if _near_kiosk() and not _api.is_busy() and _connected:
        _api.buy_product(id)

func _set_hands_enabled(enabled: bool) -> void:
    _hands.enabled = enabled
    _hands.update_pose(0.0,_step_bob,not _drive_active() and not _cycling())

func _try_strike() -> void:
    if not _foot_controls_available() or _rebirth_active() or _guest_active or _stairwell_active or not _connected or _api.is_busy():return
    if not _hands.held_item in ["","baton"]:
        _message.text=tr("Select empty hands or the training baton with Q.")
        return
    var id := _looked_at_resident()
    if id.is_empty():return
    _hands.strike()
    _api.garden_action("combat-strike:%s:%s" % ["baton" if _hands.held_item=="baton" else "fists",id])

func _try_shoot() -> void:
    if not _foot_controls_available() or _rebirth_active() or _guest_active or _stairwell_active or not _connected or _api.is_busy():return
    var permit: Dictionary = _last_snapshot.get("player",{}).get("weaponLicense",{})
    if not bool(permit.get("valid",false)) or not bool(permit.get("ownsSportWeapon",false)):return
    if int(permit.get("rounds",0))<=0:
        _message.text=tr("No game rounds remaining.")
        return
    var id := _looked_at_resident()
    _api.garden_action("weapon-fire" if id.is_empty() else "weapon-fire:"+id)

func _render_bus() -> void:
    var bus: Dictionary = _last_snapshot.get("bus",{})
    if not bool(bus.get("atStop",false)):return
    var info := Label.new()
    info.text=tr("Municipal bus · 06:00–22:00 · 2 EUR")
    _action_choices.add_child(info)
    for route in bus.get("routes",[]):
        var id := str(route.get("id",""))
        var choice := _button(tr("Bus to %s · %s min") % [route.get("label",id),route.get("minutes",0)],_ride_bus.bind(id))
        choice.disabled=not bool(bus.get("available",false))
        choice.set_meta("sold_out",choice.disabled)
        _action_choices.add_child(choice)

func _ride_bus(id: String) -> void:
    if not _connected or _api.is_busy() or not _near_kiosk() or not TownLayout.bus_stop(_current_place):return
    _action_panel.visible=false
    Input.set_mouse_mode(Input.MOUSE_MODE_CAPTURED)
    _api.garden_action("bus-ride:"+id)

func _render_armeria(permit: Dictionary) -> void:
    var player: Dictionary = _last_snapshot.get("player",{})
    var minute := int(_last_snapshot.get("minute",0))
    var shop_open := minute>=480 and minute+2<=1200
    var title := Label.new()
    title.text="Armeria · 08:00–20:00"
    _action_choices.add_child(title)
    if not bool(player.get("ownsBaton",false)):
        var baton_button := _button(tr("Buy training baton · 12 EUR"),_buy_training_baton)
        baton_button.disabled = not bool(permit.get("valid",false)) or not shop_open or int(player.get("money",0))<12
        baton_button.set_meta("sold_out",baton_button.disabled)
        _action_choices.add_child(baton_button)
    if not bool(permit.get("ownsSportWeapon",false)):
        var sport_button := _button(tr("Buy registered sporting weapon · 80 EUR"),_weapon_license_action.bind("weapon-buy-sport"))
        sport_button.disabled = not bool(permit.get("canBuySport",false))
        sport_button.set_meta("sold_out",sport_button.disabled)
        _action_choices.add_child(sport_button)
    else:
        var owned_sport := Label.new()
        owned_sport.text=tr("Registered sporting weapon: owned")
        _action_choices.add_child(owned_sport)
    if not bool(permit.get("valid",false)):
        var license_hint := Label.new()
        license_hint.text=tr("A valid weapon license is required. Visit the municipal office.")
        _action_choices.add_child(license_hint)

func _weapon_license_action(action: String) -> void:
    if not _connected or _api.is_busy():return
    if action=="weapon-license-apply" and not _near_kiosk():return
    if action!="weapon-license-apply" and not _near_sport_counter():return
    if (action=="weapon-license-apply" and _current_place=="OFFICE") or (action=="weapon-buy-sport" and _current_place=="MARKET") or (action in ["weapon-practice","weapon-refill"] and _current_place=="WORKSHOP"):
        _api.garden_action(action)

func _buy_training_baton() -> void:
    if _current_place=="MARKET" and _near_sport_counter() and _connected and not _api.is_busy():
        _api.garden_action("combat-buy-baton")

func _holdable_items() -> Array[String]:
    var player: Dictionary = _last_snapshot.get("player",{})
    var bag: Dictionary = player.get("bag",{"bread":player.get("bread",0),"water":player.get("bottledWater",0)})
    var choices: Array[String] = [""]
    if bool(player.get("ownsBaton",false)):
        choices.append("baton")
    var permit: Dictionary = player.get("weaponLicense",{})
    if not _rebirth_active() and bool(permit.get("valid",false)) and bool(permit.get("ownsSportWeapon",false)):
        choices.append("sport")
    for id in ["bread","water","grape-juice"]:
        if int(bag.get(id,0))>0:
            choices.append(id)
    return choices

func _refresh_held_item() -> void:
    if not _hands.held_item in _holdable_items():
        _hands.set_item("")
    if is_instance_valid(_held_label):
        _held_label.text = tr("Held item (Q): %s") % (tr("Empty hands") if _hands.held_item.is_empty() else tr(str(_hands.held_item).capitalize()))
        if not _rebirth_active() and _hands.held_item in ["","baton"]:
            _held_label.text += " · "+tr("X: strike")
        elif _hands.held_item=="sport":
            var permit: Dictionary = _last_snapshot.get("player",{}).get("weaponLicense",{})
            _held_label.text += " · "+tr("Rounds: %s/%s") % [permit.get("rounds",0),permit.get("capacity",5)]+" · "+tr("Left click / X: fire")

func _cycle_held_item() -> void:
    var choices := _holdable_items()
    var index := choices.find(_hands.held_item)
    _hands.set_item(choices[(index+1)%choices.size()])
    _refresh_held_item()

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

func _near_intercom() -> int:
    if _stairwell_active:return 0
    if _rebirth_active() or _drive_active() or _cycling() or _apartment_active:
        return 0
    var paths := {1:"ViaDelleMagnolie/CasaMagnolia/Intercom",2:"ViaDelleMagnolie/CasaTiglio/Intercom",3:"CasaOlmo/Intercom"}
    for code in paths:
        var bell := _scenery.get_node_or_null(paths[code]) as Node3D
        if bell!=null and _avatar.position.distance_to(bell.global_position-Vector3(0,bell.global_position.y,0))<1.8:
            return code
    return 0

func _open_intercom() -> void:
    _intercom_code = _near_intercom()
    if _intercom_code==0:return
    var address := str({1:"Via delle Magnolie 12",2:"Via delle Magnolie 14",3:"Via del Glicine 8"}[_intercom_code])
    _intercom_title.text = address
    _intercom_contacts.clear()
    for contact in _last_snapshot.get("contacts",[]):
        if str(contact.get("address",""))==address:
            _intercom_contacts.add_item(str(contact.get("name","")))
            _intercom_contacts.set_item_metadata(_intercom_contacts.item_count-1,str(contact.get("residentId","")))
    _intercom_reply.text = tr("Choose a contact who shared this address.") if _intercom_contacts.item_count>0 else tr("You do not know anyone at this address.")
    _intercom.show()
    Input.set_mouse_mode(Input.MOUSE_MODE_VISIBLE)

func _close_intercom() -> void:
    _intercom.hide()
    _intercom_code = 0
    Input.set_mouse_mode(Input.MOUSE_MODE_CAPTURED)

func _intercom_action(action: String) -> void:
    if not _intercom.visible or _intercom_code==0 or _near_intercom()!=_intercom_code or not _connected or _api.is_busy() or _intercom_contacts.selected<0:
        return
    _intercom_pending = true
    if action=="ring":
        _town_audio.play_alarm()
        _hands.reach()
    _api.garden_action("intercom:%s:%s:%s" % [action,_intercom_code,_intercom_contacts.get_item_metadata(_intercom_contacts.selected)])

func _phone_allowed() -> bool:
    if _stairwell_active:return false
    if _drive_active() or _cycling() or _jump_elapsed>=0.0 or not _road_pending.is_empty() or _home_transition:
        return false
    var rebirth = _last_snapshot.get("rebirth")
    if rebirth is Dictionary:
        if str(rebirth.get("phase",""))=="CHOICE":return false
        if str(rebirth.get("phase",""))=="CHILDHOOD" and int(rebirth.get("age",0))<12:return false
    return bool(_last_snapshot.get("player",{}).get("alive",true))

func _toggle_phone() -> void:
    if _phone.visible:
        _close_phone()
        return
    if not _phone_allowed() or _intercom.visible:return
    _close_dialog()
    _travel_panel.hide()
    _map_panel.hide()
    _action_panel.hide()
    _phone.update_data(_last_snapshot,_job_offers,_locations)
    _phone.show()
    _phone.open_app("calls" if _phone.has_ringing_call() else "home")
    Input.set_mouse_mode(Input.MOUSE_MODE_VISIBLE)

func _close_phone() -> void:
    if _phone.visible:
        _phone.hide()
        Input.set_mouse_mode(Input.MOUSE_MODE_CAPTURED)

func _on_phone_alarm(message: String) -> void:
    _phone_alert.text = message
    _phone_alert_remaining = 8.0
    _phone_alert.show()
    _town_audio.play_alarm()

func _reply_phone_call(resident_id: String,action: String) -> void:
    if _phone.visible and _phone_allowed() and _connected and not _api.is_busy():
        var route := "phone-out" if action in ["dial","hangup","invite-coffee","ask-home-visit","invite-my-home"] else "phone-call"
        _api.garden_action("%s:%s:%s" % [route,action,resident_id])

func _reply_phone_sms(resident_id: String,choice: int) -> void:
    if _phone.visible and _phone_allowed() and _connected and not _api.is_busy():
        _api.garden_action("phone-reply:%s:%s" % [choice,resident_id])

func _phone_request_jobs() -> void:
    if _connected and not _api.is_busy():
        _find_jobs()

func _phone_open_map() -> void:
    _close_phone()
    if not _map_panel.visible:
        _toggle_map()

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
            and not _action_panel.visible and not _map_panel.visible and not _intercom.visible
    if _clock.advance(delta, eligible):
        _clock_pending = true
        # The existing atomic Java action advances needs, NPCs and SQLite.
        _api.wait_for(1)

func _wait_one_hour() -> void:
    if not _api.is_busy():
        _api.wait_for(60)

func _can_walk_roads() -> bool:
    return not _stairwell_active and not _apartment_active and (not _rebirth_active() or str(_last_snapshot.get("rebirth",{}).get("phase",""))=="LINKED_ADULT")

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

func _guest_action(action: String) -> void:
    if not _connected or _api.is_busy() or _guest_pending:return
    var invitation = _last_snapshot.get("apartmentVisit")
    if not invitation is Dictionary:return
    if action=="enter" and (_guest_active or not _stairwell_active or _stairwell.near_unit(_avatar.position)!=int(invitation.get("unit",0))):return
    if action=="leave" and (not _guest_active or not _guest.near_exit(_avatar.position)):return
    if action in ["talk","coffee","snack","rest"] and not _guest_active:return
    if action=="talk" and (not is_instance_valid(_guest_host) or _avatar.position.distance_to(_guest_host.position)>=2.5):return
    if action in ["coffee","snack","dinner"] and not _guest.near_table(_avatar.position):return
    if action=="rest" and not _guest_sitting:return
    _guest_requested_action = action
    _guest_pending = true
    _api.garden_action("intercom:%s:1:%s" % [action,invitation.get("residentId","")])

func _open_guest(invitation: Dictionary) -> void:
    if _guest_active or not _stairwell_active:return
    _guest_return = _avatar.position
    _guest = GuestApartment.new()
    _guest.unit = int(invitation.get("unit",1))
    _guest.level = int(invitation.get("level",0))
    _guest.style = str(invitation.get("style","olmo"))
    _guest.resident_name = str(invitation.get("name",""))
    add_child(_guest)
    _guest_active = true
    _guest_sitting = false
    _guest_feedback_time = 0.0
    var original_root := _residents_root
    var host_root := Node3D.new()
    host_root.name = "GuestHost"
    _guest.add_child(host_root)
    _residents_root = host_root
    _guest_host = _make_resident(str(invitation.get("residentId","guest-host")),0,_guest.resident_name)
    _residents_root = original_root
    _guest_host.position = _guest.host_anchor()
    _guest_host.set_meta("activity","AT_HOME")
    var hand := _guest_host.get_node("ArmRight") as Node3D
    _box(hand,Vector3(0.20,0.025,0.15),Color("#9d6744"),Vector3(0,-0.48,-0.10))
    var book := hand.get_child(hand.get_child_count()-1) as Node3D
    book.name = "ReadingBook"
    book.hide()
    _host_activity = ""
    _host_seated = false
    _host_path.clear()
    _sync_host_routine(invitation)
    _stairwell.hide()
    _avatar.position = GuestApartment.SPAWN
    _view_yaw = 0.0
    _view_pitch = 0.0
    _reset_locomotion()
    _town_audio.play_effect("door")
    _follow_player()

func _close_guest() -> void:
    if not _guest_active:return
    _close_dialog()
    _guest_active = false
    _guest_sitting = false
    _guest_host = null
    _host_path.clear()
    _host_activity = ""
    _host_seated = false
    _guest_feedback_time = 0.0
    _guest.queue_free()
    _guest = null
    _stairwell.show()
    _avatar.position = _guest_return
    _reset_locomotion()
    _town_audio.play_effect("door")
    _follow_player()

func _toggle_guest_seat() -> void:
    if _guest_sitting:
        _guest_sitting = false
        _avatar.position = _guest_stand_at
    elif _guest.near_sofa(_avatar.position):
        _guest_stand_at = _avatar.position
        _guest_sitting = true
        _avatar.position = _guest.sofa_anchor()
    _reset_locomotion()
    _follow_player()

func _sync_host_routine(invitation: Dictionary) -> void:
    if not is_instance_valid(_guest_host):return
    _guest.update_cleanliness(int(invitation.get("cleanliness",100)))
    var household = invitation.get("household",{})
    if household is Dictionary:
        _guest.update_household(household)
        if household.get("host") is Dictionary:
            _sync_guest_contact(_guest_host,household["host"])
        _sync_guest_family(household)
    var next := str(invitation.get("activity",""))
    if next==_host_activity:return
    if _host_seated:
        _guest_host.position = _guest.routine_anchor(_host_activity)
        _host_seated = false
    _host_activity = next
    _host_path = _guest.host_path(_guest_host.position,_guest.routine_anchor(next))

# The household list is authoritative and contains only awake, present residents.
# Guests see models, not automatically learned names or a second copy of the host.
func _sync_guest_contact(person: Node3D,entry: Dictionary) -> void:
    person.set_meta("conversation_minutes",int(entry.get("conversationMinutes",10)))
    person.set_meta("conversation_reason",str(entry.get("conversationReason","PERSONAL")))
    person.set_meta("display_name",str(entry.get("name","Unknown resident")))
    person.set_meta("known_name",str(entry.get("knownName","")) if entry.get("knownName")!=null else "")
    person.set_meta("conversations",int(entry.get("conversations",0)))
    person.set_meta("relationship",str(entry.get("relationship","STRANGER")))
    person.get_node("ResidentName").hide()

func _sync_guest_family(household: Dictionary) -> void:
    var root := _guest.get_node_or_null("GuestFamily") as Node3D
    if root == null:
        root = Node3D.new()
        root.name = "GuestFamily"
        _guest.add_child(root)
    var residents = household.get("visibleResidents",[])
    if not residents is Array:
        residents = []
    var side := -1.0 if _guest.unit%2==1 else 1.0
    var spots: Array[Vector3] = []
    for at in [Vector3(-0.75*side,0,-2.0),Vector3(0.75*side,0,1.25),Vector3(2.75*side,0,-0.25),Vector3(-1.0*side,0,1.5),Vector3(0.0,0,-0.25),Vector3(-2.5*side,0,-2.0),Vector3(-1.25*side,0,-0.75)]:
        if _guest.can_stand(at,0.3):
            spots.append(at)
    var desired := {}
    for entry in residents:
        if not entry is Dictionary:continue
        var id := str(entry.get("id",""))
        if id.is_empty() or desired.has(id) or desired.size()>=mini(5,spots.size()):continue
        desired[id] = entry
    var existing := {}
    for child in root.get_children():
        var id := str(child.get_meta("resident_id",""))
        if not desired.has(id):
            root.remove_child(child)
            child.queue_free()
        else:
            existing[id] = child
    var ids := desired.keys()
    ids.sort()
    for id in ids:
        var entry: Dictionary = desired[id]
        var activity := str(entry.get("activity","RESTING"))
        var person := existing.get(id) as Node3D
        var target: Vector3 = _guest.routine_anchor(activity)
        match activity:
            "PLAYING":target=Vector3(-1.0*side,0,1.5)
            "HOMEWORK","FAMILY_DINNER":target=Vector3(-1.25*side,0,-0.75)
            "READING","WATCHING_TV","RESTING":target=Vector3(-1.0*side,0,1.5)
        var goal: Vector3 = spots[0]
        for at in spots:
            if at.distance_squared_to(target)<goal.distance_squared_to(target):
                goal=at
        spots.erase(goal)
        if person == null:
            var old_root := _residents_root
            _residents_root = root
            person = _make_resident(id,0,"")
            _residents_root = old_root
            person.position = goal
            var hand := person.get_node("ArmRight") as Node3D
            _box(hand,Vector3(0.18,0.025,0.15),Color("#9d6744"),Vector3(0,-0.48,-0.10))
            var book := hand.get_child(hand.get_child_count()-1) as Node3D
            book.name="FamilyBook"
            book.hide()
            _box(hand,Vector3(0.11,0.11,0.11),Color("#d8a445"),Vector3(0,-0.49,-0.08))
            var toy := hand.get_child(hand.get_child_count()-1) as Node3D
            toy.name="FamilyToy"
            toy.hide()
        _sync_guest_contact(person,entry)
        _sync_resident_appearance(person,ResidentAppearance.stage_for_age(int(entry.get("age",18))))
        if not person.has_meta("family_goal") or person.get_meta("family_goal")!=goal:
            var path: Array[Vector3] = _guest.host_path(person.position,goal)
            # Keep a resident in place if there is no safe route.
            if not path.is_empty():
                person.set_meta("family_goal",goal)
                person.set_meta("family_path",path)
            else:
                person.set_meta("family_path",[])
        person.set_meta("activity",activity)

func _animate_guest_family(delta: float) -> void:
    var root := _guest.get_node_or_null("GuestFamily") as Node3D
    if root == null:return
    for child in root.get_children():
        var person := child as Node3D
        var activity := str(person.get_meta("activity","RESTING"))
        var path: Array = person.get_meta("family_path",[])
        var walking := false
        var engaged := _dialog_panel.visible and _dialog_end_remaining<=0.0 and str(person.get_meta("resident_id",""))==_dialog_target
        if not engaged and not path.is_empty():
            var target: Vector3 = path[0]
            var motion := target-person.position
            if motion.length()>0.025:
                var next := person.position.move_toward(target,delta*0.95)
                if _guest.can_stand(next,0.25):
                    person.rotation.y=atan2(-motion.x,-motion.z)
                    person.position=next
                    walking=true
                else:
                    path.clear()
                    person.remove_meta("family_goal")
            else:
                path.pop_front()
        person.set_meta("family_path",path)
        var now := float(Time.get_ticks_msec())*0.001
        var reply_left := maxf(0.0,float(person.get_meta("reply_remaining",0.0))-delta)
        person.set_meta("reply_remaining",reply_left)
        _animate_resident_face(person,delta,now,engaged and reply_left>0.0)
        if engaged:
            var toward := _avatar.position-person.position
            if toward.length_squared()>0.001:
                person.rotation.y=lerp_angle(person.rotation.y,atan2(-toward.x,-toward.z),minf(delta*4.0,1.0))
        _animate_resident_head(person,delta,now,engaged,walking)
        var phase := now+float(person.get_meta("phase",0.0))
        var swing := sin(phase*9.0)*0.4 if walking else 0.0
        for limb in ["LegLeft","LegRight"]:
            var leg := person.get_node(limb) as Node3D
            leg.rotation.x=lerpf(leg.rotation.x,swing if limb=="LegLeft" else -swing,minf(delta*8.0,1.0))
        var gesture := -swing if walking else 0.0
        if not walking:
            match activity:
                "PLAYING":gesture=0.55+sin(phase*5.0)*0.5
                "HOMEWORK","READING":gesture=0.65+sin(phase*2.0)*0.15
                "COOKING","CLEANING","DRINKING","FAMILY_DINNER":gesture=0.65+sin(phase*3.0)*0.25
        var arm := person.get_node("ArmRight") as Node3D
        arm.rotation.x=lerpf(arm.rotation.x,gesture,minf(delta*6.0,1.0))
        arm.get_node("FamilyBook").visible=not walking and activity in ["HOMEWORK","READING"]
        arm.get_node("FamilyToy").visible=not walking and activity=="PLAYING"

func _animate_guest_host(delta: float) -> void:
    if not is_instance_valid(_guest_host):return
    _animate_guest_family(delta)
    var walking := false
    var engaged := _dialog_panel.visible and _dialog_end_remaining<=0.0 and str(_guest_host.get_meta("resident_id",""))==_dialog_target
    var reply_left := maxf(0.0,float(_guest_host.get_meta("reply_remaining",0.0))-delta)
    _guest_host.set_meta("reply_remaining",reply_left)
    var talking := _guest_feedback_time>0.0 or engaged
    _animate_resident_face(_guest_host,delta,float(Time.get_ticks_msec())*0.001,_guest_feedback_time>0.0 or (engaged and reply_left>0.0))
    _animate_resident_head(_guest_host,delta,float(Time.get_ticks_msec())*0.001,talking,not _host_path.is_empty())
    if not talking and not _host_path.is_empty():
        var target: Vector3 = _host_path[0]
        var motion := target-_guest_host.position
        if motion.length()>0.035:
            _guest_host.rotation.y = atan2(-motion.x,-motion.z)
            _guest_host.position = _guest_host.position.move_toward(target,delta*1.15)
            walking = true
        else:
            _host_path.pop_front()
    if not talking and not walking and _host_path.is_empty() and not _host_seated and _host_activity in ["READING","WATCHING_TV","RESTING","FAMILY_DINNER"]:
        var anchor: Vector3 = _guest.routine_anchor(_host_activity)
        if _guest_host.position.distance_to(anchor)<0.1:
            _host_seated = true
            _guest_host.position = _guest.routine_seat(_host_activity)+Vector3(0,-0.34,0)
            _guest_host.rotation.y = _guest.routine_seat_yaw(_host_activity)
    if talking:
        var toward := _avatar.position-_guest_host.position
        if toward.length_squared()>0.001:
            _guest_host.rotation.y = lerp_angle(_guest_host.rotation.y,atan2(-toward.x,-toward.z),minf(delta*4.0,1.0))
    var phase := float(Time.get_ticks_msec())*0.001
    var swing := sin(phase*9.0)*0.42 if walking else 0.0
    for limb in ["LegLeft","LegRight"]:
        var leg := _guest_host.get_node(limb) as Node3D
        var knee := leg.get_node("Knee") as Node3D
        leg.rotation.x = lerpf(leg.rotation.x,PI/2.0 if _host_seated else (swing if limb=="LegLeft" else -swing),minf(delta*8,1.0))
        knee.rotation.x = lerpf(knee.rotation.x,-PI/2.0 if _host_seated else 0.0,minf(delta*8,1.0))
    var arm := _guest_host.get_node("ArmRight") as Node3D
    var gesture := 0.0
    if talking:gesture=0.25+sin(phase*3.0)*0.18
    elif walking:gesture=-swing
    elif _host_activity=="FAMILY_DINNER" and _host_seated:
        _guest_host.rotation.y = lerp_angle(_guest_host.rotation.y,_guest.routine_seat_yaw(_host_activity),minf(delta*4.0,1.0))
        gesture=0.75+sin(phase*2.0)*0.45
    elif _host_activity in ["COOKING","CLEANING","DRINKING","READING"]:
        gesture=0.7+sin(phase*3.0)*0.2
    arm.rotation.x = lerpf(arm.rotation.x,gesture,minf(delta*6,1.0))
    arm.get_node("ReadingBook").visible = _host_seated and _host_activity=="READING" and not talking

# A short lobby appearance is supplied by real Java travel arrivals, not random NPC spawning.
func _sync_stairwell_arrivals() -> void:
    if not _stairwell_active or not is_instance_valid(_stairwell):return
    var root := _stairwell.get_node_or_null("ArrivingNeighbors") as Node3D
    if root==null:
        root=Node3D.new()
        root.name="ArrivingNeighbors"
        _stairwell.add_child(root)
    var entries = _last_snapshot.get("stairwellResidents",[])
    if not entries is Array:entries=[]
    var desired := {}
    for entry in entries:
        if not entry is Dictionary:continue
        var id := str(entry.get("id",""))
        if not id.is_empty() and desired.size()<2:desired[id]=entry
    for child in root.get_children():
        if not desired.has(str(child.get_meta("resident_id",""))):
            root.remove_child(child)
            child.queue_free()
    var ids := desired.keys()
    ids.sort()
    for i in range(ids.size()):
        var id := str(ids[i])
        var entry: Dictionary = desired[id]
        var person := root.get_node_or_null("Resident_"+id) as Node3D
        if person==null:
            var previous_root := _residents_root
            _residents_root=root
            person=_make_resident(id,i,"")
            _residents_root=previous_root
        person.position=Vector3(-0.82 if i==0 else 0.82,0,0.18)
        person.rotation.y=PI if i==0 else 0.0
        person.set_meta("activity","ARRIVING_HOME")
        _sync_guest_contact(person,entry)
        person.set_meta("conversation_minutes",4)
        person.set_meta("mood",str(entry.get("mood","CALM")))
        person.get_node("ResidentName").hide()
        _sync_resident_appearance(person,str(entry.get("appearanceStage","ADULT")))
        _sync_resident_uniform(person,str(entry.get("outfitVisual","")))

func _sync_magnolia_services() -> void:
    if not is_instance_valid(_stairwell):return
    var services: Dictionary = _last_snapshot.get("magnoliaServices",{})
    _stairwell.set_lighting_working(bool(services.get("lightingWorking",true)))
    var condo: Dictionary = _last_snapshot.get("condominium",{})
    _stairwell.set_dirt(int(condo.get("dirt",0)))

func _near_stairwell() -> bool:
    if _stairwell_active or _rebirth_active() or _drive_active() or _cycling() or _apartment_active or _current_place!="NORTH_RESIDENCES":
        return false
    var marker := _scenery.get_node_or_null("ViaDelleMagnolie/CasaMagnolia/HomeApproach") as Node3D
    return marker!=null and _avatar.position.distance_to(marker.global_position)<1.6

func _set_stairwell(active: bool) -> void:
    if active==_stairwell_active:return
    if not active and _guest_active:_close_guest()
    if active and not _near_stairwell():return
    _close_dialog()
    _reset_jump()
    _reset_locomotion()
    _stairwell_active = active
    if active:
        _stairwell_return = _avatar.position
        if not is_instance_valid(_stairwell):
            _stairwell = PublicStairwell.new()
            add_child(_stairwell)
        _stairwell.show()
        _sync_stairwell_arrivals()
        _sync_magnolia_services()
        for chunk in _chunks.values():
            chunk["root"].hide()
        _traffic_root.hide()
        _bike_model.hide()
        _main_panel.hide()
        _stairwell_hint.show()
        _avatar.position = PublicStairwell.SPAWN
        _view_yaw = 0.0
        _view_pitch = 0.0
        _town_audio.play_effect("door")
    else:
        _stairwell.hide()
        for chunk in _chunks.values():
            chunk["root"].show()
        _traffic_root.show()
        _main_panel.show()
        _stairwell_hint.hide()
        _avatar.position = _stairwell_return
        _view_yaw = PI
        _view_pitch = 0.0
        _town_audio.play_effect("door")
    _follow_player()
    _weather_fx.set_sheltered(active,_environment)

func _set_apartment(active: bool) -> void:
    if active == _apartment_active:
        return
    _reset_jump()
    _reset_locomotion()
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
        _apartment.reset_entrance()
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

func _sync_custody(justice: Dictionary) -> void:
    var status := str(justice.get("status","NONE"))
    var locked := status in ["DETAINED","PRISON"]
    if not locked:
        if _custody_active:
            _custody_active=false
            (_custody_panel.get_parent() as Control).hide()
            if is_instance_valid(_custody_cell):_custody_cell.queue_free()
            _custody_cell=null
            Input.set_mouse_mode(Input.MOUSE_MODE_CAPTURED)
            _follow_player()
            _message.text=tr("You have been released.")
        return
    if not _custody_active:
        _close_dialog()
        _close_phone()
        _close_intercom()
        _action_panel.hide()
        _map_panel.hide()
        _travel_panel.hide()
        _custody_active=true
        Input.set_mouse_mode(Input.MOUSE_MODE_VISIBLE)
        if not is_instance_valid(_custody_panel):
            var layer := CanvasLayer.new()
            layer.layer=90
            add_child(layer)
            var blocker := Control.new()
            layer.add_child(blocker)
            blocker.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
            blocker.mouse_filter=Control.MOUSE_FILTER_STOP
            blocker.name="CustodyBlocker"
            _custody_panel=PanelContainer.new()
            blocker.add_child(_custody_panel)
            _custody_panel.set_anchors_and_offsets_preset(Control.PRESET_CENTER_BOTTOM)
            _custody_panel.offset_left=-235
            _custody_panel.offset_right=235
            _custody_panel.offset_top=-245
            _custody_panel.offset_bottom=-15
            var col := VBoxContainer.new()
            _custody_panel.add_child(col)
            _custody_label=Label.new()
            _custody_label.horizontal_alignment=HORIZONTAL_ALIGNMENT_CENTER
            col.add_child(_custody_label)
            col.add_child(_button(tr("Wait one hour in custody"),_wait_custody.bind(60)))
            col.add_child(_button(tr("Serve one day"),_wait_custody.bind(1440)))
        (_custody_panel.get_parent() as Control).show()
        _custody_panel.show()
        _custody_cell=Node3D.new()
        add_child(_custody_cell)
        _custody_cell.position=Vector3(0,120,0)
        var wall := Color("#999b9a")
        _box(_custody_cell,Vector3(5,0.2,6),Color("#55595a"),Vector3(0,-0.1,0))
        _box(_custody_cell,Vector3(5,0.2,6),wall,Vector3(0,3.1,0))
        _box(_custody_cell,Vector3(5,3,0.2),wall,Vector3(0,1.5,-3))
        _box(_custody_cell,Vector3(0.2,3,6),wall,Vector3(-2.5,1.5,0))
        _box(_custody_cell,Vector3(0.2,3,6),wall,Vector3(2.5,1.5,0))
        _box(_custody_cell,Vector3(1,0.45,2),Color("#62686e"),Vector3(-1.65,0.25,-1.4))
        _box(_custody_cell,Vector3(1,0.15,2),Color("#b2b5b1"),Vector3(-1.65,0.55,-1.4))
        for i in range(13):
            _box(_custody_cell,Vector3(0.07,3,0.07),Color("#44484a"),Vector3(-2.4+i*0.4,1.5,2.9))
        var light := OmniLight3D.new()
        light.position=Vector3(0,2.6,0)
        light.omni_range=8.0
        light.light_energy=1.4
        _custody_cell.add_child(light)
    _custody_label.text=tr("Detained: waiting for court") if status=="DETAINED" else tr("Prison sentence")
    _custody_label.text+="\n"+tr("Time remaining: %s h %s min") % [int(justice.get("minutesRemaining",0))/60,int(justice.get("minutesRemaining",0))%60]
    _custody_label.text+="\n"+tr("Convictions: %s · Fine outstanding: %s EUR") % [justice.get("convictions",0),justice.get("fineOutstanding",0)]
    _custody_label.text+="\n"+tr("The town continues while you serve your sentence.")
    _camera.global_position=Vector3(0,121.65,-1.9)
    _camera.global_rotation=Vector3(0,PI,0)

func _pay_justice_fine() -> void:
    if not _custody_active and _current_place=="OFFICE" and _near_kiosk() and not _api.is_busy():
        _api.garden_action("justice-pay-fine")

func _wait_custody(minutes: int) -> void:
    if _custody_active and _connected and not _api.is_busy():
        _api.wait_for(minutes)

func _render_hud_summary(snapshot: Dictionary) -> void:
    var player: Dictionary = snapshot.get("player", {})
    var job = player.get("jobId")
    var profession := tr("unemployed") if job == null else str(job)
    _heading.text = tr(str(player.get("place", "Park")))
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
    var justice: Dictionary = player.get("justice",{})
    if str(justice.get("status","NONE"))!="NONE":
        _needs.text += "\n"+tr("Convictions: %s · Fine outstanding: %s EUR") % [justice.get("convictions",0),justice.get("fineOutstanding",0)]
        if str(justice.get("status",""))=="REPORTED":
            _needs.text+=" · "+tr("Assault reported to police")
    var utilities: Dictionary = snapshot.get("services", {})
    var fountain := "working" if utilities.get("fountainWorking", false) else "closed"
    var lighting := "lit" if utilities.get("lightsWorking", false) else "dark"
    _needs.text += tr("\nPark fountain: %s (%s clean units) | Street lights: %s") % [
        tr(fountain), str(utilities.get("cleanWater", 0)), tr(lighting)
    ]
    _needs.text += tr("\nResidents present in this district: %s") % [
        str(snapshot.get("residents", []).size())
    ]
    var demography = snapshot.get("demography")
    if demography is Dictionary:
        _needs.text += tr(" · Town population: %s") % (int(demography.get("alive",0))+2)

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
    var invitation = snapshot.get("apartmentVisit")
    if _guest_active and (not invitation is Dictionary or str(invitation.get("status",""))!="VISITING" or new_place!=_current_place or _rebirth_active()):
        _close_guest()
    if _guest_pending:
        _guest_pending = false
        if not _guest_active and _stairwell_active and invitation is Dictionary and str(invitation.get("status",""))=="VISITING" and _stairwell.near_unit(_avatar.position)==int(invitation.get("unit",0)):
            _open_guest(invitation)
    if _guest_active and invitation is Dictionary:
        _sync_host_routine(invitation)
    if _stairwell_active and (new_place!=_current_place or _rebirth_active()):
        _set_stairwell(false)
    if _stairwell_active:
        _sync_stairwell_arrivals()
        _sync_magnolia_services()
    if new_place != _current_place:
        _reset_jump()
        _reset_locomotion()
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
        _tick_public_door(1.0)
        _locations.clear() # Previous-origin travel estimates are now stale.
        _follow_player()
        _action_panel.visible = false
        _travel_panel.visible = false
    if continue_in_cabin:
        _park_in_cabin()

    _render_hud_summary(snapshot)
    _sync_custody(player.get("justice",{}))
    var home = snapshot.get("rebirth",{}).get("homeOptions")
    _set_apartment(home is Dictionary and bool(home.get("atHome",false)))
    if _apartment_active:
        _apartment.set_furniture_level(int(home.get("furnitureLevel",0)))
        _apartment.set_home_style(str(home.get("interiorStyle","standard")))
        _apartment.set_room_packages(home.get("furnishings",[]))
    _sync_family_entrance(home)
    if walking_transition and is_instance_valid(_home_entrance) and not road_arrival:
        _reset_jump()
        _reset_locomotion()
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
    _refresh_held_item()
    _town_map.update_world(_current_place, _locations)
    _render_residents()
    if _dialog_panel.visible:
        var person := _dialog_person(_dialog_target)
        if person == null:
            _finish_dialog_autonomously(_dialog_farewell if not _dialog_farewell.is_empty() else tr("They had to leave."))
        else:
            _dialog_title.text = _resident_caption(person)
            _dialog_attitude.text = tr("Attitude: %s") % tr(str(person.get_meta("relationship","STRANGER")).capitalize())
            _render_dialog_mood(person)
    _apply_weather()
    _render_actions()
    _phone.update_data(snapshot,_job_offers,_locations)
    if _intercom.visible and _near_intercom()!=_intercom_code:
        _close_intercom()
    if _phone.visible and not _phone_allowed():
        _close_phone()
    if _rebirth_active():
        _travel_panel.visible = false
        _map_panel.visible = false
        if not _child_can_walk():
            _close_dialog()
            _action_panel.visible = not walking_transition and not _phone.visible
            Input.set_mouse_mode(Input.MOUSE_MODE_VISIBLE if _action_panel.visible or _phone.visible else Input.MOUSE_MODE_CAPTURED)
        elif walking_transition:
            _action_panel.visible = false
    if _custody_active:
        _camera.global_position=Vector3(0,121.65,-1.9)
        _camera.global_rotation=Vector3(0,PI,0)
    elif _api != null and not _api.is_busy() and not clock_update:
        _api.list_locations()

func _set_weather_visuals(enabled: bool) -> void:
    _weather_fx.visual_enabled = enabled
    _apply_weather()

func _set_town_audio(enabled: bool) -> void:
    _town_audio.set_enabled(enabled)

func _footstep_surface() -> String:
    if _apartment_active or _inside_guest_room():
        return "wood"
    var at := _avatar.position
    if not _road_destination(at,17.0).is_empty():
        return "asphalt"
    if _current_place=="PARK":
        if absf(at.z+16.0)<1.6:
            return "asphalt"
        if absf(at.x)<1.5 or absf(at.z-5.0)<1.3 or (absf(at.x+11.0)<1.6 and absf(at.z+3.0)<8.0):
            return "stone"
        return "grass"
    if _current_place in ["WOODS","FARM"]:
        return "grass"
    return "stone"

func _update_town_audio(delta: float,moved: float) -> void:
    var at := _avatar.position
    var sheltered := _stairwell_active or _apartment_active or _inside_guest_room() or _drive_active()
    _town_audio.footstep(moved,_footstep_surface(),_movement_speed()>=RUN_SPEED,
        _jump_elapsed<0.0,not _drive_active() and not _cycling())
    var fountain: Variant = null
    var park: Variant = null
    var tower: Variant = null
    if _chunks.has("PARK"):
        var park_root: Node3D = _chunks["PARK"]["root"]
        park = park_root.position
        var water_node: MeshInstance3D = _chunks["PARK"]["water"]
        if is_instance_valid(water_node) and water_node.visible:
            fountain = water_node.global_position
    if _chunks.has("CENTRO"):
        var centre: Node3D = _chunks["CENTRO"]["root"]
        tower = centre.position+Vector3(6.8,10,-14.1)
    var car: Variant = null
    var car_distance := 28.0
    for edge in _traffic_edges.values():
        for record in edge["cars"]:
            var vehicle: Node3D = record["node"]
            if vehicle.visible:
                var distance := at.distance_to(vehicle.global_position)
                if distance<car_distance:
                    car_distance = distance
                    car = vehicle.global_position
    var crowd: Variant = null
    var nearby_people := 0
    var crowd_sum := Vector3.ZERO
    if is_instance_valid(_residents_root):
        for person in _residents_root.get_children():
            if str(person.get_meta("activity","")) not in ["SLEEPING","DECEASED"] and at.distance_to(person.global_position)<10.0:
                nearby_people += 1
                crowd_sum += person.global_position
        if nearby_people>=2:
            crowd = crowd_sum/float(nearby_people)
    _town_audio.update_scene(delta,at,sheltered,_last_snapshot,fountain,car,crowd,park,tower)
    var industrial: Variant = null
    var closest := 32.0
    for id in ["FACTORY","WORKSHOP","QUARRY","LOGISTICS_DEPOT"]:
        if _chunks.has(id):
            var root: Node3D = _chunks[id]["root"]
            var source := root.position+Vector3(-8,1,7)
            var distance := at.distance_to(source)
            if distance<closest:
                closest = distance
                industrial = source
    _town_audio.update_local(moved,_movement_speed()==CYCLE_SPEED and _cycling(),sheltered,at,_last_snapshot,industrial,park)

func _set_weather_audio(enabled: bool) -> void:
    _weather_fx.audio_enabled = enabled
    _apply_weather()

func _apply_weather() -> void:
    if _ground_material == null:
        return
    var climate: Dictionary = _last_snapshot.get("weather", {})
    _update_foliage(str(_last_snapshot.get("season","SPRING")),bool(climate.get("snowing",false)))
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
    _sun.light_energy = lerpf(0.08,1.27,daylight)
    _sun.light_color=Color("#f1bd83").lerp(Color("#fff5df"),smoothstep(0.0,0.65,daylight))
    _environment.ambient_light_energy = lerpf(0.30,0.66,daylight)
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
    _weather_fx.set_sheltered(_stairwell_active or _apartment_active or _inside_guest_room(),_environment)
    _weather_fx.apply(climate,_environment)
    var services: Dictionary = _last_snapshot.get("services", {})
    _lantern_glass.emission_enabled = night and bool(services.get("lightsWorking",false))
    # Decorative windows suggest warm interiors, not server-owned opening hours.
    _shop_glass.emission_enabled = minute >= 1080 or minute < 360
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
            var spray := water.get_node_or_null("FountainSpray") as CPUParticles3D
            if spray != null:
                spray.emitting = water.visible and _graphics_quality > 0

func _on_locations_updated(locations: Array) -> void:
    _locations = locations
    _phone.update_data(_last_snapshot,_job_offers,_locations)
    _town_map.update_world(_current_place, _locations)
    if _travel_panel.visible:
        _render_destinations()

func _on_job_offers_updated(jobs: Array) -> void:
    _job_offers = jobs
    _phone.update_data(_last_snapshot,_job_offers,_locations)
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
    if message.begins_with("Shot fired.") or message=="Practice shot recorded.":
        _hands.fire()
        _town_audio.play_effect("shot")
    if _guest_pending and _guest_active:
        _guest_feedback = tr(message)
        _guest_feedback_time = 7.0
        if _guest_requested_action=="coffee" and message=="You shared a coffee.":
            _guest.show_coffee()
    if _intercom_pending:
        _intercom_pending = false
        if _intercom.visible:
            _intercom_reply.text = tr(message)
    if _dialog_pending:
        _dialog_pending = false
        if _dialog_panel.visible:
            _dialog_reply.text = _localized_resident_reply(message)
            _dialog_history.append_text(_dialog_reply.text+"\n")
            var raw_reply := message.substr(message.find(": ")+2) if message.contains(": ") else message
            if _api.dialogue_language=="it" and not _api.dialogue_rendered and _dialog_reply.text==raw_reply:
                # An untranslated engine response is a subtitle, not Italian speech.
                _dialog_reply.text += "\n[Traduzione non disponibile / Translation unavailable]"
            else:
                _speak_resident_reply(_dialog_reply.text)
            var speaker := _dialog_person(_dialog_target)
            _begin_resident_reply(speaker,_dialog_reply.text)
            _dialog_input.clear()
            if not _api.dialogue_farewell.is_empty():
                _dialog_farewell=_api.dialogue_farewell
                _dialog_end_remaining=4.0
                if _dialog_reply.text!=_dialog_farewell:
                    _dialog_history.append_text(_dialog_farewell+"\n")
                    _dialog_reply.text+="\n"+_dialog_farewell
                _dialog_input.release_focus()
            else:
                _dialog_input.grab_focus()
    if not _clock_pending:
        _message.text = tr(message)
        _phone.add_notification(message)

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
    if _guest_pending:
        _guest_pending = false
        _guest_feedback = tr(message)
        _guest_feedback_time = 7.0
    if _intercom_pending:
        _intercom_pending = false
        if _intercom.visible:
            _intercom_reply.text = tr(message)
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
            _dialog_history.append_text(tr("Error")+": "+message+"\n")
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
