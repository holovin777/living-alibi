# Living Alibi

**v1.16 — Living Visitor (draft)** · Java 21+ · Maven · SQLite · console + optional Spring Boot API

> A living town where you can be an unknown visitor, build relationships, or investigate when you choose.

Living Alibi is an early prototype for an emergent detective simulation. Its residents pursue independent activities while the player observes; an authoritative event log is kept separate from what individuals witness. **The player is an ordinary park visitor; detective cases are optional. A first Godot 3D park preview now exists, but it is not a complete 3D world.** Code, documentation, and game text are English-only.

## New in v1.16 (draft)

The game still launches **from an empty SQLite file**. A new visitor is anonymous,
can roam, rest, sleep in public places, drink from finite natural sources and
gather wild berries. The new visitor starts with **50 EUR in their pocket**
and no mandatory job, detective role or qualifications. Jobs, paid lessons,
career changes and two-hour work shifts are optional. A mortality event creates a **new visitor in the same
continuous city**, without resetting NPCs, resources, or historic cases.

A deterministic **four-season calendar** (spring, summer, autumn, winter)
now drives fictional weather, temperature, seasonal berry growth, rain-fed
puddles and the health consequences of sleeping outdoors. Weather changes
are calculated from game time and require no separate weather database.

An optional **Spring Boot 4.2 API** is available under `server/` as the
first bridge to Godot. The standalone Java simulation and console remain
independent of Spring. The backend exposes world snapshots and accepts
validated player actions over versioned REST/JSON endpoints.

See [Godot / Spring integration](docs/GODOT_INTEGRATION.md),
[Player Daily Life](docs/PLAYER_DAILY_LIFE.md),
[Natural Resources](docs/NATURAL_RESOURCES.md) and
[Weather](docs/WEATHER.md) and
[Careers](docs/PLAYER_CAREERS.md) and
[Technical Work](docs/TECHNICAL_WORK.md). Trained technicians and engineers
now inspect and fix real persistent equipment using finite material stocks
before receiving a technical wage. The GDScript client is
[`godot/examples/WorldApi.gd`](godot/examples/WorldApi.gd). A first
connected 3D town preview starts from [`godot/project.godot`](godot/project.godot):
walk within a district using WASD, press E at the yellow travel gate to
visit market/school/workshop/office, and press F at a blue kiosk to
apply for work, study or collect a local resource. The Java server validates
these actions and time costs before Godot changes districts.
See [Godot controls](godot/README.md). Requires a running local Spring
API and an installed Godot 4.6.1 editor.

For a new empty world, first build the engine:

```bash
mvn -q clean test-compile
mvn -q -DskipTests install
mvn -f server/pom.xml spring-boot:run
```

The local API is available at `http://127.0.0.1:8080/api/v1/world`.
It requires no previous save. Do not run the console and Spring server
against the same SQLite database simultaneously.

## Implemented

