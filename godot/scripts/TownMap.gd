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
    "QUARRY": Vector2i(4,2), "BAKERY": Vector2i(1,3), "CAFE": Vector2i(0,3), "FARM": Vector2i(2,3), "FUEL_STATION": Vector2i(4,3), "LOGISTICS_DEPOT": Vector2i(3,3),
    "BORGO_SUD": Vector2i(0,4), "PIAZZA_FONTANA": Vector2i(1,4), "GIARDINI": Vector2i(2,4), "STAZIONE": Vector2i(3,4), "VIA_TIGLI": Vector2i(4,4),
    # Generated outer blocks, mirrored by Java stable identifiers.
    "OUTER_05_00": Vector2i(5,0),
    "OUTER_06_00": Vector2i(6,0),
    "OUTER_07_00": Vector2i(7,0),
    "OUTER_08_00": Vector2i(8,0),
    "OUTER_09_00": Vector2i(9,0),
    "OUTER_10_00": Vector2i(10,0),
    "OUTER_05_01": Vector2i(5,1),
    "OUTER_06_01": Vector2i(6,1),
    "OUTER_07_01": Vector2i(7,1),
    "OUTER_08_01": Vector2i(8,1),
    "OUTER_09_01": Vector2i(9,1),
    "OUTER_10_01": Vector2i(10,1),
    "OUTER_05_02": Vector2i(5,2),
    "OUTER_06_02": Vector2i(6,2),
    "OUTER_07_02": Vector2i(7,2),
    "OUTER_08_02": Vector2i(8,2),
    "OUTER_09_02": Vector2i(9,2),
    "OUTER_10_02": Vector2i(10,2),
    "OUTER_05_03": Vector2i(5,3),
    "OUTER_06_03": Vector2i(6,3),
    "OUTER_07_03": Vector2i(7,3),
    "OUTER_08_03": Vector2i(8,3),
    "OUTER_09_03": Vector2i(9,3),
    "OUTER_10_03": Vector2i(10,3),
    "OUTER_05_04": Vector2i(5,4),
    "OUTER_06_04": Vector2i(6,4),
    "OUTER_07_04": Vector2i(7,4),
    "OUTER_08_04": Vector2i(8,4),
    "OUTER_09_04": Vector2i(9,4),
    "OUTER_10_04": Vector2i(10,4),
    "OUTER_00_05": Vector2i(0,5),
    "OUTER_01_05": Vector2i(1,5),
    "OUTER_02_05": Vector2i(2,5),
    "OUTER_03_05": Vector2i(3,5),
    "OUTER_04_05": Vector2i(4,5),
    "OUTER_05_05": Vector2i(5,5),
    "OUTER_06_05": Vector2i(6,5),
    "OUTER_07_05": Vector2i(7,5),
    "OUTER_08_05": Vector2i(8,5),
    "OUTER_09_05": Vector2i(9,5),
    "OUTER_10_05": Vector2i(10,5),
    "OUTER_00_06": Vector2i(0,6),
    "OUTER_01_06": Vector2i(1,6),
    "OUTER_02_06": Vector2i(2,6),
    "OUTER_03_06": Vector2i(3,6),
    "OUTER_04_06": Vector2i(4,6),
    "OUTER_05_06": Vector2i(5,6),
    "OUTER_06_06": Vector2i(6,6),
    "OUTER_07_06": Vector2i(7,6),
    "OUTER_08_06": Vector2i(8,6),
    "OUTER_09_06": Vector2i(9,6),
    "OUTER_10_06": Vector2i(10,6),
    "OUTER_00_07": Vector2i(0,7),
    "OUTER_01_07": Vector2i(1,7),
    "OUTER_02_07": Vector2i(2,7),
    "OUTER_03_07": Vector2i(3,7),
    "OUTER_04_07": Vector2i(4,7),
    "OUTER_05_07": Vector2i(5,7),
    "OUTER_06_07": Vector2i(6,7),
    "OUTER_07_07": Vector2i(7,7),
    "OUTER_08_07": Vector2i(8,7),
    "OUTER_09_07": Vector2i(9,7),
    "OUTER_10_07": Vector2i(10,7),
    "OUTER_00_08": Vector2i(0,8),
    "OUTER_01_08": Vector2i(1,8),
    "OUTER_02_08": Vector2i(2,8),
    "OUTER_03_08": Vector2i(3,8),
    "OUTER_04_08": Vector2i(4,8),
    "OUTER_05_08": Vector2i(5,8),
    "OUTER_06_08": Vector2i(6,8),
    "OUTER_07_08": Vector2i(7,8),
    "OUTER_08_08": Vector2i(8,8),
    "OUTER_09_08": Vector2i(9,8),
    "OUTER_10_08": Vector2i(10,8),
    "OUTER_00_09": Vector2i(0,9),
    "OUTER_01_09": Vector2i(1,9),
    "OUTER_02_09": Vector2i(2,9),
    "OUTER_03_09": Vector2i(3,9),
    "OUTER_04_09": Vector2i(4,9),
    "OUTER_05_09": Vector2i(5,9),
    "OUTER_06_09": Vector2i(6,9),
    "OUTER_07_09": Vector2i(7,9),
    "OUTER_08_09": Vector2i(8,9),
    "OUTER_09_09": Vector2i(9,9),
    "OUTER_10_09": Vector2i(10,9),
    "OUTER_00_10": Vector2i(0,10),
    "OUTER_01_10": Vector2i(1,10),
    "OUTER_02_10": Vector2i(2,10),
    "OUTER_03_10": Vector2i(3,10),
    "OUTER_04_10": Vector2i(4,10),
    "OUTER_05_10": Vector2i(5,10),
    "OUTER_06_10": Vector2i(6,10),
    "OUTER_07_10": Vector2i(7,10),
    "OUTER_08_10": Vector2i(8,10),
    "OUTER_09_10": Vector2i(9,10),
    "OUTER_10_10": Vector2i(10,10)
}
const LABELS := {
    "NORTH_RESIDENCES": "North homes", "SOUTH_RESIDENCES": "South homes", "CENTRO": "Centro storico", "PORTICI": "Via dei Portici", "FARM": "Cascina", "FUEL_STATION": "Fuel station", "LOGISTICS_DEPOT": "Logistics depot", "BORGO_SUD": "Borgo Sud", "PIAZZA_FONTANA": "Piazza Fontana", "GIARDINI": "Giardini", "STAZIONE": "Stazione", "VIA_TIGLI": "Via dei Tigli",
    "OUTER_05_00": "Giardino 01",
    "OUTER_06_00": "Borgata 02",
    "OUTER_07_00": "Borgata 03",
    "OUTER_08_00": "Piazzetta 04",
    "OUTER_09_00": "Campagna 05",
    "OUTER_10_00": "Campagna 06",
    "OUTER_05_01": "Borgata 07",
    "OUTER_06_01": "Giardino 08",
    "OUTER_07_01": "Borgata 09",
    "OUTER_08_01": "Borgata 10",
    "OUTER_09_01": "Campagna 11",
    "OUTER_10_01": "Campagna 12",
    "OUTER_05_02": "Piazzetta 13",
    "OUTER_06_02": "Borgata 14",
    "OUTER_07_02": "Giardino 15",
    "OUTER_08_02": "Borgata 16",
    "OUTER_09_02": "Campagna 17",
    "OUTER_10_02": "Campagna 18",
    "OUTER_05_03": "Borgata 19",
    "OUTER_06_03": "Piazzetta 20",
    "OUTER_07_03": "Borgata 21",
    "OUTER_08_03": "Giardino 22",
    "OUTER_09_03": "Campagna 23",
    "OUTER_10_03": "Campagna 24",
    "OUTER_05_04": "Borgata 25",
    "OUTER_06_04": "Borgata 26",
    "OUTER_07_04": "Piazzetta 27",
    "OUTER_08_04": "Borgata 28",
    "OUTER_09_04": "Campagna 29",
    "OUTER_10_04": "Campagna 30",
    "OUTER_00_05": "Giardino 31",
    "OUTER_01_05": "Borgata 32",
    "OUTER_02_05": "Borgata 33",
    "OUTER_03_05": "Piazzetta 34",
    "OUTER_04_05": "Borgata 35",
    "OUTER_05_05": "Giardino 36",
    "OUTER_06_05": "Borgata 37",
    "OUTER_07_05": "Borgata 38",
    "OUTER_08_05": "Piazzetta 39",
    "OUTER_09_05": "Campagna 40",
    "OUTER_10_05": "Campagna 41",
    "OUTER_00_06": "Borgata 42",
    "OUTER_01_06": "Giardino 43",
    "OUTER_02_06": "Borgata 44",
    "OUTER_03_06": "Borgata 45",
    "OUTER_04_06": "Piazzetta 46",
    "OUTER_05_06": "Borgata 47",
    "OUTER_06_06": "Giardino 48",
    "OUTER_07_06": "Borgata 49",
    "OUTER_08_06": "Borgata 50",
    "OUTER_09_06": "Campagna 51",
    "OUTER_10_06": "Campagna 52",
    "OUTER_00_07": "Piazzetta 53",
    "OUTER_01_07": "Borgata 54",
    "OUTER_02_07": "Giardino 55",
    "OUTER_03_07": "Borgata 56",
    "OUTER_04_07": "Borgata 57",
    "OUTER_05_07": "Piazzetta 58",
    "OUTER_06_07": "Borgata 59",
    "OUTER_07_07": "Giardino 60",
    "OUTER_08_07": "Borgata 61",
    "OUTER_09_07": "Campagna 62",
    "OUTER_10_07": "Campagna 63",
    "OUTER_00_08": "Borgata 64",
    "OUTER_01_08": "Piazzetta 65",
    "OUTER_02_08": "Borgata 66",
    "OUTER_03_08": "Giardino 67",
    "OUTER_04_08": "Borgata 68",
    "OUTER_05_08": "Borgata 69",
    "OUTER_06_08": "Piazzetta 70",
    "OUTER_07_08": "Borgata 71",
    "OUTER_08_08": "Giardino 72",
    "OUTER_09_08": "Campagna 73",
    "OUTER_10_08": "Campagna 74",
    "OUTER_00_09": "Campagna 75",
    "OUTER_01_09": "Campagna 76",
    "OUTER_02_09": "Campagna 77",
    "OUTER_03_09": "Campagna 78",
    "OUTER_04_09": "Campagna 79",
    "OUTER_05_09": "Campagna 80",
    "OUTER_06_09": "Campagna 81",
    "OUTER_07_09": "Campagna 82",
    "OUTER_08_09": "Campagna 83",
    "OUTER_09_09": "Campagna 84",
    "OUTER_10_09": "Campagna 85",
    "OUTER_00_10": "Campagna 86",
    "OUTER_01_10": "Campagna 87",
    "OUTER_02_10": "Campagna 88",
    "OUTER_03_10": "Campagna 89",
    "OUTER_04_10": "Campagna 90",
    "OUTER_05_10": "Campagna 91",
    "OUTER_06_10": "Campagna 92",
    "OUTER_07_10": "Campagna 93",
    "OUTER_08_10": "Campagna 94",
    "OUTER_09_10": "Campagna 95",
    "OUTER_10_10": "Campagna 96"
}
const MOTOR_DISTRICTS := ["OFFICE","LIBRARY","WORKSHOP","FACTORY","QUARRY","FUEL_STATION","LOGISTICS_DEPOT"]
const TILE_SPACING := 48.0
const GRID_WIDTH := 11
const GRID_HEIGHT := 11
var current_place := "PARK"
var travel_minutes: Dictionary = {}
var overview := false

