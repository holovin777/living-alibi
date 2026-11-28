# Living Alibi

**First-person life simulation · Java · Spring Boot · SQLite · Godot 4**

> One city. Many lives. Your choices are yours; the world has a life of its own.

Living Alibi is an experimental open-world life simulator set in a fictional Lombard town inspired by Vigevano. Start as a child in an existing family, grow up, learn, find work, form relationships and decide what kind of life you want to live. Becoming a detective is one possible career, not the game's required objective.

Residents have independent routines, needs, relationships and economic responsibilities. The city continues to evolve across generations. The same world, people and history survive the end of a playable life.

**Status:** active development prototype. The current `main` branch contains experimental systems that still need integration, gameplay and balance testing. This is not a finished game or a public multiplayer service.

## Quick start — Linux / Arch Linux

Requirements: **JDK 21+**, **Maven 3.9+**, **Godot 4**, **curl**, and Git. Maven downloads dependencies on the first build. Ollama is optional.

```bash
git clone https://github.com/holovin777/living-alibi.git
cd living-alibi
bash play.sh --check
bash play.sh
```

Already have a checkout? Close the game first:

```bash
git pull origin main
bash play.sh
```

The launcher builds the Java engine and Spring Boot server, starts the local API at `127.0.0.1:8080`, and opens Godot. Closing the game stops the local server.

Your world is saved in `data/world.db`. **Do not delete this file to install updates.** Back it up with the game and server stopped. If startup fails, check the terminal and `data/server.log`.

## Graphics — NVIDIA / hybrid laptops

Both Linux launchers prefer NVIDIA PRIME offload when `prime-run` or a working `nvidia-smi` is detected. Only the interactive Godot process is offloaded; Java and asset import run normally. Compatibility (OpenGL) remains the default, with bounded directional shadows in geographic mode when NVIDIA is requested. Verify the device in Godot's startup output or `nvidia-smi`; requesting PRIME does not guarantee the driver is correctly configured.

```bash
LIVING_ALIBI_GPU=nvidia bash play-vigevano.sh --wide
LIVING_ALIBI_GPU=system bash play.sh
LIVING_ALIBI_SHADOWS=0 bash play-vigevano.sh --wide
# Experimental Vulkan renderer, if supported by your GPU/driver:
LIVING_ALIBI_RENDERER=mobile bash play-vigevano.sh --wide --preview-only
```

On Arch Linux, `nvidia-prime` provides `prime-run`; install a driver appropriate to your actual GPU first. The launcher does not install drivers. See https://wiki.archlinux.org/title/PRIME. The wide geographic mode uses the same SQLite world; back up before switching maps, or use `--preview-only` to explore without changing it.

## Your first life

On a new save, the current game intends to offer a choice of boy or girl, then links the player to a newborn in a real household. If a suitable birth is not available yet, the waiting menu advances time. Childhood, schooling and age-dependent interactions lead into adulthood. Choose a career, make friends, explore, or start a family.

Death is permanent. The world continues; after returning, the player explicitly chooses to continue, with an eligible existing child prioritized where available. Older saves may retain legacy visitor identities and family histories.

## Controls

| Key | Action |
| --- | --- |
| WASD / arrows | Move |
| Mouse | First-person look |
| Shift / Ctrl | Run / crouch when available |
| Space | Jump when available |
| E | Interact with doors, counters, entrances or travel points |
| F | Life choices / local actions |
| T | Talk to a nearby resident (or click the resident) |
| M | Map where available |
| H | Phone where available |
| Esc | Release the cursor / close menus |

Actions depend on age, location, health, available services and the current life phase. The active game scene is `godot/scenes/park.tscn` with `godot/scripts/ParkDemo.gd`.

## Natural NPC conversation — optional local AI

The game includes deterministic dialogue and optional **Ollama** dialogue. A local model receives limited facts about the resident, their current mood, profession, known family links and conversation memory. It is not allowed to change game state directly.

Install Ollama separately, start its local service, then download a model:

```bash
ollama pull qwen2.5:3b
bash play.sh
```

For richer Italian and English conversations, try the larger model if your machine has sufficient memory:

```bash
ollama pull qwen2.5:7b
LIVING_ALIBI_OLLAMA_MODEL=qwen2.5:7b bash play.sh
```

The launcher checks the configured model at `127.0.0.1:11434`. If Ollama is installed but not running, start its service or run `ollama serve` in a separate terminal. Verify the connection with `ollama list`.

No paid API key is needed for local dialogue. Inference speed depends on your hardware. If the model cannot respond, the game falls back to scripted dialogue and logs warnings. AI replies may still be inaccurate; free-form language does not yet execute arbitrary gameplay actions. Introductions, money transfers, jobs and other world changes remain validated by Java.

## The living world

| Area | Current implementation / prototype |
| --- | --- |
| Generations | Birth, childhood, aging, permanent death, inheritance, succession, genealogy and family recognition |
| Daily life | Food, water, sleep, fatigue, health, weather, households, private spaces and expenses |
| Social life | Personality, relationships, memory, trust, parenting, family searches and dialogue |
| Work and education | Schools, qualifications, employment, engineering, maintenance and Java programming tasks |
| Economy | Shops, wages, debt, finite resources, factories, freight, businesses and property |
| Infrastructure | Water, electricity, gas, roads, transport, telecommunications and maintenance |
| Public institutions | Police, investigations, evidence, courts, municipal elections, budgets and experimental corruption audits |
| Health and wellbeing | Psychological metrics, abstract substance-related consequences, clinic appointments and chronic-condition follow-up |
| Personal interests | Religious convictions, sports clubs and competitions, creative skills and cultural fairs |
| Digital services | Phone systems and experimental Living Net portal, SIM subscriptions and local operator |

