# Realistic dialogue — initial implementation

The chat's **Parlo / I speak** selector declares Italian or English separately
from the UI language. Choose the language you are actually typing. NPCs do not
automatically learn languages when you change menus. Quick actions use the
declared language but still send their existing canonical engine phrases.

All initial townspeople have Italian as their native language. Marco is
Italian-only; Elena also knows English; other IDs have a stable deterministic
English ability (roughly 35%). This is a first-pass population model, not
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
language identification yet; no persistent full chat transcript. Existing
REST clients without the language field retain the previous behavior.

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