static func outer(id: String) -> bool:
    return id.begins_with("OUTER_")

static func label_for(id: String) -> String:
    return str(LABELS.get(id,id.capitalize()))

func set_overview(enabled: bool) -> void:
    overview=enabled
    queue_redraw()

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
    if not adjacent(first,second):
        return false
    if first in MOTOR_DISTRICTS and second in MOTOR_DISTRICTS:
        return true
    var farm_route := ["FARM","BAKERY","CAFE","SOUTH_RESIDENCES","MARKET"]
    var a := farm_route.find(first)
    var b := farm_route.find(second)
    return a>=0 and b>=0 and absi(a-b)==1

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

# The default local map keeps full-size names. Overview is intentionally compact.
func _window_origin() -> Vector2i:
    var cell: Vector2i = DISTRICTS.get(current_place,DISTRICTS["PARK"])
    return Vector2i(clampi(cell.x-2,0,GRID_WIDTH-5),clampi(cell.y-2,0,GRID_HEIGHT-5))

func _point(id: String) -> Vector2:
    var cell: Vector2i = DISTRICTS[id]
    if overview:return Vector2(30+cell.x*50,19+cell.y*35)
    cell-=_window_origin()
    return Vector2(60+cell.x*112,35+cell.y*80)

func _in_window(id: String) -> bool:
    if overview:return true
    var cell: Vector2i = DISTRICTS[id]-_window_origin()
    return cell.x>=0 and cell.x<5 and cell.y>=0 and cell.y<5

