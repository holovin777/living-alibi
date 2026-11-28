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
var flat_roof_material: StandardMaterial3D
const Textures=preload("res://scripts/TownVisuals.gd")
var surface_textures: Dictionary={}
const Architecture=preload("res://scripts/GeoArchitecture.gd")
const Landscape=preload("res://scripts/GeoLandscape.gd")
var land_materials: Dictionary={}
var land_records: Dictionary={}
var detail_materials: Dictionary={}
var daylight: DirectionalLight3D
var sky_environment: Environment
var sky_material: ProceduralSkyMaterial
var mountain_horizon: Node3D
const Signals=preload("res://scripts/GeoSignals.gd")
var last_signal_second: int=-1
var headlamps: Array[SpotLight3D]=[]
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
const FireVisual=preload("res://scripts/GeoFireVisual.gd")
var fire_visual: Node3D
var phone_panel: PanelContainer
var action_list: VBoxContainer
var owned_car: Node3D
var parked_bike: Node3D
var civilian_car: Node3D
var reserve_bus: Node3D
var reserve_truck: Node3D
var reserve_scooter: Node3D
var reserve_motorcycle: Node3D
var own_scooter: Node3D
var own_motorcycle: Node3D
var rider_body: Node3D
var rider_speed: Label3D
var driving_light: String=""
var driving_bus:=false
var driving_truck:=false
var civilian_id:=""
var patrol_models: Dictionary={}
var patrol_goals: Dictionary={}
var commute_models: Dictionary={}
var commute_goals: Dictionary={}
const CommuteMotion=preload("res://scripts/GeoCommuteMotion.gd")
var commute_motion: Dictionary={}
var commute_signal_points: Dictionary={}
const Cockpit=preload("res://scripts/GeoCockpit.gd")
var driving_body: Node3D
var drive_speed:=0.0
var server_drive_speed:=0.0
var drive_audio: Node
const DriveSound=preload("res://scripts/DriveAudio.gd")
var drive_heading:=0.0
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
        hud.text="Map missing. Run bash play.sh --map to import real OSM data."
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
    var graph: Dictionary=city.get("graph",{})
    for raw_id in graph.get("signals",{}):
        if not graph.get("nodes",{}).has(raw_id):continue
        var at: Array=graph["nodes"][raw_id]
        commute_signal_points[CommuteMotion.point_key(Vector2(float(at[0]),float(at[1])))]=int(graph["signals"][raw_id])
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
    reserve_bus=traffic._bus()
    add_child(reserve_bus)
    reserve_bus.hide()
    if is_instance_valid(reserve_truck):reserve_truck.hide()
    if is_instance_valid(reserve_scooter):reserve_scooter.hide()
    if is_instance_valid(reserve_motorcycle):reserve_motorcycle.hide()
    if is_instance_valid(own_scooter):own_scooter.hide()
    if is_instance_valid(own_motorcycle):own_motorcycle.hide()
    reserve_truck=traffic._truck()
    add_child(reserve_truck)
    reserve_truck.hide()
    reserve_scooter=traffic._two_wheeler(true)
    reserve_motorcycle=traffic._two_wheeler(false)
    own_scooter=traffic._two_wheeler(true)
    own_motorcycle=traffic._two_wheeler(false)
    for vehicle in [reserve_scooter,reserve_motorcycle,own_scooter,own_motorcycle]:
        add_child(vehicle)
        vehicle.hide()
    rider_body=Node3D.new()
    add_child(rider_body)
    rider_body.hide()
    drive_audio=DriveSound.new()
    add_child(drive_audio)
    driving_body=Cockpit.new()
    add_child(driving_body)
    driving_body.hide()
    for side in [-1.0,1.0]:
        var beam:=SpotLight3D.new()
        beam.position=Vector3(float(side)*0.62,0.72,-1.65)
        beam.rotation_degrees.x=-5.0
        beam.light_color=Color("#fff1d5")
        beam.light_energy=2.4
        beam.spot_range=38.0
        beam.spot_angle=25.0
        beam.shadow_enabled=false
        beam.hide()
        driving_body.add_child(beam)
        headlamps.append(beam)
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
    land_records=city.get("land",{})
    wall_material=_material(Color("#cbb49b"))
    roof_material=_material(Color("#b47a60"))
    flat_roof_material=_material(Color("#96938b"))
    for kind in ["stucco","terracotta","paving","wood","grass"]:
        surface_textures[kind]=Textures.texture(kind)
    Textures.surface(roof_material,surface_textures["terracotta"],0.48)
    Textures.surface(flat_roof_material,surface_textures["stucco"],0.30)
    var asphalt:=ShaderMaterial.new()
    asphalt.shader=preload("res://shaders/GeoAsphalt.gdshader")
    road_material=asphalt
    rail_material=_material(Color("#8f8578"))
    var world_environment := WorldEnvironment.new()
    var environment := Environment.new()
    sky_material=ProceduralSkyMaterial.new()
    sky_material.sky_top_color=Color("#5284bd")
    sky_material.sky_horizon_color=Color("#d3e3ef")
    sky_material.ground_horizon_color=sky_material.sky_horizon_color
    sky_material.ground_bottom_color=Color("#858973")
    var sky:=Sky.new()
    sky.sky_material=sky_material
    environment.sky=sky
    environment.background_mode=Environment.BG_SKY
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
    var ground_material:=_material(Color("#a4a98c"))
    Textures.surface(ground_material,surface_textures["grass"],0.42)
    ground_visual.material_override=ground_material
    add_child(ground_visual)
    mountain_horizon=preload("res://scripts/GeoHorizon.gd").new()
    add_child(mountain_horizon)
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
            elif own_scooter.visible and walker.position.distance_to(own_scooter.position)<5:
                residents.drive_command("start-scooter",pos)
            elif own_motorcycle.visible and walker.position.distance_to(own_motorcycle.position)<5:
                residents.drive_command("start-motorcycle",pos)
            elif reserve_scooter.visible and walker.position.distance_to(reserve_scooter.position)<5:
                residents.drive_command("steal-scooter",pos)
            elif reserve_motorcycle.visible and walker.position.distance_to(reserve_motorcycle.position)<5:
                residents.drive_command("steal-motorcycle",pos)
            elif reserve_truck.visible and walker.position.distance_to(reserve_truck.position)<5:
                residents.drive_command("steal-truck",pos)
            elif reserve_bus.visible and walker.position.distance_to(reserve_bus.position)<5:
                residents.drive_command("steal-bus",pos)
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
            if _near_office_computer() and _has_office_workspace():
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
    if event is InputEventMouseMotion and Input.mouse_mode==Input.MOUSE_MODE_CAPTURED:
        if driving:
            head.rotation.y=clampf(head.rotation.y-event.relative.x*0.0025,-1.0,1.0)
            head.rotation.x=clampf(head.rotation.x-event.relative.y*0.0025,-0.5,0.45)
        else:
            walker.rotate_y(-event.relative.x*0.0025)
            head.rotation.x=clampf(head.rotation.x-event.relative.y*0.0025,-1.45,1.45)

