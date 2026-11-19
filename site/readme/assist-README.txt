AiCoder Assist for Minecraft 1.21.11

How to use (Windows)
1. Start Minecraft 1.21.11 from the official launcher, with no mods. Wait for the title screen or enter a world.
2. Run AiCoderAssist.exe. A console window says whether it got in.
3. Go back to the game and press Right Shift.

If antivirus blocks the .exe (it is unsigned, and what it does is inject), use the download
without an exe instead: the same jar started by a small readable script.

In the game
- Right Shift (or Insert) opens the menu and closes it again. In a world, a chat line
  "[AiCoder Assist] loaded" appears once when it is in: no such line means it did not get in.
- To change the menu key: STYLE in the top bar, then the last row "Menu Key": click it and press
  the key you want (Esc keeps the current one). Insert always opens the menu too, as a way back.
- Top bar: WINDOW / COLUMNS switch the layout, MOTION switches animations, STYLE opens the look settings, GAMES opens the games.
- STYLE: Skin = Aurora or Glass (liquid glass). With Glass you can set the two glass colours
  (red/green/blue sliders), the glass opacity, how dark the world behind is, and the background image.
- Background image: click "Image" for the next one, right click for the previous one, NONE = no image.
  Your own pictures: put PNG or JPG files in the folder "aicoder-assist-wallpapers" inside the game
  folder (it is created the first time you open STYLE), then click "Image" again.
- GAMES in the top bar swaps the module list for six small games: Drop, Trail, Sweep, Merge, Bounce
  and Dig (a side-view sandbox: A/D walk, W or Space jump, hold left click to dig, right click to
  place, 1-9 or the wheel to pick). P pauses, R restarts. Minecraft keeps running behind the menu.
- Columns layout: left click a module to switch it, right click for its settings.
  Window layout: click a module to select it, click its switch or right click to switch it.
- Every module has a "Chat Hints" switch at the top of its settings. Off: that module writes
  nothing to chat (no "what it did" or "why it could not" lines). On by default.
- Every module has a BIND section at the bottom of its settings: click Key and press a key
  (Esc or right click clears it), Mode is TOGGLE or HOLD.
- Telly Bridge (Utility): stand on the ground holding blocks in the hotbar, face the way the bridge
  should go and switch it on (give it a key in BIND). Straight north/south/east/west only.
  It stops when you press back or sneak, open a screen, run out of blocks or fall.
  "Bridge Mode" Telly is the ordinary telly: it sprints to the very edge, jumps, flies on facing
  forward, then turns round and fills in the blocks behind with a drag click ("Cps"), and turns
  forward again. The bridge is flat, with a second layer on top in places ("Top Blocks").
  "Bridge Mode" Telly Up is the same going up: every second jump turns round early, while you
  are still high, so the blocks go in one level up and you land on them. This needs "Turn Ticks"
  at 2; with slower turns it stays flat.
  "Bridge Mode" Speed is speed telly: it turns round the moment it is off the ground and every
  jump goes up a level. "Bumps" adds one block on top of the second block of every jump.
  "Turn Ticks" is how long half a turn takes (Speed always takes 2). "Top Blocks" is how many
  blocks a jump puts on top of the bridge while it fills in (Telly and Telly Up).
  "Fill Back" (Telly Up) is how many blocks a jump goes back for behind where it took off:
  0 leaves the bridge thin, 6 makes it nearly solid, 2 is in between.
  "Turn Side" (Telly, Telly Up, Speed) is which way the camera turns round, Right or Left;
  it turns back the way it came.
  "Max Pitch" is how far down the camera looks while turned round.
  It always turns right to look back and left to face forward again, and it presses the left
  and right keys by itself to stay on the middle of the bridge.
  The camera glides from one direction to the next. While Telly Bridge is on, moving your own
  mouse does nothing; it works again when the module stops.
  "Bridge Mode" Diagonal: backwards the whole way, back and one strafe key
  held ("Side" picks left or right), hopping, never sneaking. For this mode stand with your back
  to where the bridge should go: it turns the camera 45 degrees to the chosen side and keeps it
  there. There is no half turn. Press forward or sneak to stop.
- Mace Dive has a "Spear Mode": it dives on a firework with a spear held level, and on three ticks
  in a row the spear hits, the chestplate goes on and the mace hits. Needs a spear in the hotbar.
  Tested with Height 20 and Attack Range 1.99. Mace Dive also no longer loses its target when
  Height is more than the target Range.
- Spear Smash (Melee): when your spear hits someone while you are coming down from a height, it
  switches to the mace and hits again, then back to the spear. Wearing an elytra, it puts the
  chestplate from your hotbar on first.
  The mace hits from as far as the spear reaches (4.5 blocks): attack is pressed with the spear in
  hand and the mace is selected as the press goes through. Order is always spear, chestplate, mace.
- Wind Jump now throws the charge one tick before the jump key ("Throw Timing" -1), which measured
  about 11 blocks instead of about 9.
- Offhand Totem (Utility): when your off hand has no totem it opens the inventory, moves the mouse
  onto a totem, swaps it into the off hand and closes the inventory. "Delay" is the ticks between
  those steps. You see the inventory flash open while it works.
  Survival inventory only: in creative mode it says so and does nothing.
- Cart Bomb (Explosives): needs a Flame bow, arrows, a rail and a TNT minecart in the hotbar. Look at
  the top of a block a few blocks ahead and switch it on (give it a key in BIND): it half-draws the
  bow, aims so the slow arrow comes down on that spot, lets go, and in the same tick puts the rail
  and the cart there. The arrow lands on the cart and it explodes. One round per press.
  The blast is close to you and hurts you too. "Draw" is how many ticks the bow is drawn: 4 by
  default (reaches about 3.6 blocks), 3 is the least and reaches under 3. If a rail is already on
  that block the cart goes straight onto it.
- Settings are saved to aicoder-assist.properties in the game folder when the menu closes.

Notes
- For Minecraft Java 1.21.11: the plain game, or the game started with Fabric Loader. It must already be running.
  Not for Forge, NeoForge, Quilt or OptiFine. Fabric with other mods installed has not been tested.
- Nothing is hidden: the classes are named aicoder.assist.*, and Java prints its own warning that an
  agent was loaded. Servers may forbid these modules; that is between you and the server.

Important: one package per game session. If you ran the package for another game version first,
close Minecraft completely and start it again before running this one.

If it does not get in: everything the window shows is also saved as AiCoderAssist-log.txt in this
folder. The line starting with "Reason:" says why.
