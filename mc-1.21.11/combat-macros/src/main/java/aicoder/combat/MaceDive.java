package aicoder.combat;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.KeyBindingAccessor;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.MathHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * Elytra and mace, on a loop, against the nearest player: take off, climb straight up on fireworks until
 * high enough above them, dive at them at a slant on one more firework, put the chestplate on just before
 * arriving so the fall counts, hit with the mace, put the elytra back on and climb again.
 *
 * <p>Wear one chest piece and keep the other in the hotbar. To trade them the module selects the spare,
 * presses use (the game swaps it with what is worn) and selects the weapon again, all within one tick. The
 * server only looks at what is in the main hand once per tick, so it never sees the weapon leave the hand
 * and the sword keeps its charge. With a sword held through the dive and the mace selected only for the
 * click, the blow has the sword's attack damage and the mace's smash. A spare held in the off hand works
 * too: use with a weapon in the main hand equips what the off hand holds.
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

    private final Setting<Boolean> spearMode = sgGeneral.add(new BoolSetting.Builder()
        .name("spear-mode")
        .description("Dive with a spear held level (use key held). When it hits, put the chestplate on and follow with the mace: two hits in a moment. Needs a spear in the hotbar. The spear is levelled for the last 28 blocks, after one more firework, so dive from at least that high.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> climbPitch = sgGeneral.add(new DoubleSetting.Builder()
        .name("climb-steepness")
        .description("How steeply to climb, in degrees above level. 90 is straight up, so the dive comes down at a slant.")
        .defaultValue(90)
        .range(20, 90)
        .sliderRange(20, 90)
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
        .description("How far away a player can be and still be picked as the target. Once picked, the target is kept while you climb above it.")
        .defaultValue(40)
        .range(5, 128)
        .sliderRange(5, 100)
        .build()
    );

    private final Setting<Boolean> ignoreFriends = sgTargets.add(new BoolSetting.Builder()
        .name("ignore-friends")
        .description("Never target players on your Meteor friends list.")
        .defaultValue(true)
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
    /** Furthest a spear reaches. Nearer than 2 blocks it does not hit either. */
    private static final double SPEAR_REACH = 4.5;
    /** How far out the spear is levelled: far enough to have been held half a second on arrival. */
    private static final double SPEAR_LEAD = 28;
    private int spearPhase;
    private LivingEntity locked;

    private boolean jumpDown, boosted, useDown, spearHad, spearStrike;
    private int targetHurt;
    private double diveFrom;

    public MaceDive() {
        super(AiCoderTab.MELEE, "mace-dive", "Against the nearest player: fly up on an elytra, dive at them, swap to the chestplate and hit with the mace, then fly up and do it again.");
    }

    @Override
    public void onActivate() {
        locked = null;
        stage = Stage.Prepare;
        wait = 0;
        sinceFirework = 100;
        stageTicks = 0;
    }

    @Override
    public void onDeactivate() {
        setJump(false);
        setUse(false);
    }

    private void go(Stage next) {
        if (next != Stage.Dive) setUse(false);
        stage = next;
        stageTicks = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;
        sinceFirework++;
        stageTicks++;

        if (mc.currentScreen != null) {
            setJump(false);
            setUse(false);
            return;
        }

        LivingEntity target = findTarget();
        if (target == null) {
            setJump(false);
            setUse(false);
            return;
        }
        // The hurt flash starting on the target is how a spear hit shows on this side
        boolean struck = target.hurtTime > targetHurt;
        targetHurt = target.hurtTime;

        if (find(Items.MACE) < 0) {
            warning("No mace in your hotbar.");
            toggle();
            return;
        }

        if (wait > 0) {
            wait--;
            return;
        }

        boolean wearingElytra = mc.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA);
        Vec3d eye = mc.player.getEyePos();
        double distance = distanceTo(target, eye);
        double speed = mc.player.getVelocity().length();

        switch (stage) {
            case Prepare, Recover -> {
                setJump(false);
                if (wearingElytra) {
                    if (findChestplate() < 0 && !isChestplate(mc.player.getOffHandStack())) {
                        warning("No chestplate in your hotbar. Without one the mace hit does no fall damage.");
                        toggle();
                        return;
                    }
                    go(Stage.Launch);
                } else if (wear(true)) {
                    wait = 1;
                } else {
                    warning("No elytra: wear it or keep it in your hotbar, with the chestplate in the other place.");
                    toggle();
                }
            }

            case Launch -> {
                if (!wearingElytra) {
                    go(Stage.Prepare);
                    return;
                }
                if (mc.player.isGliding()) {
                    setJump(false);
                    go(Stage.Climb);
                    return;
                }
                // Jump is pressed on one tick and let go on the next: the first press leaves the ground, the next one in the air opens the elytra
                setJump(!jumpDown);
            }

            case Climb -> {
                if (!mc.player.isGliding()) {
                    go(wearingElytra ? Stage.Launch : Stage.Prepare);
                    return;
                }
                look(yawTo(target, eye), -climbPitch.get());

                if (mc.player.getY() - target.getY() >= height.get()) {
                    boosted = false;
                    spearHad = false;
                    spearStrike = false;
                    spearPhase = 0;
                    diveFrom = mc.player.getY() - target.getY();
                    go(Stage.Dive);
                    return;
                }
                if (sinceFirework >= fireworkGap.get()) firework();
            }

            case Dive -> {
                if (!mc.player.isGliding()) {
                    go(wearingElytra ? Stage.Launch : Stage.Prepare);
                    return;
                }
                aim(target, eye, speed);

                if (!boosted) {
                    // One firework to come down fast
                    boosted = true;
                    int early = spearMode.get() ? findSpear() : -1;
                    if (early >= 0 && distance <= SPEAR_LEAD) {
                        // A short dive: the firework and the spear in the same tick, because the spear only
                        // counts after about half a second held out and there is barely that much time
                        boolean lit = fireworkNow();
                        mc.player.getInventory().setSelectedSlot(early);
                        Clicks.useItem();
                        setUse(true);
                        spearPhase = 2;
                        if (lit && feedback.get()) info("Diving from %.0f blocks above, spear out.", diveFrom);
                        return;
                    }
                    if (firework() && feedback.get()) info("Diving from %.0f blocks above.", diveFrom);
                    return;
                }

                int spear = spearMode.get() ? findSpear() : -1;
                if (spear >= 0) {
                    if (spearPhase == 0) {
                        holdWeapon();
                        if (distance > SPEAR_LEAD) return;
                        // The last stretch: one more firework, which also holds the speed at what a firework
                        // gives instead of what a long dive builds up, then the spear
                        spearPhase = 1;
                        if (sinceFirework >= 8 && firework()) return;
                    }
                    if (spearPhase == 1) {
                        // Level the spear now. It only counts after about half a second held out.
                        mc.player.getInventory().setSelectedSlot(spear);
                        Clicks.useItem();
                        setUse(true);
                        spearPhase = 2;
                        return;
                    }
                    if (mc.player.getInventory().getSelectedSlot() != spear) mc.player.getInventory().setSelectedSlot(spear);
                    setUse(true);

                    // Spear, chestplate, mace on three ticks in a row. The hurt flash arrives a tick or two
                    // late, so the spear's turn is simply one whole tick within its reach (2 to 4.5 blocks).
                    boolean inReach = distance <= SPEAR_REACH;
                    if (struck || spearHad || distance <= Math.max(2.0, speed)) {
                        setUse(false);
                        if (mc.player.isUsingItem()) mc.interactionManager.stopUsingItem(mc.player);
                        if (feedback.get()) info("Spear %s at %.1f blocks, speed %.1f. Chestplate on, mace next.", struck ? "hit" : "in reach", distance, speed);
                        spearHad = false;
                        spearStrike = true;
                        if (wear(false)) go(Stage.Strike);
                    } else {
                        spearHad = inReach;
                    }
                    return;
                }
                holdWeapon();

                // The chestplate has to be on for a tick before the hit, or the fall does not count for the mace
                if (distance <= reach.get() + speed * 1.5 && wear(false)) go(Stage.Strike);
            }

            case Strike -> {
                aim(target, eye, speed);

                // After the spear there is no time to wait for the chosen attack range: hit as soon as the game allows
                if (distance <= (spearStrike ? Math.max(reach.get(), mc.player.getEntityInteractionRange()) : reach.get())) {
                    int mace = find(Items.MACE);
                    mc.player.getInventory().setSelectedSlot(mace);
                    KeyBinding.onKeyPressed(((KeyBindingAccessor) mc.options.attackKey).meteor$getKey());
                    if (feedback.get()) info("Mace hit at %.1f blocks, dived from %.0f above.", distance, diveFrom);
                    go(Stage.Recover);
                    wait = 1;
                } else if (stageTicks > 15 || mc.player.isOnGround()) {
                    if (feedback.get()) info("No mace hit: %s (target %.1f blocks away).", mc.player.isOnGround() ? "landed first" : "out of reach", distance);
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
        mc.options.jumpKey.setPressed(down || Input.isPressed(mc.options.jumpKey));
    }

    /** Holds the use key, or lets it go again. */
    private void setUse(boolean down) {
        if (useDown == down) return;
        useDown = down;
        mc.options.useKey.setPressed(down || Input.isPressed(mc.options.useKey));
    }

    private int findSpear() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && Registries.ITEM.getId(stack.getItem()).getPath().endsWith("_spear")) return i;
        }
        return -1;
    }

    /** One press of the use key; the game handles it later in this same tick. */
    private void pressUse() {
        KeyBinding.onKeyPressed(((KeyBindingAccessor) mc.options.useKey).meteor$getKey());
    }

    /**
     * Puts on the elytra or the chestplate, taking it from the hotbar (or the off hand) and leaving the
     * piece that was worn in its place. Returns false when the piece is not to hand.
     */
    private boolean wear(boolean elytra) {
        int slot = elytra ? find(Items.ELYTRA) : findChestplate();
        if (slot >= 0) {
            // Select, use, select the weapon again: one tick, so the server never sees the weapon put away
            mc.player.getInventory().setSelectedSlot(slot);
            Clicks.useItem();
            holdWeapon();
            return true;
        }

        ItemStack off = mc.player.getOffHandStack();
        if (elytra ? off.isOf(Items.ELYTRA) : isChestplate(off)) {
            // Use with a weapon in the main hand: the game equips what the off hand holds
            holdWeapon();
            pressUse();
            return true;
        }
        return false;
    }

    private boolean isChestplate(ItemStack stack) {
        return !stack.isEmpty() && Registries.ITEM.getId(stack.getItem()).getPath().endsWith("_chestplate");
    }

    private int findChestplate() {
        for (int i = 0; i < 9; i++) {
            if (isChestplate(mc.player.getInventory().getStack(i))) return i;
        }
        return -1;
    }

    private boolean firework() {
        int slot = find(Items.FIREWORK_ROCKET);
        if (slot < 0) return false;

        mc.player.getInventory().setSelectedSlot(slot);
        pressUse();
        sinceFirework = 0;
        return true;
    }

    /** A firework used at once, instead of through the use key later in the tick, so something else can follow in the same tick. */
    private boolean fireworkNow() {
        int slot = find(Items.FIREWORK_ROCKET);
        if (slot < 0) return false;

        mc.player.getInventory().setSelectedSlot(slot);
        Clicks.useItem();
        sinceFirework = 0;
        return true;
    }

    /** Selects the sword, or the mace when there is no sword or the swap is off. */
    private void holdWeapon() {
        int slot = useSword.get() ? findSword() : -1;
        if (slot < 0) slot = find(Items.MACE);
        if (slot >= 0 && mc.player.getInventory().getSelectedSlot() != slot) mc.player.getInventory().setSelectedSlot(slot);
    }

    private int find(net.minecraft.item.Item item) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(item)) return i;
        }
        return -1;
    }

    private int findSword() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && Registries.ITEM.getId(stack.getItem()).getPath().endsWith("_sword")) return i;
        }
        return -1;
    }

    // Aim

    private void look(double yaw, double pitch) {
        mc.player.setYaw((float) yaw);
        mc.player.setPitch((float) MathHelper.clamp(pitch, -90, 90));
    }

    private double yawTo(Entity target, Vec3d eye) {
        return Math.toDegrees(Math.atan2(-(target.getX() - eye.x), target.getZ() - eye.z));
    }

    /** Looks at the middle of the target, a little ahead of it if it is moving. */
    private void aim(LivingEntity target, Vec3d eye, double speed) {
        Vec3d centre = target.getBoundingBox().getCenter();
        double lead = Math.min(10, centre.distanceTo(eye) / Math.max(0.5, speed));
        Vec3d motion = target.getVelocity();
        double dx = centre.x + motion.x * lead - eye.x, dy = centre.y - eye.y, dz = centre.z + motion.z * lead - eye.z;

        look(Math.toDegrees(Math.atan2(-dx, dz)), -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz))));
    }

    /** Distance from the eyes to the nearest point of the target's hitbox. */
    private double distanceTo(Entity target, Vec3d eye) {
        Box box = target.getBoundingBox();
        double x = MathHelper.clamp(eye.x, box.minX, box.maxX), y = MathHelper.clamp(eye.y, box.minY, box.maxY), z = MathHelper.clamp(eye.z, box.minZ, box.maxZ);
        return eye.distanceTo(new Vec3d(x, y, z));
    }

    private LivingEntity findTarget() {
        // Once picked, a target is kept while it lives: climbing above it must not lose it, however high
        if (locked != null && locked.isAlive() && !locked.isRemoved() && mc.world.getEntityById(locked.getId()) == locked
            && locked.distanceTo(mc.player) <= range.get() + height.get() + 32) return locked;
        locked = null;

        LivingEntity best = null;
        double bestDistance = range.get();

        for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player || !entity.isAlive() || !(entity instanceof LivingEntity living)) continue;

            if (entity instanceof PlayerEntity player) {
                if (player.isSpectator()) continue;
                if (ignoreFriends.get() && Friends.get().isFriend(player)) continue;
            } else if (!mobs.get()) {
                continue;
            }

            double d = entity.distanceTo(mc.player);
            if (d < bestDistance) {
                bestDistance = d;
                best = living;
            }
        }
        locked = best;
        return best;
    }
}
