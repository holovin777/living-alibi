# Living Alibi — first connected Godot town (v1.16 draft)

This folder is a **Godot 4.6.1** project with a primitive 3D prototype.
The Java engine is the authority for time, the player's location,
relationships, careers, resources and SQLite persistence. Godot does **not**
grant itself money, skills, employment, or instant travel by moving a mesh.

## Run the game preview from an entirely new world

On **Arch Linux**, install Java 21+, Maven 3.9+, curl and Godot 4 first.
From the repository root, on the `main` branch, run:

```bash
bash play.sh --check
bash play.sh
```

This **one-command launcher** builds the plain Java simulation and the
separate Spring Boot server, starts the local API, prepares Godot's import
cache on first launch, and opens the Godot window. Exit the game window to
stop the server. The world is automatically created at `data/world.db`
when no save exists; otherwise the same world resumes. The script **never
deletes a save**. A brand-new game starts in the park with 50 EUR, no
profession, Marco, Elena, and 28 additional NPCs by default (30 total).
Existing worlds are expanded once to the configured population target, preserving all existing residents and saves. The server property `living-alibi.population` accepts 12–40 generated residents. Natural deaths do not trigger automatic replenishment.

If you prefer manual debugging, run the server in one terminal:

```bash
bash play.sh --server-only
```

Then open the Godot 4 editor, import `godot/project.godot` and press F5.
The main scene is `scenes/park.tscn`, its script is `scripts/ParkDemo.gd`,
and HTTP calls are sent by `examples/WorldApi.gd`. Running without the
server leaves a visual preview only and shows a connection error.

The launcher saves logs in `data/server.log`. Don't run another world
writer (such as the console) against the same SQLite file at the same time.

## Camera and viewpoint

Living Alibi is **first-person only**. The camera stays at the visitor's
eye level, with mouse-look, in every district and transport mode. There
is no external camera, third-person player mesh or camera toggle.
Future manual vehicle controls must keep the first-person viewpoint.

Click the empty 3D scene to capture the mouse and look around.
Press **Esc** to release the cursor. Pressing **E** at the travel gate
or **F** at a local kiosk also releases the cursor whenever the
clickable action menu opens. WASD movement remains camera-relative.

## Controls

- **WASD / arrow keys:** walk relative to the camera inside the visible
  *local public district* (this is a visual preview, not per-meter
  networked gameplay).
- **Hold Shift:** run on foot (6.5 units/s instead of 4.5).
- **Hold Ctrl:** crouch and move more slowly (2.2 units/s); the first-person view lowers smoothly. Crouching takes priority over running, and jumping requires standing. The planar collision radius is unchanged: crouching does not open new passages. Children retain their existing movement-speed scaling.
- **Camera motion:** optional subtle head bob driven by actual distance walked, disabled while jumping or riding. Turn it off in the HUD if preferred.
- **Space:** jump while walking with the mouse captured. The eye-level camera follows a gravity-based arc; holding Space cannot repeatedly jump, and bicycles, cars, menus and district transitions prevent new jumps. Horizontal obstacle footprints remain solid even in midair. This is a first local jump, not yet height-aware obstacle traversal.
- **Walk to the yellow gate, press E:** select a public district. The
  list and travel minutes come from `GET /api/v1/locations`. Confirming
  a destination calls `POST /api/v1/actions/go`, which advances the real
  game clock, saves SQLite, and returns the authorized new location.
  Godot then renders the new public district and places the avatar near
  its center.
- **Walk to the blue kiosk, press F:** available local interactions.
  Market offers shop-assistant jobs; Workshop offers technician jobs;
  Office offers engineer or private investigator positions; School
  offers engineering lessons; Library offers investigation lessons.
  A qualified employee can **inspect** the workshop pump or an office
  lighting project, then **repair / implement** it with real stocked
  materials. Technical wages require one completed task that day.
  The parked visitor can drink puddle water, the river visitor can
  drink untreated river water, and the woods visitor can forage berries.
  All choices can fail if time, location, skill, money or resource
  requirements aren't met.
- **Park fountain:** the blue Park kiosk also has an optional source of
  drinkable public water. It only works after the city pump is repaired,
  and every drink consumes one unit from a finite shared tank. The HUD
  shows public water and street-lighting status, which affect residents
  and the visitor. See [City Utilities](../docs/CITY_UTILITIES.md).
- **Industry:** see the finite iron/copper seams, freight truck inventory,
  historical scrap, factory stock, maintenance warehouse, working NPC
  crews and three independent company/city budgets. The public Quarry
  and Factory districts have placeholder 3D machinery. Production proceeds
  only when the matching NPC workers are on site and the company has cash.
- **Quarry or Factory job:** walk to the blue kiosk (**F**) and apply to be
  a `miner` or `machinist`. Once employed, the paid two-hour shift
  extracts ore or refines/recycles actual factory materials. These shifts
  are limited by real stock and payroll. A hired miner may also survey
  one hidden western ore pocket using the kiosk's **Survey** action.
  See [Industry](../docs/INDUSTRY.md).
- **Business:** display your own Workshop company's real finances,
  owned machinery, paid employee and stocked parts. At the Workshop
  blue kiosk (**F**) you can register the company (15 EUR), invest
  10/25 EUR at a time, buy a machine (25 EUR), hire an actually nearby
  NPC for a paid contract, and withdraw actual funds. The business
  machine now wears down through real manufacturing; its condition
  is shown in the dashboard. The Workshop kiosk can service a worn
  machine with **two finite spare parts and 10 EUR of company money**.
  Production stops below 20/100 condition until repaired. See
  [Player Business](../docs/PLAYER_BUSINESS.md).