- **Anonymous visitor (v1.15):** player has a separate persistent position and private identity. Walk freely between public places with `go`, inspect people actually nearby with `look`, speak face-to-face, introduce yourself by choice, and keep an independent per-NPC contact memory. Nothing obliges the player to investigate a case. No NPC learns the player's private name automatically.
- **Marco Bianchi:** baker with richer physiological needs, economic goals, permanent mortality, personal memory, and optional paid OpenAI dialogue.
- **Elena Rossi:** shopkeeper with a simpler independent needs/work system and a persistent friendship with Marco.
- **Procedural population:** generate up to 40 *additional* unique, stable residents (`resident-0001`, etc.), 1–20 per command, without any AI/API calls.
- Generated citizens have age, profession, work location, **private home unit**, stable personality traits, goals, individual food/water, money, work schedules and daily travel.
- New citizens purchase provisions, eat, drink, earn wages and can die if their basic needs aren't met. Their identity and state persist in SQLite.
- Multiple abstract locations and private home units, timed travel, authoritative world events and per-resident eyewitness logs.
- **Private events:** two residents in the same residential building do not automatically witness events inside each other's apartment. New records include a distinct `space_id`.
- Individual need phases reduce synchronized food and water actions across generated residents.
- **Autonomous relationship network:** any two residents can meet, make small talk, become friends, disagree or reconcile, with separate persistent affinity, trust, tension and interaction history. Legacy Marco/Elena state is preserved. New relationships are created only after a shared-scene encounter.
- **Social pacing:** each non-legacy pair attempts spontaneous conversation at most once per game day; contact is not guaranteed. This is still an abstract rule-based simulation, not a fully natural dialogue engine.
- **Attributed beliefs (since v1.6):** residents record notable witnessed events as `DIRECT` and can relay one claim per real conversation. Secondhand `HEARD` claims retain the original event, source ID, relay hops and a reduced confidence score; gossip never writes to world truth.
- **Secrets & Deception (new in v1.7):** some personally sensitive witnessed events become private. A resident may conceal them or intentionally give a false claim depending on trust and carefulness. The false claim enters hearsay, never world truth. All decisions are recorded separately for developer audits.
- **First Investigation (v1.8):** in a sufficiently populated shared public scene, the simulation can generate a missing-wallet case. It chooses participating living residents from their current positions and traits, records a secret authoritative perpetrator, and preserves witnesses, scene examination, interviews, and resolution in SQLite.
- **Playable prototype detective commands:** `cases`, `case <case-id>`, `inspect <case-id>`, `interview <resident-id> <case-id>`, `check-alibi <case-id> <resident-id>`, `accuse <case-id> <resident-id>`. No OpenAI key is needed.
- **Evidence & Alibis (v1.9):** new incidents freeze incident-time location snapshots privately in the engine. Inspecting a scene now produces ambiguous fabric evidence and an illegible receipt rather than the culprit's exact initials. Residents can describe clothing; an elsewhere alibi requires two separately interviewed people, with a follow-up confirmation. An unverified alibi does not establish guilt. All notes and snapshots persist.
- **Motives & Opportunity (v1.10):** theft opportunities are checked at spaced daytime windows, not on a fixed weekday. Autonomous motives combine financial pressure, relationship tension, envy, and impulsivity. A three-day city cooldown and ten-day repeat-offender cooldown limit case flooding. The culprit and motive are authoritative engine truth, never displayed to the detective.
- **Incident-time background:** `background <case-id> <resident-id>` becomes available after examining the scene and interviewing the resident. It shows broad categories for financial strain, interpersonal friction, and impulsivity from the incident instant, not a guilt score. Profiles persist separately in SQLite; old cases gain no fabricated history.
- **Economy & Debts (v1.11):** generated residents pay periodic housing/utilities bills and occasional household repairs. Unpaid expenses become arrears; trusted residents can make small zero-interest loans only if they meet in a public location and the lender has enough cash. Repayment moves existing money between residents; a late loan harms trust and increases tension. New finances affect future motive calculations, never historical case backgrounds.
- **Employment & Life Events (v1.12):** procedural residents can lose jobs, search for new work after a realistic delay, be hired into another occupation, and suffer short illnesses that prevent wage earning. Medical costs become actual household bills, paid or overdue. A daily deterministic decision rule uses a resident's fixed personal seed and the simulated calendar, not real clock time or paid AI calls.
- **Physical property (v1.13):** every resident has stable, individually identified wallets and phones; some own bicycles kept at their private units. The owner of an object is not necessarily its current holder. New wallet thefts transfer real custody without transferring title, and future cases reference their stolen item IDs privately in SQLite. A victim may buy a distinct replacement wallet at the market; the old stolen object stays in the world. Existing cases retain a null physical-item link because their past objects cannot be reconstructed.
- **Recovery & chain of custody (v1.14):** investigator searches require a scene inspection, two interviews (including the searched resident), and an independent name/clothing lead. Only three authorized searches are allowed per case. A successful search moves the actual wallet into a separate evidence space and records the seizure. `return-item` releases the wallet to its legal owner only after the case is solved. SQLite preserves the search journal; property changes record seizures and returns. Future victim selection also respects a 14-day cooldown to prevent repeated attacks on the same person.
- **Developer property diagnostics:** `inventory <id>`, `item <item-id>`, `property-events [count]` show current custody and an append-only property audit. These disclose hidden perpetrator information and are never a fair-play detective interface.
- **Developer life diagnostics:** `career <id>`, `life-events [count]`, and `life-events <id> [count]` show present employment and append-only job/health changes. Private illness details are not automatically broadcast to every nearby NPC or revealed to the detective. Marco and Elena retain their previous special-case work simulation; only generated residents use the v1.12 employment subsystem.
- **Developer finance tools:** `finances <resident-id>`, `debts`, `ledger [count]`, and `ledger <resident-id> [count]`. Loan contracts, household arrears and an append-only accounting history persist in SQLite. These reveal private information and should not be used during a blind detective run.
- **Epistemic separation:** `town-events` is authoritative developer truth, `known` is a direct sensory event log, `beliefs` is a filtered attributed claim journal, and `rumors` is a relay log.
- Deterministic generation and simulation; optional paid OpenAI only for direct `talk` and `reflect` commands, never for simulated minutes.
- Manual time skipping, accelerated live mode and optional offline catch-up *on reopening* (not a continuously hosted server).

