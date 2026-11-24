# Natural resources — first playable foundation (v1.16 draft)

## Design principle

Natural sources are world state, **not unlimited loot** and not free items
created when someone is thirsty. Every source has a stable ID, a place, a
quantity, a capacity, and a replenishment rule. Any later NPC, business,
animal or player harvesting must use these same conserved stocks.

Source quantities are **accessible harvest units**. For a flowing river
they model currently obtainable usable water, not every liter in the river.
They are intentionally simple abstractions until weather, catchments,
seasons and hydrology are simulated.

## Playable sources

| Source ID | Place | Initial stock | Daily recovery | Gameplay |
|---|---|---:|---:|---|
| `river-water` | river | 120 | 40 (max 120) | Drink 1 unit; 10% simulated immediate health risk |
| `puddle-water` | park | 5 | 0 | Drink 1 unit; 50% simulated immediate health risk |
| `wild-berries` | woods | 16 | 2 (max 16) | Forage and eat 1 unit |

These are game-balancing probabilities, not public-health data or clinical
estimates. Untreated water from a river may contain contaminants even if
no immediate penalty occurs. Puddles will only refill once rainfall has an
actual weather mechanic; they do not refill magically every day.

Commands:
```text
locations
sources
go river
sources
drink river-water
needs
go woods
sources
forage wild-berries
```

A drink takes 5 game minutes and reduces thirst by 40. Foraging takes 15
game minutes and reduces hunger by 25. Both change the authoritative
world clock, so NPC lives continue. Gathering from an inaccessible place,
while walking, or after depletion is rejected without granting resources.

## Persistence and privacy

`natural_resources` stores the stock of each known source in SQLite and
is updated in the same transaction as world and player state.
Old databases gain a resource table without changing residents, personal
memories or investigation data. Resource stock is copied when a candidate
world is simulated; a failed command cannot accidentally consume real stock.
No resource collection shares the player's private identity.

## Next systems: not yet implemented

1. **Sleep anywhere (first slice implemented):** public parks, riverbanks,
   woods and streets are possible sleeping places. A location-dependent
   exposure heuristic already affects health and can cause visitor death
   over repeated risky nights. No forced property ownership. Future weather,
   proper shelters, clothing, illness, climate and seasonal effects should
   replace this intentionally simple prototype.
2. **Ecology:** animals with births/deaths, plants with seasons and growth,
   rainfall, rivers, soil, extractable stone and wood, harvesting labor,
   spoilage and actual material conservation.
3. **Industry:** logging, farming, fishing, mining, hauling, treatment,
   storage, trading and production. Businesses and residents must acquire
   real inputs and may face shortages or environmental damage.
4. **NPC access:** all NPCs must use the same source ledger, taking
   resources in the scene and at a feasible time, not duplicating them.
5. **Food safety and wellbeing:** cooking, treating water, disease risk,
   injury, clinics, shelter and mortality, avoiding deterministic moral
   judgments about homeless or ill people.

The current console implementation is a vertical slice: only the player
can harvest these three sources so far. It is not yet a full economic or
ecological simulation. Animal hunting and enterprises will follow in
separate tested increments.