func _physics_process(delta: float) -> void:
    if not ready_to_walk:return
    mountain_horizon.position=Vector3(walker.position.x,0,walker.position.z)
    mountain_horizon.visible=room.is_empty() and daylight.light_energy>0.13
    var direction := Vector3.ZERO
    traffic.update_traffic(Vector2(walker.position.x,walker.position.z),delta)
    var second:=Signals.seconds()
    if second!=last_signal_second:
        last_signal_second=second
        for chunk in chunks.values():
            for control in chunk.get_node("RoadControls").get_children():
                Signals.update(control,second)
    property_scenery.animate(delta)
    if not is_instance_valid(fire_visual):
        fire_visual=FireVisual.new()
        add_child(fire_visual)
    var incident=residents.world.get("fireVisual")
    fire_visual.sync(incident if incident is Dictionary else {},walker.position,room.is_empty() and residents.connected)
    fire_visual.animate(delta)
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
        head.position=Vector3(0,0.55,0.32) if not driving_light.is_empty() else (Vector3(-0.48,1.05,0.28) if driving_bus or driving_truck else Vector3(-0.33,0.30,0.20))
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
        if is_instance_valid(interior_node):
            interior_node.update_work_board(residents.world.get("work",{}))
            interior_node.update_fire_board(residents.world.get("fireService",{}))
        hud.text=str(room.get("label",""))+" · INTERIOR / INTERNO\nWASD · E near exit · T talk · F actions · P phone\nDay %s · %02d:%02d · inside %s · €%s" % [residents.day,int(residents.minute/60),residents.minute%60,residents.people.size(),residents.world.get("money",0)]
        if bool(residents.world.get("rebirth",false)):hud.text+="\n"+_life_summary()+" · F: life actions · E: leave home"
        if _near_office_computer() and _has_office_workspace():hud.text+="\nBorgo Digital · E: use computer"
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
        if owned_car.visible:traffic.set_plate(owned_car,str(world.get("ownedCarPlate","")))
        if driving:owned_car.hide()
        _sync_civilian(world.get("civilianCar"),pos)
        _sync_parked_vehicle(reserve_bus,world.get("parkedBus"),pos,Vector3(0,0.05,0))
        if reserve_bus.visible:reserve_bus.rotation.y=float(world["parkedBus"].get("heading",0))
        _sync_parked_vehicle(reserve_truck,world.get("parkedTruck"),pos,Vector3(0,0.05,0))
        if reserve_truck.visible:reserve_truck.rotation.y=float(world["parkedTruck"].get("heading",0))
        _sync_parked_vehicle(reserve_scooter,world.get("parkedScooter"),pos,Vector3(0,0.05,0))
        _sync_parked_vehicle(reserve_motorcycle,world.get("parkedMotorcycle"),pos,Vector3(0,0.05,0))
        if reserve_scooter.visible:reserve_scooter.rotation.y=float(world["parkedScooter"].get("heading",0))
        if reserve_motorcycle.visible:reserve_motorcycle.rotation.y=float(world["parkedMotorcycle"].get("heading",0))
        _sync_parked_vehicle(own_scooter,world.get("ownedScooter"),pos,Vector3(0,0.05,0))
        _sync_parked_vehicle(own_motorcycle,world.get("ownedMotorcycle"),pos,Vector3(0,0.05,0))
        if driving:
            reserve_bus.hide()
            if is_instance_valid(reserve_truck):reserve_truck.hide()
            if is_instance_valid(reserve_scooter):reserve_scooter.hide()
            if is_instance_valid(reserve_motorcycle):reserve_motorcycle.hide()
            if is_instance_valid(own_scooter):own_scooter.hide()
            if is_instance_valid(own_motorcycle):own_motorcycle.hide()
            reserve_truck.hide()
        _sync_patrols(world.get("patrols",[]))
        _sync_commutes(world.get("traffic",[]))
        hud.text+="\nJava traffic: %s" % commute_models.size()
        if property_scenery.missing_yard or property_scenery.missing_site:hud.text+="\nPlot visual unavailable: no clear mapped space. F actions still work."
        if driving:civilian_car.hide()
        var licenses: Array=world.get("drivingLicenses",[])
        var categories:=PackedStringArray()
        for license in licenses:
            if bool(license.get("licensed",false)):categories.append(str(license.get("category","")))
        hud.text+="\nPatente / License: "+(" · ".join(categories) if not categories.is_empty() else "None / Nessuna")
        hud.text+="\nG: enter nearby vehicle / steal parked reserve"
        if driving:hud.text+="\nW: accelerate · Space/S: brake · A/D: choose turn · G: exit | Road target %s km/h · Fuel %s · Condition %s" % [residents.driving.get("speedLimitKmh",36),residents.driving.get("fuel",0),residents.driving.get("condition",0)]
        for doc in world.get("vehicleDocuments",[]):
            hud.text+="\n%s · %s · RC %s d · Revisione %s d" % [doc.get("kind",""),doc.get("plate",""),doc.get("insuranceDays",0),doc.get("inspectionDays",0)]
        var duty: Dictionary=world.get("fireDuty",{})
        if str(world.get("jobId",""))=="firefighter":hud.text+="\nFire duty / Servizio: kit %s%% · %s" % [duty.get("gearCondition",0),"Assigned / In missione" if duty.get("assigned",false) else "Standby / Disponibile" if duty.get("standby",false) else "Off duty / Riposo"]
        for damaged in world.get("fireRecovery",[]):
            if str(damaged.get("id",""))==str(district.get("id","")):hud.text+="\nRestoration / Ripristino: %s%% · %s" % [damaged.get("condition",100),"Closed / Chiuso" if damaged.get("closed",false) else "Open / Aperto"]
        var ownership: Array=world.get("businessOwnership",[])
        if str(district.get("id",""))=="OFFICE" and not ownership.is_empty():
            var listings:=0
            var owned_firms:=0
            var npc_firms:=0
            for firm in ownership:
                if bool(firm.get("listed",false)):listings+=1
                if bool(firm.get("owned",false)):owned_firms+=1
                if not str(firm.get("npcOwner","")).is_empty():npc_firms+=1
            hud.text+="\nBusiness registry / Registro imprese: %s for sale · %s yours · %s NPC owned" % [listings,owned_firms,npc_firms]
        var loans: Array=world.get("businessCredit",[])
        if str(district.get("id",""))=="OFFICE" and not loans.is_empty():
            var loan_balance:=0
            var overdue:=0
            for loan in loans:
                loan_balance+=int(loan.get("balance",0))
                if int(loan.get("missedDays",0))>0:overdue+=1
            hud.text+="\nBusiness loans / Prestiti: €%s outstanding · %s overdue" % [loan_balance,overdue]
        var firms: Array=world.get("businessSolvency",[])
        if str(district.get("id","")) in ["OFFICE","FACTORY","MARKET"] and not firms.is_empty():
            var suspended_count:=0
            var arrears:=0
            for firm in firms:
                if bool(firm.get("suspended",false)):suspended_count+=1
                arrears+=int(firm.get("arrears",0))
            if suspended_count>0 or arrears>0:hud.text+="\nCompanies / Imprese: %s suspended · €%s wage arrears" % [suspended_count,arrears]
        var business_insurance: Dictionary=world.get("businessInsurance",{})
        if str(district.get("id",""))=="OFFICE" and int(business_insurance.get("businesses",0))>0:hud.text+="\nBusiness cover / Imprese: %s/%s · €%s pending" % [business_insurance.get("insured",0),business_insurance.get("businesses",0),business_insurance.get("pending",0)]
        var insurer: Dictionary=world.get("householdInsurance",{})
        if str(district.get("id",""))=="OFFICE" and int(insurer.get("agents",0))>0:hud.text+="\nInsurance / Assicurazioni: %s/%s homes · %s agents · €%s claims" % [insurer.get("insured",0),insurer.get("households",0),insurer.get("onDuty",0),insurer.get("pending",0)]
        var insurance: Dictionary=world.get("homeInsurance",{})
        if bool(insurance.get("owned",false)) or int(insurance.get("pending",0))>0:hud.text+="\nHome insurance / Assicurazione: %s days · €%s pending" % [insurance.get("days",0),insurance.get("pending",0)]
        var housing: Dictionary=world.get("fireHousing",{})
        if int(housing.get("accommodated",0))+int(housing.get("waiting",0))>0:hud.text+="\nTemporary shelter / Alloggio: %s/%s assigned · %s waiting" % [housing.get("accommodated",0),housing.get("capacity",24),housing.get("waiting",0)]
        var fire: Dictionary=world.get("fireService",{})
        if str(fire.get("status","NONE"))!="NONE":hud.text+="\nFire / Incendio: %s · %s · %s min · Crew %s" % [fire.get("district",""),fire.get("status",""),fire.get("minutesRemaining",0),fire.get("assigned",0)]
        if int(fire.get("evacuating",0))>0:hud.text+="\nEvacuation / Evacuazione: %s people · %s at safe locations" % [fire.get("evacuating",0),fire.get("sheltered",0)]
        var ambulance: Dictionary=world.get("ambulance",{})
        if str(ambulance.get("status",""))=="EN_ROUTE":hud.text+="\nAmbulance / Ambulanza: %s min · Stay in district" % ambulance.get("minutesRemaining",0)
        elif str(ambulance.get("status",""))=="ARRIVED":hud.text+="\nAmbulance arrived / Arrivata · F to board near district marker"
        var tow: Dictionary=world.get("towService",{})
        if not str(tow.get("kind","")).is_empty():hud.text+="\nRecovery / Carro attrezzi: %s · %s min to workshop" % [tow.get("kind",""),tow.get("minutesRemaining",0)]
        var accidents: Dictionary=world.get("roadAccidents",{})
        if int(accidents.get("accidents",0))>0:hud.text+="\nAccidents / Incidenti: %s · Insurance paid €%s · Health %s" % [accidents.get("accidents",0),accidents.get("compensation",0),world.get("health",0)]
        var road_police: Dictionary=world.get("roadPolice",{})
        if int(road_police.get("outstanding",0))>0:hud.text+="\nTraffic fines / Multe: €%s · F at municipal office" % road_police["outstanding"]
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
        reserve_bus.hide()
        if is_instance_valid(reserve_truck):reserve_truck.hide()
        if is_instance_valid(reserve_scooter):reserve_scooter.hide()
        if is_instance_valid(reserve_motorcycle):reserve_motorcycle.hide()
        if is_instance_valid(own_scooter):own_scooter.hide()
        if is_instance_valid(own_motorcycle):own_motorcycle.hide()
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
    if not driving:rider_body.hide()
    if driving:
        driving_bus=str(pose.get("vehicleKind","CAR"))=="BUS"
        driving_truck=str(pose.get("vehicleKind","CAR"))=="TRUCK"
        var kind:=str(pose.get("vehicleKind","CAR"))
        driving_light=kind if kind in ["SCOOTER","MOTORCYCLE"] else ""
        driving_body.visible=driving_light.is_empty()
        rider_body.visible=not driving_light.is_empty()
        if not was_driving and rider_body.visible:_build_rider(kind=="SCOOTER")
        driving_body.scale=Vector3(1.40,1.55,1.30) if driving_bus or driving_truck else Vector3.ONE
        server_drive_speed=float(pose.get("speedKmh",0))
        drive_goal=Vector3(float(pose["x"]),1.05,float(pose["z"]))
        drive_heading=float(pose.get("heading",0))
        if not was_driving:
            walker.rotation.y=drive_heading
            head.rotation=Vector3.ZERO
            drive_speed=0.0
            walker.position=drive_goal
            driving_body.position=walker.position-Vector3(0,1.0,0)
            driving_body.rotation.y=drive_heading
            owned_car.hide()
            civilian_car.hide()
            reserve_bus.hide()
            if is_instance_valid(reserve_truck):reserve_truck.hide()
            if is_instance_valid(reserve_scooter):reserve_scooter.hide()
            if is_instance_valid(reserve_motorcycle):reserve_motorcycle.hide()
            if is_instance_valid(own_scooter):own_scooter.hide()
            if is_instance_valid(own_motorcycle):own_motorcycle.hide()
    elif was_driving:
        _reset_driving_view()
        walker.position=Vector3(float(pose["x"]),1.05,float(pose["z"]))
        walker.velocity=Vector3.ZERO
        _stream()

