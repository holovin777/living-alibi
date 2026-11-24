# Living Alibi — first connected Godot town (v1.16 draft)

This folder is a **Godot 4.6.1** project with a primitive 3D prototype.
The Java engine is the authority for time, the player's location,
relationships, careers, resources and SQLite persistence. Godot does **not**
grant itself money, skills, employment, or instant travel by moving a mesh.

## Run it from an entirely new world

From the **repository root**:

```bash
mvn -q -DskipTests install
mvn -f server/pom.xml spring-boot:run
```

The Spring application binds to **127.0.0.1:8080** and creates
`data/world.db` if it doesn't exist. A brand-new world starts in the
park with 50 EUR, no profession, Marco, Elena and 12 additional NPCs.

In **Godot 4.6.1**, import the directory `godot/` as a project by
selecting its `project.godot` file; press **F6** to run the scene or
**F5** to run the project. It uses `scenes/park.tscn` with
`scripts/ParkDemo.gd`. The HTTP adapter is
`examples/WorldApi.gd`. You can also start the Godot project
without the Java server, but its actions will not work and a connection
error appears.

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
and Workshop with context-sensitive interactions without requiring
an existing save or live network service. Java CI separately boots
Spring Boot with an empty SQLite file and exercises real movement,
job applications, work and study over HTTP.

The two test suites cover different parts of the bridge, but they are
not yet a full automated graphical end-to-end test of clicking a kiosk
with the Java process running.
