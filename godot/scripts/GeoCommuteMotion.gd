extends RefCounted
## Interpolate visual traffic on the authoritative directed route, not its chord.
const Signals=preload("res://scripts/GeoSignals.gd")
var points:=PackedVector2Array()
var distances:=PackedFloat64Array()
var offsets: Dictionary={}
var distance:=0.0
var target:=0.0
var index:=0
var trip: String=""
var waiting:=false

static func point_key(p: Vector2) -> Vector2i:
    return Vector2i(roundi(p.x*100.0),roundi(p.y*100.0))

func configure(record: Dictionary,signal_points: Dictionary) -> void:
    points.clear()
    distances.clear()
    offsets=signal_points
    trip=str(record.get("trip",""))
    var total:=0.0
    for raw in record.get("route",[]):
        var at:=Vector2(float(raw["x"]),float(raw["z"]))
        if not points.is_empty():
            var gap:=points[points.size()-1].distance_to(at)
            if gap<0.001:continue
            total+=gap
        points.append(at)
        distances.append(total)
    distance=clampf(float(record.get("distance",0)),0,total)
    target=distance
    index=0
    _locate()
    waiting=false

func _locate() -> void:
    while index+1<points.size()-1 and distance>=distances[index+1]-0.00001:index+=1
    while index>0 and distance<distances[index]:index-=1

func set_target(value: float) -> void:
    if distances.is_empty():return
    target=clampf(value,distance,distances[distances.size()-1])

func at() -> Vector2:
    if points.is_empty():return Vector2.ZERO
    if points.size()==1:return points[0]
    var length:=distances[index+1]-distances[index]
    return points[index].lerp(points[index+1],clampf((distance-distances[index])/maxf(length,0.001),0,1))

func direction() -> Vector2:
    if points.size()<2:return Vector2(0,-1)
    return (points[index+1]-points[index]).normalized()

func advance(budget: float,now: int) -> void:
    waiting=false
    if points.size()<2:return
    var remaining:=minf(maxf(budget,0),maxf(target-distance,0))
    for step in range(32):
        if remaining<=0.00001:break
        var end:=distances[index+1]
        var available:=maxf(0,end-distance)
        var stop:=minf(3.0,(end-distances[index])*0.8)
        var key:=point_key(points[index+1])
        if offsets.has(key) and available>=stop-0.001 and Signals.phase(int(offsets[key]),now,direction())!=2:
            var allowed:=maxf(0,available-stop)
            if remaining>=allowed:
                distance+=allowed
                waiting=true
                break
        var moved:=minf(remaining,available)
        distance+=moved
        remaining-=moved
        if distance>=end-0.00001:
            if index>=points.size()-2:break
            index+=1
        else:break