func _build_rider(scooter: bool) -> void:
    for child in rider_body.get_children():
        rider_body.remove_child(child)
        child.queue_free()
    rider_body.add_child(traffic._two_wheeler(scooter))
    rider_speed=Label3D.new()
    rider_speed.position=Vector3(0,1.20,-0.46)
    rider_speed.font_size=40
    rider_speed.pixel_size=0.0015
    rider_speed.modulate=Color("#b6e7cf")
    rider_speed.text="00 km/h"
    rider_body.add_child(rider_speed)

func _reset_driving_view() -> void:
    driving_bus=false
    driving_truck=false
    driving_light=""
    rider_body.hide()
    driving_body.scale=Vector3.ONE
    head.position=Vector3(0,0.65,0)
    head.rotation=Vector3.ZERO
    linked_age=-999 # next walking frame restores the age-specific camera
    drive_speed=0.0
    server_drive_speed=0.0
    drive_audio.update_drive(false,0,false,0)

func _drive_frame(delta: float) -> void:
    if not residents.connected or bool(residents.world.get("confined",false)) or bool(residents.world.get("rebirth",false)):
        if not residents.connected:
            drive_audio.update_drive(false,0,false,delta)
            return # freeze until authoritative state is available again
        driving=false
        driving_body.hide()
        _reset_driving_view()
        var location: Dictionary=residents.world.get("playerDistrict",{})
        if not location.is_empty():_sync_spawn(Vector2(float(location["x"]),float(location["z"])))
        return
    walker.position=walker.position.move_toward(drive_goal,delta*maxf(12.0,server_drive_speed/3.6*1.2))
    walker.rotation.y=lerp_angle(walker.rotation.y,drive_heading,minf(delta*5.0,1.0))
    driving_body.position=walker.position-Vector3(0,1.0,0)
    driving_body.rotation.y=walker.rotation.y
    drive_speed=lerpf(drive_speed,server_drive_speed,minf(delta*5.0,1.0))
    drive_audio.update_drive(true,drive_speed,Input.is_physical_key_pressed(KEY_W),delta)
    var steering_input:=float(Input.is_physical_key_pressed(KEY_D))-float(Input.is_physical_key_pressed(KEY_A))
    driving_body.update_instruments(drive_speed,steering_input,delta)
    if not driving_light.is_empty():
        rider_body.position=walker.position-Vector3(0,1.0,0)
        rider_body.rotation.y=walker.rotation.y
        rider_body.rotation.z=lerpf(rider_body.rotation.z,-steering_input*0.12,minf(1.0,delta*5.0))
        if is_instance_valid(rider_speed):rider_speed.text="%02d km/h" % roundi(drive_speed)
    _stream()
    drive_timer+=delta
    if drive_timer>=0.65:
        drive_timer=0
        var braking:=Input.is_physical_key_pressed(KEY_SPACE) or Input.is_physical_key_pressed(KEY_S)
        var accelerating:=Input.is_physical_key_pressed(KEY_W)
        if accelerating or braking or server_drive_speed>0.01:
            var turn:=0
            if Input.is_physical_key_pressed(KEY_A):turn=-1
            elif Input.is_physical_key_pressed(KEY_D):turn=1
            var command: String="brake" if braking else ("step" if accelerating else "coast")
            residents.drive_command(command,Vector2(walker.position.x,walker.position.z),turn)
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

