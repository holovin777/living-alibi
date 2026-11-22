# Memory & Beliefs (v1.6)

This is the first provenance-aware information layer, not a free-form cognitive model. It is **not** the detective UI.

## Three distinct sources

- `town_events`: authoritative events. Only the engine can create these, so model-generated dialogue cannot change world truth.
- `town_observations`: direct sensory events for residents in the precise matching public/private scene, including routine activities. Old observations are not silently turned into beliefs.
- `resident_beliefs`: *noteworthy* claims known by a particular resident. `DIRECT` means witnessed; `HEARD` means reported by somebody. Beliefs carry stable event keys, event time, source ID, learned time, relay hops and confidence.

`information_transfers` is an audit log of conversations that transmitted a claim. The speaker and listener must be alive, awake, present and in the exact same scene. No claim jumps magically across buildings or apartments.

## How it works

1. The engine publishes an event; direct observers see the world event and receive a `DIRECT` belief **only if** its text is in the limited noteworthy category.
2. A social conversation can transmit up to one relevant recent event the listener has not previously heard. The listener receives `HEARD`, with `learned_from` set to the speaker and `hops` incremented.
3. Confidence begins at 100 for direct observation; hearsay is capped below 100 and decreases with each relay and low social trust. This is a design **score**, not a probability that the event occurred.
4. Duplicate incoming event keys are ignored. Direct observation of a matching event takes priority over an existing hearsay report.
5. Beliefs and exchanges are written atomically with town events, observations and social state. The save process preserves earlier world history.

`KnowledgeLife.eventKey(Event)` uses a stable derived event signature based on day, minute, actor, exact scene and event text. Hash collisions are theoretically possible; future cases should switch to a monotonic canonical event ID.

## CLI examples

```text
simulate 14
beliefs marco 20
beliefs resident-0001 20
rumors 20
known resident-0001 20
town-events 20
```

`beliefs` and `rumors` reveal developer-only internal state. In a real detective game the player must interview residents rather than directly inspect their belief tables.

## Limits

- NPCs are not choosing their words through an LLM; conversation descriptions are rule-based.
- No intentional lying, misinformation creation, contradiction resolution, personal agendas, forgetting or sourced testimony in the detective interface yet.
- Social reports are limited to recent events (within 14 game days), and relay depth is capped at three hops.
- Older observation logs were stored without event IDs; v1.6 does not guess which historical events map to claims.
- This is a small-town prototype with a modest maximum resident count, not optimized for millions of beliefs.
