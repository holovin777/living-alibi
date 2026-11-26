extends PanelContainer

signal close_requested
var snapshot: Dictionary={}
var tabs: TabContainer
var pages: Array[VBoxContainer]=[]
var heading: Label

func _ready() -> void:
    set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
    offset_left=24
    offset_top=24
    offset_right=-24
    offset_bottom=-24
    var root := VBoxContainer.new()
    root.add_theme_constant_override("separation",12)
    add_child(root)
    var header := HBoxContainer.new()
    root.add_child(header)
    heading=Label.new()
    heading.add_theme_font_size_override("font_size",24)
    heading.size_flags_horizontal=Control.SIZE_EXPAND_FILL
    header.add_child(heading)
    var close := Button.new()
    close.name="CloseCharacter"
    close.text=tr("Close (I / Esc)")
    close.pressed.connect(func():close_requested.emit())
    header.add_child(close)
    tabs=TabContainer.new()
    tabs.size_flags_vertical=Control.SIZE_EXPAND_FILL
    root.add_child(tabs)
    for title in ["Overview","Inventory","People","Controls"]:
        var scroll := ScrollContainer.new()
        scroll.name=title
        scroll.horizontal_scroll_mode=ScrollContainer.SCROLL_MODE_DISABLED
        tabs.add_child(scroll)
        var page := VBoxContainer.new()
        page.size_flags_horizontal=Control.SIZE_EXPAND_FILL
        page.add_theme_constant_override("separation",10)
        scroll.add_child(page)
        pages.append(page)
    refresh({})

func line(page: VBoxContainer, value: String, size: int=18) -> void:
    var label := Label.new()
    label.text=value
    label.autowrap_mode=TextServer.AUTOWRAP_WORD_SMART
    label.add_theme_font_size_override("font_size",size)
    page.add_child(label)

func section(page: VBoxContainer, value: String) -> void:
    line(page,value,22)

func need(page: VBoxContainer, caption: String, value: int, inverse: bool) -> void:
    # All bars represent wellbeing: full is good, regardless of Java's raw need direction.
    var wellbeing := 100-clampi(value,0,100) if inverse else clampi(value,0,100)
    var status := tr("Good") if wellbeing>=70 else (tr("Needs attention") if wellbeing>=35 else tr("Urgent"))
    line(page,"%s · %s%% · %s" % [tr(caption),wellbeing,status])
    var bar := ProgressBar.new()
    bar.max_value=100
    bar.value=wellbeing
    bar.show_percentage=false
    bar.custom_minimum_size=Vector2(0,12)
    var fill := StyleBoxFlat.new()
    fill.bg_color=Color("#59a77b") if wellbeing>=70 else (Color("#d4a14c") if wellbeing>=35 else Color("#c96861"))
    fill.corner_radius_top_left=4
    fill.corner_radius_bottom_left=4
    fill.corner_radius_top_right=4
    fill.corner_radius_bottom_right=4
    bar.add_theme_stylebox_override("fill",fill)
    page.add_child(bar)

