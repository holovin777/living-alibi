# Living Alibi

**v1.2 — Social Life** · Headless Java detective-simulation prototype

> An independent world, private memories, and a detective who must discover what really happened.

The game is in early development. This release is a **console-based simulation**, not a finished detective game. All source code, documentation, and game interface text are in English.

## What exists today

- **Marco Bianchi**, a baker with his v1.0 physiological and financial simulation, personality, long-term goal, SQLite save data, and optional OpenAI conversations.
- **Elena Rossi**, a shopkeeper with **independent, simpler** needs, employment income, time, money, daily activities, and SQLite-persisted state.
- A small town with **two homes, a bakery, a market, a park, and a cafe**.
- Coarse, **8–10 minute travel time**, with an explicit *in-transit* state (not instant teleportation between destinations).
- An **authoritative world event log**: what actually occurred in the simulation.
- A **separate observation log per resident**: only events the resident was present to witness. This is foundational detective infrastructure, not a full psychology model.
- **Autonomous social life**: Marco and Elena may talk while co-located, building affinity and trust, sometimes arguing or reconciling. Their social relationship and interactions persist in SQLite.
- **Bounded social events**: at most one conversation per encounter, with no language-model calls. All interactions are also in the world event journal and participants' observed logs.
- Backward-compatible, additive SQLite schema extensions. Existing Marco history is retained.
- Offline operation without any API key. The optional OpenAI API is used **only** on `talk` / `reflect`, never for time stepping.

## Requirements

- Java 21+ (Java 25 on Arch Linux works)
- Maven 3.9+
- Maven dependency download on first run

```bash
mvn -q compile exec:java
```

The console creates `data/world.db` if absent. This database is local and excluded from Git.

## Commands

```text
status                # Marco's physiological / financial state
town                  # Who is in town, where, and doing what?
where marco           # Marco's location / current travel
where elena           # Elena's location / current travel
town-events 20        # DEBUG: true events from the world journal
known marco 20        # DEBUG: Marco's personal eyewitness log
known elena 20        # DEBUG: Elena's personal eyewitness log
relationships         # Marco/Elena affinity, trust, tension and conversations
social-events 20       # DEBUG: history of autonomous conversations
wait 600              # Advance 600 game minutes
simulate 7            # Advance 7 game days
personality           # Marco's enduring traits
events 30             # Marco's legacy life events
memories 20           # Marco's past conversations
facts                 # Persisted facts told to Marco
remember My name is Alex.
talk Hello, Marco!
reflect               # Optional paid API call
speed 60              # 60 game minutes per real second in live mode
live 30               # Run for 30 real seconds
offline on            # Catch up on elapsed time when the app next starts
offline off
help
exit
```

`town-events` and `known` are **developer tools**, not the eventual player's omniscient abilities. Real investigations will require interviews and evidence collection rather than direct access to world truth.

### First social-life experiment

```text
town
relationships
simulate 14
relationships
social-events 20
known marco 20
known elena 20
exit
```

Relationships evolve via simplified, seeded rules and actual meetings, not generated dialogue. If they never meet or one person dies, conversations cannot occur. Check `social-events` for the results; long simulations can lead to illness or death, so back up your save first.

### First observation experiment

```text
town
where marco
where elena
wait 600
town
town-events 30
known marco 30
known elena 30
exit
```

Compare the `known` results: both people may know about a meeting, but a person absent from an event should have no record of it.

## Updating from Living Alibi v1.1

**Stop the game first. Back up your local database if you have one:**

```bash
cd ~/Repositories/living-alibi
mkdir -p data
if [ -f data/world.db ]; then
    cp -p data/world.db "data/world.db.backup-$(date +%Y%m%d-%H%M%S)"
fi
```

Extract the `living-alibi-v1.2.zip` archive **from `~/Repositories`**, preserving your existing `.git` directory and ignored local database:

