#!/usr/bin/env bash
set -Eeuo pipefail
ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
if command -v godot >/dev/null 2>&1; then GODOT=godot
elif command -v godot4 >/dev/null 2>&1; then GODOT=godot4
else echo "Godot 4 required."; exit 1; fi
source "$ROOT/tools/game_gpu.sh"
configure_game_gpu
REGION=central
REFRESH=false
PREVIEW=false
for arg in "$@"; do
    case "$arg" in
        --wide) REGION=wide ;;
        --refresh) REFRESH=true ;;
        --preview-only) PREVIEW=true ;;
        *) echo "Usage: bash play.sh --map [--wide] [--refresh] [--preview-only]" >&2; exit 2 ;;
    esac
done
MAP="data/vigevano"
if [[ "$REGION" == wide ]]; then MAP="data/vigevano-wide"; fi
NEEDS_IMPORT=false
if [[ -s "$MAP.json" ]]; then
    if ! python3 -c 'import json,sys; d=json.load(open(sys.argv[1])); assert d["schema"] == 2 and d.get("detail_version",0) == 8 and d.get("region") == sys.argv[2]' "$MAP.json" "$REGION" 2>/dev/null; then NEEDS_IMPORT=true; fi
fi
if [[ ! -s "$MAP.json" || "$NEEDS_IMPORT" == true || "$REFRESH" == true ]]; then
    command -v python3 >/dev/null || { echo "Python 3 required."; exit 1; }
    if [[ -s "$MAP.osm" && "$REFRESH" != true ]]; then
        python3 tools/import_vigevano.py --region "$REGION" --input "$MAP.osm" --output "$MAP.json"
    else
        python3 tools/import_vigevano.py --region "$REGION" --input "$MAP.osm" --output "$MAP.json" --download
    fi
fi
"$GODOT" --headless --path "$ROOT/godot" --import
if [[ "$PREVIEW" == true ]]; then
    exec "${GODOT_COMMAND[@]}" --path "$ROOT/godot" res://scenes/vigevano.tscn -- "--city-map=$ROOT/$MAP.json"
fi
for cmd in python3 java mvn curl; do
    command -v "$cmd" >/dev/null || { echo "$cmd required for the shared Java world."; exit 1; }
done
if curl -fsS --max-time 1 http://127.0.0.1:8080/api/v1/world >/dev/null 2>&1 || curl -fsS --max-time 1 http://127.0.0.1:8091/geo/nearby >/dev/null 2>&1; then
    echo "Living Alibi already running. Close the other server first."; exit 1
fi
python3 tools/export_geo_population.py "$MAP.json" "$MAP-graph.tsv"
echo "Building shared Java world and Spring API..."
mvn -q -DskipTests install
mvn -q -f server/pom.xml -DskipTests package
java -jar server/target/living-alibi-server-1.16.0.jar --server.port=8091 --server.address=127.0.0.1 --living-alibi.database="$ROOT/data/world.db" --living-alibi.geo-graph="$ROOT/$MAP-graph.tsv" >data/vigevano-server.log 2>&1 &
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
"${GODOT_COMMAND[@]}" --path "$ROOT/godot" res://scenes/vigevano.tscn -- "--city-map=$ROOT/$MAP.json" --geo-world
