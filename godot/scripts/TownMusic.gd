extends AudioStreamPlayer
## Original procedural electric-piano score; no downloaded assets.
const RATE := 22050
const BEAT := 0.72
const BARS := 8
const CONFIG := "user://music.cfg"
var level := 25.0

func _ready() -> void:
    var config := ConfigFile.new()
    if config.load(CONFIG)==OK:
        level = clampf(float(config.get_value("audio","music",25.0)),0.0,100.0)
    stream = compose()
    volume_db = target_db(false)
    if DisplayServer.get_name()!="headless":
        play()

func set_level(value: float, persist: bool = true) -> void:
    level = clampf(value,0.0,100.0)
    if persist:
        var config := ConfigFile.new()
        config.set_value("audio","music",level)
        config.save(CONFIG)

func target_db(duck: bool) -> float:
    if level<=0.0:return -80.0
    return linear_to_db(level/100.0)-8.0-(12.0 if duck else 0.0)

func update_mix(delta: float, duck: bool) -> void:
    volume_db = lerpf(volume_db,target_db(duck),minf(1.0,delta*5.0))

static func compose() -> AudioStreamWAV:
    var count := roundi(float(BARS)*4.0*BEAT*RATE)
    var samples := PackedFloat32Array()
    samples.resize(count)
    var chords := [[48,55,59,64],[45,52,55,60],[41,48,52,57],[43,50,55,60],
        [40,47,50,55],[45,52,55,64],[38,45,48,53],[43,50,55,59]]
    var melody := [[72,76,74],[69,72,67],[65,69,72],[74,72,67],
        [71,67,64],[69,72,76],[74,69,65],[67,71,67]]
    for bar in range(BARS):
        var start := float(bar)*4.0*BEAT
        var chord: Array = chords[bar]
        _note(samples,start,2.7,float(chord[0]-12),0.10,true)
        for index in range(chord.size()):
            _note(samples,start+float(index)*0.12,2.45,float(chord[index]),0.072,true)
        for index in range(3):
            _note(samples,start+(0.65+float(index)*1.02)*BEAT,0.95,
                float(melody[bar][index]),0.095,false)
    var pcm := PackedByteArray()
    pcm.resize(count*2)
    for index in range(count):
        pcm.encode_s16(index*2,roundi(clampf(samples[index],-0.94,0.94)*32767.0))
    var result := AudioStreamWAV.new()
    result.format = AudioStreamWAV.FORMAT_16_BITS
    result.mix_rate = RATE
    result.stereo = false
    result.data = pcm
    result.loop_mode = AudioStreamWAV.LOOP_FORWARD
    result.loop_begin = 0
    result.loop_end = count
    return result

static func _note(samples: PackedFloat32Array, start: float, duration: float,
        midi: float, amplitude: float, soft: bool) -> void:
    var first := roundi(start*RATE)
    var length := roundi(duration*RATE)
    var frequency := 440.0*pow(2.0,(midi-69.0)/12.0)
    for index in range(length):
        var seconds := float(index)/RATE
        var attack := minf(1.0,seconds/(0.085 if soft else 0.025))
        var release := minf(1.0,(duration-seconds)/0.28)
        var envelope := attack*release*exp(-seconds*(0.8 if soft else 2.1))
        var phase := TAU*frequency*seconds
        var tone := sin(phase)+0.18*sin(phase*2.0)+0.05*sin(phase*3.0)
        var at := (first+index)%samples.size()
        samples[at] += amplitude*envelope*tone
