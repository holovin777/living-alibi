extends PanelContainer
signal close_requested
signal jobs_requested
signal map_requested
signal alarm_rang(message: String)
signal sms_reply_requested(resident_id: String, choice: int)
signal sms_received(message: String)
signal call_received(message: String)
signal call_action_requested(resident_id: String, action: String)
var _call_seen: Dictionary = {}
var _call_choice: OptionButton
var _call_decline: Button
var _sms_seen: Dictionary = {}
var _sms_initialized := false
var _sms_choice: OptionButton
var _sms_decline: Button
var alarm_at := -1
var _last_world_minute := -1
var _alarm_hour: SpinBox
var _alarm_minute: SpinBox
var _alarm_controls: HBoxContainer
var _alarm_cancel: Button
var app := "home"
var _snapshot: Dictionary = {}
var _jobs: Array = []
var _locations: Array = []
var _contacts: Dictionary = {}
var _messages: Array[String] = []
var _identity := ""
var _clock: Label
var _body: Label
var _action: Button

func _ready() -> void:
    name = "TownPhone"
    custom_minimum_size = Vector2(328,460)
    var shell := StyleBoxFlat.new()
    shell.bg_color = Color("#18262b")
    shell.border_color = Color("#536b70")
    shell.set_border_width_all(2)
    shell.set_corner_radius_all(20)
    shell.content_margin_left = 14
    shell.content_margin_right = 14
    shell.content_margin_top = 12
    shell.content_margin_bottom = 12
    add_theme_stylebox_override("panel",shell)
    var col := VBoxContainer.new()
    col.add_theme_constant_override("separation",9)
    add_child(col)
    _clock = Label.new()
    _clock.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
    col.add_child(_clock)
    var title := Label.new()
    title.text = "LIVING ALIBI"
    title.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
    title.add_theme_font_size_override("font_size",22)
    col.add_child(title)
    var apps := GridContainer.new()
    apps.columns = 2
    col.add_child(apps)
    for entry in [["home","Phone home"],["jobs","Phone jobs"],["map","Phone map"],["contacts","Phone contacts"],["bank","Phone bank"],["messages","Phone notifications"],["calendar","Phone calendar"],["weather","Phone weather"],["alarm","Phone alarm"],["sms","Phone SMS"],["calls","Phone calls"]]:
        var button := Button.new()
        button.text = entry[1]
        button.size_flags_horizontal = Control.SIZE_EXPAND_FILL
        button.pressed.connect(open_app.bind(entry[0]))
        apps.add_child(button)
    var scroll := ScrollContainer.new()
    scroll.custom_minimum_size = Vector2(290,165)
    scroll.size_flags_vertical = Control.SIZE_EXPAND_FILL
    scroll.horizontal_scroll_mode = ScrollContainer.SCROLL_MODE_DISABLED
    col.add_child(scroll)
    _body = Label.new()
    _body.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
    _body.size_flags_horizontal = Control.SIZE_EXPAND_FILL
    scroll.add_child(_body)
    _alarm_controls = HBoxContainer.new()
    col.add_child(_alarm_controls)
    _alarm_hour = SpinBox.new()
    _alarm_hour.min_value = 0
    _alarm_hour.max_value = 23
    _alarm_hour.value = 7
    _alarm_controls.add_child(_alarm_hour)
    var colon := Label.new()
    colon.text = ":"
    _alarm_controls.add_child(colon)
    _alarm_minute = SpinBox.new()
    _alarm_minute.min_value = 0
    _alarm_minute.max_value = 59
    _alarm_controls.add_child(_alarm_minute)
    _alarm_cancel = Button.new()
    _alarm_cancel.text = "Cancel alarm"
    _alarm_cancel.pressed.connect(cancel_alarm)
    col.add_child(_alarm_cancel)
    _sms_choice = OptionButton.new()
    col.add_child(_sms_choice)
    _sms_decline = Button.new()
    _sms_decline.text = "Reply: not today"
    _sms_decline.pressed.connect(_send_sms_reply.bind(2))
    col.add_child(_sms_decline)
    _call_choice = OptionButton.new()
    _call_choice.item_selected.connect(func(_index): _refresh_call_buttons())
    col.add_child(_call_choice)
    _call_decline = Button.new()
    _call_decline.text = tr("Decline call")
    _call_decline.pressed.connect(_send_call_action.bind("decline"))
    col.add_child(_call_decline)
    _action = Button.new()
    _action.pressed.connect(_app_action)
    col.add_child(_action)
    var close := Button.new()
    close.text = "Close phone (H)"
    close.pressed.connect(func(): close_requested.emit())
    col.add_child(close)
    refresh()

