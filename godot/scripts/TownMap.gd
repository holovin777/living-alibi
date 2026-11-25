extends Control
## Shared schematic coordinates for public districts. No private addresses,
## invented travel durations or authority to change the Java world.
const DISTRICTS := {
    "NORTH_RESIDENCES": Vector2i(0,0), "CLINIC": Vector2i(1,0),
    "SCHOOL": Vector2i(2,0), "OFFICE": Vector2i(3,0),
    "FACTORY": Vector2i(4,0),
    "MARKET": Vector2i(0,1), "CENTRO": Vector2i(1,1),
    "PARK": Vector2i(2,1), "LIBRARY": Vector2i(3,1),
    "WORKSHOP": Vector2i(4,1),
    "SOUTH_RESIDENCES": Vector2i(0,2), "PORTICI": Vector2i(1,2),
    "RIVER": Vector2i(2,2), "WOODS": Vector2i(3,2),
    "QUARRY": Vector2i(4,2), "BAKERY": Vector2i(1,3), "CAFE": Vector2i(0,3), "FARM": Vector2i(2,3)
}
const LABELS := {
    "NORTH_RESIDENCES": "North homes", "SOUTH_RESIDENCES": "South homes", "CENTRO": "Centro storico", "PORTICI": "Via dei Portici", "FARM": "Cascina"
}
const MOTOR_DISTRICTS := ["OFFICE","LIBRARY","WORKSHOP","FACTORY","QUARRY"]
const TILE_SPACING := 48.0
var current_place := "PARK"
var travel_minutes: Dictionary = {}

static func world_center(id: String) -> Vector3:
    var cell: Vector2i = DISTRICTS.get(id, DISTRICTS["PARK"])
    var origin: Vector2i = DISTRICTS["PARK"]
    return Vector3((cell.x-origin.x)*TILE_SPACING, 0.0,
            (cell.y-origin.y)*TILE_SPACING)

static func adjacent(first: String, second: String) -> bool:
    if not DISTRICTS.has(first) or not DISTRICTS.has(second):
        return false
    var a: Vector2i = DISTRICTS[first]
    var b: Vector2i = DISTRICTS[second]
    return absi(a.x-b.x)+absi(a.y-b.y) == 1

static func motor_road(first: String, second: String) -> bool:
    return adjacent(first,second) and first in MOTOR_DISTRICTS and second in MOTOR_DISTRICTS

func _ready() -> void:
    custom_minimum_size = Vector2(570, 432)
    mouse_filter = Control.MOUSE_FILTER_IGNORE

func update_world(place: String, locations: Array) -> void:
    current_place = place
    travel_minutes.clear()
    for location in locations:
        if typeof(location) == TYPE_DICTIONARY:
            var id := str(location.get("id", ""))
            if DISTRICTS.has(id) and location.has("travelMinutes"):
                travel_minutes[id] = int(location["travelMinutes"])
    queue_redraw()

func _point(id: String) -> Vector2:
    var cell: Vector2i = DISTRICTS[id]
    return Vector2(60+cell.x*112, 53+cell.y*102)

func _draw() -> void:
    draw_rect(Rect2(Vector2.ZERO, size), Color("#263a34"))
    var font := ThemeDB.fallback_font
    for a in DISTRICTS:
        for b in DISTRICTS:
            if str(a) < str(b) and adjacent(str(a),str(b)):
                var motor := motor_road(str(a),str(b))
                draw_line(_point(str(a)),_point(str(b)),Color("#68747a") if motor else Color("#86948a"),14.0 if motor else 10.0,true)
                if motor:
                    draw_line(_point(str(a)),_point(str(b)),Color("#d7d5c2"),1.0,true)
    for id in DISTRICTS:
        var center := _point(str(id))
        var rect := Rect2(center-Vector2(50,29),Vector2(100,58))
        var color := Color("#40564a")
        if id in ["PARK","WOODS","FARM"]:
            color = Color("#47784e")
        elif id in ["CENTRO","PORTICI"]:
            color = Color("#947453")
        elif id == "RIVER":
            color = Color("#326c85")
        elif id in ["FACTORY","QUARRY","WORKSHOP"]:
            color = Color("#675a50")
        draw_rect(rect,color)
        draw_rect(rect,Color("#edcb75") if id == current_place else Color("#a9b6a9"),false,2.0)
        var caption := str(LABELS.get(id,str(id).capitalize()))
        draw_string(font,center+Vector2(-49,-3),caption,HORIZONTAL_ALIGNMENT_CENTER,98,12,Color.WHITE)
        var detail := "YOU" if id == current_place else (
                "%s min" % travel_minutes[id] if travel_minutes.has(id) else "—")
        draw_string(font,center+Vector2(-49,16),detail,HORIZONTAL_ALIGNMENT_CENTER,98,12,Color("#edcb75"))
    draw_string(font,Vector2(10,417),"Walk along roads · times supplied by Java · M / Esc to close",
            HORIZONTAL_ALIGNMENT_LEFT,-1,13,Color.WHITE)