## Upgrading an existing world

Quit the console with `exit`, back up `data/world.db` while it is closed, and extract the ZIP from the `~/Repositories` parent folder. The archive never includes your personal SQLite database. v1.15 adds `player_state` and `player_contacts` alongside the existing v1.14 recovery tables. They start with an anonymous visitor at Park and no known contacts in migrated worlds. No historical social introductions are fabricated. Existing people, jobs, cases and old witness logs are retained. The items assigned on migration are initial inventory *as of the migration day*, not a claim about prior ownership or previous crimes. The old `world.db` remains the active save when starting the updated game.

## Quick start

Requires Java 21+ and Maven 3.9+:

```bash
mvn -q compile exec:java
```

The application creates `data/world.db` if missing. That directory is ignored by Git. No API key is required.

### Walk around without a mission (v1.15)

```text
explore
me
locations
look
go library
look
identity Viktor
speak resident-0001 Hello there!
introduce resident-0001
contacts
```

First set `identity` **privately**, then choose whom to tell with `introduce`.
If you prefer, stay unnamed and just talk. A resident you introduce yourself to
will remember the name after reopening the world; others will not magically
know it. The `look` and `speak` commands require actual shared public space.
The `speak` replies are currently simple offline rules; Godot and more natural
resident conversations remain future work. See `docs/PLAYER_PRESENCE.md`.

### Recovery & Chain of Custody (v1.14)

When new cases arise, use **only detective commands** to establish leads. Example format (IDs are examples, not tips about the culprit):

```text
cases
case case-0017
inspect case-0017
interview marco case-0017
interview elena case-0017
search case-0017 marco
case case-0017
accuse case-0017 marco
return-item case-0017
```

The target of a search must be interviewed, and either a witness must name the target or their jacket color must agree with the **non-unique** scene fiber and they must have been present at the incident. Unauthorized searches do not consume the three-search budget. A successful search seizes a particular known stolen wallet; it does not change legal ownership, automatically solve a case, or give the player global access to private inventories. The last two commands above only succeed when the evidence actually supports them; **the example is not a suggested suspect**. A search is an instantaneous simulated police procedure, not yet modeled as travel, a legal warrant, or a detailed inventory search. A negative search does not prove innocence. Older pre-v1.13 cases cannot be searched for retroactively invented items. Wallets returned after case resolution may coexist with replacements purchased while the originals were missing.

### Property & Possessions (v1.13)

```text
inventory resident-0001
inventory elena
item wallet-elena
property-events 20
simulate 14
cases
inventory resident-0001
```

**Developer-only data:** `inventory`, `item`, and `property-events` reveal object ownership, actual custody and potentially hidden thieves. Avoid them when solving a case. Each new reported wallet theft carries the ID of a particular wallet in the *authoritative database*, while the detective UI continues to display only imperfect clues and attributed interviews. Wallets follow their actual holder as people travel; bicycles are currently stored at home, not rideable vehicles. New wallets are purchased at the market after a loss if the resident can afford them. Basic detective-gated seizure and post-resolution return now exist in v1.14. Gifts, trade, fences, disguises, voluntary handovers and persistent ground drops are not yet implemented. Old reports (case IDs created before v1.13) do not magically gain an item history.

### Employment and short life events (v1.12)

```text
career resident-0001
simulate 21
career resident-0001
life-events 30
life-events resident-0001 20
finances resident-0001
ledger resident-0001 20
```

**No guaranteed layoffs, illnesses or loans in any particular 14-day period.** Existing residents begin v1.12 employed and healthy (no invented backdated events). Future unemployment stops their hourly pay; seeking new work can take several days and may lead to a different workplace. Two- or three-day illness episodes interrupt work and create medical bills. Affluent residents may still pay everything and have no debts. These summaries expose private data and are **developer-only**; the detective must not use them as automatic evidence.