func _draw() -> void:
    draw_rect(Rect2(Vector2.ZERO, size), Color("#263a34"))
    var font := ThemeDB.fallback_font
    var cells: Dictionary = {}
    for id in DISTRICTS:
        cells[DISTRICTS[id]]=id
    for a in DISTRICTS:
        if not _in_window(str(a)):continue
        for offset in [Vector2i(1,0),Vector2i(0,1)]:
            var cell: Vector2i = DISTRICTS[a]+offset
            if not cells.has(cell):continue
            var b := str(cells[cell])
            if not _in_window(b):continue
            var motor := motor_road(str(a),b)
            draw_line(_point(str(a)),_point(b),Color("#68747a") if motor else Color("#86948a"),3.0 if overview else (14.0 if motor else 10.0),true)
    for id in DISTRICTS:
        if not _in_window(str(id)):continue
        var center := _point(str(id))
        var half := Vector2(23,14) if overview else Vector2(50,29)
        var rect := Rect2(center-half,half*2)
        var color := Color("#40564a")
        var label := label_for(str(id))
        if id in ["PARK","WOODS","FARM","GIARDINI"] or label.begins_with("Giardino") or label.begins_with("Campagna"):
            color = Color("#47784e")
        elif id in ["CENTRO","PORTICI"] or label.begins_with("Piazzetta"):
            color = Color("#947453")
        elif id=="RIVER":
            color=Color("#326c85")
        elif id in MOTOR_DISTRICTS:
            color=Color("#675a50")
        draw_rect(rect,color)
        draw_rect(rect,Color("#edcb75") if id==current_place else Color("#a9b6a9"),false,2.0 if id==current_place else 1.0)
        if overview:
            var short_label := label.right(2) if outer(str(id)) else label.left(6)
            draw_string(font,center+Vector2(-23,4),short_label,HORIZONTAL_ALIGNMENT_CENTER,46,9,Color.WHITE)
        else:
            draw_string(font,center+Vector2(-49,-3),label,HORIZONTAL_ALIGNMENT_CENTER,98,12,Color.WHITE)
            var detail := "YOU" if id==current_place else ("%s min" % travel_minutes[id] if travel_minutes.has(id) else "—")
            draw_string(font,center+Vector2(-49,16),detail,HORIZONTAL_ALIGNMENT_CENTER,98,12,Color("#edcb75"))
    var footer := "121 districts · 528 × 528 m · Local map / M to close" if overview else "Local districts · times supplied by Java · M / Esc to close"
    draw_string(font,Vector2(10,417),footer,HORIZONTAL_ALIGNMENT_LEFT,-1,13,Color.WHITE)
