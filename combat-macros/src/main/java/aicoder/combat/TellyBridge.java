package aicoder.combat;

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
public class TellyBridge extends Module {
    public enum Mode {
        /** Sprint forward, turn around in the air to place. */
        Telly,
        /** Speed telly: sprint-jump without a break, flick round only to place a couple of blocks. */
        Speed,
        /** Walk backwards the whole way, camera 45 degrees off, hopping. */
        Diagonal
    }

    public enum Side { Left, Right }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("bridge-mode")
        .description("Telly: face where the bridge goes. An ordinary telly: it runs to the edge, jumps, turns round in the air, drag clicks and turns forward again; the bridge is flat, and because the camera comes down slowly while it clicks, some blocks land on top of it. Speed: speed telly, the same keys, but every jump builds one block higher so it lands sooner. Diagonal: stand with your back to where the bridge goes; it turns the camera 45 degrees aside (no half turn), holds back and a strafe key, hops and places.")
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
        .description("Speed mode: one more block on top of the second block of every jump, the way a speed telly bridge looks.")
        .defaultValue(true)
        .visible(() -> mode.get() == Mode.Speed)
        .build()
    );

    private final Setting<Double> mouseSpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("mouse-speed")
        .description("Telly mode: degrees a tick the camera comes down after turning round, while right click is drag clicked. Slower puts more blocks on top of the bridge instead of on its end.")
        .defaultValue(8)
        .range(2, 90)
        .sliderRange(2, 90)
        .visible(() -> mode.get() == Mode.Telly)
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
    private boolean rising() {
        return mode.get() == Mode.Speed;
    }

    /** 0 facing forward, 1 turning round, 2 round, 3 turning forward. */
    private int phase, phaseTick;
    /** Speed mode: jumps made since it was switched on. */
    private int jumps;
    /** Speed mode: where the bridge ended when this jump began, and whether its bump is in. */
    private int takeoffEnd;
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
        running = true;
        noteFloor();
    }

    @Override
    public void onDeactivate() {
        if (!running) return;
        running = false;
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

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!running) return;
        if (mc.player == null || mc.level == null) {
            toggle();
            return;
        }
        if (pressedJump) {
            pressedJump = false;
            mc.options.keyJump.setDown(false);
        }

        if (mc.gui.screen() != null) { stop("Stopped: a screen was opened."); return; }
        // The key that would undo what the module holds: back when it runs forward, forward when it walks backwards
        boolean diagonal = mode.get() == Mode.Diagonal;
        if (Input.isPressed(diagonal ? mc.options.keyUp : mc.options.keyDown) || Input.isPressed(mc.options.keyShift)) { stop("Stopped."); return; }
        if (!holdBlocks()) { stop("Stopped: out of blocks."); return; }

        double feet = mc.player.getY() - (floorY + 1);
        if (feet < -0.75) { stop("Stopped: fell off the bridge."); return; }
        if (diagonal) { diagonal(); return; }
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
        if (mc.player.onGround()) {
            phase = 0;
            reversed = false;
            // The flat bridge stays on the level it started on, also when you land on a block that
            // went on top of it
            if (rising() || jumps == 0) floorY = floor(mc.player.getY() - 0.2);
            mc.options.keyLeft.setDown(false);
            mc.options.keyRight.setDown(false);
            turn(ahead(), mc.player.getXRot() > 60 ? 20 : mc.player.getXRot());
            forward();
            // The first jump is an ordinary telly: run to the very edge of the block, then jump.
            // After it, jump the moment you land.
            double lip = 0.1;
            boolean edge = !solid(floor(mc.player.getX() + dx * lip), floorY, floor(mc.player.getZ() + dz * lip));
            if (facing(forwardYaw) && (jumps > 0 || edge)) {
                mc.options.keyJump.setDown(true);
                pressedJump = true;
                jumps++;
                bumped = false;
                int[] here = last(floorY);
                takeoffEnd = here == null ? 0 : index(here);
            }
            return;
        }

        double ex = mc.player.getX(), ey = mc.player.getEyeY(), ez = mc.player.getZ(), feetY = mc.player.getY();
        double vy = mc.player.getDeltaMovement().y;
        double vh = mc.player.getDeltaMovement().x * dx + mc.player.getDeltaMovement().z * dz;
        // This jump builds one block up. Already falling below that with no step in: the level you left.
        // The first jump stays on the level you left
        int level = rising() && jumps > 1 ? floorY + 1 : floorY;
        if (level > floorY && vy < 0 && feetY < level + 1 - 0.01 && run(level) == null) level = floorY;
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

        // Where to look while round. The camera never tilts further down than max-pitch, and a click is
        // only made with the crosshair on the spot.
        // Two ticks from coming down with nothing there yet is the one time it looks further down
        float cap = landIn > 0 && landIn <= 2 && !landing ? 89 : maxPitch.get().floatValue();
        float aimYaw = forwardYaw + 180, aimPitch = cap;
        boolean onSpot = false;
        int sign = dx != 0 ? dx : dz;
        double along = dx != 0 ? ex * dx : ez * dz;
        double fx = 0, fy = 0, fz = 0;
        boolean spot = false;
        // Blocks you are already past that are not there yet
        boolean late = end == null;
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
            late = beyond >= 1;
            need = Math.max(1, (int) Math.ceil(beyond + advance - 0.15));
        } else if (below != null) {
            // The step: the top of a block of the level below, far enough behind you for the camera
            // not to have to look further down than max-pitch
            double plane = dx != 0 ? below[0] + 0.5 + dx * 0.5 : below[1] + 0.5 + dz * 0.5;
            double back = Math.max(0.3, (ey - level) / Math.tan(Math.toRadians(cap)));
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
        }
        // The bump: one block on top of the second block of this jump, once you are clear of it and
        // it is not the moment to save the landing
        if (up && bumps.get() && !bumped && !(landIn > 0 && landIn <= 3 && !landing)) {
            int i = Integer.MIN_VALUE;
            if (up) {
                int[] low = last(level - 1);
                if (low != null) {
                    int at = index(low);
                    i = (solid(low[0], level, low[1]) ? at : at - 1) + 1;
                }
            }
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
                    }
                }
            }
        }
        if (spot) {
            double ax = fx - ex, ay = fy - ey, az = fz - ez;
            double flat = Math.sqrt(ax * ax + az * az);
            float pitch = (float) -Math.toDegrees(Math.atan2(ay, flat));
            if (flat >= 0.15) aimYaw = (float) Math.toDegrees(Math.atan2(-ax, az));
            if (pitch > cap + 0.5f) onSpot = false;
            aimPitch = Math.min(pitch, cap);
        }

        // Speed telly turns round as soon as it is off the ground. The ordinary telly flies on facing
        // forward first, and turns round when there is just time left to fill in the blocks behind:
        // one a tick, two ticks for the turn, two to spare.
        if (phase == 0 && !landing && (up || rising() || landIn > 0 && landIn <= need + 4)) {
            phase = 1;
            phaseTick = 0;
            clicks = 0;
        } else if (phase == 2 && landing && landIn <= 2) {
            phase = 3;
            phaseTick = 0;
        }

        if (phase == 0) {
            reversed = false;
            turn(ahead(), 20);
            forward();
            return;
        }

        reversed = true;
        // On the flat bridge the mouse comes down at mouse-speed while the clicks go on, so the first
        // clicks of a jump land on top of blocks further back instead of on the side of the last one.
        // Not when the bridge is a block behind you, or the landing has to be saved.
        boolean drift = !up && !rising() && !(landIn > 0 && landIn <= need + 1 && !landing);
        float pitchNow = mc.player.getXRot();
        float downTo = drift ? pitchNow + Math.max(-mouseSpeed.get().floatValue(), Math.min(mouseSpeed.get().floatValue(), aimPitch - pitchNow)) : aimPitch;
        // + is to the right of the way you go. Turned round, left moves you that way and right the other.
        double aside = (ex - (lineX + 0.5)) * -dz + (ez - (lineZ + 0.5)) * dx;
        mc.options.keyUp.setDown(false);
        mc.options.keySprint.setDown(false);
        if (phase == 1) {
            // Half a turn in two ticks. Back is pressed as soon as forward is let go of, left on the second.
            phaseTick++;
            mc.options.keyDown.setDown(true);
            if (phaseTick == 1) {
                mc.player.setYRot(forwardYaw + 90);
                mc.player.setXRot(drift ? downTo : (pitchNow + aimPitch) / 2);
            } else {
                mc.player.setYRot(aimYaw);
                mc.player.setXRot(downTo);
                mc.options.keyLeft.setDown(aside < 0.08);
                phase = 2;
            }
        } else if (phase == 2) {
            // Round: back and left held; right instead of left once you are off to that side.
            // The crosshair stays on the spot.
            mc.options.keyDown.setDown(true);
            mc.options.keyLeft.setDown(aside < 0.08);
            mc.options.keyRight.setDown(aside > 0.16);
            mc.player.setYRot(aimYaw);
            mc.player.setXRot(downTo);
        } else {
            // Half a turn forward in two ticks, through your left: right half way, forward again at the end
            phaseTick++;
            mc.options.keyDown.setDown(false);
            mc.options.keyLeft.setDown(false);
            if (phaseTick == 1) {
                mc.player.setYRot(forwardYaw + 270);
                mc.player.setXRot((mc.player.getXRot() + 20) / 2);
                mc.options.keyRight.setDown(true);
            } else {
                mc.player.setYRot(forwardYaw + 360);
                mc.player.setXRot(20);
                mc.options.keyRight.setDown(false);
                forward();
                phase = 0;
            }
        }

        // Drag click at cps, while the crosshair is on the spot
        if (phase == 2 && (up ? onSpot : flatClick(level))) {
            clicks += cps.get() / 20.0;
            while (clicks >= 1) {
                clicks -= 1;
                KeyMapping.click(((KeyMappingAccessor) mc.options.keyUse).meteor$getKey());
            }
        } else {
            clicks = Math.max(clicks, 1);
        }
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
     * The last block of the run being built at this level: like last(), but a block further back than one
     * behind the end of the level below is not part of it. That is the bump of the run before.
     */
    private int[] run(int level) {
        int[] end = last(level), low = last(level - 1);
        if (end == null || low == null) return end;
        return index(end) < index(low) - 1 ? null : end;
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

    /** The direction to run in: along the bridge, leaning back towards its middle when you have drifted. */
    private float ahead() {
        double off = dx != 0 ? mc.player.getZ() - (lineZ + 0.5) : mc.player.getX() - (lineX + 0.5);
        // Aim at the middle of the bridge three blocks on
        double tx = dx != 0 ? dx * 3 : -off, tz = dz != 0 ? dz * 3 : -off;
        return (float) Math.toDegrees(Math.atan2(-tx, tz));
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
        toggle();
    }
}
