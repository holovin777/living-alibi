extends Node3D
## A visual projection only. Geometry never owns crops, animals or money.
var city: Dictionary={}
var anchors: Dictionary={}
var models: Dictionary={}
var signatures: Dictionary={}
var hens: Array[Node3D]=[]
var elapsed:=0.0
var materials: Dictionary={}
var missing_site:=false
var missing_yard:=false

func configure(map: Dictionary) -> void:
    city=map

static func point(raw: Array) -> Vector2:
    return Vector2(float(raw[0]),float(raw[1]))

static func inside_map(map: Dictionary,center: Vector2,radius: float) -> bool:
    # Importer bounds are [south, west, north, east] in degrees.
    # Use the same local metre projection as tools/import_vigevano.py.
    var bounds: Array=map.get("bounds",[])
    var origin: Array=map.get("origin",[])
    if bounds.is_empty() and origin.is_empty():return true # synthetic test maps
    if bounds.size()!=4 or origin.size()!=2:return false
    var scale_x:=111320.0*cos(deg_to_rad(float(origin[0])))
    var west: float=(float(bounds[1])-float(origin[1]))*scale_x
    var east: float=(float(bounds[3])-float(origin[1]))*scale_x
    var north: float=(float(origin[0])-float(bounds[2]))*111132.0
    var south: float=(float(origin[0])-float(bounds[0]))*111132.0
    return center.x-radius>=west and center.x+radius<=east and center.y-radius>=north and center.y+radius<=south

static func clear_at(map: Dictionary,center: Vector2,radius: float) -> bool:
    if not inside_map(map,center,radius):return false
    # Conservative building bounding rectangles (including their interiors)
    # and distance from known roads/rail. Scan every chunk because features
    # are assigned to the chunk containing their centroid, not their bounds.
    for data in map.get("chunks",{}).values():
        for b in data.get("buildings",[]):
            var points: Array=b.get("points",[])
            if points.is_empty():continue
            var bounds:=Rect2(point(points[0]),Vector2.ZERO)
            for p in points:bounds=bounds.expand(point(p))
            if bounds.grow(radius+0.4).has_point(center):return false
        for kind in ["roads","rail"]:
            for road in data.get(kind,[]):
                var nearest:=Geometry2D.get_closest_point_to_segment(center,point(road["a"]),point(road["b"]))
                if center.distance_to(nearest)<radius+float(road.get("width",4.0))*0.5+0.6:return false
        for kind in ["trees","benches"]:
            for prop in data.get(kind,[]):
                if center.distance_to(point(prop["point"]))<radius+1.5:return false
    return true

func _anchor(id: String,record: Dictionary,radius: float,distances: Array) -> Variant:
    var origin:=Vector2(float(record["x"]),float(record["z"]))
    var key:=id+":"+str(origin)
    if anchors.has(key):return anchors[key]
    # Reserve both plots even when one is temporarily outside the view.
    var result: Variant=null
    for distance in distances:
        for step in range(16):
            var theta:=step*TAU/16.0
            var candidate:=origin+Vector2(cos(theta),sin(theta))*float(distance)
            if clear_at(city,candidate,radius):
                var other: String="yard" if id=="site" else "site"
                var other_radius: float=6.5 if other=="yard" else 4.5
                if anchors.has(other+"_reserved") and candidate.distance_to(anchors[other+"_reserved"])<radius+other_radius:continue
                result=candidate
                break
        if result!=null:break
    anchors[key]=result
    if result!=null:anchors[id+"_reserved"]=result
    return result

func _mat(color: Color) -> StandardMaterial3D:
    if not materials.has(color):
        var m:=StandardMaterial3D.new()
        m.albedo_color=color
        m.roughness=0.86
        if color.a<1:m.transparency=BaseMaterial3D.TRANSPARENCY_ALPHA
        materials[color]=m
    return materials[color]

func _box(parent: Node3D,at: Vector3,size: Vector3,color: Color,solid: bool=false) -> MeshInstance3D:
    var mesh:=BoxMesh.new()
    mesh.size=size
    var part:=MeshInstance3D.new()
    part.mesh=mesh
    part.material_override=_mat(color)
    part.position=at
    parent.add_child(part)
    if solid:
        var body:=StaticBody3D.new()
        body.position=at
        var collision:=CollisionShape3D.new()
        var shape:=BoxShape3D.new()
        shape.size=size
        collision.shape=shape
        body.add_child(collision)
        parent.add_child(body)
    return part