- **Jobs:** see current job offerings without applying.
- **Wait 60m:** advance 60 game minutes in the world.
- **Refresh:** recover the latest committed state from Java/SQLite.

Example optional life path: walk to the yellow gate and travel to
Market (10 game minutes from Park); walk to the blue kiosk and apply
for the shop-assistant job; work during business hours and earn money;
travel to School and study engineering, then to Workshop to apply
for a more technical role. Once qualified and employed, inspect the
`water-pump` at the workshop kiosk, repair it using two spare parts,
then collect an earned technical wage. The Java engine keeps the
infrastructure condition and material inventory in SQLite.

## Residents in the scene

The client reads the `residents` list from the Java `GET /api/v1/world`
snapshot. People who are *actually in the same public place* now appear as
simple 3D figures with their names above them. When the server reports
that a resident has left, their placeholder disappears on the next
snapshot. The HUD shows how many people are nearby.

The figures use **approximate display positions only** because the Java
world currently stores district-level positions, not real 3D coordinates.
There are no invented people. Articulated low-poly residents animate
locally during walking, free time, shopping and job searching. Visual
identities survive refreshes; exact NPC positions and activities within
a district are still not tracked by the Java simulation.

## Local movement and collisions

Trees, benches, lamp posts, fountain, kiosks, gate posts, building
exteriors, selected industrial equipment and the river have solid
ground-level footprints. Both the visitor and visual NPCs use the same
radius-based movement constraints; small substeps prevent walking
through obstacles and permit sliding along walls. The open gate remains
passable. NPCs start on a nearby unobstructed point and can slide or
stop at obstacles rather than walk through them.

This is a deterministic, lightweight planar collision system, not full
Godot rigid-body physics or pathfinding. It does not yet model stairs,
height-aware obstacle traversal, NPC-to-NPC collisions, interiors or continuous connected streets.
The Java engine still validates actual district transitions and actions.


## Town map — first step toward connected districts

Press **M** or the Town map button to open the schematic overview.
All 15 public districts are arranged on a shared grid, with a nominal
48-unit spacing and neighbouring road links. Your current district is
highlighted; travel times come only from the Java location response.
The map is read-only: it does not teleport the player or expose private
homes. Walk along a signed road to a neighbouring district or use the
original gate for direct travel. Both use the existing Java travel action.

The district grid now has signed roads to neighbouring districts.
Walk outward to the end of a road; the client requests travel and waits
for Java's committed response. Upon success the next district loads at
the opposite road edge without changing camera orientation. Failed
travel leaves the player in the current district; step back before
retrying. The river has a simple bridge. The gate remains available.
Godot now caches the active district and its eight neighbouring grid
cells (where present), at most nine 48-unit chunks. Nearby geometry,
roads and public landmarks are visible before crossing. On arrival,
already loaded chunks are reused and rebased around the active district;
distant chunks are freed. The same weather and utility state applies to
all loaded scenery. Residents are shown only in the Java-confirmed
current district, not fabricated in adjacent chunks.

Java's travel durations are unchanged. The player still pauses for
server confirmation at a boundary, and collision checks and NPC data
are local to the active district. A client-paced live clock is available; persistent fine-grained coordinates
remain future work.

## Live world clock

The default pace is 1:10: one committed game minute per approximately six
real seconds of idle gameplay. The World time selector offers pause,
1:1, 1:5, 1:10, 1:24 and 1:60. Godot uses the existing atomic
`POST /api/v1/actions/wait` action with one minute; only Java advances
the world and saves it. The displayed time, resident list, daylight and
municipal services follow committed snapshots.

Automatic time does not accumulate while the API is busy, a map or
interaction menu is open, or a road crossing is pending. Explicit work,
travel, study and waiting retain their existing time costs and reset the
automatic pacing interval. No background backlog is replayed. If an
automatic write fails, the clock pauses rather than retrying an uncertain
commit; refresh the world and select a speed again. Speed is a session
setting, not a new database field. This is active-client time, not a
server-side clock that keeps running when the game is closed.

## Weather atmosphere

Java supplies CLEAR, CLOUDY, RAIN or SNOW. Godot adds a single
player-following CPU precipitation system (360 rain particles or 220
snow particles), depth fog, cloudier daylight and original synthesized
rain/wind ambience. Compatibility rendering is retained. The Weather FX
and Weather sound switches are independent session preferences; turning
them off never changes temperature, health or resource rules.
Fog is a presentation effect rather than an additional weather state.
No outdoor sound files, network resources or new database fields are
required. Indoor precipitation occlusion and detailed street sounds
are not implemented yet.

## Conversations

Approach a resident within 2.5 local units and press **T**. Say hello,
ask their occupation or enter a message; "My name is Alex." introduces
an alias to that resident. Java validates public-space co-presence,
uses the existing deterministic conversation rules and saves individual
contact memory, advancing one game minute per accepted message. Godot
shows the server reply and the name the resident remembers. The local
proximity check is client-side; exact server-owned coordinates are
not implemented. There is no language model, arbitrary natural-language
understanding, or promise of a job. Ambient clock pauses in conversation.

## Personal social attitudes

Each resident's contact has independent trust (0–100) and affinity
(-100–100), saved for the current visitor in the existing contact tables.
Old databases are migrated with neutral defaults. The dialogue shows
a coarse attitude: stranger, acquaintance, familiar, friendly or wary.
Greetings, introductions and apologies can improve attitude once per
game day; explicitly dismissing a resident or changing a previously
disclosed alias can reduce it. Responses use deterministic Java rules.
There is no global reputation score or romance yet.

## Visitor news and rumors

