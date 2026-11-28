extends Node3D
## Independent geographic preview. Does not modify the Java save.
var city: Dictionary = {}
var chunks: Dictionary = {}
var chunk_size := 128.0
var loaded_cell := Vector2i(999999,999999)
var walker: CharacterBody3D
var head: Node3D
var hud: Label
var wall_material: StandardMaterial3D
var roof_material: StandardMaterial3D
const Architecture=preload("res://scripts/GeoArchitecture.gd")
var detail_materials: Dictionary={}
var daylight: DirectionalLight3D
var sky_environment: Environment
const WeatherVisual=preload("res://scripts/GeoWeather.gd")
var precipitation: Node3D
var lamp_pool: Array[OmniLight3D]=[]
const ACTIVE_LAMPS=8
var road_material: Material
var rail_material: StandardMaterial3D
var ready_to_walk := false
const RoadGraph=preload("res://scripts/GeoRoadGraph.gd")
const CityMap=preload("res://scripts/GeoCityMap.gd")
var roads=RoadGraph.new()
var city_map: Control
var route_status := ""
var street_timer := 0.0
var street_name := ""
var route_destination := Vector2.ZERO
var has_destination := false
const Traffic=preload("res://scripts/GeoTraffic.gd")
var traffic: Node3D
const Residents=preload("res://scripts/GeoResidents.gd")
var residents: Node3D
var chat: LineEdit
var chat_target:=""
var shared_mode:=false
var district_sign: Label3D
var entrance_signs: Dictionary={}
var actions_panel: PanelContainer
const PhonePanel=preload("res://scripts/GeoPhone.gd")
const WorkPanel=preload("res://scripts/GeoWorkPanel.gd")
const PropertyPanel=preload("res://scripts/GeoPropertyPanel.gd")
const PropertyScenery=preload("res://scripts/GeoPropertyScenery.gd")
var property_scenery: Node3D
var phone_panel: PanelContainer
var action_list: VBoxContainer
var owned_car: Node3D
var parked_bike: Node3D
var civilian_car: Node3D
var civilian_id:=""
var patrol_models: Dictionary={}
var patrol_goals: Dictionary={}
var commute_models: Dictionary={}
var commute_goals: Dictionary={}
var driving_body: Node3D
var drive_goal:=Vector3.ZERO
var driving:=false
var drive_timer:=0.0
var impact_flash:=0.0
var impact_overlay: ColorRect
const InteriorScene=preload("res://scripts/GeoInterior.gd")
var interior_node: Node3D
var room: Dictionary={}
var room_exit:=Vector2.ZERO
var linked_age:=-1

func _ready() -> void:
    var map_path := ProjectSettings.globalize_path("res://../data/vigevano.json")
    for arg in OS.get_cmdline_user_args():
        if arg.begins_with("--city-map="):map_path=arg.trim_prefix("--city-map=")
    var canvas := CanvasLayer.new()
    add_child(canvas)
    hud=Label.new()
    hud.position=Vector2(12,12)
    canvas.add_child(hud)
    var credit := Label.new()
    credit.text="© OpenStreetMap contributors · openstreetmap.org/copyright"
    credit.set_anchors_and_offsets_preset(Control.PRESET_BOTTOM_LEFT)
    credit.position.y=-30
    canvas.add_child(credit)
    if not FileAccess.file_exists(map_path):
        hud.text="Map missing. Run bash play-vigevano.sh to import real OSM data."
        return
    var parsed = JSON.parse_string(FileAccess.get_file_as_string(map_path))
    if not parsed is Dictionary or int(parsed.get("schema",0))!=2 or not parsed.get("chunks") is Dictionary:
        hud.text="Invalid geographic map. Re-run the importer."
        return
    city=parsed
    property_scenery=PropertyScenery.new()
    add_child(property_scenery)
    property_scenery.configure(city)
    roads.build(city.get("graph",{}))
    traffic=Traffic.new()
    add_child(traffic)
    traffic.configure(city.get("graph",{}))
    residents=Residents.new()
    add_child(residents)
    shared_mode="--geo-world" in OS.get_cmdline_user_args()
    residents.configure(traffic,shared_mode)
    traffic.set_java_population(shared_mode)
    residents.first_snapshot.connect(_sync_spawn)
    residents.interior_changed.connect(_change_interior)
    district_sign=Label3D.new()
    district_sign.billboard=BaseMaterial3D.BILLBOARD_ENABLED
    district_sign.font_size=42
    district_sign.pixel_size=0.012
    district_sign.modulate=Color("#f2d38a")
    district_sign.hide()
    add_child(district_sign)
    chat=LineEdit.new()
    canvas.add_child(chat)
    chat.position=Vector2(12,145)
    chat.custom_minimum_size=Vector2(430,36)
    chat.placeholder_text="Scrivi / Write a message, then Enter"
    chat.max_length=400
    chat.text_submitted.connect(_send_chat)
    chat.hide()
    actions_panel=PanelContainer.new()
    canvas.add_child(actions_panel)
    actions_panel.position=Vector2(12,190)
    actions_panel.custom_minimum_size=Vector2(410,0)
    var action_scroll:=ScrollContainer.new()
    action_scroll.custom_minimum_size=Vector2(410,320)
    actions_panel.add_child(action_scroll)
    action_list=VBoxContainer.new()
    action_list.size_flags_horizontal=Control.SIZE_EXPAND_FILL
    action_scroll.add_child(action_list)
    actions_panel.hide()
    phone_panel=PhonePanel.new()
    canvas.add_child(phone_panel)
    phone_panel.set_anchors_and_offsets_preset(Control.PRESET_CENTER_RIGHT)
    phone_panel.position=Vector2(maxf(12,get_viewport().get_visible_rect().size.x-350),65)
    phone_panel.requested_action.connect(_phone_action)
    phone_panel.destination_selected.connect(_phone_route)
    phone_panel.closed.connect(_close_phone)
    owned_car=traffic._car()
    add_child(owned_car)
    owned_car.hide()
    civilian_car=traffic._car()
    add_child(civilian_car)
    civilian_car.hide()
    driving_body=traffic._car(Color("#55585a"),false)
    add_child(driving_body)
    driving_body.hide()
    impact_overlay=ColorRect.new()
    canvas.add_child(impact_overlay)
    impact_overlay.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
    impact_overlay.mouse_filter=Control.MOUSE_FILTER_IGNORE
    impact_overlay.color=Color(0.8,0.15,0.05,0)
    residents.drive_pose.connect(_drive_pose)
    parked_bike=Node3D.new()
    add_child(parked_bike)
    _build_bicycle_marker(parked_bike)
    parked_bike.hide()
    city_map=CityMap.new()
    canvas.add_child(city_map)
    city_map.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
    city_map.offset_left=16
    city_map.offset_top=65
    city_map.offset_right=-16
    city_map.offset_bottom=-42
    city_map.configure(city.get("graph",{}))
    city_map.destination_selected.connect(_select_destination)
    city_map.hide()
    chunk_size=float(city.get("chunk_size",128))
    wall_material=_material(Color("#cbb49b"))
    roof_material=_material(Color("#925f4d"))
    var asphalt:=ShaderMaterial.new()
    asphalt.shader=preload("res://shaders/GeoAsphalt.gdshader")
    road_material=asphalt
    rail_material=_material(Color("#8f8578"))
    var world_environment := WorldEnvironment.new()
    var environment := Environment.new()
    environment.background_mode=Environment.BG_COLOR
    environment.background_color=Color("#a7c7de")
    environment.ambient_light_source=Environment.AMBIENT_SOURCE_COLOR
    environment.ambient_light_color=Color("#ffffff")
    environment.ambient_light_energy=0.65
    world_environment.environment=environment
    sky_environment=environment
    add_child(world_environment)
    var sun := DirectionalLight3D.new()
    sun.rotation_degrees=Vector3(-55,-35,0)
    sun.light_energy=0.85
    # Optional bounded shadows for the discrete GPU; keep the low-end default light.
    sun.shadow_enabled=OS.get_environment("LIVING_ALIBI_SHADOWS")=="1"
    sun.directional_shadow_max_distance=120.0
    add_child(sun)
    daylight=sun
    precipitation=WeatherVisual.new()
    add_child(precipitation)
    for i in range(ACTIVE_LAMPS):
        var light:=OmniLight3D.new()
        light.light_color=Color("#ffd49b")
        light.light_energy=1.65
        light.omni_range=9.0
        light.shadow_enabled=false
        light.hide()
        add_child(light)
        lamp_pool.append(light)
    var ground := StaticBody3D.new()
    add_child(ground)
    var plane := CollisionShape3D.new()
    plane.shape=WorldBoundaryShape3D.new()
    ground.add_child(plane)
    var ground_visual := MeshInstance3D.new()
    var floor_mesh := PlaneMesh.new()
    var extent := Vector2(2500,2500)
    for raw in city.get("graph",{}).get("nodes",{}).values():
        var point: Array=raw
        extent=extent.max(Vector2(absf(float(point[0])),absf(float(point[1])))+Vector2(512,512))
    floor_mesh.size=extent*2.0
    ground_visual.mesh=floor_mesh
    ground_visual.material_override=_material(Color("#a4a98c"))
    add_child(ground_visual)
    walker=CharacterBody3D.new()
    var capsule := CapsuleShape3D.new()
    capsule.radius=0.32
    capsule.height=1.8
    var collider := CollisionShape3D.new()
    collider.shape=capsule
    walker.add_child(collider)
    head=Node3D.new()
    head.position.y=0.65
    walker.add_child(head)
    var camera := Camera3D.new()
    camera.far=500
    head.add_child(camera)
    camera.current=true
    add_child(walker)
    var spawn: Array=city.get("spawn",[0,0])
    walker.position=Vector3(float(spawn[0]),1.05,float(spawn[1]))
    _stream()
    ready_to_walk=true
    print("VigevanoPreview: loaded ",city.get("counts",{})," active chunks=",chunks.size())

