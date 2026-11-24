# Godot ↔ Spring Boot ↔ Living Alibi

## Why two Java entry points?

The simulation remains **plain Java 21** in the repository root. The console
(`com.holovin.npclife.Main`) and all old smoke tests keep working without
installing Spring. A small, independent Maven project under `server/` wraps
the same Java engine in Spring Boot 4.2 and exposes JSON over HTTP.

Godot will be a **view and input client**, not a second simulation engine.
The Java world remains authoritative for time, position, residents, privacy,
economy, resources, and encounters. A 3D map is not authoritative evidence.

```text
Godot 4 (graphics, input, animations)
  -> HTTP JSON (WebSocket may follow)
Spring Boot server/ (REST controller, serialized game session)
  -> existing Java engine (LifeSimulation, TownLife, PlayerLife, ...)
  -> SQLite (world snapshot, NPC histories, resources)
```

## Building on a new machine

Requirements: JDK 21+, Maven 3.9+, and a network connection to Maven Central
the first time Spring dependencies are downloaded.

Build core and install its jar to the local Maven repository:

```bash
mvn -q clean test-compile
mvn -q -DskipTests install
```

Run the optional Spring adapter:

```bash
mvn -f server/pom.xml spring-boot:run
```

The server listens on **127.0.0.1:8080** by default. On a genuinely new
save it generates **12 procedural residents** alongside Marco and Elena, so
the town is populated on first launch. It creates
`data/world.db` if the file does not already exist (relative to the shell's
current working directory). For an isolated fresh save:

```bash
mvn -f server/pom.xml spring-boot:run \
  -Dspring-boot.run.arguments=--living-alibi.database=server/target/new-world.db
```

The console can still run as before:

```bash
mvn exec:java
```

**Do not run the console and server against the same SQLite file simultaneously.**
A dedicated, single-authority game session prevents unsynchronized writes.

## First REST contract (v1, provisional)

| Method | Path | JSON body | Response |
|---|---|---|---|
| GET | `/api/v1/world` | — | Day, year, season, weather sky, temperature, visitor needs, nearby NPCs and local resources |
| GET | `/api/v1/locations` | — | Public destinations with stable IDs and Java-computed travel minutes |
| GET | `/api/v1/jobs` | — | Optional vacancies, qualifications and remaining employer payroll |
| GET | `/api/v1/work-orders` | — | Physical town faults, condition, task stage and necessary components |
| POST | `/api/v1/actions/diagnose` | `{"orderId":"water-pump"}` | Inspect a real technical problem for 30 minutes |
| POST | `/api/v1/actions/repair` | `{"orderId":"water-pump"}` | Consume parts and complete a 90-minute city repair |
| POST | `/api/v1/actions/apply` | `{"jobId":"shop-assistant"}` | Apply at actual workplace |
| POST | `/api/v1/actions/study` | `{"subject":"engineering"}` | One-hour paid class (School / Library) |
| POST | `/api/v1/actions/work` | `{}` | One paid two-hour shift if qualified and funded |
| POST | `/api/v1/actions/go` | `{"destination":"RIVER"}` | Action outcome + updated world |
| POST | `/api/v1/actions/wait` | `{"minutes":60}` | Action outcome + updated world |
| POST | `/api/v1/actions/rest` | `{"minutes":60}` | Action outcome + updated world |
| POST | `/api/v1/actions/sleep` | `{"hours":8}` | Action outcome + updated world |
| POST | `/api/v1/actions/drink` | `{"sourceId":"river-water"}` | Action outcome + updated world |
| POST | `/api/v1/actions/forage` | `{"sourceId":"wild-berries"}` | Action outcome + updated world |

Try it from a second terminal:

```bash
curl -s http://127.0.0.1:8080/api/v1/world
curl -s -X POST http://127.0.0.1:8080/api/v1/actions/go \
  -H 'Content-Type: application/json' -d '{"destination":"river"}'
curl -s -X POST http://127.0.0.1:8080/api/v1/actions/drink \
  -H 'Content-Type: application/json' -d '{"sourceId":"river-water"}'
```

