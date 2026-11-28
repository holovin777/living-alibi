extends RefCounted
## Batched procedural detail. The OSM footprint remains authoritative;
## facade details, furniture and street lighting are artistic approximations.
const PALETTE: Array[Color]=[Color("#cdb49a"),Color("#d5c5a8"),Color("#bc907c"),Color("#e0d1b8"),Color("#b8b4a5"),Color("#d8b18f")]
const MAX_WINDOWS=2200
const MAX_LAMPS=16
var windows_count:=0
var lamps_count:=0
var lamp_positions: Array[Vector3]=[]
var seen_lamps: Dictionary={}
var batches: Dictionary={}

func _quad(kind: String,a: Vector3,b: Vector3,c: Vector3,d: Vector3) -> void:
    if not batches.has(kind):batches[kind]=PackedVector3Array()
    var vertices: PackedVector3Array=batches[kind]
    vertices.append_array(PackedVector3Array([a,b,c,a,c,d]))
    batches[kind]=vertices

func _rect(kind: String,at: Vector3,tangent: Vector3,width: float,height: float) -> void:
    var half:=tangent*width*0.5
    var up:=Vector3.UP*height*0.5
    _quad(kind,at-half-up,at+half-up,at+half+up,at-half+up)

func _box(kind: String,at: Vector3,dimensions: Vector3,tangent: Vector3,normal: Vector3) -> void:
    var x:=tangent*dimensions.x*0.5
    var y:=Vector3.UP*dimensions.y*0.5
    var z:=normal*dimensions.z*0.5
    _quad(kind,at-x-y-z,at+x-y-z,at+x+y-z,at-x+y-z)
    _quad(kind,at+x-y+z,at-x-y+z,at-x+y+z,at+x+y+z)
    _quad(kind,at-x-y+z,at-x-y-z,at-x+y-z,at-x+y+z)
    _quad(kind,at+x-y-z,at+x-y+z,at+x+y+z,at+x+y-z)
    _quad(kind,at-x+y-z,at+x+y-z,at+x+y+z,at-x+y+z)
    _quad(kind,at-x-y+z,at+x-y+z,at+x-y-z,at-x-y-z)

