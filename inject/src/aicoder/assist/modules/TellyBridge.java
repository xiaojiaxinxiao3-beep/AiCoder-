package aicoder.assist.modules;

import aicoder.assist.api.*;
import aicoder.assist.api.Module;
import aicoder.assist.api.Events.*;

import static aicoder.assist.Client.mc;

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
        /** Walk backwards the whole way, camera 45 degrees off, hopping. */
        Diagonal
    }

    public enum Side { Left, Right }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("bridge-mode")
        .description("Telly: face where the bridge goes; it sprints forward and turns around in the air. Diagonal: look back at 45 degrees yourself first; it does not turn you round, it holds back and a strafe key, hops and places.")
        .defaultValue(Mode.Telly)
        .build()
    );

    private final Setting<Side> side = sgGeneral.add(new EnumSetting.Builder<Side>()
        .name("side")
        .description("Diagonal mode: which strafe key is held with back. Left: look back and 45 degrees to your left before switching on. Right: to your right.")
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

    private final Setting<Double> turnSpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("turn-speed")
        .description("Most degrees the camera turns per tick. 180 turns around at once, lower is smoother but places fewer blocks.")
        .defaultValue(180)
        .range(30, 180)
        .sliderRange(30, 180)
        .build()
    );

    private final Setting<Double> edge = sgGeneral.add(new DoubleSetting.Builder()
        .name("edge-distance")
        .description("Jump when the floor ends this many blocks ahead of you.")
        .defaultValue(0.35)
        .range(0.05, 1)
        .sliderRange(0.05, 1)
        .build()
    );

    private final Setting<Double> turnBack = sgGeneral.add(new DoubleSetting.Builder()
        .name("turn-back-height")
        .description("Face forward again when you are falling and this close above the bridge.")
        .defaultValue(0.6)
        .range(0.1, 1.2)
        .sliderRange(0.1, 1.2)
        .build()
    );

    private final Setting<Boolean> holdBack = sgGeneral.add(new BoolSetting.Builder()
        .name("hold-back")
        .description("Hold the back key while turned around, which keeps you moving along the bridge.")
        .defaultValue(true)
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
    private int airTicks;

    public TellyBridge() {
        super(Category.UTILITY, "telly-bridge", "Bridges for you with sprint jumps: turns around in the air, places blocks behind, and turns forward again before landing.");
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
        // Telly: the way you face. Diagonal: you already look back and 45 degrees aside, so the bridge
        // goes the way that view belongs to and the camera is not swung round.
        float heading = mc.player.getYRot();
        if (mode.get() == Mode.Diagonal) heading -= 180 + (side.get() == Side.Left ? -45 : 45);
        int quarter = Math.floorMod(Math.round(heading / 90f), 4);
        dx = quarter == 1 ? -1 : quarter == 3 ? 1 : 0;
        dz = quarter == 0 ? 1 : quarter == 2 ? -1 : 0;
        forwardYaw = quarter * 90f;

        air = false;
        reversed = false;
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

        if (mc.screen != null) { stop("Stopped: a screen was opened."); return; }
        // The key that would undo what the module holds: back when it runs forward, forward when it walks backwards
        boolean diagonal = mode.get() == Mode.Diagonal;
        if (Input.isPressed(diagonal ? mc.options.keyUp : mc.options.keyDown) || Input.isPressed(mc.options.keyShift)) { stop("Stopped."); return; }
        if (!holdBlocks()) { stop("Stopped: out of blocks."); return; }

        double feet = mc.player.getY() - (floorY + 1);
        if (feet < -0.75) { stop("Stopped: fell off the bridge."); return; }
        if (diagonal) { diagonal(); return; }

        if (mc.player.onGround()) {
            air = false;
            reversed = false;
            turn(ahead(), mc.player.getXRot() > 60 ? 20 : mc.player.getXRot());
            forward();

            // The floor ends just ahead: jump. The game reads the key later in this tick.
            double ax = mc.player.getX() + dx * edge.get(), az = mc.player.getZ() + dz * edge.get();
            if (facing(forwardYaw) && !solid(floor(ax), floorY, floor(az))) {
                mc.options.keyJump.setDown(true);
                pressedJump = true;
            }
            return;
        }

        if (!air) {
            air = true;
            airTicks = 0;
        }
        airTicks++;

        // Coming down onto the bridge: face forward so the sprint carries on from the landing
        boolean falling = mc.player.getDeltaMovement().y < 0;
        if (airTicks > 2 && falling && feet < turnBack.get() && solid(floor(mc.player.getX()), floorY, floor(mc.player.getZ()))) {
            reversed = false;
            turn(ahead(), 20);
            forward();
            return;
        }

        // Turned around: aim at the side of the last block of the bridge and right click
        reversed = true;
        mc.options.keyUp.setDown(false);
        mc.options.keySprint.setDown(false);
        mc.options.keyDown.setDown(holdBack.get());

        int along = dx != 0 ? floor(mc.player.getX()) * dx : floor(mc.player.getZ()) * dz;
        int bx = 0, bz = 0;
        boolean found = false;
        for (int i = 0; i < 7 && !found; i++) {
            int a = along - i;
            bx = dx != 0 ? a * dx : lineX;
            bz = dz != 0 ? a * dz : lineZ;
            found = solid(bx, floorY, bz);
        }
        if (!found) {
            turn(forwardYaw + 180, 78);
            return;
        }

        // The face that looks along the bridge. It can only be clicked from beyond its plane.
        // Aim at the point of it straight behind you, so the camera stays square to the bridge and the
        // back key does not push you sideways.
        double ex = mc.player.getX(), ey = mc.player.getEyeY(), ez = mc.player.getZ();
        double fx = dx != 0 ? bx + 0.5 + dx * 0.5 : clamp(ex, bx + 0.12, bx + 0.88);
        double fz = dz != 0 ? bz + 0.5 + dz * 0.5 : clamp(ez, bz + 0.12, bz + 0.88);
        double fy = floorY + 0.55;
        double beyond = (ex - fx) * dx + (ez - fz) * dz;
        double vx = fx - ex, vy = fy - ey, vz = fz - ez;
        double flat = Math.sqrt(vx * vx + vz * vz);
        float pitch = (float) -Math.toDegrees(Math.atan2(vy, flat));

        if (beyond < 0.08 || !replaceable(bx + dx, floorY, bz + dz)) {
            // Not past the face yet: wait with the camera ready
            turn(forwardYaw + 180, Math.max(70, Math.min(86, pitch)));
            return;
        }

        float yaw = (float) Math.toDegrees(Math.atan2(-vx, vz));
        if (turn(yaw, pitch) && vx * vx + vy * vy + vz * vz < 4.4 * 4.4) {
            KeyMapping.click(Input.keyOf(mc.options.keyUse));
        }
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
            KeyMapping.click(Input.keyOf(mc.options.keyUse));
        }
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
        float speed = turnSpeed.get().floatValue();
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
