# Living Alibi: finite industrial economy (v1.16 draft)

The world starts with a visitor in the Park, **50 EUR and no assigned
profession**. Mining, logistics and fabrication are normal activities
undertaken by real NPCs. The player can choose to take part but is never
required to become an engineer or detective.

## Material flow, labor, time and transport

```text
Quarry: 80 iron ore + 40 copper ore in initial mineral seams
    08:45 — once the visible deposit is exhausted, an employed miner
             can commission a 30 EUR survey of one hidden western seam
    09:00 — employed miner physically present extracts up to 2 iron + 1 copper
    11:00 — miner + transport driver load a truck (capacity: 3 ore)
             factory pays quarry EUR 3 per ore
    11:00–11:30 — cargo is explicitly IN TRANSIT, not at the factory
    11:30 — employed factory machinist receives truck (if present)
    12:00 — employed factory machinist refines ore into steel and copper
    13:00 — machinist recycles 2 existing scrap units into 1 steel unit
    14:00 — machinist fabricates 2 spare parts from 1 steel unit
    15:00 — machinist delivers up to 2 parts, 1 steel, 1 copper
             city maintenance account pays factory for actual delivered items
    Repair workshop receives bounded materials used for pump/lights tasks
```

The second **western deposit physically exists from first world creation**,
but it is initially unknown and unavailable. It contains **30 finite iron
and 15 finite copper units**. Surveying it marks it as discovered and
transfers EUR 30 from the quarry to the geological survey office. It is
not the same as restoring ore in an exhausted deposit. Automatic NPC
discovery needs a working quarry miner, an exhausted initial deposit and
a solvent company.

The factory additionally starts with **10 units of old municipal metal
scrap**. Recycling permanently consumes two scrap units to recover one
steel unit. This loss is deliberate: neither mining, production nor
recycling is an infinite-resource exploit. Neither the deposits nor the
scrap pile regenerates on a timer. Actual material quantities remain
nonnegative throughout the chain.

These are simplified **game units**, not engineering-grade metal yields.
The truck has finite cargo capacity and a half-hour delay, but not yet a
physical, animated vehicle or navigable freight road. A missing driver
prevents dispatch; a missing factory worker leaves existing cargo on the
truck for a later arrival opportunity.

## Enterprise money and worker consequences

Corporate balances are saved in the same `career_accounts` ledger used
by existing player careers. Initial accounts include:

| Account | Initial EUR | Role |
|---|---:|---|
| Quarry (`miner`) | 360 | Pays miners and transport drivers |
| Factory (`machinist`) | 560 | Pays factory workers |
| City maintenance | 500 | Purchases delivered steel/copper/parts |
| Survey office | 0 | Receives paid geological surveys |

Actual working industrial NPCs earn **EUR 11 per hour from their firm's
own account**, not from an unlimited money source. Freight purchases
transfer cash **factory → quarry**. Delivered maintenance supplies move
cash **city → factory**. No account goes negative, and repeated deliveries
or player work cannot print extra wages. These accounts also fund the
player's industrial shifts.

If an enterprise cannot pay even one hourly wage, its NPCs are made
redundant the next morning. After a short, modeled unemployment period,
former industrial workers can seek a different local occupation. The
business can consequently halt for lack of funding or workers. A solvent
employer, labor availability, and real raw material are all needed for
productive shifts. The existing nonindustrial NPC economy still uses
placeholder hourly wages, so **this is not yet a complete closed economy**.

## What the player can do

At the Quarry, choose the `miner` job; at the Factory, choose
`machinist`. Neither needs engineering study as a prerequisite. Jobs
require being physically at the matching public district, an employer
with sufficient payroll, working hours and a two-hour shift. There is
only **one paid player shift per in-game day**.

- A miner shift **consumes one iron and one copper ore unit**, when
  available, and adds real ore to the quarry's shared warehouse; the
  wage is **EUR 30 from quarry funds**.
- A machinist shift **refines up to one iron and one copper ore** already
  at the factory. If there is no ore but at least two scrap units, the
  shift recycles them into steel. Pay: **EUR 34 from factory funds**.
- A hired miner can separately order a **two-hour geological survey**
  at the Quarry for EUR 30 from the quarry fund. The one hidden western
  seam is discoverable exactly once.

If raw material or funds are missing, the action is rejected without
a speculative wage or phantom inventory. Work is not a button that
creates free materials.

Console commands: `jobs`, `go quarry`, `apply miner`, `work`,
`prospect`, `go factory`, `apply machinist` and `industry`.
The Spring API uses `GET /api/v1/jobs`,
`POST /api/v1/actions/apply`, `POST /api/v1/actions/work`,
`POST /api/v1/actions/prospect`, and `GET /api/v1/industry`.

The Godot 4 preview offers Quarry and Factory districts with an **F**
kiosk for job application, optional paid surveying and industrial shifts.
The global **Industry** button shows finite ore, real truck cargo, scrap,
company accounts, maintenance supplies and working NPC counts.
Movement and kiosks remain provisional 3D placeholders; Java owns
the actual district permissions and economic rules.

## Database and first launch

SQLite `industry_stocks` tracks surface deposits, hidden pockets,
survey discovery, quarry inventories, truck freight, factory raw ore,
finished metal, spare parts and scrap. `industry_schedule` preserves
last dispatch, arrival, recycling, refining and delivery days. Both are
saved **atomically with the whole town, player money, company budgets and
citizens**. Existing world files migrate to the two added scheduling
columns rather than being reset. A deceased visitor's successor inherits
the same geological depletion, truck manifests, business accounts
and previously discovered mineral pockets.

A completely **empty database works on first launch**. Run
`bash play.sh` from the source root on Linux with the documented
Java, Maven, curl and Godot dependencies.

## Tests

- `IndustrySmokeTest`: no mining without miner, no freight without
  driver, bounded transport, money-preserving invoices, source
  conservation, one-time geological survey and recycling loss.
- `IndustryDatabaseSmokeTest`: a new database, normal stock movement,
  industrial salary balances, saved work history, no duplicate shipment.
- `IndustryTransportDatabaseSmokeTest`: stop with real ore **on the
  truck**, reload, unload later, recycle, deliver, survey, reload again
  and start a second visitor life without resetting business accounts.
- `IndustryEconomySmokeTest`: productive **player** mining and machining,
  strict salary transfers, employer bankruptcy and no double shift.
- `IndustryEmploymentSmokeTest`: broke companies lay off industrial
  NPCs; residents can seek other employment rather than collect phantom wages.
- `IndustryIntegrationSmokeTest`: multiple autonomous game days
  with worker commuting and actual finished-product deliveries.
- Spring Boot `GameSessionTest` and the clean-world HTTP smoke test
  verify actual persisted jobs and industrial read models.
- Godot headless CI checks new Quarry/Factory scenes, job kiosks and
  industrial dashboard.

## Still missing

Real trucks with driver coordinates, multiple transport routes,
energy/fuel costs, workplace safety, enterprise ownership, commercial
customer demand, procurement contracts, taxation, reinvestment, trade,
recycling from individual replaced machine parts, ore search on a real
terrain map and a wholly closed financial economy. Industrial jobs
currently use abstract timed actions, not complex machinery controls.
Each is a future extension, not a feature promised by this milestone.
