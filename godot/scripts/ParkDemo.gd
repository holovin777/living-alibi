extends Node3D
## Living Alibi vertical slice: move around a local public scene; travel and
## careers are validated by the real Java/Spring server, never by a fake client wallet.
## WASD/arrow keys: walk relative to the camera. Click the world for mouse look.
## V: first/third person. Escape: release mouse. E: gate. F: local kiosk.

const GATE_POSITION := Vector3(0.0, 0.0, -12.0)
const KIOSK_POSITION := Vector3(7.0, 0.0, 3.0)
const INTERACTION_RADIUS := 3.5
const WALK_SPEED := 4.5
const WALKER_RADIUS := 0.32
const COLLISION_STEP := 0.12
const ROAD_EDGE := 24.0
const ROAD_HALF_WIDTH := 2.0
const ROAD_TRIGGER := 23.8
const TownLayout = preload("res://scripts/TownMap.gd")
const MOUSE_SENSITIVITY := 0.0025
const EYE_HEIGHT := 1.65
const LiveClock = preload("res://scripts/LiveClock.gd")
const WeatherEffects = preload("res://scripts/WeatherEffects.gd")

# Local presentation only: true movement between districts is Java-authorized.
var _first_person := true
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
var _view_button: Button
var _jobs: Label
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
        _avatar.position = _move_on_ground(_avatar.position,
                relative_motion * delta * WALK_SPEED, WALKER_RADIUS, true)
        _follow_player()
        _try_road_crossing(relative_motion)

    _animate_residents(delta)
    _weather_fx.follow(_avatar.position)
    _tick_live_clock(delta)
    if not _near_gate():
        _travel_panel.visible = false
    if not _near_kiosk():
        _action_panel.visible = false
    if not _road_pending.is_empty():
        _hint.text = "Waiting for Java to confirm arrival..."
    elif not _road_destination(_avatar.position, ROAD_TRIGGER).is_empty():
        _hint.text = "District crossing · walk forward to travel"
    elif not _nearest_resident().is_empty():
        _hint.text = "T: talk to nearby resident · V: camera · Esc: cursor."
    elif _near_gate():
        _hint.text = "E: travel gate (game time). V: camera. Esc: cursor."
    elif _near_kiosk():
        _hint.text = "F: local interactions. V: camera. Esc: cursor."
    else:
        _hint.text = "WASD: walk · click scene: mouse look · V: camera · Esc: cursor."

func _unhandled_input(event: InputEvent) -> void:
    if event is InputEventMouseMotion and Input.mouse_mode == Input.MOUSE_MODE_CAPTURED:
        _view_yaw -= event.relative.x * MOUSE_SENSITIVITY
        _view_pitch = clampf(_view_pitch - event.relative.y * MOUSE_SENSITIVITY, -1.15, 1.15)
        _follow_player()
    elif event is InputEventMouseButton and event.button_index == MOUSE_BUTTON_LEFT and event.pressed:
        # UI consumes its own clicks before they reach this handler.
        if not _travel_panel.visible and not _action_panel.visible and not _map_panel.visible and not _dialog_panel.visible:
            Input.set_mouse_mode(Input.MOUSE_MODE_CAPTURED)

func _unhandled_key_input(event: InputEvent) -> void:
    if event is InputEventKey and event.pressed and not event.echo:
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
        elif event.keycode == KEY_V:
            _toggle_view_mode()
            get_viewport().set_input_as_handled()
        elif event.keycode == KEY_E and _near_gate() and not _map_panel.visible:
            _toggle_travel()
            get_viewport().set_input_as_handled()
        elif event.keycode == KEY_F and _near_kiosk() and not _map_panel.visible:
            _toggle_actions()
            get_viewport().set_input_as_handled()

func _near_gate() -> bool:
    return _avatar.position.distance_to(GATE_POSITION) < INTERACTION_RADIUS

func _near_kiosk() -> bool:
    return _avatar.position.distance_to(KIOSK_POSITION) < INTERACTION_RADIUS

func _toggle_view_mode() -> void:
    _first_person = not _first_person
    _view_button.text = "View: first person (V)" if _first_person else "View: third person (V)"
    _follow_player()

