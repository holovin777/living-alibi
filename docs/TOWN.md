# Living Alibi — Town Simulation (v1.1)

## Why locations matter

Every investigation needs an authoritative timeline and a restricted set of things each witness could have observed. Simply giving an NPC the full simulation event log would destroy the detective premise.

### World truth

`town_events` records events created by the deterministic Java engine:

- A resident arrives at a location after a fixed in-transit interval.
- Elena eats, drinks, purchases food/water, and receives hourly income under simplified rules.
- Marco and Elena meet when both are present at the same location at the same simulation time.

### Private observations

`town_observations` records events **only for residents physically present at that location at the event time** (including the actor, while alive and present).

Example: if Elena eats at the market and Marco is at the bakery, Elena can remember the meal, but Marco does not automatically know it happened.

An observation is a witnessed event log, **not a claim that the witness comprehended every detail or remembers it perfectly**. Later, beliefs can diverge from world truth.

## Current town

| Place | Use |
| --- | --- |
| Marco's home | Home, rest, sleep |
| Elena's home | Elena's home and rest |
| Bakery | Marco's workplace |
| Market | Elena's workplace and supplies |
| Park | Walking, afternoon activity |
| Cafe | Socializing |

Travel uses an approximate eight- or ten-minute delay. There is no spatial collision model or road network.

## NPCs

**Marco Bianchi:** bakery employee, simulated via the v1.0 `LifeSimulation` rules and saved through `world_state`. His town position is a new layer that responds to long-lasting activities such as work, leisure and sleep.

**Elena Rossi:** shopkeeper, currently a lighter self-directed agent with her own food/water, hunger/thirst, fatigue, money, health, age and location. Her work/leisure schedule reacts to day and time; she purchases supplies and earns income when physically at the market.

## Current detective-specific compromises

The game still contains instantaneous, legacy Marco shopping actions. A future action model must require **arrival at a location before actions can be executed** and reserve time for actions such as buying, speaking, or picking up an object. Until then, developer logs should not be treated as a forensic-grade history.

## Next milestone

Refactor activities into multi-minute plans with location and resource preconditions; make all NPCs use a shared simulation interface; introduce objects/evidence. Then add controlled interviews in which no witness receives world truth they did not learn.
