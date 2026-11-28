extends PanelContainer
## In-game smartphone. Reads the shared snapshot, never invents contacts or balances.
signal requested_action(id: String)
signal destination_selected(point: Vector2)
signal closed
var content: VBoxContainer
var data: Dictionary={}
var current_tab:="Calls"
var fingerprint:=0
var status: Label

func _ready() -> void:
    custom_minimum_size=Vector2(330,480)
    var outer:=VBoxContainer.new()
    add_child(outer)
    var heading:=HBoxContainer.new()
    outer.add_child(heading)
    var name_label:=Label.new()
    name_label.text="LIVING · Phone"
    name_label.size_flags_horizontal=Control.SIZE_EXPAND_FILL
    heading.add_child(name_label)
    var close:=Button.new()
    close.text="×"
    close.pressed.connect(func():closed.emit())
    heading.add_child(close)
    status=Label.new()
    status.autowrap_mode=TextServer.AUTOWRAP_WORD_SMART
    outer.add_child(status)
    var tabs:=GridContainer.new()
    tabs.columns=3
    outer.add_child(tabs)
    for tab in ["Web","Calls","Messages","Contacts","Jobs","Bank","Meetings"]:
        var btn:=Button.new()
        btn.text=tab
        btn.size_flags_horizontal=Control.SIZE_EXPAND_FILL
        btn.pressed.connect(_switch.bind(tab))
        tabs.add_child(btn)
    var scroll:=ScrollContainer.new()
    scroll.custom_minimum_size=Vector2(320,335)
    scroll.size_flags_vertical=Control.SIZE_EXPAND_FILL
    scroll.horizontal_scroll_mode=ScrollContainer.SCROLL_MODE_DISABLED
    outer.add_child(scroll)
    content=VBoxContainer.new()
    content.size_flags_horizontal=Control.SIZE_EXPAND_FILL
    scroll.add_child(content)
    hide()

func open_phone(world: Dictionary) -> void:
    show()
    if bool(world.get("rebirth",false)):current_tab="Web"
    refresh(world,"",false,true)

func refresh(world: Dictionary,message: String,pending: bool,force: bool=false) -> void:
    if not visible:return
    var next: Dictionary=world.get("phone",{})
    status.text="Day %s · %02d:%02d | %s" % [world.get("day",1),int(float(world.get("minute",0))/60.0),int(world.get("minute",0))%60,"Please wait…" if pending else message]
    var digest:=hash(next)
    if force or fingerprint!=digest:
        data=next
        fingerprint=digest
        _render()

func _switch(tab: String) -> void:
    current_tab=tab
    _render()

func _label(value: String) -> void:
    var item:=Label.new()
    item.text=value
    item.autowrap_mode=TextServer.AUTOWRAP_WORD_SMART
    content.add_child(item)

func _button(label: String,callback: Callable) -> void:
    var btn:=Button.new()
    btn.text=label
    btn.autowrap_mode=TextServer.AUTOWRAP_WORD_SMART
    btn.pressed.connect(callback)
    content.add_child(btn)

func _actions_for(prefix: String,id: String) -> void:
    for action in data.get("actions",[]):
        var key:=str(action.get("id",""))
        if key.begins_with(prefix) and key.ends_with(":"+id):
            _button(str(action.get("label","")),func():requested_action.emit(key))

func _route(record: Dictionary) -> void:
    destination_selected.emit(Vector2(float(record["x"]),float(record["z"])))

