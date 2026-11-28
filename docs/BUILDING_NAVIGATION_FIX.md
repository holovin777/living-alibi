# Building navigation
The geographic client now waits for streamed physics colliders before
choosing a clear exterior spawn after leaving a building. It closes the
interaction menus and returns mouse capture. Interior change is not followed
by a competing generic spawn in the same snapshot. Linked adult public
interiors are explicitly opened and closed by the server; they no longer
automatically reappear solely because the resident's district has an
interior. Home stays tied to the authoritative home state. The player
must still use permitted travel to change districts as a linked adult.
Legacy park navigation uses a deterministic Magnolia facade for older
snapshots without a home-unit key and checks exterior walkability on exit.
A regression test checks explicit linked public room projection.
