#!/usr/bin/env bash
# Select an already installed supported chat model, never download multi-GB models.
# Explicit user configuration always takes priority.
configure_game_dialogue() {
    local requested="${LIVING_ALIBI_OLLAMA_MODEL:-}"
    local tags model
    if ! tags="$(curl -fsS --max-time 3 http://127.0.0.1:11434/api/tags 2>/dev/null)"; then
        export LIVING_ALIBI_OLLAMA_MODEL="${requested:-qwen2.5:3b}"
        echo "NPC dialogue: Ollama not reachable; scripted replies remain available."
        return 0
    fi
    if [[ -n "$requested" ]]; then
        export LIVING_ALIBI_OLLAMA_MODEL="$requested"
        if printf '%s' "$tags" | grep -Fq "\"$requested\""; then
            echo "NPC dialogue: Ollama model $requested available (configured)."
        else
            echo "NPC dialogue: configured model $requested not found; scripted replies remain available."
        fi
        return 0
    fi
    # Prefer the small model for responsiveness, then the existing larger model.
    # Do not select arbitrary installed embedding, vision or unrelated models.
    for model in qwen2.5:3b qwen2.5:7b qwen2.5:1.5b qwen2.5:latest; do
        if printf '%s' "$tags" | grep -Fq "\"$model\""; then
            export LIVING_ALIBI_OLLAMA_MODEL="$model"
            echo "NPC dialogue: Ollama model $model selected automatically."
            return 0
        fi
    done
    export LIVING_ALIBI_OLLAMA_MODEL=qwen2.5:3b
    echo "NPC dialogue: no supported installed Qwen 2.5 chat model found; scripted replies remain available. Optional: ollama pull qwen2.5:3b"
}
