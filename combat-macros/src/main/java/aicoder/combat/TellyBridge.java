package aicoder.combat;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.KeyMappingAccessor;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;

/**
 * Telly bridging: sprint to the edge, jump, turn around in the air and place blocks on the side of the
 * last one, turn forward again before landing, repeat.
 *
 * <p>The bridge goes straight along the compass direction you face when the module is switched on.
 * Everything is a key press: the real camera turns, and forward, back, sprint, jump and use are pressed
 * the way the keyboard and mouse would press them. The block is placed by the game's own right click,
 * on whatever the crosshair is on. Where to look is worked out every tick from where you are, instead
 * of using a fixed pitch.
 *
 * <p>Speed mode is speed telly, done with the keys a player uses for it: forward and sprint on the ground
 * with jump held; round as soon as you are off the ground, back and left held, right click drag clicked
 * all the way; forward again just before landing, with right half way through the turn. Each jump goes
 * one block up, so you land near the top of the jump and lose no time falling back down.
 *
 * <p>Diagonal mode is the other way of doing it: backwards all the way, camera 45 degrees to one side,
 * back and one strafe key held, hopping, never sneaking.
 *
 * <p>Stops by itself when you run out of blocks, fall below the bridge, open a screen, or press sneak or
 * the opposite movement key yourself (back in telly mode, forward in diagonal mode).
 */
public class TellyBridge extends HintModule {
    public enum Mode {
        /** Sprint forward, turn around in the air to place. */
        Telly,
        /** The ordinary telly going up: a jump that starts facing forward turns round early and builds a level up. */
        TellyUp,
        /**
         * Telly Up with the rhythm of a player's recording: it lands still turned round, walks on backwards
         * along the bridge placing for a few ticks, and only then turns forward and jumps, so the camera
         * turns half as often.
         */
        TellyReplay,
        /** Speed telly: sprint-jump without a break, flick round only to place a couple of blocks. */
        Speed,
        /** Walk backwards the whole way, camera 45 degrees off, hopping. */
        Diagonal
    }

