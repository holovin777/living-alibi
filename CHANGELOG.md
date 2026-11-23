# Changelog

## 1.11.0 — Economy & Debts

- Add weekly housing/utilities bills and occasional repairs to procedurally generated households. Residents can pay what they can afford; unpaid amounts become persistent `unpaid_bills` instead of negative cash.
- Allow small, voluntary zero-interest loans between living, trusted, physically co-present citizens who can afford the transfer. An already indebted borrower cannot receive another personal loan until the first is settled.
- Repay principal from later earnings while retaining a cash reserve; mark unpaid contracts overdue after the due day, and apply a one-time trust/tension consequence.
- Make subsequent motive assessments reflect current unpaid bills and personal debt. Existing case background snapshots and earlier incidents remain unchanged.
- Store contracts and an append-only transaction ledger separately in SQLite. Add `finances`, `debts`, and `ledger` developer diagnostics, explicitly distinct from detective-visible evidence.
- Preserve the existing player world's state with an additive `town_people.unpaid_bills` column, plus new `economy_loans` and `economy_ledger` tables. Prior residents begin with zero new arrears, rather than imagined backdated debts.
- Add economic unit, 14-day integration and JDBC persistence/regression tests. Loans remain simplistic: no interest, court collection, bankruptcy, unemployment, property inventory or macroeconomics.

## 1.10.0 — Motives & Opportunity

- Replace one fixed weekly theft check with daytime opportunity windows, influenced by actual co-presence and a reproducible probability roll.
- Evaluate motive strength from financial hardship, tensions with a particular victim, envy and poor impulse control. The highest-scoring co-present pair is only eligible, never guaranteed to offend.
- Require at least three people in a public scene, a solvent victim, and cooldowns of three days between cases and ten days between offenses by the same citizen.
- Record hidden authoritative motive type and strength on new case files, with contemporaneous background categories for all living residents.
- Add `background <case-id> <resident-id>` after scene inspection and a resident interview. Display only broad incident-time circumstances, not guilt or hidden culprit information.
- Add additive SQLite columns and a separate `investigation_backgrounds` table, preserving all pre-v1.10 cases, interviews, alibis and existing world state.
- Update existing incident fixtures to stage specific situations without relying on the deprecated fixed weekly trigger; add motive unit, full-town and JDBC persistence tests.
- These remain abstract, rule-based wallet thefts. There are no stolen items in a real inventory, no justice standard for accusations, and no complete psychological simulation.

## 1.9.0 — Evidence & Alibis

- Remove the perfect offender-initials clue from newly generated cases: inspect finds ambiguous fabric and an illegible receipt.
- Preserve contemporary positions for every resident at the incident instant, without exposing a global location map to the detective.
- Add `check-alibi <case-id> <resident-id>`: first interview the resident; an alibi at a public scene is corroborated only after interviewing a separate co-present resident and obtaining their follow-up confirmation.
- Add daily jacket color descriptions to incident eyewitness interviews; shared colors are leads, not proof.
- Make cautious witness identification imperfect, instead of revealing the offender in every careful interview.
- Persist incident snapshots, fabric descriptions and detective alibi notes with additive SQLite migration.
- Do not reconstruct incident-time locations for pre-v1.9 saved cases; legacy cases remain open or solved exactly as before.
- Add alibi unit and JDBC regression tests and GitHub Actions steps.
- This remains a rules-based console prototype; accusations are validated against hidden engine truth, not legal proof.

## 1.8.0 — First Investigation

- Generate the first authoritative missing-wallet incident from living, co-present residents at periodic public-scene opportunities.
- Keep culprit identification hidden from case summaries and public witness accounts; the engine retains authoritative identity.
- Add persistent case files, scene examination, attributed witness interviews, and evidence-gated accusation commands.
- Keep room-level witness isolation and existing rumors, personalities, relationships and world saves intact.
- Add additive SQLite tables for cases, witnesses and interviews; add unit, integration and JDBC smoke tests.
- This version is still a **console investigation prototype**, not dynamic property simulation, free-text AI interrogation, or a police/legal system.

## 1.7.0 — Secrets & Deception

- Mark a small deterministic subset of personally sensitive DIRECT events as privately held secrets.
- At real NPC conversations, choose truthful reporting, intentional false claims or withholding based on trust and carefulness.
- Preserve world truth separately from deceptive HEARD claims and track deceptive actions in a developer-only audit journal.
- Persist `resident_secrets` and `deception_decisions` in additive SQLite tables without rewriting legacy history.
- Fix belief UPSERT so a subsequent direct observation also restores the truthful statement text in SQLite.
- Add `secrets <id>`, `deceptions`, dedicated rule-based and SQLite round-trip tests.
- Still no generated crime cases, detective interview UI, sophisticated motives or full natural language deception.


## 1.6.0 — Memory & Beliefs

- Track important firsthand events as `DIRECT` beliefs and preserve their source event identity.
- Relay selected claims during real autonomous conversations; receivers store `HEARD`, a speaker ID, relay hops and reduced confidence.
- Prevent remote conversation, source-free claims, duplication, and direct memories being silently replaced by hearsay.
- Add `resident_beliefs` and `information_transfers` with additive SQLite migration and transactional saves.
- Add `beliefs <id> [count]` and `rumors [count]` developer commands, integration/unit/JDBC smoke tests and CI coverage.
- Do not retrospectively infer historical v1.5 beliefs or enable lying, forgetting, crime generation or the detective UI.

## 1.5.0 — Social Network

- Create persistent pairwise trust, affinity, tension and conversation counts between any residents, not only Marco and Elena.
- Require precise shared public/private scene and awake co-presence before an encounter can occur.
- Limit spontaneous conversation attempts to one per pair per in-game day; simulate small talk, friendship, arguments and reconciliation without OpenAI.
- Preserve Marco and Elena's existing relationship and all previous SQLite history through additive relationship columns.
- Add `relationships [count]`, `relationship <id> <id>`, and social network smoke/persistence tests in CI.
- Generated residents and Elena no longer eat or drink meals while their activity is SLEEPING; generated wage event text is clarified.
- Still no complete NPC cognition, information exchange, lying, family trees or playable detective.

## 1.4.0 — Private Lives

- Assign a stable private home unit to every citizen, including residents generated in v1.3.
- Distinguish **public scenes** from **private apartments** when recording new town events and observations.
- Prevent neighbors sharing a residential building from witnessing private indoor activity.
- Preserve existing SQLite history with additive `home_unit` and `space_id` migrations; legacy events retain unknown room identifiers.
- Show home units with `home <id>`, `resident <id>`, `where <id>`, `town-events` and `known` developer commands.
- Stagger generated citizens' hunger and thirst cycles to avoid artificial simultaneous activities.
- Add `PrivateLivesSmokeTest` and `PrivateLivesDatabaseSmokeTest`; fix CI to invoke SQLite tests with `exec:exec` instead of inadvertently starting the game.
- Still no interior geometry, sound propagation, universal NPC relationship system, player investigation or 24/7 daemon.

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
