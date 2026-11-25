extends RefCounted
## Local lane-driving preview. Server still owns fuel, travel and parking.
const START := 14.2
const FINISH := 23.8
var distance := START
var lateral := 1.6
var speed := 0.0
var steering := 0.0

func propose(delta: float,throttle: float,turn: float,brake: bool,phase: int) -> Vector2:
    var dt := clampf(delta,0.0,0.1)
    steering = move_toward(steering,clampf(turn,-1.0,1.0),dt*4.0)
    if brake:
        speed = move_toward(speed,0.0,dt*12.0)
    elif absf(throttle)>0.01:
        speed = move_toward(speed,6.0 if throttle>0 else -2.0,dt*3.0)
    else:
        speed = move_toward(speed,0.0,dt*1.8)
    if phase==-1:
        speed = clampf(speed,-2.0,2.0)
    var next := clampf(distance+speed*dt,START,FINISH)
    if phase in [0,1] and distance<=15.0 and next>15.0:
        next = 15.0
        speed = 0.0
    if next==START and speed<0:
        speed = 0.0
    return Vector2(next,clampf(lateral+steering*speed*dt*0.20,1.05,1.85))

func accept(next: Vector2) -> void:
    distance = next.x
    lateral = next.y

func blocked() -> void:
    speed = 0.0
