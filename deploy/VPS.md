# Private VPS preview (not multiplayer yet)

The Spring API still represents ONE playable resident. Do not share access with
other players or expose port 8080 to the public internet. It has no account
authentication or per-player authorization yet. Use SSH forwarding only.

## Server

Install JDK 21+ and Maven 3.9+, clone the repository on a private Linux VPS,
and run:

    mvn -q -DskipTests install
    mvn -q -f server/pom.xml -DskipTests package

For a temporary foreground test, use bash play.sh --server-only. The process
must remain running for continuous simulation.

For a permanent installation, place the built repository at /opt/living-alibi
(readable by a dedicated non-login service account named living-alibi).
Create that account/group using your distro tools. Copy deploy/living-alibi.service
to /etc/systemd/system/, then run:

    sudo systemctl daemon-reload
    sudo systemctl enable --now living-alibi
    sudo journalctl -u living-alibi -f

The service saves to /var/lib/living-alibi/world.db. It does not import your local
save automatically. Back up a stopped database before copying/migrating it.
The provided heap is a starting point; benchmark and adjust for the VPS RAM.

## Connect from a computer

Open a separate terminal and keep the SSH tunnel running:

    ssh -N -L 127.0.0.1:8080:127.0.0.1:8080 USER@VPS_HOST

Then in your local repository, on the same revision as the server:

    bash play.sh --client-only

No Godot installation is required on the VPS. Do not open inbound 8080.

## Current limits

The server targets 1:24 time: one game minute per 2.5 real seconds, one
game day per real hour, whether the client is connected or not. After 30
seconds without requests, it releases manual routine. The client refreshes
instead of advancing time, and explicit wait actions are rejected. The
clock catches up in bounded batches when the engine is busy.

The existing synchronous travel, work and sleep actions can still advance the
world ahead of the target. Those minutes are credited against future clock
ticks, not added twice. Consequently instantaneous jumps and pauses are still
possible; true multiplayer needs timed individual actions instead. A stopped
VPS does not simulate downtime. The local single-player clock is unchanged.
Existing player partnerships can have children automatically, with no pregnancy
stage. Newborns are ordinary persistent residents and remain linked to both
parents. Future multiplayer needs authenticated account-to-resident mapping,
atomic one-time birth assignment, independent sessions and authoritative time.
Never let a second client control this same player simultaneously.