func _material(color: Color) -> StandardMaterial3D:
    var mat := StandardMaterial3D.new()
    mat.albedo_color=color
    mat.cull_mode=BaseMaterial3D.CULL_DISABLED
    mat.roughness=0.9
    return mat

func _input(event: InputEvent) -> void:
    if not ready_to_walk:return
    if phone_panel.visible:
        if event is InputEventKey and event.pressed and event.keycode in [KEY_P,KEY_ESCAPE]:
            _close_phone()
            get_viewport().set_input_as_handled()
        return
    if actions_panel.visible:
        if event is InputEventKey and event.pressed and (event.keycode==KEY_ESCAPE or (event.keycode in [KEY_F,KEY_I] and not get_viewport().gui_get_focus_owner() is TextEdit)):
            actions_panel.hide()
            Input.mouse_mode=Input.MOUSE_MODE_CAPTURED
            get_viewport().set_input_as_handled()
        return
    if chat.visible:
        if event is InputEventKey and event.pressed and event.keycode==KEY_ESCAPE:
            chat.hide()
            Input.mouse_mode=Input.MOUSE_MODE_CAPTURED
            get_viewport().set_input_as_handled()
        return
    if event is InputEventKey and event.pressed and not event.echo and not city_map.visible:
        var pos:=Vector2(walker.position.x,walker.position.z)
        if driving:
            if event.keycode==KEY_G:
                residents.drive_command("exit",pos)
            return
        if event.keycode==KEY_G and residents.connected and room.is_empty() and not bool(residents.world.get("rebirth",false)):
            if owned_car.visible and walker.position.distance_to(owned_car.position)<5:
                residents.drive_command("start",pos)
            elif civilian_car.visible and walker.position.distance_to(civilian_car.position)<5:
                residents.drive_command("steal",pos)
            return
        if event.keycode==KEY_P and residents.connected:
            phone_panel.open_phone(residents.world)
            Input.mouse_mode=Input.MOUSE_MODE_VISIBLE
            get_viewport().set_input_as_handled()
            return
        if event.keycode==KEY_I and residents.connected:
            _open_identity()
            get_viewport().set_input_as_handled()
            return
        if event.keycode==KEY_F and residents.connected:
            _open_actions()
            get_viewport().set_input_as_handled()
            return
        if event.keycode==KEY_E:
            if _near_office_computer() and residents.world.get("work",{}).get("workspace") is Dictionary:
                _open_actions(true)
                get_viewport().set_input_as_handled()
                return
            residents.enter_district(pos)
            return
        elif event.keycode==KEY_T:
            chat_target=residents.nearby_talker(pos)
            if not chat_target.is_empty():
                chat.clear()
                chat.show()
                Input.mouse_mode=Input.MOUSE_MODE_VISIBLE
                chat.grab_focus()
                get_viewport().set_input_as_handled()
                return
    if not room.is_empty() and event is InputEventKey and event.keycode==KEY_M:return
    if event is InputEventKey and event.pressed and not event.echo and (event.keycode==KEY_M or (event.keycode==KEY_ESCAPE and city_map.visible)):
        city_map.visible=not city_map.visible
        Input.mouse_mode=Input.MOUSE_MODE_VISIBLE if city_map.visible else Input.MOUSE_MODE_CAPTURED
        city_map.queue_redraw()
        get_viewport().set_input_as_handled()
        return
    if city_map.visible:return
    if event is InputEventMouseButton and event.pressed and event.button_index==MOUSE_BUTTON_LEFT:
        Input.mouse_mode=Input.MOUSE_MODE_CAPTURED
    if event is InputEventKey and event.pressed and event.keycode==KEY_ESCAPE:
        Input.mouse_mode=Input.MOUSE_MODE_VISIBLE
    if event is InputEventMouseMotion and Input.mouse_mode==Input.MOUSE_MODE_CAPTURED and not driving:
        walker.rotate_y(-event.relative.x*0.0025)
        head.rotation.x=clampf(head.rotation.x-event.relative.y*0.0025,-1.45,1.45)

