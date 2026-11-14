package aicoder.combat;

import meteordevelopment.meteorclient.events.entity.EntityAddedEvent;
import meteordevelopment.meteorclient.events.entity.player.InteractItemEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.AbstractWindChargeEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3d;

/**
 * Throws a wind charge after your ender pearl so that the two meet in the air. A pearl counts a wind
 * charge as something it can land on, so it goes off there and you arrive in mid-air.
 *
 * <p>The wind charge flies straight at a constant 1.5 blocks per tick while the pearl slows down and
 * falls, so the charge catches up from behind. The module steps the pearl forward tick by tick, finds the
 * tick where a charge thrown now would be level with it, and throws along that line. The longer the delay
 * after the pearl, the further out (and, thrown upwards, the higher) they meet.
 */
public class WindPearl extends Module {
    /** Blocks per tick of a thrown wind charge. It has no gravity and no drag. */
    private static final double CHARGE_SPEED = 1.5;
    private static final double PEARL_GRAVITY = 0.03;
    private static final double PEARL_DRAG = 0.99;
    /** A pearl hits a wind charge when it passes within about this distance of its centre. */
    private static final double HIT_RADIUS = 1.1;
    private static final int HORIZON = 80;
    /** Ticks the pearl on screen trails the one on the server even with no ping; found by trial in single player. */
    private static final int CLIENT_LAG = 2;

    private enum Stage {
        Idle,
        Wait,
        Throw,
        SwitchBack,
        /** The charge is in the air: watch how close it gets, to report back. */
        Watch
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgAim = settings.createGroup("Prediction");

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Ticks between the pearl leaving your hand and the wind charge. More delay means they meet further away.")
        .defaultValue(2)
        .range(0, 30)
        .sliderRange(0, 15)
        .build()
    );

