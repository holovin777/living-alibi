# Living Alibi — Godot client

The Godot 4 client presents a first-person view of the persistent Java simulation. The active scene is `scenes/park.tscn` and its script is `scripts/ParkDemo.gd`. The HTTP client is `examples/WorldApi.gd`. The geographic preview is a separate development interface; features in it may not yet appear in the main scene.

## Launch

From the repository root:

```bash
bash play.sh
```

See the [main README](../README.md) for requirements, updating, world saves and optional Ollama dialogue. Do not delete the save when updating.

## Controls

| Key | Action |
| --- | --- |
| WASD / arrows | Walk relative to the camera |
| Mouse | Look around after capturing the cursor |
| Shift / Ctrl | Run / crouch where available |
| Space | Jump when permitted |
| E | Context-sensitive interaction, doors and travel |
| F | Local actions and life choices |
| T | Talk to a nearby resident |
| M | Map where available |
| H | Phone where available |
| B / P | Transport / parking in supported modes |
| Esc | Release cursor or close menus |

The available actions depend on the current life phase, location, and server validation. The newborn/childhood interface differs from the adult and legacy visitor interfaces. Family entrances should be attached to residential facades; not all buildings have complete interiors.

## Integration and limitations

The Java server is authoritative for time, identities, relationships, money, jobs and district transitions. Godot handles local walking, visuals and interaction menus. The current world uses procedural districts, simplified character models, partial interiors and approximate local navigation. NPC dialogue may use optional local Ollama with scripted fallback. New engine features still require UI integration and gameplay verification.

For private server testing, use `bash play.sh --server-only` and `bash play.sh --client-only` from the repository root. Do not expose the unauthenticated server directly to the internet.

See the [previous Godot documentation](https://github.com/holovin777/living-alibi/blob/a84daf56bceaf904185ade64ed729e5e712d51a7/godot/README.md) for historical technical notes.
