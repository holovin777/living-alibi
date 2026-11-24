extends Node3D
## Living Alibi vertical slice: move around a local public scene; travel and
## careers are validated by the real Java/Spring server, never by a fake client wallet.
## WASD/arrow keys: walk. E: town travel gate. F: local interaction kiosk.

const GATE_POSITION := Vector3(0.0, 0.0, -12.0)
const KIOSK_POSITION := Vector3(7.0, 0.0, 3.0)
const INTERACTION_RADIUS := 3.5

var _avatar: Node3D
var _camera: Camera3D
var _scenery: Node3D
var _ground_material: StandardMaterial3D
var _environment: Environment
var _api
var _current_place := ""
var _last_snapshot: Dictionary = {}
var _connected := false
var _locations: Array = []
var _job_offers: Array = []
var _work_orders: Array = []

var _heading: Label
var _status: Label
var _weather: Label
var _needs: Label
var _message: Label
var _hint: Label
var _jobs: Label
var _travel_panel: PanelContainer
var _travel_choices: GridContainer
var _action_panel: PanelContainer
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
    _api.action_feedback.connect(_on_action_feedback)
    _api.request_failed.connect(_on_request_failed)
    _api.request_started.connect(_on_request_started)
    _api.request_finished.connect(_on_request_finished)
    _api.refresh_world()

func _process(delta: float) -> void:
    var direction := Vector3.ZERO
    if Input.is_key_pressed(KEY_W) or Input.is_key_pressed(KEY_UP):
        direction.z -= 1.0
    if Input.is_key_pressed(KEY_S) or Input.is_key_pressed(KEY_DOWN):
        direction.z += 1.0
    if Input.is_key_pressed(KEY_A) or Input.is_key_pressed(KEY_LEFT):
        direction.x -= 1.0
    if Input.is_key_pressed(KEY_D) or Input.is_key_pressed(KEY_RIGHT):
        direction.x += 1.0
    if direction.length_squared() > 0.0:
        _avatar.position += direction.normalized() * delta * 4.5
        _avatar.position.x = clampf(_avatar.position.x, -17.0, 17.0)
        _avatar.position.z = clampf(_avatar.position.z, -17.0, 17.0)
        _follow_player()

    if not _near_gate():
        _travel_panel.visible = false
    if not _near_kiosk():
        _action_panel.visible = false
    if _near_gate():
        _hint.text = "E: town travel gate — journeys consume real game time."
    elif _near_kiosk():
        _hint.text = "F: interact here — employment, study, or local resources."
    else:
        _hint.text = "WASD/arrows: walk. Yellow gate: travel. Blue kiosk: interact."

func _unhandled_key_input(event: InputEvent) -> void:
    if event is InputEventKey and event.pressed and not event.echo:
        if event.keycode == KEY_E and _near_gate():
            _toggle_travel()
            get_viewport().set_input_as_handled()
        elif event.keycode == KEY_F and _near_kiosk():
            _toggle_actions()
            get_viewport().set_input_as_handled()

func _near_gate() -> bool:
    return _avatar.position.distance_to(GATE_POSITION) < INTERACTION_RADIUS

func _near_kiosk() -> bool:
    return _avatar.position.distance_to(KIOSK_POSITION) < INTERACTION_RADIUS

func _follow_player() -> void:
    _camera.position = _avatar.position + Vector3(0.0, 7.0, 12.0)
    _camera.look_at(_avatar.position + Vector3(0.0, 1.0, 0.0))

func _build_world() -> void:
    _environment = Environment.new()
    _environment.background_mode = Environment.BG_COLOR
    _environment.background_color = Color(0.63, 0.80, 0.96)
    _environment.ambient_light_source = Environment.AMBIENT_SOURCE_COLOR
    _environment.ambient_light_color = Color(0.80, 0.86, 0.93)
    var lighting := WorldEnvironment.new()
    lighting.environment = _environment
    add_child(lighting)

    var sun := DirectionalLight3D.new()
    sun.rotation_degrees = Vector3(-55.0, -25.0, 0.0)
    sun.light_energy = 1.1
    add_child(sun)

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

    _camera = Camera3D.new()
    _camera.current = true
    add_child(_camera)
    _follow_player()
    _render_place("PARK")
    _current_place = "PARK"