func update_data(snapshot: Dictionary,jobs: Array,locations: Array) -> void:
    _snapshot = snapshot
    _jobs = jobs
    _locations = locations
    var player: Dictionary = snapshot.get("player",{})
    var rebirth: Dictionary = snapshot.get("rebirth",{}) if snapshot.get("rebirth") is Dictionary else {}
    var identity := "%s:%s" % [player.get("incarnation",1),rebirth.get("childName","")]
    if identity != _identity:
        _contacts.clear()
        _messages.clear()
        _identity = identity
        _sms_seen.clear()
        _call_seen.clear()
        _sms_initialized = false
        alarm_at = -1
        _last_world_minute = -1
    if snapshot.get("contacts") is Array:
        _contacts.clear()
        for contact in snapshot["contacts"]:
            if contact is Dictionary and not str(contact.get("residentId","")).is_empty():
                _contacts[str(contact["residentId"])] = {"name":str(contact.get("name","")),"address":str(contact.get("address",""))}
    for resident in snapshot.get("residents",[]):
        if resident is Dictionary and int(resident.get("conversations",0))>0 and str(resident.get("name","Unknown resident"))!="Unknown resident":
            var id := str(resident.get("id",""))
            if not id.is_empty():
                if not _contacts.has(id):
                    _contacts[id] = {"name":str(resident.get("name","")),"address":""}
    var phone = snapshot.get("phone")
    if phone is Dictionary:
        for sms in phone.get("messages",[]):
            var key := "%s:%s:%s" % [sms.get("residentId",""),sms.get("day",0),sms.get("minute",0)]
            if not _sms_seen.has(key):
                _sms_seen[key] = true
                if _sms_initialized:
                    sms_received.emit(tr("New SMS from %s") % sms.get("name",""))
        _sms_initialized = true
        for call in phone.get("calls",[]):
            var call_key := "%s:%s:%s" % [call.get("residentId",""),call.get("day",0),call.get("minute",0)]
            if call.get("status","")=="RINGING" and not _call_seen.has(call_key):
                _call_seen[call_key] = true
                call_received.emit(tr("Incoming call from %s · H") % call.get("name",""))
    var now := world_minute(snapshot)
    if alarm_at>=0 and _last_world_minute>=0 and now>=alarm_at and _last_world_minute<alarm_at:
        var message := tr("Alarm: day %s, %s") % [alarm_at/1440+1,format_time(alarm_at%1440)]
        alarm_at = -1
        add_notification(message)
        alarm_rang.emit(message)
    _last_world_minute = now
    if visible:
        refresh()

static func world_minute(snapshot: Dictionary) -> int:
    return (maxi(1,int(snapshot.get("day",1)))-1)*1440+int(snapshot.get("minute",450))

static func format_time(minute: int) -> String:
    return "%02d:%02d" % [minute/60,minute%60]

func set_alarm(hour: int, minute: int) -> void:
    if hour<0 or hour>23 or minute<0 or minute>59:return
    var now := world_minute(_snapshot)
    var target := now-now%1440+hour*60+minute
    if target<=now:target += 1440
    alarm_at = target
    refresh()

func cancel_alarm() -> void:
    alarm_at = -1
    refresh()

func add_notification(message: String) -> void:
    if message.is_empty():return
    _messages.push_front(message)
    if _messages.size()>20:
        _messages.pop_back()
    if visible and app=="messages":
        refresh()

func open_app(id: String) -> void:
    app = id
    refresh()
    if app=="jobs":
        jobs_requested.emit()

func _app_action() -> void:
    if app=="jobs":
        jobs_requested.emit()
    elif app=="map":
        map_requested.emit()
    elif app=="alarm":
        set_alarm(int(_alarm_hour.value),int(_alarm_minute.value))
    elif app=="sms":
        _send_sms_reply(1)
    elif app=="calls":
        if _call_choice.selected>=0:
            var entry: Dictionary = _call_choice.get_item_metadata(_call_choice.selected)
            _send_call_action(str(entry.get("action","answer")))

func _send_sms_reply(choice: int) -> void:
    if _sms_choice.selected>=0:
        sms_reply_requested.emit(str(_sms_choice.get_item_metadata(_sms_choice.selected)),choice)

