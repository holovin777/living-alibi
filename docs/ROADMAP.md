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

## Proposed, not implemented

1. **Shared agent engine:** move Marco and Elena to the same extensible, non-hardcoded NPC engine; realistic time/position-aware action execution.
2. **Town simulation:** more residents, work schedules, relationships and interaction durations, dynamic place access, physical objects.
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