### Household finance diagnostics (v1.11)

```text
finances resident-0001
debts
ledger 20
ledger resident-0001 20
simulate 14
finances resident-0001
```

These are **developer-only diagnostics**, not discoveries available to the player character. If your existing world is prosperous, you may see bills but no loans; loans require actual cash shortages, trust and co-location. Only generated citizens get these new automatic housing/repair bills. The existing economy for Marco is not charged twice, and Elena's legacy shop income/expenses are unchanged.

### Try your first investigation

If your saved world already has 12 generated residents, **do not run `population generate 12` again**.

```text
cases
simulate 14
cases
case case-0001
inspect case-0001
population 20
interview marco case-0001
interview elena case-0001
check-alibi case-0001 elena
background case-0001 elena
case case-0001
```

Use `interview <resident-id> <case-id>` to question other residents. Try `check-alibi <case-id> <resident-id>` after interviewing that resident, then interview a possible public-place corroborator and repeat the check. Only once you have enough evidence, try `accuse <case-id> <resident-id>` with your own suspect, **not an example ID supplied by this guide**. The culprit is not shown in `cases`, `case`, or `inspect` for newly generated cases. For debugging, `town-events` may expose authoritative events; avoid it while playing detective.

Prototype limitations: incident opportunities are now checked in sparse, deterministic daytime windows with a probability based on motivation; a case requires at least three residents co-present at a public location, so case counts and times depend on the town. Stronger incentives do not guarantee crime. The only incident type is wallet theft: v1.13 linked new reports to actual inventory objects, and v1.14 adds evidence searches and recovery, but investigation, forensics and criminal justice remain simplified. Scene inspection and interviewing are instantaneous and location-independent, and the final accusation is judged against hidden engine truth rather than legal proof. Evidence remains basic and ambiguous, and interrogation uses fixed rules, not open-ended AI speech. Witness accounts may be uncertain or mistaken. The accusation outcome still compares against hidden engine truth, not a judicial standard. Legacy pre-v1.9 cases have no incident-time location snapshots; the engine does not invent old alibis or reconstruct physical clues from the culprit's identity. Pre-v1.10 cases likewise have no recorded motive profiles and the detective cannot reconstruct them retrospectively.

### Try a generated town

```text
population
population generate 12
population
resident resident-0001
home resident-0001
simulate 7
population 20
where resident-0001
known resident-0001 20
town-events 20
relationships 20
relationship resident-0001 resident-0002
social-events 20
beliefs resident-0001 20
rumors 20
secrets marco 20
deceptions 20
exit
```

`population generate 12` **adds** residents to the existing world (Marco and Elena remain). Generation is deterministic by stable resident index. The command does not automatically create births, migration or families. Relationships form later when residents spend time together. Each citizen receives a unique **abstract private unit** within an existing residential area, not a furnished or geometrically modeled apartment.

### Other commands

```text
help
status
observe
personality
town
population [count]
population generate <count>   # or: population add <count>
resident <id>
home <id>                # DEBUG: assigned private home unit
where <id>
known <id> [count]         # DEBUG: private observation journal
town-events [count]        # DEBUG: authoritative world truth
relationships [count]       # strongest/most active links first
relationship <id> <id>     # e.g. relationship resident-0001 resident-0002
social-events [count]
beliefs <id> [count]       # DEBUG: DIRECT/HEARD, source, confidence, hops
rumors [count]            # DEBUG: who told what to whom
secrets <id> [count]      # DEBUG ONLY: sensitive event keys for a resident
deceptions [count]        # DEBUG ONLY: withheld / deliberate lies
cases                    # public case reports; no culprit shown
case <case-id>           # case summary and your collected evidence
inspect <case-id>        # search the incident scene (abstract, instant)
interview <id> <case-id>  # record a resident's statement
accuse <case-id> <id>     # submit a theory after at least two interviews

events [count]
memories [count]
facts
remember My name is Alex.
talk Hello, Marco!          # optional paid dialogue when OpenAI API is enabled
reflect                     # optional paid reflection
wait 600
simulate 7                  # up to 365 days per command
speed 60
live 30
offline on
offline off
exit
```

