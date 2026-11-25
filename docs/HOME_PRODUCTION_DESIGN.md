# Home production — proposed implementation

Status: design only. The current GitHub main branch is the v1.15 console
prototype. The newer first-person Godot/Spring Boot working tree, including
the reported private orchard beside Casa del Glicine, has not been recovered
in this checkout. Implement against that newer tree after it is published.
Do not replace its existing code with this prototype.

## Player experience

The player begins as an anonymous person in the park, not as a detective.
Home ownership, gardening, trade, education, engineering and investigation
are optional life paths. All player interactions must be accessible in
first-person view. No external AI service is required.

The private greenhouse (Serra privata) complements the orchard rather than
replacing it. At a greenhouse bed the player can plant, water, inspect or
harvest. Each bed has a stable identity and visible growth stage. Outdoor
weather and seasons still affect the existing orchard; the greenhouse
extends the growing season, but does not eliminate watering or growth time.

The compost bin (Compostiera) accepts eligible plant waste, not money or
arbitrary inventory. Mature compost improves a subsequent crop, is consumed
when applied, and must never generate free inventory on reload.

The cellar (Cantina) holds stored products. The kitchen turns actual
ingredients into homemade bread or jam. Local market sales use the same
economy as other jobs and purchases. Start with direct market sales, then
consider repeat customer orders and an optional private shop.

## Initial balancing proposal (game days, subject to playtesting)

| Crop | Days to maturity | Base yield |
| --- | ---: | ---: |
| Lettuce | 5 | 2 |
| Tomato | 10 | 4 |
| Carrot | 8 | 3 |

One seed per bed, one watering per day; a dry day pauses growth instead of
advancing it. A bed can be harvested only once per planting in this first
iteration. Compost: 3 eligible waste units, 3 game days, 1 compost unit.
Bread: 2 flour + 1 water -> 1 bread. Jam: 3 compatible fruit + 1 sugar +
1 empty jar -> 1 jar of jam. These are proposed simulation units rather
than real-world recipes. Prices should use the existing market and
currency model; do not create a separate money balance.

## Integration boundaries

- Java is authoritative for time, inventory, ownership, recipes and sales.
  Godot renders state and requests actions; it must not mint goods or cash.
- Reuse existing player inventory, home ownership, orchard, season, clock
  and market services. Inspect their actual interfaces before coding.
- Persist stable bed IDs, seed type, planting day, progress, watering day,
  compost batches, storage lots and production state in SQLite using
  additive migrations. No historical harvest or ownership should be invented.
- Validate reachability, ownership, ingredients, quantity and capacity on
  the server. A failed action must not advance or mutate unrelated state.
- Inventory debits, outputs, sales and state changes are atomic; duplicate
  requests must not duplicate harvests, recipe output or payment.
- Storage uses the existing game's spoilage rules if present; otherwise
  introduce those rules separately rather than silently deleting old goods.
- First-person prompts should show only nearby interactable objects and
  the player's own state. No global private NPC information.
- Optional producer reputation should increase only after genuine sales,
  with bounded changes per customer/day. NPC purchasing must consume goods
  and respect existing NPC funds rather than creating money.

## Implementation order and acceptance checks

1. Recover the latest working tree and verify the existing clean start
   (`bash play.sh`, if still the supported launcher), existing saves and
   first-person camera.
2. Add greenhouse beds and deterministic daily growth. Verify planting
   consumes seeds, a dry day pauses growth, winter growth works, and
   harvesting twice does not grant additional products.
3. Add compost and verify maturation, single-use application and persistence.
4. Add kitchen recipes and cellar storage. Verify insufficient ingredients,
   atomic production, capacity and reload behavior.
5. Integrate market sales, with actual inventory and monetary accounting;
   later add repeat orders and producer reputation.
6. Run backend tests, SQLite migration/restart tests and a Godot interaction
   smoke test. Test both a new database and a copy of an existing save.
   Document actual results; this design is not evidence of a passing build.

Keep each feature independently testable and commit small coherent changes.
Never commit a personal save database, API keys or local credentials.