func _render_place(place_id: String) -> void:
    if _scenery != null:
        remove_child(_scenery)
        _scenery.queue_free()
    _scenery = Node3D.new()
    _scenery.name = "PublicScene_" + place_id
    add_child(_scenery)

    _ground_material = StandardMaterial3D.new()
    _ground_material.albedo_color = Color(0.27, 0.49, 0.26)
    var ground := BoxMesh.new()
    ground.size = Vector3(40.0, 0.3, 40.0)
    var floor := MeshInstance3D.new()
    floor.mesh = ground
    floor.material_override = _ground_material
    floor.position.y = -0.2
    _scenery.add_child(floor)

    _box(_scenery, Vector3(3.0, 0.05, 35.0), Color(0.63, 0.57, 0.45),
            Vector3(0.0, -0.015, 0.0))
    _create_gate_and_kiosk()

    match place_id:
        "PARK":
            for p in [
                Vector2(-9.0, -8.0), Vector2(10.0, -10.0),
                Vector2(-12.0, 7.0), Vector2(9.0, 8.0)
            ]:
                _tree(Vector3(p.x, 0.0, p.y))
            _box(_scenery, Vector3(2.0, 0.18, 0.75), Color(0.37, 0.23, 0.14),
                    Vector3(-6.0, 0.65, 2.0))
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
            # Placeholder rock face and ore heaps; the finite seam is Java-owned.
            _box(_scenery, Vector3(11.0, 7.0, 3.0), Color(0.42, 0.43, 0.46),
                    Vector3(-8.0, 3.4, 9.0))
            for offset in [-11.0, -8.0, -5.0]:
                _box(_scenery, Vector3(1.8, 1.1, 2.0), Color(0.57, 0.38, 0.28),
                        Vector3(offset, 0.55, 3.0))
        "FACTORY":
            _building("Parts factory", Color(0.50, 0.53, 0.56))
            _box(_scenery, Vector3(1.5, 8.0, 1.5), Color(0.36, 0.37, 0.40),
                    Vector3(-10.0, 8.0, 7.0))
            for offset in [-8.0, -5.0, -2.0]:
                _box(_scenery, Vector3(2.0, 1.0, 1.6), Color(0.32, 0.38, 0.42),
                        Vector3(offset, 0.5, 2.2))
        "RIVER":
            _box(_scenery, Vector3(10.0, 0.11, 35.0), Color(0.12, 0.40, 0.67),
                    Vector3(-11.0, 0.02, 0.0))
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

    _apply_weather()

func _building(_title: String, wall_color: Color) -> void:
    _box(_scenery, Vector3(11.0, 4.5, 7.0), wall_color,
            Vector3(-7.0, 2.25, 7.5))
    _box(_scenery, Vector3(12.0, 0.7, 8.0), Color(0.27, 0.30, 0.33),
            Vector3(-7.0, 4.85, 7.5))
    _box(_scenery, Vector3(2.1, 3.0, 0.1), Color(0.20, 0.28, 0.32),
            Vector3(-7.0, 1.5, 3.95))

func _create_gate_and_kiosk() -> void:
    # A yellow gate is a map transition — actual travel is authorized by Java.
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
    buttons.add_child(_button("Wait 60m", _wait_one_hour))
    _hint = Label.new()
    _hint.text = "WASD/arrow keys to walk; E at gate; F at kiosk."
    column.add_child(_hint)
    _jobs = Label.new()
    _jobs.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
    _jobs.text = ""
    column.add_child(_jobs)

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

func _toggle_travel() -> void:
    if not _near_gate() or not _connected:
        _message.text = "Approach the yellow gate with the Java server running."
        return
    _action_panel.visible = false
    _travel_panel.visible = not _travel_panel.visible
    if _travel_panel.visible:
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
    _travel_panel.visible = false
    _api.go_to(destination)

func _toggle_actions() -> void:
    if not _near_kiosk() or not _connected:
        _message.text = "Approach the blue kiosk with the Java server running."
        return
    _travel_panel.visible = false
    _action_panel.visible = not _action_panel.visible
    if _action_panel.visible:
        _render_actions()
        if _current_place in ["WORKSHOP", "OFFICE"] and not _api.is_busy():
            _api.list_work_orders()

func _render_actions() -> void:
    _replace_choices(_action_choices, true)
    _action_title.text = "Local actions — " + _current_place.capitalize()
    var player: Dictionary = _last_snapshot.get("player", {})
    var employed := str(player.get("jobId", ""))
    match _current_place:
        "MARKET":
            _action_choices.add_child(_button("Apply: market assistant",
                    _apply_job.bind("shop-assistant")))
        "WORKSHOP":
            _action_choices.add_child(_button("Apply: technician",
                    _apply_job.bind("technician")))
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

func _wait_one_hour() -> void:
    if not _api.is_busy():
        _api.wait_for(60)

func _on_world_updated(snapshot: Dictionary) -> void:
    _connected = true
    _last_snapshot = snapshot
    var player: Dictionary = snapshot.get("player", {})
    var new_place := str(player.get("placeId", "PARK"))
    if new_place != _current_place:
        _current_place = new_place
        _render_place(new_place)
        _avatar.position = Vector3.ZERO
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
    _apply_weather()
    _render_actions()
    if _api != null and not _api.is_busy():
        _api.list_locations()

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
    else:
        _ground_material.albedo_color = Color(0.27, 0.49, 0.26)
        _environment.background_color = Color(0.63, 0.80, 0.96)

func _on_locations_updated(locations: Array) -> void:
    _locations = locations
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
    _jobs.text += "Working now: miners %s, machinists %s" % [
        str(report.get("minersAtWork", 0)), str(report.get("factoryWorkersAtWork", 0))
    ]

func _on_work_orders_updated(orders: Array) -> void:
    _work_orders = orders
    if _action_panel.visible:
        _render_actions()

func _on_action_feedback(message: String) -> void:
    _message.text = message

func _on_request_started(_route: String) -> void:
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
    _message.text = "Java server: " + message
