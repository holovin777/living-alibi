extends PanelContainer
signal close_requested
signal jobs_requested
signal map_requested
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
    for entry in [["home","Phone home"],["jobs","Phone jobs"],["map","Phone map"],["contacts","Phone contacts"],["bank","Phone bank"],["messages","Phone notifications"]]:
        var button := Button.new()
        button.text = entry[1]
        button.size_flags_horizontal = Control.SIZE_EXPAND_FILL
        button.pressed.connect(open_app.bind(entry[0]))
        apps.add_child(button)
    var scroll := ScrollContainer.new()
    scroll.custom_minimum_size = Vector2(290,215)
    scroll.size_flags_vertical = Control.SIZE_EXPAND_FILL
    scroll.horizontal_scroll_mode = ScrollContainer.SCROLL_MODE_DISABLED
    col.add_child(scroll)
    _body = Label.new()
    _body.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
    _body.size_flags_horizontal = Control.SIZE_EXPAND_FILL
    scroll.add_child(_body)
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
    for resident in snapshot.get("residents",[]):
        if resident is Dictionary and int(resident.get("conversations",0))>0:
            var id := str(resident.get("id",""))
            if not id.is_empty():
                _contacts[id] = str(resident.get("name",""))
    if visible:
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

func refresh() -> void:
    if not is_instance_valid(_body):return
    var minute := int(_snapshot.get("minute",450))
    _clock.text = "%02d:%02d  |  %s %s" % [minute/60,minute%60,tr("Day"),_snapshot.get("day",1)]
    var player: Dictionary = _snapshot.get("player",{})
    var lines := PackedStringArray()
    _action.hide()
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
                lines.append(str(_contacts[id]))
            lines.append("\n"+tr("Calls and texts are not available yet."))
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
        "messages":
            lines.append(tr("Phone notifications"))
            if _messages.is_empty():
                lines.append(tr("No notifications this session."))
            for message in _messages:
                lines.append("\n"+tr(message))
    _body.text = "\n".join(lines)
