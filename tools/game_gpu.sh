#!/usr/bin/env bash
# Source after selecting GODOT. Only the interactive renderer uses PRIME.
# System GPU by default; explicit prime-run from the caller is inherited.
# system: use the normal display GPU. No driver installation or sudo.
configure_game_gpu() {
    GODOT_COMMAND=("$GODOT")
    local mode="${LIVING_ALIBI_GPU:-system}"
    local selected=system
    case "$mode" in
        auto)
            if [[ "$(uname -s)" == Linux ]] && { command -v prime-run >/dev/null 2>&1 || { command -v nvidia-smi >/dev/null 2>&1 && nvidia-smi -L >/dev/null 2>&1; }; }; then
                selected=nvidia
            fi
            ;;
        nvidia)
            if [[ "$(uname -s)" != Linux ]]; then
                echo "Explicit PRIME offload is supported on Linux only. Use LIVING_ALIBI_GPU=system." >&2
                return 2
            fi
            selected=nvidia
            ;;
        system) ;;
        *) echo "Unknown LIVING_ALIBI_GPU=$mode (auto, nvidia, system)." >&2; return 2 ;;
    esac
    if [[ "$selected" == nvidia ]]; then
        if command -v prime-run >/dev/null 2>&1; then
            GODOT_COMMAND=(env "LIVING_ALIBI_SHADOWS=${LIVING_ALIBI_SHADOWS:-1}" prime-run "$GODOT")
        else
            GODOT_COMMAND=(env "LIVING_ALIBI_SHADOWS=${LIVING_ALIBI_SHADOWS:-1}" __NV_PRIME_RENDER_OFFLOAD=1 __GLX_VENDOR_LIBRARY_NAME=nvidia __VK_LAYER_NV_optimus=NVIDIA_only "$GODOT")
        fi
        echo "Graphics: NVIDIA PRIME requested. Verify NVIDIA in the Godot startup log."
        echo "No driver installation is performed. LIVING_ALIBI_GPU=system opts out."
    else
        if [[ "${__NV_PRIME_RENDER_OFFLOAD:-0}" == 1 ]]; then
            GODOT_COMMAND=(env "LIVING_ALIBI_SHADOWS=${LIVING_ALIBI_SHADOWS:-1}" "$GODOT")
            echo "Graphics: inherited NVIDIA PRIME request."
        elif [[ -n "${DRI_PRIME:-}" && "${DRI_PRIME}" != 0 ]]; then
            echo "Graphics: inherited Mesa DRI_PRIME=$DRI_PRIME request. Verify the Godot device."
        else
            echo "Graphics: system default GPU."
        fi
    fi
    case "${LIVING_ALIBI_RENDERER:-compatibility}" in
        compatibility) GODOT_COMMAND+=(--rendering-method gl_compatibility) ;;
        mobile) GODOT_COMMAND+=(--rendering-method mobile) ;;
        forward_plus) GODOT_COMMAND+=(--rendering-method forward_plus) ;;
        *) echo "Unknown LIVING_ALIBI_RENDERER (compatibility, mobile, forward_plus)." >&2; return 2 ;;
    esac
}
