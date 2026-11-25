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

The 3D preview now **starts in first person**: you look out from the ordinary
visitor's eye level rather than watching a fixed camera behind a model.
Press **V** to switch between first person and third person at any time;
the camera setting is only visual and never changes the Java/SQLite world.

Click the empty 3D scene to capture the mouse and look around.
Press **Esc** to release the cursor. Pressing **E** at the travel gate
or **F** at a local kiosk also releases the cursor whenever the
clickable action menu opens. In third person the visitor's temporary
simple avatar is visible; in first person the avatar mesh is hidden
to prevent it from blocking the view. Both modes use the same local
WASD movement and interaction ranges.

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
