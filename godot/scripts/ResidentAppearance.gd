extends RefCounted
## Stable procedural identity; the server provides a coarse visible life stage.
static func profile(id: String) -> Dictionary:
    var rng := RandomNumberGenerator.new()
    rng.seed = absi(id.hash())
    var skins := [Color("#d8aa87"),Color("#b9845f"),Color("#edc4a6"),Color("#8e5a41"),Color("#c69574")]
    var hair := [Color("#382b25"),Color("#594437"),Color("#262322"),Color("#936c43"),Color("#79513a"),Color("#77736b")]
    var clothes := [Color("#546c7d"),Color("#7c4e48"),Color("#6a7651"),Color("#b29a75"),Color("#53535f"),Color("#476b64"),Color("#935e40"),Color("#8a8096")]
    var pants := [Color("#303d4d"),Color("#55554e"),Color("#47433c"),Color("#72716a")]
    var result := {
        "height":rng.randf_range(0.86,1.10),
        "build":rng.randf_range(0.76,1.22),
        "skin":skins[rng.randi_range(0,skins.size()-1)],
        "hair":hair[rng.randi_range(0,hair.size()-1)],
        "jacket":clothes[rng.randi_range(0,clothes.size()-1)],
        "pants":pants[rng.randi_range(0,pants.size()-1)],
        "style":rng.randi_range(0,3),
        "accessory":rng.randi_range(0,4),
        "pace":rng.randf_range(0.82,1.18),
        "stride":rng.randf_range(0.30,0.48)
    }

    result["eye"]=[Color("#413427"),Color("#50694c"),Color("#527387"),Color("#807154")][rng.randi_range(0,3)]
    result["nose"]=rng.randf_range(0.027,0.047)
    result["face_width"]=rng.randf_range(0.90,1.10)
    result["beard"]=rng.randi_range(0,5)
    result["hair_detail"]=rng.randi_range(0,4)
    result["clothing_style"]=rng.randi_range(0,6)
    result["accent"]=[Color("#ded5c3"),Color("#a85d49"),Color("#7eaa9d"),Color("#5a739c"),Color("#c3a35b")][rng.randi_range(0,4)]
    result["shoulders"]=rng.randf_range(0.87,1.13)
    result["waist"]=rng.randf_range(0.88,1.12)
    result["hair_extra"]=rng.randi_range(0,5)
    result["face_detail"]=rng.randi_range(0,5)
    result["shoe_color"]=[Color("#303032"),Color("#76513d"),Color("#d4d0c5"),Color("#4a5966")][rng.randi_range(0,3)]
    return result

static func stage_for_age(age: int) -> String:
    if age<6:return "PRESCHOOL"
    if age<12:return "CHILD"
    if age<18:return "TEEN"
    if age<45:return "ADULT"
    if age<67:return "MATURE"
    return "SENIOR"

static func body_scale(profile_data: Dictionary,stage: String) -> Vector3:
    var height := float(profile_data.get("height",1.0))
    var build := float(profile_data.get("build",1.0))
    var growth := 1.0
    var width := 1.0
    match stage:
        "PRESCHOOL":growth=0.53; width=0.62
        "CHILD":growth=0.72; width=0.75
        "TEEN":growth=0.90; width=0.88
        "SENIOR":growth=0.95
    return Vector3(build*width,height*growth,build*width)

static func hair_color(base: Color,stage: String) -> Color:
    if stage=="SENIOR":return base.lerp(Color("#d2d0c8"),0.75)
    if stage=="MATURE":return base.lerp(Color("#b2aca3"),0.27)
    return base

static func pace_factor(stage: String) -> float:
    return 0.72 if stage in ["PRESCHOOL","SENIOR"] else (0.90 if stage=="CHILD" else 1.0)
