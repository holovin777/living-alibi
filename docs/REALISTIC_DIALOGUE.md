# Realistic dialogue — initial implementation

The chat's **Parlo / I speak** selector declares Italian or English separately
from the UI language. Choose the language you are actually typing. NPCs do not
automatically learn languages when you change menus. Quick actions use the
declared language but still send their existing canonical engine phrases.

`WorldSpeechRegion` defines the local language independently of the interface:

| Region | Native speech locale | Native variety |
| --- | --- | --- |
| Italy (current Vigevano world) | it-IT | Italian |
| United States (future world) | en-US | American English |
| United Kingdom (future world) | en-GB | British English |

All initial Vigevano townspeople have Italian as their native language. Marco
is Italian-only; Elena knows English at B2; other IDs have a stable deterministic
English ability (roughly 35%, B1/B2/C1). English as a foreign language currently
uses the British English convention. The region and level are passed into
Ollama's prompt; dialogue responses carry both the base language and regional
locale. The UI prefers a matching regional voice when installed. The levels
are initial profile metadata and prompt guidance, not a validated language
assessment or a learning mechanic. Only the Italian map exists today. Future
maps must select their own regional policy, and immigrant profiles will require
explicit persistent resident language data. This is a first-pass population model, not
individual language-learning histories. The skills are not public metadata.
Ask in Italian “Parli inglese?” to discover a person's ability. If English is
not understood the NPC answers in Italian and no contact/name disclosure or
transfer is recorded. The dialogue attempt still consumes a minute.

Known intents continue to use the deterministic Java world rules. Free-form
small talk can call Ollama at `http://127.0.0.1:11434/api/chat` using
`qwen2.5:3b`. Set `LIVING_ALIBI_OLLAMA_MODEL` to override the model.
No API key, cloud service or automatic installation is required. A missing
model, unavailable Ollama or a 20-second timeout falls back to existing
dialogue. The model receives only observable activity, sociability, coarse
attitude, and the current/previous player message, never the private world
log. Its output is not executed as an action. It can still produce imperfect
or invented speech.

Italian rule-based replies use existing Godot translation strings regardless
of menu locale; missing translations are displayed without speech, marked as
unavailable. Voice selection follows the actual reply language and requires
an installed platform voice. No microphone recognition or automatic spoken
language identification yet. Existing
REST clients without the language field retain the previous behavior.

## Private conversation memory

Each resident contact stores the last 32 understood dialogue turns in the
existing SQLite contact row, including game day, minute, declared language,
player utterance and NPC reply (engine replies may be in canonical English
before client localization). The migration preserves existing relationships
and legacy last-heard text; it cannot recover conversations never recorded.
NPCs do not receive another resident's history or a previous player's life.
Ununderstood language attempts are not stored.

For Ollama small talk, the prompt includes the last six turns and up to four
older turns with overlapping topic words, with bounded excerpts. Past speech
is not authoritative world truth and cannot execute actions. Explicit questions
such as "Di cosa abbiamo parlato?" and "What did we talk about last time?"
quote the most recent remembered non-recall player utterance without a model.
Existing emotional and social memory rules remain intact.

History applies to the Godot/REST conversation route; the older console
continues its original behavior. Long-term semantic summaries, cross-language
topic matching and unlimited transcripts are not implemented. The model can
still misinterpret a remembered statement. A new player life has no inherited
contact history.

## Selective long-term memory

The same contact row now also holds up to seven durable milestones: the first
recorded meeting, latest introduction, support, hurt, broken trust, apology,
and received aid. Social milestones are captured only when the corresponding
authoritative social-memory date advances; a claim in dialogue alone does
not create a world event. Ordinary recent turns still roll off at 32.
Milestones survive that eviction and restarts. Ollama receives compact dated
milestone excerpts, and "Ti ricordi quando ci siamo conosciuti?" recalls the
recorded first meeting without a model. Old array-format history remains
readable; the first meeting cannot be reconstructed for preexisting contacts.
The first meeting is preserved; other categories keep their latest verified
example. Same-day repeats and semantic summaries remain future work.

## Emotional drift during absence

At midnight the authoritative world clock advances private contact attitudes.
After 14 days without a recorded conversation, affinity moves one point
towards neutral every seven days. After 30 days trust moves one point towards
50 every 30 days; serious unresolved betrayal and overdue debt prevent
automatic recovery of low trust. An ordinary hurt can cease to be unresolved
after 90 days without erasing its event. Serious betrayal still requires
reconciliation. These are initial game-balance rules, not a psychological model.
Conversation resets the absence timer but does not automatically restore trust.
A persisted daily cursor prevents applying the same day twice after reload.
Past names, milestones, debts and other world facts are unaffected.
Only player-contact relationships use this rule; NPC-to-NPC relationships
retain their existing simulation.

## Incoming NPC calls

In the current Italian town, ask a known and trusted adult in person
"Posso darti il mio numero?" (or "Can I give you my number?"). Sharing is
stored for this contact and player incarnation. Calls require consent,
trust of at least 65, affinity of at least 20, and a living, sociable,
free resident. There is at most one new incoming call per game day and
a seven-day cooldown per caller. Calls are available in the visitor
life stage, from 10:00 to 19:00, and expire after five in-game minutes.

The existing phone alert sounds when a call arrives. Press H to open the
phone's Calls screen; answer or decline. A friendly caller may offer
coffee tomorrow at 18:00. Accepting creates a real appointment in the
existing calendar and NPC meeting scheduler. Unanswered calls become
missed; the latest call per contact is persisted. Answering currently
shows a short deterministic Italian line as text, not a continuous
voice conversation. Existing SMS behavior is unchanged. Outgoing calls,
number revocation, and free-form voice or microphone calls remain future
work. All calls are simulated in the game; no real phone numbers needed.

Manual check:
```sh
ollama pull qwen2.5:3b
curl http://127.0.0.1:11434/api/tags
bash play.sh
```

Try speaking to different residents in English and Italian, including with
the interface language set to the opposite language. Check that failure to
understand does not disclose names or make transactions. Stop Ollama and
check that ordinary deterministic dialogue still works.

Offline language unit smoke test:
```sh
mvn -q test-compile
java -ea -cp target/classes:target/test-classes com.holovin.npclife.ResidentLanguagesSmokeTest
```