func _send_call_action(action: String) -> void:
    if _call_choice.selected>=0:
        var entry: Dictionary = _call_choice.get_item_metadata(_call_choice.selected)
        call_action_requested.emit(str(entry.get("id","")),action)

func has_ringing_call() -> bool:
    var phone = _snapshot.get("phone")
    if phone is Dictionary:
        for call in phone.get("calls",[]):
            if call.get("status","")=="RINGING":return true
    return false

func _refresh_call_buttons() -> void:
    if _call_choice.selected<0:return
    var entry: Dictionary = _call_choice.get_item_metadata(_call_choice.selected)
    var incoming := str(entry.get("action",""))=="answer"
    _action.text = tr("Answer call") if incoming else tr("Accept coffee invitation")
    _call_decline.visible = incoming

func refresh() -> void:
    if not is_instance_valid(_body):return
    var minute := int(_snapshot.get("minute",450))
    _clock.text = "%02d:%02d  |  %s %s" % [minute/60,minute%60,tr("Day"),_snapshot.get("day",1)]
    var player: Dictionary = _snapshot.get("player",{})
    var lines := PackedStringArray()
    _action.hide()
    _sms_choice.hide()
    _sms_decline.hide()
    _call_choice.hide()
    _call_decline.hide()
    _alarm_controls.visible = app=="alarm"
    _alarm_cancel.visible = app=="alarm" and alarm_at>=0
    match app:
        "home":
            lines.append(tr("Phone home"))
            lines.append("")
            lines.append(str(player.get("place",tr("Unknown location"))))
            lines.append(tr("Cash: %s EUR") % player.get("money","—"))
            lines.append(tr("Choose an app to explore the town."))
        "jobs":
            lines.append(tr("Phone jobs"))
            if _jobs.is_empty():
                lines.append(tr("No job listings loaded. Refresh to check."))
            for offer in _jobs:
                lines.append("\n%s\n%s · %s EUR/shift" % [offer.get("title",""),offer.get("workplace",""),offer.get("wage",0)])
            lines.append("\n"+tr("Apply in person at the workplace."))
            _action.text = "Refresh job listings"
            _action.show()
        "map":
            lines.append(tr("You are here: %s") % player.get("place",tr("Unknown location")))
            for destination in _locations:
                lines.append("%s · %s min" % [destination.get("label",""),destination.get("travelMinutes","—")])
            _action.text = "Open town map"
            _action.show()
        "contacts":
            lines.append(tr("Phone contacts"))
            if _contacts.is_empty():
                lines.append(tr("Talk to residents to add known contacts."))
            for id in _contacts:
                var contact: Dictionary = _contacts[id]
                lines.append(str(contact.get("name","")))
                if not str(contact.get("address","")).is_empty():
                    lines.append("  "+str(contact["address"]))
            lines.append("\n"+tr("Share your number in person to receive calls."))
        "bank":
            var bank = _snapshot.get("bank")
            lines.append(tr("Phone bank"))
            lines.append(tr("Cash: %s EUR") % player.get("money","—"))
            if bank is Dictionary:
                lines.append(tr("Personal balance: %s EUR") % bank.get("personal",0))
                lines.append(tr("Business balance: %s EUR") % bank.get("business",0))
                lines.append(tr("Debt: %s EUR") % bank.get("debt",0))
            else:
                lines.append(tr("Bank information unavailable."))
            lines.append("\n"+tr("Bank transactions require the bank office."))
        "calendar":
            lines.append(tr("Phone calendar"))
            var day := int(_snapshot.get("day",1))
            lines.append(tr("Year %s · Day %s of 365") % [_snapshot.get("year",(day-1)/365+1),_snapshot.get("dayOfYear",(day-1)%365+1)])
            lines.append(tr(str(_snapshot.get("season","SPRING"))))
            var job = player.get("jobId")
            lines.append("\n"+tr("Occupation: %s") % (str(job) if job!=null else tr("unemployed")))
            lines.append(tr("Work schedules are confirmed at the workplace."))
            var phone = _snapshot.get("phone")
            if phone is Dictionary:
                for meeting in phone.get("meetings",[]):
                    lines.append("\n"+tr("Coffee with %s · day %s · %s") % [meeting.get("name",""),meeting.get("day",0),format_time(int(meeting.get("minute",1080)))])
                    lines.append(tr("Cafe") + " · " + tr(str(meeting.get("status","SCHEDULED"))))
            if alarm_at>=0:
                lines.append(tr("Alarm: day %s, %s") % [alarm_at/1440+1,format_time(alarm_at%1440)])
        "weather":
            lines.append(tr("Phone weather"))
            var weather = _snapshot.get("weather")
            if weather is Dictionary:
                lines.append("\n%s · %s °C" % [tr(str(weather.get("sky","CLEAR"))),weather.get("temperatureC","—")])
                lines.append(tr(str(_snapshot.get("season","SPRING"))))
                if weather.get("raining",false):lines.append(tr("Rain"))
                if weather.get("snowing",false):lines.append(tr("Snow"))
            else:
                lines.append(tr("Weather information unavailable."))
            lines.append("\n"+tr("Current conditions, not a forecast."))
        "alarm":
            lines.append(tr("Phone alarm"))
            lines.append(tr("Choose an in-game hour and minute."))
            lines.append(tr("Alarm: day %s, %s") % [alarm_at/1440+1,format_time(alarm_at%1440)] if alarm_at>=0 else tr("No alarm set."))
            lines.append(tr("If the time has passed, the alarm is set for tomorrow."))
            _action.text = "Set alarm"
            _action.show()
        "sms":
            lines.append(tr("Phone SMS"))
            var phone = _snapshot.get("phone")
            if not phone is Dictionary or not phone.get("available",false):
                lines.append(tr("SMS is unavailable in this life stage."))
            else:
                var selected_id := str(_sms_choice.get_item_metadata(_sms_choice.selected)) if _sms_choice.selected>=0 else ""
                _sms_choice.clear()
                for sms in phone.get("messages",[]):
                    lines.append("\n%s · %s %s · %s" % [sms.get("name",""),tr("Day"),sms.get("day",0),format_time(int(sms.get("minute",0)))])
                    lines.append(tr(str(sms.get("text",""))))
                    if str(sms.get("text",""))=="Coffee tomorrow at 18:00? Let me know." and str(sms.get("reply","")).is_empty():
                        lines.append(tr("Reply thanks to accept today; tomorrow at 18:00 in the cafe."))
                    if str(sms.get("reply","")).is_empty():
                        _sms_choice.add_item(str(sms.get("name","")))
                        var index := _sms_choice.item_count-1
                        _sms_choice.set_item_metadata(index,sms.get("residentId",""))
                        if str(sms.get("residentId",""))==selected_id:_sms_choice.select(index)
                    else:
                        lines.append(tr("You: %s") % tr(str(sms.get("reply",""))))
                if phone.get("messages",[]).is_empty():
                    lines.append(tr("No SMS yet. Introduce yourself and build friendships."))
                if _sms_choice.item_count>0:
                    if _sms_choice.selected<0:_sms_choice.select(0)
                    _sms_choice.show()
                    _sms_decline.show()
                    _action.text = "Reply: thanks"
                    _action.show()
        "calls":
            lines.append(tr("Phone calls"))
            _call_choice.clear()
            var phone = _snapshot.get("phone")
            if not phone is Dictionary or not phone.get("available",false):
                lines.append(tr("Calls are unavailable in this life stage."))
            else:
                for call in phone.get("calls",[]):
                    var status := str(call.get("status",""))
                    lines.append("\n%s · %s %s · %s" % [call.get("name",""),tr("Day"),call.get("day",0),format_time(int(call.get("minute",0)))])
                    lines.append(tr(status))
                    if not str(call.get("text","")).is_empty():lines.append(str(call.get("text","")))
                    if status=="RINGING" or (status=="ANSWERED" and call.get("invitation",false) and int(call.get("day",0))==int(_snapshot.get("day",1))):
                        _call_choice.add_item(str(call.get("name","")))
                        _call_choice.set_item_metadata(_call_choice.item_count-1,{"id":call.get("residentId",""),"action":"answer" if status=="RINGING" else "accept"})
                if phone.get("calls",[]).is_empty():lines.append(tr("No calls yet. Share your number with a friend."))
                if _call_choice.item_count>0:
                    _call_choice.select(0)
                    _call_choice.show()
                    _action.show()
                    _refresh_call_buttons()
        "messages":
            lines.append(tr("Phone notifications"))
            if _messages.is_empty():
                lines.append(tr("No notifications this session."))
            for message in _messages:
                lines.append("\n"+tr(message))
    _body.text = "\n".join(lines)
