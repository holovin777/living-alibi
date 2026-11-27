# Social Network (v1.5)

This milestone gives each pair of residents **its own persistent relationship**. It does not prepopulate every possible pair: a pair begins existing when the people first meet in a shared scene. All text and systems remain English-only.

## Requirements for meeting

- Two residents must be **alive, awake and not travelling**.
- Both must be in **exactly the same scene**. Separate private units within a single residential area are different scenes; public locations such as the library and market are shared scenes.
- Residents must have spent at least ten game minutes together before an approach attempt can succeed.
- At most one approach roll per pair per game day; at most one successful conversation per pair per day. A failed approach does not generate a conversation.
- Interactions update affinity, trust and tension; conversations are logged as world facts and observed only by physically co-present witnesses.

## Autonomous workplace conversations

Co-present, employed NPCs in the same occupation and workplace can initiate
a work conversation during their shifts. Sick, travelling and off-duty
workers do not qualify. Shared duties modestly increase the existing
approach chance; the same per-pair daily and encounter limits still apply.
Topics depend on occupation, fatigue, carefulness, sociability and trust:
work orders, code review, quality checks, workload, breaks and jokes.
Existing tension can produce disagreements or reconciliation. These
conversations update the same persisted relationship and witness journal.
No new database schema or client commands are required.

This is a rule-based journal event, not a scheduled break, actual task
transfer, spoken exchange or exact employer roster. Matching job and place
is a conservative proxy until every NPC has an employer ID.

## Data storage

`town_relationships` stores `first_id`, `second_id`, affinity, trust, tension, encounters, conversations, arguments, RNG state and timing. SQLite migrations add `last_seen_tick`, `meeting_since_tick`, and `last_attempt_day` without dropping old columns or rows. First and second IDs for generated pairs use lexicographically sorted stable IDs. The original (`marco`, `elena`) relationship remains in its original row for backward compatibility.

`social_interactions` holds conversations as distinct history records. `town_events` holds authoritative event text, while `town_observations` only contains observations from the matching public/private scene.

## CLI commands

```text
relationships 20
relationship marco elena
relationship resident-0001 resident-0002
social-events 20
known resident-0001 20
```

## Limitations and next steps

- Interaction text is a compact rule-generated description, **not spoken dialogue** or a transferable fact.
- Social influence on generated residents' future schedules and goals is not implemented.
- Social state is currently undirected (one pair state), although later beliefs and opinions should be directional.
- Work on actual memory, beliefs, testimony, deception and evidence must follow before a real detective can investigate.
