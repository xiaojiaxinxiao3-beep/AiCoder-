AiCoder Assist for Minecraft 1.8.9
==================================

For the plain game and for 1.8.9 with OptiFine. Not for Forge, and not for other versions.

How to use
  1. Start Minecraft 1.8.9 and wait for the main menu.
  2. Run AiCoderAssist-1.8.9.bat (or AiCoderAssist-1.8.9.exe). A window says "AiCoder Assist is in."
  3. In the game press Right Shift (or Insert) to open the menu.

Java
  1.8.9 runs on Java 8, and the Java 8 that launchers bring along cannot attach to a running game.
  The injector then looks for a full Java on the computer (any JDK, version 9 or newer) and uses that.
  If it finds none it says so: install a JDK (for example Temurin 21) and run it again.

What is in it
  Telly Bridge     the four bridge modes (Telly, Telly Up, Telly Replay, Speed) and Diagonal
  Left Clicker     hold the left button for repeated left clicks
  Right Clicker    hold the right button for repeated right clicks
  Trigger Bot      attacks when the crosshair is on a player in range. 1.8.9 has no weapon cooldown,
                   so "Auto" is a steady ten clicks a second; "Custom" is your own delay in ticks.
  Crosshair Drift  the crosshair drifts slowly towards a player in range
  The menu with both looks (aurora, liquid glass), wallpapers, key binds, and the small games.

What is not in it
  Everything built on things 1.8.9 does not have: maces, spears, wind charges, respawn anchors,
  end crystal macros, totems in the off hand, the elytra dive. Cart Bomb is left out too.
  There is no licence check in this build.

Each module has "Chat Hints" to switch its chat messages off.
Settings are saved in aicoder-assist.properties in the game folder.

What has been tested
  On Linux, plain 1.8.9 and 1.8.9 with OptiFine HD U M5, on a local server: injecting (the game on
  Java 8, the injector handed over to a newer Java), the menu, and 15 second runs of Telly, Telly Up,
  Telly Replay and Speed without a fall. Not tested: Windows, the .exe, public servers, Forge (not
  supported), the clickers, Trigger Bot and Crosshair Drift against a real target, the small games.