func facade(feature: Dictionary) -> void:
    var points: Array=feature["points"]
    var h:=float(feature["height"])
    var type:=str(feature.get("kind","yes"))
    if points.size()<3:return
    var id_hash:=str(feature.get("id","")).hash()
    var wall_key: String="wall_%d" % posmod(id_hash,PALETTE.size())
    var signed_area:=0.0
    for i in range(points.size()):
        var p: Array=points[i]
        var q: Array=points[(i+1)%points.size()]
        signed_area+=float(p[0])*float(q[1])-float(q[0])*float(p[1])
    var floors:=clampi(roundi(h/3.2),1,12)
    var floor_height:=h/floors
    var residential:=type in ["yes","residential","apartments","house","terrace","detached"]
    var commercial:=type in ["retail","commercial","supermarket","shop"]
    for i in range(points.size()):
        var p: Array=points[i]
        var q: Array=points[(i+1)%points.size()]
        var a:=Vector3(float(p[0]),0,float(p[1]))
        var b:=Vector3(float(q[0]),0,float(q[1]))
        var length:=a.distance_to(b)
        if length<0.02:continue
        var tangent: Vector3=(b-a)/length
        var normal:=Vector3(tangent.z,0,-tangent.x) if signed_area>=0 else Vector3(-tangent.z,0,tangent.x)
        _quad(wall_key,a,b,b+Vector3.UP*h,a+Vector3.UP*h)
        if length<3.2 or floor_height<2.0 or type in ["garage","garages","shed","roof","ruins","church","chapel","industrial","warehouse"]:continue
        var storefront:=commercial and i==0 and length>=5.0 and floor_height>=2.6 and windows_count<MAX_WINDOWS
        if storefront:_shopfront(a,b,tangent,normal,floor_height,id_hash)
        var columns:=clampi(floori(length/3.0),1,14)
        var door_column:=columns/2
        for floor_index in range(floors):
            if storefront and floor_index==0:continue
            for column in range(columns):
                if windows_count>=MAX_WINDOWS:break
                var middle:=a.lerp(b,(column+0.5)/columns)+normal*0.04
                var is_door:=floor_index==0 and i==0 and column==door_column
                if is_door:
                    _rect("stone",middle+Vector3.UP*1.18,tangent,1.44,2.36)
                    _rect("door",middle+normal*0.025+Vector3.UP*1.1,tangent,1.18,2.15)
                else:
                    var at:=middle+Vector3.UP*(floor_height*floor_index+floor_height*0.57)
                    var wh:=minf(1.35,floor_height*0.48)
                    _rect("stone",at,tangent,1.28,wh+0.18)
                    _rect("glass",at+normal*0.02,tangent,1.05,wh)
                    _rect("frame",at+normal*0.035,tangent,0.055,wh)
                    _rect("frame",at+normal*0.035,tangent,1.05,0.055)
                    if residential:
                        _rect("shutter",at-tangent*0.69+normal*0.03,tangent,0.28,wh)
                        _rect("shutter",at+tangent*0.69+normal*0.03,tangent,0.28,wh)
                    if residential and floor_index>0 and floor_index<5 and posmod(id_hash+i+column,5)==0:
                        var deck:=middle+Vector3.UP*(floor_index*floor_height+0.12)+normal*0.46
                        _box("stone",deck,Vector3(1.65,0.15,0.9),tangent,normal)
                        _box("iron",deck+normal*0.42+Vector3.UP*0.88,Vector3(1.65,0.055,0.055),tangent,normal)
                        for rail in range(5):
                            _box("iron",deck+normal*0.42+tangent*(rail-2)*0.38+Vector3.UP*0.48,Vector3(0.035,0.82,0.035),tangent,normal)
                    windows_count+=1

func _shopfront(a: Vector3,b: Vector3,tangent: Vector3,normal: Vector3,floor_height: float,id_hash: int) -> void:
    var length:=a.distance_to(b)
    var middle: Vector3=(a+b)*0.5+normal*0.07
    var width:=minf(length-0.7,10.0)
    var glass_height:=minf(1.95,floor_height-0.65)
    var center:=middle+Vector3.UP*(glass_height*0.5+0.26)
    _rect("shop_frame",center,tangent,width,glass_height+0.16)
    var bays:=clampi(floori(width/1.6),2,6)
    var bay_width:=width/float(bays)
    for i in range(bays):
        var at:=center+tangent*((float(i)+0.5)*bay_width-width*0.5)+normal*0.025
        _rect("shop_glass",at,tangent,bay_width-0.10,glass_height)
        _rect("shop_frame",at+normal*0.02,tangent,bay_width-0.09,0.045)
    var door_at:=center+normal*0.055
    _rect("shop_frame",door_at,tangent,0.95,glass_height)
    _rect("shop_glass",door_at+normal*0.02,tangent,0.79,glass_height-0.12)
    _rect("iron",door_at+normal*0.045+tangent*0.30,tangent,0.035,0.24)
    var awning_at:=middle+Vector3.UP*minf(floor_height-0.15,2.62)+normal*0.53
    var awning_kind: String="awning_red" if posmod(id_hash,2)==0 else "awning_green"
    _box(awning_kind,awning_at,Vector3(width,0.10,1.04),tangent,normal)
    _box(awning_kind,awning_at+normal*0.50-Vector3.UP*0.10,Vector3(width,0.18,0.06),tangent,normal)
    windows_count+=bays

