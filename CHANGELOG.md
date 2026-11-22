## v1.2.1 — NPC survival regression fix
- Elena can visit the market on weekends when food or water stocks run low.
- Health recovers slowly during adequate care; it no longer remains permanently reduced after a temporary shortage.
- Historical relationship status explicitly marks deceased participants.
- Added `ElenaSurvivalSmokeTest` covering 35 game days and permanent death.
- Saved deaths are **not** reversed by upgrading. To retest a fresh world, move `data/world.db` aside after backing it up.

# Changelog

## 1.2.0 — Social Life

- Add persistent social relationship for Marco and Elena: affinity, trust, tension, encounters, conversations and arguments.
- Generate rule-based local small talk, friendly chats, conflict and reconciliation while both residents are co-located and awake.
- Limit chats to one per encounter; persist random state for reproducible continuation after restarting.
- Record each social interaction in world-truth and eyewitness logs, plus a dedicated social interaction journal.
- Extend SQLite schema additively without dropping existing saves or memories.
- Add `relationships` and `social-events` commands and three social tests (including a JDBC integration test).
- No AI calls for NPC-to-NPC simulation; no procedural population generation yet.

# Changelog

## 1.1.0 — The Living Town

- Add Elena Rossi, an autonomous second resident with simplified independent needs and finances.
- Add six named places and timed travel with a distinct in-transit state.
- Track world-authoritative town events separately from per-resident eyewitness observations.
- Add `town`, `where`, `town-events`, and `known` console commands.
- Add new SQLite tables with additive migration, atomic saves for world and town advancements.
- Pass Marco's witnessed town observations (not omniscient world events) to optional AI dialogue.
- Add `TownSmokeTest` and keep the earlier `SimulationSmokeTest` unchanged.
- Retain the v1.0 Marco save schema, core rules, and existing dialogue history.

## 1.0.0 — Marco Bianchi

- Autonomous physiology, financial state, aging, and long-term goals.
- SQLite state/memory persistence and optional paid OpenAI conversation/reflection.
- Time controls and optional offline catch-up.