func _render() -> void:
    for child in content.get_children():
        content.remove_child(child)
        child.queue_free()
    var report: Dictionary=data.get("report",{})
    if current_tab=="Web":
        _render_web()
        return
    if not bool(report.get("available",false)):
        _label("Phone unavailable / Telefono non disponibile")
        return
    match current_tab:
        "Calls":
            for action in data.get("actions",[]):
                if str(action.get("id",""))=="phone-ambulance":_button(str(action.get("label","")),func():requested_action.emit("phone-ambulance"))
                if str(action.get("id",""))=="phone-fire-report":_button(str(action.get("label","")),func():requested_action.emit("phone-fire-report"))
            if report.get("calls",[]).is_empty() and report.get("dialContacts",[]).is_empty():_label("No calls or shared numbers yet.")
            for call in report.get("calls",[]):
                _label(str(call.get("name",""))+" · "+str(call.get("status",""))+"\n"+str(call.get("text","")))
                _actions_for("phone-call:",str(call.get("residentId","")))
            for contact in report.get("dialContacts",[]):
                _label(str(contact.get("name","")))
                _actions_for("phone-out:",str(contact.get("residentId","")))
            for call in report.get("outgoingCalls",[]):
                _label(str(call.get("name",""))+" · "+str(call.get("status",""))+"\n"+str(call.get("text","")))
        "Messages":
            if report.get("messages",[]).is_empty():_label("No messages / Nessun messaggio")
            for msg in report.get("messages",[]):
                _label("%s · Day %s\n%s\n%s" % [msg.get("name",""),msg.get("day",0),msg.get("text",""),msg.get("reply","")])
                _actions_for("phone-reply:",str(msg.get("residentId","")))
        "Contacts":
            if data.get("contacts",[]).is_empty():_label("Meet residents and learn their names.")
            for contact in data.get("contacts",[]):
                var address:=str(contact.get("address",""))
                _label(str(contact.get("name",""))+"\n"+(address if not address.is_empty() else "Address not shared"))
                var entrances: Dictionary=data.get("contactEntrances",{})
                var entrance: Dictionary=entrances.get(str(contact.get("residentId","")),{})
                if not address.is_empty() and not entrance.is_empty():
                    _button("Directions to intercom / Indicazioni citofono",_route.bind(entrance))
        "Jobs":
            for job in data.get("jobs",[]):
                var place: Dictionary=job.get("workplace",{})
                _label("%s · €%s / shift\n%s · skill %s/%s%s" % [job.get("title",""),job.get("wage",0),place.get("label",""),job.get("currentSkill",0),job.get("requiredSkill",0),"" if job.get("funded",false) else " · payroll unavailable"])
                if not place.is_empty():_button("Map / Indicazioni",_route.bind(place))
            _label("Apply at the actual workplace with F.")
        "Bank":
            var bank=data.get("bank")
            if bank is Dictionary:
                _label("Savings / Risparmi: €%s\nBusiness: €%s\nLoan / Debito: €%s" % [bank.get("personal",0),bank.get("business",0),bank.get("debt",0)])
                _label("Cash operations at Offices, 09:00–17:00.")
                var office: Dictionary=data.get("bankLocation",{})
                if not office.is_empty():_button("Bank directions / Banca",_route.bind(office))
        "Meetings":
            if report.get("meetings",[]).is_empty():_label("No scheduled meetings.")
            for meeting in report.get("meetings",[]):
                var minute:=int(meeting.get("minute",0))
                _label("%s · Day %s %02d:%02d\n%s · %s" % [meeting.get("name",""),meeting.get("day",0),int(float(minute)/60.0),minute%60,meeting.get("place",""),meeting.get("status","")])

func _render_web() -> void:
    var web=data.get("digital")
    if not web is Dictionary:
        _label("Web portal unavailable / Portale non disponibile")
        return
    var telecom=web.get("telecom")
    if telecom is Dictionary:
        _label("Living Mobile · SIM: "+str(telecom.get("number",""))+" · Valid until day "+str(telecom.get("paidUntil",0)))
        _label("Network / Rete: "+str(telecom.get("status",""))+" · Equipment / Impianto: "+str(telecom.get("condition",0))+"%")
        for action in data.get("actions",[]):
            if str(action.get("id",""))=="phone-telecom-subscribe":_button(str(action.get("label","")),func():requested_action.emit("phone-telecom-subscribe"))
    if not bool(web.get("online",false)):
        _label("Network offline or subscription expired / Rete non disponibile o abbonamento scaduto")
        return
    _label("LIVING NET · In-game web / Rete interna")
    _label("Wallet / Portafoglio: €%s · Bills / Debiti: €%s" % [web.get("wallet",0),web.get("bills",0)])
    for action in data.get("actions",[]):
        if str(action.get("id",""))=="phone-digital-pay":_button(str(action.get("label","")),func():requested_action.emit("phone-digital-pay"))
    for site in web.get("sites",[]):
        _label(str(site.get("name",""))+" · "+str(site.get("status",""))+"\n"+str(site.get("description","")))
    _label("Job board / Offerte di lavoro")
    for job in web.get("jobs",[]):_label(str(job))
    _label("Apply in person / Candidati di persona")
    _label("Local news / Notizie locali")
    for notice in web.get("news",[]):_label("Day "+str(notice.get("day",0))+" · "+str(notice.get("text","")))
