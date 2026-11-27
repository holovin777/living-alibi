#!/usr/bin/env bash
# Living Alibi: one-command local preview for Linux / Arch Linux.
# Keeps the existing SQLite world; never resets saves.
set -Eeuo pipefail

ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
MODE="${1:-play}"

usage() {
    cat <<'EOF'
Living Alibi — local Godot preview
Usage:
  bash play.sh                 Build Java + Spring, start server, open Godot.
  bash play.sh --check         Check required installed programs.
  bash play.sh --server-only   Build and run only the local Spring server.
  bash play.sh --help          Show this help.

Requires: JDK 21+, Maven 3.9+, curl and (for the game window) Godot 4.
Saved world: data/world.db (kept between launches; never erased).
No API key or cloud server is needed for the current offline demo.
EOF
}

case "$MODE" in
    play|--check|--server-only|--help) ;;
    *) usage >&2; echo "Unknown option: $MODE" >&2; exit 2 ;;
esac
if [[ $# -gt 1 ]]; then
    echo "Only one option is supported." >&2
    exit 2
fi
if [[ "$MODE" == "--help" ]]; then usage; exit 0; fi

require() {
    if ! command -v "$1" >/dev/null 2>&1; then
        echo "Missing '$1'. Install the dependency before running the game." >&2
        exit 1
    fi
}

require java
require mvn
require curl

GODOT=""
if [[ "$MODE" != "--server-only" ]]; then
    if command -v godot >/dev/null 2>&1; then
        GODOT="$(command -v godot)"
    elif command -v godot4 >/dev/null 2>&1; then
        GODOT="$(command -v godot4)"
    else
        echo "Godot 4 is missing. On Arch Linux: sudo pacman -S godot" >&2
        exit 1
    fi
fi

if [[ "$MODE" == "--check" ]]; then
    echo "Dependencies found: Java, Maven, curl, Godot ($GODOT)."
    java -version 2>&1 | head -n 1
    "$GODOT" --version
    echo "World path: $ROOT/data/world.db"
    exit 0
fi

cd "$ROOT"
if [[ ! -f "$ROOT/godot/project.godot" || ! -f "$ROOT/server/pom.xml" ]]; then
    echo "This directory does not contain the complete Living Alibi game." >&2
    exit 1
fi

if curl -fsS --max-time 2 http://127.0.0.1:8080/api/v1/world >/dev/null 2>&1 || curl -fsS --max-time 2 http://127.0.0.1:8091/geo/nearby >/dev/null 2>&1; then
    echo "A Living Alibi server already answers on port 8080." >&2
    echo "Close the other server before launching another instance." >&2
    exit 1
fi

echo "Building the Java world engine..."
mvn -q -DskipTests install
echo "Building the Spring Boot game server..."
mvn -q -f server/pom.xml -DskipTests package

JAR="$ROOT/server/target/living-alibi-server-1.16.0.jar"
if [[ ! -f "$JAR" ]]; then
    echo "Spring Boot did not produce the expected game server jar: $JAR" >&2
    exit 1
fi

mkdir -p "$ROOT/data"
SAVE="$ROOT/data/world.db"

if [[ "$MODE" == "--server-only" ]]; then
    echo "Game API: http://127.0.0.1:8080/api/v1/world"
    echo "Save: $SAVE"
    exec java -jar "$JAR" --living-alibi.database="$SAVE"
fi

SERVER_PID=""
cleanup() {
    if [[ -n "$SERVER_PID" ]] && kill -0 "$SERVER_PID" 2>/dev/null; then
        echo "Closing local game server..."
        kill "$SERVER_PID" 2>/dev/null || true
        wait "$SERVER_PID" 2>/dev/null || true
    fi
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

echo "Starting local world (save: $SAVE)..."
java -jar "$JAR" --living-alibi.database="$SAVE" >"$ROOT/data/server.log" 2>&1 &
SERVER_PID="$!"

READY=false
for _ in {1..60}; do
    if curl -fsS --max-time 2 http://127.0.0.1:8080/api/v1/world >/dev/null 2>&1; then
        READY=true
        break
    fi
    if ! kill -0 "$SERVER_PID" 2>/dev/null; then
        break
    fi
    sleep 1
done

if [[ "$READY" != true ]]; then
    echo "The game server did not start. Log: $ROOT/data/server.log" >&2
    tail -n 30 "$ROOT/data/server.log" >&2 || true
    exit 1
fi

# Godot needs to build its local import cache on the very first run.
if [[ ! -d "$ROOT/godot/.godot" ]]; then
    echo "Preparing Godot assets on first launch..."
    "$GODOT" --headless --path "$ROOT/godot" --editor --quit
fi

echo "Opening Living Alibi..."
echo "WASD/arrows = walk; click scene = mouse look; first-person only; Esc = cursor."
echo "E at yellow gate = travel; F at blue kiosk = actions."
echo "Close the Godot window to stop the server. The SQLite save is preserved."
"$GODOT" --path "$ROOT/godot"
