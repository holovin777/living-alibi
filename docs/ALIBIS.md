# Evidence & Alibis — v1.9

## Why this version exists

In v1.8, an inspected receipt always displayed the offender's exact initials, and careful witnesses could identify the same person without error. Combined with an example command that happened to name the culprit, this undermined investigation gameplay.

v1.9 removes that perfect clue for **new** cases. Evidence is now ambiguous and the detective can cross-check a person's claimed whereabouts through separate interviews.

## New case generation

- At the exact incident minute, every alive resident who is not traveling receives an internal snapshot entry: a public scene ID or private-unit ID.
- The incident still requires three residents in the same public place, and the culprit is still chosen from their real positions and stable traits.
- A scene carries a fabric color derived from the offender's clothing that day. Colors are common, so a match is only circumstantial. A torn receipt has no readable owner.
- Eyewitnesses may remember someone's proximity to the missing belongings, but their recollections can be incorrect; neither presence nor reluctance is proof.
- This snapshot is simulator reference truth, **not a map displayed to the player**.

## Player commands

```text
cases
case <case-id>
inspect <case-id>
interview <resident-id> <case-id>
check-alibi <case-id> <resident-id>
accuse <case-id> <resident-id>
```

1. Inspect the case. The fabric clue cannot uniquely identify a culprit.
2. Interview a resident to hear where they say they were. Witnesses may describe clothing they wore at the incident.
3. Use `check-alibi`. Without an independent interview, the result is `UNVERIFIED`.
4. Interview a second possible corroborator. In an eligible public scene, a follow-up can confirm having seen the first resident there, giving `CORROBORATED`.
5. A private home is not automatically corroborated by a neighbor in another private unit. Travel snapshots also do not serve as verified alibis.
6. We still allow a case to be accused after inspecting and recording two interviews. The *hidden engine verdict* is not a realistic judicial standard.

A corroborated alibi supports a person being elsewhere at the incident time; an unverified alibi does not prove guilt. Even a correct color or named sighting is not itself proof of theft.

## Save compatibility

SQLite migration adds nullable `investigation_cases.fabric_color` and the `investigation_positions` and `investigation_alibis` tables. Every new case carries its own snapshot; older v1.8 cases have a `NULL` color and no snapshot. `check-alibi` reports that the old location evidence is unavailable rather than guessing historical positions. Old case outcomes and interviews stay in place.

## Tests

- `AlibiSmokeTest`: the fabric clue is ambiguous, third-party corroboration requires separate interviews, private units remain isolated, and old cases lack invented alibis.
- `AlibiDatabaseSmokeTest`: snapshots, interviews, clue colors and verified notes survive restart; a mock v1.8 SQLite schema is migrated without deleting its case.
- Existing investigation and town / population / social / knowledge / deception tests remain in CI.

## Next limitations to address

- A true crime scene needs spatial geometry, chain of custody and physical property objects.
- Witness credibility and clothing are deterministic approximations, not a perception system.
- Interviews and inspections currently happen instantly without traveling or spending investigation time.
- The accusation verdict still compares a chosen ID to the authoritative offender, not to an evidence standard; repeated guesses are not yet penalized.
- Cases still generate at a fixed opportunity sample rather than from complex motives and realistic constraints.
