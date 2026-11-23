# Living Alibi v1.10 — Motives & Opportunity

## Goal

Previously a theft was scheduled at 17:30 every seventh game day, then the offender was chosen mostly by carefulness. This produced predictable crimes without any personal causal pressure. v1.10 makes a modest rule-based improvement.

## How incidents emerge

- At sparse daytime windows (one opportunity roll every 13 minutes from 11:00 to 19:30), the simulation considers living, awake residents at the **same public scene**. Three or more must be present.
- For each eligible offender/victim pair, the model evaluates the offender's need for money, their relationship tension with that victim, the victim/offender wealth gap, and personality-linked impulsivity.
- A sufficiently strong combination permits a probabilistic decision. A deterministic hash avoids unreproducible results for the same saved world; the decision does not depend on an OpenAI API call.
- At most one reported theft can occur within any three-day window. The same person cannot offend again for ten days. Those limitations are for gameplay pacing, not claims about real-world crime.
- The victim loses EUR 15 and the culprit gains EUR 15, as before. A wallet incident, its participants, and all contemporary witness positions are saved.

## Motives are hidden; backgrounds are leads

The engine privately records the strongest of four motive categories: `FINANCIAL_PRESSURE`, `PERSONAL_GRIEVANCE`, `ENVY`, `IMPULSIVE_TEMPTATION`. A strong computed motive is **not proof of guilt**. It is not disclosed by the detective CLI.

The detective may ask for historical **background indicators** after inspecting the scene and interviewing a particular resident:

```text
cases
case case-0004
inspect case-0004
interview resident-0001 case-0004
background case-0004 resident-0001
check-alibi case-0004 resident-0001
```

These commands are examples of syntax, not suspect recommendations. `background` shows only `low`, `moderate`, or `elevated` values for three circumstantial factors: financial strain, personal friction with the victim, and impulsivity. They can be similar for innocent residents. There is **no automatic attribution of guilt** and the engine still checks accusation against hidden truth rather than forensic standards.

The background snapshot is recorded for every living resident at the **incident instant**, including those traveling; their location evidence is only recorded if their position is known. It uses pre-theft finances, not later cash balances. The backend saves the hidden motive separately from these publicly accessible indicators.

## SQLite and migration

- `investigation_cases.motive_code TEXT` and `.motive_strength INTEGER` are nullable new columns. Old rows retain `NULL` rather than receiving invented motives.
- `investigation_backgrounds(case_id,resident_id,financial,conflict,impulse)` captures a time-local lead per resident, inserted only once. Pre-v1.10 cases have no corresponding rows.
- Existing case IDs, solutions, interviews, fabric clues, incident-time location snapshots, alibi notes, resident identities, and world history remain untouched.
- `data/world.db` is local and ignored by Git. Always back it up before upgrading.

## Tests

```bash
mvn -q clean test-compile
java -ea -cp target/classes:target/test-classes com.holovin.npclife.MotiveSmokeTest
java -ea -cp target/classes:target/test-classes com.holovin.npclife.MotiveIntegrationSmokeTest
mvn -o -Dexec.executable=java -Dexec.classpathScope=test '-Dexec.args=-ea -cp %classpath com.holovin.npclife.MotiveDatabaseSmokeTest' exec:exec
```

## Still missing

Theft still occurs abstractly, without item inventories, surveillance, paths, or plausible interrogation time budgets. Financial conditions are simple money/need numbers (generated citizens do not yet have debts). Social conflicts are rare in a friendly town. The scene clue and eyewitness system are unchanged from v1.9. No legal evidence threshold or courtroom process exists. A future version can add property ownership, debt/lending, varied crimes, evidence decay, and actual travel for detectives.

## Update in v1.11

The above limitations describe v1.10 as originally released. Since v1.11, generated residents can acquire unpaid household bills and zero-interest personal loans. These balances affect **new** incident-time financial strain; stored v1.10 profiles remain historical snapshots and are not recalculated.
