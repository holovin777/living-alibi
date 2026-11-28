extends RefCounted
## Bounded local driving. Java remains authoritative for travel and resources.
const START := 14.2
const FINISH := 23.8
var distance := START
var lateral := 1.6
var speed := 0.0
var steering := 0.0
var heading_offset := 0.0
var lateral_velocity := 0.0
var pitch := 0.0
var roll := 0.0
var acceleration := 3.0
var top_speed := 8.0
var reverse_speed := 3.0
var brake_force := 8.0
var grip := 1.0
var lane_min := 1.05
var lane_max := 1.85
var gear := "N"

func configure(heavy: bool, wet: bool, condition: int) -> void:
    grip=0.65 if wet else 1.0
    var health_factor:=lerpf(0.55,1.0,clampf(float(condition)/100.0,0.0,1.0))
    acceleration=(1.7 if heavy else 3.8)*health_factor
    top_speed=(6.0 if heavy else 10.0)*health_factor
    reverse_speed=2.0 if heavy else 3.5
    brake_force=5.0 if heavy else 9.0
    lane_min=0.95
    lane_max=2.45

func propose(delta: float,throttle: float,turn: float,brake: bool,phase: int,handbrake: bool=false) -> Vector2:
    var dt := clampf(delta,0.0,0.1)
    var initial:=speed
    var input:=clampf(turn,-1.0,1.0)
    var steering_limit:=lerpf(1.0,0.45,clampf(absf(speed)/top_speed,0.0,1.0))
    steering=move_toward(steering,input*steering_limit,dt*4.0)
    if handbrake or brake:
        speed=move_toward(speed,0.0,dt*brake_force*grip*(1.45 if handbrake else 1.0))
    elif throttle>0.01:
        speed=move_toward(speed,0.0 if speed<0.0 else top_speed,dt*(brake_force*grip if speed<0.0 else acceleration))
    elif throttle<-0.01:
        speed=move_toward(speed,0.0 if speed>0.0 else -reverse_speed,dt*(brake_force*grip if speed>0.0 else acceleration*0.65))
    else:
        speed=move_toward(speed,0.0,dt*1.2)
    if phase==-1:speed=clampf(speed,-2.0,2.0)
    var next:=clampf(distance+speed*dt,START,FINISH)
    if phase in [0,1] and distance<=15.0 and next>15.0:
        next=15.0
        speed=0.0
    if next==START and speed<0.0:speed=0.0
    var target_lateral:=steering*speed*0.26
    lateral_velocity=move_toward(lateral_velocity,target_lateral,dt*(2.5 if handbrake else 5.0)*grip)
    if absf(speed)<0.02:lateral_velocity=move_toward(lateral_velocity,0.0,dt*5.0)
    var sideways:=clampf(lateral+lateral_velocity*dt,lane_min,lane_max)
    if sideways==lane_min or sideways==lane_max:lateral_velocity=0.0
    heading_offset=move_toward(heading_offset,steering*clampf(speed/4.0,-1.0,1.0)*0.20,dt*2.0)
    pitch=lerpf(pitch,clampf((initial-speed)/maxf(dt,0.001)*0.008,-0.065,0.065),minf(dt*7.0,1.0))
    roll=lerpf(roll,-steering*clampf(absf(speed)/6.0,0.0,1.0)*0.055,minf(dt*6.0,1.0))
    gear="R" if speed<-0.05 else ("N" if speed<0.05 else "D")
    return Vector2(next,sideways)

func accept(next: Vector2) -> void:
    distance=next.x
    lateral=next.y

func blocked() -> void:
    speed=0.0
    lateral_velocity=0.0
    gear="N"
