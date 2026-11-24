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
- `health`: 100 is healthy; currently unchanged by passive need growth.

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

## SQLite compatibility

The additive `player_state` migration provides default needs of 0/0/0/100
for older worlds. Subsequent values persist in the same world transaction
as the player's location and per-resident contact records.

`PlayerNeedsSmokeTest` covers metabolism, rest and travel isolation.
`PlayerNeedsDatabaseSmokeTest` covers migration from the old player
table and repeated save/reload. GitHub Actions runs both tests.

## Deliberately not implemented yet

Player-owned food and water, purchasing, private housing and sleeping,
medicine, and health consequences are **not** part of this increment.
The player cannot become ill or die solely because of this partial need
simulation; a full care loop must exist before punitive consequences are
introduced. No mandatory missions or automatic knowledge of the player's
identity have been added.