An ordinary introduction stays private to the resident. Use **Allow
sharing my name** after introducing yourself to create an explicitly
shareable report of that meeting, not proof of the visitor's identity.
Existing NPC social encounters can relay the report between co-present,
awake residents with sufficient trust and sociability. Reports retain
their source, confidence and hop count, and stop spreading after three
hops or fourteen game days. The existing knowledge tables persist them.
**Ask for visitor news** reads the resident's own recent report and
distinguishes first-hand recollection from hearsay. Hearing a name does
not automatically identify the player or create a personal contact.
No private name, arbitrary chat transcript or global reputation score
is propagated. This is a first narrow topic, not general gossip.

## Market and personal bag

The market kiosk sells bread (3 EUR) and bottled water (1 EUR) between
08:00 and 20:00. Each purchase transfers cash to the market payroll,
subtracts a finite retail unit and adds it to the visitor's 12-unit bag.
The bag is saved in SQLite and resets for a new incarnation. Eat or
drink from the HUD in any district; bread reduces hunger by up to 35,
bottled water reduces thirst by up to 40. Retail deliveries replenish
a capped counter at 06:00. This initial retail counter is separate from
NPC shopping and uses abstract external deliveries, not a simulated
food supply chain. Old saves receive empty bags and initial stock.

## First rented room

At the north residences kiosk, prepay 35 EUR for seven game days in
a guesthouse. The payment reaches a persistent guesthouse account.
Rent is never deducted automatically; renew during the last 24 hours.
The HUD displays remaining hours. Sleep for two or eight hours in the
room only at that kiosk and only with a valid lease for the entire
sleep. Sheltered sleep avoids outdoor exposure and weather penalties;
hunger and thirst still advance. A new visitor has no lease. Existing
saves default to no room. The guesthouse is an abstract public kiosk
action, not an enterable interior or shared housing market yet.

## Centro Storico

The fictional Lombard historic centre sits immediately west of Park,
between Market, Clinic and Cafe. Bakery moves one cell south of Cafe;
its existing Java identity and jobs do not change. Piazza Centrale
uses original procedural geometry inspired by Vigevano/Pavia: warm
facades, green shutters, covered walkable porticoes, a civic tower,
stone paving, café tables and four lamps. All four axial road exits
remain open. Some generated residents choose Centro for free time.
The existing job noticeboard opens the real Java job list. The café
and shopfronts are decorative; trading and other services remain in
their original districts. No actual city map or third-party assets are
included. The existing 48-unit chunk size and park start are unchanged.

The square now has deterministic individual stone blocks rendered as
one MultiMesh, four distinct facade palettes, timber and glass shop
fronts, decorative iron balconies, flower boxes, cornices and a small
walkable garden court (Cortile dei Fiori) with collision footprints.
Upper floors and storefront interiors are still non-enterable.

## Via dei Portici

South of Centro, a new public district connects the square, Bakery,
River and South residences. Cafe is relocated west of Bakery on the
schematic grid; no existing Java place IDs or save data are removed.
The 48-unit street includes narrow sidewalks, varied three-storey
building shells, door surrounds, shutters and lanterns. An Alimentari
shop has an open doorway, a walkable ground-floor interior, shelves,
lighting and the existing interaction kiosk inside. It sells bread
and bottled water from the same finite Market stock/account during
the same opening hours. This is a branch counter, not a new independent
business. Other doors and upper floors are decorative. Local collision
checks enforce the doorway; the server validates the district, not
exact indoor coordinates. The room rental is still a kiosk action.

## Casa Ospiti: first furnished room

North residences now includes a walkable furnished ground-floor
guest studio. Pay or renew at reception, then cross the street to
Camera 1. The doorway opens only with a server-confirmed active lease.
Approach the bed and press F to sleep. A lease that expires while
inside returns the visitor to the doorstep. Bedroom, kitchen/dining
corner, bathroom and wardrobe use solid local furniture footprints.
Rain/snow particles and fog are suppressed indoors and weather audio
is quieter; this is a local shelter presentation, not full building
weather occlusion. The upper floor, sink, shower and appliances are
decorative. Java still validates lease and district for sleep, not
exact bed coordinates; no local coordinates persist after restart.

## Household supplies and hygiene

The rented kitchen has a private 24-unit cupboard. Transfer one bread
or bottled water between it and the 12-unit bag. Preparing bread soup
takes 15 game minutes, consumes one stored bread and water, and reduces
hunger by up to 45 and thirst by up to 20. Washing takes ten minutes;
hygiene starts at 100 and falls by one each game hour. It has no other
health or social effects yet. Cupboard contents and hygiene are saved
with the player; checkout clears stored perishables (not carried items).
Renew before expiry to keep supplies. The server checks the lease and
district, while Godot checks proximity to the kitchen or bathroom.
Water utilities, electricity, ingredients and cooking equipment are
still abstracted for this first household version.

## Groceries and recipes

Market and Alimentari share seven products: bread, bottled water,
flour, eggs, pasta, vegetables and milk. All use the same bag and
cupboard capacity limits. Ingredient rows are saved atomically with
the existing bread/water columns and cleared on a new visitor;
expired leases clear the cupboard only. Four recipes consume exact
stored ingredients: bread soup, pasta with vegetables, vegetable
frittata and pancakes. The server validates the entire recipe before
consuming anything. Raw new ingredients cannot be eaten directly.
Deliveries still use capped scheduled replenishment; no spoilage,
refrigeration, supplier contracts or NPC shared purchases yet.
The kitchen and shop action panel is scrollable.

## Cascina delle Risaie

