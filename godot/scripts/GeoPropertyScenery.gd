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
var commercial_anchors: Dictionary={}
var commercial_signature:=0

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
    _sync_commercial(data.get("commercial",[]))

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

# Commercial lots are visual projections only; deterministic clear-space search
# avoids imported footprints, roads, and other reserved commercial plots.
func _sync_commercial(projects: Array) -> void:
    var signature:=hash(projects)
    if signature==commercial_signature:return
    commercial_signature=signature
    var wanted: Dictionary={}
    for project in projects:
        var id: String="commercial:"+str(project.get("id",""))
        wanted[id]=true
        if not commercial_anchors.has(id):
            var anchor: Dictionary=project.get("anchor",{})
            var origin:=Vector2(float(anchor.get("x",0)),float(anchor.get("z",0)))
            var found: Variant=null
            for distance in [70,85,100,120,150]:
                for step in range(16):
                    var angle:=float(step)*TAU/16.0
                    var candidate:=origin+Vector2(cos(angle),sin(angle))*float(distance)
                    if not clear_at(city,candidate,7.0):continue
                    var overlap:=false
                    for other in commercial_anchors.values():
                        if other!=null and candidate.distance_to(other)<15:overlap=true
                    for other_id in ["yard_reserved","site_reserved"]:
                        if anchors.has(other_id) and candidate.distance_to(anchors[other_id])<15:overlap=true
                    if not overlap:
                        found=candidate
                        break
                if found!=null:break
            commercial_anchors[id]=found
        var center: Variant=commercial_anchors[id]
        if center==null:continue
        var sig:=hash(project)
        if signatures.get(id)==sig and models.has(id):continue
        _remove(id)
        var root:=Node3D.new()
        root.position=Vector3(center.x,0.06,center.y)
        add_child(root)
        models[id]=root
        signatures[id]=sig
        _commercial_site(root,project)
    for id in models.keys():
        if str(id).begins_with("commercial:") and not wanted.has(id):_remove(id)

func _commercial_site(root: Node3D,project: Dictionary) -> void:
    var stage:=clampi(int(project.get("stage",0)),0,3)
    var active: bool=bool(project.get("active",false))
    var utility: bool=str(project.get("id","")).begins_with("utility:")
    if utility:
        _utility_site(root,stage,active,str(project.get("id","")).trim_prefix("utility:"))
        return
    var industrial: bool=str(project.get("id","")).begins_with("industrial:")
    if industrial:
        _industrial_site(root,stage,active,str(project.get("id","")).trim_prefix("industrial:"))
        return
    var residential: bool=str(project.get("id","")).begins_with("residential:")
    if residential:
        _residential_site(root,stage,active,str(project.get("id","")).trim_prefix("residential:"))
        return
    _box(root,Vector3.ZERO,Vector3(8,0.08,7),Color("#a7977d"))
    if stage>=1:_box(root,Vector3(0,0.17,0),Vector3(6.2,0.3,5.4),Color("#a9acae"),true)
    if stage>=2:
        var wall:=Color("#d2c2aa")
        _box(root,Vector3(0,2.0,0),Vector3(6,3.5,5.2),wall,true)
        for x in [-2.0,0.0,2.0]:
            _box(root,Vector3(x,2.2,2.62),Vector3(1.3,1.5,0.06),Color("#62818d"))
    if stage==3:
        _box(root,Vector3(0,3.85,0),Vector3(6.5,0.28,5.7),Color("#777c7e"))
        _box(root,Vector3(0,1.2,2.64),Vector3(1.1,2.0,0.08),Color("#425a63"))
        _sign(root,Vector3(0,4.4,2.7),"UFFICI · "+str(project.get("id","")).to_upper())
    elif active:
        var yellow:=Color("#d9a643")
        # Static tower crane and delivered material pallets are scenery,
        # not independently moving trucks or physics equipment.
        _box(root,Vector3(-3.2,2.8,-2.7),Vector3(0.25,5.6,0.25),yellow)
        _box(root,Vector3(-1.2,5.6,-2.7),Vector3(4.3,0.22,0.22),yellow)
        _box(root,Vector3(0.5,4.6,-2.7),Vector3(0.04,1.8,0.04),Color("#454545"))
        _box(root,Vector3(3.3,0.25,1.8),Vector3(0.8,0.5,1.6),Color("#a3a5a3"))
        for x in [-3.6,3.6]:_box(root,Vector3(x,0.6,3.25),Vector3(0.12,1.2,0.12),yellow)
        _box(root,Vector3(0,0.85,3.25),Vector3(7.2,0.12,0.12),yellow)
        _sign(root,Vector3(0,2.0,3.3),"CANTIERE · %s/3" % stage)