    public enum Side { Left, Right }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("bridge-mode")
        .description("Telly: face where the bridge goes. An ordinary telly: it runs to the edge, jumps, flies on a moment, turns round, drag clicks and turns forward again. The bridge is flat, with a block on top here and there. Telly Up: the same keys, but it turns round at once and builds two layers together, a block on and then a block on top of it, the way a player's bridge grows. You land on the upper layer and the next jump carries on from there, so the bridge climbs. Best with Turn Ticks at 2. Telly Replay: Telly Up with a recording's rhythm, where it stays turned round on the bridge after landing and goes on placing before it turns forward to jump, so the camera turns half as often. Speed: speed telly, it turns round the moment it is off the ground and every jump goes up a level. Diagonal: stand with your back to where the bridge goes; it turns the camera 45 degrees aside (no half turn), holds back and a strafe key, hops and places.")
        .defaultValue(Mode.Telly)
        .build()
    );

    private final Setting<Side> side = sgGeneral.add(new EnumSetting.Builder<Side>()
        .name("side")
        .description("Diagonal mode: which way the camera turns its 45 degrees, and which strafe key is held with back.")
        .defaultValue(Side.Left)
        .visible(() -> mode.get() == Mode.Diagonal)
        .build()
    );

    private final Setting<Boolean> hop = sgGeneral.add(new BoolSetting.Builder()
        .name("jump")
        .description("Diagonal mode: keep jumping while bridging. Off walks.")
        .defaultValue(true)
        .visible(() -> mode.get() == Mode.Diagonal)
        .build()
    );

    private final Setting<Side> turnSide = sgGeneral.add(new EnumSetting.Builder<Side>()
        .name("turn-side")
        .description("Telly, Telly Up and Speed modes: which way the camera turns round to place. It always turns back the way it came.")
        .defaultValue(Side.Right)
        .visible(() -> mode.get() != Mode.Diagonal && mode.get() != Mode.TellyReplay)
        .build()
    );

    private final Setting<Integer> cps = sgGeneral.add(new IntSetting.Builder()
        .name("cps")
        .description("Telly and Speed modes: clicks per second of the drag click, which goes on from the start of the turn round to the end of the turn forward.")
        .defaultValue(25)
        .range(5, 40)
        .sliderRange(5, 40)
        .visible(() -> mode.get() != Mode.Diagonal)
        .build()
    );

    private final Setting<Boolean> bumps = sgGeneral.add(new BoolSetting.Builder()
        .name("bumps")
        .description("Speed mode: one more block on top of the second block of every jump.")
        .defaultValue(true)
        .visible(() -> mode.get() == Mode.Speed)
        .build()
    );

    private final Setting<Integer> turnTicks = sgGeneral.add(new IntSetting.Builder()
        .name("turn-ticks")
        .description("Telly and Telly Up modes: how many ticks half a turn takes, round and forward again. More is slower and easier on the eyes, and leaves less time for placing: above 2, Telly Up cannot go up any more. Speed mode always takes two.")
        .defaultValue(4)
        .range(2, 6)
        .sliderRange(2, 6)
        .visible(() -> mode.get() == Mode.Telly || mode.get() == Mode.TellyUp)
        .build()
    );

    private final Setting<Integer> topBlocks = sgGeneral.add(new IntSetting.Builder()
        .name("top-blocks")
        .description("Telly and Telly Up modes: how many blocks a jump puts on top of the bridge while it fills in. For each of them the crosshair comes down onto the top of the last block first, a few degrees above its edge, and then over the edge onto its side. It turns round that many ticks earlier to have the time.")
        .defaultValue(2)
        .range(0, 4)
        .sliderRange(0, 4)
        .visible(() -> mode.get() == Mode.Telly || mode.get() == Mode.TellyUp)
        .build()
    );

    private final Setting<Integer> fillBack = sgGeneral.add(new IntSetting.Builder()
        .name("fill-back")
        .description("Telly Up mode: how many blocks a jump goes back for behind where it took off, to fill what earlier jumps left open. 0 leaves the bridge thin, with gaps in the upper layer; 6 makes it nearly solid.")
        .defaultValue(2)
        .range(0, 6)
        .sliderRange(0, 6)
        .visible(() -> mode.get() == Mode.TellyUp)
        .build()
    );

    private final Setting<Integer> stayRound = sgGeneral.add(new IntSetting.Builder()
        .name("stay-round")
        .description("Telly Replay mode: ticks it stays turned round on the bridge after landing, still placing, before it turns forward and jumps again.")
        .defaultValue(5)
        .range(1, 12)
        .sliderRange(1, 12)
        .visible(() -> false)
        .build()
    );

    private final Setting<Side> replaySide = sgGeneral.add(new EnumSetting.Builder<Side>()
        .name("replay-side")
        .description("Telly Replay mode: which way the camera swings. In the recording it is Left: the camera faces 45 degrees left of the bridge to jump (forward and right held) and turns on to the left to look back.")
        .defaultValue(Side.Left)
        .visible(() -> false)
        .build()
    );

    private final Setting<Boolean> finishJump = sgGeneral.add(new BoolSetting.Builder()
        .name("finish-jump")
        .description("When you switch the module off while it is bridging, it first turns forward and jumps on, the way the recording ends, instead of stopping where it is. Switch it off a second time to stop at once.")
        .defaultValue(true)
        .visible(() -> mode.get() != Mode.Diagonal)
        .build()
    );

    private final Setting<Integer> glide = sgGeneral.add(new IntSetting.Builder()
        .name("glide-ticks")
        .description("Telly Replay mode: after turning round in the air the camera drifts down slowly until this tick of the jump before the first block goes on, as in the recording (which waits about 6). Up to 2 the bridge climbs; from 3 on there is no time left for the upper layer and the bridge is flat with a block on top each jump.")
        .defaultValue(2)
        .range(0, 8)
        .sliderRange(0, 8)
        .visible(() -> false)
        .build()
    );

    private final Setting<Double> maxPitch = sgGeneral.add(new DoubleSetting.Builder()
        .name("max-pitch")
        .description("Telly and Speed modes: the furthest down the camera looks while turned round, in degrees. 90 is straight down. Only when you are two ticks from landing with no block there yet does it look further down.")
        .defaultValue(80)
        .range(60, 89)
        .sliderRange(60, 89)
        .visible(() -> mode.get() != Mode.Diagonal)
        .build()
    );

    private final Setting<Double> turnSpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("turn-speed")
        .description("Telly and Diagonal modes: most degrees the camera turns per tick. 180 turns around at once, lower is smoother but places fewer blocks. Speed mode always takes two ticks for each half turn.")
        .defaultValue(180)
        .range(30, 180)
        .sliderRange(30, 180)
        .build()
    );

    private final Setting<Boolean> restoreSlot = sgGeneral.add(new BoolSetting.Builder()
        .name("restore-slot")
        .description("Select the item you were holding again when the module stops.")
        .defaultValue(true)
        .build()
    );

    /** Unit step of the bridge, one of the four compass directions. */
    private int dx, dz;
    private float forwardYaw;
    /** Level of the bridge blocks, and the column the bridge runs along. */
    private int floorY, lineX, lineZ;
    private int restSlot = -1;
    private boolean running, air, pressedJump, reversed;
    /** Where the camera pointed a tick ago and where it points this tick; frames glide between them. */
    private float fromYaw, fromPitch, toYaw, toPitch;
    private boolean steering;
    /** Telly Up and Speed build higher; Telly stays on the level it started on. */
    /** Blocks of the jump speed telly opens with. */
    private static final int OPENING_BLOCKS = 4;

    private boolean rising() {
        return tellyUp() || mode.get() == Mode.Speed;
    }

    private boolean speedy() {
        return mode.get() == Mode.Speed;
    }

    /** 0 facing forward, 1 turning round, 2 round, 3 turning forward. */
    private int phase, phaseTick;
    /** Speed mode: jumps made since it was switched on. */
    private int jumps;
    /** Speed mode: where the bridge ended when this jump began, and whether its bump is in. */
    private int takeoffEnd;
    /** Whether the jump being made builds one block up. */
    private boolean upJump;
    private boolean pairJump;
    /** Ticks in the air this jump, and where the run of this jump starts (its step), if it is in. */
    private int airT, runStart = Integer.MAX_VALUE;
    /** Blocks this jump has put on top of the bridge. */
    private int tops;
    /** Telly Up: ticks spent turned round on the ground since the last jump. */
    private int stayT;
    private boolean gliding;
    /** Telly Replay: ticks for the half turn forward; three here (the recording takes about four, but with four the jump starts too slowly to build upward), and two to turn round. */
    private static final int REPLAY_FORWARD = 5;
    /** Telly Replay, read off the recording: degrees from straight ahead while facing forward, and tick by tick on the way round and back. */
    private static final float REPLAY_FACE = 45;
    private static final float[] REPLAY_ROUND = {135};
    private static final float[] REPLAY_BACK = {160, 130, 100, 70, 45};
    /** Degrees the camera looks down: facing forward, and just after turning round. */
    private static final float REPLAY_PITCH_FORWARD = 50, REPLAY_PITCH_ROUND = 26;
    private int forwardT;
    /** Telly Up: blocks this jump has gone back for, behind where it took off. */
    private int backFills;
    /** A jump up of the ordinary telly: whether its one more block on the lower level is in, and where. */
    private boolean baseDone;
    private int baseIndex;
    private boolean bumped;
    private double clicks;

    public TellyBridge() {
        super(AiCoderTab.UTILITY, "telly-bridge", "Bridges for you with sprint jumps: turns around in the air, places blocks behind, and turns forward again before landing.");
    }

    @Override
    public void onActivate() {
        running = false;
        if (mc.player == null || mc.level == null) return;
        if (!mc.player.onGround()) {
            warning("Stand on the ground first.");
            toggle();
            return;
        }
        restSlot = mc.player.getInventory().getSelectedSlot();
        if (!holdBlocks()) {
            warning("No blocks in your hotbar.");
            toggle();
            return;
        }

        // Nearest compass direction. Yaw 0 looks along +z, 90 along -x.
        // Telly: the way you face. Diagonal: you stand with your back to where the bridge goes, so it
        // goes the opposite way from your view and the camera only has the 45 degrees to turn, never
        // the half turn. Leaning the reading half way towards the chosen side takes a view that is
        // already 45 degrees aside as well.
        float heading = mc.player.getYRot();
        if (mode.get() == Mode.Diagonal) heading -= 180 + (side.get() == Side.Left ? -22.5f : 22.5f);
        int quarter = Math.floorMod(Math.round(heading / 90f), 4);
        dx = quarter == 1 ? -1 : quarter == 3 ? 1 : 0;
        dz = quarter == 0 ? 1 : quarter == 2 ? -1 : 0;
        forwardYaw = quarter * 90f;

        air = false;
        reversed = false;
        phase = 0;
        jumps = 0;
        rStage = 0;
        rT = 0;
        finishing = false;
        forceOff = false;
        steering = false;
        running = true;
        noteFloor();
    }

    @Override
    public void onDeactivate() {
        if (!running) return;
        running = false;
        if (steering && mc.player != null) {
            // Leave the camera where the last tick put it
            mc.player.setYRot(toYaw);
            mc.player.setXRot(toPitch);
        }
        steering = false;
        // Leave every key as the keyboard has it
        mc.options.keyUp.setDown(Input.isPressed(mc.options.keyUp));
        mc.options.keyDown.setDown(Input.isPressed(mc.options.keyDown));
        mc.options.keySprint.setDown(Input.isPressed(mc.options.keySprint));
        mc.options.keyJump.setDown(Input.isPressed(mc.options.keyJump));
        mc.options.keyLeft.setDown(Input.isPressed(mc.options.keyLeft));
        mc.options.keyRight.setDown(Input.isPressed(mc.options.keyRight));
        pressedJump = false;
        if (mc.player != null && restoreSlot.get() && restSlot >= 0) mc.player.getInventory().setSelectedSlot(restSlot);
    }

    /**
     * The camera is moved, not placed. Each tick works out where it should point and the game uses that for
     * the tick, but what you see glides there from where it pointed the tick before, frame by frame, the
     * way a mouse movement looks. While the module steers, your own mouse is left out: every frame the
     * camera is put back on that path, so moving the mouse cannot spoil the bridge.
     */
    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!running || mc.player == null) {
            step();
            return;
        }
        // Back to where the last tick left it, whatever the frames and the mouse did since
        if (steering) {
            mc.player.setYRot(toYaw);
            mc.player.setXRot(toPitch);
        }
        float beforeYaw = mc.player.getYRot(), beforePitch = mc.player.getXRot();
        step();
        if (!running || mc.player == null) return;
        fromYaw = steering ? toYaw : beforeYaw;
        fromPitch = steering ? toPitch : beforePitch;
        // The same way round every time: half a turn is never taken as the other half
        float turned = wrap(mc.player.getYRot() - fromYaw);
        if (Math.abs(Math.abs(turned) - 180) < 1) turned = 180 * turnSign();
        toYaw = fromYaw + turned;
        toPitch = mc.player.getXRot();
        steering = true;
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!running || !steering || mc.player == null) return;
        float t = Math.max(0, Math.min(1, event.tickDelta));
        mc.player.setYRot(fromYaw + (toYaw - fromYaw) * t);
        mc.player.setXRot(fromPitch + (toPitch - fromPitch) * t);
    }

    private void step() {
        if (!running) return;
        if (mc.player == null || mc.level == null) {
            toggle();
            return;
        }
        if (pressedJump) {
            pressedJump = false;
            mc.options.keyJump.setDown(false);
        }
        if (finishing) { finishStep(); return; }

        if (mc.gui.screen() != null) { stop("Stopped: a screen was opened."); return; }
        // The key that would undo what the module holds: back when it runs forward, forward when it walks backwards
        boolean diagonal = mode.get() == Mode.Diagonal;
        if (Input.isPressed(diagonal ? mc.options.keyUp : mc.options.keyDown) || Input.isPressed(mc.options.keyShift)) { stop("Stopped."); return; }
        if (!holdBlocks()) { stop("Stopped: out of blocks."); return; }

        double feet = mc.player.getY() - (floorY + 1);
        if (feet < -0.75) { stop("Stopped: fell off the bridge."); return; }
        if (diagonal) { diagonal(); return; }
        if (mode.get() == Mode.TellyReplay) { replay(); return; }
        telly();
    }

    /**
     * Backwards the whole way. The camera looks back and 45 degrees to one side; back and the strafe key
     * of that side together move straight along the bridge. Each tick the side of the last block is
     * clicked as soon as the eyes are past it, on the ground at the edge or in the air during a hop.
     */
    private void diagonal() {
        int sign = side.get() == Side.Left ? -1 : 1;
        // Left and right the camera stays exactly here; only up and down it follows the block being placed
        float base = forwardYaw + 180 + sign * 45;

        mc.options.keyUp.setDown(false);
        mc.options.keySprint.setDown(false);
        mc.options.keyDown.setDown(true);
        mc.options.keyLeft.setDown(sign < 0);
        mc.options.keyRight.setDown(sign > 0);
        if (hop.get()) {
            // Held: the game jumps again the moment you land
            mc.options.keyJump.setDown(true);
            pressedJump = true;
        }

        // The last block of the bridge, looking back from where you are
        int along = dx != 0 ? floor(mc.player.getX()) * dx : floor(mc.player.getZ()) * dz;
        int bx = 0, bz = 0;
        boolean found = false;
        for (int i = 0; i < 7 && !found; i++) {
            int a = along - i;
            bx = dx != 0 ? a * dx : lineX;
            bz = dz != 0 ? a * dz : lineZ;
            found = solid(bx, floorY, bz);
        }

        double ex = mc.player.getX(), ey = mc.player.getEyeY(), ez = mc.player.getZ();
        double planeX = bx + 0.5 + dx * 0.5, planeZ = bz + 0.5 + dz * 0.5;
        double beyond = (ex - planeX) * dx + (ez - planeZ) * dz;
        if (!found || beyond < 0.06 || !replaceable(bx + dx, floorY, bz + dz)) {
            // Not past its face yet: hold still
            turn(base, mc.player.getXRot());
            return;
        }

        // Where the 45 degree line of sight meets that face, kept inside the face
        double yaw = Math.toRadians(base), lx = -Math.sin(yaw), lz = Math.cos(yaw);
        double k = beyond / Math.max(0.2, -(lx * dx + lz * dz));
        double sx = ex + lx * k, sz = ez + lz * k;
        double fx = dx != 0 ? planeX : clamp(sx, bx + 0.12, bx + 0.88);
        double fz = dz != 0 ? planeZ : clamp(sz, bz + 0.12, bz + 0.88);
        // On the face already: no sideways turn at all
        boolean straight = fx == (dx != 0 ? planeX : sx) && fz == (dz != 0 ? planeZ : sz);
        double vx = fx - ex, vy = floorY + 0.55 - ey, vz = fz - ez;
        float pitch = (float) -Math.toDegrees(Math.atan2(vy, Math.sqrt(vx * vx + vz * vz)));

        if (turn(straight ? base : (float) Math.toDegrees(Math.atan2(-vx, vz)), pitch) && vx * vx + vy * vy + vz * vz < 4.4 * 4.4) {
            KeyMapping.click(((KeyMappingAccessor) mc.options.keyUse).meteor$getKey());
        }
    }

    /**
     * Speed telly, with the keys and in the order a player does it:
     * <ol>
     *   <li>On the ground: facing forward, forward and sprint held, jump held (it jumps the moment it lands).</li>
     *   <li>Off the ground: half a turn in two ticks. Forward is let go of and back pressed at once, then
     *   back and left are held. Right click is drag clicked at cps from here to the end of the turn
     *   forward, whatever the crosshair is on.</li>
     *   <li>Round: the camera is pointed at the block to click, a little above the middle of its side, then
     *   left alone for a tick while the clicks go on, then pointed again. It is not put on every block
     *   anew, so the clicks land where the crosshair has got to by then.</li>
     *   <li>Two ticks before landing, once there is a block where you come down: half a turn forward in two
     *   ticks, right pressed half way (the camera faces left then, so it pushes you on along the bridge),
     *   forward and sprint again at the end.</li>
     * </ol>
     * Every jump goes one block up: the first click goes on top of the last block (the step, which fits
     * once your feet are above it or past it), the next ones on along that level from the step's side.
     * You come down on it in the ninth tick of the jump, near the top, instead of the twelfth back at the
     * level you left, which is the time speed telly saves. It comes to about four blocks a jump.
     */
    private void telly() {
        // Telly Up, the unhurried way: land still turned round, stay that way on the bridge for a few
        // ticks and go on placing, then turn forward standing where you are, and only then jump
        boolean calm = mode.get() == Mode.TellyReplay && pairJump;
        // It walks on backwards meanwhile, bridging the level it stands on, and turns forward only with
        // enough of that level ahead to turn on
        int turnN = REPLAY_FORWARD;
        boolean staying = calm && mc.player.onGround() && phase == 2
            && (stayT < stayRound.get() || overhang() > -0.6);
        if (staying) {
            stayT++;
            floorY = floor(mc.player.getY() - 0.2);
        }
        if (mc.player.onGround() && !staying) {
            // The flat bridge stays on the level it started on, also when you land on a block that
            // went on top of it
            // Telly keeps the level it started on. Telly Up and Speed take the level you are standing
            // on: in Telly Up that is higher when you came down on blocks that went on top of the
            // bridge at the end of the jump.
            int standing = floor(mc.player.getY() - 0.2);
            if (speedy() || tellyUp() || jumps == 0) floorY = standing;
            // Landed still turned round, or part of the way forward: it goes on turning forward the
            // way it always does (to the left), not whichever way is shorter, and jumps at once while it
            // does. You land on the very end of the bridge, so a tick spent turning on the ground walks
            // you off it.
            if (phase != 0) {
                if (phase != 3) {
                    phase = 3;
                    phaseTick = 0;
                }
                turnForward(speedy() ? 2 : calm ? REPLAY_FORWARD : turnN());
                // The unhurried Telly Up turns on the bridge and jumps once it faces forward; only
                // with the end of the bridge under its feet does it jump while still turning
                if (calm && overhang() < 0.15) return;
                // With half a turn in two ticks there is time to face forward and turn round again
                // early enough to go up; with slower turns there is not
                takeOff(speedy() || rising() && turnN() <= 2);
                return;
            }
            if (calm) {
                // The recording does not face straight ahead to jump: 45 degrees to one side with
                // forward and the far strafe key, which carries you straight along the bridge. A few
                // degrees more or less keeps you on its middle line.
                phase = 0;
                reversed = false;
                double aside = (mc.player.getX() - (lineX + 0.5)) * -dz + (mc.player.getZ() - (lineZ + 0.5)) * dx;
                float trim = (float) Math.max(-12, Math.min(12, -aside * 40));
                mc.player.setYRot(forwardYaw + turnSign() * REPLAY_FACE + trim);
                mc.player.setXRot(mc.player.getXRot() + (REPLAY_PITCH_FORWARD - mc.player.getXRot()) * 0.5f);
                replayKeys(REPLAY_FACE);
                forwardT++;
                if (forwardT >= 2 || overhang() > -0.35) takeOff(true);
                return;
            }
            phase = 0;
            reversed = false;
            mc.options.keyLeft.setDown(false);
            mc.options.keyRight.setDown(false);
            turn(forwardYaw, mc.player.getXRot() > 60 ? 20 : mc.player.getXRot());
            forward();
            steer(true);
            // The first jump is an ordinary telly: run to the very edge of the block, then jump.
            // After it, jump the moment you land.
            double lip = 0.1;
            boolean edge = !solid(floor(mc.player.getX() + dx * lip), floorY, floor(mc.player.getZ() + dz * lip));
            if (facing(forwardYaw) && (jumps > 0 || edge)) {
                takeOff(rising());
            }
            return;
        }

        airT++;
        double ex = mc.player.getX(), ey = mc.player.getEyeY(), ez = mc.player.getZ(), feetY = mc.player.getY();
        double vy = mc.player.getDeltaMovement().y;
        double vh = mc.player.getDeltaMovement().x * dx + mc.player.getDeltaMovement().z * dz;
        // This jump builds one block up. Already falling below that with no step in: the level you left.
        // The first jump stays on the level you left
        // Whether this jump builds one block up was settled when it began
        int level = upJump ? floorY + 1 : floorY;
        boolean up = level > floorY;
        int[] end = up ? run(level) : last(level);
        int[] below = end == null ? last(level - 1) : null;

        // When and where you come down on this level: the game adds the push of the keys, moves, then
        // takes off gravity and drag
        double y = feetY, v = vy, h = vh, advance = 0;
        int landIn = 0;
        for (int k = 1; k <= 14 && landIn == 0; k++) {
            boolean down = v < 0;
            h += 0.0196;
            y += v;
            advance += h;
            v = (v - 0.08) * 0.98;
            h *= 0.91;
            if (down && y <= level + 1) landIn = k;
        }
        boolean landing = landIn > 0 && under(advance, level);
        // Telly Up builds two layers and comes down on the upper one if it is there
        boolean pair = pairJump && !up && !speedy() && !staying;
        if (pair) {
            double y2 = feetY, v2 = vy, h2 = vh, adv2 = 0;
            for (int k = 1; k <= 14; k++) {
                boolean down = v2 < 0;
                h2 += 0.0196;
                y2 += v2;
                adv2 += h2;
                v2 = (v2 - 0.08) * 0.98;
                h2 *= 0.91;
                if (down && y2 <= level + 2) {
                    if (under(adv2, level + 1)) {
                        landIn = k;
                        landing = true;
                    }
                    break;
                }
            }
        }

        // Where to look while round. The camera never tilts further down than max-pitch, and a click is
        // only made with the crosshair on the spot.
        // Two ticks from coming down with nothing there yet is the one time it looks further down
        float cap = staying || landIn > 0 && landIn <= 2 && !landing ? 89 : maxPitch.get().floatValue();
        float aimYaw = forwardYaw + 180, aimPitch = cap;
        boolean onSpot = false;
        int sign = dx != 0 ? dx : dz;
        double along = dx != 0 ? ex * dx : ez * dz;
        double fx = 0, fy = 0, fz = 0;
        boolean spot = false;
        boolean spotAhead = false;
        int stepIndex = Integer.MIN_VALUE;
        boolean bumpSpot = false;
        boolean topSpot = false;
        boolean pairTop = false;
        boolean backSpot = false;
        boolean baseSpot = false;
        // Blocks between the end of the bridge and where you come down
        int need = 1;
        if (end != null) {
            // The side of the last block, near its top
            double plane = dx != 0 ? end[0] + 0.5 + dx * 0.5 : end[1] + 0.5 + dz * 0.5;
            double beyond = dx != 0 ? (ex - plane) * dx : (ez - plane) * dz;
            fx = dx != 0 ? plane : clamp(ex, end[0] + 0.12, end[0] + 0.88);
            fz = dz != 0 ? plane : clamp(ez, end[1] + 0.12, end[1] + 0.88);
            fy = level + 0.95;
            spot = true;
            onSpot = beyond >= 0.08 && replaceable(end[0] + dx, level, end[1] + dz);
            // Speed telly opens with a "4b": the first jump goes forward and puts exactly four blocks
            // in a straight line behind it, and the speed telly begins from the end of those
            if (speedy() && jumps == 1 && index(end) - takeoffEnd >= OPENING_BLOCKS) onSpot = false;
            need = Math.max(1, (int) Math.ceil(beyond + advance - 0.15));
            // A block on top first. Seen from behind and above, the top of the last block is only four
            // to nine degrees above its side, so the camera hardly moves between the two: it is your
            // flying on that carries the crosshair over the edge. The top can be clicked once you are
            // clear of the block, and not when the landing has to be saved.
            if (pair) {
                // The way a player's bridge grows in the replay: a block on, then a block on top of
                // it, then the next one on, and so on, two layers at once. The block on top goes on
                // the first block of this jump that has none yet, once your feet are above it or you
                // are past it; until then it carries on along the lower layer.
                // To keep the bridge two blocks thick it also goes back for what earlier jumps left
                // open behind you: a gap in the layer you stand on, then a block of it with nothing
                // on top. Not when the landing still has to be saved.
                int endI = index(end);
                boolean saving = landIn > 0 && landIn <= 3 && !landing;
                for (int pass = 0; pass < 2 && !pairTop && !saving; pass++) {
                    // pass 0: gaps in this layer, over a block of the layer below; pass 1: no block on top
                    int base = pass == 0 ? level - 1 : level;
                    for (int i = Math.max(endI - 6, takeoffEnd - 5); i <= endI && !pairTop; i++) {
                        int bx = dx != 0 ? i * dx : lineX, bz = dz != 0 ? i * dz : lineZ;
                        if (!solid(bx, base, bz) || !replaceable(bx, base + 1, bz)) continue;
                        // Blocks of earlier jumps: only fill-back of them a jump
                        boolean old = pass == 0 || i <= takeoffEnd;
                        if (old && backFills >= fillBack.get()) continue;
                        double front = sign > 0 ? i + 1 : i;
                        if (feetY < base + 2 + 0.01 && along - 0.3 <= front + 0.01) {
                            // The newest block, still under your feet: on along the lower layer first
                            if (pass == 1) break;
                            continue;
                        }
                        double at = Math.min(along - 0.1, front - 0.35) * sign;
                        double tx = dx != 0 ? at : clamp(ex, bx + 0.12, bx + 0.88), tz = dz != 0 ? at : clamp(ez, bz + 0.12, bz + 0.88);
                        double ty = base + 1 - 0.02;
                        // Within reach only
                        if ((tx - ex) * (tx - ex) + (ty - ey) * (ty - ey) + (tz - ez) * (tz - ez) > 4.2 * 4.2) continue;
                        fx = tx;
                        fy = ty;
                        fz = tz;
                        onSpot = true;
                        topSpot = true;
                        pairTop = true;
                        backSpot = old;
                    }
                }
            } else if (!up && !speedy() && tops < topBlocks.get() && beyond > 0.4 && replaceable(end[0], level + 1, end[1])
                && !(landIn > 0 && landIn <= need + 1 && !landing)) {
                fx = dx != 0 ? plane - dx * 0.35 : fx;
                fz = dz != 0 ? plane - dz * 0.35 : fz;
                fy = level + 1 - 0.02;
                onSpot = true;
                topSpot = true;
            }
        } else if (below != null && !speedy()) {
            // The ordinary telly going up carries on from the front of the last block first: one more
            // block on the level below. Then, still high in the air, the click on top of that new
            // block, which is the step, and from the step's side on along the upper level.
            if (baseDone) {
                int bx = dx != 0 ? baseIndex * dx : lineX, bz = dz != 0 ? baseIndex * dz : lineZ;
                if (!solid(bx, level - 1, bz)) baseDone = false;
            }
            if (!baseDone) {
                double plane = dx != 0 ? below[0] + 0.5 + dx * 0.5 : below[1] + 0.5 + dz * 0.5;
                double beyond = dx != 0 ? (ex - plane) * dx : (ez - plane) * dz;
                fx = dx != 0 ? plane : clamp(ex, below[0] + 0.12, below[0] + 0.88);
                fz = dz != 0 ? plane : clamp(ez, below[1] + 0.12, below[1] + 0.88);
                fy = level - 0.05;
                spot = true;
                onSpot = beyond >= 0.08 && replaceable(below[0] + dx, level - 1, below[1] + dz);
                baseSpot = true;
            } else {
                int bx = dx != 0 ? baseIndex * dx : lineX, bz = dz != 0 ? baseIndex * dz : lineZ;
                double front = sign > 0 ? baseIndex + 1 : baseIndex;
                double at = (front - 0.35) * sign;
                fx = dx != 0 ? at : clamp(ex, bx + 0.12, bx + 0.88);
                fz = dz != 0 ? at : clamp(ez, bz + 0.12, bz + 0.88);
                fy = level - 0.02;
                spot = true;
                boolean clear = feetY >= level + 1 + 0.01 || along - 0.3 > front + 0.01;
                onSpot = clear && replaceable(bx, level, bz);
                if (onSpot) stepIndex = baseIndex;
            }
        } else if (below != null) {
            // Speed telly's step: the top of a block of the level below, far enough behind you for
            // the camera not to have to look further down than max-pitch
            double plane = dx != 0 ? below[0] + 0.5 + dx * 0.5 : below[1] + 0.5 + dz * 0.5;
            // The ordinary telly looks back at a flatter angle, so its step goes further back
            double back = Math.max(0.3, (ey - level) / Math.tan(Math.toRadians(speedy() ? cap : Math.min(cap, 55))));
            double at = Math.min(along - back, plane * sign - 0.15) * sign;
            int cx = dx != 0 ? floor(at) : lineX, cz = dz != 0 ? floor(at) : lineZ;
            fx = dx != 0 ? at : clamp(ex, cx + 0.12, cx + 0.88);
            fz = dz != 0 ? at : clamp(ez, cz + 0.12, cz + 0.88);
            // Just under the top, so the line of sight comes in through the top and not the side
            fy = level - 0.02;
            spot = true;
            // It fits once your feet are above it or you are past it
            double front = sign > 0 ? floor(at) + 1 : -floor(at);
            boolean clear = feetY >= level + 1 + 0.01 || along - 0.3 > front + 0.01;
            onSpot = clear && solid(cx, level - 1, cz) && replaceable(cx, level, cz);
            if (onSpot) stepIndex = dx != 0 ? cx * dx : cz * dz;
        }
        // The bump: one block on top of the second block of this jump, once you are clear of it and
        // it is not the moment to save the landing
        if (up && speedy() && bumps.get() && !bumped && !(landIn > 0 && landIn <= 3 && !landing)) {
            int i = runStart != Integer.MAX_VALUE ? runStart + 1 : Integer.MIN_VALUE;
            if (i != Integer.MIN_VALUE) {
                int bx = dx != 0 ? i * dx : lineX, bz = dz != 0 ? i * dz : lineZ;
                if (!replaceable(bx, level + 1, bz)) {
                    bumped = solid(bx, level, bz);
                } else if (solid(bx, level, bz)) {
                    double front = sign > 0 ? i + 1 : i;
                    boolean clear = feetY >= level + 2 + 0.01 || along - 0.3 > front + 0.01;
                    double at = (front - 0.2) * sign;
                    double tx = dx != 0 ? at : clamp(ex, bx + 0.12, bx + 0.88), tz = dz != 0 ? at : clamp(ez, bz + 0.12, bz + 0.88);
                    double ty = level + 1 - 0.02;
                    double flat = Math.sqrt((tx - ex) * (tx - ex) + (tz - ez) * (tz - ez));
                    float pitch = (float) -Math.toDegrees(Math.atan2(ty - ey, flat));
                    if (clear && pitch <= cap) {
                        fx = tx;
                        fy = ty;
                        fz = tz;
                        spot = true;
                        onSpot = true;
                        bumpSpot = true;
                    }
                }
            }
        }
        if (spot) {
            double ax = fx - ex, ay = fy - ey, az = fz - ez;
            double flat = Math.sqrt(ax * ax + az * az);
            float pitch = (float) -Math.toDegrees(Math.atan2(ay, flat));
            // Turned round, the camera stays behind you. A spot that is still ahead of you (you have
            // not flown past it yet) is not looked at: that would swing the camera forward and back
            // again, a whole turn. It waits facing straight back and does not click.
            boolean behind = ax * dx + az * dz < -0.05;
            if (!behind) {
                onSpot = false;
                spotAhead = true;
                pitch = cap;
            } else if (flat >= 0.15) {
                aimYaw = forwardYaw + 180 + Math.max(-30, Math.min(30, wrap((float) Math.toDegrees(Math.atan2(-ax, az)) - forwardYaw - 180)));
            }
            // The block on top in a two layer jump is nearly under you: that one may be looked at
            // straight down
            float most = pairTop ? 89 : cap;
            if (pitch > most + 0.5f) onSpot = false;
            aimPitch = Math.min(pitch, most);
        }

        // Speed telly turns round as soon as it is off the ground. The ordinary telly flies on facing
        // forward first, and turns round when there is just time left to fill in the blocks behind:
        // one a tick, two ticks for the turn, two to spare.
        // The ordinary telly going up flies on for two ticks and turns round on the third: the step then
        // goes on a block further back, and the new level lies over the last blocks of the old one.
        int n = speedy() ? 2 : calm ? REPLAY_ROUND.length + 1 : turnN();
        // Speed telly's opening jump is flown like an ordinary telly: forward at a sprint first, so it
        // carries far enough for four blocks, and round only when there is just time to put them in
        boolean opening = speedy() && jumps == 1;
        boolean turnNow = speedy() && !opening || pair || (up ? airT >= Math.max(1, 5 - n) : landIn > 0 && landIn <= need + (opening ? 0 : topBlocks.get()) + n + 2);
        if (phase == 0 && !landing && turnNow) {
            phase = 1;
            phaseTick = 0;
            clicks = 0;
        } else if (phase == 2 && landing && landIn <= n && !calm) {
            phase = 3;
            phaseTick = 0;
        }

        if (phase == 0) {
            reversed = false;
            turn(forwardYaw, 20);
            forward();
            steer(true);
            return;
        }

        reversed = true;
        gliding = false;
        float pitchNow = mc.player.getXRot();
        float downTo = aimPitch;
        // Telly Replay comes round looking about 30 degrees down and drifts on from there
        boolean drift = calm && glide.get() > 2 && !(landIn > 0 && landIn <= 4 && !landing);
        if (phase == 1) {
            // Half a turn to the right in n ticks. Back is pressed as soon as forward is let go of.
            phaseTick++;
            if (phaseTick < n && calm) {
                float off = REPLAY_ROUND[phaseTick - 1];
                replayKeys(off);
                mc.player.setYRot(forwardYaw + turnSign() * off);
                mc.player.setXRot(pitchNow + ((drift ? REPLAY_PITCH_ROUND : aimPitch) - pitchNow) / (n - phaseTick + 1));
            } else if (phaseTick < n) {
                turnKeys(180f * phaseTick / n);
                mc.player.setYRot(forwardYaw + turnSign() * 180f * phaseTick / n);
                mc.player.setXRot(pitchNow + ((drift ? 30 : aimPitch) - pitchNow) / (n - phaseTick + 1));
            } else {
                mc.options.keyUp.setDown(false);
                mc.options.keySprint.setDown(false);
                mc.options.keyDown.setDown(true);
                mc.player.setYRot(aimYaw);
                gliding = drift;
                mc.player.setXRot(drift ? (calm ? REPLAY_PITCH_ROUND : 30) : downTo);
                steer(false);
                phase = 2;
            }
        } else if (phase == 2) {
            // Round: back and left held; right instead of left once you are off to that side.
            // The crosshair stays on the spot.
            mc.options.keyUp.setDown(false);
            mc.options.keySprint.setDown(false);
            mc.options.keyDown.setDown(true);
            steer(false);
            if (staying && overhang() > 0.2) {
                // Nothing went on in front in time: stop at the end instead of walking off it
                mc.options.keyDown.setDown(false);
                mc.options.keyUp.setDown(vh > 0.03);
            }
            mc.player.setYRot(aimYaw);
            // The recording: round in two ticks at about 30 degrees down, then a slow drift to about 50
            // with nothing placed, and only then down onto the end of the bridge
            gliding = calm && !staying && airT <= glide.get() && !(landIn > 0 && landIn <= 4 && !landing);
            if (gliding) downTo = Math.min(downTo, REPLAY_PITCH_ROUND + 3.2f * Math.max(0, airT - 2));
            mc.player.setXRot(downTo);
        } else {
            turnForward(calm ? REPLAY_FORWARD : n);
        }

        // Drag click at cps, while the crosshair is on the spot
        if (phase == 2 && !spotAhead && onSpot && stepIndex != Integer.MIN_VALUE && !bumpSpot) runStart = stepIndex;
        if (phase == 2 && !spotAhead && !gliding && (up || pairTop ? onSpot : flatClick(level))) {
            if (topSpot) tops++;
            if (backSpot) backFills++;
            if (baseSpot) {
                baseDone = true;
                baseIndex = index(below) + 1;
            }
            clicks += cps.get() / 20.0;
            while (clicks >= 1) {
                clicks -= 1;
                KeyMapping.click(((KeyMappingAccessor) mc.options.keyUse).meteor$getKey());
            }
        } else {
            clicks = Math.max(clicks, 1);
        }
    }

    /**
     * Keeps you on the middle of the bridge with the left and right keys, the camera staying square to it.
     * Looks at where you will be a few ticks on, so it lets go before you are across the middle.
     * Facing forward, left moves you to the left of the way you go; turned round it is the other way about.
     */
    private void steer(boolean facingForward) {
        // + is to the right of the way you go
        double aside = (mc.player.getX() - (lineX + 0.5)) * -dz + (mc.player.getZ() - (lineZ + 0.5)) * dx;
        double drift = mc.player.getDeltaMovement().x * -dz + mc.player.getDeltaMovement().z * dx;
        double soon = aside + drift * (mc.player.onGround() ? 2 : 4);
        boolean goLeft = soon > 0.1, goRight = soon < -0.1;
        mc.options.keyLeft.setDown(facingForward ? goLeft : goRight);
        mc.options.keyRight.setDown(facingForward ? goRight : goLeft);
    }

    /**
     * One tick of the half turn forward, to the left, back the way it came. While the camera faces the
     * right of the bridge it is the left key that pushes you on along it; forward and sprint come back
     * once it is within 20 degrees of straight ahead.
     */
    private void turnForward(int n) {
        if (mode.get() == Mode.TellyReplay && pairJump) {
            // Back the way it came, in five ticks, to 45 degrees short of straight ahead
            phaseTick++;
            float off = REPLAY_BACK[Math.min(phaseTick, REPLAY_BACK.length) - 1];
            mc.player.setYRot(forwardYaw + turnSign() * off);
            mc.player.setXRot(mc.player.getXRot() + (REPLAY_PITCH_FORWARD - mc.player.getXRot()) / Math.max(1, REPLAY_BACK.length - phaseTick + 1));
            replayKeys(off);
            if (phaseTick >= REPLAY_BACK.length) {
                phase = 0;
                forwardT = 0;
            }
            return;
        }
        phaseTick++;
        float off = Math.max(0, 180f - 180f * phaseTick / n);
        mc.player.setYRot(forwardYaw + turnSign() * off);
        mc.player.setXRot(mc.player.getXRot() + (20 - mc.player.getXRot()) / Math.max(1, n - phaseTick + 1));
        turnKeys(off);
        if (phaseTick >= n) phase = 0;
    }

    /**
     * The key that pushes you on along the bridge while the camera is this many degrees to the right of
     * straight ahead: forward up to 67, left from there to 113 (the camera is square to the bridge then),
     * back beyond. What forward and back push sideways on the way round cancels out. Sprint only when
     * nearly straight, or a jump would be thrown aslant.
     */
    private void turnKeys(float off) {
        // (turnSign: 1 when the camera turns round to the right, -1 to the left)
        mc.options.keyUp.setDown(off < 67);
        mc.options.keySprint.setDown(off <= 20);
        // Turned to the right it is the left key that pushes you on; turned to the left, the right key
        boolean square = off >= 67 && off <= 113;
        mc.options.keyLeft.setDown(square && turnSign() > 0);
        mc.options.keyRight.setDown(square && turnSign() < 0);
        mc.options.keyDown.setDown(off > 113);
    }

    /**
     * Presses jump and notes where this jump began. A jump may go up a level after the first one: speed
     * telly always, Telly Up when it starts facing forward. Going up means turning round early, while you
     * are still high, so that the blocks go in one level up and you come down on them. Landing still
     * turned round leaves no time for that, and that jump is filled in late and flat instead.
     */
    private void takeOff(boolean mayGoUp) {
        // Telly Up takes turns: a jump on along the level, then one up
        upJump = mayGoUp && jumps >= 1 && speedy();
        // Telly Up, after the first jump: two layers at once, a block on, a block on top of it
        pairJump = tellyUp() && jumps >= 1;
        mc.options.keyJump.setDown(true);
        pressedJump = true;
        jumps++;
        tops = 0;
        stayT = 0;
        forwardT = 0;
        backFills = 0;
        baseDone = false;
        airT = 0;
        runStart = Integer.MAX_VALUE;
        bumped = false;
        int[] here = last(floorY);
        takeoffEnd = here == null ? 0 : index(here);
    }

    /** Whether there is a bridge block of this level under your feet, this far on along the bridge. */
    private boolean under(double advance, int level) {
        for (int i = -1; i <= 1; i += 2) {
            double a = advance + i * 0.15;
            int bx = dx != 0 ? floor(mc.player.getX() + dx * a) : lineX;
            int bz = dz != 0 ? floor(mc.player.getZ() + dz * a) : lineZ;
            if (solid(bx, level, bz)) return true;
        }
        return false;
    }

    /** The last solid block of the bridge at this level, from where you are back: {x, z}, or null within seven. */
    /**
     * The flat bridge: whether a click where the camera points now does one of the two things wanted,
     * a block on top of a block of the bridge (nothing on top of that) or a block on the end of the bridge.
     * Follows the line of sight in small steps to the first solid block.
     */
    private boolean flatClick(int level) {
        double yaw = Math.toRadians(mc.player.getYRot()), pitch = Math.toRadians(mc.player.getXRot());
        double vx = -Math.sin(yaw) * Math.cos(pitch), vy = -Math.sin(pitch), vz = Math.cos(yaw) * Math.cos(pitch);
        double ox = mc.player.getX(), oy = mc.player.getEyeY(), oz = mc.player.getZ();
        int px = floor(ox), py = floor(oy), pz = floor(oz);
        for (double t = 0.02; t < 4.4; t += 0.02) {
            int bx = floor(ox + vx * t), by = floor(oy + vy * t), bz = floor(oz + vz * t);
            if (solid(bx, by, bz)) {
                if (by != level) return false;
                // In through the top: a block on top, if it is the bridge and nothing is on it
                if (py == by + 1 && px == bx && pz == bz) return (dx != 0 ? bz == lineZ : bx == lineX) && replaceable(bx, by + 1, bz);
                // In through the side that faces the way you go: the next block of the bridge
                return py == by && px == bx + dx && pz == bz + dz && replaceable(px, py, pz);
            }
            px = bx;
            py = by;
            pz = bz;
        }
        return false;
    }

    /** How far along the bridge a block is, in blocks, growing the way you go. */
    private int index(int[] block) {
        return dx != 0 ? block[0] * dx : block[1] * dz;
    }

    /**
     * The last block of the run this jump is building at this level: a block of that level from the step
     * of this jump on. Blocks further back are bumps of earlier jumps.
     */
    private int[] run(int level) {
        int[] end = last(level);
        return end != null && runStart != Integer.MAX_VALUE && index(end) >= runStart ? end : null;
    }

    private int[] last(int level) {
        int along = dx != 0 ? floor(mc.player.getX()) * dx : floor(mc.player.getZ()) * dz;
        for (int i = 0; i < 7; i++) {
            int a = along - i;
            int bx = dx != 0 ? a * dx : lineX, bz = dz != 0 ? a * dz : lineZ;
            if (solid(bx, level, bz)) return new int[] { bx, bz };
        }
        return null;
    }

    private void forward() {
        mc.options.keyDown.setDown(false);
        mc.options.keyUp.setDown(true);
        mc.options.keySprint.setDown(true);
    }

    /** Remembers the block the player stands on as the bridge. */
    private void noteFloor() {
        floorY = floor(mc.player.getY() - 0.2);
        lineX = floor(mc.player.getX());
        lineZ = floor(mc.player.getZ());
    }

    /** Turns the camera towards a direction, at most turn-speed degrees. True once it points there. */
    private boolean turn(float yaw, float pitch) {
        return turn(yaw, pitch, turnSpeed.get().floatValue());
    }

    private boolean turn(float yaw, float pitch, float speed) {
        float current = mc.player.getYRot();
        float dy = wrap(yaw - current), dp = pitch - mc.player.getXRot();
        float stepY = Math.max(-speed, Math.min(speed, dy)), stepP = Math.max(-speed, Math.min(speed, dp));
        mc.player.setYRot(current + stepY);
        mc.player.setXRot(mc.player.getXRot() + stepP);
        return Math.abs(dy - stepY) < 0.5f && Math.abs(dp - stepP) < 0.5f;
    }

    private static double clamp(double value, double low, double high) {
        return Math.max(low, Math.min(high, value));
    }

    private boolean facing(float yaw) {
        return Math.abs(wrap(yaw - mc.player.getYRot())) < 20;
    }

    private static float wrap(float degrees) {
        degrees %= 360;
        if (degrees >= 180) degrees -= 360;
        if (degrees < -180) degrees += 360;
        return degrees;
    }

    private static int floor(double value) {
        return (int) Math.floor(value);
    }

    private boolean solid(int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        return !mc.level.getBlockState(pos).getCollisionShape(mc.level, pos).isEmpty();
    }

    private boolean replaceable(int x, int y, int z) {
        return mc.level.getBlockState(new BlockPos(x, y, z)).canBeReplaced();
    }

    /** Keeps a stack of blocks selected, picking the next one in the hotbar when a stack runs out. */
    private boolean holdBlocks() {
        if (mc.player.getMainHandItem().getItem() instanceof BlockItem) return true;
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getItem(i).getItem() instanceof BlockItem) {
                mc.player.getInventory().setSelectedSlot(i);
                return true;
            }
        }
        return false;
    }

    private void stop(String why) {
        info(why);
        forceOff = true;
        toggle();
    }

    private boolean finishing, finishJumped, forceOff;
    private int finishT;

    /** Switched off while bridging: with finish-jump on it turns forward and jumps on first. */
    @Override
    public void toggle() {
        if (isActive() && running && !forceOff && !finishing && finishJump.get() && mode.get() != Mode.Diagonal && mc.player != null) {
            finishing = true;
            finishJumped = false;
            finishT = 0;
            return;
        }
        forceOff = false;
        finishing = false;
        super.toggle();
    }

    private void finishStep() {
        finishT++;
        float yaw = mc.player.getYRot();
        float step = Math.max(-60, Math.min(60, wrap(forwardYaw - yaw)));
        mc.player.setYRot(yaw + step);
        mc.player.setXRot(mc.player.getXRot() + (15 - mc.player.getXRot()) * 0.4f);
        boolean straight = Math.abs(wrap(forwardYaw - mc.player.getYRot())) < 25;
        mc.options.keyDown.setDown(false);
        mc.options.keyLeft.setDown(false);
        mc.options.keyRight.setDown(false);
        mc.options.keyUp.setDown(straight);
        mc.options.keySprint.setDown(straight);
        if (straight && mc.player.onGround() && !finishJumped) {
            mc.options.keyJump.setDown(true);
            pressedJump = true;
            finishJumped = true;
            finishT = 0;
        } else if (finishJumped && mc.player.onGround() && finishT > 3 || finishT > 40) {
            forceOff = true;
            toggle();
        }
    }

    // ---- Telly Replay: the cycle worked out from the recording and checked in a simulation of the
    // game's movement. One jump is twenty ticks, eight blocks, four blocks on and one level up. ----
    /** 0 running up to the first jump, 1 the level jump, 2 the jump up. */
    private int rStage;
    private int rT;
    /** The level of the blocks the jump left from. */
    private int rWalk;

    private int along1() {
        return dx != 0 ? dx : dz;
    }

    /** Where you are along the bridge, growing the way it goes. */
    private double alongNow() {
        return (dx != 0 ? mc.player.getX() : mc.player.getZ()) * along1();
    }

    /** Cell c along the bridge is the block from c to c + 1. */
    private int cellX(int c) {
        return dx != 0 ? (dx > 0 ? c : -c - 1) : lineX;
    }

    private int cellZ(int c) {
        return dz != 0 ? (dz > 0 ? c : -c - 1) : lineZ;
    }

    private boolean cell(int c, int level) {
        return solid(cellX(c), level, cellZ(c));
    }

    /** The cell of the last block of a level near you, or Integer.MIN_VALUE. */
    private int lastCell(int level) {
        int from = floor(alongNow()) + 4;
        for (int c = from; c >= from - 12; c--) {
            if (cell(c, level)) return c;
        }
        return Integer.MIN_VALUE;
    }

    /** Would a block in this cell be in your way: the game does not place a block inside you. */
    private boolean inside(int c, int level) {
        double p = alongNow(), y = mc.player.getY();
        return c < p + 0.3 - 1e-6 && c + 1 > p - 0.3 + 1e-6 && level < y + 1.8 - 1e-6 && level + 1 > y + 1e-6;
    }

    /**
     * Clicks a face of a block: its front (the side the bridge grows from) or its top. Of the points on
     * that face that can be seen, it takes the one nearest to where the camera is meant to be looking
     * anyway, so a click made while turning does not swing the camera back.
     */
    private boolean replayClick(int c, int level, boolean top, float wantYaw) {
        double ex = mc.player.getX(), ey = mc.player.getEyeY(), ez = mc.player.getZ();
        double bestYaw = 0, bestPitch = 0, bestCost = Double.MAX_VALUE;
        for (int i = 0; i <= 8; i++) {
            double aside = 0.1 + 0.1 * i;
            for (int j = 0; j <= (top ? 8 : 0); j++) {
                double a = top ? c + 0.1 + 0.1 * j : c + 1;
                double y = top ? level + 1 : level + 0.25 + 0.1 * (i % 3) + 0.5;
                double wx = dx != 0 ? a * along1() : lineX + aside;
                double wz = dz != 0 ? a * along1() : lineZ + aside;
                double ax = wx - ex, ay = y - ey, az = wz - ez;
                double flat = Math.sqrt(ax * ax + az * az);
                if (ax * ax + ay * ay + az * az > 4.4 * 4.4) continue;
                float yaw = flat < 0.05 ? wantYaw : (float) Math.toDegrees(Math.atan2(-ax, az));
                float pitch = (float) -Math.toDegrees(Math.atan2(ay, flat));
                if (pitch > 89.5f) continue;
                if (!replaySees(ex, ey, ez, wx, y, wz)) continue;
                double cost = Math.abs(wrap(yaw - wantYaw)) * (flat < 0.4 ? flat / 0.4 : 1) + Math.abs(aside - 0.5) * 6;
                if (cost < bestCost) {
                    bestCost = cost;
                    bestYaw = yaw;
                    bestPitch = pitch;
                }
            }
        }
        if (bestCost == Double.MAX_VALUE) return false;
        mc.player.setYRot(wantYaw + wrap((float) bestYaw - wantYaw));
        mc.player.setXRot((float) bestPitch);
        KeyMapping.click(((KeyMappingAccessor) mc.options.keyUse).meteor$getKey());
        return true;
    }

    /** Nothing solid between the eye and a point on a face: otherwise the click would land on what is in the way. */
    private boolean replaySees(double ex, double ey, double ez, double tx, double ty, double tz) {
        double ax = tx - ex, ay = ty - ey, az = tz - ez;
        double length = Math.sqrt(ax * ax + ay * ay + az * az);
        for (double d = 0.05; d < length - 0.04; d += 0.05) {
            double f = d / length;
            if (solid(floor(ex + ax * f), floor(ey + ay * f), floor(ez + az * f))) return false;
        }
        return true;
    }

    /** A block on from the front of the last block of a level; false when you are not past that front yet. */
    private boolean replayExtend(int level, float wantYaw) {
        int e = lastCell(level);
        if (e == Integer.MIN_VALUE) return false;
        if (alongNow() <= e + 1 + 0.01 || inside(e + 1, level) || !replaceable(cellX(e + 1), level, cellZ(e + 1))) return false;
        return replayClick(e, level, false, wantYaw);
    }

    /** A block on top of the block in cell c of a level; false when it would be inside you or is taken. */
    private boolean replayTop(int c, int level, float wantYaw) {
        if (!cell(c, level) || inside(c, level + 1) || !replaceable(cellX(c), level + 1, cellZ(c))) return false;
        return replayClick(c, level, true, wantYaw);
    }

    /** Ticks until your feet come down to a height, and where along the bridge you are then; null if never. */
    private double[] replayLanding(double height) {
        double x = alongNow(), y = mc.player.getY();
        double vx = (dx != 0 ? mc.player.getDeltaMovement().x : mc.player.getDeltaMovement().z) * along1();
        double vy = mc.player.getDeltaMovement().y;
        for (int k = 1; k <= 16; k++) {
            vx += 0.0196;
            x += vx;
            y += vy;
            if (vy < 0 && y <= height) return new double[] {k, x};
            vy = (vy - 0.08) * 0.98;
            vx *= 0.91;
        }
        return null;
    }

    /**
     * Telly Replay is not worked out: it is one round of a player's recording, played back. Read off
     * the film frame by frame (the camera's direction from the far scenery, the keys from the key
     * display, the blocks from the count in the hotbar) and brought to the game's twenty ticks a second:
     * where the camera points (degrees from straight ahead, left is negative, and degrees down), which
     * keys are down, and how many clicks. A round is twenty-one ticks and two jumps; the jump key is
     * pressed whenever the feet touch down, which is also what keeps the table in step.
     */
    private static final float[] REC_YAW = {-55.1f, -29.1f, -23.1f, -52.9f, -107.7f, -167.9f, -179.3f, -179.3f, -178.7f, -176.7f, -173.4f, -168.0f, -162.2f, -157.0f, -150.7f, -143.4f, -137.4f, -129.5f, -111.8f, -83.9f, -58.9f};
    private static final float[] REC_PITCH = {75.3f, 69.1f, 62.2f, 56.9f, 51.3f, 45.0f, 43.9f, 44.0f, 44.7f, 47.2f, 51.6f, 56.9f, 61.5f, 64.8f, 67.6f, 70.5f, 72.3f, 73.2f, 74.7f, 76.6f, 76.3f};
    private static final String[] REC_KEYS = {"D", "WD", "WD", "WSD", "SD", "SD", "SD", "S", "S", "S", "S", "S", "S", "S", "S", "S", "SD", "SD", "D", "WD", "WD"};
    private static final int[] REC_CLICKS = {0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 1, 1, 0, 0, 1, 0};
    /** The tick of the table at which the second jump of a round is taken. */
    private static final int REC_SECOND_JUMP = 11;
    /**
     * The table tick a round's first jump is set to. One tick into the swing forward the camera is about
     * 29 degrees off straight ahead with forward held, so the jump is a sprint jump whose push runs
     * along the bridge; at tick 0 (55 degrees, no forward key) it is a walking jump a block shorter, and
     * a sprint jump there throws you off to the side (tools/telly-sim/lateral.py).
     */
    private static final int REC_FIRST_JUMP = 1;

    private void replay() {
        int stand = floor(mc.player.getY() - 0.2);
        boolean ground = mc.player.onGround();

        if (rStage == 0) {
            // Before the first jump, as the recording begins: straight ahead, looking down, at a run to
            // the end of the block
            floorY = stand;
            mc.player.setYRot(forwardYaw);
            mc.player.setXRot(mc.player.getXRot() + (57 - mc.player.getXRot()) * 0.5f);
            mc.options.keyDown.setDown(false);
            mc.options.keyLeft.setDown(false);
            mc.options.keyRight.setDown(false);
            mc.options.keyUp.setDown(true);
            mc.options.keySprint.setDown(true);
            int e = lastCell(stand);
            if (ground && e != Integer.MIN_VALUE && alongNow() >= e + 1 - 0.1) {
                replayJump(1, stand);
                rT = 2;
            }
            return;
        }

        if (ground) {
            // Touched down: the next jump at once, and the table set to the jump it belongs to
            floorY = stand;
            boolean second = rT >= 6 && rT <= 15;
            replayJump(second ? 2 : 1, stand);
            rT = second ? REC_SECOND_JUMP : REC_FIRST_JUMP;
        }

        int k = rT % REC_YAW.length;
        // The recording's camera, nudged a few degrees towards the middle line of the bridge when you
        // have drifted off it: the recording is only five rounds long, and played back blind the error
        // of every round adds up
        double aside = (mc.player.getX() - (lineX + 0.5)) * -dz + (mc.player.getZ() - (lineZ + 0.5)) * dx;
        double drift = mc.player.getDeltaMovement().x * -dz + mc.player.getDeltaMovement().z * dx;
        float trim = (float) Math.max(-10, Math.min(10, -(aside + drift * 3) * 40));
        float want = forwardYaw + REC_YAW[k] + trim;
        mc.player.setYRot(want);
        mc.player.setXRot(REC_PITCH[k]);
        String keys = REC_KEYS[k];
        boolean forward = keys.indexOf('W') >= 0;
        mc.options.keyUp.setDown(forward);
        mc.options.keySprint.setDown(forward);
        mc.options.keyDown.setDown(keys.indexOf('S') >= 0);
        mc.options.keyLeft.setDown(keys.indexOf('A') >= 0);
        mc.options.keyRight.setDown(keys.indexOf('D') >= 0);
        rT = (k + 1) % REC_YAW.length;

        // Which block each click is for follows the bridge such a round leaves behind (a side view of
        // one, and the same rules run in tools/telly-sim/try4.py): every level is two blocks, a gap where
        // you stood to jump, and then a long row, each level lying over the end of the one below. The
        // point clicked is the one on that face nearest to where the recording looks, so the camera
        // moves a few degrees at most.
        if (ground) return;
        double p = alongNow();
        if (rStage == 1) {
            int e = lastCell(rWalk);
            if (e == Integer.MIN_VALUE) return;
            if (k <= 3) {
                // Just off the ground, looking forward and down: this level goes on over the layer
                // below, where that reaches further
                int c = e + 1;
                if (c <= floor(p) + 2 && cell(c, rWalk - 1)) replayTop(c, rWalk - 1, want);
                return;
            }
            // Then on from its end, a block a tick, as late as still puts one under you
            double[] land = replayLanding(rWalk + 1);
            if (land == null) return;
            int n = floor(land[1]) - e;
            if (n > 0 && land[0] <= n) replayExtend(rWalk, want);
            return;
        }
        // The jump up: first the blocks on top of the cells behind the one you jumped from, then the
        // level you left one block further, then on top of that, where you come down
        int up = rWalk + 1;
        double[] land = replayLanding(up + 1);
        if (land == null) {
            if (mc.player.getDeltaMovement().y < 0) replayExtend(rWalk, want);
            return;
        }
        int target = floor(land[1]);
        // One block on top behind the cell you jumped from; your own cell stays open, which is the gap
        // every level of such a bridge has after its first block
        for (int c = rCell - 1; c <= rCell - 1 && rBehind < 1; c++) {
            if (replayTop(c, rWalk, want)) {
                rBehind++;
                return;
            }
        }
        int e = lastCell(rWalk);
        if (e != Integer.MIN_VALUE && e < target + 1 && replayExtend(rWalk, want)) return;
        // In the recording the block you come down on is not the highest: one more goes on top of the
        // block behind first (frame 113), and only then the block for your feet, a level lower and in
        // front of it (frame 114)
        if (!rHigher && rBehind >= 1 && mc.player.getY() >= up + 1 && replayTop(rCell - 1, up, want)) {
            rHigher = true;
            return;
        }
        for (int c = rCell + 1; c <= target; c++) {
            if (replayTop(c, rWalk, want)) return;
        }
    }

    /** The cell the jump up left from, and how many blocks have gone on top behind it. */
    private int rCell, rBehind;
    private boolean rHigher;

    private void replayJump(int stage, int level) {
        rStage = stage;
        rWalk = level;
        rCell = floor(alongNow());
        rBehind = 0;
        rHigher = false;
        rT = 0;
        jumps++;
        mc.options.keyJump.setDown(true);
        pressedJump = true;
    }

    /** 1 when the camera turns round to the right, -1 to the left. */
    private int turnSign() {
        if (mode.get() == Mode.TellyReplay) return replaySide.get() == Side.Left ? -1 : 1;
        return mode.get() != Mode.Diagonal && turnSide.get() == Side.Left ? -1 : 1;
    }

    /**
     * Telly Replay: the keys that push you straight along the bridge with the camera this many degrees
     * from straight ahead, as the recording shows them: forward and strafe at 45, strafe alone at 90,
     * back and strafe at 135, back alone looking straight back. The strafe key is the one away from the
     * side the camera is turned to.
     */
    private void replayKeys(float off) {
        boolean strafe = off > 22.5f && off < 157.5f;
        mc.options.keyUp.setDown(off < 67.5f);
        // The level jump is taken at a sprint
        mc.options.keySprint.setDown(off < 67.5f);
        mc.options.keyDown.setDown(off > 112.5f);
        mc.options.keyLeft.setDown(strafe && turnSign() > 0);
        mc.options.keyRight.setDown(strafe && turnSign() < 0);
    }

    /** How far you are past the front of the last block of the level you stand on; negative while it is still ahead. */
    private double overhang() {
        int[] end = last(floor(mc.player.getY() - 0.2));
        if (end == null) return 9;
        double plane = dx != 0 ? end[0] + 0.5 + dx * 0.5 : end[1] + 0.5 + dz * 0.5;
        return dx != 0 ? (mc.player.getX() - plane) * dx : (mc.player.getZ() - plane) * dz;
    }

    /** Telly Up or its replay rhythm: both build the same two layers. */
    private boolean tellyUp() {
        return mode.get() == Mode.TellyUp || mode.get() == Mode.TellyReplay;
    }

    /** Ticks for half a turn. The recording turns in two to three, so Telly Replay keeps to that. */
    private int turnN() {
        int n = turnTicks.get();
        return mode.get() == Mode.TellyReplay ? Math.max(2, Math.min(3, n)) : n;
    }
}