func _sphere(parent: Node3D,at: Vector3,size: Vector3,color: Color) -> void:
    var mesh:=SphereMesh.new()
    mesh.radial_segments=8
    mesh.rings=4
    var part:=MeshInstance3D.new()
    part.mesh=mesh
    part.material_override=_mat(color)
    part.position=at
    part.scale=size
    parent.add_child(part)

func _sign(parent: Node3D,at: Vector3,words: String) -> void:
    var label:=Label3D.new()
    label.text=words
    label.font_size=36
    label.pixel_size=0.009
    label.billboard=BaseMaterial3D.BILLBOARD_ENABLED
    label.position=at
    parent.add_child(label)

func _remove(id: String) -> void:
    if models.has(id):
        var old: Node3D=models[id]
        remove_child(old)
        old.queue_free()
        models.erase(id)
    signatures.erase(id)
    if id=="yard":hens.clear()

func sync(data: Dictionary) -> void:
    missing_yard=false
    missing_site=false
    for id in ["yard","site"]:
        var record=data.get(id)
        if not record is Dictionary:
            _remove(id)
            continue
        var center=_anchor(id,record["anchor"],6.5 if id=="yard" else 4.5,[9,12] if id=="yard" else [20,28,36,48,60])
        if center==null:
            _remove(id)
            if id=="yard":missing_yard=true
            else:missing_site=true
            continue
        var signature:=hash(record)
        if signatures.get(id)==signature and models.has(id):continue
        _remove(id)
        var root:=Node3D.new()
        root.position=Vector3(center.x,0.06,center.y)
        add_child(root)
        models[id]=root
        signatures[id]=signature
        if id=="yard":_yard(root,record)
        else:_site(root,record)

func _yard(root: Node3D,record: Dictionary) -> void:
    var garden: Dictionary=record["garden"]
    var orchard: Dictionary=record["orchard"]
    var flock: Dictionary=record["hens"]
    var wood:=Color("#96704b")
    var soil:=Color("#76533c")
    var grass:=Color("#78935c")
    _box(root,Vector3(0,0,0),Vector3(9,0.08,7),grass)
    _box(root,Vector3(-2.1,0.09,-1.6),Vector3(3.5,0.20,2.4),soil)
    for row in range(3):
        _box(root,Vector3(-2.1,0.23,-2.35+row*0.75),Vector3(3.15,0.12,0.40),soil.darkened(0.2))
        if garden.get("planted",false):
            var growth:=float(garden.get("growth",0))/maxf(1,float(garden.get("required",3)))
            for i in range(5):
                var height:=0.12+growth*0.4
                var at:=Vector3(-3.4+i*0.64,0.31+height*0.5,-2.35+row*0.75)
                _sphere(root,at,Vector3(0.16+growth*0.25,height,0.16+growth*0.25),Color("#4c873b"))
    if garden.get("greenhouse",false):
        var frame:=Color("#93b1a7")
        var glass:=Color(0.63,0.85,0.78,0.18)
        for x in [-4.0,-0.2]:
            for z in [-2.95,-0.25]:_box(root,Vector3(x,1.1,z),Vector3(0.08,2.2,0.08),frame)
            _box(root,Vector3(x,1.1,-1.6),Vector3(0.035,2.1,2.7),glass)
        _box(root,Vector3(-2.1,2.2,-1.6),Vector3(3.85,0.06,2.8),glass)
        _box(root,Vector3(-2.1,1.1,-2.95),Vector3(3.85,2.1,0.035),glass)
    if garden.get("barrel",false):
        _box(root,Vector3(-4.0,0.49,0),Vector3(0.65,0.98,0.65),Color("#426c83"))
        var level:=clampf(float(garden.get("rainwater",0))/8.0,0,1)
        _box(root,Vector3(-4.0,0.15+level*0.7,0.34),Vector3(0.13,0.08,0.04),Color("#b4e0f2"))
    if garden.get("composter",false):_box(root,Vector3(-3.6,0.45,1.1),Vector3(0.9,0.9,0.8),wood)
    for i in range(orchard.get("plants",[]).size()):
        var plant: Dictionary=orchard["plants"][i]
        if not plant.get("planted",false):continue
        var at:=Vector3(0.6+i%2*2.5,0,-2.4+floori(i/2.0)*2.0)
        var growth:=clampf(float(plant.get("age",0))/5.0,0,1)
        var height:=0.55+growth*1.6
        _box(root,at+Vector3.UP*height*0.5,Vector3(0.10+growth*0.11,height,0.10+growth*0.11),wood)
        var crown:=0.35+growth*1.05
        _sphere(root,at+Vector3.UP*height,Vector3(crown,crown,crown),Color("#5b7d43"))
        if int(plant.get("fruit",0))>0:
            var fruit_color:=Color("#c64c35") if str(plant.get("id",""))=="apples" else Color("#c8aa57")
            for n in range(mini(6,int(plant["fruit"])*2)):
                var angle:=n*TAU/6
                _sphere(root,at+Vector3(cos(angle)*crown*0.42,height,sin(angle)*crown*0.42),Vector3.ONE*0.14,fruit_color)
    if orchard.get("hive",false):_box(root,Vector3(4.0,0.55,-0.4),Vector3(0.6,0.9,0.6),Color("#d5a14b"))
    if orchard.get("cellar",false):_box(root,Vector3(3.7,0.7,2.4),Vector3(1.2,1.4,1.0),wood)
    if flock.get("coop",false):
        _box(root,Vector3(-2.9,0.55,2.35),Vector3(1.65,1.1,1.3),wood)
        _box(root,Vector3(-2.9,1.16,2.35),Vector3(1.9,0.14,1.55),Color("#ad6850"))
        for i in range(clampi(int(flock.get("hens",0)),0,3)):
            var bird:=Node3D.new()
            root.add_child(bird)
            hens.append(bird)
            _sphere(bird,Vector3(0,0.25,0),Vector3(0.32,0.38,0.45),Color("#ede3cd"))
            _sphere(bird,Vector3(0,0.44,-0.18),Vector3.ONE*0.21,Color("#f0e8d8"))
            _box(bird,Vector3(0,0.44,-0.30),Vector3(0.09,0.07,0.12),Color("#e1a13b"))
            _box(bird,Vector3(0,0.55,-0.18),Vector3(0.045,0.10,0.12),Color("#b84034"))
            for x in [-0.07,0.07]:_box(bird,Vector3(x,0.06,0),Vector3(0.035,0.12,0.04),Color("#d19b48"))
    _sign(root,Vector3(0,1.9,3.4),"CASA DEL GLICINE · ORTO")

