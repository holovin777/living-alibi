extends RefCounted
## Client-side visual identity, not a claim about authoritative age or health.
static func profile(id: String) -> Dictionary:
    var rng := RandomNumberGenerator.new()
    rng.seed = absi(id.hash())
    var skins := [Color("#d8aa87"),Color("#b9845f"),Color("#edc4a6"),Color("#8e5a41"),Color("#c69574")]
    var hair := [Color("#382b25"),Color("#594437"),Color("#262322"),Color("#936c43"),Color("#79513a"),Color("#77736b")]
    var clothes := [Color("#546c7d"),Color("#7c4e48"),Color("#6a7651"),Color("#b29a75"),Color("#53535f"),Color("#476b64"),Color("#935e40"),Color("#8a8096")]
    var pants := [Color("#303d4d"),Color("#55554e"),Color("#47433c"),Color("#72716a")]
    return {
        "height":rng.randf_range(0.89,1.05),
        "build":rng.randf_range(0.86,1.12),
        "skin":skins[rng.randi_range(0,skins.size()-1)],
        "hair":hair[rng.randi_range(0,hair.size()-1)],
        "jacket":clothes[rng.randi_range(0,clothes.size()-1)],
        "pants":pants[rng.randi_range(0,pants.size()-1)],
        "style":rng.randi_range(0,3),
        "accessory":rng.randi_range(0,4),
        "pace":rng.randf_range(0.82,1.18),
        "stride":rng.randf_range(0.30,0.48)
    }