# Only nearly rectangular houses receive a gable. Irregular footprints retain the
# original triangulated roof rather than extending outside the mapped polygon.
func pitched_roof(feature: Dictionary) -> PackedVector3Array:
    var out:=PackedVector3Array()
    var points: Array=feature["points"]
    var kind:=str(feature.get("kind","yes"))
    if points.size()!=4 or kind not in ["house","detached","terrace","residential","yes"]:return out
    var h:=float(feature["height"])
    if h<3.0 or h>13.0:return out
    var corners: Array[Vector3]=[]
    for p in points:corners.append(Vector3(float(p[0]),h,float(p[1])))
    var e:=corners[1]-corners[0]
    var f:=corners[3]-corners[0]
    if e.length()<3 or f.length()<3 or e.length()>35 or f.length()>35:return out
    if absf(e.normalized().dot(f.normalized()))>0.06 or (corners[0]+e+f).distance_to(corners[2])>0.25:return out
    if f.length()>e.length():
        var first: Vector3=corners.pop_front()
        corners.append(first)
    var rise:=clampf(corners[0].distance_to(corners[3])*0.23,0.65,2.8)
    var r0: Vector3=(corners[0]+corners[3])*0.5+Vector3.UP*rise
    var r1: Vector3=(corners[1]+corners[2])*0.5+Vector3.UP*rise
    out.append_array(PackedVector3Array([corners[0],corners[1],r1,corners[0],r1,r0,corners[3],r0,r1,corners[3],r1,corners[2]]))
    var wall_key: String="wall_%d" % posmod(str(feature.get("id","")).hash(),PALETTE.size())
    var verts: PackedVector3Array=batches.get(wall_key,PackedVector3Array())
    verts.append_array(PackedVector3Array([corners[0],r0,corners[3],corners[1],corners[2],r1]))
    batches[wall_key]=verts
    return out

func road(record: Dictionary) -> void:
    var a:=Vector3(float(record["a"][0]),0.045,float(record["a"][1]))
    var b:=Vector3(float(record["b"][0]),0.045,float(record["b"][1]))
    var length:=a.distance_to(b)
    if length<0.1:return
    var direction: Vector3=(b-a)/length
    var side:=Vector3(-direction.z,0,direction.x)
    var width:=float(record["width"])
    var kind:=str(record.get("kind",""))
    var sidewalk:=str(record.get("sidewalk",""))
    if bool(record.get("bridge",false)) and not bool(record.get("tunnel",false)):
        _bridge_edges(a,b,direction,side,width,sidewalk)
    if kind in ["motorway","trunk","motorway_link","trunk_link"]:
        # Roadside white edge lines on major paved roads. No invented sidewalks.
        if width>=3.0 and str(record.get("surface","")) not in ["gravel","ground","dirt","unpaved"]:
            for sign_value in [-1.0,1.0]:
                var edge:=side*float(sign_value)*(width*0.5-0.18)
                var start:=a+edge+Vector3.UP*0.012
                var end:=b+edge+Vector3.UP*0.012
                _quad("mark",start-side*0.065,end-side*0.065,end+side*0.065,start+side*0.065)
        return
    if kind in ["track","footway","path","cycleway","steps","pedestrian","service","living_street"]:return
    if width<4.5:return
    for sign_value in [-1.0,1.0]:
        var sign_num: float=sign_value
        var allowed:=sidewalk in ["both","yes"] or sidewalk=="left" and sign_num<0 or sidewalk=="right" and sign_num>0
        if sidewalk in ["no","none","separate"]:allowed=false
        if sidewalk.is_empty():allowed=width>=5.0 # older cached maps, provisional pavement
        if allowed:
            var inner:=side*sign_num*(width*0.5)
            var outer:=side*sign_num*(width*0.5+1.05)
            _quad("pavement",a+inner+Vector3.UP*0.035,b+inner+Vector3.UP*0.035,b+outer+Vector3.UP*0.035,a+outer+Vector3.UP*0.035)
            _quad("curb",a+inner,b+inner,b+inner+Vector3.UP*0.035,a+inner+Vector3.UP*0.035)
    if width>=6.0 and str(record.get("oneway","no")) not in ["yes","1","true","-1"] and str(record.get("junction",""))!="roundabout" and str(record.get("surface","")) not in ["sett","cobblestone","paving_stones","gravel","dirt","ground","unpaved"]:
        for i in range(ceili(length/7.0)):
            var start:=a+direction*(i*7.0)+Vector3.UP*0.012
            var end:=a+direction*minf(length,i*7.0+2.5)+Vector3.UP*0.012
            _quad("mark",start-side*0.055,end-side*0.055,end+side*0.055,start+side*0.055)
    if lamps_count>=MAX_LAMPS or length<12:return
    var loc: Vector3=a.lerp(b,0.5)+side*(width*0.5+0.65)
    var key:=Vector2i(roundi(loc.x/22.0),roundi(loc.z/22.0))
    if seen_lamps.has(key):return
    seen_lamps[key]=true
    lamps_count+=1
    _box("iron",loc+Vector3.UP*1.75,Vector3(0.09,3.5,0.09),direction,side)
    _box("iron",loc+Vector3.UP*3.46-side*0.28,Vector3(0.10,0.09,0.62),direction,side)
    var bulb:=loc+Vector3.UP*3.39-side*0.56
    _box("lamp",bulb,Vector3(0.24,0.13,0.36),direction,side)
    lamp_positions.append(bulb-Vector3.UP*0.13)

