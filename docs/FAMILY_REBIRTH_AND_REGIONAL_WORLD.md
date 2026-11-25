# Family rebirth and regional world

Status: proposed design, not implemented gameplay. This replaces the proposed migration-first roadmap. Existing first launch remains an adult visitor in the park; subsequent lives should support birth into a real family.

## Current state

- One simulated town, inspired by Vigevano. Centro Storico, Portici, residential districts, countryside, factory, quarry and other places are districts of that same town, not independent cities.
- The player is separate from generated residents. After death, GameSession currently calls PlayerLife.beginNewLife automatically, producing another adult visitor in the park with EUR 50.
- DemographyLife has persistent parent and partner identifiers, age, births, guardian support, school attendance and retirement. Actual births have eligibility checks and finite household resources.
- Godot uses first-person exploration and a LiveClock; the Java simulation is authoritative and SQLite commits each action atomically.

## Target rebirth flow

1. Record death and settle the previous incarnation exactly once. Persist a pending-rebirth phase; never reset the town, archive or existing NPCs.
2. Present available existing couples, with public names, occupations, neighborhood and household living conditions. Do not reveal secret NPC memories, account balances or hidden investigation evidence. Only offer pairs meeting demographic eligibility and housing checks. Do not guarantee wealthy families.
3. Select a family; revalidate eligibility on the server, create a unique child identity, record both parents and birth date, and use the household's real resources. No duplicate autonomous child and player avatar.
4. If no pair qualifies, offer to wait for the next eligible family or optionally start an adult life as a legacy fallback. Never fabricate a partnership or force a birth.
5. On restart preserve the pending choice or chosen child's identity. A different incarnation never automatically inherits the former player's job, funds, reputation, private memories or political office.
6. Remain first-person at every playable stage. Newborn/infant stages can use short first-person household scenes; no third-person avatar requirement.

## Childhood and calendar

Use one authoritative calendar for the entire world. Age equals elapsed game years since birth. Do not accelerate the child's biological age independently of parents or skip world history.

Proposed adjustable real-time clock presets (relative to ordinary adult play):

| Age | Stage | Default speed |
| --- | --- | --- |
| 0–5 | Home, family, early exploration | 4x |
| 6–12 | Primary school, friends, interests | 3x |
| 13–17 | Secondary school, hobbies, career interests | 2x |
| 18+ | Adult free life | 1x |

Allow pausing/slowing at any age. These are tuning proposals, not validated performance numbers. For players who prefer a shorter childhood, offer explicit milestone fast-forward in bounded, resumable simulation chunks. All other residents, business ledgers, weather, harvests, budgets and elections must advance using the same clock. Never submit a single eighteen-year minute-by-minute HTTP request. Benchmark before enabling multi-year advancement; stop safely on death, guardian loss, or other important events. Preserve progress after every committed chunk.

Infants need guardian care, children school and age-appropriate routines. Adults unlock jobs and contracts only at the appropriate age. A chosen child should retain one persistent identity and their parent links after adulthood. Do not duplicate child funds, relationships or census records when transferring control. Define adult aging, natural death and inheritance before advertising a complete lifetime. Until age restrictions and save migration pass tests, do not enable the new default rebirth flow.

## Regional expansion

First establish a reliable city identity above district identity. Keep the current Vigevano-inspired town as city 1. Add a second Pavia-inspired town as city 2, using fictionalized geography. A regional travel connection (road, then potentially rail) connects town entrances; no instantaneous inter-city walking. An initially abstract destination may become a fully explorable first-person place later, but label its status honestly.

Each town needs its own population, employers, market stocks, housing, treasury, services and local events. The global calendar, resident IDs, family history and world archive are shared. Migrations transfer the same resident or household across town identities, retaining skills and family links; they cannot create a duplicate or mint money. Trade moves finite goods and payments. Preserve existing saves by assigning current districts to city 1.

Do not add multiple detailed cities before validating the original city, performance and first launch. Initially simulate the remote town at a coarser rate only if its rules conserve time and resources and support consistent arrival/departure events.

## Implementation sequence and verification

- A: persistent death-choice phase, eligible-family query, atomic birth, no-family fallback and restart tests.
- B: player-controlled child identity, age-specific actions, first-person household/school access, guardian routines, adult transition and natural aging.
- C: clock presets and bounded milestone fast-forward with long-run tests and performance measurements; preserve ordinary first-launch behavior.
- D: city identifiers, migration of existing saves, second town and regional travel, then household migration.

Regression cases: clean database launch, no eligible family, invalid/duplicate selection, interrupted save, birth into an eligible family, death while a minor, guardian death, restart during childhood, same calendar for parent and child, no adult actions for minors, adulthood without duplicate identities, no double estate settlement, first-person controls, cross-city travel and conserved funds. Java CI and Godot Park Scene must pass before merging gameplay changes. Do not claim century-scale performance without measuring it.
