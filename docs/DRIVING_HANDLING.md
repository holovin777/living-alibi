# Driving handling
Classic play.sh client: separate heavy and car tuning, condition-dependent
engine performance, wet braking grip, speed-dependent steering, lateral
inertia, bounded wider lane, brake-to-reverse, Shift handbrake, subtle
cockpit pitch and roll. Existing collision checks and Java travel remain.
Geographic client: server-owned signed reverse speed and handbrake,
reverse distance consumes the same fuel/wear budget and cannot cross the
start of the current road edge. Speed display uses absolute speed and R
gear. No wheel suspension solver, unrestricted steering, actual off-road
movement, deformation or new vehicle assets in this change.
