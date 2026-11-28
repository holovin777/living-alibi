#!/usr/bin/env bash
# Explicit, one-time optional download. No sudo, no changes to the OS or saves.
set -euo pipefail
ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
DEST="$ROOT/godot/assets/vehicles/realistic"
mkdir -p "$DEST"
command -v curl >/dev/null || { echo "curl is required" >&2; exit 1; }
command -v python3 >/dev/null || { echo "python3 is required" >&2; exit 1; }
validate() {
    python3 - "$1" <<'PY'
import json, pathlib, struct, sys
p = pathlib.Path(sys.argv[1])
with p.open("rb") as f:
    header = f.read(12)
    if len(header) != 12:
        raise SystemExit("Truncated GLB")
    magic, version, size = struct.unpack("<4sII", header)
    if magic != b"glTF" or version != 2 or size != p.stat().st_size:
        raise SystemExit("Invalid GLB header/length")
    chunk_length, kind = struct.unpack("<I4s", f.read(8))
    if kind != b"JSON" or chunk_length > size - 20:
        raise SystemExit("Invalid GLB JSON chunk")
    doc = json.loads(f.read(chunk_length))
    if not doc.get("meshes") or not doc.get("scenes"):
        raise SystemExit("Missing GLB meshes/scenes")
PY
}
tmp=""
trap 'if [[ -n "$tmp" ]]; then rm -f -- "$tmp"; fi' EXIT
for name in sport-sedan hot-hatchback luxury-suv; do
    file="$DEST/$name.glb"
    if [[ -f "$file" ]] && validate "$file"; then
        echo "Already installed: $name"
        continue
    fi
    tmp="$(mktemp "$DEST/.$name.XXXXXX")"
    echo "Downloading $name (vinish.dev, CC BY 4.0)..."
    curl --fail --location --retry 2 --connect-timeout 15 --max-time 120 --silent --show-error \
        "https://models.vinish.dev/realistic-cars/$name.glb" -o "$tmp"
    validate "$tmp"
    mv -f -- "$tmp" "$file"
    tmp=""
done
echo "Realistic cars installed. Start Living Alibi normally; Godot will import them."