func _bridge_edges(a: Vector3,b: Vector3,direction: Vector3,side: Vector3,width: float,sidewalk: String) -> void:
    var length:=a.distance_to(b)
    if length<0.1:return
    for sign_value in [-1.0,1.0]:
        var s: float=float(sign_value)
        var has_walk:=sidewalk in ["both","yes"] or (sidewalk=="left" and s<0) or (sidewalk=="right" and s>0) or (sidewalk.is_empty() and width>=5)
        var offset:=side*s*(width*0.5+(1.12 if has_walk else 0.12))
        var center: Vector3=(a+b)*0.5+offset
        _box("bridge_concrete",center+Vector3.UP*0.10,Vector3(length,0.20,0.24),direction,side)
        for h in [0.47,0.96]:
            _box("iron",center+Vector3.UP*float(h),Vector3(length,0.065,0.065),direction,side)
        var posts:=maxi(1,ceili(length/2.6))
        for i in range(posts):
            var at:=a.lerp(b,float(i)/posts)+offset
            _box("iron",at+Vector3.UP*0.58,Vector3(0.10,1.12,0.10),direction,side)

func bench(record: Dictionary) -> void:
    var at:=Vector3(float(record["point"][0]),0,float(record["point"][1]))
    var angle:=float(posmod(str(record.get("id","")).hash(),4))*PI/2.0
    var tangent:=Vector3(cos(angle),0,sin(angle))
    var normal:=Vector3(-tangent.z,0,tangent.x)
    for sign_value in [-1.0,1.0]:
        _box("iron",at+tangent*float(sign_value)*0.65+Vector3.UP*0.25,Vector3(0.09,0.50,0.45),tangent,normal)
    _box("bark",at+Vector3.UP*0.50,Vector3(1.65,0.09,0.49),tangent,normal)
    _box("bark",at+Vector3.UP*0.83+normal*0.22,Vector3(1.65,0.29,0.09),tangent,normal)

