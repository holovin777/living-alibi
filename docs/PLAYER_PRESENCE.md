# Anonymous Visitor — v1.15

## Core design

The player is a **physical visitor**, not the automatically appointed detective and not
one of the procedurally generated NPCs. There is no requirement to take cases or missions.
A world can continue while the player simply walks, waits, or chats.

NPCs do **not** automatically know the player's name from proximity, a global profile,
world simulation, or other residents' player contacts. The visitor may have a private
name that no one has been told. The player can also explicitly give a different alias
to each resident. NPC-specific knowledge stays attached to that NPC; meeting somebody
else does not broadcast identity. The remembered contact survives SQLite restarts.

## Commands (inside the game)

```text
explore
me
locations
look
go cafe
look
speak resident-0001 Hello!
identity Viktor
introduce resident-0001
contacts
contact resident-0001
```

`identity <name>` edits **only the player's private identity**. It never broadcasts it.
`introduce <id> [alias]` discloses a name/alias **only** to that nearby resident.
Without an alias argument, the previously saved private identity is used.
A sentence beginning `My name is ` in `speak <id> My name is ...` is also an
explicit disclosure; all other dialogue keeps the player's name unknown.

`go <place>` takes simulated walking time (8–10 minutes). `look` lists only
residents sharing the visitor's **public** scene. A street outside a residential
building is not access to the private apartments. `speak` and `introduce` work
only when the resident is in the same public scene, alive and available. Each
rule-based `speak` takes three game minutes, and `introduce` takes two.
`wait` and `live` also advance the town normally. There is no game-over for
ignoring cases, and `explore` does not turn off autonomous NPC events.

## Persistence and privacy

The additive migration creates `player_state` (current public place,
destination, travel time, optional **private** name) and `player_contacts`
(one row per NPC that actually had a conversation/introduction with the visitor,
including `known_name`, conversation count, time and last heard statement).
Nothing creates a known name for every NPC. No NPC receives any secret
investigator-only data from these tables. The player's state is copied and saved
transactionally with the world simulation, using the existing save API.

**Legacy information caveat:** Existing v1.14 `memories` and `character_facts`
explicitly told to Marco remain intact after upgrade; previously revealed facts
are **not forgotten or erased** by the new visitor model. Fresh player contacts
start anonymous with no fabricated historical introductions. The old `talk` and
`remember` commands now require Marco to be physically nearby; `remember` is an
explicit statement made to Marco, and a `My name is ...` fact is an introduction.
The old direct Marco chat still has optional paid OpenAI support. `speak` with
any resident uses offline rule-based responses and makes **no API requests**.

## Not yet implemented

The visitor has physical location and persistent social introductions, but no
full hunger/sleep/job/household simulation, no 3D movement, no free-text AI for
all NPCs, no autonomous NPC-initiated player conversations, and no complete
long-term dialogue history beyond the latest heard statement. Detective and
developer commands remain in the console for testing; they are always optional
and currently are not access-controlled by a separate role. A later release
should provide a genuine immersive play mode that hides global debug information.