The API never trusts a Godot request to arbitrarily modify health, place,
inventory or NPC knowledge; it accepts *intent* and validates action rules
using the same engine as the console. Each action is serialized, simulated,
and saved transactionally before a JSON response is sent.

## Connected Godot town preview

A small Godot 4 project now lives in [`godot/`](../godot/project.godot).
Open `godot/project.godot` in Godot 4.6.1 and run the project. The
single `scenes/park.tscn` entry scene builds the currently visited public
district using `scripts/ParkDemo.gd`: a park, market, school, library,
workshop, office, river or woods (other places use generic public scenery).
The visuals are primitive placeholder geometry.

**WASD/arrow keys** walk within the visible district. Walk to the
**yellow gate** and press **E** to open the destination menu. The menu
requests `GET /api/v1/locations` and shows travel minutes calculated by
Java. Choosing a destination issues `POST /api/v1/actions/go`; only
after the Java world saves and returns its canonical `player.placeId`
does Godot switch its scene and reset the local avatar position.

Walk to a **blue kiosk** and press **F** to access local actions:
market job applications, school/library lessons, workshop/office career
applications, real inspection/repair tasks and earned paid shifts, plus
local water/berry gathering. The Java
server checks town location, business hours, qualifications, resources
and finances before advancing game time. The kiosk proximity check itself
is currently client-side; real meter-level server navigation will follow.

The HTTP adapter [`godot/examples/WorldApi.gd`](../godot/examples/WorldApi.gd)
serializes requests to avoid duplicate/out-of-order actions. It provides
`world_updated`, `locations_updated`, `job_offers_updated`,
`action_feedback` and failure signals. The local server must be running
on the same machine at `127.0.0.1:8080`. Without it, the graphical
prototype still opens but cannot move between authoritative locations.

**Testing:** GitHub Actions tests Godot 4.6.1 import and headless startup,
plus a specific `SceneNavigationSmoke` script that changes districts and
checks job/study kiosk UI. Java CI separately boots a fresh server and
tests park/market/school travel, hiring and studying over HTTP. This is
not yet a fully automated click-through test with Godot and Spring in
the same process; see [Godot README](../godot/README.md).

The player is not automatically a detective. See
[Careers](PLAYER_CAREERS.md) for jobs, engineering and investigation training;
[Technical Work](TECHNICAL_WORK.md) for persistent faults, finite materials
and salary prerequisites.

## Security and offline considerations

The default server deliberately binds only to `127.0.0.1`; there is **no
login or access control yet**. Do not expose it to the public internet.
For multiplayer, add authentication, authorization, per-player permissions,
replay protection and game-session isolation before allowing remote clients.

For a standalone Godot desktop release we can bundle/start a local Java
runtime and Spring server, or offer an alternate embedded engine later.
A persistent hosted city will use a separately managed server. Spring Boot
is an **API adapter**, not a requirement for every Godot export.

## Seasons and the rest of the roadmap

Introduce seasons and temperature into the **Java world clock**, not into a
GDScript sprite shader. The authoritative engine determines season, weather,
resource growth, exposure and NPC consequences. Godot simply displays
snow/rain and adjusts lighting from that state.

The API reports `year`, `dayOfYear` and `season` from the world
calendar, plus an authoritative `weather` object: `sky` (`CLEAR`,
`CLOUDY`, `RAIN`, `SNOW`), `temperatureC`, `raining` and `snowing`.
It is **fictional, deterministic gameplay climate**, not real weather.
The Java engine already applies rain to water stocks and cold/snow/rain
to outdoor sleeping. See [Weather](WEATHER.md).

Next steps: replace abstract travel gates with connected 3D streets,
persist/map exact player coordinates in Java, add a clock subscription or
event streaming, render actually visible NPCs, and animate the characters.
Weather should be rendered from the API response, never recomputed
independently in Godot. Optimize for a reliable first game launch;
compatibility with disposable pre-release saves is a secondary priority.