func _physics_process(delta: float) -> void:
    if not ready_to_walk:return
    var direction := Vector3.ZERO
    traffic.update_traffic(Vector2(walker.position.x,walker.position.z),delta)
    property_scenery.animate(delta)
    _animate_patrols(delta)
    _animate_commutes(delta)
    impact_flash=maxf(0.0,impact_flash-delta*2.0)
    impact_overlay.color=Color(0.8,0.15,0.05,impact_flash*0.32)
    residents.update_residents(Vector2(walker.position.x,walker.position.z),delta)
    precipitation.update_weather(residents.world.get("weather",{}),walker.position,not room.is_empty(),delta)
    if phone_panel.visible:phone_panel.refresh(residents.world,residents.last_message,residents.action_pending)
    var life: Dictionary=residents.world.get("life",{})
    var linked:=bool(residents.world.get("rebirth",false))
    if not actions_panel.visible and not residents.action_pending and linked and str(life.get("phase",""))=="CHOICE":
        for action in residents.world.get("actions",[]):
            if str(action.get("id","")) in ["rebirth-first-boy","rebirth-return"]:
                _open_actions()
                break
    var age:=int(life.get("age",0)) if linked else -1
    if age!=linked_age:
        linked_age=age
        head.position.y=0.18 if age>=0 and age<6 else (0.35 if age>=6 and age<13 else (0.52 if age>=13 and age<18 else 0.65))
    if driving:
        _drive_frame(delta)
        return
    if city_map.visible or chat.visible or actions_panel.visible or phone_panel.visible or residents.action_pending or (shared_mode and (not residents.connected or bool(residents.world.get("confined",false)) or (linked and not bool(life.get("canWalk",false))))):
        walker.velocity.x=0
        walker.velocity.z=0
        street_timer+=delta
        if street_timer>=0.5:
            street_timer=0
            _update_navigation()
        return
    if Input.is_physical_key_pressed(KEY_W) or Input.is_physical_key_pressed(KEY_UP):direction.z-=1
    if Input.is_physical_key_pressed(KEY_S) or Input.is_physical_key_pressed(KEY_DOWN):direction.z+=1
    if Input.is_physical_key_pressed(KEY_A) or Input.is_physical_key_pressed(KEY_LEFT):direction.x-=1
    if Input.is_physical_key_pressed(KEY_D) or Input.is_physical_key_pressed(KEY_RIGHT):direction.x+=1
    direction=walker.basis*direction.normalized()
    var cycling:=room.is_empty() and bool(residents.world.get("cycling",false))
    var speed := 8.0 if cycling else (7.0 if Input.is_physical_key_pressed(KEY_SHIFT) else 4.5)
    if linked:
        speed*=0.72 if age<13 else (0.88 if age<18 else 1.0)
        if room.is_empty():
            var district: Dictionary=residents.world.get("playerDistrict",{})
            if not district.is_empty():
                var center:=Vector2(float(district["x"]),float(district["z"]))
                var current:=Vector2(walker.position.x,walker.position.z)
                var predicted:=current+Vector2(direction.x,direction.z)*speed*delta
                if predicted.distance_to(center)>float(life.get("radius",24)) and predicted.distance_to(center)>current.distance_to(center):direction=Vector3.ZERO
    walker.velocity.x=direction.x*speed
    walker.velocity.z=direction.z*speed
    if not walker.is_on_floor():walker.velocity.y-=9.8*delta
    elif not cycling and Input.is_physical_key_pressed(KEY_SPACE):walker.velocity.y=4.3
    else:walker.velocity.y=0
    walker.move_and_slide()
    _stream()
    street_timer+=delta
    if street_timer>=0.5:
        street_timer=0
        _update_navigation()

func _stream() -> void:
    if not room.is_empty():return
    var cell := Vector2i(floori(walker.position.x/chunk_size),floori(walker.position.z/chunk_size))
    if cell==loaded_cell:return
    loaded_cell=cell
    var wanted: Dictionary={}
    for y in range(cell.y-1,cell.y+2):
        for x in range(cell.x-1,cell.x+2):
            var key := "%s,%s" % [x,y]
            if city["chunks"].has(key):wanted[key]=true
    for key in chunks.keys():
        if not wanted.has(key):
            var old: Node3D=chunks[key]
            remove_child(old)
            old.queue_free()
            chunks.erase(key)
    for key in wanted:
        if not chunks.has(key):_build_chunk(key,city["chunks"][key])
    _update_navigation()

func _select_destination(point: Vector2) -> void:
    route_destination=point
    has_destination=true
    _update_navigation()

func _sync_entrance_signs(entrances: Array,pos: Vector2) -> void:
    var seen: Dictionary={}
    for door in entrances:
        var id:=str(door.get("id",""))
        var at:=Vector2(float(door["x"]),float(door["z"]))
        if at.distance_to(pos)>55:continue
        seen[id]=true
        if not entrance_signs.has(id):
            var sign:=Label3D.new()
            sign.font_size=40
            sign.pixel_size=0.008
            sign.billboard=BaseMaterial3D.BILLBOARD_ENABLED
            sign.modulate=Color("#a5d9ee")
            add_child(sign)
            entrance_signs[id]=sign
        var label: Label3D=entrance_signs[id]
        label.position=Vector3(at.x,1.7+0.42*(int(id)-1),at.y)
        label.text=str(door.get("label",""))+" · CITOFONO / INTERCOM"
        if at.distance_to(pos)<=4:label.text+=" · F"
    for id in entrance_signs.keys():
        if not seen.has(id):
            entrance_signs[id].queue_free()
            entrance_signs.erase(id)