A new agricultural district south of River, east of Bakery, has a
Lombard farmhouse, vegetable beds, rice paddies and a stationary
tractor. The optional farm worker job pays 26 EUR per two-hour shift
from the finite farm account and moves up to four units of each crop
from field to barn. Seasonal growth is capped at 24 units per crop.
An abstract paid contractor harvests at 08:00; at 11:00 up to six
units move from barn to shared retail stock, paid at wholesale prices
from the market account. The physical stock ledger stores fields,
barn and daily markers. Rice and vegetables no longer receive
external daily retail top-ups; other grocery products still do.
Rice can be cooked with vegetables and water. Crop meshes are static
illustrations, not individual harvestable plants; the tractor is not
driveable, and shipment timing is abstract rather than a moving truck.
No named farm NPC or irrigation network yet.

## First transport: used bicycle

The workshop sells one portable used bicycle for 45 EUR during
08:00–18:00. The money goes to the existing technician account.
Ownership and walk/cycle preference persist in SQLite; a new visitor
owns none. Press B or use the transport button to switch modes.
Server-validated district journeys take half the existing walking
time when cycling (5 or 4 minutes). Outdoor local movement is faster;
the studio and grocery interior use ordinary walking speed. This
is the first transport abstraction: no bicycle model, mounting
animation, parking, theft, road traffic, fuel or wear yet.

## Bicycle models and parking

Original procedural bicycle geometry has a frame, tires, spokes,
handlebar, seat and rack; wheel rotation follows local movement.
Press P near a marked bicycle rack in Park, Workshop or North
residences to park/retrieve. The server stores the parking district;
cycling is unavailable until the bicycle is retrieved in that
district, including after restarting. The client enforces rack
proximity and renders the parked model; exact parking coordinates
and theft are not simulated. Walking with an unparked bicycle still
abstracts carrying it. No full rider pose or vehicle steering physics.

## Motor infrastructure

The office/library/workshop/factory/quarry network marks adjacent
motor-road links symmetrically on the schematic map. Those links have
6.4-unit asphalt surfaces, centre/edge markings and zebra crossings;
other connections remain pedestrian-style, including Centro. The
existing walking seam width is unchanged. Workshop now has a marked
parking court, two-bay open service garage and two original compact
car models with solid collision footprints. Garage bay 1 is walkable.
Cars are static scenery, not owned or driveable; there is no traffic
AI, signal control, motor-road routing or vehicle physics yet.

## First motor traffic

Motor links between loaded districts now share two lane-following
compact cars, with positions rebased when the visitor changes chunks.
Cars travel in opposite right-hand lanes on the central section of
the link, stop at red/amber pedestrian crossings, yield to nearby
visible pedestrians and have dynamic local collision footprints.
A 24-second local visual cycle switches traffic lights green, amber,
red. When Java reports failed public lighting, the signals go dark
and cars slow down while still yielding. Historic pedestrian links
remain car-free. This is ambient traffic, not actual NPC car ownership
or Java-managed transport: cars respawn after a short delay at the
ends of each road segment. No junction routing, accidents, player
driving or persistent traffic state yet.

## First privately owned compact car

The workshop sells a used compact car for 180 EUR (6/12 fuel,
80/100 condition). Refuel up to three units for 2 EUR per unit;
service for 15 EUR restores up to 30 condition. Payments transfer
to the workshop account. Public parking spaces exist in all five
motor districts. Approach your parked car and press F for adjacent
motor destinations. Each server-validated trip takes three game
minutes, one fuel unit and three condition points; arrival parks
the car at the destination. Walking away leaves it behind. Park a
portable bicycle before driving. Car state survives SQLite restart.
Historic pedestrian districts are excluded. This is route-based
travel, not manual steering or a driving physics simulation.
Fuel is abstract and available only at the workshop; plan enough
fuel/condition for the return trip. No towing or roadside refuelling
yet, and no fuel wholesaler inventory.

## First-person manual road section

At your parked car, F opens the adjacent motor route choices. Selecting
one places you in the driver's cabin at the start of that road section.
W accelerates, S reverses, A/D steer within the lane, Space brakes,
and mouse look turns your head. The first-person cabin includes a
dashboard, steering wheel, pillars and decorative mirrors. Traffic
and red/amber crossings stop the car. Crossing the road seam commits
the existing Java drive action (3 minutes, 1 fuel, 3 condition) exactly
once; confirmed arrival parks the car and keeps you in the driver seat.
Press 1–3 for the listed adjacent routes, or F/Escape to exit the parked
car. Escape before submission cancels the current preview and returns
to the last committed parking space. The main action panel is hidden
while driving or seated. This is a short
lane-constrained local driving preview: no free steering around town,
manual parking, physical mirror reflections, or server-side vehicle
position persistence yet. An uncertain response requires refresh
before another trip.

## Stazione di servizio

A new public fuel station south of Quarry connects to the motor
network. It has two pump models, a canopy, a walkable mini-market
shell, a public parking space and a local action kiosk. Refuelling
at the station (06:00–22:00) or garage (08:00–18:00) draws from the
same 48-unit fuel stock. The station account receives 2 EUR per
unit; a paid external supplier replenishes up to 12 units at 07:00
for 1 EUR each, limited by cash and capacity. Stock and delivery
markers persist in the industry ledger. A fuel attendant earns
25 EUR per two-hour shift from finite station funds. The shop sells
bread and water from shared market supplies (08:00–20:00). Local
actions are still at the kiosk, not physical pump controls; supplier
transport is abstract, and the new car's included fuel remains part
of its purchase package.

## First player-operated freight delivery

