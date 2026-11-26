extends SceneTree
## Offline scene test: visual districts, gate UI and career kiosk can render
## even if the local Java server is not running. Backend HTTP paths are
## independently exercised by Java CI.

func _initialize() -> void:
    call_deferred("_exercise")

func _exercise() -> void:
    var packed: PackedScene = load("res://scenes/park.tscn")
    if packed == null:
        push_error("Cannot load the initial park scene.")
        quit(1)
        return
    var world = packed.instantiate()
    root.add_child(world)
    await process_frame
    world.GameLanguage.select_locale("en",false)
    var score = world._music.stream
    if not _check(score is AudioStreamWAV and score.loop_mode==AudioStreamWAV.LOOP_FORWARD
            and score.data.size()>100000 and score.loop_end==score.data.size()/2,
            "original music is a looping PCM stream"):
        return
    world._music.set_level(25.0,false)
    if not _check(world._music.target_db(true)<world._music.target_db(false),"music ducks during spoken conversations"):
        return
    world._music.set_level(0.0,false)
    if not _check(world._music.target_db(false)==-80.0,"music can be muted"):
        return
    world._music.set_level(25.0,false)
    if not _check(world._main_panel.theme==world._dialog_panel.theme
            and world._main_panel.theme.get_stylebox("panel","PanelContainer").bg_color.g
                >world._main_panel.theme.get_stylebox("panel","PanelContainer").bg_color.r,
            "HUD and chat share the custom town theme"):
        return

    var clock = preload("res://scripts/LiveClock.gd").new()
    for i in range(5):
        if not _check(not clock.advance(1.0,true), "1:10 waits six real seconds"):
            return
    if not _check(clock.advance(1.0,true), "1:10 advances one game minute"):
        return
    if not _check(not clock.advance(1.0,false) and is_zero_approx(clock.elapsed),
            "busy or menu time does not accumulate"):
        return
    clock.set_speed(60)
    if not _check(clock.advance(1.0,true), "1:60 advances every real second"):
        return
    clock.set_speed(0)
    if not _check(not clock.advance(1.0,true), "paused clock sends no request"):
        return
    world._clock_pending = true
    world._on_request_failed("uncertain response")
    if not _check(not world._clock_pending and world._clock.speed == 0
            and world._clock_selector.get_selected_id() == 0,
            "automatic time pauses after uncertain server response"):
        return
    world._clock_selector.select(3)
    world._on_clock_speed_selected(3)
    if not _check(world._clock.speed == 10, "speed selection restores default"):
        return

    # First person is the only camera mode, including after old V input.
    if not _check(not world._avatar.visible and world._avatar.get_child_count()==0
            and world._camera.position.is_equal_approx(
                    world._avatar.position+Vector3(0,world.EYE_HEIGHT,0)),
            "visitor starts at eye level without a third-person mesh"):
        return
    var legacy_key := InputEventKey.new()
    legacy_key.keycode = KEY_V
    legacy_key.pressed = true
    world._unhandled_key_input(legacy_key)
    world._view_yaw = PI/2.0
    world._view_pitch = 0.25
    world._avatar.position = Vector3(1,0,2)
    world._follow_player()
    if not _check(not world._avatar.visible
            and world._camera.position.is_equal_approx(
                    world._avatar.position+Vector3(0,world.EYE_HEIGHT,0))
            and is_equal_approx(world._camera.rotation.y,world._view_yaw)
            and is_equal_approx(world._camera.rotation.x,world._view_pitch),
            "old V key cannot change first-person viewpoint"):
        return
    world._avatar.position = Vector3.ZERO
    world._view_yaw = 0.0
    world._view_pitch = 0.0
    world._follow_player()

    world._on_world_updated(_world("PARK","Park"))
    world._avatar.position=Vector3.ZERO
    world._view_yaw=0.0
    world._update_first_steps()
    if not _check(world._guide_direction(Vector3(0,0,-12))=="↑ 12 m"
            and world._guide_direction(Vector3(12,0,0))=="→ 12 m"
            and world._guide_label.text.contains("Market"),
            "new visitor has a visible optional job goal and directional guidance"):
        return
    world._view_yaw=PI/2.0
    if not _check(world._guide_direction(Vector3(0,0,-12))=="→ 12 m",
            "guide follows the first-person camera heading"):
        return
    world._view_yaw=0.0
    world._set_guide_enabled(false)
    if not _check(not world._guide_label.visible,"optional guidance can be hidden"):
        return
    world._set_guide_enabled(true)
    world._follow_player()

    var layout = world._town_map
    if not _check(layout.DISTRICTS.size() == 20
            and layout.world_center("PARK") == Vector3.ZERO
            and layout.adjacent("PARK", "SCHOOL")
            and not layout.adjacent("PARK", "QUARRY")
            and not layout.DISTRICTS.has("MARCO_HOME"),
            "public map defines all districts, neighbours and no private residences"):
        return
    world._toggle_map()
    if not _check(world._map_panel.visible and not world._travel_panel.visible
            and not world._action_panel.visible and layout.current_place == "PARK",
            "map opens independently of the gate"):
        return
    world._toggle_map()
    if not _check(not world._map_panel.visible, "map closes without travelling"):
        return

    # The nine surrounding chunks are real scenery, reused across arrivals.
    if not _check(world._chunks.size() == 9 and world._chunks.has("SCHOOL")
            and world._chunks.has("CENTRO") and not world._chunks.has("QUARRY"),
            "park streams its eight nearest neighbours only"):
        return
    if not _check(world._environment.background_mode==Environment.BG_SKY
            and world._environment.sky.sky_material is ProceduralSkyMaterial
            and world._surface_textures.has_all(["grass","paving","asphalt","stucco","terracotta","wood"])
            and world._ground_material.albedo_texture==world._surface_textures["grass"]
            and world._chunks["SCHOOL"]["ground"].albedo_texture==world._surface_textures["grass"]
            and world._scenery.has_node("TreeCrown"),
            "procedural sky, textured shared ground and clustered trees are available"):
        return
    world._set_graphics_quality(0)
    if not _check(not world._sun.shadow_enabled and not world._graphics_details[0].visible,
            "low quality hides optional details and sun shadows"):
        return
    world._set_graphics_quality(2)
    if not _check(world._sun.shadow_enabled and world._graphics_details[0].visible
            and is_equal_approx(world._sun.directional_shadow_max_distance,80.0),
            "high quality restores visual detail and extended shadows"):
        return
    world._set_graphics_quality(1)
    world._update_foliage("AUTUMN",false)
    var autumn: Color = world._leaf_materials[0].albedo_color
    world._update_foliage("WINTER",true)
    if not _check(world._leaf_materials[0].albedo_color != autumn,
            "shared foliage responds to seasons and snow"):
        return
    var sound = world._town_audio
    if not _check(sound._streams.has_all(["grass","stone","wood","asphalt","wind","water","traffic","voices","bird","bell","landing","door","bicycle","industry","crickets","shot"]) and sound.water is AudioStreamPlayer3D
            and sound.traffic is AudioStreamPlayer3D,
            "town has original sound streams and positional audio sources"):
        return
    sound.footstep(0.9,"grass",false,true,true)
    if not _check(sound.steps.stream==sound._streams["grass"],
            "walking distance triggers surface-specific footsteps"):
        return
    sound.play_effect("landing")
    if not _check(sound.effects.stream==sound._streams["landing"],
            "landing uses a distinct impact sound"):
        return
    sound.play_effect("door")
    if not _check(sound.effects.stream==sound._streams["door"]
            and sound.industry is AudioStreamPlayer3D and sound.crickets is AudioStreamPlayer3D,
            "door latch and additional spatial sources exist"):
        return
    sound.play_effect("shot")
    if not _check(sound.effects.stream==sound._streams["shot"],"shooting has a distinct sound"):
        return
    sound.set_enabled(false)
    if not _check(not sound.enabled and not sound.steps.playing and not sound.water.playing and not sound.effects.playing and not sound.bicycle.playing,
            "town sound can be muted independently"):
        return
    sound.set_enabled(true)
    if not _check(world._hands.get_parent()==world._camera and world._hands.held_item=="",
            "first-person hands are attached to the camera, not the hidden avatar"):
        return
    world._set_hands_enabled(false)
    if not _check(not world._hands.visible,"hands can be disabled"):
        return
    world._set_hands_enabled(true)
    var old_bag_snapshot: Dictionary = world._last_snapshot
    world._last_snapshot = {"player":{"bag":{"bread":1,"water":1}}}
    world._cycle_held_item()
    if not _check(world._hands.held_item=="bread","holding selects an item actually in inventory"):
        return
    world._last_snapshot = {"player":{"bag":{}}}
    world._refresh_held_item()
    if not _check(world._hands.held_item=="","removed inventory item no longer appears in hand"):
        return
    world._last_snapshot = old_bag_snapshot
    world._toggle_phone()
    if not _check(world._phone.visible and world._phone.app=="home"
            and not world._foot_controls_available(),
            "phone opens a modal first-person interface"):
        return
    world._phone.update_data({"player":{"incarnation":1,"money":42},"bank":{"personal":99},
        "residents":[{"id":"test-contact","name":"Test Resident","conversations":1}]},
        [{"title":"Test job","wage":25}],[])
    world._phone.open_app("bank")
    if not _check("99" in world._phone._body.text and "42" in world._phone._body.text,
            "phone wallet reflects server snapshot"):
        return
    world._phone.update_data({"player":{"incarnation":1},"contacts":[{"residentId":"test-contact","name":"Test Resident","address":"Via delle Magnolie 12"}]},[],[])
    world._phone.open_app("contacts")
    if not _check("Via delle Magnolie 12" in world._phone._body.text,"phone shows shared address from Java"):
        return
    if not _check("Test Resident" in world._phone._body.text,
            "known residents appear in session contacts"):
        return
    world._phone.update_data({"player":{"incarnation":2}},[],[])
    if not _check(world._phone._contacts.is_empty(),"new life clears local contacts"):
        return
    world._phone.update_data({"day":5,"minute":1435,"player":{"incarnation":2},
        "weather":{"sky":"RAIN","temperatureC":12,"raining":true},"year":1,"dayOfYear":5},[],[])
    world._phone.open_app("weather")
    if not _check("12" in world._phone._body.text,"phone displays committed weather"):
        return
    world._phone.set_alarm(0,5)
    if not _check(world._phone.alarm_at==5*1440+5,"past hour schedules tomorrow"):
        return
    world._close_phone()
    world._phone.update_data({"day":6,"minute":10,"player":{"incarnation":2}},[],[])
    if not _check(world._phone.alarm_at==-1 and world._phone_alert.visible
            and world._phone._messages.size()==1,"alarm rings once when committed time crosses target even with phone closed"):
        return
    world._phone.update_data({"day":6,"minute":11,"player":{"incarnation":2}},[],[])
    if not _check(world._phone._messages.size()==1,"alarm does not repeat"):
        return
    world._phone.set_alarm(7,0)
    world._phone.cancel_alarm()
    if not _check(world._phone.alarm_at==-1,"alarm can be canceled"):
        return
    if not _check(not world._phone.visible,"phone closes"):
        return
    var market_root: Node3D = world._chunks["MARKET"]["root"] if world._chunks.has("MARKET") else null
    if market_root!=null:
        if not _check(market_root.has_node("PhysicalSign"),"market has a physical facade sign"):
            return
    var school_root: Node3D = world._chunks["SCHOOL"]["root"]
    var park_root: Node3D = world._chunks["PARK"]["root"]
    if not _check(park_root.has_node("StreetMarker/PhysicalSign"),"park street has physical name sign"):
        return
    if not _check(school_root.position == Vector3(0,0,-48)
            and park_root.position == Vector3.ZERO,
            "neighbour geometry aligns with the public map coordinates"):
        return
    world._on_world_updated(_world("SCHOOL", "School"))
    if not _check(world._scenery == school_root
            and school_root.position == Vector3.ZERO
            and park_root.position == Vector3(0,0,48)
            and world._chunks.size() == 6,
            "arriving at school reuses visible geometry and unloads distant rows"):
        return
    world._on_world_updated(_world("PARK", "Park"))
    if not _check(world._scenery == park_root and world._chunks.size() == 9,
            "returning to park keeps its cached geometry"):
        return

    var centro_root: Node3D = world._chunks["CENTRO"]["root"]
    world._on_world_updated(_world("CENTRO","Centro Storico"))
    if not _check(world._scenery == centro_root and centro_root.has_node("PiazzaCentrale")
            and world._road_links.size() == 4 and world._park_lamps.size() == 4,
            "historic square has cached scenery, four roads and lamps"):
        return
    if not _check(world._can_stand(Vector3(-13,0,-9),world.WALKER_RADIUS)
            and not world._can_stand(Vector3(-15.8,0,-9),world.WALKER_RADIUS),
            "portico is walkable but building back wall is solid"):
        return
    if not _check(centro_root.has_node("PiazzaCentrale/PasseggiataDeiTigli")
            and world._can_stand(Vector3(6,0,18.0),world.WALKER_RADIUS)
            and not world._can_stand(Vector3(6,0,22),world.WALKER_RADIUS),
            "historic centre promenade expands walkable space without crossing the tile boundary"):
        return
    var food_saved_position: Vector3=world._avatar.position
    for x in [-6.0,6.0]:
        var reached: Vector3=world._move_on_ground(Vector3(x,0,14.0),Vector3(0,0,4.0),world.WALKER_RADIUS)
        world._avatar.position=reached
        if not _check(reached.z>17.8 and world._near_public_counter()
                and world._centro_food_counter()==("pizza" if x<0 else "gelato")
                and not world._can_stand(Vector3(x,0,19.1),world.WALKER_RADIUS),
                "food shop entrance and correct counter are accessible, furniture solid"):
            return
    world._avatar.position=food_saved_position
    var barber_saved_position: Vector3=world._avatar.position
    var barber_arrival: Vector3=world._move_on_ground(Vector3(-7.8,0,9.4),Vector3(0,0,-3.0),world.WALKER_RADIUS)
    world._avatar.position=barber_arrival
    if not _check(centro_root.has_node("PiazzaCentrale/BarbiereDelBorgo/BarberChair")
            and barber_arrival.z<6.6 and world._near_barber()
            and not world._can_stand(Vector3(-7.8,0,5.15),world.WALKER_RADIUS),
            "barber entrance is walkable, service point accessible and chair solid"):
        return
    world._avatar.position=barber_saved_position
    var square := centro_root.get_node("PiazzaCentrale")
    var paving := square.get_node("DucalePebbleMosaic") as MeshInstance3D
    var mosaic := paving.mesh as PlaneMesh
    var mosaic_material := mosaic.material as StandardMaterial3D if mosaic!=null else null
    if not _check(mosaic_material!=null and mosaic_material.albedo_texture != null and square.has_node("Facade_0")
            and square.has_node("Facade_3") and square.has_node("CortileDeiFiori") and square.has_node("DuomoInspiredFacade"),
            "historic square has a textured pebble mosaic, arcades, a cathedral facade and a garden court"):
        return
    if not _check(world._can_stand(Vector3(-6.4,0,-14.3),world.WALKER_RADIUS)
            and not world._can_stand(Vector3(-8.4,0,-14.3),world.WALKER_RADIUS),
            "courtyard centre is accessible and boundary wall blocks walking"):
        return
    for at in [Vector3(0,0,-23.9),Vector3(23.9,0,0),Vector3(0,0,23.9),Vector3(-23.9,0,0)]:
        if not _check(world._can_stand(at,world.WALKER_RADIUS,true),
                "all four Centro exits remain walkable"):
            return
    world._on_world_updated(_world("PARK","Park"))

    var open_portici := _world("PORTICI","Via dei Portici")
    open_portici["minute"]=600
    world._on_world_updated(open_portici)
    if not _check(world._scenery.has_node("ViaDeiPortici/BottegaAlimentare")
            and world._road_links.size() == 4
            and world._road_destination(Vector3(0,0,-23.9),23.8) == "CENTRO",
            "historic street connects to the square and has a real shop doorway"):
        return
    var entered: Vector3 = world._move_on_ground(Vector3(3,0,5.4),Vector3(4,0,0),world.WALKER_RADIUS)
    if not _check(entered.x > 6.8 and not world._can_stand(Vector3(4.4,0,3.2),world.WALKER_RADIUS),
            "walk through open grocery door, but not through its wall"):
        return
    world._avatar.position=Vector3(3,0,5.3)
    open_portici["minute"]=1200
    world._on_world_updated(open_portici)
    world._tick_public_door(1.0)
    var blocked: Vector3=world._move_on_ground(Vector3(3,0,5.3),Vector3(3,0,0),world.WALKER_RADIUS)
    if not _check(blocked.x<4.4 and not world._door_open_hours(world._public_door()),"closed shop door blocks entry"):
        return
    world._avatar.position=Vector3(6,0,5.3)
    world._tick_public_door(1.0)
    var exit: Vector3=world._move_on_ground(world._avatar.position,Vector3(-3,0,0),world.WALKER_RADIUS)
    if not _check(exit.x<4.0,"after-hours customer can safely leave"):
        return
    world._avatar.position=exit
    world._tick_public_door(1.0)
    open_portici["minute"]=600
    world._on_world_updated(open_portici)
    world._tick_public_door(1.0)
    world._avatar.position=world.GROCERY_COUNTER
    if not _check(world._can_stand(world.GROCERY_COUNTER,world.WALKER_RADIUS) and world._near_public_counter() and world._near_kiosk(),"visitor reaches unobstructed grocery checkout"):
        return
    var visitor_bakery := _world("BAKERY","Bakery")
    visitor_bakery["shop"]=[{"id":"bread","label":"Bread","price":3,"stock":5}]
    world._on_world_updated(visitor_bakery)
    world._avatar.position=world.BAKERY_COUNTER
    world._toggle_actions()
    if not _check(world._near_public_counter() and world._action_panel.visible and _has_button(world._action_choices,"Buy Bread"),"ordinary visitor can use bakery checkout"):
        return
    world._on_world_updated(_world("LIBRARY","Library"))
    var library_walk: Vector3=world._move_on_ground(Vector3(-7,0,2.8),Vector3(0,0,3.3),world.WALKER_RADIUS)
    world._avatar.position=library_walk
    if not _check(world._scenery.has_node("BibliotecaComunale") and library_walk.z>5.8 and world._near_public_counter() and not world._can_stand(Vector3(-10,0,4),world.WALKER_RADIUS),"library doorway and reception accessible, walls remain solid"):
        return
    world._on_world_updated(_world("PARK","Park"))

    var guest := _world("NORTH_RESIDENCES","North residences")
    world._on_world_updated(guest)
    guest["contacts"]=[{"residentId":"marco","name":"Marco","address":"Via delle Magnolie 12"}]
    world._on_world_updated(guest)
    var bell_position := Vector3(12.2,0,-2.8)
    world._avatar.position=bell_position
    if not _check(world._near_intercom()==1,"intercom detects the correct nearby facade"):
        return
    world._open_intercom()
    if not _check(world._intercom.visible and world._intercom_contacts.item_count==1 and not world._foot_controls_available(),"intercom lists only shared contacts and blocks movement"):
        return
    world._close_intercom()
    world._avatar.position=Vector3(11,0,-2.8)
    if not _check(world._near_stairwell(),"public stairwell is accessed from Magnolia entrance"):
        return
    world._set_stairwell(true)
    world._avatar.position=Vector3(-0.9,0,1)
    if not _check(world._stairwell.near_unit(world._avatar.position)==1,"numbered doorway proximity"):
        return
    world._open_guest({"unit":1,"level":2,"style":"rosa","name":"Marco","activity":"COOKING","cleanliness":65})
    if not _check(world._host_activity=="COOKING" and not world._host_path.is_empty() and world._guest.get_node("Clutter").visible,"server home routine generates a route and cleanliness decor"):
        return
    world._sync_host_routine({"activity":"FAMILY_DINNER","cleanliness":95,"household":{"members":3,"present":3,"dinnerNow":true,"visibleResidents":[{"id":"guest-child","age":8,"activity":"FAMILY_DINNER"},{"id":"guest-adult","age":35,"activity":"FAMILY_DINNER"}]}})
    if not _check(world._host_activity=="FAMILY_DINNER" and world._guest.get_node("FamilyDinner").visible and not world._guest.get_node("FamilyDinner").get_child(3).visible,"family dinner places match present household"):
        return
    var family := world._guest.get_node("GuestFamily") as Node3D
    if not _check(family.get_child_count()==2 and not family.get_child(0).get_node("ResidentName").visible and world._guest.can_stand(family.get_child(0).position,0.3),"present relatives appear without names at clear positions"):
        return
    if not _check(not world._host_path.is_empty() and world._guest.can_stand(world._guest.routine_anchor("FAMILY_DINNER"),0.25),"dinner chair has a navigable approach"):
        return
    for tick in range(160):
        world._animate_guest_host(0.1)
    if not _check(world._host_seated and world._guest_host.position.is_equal_approx(world._guest.routine_seat("FAMILY_DINNER")+Vector3(0,-0.34,0)),"host sits at dinner table"):
        return
    var child := family.get_node("Resident_guest-child") as Node3D
    var before_play := child.position
    var playing := {"activity":"FAMILY_DINNER","cleanliness":95,"household":{"members":3,"present":3,"dinnerNow":true,"visibleResidents":[{"id":"guest-child","age":8,"activity":"PLAYING"},{"id":"guest-adult","age":35,"activity":"FAMILY_DINNER"}]}}
    world._sync_host_routine(playing)
    if not _check(child.position.is_equal_approx(before_play) and not child.get_meta("family_path").is_empty(),"child plans a route instead of teleporting"):
        return
    for tick in range(120):
        world._animate_guest_family(0.1)
        if not _check(world._guest.can_stand(child.position,0.25),"child navigation avoids furniture"):
            return
    if not _check(child.position.distance_to(before_play)>0.5 and child.position.distance_to(child.get_meta("family_goal"))<0.05 and child.get_node("ArmRight/FamilyToy").visible,"child reaches play station and holds toy"):
        return
    var arrived := child.position
    world._sync_host_routine(playing)
    if not _check(child.position.is_equal_approx(arrived) and child.get_meta("family_path").is_empty(),"unchanged snapshot does not restart movement"):
        return
    world._sync_host_routine({"activity":"READING","cleanliness":95,"household":{"members":3,"present":1,"dinnerNow":false}})
    if not _check(not world._host_seated and world._guest_host.position.is_equal_approx(world._guest.routine_anchor("FAMILY_DINNER")),"host stands at safe approach when dinner ends"):
        return
    if not _check(family.get_child_count()==0,"departed family members disappear from apartment"):
        return
    if not _check(not world._guest.get_node("FamilyDinner").visible,"finished dinner clears table preview"):
        return
    if not _check(world._host_activity=="READING" and not world._host_path.is_empty() and not world._guest.get_node("Clutter").visible,"host changes routine and home cleanliness updates"):
        return
    if not _check(world._guest_active and world._guest.has_node("CoffeeMachine")
            and not world._guest.can_stand(Vector3(-3.1,0,-2.6),world.WALKER_RADIUS)
            and world._guest.can_stand(world._avatar.position,world.WALKER_RADIUS),"furnished guest room has walkable space and solid furniture"):
        return
    if not _check(is_instance_valid(world._guest_host) and world._guest_host.get_parent().name=="GuestHost","host uses resident model in apartment"):
        return
    world._avatar.position=Vector3(-1.0,0,1.8)
    world._toggle_guest_seat()
    if not _check(world._guest_sitting and absf(world._camera.position.y-1.12)<0.01,"sitting lowers first person viewpoint"):
        return
    world._toggle_guest_seat()
    if not _check(not world._guest_sitting and world._avatar.position.is_equal_approx(Vector3(-1,0,1.8)),"stand restores original position"):
        return
    world._guest.show_coffee()
    if not _check(world._guest.get_node("CoffeeTray").visible,"coffee cups appear after accepted action"):
        return
    world._close_guest()
    if not _check(not world._guest_active and world._avatar.position.is_equal_approx(Vector3(-0.9,0,1)),"guest exit returns to apartment door"):
        return
    world._avatar.position=world.PublicStairwell.SPAWN
    var climb: Vector3 = world._move_on_ground(world._avatar.position,Vector3(0,0,-14.8),world.WALKER_RADIUS)
    if not _check(world._stairwell_active and absf(climb.y-6.2)<0.01 and world._stairwell.floor_number(climb)==2
            and not world._can_stand(Vector3(1.5,climb.y,climb.z),world.WALKER_RADIUS)
            and not world._can_walk_roads(),"stairs reach second upper level; side walls and district travel blocked"):
        return
    var down: Vector3 = world._move_on_ground(climb,Vector3(0,0,14.8),world.WALKER_RADIUS)
    if not _check(absf(down.y)<0.01 and world._stairwell.near_exit(down),"stairs descend back to entrance"):
        return
    world._avatar.position=down
    world._set_stairwell(false)
    if not _check(not world._stairwell_active and world._avatar.position.is_equal_approx(Vector3(11,0,-2.8)),"exit restores street position"):
        return
    world._avatar.position=Vector3.ZERO
    if not _check(world._near_intercom()==0,"door cannot be called from across the district"):
        return
    if not _check(world._scenery.has_node("ViaDelleMagnolie/CasaMagnolia/HomeApproach")
            and world._scenery.has_node("ViaDelleMagnolie/CasaTiglio")
            and world._can_stand(Vector3(11,0,-2.8),world.WALKER_RADIUS)
            and not world._can_stand(Vector3(11,0,-6.8),world.WALKER_RADIUS),
            "Lombard neighborhood has walkable sidewalk, distinct buildings and solid facades"):
        return
    if not _check(world._scenery.has_node("CasaOspiti/LeaseDoor")
            and not world._can_stand(Vector3(-5,0,8.5),world.WALKER_RADIUS),
            "unrented guest room is locked"):
        return
    guest["player"]["roomActive"] = true
    guest["player"]["roomUntil"] = 10080
    world._on_world_updated(guest)
    var door := world._scenery.get_node("CasaOspiti/LeaseDoor") as Node3D
    var room_path: Vector3 = world._move_on_ground(Vector3(-3,0,8.5),Vector3(-6.7,0,0),world.WALKER_RADIUS,true)
    if not _check(not door.visible and room_path.x < -9.5,
            "paid room opens and bedroom can be reached through both doorways"):
        return
    world._avatar.position = Vector3(-9.8,0,7.7)
    world._render_actions()
    if not _check(world._near_room_bed() and _has_button(world._action_choices,"Sleep in guest room"),
            "bed gives sheltered sleep actions instead of reception"):
        return
    world._avatar.position = Vector3(-5.8,0,11.1)
    world._render_actions()
    if not _check(world._near_kitchen() and _has_button(world._action_choices,"Cook bread soup")
            and _has_button(world._action_choices,"Store bread"),"kitchen exposes cooking and storage"):
        return
    world._avatar.position = Vector3(-10.6,0,12.2)
    world._render_actions()
    if not _check(world._near_bath() and _has_button(world._action_choices,"Wash"),"bathroom exposes hygiene"):
        return
    guest["weather"] = {"sky":"RAIN","raining":true}
    world._on_world_updated(guest)
    if not _check(world._weather_fx.sheltered and not world._weather_fx.rain.emitting,
            "room suppresses local precipitation"):
        return
    guest["player"]["roomActive"] = false
    world._on_world_updated(guest)
    if not _check(door.visible and world._avatar.position == world.ROOM_EXIT
            and not world._weather_fx.sheltered,
            "expired lease moves visitor safely outside and closes entrance"):
        return
    world._on_world_updated(_world("PARK","Park"))

    world._on_world_updated(_world("FARM","Cascina delle Risaie"))
    if not _check(world._scenery.has_node("CascinaDelleRisaie")
            and world._road_destination(Vector3(0,0,-23.9),23.8) == "RIVER"
            and not world._can_stand(Vector3(10,0,-8),world.WALKER_RADIUS)
            and _has_button(world._action_choices,"Apply: farm worker"),
            "farm district is connected with solid paddy and available job"):
        return
    world._on_world_updated(_world("PARK","Park"))

    var cyclist := _world("PARK","Park")
    cyclist["player"]["ownsBicycle"] = true
    cyclist["player"]["cycling"] = true
    world._on_world_updated(cyclist)
    if not _check(world._movement_speed() == world.CYCLE_SPEED
            and world._transport_button.text.contains("cycling"),
            "confirmed bicycle mode speeds up local outdoor travel"):
        return
    if not _check(world._bike_model.visible and world._scenery.has_node("BicycleRack/ParkedBicycle"),
            "cycling has a visible bike model and public rack"):
        return
    cyclist["player"]["cycling"] = false
    cyclist["player"]["bicycleParked"] = "PARK"
    world._on_world_updated(cyclist)
    var parked_model := world._scenery.get_node("BicycleRack/ParkedBicycle") as Node3D
    if not _check(parked_model.visible and not world._bike_model.visible and world._transport_button.disabled,
            "parked bicycle remains at rack and cannot be ridden"):
        return
    cyclist["player"]["bicycleParked"] = null
    world._on_world_updated(cyclist)
    if not _check(world._movement_speed() == world.WALK_SPEED,
            "walking mode restores ordinary movement"):
        return

    if not _check(layout.motor_road("WORKSHOP","FACTORY")
            and layout.motor_road("FACTORY","WORKSHOP")
            and not layout.motor_road("PARK","CENTRO"),
            "motor links are symmetric and historic square remains pedestrian"):
        return
    world._on_world_updated(_world("WORKSHOP","Workshop"))
    if not _check(world._scenery.has_node("OfficinaMotorCourt/ServiceGarage/CityCar")
            and world._scenery.has_node("DistrictRoads/MotorLane")
            and not world._can_stand(Vector3(-13.1,0,-10),world.WALKER_RADIUS),
            "parking court has a solid car and marked motor links"):
        return
    if not _check(world._traffic_edges.has("FACTORY:WORKSHOP")
            and world._scenery.has_node("DistrictRoads/MotorLane/TrafficSignal")
            and world._traffic_phase(0.0)==2 and world._traffic_phase(11.0)==1
            and world._traffic_phase(16.0)==0,
            "loaded motor link has cars and a three-phase crossing signal"):
        return
    var traffic: Dictionary = world._traffic_edges["FACTORY:WORKSHOP"]
    var sample_car: Node3D = traffic["cars"][0]["node"]
    if not _check(sample_car.has_meta("body_style") and sample_car.get_child_count()>20,
            "traffic uses detailed compact vehicle models"):
        return
    var inbound: Dictionary = traffic["cars"][1]
    inbound["delay"] = 0.0
    inbound["distance"] = 14.9
    world._traffic_seconds = 16.0
    world._position_traffic_edge(traffic,"WORKSHOP")
    world._advance_traffic(0.1)
    if not _check(is_equal_approx(float(inbound["distance"]),15.0),
            "car stops before crossing on red"):
        return
    world._traffic_seconds = 0.0
    world._advance_traffic(0.1)
    if not _check(float(inbound["distance"])>15.0,
            "car proceeds on green"):
        return
    var inbound_car: Node3D = inbound["node"]
    world._avatar.position = inbound_car.position+Vector3(0,0,-3)
    var stopped := float(inbound["distance"])
    world._advance_traffic(0.1)
    if not _check(is_equal_approx(float(inbound["distance"]),stopped)
            and not world._can_stand(inbound_car.position,world.WALKER_RADIUS,true),
            "car yields to pedestrian ahead and has a collision footprint"):
        return
    world._avatar.position = Vector3.ZERO
    var garage_entry: Vector3 = world._move_on_ground(Vector3(7.5,0,5),Vector3(0,0,7),world.WALKER_RADIUS)
    if not _check(garage_entry.z > 11.9
            and not world._can_stand(Vector3(10,0,12),world.WALKER_RADIUS)
            and world._road_destination(Vector3(0,0,-23.9),23.8) == "FACTORY",
            "open garage bay remains walkable, walls and district seam intact"):
        return
    world._on_world_updated(_world("PARK","Park"))

    var driver := _world("WORKSHOP","Workshop")
    driver["player"]["carPlace"] = "WORKSHOP"
    driver["player"]["carFuel"] = 6
    driver["player"]["carCondition"] = 80
    driver["player"]["carDestinations"] = ["LIBRARY","FACTORY","QUARRY"]
    world._on_world_updated(driver)
    world._avatar.position = world.CAR_ARRIVAL
    world._render_actions()
    var owned_car := world._scenery.get_node("PublicCarPark/OwnedCar") as Node3D
    if not _check(owned_car.visible and world._near_car()
            and _has_button(world._action_choices,"Drive to Library")
            and not world._can_stand(world.CAR_PARK,world.WALKER_RADIUS),
            "owned car is parked physically and offers nearby motor routes"):
        return
    var controls = preload("res://scripts/DriveControls.gd").new()
    for i in range(100):
        controls.accept(controls.propose(0.1,1.0,0.0,false,0))
    if not _check(is_equal_approx(controls.distance,15.0) and is_zero_approx(controls.speed),
            "manual drive stops at red crossing"):
        return
    for i in range(15):
        controls.accept(controls.propose(0.1,1.0,1.0,false,2))
    if not _check(controls.distance>15.0 and controls.lateral<=1.851 and controls.speed>0,
            "gas and limited steering move car on green"):
        return
    controls.accept(controls.propose(0.1,0.0,0.0,true,2))
    if not _check(controls.speed<4.5,"brake slows car"):
        return
    for edge in world._traffic_edges.values():
        for record in edge["cars"]:
            record["node"].visible = false
    if not _check(world._begin_driving("LIBRARY") and world._cockpit.visible
            and world._camera.position.is_equal_approx(world._avatar.position+Basis(Vector3.UP,world._view_yaw)*world.DRIVER_EYE),
            "manual driving starts in first-person cabin"):
        return
    world._end_driving(true)
    if not _check(not world._cockpit.visible and world._avatar.position==world.CAR_ARRIVAL
            and world._camera.position.is_equal_approx(world.CAR_ARRIVAL+Vector3(0,world.EYE_HEIGHT,0)),
            "cancelling restores first-person walking at original car"):
        return
    driver = driver.duplicate(true) # A fresh server response, not mutation of the previous snapshot.
    driver["player"]["carPlace"] = "LIBRARY"
    driver["player"]["placeId"] = "LIBRARY"
    driver["player"]["place"] = "Library"
    world._on_world_updated(driver)
    if not _check(world._avatar.position==world.CAR_ARRIVAL and world._near_car()
            and world._camera.position.is_equal_approx(world.CAR_ARRIVAL+Vector3(0,world.EYE_HEIGHT,0)),
            "confirmed car trip arrives at destination parking"):
        return
    driver = driver.duplicate(true)
    driver["player"]["carDestinations"] = ["WORKSHOP","OFFICE"]
    world._on_world_updated(driver)
    for edge in world._traffic_edges.values():
        for record in edge["cars"]:
            record["node"].visible = false
    if not _check(world._begin_driving("OFFICE"),"second motor route can start"):
        return
    world._drive_pending = true
    driver = driver.duplicate(true)
    driver["player"]["placeId"] = "OFFICE"
    driver["player"]["place"] = "Office"
    driver["player"]["carPlace"] = "OFFICE"
    driver["player"]["carDestinations"] = ["FACTORY","LIBRARY"]
    driver["player"]["carFuel"] = 5
    world._on_world_updated(driver)
    if not _check(world._drive_parked and world._drive_target.is_empty()
            and world._cockpit.visible and world._avatar.position==world.CAR_PARK
            and world._camera.position.is_equal_approx(world.CAR_PARK+world.DRIVER_EYE)
            and world._drive_hud.text.contains("1: Factory"),
            "confirmed trip keeps the driver seated at the new parking space"):
        return
    for edge in world._traffic_edges.values():
        for record in edge["cars"]:
            record["node"].visible = false
    if not _check(world._begin_driving("FACTORY") and not world._drive_parked and world._cockpit.visible,
            "choose another adjacent road without leaving the cabin"):
        return
    world._end_driving(true)
    world._on_world_updated(_world("PARK","Park"))

    var station := _world("FUEL_STATION","Stazione di servizio")
    station["player"]["carPlace"] = "FUEL_STATION"
    station["fuel"] = {"stock":48,"capacity":48,"price":2}
    world._on_world_updated(station)
    if not _check(layout.motor_road("QUARRY","FUEL_STATION")
            and world._scenery.has_node("StazioneDiServizio/StationShop")
            and not world._can_stand(Vector3(-12,0,-6),world.WALKER_RADIUS)
            and _has_button(world._action_choices,"Refuel")
            and _has_button(world._action_choices,"Apply: fuel station attendant"),
            "station has solid pumps, a connected motor road and local services"):
        return
    world._on_world_updated(_world("PARK","Park"))

    var cargo := _world("QUARRY","Quarry")
    cargo["player"]["jobId"] = "freight-driver"
    cargo["freight"] = {"active":true,"truckPlace":"QUARRY","nextDestination":"WORKSHOP","iron":2,"copper":1}
    world._on_world_updated(cargo)
    world._avatar.position = world.TRUCK_EXIT
    world._render_actions()
    if not _check(world._near_truck()
            and world._scenery.get_node("FreightParking/AssignedTruck").visible
            and _has_button(world._action_choices,"Drive truck to Workshop"),
            "assigned truck has visible cargo and a legal route"):
        return
    for edge in world._traffic_edges.values():
        for record in edge["cars"]:
            record["node"].visible = false
    if not _check(world._begin_driving("WORKSHOP",true) and world._drive_freight
            and world._cockpit.visible and world._drive_hud.text.contains("FREIGHT"),
            "truck uses the first-person driving controls"):
        return
    world._end_driving(true)
    world._on_world_updated(_world("PARK","Park"))

    var produce := _world("FARM","Cascina delle Risaie")
    produce["player"]["jobId"] = "freight-driver"
    produce["freight"] = {"active":true,"truckPlace":"FARM","nextDestination":"BAKERY","firstLabel":"Vegetables","secondLabel":"Rice","firstUnits":2,"secondUnits":1}
    world._on_world_updated(produce)
    world._avatar.position = world._truck_exit("FARM")
    world._render_actions()
    if not _check(layout.motor_road("FARM","BAKERY") and layout.motor_road("CAFE","SOUTH_RESIDENCES")
            and layout.motor_road("SOUTH_RESIDENCES","MARKET") and not layout.motor_road("PORTICI","CENTRO")
            and world._near_truck() and world._scenery.get_node("FreightParking/AssignedTruck").visible
            and world._action_title.text.contains("Vegetables 2") and _has_button(world._action_choices,"Drive truck to Bakery"),
            "farm freight route and cargo display preserve pedestrian centre"):
        return
    world._on_world_updated(_world("PARK","Park"))

    var carrier := _world("WORKSHOP","Workshop")
    carrier["logistics"] = {"registered":true,"yours":true,"cash":300,"truck":false}
    world._on_world_updated(carrier)
    if not _check(_has_button(world._action_choices,"Logistics: invest 100 EUR")
            and _has_button(world._action_choices,"Buy company truck"),
            "owner can manage separate carrier funds and purchase a truck"):
        return
    carrier = _world("FACTORY","Factory")
    carrier["logistics"] = {"registered":true,"yours":true,"cash":80,"truck":true,"truckPlace":"WORKSHOP"}
    world._on_world_updated(carrier)
    if not _check(_has_button(world._action_choices,"Company freight contract"),
            "company owner can accept freight without being an employee"):
        return
    world._on_world_updated(_world("PARK","Park"))

    carrier = _world("WORKSHOP","Workshop")
    carrier["logistics"] = {"registered":true,"yours":true,"cash":100,"truck":true,"driverName":null,"candidates":[{"id":"worker","name":"Test Driver"}]}
    world._on_world_updated(carrier)
    if not _check(_has_button(world._action_choices,"Hire driver: Test Driver"),"real resident hiring appears in carrier menu"):
        return
    carrier = carrier.duplicate(true)
    carrier["logistics"]["driverName"] = "Test Driver"
    carrier["logistics"]["automatic"] = false
    carrier["logistics"]["condition"] = 65
    carrier["logistics"]["truckPlace"] = "WORKSHOP"
    carrier["logistics"]["financials"] = {"revenue":40,"operatingNet":20}
    world._on_world_updated(carrier)
    if not _check(_has_button(world._action_choices,"Enable automatic freight")
            and _has_button(world._action_choices,"Service company truck"),"owner can enable contracts and repair a worn truck"):
        return
    world._on_world_updated(_world("PARK","Park"))

    carrier = _world("WORKSHOP","Workshop")
    carrier["logistics"] = {"registered":true,"yours":true,"cash":300,"truck":true,"driverName":"Driver One","secondTruck":{"purchased":false}}
    world._on_world_updated(carrier)
    if not _check(_has_button(world._action_choices,"Buy second truck"),"company can expand to second truck"):
        return
    carrier = carrier.duplicate(true)
    carrier["logistics"]["secondTruck"] = {"purchased":true,"place":"WORKSHOP","condition":65,"driverName":"Driver Two","active":false,"automatic":false}
    world._on_world_updated(carrier)
    if not _check(_has_button(world._action_choices,"Enable truck 2 dispatch") and _has_button(world._action_choices,"Service truck 2"),
            "second truck has independent dispatch and maintenance controls"):
        return
    world._on_world_updated(_world("PARK","Park"))

    var depot := _world("LOGISTICS_DEPOT","Deposito Logistico")
    depot["logistics"] = {"registered":true,"yours":true,"cash":100,"truck":true,"truckPlace":"LOGISTICS_DEPOT","condition":80,
        "secondTruck":{"purchased":true,"place":"LOGISTICS_DEPOT","condition":90}}
    world._on_world_updated(depot)
    world._avatar.position = world.DEPOT_DESK+Vector3(0,0,-1.2)
    world._render_actions()
    if not _check(layout.motor_road("FUEL_STATION","LOGISTICS_DEPOT")
            and not layout.motor_road("FARM","LOGISTICS_DEPOT")
            and world._scenery.has_node("DepositoLogistico/Office")
            and world._scenery.get_node("DepositoLogistico/Garage/Truck1").visible
            and world._scenery.get_node("DepositoLogistico/Garage/Truck2").visible
            and world._near_kiosk() and _has_button(world._action_choices,"Logistics: invest 100 EUR")
            and world._can_stand(Vector3(-9,0,6),world.WALKER_RADIUS)
            and not world._can_stand(Vector3(-14,0,10),world.WALKER_RADIUS)
            and not world._can_stand(Vector3(6.5,0,12),world.WALKER_RADIUS),
            "depot office doorway, management desk, garage trucks and road are accessible"):
        return
    world._on_world_updated(_world("PARK","Park"))

    var home := _world("SOUTH_RESIDENCES","South residences")
    world._on_world_updated(home)
    world._avatar.position = world.KIOSK_POSITION+Vector3(0,0,1.5)
    world._render_actions()
    var house_door := world._scenery.get_node("CasaDelGlicine/LeaseDoor") as Node3D
    if not _check(house_door.visible and not world._can_stand(Vector3(-5,0,8.5),world.WALKER_RADIUS)
            and _has_button(world._action_choices,"Buy Casa del Glicine"),"unowned house is locked and offered for sale"):
        return
    home = home.duplicate(true)
    home["player"]["ownsHouse"] = true
    world._on_world_updated(home)
    world._avatar.position = Vector3(-9.8,0,7.7)
    world._render_actions()
    if not _check(not house_door.visible and world._near_room_bed()
            and _has_button(world._action_choices,"Sleep in your house"),"owned house offers permanent sheltered sleep"):
        return
    home = home.duplicate(true)
    home["garden"] = {"planted":true,"growth":3,"required":3,"barrel":true,"rainwater":7}
    world._on_world_updated(home)
    world._avatar.position = world.GARDEN_CENTER+Vector3(-2.8,0,0)
    world._render_actions()
    if not _check(world._near_garden() and _has_button(world._action_choices,"Harvest 4 vegetables")
            and world._scenery.get_node("OrtoPrivato/Stage3").visible
            and world._scenery.get_node("OrtoPrivato/RainBarrel").visible
            and not world._can_stand(world.GARDEN_CENTER+Vector3(2.85,0,0),world.WALKER_RADIUS)
            and not world._can_stand(world.GARDEN_CENTER,world.WALKER_RADIUS),"garden renders mature crop and local harvest"):
        return
    world._on_world_updated(_world("PARK","Park"))

    home = home.duplicate(true)
    home["hens"] = {"coop":true,"hens":2,"health":85,"eggs":3}
    world._on_world_updated(home)
    world._avatar.position = world.COOP_CENTER+Vector3(0,0,3.2)
    world._render_actions()
    if not _check(world._near_coop() and _has_button(world._action_choices,"Collect egg")
            and _has_button(world._action_choices,"Feed hens")
            and world._scenery.get_node("PollaioPrivato/Coop/Hen2").visible
            and not world._scenery.get_node("PollaioPrivato/Coop/Hen3").visible
            and not world._can_stand(world.COOP_CENTER,world.WALKER_RADIUS),
            "coop shows purchased hens and allows care and collection"):
        return
    world._on_world_updated(_world("PARK","Park"))

    home = home.duplicate(true)
    home["orchard"] = {"plants":[{"id":"apples","label":"Apple tree","planted":true,"age":5,"fruit":3,"ready":true,"season":"AUTUMN","marketFree":12}]}
    world._on_world_updated(home)
    world._avatar.position = world.ORCHARD_CENTER
    world._render_actions()
    if not _check(world._near_orchard() and _has_button(world._action_choices,"Harvest apples")
            and world._scenery.get_node("FruttetoPrivato/Plant0/Tree/Fruit").visible
            and not world._can_stand(world.ORCHARD_PLOTS[0],world.WALKER_RADIUS),
            "orchard shows mature fruit and local harvest"):
        return
    world._on_world_updated(_world("PARK","Park"))

    var trade := _world("MARKET","Market")
    trade["player"]["bag"] = {"vegetables":2,"eggs":1,"apples":1}
    trade["orchard"] = {"plants":[{"id":"apples","marketFree":12}],"marketBudget":20}
    trade["hens"] = {"marketFree":1,"marketBudget":20}
    trade["garden"] = {"marketFree":1,"marketBudget":20}
    world._on_world_updated(trade)
    world._avatar.position = world.KIOSK_POSITION+Vector3(0,0,1.5)
    world._render_actions()
    if not _check(_has_button(world._action_choices,"Sell vegetables") and _has_button(world._action_choices,"Sell eggs") and _has_button(world._action_choices,"Sell apples"),"market accepts carried produce"):
        return
    world._on_world_updated(_world("PARK","Park"))

    # Physical footprints: long steps cannot tunnel through solid objects.
    var kiosk_stop: Vector3 = world._move_on_ground(Vector3(7,0,6),
            Vector3(0,0,-6), world.WALKER_RADIUS)
    if not _check(kiosk_stop.z > 4.1 and kiosk_stop.z < 4.3
            and world._can_stand(kiosk_stop, world.WALKER_RADIUS),
            "kiosk blocks even a long movement step"):
        return
    var fountain_stop: Vector3 = world._move_on_ground(Vector3(8,0,-1),
            Vector3(0,0,-8), world.WALKER_RADIUS)
    if not _check(fountain_stop.z > -2.8 and fountain_stop.z < -2.5,
            "visitor cannot walk through the fountain"):
        return
    var tree_stop: Vector3 = world._move_on_ground(Vector3(-8,0,-3),
            Vector3(0,0,-4), world.WALKER_RADIUS)
    if not _check(tree_stop.z > -4.5 and tree_stop.z < -4.3,
            "tree trunk blocks movement"):
        return
    var gate_pass: Vector3 = world._move_on_ground(Vector3(0,0,-9),
            Vector3(0,0,-5), world.WALKER_RADIUS)
    if not _check(gate_pass.is_equal_approx(Vector3(0,0,-14)),
            "the open gate remains passable"):
        return
    var slid: Vector3 = world._move_on_ground(Vector3(7,0,4.3),
            Vector3(0.6,0,-1.0), world.WALKER_RADIUS)
    if not _check(slid.x > 7.5 and slid.z >= 4.1,
            "diagonal movement slides along an obstacle"):
        return
    var edge: Vector3 = world._move_on_ground(Vector3(16,0,0),
            Vector3(4,0,0), world.WALKER_RADIUS)
    if not _check(edge.x <= 17.0 and edge.x > 16.8,
            "district boundary remains enforced"):
        return

    if not _check(world._road_links.size() == 4
            and world._road_destination(Vector3(0,0,-23.9),23.8) == "SCHOOL"
            and world._road_destination(Vector3(23.9,0,0),23.8) == "LIBRARY"
            and world._road_destination(Vector3(-23.9,0,0),23.8) == "CENTRO"
            and world._road_destination(Vector3(0,0,23.9),23.8) == "RIVER",
            "park has four walkable connections matching the town layout"):
        return
    var road_end: Vector3 = world._move_on_ground(Vector3(0,0,16),
            Vector3(0,0,12),world.WALKER_RADIUS,true)
    if not _check(road_end.z >= 23.8 and road_end.z <= 24.001,
            "visitor can walk to a road seam"):
        return
    var offroad: Vector3 = world._move_on_ground(Vector3(8,0,16),
            Vector3(0,0,12),world.WALKER_RADIUS,true)
    if not _check(offroad.z > 20.0 and offroad.z <= 21.0
            and world._scenery.has_node("ParkDetails/SouthGardenPromenade")
            and world._can_stand(Vector3(8,0,18.6),world.WALKER_RADIUS),
            "expanded park promenade is walkable but outer boundary remains solid"):
        return
    if not _check(world._scenery.has_node("ParkDetails/ParcoGiochi/SwingFrame")
            and world._scenery.has_node("ParkDetails/ParcoGiochi/Slide")
            and world._can_stand(Vector3(-7.8,0,17.4),world.WALKER_RADIUS)
            and not world._can_stand(Vector3(-9.5,0,13.9),world.WALKER_RADIUS),
            "playground has accessible approach and solid equipment footprints"):
        return
    # Simulate server rejection and committed arrival without a live Java process.
    world._road_pending = "SCHOOL"
    world._road_arrival = Vector3(0,0,23)
    world._on_request_failed("offline test")
    if not _check(world._road_pending.is_empty() and world._road_blocked == "SCHOOL"
            and world._current_place == "PARK", "failed crossing keeps the original district"):
        return
    world._road_pending = "SCHOOL"
    world._view_yaw = 0.25
    world._on_world_updated(_world("SCHOOL", "School"))
    if not _check(world._current_place == "SCHOOL"
            and world._avatar.position == Vector3(0,0,23)
            and is_equal_approx(world._view_yaw,0.25)
            and world._road_destination(Vector3(0,0,23.9),23.8) == "PARK",
            "confirmed road arrival preserves orientation and enters opposite edge"):
        return
    world._road_pending = "PARK"
    world._road_arrival = Vector3(0,0,-23)
    world._on_world_updated(_world("PARK", "Park"))
    world._avatar.position = Vector3.ZERO
    world._view_yaw = 0.0
    world._follow_player()
    world._on_world_updated(_world("RIVER", "River"))
    var bridge: Vector3 = world._move_on_ground(Vector3(-5,0,0),
            Vector3(-12,0,0),world.WALKER_RADIUS,true)
    if not _check(bridge.x < -16.8 and world._can_stand(bridge,world.WALKER_RADIUS,true),
            "river crossing has a walkable bridge"):
        return
    world._on_world_updated(_world("PARK", "Park"))

    var populated_park := _world("PARK", "Park")
    populated_park["residents"] = [
        {"id": "marco", "name": "Marco Bianchi", "activity": "WALKING"},
        {"id": "elena", "name": "Elena Rossi", "activity": "SHOPPING"}
    ]
    world._on_world_updated(populated_park)
    if not _check(world._residents_root.get_child_count() == 2
            and world._residents_root.get_child(0).name == "Resident_marco",
            "only actually co-present residents appear in the park"):
        return
    var marco = world._residents_root.get_node("Resident_marco")
    var appearance: Dictionary = marco.get_meta("appearance")
    var elena = world._residents_root.get_node("Resident_elena")
    if not _check(not marco.get_node("ResidentName").visible and not marco.get_node("ActivityLabel").visible,"resident identity and activity labels hidden"):
        return
    if not _check(appearance == world.ResidentAppearance.profile("marco")
            and appearance != elena.get_meta("appearance")
            and marco.has_node("LegLeft") and marco.has_node("ArmRight"),
            "residents have stable distinct visual profiles and articulated limbs"):
        return
    if not _check(marco.has_node("Head") and marco.has_node("CasualDetails")
            and marco.get_node("CasualDetails").get_child_count()>0
            and appearance.has("clothing_style") and appearance.has("shoe_color"),
            "resident has stable style details and an articulated head"):
        return
    world._animate_resident_head(marco,1.0,2.0,false,false)
    var head_pivot := marco.get_node("Head") as Node3D
    if not _check(absf(head_pivot.rotation.y)>0.001
            and (head_pivot.transform*Vector3(0,1.49,0)).is_equal_approx(Vector3(0,1.49,0)),
            "head looks around without moving its neck pivot"):
        return
    if not _check(world._resident_wave(marco,0.1,true,true)
            and not world._resident_wave(marco,2.0,true,true),
            "familiar greeting ends after a bounded duration"):
        return
    world._resident_wave(marco,0.1,true,false)
    if not _check(not world._resident_wave(marco,0.1,true,true),
            "greeting cooldown prevents repeated waving"):
        return
    marco.remove_meta("wave_remaining")
    marco.remove_meta("wave_near")
    marco.remove_meta("wave_cooldown")
    if not _check(not world._person_clear(Vector3(0,0,0),Vector3(0.4,0,0),0.3,Vector3(0.8,0,0),0.3)
            and world._person_clear(Vector3(0.4,0,0),Vector3(0.3,0,0),0.3,Vector3(0.8,0,0),0.3),
            "personal space blocks penetration but allows escape from overlap"):
        return
    var presence_old: Vector3=marco.position
    var presence_yaw: float=marco.rotation.y
    var presence_avatar: Vector3=world._avatar.position
    marco.position=Vector3(0,0,0)
    marco.rotation.y=0.0
    world._avatar.position=Vector3(0.5,0,-2.0)
    world._resident_attention(marco)
    if not _check(marco.has_meta("attention_yaw") and float(marco.get_meta("attention_yaw"))<0.0,
            "resident notices nearby visitor in front"):
        return
    world._avatar.position=Vector3(0,0,2)
    world._resident_attention(marco)
    if not _check(not marco.has_meta("attention_yaw"),"resident does not stare through the back of the head"):
        return
    world._animate_resident_posture(marco,1.0,1.0,PI/2.0,true,false)
    if not _check(absf(marco.get_node("Shoulders").rotation.y)>0.04 and marco.position==Vector3.ZERO,
            "walking posture rotates shoulders without moving collision body"):
        return
    marco.position=presence_old
    marco.rotation.y=presence_yaw
    world._avatar.position=presence_avatar
    if not _check(world._idle_gesture_choice("relaxed","TIRED","ADULT",0)=="STRETCH"
            and world._idle_gesture_choice("quiet","STRESSED","ADULT",0)=="TAP_FOOT"
            and world._idle_gesture_choice("perfectionist","CALM","ADULT",3)=="CHECK_WATCH"
            and world._idle_gesture_choice("perfectionist","CALM","CHILD",3)=="SCRATCH_HEAD",
            "idle gestures depend on mood, personality and age"):
        return
    marco.set_meta("idle_wait",0.0)
    var first_idle: String=world._tick_idle_gesture(marco,0.1,true)
    if not _check(not first_idle.is_empty()
            and world._tick_idle_gesture(marco,0.2,true)==first_idle
            and world._tick_idle_gesture(marco,0.1,false).is_empty(),
            "idle gesture persists briefly but stops when another action takes priority"):
        return
    if not _check(world._tick_idle_gesture(marco,0.1,true).is_empty(),
            "idle gesture has a cooldown"):
        return
    marco.set_meta("idle_wait",0.0)
    world._tick_idle_gesture(marco,0.1,true)
    if not _check(world._tick_idle_gesture(marco,3.0,true).is_empty(),
            "idle gesture ends without becoming a looping action"):
        return
    var stretch_pose: Vector3=world._idle_arm_pose("STRETCH",true,0.0)
    if not _check(stretch_pose.x>2.0
            and world._idle_arm_pose("CHECK_WATCH",true,0.0)==Vector3.ZERO
            and world._idle_arm_pose("CHECK_WATCH",false,0.0).z>0.0,
            "stretch and wrist check use distinct arm poses"):
        return
    if not _check(world._peer_reaction_phase("LAUGH",7.0)=="CHAT"
            and world._peer_reaction_phase("LAUGH",4.5)=="LAUGH"
            and world._peer_reaction_phase("ARGUMENT",4.5)=="ARGUMENT"
            and world._peer_reaction_phase("RECONCILE",1.5)=="PEACE_GESTURE",
            "social reaction phases distinguish laughter, conflict and reconciliation"):
        return
    marco.set_meta("peer_expression","ENTHUSIASTIC")
    marco.set_meta("peer_reaction","LAUGH")
    world._animate_resident_face(marco,1.0,2.0,false)
    if not _check(marco.get_node("Head/Mouth/Opening").visible
            and marco.get_node("Head/Mouth/Right").rotation.z>0.0,
            "shared laughter opens smiling mouth even when not speaking"):
        return
    marco.remove_meta("peer_expression")
    marco.remove_meta("peer_reaction")
    var companion_marco_position: Vector3=marco.position
    var companion_elena_position: Vector3=elena.position
    var companion_marco_activity: String=str(marco.get_meta("activity",""))
    var companion_elena_activity: String=str(elena.get_meta("activity",""))
    marco.position=Vector3(0,0,0)
    elena.position=Vector3(0,0,-3)
    marco.set_meta("activity","FREE_TIME")
    elena.set_meta("activity","FREE_TIME")
    marco.set_meta("walking_companion","elena")
    elena.set_meta("walking_companion","marco")
    world._tick_walking_companions()
    if not _check(marco.has_meta("companion_target") and elena.has_meta("companion_target")
            and (bool(marco.get_meta("companion_wait")) or bool(elena.get_meta("companion_wait"))),
            "companions wait for a distant partner"):
        return
    elena.set_meta("activity","WORKING")
    world._tick_walking_companions()
    if not _check(not marco.has_meta("companion_target") and not elena.has_meta("companion_target"),
            "companionship yields to work and other priority activities"):
        return
    marco.set_meta("walking_companion","")
    elena.set_meta("walking_companion","")
    marco.position=companion_marco_position
    elena.position=companion_elena_position
    marco.set_meta("activity",companion_marco_activity)
    elena.set_meta("activity",companion_elena_activity)
    var peer_old_marco: Vector3=marco.position
    var peer_old_elena: Vector3=elena.position
    var peer_old_activity: String=str(elena.get_meta("activity",""))
    var peer_marco_activity: String=str(marco.get_meta("activity",""))
    marco.set_meta("activity","FREE_TIME")
    elena.set_meta("activity","FREE_TIME")
    marco.position=Vector3(0,0,0)
    elena.position=Vector3(3,0,0)
    world._last_snapshot["peerEncounters"]=[{"firstId":"marco","secondId":"elena","tick":120,"tone":"WARM"}]
    world._tick_peer_encounters(0.1)
    if not _check(marco.has_meta("peer_target") and elena.has_meta("peer_target"),
            "recent encounter cues approach a visible nearby peer"):
        return
    elena.position=Vector3(1.3,0,0)
    world._tick_peer_encounters(0.1)
    if not _check(marco.has_meta("peer_face") and elena.has_meta("peer_face")
            and bool(marco.get_meta("peer_speaking"))!=bool(elena.get_meta("peer_speaking")),
            "peer conversation uses alternating speakers"):
        return
    world._tick_peer_encounters(10.0)
    world._tick_peer_encounters(0.1)
    if not _check(not marco.has_meta("peer_face") and not elena.has_meta("peer_face"),
            "expired visual encounter does not replay on the next frame"):
        return
    world._last_snapshot.erase("peerEncounters")
    world._tick_peer_encounters(0.1)
    marco.position=peer_old_marco
    elena.position=peer_old_elena
    marco.set_meta("activity",peer_marco_activity)
    elena.set_meta("activity",peer_old_activity)
    var right_arm := marco.get_node("ArmRight") as Node3D
    var right_elbow := right_arm.get_node("Elbow") as Node3D
    var right_foot := marco.get_node("LegRight/Knee/Foot") as Node3D
    if not _check(right_elbow.has_node("Palm") and right_elbow.get_node("Fingers").get_child_count()==5
            and right_foot.get_child_count()>=2,
            "resident has articulated hands with five digits and rounded shoes"):
        return
    var initial_gait: float=world._resident_gait(marco,0.0)
    var advanced_gait: float=world._resident_gait(marco,0.35)
    if not _check(not is_equal_approx(initial_gait,advanced_gait)
            and is_equal_approx(advanced_gait,world._resident_gait(marco,0.0)),
            "gait progresses only over actual distance"):
        return
    world._animate_resident_leg(marco.get_node("LegRight"),1.0,PI/2.0,true,false)
    if not _check(marco.get_node("LegRight/Knee").rotation.x< -0.2
            and absf(right_foot.rotation.x)>0.04,
            "walking bends knee and ankle"):
        return
    var held_phone := right_arm.get_node("Phone") as Node3D
    var phone_rest: Transform3D=held_phone.transform
    world._animate_resident_elbow(right_arm,1.0,-0.5,true)
    if not _check(right_elbow.rotation.x< -0.4
            and not held_phone.position.is_equal_approx(phone_rest.origin)
            and right_elbow.get_node("Fingers").rotation.x< -0.3,
            "elbow articulation carries held objects and curls fingers"):
        return
    world._animate_resident_elbow(right_arm,1.0,0.0,false)
    if not _check(held_phone.transform.is_equal_approx(phone_rest),
            "held props restore their original pose"):
        return
    var face_seed := absi(str(marco.get_meta("resident_id")).hash())
    var face_cycle := 3.2+float(face_seed%19)*0.11
    var blink_at := face_cycle-float(face_seed%97)*0.073+0.09
    marco.set_meta("mood","CALM")
    world._animate_resident_face(marco,1.0,blink_at,false)
    if not _check(marco.get_node("Head/EyeLeft").scale.y<0.15,"resident blinks at stable individual timing"):
        return
    world._animate_resident_face(marco,1.0,blink_at+0.6,false)
    if not _check(marco.get_node("Head/EyeLeft").scale.y>0.9,"resident eyes reopen"):
        return
    marco.set_meta("mood","HAPPY")
    world._animate_resident_face(marco,1.0,blink_at+0.6,false)
    if not _check(marco.get_node("Head/Mouth/Right").rotation.z>0.0
            and marco.get_node("Head/Mouth/Left").rotation.z<0.0,
            "happy mood produces a smile"):
        return
    marco.set_meta("mood","SAD")
    world._animate_resident_face(marco,1.0,blink_at+0.6,false)
    if not _check(marco.get_node("Head/Mouth/Right").rotation.z<0.0,"sad mood produces a frown"):
        return
    marco.set_meta("mood","IRRITATED")
    world._animate_resident_face(marco,1.0,blink_at+0.6,false)
    if not _check(marco.get_node("Head/Hair/BrowRight").rotation.z>0.3,"irritated mood changes brow angle"):
        return
    world._begin_resident_reply(marco,"Hello there.")
    world._animate_resident_face(marco,1.0,blink_at+0.6,true)
    if not _check(float(marco.get_meta("reply_remaining",0.0))>0.0
            and marco.get_node("Head/Mouth/Opening").visible
            and not marco.get_node("Head/Mouth/Center").visible,
            "delivered replies have bounded mouth motion"):
        return
    world._animate_resident_face(marco,1.0,blink_at+1.0,false)
    if not _check(not marco.get_node("Head/Mouth/Opening").visible
            and marco.get_node("Head/Mouth/Center").visible,"mouth closes while listening"):
        return
    marco.set_meta("reply_remaining",0.0)
    marco.set_meta("mood","CALM")
    var adult_scale: Vector3=marco.scale
    var original_appearance: Dictionary=marco.get_meta("appearance").duplicate()
    populated_park["residents"][0]["appearanceStage"]="CHILD"
    world._on_world_updated(populated_park)
    if not _check(world._residents_root.get_node("Resident_marco")==marco
            and marco.scale.y<adult_scale.y*0.8
            and marco.scale.x<adult_scale.x
            and not marco.get_node("Head/FacialHair").visible,
            "children have smaller bodies, no facial hair and retain the same resident model"):
        return
    populated_park["residents"][0]["appearanceStage"]="SENIOR"
    world._on_world_updated(populated_park)
    var hair_mesh := marco.get_node("Head/Hair").get_child(0) as MeshInstance3D
    if not _check(hair_mesh.material_override.albedo_color.is_equal_approx(world.ResidentAppearance.hair_color(original_appearance["hair"],"SENIOR"))
            and marco.get_meta("appearance")==original_appearance
            and marco.scale.y>adult_scale.y*0.9,
            "older residents gain gray hair without losing their stable identity"):
        return
    populated_park["residents"][0]["appearanceStage"]="ADULT"
    world._on_world_updated(populated_park)
    if not _check(marco.scale.is_equal_approx(adult_scale),"adult body scale is restored without compounding"):
        return
    world._sync_resident_uniform(marco,"BAKER_APRON")
    if not _check(marco.has_node("Uniform") and marco.get_node("Uniform").get_child_count()>2,
            "visible work outfit has apron geometry"):
        return
    world._sync_resident_uniform(marco,"")
    if not _check(not marco.has_node("Uniform"),"uniform disappears after work"):
        return
    var previous_weather: Dictionary=world._last_snapshot.get("weather",{}).duplicate()
    world._last_snapshot["weather"]={"temperatureC":3,"raining":true}
    var rain_resident: Node3D=marco
    if absi(str(rain_resident.get_meta("resident_id")).hash())%3==0:
        rain_resident=elena
    var old_id: String=str(rain_resident.get_meta("resident_id"))
    var umbrella_id := "umbrella-test"
    while absi(umbrella_id.hash())%3==0:umbrella_id+="x"
    rain_resident.set_meta("resident_id",umbrella_id)
    if not _check(world._sync_resident_weather(rain_resident,false,false)
            and rain_resident.get_node("Umbrella").visible
            and rain_resident.get_node("WinterClothes").visible,
            "cold rain displays winter clothes and an umbrella outdoors"):
        return
    world._last_snapshot["weather"]={"temperatureC":24,"raining":false}
    world._sync_resident_weather(rain_resident,false,false)
    if not _check(not rain_resident.get_node("Umbrella").visible
            and not rain_resident.get_node("WinterClothes").visible,
            "warm dry weather hides rain and winter accessories"):
        return
    rain_resident.set_meta("resident_id",old_id)
    world._last_snapshot["weather"]=previous_weather
    var start_position: Vector3 = marco.position
    world._animate_residents(0.5)
    if not _check(marco.position.distance_to(start_position) > 0.01,
            "walking residents move within the local visual scene"):
        return
    world._on_world_updated(populated_park)
    if not _check(world._chunks["SCHOOL"]["residents"].get_child_count() == 0,
            "neighbour scenery never fabricates unseen residents"):
        return
    if not _check(world._residents_root.get_node("Resident_marco") == marco,
            "refresh retains the same resident model and animation state"):
        return
    populated_park["residents"][0]["activity"] = "RELAXING"
    populated_park["residents"][1]["activity"] = "FREE_TIME"
    world._on_world_updated(populated_park)
    world._animate_residents(0.5)
    if not _check(int(marco.get_meta("seat_index",-1))>=0
            and marco.get_meta("seat_index") != elena.get_meta("seat_index")
            and is_equal_approx(marco.get_node("LegLeft").rotation.x,PI/2.0)
            and is_equal_approx(marco.get_node("LegLeft/Knee").rotation.x,-PI/2.0),
            "resting residents use distinct bench seats and bend knees"):
        return
    populated_park["residents"][0]["activity"] = "WALKING"
    world._on_world_updated(populated_park)
    world._animate_residents(0.5)
    if not _check(int(marco.get_meta("seat_index",-1))==-1
            and is_zero_approx(marco.position.y)
            and marco.get_node("LegLeft/Knee").rotation.x>=-0.59,
            "walking releases the seat and restores standing pose"):
        return
    var rainy := _world("PARK", "Park")
    rainy["weather"] = {"sky":"RAIN","raining":true,"snowing":false}
    world._on_world_updated(rainy)
    if not _check(world._weather_fx.rain.emitting and not world._weather_fx.snow.emitting
            and world._environment.fog_enabled and world._weather_fx.ambience.stream != null,
            "rain starts particles, visibility fog and synthetic rain ambience"):
        return
    world._set_weather_visuals(false)
    if not _check(not world._weather_fx.rain.emitting and not world._environment.fog_enabled
            and world._last_snapshot["weather"]["raining"],
            "visual toggle does not change authoritative weather"):
        return
    world._set_weather_visuals(true)
    world._set_weather_audio(false)
    if not _check(world._weather_fx.rain.emitting and world._weather_fx.ambience.stream == null,
            "weather audio can be muted independently"):
        return
    var snowy := _world("PARK", "Park")
    snowy["weather"] = {"sky":"SNOW","raining":false,"snowing":true}
    world._on_world_updated(snowy)
    if not _check(world._weather_fx.snow.emitting and not world._weather_fx.rain.emitting,
            "snow switches precipitation without overlapping rain"):
        return
    world._weather_fx.follow(Vector3(3,0,4))
    if not _check(world._weather_fx.position == Vector3(3,9,4),
            "one precipitation emitter follows the player across chunks"):
        return
    world._on_world_updated(_world("PARK", "Park"))
    if not _check(not world._weather_fx.rain.emitting and not world._weather_fx.snow.emitting
            and not world._environment.fog_enabled,
            "clear weather removes precipitation and fog"):
        return
    world._set_weather_audio(true)

    var social := _world("PARK", "Park")
    social["residents"] = [{"id":"marco","name":"Marco","activity":"FREE_TIME",
            "knownName":"Alex","conversations":2,"relationship":"FAMILIAR"}]
    world._on_world_updated(social)
    var social_person = world._residents_root.get_node("Resident_marco")
    world._avatar.position = social_person.position
    if not _check(world._nearest_resident() == "marco"
            and social_person.get_meta("known_name") == "Alex"
            and social_person.get_meta("relationship") == "FAMILIAR",
            "nearby resident carries server-owned conversation memory"):
        return
    # An adult born in the simulated town must be able to type to a nearby NPC.
    # The old rebirth key handler swallowed T, and every world refresh closed chat.
    if not _check(not world._hud_details.visible,"advanced HUD starts collapsed"):
        return
    world._toggle_hud_details(true)
    if not _check(world._hud_details.visible,"extra HUD settings can be expanded"):
        return
    world._toggle_hud_details(false)
    var reborn_social := social.duplicate(true)
    reborn_social["rebirth"] = {"phase":"LINKED_ADULT","age":30,"childName":"Anna"}
    world._on_world_updated(reborn_social)
    world._avatar.position = world._residents_root.get_node("Resident_marco").position
    var talk_key := InputEventKey.new()
    talk_key.keycode = KEY_T
    talk_key.pressed = true
    world._unhandled_key_input(talk_key)
    print("DIALOGUE TEST: connected=",world._connected,
            " rebirth=",world._rebirth_active()," can_walk=",world._child_can_walk(),
            " npc=",world._nearest_resident()," pending=",world._dialog_pending,
            " api_busy=",world._api.is_busy()," guest=",world._guest_active,
            " stairwell=",world._stairwell_active," phone=",world._phone.visible,
            " intercom=",world._intercom.visible," action_menu=",world._action_panel.visible,
            " chat=",world._dialog_panel.visible," target=",world._dialog_target)
    if not _check(world._dialog_panel.visible and not world._action_panel.visible
            and world._dialog_target == "marco",
            "T opens real text chat instead of the life choices menu"):
        return
    world._dialog_pending = true
    world._on_action_feedback("Marco: Hello, visitor.")
    if not _check(not world._dialog_pending and world._dialog_history.get_parsed_text().contains("Hello, visitor."),
            "NPC's accepted reply appears in text conversation history"):
        return
    world._on_world_updated(reborn_social)
    if not _check(world._dialog_panel.visible and not world._action_panel.visible,
            "refreshing a linked adult life keeps the dialogue open"):
        return
    var escape_key := InputEventKey.new()
    escape_key.keycode = KEY_ESCAPE
    escape_key.pressed = true
    world._unhandled_key_input(escape_key)
    if not _check(not world._dialog_panel.visible and not world._action_panel.visible,
            "Escape closes chat without reopening the large menu"):
        return
    world._on_world_updated(social)
    world._avatar.position = world._residents_root.get_node("Resident_marco").position
    world._avatar.position = Vector3(16,0,16)
    if not _check(world._nearest_resident().is_empty(),
            "distant residents cannot be selected for conversation"):
        return
    world._avatar.position = Vector3.ZERO

    var noon := _world("PARK", "Park")
    noon["minute"] = 720
    world._on_world_updated(noon)
    var noon_light: float = world._sun.light_energy
    var dawn := _world("PARK", "Park")
    dawn["minute"] = 360
    world._on_world_updated(dawn)
    if not _check(noon_light > world._sun.light_energy,
            "solar lighting follows committed world minutes"):
        return
    var night := _world("PARK", "Park")
    night["minute"] = 1260
    night["services"] = {"lightsWorking": true, "fountainWorking": true, "cleanWater": 5}
    world._on_world_updated(night)
    if not _check(world._park_lamps.size() == 6 and world._park_lamps[0].visible
            and world._fountain_water.visible
            and world._fountain_water.material_override is ShaderMaterial
            and world._fountain_water.get_node("FountainSpray").emitting
            and world._lantern_glass.emission_enabled and world._shop_glass.emission_enabled,
            "night park lights, animated water and spray reflect working municipal services"):
        return
    night["services"] = {"lightsWorking": false, "fountainWorking": false, "cleanWater": 0}
    world._on_world_updated(night)
    if not _check(not world._park_lamps[0].visible and not world._fountain_water.visible
            and not world._fountain_water.get_node("FountainSpray").emitting
            and not world._lantern_glass.emission_enabled,
            "broken services turn off real visual indicators"):
        return
    world._on_world_updated(_world("PARK", "Park"))
    if not _check(world._residents_root.get_child_count() == 0,
            "residents disappear when the authoritative snapshot reports nobody"):
        return

    world._on_world_updated(_world("PARK", "Park"))
    if not _check(world._current_place == "PARK",
            "first scene is the park"):
        return
    if not _check(world._scenery.name == "PublicScene_PARK",
            "park scenery was generated"):
        return
    world._avatar.position = world.KIOSK_POSITION + Vector3(0,0,2)
    world._toggle_actions()
    if not _check(_has_button(world._action_choices, "Drink from park fountain"),
            "public park offers the limited fountain separately from unsafe puddles"):
        return
    world._action_panel.visible = false


    world._on_locations_updated([
        {"id": "PARK", "label": "Park", "travelMinutes": 0},
        {"id": "MARKET", "label": "Market", "travelMinutes": 10},
        {"id": "SCHOOL", "label": "School", "travelMinutes": 10}
    ])
    if not _check(layout.travel_minutes.get("SCHOOL") == 10,
            "map reads travel minutes from the Java location response"):
        return
    world._avatar.position = world.GATE_POSITION
    world._toggle_travel()
    if not _check(world._travel_panel.visible,
            "gate opens only when the visitor approaches"):
        return
    if not _check(world._travel_choices.get_child_count() == 3,
            "destinations show server-supplied travel times"):
        return

    world._on_world_updated(_world("MARKET", "Market"))
    if not _check(world._chunks.size() == 6 and not world._chunks.has("PARK")
            and world._chunks["CENTRO"]["root"].position == Vector3(48,0,0),
            "long-distance travel unloads distant chunks and rebases the grid"):
        return
    if not _check(layout.current_place == "MARKET" and layout.travel_minutes.is_empty(),
            "map follows committed arrival and clears stale travel estimates"):
        return
    var wall_stop: Vector3 = world._move_on_ground(Vector3(-11.5,0,0),
            Vector3(0,0,9), world.WALKER_RADIUS)
    if not _check(wall_stop.z > 3.5 and wall_stop.z < 3.8,
            "market building has a solid exterior"):
        return
    if not _check(world._scenery.name == "PublicScene_MARKET" and
            world._avatar.position == Vector3.ZERO,
            "authoritative market arrival regenerates scenery and spawns visitor"):
        return
    var market_crowd := _world("MARKET","Market")
    market_crowd["residents"]=[
        {"id":"vendor-a","activity":"WORKING","workVisual":"CRATE"},
        {"id":"buyer-a","activity":"SHOPPING"},
        {"id":"buyer-b","activity":"SHOPPING"},
        {"id":"walker-a","activity":"WALKING"}]
    world._on_world_updated(market_crowd)
    var vendor := world._residents_root.get_node("Resident_vendor-a") as Node3D
    var buyer_a := world._residents_root.get_node("Resident_buyer-a") as Node3D
    var buyer_b := world._residents_root.get_node("Resident_buyer-b") as Node3D
    var passer := world._residents_root.get_node("Resident_walker-a") as Node3D
    if not _check(world._scenery.has_node("MarketCounters")
            and vendor.position.is_equal_approx(Vector3(-9,0,2.3))
            and buyer_a.get_meta("market_queue_target",Vector3.ZERO).is_equal_approx(Vector3(-9,0,0.12))
            and buyer_b.get_meta("market_queue_target",Vector3.ZERO).is_equal_approx(Vector3(-9,0,-0.98))
            and not passer.has_meta("market_queue_target"),
            "only actual shopping residents queue at a staffed market counter"):
        return
    buyer_a.position=buyer_a.get_meta("market_queue_target")
    world._animate_residents(0.2)
    if not _check(buyer_a.position.is_equal_approx(Vector3(-9,0,0.12)),
            "shopper stops at the counter rather than wandering"):
        return
    market_crowd["residents"][0]["activity"]="FREE_TIME"
    market_crowd["residents"][0]["workVisual"]=""
    world._on_world_updated(market_crowd)
    if not _check(not buyer_a.has_meta("market_queue_target") and not buyer_b.has_meta("market_queue_target"),
            "queues dissolve when the vendor stops working"):
        return
    world._on_world_updated(_world("MARKET","Market"))
    world._avatar.position = world.KIOSK_POSITION + Vector3(0,0,2)
    world._toggle_actions()
    if not _check(world._action_panel.visible and
            _has_button(world._action_choices, "Apply: market assistant"),
            "market has its actual employment action"):
        return

    world._on_world_updated(_world("SCHOOL", "School"))
    world._avatar.position = world.KIOSK_POSITION + Vector3(0,0,2)
    world._toggle_actions()
    if not _check(world._action_panel.visible and
            _has_button(world._action_choices, "Study engineering"),
            "school provides engineering classes rather than arbitrary jobs"):
        return

    world._on_world_updated(_world("WORKSHOP", "Workshop"))
    world._avatar.position = world.KIOSK_POSITION + Vector3(0,0,2)
    world._toggle_actions()
    if not _check(_has_button(world._action_choices, "Apply: technician"),
            "workshop provides technician applications"):
        return
    world._on_work_orders_updated([{
        "id": "water-pump", "place": "WORKSHOP",
        "title": "Park water-pump maintenance",
        "phase": "OPEN", "condition": 45
    }])
    if not _check(_has_button(world._action_choices, "Diagnose: water-pump"),
            "actual workshop fault offers diagnosis rather than free wages"):
        return
    world._on_work_orders_updated([{
        "id": "water-pump", "place": "WORKSHOP",
        "title": "Park water-pump maintenance",
        "phase": "INSPECTED", "condition": 45
    }])
    if not _check(_has_button(world._action_choices, "Repair / implement: water-pump"),
            "inspected workshop fault offers repair"):
        return

    world._on_world_updated(_world("OFFICE", "Office"))
    world._avatar.position = world.KIOSK_POSITION + Vector3(0,0,2)
    world._toggle_actions()
    world._on_work_orders_updated([{
        "id": "street-lights", "place": "OFFICE",
        "title": "Public lighting efficiency upgrade",
        "phase": "OPEN", "condition": 50
    }])
    if not _check(_has_button(world._action_choices, "Diagnose: street-lights"),
            "office offers engineering task rather than magically paying salary"):
        return

    world._on_world_updated(_world("QUARRY", "Quarry"))
    if not _check(world._scenery.name == "PublicScene_QUARRY",
            "quarry has its own physical district"):
        return
    world._avatar.position = world.KIOSK_POSITION + Vector3(0,0,2)
    world._toggle_actions()
    if not _check(_has_button(world._action_choices, "Apply: quarry miner"),
            "quarry offers a real paid mining career"):
        return
    var crew := _world("QUARRY","Quarry")
    crew["residents"]=[{"id":"miner-a","activity":"WORKING","workVisual":"PICK"},{"id":"miner-b","activity":"WORKING","workVisual":"PICK"}]
    world._on_world_updated(crew)
    var worker_a := world._residents_root.get_node("Resident_miner-a") as Node3D
    var worker_b := world._residents_root.get_node("Resident_miner-b") as Node3D
    if not _check(worker_a.position.distance_to(worker_b.position)>1.0 and absf(worker_a.position.z-4.7)<0.01 and absf(worker_b.position.z-4.7)<0.01 and world._can_stand(worker_a.position,world.WALKER_RADIUS),"miners work at different ore heaps, not random street points"):
        return
    if not _check(worker_a.has_node("ArmRight/WorkProp") and worker_a.get_node("ArmRight/WorkProp").get_meta("kind")=="PICK" and not worker_a.get_node("ResidentName").visible,"working miner visibly carries a pick without a name label"):
        return
    crew["residents"][0]["activity"]="FREE_TIME"
    crew["residents"][0]["workVisual"]=""
    world._on_world_updated(crew)
    if not _check(not worker_a.has_node("ArmRight/WorkProp"),"work tool is removed when shift ends"):
        return
    var employed_miner = _world("QUARRY", "Quarry")
    employed_miner["player"]["jobId"] = "miner"
    world._on_world_updated(employed_miner)
    if not _check(_has_button(world._action_choices, "Survey western mineral deposit"),
            "employed quarry miner can explore a second finite deposit"):
        return
    world._on_world_updated(_world("FACTORY", "Factory"))
    if not _check(world._scenery.name == "PublicScene_FACTORY",
            "manufacturing factory has its own physical district"):
        return
    world._avatar.position = world.KIOSK_POSITION + Vector3(0,0,2)
    world._toggle_actions()
    if not _check(_has_button(world._action_choices, "Apply: factory machinist"),
            "factory lets the visitor become a worker rather than only watch NPCs"):
        return
    world._on_industry_updated({
        "ironRemaining": 78, "copperRemaining": 39,
        "oreAtQuarry": 0, "oreAtFactory": 0,
        "factorySteel": 0, "factoryCopper": 0, "factoryParts": 0,
        "workshopSteel": 5, "workshopCopper": 4, "workshopParts": 8,
        "minersAtWork": 1, "factoryWorkersAtWork": 1
    })
    if not _check(world._jobs.text.contains("iron ore 78") and
            world._jobs.text.contains("spare parts 8"),
            "Godot displays the authoritative finite industrial inventories"):
        return

    world._on_world_updated(_world("WORKSHOP", "Workshop"))
    world._avatar.position = world.KIOSK_POSITION + Vector3(0,0,2)
    world._action_panel.visible = true
    world._on_business_updated({
        "registered": false, "yours": false, "ownerIncarnation": 0,
        "machine": false, "cash": 0, "employeeId": null,
        "employeeName": null, "spareParts": 0, "paidHoursToday": 0
    })
    if not _check(_has_button(world._action_choices, "Register / acquire workshop"),
            "the visitor can register a first business at the actual Workshop"):
        return
    world._on_business_updated({
        "registered": true, "yours": true, "ownerIncarnation": 1,
        "machine": true, "machineCondition": 85, "cash": 70,
        "employeeId": null, "employeeName": null, "spareParts": 2,
        "paidHoursToday": 2
    })
    world._on_business_candidates_updated([{
        "id": "resident-0002", "name": "A local mechanic",
        "currentOccupation": "Mechanic"
    }])
    if not _check(_has_button(world._action_choices, "Invest 25 EUR") and
            _has_button(world._action_choices, "Hire A local mechanic") and
            _has_button(world._action_choices, "Service machine") and
            _has_button(world._action_choices, "Withdraw 10 EUR"),
            "owner sees actual investment, local hiring and available profit controls"):
        return
    if not _check(world._jobs.text.contains("bank 70 EUR") and
            world._jobs.text.contains("85/100"),
            "business dashboard exposes real funds and machine wear"):
        return

    var apartment := _world("NORTH_RESIDENCES","North residences")
    apartment["rebirth"]={"phase":"LINKED_ADULT","age":30,"childName":"Anna","homeOptions":{"atHome":true,"district":"North residences","food":3,"water":3,"furnitureLevel":1,"actions":["go","meal","rest","family","wash"]}}
    apartment["residents"]=[{"id":"resident-0099","name":"Family member","activity":"AT_HOME"}]
    world._on_world_updated(apartment)
    if not _check(world._apartment_active and world._apartment.visible
            and world._apartment.has_node("BedFrame") and world._apartment.has_node("KitchenCounter")
            and world._residents_root.has_node("Resident_resident-0099")
            and not world._scenery.visible,
            "private home snapshot activates furnished apartment and actual resident"):
        return
    if not _check(world._can_stand(Vector3(-3,0,0),world.WALKER_RADIUS)
            and world._can_stand(Vector3(3,0,0),world.WALKER_RADIUS)
            and not world._can_stand(Vector3(0,0,-2),world.WALKER_RADIUS)
            and not world._can_stand(Vector3(4.7,0,-3.9),world.WALKER_RADIUS),
            "room doorways are open but partitions and furniture are solid"):
        return
    world._avatar.position=Vector3(3.3,0,-2.6)
    world._follow_player()
    if not _check(world._apartment_action()=="meal" and world._camera.position.is_equal_approx(world._avatar.position+Vector3(0,world.EYE_HEIGHT,0)),
            "kitchen contextual action and first-person camera"):
        return
    if not _check(world._apartment.has_node("Washbasin") and world._apartment.has_node("EntranceDoor")
            and world._apartment.get_node("WallArt").visible and not world._apartment.get_node("PremiumHeadboard").visible
            and world._can_stand(Vector3(3.8,0,2.2),world.WALKER_RADIUS)
            and not world._can_stand(Vector3(3.8,0,0.95),world.WALKER_RADIUS),
            "bathroom opening, furnishings and wall collision"):
        return
    if not _check(not world._apartment.get_node("KitchenMicrowave").visible and not world._apartment.get_node("ChildPlayMat").visible,"unpurchased furniture hidden"):
        return
    apartment["rebirth"]["homeOptions"]["interiorStyle"]="rosa"
    apartment["rebirth"]["homeOptions"]["furnishings"]=[{"id":"kitchen","installed":true},{"id":"children","installed":true}]
    world._on_world_updated(apartment)
    if not _check(world._apartment.get_node("KitchenMicrowave").visible and world._apartment.get_node("ChildPlayMat").visible and not world._apartment.get_node("BedroomArtwork").visible,"server purchases control installed furnishings"):
        return
    if not _check(world._can_stand(Vector3(3,0,0),world.WALKER_RADIUS) and world._can_stand(Vector3(-3,0,0),world.WALKER_RADIUS),"furnishings preserve room access"):
        return
    world._avatar.position=Vector3(4.4,0,2.2)
    if not _check(world._apartment_action()=="wash","bathroom contextual wash"):
        return
    if not _check(not world._can_stand(Vector3(0,0,4.5),world.WALKER_RADIUS), "closed entrance blocks walking"):
        return
    world._apartment.toggle_entrance(Vector3(0,0,3.5))
    if not _check(world._apartment.entrance_open and not world._can_stand(Vector3(0,0,4.5),world.WALKER_RADIUS),
            "door stays solid during opening"):
        return
    world._apartment.advance_door(0.6)
    if not _check(world._apartment.get_node("EntranceDoor").visible
            and is_equal_approx(world._apartment.get_node("EntranceDoor").rotation.y,-PI/2.0),
            "open door rotates on hinge instead of disappearing"):
        return
    var lobby_pos: Vector3 = world._move_on_ground(Vector3(0,0,3.5),Vector3(0,0,4.5),world.WALKER_RADIUS)
    if not _check(lobby_pos.z>7.9 and not world._can_stand(Vector3(2.1,0,6.5),world.WALKER_RADIUS), "open entrance allows continuous walk into lobby; lobby walls solid"):
        return
    world._avatar.position=lobby_pos
    if not _check(world._apartment_action()=="exit","building exit offers travel choices"):
        return
    var outside := apartment.duplicate(true)
    outside["rebirth"]["homeOptions"]["atHome"]=false
    outside["rebirth"]["homeOptions"]["outsideHome"]=true
    outside["residents"]=[]
    world._home_transition=true
    world._on_world_updated(outside)
    if not _check(not world._apartment_active and world._near_family_entrance() and world._can_walk_roads() and not world._action_panel.visible
            and world._home_entrance.position.is_equal_approx(Vector3(11,0,-2.8)),
            "confirmed exit opens public street and continuous walking without menu"):
        return
    world._road_pending="CLINIC"
    world._road_arrival=Vector3(-23,0,0)
    var next_street := _world("CLINIC","Clinic")
    next_street["rebirth"]=outside["rebirth"].duplicate(true)
    next_street["rebirth"]["homeOptions"]["outsideHome"]=false
    world._on_world_updated(next_street)
    if not _check(world._avatar.position.x < -22 and not world._action_panel.visible,"linked adult road arrival preserves walking direction"):
        return
    world._on_world_updated(_world("PARK","Park"))
    if not _check(not world._apartment_active and not world._apartment.visible and world._scenery.visible,
            "authoritative departure restores outdoor scenery"):
        return

    var shopping := _world("PORTICI","Via dei Portici")
    shopping["minute"]=600
    shopping["rebirth"]={"phase":"LINKED_ADULT","age":30,"childName":"Anna","grocery":{"open":true,"food":2,"water":3,"capacity":24,"items":[{"id":"bread","label":"Bread","price":3,"stock":2,"available":true},{"id":"water","label":"Bottled water","price":1,"stock":0,"available":false}]}}
    world._on_world_updated(shopping)
    if not _check(world._scenery.has_node("ViaDeiPortici/BottegaAlimentare/GroceryCounter") and not world._near_grocery_counter(),
            "grocery has an interior counter, not accessible from street"):
        return
    world._avatar.position=Vector3(7.3,0,3.8)
    world._grocery_menu=true
    world._render_actions()
    if not _check(world._near_grocery_counter() and _has_button(world._action_choices,"Bread · 3 EUR") and _has_button(world._action_choices,"Bottled water · 1 EUR"),
            "grocery counter displays authoritative stock and prices"):
        return
    world._close_grocery()
    if not _check(not world._grocery_menu and not world._action_panel.visible,"leave shop counter and continue walking"):
        return
    var bakery := _world("BAKERY","Bakery")
    bakery["rebirth"]={"phase":"LINKED_ADULT","age":30,"childName":"Anna","grocery":{"name":"Panetteria del Borgo","hours":"07:00–18:00","open":true,"food":4,"water":2,"capacity":24,"items":[{"id":"bread","label":"Bread","price":3,"stock":2,"available":true}]}}
    world._on_world_updated(bakery)
    if not _check(world._scenery.has_node("PanetteriaDelBorgo/BreadOven") and world._scenery.has_node("PanetteriaDelBorgo/BakeryCounter") and not world._near_grocery_counter(),"bakery interior and checkout"):
        return
    var bakery_walk: Vector3 = world._move_on_ground(Vector3(-2,0,7),Vector3(-4.3,0,0),world.WALKER_RADIUS)
    if not _check(bakery_walk.x < -6 and not world._can_stand(Vector3(-4,0,4.7),world.WALKER_RADIUS),"bakery doorway passable and facade solid"):
        return
    world._avatar.position=bakery_walk
    world._grocery_menu=true
    world._render_actions()
    if not _check(world._near_grocery_counter() and world._action_title.text=="Panetteria del Borgo" and _has_button(world._action_choices,"Bread · 3 EUR"),"bakery has distinct name and live stock"):
        return
    world._close_grocery()
    var cafe := _world("CAFE","Cafe")
    cafe["rebirth"]={"phase":"LINKED_ADULT","age":30,"childName":"Anna","cafe":{"name":"Bar Centrale","hours":"07:00–21:00","open":true,"orders":[{"id":"espresso","label":"Espresso","price":1,"available":true}],"canRest":true,"guests":[{"id":"resident-0002","name":"Marco"}]}}
    world._on_world_updated(cafe)
    var cafe_walk: Vector3 = world._move_on_ground(Vector3(-2,0,7),Vector3(-5,0,0),world.WALKER_RADIUS)
    if not _check(cafe_walk.x < -6.9 and world._scenery.has_node("BarCentrale/CafeTables") and not world._can_stand(Vector3(-4,0,4.45),world.WALKER_RADIUS),"cafe walkable doorway, tables and solid facade"):
        return
    world._avatar.position=Vector3(-7,0,5.9)
    world._cafe_menu=true
    world._render_actions()
    if not _check(world._cafe_station()=="counter" and _has_button(world._action_choices,"Espresso · 1 EUR"),"cafe order menu at counter"):
        return
    world._avatar.position=Vector3(-7,0,9)
    world._render_actions()
    if not _check(world._cafe_station()=="table" and _has_button(world._action_choices,"Rest at table") and _has_button(world._action_choices,"Talk with Marco"),"cafe table rest and actual guests"):
        return
    world._close_cafe()
    var medical := _world("CLINIC","Clinic")
    medical["rebirth"]={"phase":"LINKED_ADULT","age":30,"childName":"Anna","medical":{"atClinic":true,"health":75,"ill":true,"canConsult":true,"canFirstAid":true}}
    world._on_world_updated(medical)
    if not _check(world._scenery.has_node("AmbulatorioCivico/ServiceCounter") and world._scenery.has_node("FarmaciaDelBorgo/ServiceCounter"),"clinic and pharmacy have distinct interiors"):
        return
    var clinic_walk: Vector3 = world._move_on_ground(Vector3(-2,0,7),Vector3(-4.3,0,0),world.WALKER_RADIUS)
    var pharmacy_walk: Vector3 = world._move_on_ground(Vector3(2,0,-7),Vector3(4.3,0,0),world.WALKER_RADIUS)
    if not _check(clinic_walk.x < -6 and pharmacy_walk.x > 6,"both medical entrances are walkable"):
        return
    world._avatar.position=clinic_walk
    world._medical_menu=true
    world._render_actions()
    if not _check(world._medical_station()=="consult" and _has_button(world._action_choices,"Consultation · 15 EUR"),"clinic contextual menu"):
        return
    world._avatar.position=pharmacy_walk
    world._render_actions()
    if not _check(world._medical_station()=="first-aid" and _has_button(world._action_choices,"Basic pharmacy care · 5 EUR"),"pharmacy contextual menu"):
        return
    world._close_medical()
    var campus := _world("SCHOOL","School")
    campus["rebirth"]={"phase":"LINKED_ADULT","age":30,"childName":"Anna","college":{"atCampus":true,"open":true,"enrolled":null,"courses":[{"id":"engineering","label":"Engineering","required":60,"completed":0,"canEnroll":true}]}}
    world._on_world_updated(campus)
    var campus_walk: Vector3 = world._move_on_ground(Vector3(-2,0,7),Vector3(-4.3,0,0),world.WALKER_RADIUS)
    if not _check(campus_walk.x < -6 and world._scenery.has_node("IstitutoLeonardo/Classroom"),"college entrance and classroom"):
        return
    world._avatar.position=campus_walk
    world._college_menu=true
    world._render_actions()
    if not _check(world._near_college() and _has_button(world._action_choices,"Enroll: Engineering"),"college registration menu"):
        return
    world._close_college()
    campus["minute"]=600
    campus["residents"]=[
        {"id":"school-pupil-a","name":"Unknown resident","activity":"STUDYING","appearanceStage":"CHILD","outfitVisual":"BACKPACK"},
        {"id":"school-pupil-b","name":"Unknown resident","activity":"STUDYING","appearanceStage":"TEEN"},
        {"id":"school-teacher","name":"Unknown resident","activity":"WORKING","workVisual":"TEACHING"},
        {"id":"school-visitor","name":"Unknown resident","activity":"FREE_TIME"}]
    world._on_world_updated(campus)
    var pupil_a = world._residents_root.get_node("Resident_school-pupil-a")
    var pupil_b = world._residents_root.get_node("Resident_school-pupil-b")
    var teacher = world._residents_root.get_node("Resident_school-teacher")
    var visitor = world._residents_root.get_node("Resident_school-visitor")
    if not _check(pupil_a.get_meta("seat_kind")=="classroom" and pupil_b.get_meta("seat_kind")=="classroom"
            and pupil_a.position.distance_to(pupil_b.position)>0.8 and pupil_a.position.x < -4 and pupil_b.position.x < -4,
            "studying residents occupy different real indoor classroom chairs"):
        return
    if not _check(teacher.position.x < -4 and teacher.position.z>3 and teacher.position.z<11
            and teacher.get_meta("work_visual")=="TEACHING" and visitor.get_meta("seat_index",-1)==-1,
            "teacher works inside classroom while unrelated visitors do not occupy pupil chairs"):
        return
    var pupil_at: Vector3=pupil_a.position
    world._render_residents()
    if not _check(pupil_a.position.is_equal_approx(pupil_at),"classroom seats remain stable across snapshots"):
        return
    campus["residents"][0]["activity"]="FREE_TIME"
    world._on_world_updated(campus)
    if not _check(pupil_a.get_meta("seat_index",-1)==-1 and pupil_a.get_meta("seat_kind","").is_empty(),
            "pupil releases classroom seat when lesson ends"):
        return
    var workshop := _world("WORKSHOP","Workshop")
    workshop["rebirth"]={"phase":"LINKED_ADULT","age":30,"childName":"Anna","engineeringOrders":[{"id":"water-pump","title":"Park pump","district":"WORKSHOP","phase":"OPEN","condition":45,"materials":"2 spare-parts","wage":38,"canInspect":true,"canComplete":false}]}
    world._on_world_updated(workshop)
    world._avatar.position=world.KIOSK_POSITION
    world._engineering_menu=true
    world._render_actions()
    if not _check(world._near_engineering() and _has_button(world._action_choices,"Inspect · 30 min") and _has_button(world._action_choices,"Repair · 90 min"),"linked engineering work orders"):
        return
    world._close_engineering()
    var office := _world("OFFICE","Office")
    office["rebirth"]={"phase":"LINKED_ADULT","age":30,"childName":"Anna","softwareProjects":[{"id":"water-control","title":"Water control","phase":"OPEN","effect":"Water capacity +2","wage":45,"nextAction":"Develop","minutes":60,"canAdvance":true,"deployed":false}]}
    world._on_world_updated(office)
    if not _check(world._scenery.has_node("BorgoDigital/DevelopmentRoom") and world._scenery.has_node("BorgoDigital/ServerRoom") and world._scenery.has_node("BorgoDigital/MeetingRoom"),"office rooms exist"):
        return
    var office_entry: Vector3 = world._move_on_ground(Vector3(-2,0,8),Vector3(-4,0,0),world.WALKER_RADIUS)
    if not _check(office_entry.x < -5.9,"office doorway passable"):
        return
    var office_hall: Vector3 = world._move_on_ground(office_entry,Vector3(0,0,-4),world.WALKER_RADIUS)
    var dev_walk: Vector3 = world._move_on_ground(office_hall,Vector3(-4,0,0),world.WALKER_RADIUS)
    if not _check(dev_walk.distance_to(world.JAVA_DESK)<0.2 and world._can_stand(world.DEVOPS_DESK,world.WALKER_RADIUS) and world._can_stand(world.COMPANY_DESK,world.WALKER_RADIUS),"office stations reachable"):
        return
    var blocked_wall: Vector3 = world._move_on_ground(Vector3(-2,0,4),Vector3(-4,0,0),world.WALKER_RADIUS)
    if not _check(blocked_wall.x > -4,"office facade solid"):
        return
    world._avatar.position=Vector3(5,0,3)
    if not _check(not world._near_software(),"outdoor kiosk no longer opens software"):
        return
    world._avatar.position=dev_walk
    world._software_menu=true
    world._render_actions()
    if not _check(world._near_software() and _has_button(world._action_choices,"Develop · 60 min"),"software project menu"):
        return
    world._close_software()
    office["rebirth"]["javaWorkspace"]={"intro":"Welcome to the Java team.","onboarded":false,"canOnboard":true,"tickets":[{"id":"water-control","number":"BD-101","requirement":"Respect available stock and free capacity","signature":"int waterUnits(int requested, int stock, int freeCapacity)","examples":"(6,2,9) -> 2"}]}
    world._on_world_updated(office)
    world._avatar.position=world.JAVA_DESK
    world._software_menu=true
    world._render_actions()
    if not _check(_has_button(world._action_choices,"Complete onboarding"),"java team onboarding"):
        return
    office["rebirth"]["javaWorkspace"]["onboarded"]=true
    world._on_world_updated(office)
    world._software_menu=true
    world._render_actions()
    if not _check(_has_button(world._action_choices,"Run tests and submit") and world._action_choices.has_node("Java_water-control"),"Java ticket editor and test submission"):
        return
    world._close_software()
    office["rebirth"]["softwareProjects"][0]["command"]="branch"
    office["rebirth"]["softwareProjects"][0]["nextAction"]="Create branch"
    office["rebirth"]["softwareProjects"][0]["minutes"]=5
    world._on_world_updated(office)
    world._software_menu=true
    world._render_actions()
    if not _check(_has_button(world._action_choices,"Create branch") and not world._action_choices.has_node("Java_water-control"),"Git branch precedes code editor"):
        return
    world._close_software()
    office["rebirth"]["softwareProjects"]=[{"id":"retail-sql","title":"Low-stock report","phase":"OPEN","command":"code","canAdvance":true}]
    office["rebirth"]["javaWorkspace"]["tickets"]=[{"id":"retail-sql","number":"BD-104","language":"SQL","signature":"products(id,stock,price)","requirement":"Find low stock","examples":"bread=0","starter":"SELECT id FROM products"}]
    world._on_world_updated(office)
    world._software_menu=true
    world._render_actions()
    var sql_editor = world._action_choices.get_node_or_null("Java_retail-sql")
    if not _check(sql_editor is LineEdit and sql_editor.text=="SELECT id FROM products","SQL ticket uses language-specific starter"):
        return
    world._close_software()
    office["rebirth"]["javaWorkspace"]["career"]={"rank":"Junior","xp":0,"technical":55,"releases":0,"audits":0,"nextRank":"Middle","requiredXp":4,"requiredTechnical":55,"requiredAudits":2,"canPromote":false,"audit":{"title":"Inventory audit","wage":18,"prompt":"Bread 3 · Water 2","available":true}}
    world._on_world_updated(office)
    world._avatar.position=world.JAVA_DESK
    world._software_menu=true
    world._render_actions()
    if not _check(_has_button(world._action_choices,"Promotion: Middle") and _has_button(world._action_choices,"Submit daily audit") and world._action_choices.has_node("DeveloperAuditAnswer"),"developer career and daily audit menu"):
        return
    world._close_software()
    office["rebirth"]["javaWorkspace"]["devops"]={"status":"OUTAGE","resolved":0,"guide":"Inspect the service","output":"HTTP 503","trained":true,"active":true,"canRun":true}
    world._on_world_updated(office)
    world._avatar.position=world.DEVOPS_DESK
    world._software_menu=true
    world._render_actions()
    if not _check(world._office_action_allowed("rebirth-devops-train") and not world._office_action_allowed("rebirth-it:register") and not world._office_action_allowed("rebirth-java-onboard") and _has_button(world._action_choices,"Run command") and world._action_choices.has_node("DevOpsCommand"),"DevOps training terminal"):
        return
    world._close_software()
    office["rebirth"]["javaWorkspace"]["companyStatus"]={"registered":true,"cash":40,"rentUntil":8,"contracts":0,"taxPaid":0,"canInvest":true,"canWithdraw":true,"canRent":false,"offers":[{"id":"retail","title":"Retail support","gross":40,"tax":8,"prompt":"Count stock","available":true}]}
    world._on_world_updated(office)
    world._avatar.position=world.COMPANY_DESK
    world._software_menu=true
    world._render_actions()
    if not _check(world._office_action_allowed("rebirth-it:register") and not world._office_action_allowed("rebirth-devops-train") and not _has_button(world._action_choices,"Run command") and _has_button(world._action_choices,"Invest 20 EUR") and _has_button(world._action_choices,"Deliver contract") and world._action_choices.has_node("IT_retail"),"IT company account and contract UI"):
        return
    world._close_software()
    office["rebirth"]["javaWorkspace"]["companyStatus"]["staff"]={"employee":{"id":"resident-0002","name":"Marco","technical":35,"activity":"WORKING","sick":false},"canDismiss":true,"delegated":0,"delegatable":["retail"]}
    world._on_world_updated(office)
    world._avatar.position=world.COMPANY_DESK
    world._software_menu=true
    world._render_actions()
    if not _check(_has_button(world._action_choices,"Delegate · 12 EUR wage") and _has_button(world._action_choices,"End support contract"),"IT staff delegation controls"):
        return
    world._close_software()
    office["residents"]=[{"id":"resident-0002","name":"Marco","activity":"WORKING"}]
    world._on_world_updated(office)
    var assistant := world._residents_root.get_node_or_null("Resident_resident-0002") as Node3D
    if not _check(assistant!=null and assistant.position.distance_to(world.OFFICE_STAFF_ANCHOR)<0.1,"actual working assistant appears inside office"):
        return
    world._avatar.position=world.JAVA_DESK
    world._software_menu=true
    world._render_actions()
    if not _check(_has_button(world._action_choices,"Code editor") and _has_button(world._action_choices,"Issue tracker") and _has_button(world._action_choices,"Inbox") and _has_button(world._action_choices,"Intranet"),"BorgoOS app dock"):
        return
    var draft = world._action_choices.get_node_or_null("Java_retail-sql")
    if not _check(draft is LineEdit,"code editor available"):
        return
    draft.text="SELECT id FROM products WHERE stock < 3 ORDER BY id"
    draft.text_changed.emit(draft.text)
    world._switch_office_app("tasks")
    if not _check(world._office_app=="tasks" and not world._action_choices.has_node("Java_retail-sql"),"issue tracker separate from editor"):
        return
    world._switch_office_app("mail")
    if not _check(world._office_app=="mail" and _has_button(world._action_choices,"Leave workstation"),"mail application"):
        return
    world._switch_office_app("browser")
    if not _check(world._office_app=="browser" and _has_button(world._action_choices,"Leave workstation"),"intranet application"):
        return
    world._switch_office_app("work")
    var restored = world._action_choices.get_node_or_null("Java_retail-sql")
    if not _check(restored is LineEdit and restored.text=="SELECT id FROM products WHERE stock < 3 ORDER BY id","draft survives app switching"):
        return
    world._close_software()
    # A real present worker makes jokes; the player cannot hear them outside.
    world._avatar.position=Vector3.ZERO
    world._tick_office_joker(60.0)
    var joker = world._residents_root.get_node_or_null("Resident_resident-0002")
    if not _check(joker!=null and world._office_joker_id=="resident-0002" and not joker.has_node("OfficeJoke"),"no ambient jokes outside the office"):
        return
    world._avatar.position=world.JAVA_DESK
    world._tick_office_joker(9.0)
    var speech = joker.get_node_or_null("OfficeJoke")
    if not _check(speech is Label3D and speech.visible and speech.text==world.OFFICE_JOKE_LINES[0],"working colleague calls out in the office"):
        return
    world._tick_office_joker(5.0)
    if not _check(not speech.visible,"joke expires"):
        return
    world._tick_office_joker(1.0)
    if not _check(not speech.visible,"cooldown prevents repeated jokes"):
        return
    world._tick_office_joker(45.0)
    if not _check(speech.visible and speech.text==world.OFFICE_JOKE_LINES[1],"jokes rotate"):
        return
    world._avatar.position=Vector3.ZERO
    world._tick_office_joker(1.0)
    if not _check(not speech.visible,"walking away hides speech"):
        return
    office["residents"][0]["mood"]="STRESSED"
    world._on_world_updated(office)
    world._avatar.position=world.JAVA_DESK
    world._tick_office_joker(60.0)
    if not _check(str(joker.get_meta("mood",""))=="STRESSED" and not speech.visible,"stressed colleague stops joking"):
        return
    office["residents"]=[]
    world._on_world_updated(office)
    world._avatar.position=world.JAVA_DESK
    world._tick_office_joker(60.0)
    if not _check(world._residents_root.get_child_count()==0 and world._office_joker_id=="resident-0002","absent worker is not fabricated or replaced"):
        return
    var language = preload("res://scripts/GameLanguage.gd")
    var personality = preload("res://scripts/ResidentPersonality.gd")
    var styles := {}
    for i in range(100):
        var resident_id := "resident-"+str(i)
        var style: String = personality.style(resident_id)
        if not _check(style==personality.style(resident_id),"resident style is deterministic"):
            return
        styles[style]=true
    if not _check(styles.size()==7,"all seven personality styles appear"):
        return
    language.select_locale("it",false)
    world._refresh_localized_hud()
    if not _check(world._needs.text.begins_with("Fame") and str(TranslationServer.translate("Jobs"))=="Lavoro" and str(TranslationServer.translate(world.OFFICE_JOKE_LINES[0]))=="CHI HA FATTO IL MERGE DI VENERDÌ?!","Italian HUD, buttons and office jokes"):
        return
    language.select_locale("en",false)
    world._refresh_localized_hud()
    if not _check(world._needs.text.begins_with("Hunger") and str(TranslationServer.translate("Jobs"))=="Jobs","English fallback restores original text"):
        return
    print("SceneNavigationSmoke: ALL CHECKS PASSED")
    quit(0)

func _world(place_id: String, label: String) -> Dictionary:
    return {
        "day": 1,
        "minute": 450,
        "season": "SPRING",
        "weather": {
            "sky": "CLEAR", "temperatureC": 15,
            "raining": false, "snowing": false
        },
        "player": {
            "incarnation": 1, "placeId": place_id, "place": label,
            "money": 50, "jobId": null, "health": 100,
            "hunger": 0, "thirst": 0, "fatigue": 0,
            "engineeringSkill": 0, "investigationSkill": 0
        }
    }

func _has_button(parent: Node, prefix: String) -> bool:
    for node in parent.get_children():
        if node is Button and node.text.begins_with(prefix):
            return true
        if _has_button(node,prefix):
            return true
    return false

func _check(ok: bool, label: String) -> bool:
    if not ok:
        push_error("Scene navigation failed: " + label)
        quit(1)
        return false
    return true
