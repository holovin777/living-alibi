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
var road_material: StandardMaterial3D
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
var actions_panel: PanelContainer
var action_list: VBoxContainer
var owned_car: Node3D
var parked_bike: Node3D

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
    owned_car=traffic._car()
    add_child(owned_car)
    owned_car.hide()
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
    road_material=_material(Color("#64696a"))
    rail_material=_material(Color("#8f8578"))
    var world_environment := WorldEnvironment.new()
    var environment := Environment.new()
    environment.background_mode=Environment.BG_COLOR
    environment.background_color=Color("#a7c7de")
    environment.ambient_light_source=Environment.AMBIENT_SOURCE_COLOR
    environment.ambient_light_color=Color("#ffffff")
    environment.ambient_light_energy=0.65
    world_environment.environment=environment
    add_child(world_environment)
    var sun := DirectionalLight3D.new()
    sun.rotation_degrees=Vector3(-55,-35,0)
    sun.light_energy=0.85
    add_child(sun)
    var ground := StaticBody3D.new()
    add_child(ground)
    var plane := CollisionShape3D.new()
    plane.shape=WorldBoundaryShape3D.new()
    ground.add_child(plane)
    var ground_visual := MeshInstance3D.new()
    var floor_mesh := PlaneMesh.new()
    floor_mesh.size=Vector2(5000,5000)
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
    if actions_panel.visible:
        if event is InputEventKey and event.pressed and event.keycode in [KEY_ESCAPE,KEY_F]:
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
        if event.keycode==KEY_F and residents.connected:
            _open_actions()
            get_viewport().set_input_as_handled()
            return
        if event.keycode==KEY_E:
            residents.enter_district(pos)
        elif event.keycode==KEY_T:
            chat_target=residents.nearby_talker(pos)
            if not chat_target.is_empty():
                chat.clear()
                chat.show()
                Input.mouse_mode=Input.MOUSE_MODE_VISIBLE
                chat.grab_focus()
                get_viewport().set_input_as_handled()
                return
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
        walker.rotate_y(-event.relative.x*0.0025)
        head.rotation.x=clampf(head.rotation.x-event.relative.y*0.0025,-1.45,1.45)

func _physics_process(delta: float) -> void:
    if not ready_to_walk:return
    var direction := Vector3.ZERO
    traffic.update_traffic(Vector2(walker.position.x,walker.position.z),delta)
    residents.update_residents(Vector2(walker.position.x,walker.position.z),delta)
    if city_map.visible or chat.visible or actions_panel.visible or residents.action_pending or (shared_mode and (not residents.connected or bool(residents.world.get("confined",false)) or bool(residents.world.get("rebirth",false)))):
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
    var cycling:=bool(residents.world.get("cycling",false))
    var speed := 8.0 if cycling else (7.0 if Input.is_physical_key_pressed(KEY_SHIFT) else 4.5)
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

func _update_navigation() -> void:
    if not is_instance_valid(walker) or not is_instance_valid(city_map):return
    var pos:=Vector2(walker.position.x,walker.position.z)
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
        hud.text+="\nCurrent: "+str(district.get("label",""))+" · E: enter "+str(nearest.get("label",""))+" · T: talk · F: actions"
        if bool(world.get("confined",false)) or bool(world.get("rebirth",false)):hud.text+="\nOpen regular game to continue custody / rebirth."
        hud.text+="\nHunger %s · Thirst %s · %s" % [world.get("hunger",0),world.get("thirst",0),"Cycling" if world.get("cycling",false) else "Walking"]
        if residents.action_pending:hud.text+="\nJava is processing your action..."
        if not residents.last_message.is_empty():hud.text+="\n"+residents.last_message
        _sync_parked_vehicle(owned_car,world.get("parkedCar"),pos,Vector3(2.5,0.05,0))
        _sync_parked_vehicle(parked_bike,world.get("parkedBicycle"),pos,Vector3(-2,0.05,0))
        if not nearest.is_empty():
            district_sign.position=Vector3(float(nearest["x"]),2.5,float(nearest["z"]))
            district_sign.text=str(nearest.get("label",""))+" · E"
            district_sign.show()
    elif shared_mode:
        district_sign.hide()
        owned_car.hide()
        parked_bike.hide()
        hud.text+="\nConnecting to shared Java world..."
    else:
        hud.text+="\nLocal traffic: %s cars · %s pedestrians (preview)" % [traffic.count("drive"),traffic.count("walk")]
    if not route_status.is_empty():hud.text+="\n"+route_status

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

func _open_actions() -> void:
    for child in action_list.get_children():
        action_list.remove_child(child)
        child.queue_free()
    var title:=Label.new()
    title.text="Actions / Azioni"
    action_list.add_child(title)
    var info:=Label.new()
    info.text="Java validates availability, money and time."
    action_list.add_child(info)
    for action in residents.world.get("actions",[]):
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
    var walls := PackedVector3Array()
    var roofs := PackedVector3Array()
    var roads := PackedVector3Array()
    var rails := PackedVector3Array()
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
    for record in data.get("roads",[]):_strip(roads,record,0.025)
    for record in data.get("rail",[]):_strip(rails,record,0.035)
    _surface(node,walls,wall_material,true)
    _surface(node,roofs,roof_material,false)
    _surface(node,roads,road_material,false)
    _surface(node,rails,rail_material,false)

func _surface(parent: Node3D,vertices: PackedVector3Array,material: StandardMaterial3D,collision: bool) -> void:
    if vertices.is_empty():return
    var builder := SurfaceTool.new()
    builder.begin(Mesh.PRIMITIVE_TRIANGLES)
    for v in vertices:builder.add_vertex(v)
    builder.generate_normals()
    var mesh := builder.commit()
    var instance := MeshInstance3D.new()
    instance.mesh=mesh
    instance.material_override=material
    parent.add_child(instance)
    if collision:
        instance.create_trimesh_collision()