Apply for Freight driver at Quarry. Load up to two iron and one
copper ore units if available. Employer funds reserve the 32 EUR
wage, factory money reserves the ore purchase, and two fuel units
are bought from the station. The assigned truck is distinct from
the existing NPC truck. Press F near it to drive Quarry → Workshop
→ Factory using the same first-person lane controls. On arrival
the truck parks; approach it and press F to continue or unload.
Only unloading transfers the cargo into factory stock, pays the
quarry and releases the driver's wage. The active manifest and
escrow survive restart. On visitor death the cargo and escrow
return to their sources (allocated fuel is not refunded). A
completed delivery uses the daily work allowance. Truck driving
reuses the compact car cabin and lane physics; no cargo weight
or custom gearbox yet. Farm and station supplier deliveries are
still scheduled abstractly.

## Additional freight routes

Freight drivers can load ore at Quarry, parts/steel at Factory,
or vegetables/rice at Farm. The parts route ends at Workshop and
supplies existing technical work materials. The farm route uses
a separate southern motor corridor (Farm → Bakery → Cafe → South
homes → Market); these links are currently freight-only for the
player, not connected to private car travel. The farm truck parking
space avoids the rice paddy. The load reserves destination shelf
space and source barn space until unload/recovery, preventing
scheduled deliveries or harvesting from overfilling storage.
All routes pay 32 EUR from finite freight employer funds; fuel
is purchased once at loading, one unit per planned leg. NPC
scheduled deliveries still operate using unreserved stock. Fuel
supplier freight, a self-financing player logistics company and
distinct truck physics remain future features.

## Owner-operated logistics company

At the Workshop kiosk, register or acquire a carrier for 20 EUR.
Invest 25 or 100 EUR from personal cash into a separate account,
buy one used truck for 220 EUR, and withdraw 25 EUR at a time.
A company contract can be loaded at any of the existing three
pickup sites without a freight employment job. The buyer reserves
40 EUR in addition to the goods invoice; the company pays fuel
at loading, and receives the fee only at unloading. If the truck
is at another district, an outsourced transfer costs 8 EUR plus
one fuel unit. Its district, ownership, company cash and active
contract persist. The same daily work allowance prevents repeated
instant contracts. Company money never counts as personal wages.
Death returns cargo and the reserved invoice/fee to the sources;
the company remains an estate available for paid takeover.
Only one truck and owner-driven contracts are supported. Empty
truck relocation is abstract, not manual driving; the physical
truck is shown during an active manifest. Hiring drivers, fleet
expansion and maintenance are not implemented.

## Hired freight driver

An owner with a truck and at least 40 EUR in the company can hire
one eligible resident currently at Workshop. A 10 EUR signing
payment transfers to the resident. Automatic freight is opt-in.
On working days the driver reports to Workshop; the company can
dispatch one funded available contract per day from 09:00–13:00.
The 18 EUR driver wage is reserved with the fuel and relocation
expenses, then paid when the driver reaches the final delivery
district. The employee uses the existing resident travel system
and needs; the manifest advances at quarter-hour ticks once
the driver reaches each district. Cargo, wages and the completed
counter persist. If the driver is sick, dies or leaves the job,
cargo and escrow are recovered, excluding fuel and relocation
already paid. The owner cannot drive the assigned truck during
automatic work, but can walk, study or work independently.
One shared freight manifest and one company truck remain the
current limits. This is simulation-time logistics, not continuous
background processing while the server is stopped or full NPC
vehicle pathfinding.

## Carrier maintenance and accounts

Company truck condition starts at 100 (including migrated saves).
Each loaded road leg and paid empty relocation costs three condition
points; dispatch reserves sufficient condition for the whole route.
At Workshop, service costs 15 EUR and consumes one existing technical
spare part, restoring up to 30 points in 30 game minutes. A truck
elsewhere can be transported back for 8 EUR and one fuel unit;
the recovery transport does not wear the truck. The company records
actual revenue, fuel, relocation, paid wages, hiring, repairs,
capital purchases, owner investments and withdrawals separately.
Operating net excludes capital purchases and owner transfers.
Escrowed wages are recorded as expenses only when paid. Historic
transactions before this update are not reconstructed; existing
cash is retained and new counters begin at zero. Both manual and
automatic contracts use these same constraints. One truck remains
the fleet limit.

## Two-truck fleet

The Workshop office can purchase a second truck for 220 EUR, hire
a distinct resident for 10 EUR and enable its own automatic dispatch.
It has independent condition, district, cargo manifest, daily dispatch
marker, driver and completed count. Both trucks use the same cash,
fuel and physical warehouse ledgers; reserved farm cargo is counted
across both manifests. The second truck uses all three existing routes,
pays 18 EUR to its driver at delivery and 40 EUR to the company.
Service uses the same money and spare parts as truck one. Both can
deliver at the same time, with independent recovery on illness/death.
The second truck is automated only, represented in the office report,
not yet a separate physical vehicle in the 3D scene. Fleet capacity
is two; the depot remains the Workshop office.

## Deposito Logistico

The twentieth public district fills the grid gap between Farm and
Fuel station. Private cars can reach it via Fuel station, and
pedestrians via all adjacent paths. The walk-in office desk (F)
offers the same registration, accounts, hiring, dispatch and service
controls as Workshop. Trucks purchased or serviced at the depot
park there; drivers hired there report to its office instead of
Workshop. Existing hires and workshop controls remain compatible.
Two garage models reflect authoritative parked truck locations,
with solid footprints when present. Warehouse shelves are scenery,
not a separate inventory. The depot site is public infrastructure,
not yet a separately purchased property, and garage repairs still
use the existing shared technical spare-parts ledger. All views
remain first person.

## Casa del Glicine

The south residences now contain a furnished buyable home with
bedroom, kitchen, cupboard and bathroom, sharing the tested guest
studio floor plan. Buy at the local blue kiosk for 320 EUR; payment
goes to the finite city-maintenance account. The door stays locked
until purchase. Purchase ends any existing lease without refund and
moves its remaining pantry supplies, rather than cloning inventory.
The existing sleep, storage, cooking and wash actions validate access
to the correct district. No rent renewal is required for owners.
Ownership persists in the visitor record; on visitor death the house
returns to the public sale inventory for the new life. No sale by
the living owner, rental income, customization, utility bills or
separate real-estate market yet.

