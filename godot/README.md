# Living Alibi — first connected Godot town (v1.16 draft)

This folder is a **Godot 4.6.1** project with a primitive 3D prototype.
The Java engine is the authority for time, the player's location,
relationships, careers, resources and SQLite persistence. Godot does **not**
grant itself money, skills, employment, or instant travel by moving a mesh.

## Run the game preview from an entirely new world

On **Arch Linux**, install Java 21+, Maven 3.9+, curl and Godot 4 first.
From the repository root, in the `feat/player-needs-persistence-v1.16`
branch, run:

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
profession, Marco, Elena, and 12 additional NPCs.

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
jumping, NPC-to-NPC collisions, interiors or continuous connected streets.
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
