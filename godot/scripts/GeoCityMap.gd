extends Control
signal destination_selected(point: Vector2)
var lines := PackedVector2Array()
var route := PackedVector2Array()
var player := Vector2.ZERO
var min_point := Vector2.ZERO
var max_point := Vector2.ONE
var scale_factor := 1.0
var offset := Vector2.ZERO

func configure(graph: Dictionary) -> void:
    var points: Dictionary=graph.get("nodes",{})
    if points.is_empty():return
    min_point=Vector2(INF,INF)
    max_point=Vector2(-INF,-INF)
    for id in points:
        var p: Array=points[id]
        var v:=Vector2(float(p[0]),float(p[1]))
        min_point=min_point.min(v)
        max_point=max_point.max(v)
    lines.clear()
    for e in graph.get("edges",[]):
        if not points.has(e["a"]) or not points.has(e["b"]):continue
        var a: Array=points[e["a"]]
        var b: Array=points[e["b"]]
        lines.append(Vector2(float(a[0]),float(a[1])))
        lines.append(Vector2(float(b[0]),float(b[1])))
    queue_redraw()

func _transform_map() -> void:
    var span := (max_point-min_point).max(Vector2.ONE)
    var area := (size-Vector2(36,76)).max(Vector2.ONE)
    scale_factor=minf(area.x/span.x,area.y/span.y)
    offset=Vector2(18,40)+(area-span*scale_factor)*0.5-min_point*scale_factor

func _screen(p: Vector2) -> Vector2:
    return p*scale_factor+offset

func _draw() -> void:
    _transform_map()
    draw_rect(Rect2(Vector2.ZERO,size),Color("#253c39"))
    var pixels := PackedVector2Array()
    for p in lines:pixels.append(_screen(p))
    if pixels.size()>=2:draw_multiline(pixels,Color("#a7b2a5"),1.0,true)
    if route.size()>1:
        var path := PackedVector2Array()
        for p in route:path.append(_screen(p))
        draw_polyline(path,Color("#e9c45f"),3.5,true)
        draw_circle(path[-1],5,Color("#e9c45f"))
    draw_circle(_screen(player),6,Color("#6bd4f0"))
    draw_arc(_screen(player),8,0,TAU,24,Color.WHITE,1.2,true)
    var font:=ThemeDB.fallback_font
    draw_string(font,Vector2(12,23),"VIGEVANO · click a street for a walking route",HORIZONTAL_ALIGNMENT_LEFT,-1,16,Color.WHITE)
    draw_string(font,Vector2(12,size.y-12),"M / Esc: close · © OpenStreetMap contributors",HORIZONTAL_ALIGNMENT_LEFT,-1,12,Color.WHITE)

func _gui_input(event: InputEvent) -> void:
    if event is InputEventMouseButton and event.pressed and event.button_index==MOUSE_BUTTON_LEFT:
        _transform_map()
        destination_selected.emit((event.position-offset)/scale_factor)
        accept_event()
