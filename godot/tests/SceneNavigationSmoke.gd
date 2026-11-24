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

    world._on_world_updated(_world("PARK", "Park"))
    if not _check(world._current_place == "PARK",
            "first scene is the park"):
        return
    if not _check(world._scenery.name == "PublicScene_PARK",
            "park scenery was generated"):
        return

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
    if not _check(world._scenery.name == "PublicScene_MARKET" and
            world._avatar.position == Vector3.ZERO,
            "authoritative market arrival regenerates scenery and spawns visitor"):
        return
    world._avatar.position = world.KIOSK_POSITION
    world._toggle_actions()
    if not _check(world._action_panel.visible and
            _has_button(world._action_choices, "Apply: market assistant"),
            "market has its actual employment action"):
        return

    world._on_world_updated(_world("SCHOOL", "School"))
    world._avatar.position = world.KIOSK_POSITION
    world._toggle_actions()
    if not _check(world._action_panel.visible and
            _has_button(world._action_choices, "Study engineering"),
            "school provides engineering classes rather than arbitrary jobs"):
        return

    world._on_world_updated(_world("WORKSHOP", "Workshop"))
    world._avatar.position = world.KIOSK_POSITION
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
    world._avatar.position = world.KIOSK_POSITION
    world._toggle_actions()
    world._on_work_orders_updated([{
        "id": "street-lights", "place": "OFFICE",
        "title": "Public lighting efficiency upgrade",
        "phase": "OPEN", "condition": 50
    }])
    if not _check(_has_button(world._action_choices, "Diagnose: street-lights"),
            "office offers engineering task rather than magically paying salary"):
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
