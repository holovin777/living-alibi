extends RefCounted
## Original synthesized emergency tones, spatially attached to vehicles.
const RATE=16000
const FRAMES=38400
static var streams: Dictionary={}

static func tone(kind: String) -> AudioStreamWAV:
    if streams.has(kind):return streams[kind]
    var bytes:=PackedByteArray()
    bytes.resize(FRAMES*2)
    var phase:=0.0
    for i in range(FRAMES):
        var t:=float(i)/RATE
        var frequency: float
        if kind=="AMBULANCE":
            # Two alternating pitches with smoothed transitions.
            frequency=735.0+145.0*tanh(6.0*sin(TAU*t/1.2))
        else:
            # Distinct police sweep.
            frequency=850.0+260.0*sin(TAU*t/2.4)
        phase=fposmod(phase+TAU*frequency/RATE,TAU)
        var envelope:=minf(1.0,minf(t/0.018,(2.4-t)/0.018))
        var sample: float=(sin(phase)*0.64+sin(phase*2.0)*0.15+sin(phase*3.0)*0.06)*envelope
        bytes.encode_s16(i*2,roundi(clampf(sample,-1.0,1.0)*32767.0))
    var wav:=AudioStreamWAV.new()
    wav.format=AudioStreamWAV.FORMAT_16_BITS
    wav.mix_rate=RATE
    wav.data=bytes
    wav.loop_mode=AudioStreamWAV.LOOP_FORWARD
    wav.loop_end=FRAMES
    streams[kind]=wav
    return wav

static func attach(vehicle: Node3D,kind: String) -> AudioStreamPlayer3D:
    var existing:=vehicle.get_node_or_null("EmergencySiren") as AudioStreamPlayer3D
    if existing!=null:return existing
    var player:=AudioStreamPlayer3D.new()
    player.name="EmergencySiren"
    player.position=Vector3(0,1.7,0)
    player.stream=tone(kind)
    player.unit_size=10.0
    player.max_distance=160.0
    player.volume_db=-14.0
    player.max_db=-12.0
    vehicle.add_child(player)
    return player

static func update(vehicle: Node3D,kind: String,active: bool) -> void:
    var player:=vehicle.get_node_or_null("EmergencySiren") as AudioStreamPlayer3D
    if active:
        if player==null:player=attach(vehicle,kind)
        if not player.playing:player.play()
    elif player!=null and player.playing:player.stop()
