#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
exec "$ROOT/runtime/bin/java" -jar "$ROOT/launcher.jar" "$@"
