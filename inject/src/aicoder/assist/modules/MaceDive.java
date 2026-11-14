package aicoder.assist.modules;

import aicoder.assist.api.*;
import aicoder.assist.api.Module;
import aicoder.assist.api.Events.*;

import static aicoder.assist.Client.mc;

import net.minecraft.client.KeyMapping;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Elytra and mace, on a loop, against the nearest player: take off, climb on fireworks until high enough
 * above them, dive at them on one more firework, put the chestplate on just before arriving so the fall
 * counts, hit with the mace, put the elytra back on and climb again.
 *
 * <p>The two chest pieces trade places through the off hand: wear one and hold the other there. Pressing
 * use with a sword or mace in the main hand does nothing for the main hand, so the game equips what the
 * off hand holds, and the main hand never changes. That matters for the hit: with a sword held through
 * the dive and the mace selected only for the click, the blow keeps the sword's attack damage and charge
 * and adds the mace's smash.
 *
 * <p>Everything is a key press (jump, use, attack, hotbar slots) and the view really turns.
 */
public class MaceDive extends Module {
    private enum Stage {
        /** Getting the elytra on. */
        Prepare,
        /** Jumping and opening the elytra. */
        Launch,
        Climb,
        Dive,
        /** Chestplate is on; waiting to be within reach. */
        Strike,
        /** Putting the elytra back on after the hit. */
        Recover
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgTargets = settings.createGroup("Targets");

    private final Setting<Double> height = sgGeneral.add(new DoubleSetting.Builder()
        .name("height")
        .description("How many blocks above the target to climb before each dive.")
        .defaultValue(20)
        .range(5, 150)
        .sliderRange(5, 80)
        .build()
    );

    private final Setting<Double> reach = sgGeneral.add(new DoubleSetting.Builder()
        .name("attack-range")
        .description("Distance at which the hit is made.")
        .defaultValue(3)
        .range(1.5, 4.5)
        .sliderRange(1.5, 4.5)
        .build()
    );

    private final Setting<Boolean> useSword = sgGeneral.add(new BoolSetting.Builder()
        .name("sword-swap")
        .description("Hold a sword through the dive and switch to the mace only for the click, so the hit has the sword's damage and the mace's smash. Off holds the mace all the way.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> climbPitch = sgGeneral.add(new DoubleSetting.Builder()
        .name("climb-angle")
        .description("How steeply to climb, in degrees above level.")
        .defaultValue(60)
        .range(20, 85)
        .sliderRange(20, 85)
        .build()
    );

    private final Setting<Integer> fireworkGap = sgGeneral.add(new IntSetting.Builder()
        .name("firework-gap")
        .description("Fewest ticks between two fireworks while climbing.")
        .defaultValue(18)
        .range(5, 60)
        .sliderRange(5, 40)
        .build()
    );

    private final Setting<Boolean> feedback = sgGeneral.add(new BoolSetting.Builder()
        .name("feedback")
        .description("Say in chat when a dive starts and when the hit is made.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> range = sgTargets.add(new DoubleSetting.Builder()
        .name("range")
        .description("How far away a player can be and still be picked as the target.")
        .defaultValue(40)
        .range(5, 128)
        .sliderRange(5, 100)
        .build()
    );

    private final Setting<Boolean> mobs = sgTargets.add(new BoolSetting.Builder()
        .name("mobs")
        .description("Also target mobs. Meant for trying the module out in a single player world.")
        .defaultValue(false)
        .build()
    );

    private Stage stage = Stage.Prepare;
    private int wait, sinceFirework, stageTicks;
    private boolean jumpDown, boosted;
    private double diveFrom;

    public MaceDive() {
        super(Category.MELEE, "mace-dive", "Against the nearest player: fly up on an elytra, dive at them, swap to the chestplate and hit with the mace, then fly up and do it again.");
    }

    @Override
    public void onActivate() {
        stage = Stage.Prepare;
        wait = 0;
        sinceFirework = 100;
        stageTicks = 0;
    }

    @Override
    public void onDeactivate() {
        setJump(false);
    }

    private void go(Stage next) {
        stage = next;
        stageTicks = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.level == null) return;
        sinceFirework++;
        stageTicks++;

        if (mc.screen != null) {
            setJump(false);
            return;
        }

        LivingEntity target = findTarget();
        if (target == null) {
            setJump(false);
            return;
        }

        if (find(Items.MACE) < 0) {
            warning("No mace in your hotbar.");
            toggle();
            return;
        }

        if (wait > 0) {
            wait--;
            return;
        }

        boolean wearingElytra = mc.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA);
        boolean elytraInOffhand = mc.player.getOffhandItem().is(Items.ELYTRA);
        Vec3 eye = mc.player.getEyePosition();
        double distance = distanceTo(target, eye);
        double speed = mc.player.getDeltaMovement().length();

        switch (stage) {
            case Prepare, Recover -> {
                setJump(false);
                if (wearingElytra) {
                    go(Stage.Launch);
                } else if (elytraInOffhand) {
                    // Use with a weapon in the main hand: the game equips what the off hand holds
                    holdWeapon();
                    pressUse();
                    wait = 1;
                } else {
                    warning("Wear the elytra and hold the chestplate in your off hand (or the other way round).");
                    toggle();
                }
            }

            case Launch -> {
                if (!wearingElytra) {
                    go(Stage.Prepare);
                    return;
                }
                if (mc.player.isFallFlying()) {
                    setJump(false);
                    go(Stage.Climb);
                    return;
                }
                // Jump is pressed on one tick and let go on the next: the first press leaves the ground, the next one in the air opens the elytra
                setJump(!jumpDown);
            }

            case Climb -> {
                if (!mc.player.isFallFlying()) {
                    go(wearingElytra ? Stage.Launch : Stage.Prepare);
                    return;
                }
                look(yawTo(target, eye), -climbPitch.get());

                if (mc.player.getY() - target.getY() >= height.get()) {
                    boosted = false;
                    diveFrom = mc.player.getY() - target.getY();
                    go(Stage.Dive);
                    return;
                }
                if (sinceFirework >= fireworkGap.get()) firework();
            }

            case Dive -> {
                if (!mc.player.isFallFlying()) {
                    go(wearingElytra ? Stage.Launch : Stage.Prepare);
                    return;
                }
                aim(target, eye, speed);

                if (!boosted) {
                    // One firework to come down fast
                    boosted = true;
                    if (firework() && feedback.get()) info("Diving from %.0f blocks above.", diveFrom);
                    return;
                }
                holdWeapon();

                // The chestplate has to be on for a tick before the hit, or the fall does not count for the mace
                if (distance <= reach.get() + speed * 1.5) {
                    pressUse();
                    go(Stage.Strike);
                }
            }

            case Strike -> {
                aim(target, eye, speed);

                if (distance <= reach.get()) {
                    int mace = find(Items.MACE);
                    mc.player.getInventory().setSelectedSlot(mace);
                    KeyMapping.click(Input.keyOf(mc.options.keyAttack));
                    if (feedback.get()) info("Hit after falling %.1f blocks.", mc.player.fallDistance);
                    go(Stage.Recover);
                    wait = 1;
                } else if (stageTicks > 15 || mc.player.onGround()) {
                    go(Stage.Recover);
                }
            }
        }
    }

