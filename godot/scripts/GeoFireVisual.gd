extends Node3D
## Bounded district incident indicator; never creates or spreads a simulation fire.
var flames: Array[MeshInstance3D]=[]
var smoke: Array[MeshInstance3D]=[]
var light: OmniLight3D
var spray: Array[MeshInstance3D]=[]
var fighting:=false
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
    var droplet_mesh:=SphereMesh.new()
    droplet_mesh.radius=0.065
    droplet_mesh.height=0.13
    droplet_mesh.radial_segments=6
    droplet_mesh.rings=3
    var water:=StandardMaterial3D.new()
    water.albedo_color=Color(0.67,0.85,0.96,0.7)
    water.transparency=BaseMaterial3D.TRANSPARENCY_ALPHA
    water.roughness=0.22
    for i in range(24):
        var drop:=MeshInstance3D.new()
        drop.mesh=droplet_mesh
        drop.material_override=water
        drop.cast_shadow=GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
        drop.hide()
        add_child(drop)
        spray.append(drop)
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
    fighting=bool(record.get("fighting",false))
    for drop in spray:drop.visible=fighting
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
    if fighting:
        # Lightweight stylized suppression spray, not physical hydraulics.
        for i in range(spray.size()):
            var f:=fposmod(elapsed*1.8+float(i)/spray.size(),1.0)
            var side:=1.0 if i%2==0 else -1.0
            spray[i].position=Vector3(side*4.5*(1.0-f),1.0+sin(f*PI)*1.8,side*0.8*(1.0-f)+sin(i*2.1)*0.10)
            spray[i].scale=Vector3.ONE*(0.8+f*0.6)
    light.position=Vector3(0,1.4,0)
    light.light_energy=intensity*(2.3+0.35*sin(elapsed*11.0))
    light.visible=intensity>0.02
