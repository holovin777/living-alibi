extends RefCounted
# The desktop speech service is optional. Subtitles remain independent.
var enabled := true
var last_key := ""
var active := false
var voices_by_language := {}

func stop() -> void:
    if active and DisplayServer.get_name()!="headless":
        DisplayServer.tts_stop()
    active=false

func set_enabled(value: bool) -> void:
    enabled=value
    if not enabled:stop()

func present(key: String,line: String,speaker_id: String,distance: float,language: String) -> void:
    if key.is_empty() or line.is_empty() or distance>4.5 or not enabled:
        stop()
        return
    if key==last_key:return
    stop()
    last_key=key
    if DisplayServer.get_name()=="headless":return
    if not voices_by_language.has(language):
        voices_by_language[language]=DisplayServer.tts_get_voices_for_language(language)
    var voices: PackedStringArray=voices_by_language[language]
    if voices.is_empty():return
    var seed_value := absi(speaker_id.hash())
    var volume := roundi(lerpf(75.0,20.0,clampf(distance/4.5,0.0,1.0)))
    DisplayServer.tts_speak(line,voices[seed_value%voices.size()],volume,0.9+float(seed_value%21)/100.0,1.0)
    active=true
