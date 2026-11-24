# Natural resources and working industries — v1.16 prototype

## Why a city has industry

The player enters the public Park with **50 EUR, no profession and no
mandatory mission**. Meanwhile the city has its own workers and limited
natural materials. Engineers and technicians repairing pumps or public
lighting no longer depend only on supplies that existed on the first day.

The Java simulation is authoritative for quarry geology, NPC work attendance,
cargo, factory stock and repair materials; Godot displays these outcomes.

## First production chain

```text
Finite underground deposits — Quarry
    80 iron-ore units + 40 copper-ore units initially
        | Mining, 09:00 (Miner on site)
        v
Quarry inventory (2 iron + 1 copper per staffed working day)
        | Cargo, 11:00 (Miner + machinist on site)
        v
Factory raw-ore inventory
        | Refining, 12:00 (Factory machinist on site)
        v
Steel (2 units) + refined copper (1 unit)
        | Fabrication, 14:00 (1 steel -> 2 spare parts)
        v
Factory products
        | Dispatch, 15:00 (Factory machinist on site)
        v
City technical-material warehouse
        1 steel + 1 copper + 2 spare parts per full batch
        |
        +--> Technician repairs Park water pump
        +--> Engineer modernizes public street lighting
```

Quantities are simplified **fictional game units**, not real metallurgical
yields. Iron is accounted for as steel, while each fabricated spare-part
unit represents half a steel unit. Other ore remains in the factory/quarry
until a later production run. **Nothing is regenerated in the geological
seam**; once deposits are depleted, work must eventually rely on trade,
recycling or a new deposit (not implemented).

## People actually have to work

A newly created Spring town generates twelve additional residents, including
one **Miner** (at the Quarry) and one **Factory machinist** (at the Factory).
They have the existing independent NPC job schedule, home, walking commute,
sleep, needs, health, illness, job loss and mortality. Production does not
run just because a calendar day passes: the appropriate NPC must actually
be **alive, employed, on site and working** when the operation is scheduled.

The shipment stage requires both a quarry worker and factory worker present;
the later stages need the machinist. On holidays, during sick leave, when
unemployed, while traveling, or when a deposit is empty, production can stop.
These setbacks are a normal part of the living town, not a broken save.

The mining and factory jobs are initially **NPC occupations**.
Player employment inside those two enterprises, business ownership,
enterprise payrolls, and a full market for their products are future
features. Generated workers continue to earn wages using the older NPC
employment rules; their wages are **not yet booked against factory revenue**.
Do not treat this as a complete conservation-of-money economy.

## Persistence and truth

The SQLite tables `industry_stocks` and `industry_schedule` contain
the distinct deposit, quarry, factory inventories and last processed day
for each stage. They are stored **in the same SQLite transaction** as
workshop materials, company accounts, player state, citizens and town events.

Daily timestamps make each operation idempotent after a save/reload at
the same simulated minute. A new visitor after the previous player's
death does not reset the mine or factory. Depleted rock remains depleted;
completed shipments are not doubled.

Public operations use `TownLife.publish` with a real working resident
as the actor. Other characters can observe those events only according to
the existing location and witness rules; workers' private health details
aren't exposed as an industrial log.

## Player view

The console command `industry` shows current mineral reserves, cargo,
factory products, workshop stock and workers on duty. `locations` includes
`quarry` and `factory`; there is no assigned career requirement to visit
the public parts of those districts.

Spring Boot `GET /api/v1/industry` returns a public summary of the same
authoritative inventory. The Godot preview offers an **Industry** button
and placeholder 3D Quarry and Factory districts. Manufacturing is performed
by autonomous NPCs in the Java engine, not by fake client-side animation.

## Test coverage

- `IndustrySmokeTest`: staffed and unstaffed production, finite geology,
  daily idempotence, ore transport, refining, component fabrication,
  physical warehouse delivery, and balanced iron/copper equivalents.
- `IndustryDatabaseSmokeTest`: genuinely empty database startup, extracted
  ore saved at mid-production, restart/resume without duplication,
  shipment persistence, and a new player life in the same town.
- Spring `GameSessionTest`: finite inventory on clean startup, public
  Quarry/Factory destinations and committed inventory across server starts.
- Java CI calls the actual Spring HTTP industry endpoint on a brand-new
  world. Godot CI checks scene rendering and the supply dashboard.

## What's intentionally still missing

Mine exhaustion has no automatic rescue system; we need exploration,
recycling or import contracts. Factory operations currently use abstract
cargo transfers rather than a truck NPC with continuous coordinates.
There is no player manufacturing mini-game, contracts, energy consumption,
mine safety system, by-products, pollution, shipping cost, waste recovery,
variable yields, seasonal demand, supply bidding, factory ownership, nor
integrated wage accounting. These can be developed without replacing
the shared Java/Spring/Godot architecture.