    // Keys

    private void setJump(boolean down) {
        if (jumpDown == down) return;
        jumpDown = down;
        // When letting go, leave the key as the keyboard has it
        mc.options.keyJump.setDown(down || Input.isPressed(mc.options.keyJump));
    }

    /** One press of the use key; the game handles it later in this same tick. */
    private void pressUse() {
        KeyMapping.click(Input.keyOf(mc.options.keyUse));
    }

    private boolean firework() {
        int slot = find(Items.FIREWORK_ROCKET);
        if (slot < 0) return false;

        mc.player.getInventory().setSelectedSlot(slot);
        pressUse();
        sinceFirework = 0;
        return true;
    }

    /** Selects the sword, or the mace when there is no sword or the swap is off. */
    private void holdWeapon() {
        int slot = useSword.get() ? findSword() : -1;
        if (slot < 0) slot = find(Items.MACE);
        if (slot >= 0 && mc.player.getInventory().getSelectedSlot() != slot) mc.player.getInventory().setSelectedSlot(slot);
    }

    private int find(net.minecraft.world.item.Item item) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getItem(i).is(item)) return i;
        }
        return -1;
    }

    private int findSword() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (!stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().endsWith("_sword")) return i;
        }
        return -1;
    }

    // Aim

    private void look(double yaw, double pitch) {
        mc.player.setYRot((float) yaw);
        mc.player.setXRot((float) Mth.clamp(pitch, -90, 90));
    }

    private double yawTo(Entity target, Vec3 eye) {
        return Math.toDegrees(Math.atan2(-(target.getX() - eye.x), target.getZ() - eye.z));
    }

    /** Looks at the middle of the target, a little ahead of it if it is moving. */
    private void aim(LivingEntity target, Vec3 eye, double speed) {
        Vec3 centre = target.getBoundingBox().getCenter();
        double lead = Math.min(10, centre.distanceTo(eye) / Math.max(0.5, speed));
        Vec3 motion = target.getDeltaMovement();
        double dx = centre.x + motion.x * lead - eye.x, dy = centre.y - eye.y, dz = centre.z + motion.z * lead - eye.z;

        look(Math.toDegrees(Math.atan2(-dx, dz)), -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz))));
    }

    /** Distance from the eyes to the nearest point of the target's hitbox. */
    private double distanceTo(Entity target, Vec3 eye) {
        AABB box = target.getBoundingBox();
        double x = Mth.clamp(eye.x, box.minX, box.maxX), y = Mth.clamp(eye.y, box.minY, box.maxY), z = Mth.clamp(eye.z, box.minZ, box.maxZ);
        return eye.distanceTo(new Vec3(x, y, z));
    }

    private LivingEntity findTarget() {
        LivingEntity best = null;
        double bestDistance = range.get();

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity == mc.player || !entity.isAlive() || !(entity instanceof LivingEntity living)) continue;

            if (entity instanceof Player player) {
                if (player.isSpectator()) continue;
            } else if (!mobs.get()) {
                continue;
            }

            double d = entity.distanceTo(mc.player);
            if (d < bestDistance) {
                bestDistance = d;
                best = living;
            }
        }
        return best;
    }
}