func _update_navigation() -> void:
    if not is_instance_valid(walker) or not is_instance_valid(city_map):return
    _update_daylight()
    property_scenery.sync(residents.world.get("propertyScenery",{}) if room.is_empty() and residents.connected else {})
    var pos:=Vector2(walker.position.x,walker.position.z)
    _sync_entrance_signs(residents.world.get("entrances",[]) if room.is_empty() and residents.connected else [],pos)
    if not room.is_empty():
        if is_instance_valid(interior_node):interior_node.update_work_board(residents.world.get("work",{}))
        hud.text=str(room.get("label",""))+" · INTERIOR / INTERNO\nWASD · E near exit · T talk · F actions · P phone\nDay %s · %02d:%02d · inside %s · €%s" % [residents.day,int(residents.minute/60),residents.minute%60,residents.people.size(),residents.world.get("money",0)]
        if bool(residents.world.get("rebirth",false)):hud.text+="\n"+_life_summary()+" · F: life actions · E: leave home"
        if _near_office_computer() and residents.world.get("work",{}).get("workspace") is Dictionary:hud.text+="\nBorgo Digital · E: use computer"
        if not residents.last_message.is_empty():hud.text+="\n"+residents.last_message
        return
    city_map.player=pos
    street_name=roads.street(pos)
    route_status=""
    if has_destination:
        var path: PackedVector2Array=roads.route(pos,route_destination)
        city_map.route=path
        if path.is_empty():
            route_status="No connected walking route"
        else:
            route_status="Walking route: %.0f m along streets" % RoadGraph.length(path)
    city_map.queue_redraw()
    hud.text="VIGEVANO · "+(street_name if not street_name.is_empty() else "Geographic preview")+"\nWASD · Space jump · M map · Esc cursor | chunks %s/9" % chunks.size()
    if residents.connected:
        var world: Dictionary=residents.world
        var district: Dictionary=world.get("playerDistrict",{})
        var nearest: Dictionary=world.get("nearestDistrict",{})
        hud.text+="\nDay %s · %02d:%02d · residents %s · nearby %s · €%s · health %s" % [residents.day,int(residents.minute/60),residents.minute%60,residents.population,residents.people.size(),world.get("money",0),world.get("health",0)]
        hud.text+="\nCurrent: "+str(district.get("label",""))+" · E: enter "+str(nearest.get("label",""))+" · T: talk · F: actions · P: phone"
        for door in world.get("entrances",[]):
            if pos.distance_to(Vector2(float(door["x"]),float(door["z"])))<=4:
                var expected: String="SOUTH_RESIDENCES" if str(door["id"])=="3" else "NORTH_RESIDENCES"
                hud.text+="\n"+str(door["label"])+" · "+("F: known contacts / Citofono" if str(district.get("id",""))==expected else "E at residential district first / Entra nel quartiere")
        if bool(world.get("confined",false)):hud.text+="\nOpen regular game to continue custody."
        if bool(world.get("rebirth",false)):hud.text+="\n"+_life_summary()+" · Local walking · F: travel/life"
        var phone: Dictionary=world.get("phone",{})
        var report: Dictionary=phone.get("report",{})
        for call in report.get("calls",[]):
            if str(call.get("status",""))=="RINGING":
                hud.text+="\nIncoming call: "+str(call.get("name",""))+" · P to answer"
                break
        var weather: Dictionary=world.get("weather",{})
        hud.text+="\n%s · %s · %s°C" % [weather.get("season",""),weather.get("sky",""),weather.get("temperatureC","—")]
        hud.text+="\nHunger %s · Thirst %s · %s" % [world.get("hunger",0),world.get("thirst",0),"Cycling" if world.get("cycling",false) else "Walking"]
        if residents.action_pending:hud.text+="\nJava is processing your action..."
        if not residents.last_message.is_empty():hud.text+="\n"+residents.last_message
        _sync_parked_vehicle(owned_car,world.get("parkedCar"),pos,Vector3(2.5,0.05,0))
        if driving:owned_car.hide()
        _sync_civilian(world.get("civilianCar"),pos)
        _sync_patrols(world.get("patrols",[]))
        _sync_commutes(world.get("traffic",[]))
        hud.text+="\nJava traffic: %s" % commute_models.size()
        if property_scenery.missing_yard or property_scenery.missing_site:hud.text+="\nPlot visual unavailable: no clear mapped space. F actions still work."
        if driving:civilian_car.hide()
        hud.text+="\nG: enter own car / steal nearby civilian car"
        if driving:hud.text+="\nW: accelerate · A/D: choose turn · G: exit | Fuel %s · Condition %s" % [residents.driving.get("fuel",0),residents.driving.get("condition",0)]
        var justice: Dictionary=world.get("justice",{})
        if str(justice.get("status",""))=="REPORTED":hud.text+="\nPolice report · possible detention in %s game min" % justice.get("minutesRemaining",0)
        _sync_parked_vehicle(parked_bike,world.get("parkedBicycle"),pos,Vector3(-2,0.05,0))
        if not nearest.is_empty():
            district_sign.position=Vector3(float(nearest["x"]),2.5,float(nearest["z"]))
            district_sign.text=str(nearest.get("label",""))+" · E"
            district_sign.show()
    elif shared_mode:
        district_sign.hide()
        owned_car.hide()
        civilian_car.hide()
        _sync_patrols([])
        _sync_commutes([])
        parked_bike.hide()
        hud.text+="\nConnecting to shared Java world..."
    else:
        hud.text+="\nLocal traffic: %s cars · %s pedestrians (preview)" % [traffic.count("drive"),traffic.count("walk")]
    if not route_status.is_empty():hud.text+="\n"+route_status

func _drive_pose(pose: Dictionary) -> void:
    if str(pose.get("message","")).begins_with("Collision:"):impact_flash=1.0
    var was_driving:=driving
    driving=bool(pose.get("active",false))
    driving_body.visible=driving
    if driving:
        drive_goal=Vector3(float(pose["x"]),1.05,float(pose["z"]))
        walker.rotation.y=float(pose.get("heading",0))
        head.rotation.x=0
        if not was_driving:
            walker.position=drive_goal
            owned_car.hide()
            civilian_car.hide()
    elif was_driving:
        walker.position=Vector3(float(pose["x"]),1.05,float(pose["z"]))
        walker.velocity=Vector3.ZERO
        _stream()

