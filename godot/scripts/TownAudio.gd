extends Node3D
## Original synthesized sound effects. No network, speech or licensed assets.
const RATE := 16000
var enabled := true
var _streams: Dictionary = {}
var steps: AudioStreamPlayer
var wind: AudioStreamPlayer
var water: AudioStreamPlayer3D
var traffic: AudioStreamPlayer3D
var birds: AudioStreamPlayer3D
var bell: AudioStreamPlayer3D
var voices: AudioStreamPlayer3D
var effects: AudioStreamPlayer
var bicycle: AudioStreamPlayer
var industry: AudioStreamPlayer3D
var crickets: AudioStreamPlayer3D
var _step_distance := 0.0
var _bird_timer := 2.0
var _last_hour := -1
var _rng := RandomNumberGenerator.new()

func _ready() -> void:
    _rng.seed = 97431
    for kind in ["grass","stone","wood","asphalt"]:
        _streams[kind] = _make_sound(kind,0.22,false)
    for kind in ["wind","water","traffic","voices"]:
        _streams[kind] = _make_sound(kind,3.0,true)
    _streams["bird"] = _make_sound("bird",0.72,false)
    _streams["bell"] = _make_sound("bell",2.8,false)
    _streams["landing"] = _make_sound("landing",0.32,false)
    _streams["shot"] = _make_sound("shot",0.35,false)
    _streams["door"] = _make_sound("door",0.65,false)
    for kind in ["bicycle","industry","crickets"]:
        _streams[kind] = _make_sound(kind,3.0,true)
    steps = AudioStreamPlayer.new()
    steps.volume_db = -18.0
    add_child(steps)
    wind = AudioStreamPlayer.new()
    wind.volume_db = -31.0
    wind.stream = _streams["wind"]
    add_child(wind)
    water = _source("FountainAudio","water",-17.0,18.0)
    traffic = _source("TrafficAudio","traffic",-20.0,28.0)
    birds = _source("BirdAudio","bird",-18.0,24.0)
    bell = _source("BellAudio","bell",-13.0,85.0)
    voices = _source("StreetMurmur","voices",-26.0,10.0)
    effects = AudioStreamPlayer.new()
    effects.volume_db = -17.0
    add_child(effects)
    bicycle = AudioStreamPlayer.new()
    bicycle.stream = _streams["bicycle"]
    bicycle.volume_db = -25.0
    add_child(bicycle)
    industry = _source("IndustryAudio","industry",-24.0,32.0)
    crickets = _source("NightCrickets","crickets",-23.0,25.0)

func _source(id: String, kind: String, volume: float, distance: float) -> AudioStreamPlayer3D:
    var p := AudioStreamPlayer3D.new()
    p.name = id
    p.stream = _streams[kind]
    p.volume_db = volume
    p.max_distance = distance
    p.unit_size = 4.0
    p.attenuation_model = AudioStreamPlayer3D.ATTENUATION_INVERSE_DISTANCE
    add_child(p)
    return p

func _make_sound(kind: String, duration: float, looping: bool) -> AudioStreamWAV:
    var count := roundi(duration*RATE)
    var data := PackedByteArray()
    data.resize(count*2)
    var low := 0.0
    var slow := 0.0
    for i in range(count):
        var t := float(i)/RATE
        var white := _rng.randf_range(-1.0,1.0)
        low = lerpf(low,white,0.14)
        slow = lerpf(slow,white,0.018)
        var sample := 0.0
        match kind:
            "grass":
                sample = (white-low)*0.48*exp(-t*21.0)+low*0.3*exp(-t*12.0)
            "stone":
                sample = (white*0.32+sin(TAU*125.0*t)*0.38)*exp(-t*30.0)
            "wood":
                sample = (low*0.35+sin(TAU*95.0*t)*0.42+sin(TAU*210.0*t)*0.12)*exp(-t*23.0)
            "asphalt":
                sample = (low*0.55+white*0.16)*exp(-t*17.0)
            "wind":
                sample = slow*(0.7+0.3*sin(TAU*t/3.0))*1.8
            "water":
                sample = (white-low)*0.24+low*0.35+sin(TAU*230.0*t)*0.018*sin(TAU*7.0*t)
            "traffic":
                sample = sin(TAU*60.0*t)*0.19+sin(TAU*120.0*t)*0.08+low*0.45
            "voices":
                # Quiet unintelligible texture, not recorded or generated dialogue.
                sample = (sin(TAU*146.0*t+sin(TAU*1.0*t)*1.5)*0.10+sin(TAU*218.0*t)*0.05+low*0.42)*(0.5+0.5*sin(TAU*2.0*t))
            "bird":
                var burst := fmod(t,0.24)
                var shape := sin(PI*clampf(burst/0.16,0.0,1.0))
                sample = sin(TAU*(1900.0*burst+2800.0*burst*burst))*shape*0.5
            "bell":
                sample = (sin(TAU*392.0*t)*exp(-t*1.5)+0.42*sin(TAU*786.0*t)*exp(-t*2.4)+0.25*sin(TAU*1151.0*t)*exp(-t*3.4))*0.55
            "shot":
                sample = (white*0.65+low*0.3)*exp(-t*23.0)
            "landing":
                sample = (sin(TAU*72.0*t)*0.48+low*0.60+white*0.14)*exp(-t*18.0)
            "door":
                var latch := maxf(0.0,t-0.44)
                sample = (sin(TAU*(170.0*t+110.0*t*t))*0.23+low*0.32)*sin(PI*clampf(t/0.48,0.0,1.0))
                if t>=0.44:
                    sample += (white*0.30+sin(TAU*105.0*latch)*0.45)*exp(-latch*32.0)
            "bicycle":
                var tick := fmod(t,0.075)
                sample = (white*0.32+sin(TAU*850.0*t)*0.14)*exp(-tick*95.0)+low*0.10
            "industry":
                sample = sin(TAU*50.0*t)*0.27+sin(TAU*100.0*t)*0.12+low*0.48*(0.75+0.25*sin(TAU*3.0*t))
            "crickets":
                var pulse := fmod(t,0.18)
                var gate := sin(PI*clampf(pulse/0.065,0.0,1.0))
                sample = sin(TAU*3700.0*t)*gate*0.32*(0.6+0.4*sin(TAU*t/1.5))
        # Fade at boundaries to avoid clicks, including loop wrap.
        var fade := 0.025 if looping else 0.008
        var envelope := minf(1.0,minf(t/fade,float(count-1-i)/RATE/fade))
        data.encode_s16(i*2,roundi(clampf(sample*envelope,-1.0,1.0)*32767.0))
    var stream := AudioStreamWAV.new()
    stream.format = AudioStreamWAV.FORMAT_16_BITS
    stream.mix_rate = RATE
    stream.data = data
    if looping:
        stream.loop_mode = AudioStreamWAV.LOOP_FORWARD
        stream.loop_end = count
    return stream

