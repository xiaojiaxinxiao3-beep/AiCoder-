package aicoder.cruise;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.pathing.IPathManager;
import meteordevelopment.meteorclient.pathing.NopPathManager;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Takes off, climbs above the build limit on fireworks, then travels with the "pitch 40" glide,
 * which trades height for speed and back and needs no fireworks at all.
 *
 * <pre>
 * [WALK_TO_SKY] -> TAKEOFF -> CLIMB -> CRUISE
 *                     ^________________|   landed, auto-takeoff on
 *                              ^_______|   sank too far below the glide band
 * </pre>
 */
public class ElytraCruise extends Module {
    public enum DirectionMode {
        Locked,
        Custom,
        Free
    }

    private enum State {
        WALK_TO_SKY,
        TAKEOFF,
        CLIMB,
        CRUISE
    }

    // The pitch 40 numbers, taken from Meteor's ElytraFly Pitch40 mode
    private static final float DIVE_PITCH = 37.72F;
    private static final float PULL_UP_PITCH = -54.77F;
    private static final float PULL_UP_SPEED = 5.45F;
    private static final float PUSH_DOWN_SPEED = 0.90F;
    /** Nose-down rate once above the band. At Pitch40's own 0.90 the pull-up coasts 60 to 80 blocks past the top; this fast it is under 30. */
    private static final float OVERSHOOT_PUSH_DOWN_SPEED = 5.45F;

    /** How far below the glide band the player may sink before fireworks are used to get back up. */
    private static final int RECOVERY_MARGIN = 20;
    private static final int SKY_SEARCH_RADIUS = 32;
    /** One block of height difference is as bad as this many blocks of walking. */
    private static final int HEIGHT_COST = 8;
    /** Baritone reports "not pathing" while it is still calculating, so give it this long to start. */
    private static final int PATH_START_GRACE = 60;
    /** Coming down sooner than this after a takeoff means it hit something. */
    private static final int FAILED_TAKEOFF_TICKS = 60;
    private static final int MAX_FAILED_TAKEOFFS = 3;
    private static final int TAKEOFF_RETRY_DELAY = 20;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgAltitude = settings.createGroup("Altitude");
    private final SettingGroup sgTakeoff = settings.createGroup("Takeoff");
    private final SettingGroup sgSafety = settings.createGroup("Safety");

    // General

    private final Setting<DirectionMode> directionMode = sgGeneral.add(new EnumSetting.Builder<DirectionMode>()
        .name("direction-mode")
        .description("Locked: keep the direction you faced when enabling. Custom: fly along the yaw setting. Free: steer with the mouse.")
        .defaultValue(DirectionMode.Locked)
        .build()
    );

    private final Setting<Double> yaw = sgGeneral.add(new DoubleSetting.Builder()
        .name("yaw")
        .description("Direction to fly in. 0 = south (+Z), 90 = west (-X), 180 = north (-Z), -90 = east (+X).")
        .defaultValue(0)
        .range(-180, 180)
        .sliderRange(-180, 180)
        .visible(() -> directionMode.get() == DirectionMode.Custom)
        .build()
    );