func _drive_frame(delta: float) -> void:
    if not residents.connected or bool(residents.world.get("confined",false)) or bool(residents.world.get("rebirth",false)):
        if not residents.connected:return # freeze until authoritative state is available again
        driving=false
        driving_body.hide()
        var location: Dictionary=residents.world.get("playerDistrict",{})
        if not location.is_empty():_sync_spawn(Vector2(float(location["x"]),float(location["z"])))
        return
    walker.position=walker.position.move_toward(drive_goal,delta*12.0)
    driving_body.position=walker.position-Vector3(0,1.0,0)
    driving_body.rotation.y=walker.rotation.y
    _stream()
    drive_timer+=delta
    if drive_timer>=0.65:
        drive_timer=0
        if Input.is_physical_key_pressed(KEY_W) and not Input.is_physical_key_pressed(KEY_SPACE):
            var turn:=0
            if Input.is_physical_key_pressed(KEY_A):turn=-1
            elif Input.is_physical_key_pressed(KEY_D):turn=1
            residents.drive_command("step",Vector2(walker.position.x,walker.position.z),turn)
        _update_navigation()

func _sync_civilian(record,pos: Vector2) -> void:
    if not record is Dictionary:
        civilian_car.hide()
        return
    var id:=str(record.get("id",""))
    if id!=civilian_id:
        civilian_id=id
        civilian_car.queue_free()
        var palette: Array[Color]=[Color("#9b403d"),Color("#456b89"),Color("#d0ccc0"),Color("#64696b"),Color("#73805a")]
        civilian_car=traffic._car(palette[posmod(id.hash(),palette.size())])
        add_child(civilian_car)
    _sync_parked_vehicle(civilian_car,record,pos,Vector3(0,0.05,0))
    civilian_car.rotation.y=float(record.get("heading",0))

func _sync_commutes(records: Array) -> void:
    var seen: Dictionary={}
    for record in records:
        var id:=str(record.get("id",""))
        var goal:=Vector3(float(record["x"]),0.05,float(record["z"]))
        seen[id]=true
        if not commute_models.has(id):
            var car: Node3D
            if str(record.get("kind",""))=="BUS":
                car=Node3D.new()
                traffic._box(car,Vector3(2.2,2.2,6.4),Vector3(0,1.3,0),Color("#e3ad45"))
                traffic._box(car,Vector3(2.24,0.8,5.8),Vector3(0,1.9,0),Color("#405767"))
                traffic._box(car,Vector3(2.25,0.25,6.5),Vector3(0,2.45,0),Color("#e3ad45"))
            else:
                var palette: Array[Color]=[Color("#9b403d"),Color("#456b89"),Color("#d0ccc0"),Color("#64696b"),Color("#73805a")]
                car=traffic._car(palette[posmod(id.hash(),palette.size())])
            add_child(car)
            car.position=goal
            commute_models[id]=car
        commute_goals[id]={"point":goal,"heading":float(record.get("heading",0))}
    for id in commute_models.keys():
        if not seen.has(id):
            commute_models[id].queue_free()
            commute_models.erase(id)
            commute_goals.erase(id)

func _animate_commutes(delta: float) -> void:
    for id in commute_models:
        var car: Node3D=commute_models[id]
        var goal: Dictionary=commute_goals[id]
        car.position=car.position.move_toward(goal["point"],delta*12.0)
        car.rotation.y=lerp_angle(car.rotation.y,float(goal["heading"]),minf(1.0,delta*5.0))

func _sync_patrols(records: Array) -> void:
    var seen: Dictionary={}
    for record in records:
        var id:=str(record.get("id",""))
        var goal:=Vector3(float(record["x"]),0.05,float(record["z"]))
        seen[id]=true
        if not patrol_models.has(id):
            var car: Node3D=traffic._car(Color("#254a84"),false)
            add_child(car)
            traffic._box(car,Vector3(1.60,0.13,2.6),Vector3(0,0.96,0),Color("#d9e3ec"))
            var lamp: MeshInstance3D=traffic._box(car,Vector3(0.9,0.17,0.26),Vector3(0,1.61,0),Color("#3688ee"))
            lamp.name="Beacon"
            car.position=goal
            patrol_models[id]=car
        patrol_goals[id]={"point":goal,"heading":float(record.get("heading",0)),"responding":bool(record.get("responding",false))}
    for id in patrol_models.keys():
        if not seen.has(id):
            patrol_models[id].queue_free()
            patrol_models.erase(id)
            patrol_goals.erase(id)

func _animate_patrols(delta: float) -> void:
    for id in patrol_models:
        var car: Node3D=patrol_models[id]
        var goal: Dictionary=patrol_goals[id]
        car.position=car.position.move_toward(goal["point"],delta*9.0)
        car.rotation.y=lerp_angle(car.rotation.y,float(goal["heading"]),minf(1.0,delta*5.0))
        car.get_node("Beacon").visible=not bool(goal["responding"]) or (Time.get_ticks_msec()/250)%2==0

func _build_bicycle_marker(node: Node3D) -> void:
    # Lightweight parked bicycle silhouette, not a physics vehicle.
    for z in [-0.55,0.55]:
        var wheel:=MeshInstance3D.new()
        var mesh:=TorusMesh.new()
        mesh.inner_radius=0.25
        mesh.outer_radius=0.32
        wheel.mesh=mesh
        wheel.rotation.z=PI/2
        wheel.position=Vector3(0,0.33,z)
        node.add_child(wheel)
    traffic._box(node,Vector3(0.10,0.12,1.0),Vector3(0,0.63,0),Color("#50705e"))
    traffic._box(node,Vector3(0.3,0.08,0.25),Vector3(0,0.90,0.25),Color("#353535"))

func _sync_parked_vehicle(node: Node3D,record,pos: Vector2,offset: Vector3) -> void:
    if not record is Dictionary:
        node.hide()
        return
    var location:=Vector2(float(record["x"]),float(record["z"]))
    node.visible=pos.distance_to(location)<=110
    node.position=Vector3(location.x,0,location.y)+offset

func _close_phone() -> void:
    phone_panel.hide()
    Input.mouse_mode=Input.MOUSE_MODE_CAPTURED

func _phone_action(id: String) -> void:
    residents.perform(id,Vector2(walker.position.x,walker.position.z))

func _phone_route(point: Vector2) -> void:
    _close_phone()
    _select_destination(point)
    if room.is_empty():
        city_map.show()
        Input.mouse_mode=Input.MOUSE_MODE_VISIBLE
        city_map.queue_redraw()

