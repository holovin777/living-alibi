extends RefCounted
# One voice at a time, with optional positional Linux synthesis.
var enabled := true
var last_key := ""
var active := false
var voices_by_language := {}
var player: AudioStreamPlayer3D
var pid := -1
var wav_path := ""
var started_at := 0

func setup(parent: Node3D) -> void:
    player=AudioStreamPlayer3D.new()
    player.name="NpcVoice"
    player.unit_size=2.0
    player.max_distance=5.0
    player.volume_db=-5.0
    parent.add_child(player)

func _remove_wave() -> void:
    if not wav_path.is_empty() and FileAccess.file_exists(wav_path):
        DirAccess.remove_absolute(wav_path)
    wav_path=""

func stop() -> void:
    if pid>0:
        if OS.is_process_running(pid):OS.kill(pid)
        pid=-1
    _remove_wave()
    if is_instance_valid(player):player.stop()
    if active and DisplayServer.get_name()!="headless":
        DisplayServer.tts_stop()
    active=false

func set_enabled(value: bool) -> void:
    enabled=value
    if not enabled:stop()

func _poll() -> void:
    if pid<=0:return
    if OS.is_process_running(pid):
        if Time.get_ticks_msec()-started_at>5000:stop()
        return
    var code := OS.get_process_exit_code(pid)
    pid=-1
    if code==0 and FileAccess.file_exists(wav_path) and is_instance_valid(player):
        var wave := AudioStreamWAV.load_from_file(wav_path)
        if wave!=null:
            player.stream=wave
            player.play()
    _remove_wave()

func present(key: String,line: String,speaker_id: String,distance: float,language: String,at: Vector3=Vector3.ZERO) -> void:
    if key.is_empty() or line.is_empty() or distance>4.5 or not enabled:
        stop()
        return
    if is_instance_valid(player):player.global_position=at
    if key==last_key:
        _poll()
        return
    stop()
    last_key=key
    if DisplayServer.get_name()=="headless":return
    var seed_value := absi(speaker_id.hash())
    if OS.get_name()=="Linux" and FileAccess.file_exists("/usr/bin/espeak-ng") and is_instance_valid(player):
        wav_path=ProjectSettings.globalize_path("user://npc-voice-%d-%d.wav" % [OS.get_process_id(),Time.get_ticks_usec()])
        pid=OS.create_process("/usr/bin/espeak-ng",PackedStringArray(["-v",language,"-p",str(40+seed_value%21),"-s",str(150+seed_value%25),"-w",wav_path,"--",line]))
        if pid>0:
            started_at=Time.get_ticks_msec()
            return
        _remove_wave()
    # Portable non-positional fallback where a system TTS service exists.
    if not voices_by_language.has(language):
        voices_by_language[language]=DisplayServer.tts_get_voices_for_language(language)
    var voices: PackedStringArray=voices_by_language[language]
    if voices.is_empty():return
    var volume := roundi(lerpf(75.0,20.0,clampf(distance/4.5,0.0,1.0)))
    DisplayServer.tts_speak(line,voices[seed_value%voices.size()],volume,0.9+float(seed_value%21)/100.0,1.0)
    active=true
