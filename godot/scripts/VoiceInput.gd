extends RefCounted
# No microphone is opened until the player explicitly clicks Record.
var pid := -1
var stage := ""
var prefix := ""
var language := "en"
var model := ""
var started := 0

func cleanup() -> void:
    if pid>0 and OS.is_process_running(pid):OS.kill(pid)
    pid=-1
    for suffix in [".wav",".txt"]:
        if not prefix.is_empty() and FileAccess.file_exists(prefix+suffix):
            DirAccess.remove_absolute(prefix+suffix)
    prefix=""
    stage=""

func start(locale: String) -> String:
    cleanup()
    if OS.get_name()!="Linux" or DisplayServer.get_name()=="headless":
        return "Microphone capture is available on Linux desktop."
    model=OS.get_environment("LIVING_ALIBI_WHISPER_MODEL")
    if not FileAccess.file_exists(model) or not FileAccess.file_exists("/usr/bin/arecord") or not FileAccess.file_exists("/usr/bin/whisper-cli"):
        return "Install alsa-utils and whisper-cpp, then set LIVING_ALIBI_WHISPER_MODEL."
    language="it" if locale.begins_with("it") else "en"
    prefix=ProjectSettings.globalize_path("user://voice-input-%d-%d" % [OS.get_process_id(),Time.get_ticks_usec()])
    pid=OS.create_process("/usr/bin/arecord",PackedStringArray(["-q","-f","S16_LE","-r","16000","-c","1","-d","5","-t","wav",prefix+".wav"]))
    if pid<=0:
        cleanup()
        return "Could not start microphone."
    stage="recording"
    started=Time.get_ticks_msec()
    return ""

func poll() -> Dictionary:
    if stage.is_empty():return {}
    if OS.is_process_running(pid):
        if Time.get_ticks_msec()-started>(15000 if stage=="recording" else 120000):
            cleanup()
            return {"error":"Voice input timed out."}
        return {}
    var code := OS.get_process_exit_code(pid)
    pid=-1
    if code!=0:
        cleanup()
        return {"error":"Voice input failed. Check microphone and model."}
    if stage=="recording":
        pid=OS.create_process("/usr/bin/whisper-cli",PackedStringArray(["-m",model,"-f",prefix+".wav","-l",language,"-otxt","-of",prefix,"-nt","-ng"]))
        if pid<=0:
            cleanup()
            return {"error":"Could not start transcription."}
        stage="transcribing"
        started=Time.get_ticks_msec()
        return {}
    var line := FileAccess.get_file_as_string(prefix+".txt").strip_edges() if FileAccess.file_exists(prefix+".txt") else ""
    cleanup()
    if line.is_empty():return {"error":"No speech recognized."}
    return {"text":line.left(1000)}
