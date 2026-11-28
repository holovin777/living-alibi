#!/usr/bin/env bash
# Idempotent asset installation. No sudo, no changes to the OS or saves.
set -euo pipefail
ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
DEST="$ROOT/godot/assets/vehicles/realistic"
if [[ "${LIVING_ALIBI_SKIP_VEHICLE_DOWNLOAD:-0}" == 1 ]]; then
    echo "Vehicle downloads disabled; using local assets."
    exit 0
fi
mkdir -p "$DEST"
timeout=120
connect=15
retries=2
if [[ "${1:-}" == --auto ]]; then
    timeout=30
    connect=5
    retries=0
fi
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
    if any("uri" in x and not x["uri"].startswith("data:") for x in doc.get("buffers", []) + doc.get("images", [])):
        raise SystemExit("External GLB dependencies are not allowed")
PY
}
tmp=""
trap 'if [[ -n "$tmp" ]]; then rm -f -- "$tmp"; fi' EXIT
failed=0
for name in sport-sedan hot-hatchback luxury-suv city-van minibus city-bus box-truck; do
    file="$DEST/$name.glb"
    if [[ -f "$file" ]] && validate "$file"; then
        continue
    fi
    case "$name" in
        city-van) url="https://vinish-models.pages.dev/vans/city-van.glb" ;;
        minibus|city-bus) url="https://vinish-models.pages.dev/buses/$name.glb" ;;
        box-truck) url="https://vinish-models.pages.dev/trucks/$name.glb" ;;
        *) url="https://models.vinish.dev/realistic-cars/$name.glb" ;;
    esac
    tmp="$(mktemp "$DEST/.$name.XXXXXX")"
    echo "Downloading $name (vinish.dev, CC BY 4.0)..."
    if curl --fail --location --proto '=https' --proto-redir '=https' --retry "$retries" --connect-timeout "$connect" --max-time "$timeout" --max-filesize 25000000 --silent --show-error "$url" -o "$tmp" && validate "$tmp"; then
        mv -f -- "$tmp" "$file"
        echo "Installed: $name"
    else
        rm -f -- "$tmp"
        echo "Could not install $name; keeping bundled fallback." >&2
        failed=1
    fi
    tmp=""
done
exit "$failed"
