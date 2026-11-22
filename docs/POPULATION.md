# Procedural Population — v1.3

## Scope

This is the **first procedural population milestone**, not an entire living society. The console can add 1–20 new residents per request, up to a conservative 40 generated citizens. The two existing characters, Marco and Elena, remain unchanged. The generator does not require a language-model API.

## Identity

`PopulationGenerator.add(town, count)` assigns permanent stable IDs like `resident-0001`, `resident-0002`, then combines first and last names drawn from fixed sets. New names are unique within one town. Profiles are reproducible from each generation index and a fixed seed; generation does not depend on host time, external services or API credits. Fresh towns generate the same first index consistently.

Each generated resident has:

- Name, age, occupation and permanent ID.
- Home building and employment location.
- Sociability, carefulness, curiosity and a broad life goal.
- Needs (hunger, thirst, fatigue and health), money, food and water.
- Work schedule, travel state and a persisted personal seed.

## Autonomy

`PopulationLife.tick` advances each living generated resident once per game minute. They work on weekdays, travel between abstract places, earn wages, buy provisions when inventory runs low, eat, drink, sleep and choose a leisure destination. Needs can affect health and survival. Decisions are simplified rule-driven gameplay, **not medical or psychological models**.

Each arrival and selected interaction is added to the authoritative `town_events` journal. Only characters present at the same abstract place receive an entry in their private `town_observations`. This is a coarse **potential witness** model, not proof that someone actually noticed or understood an event.

## Persistence

`WorldDatabase` extends the existing `town_people` table additively to save generated profiles alongside their evolving needs and location state. New residents are recreated from their database records on restart. Existing two-character saves, conversations and friendships are preserved. IDs never get reused simply because a resident has died.

Generation uses a copy of the active town and writes it to SQLite before replacing the in-memory town. A database error should therefore not leave an unsaved newly added population in memory.

## Commands

```text
population
population generate 12
resident resident-0001
simulate 7
population 50
where resident-0001
known resident-0001 20
```

The command `population add 12` is an alias for `population generate 12`. Running it twice adds two independent groups of residents; it does not reset the world.

## Deliberate limitations

- No birth, immigration, migration or automatic spawning yet.
- No autonomous relationships among generated residents yet. Only the legacy Marco/Elena pair has a relationship engine.
- Two shared residential buildings do **not** distinguish apartments or privacy; pathfinding, rooms, doors and line-of-sight are not modeled.
- Residents have a few occupations and stable preferences, not unique life stories or deep personal memories.
- All generated jobs use an abstract wage scale. No dynamic business economy, crimes or criminal investigations exist yet.
- The MAX_GENERATED cap of 40 limits test scope. It is not a promise that larger cities run efficiently.

The next milestone should introduce a **shared multi-agent engine** and pairwise social relationships that scale beyond two hardcoded names, then accurate personal memories and evidence mechanics.