    private final Setting<Double> turnSpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("turn-speed")
        .description("Most degrees to turn per tick. Turning fast costs speed.")
        .defaultValue(4)
        .range(0.5, 20)
        .sliderRange(0.5, 20)
        .visible(() -> directionMode.get() != DirectionMode.Free)
        .build()
    );

    // Altitude

    private final Setting<Integer> heightAboveBuildLimit = sgAltitude.add(new IntSetting.Builder()
        .name("height-above-build-limit")
        .description("Top of the glide, in blocks above the build limit.")
        .defaultValue(20)
        .range(0, 200)
        .sliderRange(0, 200)
        .build()
    );

    private final Setting<Integer> glideBand = sgAltitude.add(new IntSetting.Builder()
        .name("glide-band")
        .description("How far the glide dives before pulling back up. Under 40 there is not enough speed to climb back.")
        .defaultValue(40)
        .range(40, 150)
        .sliderRange(40, 150)
        .build()
    );

    // Takeoff

    private final Setting<Boolean> autoTakeoff = sgTakeoff.add(new BoolSetting.Builder()
        .name("auto-takeoff")
        .description("Take off again after landing. When off, the module turns itself off on landing.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> climbPitch = sgTakeoff.add(new DoubleSetting.Builder()
        .name("climb-pitch")
        .description("Pitch while climbing on fireworks. Closer to -90 gains more height per firework.")
        .defaultValue(-75)
        .range(-90, -30)
        .sliderRange(-90, -30)
        .build()
    );

    private final Setting<Double> minClimbSpeed = sgTakeoff.add(new DoubleSetting.Builder()
        .name("min-climb-speed")
        .description("Use the next firework when speed drops below this, in blocks per tick.")
        .defaultValue(0.8)
        .range(0.3, 1.5)
        .sliderRange(0.3, 1.5)
        .build()
    );

    private final Setting<Integer> fireworkCooldown = sgTakeoff.add(new IntSetting.Builder()
        .name("firework-cooldown")
        .description("Fewest ticks between two fireworks.")
        .defaultValue(12)
        .range(5, 60)
        .sliderRange(5, 60)
        .build()
    );

    private final Setting<Boolean> useBaritone = sgTakeoff.add(new BoolSetting.Builder()
        .name("use-baritone")
        .description("When there is no open sky overhead, walk to the nearest open spot first. Needs Baritone.")
        .defaultValue(true)
        .build()
    );

    // Safety

    private final Setting<Integer> minElytraDurability = sgSafety.add(new IntSetting.Builder()
        .name("min-elytra-durability")
        .description("Turn off when the elytra has this little durability left.")
        .defaultValue(10)
        .range(0, 100)
        .sliderRange(0, 100)
        .build()
    );

    private final Setting<Boolean> replenishFireworks = sgSafety.add(new BoolSetting.Builder()
        .name("replenish-fireworks")
        .description("Move fireworks from the inventory to the hotbar when the hotbar has none.")
        .defaultValue(true)
        .build()
    );

    private State state = State.TAKEOFF;
    private float lockedYaw;

    private boolean pitchingDown = true;
    private float pitch = DIVE_PITCH;

    private int ticksSinceFirework;
    private int ticksSinceFlyPacket;
    private int ticksPathing;
    private int ticksSinceTakeoff;
    private int takeoffDelay;
    private int failedTakeoffs;
    private boolean startedPathing;
    private boolean warnedNoSky;

    public ElytraCruise() {
        super(Categories.Movement, "elytra-cruise", "Takes off, climbs above the build limit with fireworks, then glides onward without using any.");
    }

    @Override
    public void onActivate() {
        startedPathing = false;
        warnedNoSky = false;
        ticksSinceFirework = 1000;
        ticksSinceFlyPacket = 1000;
        ticksSinceTakeoff = 1000;
        takeoffDelay = 0;
        failedTakeoffs = 0;

        if (mc.player == null) {
            state = State.TAKEOFF;
            return;
        }

        lockedYaw = mc.player.getYRot();

        // Enabled mid-flight: skip the takeoff. CLIMB hands over to CRUISE by itself when already high enough.
        state = mc.player.isFallFlying() ? State.CLIMB : State.TAKEOFF;
    }

    @Override
    public void onDeactivate() {
        stopPathing();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.level == null) return;

        ticksSinceFirework++;
        ticksSinceFlyPacket++;
        ticksSinceTakeoff++;

        ItemStack chest = mc.player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.getItem() != Items.ELYTRA) {
            error("No elytra in the chest slot.");
            toggle();
            return;
        }

        int durabilityLeft = chest.getMaxDamage() - chest.getDamageValue();
        if (durabilityLeft <= minElytraDurability.get()) {
            warning("Elytra has only %d durability left, stopping.", durabilityLeft);
            toggle();
            return;
        }

        switch (state) {
            case WALK_TO_SKY -> tickWalkToSky();
            case TAKEOFF -> tickTakeoff();
            case CLIMB -> tickClimb();
            case CRUISE -> tickCruise();
        }
    }

    // WALK_TO_SKY

    private void tickWalkToSky() {
        IPathManager paths = PathManagers.get();
        ticksPathing++;

        boolean arrived = hasOpenSky(mc.player.blockPosition()) && mc.player.onGround();
        boolean gaveUp = !paths.isPathing() && ticksPathing > PATH_START_GRACE;

        if (!arrived && !gaveUp) return;

        stopPathing();
        if (!arrived) warnNoSky("Could not reach open sky, trying to take off here.");
        state = State.TAKEOFF;
    }

    /** @return true when the walk was started and the state changed */
    private boolean startWalkToSky() {
        if (!useBaritone.get()) return false;

        IPathManager paths = PathManagers.get();
        if (paths instanceof NopPathManager) return false;

        BlockPos target = findOpenSky();
        if (target == null) return false;

        info("No open sky here, walking to %d %d %d with %s.", target.getX(), target.getY(), target.getZ(), paths.getName());
        // moveTo(pos) is satisfied next to the block, which can still be under the edge of the roof.
        // Ignoring Y makes the goal the column itself.
        paths.moveTo(target, true);

        startedPathing = true;
        ticksPathing = 0;
        state = State.WALK_TO_SKY;
        return true;
    }

    /**
     * The nearest standable spot with nothing above it. Under a roof every column's surface is the roof itself,
     * so height difference is weighted heavily: a spot outside at the player's level beats the roof right overhead.
     */
    private BlockPos findOpenSky() {
        BlockPos origin = mc.player.blockPosition();
        BlockPos best = null;
        double bestCost = Double.MAX_VALUE;

        for (int dx = -SKY_SEARCH_RADIUS; dx <= SKY_SEARCH_RADIUS; dx++) {
            for (int dz = -SKY_SEARCH_RADIUS; dz <= SKY_SEARCH_RADIUS; dz++) {
                if (dx * dx + dz * dz > SKY_SEARCH_RADIUS * SKY_SEARCH_RADIUS) continue;

                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                if (!mc.level.hasChunk(x >> 4, z >> 4)) continue;

                // First free block above the highest block that stops movement
                int y = mc.level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                if (y <= mc.level.getMinY()) continue; // nothing to stand on

                // Water and lava count as motion blocking, but you cannot take off from them
                if (!mc.level.getFluidState(new BlockPos(x, y - 1, z)).isEmpty()) continue;

                double cost = Math.sqrt(dx * dx + dz * dz) + HEIGHT_COST * Math.abs(y - origin.getY());
                if (cost < bestCost) {
                    bestCost = cost;
                    best = new BlockPos(x, y, z);
                }
            }
        }

        return best;
    }

    /** Nothing that stops movement at or above this position. Unlike canSeeSky(), glass and leaves count as a roof. */
    private boolean hasOpenSky(BlockPos pos) {
        return mc.level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ()) <= pos.getY();
    }

    private void stopPathing() {
        if (!startedPathing) return;

        startedPathing = false;
        PathManagers.get().stop();
    }

    private void warnNoSky(String message) {
        if (warnedNoSky) return;

        warnedNoSky = true;
        warning(message);
    }

    // TAKEOFF

    private void tickTakeoff() {
        if (mc.player.isFallFlying()) {
            info("Airborne, climbing to Y %d.", top());
            useFirework();
            ticksSinceTakeoff = 0;
            state = State.CLIMB;
            return;
        }

        if (mc.player.onGround()) {
            if (takeoffDelay > 0) {
                takeoffDelay--;
                return;
            }

            if (!hasOpenSky(mc.player.blockPosition()) && !warnedNoSky) {
                if (startWalkToSky()) return;

                warnNoSky(PathManagers.get() instanceof NopPathManager && useBaritone.get()
                    ? "No open sky overhead and Baritone is not installed. Walk to an open spot; trying to take off anyway."
                    : "No open sky overhead. Walk to an open spot; trying to take off anyway.");
            }

            steer();
            mc.player.jumpFromGround();
            return;
        }

        steer();

        // Meteor sends this on the same tick as the jump and servers sometimes ignore that. Once falling it is accepted.
        if (mc.player.getDeltaMovement().y < 0 && ticksSinceFlyPacket >= 5) {
            ticksSinceFlyPacket = 0;
            mc.getConnection().send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
            mc.player.startFallFlying();
        }
    }

    // CLIMB

    private void tickClimb() {
        if (landed()) return;

        if (mc.player.getY() >= top()) {
            info("Reached cruise altitude, gliding between Y %d and %d.", top() - glideBand.get(), top());
            failedTakeoffs = 0;
            pitchingDown = true;
            pitch = mc.player.getXRot();
            state = State.CRUISE;
            return;
        }

        steer();
        mc.player.setXRot(climbPitch.get().floatValue());

        // Only when the last boost has nearly run out, so none are wasted
        if (mc.player.getDeltaMovement().length() < minClimbSpeed.get() && ticksSinceFirework >= fireworkCooldown.get()) {
            if (!useFirework()) {
                error("Out of fireworks.");
                toggle();
            }
        }
    }

    // CRUISE

    private void tickCruise() {
        if (landed()) return;

        int upper = top();
        int lower = upper - glideBand.get();
        double y = mc.player.getY();

        if (y < lower - RECOVERY_MARGIN) {
            info("Lost too much height, climbing back to Y %d.", upper);
            state = State.CLIMB;
            return;
        }

        if (pitchingDown && y <= lower) pitchingDown = false;
        else if (!pitchingDown && y >= upper) pitchingDown = true;

        if (!pitchingDown) {
            // Pull up hard to turn the speed back into height
            pitch -= PULL_UP_SPEED;

            if (pitch < PULL_UP_PITCH) {
                pitch = PULL_UP_PITCH;
                pitchingDown = true;
            }
        } else if (pitch < DIVE_PITCH) {
            // Then ease back into the dive
            pitch = Math.min(pitch + (y >= upper ? OVERSHOOT_PUSH_DOWN_SPEED : PUSH_DOWN_SPEED), DIVE_PITCH);
        }

        steer();
        mc.player.setXRot(pitch);
    }

    // Shared

    /** Top of the glide band, and the height CLIMB aims for. */
    private int top() {
        return mc.level.getMaxY() + heightAboveBuildLimit.get();
    }

    /** Handles touching down or losing the glide in the air. @return true when the state changed */
    private boolean landed() {
        if (mc.player.isFallFlying() && !mc.player.onGround()) return false;

        // Under a roof every attempt ends right back on the ground and costs a firework, so stop after a few
        if (ticksSinceTakeoff < FAILED_TAKEOFF_TICKS) {
            ticksSinceTakeoff = 1000;

            if (++failedTakeoffs >= MAX_FAILED_TAKEOFFS) {
                error("Takeoff failed %d times in a row. Is something overhead?", failedTakeoffs);
                toggle();
                return true;
            }
        }

        if (autoTakeoff.get()) {
            takeoffDelay = TAKEOFF_RETRY_DELAY;
            info("No longer gliding, taking off again.");
            warnedNoSky = false;
            state = State.TAKEOFF;
        } else {
            info("Landed.");
            toggle();
        }

        return true;
    }

    /** Turns towards the chosen direction by the shortest way, a little per tick. */
    private void steer() {
        float target;

        switch (directionMode.get()) {
            case Locked -> target = lockedYaw;
            case Custom -> target = yaw.get().floatValue();
            default -> {
                return;
            }
        }

        float current = mc.player.getYRot();
        float difference = Mth.wrapDegrees(target - current);
        float step = turnSpeed.get().floatValue();

        mc.player.setYRot(current + Mth.clamp(difference, -step, step));
    }

    /** @return false when there is no firework to use */
    private boolean useFirework() {
        FindItemResult result = InvUtils.findInHotbar(Items.FIREWORK_ROCKET);

        if (!result.found() && replenishFireworks.get()) {
            FindItemResult stored = InvUtils.find(Items.FIREWORK_ROCKET);

            if (stored.found()) {
                FindItemResult empty = InvUtils.findInHotbar(ItemStack::isEmpty);
                InvUtils.move().from(stored.slot()).toHotbar(empty.found() && !empty.isOffhand() ? empty.slot() : 8);
                result = InvUtils.findInHotbar(Items.FIREWORK_ROCKET);
            }
        }

        if (!result.found()) return false;

        if (result.isOffhand()) {
            mc.gameMode.useItem(mc.player, InteractionHand.OFF_HAND);
            mc.player.swing(InteractionHand.OFF_HAND, mc.player.getOffhandItem().getInteractAnimation(), false);
        } else {
            InvUtils.swap(result.slot(), true);

            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            mc.player.swing(InteractionHand.MAIN_HAND, mc.player.getMainHandItem().getInteractAnimation(), false);

            InvUtils.swapBack();
        }

        ticksSinceFirework = 0;
        return true;
    }
}