const EmergencySound=preload("res://scripts/EmergencyAudio.gd")

func _sync_commutes(records: Array) -> void:
    var seen: Dictionary={}
    for record in records:
        var id:=str(record.get("id",""))
        var goal:=Vector3(float(record["x"]),0.05,float(record["z"]))
        seen[id]=true
        if not commute_models.has(id):
            var car: Node3D
            if str(record.get("kind",""))=="FIRE":
                car=traffic._fire_engine()
            elif str(record.get("kind",""))=="AMBULANCE":
                car=traffic._ambulance()
            elif str(record.get("kind",""))=="BUS":
                car=traffic._bus()
            else:
                var palette: Array[Color]=[Color("#9b403d"),Color("#456b89"),Color("#d0ccc0"),Color("#64696b"),Color("#73805a")]
                car=traffic._car(palette[posmod(id.hash(),palette.size())])
            add_child(car)
            car.position=goal
            commute_models[id]=car
        traffic.set_plate(commute_models[id],str(record.get("plate","")),str(record.get("kind","CAR")))
        var kind:=str(record.get("kind","CAR"))
        var responding:=kind in ["AMBULANCE","FIRE"] and not str(record.get("trip","")).ends_with(":arrived")
        commute_goals[id]={"point":goal,"heading":float(record.get("heading",0)),"kind":kind,"responding":responding}
        if kind in ["AMBULANCE","FIRE"]:EmergencySound.update(commute_models[id],kind,responding)
        if record.get("route",[]).size()>=2:
            if not commute_motion.has(id) or commute_motion[id].trip!=str(record.get("trip","")):
                var motion=CommuteMotion.new()
                motion.configure(record,commute_signal_points)
                commute_motion[id]=motion
                var pos: Vector2=motion.at()
                commute_models[id].position=Vector3(pos.x,0.05,pos.y)
            commute_motion[id].set_target(float(record.get("distance",0)))
        else:commute_motion.erase(id)
    for id in commute_models.keys():
        if not seen.has(id):
            commute_models[id].queue_free()
            commute_models.erase(id)
            commute_goals.erase(id)
            commute_motion.erase(id)

