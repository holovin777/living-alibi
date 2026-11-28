extends RefCounted
## Planar bicycle-model handling for the local 48-metre district.
var speed:=0.0
var yaw:=0.0
var steering:=0.0
var pitch:=0.0
var roll:=0.0

func step(delta: float,gas: float,turn: float,brake: bool,handbrake: bool,grip: float,condition: int) -> Vector3:
    var dt:=clampf(delta,0.0,0.1)
    var before:=speed
    var power:=lerpf(0.45,1.0,clampf(float(condition)/100.0,0.0,1.0))
    if brake or handbrake:
        speed=move_toward(speed,0.0,dt*9.0*grip*(1.4 if handbrake else 1.0))
    elif gas>0:
        speed=move_toward(speed,0.0 if speed<0 else 11.0*power,dt*(8.0*grip if speed<0 else 3.8*power))
    elif gas<0:
        speed=move_toward(speed,0.0 if speed>0 else -3.5,dt*(8.0*grip if speed>0 else 2.4*power))
    else:
        speed=move_toward(speed,0.0,dt*1.2)
    var limit:=lerpf(0.55,0.19,clampf(absf(speed)/11.0,0.0,1.0))
    steering=move_toward(steering,clampf(turn,-1.0,1.0)*limit,dt*2.8)
    yaw=wrapf(yaw-speed/2.55*tan(steering)*dt,-PI,PI)
    pitch=lerpf(pitch,clampf((before-speed)/maxf(dt,0.001)*0.008,-0.065,0.065),minf(1.0,dt*7.0))
    roll=lerpf(roll,-steering*clampf(absf(speed)/6.0,0.0,1.0)*0.09,minf(1.0,dt*6.0))
    return Vector3(-sin(yaw),0,-cos(yaw))*speed*dt

func stop() -> void:
    speed=0.0
