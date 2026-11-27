#!/usr/bin/env bash
set -Eeuo pipefail
ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"
if command -v godot >/dev/null 2>&1; then GODOT=godot
elif command -v godot4 >/dev/null 2>&1; then GODOT=godot4
else echo "Godot 4 required."; exit 1; fi
NEEDS_IMPORT=false
if [[ -s data/vigevano.json ]]; then
    if ! python3 -c 'import json; assert json.load(open("data/vigevano.json"))["schema"] == 2' 2>/dev/null; then NEEDS_IMPORT=true; fi
fi
if [[ ! -s data/vigevano.json || "$NEEDS_IMPORT" == true || "${1:-}" == "--refresh" ]]; then
    command -v python3 >/dev/null || { echo "Python 3 required."; exit 1; }
    if [[ -s data/vigevano.osm && "${1:-}" != "--refresh" ]]; then
        python3 tools/import_vigevano.py
    else
        python3 tools/import_vigevano.py --download
    fi
fi
if [[ "${1:-}" == "--preview-only" ]]; then
    exec "$GODOT" --path "$ROOT/godot" res://scenes/vigevano.tscn -- "--city-map=$ROOT/data/vigevano.json"
fi
for cmd in python3 java mvn curl; do
    command -v "$cmd" >/dev/null || { echo "$cmd required for the shared Java world."; exit 1; }
done
if curl -fsS --max-time 1 http://127.0.0.1:8080/api/v1/world >/dev/null 2>&1 || curl -fsS --max-time 1 http://127.0.0.1:8091/geo/nearby >/dev/null 2>&1; then
    echo "Living Alibi already running. Close the other server first."; exit 1
fi
python3 tools/export_geo_population.py data/vigevano.json data/vigevano-graph.tsv
echo "Building shared Java world and Spring API..."
mvn -q -DskipTests install
mvn -q -f server/pom.xml -DskipTests package
java -jar server/target/living-alibi-server-1.16.0.jar --server.port=8091 --server.address=127.0.0.1 --living-alibi.database="$ROOT/data/world.db" --living-alibi.geo-graph="$ROOT/data/vigevano-graph.tsv" >data/vigevano-server.log 2>&1 &
SERVER_PID=$!
cleanup(){ kill "$SERVER_PID" 2>/dev/null || true; wait "$SERVER_PID" 2>/dev/null || true; }
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
READY=false
for _ in {1..60}; do
    if curl -fsS --max-time 2 http://127.0.0.1:8091/geo/nearby >/dev/null 2>&1; then READY=true; break; fi
    kill -0 "$SERVER_PID" 2>/dev/null || break
    sleep 1
done
if [[ "$READY" != true ]]; then
    echo "Shared Java world failed."; tail -n 20 data/vigevano-server.log; exit 1
fi
"$GODOT" --path "$ROOT/godot" res://scenes/vigevano.tscn -- "--city-map=$ROOT/data/vigevano.json" --geo-world