func _residential_site(root: Node3D,stage: int,active: bool,id: String) -> void:
    _box(root,Vector3.ZERO,Vector3(8,0.08,7),Color("#8a9b70") if stage==3 else Color("#a7977d"))
    if stage>=1:_box(root,Vector3(0,0.18,0),Vector3(6,0.35,5.2),Color("#a9acae"),true)
    if stage>=2:
        _box(root,Vector3(0,1.85,0),Vector3(5.7,3.0,4.9),Color("#e0c6a4"),true)
        for x in [-1.8,1.8]:
            _box(root,Vector3(x,2.0,2.47),Vector3(1.0,1.25,0.07),Color("#6d929a"))
            for side in [-0.65,0.65]:_box(root,Vector3(x+side,2.0,2.53),Vector3(0.27,1.35,0.09),Color("#637a55"))
    if stage==3:
        for side in [-1.0,1.0]:
            var roof:=_box(root,Vector3(side*1.55,3.85,0),Vector3(3.4,0.20,5.5),Color("#a85d49"))
            roof.rotation.z=-side*0.35
        _box(root,Vector3(0,1.35,2.47),Vector3(0.95,2.05,0.10),Color("#76563d"))
        _sign(root,Vector3(0,3.3,2.7),id.to_upper())
    elif active:
        for x in [-3.6,3.6]:_box(root,Vector3(x,0.7,3.25),Vector3(0.12,1.4,0.12),Color("#d1a147"))
        _box(root,Vector3(0,0.95,3.25),Vector3(7.2,0.12,0.12),Color("#d1a147"))
        _sign(root,Vector3(0,1.8,3.3),"CANTIERE · %s/3" % stage)

func _industrial_site(root: Node3D,stage: int,active: bool,id: String) -> void:
    _box(root,Vector3.ZERO,Vector3(10,0.10,8),Color("#93928b"))
    if stage>=1:_box(root,Vector3(0,0.17,0),Vector3(8.0,0.3,6.5),Color("#a9acae"),true)
    if stage>=2:
        _box(root,Vector3(0,2.4,0),Vector3(7.8,4.3,6.2),Color("#b6bbb8"),true)
        for x in [-2.7,0.0,2.7]:_box(root,Vector3(x,3.0,3.13),Vector3(1.4,0.65,0.07),Color("#657c86"))
    if stage==3:
        _box(root,Vector3(0,4.65,0),Vector3(8.2,0.3,6.6),Color("#757f81"))
        _box(root,Vector3(0,1.65,3.14),Vector3(2.7,2.7,0.10),Color("#62696c"))
        for x in [-3.0,3.0]:_box(root,Vector3(x,5.5,-2.0),Vector3(0.7,2.8,0.7),Color("#838e8c"))
        _sign(root,Vector3(0,4.1,3.3),id.to_upper())
    elif active:
        _box(root,Vector3(-4.2,2.5,-3.2),Vector3(0.25,5,0.25),Color("#d2a147"))
        _box(root,Vector3(-2.1,5,-3.2),Vector3(4.5,0.2,0.2),Color("#d2a147"))
        _sign(root,Vector3(0,2.0,3.5),"CANTIERE INDUSTRIALE · %s/3" % stage)

func _utility_site(root: Node3D,stage: int,active: bool,id: String) -> void:
    if stage<3:
        _industrial_site(root,stage,active,id)
        return
    _box(root,Vector3.ZERO,Vector3(9,0.12,7),Color("#939b8e"))
    if id.begins_with("solar"):
        for x in [-2.7,0.0,2.7]:
            for z in [-1.5,1.5]:
                _box(root,Vector3(x,0.7,z),Vector3(0.15,1.4,0.15),Color("#81898b"))
                var panel:=_box(root,Vector3(x,1.45,z),Vector3(2.4,0.12,2.1),Color("#304d73"))
                panel.rotation.x=0.35
    elif id.ends_with("substation"):
        for x in [-2.0,2.0]:
            _box(root,Vector3(x,1.0,0),Vector3(2.6,2.0,2.4),Color("#758d86"),true)
            for z in [-0.7,0.7]:_box(root,Vector3(x,2.4,z),Vector3(0.2,0.8,0.2),Color("#c4c3b5"))
        for x in [-4.1,4.1]:_box(root,Vector3(x,0.8,0),Vector3(0.15,1.6,6.6),Color("#a3a8a6"))
    else:
        for x in [-2.0,2.0]:
            _box(root,Vector3(x,0.55,0),Vector3(3.2,1.1,4.8),Color("#a2b0b5"),true)
            _box(root,Vector3(x,1.15,0),Vector3(2.9,0.08,4.5),Color("#557f91"))
    _sign(root,Vector3(0,2.8,3.4),id.to_upper())