```bash
cd ~/Repositories
unzip -o ~/Downloads/living-alibi-v1.2.zip
cd living-alibi
mvn -q test-compile
java -ea -cp target/classes:target/test-classes com.holovin.npclife.SimulationSmokeTest
java -ea -cp target/classes:target/test-classes com.holovin.npclife.TownSmokeTest
java -ea -cp target/classes:target/test-classes com.holovin.npclife.SocialSmokeTest
mvn -q -Dexec.mainClass=com.holovin.npclife.SocialDatabaseSmokeTest -Dexec.classpathScope=test exec:java
mvn -q compile exec:java
```

Inspect changes and commit your update using your existing GitHub SSH remote:

```bash
git status --short
git add .github README.md CHANGELOG.md docs pom.xml src
git commit -m "Add autonomous social life and persistent relationships"
git push
```

Nothing in the update archive overwrites `data/`, your `.git` directory, or any API configuration.

GitHub Actions automatically compiles and runs four smoke tests on pushes and pull requests.

## Persistent storage

The previous `world_state`, `life_events`, `memories`, and `character_facts` tables are not dropped. The new SQLite tables are:

- `town_people` — town positions and Elena's independent game state (and a snapshot for Marco)
- `town_meta` — whether the two people were already co-located, and when their meeting began
- `town_events` — **world-authoritative** location/action events
- `town_observations` — personal witnessed events keyed by resident

Additional tables in v1.2:

- `town_relationships` — persistent affinity, trust, tension, encounters, conversation counts and deterministic random state
- `social_interactions` — timestamped social events with participants, place, and kind

Simulation state and both event journals are committed in **one SQLite transaction** during time advancement. These are developer-side debug logs, not proof of what an NPC believes or will say under pressure.

## OpenAI API (optional)

ChatGPT subscriptions and OpenAI API billing are separate. No API access is required for simulation. To enable paid conversation/reflection calls, configure a strict API budget, then set a key in your shell without storing it in files:

```bash
read -rs OPENAI_API_KEY
export OPENAI_API_KEY
mvn -q compile exec:java
```

`talk` supplies Marco's saved facts, prior dialogue, important **personal life** events, and his **own eyewitness observations**; it does **not** supply the omniscient town event journal. The model can still make mistakes, so all actions and authoritative world facts remain controlled by Java.

Default model: `gpt-4.1-mini`, override with `OPENAI_MODEL` if supported by your API account.

## Code overview

```text
src/main/java/com/holovin/npclife/
  Main.java             Console commands and simulation loop
  WorldState.java       Marco's original state
  LifeSimulation.java   Marco's original autonomous life rules
  WorldDatabase.java    SQLite persistence and additive migrations
  TownLife.java         Two residents, places, movement, event/observation generation
  SocialLife.java       Local conversations, trust, affinity and conflict
  OpenAiDialogue.java   Optional paid dialogue/reflection
src/test/java/com/holovin/npclife/
  SimulationSmokeTest.java
  TownSmokeTest.java
  SocialSmokeTest.java
  SocialDatabaseSmokeTest.java
```

See [docs/SOCIAL.md](docs/SOCIAL.md) for social mechanics, [docs/TOWN.md](docs/TOWN.md) for modeling limits and [docs/ROADMAP.md](docs/ROADMAP.md) for the detective roadmap.

## Known limitations

1. **Only two hand-authored test residents.** Procedural NPC generation and a scalable shared citizen engine are planned, not implemented. Elena has a lightweight needs/schedule model.
2. Travel is only a timed change of abstract locations, without roads, geometry, maps, or pathfinding.
3. Marco's older v1.0 shopping/eating actions can still happen instantly; their spatial preconditions need refactoring before real investigation mechanics rely on them.
4. Social conversations are rule-generated **summaries**, not freeform NPC-to-NPC speech. There is no rumor transmission, lying, belief uncertainty, gossip, forgetting or evidence yet. Witness observations track only some events.
5. There are no crimes, solved cases, detective inventory, courtroom mechanics, 3D, or multiplayer.
6. `offline on` **replays** elapsed time on the next launch; it is not a persistent 24/7 server.
7. Test classes are assertion-based smoke tests, not a complete production test suite. Back up game databases before upgrading.

License: none specified yet. Public source visibility alone does not establish an open-source license.
