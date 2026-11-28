extends Node
## Original procedural cabin audio: no external files, network or licensed samples.
const RATE=16000
const Cabin=preload("res://scripts/GeoCockpit.gd")
var engine: AudioStreamPlayer
var tires: AudioStreamPlayer

func _ready() -> void:
    engine=_player(false)
    tires=_player(true)

func _player(rolling: bool) -> AudioStreamPlayer:
    var player:=AudioStreamPlayer.new()
    var bytes:=PackedByteArray()
    bytes.resize(RATE*4)
    var rng:=RandomNumberGenerator.new()
    rng.seed=491
    var low:=0.0
    for i in range(RATE*2):
        var t:=float(i)/RATE
        var white:=rng.randf_range(-1.0,1.0)
        low=lerpf(low,white,0.12)
        var sample: float
        if rolling:
            sample=low*0.65+(white-low)*0.12
        else:
            sample=sin(TAU*48.0*t)*0.32+sin(TAU*96.0*t)*0.14+sin(TAU*144.0*t)*0.06+low*0.10
        var envelope:=minf(1.0,minf(t/0.012,(2.0-t)/0.012))
        bytes.encode_s16(i*2,roundi(clampf(sample*envelope,-1,1)*32767))
    var stream:=AudioStreamWAV.new()
    stream.format=AudioStreamWAV.FORMAT_16_BITS
    stream.mix_rate=RATE
    stream.data=bytes
    stream.loop_mode=AudioStreamWAV.LOOP_FORWARD
    stream.loop_end=RATE*2
    player.stream=stream
    player.volume_db=-40
    add_child(player)
    return player

func update_drive(active: bool,speed_kmh: float,accelerating: bool,delta: float) -> void:
    if not active:
        engine.stop()
        tires.stop()
        return
    if not engine.playing:engine.play()
    var load_factor:=clampf(absf(speed_kmh)/65.0,0,1)
    var gear:=Cabin.gear_for_speed(absf(speed_kmh))
    var bands: Array[float]=[0.0,15.0,30.0,48.0,65.0,88.0]
    var rev:=clampf((absf(speed_kmh)-bands[gear-1])/(bands[gear]-bands[gear-1]),0.0,1.0)
    var pitch:=0.85+rev*0.75+(0.16 if accelerating else 0.0)
    engine.pitch_scale=lerpf(engine.pitch_scale,pitch,minf(1,delta*5))
    engine.volume_db=lerpf(engine.volume_db,-26.0+load_factor*8.0+(2.0 if accelerating else 0.0),minf(1,delta*5))
    if speed_kmh>1.5:
        if not tires.playing:tires.play()
        tires.volume_db=lerpf(tires.volume_db,-42.0+load_factor*19.0,minf(1,delta*4))
    elif tires.playing:tires.stop()
