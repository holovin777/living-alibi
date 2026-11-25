# Starting with nothing to prove — careers and education (v1.16 draft)

## Player fantasy

A new player arrives in the **public park**, carrying **50 EUR** of personal
pocket money. They are **not a detective, engineer, officer, employee, or
automatic investigator**. They have no assigned quests, no debts, no
qualification or professional authority, and no knowledge of NPCs. They may
wander, get to know people, find work, save money, pay for study, or
eventually change careers. Detective cases remain optional.

The default conditions are **game-balancing defaults**, not statements about
a real person. New visitor generations start again with 50 EUR, an
independent identity, no inherited job or qualifications; NPC city history
and company accounts remain intact.

## Industrial careers

Players can now voluntarily join a quarry as a **miner** (no prior skills,
EUR 30 per productive two-hour shift) or a parts factory as a
**machinist** (no prior skills, EUR 34 per productive shift).
The quarry consumes real finite mineral deposits; the factory
refines stockpiled ore or recycles limited historical scrap.
Both roles are paid directly from the same finite enterprise
accounts as industrial NPC workers. If the company cannot afford a
shift or usable materials are unavailable, no pay is created.
A hired miner may commission a two-hour, EUR 30 geological survey
of one undiscovered western ore seam. See [Industry](INDUSTRY.md).

## First playable opportunities

| Job ID | Workplace | Required subject / skill | Wages per two-hour shift |
|---|---|---|---:|
| `shop-assistant` | Market | None, 0 | 24 EUR |
| `technician` | Workshop | Engineering, 3 | 38 EUR |
| `engineer` | Office | Engineering, 8 | 62 EUR |
| `investigator` | Office | Investigation, 8 | 54 EUR |
| `miner` | Quarry | None, 0 | 30 EUR |
| `machinist` | Factory | None, 0 | 34 EUR |

Jobs are voluntary. `jobs` shows the public vacancy board, `apply` only
works **in person at the specified workplace**, and `work` requires
remaining employed there. Working occupies 120 game minutes, with
08:00–18:00 business hours and **only one paid shift per world day**.
**Workshop technicians and junior engineers must first diagnose and complete
an actual city work order**; merely having a job title no longer produces
their salary. See [Technical Work](TECHNICAL_WORK.md).
You can change professions when qualified; certificates/skills remain
with that visitor. The `investigator` job is private investigation,
**not an automatic police badge or permission to read secret evidence**.

### Study and promotion

`study engineering` is available at the **School**.
`study investigation` is available at the **Library**.
Each lesson takes 60 game minutes, costs 10 EUR and increases one skill
by 1 point. Lessons take place 08:00–18:00. Courses are priced and
limited by available personal cash; money goes into the school/library
account. Higher professions require investment, not instant magic
promotions.

### Finite payrolls

Employers begin with bounded salary accounts, saved in SQLite:

- market assistant employer: 480 EUR
- workshop technician employer: 760 EUR
- junior engineering employer: 1240 EUR
- private investigation employer: 1080 EUR
- school and library: 200 EUR each

A paid shift **transfers** money from its employer's account to the
player's pocket. Tuition **transfers** money from the player to the
institution. When an employer cannot fund a shift, it cannot pay wages.
These accounts are intentionally **provisional** until actual businesses,
products, sales, taxes and institutional funding become a shared economy.
They do not replenish invisibly each day. This slice does not yet unify
player earnings with the older generated-NPC economy.

## Example starting route

```text
me
career
jobs
go market
apply shop-assistant
wait 5
work
go school
study engineering
career
```

The example assumes a completely fresh world starting at 07:30. It is
only an optional path: `explore`, `go river`, `look`, or other
activities remain valid without accepting any employment.

## Spring API and Godot

The authoritative API returns `player.money`, `player.jobId`,
`player.engineeringSkill`, and `player.investigationSkill`.

- `GET /api/v1/jobs` lists vacancies, prerequisites and payroll.
- `POST /api/v1/actions/apply` with `{"jobId":"shop-assistant"}`.
- `POST /api/v1/actions/study` with `{"subject":"engineering"}`.
- `GET /api/v1/work-orders` lists real broken equipment and city upgrade tasks.
- `POST /api/v1/actions/diagnose` and `POST /api/v1/actions/repair`
  accept `{"orderId":"water-pump"}`, validated by the Java engine.
- `POST /api/v1/actions/work` with `{}`.

All state modifications use the **same career rules** as the Java console,
advance the authoritative world clock, persist the player and company
accounts together, and return the updated world. A Godot 4 `HTTPRequest`
example is available in `godot/examples/WorldApi.gd`.

## Verified invariants / gaps

`CareerSmokeTest` verifies that no job is assigned on spawn,
application requires being present, higher jobs require study, wages
do not appear from nothing, institutions receive tuition, and newly
born visitors do not inherit their predecessors' jobs.
`CareerDatabaseSmokeTest` and Spring `GameSessionTest` verify first
launch, SQLite persistence, payroll, and restarts.

Still missing: resumes and interviews, employer vacancies depending on
business demand, real university programs, certifications, licensing,
worker contracts/rights, taxation, detailed skill use in mechanical tasks,
a police/detective career, advanced interactive engineering designs,
and many competing owner-operated enterprises. A **first player-owned
fabrication company with real wage and procurement transfers** now exists;
see [Player Business](PLAYER_BUSINESS.md). The first physical workshop repair
and public-lighting upgrade do now exist as shared, finite-material
work orders rather than automatic pay. None of those are represented as
implemented.
