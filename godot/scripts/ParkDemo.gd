extends Node3D
## A visible first scene: you are an ordinary person in the public park.
## This renders the world; the Java simulation remains authoritative.
## WASD moves the placeholder avatar ONLY within this park preview.

var _avatar: Node3D
var _camera: Camera3D
var _api
var _status: Label
var _weather: Label
var _jobs: Label
var _grass: StandardMaterial3D
var _environment: Environment

func _ready() -> void:
    _create_park()
    _create_interface()
    _api = preload("res://examples/WorldApi.gd").new()
    add_child(_api)
    _api.world_updated.connect(_on_world_updated)
    _api.job_offers_updated.connect(_on_job_offers_updated)
    _api.request_failed.connect(_on_request_failed)
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
        _camera.position = _avatar.position + Vector3(0.0, 7.0, 12.0)
        _camera.look_at(_avatar.position + Vector3(0.0, 1.0, 0.0))

func _create_park() -> void:
    _environment = Environment.new()
    _environment.background_mode = Environment.BG_COLOR
    _environment.background_color = Color(0.63, 0.80, 0.96)
    _environment.ambient_light_source = Environment.AMBIENT_SOURCE_COLOR
    _environment.ambient_light_color = Color(0.80, 0.86, 0.93)
    var world_environment := WorldEnvironment.new()
    world_environment.environment = _environment
    add_child(world_environment)

    var sun := DirectionalLight3D.new()
    sun.rotation_degrees = Vector3(-55, -25, 0)
    sun.light_energy = 1.1
    add_child(sun)

    _grass = StandardMaterial3D.new()
    _grass.albedo_color = Color(0.27, 0.49, 0.26)
    var ground := MeshInstance3D.new()
    var terrain := BoxMesh.new()
    terrain.size = Vector3(40.0, 0.3, 40.0)
    ground.mesh = terrain
    ground.material_override = _grass
    ground.position.y = -0.2
    add_child(ground)

    _box(self, Vector3(3.0, 0.05, 35.0), Color(0.63, 0.57, 0.45),
            Vector3(0.0, -0.015, 0.0))

    for tree_pos in [
        Vector2(-9.0, -8.0), Vector2(10.0, -10.0), Vector2(-12.0, 7.0),
        Vector2(9.0, 8.0), Vector2(-15.0, -15.0), Vector2(15.0, 13.0)
    ]:
        _tree(Vector3(tree_pos.x, 0.0, tree_pos.y))

    _box(self, Vector3(2.0, 0.18, 0.75), Color(0.37, 0.23, 0.14),
            Vector3(5.0, 0.65, 2.0))
    _box(self, Vector3(0.15, 0.7, 0.15), Color(0.20, 0.20, 0.20),
            Vector3(4.3, 0.26, 2.0))
    _box(self, Vector3(0.15, 0.7, 0.15), Color(0.20, 0.20, 0.20),
            Vector3(5.7, 0.26, 2.0))

    _avatar = Node3D.new()
    _avatar.name = "OrdinaryVisitor"
    add_child(_avatar)
    _avatar.position = Vector3.ZERO
    _box(_avatar, Vector3(0.65, 1.15, 0.38), Color(0.20, 0.37, 0.68),
            Vector3(0.0, 1.0, 0.0))
    _sphere(_avatar, 0.30, Color(0.89, 0.70, 0.54),
            Vector3(0.0, 1.85, 0.0))
    _box(_avatar, Vector3(0.20, 0.65, 0.23), Color(0.19, 0.24, 0.29),
            Vector3(-0.18, 0.33, 0.0))
    _box(_avatar, Vector3(0.20, 0.65, 0.23), Color(0.19, 0.24, 0.29),
            Vector3(0.18, 0.33, 0.0))

    _camera = Camera3D.new()
    _camera.current = true
    add_child(_camera)
    _camera.position = Vector3(0.0, 7.0, 12.0)
    _camera.look_at(Vector3(0.0, 1.0, 0.0))