**Developer-only commands:** `town-events` exposes world truth, `known` exposes direct witness logs, `beliefs`/`rumors` expose attributed reports and `secrets`/`deceptions` expose confidential knowledge and deception decisions. The eventual player-detective will instead need to investigate, interview and verify evidence.

## Updating safely from v1.6

Stop the game first. Preserve the local database (important because deaths and other world events are permanent):

```bash
cd ~/Repositories/living-alibi
mkdir -p data
if [ -f data/world.db ]; then
    cp -p data/world.db "data/world.db.backup-$(date +%Y%m%d-%H%M%S)"
fi
```

Download the `living-alibi-v1.7.zip` release archive, then **unzip from `~/Repositories`** so the files update the existing checkout instead of creating an unrelated project:

```bash
cd ~/Repositories
unzip -o ~/Downloads/living-alibi-v1.7.zip
cd living-alibi
mvn -q clean test-compile
java -ea -cp target/classes:target/test-classes com.holovin.npclife.PrivateLivesSmokeTest
java -ea -cp target/classes:target/test-classes com.holovin.npclife.SocialNetworkSmokeTest
java -ea -cp target/classes:target/test-classes com.holovin.npclife.KnowledgeSmokeTest
java -ea -cp target/classes:target/test-classes com.holovin.npclife.KnowledgeIntegrationSmokeTest
java -ea -cp target/classes:target/test-classes com.holovin.npclife.DeceptionSmokeTest
java -ea -cp target/classes:target/test-classes com.holovin.npclife.DeceptionIntegrationSmokeTest
mvn -o -Dexec.executable=java -Dexec.classpathScope=test '-Dexec.args=-ea -cp %classpath com.holovin.npclife.PrivateLivesDatabaseSmokeTest' exec:exec
mvn -o -Dexec.executable=java -Dexec.classpathScope=test '-Dexec.args=-ea -cp %classpath com.holovin.npclife.SocialNetworkDatabaseSmokeTest' exec:exec
mvn -o -Dexec.executable=java -Dexec.classpathScope=test '-Dexec.args=-ea -cp %classpath com.holovin.npclife.KnowledgeDatabaseSmokeTest' exec:exec
mvn -o -Dexec.executable=java -Dexec.classpathScope=test '-Dexec.args=-ea -cp %classpath com.holovin.npclife.DeceptionDatabaseSmokeTest' exec:exec
mvn -q compile exec:java
```

Only after checking the new version:

```bash
git status
git add .github README.md CHANGELOG.md docs pom.xml src
git commit -m "Add private NPC secrets and deceptive testimony"
git push
```

The ZIP does **not** contain `.git`, `data`, an API key, or a SQLite database. Database migration adds `resident_secrets` and `deception_decisions` without deleting v1.6 beliefs, world events, relationships or dialogue. Past events are not retroactively assigned secrets.

## Architecture

| File | Purpose |
| --- | --- |
| `LifeSimulation.java`, `WorldState.java` | Marco's original character engine |
| `TownLife.java` | Shared town, movement, public/private spaces and witnessed events |
| `SocialLife.java` | Marco/Elena relationships |
| `PopulationGenerator.java` | Stable identities, traits, homes and occupations |
| `PopulationLife.java` | Autonomous needs, schedules, shopping and travel of generated residents |
| `SocialNetwork.java` | Autonomous pairwise interactions among all residents |
| `KnowledgeLife.java` | Provenance-aware, first-hand and hearsay claims |
| `DeceptionLife.java` | Secret eyewitness events, withholding, and false reports |
| `WorldDatabase.java` | SQLite persistence and additive migrations |
| `Main.java` | Console commands and time management |
| `OpenAiDialogue.java` | Optional paid conversation/reflection (Marco only) |

See [docs/SECRETS_AND_DECEPTION.md](docs/SECRETS_AND_DECEPTION.md), [docs/BELIEFS.md](docs/BELIEFS.md), [docs/PRIVATE_LIVES.md](docs/PRIVATE_LIVES.md), [docs/SOCIAL_NETWORK.md](docs/SOCIAL_NETWORK.md) and [docs/ROADMAP.md](docs/ROADMAP.md).

## Tests