func _animate_commutes(delta: float) -> void:
    for id in commute_models:
        var car: Node3D=commute_models[id]
        var goal: Dictionary=commute_goals[id]
        if str(goal.get("kind","")) in ["AMBULANCE","FIRE"]:
            var phase:=int(Time.get_ticks_msec()/180.0)%4
            car.get_node("BeaconL").visible=phase==0
            car.get_node("BeaconR").visible=phase==2
            car.get_node("EmergencyLight").visible=phase==0 or phase==2
        if commute_motion.has(id):
            var motion=commute_motion[id]
            var dir: Vector2=motion.direction()
            var pos: Vector2=motion.at()
            var gap:=12.0*delta
            # Keep a queue on the road rather than passing through stopped cars.
            for other_id in commute_models:
                if other_id==id:continue
                var other: Node3D=commute_models[other_id]
                var offset:=Vector2(other.position.x,other.position.z)-pos
                var ahead:=offset.dot(dir)
                if ahead>0.1 and ahead<9.0 and absf(offset.cross(dir))<1.6:
                    gap=minf(gap,maxf(0.0,ahead-5.5))
            motion.advance(gap,Signals.seconds())
            var at: Vector2=motion.at()
            var heading: Vector2=motion.direction()
            car.position=Vector3(at.x,0.05,at.y)
            car.rotation.y=lerp_angle(car.rotation.y,atan2(-heading.x,-heading.y),minf(1.0,delta*5.0))
        else:
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
        EmergencySound.update(patrol_models[id],"POLICE",bool(record.get("responding",false)))
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
    var id:=str(record.get("id",""))
    var kind: String="BUS" if id=="municipal-reserve-bus" else ("TRUCK" if id=="freight-reserve-truck" else ("SCOOTER" if id.ends_with("scooter") else ("MOTORCYCLE" if id.ends_with("motorcycle") else "CAR")))
    if node.visible:traffic.set_plate(node,str(record.get("plate","")),kind)

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

