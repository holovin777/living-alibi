extends RefCounted
## Shared town interface style.
static func surface(fill: Color, edge: Color, radius: int, margin: float) -> StyleBoxFlat:
    var s := StyleBoxFlat.new()
    s.bg_color = fill
    s.border_color = edge
    s.set_border_width_all(1)
    s.set_corner_radius_all(radius)
    s.content_margin_left = margin
    s.content_margin_right = margin
    s.content_margin_top = margin
    s.content_margin_bottom = margin
    return s

static func build() -> Theme:
    var theme := Theme.new()
    theme.default_font_size = 15
    var cream := Color("#f4ede0")
    var gold := Color("#e4bd79")
    var panel := surface(Color(0.055,0.13,0.12,0.93),Color("#507568"),12,12)
    panel.shadow_color = Color(0,0,0,0.28)
    panel.shadow_size = 7
    panel.shadow_offset = Vector2(0,3)
    theme.set_stylebox("panel","PanelContainer",panel)
    theme.set_color("font_color","Label",cream)
    theme.set_color("default_color","RichTextLabel",cream)
    for kind in ["Button","OptionButton","CheckButton","CheckBox"]:
        theme.set_color("font_color",kind,cream)
        theme.set_color("font_hover_color",kind,Color.WHITE)
        theme.set_color("font_pressed_color",kind,cream)
        theme.set_color("font_focus_color",kind,cream)
        theme.set_color("font_disabled_color",kind,Color("#7e9991"))
        theme.set_stylebox("normal",kind,surface(Color("#23453d"),Color("#446b5f"),7,7))
        theme.set_stylebox("hover",kind,surface(Color("#366254"),gold,7,7))
        theme.set_stylebox("pressed",kind,surface(Color("#31594b"),gold,7,7))
        theme.set_stylebox("disabled",kind,surface(Color("#1b322e"),Color("#345149"),7,7))
        var focus := surface(Color(0,0,0,0),gold,7,0)
        focus.set_border_width_all(2)
        theme.set_stylebox("focus",kind,focus)
    theme.set_stylebox("normal","LineEdit",surface(Color("#102a25"),Color("#659083"),7,9))
    theme.set_stylebox("focus","LineEdit",surface(Color(0,0,0,0),gold,7,0))
    theme.set_color("font_color","LineEdit",cream)
    theme.set_color("font_placeholder_color","LineEdit",Color("#b9cbc5"))
    theme.set_color("caret_color","LineEdit",gold)
    theme.set_stylebox("normal","TextEdit",surface(Color("#102a25"),Color("#659083"),7,8))
    theme.set_color("font_color","TextEdit",cream)
    theme.set_stylebox("normal","RichTextLabel",surface(Color("#102a25"),Color("#35584e"),7,9))
    theme.set_stylebox("slider","HSlider",surface(Color("#24463d"),Color("#446b5f"),5,3))
    theme.set_stylebox("grabber_area","HSlider",surface(Color("#a98c59"),Color("#a98c59"),5,3))
    theme.set_constant("separation","VBoxContainer",7)
    theme.set_constant("separation","HBoxContainer",6)
    theme.set_constant("h_separation","GridContainer",6)
    theme.set_constant("v_separation","GridContainer",6)
    theme.set_stylebox("panel","PopupMenu",surface(Color("#14332b"),Color("#659083"),8,7))
    theme.set_color("font_color","PopupMenu",cream)
    theme.set_stylebox("hover","PopupMenu",surface(Color("#366254"),gold,5,3))
    return theme

static func title(label: Label) -> void:
    label.add_theme_color_override("font_color",Color("#f2cc87"))
    label.add_theme_font_size_override("font_size",20)

static func secondary(label: Label) -> void:
    label.add_theme_color_override("font_color",Color("#b9cbc5"))
    label.add_theme_font_size_override("font_size",13)