Some newer systems exist in the Java engine or an alternate geographic UI but are **not yet fully accessible from the main Godot scene**. Named services, abstract activities and mathematical competitions are not necessarily complete physical buildings or playable activities. The map is fictional, not an exact reconstruction of Vigevano.

The Spring server currently defaults to **240 generated residents**, with an experimental cap of **256 living generated residents** (in addition to legacy named characters). The expanded city uses a 21 × 21 district grid. These settings are prototypes, not guarantees of performance or staffing coverage.

## Persistence and private server

Normal local play uses SQLite and bounded offline catch-up when reopened. The private persistent server can continue independently at the configured rate of **24 game hours per real hour**:

```bash
bash play.sh --server-only
# In another terminal, with the local server or an SSH tunnel available:
bash play.sh --client-only
```

The API binds to localhost by default. **Do not expose the current unauthenticated game-control API publicly.** Multi-account authentication, simultaneous multiplayer, account-owned family members and shared-world security are future work. Avoid running two processes against the same SQLite file.

## Architecture

```text
Godot 4 — first-person client, local visuals and interaction
    │ HTTP / JSON
Spring Boot — authoritative action validation and game session
    │
Java simulation — residents, calendar, economy, institutions
    │
SQLite — persistent world, identities, transactions and history

Optional Ollama — local dialogue generation (no authority over world state)
```

| Directory | Purpose |
| --- | --- |
| `src/main/java/` | Core Java simulation |
| `src/test/java/` | Core smoke tests |
| `server/` | Spring Boot API and integration tests |
| `godot/` | 3D first-person client |
| `docs/` | Design notes and subsystem documentation |

## Development and validation

```bash
mvn clean test
mvn -DskipTests install
mvn -f server/pom.xml test
```

The launch script builds the game but does not run the entire regression suite. Test results, client integration and performance must be verified separately. Current priorities are fixing startup and UI regressions, ensuring consistent family and resident state, validating persistence, improving buildings and NPC navigation, and connecting all new engine systems to the active scene.

Further reading: [Godot client](godot/README.md), [city expansion](docs/CITY_EXPANSION.md), [economy](docs/ECONOMY.md), [industry](docs/INDUSTRY.md), [dialogue](docs/REALISTIC_DIALOGUE.md), [roadmap](docs/ROADMAP.md). Older subsystem documents describe earlier development stages and may not reflect the latest implementation.

English and Italian are the supported interface languages; some text is still untranslated. Earlier development notes and version history are available in the [previous README](https://github.com/holovin777/living-alibi/blob/a84daf56bceaf904185ade64ed729e5e712d51a7/README.md) and Git history.

## Project vision

Build a believable, persistent city in which residents have their own lives and the player is not the center of the world. Improve realism gradually without taking away important personal decisions from the player.

No open-source license has been selected; see the repository for its current licensing status.


### A lost child can ask for help

When playing a linked child aged 3–17, tell a nearby resident in Italian
`Mi sono perso` or `Non trovo i miei genitori` (English: `I'm lost`).
Safety requests bypass ordinary NPC conversation limits. Most adults,
including police, help and report a missing child; a few may be unwilling or
unable to assist and direct the child to someone else. Reports start the
existing parent/police search. An actual sighting provides a last-known district,
not a live position. After an officer finds a child, the case remains in
`WAITING_FOR_FAMILY` until a parent physically arrives in the same simulated
space. Available parents can interrupt work and travel to the reported district.
The officer stays assigned during the reunion phase, and the arriving parent
has a short reunion window. No character is teleported. A previous incorrectly
closed report can be reopened by asking for help again.

### Medical counter conversations

On-duty pharmacists, doctors and nurses offer a greeting to nearby customers.
Italian requests such as `Voglio le pastiglie` prompt clarification; fictional
recovery kits and existing medical services can be requested in dialogue.
Explicit confirmation (`confermo il kit`, `confermo assistenza`, or
`confermo la visita`) uses the existing eligibility, cost and time rules.
The game does not prescribe real medicine. No purchase follows a vague request.

### More natural conversations

Residents allow longer exchanges based on sociability, friendship, family,
work and urgent needs. Public-facing staff can initiate a greeting and have
longer service conversations. A willing resident may finish a brief reply
before resuming their schedule, without erasing their work activity.
Ollama is still optional and cannot change the authoritative world state.

### Physical bus approaches

Municipal bus stops now use collision-checked local through lanes. A visible
bus approaches and departs over several seconds, with a stationary boarding
phase and a moving collision footprint. If no clear lane fits, the kiosk
service remains instead of placing a vehicle in an isolated parking bay.
These are local street segments, not yet continuous cross-district bus routes.

### Street traffic presentation

Traffic cars have four compact body styles, more paint colors, rolling wheel
rims and smoother acceleration. They yield to visible municipal buses.
Lightweight road-edge state is retained during local chunk streaming; cars
on unloaded roads are not yet simulated continuously or saved to SQLite.
Core streets still use bounded loops where no safe through junction exists.

### Imported vehicle models

The main Godot city scene uses four bundled Kenney Car Kit glTF models (sedan,
sport hatchback, van, SUV), with animated wheel nodes and a revised first-person
dashboard. The original models are CC0; see `godot/assets/vehicles/CREDITS.md`.
No network download is required at launch. These are stylized game assets, not
photorealistic vehicle interiors. Driving still uses the existing road-leg system.

### Driving in a born life

A linked adult can buy the existing used car at the workshop for 180 EUR,
using their own savings, and enter it with E near the parked vehicle. F opens
purchase, fuel and service choices. Choose an adjacent road with number keys
while seated, then W/S, A/D and Space. Children cannot use the car system.
The same fuel/condition rules apply; the legacy visitor remains inactive.
Driving is still a short lane preview with a server-confirmed district arrival.
