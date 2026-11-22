# Living Alibi — Roadmap

The eventual game is an emergent detective simulation. The player investigates events in a town where residents continue to pursue their own lives independently.

## Completed

### Marco v1.0 — Autonomous console resident

- Marco's needs, economy, aging and mortality
- SQLite persistence and optional OpenAI dialogue
- Time skipping, controlled live mode and optional catch-up on reopening

### The Living Town v1.1 — First witness infrastructure

- Second named resident (Elena Rossi), with a simplified independent state
- Two homes and four public places, timed travel
- Separate authoritative town events and personal observation logs
- SQLite migration without deleting Marco's history
- Tests for independent life state, meetings, and observation isolation

### Social Life v1.2 — First autonomous relationships

- Rule-based NPC-to-NPC conversations requiring actual co-location
- Affinity, trust, tension, small talk, arguments and reconciliation
- Persistent relationship progress and private observations of social encounters
- Deterministic social rules and smoke tests, without API cost

### Population Generation v1.3 — First procedurally generated citizens

- Reproducible names and stable citizen IDs, traits, professions, abstract home/work locations and simple life goals
- Rule-driven activity, travel, needs, work, shopping, mortality and SQLite persistence for generated citizens
- A conservative cap of 40 generated residents and CLI tools to inspect population and resident state
- Note: no generated-citizen friendship network, births or spontaneous immigration yet

### Private Lives v1.4 — Individual apartments and scene-based witnessing

- Every citizen has a stable private home unit, distinct from the wider residential place.
- Witnesses are assigned only when their current public/private space matches the authoritative event scene.
- SQLite stores new event `space_id` and upgrades v1.3 world saves additively.
- Individualized need phases reduce synchronized resident behavior.
- No room geometry, audibility, household cohabitation, or social graph for generated citizens yet.

## Proposed, not implemented

1. **Shared agent engine:** move Marco, Elena and procedural residents to the same extensible NPC engine; time/position-aware action execution.
2. **Dynamic society:** social network and relationships among all residents, births and migration, shared households, detailed room geometry and scalable work schedules.
3. **Memory and belief:** uncertain memories, information exchange, deception, forgetting, and private motives.
4. **Incidents and evidence:** theft/lost property, alibis, trails, witnesses, event causality, evidence acquisition.
5. **Playable detective:** interviews, case notebook, deduction, accusations and feedback grounded in world truth.
6. **Continuous hosting:** headless daemon + eventual optional Godot 3D client.

## Principles

- The simulation, not the language model, determines world truth and physical outcomes.
- Every resident knows only what they could have witnessed or learned.
- No mandatory paid AI call during time steps.
- Persistent history outlives individual residents.
- API secrets and local conversation history never belong in the public Git repository.