func _life_summary() -> String:
    var life: Dictionary=residents.world.get("life",{})
    if str(life.get("phase",""))=="CHOICE":return "Awaiting a new life / In attesa di una nuova vita"
    return "%s · age %s · %s · school days %s" % [life.get("name",""),life.get("age",0),life.get("activity",""),life.get("schoolDays",0)]

func _near_office_computer() -> bool:
    if str(room.get("id",""))!="OFFICE":return false
    var at:=Vector2(walker.position.x,walker.position.z)-room_exit
    return minf(at.distance_to(Vector2(-4,4.1)),at.distance_to(Vector2(4,4.1)))<=2.0

func _open_identity() -> void:
    for child in action_list.get_children():
        action_list.remove_child(child)
        child.queue_free()
    var heading:=Label.new()
    heading.text="CARTA D'IDENTITÀ / IDENTITY"
    action_list.add_child(heading)
    var passport=residents.world.get("life",{}).get("passport")
    var info:=Label.new()
    info.autowrap_mode=TextServer.AUTOWRAP_WORD_SMART
    if passport is Dictionary:
        var birth:="Unknown / Sconosciuta"
        if passport.get("birthYear")!=null:
            birth="World year / Anno %s · day / giorno %s" % [passport.get("birthYear"),passport.get("birthDayOfYear")]
        var age:=int(passport.get("age",-1))
        info.text="Name / Nome: %s\nAge / Età: %s\nBirth / Nascita: %s\nBirthplace / Luogo: %s\nLife / Vita: #%s" % [passport.get("name",""),str(age) if age>=0 else "—",birth,passport.get("birthplace",""),passport.get("incarnation",0)]
        var occupation=passport.get("occupation")
        var residence=passport.get("residence")
        info.text+="\nOccupation / Professione: "+("Unemployed / Disoccupato" if occupation==null else str(occupation))
        info.text+="\nResidence / Residenza: "+("No fixed home / Senza dimora fissa" if residence==null else str(residence))
        if passport.get("alias")!=null:info.text+="\nAlias: "+str(passport.get("alias"))
        if passport.get("parentA")!=null:info.text+="\nParent / Genitore: "+str(passport.get("parentA"))
        if passport.get("parentB")!=null:info.text+="\nParent / Genitore: "+str(passport.get("parentB"))
        info.text+="\n\nPrivate identity / Identità privata"
    else:
        info.text="Awaiting birth. / In attesa della nascita."
    action_list.add_child(info)
    var close:=Button.new()
    close.text="Close / Chiudi"
    close.pressed.connect(func():
        actions_panel.hide()
        Input.mouse_mode=Input.MOUSE_MODE_CAPTURED)
    action_list.add_child(close)
    actions_panel.show()
    Input.mouse_mode=Input.MOUSE_MODE_VISIBLE

