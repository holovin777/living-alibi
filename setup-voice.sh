#!/usr/bin/env bash
# Run via source setup-voice.sh to export model in the current shell.
living_alibi_voice_setup() {
    local model="${LIVING_ALIBI_WHISPER_MODEL:-$HOME/.local/share/living-alibi/ggml-base.bin}"
    local missing=() answer pkg tmp
    for pkg in espeak-ng whisper-cpp; do
        if [[ "$pkg" == whisper-cpp ]]; then
            command -v whisper-cli >/dev/null 2>&1 || missing+=("$pkg")
        else
            command -v espeak-ng >/dev/null 2>&1 || missing+=("$pkg")
        fi
    done
    if (( ${#missing[@]} )); then
        if ! command -v pacman >/dev/null || ! command -v sudo >/dev/null; then
            echo "Optional voices: install espeak-ng and whisper-cli using your distribution." >&2
            return 0
        fi
        if [[ ! -t 0 ]]; then return 0; fi
        read -r -p "Install optional voice packages (${missing[*]}) using sudo pacman? [y/N] " answer || return 0
        [[ "$answer" == y || "$answer" == Y ]] || return 0
        sudo pacman -S --needed "${missing[@]}" || { echo "Optional voice installation skipped." >&2; return 0; }
    fi
    if [[ ! -f "$model" ]]; then
        [[ -t 0 ]] || return 0
        read -r -p "Download multilingual Whisper base model (148 MB) for offline microphone input? [y/N] " answer || return 0
        [[ "$answer" == y || "$answer" == Y ]] || return 0
        mkdir -p "$(dirname "$model")" || return 0
        tmp="$(mktemp "${model}.part.XXXXXX")" || return 0
        if curl --fail --location --retry 2 --output "$tmp" 'https://huggingface.co/ggerganov/whisper.cpp/resolve/fb5ed9369c26eaf08392504d8446da431e0fffb9/ggml-base.bin' &&
            printf '%s  %s\n' '60ed5bc3dd14eea856493d334349b405782ddcaf0028d4b5df4088345fba2efe' "$tmp" | sha256sum -c - &&
            mv -f -- "$tmp" "$model"; then
            echo "Offline voice model ready."
        else
            rm -f -- "$tmp"
            echo "Voice model download failed; typed dialogue still works." >&2
            return 0
        fi
    fi
    export LIVING_ALIBI_WHISPER_MODEL="$model"
    export LIVING_ALIBI_WHISPER_CLI="$(command -v whisper-cli)"
}
living_alibi_voice_setup
