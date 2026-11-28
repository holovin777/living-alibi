# Free driving prototype
E or Enter car in the classic client begins local free steering instead of
requiring a destination. A planar bicycle model supports forward/reverse,
braking and steering. Car footprint samples check scenery, traffic and
pedestrians. Near a connected motor road edge, the existing authoritative
drive action confirms the next district and applies its ordinary fuel,
wear and travel costs. The client rebases the car to the receiving road.
E/F/Esc exits a stopped car; local parked position and heading are kept
for this session, not saved in SQLite. After restart the vehicle appears
at its district parking lot. Local cruising does not yet charge fuel or
damage, and the model is not a per-wheel physics simulation. Freight and
explicit route selection retain the existing lane-driving mode.
