# Changelog

## 1.3.0 — Population Generation

- Introduce reproducible procedural residents with permanent unique IDs, names, occupations, home and work locations, stable traits and goals.
- Allow generating 1–20 residents at a time, up to 40 additional residents per town.
- Add lightweight autonomous activities for new residents: travel, jobs, supplies, eating, drinking, sleep and survival.
- Preserve generated identities, individual state, routes and personal preferences through additive SQLite `town_people` migrations.
- Record generated events and per-resident observations; extend `where` and `known` to all known resident IDs.
- Add `population`, `population generate N`, `population add N` and `resident ID` commands.
- Add long-running `PopulationSmokeTest` plus SQLite `PopulationDatabaseSmokeTest` and CI steps.
- Keep Marco and Elena's original simulation, friendship and saved histories intact.

## 1.2.1 — Survival Fix

- Elena can buy food and water on weekends, independent of weekday shifts.
- Restore health after shortages when basic needs are met; preserve permanent death once it occurs.
- Add the 35-day `ElenaSurvivalSmokeTest` regression.

## 1.2.0 — Social Life

- Add persistent, rule-driven conversations, affinity, trust, tension and conflict for Marco and Elena.
- Capture social encounters in world truth, each observer's log and a dedicated SQLite table.
- Add `relationships` and `social-events`, with smoke and database migration tests.

## 1.1.0 — The Living Town

- Add Elena Rossi, six named places, travel times, authoritative town events and private witness observations.
- Add additive SQLite migration and `town`, `where`, `known`, and `town-events` commands.

## 1.0.0 — Marco Bianchi

- Add original autonomous physiology, goals, economy, mortality, time controls, SQLite persistence and optional OpenAI dialogue.
