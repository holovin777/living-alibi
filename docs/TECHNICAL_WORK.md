# Physical technical work — first real engineering tasks (v1.16 draft)

## Not merely a salary button

A visitor still arrives with 50 EUR and **no chosen profession**. They can
freely become a market assistant, study at school, find a workshop job, or
train for engineering. Neither a detective role nor a technical role is
assigned automatically.

A hired **technician or junior engineer must physically finish a specific
work order before receiving their day's technical wages**. Work orders are
shared persistent town assets, not client-local tasks and not unlimited
money factories. The shop assistant and private investigator currently
use their earlier simplified employment rules.

## Two physical city problems

| Work order ID | Location / job | Initial condition | Repair ingredients | Result |
|---|---|---:|---|---|
| `water-pump` | Workshop / technician | 45/100 | 2 spare parts | Pump restored to 100/100 |
| `street-lights` | Office / junior engineer | 50/100 | 1 steel + 1 copper | Public lighting upgraded to 90/100 |

The current city begins with **6 spare parts, 4 steel and 3 copper units**.
Those are finite and persist in SQLite. They are not currently produced by
mines, factories, player crafting or NPC logistics.

Every task has stages:

1. **Open** — a public problem exists at a real location. Go there, find
   employment and earn the necessary skill.
2. **Inspected** — spend 30 game minutes diagnosing the problem. Inspection
   identifies the physical components needed.
3. **Completed** — spend 90 game minutes executing the repair or engineering
   implementation. Only then are the components consumed, the public
   infrastructure's condition improved, and a daily technical-work credit
   awarded.
4. **Paid shift** — while still at the workplace, complete a paid two-hour
   shift. The company pays an existing bounded salary account rather than
   inventing new currency.

A worker must be physically present, employed in that specific role, and
qualified (engineering skill 3 for the technician, 8 for the engineer).
The work must finish during 08:00–18:00. Only one technical completion and
one paid shift are allowed per person per day. Inspecting the same problem
twice or repairing without inspection cannot mint pay, duplicate components,
or secretly improve infrastructure.

Each midnight introduces small equipment wear. When a previously completed
asset falls to 65/100 or less, a fresh problem reopens. Finite material
stock still applies; the town will eventually need a real production
or supply chain to support repeated repairs. The first release is an
explicitly bounded proof of real jobs, not a finished industrial economy.

## Console

```text
jobs
go school
study engineering
study engineering
study engineering
go workshop
apply technician
work-orders
diagnose water-pump
repair water-pump
work
```

This is an illustrative command sequence. School and workshop hours matter,
and the game clock advances for every journey, lesson, inspection,
repair and paid shift. The player can pursue another path instead.

## Godot 4 and Spring Boot

`GET /api/v1/work-orders` lists actual town tasks with ID, job, location,
phase, equipment condition, completion day and required material types.
`POST /api/v1/actions/diagnose` and
`POST /api/v1/actions/repair` accept `{"orderId":"water-pump"}` and
return the authoritative updated world snapshot or HTTP 400 for invalid
actions. A new local Godot kiosk can show diagnosis, repair and engineering
tasks. Godot cannot grant a fake completion or money by modifying UI state.

SQLite persists `technical_orders`, `technical_materials`, and the
player's `last_technical_day` with the same transaction as NPCs,
resources, player work qualifications and money. The next visitor after
death inherits none of the former worker's skills or work credit, but
city pump condition, material stock and employer funds remain intact.

Automated checks:

- `TechnicalWorkSmokeTest`: qualification, inspected state, material
  consumption, distinct technician/engineer tasks, duplicate rejection,
  infrastructure wear and employer payroll.
- `TechnicalWorkDatabaseSmokeTest`: no-save startup, half-completed
  diagnosis, paid repair, multiple database restarts and successor isolation.
- Spring `GameSessionTest`: school → workshop → diagnose → repair →
  receive a technical salary and reload.
- Java CI checks public work order listing and HTTP rejection of a remote
  unauthorized diagnosis. Godot scene CI checks contextual action buttons.

## Effects on residents and the public

The park pump and lighting no longer exist only as repair scores. A repaired
pump allows the shared public fountain to supply clean water using finite
river-accessible stock; people at the park can refill their carried water
from the same bounded tank used by the player. Low-condition lighting adds a
small night fatigue penalty for people outside in public streets; a repaired
lighting system removes the extra penalty. Clean-water stock and its last
pump day survive SQLite restarts. See [City Utilities](CITY_UTILITIES.md).

## Current limitations

Workshops and offices use **placeholder 3D kiosks**, not physical tools,
machines with component-level simulation, player-controlled CAD or full
engineering drawings. Machinery wear is a prototype game rule. Tasks
currently affect infrastructure condition and material stocks, but that
condition does not yet control citizens' tap water or street lighting,
reliability-based utility bills, construction projects, workplace accidents,
contracts or resource extraction. Those effects must be introduced before
we call this a complete industrial simulation.