func refresh(data: Dictionary) -> void:
    snapshot=data
    if not is_instance_valid(tabs):return
    heading.text=tr("My character")
    for i in range(pages.size()):
        tabs.set_tab_title(i,tr(["Overview","Inventory","People","Controls"][i]))
        for item in pages[i].get_children():
            pages[i].remove_child(item)
            item.queue_free()
    if data.is_empty():
        line(pages[0],tr("Connecting to the world..."))
        return
    var p: Dictionary=data.get("player",{})
    var life: Dictionary=data.get("rebirth",{})
    var identity=data.get("rebirth",{}).get("identity")
    var linked := str(life.get("phase","ADULT")) in ["CHILDHOOD","LINKED_ADULT"]
    var name_value=p.get("privateName")
    if linked:name_value=life.get("childName")
    var display_name := tr("Unnamed visitor") if name_value==null or str(name_value).is_empty() else str(name_value)
    if str(life.get("phase",""))=="CHOICE":
        section(pages[0],tr("Choose a new family"))
        line(pages[0],tr("Close this menu to choose your next life."))
        return
    section(pages[0],display_name)
    if linked:line(pages[0],tr("Age: %s") % life.get("age",0))
    var occupation=p.get("jobId")
    if linked and life.get("adultOptions") is Dictionary:occupation=life["adultOptions"].get("occupation")
    elif linked:occupation=tr("Student" if int(life.get("age",0))>=6 else "Child")
    line(pages[0],tr("Location: %s") % str(p.get("place",p.get("placeId",""))))
    line(pages[0],tr("Occupation: %s") % (tr("Not employed") if occupation==null or str(occupation).is_empty() else str(occupation)))
    line(pages[0],tr("Money: %s EUR") % (identity.get("savings",p.get("money",0)) if linked and identity is Dictionary else p.get("money",0)))
    section(pages[0],tr("Wellbeing — full bars are good"))
    var stats: Dictionary=data.get("characterNeeds",{} if linked else p)
    if stats.is_empty():
        line(pages[0],tr("Current wellbeing is unavailable."))
    else:
        need(pages[0],"Fullness",int(stats.get("hunger",0)),true)
        need(pages[0],"Hydration",int(stats.get("thirst",0)),true)
        need(pages[0],"Energy",int(stats.get("fatigue",0)),true)
        need(pages[0],"Health",int(stats.get("health",100)),false)
        need(pages[0],"Hygiene",int(stats.get("hygiene",100)),false)
    section(pages[0],tr("What can I do now?"))
    if linked:
        line(pages[0],tr("F: life choices, education, work and family. E: nearby services."))
    else:
        if int(p.get("thirst",0))>=65:line(pages[0],tr("Drink water from your bag or find a drinking source."))
        if int(p.get("hunger",0))>=65:line(pages[0],tr("Eat food from your bag or visit a shop."))
        if int(p.get("fatigue",0))>=65:line(pages[0],tr("Find a place to rest."))
        if occupation==null:line(pages[0],tr("H: phone and job listings. Follow First steps to find work."))
        line(pages[0],tr("Explore the town, talk to residents and find local services."))
    section(pages[1],tr("Items carried"))
    if linked:
        line(pages[1],tr("Your household supplies are available in F: life choices."))
    else:
        var bag: Dictionary=p.get("bag",{"bread":p.get("bread",0),"water":p.get("bottledWater",0)})
        var count := 0
        for item in bag:
            var quantity := int(bag[item])
            if quantity>0:
                count+=quantity
                line(pages[1],"%s × %s" % [tr(str(item).replace("-"," ").capitalize()),quantity])
        line(pages[1],tr("Bag: %s / 12") % count)
        if count==0:line(pages[1],tr("Your bag is empty."))
    section(pages[2],tr("Known people"))
    var known: Array=identity.get("relationships",[]) if linked and identity is Dictionary else data.get("contacts",[])
    if known.is_empty():line(pages[2],tr("No known people yet. Talk to residents to make contacts."))
    for person in known:
        if not person is Dictionary:continue
        var person_name=person.get("name")
        if person_name==null or str(person_name).is_empty():continue
        var details := str(person_name)
        if person.has("bond"):details+=" · "+tr(str(person["bond"]))
        if not bool(person.get("alive",true)):details+=" · "+tr("Deceased")
        line(pages[2],details)
        if int(person.get("sharedChildhoodDays",0))>0:
            line(pages[2],tr("Shared school breaks: %s") % person["sharedChildhoodDays"],16)
    for row in ["WASD: move","Mouse: look around","Space: jump","Shift: run · Ctrl: crouch","T: speak to a nearby person","E: nearby interaction or travel","F: local services / life choices","H: phone · M: town map","Q: cycle held item","I: character menu · Esc: close"]:
        line(pages[3],tr(row))
