# Living Alibi

**NPC Life Lab: Marco Bianchi v1.0 — console prototype**

**One observable person. Autonomous daily life. Persistent history. Optional language-model conversations.**

This repository is the first Java simulation milestone for **Living Alibi**, a planned emergent detective game. The detective investigation system and multi-NPC town are not implemented yet.

This is the **complete v1.0 console-stage character prototype**, not a finished 3D game or a scientifically accurate model of a human being. Marco continues taking autonomous actions during simulated time without any language model. You can watch him, speak to him, or simulate weeks and years. His state, event log, and conversations survive restarts.

The UI, code, project documentation, and sample dialogues are **English-only**.

## Requirements

- Java 21 or newer (Java 25 on Arch Linux is fine)
- Apache Maven 3.9+
- Internet connection only for Maven's first dependency download and optional OpenAI calls

Run it from the directory containing `pom.xml`:

```bash
mvn -q compile exec:java
```

It automatically creates `data/world.db` on first run. No API key is required.

## What is implemented

- Persistent world day, time, character state and event journal
- Real autonomous needs: hunger, thirst, fatigue, hygiene, stress, loneliness, mood and health in **simplified game units**
- Food and water inventory; shopping only when stores are open and Marco has money
- Income from working as a baker, spending on groceries, rent, debt repayment
- A personal goal (open a bakery) and a possible ownership transition
- Illness, recovery, medical visits, aging and probabilistic mortality; **death is permanent in this world**
- A stable, distinct personality (sociable / careful / curious traits) that affects some choices
- Autonomous work, sleep, exploration, socializing, care and rest
- Persistent player conversations, explicit long-term facts, life milestones
- Optional OpenAI-generated **dialogue** and an explicit **reflect** action to pick a long-term priority (API charges apply)
- Flexible time: manual time-jump, accelerated live mode, optional *offline catch-up* when the app is reopened
- Schema migration that **adds columns and tables without deleting** existing `world_state`, `memories` or `life_events` from v0.1/v0.2

## Commands

```text
help
status
observe
personality
events 30
memories 20
facts
remember My name is Alex.
talk Hello, Marco. Remember my name?
wait 600
simulate 7
speed 60
live 30
offline on
offline off
reflect
exit
```

- `wait 600` simulates 600 game minutes.
- `simulate 7` simulates 7 game days; max 365 days per command. **No API calls are made for simulation.**
- `speed 60` sets 60 game minutes per real second for `live` mode. Supported values: 1, 10, 60 and 1440.
- `live 30` runs for 30 real seconds, saving once per second; max 600 seconds per invocation.
- `offline on` makes the app, **when launched next time**, calculate the minutes elapsed while it was closed and replay up to one game year. It is not an actual background process. `offline off` means world time pauses when closed (the default).
- `talk` uses the model when the API key is present. Without a key it gives a clearly labeled, predefined demo response.
- `remember` explicitly records a permanent fact. Older dialogues remain in SQLite but the language model only receives recent messages plus saved facts and selected milestones.
- `reflect` makes **one paid model request** to choose one priority from a restricted list; normal actions remain controlled by deterministic simulation code.

## Update safely from v0.2

**Important: stop the program before updating.** From your existing project directory:

```bash
cd ~/Repositories/npc-life-lab
mkdir -p data
cp -a data/world.db data/world.db.backup-$(date +%Y%m%d-%H%M%S)
```

Then extract the v1.0 ZIP over the old project:

```bash
cd ~/Repositories
unzip -o ~/Downloads/npc-life-lab-v1.0.zip
cd npc-life-lab
mvn -q compile exec:java
```

The archive deliberately **does not include any `data` directory or database file**. The next start will add new state columns and a new `character_facts` table if necessary. It preserves earlier dialogue text, including Ukrainian text already saved in v0.1, while **all new UI messages are in English**.

If an update fails, stop the program and restore your `.backup-...` file as `data/world.db`.

## OpenAI API (optional)

OpenAI API billing is **separate** from ChatGPT subscriptions. Create an API key, set a strict budget in your API account, and then:

```bash
read -rs OPENAI_API_KEY
export OPENAI_API_KEY
mvn -q compile exec:java
```

Enter the key invisibly after `read -rs`, then press Enter. Never commit keys to Git or store them in game state.

Optional model selection:

```bash
export OPENAI_MODEL=gpt-4.1-mini
```

The selected model must be available to your API account. The application sends selected game state, the player's dialogue, recent memories, milestones and explicit persistent facts to OpenAI during `talk` / `reflect`. Responses are not used as world-state authority. The request uses `store=false`. If the request fails, `talk` falls back to demo replies, and `reflect` makes no state change.

## File structure

```text
npc-life-lab/
├── pom.xml
├── README.md
├── CHANGELOG.md
├── .gitignore
└── src/
    ├── main/java/com/holovin/npclife/
    │   ├── Main.java             # English CLI and time controls
    │   ├── WorldState.java       # Marco's state and personality
    │   ├── LifeSimulation.java   # Needs, actions, economy, age, mortality
    │   ├── WorldDatabase.java    # Additive SQLite migrations & persistence
    │   └── OpenAiDialogue.java   # Optional paid dialogue/reflection API
    └── test/java/com/holovin/npclife/
        └── SimulationSmokeTest.java
```

Run the dependency-free logic smoke tests after compilation:

```bash
mvn -q test-compile
java -ea -cp target/classes:target/test-classes com.holovin.npclife.SimulationSmokeTest
```

## Intentional limits — important

1. This is **one NPC**, not a society. Social interaction with other named, fully simulated NPCs is not yet implemented.
2. Marco makes **rule/utility-based autonomous choices**. An OpenAI call can generate dialogue and, on an explicit `reflect` command, suggest one allowed priority. The model does not independently think every minute.
3. The physiology, economy, illness and mortality are simplified **game mechanics**. They are not medical or economic predictions.
4. The app is not a continuously running 24/7 server. `offline on` merely simulates elapsed time when restarted; someday a dedicated headless Java daemon can run it around the clock.
5. There is no graphics, animation, Godot, audio, pathfinding or multiplayer yet.
6. SQLite contains permanent local conversation history and personal facts, so treat `data/world.db` as private user data.
7. Saves are local and additive, but backups are still essential before each update. Large time jumps can produce irreversible simulated events, including death.

### Recommended first test

```text
status
personality
simulate 1
events 20
remember The player prefers quiet walks in nature.
talk Hello, Marco! How did your day go?
exit
```

Restart with `mvn -q compile exec:java`, then run `memories` and `facts` to confirm persistence.
