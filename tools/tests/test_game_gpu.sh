#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
source "$ROOT/tools/game_gpu.sh"
GODOT=/example/godot
unset __NV_PRIME_RENDER_OFFLOAD LIVING_ALIBI_GPU
LIVING_ALIBI_RENDERER=compatibility
configure_game_gpu
[[ "${GODOT_COMMAND[*]}" == "/example/godot --rendering-method gl_compatibility" ]]
LIVING_ALIBI_GPU=system
LIVING_ALIBI_RENDERER=compatibility
configure_game_gpu
[[ "${GODOT_COMMAND[*]}" == "/example/godot --rendering-method gl_compatibility" ]]
LIVING_ALIBI_RENDERER=mobile
configure_game_gpu
[[ "${GODOT_COMMAND[*]}" == "/example/godot --rendering-method mobile" ]]
LIVING_ALIBI_RENDERER=invalid
if configure_game_gpu; then exit 1; fi
LIVING_ALIBI_RENDERER=compatibility
LIVING_ALIBI_GPU=invalid
if configure_game_gpu; then exit 1; fi
if [[ "$(uname -s)" == Linux ]]; then
    # Simulate prime-run without requiring an NVIDIA device on CI.
    prime-run() { :; }
    unset LIVING_ALIBI_GPU
    configure_game_gpu
    [[ "${GODOT_COMMAND[*]}" == "/example/godot --rendering-method gl_compatibility" ]]
    __NV_PRIME_RENDER_OFFLOAD=1
    configure_game_gpu
    [[ "${GODOT_COMMAND[*]}" == *"LIVING_ALIBI_SHADOWS="*"/example/godot --rendering-method gl_compatibility" ]]
    unset __NV_PRIME_RENDER_OFFLOAD
    LIVING_ALIBI_GPU=auto
    configure_game_gpu
    [[ "${GODOT_COMMAND[*]}" == *"prime-run /example/godot --rendering-method gl_compatibility" ]]
    unset -f prime-run
    LIVING_ALIBI_GPU=nvidia
    configure_game_gpu
    [[ "${GODOT_COMMAND[*]}" == *prime-run* || "${GODOT_COMMAND[*]}" == *__NV_PRIME_RENDER_OFFLOAD=1* ]]
fi
echo "GPU launcher checks passed"
