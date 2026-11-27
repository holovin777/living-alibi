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

After 30 seconds without client requests, the server releases manual routine
and advances a game minute per idle tick (10 seconds by default, plus processing).
Connected clients retain action-driven time; this is not yet a unified
multiplayer clock. Local client sends a heartbeat to prevent false absence.
Existing player partnerships can have children automatically, with no pregnancy
stage. Newborns are ordinary persistent residents and remain linked to both
parents. Future multiplayer needs authenticated account-to-resident mapping,
atomic one-time birth assignment, independent sessions and authoritative time.
Never let a second client control this same player simultaneously.
