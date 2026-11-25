extends RefCounted
## Paces requests only; never owns the world clock or replays failed writes.
const SPEEDS := [0, 1, 5, 10, 24, 60]
var speed := 10
var elapsed := 0.0

func set_speed(value: int) -> void:
    if value in SPEEDS:
        speed = value
        elapsed = 0.0

func reset() -> void:
    elapsed = 0.0

func advance(delta: float, eligible: bool) -> bool:
    if not eligible or speed == 0:
        return false
    var interval := 60.0 / float(speed)
    elapsed += clampf(delta, 0.0, 1.0)
    if elapsed + 0.000001 < interval:
        return false
    # Never queue a backlog or a second concurrent mutation.
    elapsed = maxf(0.0, elapsed - interval)
    return true
