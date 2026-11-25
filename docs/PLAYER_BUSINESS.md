# Own a small business — v1.16 workshop prototype

Living Alibi is not a detective-only game. Your visitor arrives anonymously
in the Park with **50 EUR**, no job, no assigned property and no quest.
You can earn a salary, study, or eventually **found an independent
fabrication shop** in the public Workshop district.

## Opening a real company

A single small fabrication workshop is available in this first prototype.
Its state belongs to the **town**, not the player's inventory, and it
uses a separate real company account.

1. **Go to Workshop** (Godot yellow travel gate or console `go workshop`).
2. **Register the business** for 15 EUR. This transfers money from your
   personal wallet into the city's maintenance budget. The registration
   does not come with free company money.
3. **Invest company capital** from your own savings using `business invest
   10`, `business invest 25`, or an arbitrary positive integer in the
   console or Spring API. Money moves between the two accounts; it is
   not created.
4. **Buy a fabrication machine** for 25 EUR from an actual
   `equipment-supplier` company account.
5. **Hire an available resident** who is physically at the Workshop.
   Each new 12-resident city includes a real mechanic with a normal
   morning Workshop commute, so there is an initial potential applicant
   without spawning an employee from nothing. Applicants may leave for
   other activities or become unavailable, so recruitment depends on
   meeting them during work hours. A contract pays an actual 10 EUR signing bonus from the business
   account to that citizen. To hire, the shop must also have enough
   cash to cover at least two hours of wages and a steel purchase
   (36 EUR in total before the bonus is paid).
6. The newly employed resident follows their own needs, illness and
   two-hour job shifts. They normally come to work from 11:00 and
   receive two hourly wages at 12:00 and 13:00, 11 EUR each. If they
   are absent, ill or the company cannot pay them, production stops.
7. **13:15:** A working, paid employee uses the owned machine (at least
   **20/100 condition**) and buys one physical steel unit from the city
   maintenance warehouse for 4 EUR, making two spare parts. Production
   wears the machine by **15 points per batch**. Below 20/100 it stops.
8. **13:45:** If the worker remains on site, and city maintenance can
   afford a purchase, the shop sells two parts to the municipality
   for 15 EUR each. Those parts move to the **same finite repair
   warehouse** that technicians use to repair public infrastructure.
   Company cash changes by actual sales and purchases. No imaginary
   components or wages.

If there are no parts, steel, buyers, workers, usable machinery or
corporate funds, the corresponding step does not take place. Business earnings are
**not** automatically added to the player's pocket. You can choose to
withdraw a bounded amount of your company's actual cash using
`business withdraw 10` or a custom amount at the Workshop, but
doing so may leave the firm unable to pay wages or buy supplies.

A worn machine can be serviced at the Workshop for **45 game minutes**,
spending **10 EUR of company money** and **two real municipal spare parts**.
This restores 60 condition points, up to 100/100. A perfect machine cannot
be serviced; an empty parts warehouse cannot supply repairs. Workers
prioritize paid shifts over routine shopping, but illness still disrupts
production. Hourly wage receipts record the exact paid minute, preventing
repeat payments if a world is reopened at the same clock time.

You cannot open a productive workshop instantly with only the
initial 50 EUR: the game encourages you to find employment first,
then build capital. Startup progress is optional, not a scripted quest.

## Local commands and API

- `business` — current registration, owner, machine, payroll, employee
  and finished parts; no travel required for this status.
- `business candidates` — list **actually nearby** eligible residents
  at the Workshop, with public IDs and current occupations.
- `business open` — register for 15 EUR (or purchase an abandoned
  prior-owner workshop after the former visitor's death).
- `business invest <EUR>` — add your personal savings to company cash.
- `business machine` — buy the company's actual production equipment.
- `business service` — repair worn machinery using two finite city spare
  parts and 10 EUR over 45 game minutes.
- `business hire <resident-id>` — offer a real co-present citizen a
  contract and pay a signing bonus.
- `business withdraw <EUR>` — move company cash back to your wallet.

All commands that change money or assets require you to be **alive
and physically present at the Workshop**.

Spring exposes `GET /api/v1/business`,
`GET /api/v1/business/candidates`,
`POST /api/v1/actions/business/open`,
`POST /api/v1/actions/business/invest` with `{"amount":25}`,
`POST /api/v1/actions/business/machine`,
`POST /api/v1/actions/business/service`,
`POST /api/v1/actions/business/hire` with
`{"residentId":"resident-0002"}`,
and `POST /api/v1/actions/business/withdraw` with `{"amount":10}`.

Godot has a **Business** dashboard button and contextual **F** kiosk
choices at the Workshop. Nearby applicants appear by name.
The Java server validates money, equipment, co-location and player
ownership; the UI cannot fake wages or sale proceeds.

## Ownership across lives

Business ownership is assigned to a specific **visitor incarnation**.
When the owner dies, their equipment, company cash, worker and
stock remain in the world. The next visitor starts without ownership,
skills or prior contacts. That character can pay a new 15 EUR
registration/takeover fee to acquire the old enterprise without
magically inheriting its assets. The first prototype supports
**one independent fabrication business** in the town. More competing
companies and legal inheritance are future tasks.

## Persistence, safety and tests

SQLite table `player_business` persists registration, owner incarnation,
cash, machine ownership **and condition**, employee ID, real finished
parts, last paid **clock minute**, production and sale day stamps. It is committed in the **same
transaction** as personal cash, city payrolls, NPC jobs and municipal
materials. Existing saves migrate additively to include the machine
condition and paid-minute receipt. Previously owned machines begin at
100/100 and existing paid hours are respected. Saves without any company
start with an empty registry; residents and history remain unchanged. The launcher `bash play.sh` remains unchanged.

- `BusinessSmokeTest` verifies separate wallets, supplier payment,
  co-present hiring, wages, component conservation, municipality
  purchase, duplicate sale prevention and founder succession.
- `BusinessDatabaseSmokeTest` starts with an empty database, saves
  mid-employment and mid-production, reloads business and worker state,
  checks no duplicate sales, and transfers ownership across generations.
- `BusinessIntegrationSmokeTest` proves a real NPC follows a workday,
  receives wages, manufactures parts and completes a municipal sale.
- `BusinessMachineSmokeTest` validates breakdown, finite repairs and no
  repeated hourly wages; `BusinessMachineDatabaseSmokeTest` verifies
  old-save migrations, restored machine status and wage replay protection.
- Spring `GameSessionTest` validates opening, investment, service costs,
  rejecting unaffordable contracts and restart persistence.
- Godot scene tests cover registration, investment, hiring and
  withdrawal UI states.

The revenue model is a fictional starter economy, **not a realistic
market-clearing financial simulation**. Municipal demand is a simple
fixed procurement rule. Business loans, custom product design,
shops at other locations, detailed equipment failure modes, company debts,
multiple competing owners, estate and inheritance law, labor contracts
with explicit NPC negotiations and full accounting need future work.