func _open_actions(work_only: bool=false) -> void:
    for child in action_list.get_children():
        action_list.remove_child(child)
        child.queue_free()
    var title:=Label.new()
    title.text="Borgo Digital · Workstation" if work_only else ("Life / Vita" if bool(residents.world.get("rebirth",false)) else "Actions / Azioni")
    action_list.add_child(title)
    var info:=Label.new()
    info.text="Java validates availability, money and time."
    action_list.add_child(info)
    if bool(residents.world.get("rebirth",false)):
        var life: Dictionary=residents.world.get("life",{})
        var biography:=Label.new()
        biography.autowrap_mode=TextServer.AUTOWRAP_WORD_SMART
        biography.text=_life_summary()+"\nSavings / Risparmi: €"+str(life.get("savings",0))
        if life.get("parentA")!=null:biography.text+="\nParents / Genitori: "+str(life.get("parentA"))+" + "+str(life.get("parentB"))
        var conviction=life.get("conviction")
        if conviction is Dictionary:biography.text+="\nBelief / Convinzione: "+str(conviction.get("belief",""))
        var psych=life.get("psychology")
        if psych is Dictionary:biography.text+="\nStress: %s · Loneliness / Solitudine: %s · Satisfaction / Soddisfazione: %s" % [psych.get("stress",0),psych.get("loneliness",0),psych.get("satisfaction",0)]
        var substance=life.get("substance")
        if substance is Dictionary and int(substance.get("dependence",0))>0:biography.text+="\nDependence / Dipendenza: "+str(substance.get("dependence",0))
        var clinical=life.get("clinical")
        if clinical is Dictionary:
            var pending=str(clinical.get("requested",""))
            if not pending.is_empty():biography.text+="\nAppointment / Visita: "+pending
            for condition in clinical.get("diagnosed",[]):biography.text+="\n"+str(condition.get("name",""))+" · "+str(condition.get("status",""))
        var athletics=life.get("athletics")
        if athletics is Dictionary and not str(athletics.get("club","")).is_empty():
            biography.text+="\n"+str(athletics.get("club",""))+" · Skill / Abilità: "+str(athletics.get("skill",0))+" · Wins / Vittorie: "+str(athletics.get("wins",0))
        var culture=life.get("culture")
        if culture is Dictionary and int(culture.get("works",0))>0:
            biography.text+="\nCreative works / Opere: "+str(culture.get("works",0))+" · Sales / Vendite: "+str(culture.get("sales",0))+" · Reputation / Reputazione: "+str(culture.get("reputation",0))
        action_list.add_child(biography)
        var search=life.get("childSearch")
        if search is Dictionary and str(search.get("status","NONE"))!="NONE":
            var warning:=Label.new()
            warning.autowrap_mode=TextServer.AUTOWRAP_WORD_SMART
            warning.text="Expected home / Rientro previsto · "+str(search.get("status",""))+" · "+str(search.get("minutesOverdue",0))+" min"
            if search.get("searchingParent")!=null:warning.text+="\nSearching / Ti cerca: "+str(search.get("searchingParent"))
            action_list.add_child(warning)
        var upbringing=life.get("upbringing")
        if upbringing is Dictionary and int(upbringing.get("observedDays",0))>0:
            var development:=Label.new()
            development.autowrap_mode=TextServer.AUTOWRAP_WORD_SMART
            development.text="Childhood / Infanzia\nSecurity / Sicurezza: %s · Confidence / Fiducia: %s · Independence / Autonomia: %s\nDays together / Giorni insieme: %s/%s" % [upbringing.get("security",50),upbringing.get("confidence",50),upbringing.get("independence",50),upbringing.get("supportedDays",0),upbringing.get("observedDays",0)]
            action_list.add_child(development)
        var relatives: Array=life.get("familyTree",[])
        if not relatives.is_empty():
            var tree:=Label.new()
            tree.autowrap_mode=TextServer.AUTOWRAP_WORD_SMART
            tree.text="Family tree / Albero genealogico"
            for relative in relatives:
                tree.text+="\n%s / %s: %s · %s%s" % [relative.get("role",""),relative.get("roleIt",""),relative.get("name",""),relative.get("age",0),"" if relative.get("alive",true) else " · Deceased / Deceduto"]
                if int(relative.get("incarnation",0))>0:tree.text+=" · Life / Vita #"+str(relative.get("incarnation"))
            action_list.add_child(tree)
    var work: Dictionary=residents.world.get("work",{})
    if work_only or str(room.get("id",""))=="SCHOOL":WorkPanel.build(action_list,work,_perform_action)
    elif work.get("workspace") is Dictionary:
        var hint:=Label.new()
        hint.text="Use E at an office computer / Usa il PC in ufficio"
        hint.autowrap_mode=TextServer.AUTOWRAP_WORD_SMART
        action_list.add_child(hint)
    var enterprise=residents.world.get("enterprise")
    if enterprise is Dictionary and bool(enterprise.get("yours",false)):
        var summary:=Label.new()
        summary.autowrap_mode=TextServer.AUTOWRAP_WORD_SMART
        summary.text="Workshop / Officina\nCompany cash: €%s · machine: %s/100\nParts: %s · staff: %s\nPaid hours today: %s" % [enterprise.get("cash",0),enterprise.get("machineCondition",0),enterprise.get("spareParts",0),enterprise.get("employeeName","—"),enterprise.get("paidHoursToday",0)]
        action_list.add_child(summary)
    if not work_only:
        PropertyPanel.build(action_list,residents.world.get("property",{}))
        var commerce: Dictionary=residents.world.get("commerce",{})
        var stall=commerce.get("stall")
        if stall is Dictionary and bool(stall.get("registered",false)):
            WorkPanel.label(action_list,"MARKET STALL / BANCARELLA\nStock %s/%s · sales %s · revenue €%s\nPayroll €%s · regular customers %s" % [stall.get("stocked",0),stall.get("capacity",0),stall.get("sales",0),stall.get("revenue",0),stall.get("reserve",0),stall.get("regularCustomers",0)])
            var supply: Dictionary=stall.get("supply",{})
            WorkPanel.label(action_list,"Farm supply €%s · in transit %s · delivered %s" % [supply.get("reserve",0),supply.get("cargo",0),supply.get("delivered",0)])
            for item in stall.get("items",[]):
                WorkPanel.label(action_list,"%s: %s units · €%s" % [item.get("id",""),item.get("stock",0),item.get("price",0)])
        var freight=commerce.get("freight")
        if freight is Dictionary:
            WorkPanel.label(action_list,"FREIGHT / TRASPORTO\n%s · %s %s + %s %s\nTruck: %s · next: %s" % [freight.get("cargoLabel",""),freight.get("firstUnits",0),freight.get("firstLabel",""),freight.get("secondUnits",0),freight.get("secondLabel",""),freight.get("truckPlace",""),str(freight.get("nextDestination","Unload"))])
        var logistics=commerce.get("logistics")
        if logistics is Dictionary and bool(logistics.get("yours",false)):
            var accounts: Dictionary=logistics.get("financials",{})
            WorkPanel.label(action_list,"CARRIER / TRASPORTI\nCash €%s · truck condition %s/100 · operating net €%s\nDriver: %s" % [logistics.get("cash",0),logistics.get("condition",0),accounts.get("operatingNet",0),str(logistics.get("driverName",""))])
            var second: Dictionary=logistics.get("secondTruck",{})
            if bool(second.get("purchased",false)):WorkPanel.label(action_list,"Truck 2 · condition %s/100 · driver %s · deliveries %s" % [second.get("condition",0),str(second.get("driverName","")),second.get("completed",0)])
    for action in ([] if work_only else residents.world.get("actions",[])):
        var button:=Button.new()
        button.text=str(action.get("label",""))
        button.pressed.connect(_perform_action.bind(str(action.get("id",""))))
        action_list.add_child(button)
    var close:=Button.new()
    close.text="Close / Chiudi"
    close.pressed.connect(func():
        actions_panel.hide()
        Input.mouse_mode=Input.MOUSE_MODE_CAPTURED)
    action_list.add_child(close)
    actions_panel.show()
    Input.mouse_mode=Input.MOUSE_MODE_VISIBLE

func _perform_action(id: String) -> void:
    actions_panel.hide()
    Input.mouse_mode=Input.MOUSE_MODE_CAPTURED
    residents.perform(id,Vector2(walker.position.x,walker.position.z))

func _change_interior(next_room: Dictionary) -> void:
    if is_instance_valid(interior_node):
        remove_child(interior_node)
        interior_node.queue_free()
        interior_node=null
    room=next_room
    walker.velocity=Vector3.ZERO
    if not room.is_empty():
        room_exit=Vector2(float(room["x"]),float(room["z"]))
        interior_node=InteriorScene.new()
        add_child(interior_node)
        interior_node.build(room)
        walker.position=Vector3(room_exit.x,151.05,room_exit.y+5.6)
        walker.rotation.y=0
        head.rotation.x=0
        for node in chunks.values():node.hide()
        district_sign.hide()
        owned_car.hide()
        civilian_car.hide()
        parked_bike.hide()
        _sync_patrols([])
        _sync_commutes([])
        city_map.hide()
    else:
        for node in chunks.values():node.show()
        var place: Dictionary=residents.world.get("playerDistrict",{})
        if not place.is_empty():room_exit=Vector2(float(place["x"]),float(place["z"]))
        walker.position=Vector3(room_exit.x,1.05,room_exit.y)
        _stream()
    _update_navigation()

func _sync_spawn(point: Vector2) -> void:
    walker.position=Vector3(point.x,1.05,point.y)
    walker.velocity=Vector3.ZERO
    _stream()
    _update_navigation()

func _send_chat(message: String) -> void:
    chat.hide()
    Input.mouse_mode=Input.MOUSE_MODE_CAPTURED
    if not message.strip_edges().is_empty():
        residents.speak(chat_target,message,Vector2(walker.position.x,walker.position.z))

