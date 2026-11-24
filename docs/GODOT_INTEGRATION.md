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

The server listens on **127.0.0.1:8080** by default. It creates
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
| GET | `/api/v1/world` | — | Day, year, season, visitor needs, nearby NPCs, accessible local resources |
| GET | `/api/v1/locations` | — | Public destinations |
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

## Godot starter client

A small `HTTPRequest`-based GDScript example is in
[`godot/examples/WorldApi.gd`](../godot/examples/WorldApi.gd).
Attach it to a Node; the script emits `world_updated` when the server
returns its state. The client must run on the same computer as the server
for the default localhost URL.

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

The API already reports `year`, `dayOfYear` and `season` from a
365-day four-season game calendar. **Weather and temperature are not yet
simulated**; a season label is not a weather forecast.

Next steps: a server-backed Godot scene, weather/temperature snapshots,
clock subscription or event streaming, visibility-aware NPC snapshots,
then navigation/animation. Optimize for a reliable first game launch;
compatibility with disposable pre-release saves is a secondary priority.