func _foliage(kind: String,at: Vector3,radius: Vector3) -> void:
    var rings:=6
    var sides:=10
    if not batches.has(kind):batches[kind]=PackedVector3Array()
    var vertices: PackedVector3Array=batches[kind]
    for ring in range(rings):
        var lower: float=-PI/2.0+PI*ring/rings
        var upper: float=-PI/2.0+PI*(ring+1)/rings
        for i in range(sides):
            var a:=TAU*i/sides
            var b:=TAU*(i+1)/sides
            var p:=at+Vector3(cos(lower)*cos(a),sin(lower),cos(lower)*sin(a))*radius
            var q:=at+Vector3(cos(lower)*cos(b),sin(lower),cos(lower)*sin(b))*radius
            var r:=at+Vector3(cos(upper)*cos(b),sin(upper),cos(upper)*sin(b))*radius
            var s:=at+Vector3(cos(upper)*cos(a),sin(upper),cos(upper)*sin(a))*radius
            if ring>0:vertices.append_array(PackedVector3Array([p,q,r]))
            if ring<rings-1:vertices.append_array(PackedVector3Array([p,r,s]))
    batches[kind]=vertices

func tree(record: Dictionary) -> void:
    var at:=Vector3(float(record["point"][0]),0,float(record["point"][1]))
    _box("bark",at+Vector3.UP*1.4,Vector3(0.26,2.8,0.26),Vector3.RIGHT,Vector3.BACK)
    var variation:=float(posmod(str(record.get("id","")).hash(),7))*0.08+0.85
    _foliage("leaves",at+Vector3.UP*3.1,Vector3(1.35,1.6,1.35)*variation)
    _foliage("leaves_light",at+Vector3(-0.4,4.0,0.2),Vector3(1.0,0.95,0.95)*variation)

static func roof_kind(building_kind: String) -> String:
    return "flat" if building_kind in ["industrial","warehouse","commercial","retail","garage","garages","roof"] else "clay"

static func texture_kind(kind: String) -> String:
    if kind.begins_with("wall_") or kind in ["stone","bridge_concrete"]:return "stucco"
    if kind in ["pavement","stone_road","curb"]:return "paving"
    if kind in ["door","shutter","bark"]:return "wood"
    return ""

static func street_sign_candidate(record: Dictionary) -> bool:
    if bool(record.get("bridge",false)) or bool(record.get("tunnel",false)):return false
    if str(record.get("kind","")) not in ["residential","living_street","tertiary","secondary","pedestrian"]:return false
    var a:=Vector2(float(record["a"][0]),float(record["a"][1]))
    var b:=Vector2(float(record["b"][0]),float(record["b"][1]))
    return a.distance_to(b)>=8.0

static func material(kind: String) -> StandardMaterial3D:
    var mat:=StandardMaterial3D.new()
    var colors: Dictionary={"shop_frame":Color("#424c4b"),"shop_glass":Color("#586b70"),"awning_red":Color("#963e37"),"awning_green":Color("#486b53"),"bridge_concrete":Color("#a4a39a"),"waterway":Color("#467b80"),"stone_road":Color("#8b8378"),"dirt_road":Color("#9b896b"),"stone":Color("#d7cab4"),"glass":Color("#43555e"),"door":Color("#6c4c38"),"frame":Color("#a9a197"),"shutter":Color("#526c5a"),"iron":Color("#40484b"),"pavement":Color("#b2aba1"),"curb":Color("#d0c9bd"),"mark":Color("#e0d9c4"),"lamp":Color("#f4db9a"),"bark":Color("#70543d"),"leaves":Color("#657e4c"),"leaves_light":Color("#80985c")}
    mat.albedo_color=PALETTE[int(kind.trim_prefix("wall_"))] if kind.begins_with("wall_") else colors.get(kind,Color.WHITE)
    mat.cull_mode=BaseMaterial3D.CULL_DISABLED
    mat.roughness=0.87
    if kind=="shop_glass":
        mat.roughness=0.25
        mat.emission_enabled=true
        mat.emission=Color("#e4c69d")
        mat.emission_energy_multiplier=0.08
    if kind=="waterway":
        mat.roughness=0.22
        mat.metallic=0.12
    if kind=="lamp":
        mat.emission_enabled=true
        mat.emission=Color("#ffcf88")
        mat.emission_energy_multiplier=0.65
    return mat
