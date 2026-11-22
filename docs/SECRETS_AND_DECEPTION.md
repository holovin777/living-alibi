# Secrets & Deception (v1.7)

This is a **game-rule prototype**, not a general lie detector or a natural-language motive model. No OpenAI requests are made during autonomous simulation.

## Three separate layers

1. **World truth** (`town_events`): authoritative simulated events. NPCs cannot change it by saying something.
2. **Personal knowledge** (`resident_beliefs`): the resident's `DIRECT` observations or `HEARD` claims. These can contradict objective truth; confidence expresses the game's *subjective* evidence score, not factual probability.
3. **Private motives and actions** (`resident_secrets`, `deception_decisions`): selected sensitive firsthand experiences that an involved resident wants to protect, and the decision to speak truthfully, withhold, or lie. Only developer diagnostics reveal which decisions were dishonest.

## Rule-based mechanics

- When a resident directly witnesses selected noteworthy events involving them (an encounter, argument, death or theft-related report), a reproducible small fraction are privately marked as sensitive. A secret is about an **actual observed event**, not a fabricated world event.
- A speaker can share any other known claim normally. When sharing **their own secret** they choose based on trust and carefulness: high trust permits a truthful account; low trust causes withholding or, for a careful speaker, an intentional lie.
- A deliberate false claim changes the **reported statement**, not the original `town_events` row, eyewitness log or event key.
- A recipient stores a lie as ordinary `HEARD` information with the immediate speaker as the source. The recipient does *not* automatically know they were deceived. The hidden `deception_decisions` audit is accessible only in developer mode.
- If a resident subsequently observes the same event directly, the `DIRECT` account replaces the false `HEARD` account. The SQLite upsert now updates the account's **statement text** as well as its provenance.
- Secrets and deception history survive restarts through additive tables. Previously existing v1.6 beliefs and historical events are preserved.

## Developer CLI

```text
secrets marco 20
secrets resident-0001 20
deceptions 20
rumors 20
beliefs resident-0001 20
town-events 20
```

`secrets` displays protected event keys (not automatically the underlying event text). `deceptions` lists intentional `WITHHELD` / `LIE` actions for developer debugging. `rumors` displays claims as heard by recipients, even if false. These commands **must not be available to the eventual player detective**, who should investigate and interview to learn what happened.

## Limitations

- No autonomous theft, murders, generated crimes, forensic evidence, false alibi construction, interviews or police cases yet.
- Sensitive event selection and lie/withhold behavior are simplified deterministic rules. There are no complex motives, fear of prosecution, contradictory testimonies, psychological lying styles or persistent deception plans yet.
- Only one claim may be transmitted per qualifying conversation. Secrets can crowd out other recent claims; conversation content is still formulaic.
- Legacy historical v1.6 direct observations are **not retroactively** classified as secrets.
- The `event_key` is derived from event contents, not a globally unique database identity. Strong case/evidence IDs should come before full detective mechanics.
