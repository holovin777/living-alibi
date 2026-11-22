# Living Alibi — Roadmap

Living Alibi is a planned detective simulation where residents have persistent autonomous lives. A detective can investigate emergent events by interviewing people, checking timelines, and comparing evidence. Truth belongs to the world simulation; NPC dialogue is not an authoritative event log.

## Current milestone: Marco v1.0

- Console-based autonomous life simulation for one resident
- Simplified needs, economy, aging, illness, and mortality
- SQLite persistence for world state, conversations, and events
- Optional OpenAI-powered dialogue and explicit reflection

## Proposed next milestones (not yet implemented)

1. Refactor one-person simulation into a multi-agent world engine.
2. Add locations, travel times, and independently observable activities.
3. Implement private per-NPC memories, witnesses, uncertain beliefs, and gossip.
4. Introduce world-authoritative incidents, evidence, and investigations.
5. Add player detective tools, interviews, case notebook, and verification.
6. Enable headless server operation and eventually an optional 3D client.

## Design principles

- The world advances independently of the player.
- NPCs only know things they could have witnessed, inferred, or learned.
- Rules and simulation decide physical outcomes; language models do not silently rewrite world truth.
- Store personal game data and API credentials outside version control.
- Keep the CLI and documentation in English.
