Living Alibi — portable x64 desktop preview

Extract the complete archive first. On Windows double-click Play.cmd.
On Linux run ./play.sh from the extracted folder (or bash play.sh).
Do not start LivingAlibi.exe directly: the launcher starts the local Java world.

Java, Maven and Godot editor are not required on the player's computer.
A compatible 64-bit desktop OS, graphics/audio drivers and standard OS libraries are required.

Saves and logs:
 Windows: %LOCALAPPDATA%\LivingAlibi
 Linux: $XDG_DATA_HOME/living-alibi or ~/.local/share/living-alibi
The game creates world.db on the first run. Updating the extracted game
does not replace that database. Back up the save before upgrading.
Existing development saves in data/world.db are not migrated automatically.

The game is a development preview. No external server or API key is needed.
The package is unsigned. Voice dependencies are not bundled in this initial
portable packaging change. See the repository for current feature status.
