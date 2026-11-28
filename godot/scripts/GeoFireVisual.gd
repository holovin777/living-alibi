extends Node3D
## Bounded district incident indicator; never creates or spreads a simulation fire.
var flames: Array[MeshInstance3D]=[]
var smoke: Array[MeshInstance3D]=[]
var light: OmniLight3D
var elapsed:=0.0
var intensity:=0.0
var target:=0.0
var incident:=""
var flame_material: StandardMaterial3D
var smoke_material: StandardMaterial3D

func _ready() -> void:
    flame_material=StandardMaterial3D.new()
    flame_material.albedo_color=Color("#ff9b39")
    flame_material.emission_enabled=true
    flame_material.emission=Color("#ff6720")
    flame_material.emission_energy_multiplier=2.0
    smoke_material=StandardMaterial3D.new()
    smoke_material.transparency=BaseMaterial3D.TRANSPARENCY_ALPHA
    smoke_material.albedo_color=Color(0.16,0.17,0.18,0.32)
    var flame_mesh:=SphereMesh.new()
    flame_mesh.radius=0.48
    flame_mesh.height=0.96
    flame_mesh.radial_segments=8
    flame_mesh.rings=4
    var smoke_mesh:=SphereMesh.new()
    smoke_mesh.radius=0.9
    smoke_mesh.height=1.8
    smoke_mesh.radial_segments=8
    smoke_mesh.rings=4
    for i in range(7):
        var flame:=MeshInstance3D.new()
        flame.mesh=flame_mesh
        flame.material_override=flame_material
        flame.cast_shadow=GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
        add_child(flame)
        flames.append(flame)
    for i in range(12):
        var puff:=MeshInstance3D.new()
        puff.mesh=smoke_mesh
        puff.material_override=smoke_material
        puff.cast_shadow=GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
        add_child(puff)
        smoke.append(puff)
    light=OmniLight3D.new()
    light.light_color=Color("#ff732d")
    light.omni_range=11
    light.shadow_enabled=false
    add_child(light)
    hide()

func sync(record: Dictionary,viewer: Vector3,outdoors: bool) -> void:
    if not outdoors or record.is_empty():
        incident=""
        target=0.0
        intensity=0.0
        hide()
        return
    var point:=Vector3(float(record.get("x",0)),0.2,float(record.get("z",0)))
    if Vector2(point.x-viewer.x,point.z-viewer.z).length()>280.0:
        target=0.0
        intensity=0.0
        hide()
        return
    var next:=str(record.get("id",""))
    if incident!=next:
        incident=next
        intensity=0.0
        elapsed=0.0
    position=point
    target=clampf(float(record.get("intensity",0)),0.0,1.0)
    show()

func animate(delta: float) -> void:
    if not visible:return
    elapsed+=delta
    intensity=move_toward(intensity,target,delta*0.65)
    for i in range(flames.size()):
        var phase:=elapsed*5.0+i*1.7
        var strength:=intensity*(0.8+0.2*sin(phase))
        var angle:=i*TAU/flames.size()
        var flame:=flames[i]
        flame.position=Vector3(cos(angle)*0.9,0.5+strength*0.7,sin(angle)*0.9)
        flame.scale=Vector3(strength*(0.8+0.2*sin(phase)),strength*(2.5+0.5*cos(phase)),strength)
        flame.visible=strength>0.02
    for i in range(smoke.size()):
        var f:=fposmod(elapsed*0.17+float(i)/smoke.size(),1.0)
        var puff:=smoke[i]
        var size: float=intensity*(0.5+f*2.5)*(1.0-f*0.35)
        puff.position=Vector3(f*f*5.0+sin(i*2.4)*0.45,1.6+f*15.0,sin(f*4.0+i)*0.6)
        puff.scale=Vector3.ONE*size
        puff.visible=size>0.02
    light.position=Vector3(0,1.4,0)
    light.light_energy=intensity*(2.3+0.35*sin(elapsed*11.0))
    light.visible=intensity>0.02
