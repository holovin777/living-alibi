# Living Alibi — Private Lives (v1.4)

## Why this matters

A detective cannot assume that every citizen in a residential neighborhood witnessed an activity inside another person's apartment. The previous version observed events by `Place` only; several distinct residents could share `NORTH_RESIDENCES`, causing unreasonable knowledge of private meals and late-night drinking.

## Two layers of location

- **Place:** a coarse part of the town (e.g. `NORTH_RESIDENCES`, `MARKET`, `PARK`). Travel and workplaces still use `Place`.
- **spaceId:** the precise abstract scene. `unit:<citizen-id>` describes a personal residence; `public:<place>` describes a shared public scene. These are logical locations, not 3D rooms.

Every citizen, including Marco and Elena, receives an individual `homeUnit`. If the citizen is at their `home` and not traveling, their current space is that private unit. Otherwise they are in the public space of their current `Place`.

When an event occurs, the simulation writes authoritative truth with `spaceId`. A direct eyewitness log is written **only for a living person in that same scene**. Two citizens inside different units in `NORTH_RESIDENCES` do not witness each other. Two citizens together at `PARK` can witness public events.

**Limitations:** Doors, windows, sound, proximity, home visits, shared families, sightlines and witness reliability are not simulated. The game does not yet distinguish observing an event from interpreting it correctly. The detector UI still exposes all observations as a debug tool.

## Migration

- Existing `town_people` records receive `home_unit` with a unique deterministic fallback for v1.3 data.
- Existing `town_events` and `town_observations` receive `space_id`; old rows have an empty string because exact indoor location cannot be recovered. No historic observations are deleted or silently reinterpreted.
- New events carry public/private space IDs, which persist through save and reload.

Always keep a backup of `data/world.db` before installing an update. It remains excluded from Git by `.gitignore`.

## Developer commands

```text
home resident-0001
resident resident-0001
where resident-0001
simulate 1
known resident-0001 20
town-events 20
```

Use `PrivateLivesSmokeTest` to verify neighbor isolation and public witnesses. Use `PrivateLivesDatabaseSmokeTest` to verify the additive legacy schema migration and persisted isolation.
