# Recovery & Chain of Custody (v1.14)

## Authority vs detective knowledge

The engine alone knows the actual wallet's legal owner and current holder. An investigation case records a private `physicalItemId` for post-v1.13 thefts. The detective interface does **not** reveal that ID before finding the item. Developers can inspect authoritative item state with `inventory`, `item`, and `property-events`, but should not use those commands for a fair-play case.

## Search procedure

1. `inspect <case-id>` records a scene examination.
2. `interview <resident-id> <case-id>` collects distinct statements. A search needs at least two interviews including the search target.
3. `search <case-id> <resident-id>` proceeds only if an eyewitness has named the resident or the resident was physically present with a jacket color matching the ambiguous fiber evidence.
4. No more than three distinct **authorized** searches per case. Denied searches are free. Prior results are retained and replays are idempotent.
5. If the case-linked wallet is physically held by that resident, seize the **same** object, move it into `evidence:<case-id>`, and record `SEIZED` in the append-only chain of custody.
6. Solve the case with `accuse`, then `return-item <case-id>` to release seized property. It preserves legal title and logs a single `RETURNED` change.

A negative search never proves innocence. The culprit may have passed the wallet elsewhere in future versions; the present version does not simulate informal transfers of stolen property. The player's search is currently instant; it does not claim to model travel or legal warrant standards.

## Persistence and compatibility

`investigation_searches(case_id,resident_id,result)` and the `investigation_cases.returned` flag are additive. It begins empty for cases from v1.13 and older. The existing `property_items` row changes holder/stored space, while `property_changes` records custody transitions. A returned case stays historically marked as returned even if its same wallet is stolen in a later incident. No historic case gets new physical clues or item identities. An older physical case can be searched if its stolen item is still available to recover; an even older pre-v1.13 case cannot.

## Repeat victim prevention

Before selecting a victim for **future** emergent incidents, the engine checks whether the same victim was reported within the last 14 game days. This affects only new opportunities and keeps all earlier case records untouched. A different eligible resident may still be a victim.

## Possible next steps

- Make stolen possessions circulate between holders via voluntary exchange, fencing or concealed home storage, with witnessable transfers.
- Add lawful searches, physical travel, proof of possession, and conflicting witness claims that are not derived from hidden truth.
- Introduce emergent group authority, intimidation and debts, without stereotypes or deterministic criminal status based on disability, poverty or dependency.
- Keep Godot as a client after the authoritative life engine and investigative rules are mature.