## Private vegetable garden

Casa del Glicine has one outdoor vegetable bed. Press F beside it.
Planting costs 3 EUR (paid to the existing farm account); the seed
purchase and planting are a single action. Watering spends one
water unit from the bag, once per dry day before 18:00. At 18:00
the bed gains one growth point if watered or raining; winter
halts growth. Three growth points permit harvesting four vegetable
units into the existing home cupboard, provided four spaces are
free. The 3D crop models reflect the persisted growth stage.
Growth uses game time, not real-world elapsed time. Death resets
the private bed. No produce sales, additional crops, or separate
seed inventory yet.

## Rainwater and produce trade

Install one rain barrel beside the private garden for 8 EUR.
It collects three irrigation units at 18:00 on rainy days, up
to eight stored units; snowfall does not fill it. Rainwater is
not drinking water and cannot enter the player's bag. On dry
days, choose bottled water or a barrel unit for the same daily
watering action. The barrel and water survive restart and reset
with the private garden on death.

Retrieve vegetables from the cupboard into the bag and sell them
at Market 08:00–20:00 for 2 EUR per unit. Sales move physical
inventory and debit the market account; both freight manifests'
reserved shelf space remains unavailable for sale. All vegetables
in the bag are eligible, not only those harvested at home; buying
at 3 EUR and reselling at 2 EUR loses money. No market stall or
other crop types yet.

## Private chicken coop

Build a three-hen coop at Casa del Glicine for 20 EUR and buy
hens for 12 EUR each (farm-account purchases, not a breeding
simulation). Care before 18:00 uses one rice and one bottled-water
unit from the bag for the whole flock. The daily tick produces one
egg unit per hen if both needs are met and flock condition is at
least 50; in winter production is limited to one unit per day.
Missed care reduces condition by 15 points; complete care restores
10. The flock recovers with care; mortality and individual animal
health are not implemented. The nest holds six units. Collect
one at a time into the kitchen cupboard and cook existing egg
recipes or carry eggs to Market to sell for 2 EUR, subject to
market cash and shelf capacity. Purchases, flock condition, care
markers and eggs persist; the private coop resets on visitor death.
The 3D hens are static models reflecting the current flock, not
autonomous animals. No rainwater drinking, breeding, spoilage or
independent feed inventory yet.

## Private orchard

The north-east yard of Casa del Glicine has four perennial plots:
apple and pear trees (12 EUR each), plum tree and grapevine (10 EUR
each). Establishment needs five watered or rainy game days outside
winter; mature plants need three such days in their fruiting season
(plums in summer, others in autumn). One harvest of three units per
plant per game year is allowed. Unpicked fruit resets outside its
season, while mature plants survive winter. One daily orchard watering
uses a bottled-water or rain-barrel unit for all planted crops.
Harvest goes into the existing 24-unit cupboard. Fruit plates use
one unit, and Market buys fruit for 2 EUR per unit when shelf space
and money permit. New fruit shelves start empty with no automatic
external delivery. All crop markers persist and reset on visitor death.
The maturity timing is deliberately accelerated; models are static
procedural scenery with size and ripe-fruit states. Jam, greenhouse
and compost remain future work.

## Boundaries

This is an incremental game prototype rather than a finished open-world
city. Districts use simple procedural geometry and are streamed in a small
neighbourhood rather than loading a fully detailed city. Walking an avatar within a district is
local rendering only. The transition between districts *is* committed
by Java, including costs in world time and a SQLite save.

Later we can replace these placeholder scenes with real 3D assets,
server-authoritative world coordinates, door/interior permissions,
dynamic NPC movement, true job tasks, and proximity validation for each
kiosk. In the current prototype, the Java server validates the district,
while the client alone enforces proximity to the visual kiosk. The local
HTTP port has no authentication, so **don't expose it publicly**.

## Verification

GitHub Actions checks that Godot 4.6.1 imports/starts the project and
that `tests/SceneNavigationSmoke.gd` renders Park, Market, School
Workshop, Quarry and Factory with context-sensitive interactions without requiring
an existing save or live network service. Java CI separately boots
Spring Boot with an empty SQLite file and exercises real movement,
job applications, work and study over HTTP.

The two test suites cover different parts of the bridge, but they are
not yet a full automated graphical end-to-end test of clicking a kiosk
with the Java process running.

## Surface detail and antialiasing

Grass, asphalt and staggered stone paving have shared 256 px procedural textures with mipmaps, and 2× MSAA smooths geometry edges in the Compatibility renderer. No external assets are required. MSAA may increase GPU load on older hardware.

## Lombard town visual detail

The park, Piazza Centrale, Via dei Portici and Lombard residential facades now use original shared plaster, terracotta and wood textures. Trees have irregular shared-mesh canopies, seasonal colors and snowy tints. Park flowers are batched, benches have individual slats, and street lamps have traditional lantern shapes. These are decorative changes: no collision footprints, travel rules or saved state are changed.

The HUD Graphics quality selector applies immediately for this session: Low disables MSAA, sun shadows and optional foliage/flowers; Medium (default) uses 2x MSAA and 55-unit sun shadow distance; High uses 4x and 80 units; Ultra uses 8x and 105 units. All preserve Compatibility rendering, and higher levels may be demanding on integrated or older GPUs. Actual rendering performance must be evaluated on the target computer.

### Water and evening streets