func _site(root: Node3D,record: Dictionary) -> void:
    var stage:=clampi(int(record.get("stage",0)),0,3)
    _box(root,Vector3(0,0,0),Vector3(6,0.10,5.6),Color("#a68c6b"))
    if stage>=1:_box(root,Vector3(0,0.19,0),Vector3(5.4,0.35,4.8),Color("#a9acaa"),true)
    if stage>=2:
        var wall:=Color("#c8a788")
        _box(root,Vector3(0,1.75,-2.25),Vector3(5,3,0.25),wall,true)
        for x in [-2.4,2.4]:_box(root,Vector3(x,1.75,0),Vector3(0.25,3,4.5),wall,true)
        for x in [-1.65,1.65]:_box(root,Vector3(x,1.75,2.25),Vector3(1.7,3,0.25),wall,true)
        _box(root,Vector3(0,2.85,2.25),Vector3(1.6,0.8,0.25),wall,true)
    if stage>=3:
        for side in [-1.0,1.0]:
            var roof:=_box(root,Vector3(float(side)*1.4,3.75,0),Vector3(3.15,0.18,5.0),Color("#ad624b"))
            roof.rotation.z=-float(side)*0.32
        _box(root,Vector3(0,1.3,2.25),Vector3(1.15,2.15,0.16),Color("#654c39"),true)
    elif record.get("active",false):
        for x in [-2.8,2.8]:_box(root,Vector3(x,0.65,2.65),Vector3(0.09,1.3,0.09),Color("#d4a049"))
        _box(root,Vector3(0,0.95,2.65),Vector3(5.65,0.13,0.10),Color("#d4a049"))
    _sign(root,Vector3(0,2.25 if stage<2 else 4.7,2.65),"VIA VERDI · %s/3" % stage)

func animate(delta: float) -> void:
    elapsed+=delta
    for i in range(hens.size()):
        var bird:=hens[i]
        var angle:=elapsed*0.65+i*TAU/3.0
        bird.position=Vector3(-0.8+cos(angle)*0.65,0.05,2.35+sin(angle)*0.55)
        bird.rotation.y=atan2(sin(angle),-cos(angle))