func _has_office_workspace() -> bool:
    if residents.world.get("work",{}).get("workspace") is Dictionary:return true
    for firm in residents.world.get("businessOwnership",[]):
        if bool(firm.get("owned",false)):return true
    return false

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
    title.text="Office computer / Computer ufficio" if work_only else ("Life / Vita" if bool(residents.world.get("rebirth",false)) else "Actions / Azioni")
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
    if str(room.get("id",""))=="OFFICE":
        for plot in residents.world.get("commercialPlots",[]):
            WorkPanel.label(action_list,"%s · Owner / Proprietario: %s\nConstruction / Costruzione %s/%s · Weekly rent / Affitto €%s\nTenant / Inquilino: %s · Until / Fino al giorno %s" % [plot.get("id",""),plot.get("owner",""),plot.get("stage",0),plot.get("stages",3),plot.get("weeklyRent",0),plot.get("tenant",""),plot.get("leaseUntil",0)])
        for entry in residents.world.get("businessOwnership",[]):
            if not str(entry.get("npcOwner","")).is_empty():
                WorkPanel.label(action_list,"%s · Owner / Proprietario: %s" % [entry.get("account",""),entry.get("npcOwner","")])
    if work_only:
        for firm in residents.world.get("businessOwnership",[]):
            if not bool(firm.get("owned",false)):continue
            var management: Dictionary=firm.get("management",{})
            var gov: Dictionary=management.get("governance",{})
            WorkPanel.label(action_list,"%s · Cash / Cassa €%s\nDirector / Direttore: %s · Staff / Organico %s/%s · €%s/h" % [firm.get("account",""),firm.get("cash",0),management.get("director",""),management.get("employees",0),management.get("target",0),management.get("hourlyWage",0)])
            WorkPanel.label(action_list,"Department head / Caporeparto: %s\nAccountant / Contabile: %s\nHR / Risorse umane: %s" % [gov.get("departmentHead",""),gov.get("accountant",""),gov.get("hr","")])
            var decisions: Dictionary={"FREEZE_HIRING":"Freeze hiring / Blocco assunzioni","RECRUIT":"Recruit / Assumere","MAINTAIN":"Maintain staff / Mantieni organico","PENDING":"Pending / In attesa"}
            WorkPanel.label(action_list,"Decision / Decisione: %s · Day / Giorno %s" % [decisions.get(gov.get("decision","PENDING"),""),gov.get("decisionDay",0)])
            if int(gov.get("auditDay",0))>0:
                WorkPanel.label(action_list,"Audit / Controllo · Day / Giorno %s · Cash / Cassa €%s · Arrears / Arretrati €%s" % [gov.get("auditDay",0),gov.get("auditCash",0),gov.get("auditArrears",0)])
            var outcomes: Dictionary={"SCHEDULED":"Scheduled / Programmato","HIRED":"Hired / Assunto","REJECTED":"Not hired / Non assunto","NONE":"None / Nessuno"}
            WorkPanel.label(action_list,"Interview / Colloquio: %s · %s · Day / Giorno %s" % [gov.get("candidate",""),outcomes.get(gov.get("interviewResult","NONE"),""),maxi(int(gov.get("appointmentDay",0)),int(gov.get("interviewDay",0)))])
            var order_states: Dictionary={"ORDERED":"Ordered / Ordinato","PAID":"Paid / Pagato","NO_FUNDS":"No funds / Fondi insufficienti","UNFULFILLED":"Unfulfilled / Inevaso","ON_CREDIT":"On credit / A credito"}
            for order in management.get("procurement",[]):
                WorkPanel.label(action_list,"Purchase / Acquisto: %s ×%s · €%s · %s\nSupplier / Fornitore: %s · Day / Giorno %s" % [order.get("item",""),order.get("quantity",0),order.get("amount",0),order_states.get(order.get("status",""),""),order.get("supplier",""),order.get("day",0)])
                if int(order.get("invoiceDay",0))>0:
                    WorkPanel.label(action_list,"Paid invoice / Fattura pagata · Day / Giorno %s · %s units · €%s" % [order.get("invoiceDay",0),order.get("invoiceQuantity",0),order.get("invoiceAmount",0)])
            var trade: Dictionary=management.get("tradeCredit",{})
            WorkPanel.label(action_list,"Supplier debts / Debiti fornitori €%s · Receivables / Crediti €%s" % [trade.get("payables",0),trade.get("receivables",0)])
            for invoice in trade.get("invoices",[]):
                var status_text: String="Paid / Pagata" if int(invoice.get("balance",0))==0 else ("Overdue / Scaduta" if residents.day>int(invoice.get("dueDay",0)) else "Open / Aperta")
                WorkPanel.label(action_list,"%s → %s · %s · €%s\nDue / Scadenza day %s · Balance / Residuo €%s · %s" % [invoice.get("buyer",""),invoice.get("supplier",""),invoice.get("item",""),invoice.get("amount",0),invoice.get("dueDay",0),invoice.get("balance",0),status_text])
            for report in gov.get("reports",[]):
                WorkPanel.label(action_list,"Day / Giorno %s · 06:30–06:30\nCash / Cassa €%s → €%s · Net cash / Flusso netto €%s\nPayroll / Stipendi €%s · Arrears / Arretrati €%s · Bank debt / Debito bancario €%s" % [report.get("day",0),report.get("openingCash",0),report.get("closingCash",0),report.get("netCashChange",0),report.get("payrollPaid",0),report.get("wageDebt",0),report.get("bankDebt",0)])
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
    for action in residents.world.get("actions",[]):
        if work_only and not str(action.get("id","")).begins_with("firm:"):continue
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
        reserve_bus.hide()
        if is_instance_valid(reserve_truck):reserve_truck.hide()
        if is_instance_valid(reserve_scooter):reserve_scooter.hide()
        if is_instance_valid(reserve_motorcycle):reserve_motorcycle.hide()
        if is_instance_valid(own_scooter):own_scooter.hide()
        if is_instance_valid(own_motorcycle):own_motorcycle.hide()
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
    var weather_kind:=str(weather.get("sky","CLEAR"))
    var overcast:=weather_kind!="CLEAR"
    var wet:=weather_kind=="RAIN"
    (road_material as ShaderMaterial).set_shader_parameter("wetness",1.0 if wet else 0.0)
    daylight.rotation_degrees=Vector3(-12.0-sun_amount*55.0,-35.0+(hour-12.0)*7.0,0)
    daylight.light_energy=(0.08+sun_amount*0.90)*(0.58 if overcast else 1.0)
    daylight.light_color=Color("#e6bd94").lerp(Color("#fff4e3"),sun_amount)
    sky_environment.background_color=Color("#263447").lerp(Color("#9da8b0") if overcast else Color("#a7c7de"),sun_amount)
    var daylight_blend:=smoothstep(0.0,0.40,sun_amount)
    var horizon:=Color("#edc49a").lerp(Color("#d3e3ef"),smoothstep(0.0,0.75,sun_amount))
    if overcast:horizon=Color("#b4c1ca")
    sky_material.sky_top_color=Color("#101b31").lerp(Color("#788995") if overcast else Color("#5284bd"),daylight_blend)
    sky_material.sky_horizon_color=Color("#263447").lerp(horizon,daylight_blend)
    sky_material.ground_horizon_color=sky_material.sky_horizon_color
    sky_material.ground_bottom_color=Color("#32372e").lerp(Color("#858973"),daylight_blend)
    mountain_horizon.update_view(walker.position,sun_amount,overcast,sky_material.sky_horizon_color,not room.is_empty())
    for beam in headlamps:beam.visible=sun_amount<=0.25 or wet
    sky_environment.ambient_light_color=Color("#a1b1ce").lerp(Color.WHITE,sun_amount)
    sky_environment.ambient_light_energy=0.32+sun_amount*0.34
    if detail_materials.has("lamp"):
        var lamp: StandardMaterial3D=detail_materials["lamp"]
        lamp.emission_energy_multiplier=0.10 if sun_amount>0.20 else 2.0
    if detail_materials.has("shop_glass"):
        var shop_glass: StandardMaterial3D=detail_materials["shop_glass"]
        shop_glass.emission_energy_multiplier=0.42 if sun_amount<0.20 else 0.08
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
    var controls:=Node3D.new()
    controls.name="RoadControls"
    node.add_child(controls)
    for record in data.get("controls",[]):Signals.create(controls,record)
    var detail=Architecture.new()
    _build_land(node,key,data,detail)
    var walls := PackedVector3Array()
    var roofs := PackedVector3Array()
    var flat_roofs := PackedVector3Array()
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
        detail.facade(feature)
        var pitched: PackedVector3Array=detail.pitched_roof(feature)
        if not pitched.is_empty():
            roofs.append_array(pitched)
        else:
            for i in triangles:
                if Architecture.roof_kind(str(feature.get("kind","yes")))=="flat":
                    flat_roofs.append(_point(points[i],h))
                else:
                    roofs.append(_point(points[i],h))
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
    _street_signs(node,data.get("roads",[]))
    for record in data.get("trees",[]):detail.tree(record)
    for record in data.get("benches",[]):detail.bench(record)
    node.set_meta("lamp_positions",detail.lamp_positions)
    for record in data.get("rail",[]):_strip(rails,record,0.035)
    _surface(node,walls,wall_material,true,false)
    for kind in detail.batches:
        if not detail_materials.has(kind):detail_materials[kind]=_detail_material(kind)
        _surface(node,detail.batches[kind],detail_materials[kind],false)
    _surface(node,roofs,roof_material,false)
    _surface(node,flat_roofs,flat_roof_material,false)
    _surface(node,roads,road_material,false)
    for kind in ["stone_road","dirt_road"]:
        if not detail_materials.has(kind):detail_materials[kind]=_detail_material(kind)
    _surface(node,stone_roads,detail_materials["stone_road"],false)
    _surface(node,dirt_roads,detail_materials["dirt_road"],false)
    _surface(node,rails,rail_material,false)
    if not detail_materials.has("waterway"):detail_materials["waterway"]=Architecture.material("waterway")
    _surface(node,water,detail_materials["waterway"],false)

