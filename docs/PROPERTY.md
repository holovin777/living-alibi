# Property & Possessions (v1.13)

## World truth and detective evidence

`PropertyLife` creates persistent physical item identities for wallets, phones and some bicycles. A record has a **legal owner** and optionally a separate **holder**. Carried belongings follow the holder's real position (and follow them during travel); stored bicycles remain in the person's private home unit. A theft transfers custody, **not ownership**. This prevents a missing wallet from being treated as a pure subtraction from the victim's balance.

All `inventory`, `item` and `property-events` commands are developer diagnostics. They can reveal actual perpetrators and must not be available as direct detective knowledge. The original witness isolation, scene examination, interview and accusation mechanics remain distinct.

## New wallet thefts

- A new opportunity requires a victim carrying an identified wallet. An offender must share their public scene.
- `InvestigationLife.recordIncident` captures the case's `physicalItemId` and calls `PropertyLife.stealWallet`. The object's owner ID remains the victim; its holder becomes the offender.
- The separate EUR 15 money transfer remains the prototype cash loss recorded in v1.8–v1.12. The wallet is **an additional identified object**, not a second EUR 35 payout. The `value` field is estimated replacement value and does not itself change banknotes.
- The developer-only property audit records the actual transfer. Public case text and eyewitness summaries do **not** reveal the item's unique ID.
- At a market visit, a resident without a carried wallet and at least EUR 40 cash can buy a new one for EUR 12. That produces a new stable ID while the previous stolen item stays in existence.
- The prototype cannot yet return a recovered item or money through an investigation verdict. `accuse` still closes the case, not a police property-recovery workflow.

## Migration and transactions

`property_items` is an upserted snapshot; `property_changes` is an append-only history. `investigation_cases.physical_item_id` is nullable for all cases created before v1.13. Existing residents receive a baseline starter inventory on the first post-upgrade save; `ACQUIRED_AT_MIGRATION` indicates an initialization, not a historical purchase. No old case is linked to these objects retroactively.

Inventory state and the property change journal are committed within the same database transaction as cases, funds, relationships, and eyewitness observations. The in-memory `Town.copy()` also deep-copies items so an unsuccessful candidate action cannot corrupt the active save.

## CLI developer diagnostics

```text
inventory resident-0001
item wallet-resident-0001
property-events 20
```

**Pending work:** privately discoverable evidence, lawful gift/trade transactions, storage and drops, borrowing physical objects, police seizures and returns, and physical routes/rooms. Only wallets can presently be stolen through the autonomous incident system. No paid AI requests occur during property simulation.
