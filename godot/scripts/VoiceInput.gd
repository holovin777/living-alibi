extends RefCounted
# Recording begins only after the user explicitly clicks Record.
var pid := -1
var stage := ""
var prefix := ""
var language := "en"
var model := ""
var executable := ""
var started := 0
var parent: Node
var mic: AudioStreamPlayer
var recorder: AudioEffectRecord
const BUS := "LivingAlibiMicrophone"

func setup(owner: Node) -> void:
    parent=owner

func _release_mic() -> void:
    if is_instance_valid(mic):
        mic.stop()
        mic.queue_free()
    mic=null
    recorder=null
    var index := AudioServer.get_bus_index(BUS)
    if index>=0:AudioServer.remove_bus(index)

func cleanup() -> void:
    if recorder!=null:recorder.set_recording_active(false)
    _release_mic()
    if pid>0 and OS.is_process_running(pid):OS.kill(pid)
    pid=-1
    for suffix in [".wav",".txt"]:
        if not prefix.is_empty() and FileAccess.file_exists(prefix+suffix):
            DirAccess.remove_absolute(prefix+suffix)
    prefix=""
    stage=""

func start(locale: String) -> String:
    cleanup()
    if OS.get_name() not in ["Linux","Windows"] or DisplayServer.get_name()=="headless" or not is_instance_valid(parent):
        return "Microphone capture requires the desktop game."
    model=OS.get_environment("LIVING_ALIBI_WHISPER_MODEL")
    executable=OS.get_environment("LIVING_ALIBI_WHISPER_CLI")
    if executable.is_empty() and OS.get_name()=="Linux":executable="/usr/bin/whisper-cli"
    if not FileAccess.file_exists(model) or not FileAccess.file_exists(executable):
        return "Run the optional voice setup first / Avvia prima la configurazione vocale."
    language="it" if locale.begins_with("it") else "en"
    prefix=ProjectSettings.globalize_path("user://voice-input-%d-%d" % [OS.get_process_id(),Time.get_ticks_usec()])
    AudioServer.add_bus()
    var index := AudioServer.bus_count-1
    AudioServer.set_bus_name(index,BUS)
    # Effects run before bus volume. Never monitor microphone back to speakers.
    AudioServer.set_bus_volume_db(index,-80.0)
    recorder=AudioEffectRecord.new()
    recorder.format=AudioStreamWAV.FORMAT_16_BITS
    AudioServer.add_bus_effect(index,recorder)
    mic=AudioStreamPlayer.new()
    mic.stream=AudioStreamMicrophone.new()
    mic.bus=BUS
    parent.add_child(mic)
    recorder.set_recording_active(true)
    mic.play()
    stage="recording"
    started=Time.get_ticks_msec()
    return ""

# Whisper expects 16 kHz mono signed PCM. Convert Godot's native mix rate.
static func pcm16_mono(wave: AudioStreamWAV) -> AudioStreamWAV:
    if wave==null or wave.format!=AudioStreamWAV.FORMAT_16_BITS or wave.mix_rate<=0:return null
    var channels := 2 if wave.stereo else 1
    var source := wave.data
    var frames := floori(float(source.size())/(2*channels))
    if frames<2:return null
    var output_frames := floori(float(frames)*16000.0/float(wave.mix_rate))
    var bytes := PackedByteArray()
    bytes.resize(output_frames*2)
    for i in range(output_frames):
        var j := mini(frames-1,floori(float(i)*wave.mix_rate/16000.0))
        var sample := source.decode_s16(j*channels*2)
        if channels==2:sample=roundi((sample+source.decode_s16(j*4+2))/2.0)
        bytes.encode_s16(i*2,sample)
    var result := AudioStreamWAV.new()
    result.format=AudioStreamWAV.FORMAT_16_BITS
    result.mix_rate=16000
    result.stereo=false
    result.data=bytes
    return result

func poll() -> Dictionary:
    if stage.is_empty():return {}
    if stage=="recording":
        if Time.get_ticks_msec()-started<5000:return {}
        recorder.set_recording_active(false)
        var wave := pcm16_mono(recorder.get_recording())
        _release_mic()
        if wave==null or wave.save_to_wav(prefix+".wav")!=OK:
            cleanup()
            return {"error":"No microphone recording. Check operating system permissions."}
        pid=OS.create_process(executable,PackedStringArray(["-m",model,"-f",prefix+".wav","-l",language,"-otxt","-of",prefix,"-nt","-ng"]))
        if pid<=0:
            cleanup()
            return {"error":"Could not start transcription."}
        stage="transcribing"
        started=Time.get_ticks_msec()
        return {}
    if OS.is_process_running(pid):
        if Time.get_ticks_msec()-started>120000:
            cleanup()
            return {"error":"Voice input timed out."}
        return {}
    var code := OS.get_process_exit_code(pid)
    pid=-1
    if code!=0:
        cleanup()
        return {"error":"Voice input failed. Check model and executable."}
    var line := FileAccess.get_file_as_string(prefix+".txt").strip_edges() if FileAccess.file_exists(prefix+".txt") else ""
    cleanup()
    if line.is_empty():return {"error":"No speech recognized."}
    return {"text":line.left(1000)}
