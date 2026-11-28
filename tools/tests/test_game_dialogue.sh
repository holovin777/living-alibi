#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
source "$ROOT/tools/game_dialogue.sh"
# Mock local HTTP response; no actual Ollama service/network required.
curl() {
    if [[ "${FAKE_OFFLINE:-0}" == 1 ]]; then return 7; fi
    printf '%s' "$FAKE_TAGS"
}
unset LIVING_ALIBI_OLLAMA_MODEL
FAKE_TAGS='{"models":[{"name":"qwen2.5:7b","model":"qwen2.5:7b"}]}'
configure_game_dialogue
[[ "$LIVING_ALIBI_OLLAMA_MODEL" == qwen2.5:7b ]]
unset LIVING_ALIBI_OLLAMA_MODEL
FAKE_TAGS='{"models":[{"name":"qwen2.5:7b"},{"name":"qwen2.5:3b"}]}'
configure_game_dialogue
[[ "$LIVING_ALIBI_OLLAMA_MODEL" == qwen2.5:3b ]]
LIVING_ALIBI_OLLAMA_MODEL=qwen2.5:7b
configure_game_dialogue
[[ "$LIVING_ALIBI_OLLAMA_MODEL" == qwen2.5:7b ]]
LIVING_ALIBI_OLLAMA_MODEL=custom:latest
configure_game_dialogue
[[ "$LIVING_ALIBI_OLLAMA_MODEL" == custom:latest ]]
unset LIVING_ALIBI_OLLAMA_MODEL
FAKE_TAGS='{"models":[{"name":"nomic-embed-text:latest"}]}'
configure_game_dialogue
[[ "$LIVING_ALIBI_OLLAMA_MODEL" == qwen2.5:3b ]]
unset LIVING_ALIBI_OLLAMA_MODEL
FAKE_OFFLINE=1
configure_game_dialogue
[[ "$LIVING_ALIBI_OLLAMA_MODEL" == qwen2.5:3b ]]
echo "Dialogue launcher checks passed."