```bash
mvn -q test-compile
java -ea -cp target/classes:target/test-classes com.holovin.npclife.SimulationSmokeTest
java -ea -cp target/classes:target/test-classes com.holovin.npclife.TownSmokeTest
java -ea -cp target/classes:target/test-classes com.holovin.npclife.SocialSmokeTest
java -ea -cp target/classes:target/test-classes com.holovin.npclife.ElenaSurvivalSmokeTest
java -ea -cp target/classes:target/test-classes com.holovin.npclife.PopulationSmokeTest
java -ea -cp target/classes:target/test-classes com.holovin.npclife.PrivateLivesSmokeTest
mvn -o -Dexec.executable=java -Dexec.classpathScope=test '-Dexec.args=-ea -cp %classpath com.holovin.npclife.SocialDatabaseSmokeTest' exec:exec
mvn -o -Dexec.executable=java -Dexec.classpathScope=test '-Dexec.args=-ea -cp %classpath com.holovin.npclife.PopulationDatabaseSmokeTest' exec:exec
mvn -o -Dexec.executable=java -Dexec.classpathScope=test '-Dexec.args=-ea -cp %classpath com.holovin.npclife.PrivateLivesDatabaseSmokeTest' exec:exec
```

Smoke tests do not replace more thorough integration and balance testing. GitHub Actions runs all of the above when code is pushed.

## Known limitations

- **Not yet one unified citizen engine.** Marco, Elena and generated people currently have different rule implementations; behavior isn't fully comparable. A shared agent architecture is the next technical priority.
- Each resident has an abstract private apartment within a building, but there are no doors, sightlines, rooms, shared households or audibility mechanics yet. Public locations still treat everyone present as an eligible witness. **This is not forensic-quality evidence.**
- Generated residents form basic pairwise relationships and can conceal or lie about selected experiences, but have no children, migration, autonomous crimes, sophisticated deception plans, complex memory decay or freeform AI conversations yet.
- Only two named residents are pre-authored. New identities appear **only when you request population generation**, not automatically on a timer.
- Travel uses approximate times and does not have routes or a map. Some legacy Marco actions do not yet require physical presence at a site.
- Maximum 40 generated residents (42 including Marco and Elena) is a conservative testing cap, not a general engine limit.
- Local SQLite history is private; do not commit it. Back up before upgrading or performing long simulations.
- A public repository does not automatically grant an open-source license; no license is selected yet.

## Optional OpenAI API

The game works without API calls. ChatGPT subscription charges and OpenAI API charges are separate. For optional Marco dialogue/reflection, configure an API budget first and set the secret in your terminal:

```bash
read -rs OPENAI_API_KEY
export OPENAI_API_KEY
mvn -q compile exec:java
```

Never commit API keys. OpenAI dialogue receives selected personal memory and eyewitness data; it is not allowed to overwrite world truth. New NPC-to-NPC attributed beliefs are rule-based and do not require the API.

## Memory & Beliefs v1.6 verification

Stop the simulation before updating; back up `data/world.db` first and unzip v1.6 over the existing `~/Repositories/living-alibi` directory. Do **not** delete your saved world.

```bash
mvn -q clean test-compile
java -ea -cp target/classes:target/test-classes com.holovin.npclife.KnowledgeSmokeTest
java -ea -cp target/classes:target/test-classes com.holovin.npclife.KnowledgeIntegrationSmokeTest
mvn -o -Dexec.executable=java -Dexec.classpathScope=test '-Dexec.args=-ea -cp %classpath com.holovin.npclife.KnowledgeDatabaseSmokeTest' exec:exec
```

The JDBC smoke test uses a **temporary SQLite file**, not your own `data/world.db`. The existing town remains intact; v1.6 creates two new tables without removing older events or memories. Existing v1.5 observations are **not** retrospectively promoted to beliefs because the old rows lack some provenance information.

Inside the running game:

```text
simulate 14
rumors 20
beliefs marco 20
beliefs resident-0001 20
known resident-0001 20
```

`rumors` and `beliefs` are **developer-only inspection tools**, not detective UI. The original `known` command still returns direct observations and may contain routine events such as eating. The new `beliefs` journal selects only notable events (meetings, arguments, deaths and potential crime-relevant reports).

The system relays only a limited, accurate description of an event; **there is no intentional lying, source impersonation, forgetting, independent fact-checking, or probabilistic memory error yet**. The confidence score is a simplified heuristic based on whether a claim was witnessed, relay distance and pair trust, not a calibrated probability of truth. No API charges are generated by simulated conversations.
