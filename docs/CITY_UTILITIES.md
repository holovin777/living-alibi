# Living public utilities — water and light (v1.16 prototype)

## Why maintenance changes the town

City infrastructure is shared across player generations and NPCs. Its condition
is already part of the existing paid technical career work: `water-pump`
at the Workshop and `street-lights` at the Office. This step makes their
condition matter for what residents can do, not merely for an engineering
score in SQLite.

### Park water fountain

- The public park fountain only operates when `water-pump` condition is **60/100
  or higher**. A new city starts with a broken pump (45/100), so the fountain
  is not immediately available.
- Completing the technical pump repair raises its condition to 100/100,
  uses **2 spare parts**, and can immediately transfer up to **6 units**
  from existing, finite river-accessible stock into the public clean-water
  reservoir. This *game abstraction* assumes municipal filtration: it is
  not instructions for treating actual river water.
- The clean-water tank is capped at **24 units**. On each later game-day
  boundary, a working pump can move at most **4 additional units** from the
  same finite river supply into the tank. If river stock is low or the pump
  is broken, water is not created or refilled.
- Generated residents with low drinking-water inventory now make an
  **autonomous destination choice**. When the repaired fountain has stocked
  water and they do not need food, they travel to the Park to refill; when
  it is broken or empty, those who can afford it go to the Market instead.
  This changes their actual journey, water stock and personal spending,
  rather than merely displaying the pump condition.
- Each refill consumes one common water unit and creates a witnessable
  public event. If the reservoir empties, residents resume their market
  route instead of pretending free water exists. This first model does
  **not** simulate district-wide household pipe delivery.
- Players may use `drink park-fountain` in the Park (5 minutes). This
  reduces thirst by 35, consumes one unit from the same limited tank,
  and fails if the pump is broken, the tank is empty, or the player is
  elsewhere. The unsafe puddle remains a separate optional water source.

### Street lighting and fatigue

- The street-lighting system starts at **50/100** and requires engineering
  work using **steel and copper** to improve to 90/100.
- Below **70/100**, citizens and the player outside at public urban places
  receive a small extra fatigue penalty on hourly ticks between **20:00
  and 06:00**. Residents safely inside their private homes do **not**
  receive this exposure penalty. The woods remain outside the scope of
  the city's urban lamps.
- A second, observable behavior is evening destination choice: from 18:00
  to 20:00, generated residents who would otherwise enjoy the Park prefer
  a covered Cafe when the street lights are faulty. Elena makes a similar
  choice. When the engineer fixes the street lights, their preferred park
  visit resumes. This is a simplified evening safety rule, not a detailed
  solar/daylight model.
- Once the engineer completes the upgrade, the additional urban night
  fatigue penalty disappears until ongoing infrastructure wear again
  reduces lighting condition. This is intentionally a mild, fictional
  gameplay effect, not a claim that lighting predicts real health outcomes.

### Persistence and fair simulation

`municipal_services` is a small additive SQLite table containing the tank's
clean-water count and the last day when it was pumped. Repairs, extraction,
NPC refills, city equipment condition and player actions are committed
atomically with the rest of the town. Reopening a save on midnight never
duplicates water; dying and starting a new visitor never resets shared
public infrastructure.

The console provides `utilities` and `drink park-fountain`.
Spring `GET /api/v1/world` includes a `services` object, and
`GET /api/v1/utilities` returns the same status. The existing
`POST /api/v1/actions/drink` with
`{"sourceId":"park-fountain"}` uses exactly the same simulation logic.
Godot's HUD shows the public services and its Park kiosk offers the
public fountain as a voluntary alternative to a puddle.

### Tests and boundaries

- `UtilitySmokeTest` checks initial outages, repairs restoring the fountain,
  resident bottle refill events, player use of finite water, river-stock
  conservation, exactly-once daily transfers, and extra nighttime fatigue
  disappearing after a lighting upgrade.
- `UtilityBehaviorSmokeTest` proves that working public water changes an
  NPC's destination from market to park, avoids the market expense, and
  actually consumes finite fountain water; it also verifies NPC leisure
  choices with repaired or broken lighting.
- `UtilityDatabaseSmokeTest` starts without a database, reopens several
  times, checks extraction is not duplicated at midnight, and verifies
  a successor after death inherits the same shared municipal water stock.
- Spring session tests cover real API drinking, infrastructure status,
  and persistence, while HTTP CI verifies a new city refuses use of a
  broken fountain.

**Not yet implemented:** a true filtration plant, pipelines, separate
household water taps, street lamps on a continuous 3D street map, nighttime
crime rates, real municipal finances, construction contracts, NPC
technicians autonomously repairing faults, river treatment costs, or
supply logistics. The goal is to connect the first two infrastructure
repairs to believable, bounded consequences without pretending the entire
town utility network has been simulated.