func _update_daylight() -> void:
    if not is_instance_valid(daylight) or sky_environment==null:return
    var hour:=float(residents.minute)/60.0 if residents.connected else 12.0
    var sun_amount:=clampf(sin((hour-6.0)*PI/12.0),0.0,1.0)
    var weather: Dictionary=residents.world.get("weather",{})
    var overcast:=str(weather.get("sky","CLEAR"))!="CLEAR"
    daylight.rotation_degrees=Vector3(-12.0-sun_amount*55.0,-35.0+(hour-12.0)*7.0,0)
    daylight.light_energy=(0.08+sun_amount*0.90)*(0.58 if overcast else 1.0)
    daylight.light_color=Color("#e6bd94").lerp(Color("#fff4e3"),sun_amount)
    sky_environment.background_color=Color("#263447").lerp(Color("#9da8b0") if overcast else Color("#a7c7de"),sun_amount)
    sky_environment.ambient_light_color=Color("#a1b1ce").lerp(Color.WHITE,sun_amount)
    sky_environment.ambient_light_energy=0.32+sun_amount*0.34
    if detail_materials.has("lamp"):
        var lamp: StandardMaterial3D=detail_materials["lamp"]
        lamp.emission_energy_multiplier=0.10 if sun_amount>0.20 else 2.0
    _local_lights(sun_amount<=0.20 and room.is_empty())
    var season:=str(weather.get("season","SPRING"))
    var foliage:=Color("#657e4c")
    match season:
        "SUMMER":foliage=Color("#4e713e")
        "AUTUMN":foliage=Color("#b08542")
        "WINTER":foliage=Color("#77796a")
    if detail_materials.has("leaves"):detail_materials["leaves"].albedo_color=foliage
    if detail_materials.has("leaves_light"):detail_materials["leaves_light"].albedo_color=foliage.lightened(0.15)

func _local_lights(enabled: bool) -> void:
    var nearby: Array[Vector3]=[]
    if enabled:
        for chunk in chunks.values():
            for at in chunk.get_meta("lamp_positions",[]):
                if walker.position.distance_squared_to(at)<3600:nearby.append(at)
        nearby.sort_custom(func(a: Vector3,b: Vector3):return walker.position.distance_squared_to(a)<walker.position.distance_squared_to(b))
    for i in range(lamp_pool.size()):
        var light: OmniLight3D=lamp_pool[i]
        light.visible=i<nearby.size()
        if light.visible:light.position=nearby[i]

func _point(value: Array,y: float) -> Vector3:
    return Vector3(float(value[0]),y,float(value[1]))

func _quad(out: PackedVector3Array,a: Vector3,b: Vector3,c: Vector3,d: Vector3) -> void:
    out.append_array(PackedVector3Array([a,b,c,a,c,d]))

func _strip(out: PackedVector3Array,record: Dictionary,y: float) -> void:
    var a := _point(record["a"],y)
    var b := _point(record["b"],y)
    var v := (b-a).normalized()
    var side := Vector3(-v.z,0,v.x)*float(record["width"])*0.5
    _quad(out,a+side,b+side,b-side,a-side)

func _build_chunk(key: String,data: Dictionary) -> void:
    var node := Node3D.new()
    add_child(node)
    chunks[key]=node
    var detail=Architecture.new()
    var walls := PackedVector3Array()
    var roofs := PackedVector3Array()
    var roads := PackedVector3Array()
    var stone_roads := PackedVector3Array()
    var dirt_roads := PackedVector3Array()
    var rails := PackedVector3Array()
    var water := PackedVector3Array()
    for record in data.get("water",[]):_strip(water,record,0.012)
    for feature in data.get("buildings",[]):
        var points: Array=feature["points"]
        var h := float(feature["height"])
        var polygon := PackedVector2Array()
        for p in points:polygon.append(Vector2(float(p[0]),float(p[1])))
        var triangles := Geometry2D.triangulate_polygon(polygon)
        if triangles.is_empty():continue
        for i in range(points.size()):
            var a := _point(points[i],0)
            var b := _point(points[(i+1)%points.size()],0)
            if a.distance_squared_to(b)<0.0001:continue
            _quad(walls,a,b,b+Vector3.UP*h,a+Vector3.UP*h)
            # Both faces are solid, including on the exterior irrespective of OSM winding.
            _quad(walls,b,a,a+Vector3.UP*h,b+Vector3.UP*h)
        for i in triangles:
            roofs.append(_point(points[i],h))
        detail.facade(feature)
    for record in data.get("roads",[]):
        var surface:=str(record.get("surface",""))
        var kind:=str(record.get("kind",""))
        if surface in ["paving_stones","sett","cobblestone","unhewn_cobblestone"]:
            _strip(stone_roads,record,0.026)
        elif surface in ["gravel","fine_gravel","ground","dirt","earth","grass","unpaved","compacted","sand"] or (surface.is_empty() and kind in ["track","path"]):
            _strip(dirt_roads,record,0.024)
        else:
            _strip(roads,record,0.025)
        detail.road(record)
    for record in data.get("trees",[]):detail.tree(record)
    for record in data.get("benches",[]):detail.bench(record)
    node.set_meta("lamp_positions",detail.lamp_positions)
    for record in data.get("rail",[]):_strip(rails,record,0.035)
    _surface(node,walls,wall_material,true,false)
    for kind in detail.batches:
        if not detail_materials.has(kind):detail_materials[kind]=Architecture.material(kind)
        _surface(node,detail.batches[kind],detail_materials[kind],false)
    _surface(node,roofs,roof_material,false)
    _surface(node,roads,road_material,false)
    for kind in ["stone_road","dirt_road"]:
        if not detail_materials.has(kind):detail_materials[kind]=Architecture.material(kind)
    _surface(node,stone_roads,detail_materials["stone_road"],false)
    _surface(node,dirt_roads,detail_materials["dirt_road"],false)
    _surface(node,rails,rail_material,false)
    if not detail_materials.has("waterway"):detail_materials["waterway"]=Architecture.material("waterway")
    _surface(node,water,detail_materials["waterway"],false)

func _surface(parent: Node3D,vertices: PackedVector3Array,material: Material,collision: bool,visual: bool=true) -> void:
    if vertices.is_empty():return
    var builder := SurfaceTool.new()
    builder.begin(Mesh.PRIMITIVE_TRIANGLES)
    for v in vertices:builder.add_vertex(v)
    builder.generate_normals()
    var mesh := builder.commit()
    var instance := MeshInstance3D.new()
    instance.mesh=mesh
    instance.material_override=material
    instance.visible=visual
    parent.add_child(instance)
    if collision:
        instance.create_trimesh_collision()
