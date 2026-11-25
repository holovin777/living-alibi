extends RefCounted
## Deterministic presentation traits: same resident ID, same style across restarts.
const TYPES := ["joker","quiet","perfectionist","optimist","pessimist","relaxed","talkative"]
const GREETINGS := {
    "joker":"Hello! Don't worry, I'm almost working.",
    "quiet":"Hello.",
    "perfectionist":"One moment, let me finish this.",
    "optimist":"We can make today a good day.",
    "pessimist":"Something will probably go wrong.",
    "relaxed":"Is it break time yet?",
    "talkative":"Have you heard the latest?"
}
static func style(id: String) -> String:
    return TYPES[id.hash() % TYPES.size()]

static func greeting(style_id: String) -> String:
    return GREETINGS.get(style_id,"Hey! Good to see you!")