The public fountain and river share a lightweight animated opaque water shader, without screen-space reflections or refraction. The fountain has a stone rim and a small local particle spray, only while Java confirms a working fountain and available clean water. Low graphics quality disables spray. Municipal lantern glass emits warm light only when the committed time is night and the street-light service works. Historic shopfronts show decorative warm windows in the evening; this does not imply shops are open or change business schedules. No new game state or collision geometry is introduced.

## Individual residents and vehicle styling

Resident ID seeds an original, stable visual profile: height and build, skin and hair palettes, hairstyles, clothing, optional glasses, backpack or cap, walking speed and stride. These are client-side cosmetic traits, not authoritative age, demographic or health data. Existing residents remain the only figures displayed; their identity, activities, collision radius and save state are unchanged. Faces and articulated bodies are still stylized procedural meshes, not photorealistic humans.

Compact traffic cars use a stable color palette per road and lane, two roof heights, slanted windscreens, hub spokes, mirrors, door handles and grille details. Their existing traffic and collision rules are unchanged; cars still recycle at the ends of modeled road segments.

## Everyday resident animation

Present residents with relaxing, resting, free-time, eating or drinking activity can occupy distinct existing park benches and café chairs. Each keeps their seat while eligible and returns to a collision-safe standing anchor when activity changes. Knees bend in the seated pose. Café visitors can hold a decorative cup; idle unseated residents can briefly hold a phone. During an actual player conversation, standing residents turn toward the visitor and gesture, with props hidden. These are cosmetic interpretations, not purchases, meals, phone calls, changes to relationships or new AI-generated conversations. Seat transitions currently reposition the resident; no walk-to-seat navigation or full skeletal animation yet.

## Town soundscape

Original lightweight synthesized audio: distance-based footsteps on approximate grass, stone, asphalt and indoor wood surfaces; outdoor wind, positional fountain water when municipal service works, nearest visible traffic, occasional daytime bird calls near the park, a bell at the hourly game-time transition near the civic tower, and a quiet nonverbal crowd texture when at least two present residents are nearby. Sounds attenuate with distance and outdoor sources stop in sheltered rooms or the car. Jumping and cycling suppress walking steps. Town sound is independently toggleable from weather audio. These are synthesized placeholders, not recorded field sound, speech or actual NPC conversations; only the nearest traffic emitter is modeled. No additional audio downloads or Java state.

### Interactive and district ambience

A landing thump plays only on completed jumps, and the family apartment door plays a creak/latch only when its open state actually changes. Bicycle rolling sound plays only during bicycle movement. Nearby industrial districts have a quiet daytime machinery texture (a cosmetic schedule, not authoritative machine operation). Nocturnal crickets are heard near the park outside winter, rain and snow. All follow the existing Town sound mute control; synthesized audio still requires listening and balance checks on a real device.

## First-person interaction preview

The family apartment entrance now swings on a hinge in about 0.55 seconds. The door and handle remain visible, collision follows the panel, and the doorway stays blocked during most of the opening. The existing E interaction triggers the sound and a short first-person reaching gesture. Optional camera-relative hands are visible on foot; Q cycles empty hands and available bread, water or grape juice from the authoritative inventory. Selecting an item does not consume or create it, and it disappears when the server reports none remaining. Existing consumption controls still submit to Java. Hands can be hidden in the HUD. This is a stylized preview, not a full grasping system, and no stairs or extra floors are added here.

## In-game smartphone preview

Press H or Open phone in the HUD while stationary/on foot to open the smartphone. Home shows current game time, place and cash; Jobs uses existing server listings; Map shows available destinations and opens the existing map; Bank displays the committed cash and bank balances read-only; Contacts remembers residents spoken to during this session; Notifications shows up to 20 actual action feedback messages in this session. Closing returns to first-person controls. Phone is unavailable while driving, cycling, airborne, in transit, or before age 12 in childhood. The prototype is free and has no battery, ownership persistence, remote calls, SMS, or banking transactions. Jobs still require in-person applications, and remote interaction restrictions are unchanged. English and Italian UI labels are included.

### Phone planner

Calendar shows the engine's year, day-of-year, season and occupation (no invented shift schedule). Weather shows the current committed sky, temperature and precipitation, not a forecast. Set a one-shot alarm by hour and minute; a passed time schedules the next day. It fires once on the next committed snapshot crossing the target, including when the phone is closed or a wait action skips across the alarm. A HUD alert and notification accompany a sound controlled by Town sound. Cancel from the Alarm app. Alarms are session-only, reset with a new life, and cannot advance game time or wake a sleeping character by themselves.

### Persistent SMS preview

For the ordinary visitor life, adult residents who know the visitor's name, have at least two conversations, sufficient trust/affinity and sociability may send a daytime SMS. At most one arrives per game day and each sender has a three-day cooldown. Strong affinity can produce a coffee invitation; hunger/thirst can produce a welfare check. The latest message and one preset reply per sender are persisted in the existing incarnation-scoped industry-stock map through the normal atomic Java/SQLite save, not a new database schema. A new incarnation cannot access old messages. The phone SMS app displays the inbox and submits either preset reply to Java, with a HUD/audio notification for new arrivals. Coffee invitations can be accepted on the sending day, creating one appointment for tomorrow at 18:00 in the cafe. The calendar displays its status; the resident's routine targets the cafe between 17:30 and 18:30 when not working, ill or occupied with essential needs. Attendance requires both participants physically present between 18:00 and 18:30, otherwise the appointment expires. The appointment and reply are persisted. No actual coffee purchase, remote transfers, job offers, calls, free-form texting or childhood/linked-adult messaging are implemented.

### Natural introductions and physical signs