    private final Setting<Boolean> switchBack = sgGeneral.add(new BoolSetting.Builder()
        .name("switch-back")
        .description("Select the slot you had before, after the wind charge is thrown.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> lookBack = sgGeneral.add(new BoolSetting.Builder()
        .name("look-back")
        .description("Turn your view back to where it was after the throw. Off leaves you looking along the wind charge.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> feedback = sgGeneral.add(new BoolSetting.Builder()
        .name("feedback")
        .description("Say in chat where the two are expected to meet, or why nothing was thrown.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> pingLead = sgAim.add(new BoolSetting.Builder()
        .name("ping-compensation")
        .description("Aim further ahead by the time your connection takes, since the server sees the wind charge later than you throw it.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> extraLead = sgAim.add(new IntSetting.Builder()
        .name("extra-lead")
        .description("Ticks added to the lead. Raise it if the wind charge passes behind the pearl, lower it if it passes in front.")
        .defaultValue(0)
        .range(-10, 20)
        .sliderRange(-5, 10)
        .build()
    );

    private final Setting<Double> maxMiss = sgAim.add(new DoubleSetting.Builder()
        .name("max-miss")
        .description("Largest predicted gap between pearl and wind charge, in blocks, at which the charge is still thrown. They touch at about 1.1.")
        .defaultValue(0.9)
        .range(0.1, 3)
        .sliderRange(0.1, 2)
        .build()
    );

    private final Setting<Boolean> report = sgAim.add(new BoolSetting.Builder()
        .name("report-result")
        .description("After each throw, say in chat how close the wind charge came to the pearl and on which side, to help you tune extra-lead.")
        .defaultValue(false)
        .build()
    );

    private Stage stage = Stage.Idle;
    private AbstractWindChargeEntity charge;
    private Vec3d aimDir;
    private double closest, closestAlong;
    private int watched, closestAt;
    private EnderPearlEntity pearl;
    private int pearlUsed = -100;
    private int ticks;
    private int wait;
    private int returnSlot = -1;
    private boolean clicking;
    private boolean thrown;

    // Result of the last prediction
    private float aimYaw, aimPitch;
    private Vec3d meet;
    private int meetTick;
    private double miss;

    public WindPearl() {
        super(AiCoderTab.UTILITY, "wind-pearl", "After you throw an ender pearl, throws a wind charge aimed to meet it in the air, so the pearl goes off there and takes you with it.");
    }

    @Override
    public void onActivate() {
        reset();
        pearlUsed = -100;
    }

    private void reset() {
        stage = Stage.Idle;
        pearl = null;
        wait = 0;
    }

    /** The player pressed use with a pearl in hand: the next pearl that appears next to them is theirs. */
    @EventHandler
    private void onInteractItem(InteractItemEvent event) {
        if (clicking || mc.player == null) return;
        if (mc.player.getStackInHand(event.hand).isOf(Items.ENDER_PEARL)) pearlUsed = ticks;
    }

    @EventHandler
    private void onEntityAdded(EntityAddedEvent event) {
        if (stage != Stage.Idle || mc.player == null) return;
        if (!(event.entity instanceof EnderPearlEntity thrown)) return;
        if (ticks - pearlUsed > 40) return;
        if (thrown.getOwner() != mc.player && thrown.squaredDistanceTo(mc.player.getEyePos()) > 36) return;

        if (findCharge() < 0) {
            if (feedback.get()) warning("No wind charge in your hotbar.");
            return;
        }

        pearl = thrown;
        this.thrown = false;
        pearlUsed = -100;
        wait = delay.get();
        stage = Stage.Wait;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        ticks++;
        if (stage == Stage.Idle) return;
        if (mc.player == null || mc.world == null || mc.interactionManager == null) {
            reset();
            return;
        }

        if (wait > 0) {
            wait--;
            return;
        }

        switch (stage) {
            case Wait -> {
                if (pearl == null || !pearl.isAlive() || mc.currentScreen != null) {
                    reset();
                    return;
                }

                int slot = findCharge();
                if (slot < 0) {
                    reset();
                    return;
                }
                returnSlot = mc.player.getInventory().getSelectedSlot();
                mc.player.getInventory().setSelectedSlot(slot);
                stage = Stage.Throw;
                throwCharge();
            }

            case Throw -> throwCharge();

            case SwitchBack -> {
                if (switchBack.get() && returnSlot >= 0 && returnSlot < 9 && mc.player.getMainHandStack().isOf(Items.WIND_CHARGE)) {
                    mc.player.getInventory().setSelectedSlot(returnSlot);
                }
                if (report.get() && thrown && pearl != null) {
                    stage = Stage.Watch;
                    charge = null;
                    closest = Double.MAX_VALUE;
                    watched = 0;
                } else {
                    reset();
                }
            }

            case Watch -> watch();

            default -> reset();
        }
    }

    private void throwCharge() {
        if (pearl == null || !pearl.isAlive()) {
            finish();
            return;
        }

        ItemStack held = mc.player.getMainHandStack();
        if (!held.isOf(Items.WIND_CHARGE)) {
            finish();
            return;
        }
        // Still cooling down from an earlier throw: try again next tick, while the pearl is in the air
        if (mc.player.getItemCooldownManager().isCoolingDown(held) || mc.player.isUsingItem()) return;

        if (!predict()) {
            if (feedback.get()) warning("No line found where the wind charge meets the pearl (closest %.1f blocks). Nothing thrown.", miss);
            finish();
            return;
        }

        float yaw = mc.player.getYaw(), pitch = mc.player.getPitch();
        mc.player.setYaw(aimYaw);
        mc.player.setPitch(aimPitch);

        thrown = true;
        clicking = true;
        try {
            Clicks.useItem();
        } finally {
            clicking = false;
        }

        if (lookBack.get()) {
            mc.player.setYaw(yaw);
            mc.player.setPitch(pitch);
        }

        if (feedback.get()) {
            info("Meeting in %d ticks, %.1f blocks away, %.1f blocks above you.", meetTick, meet.distanceTo(mc.player.getEyePos()), meet.y - mc.player.getY());
        }
        finish();
    }

    /** Follows pearl and charge on the client and reports their closest approach. */
    private void watch() {
        watched++;

        if (charge == null) {
            for (Entity entity : mc.world.getEntities()) {
                if (entity instanceof AbstractWindChargeEntity c && c.age < 6 && (charge == null || c.age < charge.age)
                    && c.squaredDistanceTo(mc.player.getEyePos()) < 100) charge = c;
            }
        }

        boolean pearlGone = !pearl.isAlive();
        if (charge != null && charge.isAlive() && !pearlGone) {
            Vec3d between = charge.getEntityPos().subtract(pearl.getEntityPos());
            double distance = between.length();
            if (distance < closest) {
                closest = distance;
                closestAt = watched;
                closestAlong = between.dotProduct(aimDir);
            }
        }

        // Keep watching a few ticks past the closest point: a pearl that vanishes right there went off on the charge
        boolean passed = closest != Double.MAX_VALUE && watched - closestAt > 4;
        if (pearlGone || passed || watched > 100 || (charge != null && !charge.isAlive())) {
            if (closest == Double.MAX_VALUE) info("Result: the wind charge was never seen next to the pearl.");
            else info("Result: closest %.1f blocks, wind charge %s the pearl. %s", closest, closestAlong < 0 ? "behind" : "ahead of",
                pearlGone && watched - closestAt <= 3 && closest < 2.5 ? "The pearl went off there." : "The pearl flew on.");
            reset();
        }
    }

    private void finish() {
        stage = Stage.SwitchBack;
        wait = 1;
    }

    private int findCharge() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(Items.WIND_CHARGE)) return i;
        }
        return -1;
    }

    /**
     * Finds the direction for a wind charge thrown this tick. Leaves the answer in aimYaw/aimPitch/meet
     * and returns whether pearl and charge are predicted to come within reach of each other.
     */
    private boolean predict() {
        // The charge starts at the eyes, and like every thrown thing it carries the thrower's own motion
        Vec3d origin = new Vec3d(mc.player.getX(), mc.player.getEyeY(), mc.player.getZ());
        Vec3d carried = mc.player.getMovement();
        if (mc.player.isOnGround()) carried = new Vec3d(carried.x, 0, carried.z);

        double px = pearl.getX(), py = pearl.getY(), pz = pearl.getZ();
        Vec3d pv = pearl.getVelocity();
        double vx = pv.x, vy = pv.y, vz = pv.z;

        // By the time the server spawns the charge, the pearl is this many ticks further along
        int lead = CLIENT_LAG + extraLead.get() + (pingLead.get() ? Math.round(PlayerUtils.getPing() / 50f) : 0);
        lead = Math.max(0, lead);
        for (int i = 0; i < lead; i++) {
            vy -= PEARL_GRAVITY;
            vx *= PEARL_DRAG;
            vy *= PEARL_DRAG;
            vz *= PEARL_DRAG;
            px += vx;
            py += vy;
            pz += vz;
        }

        miss = Double.MAX_VALUE;
        Vec3d bestDir = null;
        BlockPos.Mutable block = new BlockPos.Mutable();

        for (int t = 1; t <= HORIZON; t++) {
            vy -= PEARL_GRAVITY;
            vx *= PEARL_DRAG;
            vy *= PEARL_DRAG;
            vz *= PEARL_DRAG;
            px += vx;
            py += vy;
            pz += vz;

            // The pearl has landed by now: nothing later can be reached
            block.set(MathHelper.floor(px), MathHelper.floor(py), MathHelper.floor(pz));
            if (!mc.world.getBlockState(block).getCollisionShape(mc.world, block).isEmpty()) break;

            // Velocity the charge needs to be exactly here after t ticks, less what the player's motion adds
            double nx = (px - origin.x) / t - carried.x;
            double ny = (py - origin.y) / t - carried.y;
            double nz = (pz - origin.z) / t - carried.z;
            double needed = Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (needed < 1e-6) continue;

            // The charge only flies at one speed, so it is ahead of or behind this point by:
            double gap = Math.abs(needed - CHARGE_SPEED) * t;
            if (gap < miss) {
                miss = gap;
                bestDir = new Vec3d(nx / needed, ny / needed, nz / needed);
                meet = new Vec3d(px, py, pz);
                meetTick = t + lead;
            }

            // Close enough to touch, and the earliest tick that is: take it
            if (gap < HIT_RADIUS * 0.4) break;
        }

        if (bestDir == null) {
            miss = Double.NaN;
            return false;
        }

        aimDir = bestDir;
        aimYaw = (float) Math.toDegrees(Math.atan2(-bestDir.x, bestDir.z));
        aimPitch = (float) -Math.toDegrees(Math.asin(MathHelper.clamp(bestDir.y, -1, 1)));
        return miss <= maxMiss.get();
    }
}