func _follow_player() -> void:
    # Hide the mesh at eye height to prevent looking through your own head.
    _avatar.visible = not _first_person
    _avatar.rotation.y = _view_yaw
    if _first_person:
        _camera.position = _avatar.position + Vector3(0.0, EYE_HEIGHT, 0.0)
        _camera.rotation = Vector3(_view_pitch, _view_yaw, 0.0)
    else:
        var orbit := Basis(Vector3.UP, _view_yaw) * Vector3(
                0.0, 3.8 - _view_pitch * 2.5, 6.0)
        _camera.position = _avatar.position + orbit
        _camera.look_at(_avatar.position + Vector3(0.0, 1.4, 0.0))

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
    add_child(_avatar)
    _box(_avatar, Vector3(0.65, 1.15, 0.38), Color(0.19, 0.37, 0.70),
            Vector3(0.0, 1.0, 0.0))
    _sphere(_avatar, 0.30, Color(0.88, 0.69, 0.53),
            Vector3(0.0, 1.85, 0.0))
    for x_offset in [-0.18, 0.18]:
        _box(_avatar, Vector3(0.2, 0.65, 0.23), Color(0.19, 0.24, 0.29),
                Vector3(x_offset, 0.33, 0.0))

    _weather_fx = WeatherEffects.new()
    add_child(_weather_fx)
    _weather_fx.follow(_avatar.position)
    _camera = Camera3D.new()
    _camera.current = true
    add_child(_camera)
    _follow_player()
    _render_place("PARK")
    _current_place = "PARK"

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
        var dimensions := Vector3(4.0,0.05,15.0) if vertical else Vector3(15.0,0.05,4.0)
        _box(roads, dimensions, Color(0.49,0.47,0.42),
                direction * 16.5 + Vector3(0,0.025,0))
        _sign(roads, str(id).capitalize(), direction * 19.0 + Vector3(0,2.8,0))

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
    for obstacle in _obstacles:
        var nearest := p.clamp(obstacle.position, obstacle.end)
        if p.distance_squared_to(nearest) < radius * radius:
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
    _view_button = _button("View: first person (V)", _toggle_view_mode)
    column.add_child(_view_button)
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
    _hint.text = "WASD: walk · click scene: mouse look · V: camera · Esc: cursor."
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
    _action_choices = VBoxContainer.new()
    _action_panel.add_child(_action_choices)
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
    if not _near_kiosk() or not _connected:
        _message.text = "Approach the blue kiosk with the Java server running."
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
    match _current_place:
        "QUARRY":
            _action_choices.add_child(_button("Apply: quarry miner",
                    _apply_job.bind("miner")))
            if employed == "miner":
                _action_choices.add_child(_button("Survey western mineral deposit · 2h",
                        _prospect))
        "FACTORY":
            _action_choices.add_child(_button("Apply: factory machinist",
                    _apply_job.bind("machinist")))
        "MARKET":
            _action_choices.add_child(_button("Apply: market assistant",
                    _apply_job.bind("shop-assistant")))
        "WORKSHOP":
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
    _last_snapshot = snapshot
    var player: Dictionary = snapshot.get("player", {})
    var new_place := str(player.get("placeId", "PARK"))
    var road_arrival := new_place == _road_pending and not _road_pending.is_empty()
    if not _road_pending.is_empty():
        _road_blocked = "" if road_arrival else _road_pending
        _road_pending = ""
    if new_place != _current_place:
        _close_dialog()
        _current_place = new_place
        _render_place(new_place)
        _avatar.position = _road_arrival if road_arrival else Vector3.ZERO
        if not _can_stand(_avatar.position, WALKER_RADIUS, true):
            _avatar.position = Vector3.ZERO
        _locations.clear() # Previous-origin travel estimates are now stale.
        _follow_player()
        _action_panel.visible = false
        _travel_panel.visible = false

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
    var utilities: Dictionary = snapshot.get("services", {})
    var fountain := "working" if utilities.get("fountainWorking", false) else "closed"
    var lighting := "lit" if utilities.get("lightsWorking", false) else "dark"
    _needs.text += "\nPark fountain: %s (%s clean units) | Street lights: %s" % [
        fountain, str(utilities.get("cleanWater", 0)), lighting
    ]
    _needs.text += "\nResidents present in this district: %s" % [
        str(snapshot.get("residents", []).size())
    ]
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

func _set_request_busy(busy: bool) -> void:
    for node in get_tree().get_nodes_in_group("world_api_buttons"):
        if node is Button:
            var same_place := node.has_meta("destination") and (
                    str(node.get_meta("destination")) == _current_place)
            node.disabled = busy or same_place

func _on_request_failed(message: String) -> void:
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
