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
exec "$GODOT" --path "$ROOT/godot" res://scenes/vigevano.tscn -- "--city-map=$ROOT/data/vigevano.json"