func _detail_material(kind: String) -> StandardMaterial3D:
    var mat:=Architecture.material(kind)
    var texture_kind:=Architecture.texture_kind(kind)
    if surface_textures.has(texture_kind):
        Textures.surface(mat,surface_textures[texture_kind],0.55 if texture_kind=="paving" else 0.32)
    return mat

func _street_signs(node: Node3D,records: Array) -> void:
    # Decorative street-name plates, not regulatory signs. Names come from OSM.
    var used: Dictionary={}
    var pole_material:=_material(Color("#686e70"))
    var sign_material:=_material(Color("#ede9dc"))
    for record in records:
        if used.size()>=4:break
        var name:=str(record.get("name","")).strip_edges()
        if name.is_empty() or used.has(name) or not Architecture.street_sign_candidate(record):continue
        var a:=_point(record["a"],0)
        var b:=_point(record["b"],0)
        var direction: Vector3=(b-a).normalized()
        var side:=Vector3(-direction.z,0,direction.x)
        var at:=a.lerp(b,0.5)+side*(float(record["width"])*0.5+1.25)
        var plate:=Node3D.new()
        plate.position=at
        plate.rotation.y=atan2(-side.x,-side.z)
        node.add_child(plate)
        var pole:=MeshInstance3D.new()
        var shaft:=BoxMesh.new()
        shaft.size=Vector3(0.07,2.1,0.07)
        pole.mesh=shaft
        pole.material_override=pole_material
        pole.position.y=1.05
        plate.add_child(pole)
        var board:=MeshInstance3D.new()
        var panel:=BoxMesh.new()
        var title_text:=name.left(40)
        panel.size=Vector3(clampf(float(title_text.length())*0.055+0.22,0.85,2.45),0.28,0.05)
        board.mesh=panel
        board.material_override=sign_material
        board.position.y=2.05
        plate.add_child(board)
        for face in [-1.0,1.0]:
            var label:=Label3D.new()
            label.text=title_text
            label.font_size=48
            label.pixel_size=0.0018
            label.outline_size=0
            label.modulate=Color("#26364a")
            label.position=Vector3(0,2.05,float(face)*0.032)
            label.rotation.y=PI if face<0 else 0.0
            plate.add_child(label)
        used[name]=true

