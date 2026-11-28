extends RefCounted
## UI restrictions and authoritative life rules are distinct from network errors.
static func block_reason(map_open: bool,chat_open: bool,actions_open: bool,phone_open: bool,shared: bool,has_snapshot: bool,world: Dictionary) -> String:
    if map_open:return "Map open / Mappa aperta · M or Esc"
    if chat_open:return "Chat open / Chat aperta · Esc"
    if actions_open:return "Actions open / Azioni aperte · Esc"
    if phone_open:return "Phone open / Telefono aperto · P or Esc"
    if not shared:return ""
    if not has_snapshot:return "Waiting for world / Attesa del mondo..."
    if bool(world.get("confined",false)):return "In custody / In custodia · F: actions"
    if bool(world.get("rebirth",false)):
        var life: Dictionary=world.get("life",{})
        if not bool(life.get("canWalk",false)):
            return "Walking unavailable / Movimento non disponibile · %s · age / età %s · F: life actions" % [life.get("activity",life.get("phase","")),life.get("age",0)]
    return ""

static func outside_limit(life: Dictionary,center: Vector2,current: Vector2,predicted: Vector2) -> bool:
    var radius:=float(life.get("radius",24))
    if radius<=0.0 or str(life.get("phase",""))=="LINKED_ADULT":return false
    return predicted.distance_to(center)>radius and predicted.distance_to(center)>current.distance_to(center)