Resident names and activity labels are hidden above heads. Ordinary dialogue titles show Unknown resident, Familiar face or the learned name. Ask their name (English/Italian); the resident may refuse based on trust, mood, sociability and whether you introduced yourself. Known identities are saved per visitor incarnation. SMS requires mutual introductions; phone contacts only show names known by the visitor. Core public building signs use fixed Italian plaques, and road directions have posts outside the carriageway instead of floating district labels. Existing interaction markers and other secondary labels remain unchanged. No new database tables; specialized career/quest/public records may still reveal identities independently.

### Town addresses

Every district receives a fictional Italian street marker. North residential buildings have numbers 12 and 14 on Via delle Magnolie, the southern apartment block is Via del Glicine 8; numbered plaques and decorative mailboxes accompany the facades. Ask their address (Where do you live? / Dove abiti?) requires mutual introductions and sufficiently positive trust/affinity. Only adult residents with mapped homes disclose a building address, not a private unit or current location. The disclosed address is recorded per incarnation in SQLite and shown in server-backed phone contacts even after restart. Addresses identify shared building facades rather than individually modeled apartments; old shared addresses do not automatically update after a move. Marco/Elena have representative street addresses for their existing private abstract homes. No visiting private homes, knocking, mail delivery or apartment navigation yet. Public street labels are local fictional geography, not a recreation of exact real Vigevano addresses.

### Residential intercoms

Approach a numbered residential facade and press E near its intercom. Select only a contact who shared that address, then Ring bell. Java checks the visitor's district, known building, actual resident home presence and awake/free state, daytime and relationship. Repeated ringing has a 10-minute cooldown; a willing resident grants a 10-minute invitation to chat at the entrance. Chat consumes five game minutes, modestly improves trust/affinity once per day and consumes the invitation. The frontend enforces exact local door proximity while Java validates district-level location. No actual interior access, NPC doorway animation, postal delivery or invitation to the player's home yet. Ordinary visitor only. English/Italian labels.

### Public stairwell preview

At Casa Magnolia (Via delle Magnolie 12), G opens the schematic common stairwell in first person, E remains the outdoor intercom. Walk up two flights of 18 steps to floors 1 and 2; landings have numbered locked doors, handrails and lighting. Ground height follows steps and collision prevents leaving the stair corridor. Descend and press G near the exit to return to the exact street position. This is a separate local preview scene, not yet the actual facade footprint or a server-modeled interior. No private apartment entry, resident floor assignment, lift or saved indoor position. Jumping/other key-driven town actions are disabled indoors; the Java district stays unchanged.

### Invited apartment visits

At Casa Magnolia, ring a known contact, then Ask to come inside. Java requires a fresh doorstep invitation, home presence, trust >=80 and affinity >=35. It assigns Marco apartment 1 and up to five further distinct home units to doors 2–6, persisted with the existing intercom keys. A 30-game-minute invitation exposes the unit and a visual profile. Walk to that numbered door and press E; Java commits entry before the client loads a guest interior. G at the exit returns to the landing, and expired invitations or unavailable hosts close the preview automatically. Two mirrored layouts, three furniture tiers based on current money, and three personality palettes. No transfers or bedroom actions, host model, detailed bathroom, independently simulated private scene, or saved player indoor coordinates yet. Only Casa Magnolia is supported; shared apartment slots are a bounded prototype, not a citywide housing registry.

### Social guest visits

The host uses their existing resident appearance and faces the visitor, with simple hand gestures. T near the host chats (5 minutes); C and F near the dining table share coffee or a snack (5/8 minutes, consumes one host water/food unit, reduces visitor thirst/hunger); E near the sofa sits/stands with lowered first-person camera; R while seated relaxes (10 minutes, reduces fatigue). Java requires an active invitation, enough time remaining and limits each activity to once per host per day; it raises trust and affinity modestly. Coffee cups appear after a successful response. No separate coffee inventory, free-form indoor conversations, cooking animation, actual host seating or autonomous host indoor routine yet.

### Autonomous home routines

Java derives private household activity from committed time, individual traits, needs and existing home presence, independent of visits. Dirt accumulates on half-hour home ticks and cleaning lowers it; cleanliness is persisted per home unit. Existing hunger/thirst, sleep, work and supplies remain authoritative, without a duplicate meal consumption model. Invited apartment snapshots expose only the host's current routine and cleanliness; guests see the host move via a local obstacle grid between kitchen, chair and cleaning stations. Reading, TV and resting use a seated pose; cooking, drinking and cleaning have simple gestures. TV and book props and visible clutter are procedural. No individual cooking ingredients, appliance simulation or full unseen 3D scenes; animation and geometry need interactive verification.

### Autonomous households

Established autonomous partners may move into one existing private home after 30 days of a stable relationship, if both are at home and neither has a special housing unit. Existing child/guardian housing rules remain authoritative. From 18:00 to 21:00, co-present awake relatives can share one daily dinner when an adult has sufficient food for everyone. Each portion comes from existing supplies. Dinner reduces hunger; fatigue and unpaid bills can produce a tense conversation, while ordinary dinners improve trust and affinity. On weekly dinner days, present partners with enough cash set aside EUR 10 each in the existing family savings ledger. Linked adult players are excluded from autonomous family decisions. Invited apartment views include household counts, dinner status, host activity and a four-place table preview; other family members are not yet rendered as separate models. Household dinner records persist in SQLite. No autonomous forced spending of savings, marriage ceremony, or automatic separation yet.

### Seated family dinner

During a server-confirmed FAMILY_DINNER routine, the invited host walks to a clear approach beside the dining chair, sits facing the table and plays a simple eating gesture. When the routine changes, the host stands at the unobstructed approach and resumes navigation. The chair is solid furniture in both mirrored layouts. This is presentation only: no extra meal, food consumption, or additional household member models are created.
