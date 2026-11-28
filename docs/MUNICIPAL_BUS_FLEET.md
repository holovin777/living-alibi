# Municipal bus fleet
Up to two buses are purchased with municipal cash. Each has a persistent
identity, condition, current district, route endpoints and trip clock.
The city transfers EUR 60 purchase and EUR 30 working capital; low fleet
cash may receive EUR 20 support without crossing the municipal reserve.
Drivers, dispatcher and mechanic are staffed roles. Every departure
requires on-duty employees, finite fuel and funds; a leg is scheduled
every two hours from 08:00 through 18:00. A bus stays at its destination
until the next leg, then returns to the station. New district stops may
change the next outbound destination. Maintenance at the station requires
a real mechanic, spare part and payment.

During the departure minute or the next minute, matching passengers can
board a fleet bus (12 seats). Paid fares go into the fleet account.
Existing abstract bus journeys remain as fallback, not the same fleet
service. Fleet passengers do not generate duplicate passenger-based
vehicle models. Godot projects buses on the existing motor route graph,
including stopped buses after their first leg. Driver/dispatcher duties
are economically modeled at the depot; no playable vehicle interior,
physical boarding or new road graph is added. Existing routes with
missing mapped motor paths may lack a 3D bus.
