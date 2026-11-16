package aicoder.assist.modules;

import aicoder.assist.api.*;
import aicoder.assist.api.Module;
import aicoder.assist.api.Events.*;

import static aicoder.assist.Client.mc;

import net.minecraft.client.KeyMapping;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;

/**
 * Spear first, mace right after. When you hit someone with a spear while coming down from a height, the
 * module selects the mace and left clicks for you, so the fall is spent on a mace smash as well. If you
 * are wearing an elytra it puts the chestplate from your hotbar on first: while gliding the mace has no
 * smash at all.
 *
 * <p>Both spear attacks count: the jab (left click) and the levelled spear (use key held). The jab is
 * seen when you press attack with the crosshair on someone; the levelled spear is seen when the hurt
 * flash starts on whoever is in front of you. Every step is a key press: hotbar slot, use, attack.
 */
public class SpearSmash extends Module {
    private enum Stage { Idle, Equip, Hit, Back }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> minFall = sgGeneral.add(new DoubleSetting.Builder()
        .name("min-fall")
        .description("Only follow up after falling at least this many blocks. The mace needs more than 1.5 for a smash.")
        .defaultValue(1.5)
        .range(0, 50)
        .sliderRange(0, 20)
        .build()
    );

    private final Setting<Boolean> chestplate = sgGeneral.add(new BoolSetting.Builder()
        .name("swap-chestplate")
        .description("When wearing an elytra, put on the chestplate from the hotbar (or off hand) before the mace hit.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> window = sgGeneral.add(new IntSetting.Builder()
        .name("follow-up-window")
        .description("Ticks to wait for the target to be under the crosshair and in reach of the mace before giving up.")
        .defaultValue(20)
        .range(1, 40)
        .sliderRange(1, 20)
        .build()
    );

    private final Setting<Boolean> swapBack = sgGeneral.add(new BoolSetting.Builder()
        .name("swap-back")
        .description("Select the spear again after the mace hit.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> backDelay = sgGeneral.add(new IntSetting.Builder()
        .name("swap-back-delay")
        .description("Ticks the mace stays in your hand after the hit.")
        .defaultValue(3)
        .range(1, 40)
        .sliderRange(1, 20)
        .visible(swapBack::get)
        .build()
    );

    private final Setting<Boolean> playersOnly = sgGeneral.add(new BoolSetting.Builder()
        .name("players-only")
        .description("Only follow up on players. Off also follows up on mobs.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> feedback = sgGeneral.add(new BoolSetting.Builder()
        .name("messages")
        .description("Say in chat when the mace follow-up is made, or why it was not.")
        .defaultValue(true)
        .build()
    );

    private Stage stage = Stage.Idle;
    private int wait, spearSlot = -1, maceSlot = -1;
    private Entity watched;
    private int watchedHurt;

    public SpearSmash() {
        super(Category.MELEE, "spear-smash", "When your spear hits someone while you are coming down from a height, switches to the mace and hits again. Swaps an elytra for the chestplate first.");
    }

    @Override
    public void onActivate() {
        stage = Stage.Idle;
        watched = null;
    }

    /** The jab: attack pressed with a spear in hand and someone under the crosshair. */
    @EventHandler(priority = EventPriority.LOWEST)
    private void onAttack(DoAttackEvent event) {
        if (event.isCancelled() || mc.player == null || stage != Stage.Idle) return;
        if (!isSpear(mc.player.getMainHandItem()) || !falling()) return;
        // The jab reaches further than the crosshair does, so also count whoever the spear points at
        Entity target = mc.hitResult instanceof EntityHitResult hit ? hit.getEntity() : nearestAhead();
        if (target == null || !wanted(target)) return;
        begin();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.level == null) {
            stage = Stage.Idle;
            return;
        }

        switch (stage) {
            case Idle -> watchLevelled();
            case Equip -> {
                if (mc.player.isUsingItem()) mc.gameMode.releaseUsingItem(mc.player);
                stage = Stage.Hit;
                wait = window.get();

                if (!mc.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA) || !chestplate.get()) {
                    hit();
                    return;
                }
                // Chestplate on in this tick, with the spear back in hand before the tick ends; the mace follows next tick
                int slot = findChestplate();
                if (slot >= 0) {
                    int held = mc.player.getInventory().getSelectedSlot();
                    mc.player.getInventory().setSelectedSlot(slot);
                    Clicks.useItem();
                    mc.player.getInventory().setSelectedSlot(held);
                } else if (isChestplate(mc.player.getOffhandItem())) {
                    KeyMapping.click(Input.keyOf(mc.options.keyUse));
                } else {
                    warning("No chestplate in your hotbar: with the elytra on, the mace has no smash.");
                    stage = Stage.Idle;
                }
            }
            case Hit -> hit();
            case Back -> {
                if (wait-- > 0) return;
                if (swapBack.get() && mc.player.getInventory().getSelectedSlot() == maceSlot) mc.player.getInventory().setSelectedSlot(spearSlot);
                stage = Stage.Idle;
            }
        }
    }

    /** The levelled spear: while the use key holds it out, a hurt flash starting on whoever is ahead is its hit. */
    private void watchLevelled() {
        if (!mc.player.isUsingItem() || !isSpear(mc.player.getMainHandItem())) {
            watched = null;
            return;
        }
        Entity ahead = mc.hitResult instanceof EntityHitResult hit ? hit.getEntity() : nearestAhead();
        if (ahead != watched) {
            watched = ahead;
            watchedHurt = ahead instanceof LivingEntity living ? living.hurtTime : 0;
            return;
        }
        if (!(ahead instanceof LivingEntity living)) return;

        boolean struck = living.hurtTime > watchedHurt;
        watchedHurt = living.hurtTime;
        if (struck && falling() && wanted(living)) begin();
    }

    private void begin() {
        maceSlot = find(Items.MACE);
        if (maceSlot < 0) {
            if (feedback.get()) warning("No mace in your hotbar.");
            return;
        }
        spearSlot = mc.player.getInventory().getSelectedSlot();
        // The spear's own hit goes out in this tick; the follow-up starts on the next one
        stage = Stage.Equip;
    }

    /** Selects the mace and presses attack once the crosshair is on someone in reach. */
    private void hit() {
        boolean ready = mc.hitResult instanceof EntityHitResult target && wanted(target.getEntity())
            && target.getLocation().distanceTo(mc.player.getEyePosition()) <= mc.player.entityInteractionRange();
        if (!ready) {
            if (wait-- <= 0) {
                if (feedback.get()) info("No follow-up: the target was not under the crosshair within %.1f blocks in time.", mc.player.entityInteractionRange());
                stage = Stage.Idle;
            }
            return;
        }
        mc.player.getInventory().setSelectedSlot(maceSlot);
        KeyMapping.click(Input.keyOf(mc.options.keyAttack));
        if (feedback.get()) info("Mace after the spear, fallen %.1f blocks.", mc.player.fallDistance);
        stage = Stage.Back;
        wait = backDelay.get();
    }

    private boolean falling() {
        if (mc.player.onGround()) return false;
        // Gliding keeps the fall distance near zero unless the dive is steep, so coming down on an elytra counts by itself
        if (mc.player.isFallFlying() && mc.player.getDeltaMovement().y < -0.05) return true;
        return mc.player.fallDistance >= minFall.get();
    }

    private boolean wanted(Entity entity) {
        if (!(entity instanceof LivingEntity) || !entity.isAlive() || entity == mc.player) return false;
        return entity instanceof Player || !playersOnly.get();
    }

    /** The living thing closest to where the spear points, within its reach. */
    private Entity nearestAhead() {
        Entity best = null;
        double bestScore = 0.9;
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!wanted(entity)) continue;
            double dx = entity.getX() - mc.player.getX(), dy = entity.getY() + entity.getBbHeight() / 2 - mc.player.getEyeY(), dz = entity.getZ() - mc.player.getZ();
            double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (length > 7 || length < 0.01) continue;
            double yaw = Math.toRadians(mc.player.getYRot()), pitch = Math.toRadians(mc.player.getXRot());
            double lx = -Math.sin(yaw) * Math.cos(pitch), ly = -Math.sin(pitch), lz = Math.cos(yaw) * Math.cos(pitch);
            double score = (dx * lx + dy * ly + dz * lz) / length;
            if (score > bestScore) {
                bestScore = score;
                best = entity;
            }
        }
        return best;
    }

    private int find(net.minecraft.world.item.Item item) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getItem(i).is(item)) return i;
        }
        return -1;
    }

    private int findChestplate() {
        for (int i = 0; i < 9; i++) {
            if (isChestplate(mc.player.getInventory().getItem(i))) return i;
        }
        return -1;
    }

    private boolean isChestplate(ItemStack stack) {
        return !stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().endsWith("_chestplate");
    }

    private boolean isSpear(ItemStack stack) {
        return !stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().endsWith("_spear");
    }
}