func set_enabled(value: bool) -> void:
    enabled = value
    if not enabled:
        for player in [steps,wind,water,traffic,birds,bell,voices,effects,bicycle,industry,crickets]:
            player.stop()
        _step_distance = 0.0

func _loop(player, active: bool) -> void:
    if active and not player.playing:
        player.play()
    elif not active and player.playing:
        player.stop()

func footstep(moved: float, surface: String, running: bool, grounded: bool, on_foot: bool) -> void:
    if not enabled or not grounded or not on_foot:
        _step_distance = 0.0
        return
    _step_distance += maxf(moved,0.0)
    var spacing := 1.15 if running else 0.86
    if _step_distance < spacing:
        return
    _step_distance = fmod(_step_distance,spacing)
    steps.stream = _streams.get(surface,_streams["stone"])
    steps.pitch_scale = _rng.randf_range(0.88,1.12)
    steps.volume_db = -15.0 if running else -19.0
    steps.play()

func play_alarm() -> void:
    if not enabled:return
    effects.stream = _streams["bell"]
    effects.pitch_scale = 1.45
    effects.play()

func play_effect(kind: String) -> void:
    if not enabled or not kind in ["landing","door","shot"]:
        return
    effects.stream = _streams[kind]
    effects.pitch_scale = _rng.randf_range(0.92,1.08)
    effects.play()

func update_local(moved: float, cycling: bool, sheltered: bool, at: Vector3, snapshot: Dictionary, industrial: Variant, park: Variant) -> void:
    if not enabled:
        return
    var minute := int(snapshot.get("minute",450))
    var weather: Dictionary = snapshot.get("weather",{})
    var night := minute<360 or minute>=1200
    var season := str(snapshot.get("season","SPRING"))
    _loop(bicycle,cycling and moved>0.0001 and not sheltered)
    if industrial is Vector3:
        industry.position = industrial
    _loop(industry,not sheltered and industrial is Vector3 and at.distance_to(industry.position)<32.0 and minute>=420 and minute<1140)
    if park is Vector3:
        crickets.position = park
    _loop(crickets,not sheltered and night and season!="WINTER" and not bool(weather.get("raining",false)) and not bool(weather.get("snowing",false)) and park is Vector3 and at.distance_to(crickets.position)<25.0)

func update_scene(delta: float, at: Vector3, sheltered: bool, snapshot: Dictionary,
        fountain: Variant, car: Variant, crowd: Variant, park: Variant, tower: Variant) -> void:
    var minute := int(snapshot.get("minute",450))
    var hour := int(snapshot.get("day",1))*24+floori(float(minute)/60.0)
    var ring := _last_hour>=0 and hour==_last_hour+1 and minute%60<5
    _last_hour = hour
    if not enabled:
        return
    var outdoor := not sheltered
    var weather: Dictionary = snapshot.get("weather",{})
    var day := minute>=360 and minute<1200
    _loop(wind,outdoor and not bool(weather.get("raining",false)))
    if fountain is Vector3:
        water.position = fountain
    _loop(water,outdoor and fountain is Vector3 and at.distance_to(water.position)<18.0)
    if car is Vector3:
        traffic.position = car
    _loop(traffic,outdoor and car is Vector3 and at.distance_to(traffic.position)<28.0)
    if crowd is Vector3:
        voices.position = crowd
    _loop(voices,crowd is Vector3 and at.distance_to(voices.position)<10.0)
    _bird_timer -= delta
    if _bird_timer<=0.0:
        _bird_timer = _rng.randf_range(5.0,12.0)
        if outdoor and day and park is Vector3 and not bool(weather.get("raining",false)):
            birds.position = park+Vector3(_rng.randf_range(-5.0,5.0),3.0,_rng.randf_range(-5.0,5.0))
            if at.distance_to(birds.position)<24.0:
                birds.pitch_scale = _rng.randf_range(0.84,1.15)
                birds.play()
    if not outdoor:
        birds.stop()
        bell.stop()
    elif ring and tower is Vector3:
        bell.position = tower
        if at.distance_to(bell.position)<85.0:
            bell.play()
