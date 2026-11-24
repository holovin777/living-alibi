# Player Daily Life — v1.16 (in progress)

## A visitor lives on the same world clock

The player is still an optional, anonymous visitor rather than a forced detective.
Their physical condition belongs to `PlayerLife.Player`, never to Marco, Elena,
or any investigation evidence. Nothing about these needs reveals the visitor's
private name to NPCs.

Four persisted attributes use the range 0–100:
- `hunger`: 0 is not hungry; higher means hungrier.
- `thirst`: 0 is not thirsty; higher means thirstier.
- `fatigue`: 0 is rested; higher means more tired.
- `health`: 100 is healthy; unsafe water and hazardous sleep can reduce it.

The world advances one minute at a time. Hunger increases by 1 every 30 game
minutes, thirst by 1 every 20 game minutes, and fatigue by 1 every 30 game
minutes, all capped at 100. These rates share the authoritative clock, including
walking, waiting, live simulation, and optional offline catch-up. They are
deterministic and do not invoke paid AI.

## Commands

```text
me
needs
rest
rest 60
sleep 8
go library
wait 120
needs
```

`me` shows location, private name, and current needs.
`needs` shows the four current condition values.
`rest [minutes]` is a public short break: from 15 to 240 minutes in
multiples of 15 (default 60), reducing fatigue by up to 1 point per 6
minutes. Time still advances for every resident. Rest cannot start during
travel. This is not sleep, and it does not grant access to a private home.

`sleep [hours]` (default 8, permitted 1–12) works in **every accessible
public scene**, including a park, riverbank, forest or residential street.
No private house is granted or unlocked. The sleeping visitor is not on a
mission, and the authoritative world advances while they sleep. An hour
recovers up to 11 fatigue points (fatigue still grows a little as the clock
advances). Sleeping in an exposed spot can harm health over many nights.

Exposure currently follows a simple **gameplay-only** location heuristic:
riverbank (high risk), woods/park/streets (medium risk), other outdoor
public places (low risk), and covered public approaches by cafe, library
and clinic (lowest risk). Those covered spaces do not grant indoor access,
a bed or any legal right to stay there. Future weather, clothing, season,
ownership and permissions will refine the model. A sufficiently depleted
player health score reaches 0 and causes a persistent visitor death; the
town and its people do not stop. No NPC automatically learns who the visitor
was. Severe hunger and thirst make outdoor sleep riskier, but passive
needs **alone** do not yet cause death; this is an incomplete survival loop.

## SQLite compatibility

The additive `player_state` migration provides default needs of 0/0/0/100
and `alive=true` for older worlds. Subsequent values persist in the same world transaction
as the player's location and per-resident contact records.

`PlayerNeedsSmokeTest` covers metabolism, rest and travel isolation.
 `PlayerNeedsDatabaseSmokeTest` covers migration from the old player
table and repeated save/reload. `PlayerSleepSmokeTest` checks sleep
conditions, travel restrictions and mortality. `PlayerSleepDatabaseSmokeTest`
covers upgrading a player table without an `alive` column, death persistence
and the continued privacy of the player's name. GitHub Actions runs all tests.

## Deliberately not implemented yet

Player-owned food and water, purchasing, private housing, weather, actual
clothing/shelters, medicine, disease progression, NPC discovery of a sleeping
player, and mortality from prolonged hunger/thirst **without sleep** are not yet
implemented. The exposed-sleep consequence is the first incomplete mortality
rule; safer public sleeping places exist and the player is not forced to
purchase housing. No mandatory missions or automatic knowledge of the player's
identity have been added.
