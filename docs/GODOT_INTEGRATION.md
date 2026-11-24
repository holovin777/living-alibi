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
| GET | `/api/v1/locations` | — | Public destinations |
| GET | `/api/v1/jobs` | — | Optional vacancies, qualifications and remaining employer payroll |
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

## Godot starter project — visible park

A minimal Godot 4 project now lives in [`godot/`](../godot/project.godot).
Open **`godot/project.godot`** in the Godot 4 editor and run it.
The `scenes/park.tscn` scene creates a placeholder visitor in a green park
with a walking path, trees, a bench, a camera, an on-screen wallet/career
display and a **Find jobs** button. WASD/arrow keys move the visual avatar
**only within the park preview**. This local movement is *not yet a
server-authoritative world position or full 3D navigation*; movement between
abstract town locations still uses the Java game actions.

The project communicates with the local Spring API using
[`godot/examples/WorldApi.gd`](../godot/examples/WorldApi.gd). The script
emits `world_updated` for state and `job_offers_updated` for vacancies.
A missing server leaves the preview visible and shows a connection error.
The server must currently run on the same machine, on localhost port 8080.

The Java and Spring tests are automated in GitHub Actions; **Godot 4 editor
scene import/runtime is not yet part of CI**, so this graphical scene is
an unverified visual prototype rather than a polished game release.

The player is not automatically a detective. See
[Careers](PLAYER_CAREERS.md) for jobs, engineering and investigation training.

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

Next steps: validate and iterate on the first Godot park scene, add
server-authoritative navigation between real scene coordinates, clock
subscription or event streaming, visible NPCs and character animations.
Weather should be rendered from the API response, never recomputed
independently in Godot. Optimize for a reliable first game launch;
compatibility with disposable pre-release saves is a secondary priority.