func _tree(at: Vector3) -> void:
    _box(self, Vector3(0.45, 1.8, 0.45), Color(0.33, 0.19, 0.11),
            at + Vector3(0.0, 0.9, 0.0))
    _sphere(self, 1.15, Color(0.16, 0.37, 0.16),
            at + Vector3(0.0, 2.4, 0.0))

func _box(parent: Node3D, size: Vector3, color: Color,
        where: Vector3) -> void:
    var mesh := BoxMesh.new()
    mesh.size = size
    var body := MeshInstance3D.new()
    body.mesh = mesh
    var material := StandardMaterial3D.new()
    material.albedo_color = color
    body.material_override = material
    body.position = where
    parent.add_child(body)

func _sphere(parent: Node3D, radius: float, color: Color,
        where: Vector3) -> void:
    var mesh := SphereMesh.new()
    mesh.radius = radius
    mesh.height = radius * 2.0
    var body := MeshInstance3D.new()
    body.mesh = mesh
    var material := StandardMaterial3D.new()
    material.albedo_color = color
    body.material_override = material
    body.position = where
    parent.add_child(body)

func _create_interface() -> void:
    var canvas := CanvasLayer.new()
    add_child(canvas)
    var panel := PanelContainer.new()
    panel.position = Vector2(16.0, 16.0)
    panel.custom_minimum_size = Vector2(410.0, 0.0)
    canvas.add_child(panel)

    var column := VBoxContainer.new()
    panel.add_child(column)
    _status = Label.new()
    _status.text = "Living Alibi — ordinary visitor in the park"
    column.add_child(_status)
    _weather = Label.new()
    _weather.text = "Connecting to the local Java world..."
    column.add_child(_weather)

    var actions := HBoxContainer.new()
    column.add_child(actions)
    var refresh_button := Button.new()
    refresh_button.text = "Refresh world"
    refresh_button.pressed.connect(_refresh)
    actions.add_child(refresh_button)
    var vacancies_button := Button.new()
    vacancies_button.text = "Find jobs"
    vacancies_button.pressed.connect(_find_jobs)
    actions.add_child(vacancies_button)

    _jobs = Label.new()
    _jobs.text = "WASD/arrows: walk locally. No quest or profession is forced."
    column.add_child(_jobs)

func _refresh() -> void:
    _api.refresh_world()

func _find_jobs() -> void:
    _api.list_jobs()

func _on_world_updated(snapshot: Dictionary) -> void:
    var player: Dictionary = snapshot.get("player", {})
    var weather: Dictionary = snapshot.get("weather", {})
    var profession = player.get("jobId")
    var label := "unemployed" if profession == null else str(profession)
    _status.text = "Visitor #%s | %s | %s EUR | %s" % [
        str(player.get("incarnation", 1)), str(player.get("place", "Park")),
        str(player.get("money", 50)), label
    ]
    _weather.text = "Day %s · %s · %s · %s °C" % [
        str(snapshot.get("day", 1)), str(snapshot.get("season", "SPRING")),
        str(weather.get("sky", "CLEAR")), str(weather.get("temperatureC", 0))
    ]
    if weather.get("snowing", false):
        _grass.albedo_color = Color(0.82, 0.86, 0.88)
        _environment.background_color = Color(0.72, 0.75, 0.80)
    elif weather.get("raining", false):
        _grass.albedo_color = Color(0.23, 0.38, 0.24)
        _environment.background_color = Color(0.43, 0.49, 0.55)
    else:
        _grass.albedo_color = Color(0.27, 0.49, 0.26)
        _environment.background_color = Color(0.63, 0.80, 0.96)

func _on_job_offers_updated(offers: Array) -> void:
    var lines: Array[String] = ["Job board (apply at the actual workplace):"]
    for offer in offers:
        lines.append("%s — %s EUR/shift, skill %s %s" % [
            str(offer.get("title", "?")), str(offer.get("wage", 0)),
            str(offer.get("subject", "none")),
            str(offer.get("requiredSkill", 0))
        ])
    _jobs.text = "\n".join(lines)

func _on_request_failed(message: String) -> void:
    _weather.text = "Java server unavailable: " + message
