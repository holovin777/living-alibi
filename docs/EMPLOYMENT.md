# Employment & Life Events — v1.12

## Scope

Each **procedurally generated** citizen now has an independently persistent employment status (`employed`), number of consecutive unemployed days, last new-hire day, number of career changes and an end date for short sick leave. Marco and Elena retain their earlier bespoke simulation and are not subject to this new subsystem.

Every simulated day at 06:30, living procedural citizens may lose a job, be rehired after at least five unemployed days, or begin a brief illness. The rolls are deterministic for a given resident seed and in-game day, require no real-time clocks or AI calls, and are not guaranteed to occur in small samples. Job search does not pay wages. Re-hiring may update both occupation and workplace. A new job is protected from another layoff for its first 14 days.

Illness lasts two or three simulated days; the resident stays home, stops earning wages during sickness and incurs one healthcare expense. That expense enters the established household bill/ledger system and becomes arrears if unaffordable. Residents with cash can receive basic grocery/water deliveries at home; ill residents without money still face realistic resource pressure. At the end of leave, the person returns to work or resumes job search. This is abstract gameplay, not clinical advice.

## Console diagnostics

```text
career resident-0001
life-events 20
life-events resident-0001 20
simulate 30
career resident-0001
finances resident-0001
ledger resident-0001 20
```

`career` shows a snapshot; `life-events` shows an append-only event journal (`JOB_LOSS`, `HIRED`, `SICK_LEAVE`, `RECOVERED`, `MEDICAL_EXPENSE`). These commands are **developer-only** and must not be treated as detective evidence. Health/financial records are not pushed to public eyewitness logs. The ordinary economy ledger still tracks bill creation, payment and transfers.

## Upgrade and persistence

- SQLite adds `employed`, `unemployment_days`, `sick_until_day`, `last_hired_day`, `job_changes` to `town_people`, using safe defaults for older worlds; it never manufactures historical layoffs or illness.
- `resident_life_changes` stores only events from the moment v1.12 is active, appended transactionally with world state and financial records. Re-saving without new simulation does not duplicate the events.
- An old `case-0002` or `case-0003` remains unchanged. New financial strain can influence only future motive calculations and new case snapshots.
- Back up `data/world.db` before upgrading, then run the Java and SQLite smoke tests.

## Limitations

No finite job vacancies, company bankruptcies, labor market supply/demand, realistic diagnoses, insurance, hospitals, sick-leave payments, households sharing finances or legal employment rights. Citizens remain simple agents; intermittent loss/re-employment is probabilistic, not an external economic forecast.
