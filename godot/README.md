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

## Controls

- **WASD / arrow keys:** move the placeholder person inside the visible
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
  produces/sells parts only when workers, cash and finite steel are
  available. See [Player Business](../docs/PLAYER_BUSINESS.md).
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

## Boundaries

This is an incremental game prototype rather than a finished open-world
city. Each district currently has simple geometry rather than detailed
buildings, roads and NPC models. Walking an avatar within a district is
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