func _build_land(node: Node3D,key: String,data: Dictionary,detail) -> void:
    var coords:=key.split(",")
    var cell:=Vector2i(int(coords[0]),int(coords[1]))
    var nearby: Array=[]
    for x in range(cell.x-1,cell.x+2):
        for y in range(cell.y-1,cell.y+2):
            nearby.append(city["chunks"].get("%s,%s" % [x,y],{}))
    var batches: Dictionary={}
    var tree_sites: Dictionary={}
    for id in data.get("land",[]):
        if not land_records.has(id):continue
        var area: Dictionary=land_records[id]
        var kind:=str(area["kind"])
        for part in Landscape.pieces(area["points"],cell,chunk_size):
            var triangles:=Geometry2D.triangulate_polygon(part)
            if triangles.is_empty():continue
            if not batches.has(kind):batches[kind]=PackedVector3Array()
            var verts: PackedVector3Array=batches[kind]
            var level: float=0.009 if kind=="water" else 0.004
            for i in triangles:verts.append(Vector3(part[i].x,level,part[i].y))
            batches[kind]=verts
            if kind not in ["forest","orchard"]:continue
            # Stable sparse canopy, capped per tile and kept away from mapped paths.
            for x in range(8,int(chunk_size),16):
                for y in range(8,int(chunk_size),16):
                    if tree_sites.size()>=64:break
                    var site:=Vector2(cell)*chunk_size+Vector2(x,y)
                    var site_key:=Vector2i(x,y)
                    if tree_sites.has(site_key) or not Geometry2D.is_point_in_polygon(site,part):continue
                    if not Landscape.clear_of_paths(site,nearby):continue
                    tree_sites[site_key]=true
                    detail.tree({"id":"land-%s-%s-%s" % [key,x,y],"point":[site.x,site.y]})
    for kind in batches:
        if not land_materials.has(kind):land_materials[kind]=Landscape.material(kind)
        _surface(node,batches[kind],land_materials[kind],false)

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
