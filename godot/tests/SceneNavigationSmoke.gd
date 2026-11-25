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

    # The initial presentation is eye-level first person; V can show the
    # placeholder avatar without moving the authoritative player in Java.
    if not _check(world._first_person and not world._avatar.visible
            and world._camera.position.is_equal_approx(
                    world._avatar.position + Vector3(0.0, world.EYE_HEIGHT, 0.0)),
            "visitor starts in first-person at eye height"):
        return
    world._toggle_view_mode()
    if not _check(not world._first_person and world._avatar.visible
            and world._camera.position.distance_to(world._avatar.position) > 5.0,
            "third-person toggle shows the local avatar"):
        return
    world._toggle_view_mode()
    world._view_yaw = PI / 2.0
    world._follow_player()
    if not _check(world._first_person and not world._avatar.visible
            and is_equal_approx(world._camera.rotation.y, world._view_yaw),
            "returning to first-person retains mouse-look orientation"):
        return
    world._view_yaw = 0.0
    world._follow_player()

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
    var start_position: Vector3 = marco.position
    world._animate_residents(0.5)
    if not _check(marco.position.distance_to(start_position) > 0.01,
            "walking residents move within the local visual scene"):
        return
    world._on_world_updated(populated_park)
    if not _check(world._residents_root.get_node("Resident_marco") == marco,
            "refresh retains the same resident model and animation state"):
        return
    var night := _world("PARK", "Park")
    night["minute"] = 1260
    night["services"] = {"lightsWorking": true, "fountainWorking": true, "cleanWater": 5}
    world._on_world_updated(night)
    if not _check(world._park_lamps.size() == 4 and world._park_lamps[0].visible
            and world._fountain_water.visible,
            "night park lights and water reflect working municipal services"):
        return
    night["services"] = {"lightsWorking": false, "fountainWorking": false, "cleanWater": 0}
    world._on_world_updated(night)
    if not _check(not world._park_lamps[0].visible and not world._fountain_water.visible,
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
    world._avatar.position = world.GATE_POSITION
    world._toggle_travel()
    if not _check(world._travel_panel.visible,
            "gate opens only when the visitor approaches"):
        return
    if not _check(world._travel_choices.get_child_count() == 3,
            "destinations show server-supplied travel times"):
        return

    world._on_world_updated(_world("MARKET", "Market"))
    var wall_stop: Vector3 = world._move_on_ground(Vector3(-7,0,0),
            Vector3(0,0,9), world.WALKER_RADIUS)
    if not _check(wall_stop.z > 3.5 and wall_stop.z < 3.8,
            "market building has a solid exterior"):
        return
    if not _check(world._scenery.name == "PublicScene_MARKET" and
            world._avatar.position == Vector3.ZERO,
            "authoritative market arrival regenerates scenery and spawns visitor"):
        return
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
    return false

func _check(ok: bool, label: String) -> bool:
    if not ok:
        push_error("Scene navigation failed: " + label)
        quit(1)
        return false
    return true
