extends Node3D
## Visual/audio weather only. The server controls precipitation and temperature.
## CPU particles and standard depth fog work with the Compatibility renderer.
var rain: CPUParticles3D
var snow: CPUParticles3D
var ambience: AudioStreamPlayer
var visual_enabled := true
var sheltered := false
var audio_enabled := true
var mode := "CLEAR"
var _rain_sound: AudioStreamWAV
var _wind_sound: AudioStreamWAV

func _ready() -> void:
    rain = _precipitation(false)
    snow = _precipitation(true)
    ambience = AudioStreamPlayer.new()
    ambience.volume_db = -28.0
    add_child(ambience)
    _rain_sound = _noise_loop(false)
    _wind_sound = _noise_loop(true)
    _sync()

func _precipitation(flakes: bool) -> CPUParticles3D:
    var p := CPUParticles3D.new()
    p.name = "Snow" if flakes else "Rain"
    p.emitting = false
    p.amount = 220 if flakes else 360
    p.lifetime = 5.0 if flakes else 1.25
    p.local_coords = false
    p.emission_shape = CPUParticles3D.EMISSION_SHAPE_BOX
    p.emission_box_extents = Vector3(11.0, 0.5, 11.0)
    p.direction = Vector3(0.12,-1.0,0.06)
    p.spread = 8.0 if flakes else 2.0
    p.gravity = Vector3(0,-0.25,0) if flakes else Vector3(0,-4.0,0)
    p.initial_velocity_min = 1.1 if flakes else 7.0
    p.initial_velocity_max = 1.7 if flakes else 9.0
    var mesh := BoxMesh.new()
    mesh.size = Vector3(0.065,0.065,0.065) if flakes else Vector3(0.018,0.40,0.018)
    var mat := StandardMaterial3D.new()
    mat.shading_mode = BaseMaterial3D.SHADING_MODE_UNSHADED
    mat.albedo_color = Color(0.92,0.96,1.0) if flakes else Color(0.57,0.72,0.88)
    mesh.material = mat
    p.mesh = mesh
    p.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
    add_child(p)
    return p

func _noise_loop(wind: bool) -> AudioStreamWAV:
    # Small original synthetic ambience, no downloads or licensed audio.
    var rng := RandomNumberGenerator.new()
    rng.seed = 731 if wind else 937
    var data := PackedByteArray()
    var samples := 44100
    data.resize(samples * 2)
    var low := 0.0
    for i in range(samples):
        var white := rng.randf_range(-1.0,1.0)
        low = lerpf(low,white,0.035 if wind else 0.28)
        var signal_value := low * 0.8 if wind else (white-low)*0.38
        var envelope := minf(1.0,minf(float(i)/220.0,float(samples-1-i)/220.0))
        data.encode_s16(i*2,int(clampf(signal_value*envelope,-1.0,1.0)*32767.0))
    var stream := AudioStreamWAV.new()
    stream.format = AudioStreamWAV.FORMAT_16_BITS
    stream.mix_rate = 22050
    stream.loop_mode = AudioStreamWAV.LOOP_FORWARD
    stream.loop_end = samples
    stream.data = data
    return stream

func follow(at: Vector3) -> void:
    position = at + Vector3(0.0,9.0,0.0)

func set_sheltered(value: bool, environment: Environment) -> void:
    if sheltered == value:
        return
    sheltered = value
    _sync()
    environment.fog_enabled = visual_enabled and not sheltered and mode in ["RAIN","SNOW","CLOUDY"]

func apply(climate: Dictionary, environment: Environment) -> void:
    mode = "SNOW" if climate.get("snowing",false) else (
            "RAIN" if climate.get("raining",false) else str(climate.get("sky","CLEAR")))
    _sync()
    # Fog is a visual approximation of reduced visibility, not a new Java weather type.
    environment.fog_enabled = visual_enabled and not sheltered and mode in ["RAIN","SNOW","CLOUDY"]
    environment.fog_density = 0.024 if mode == "SNOW" else (0.015 if mode == "RAIN" else 0.006)
    environment.fog_light_color = environment.background_color

func _sync() -> void:
    if rain == null:
        return
    var rain_on := visual_enabled and not sheltered and mode == "RAIN"
    var snow_on := visual_enabled and not sheltered and mode == "SNOW"
    rain.emitting = rain_on
    rain.visible = rain_on
    snow.emitting = snow_on
    snow.visible = snow_on
    ambience.volume_db = -40.0 if sheltered else -28.0
    var desired: AudioStreamWAV = null
    if audio_enabled and mode == "RAIN":
        desired = _rain_sound
    elif audio_enabled and mode in ["SNOW","CLOUDY"]:
        desired = _wind_sound
    if ambience.stream != desired:
        ambience.stop()
        ambience.stream = desired
        if desired != null:
            ambience.play()
