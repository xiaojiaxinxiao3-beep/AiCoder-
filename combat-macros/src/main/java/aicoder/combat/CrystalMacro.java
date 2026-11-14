package aicoder.combat;

import meteordevelopment.meteorclient.events.entity.player.InteractBlockEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.EntityHitResult;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Hits the end crystal you just placed. Placing stays yours: this module only adds the left click, and
 * only while the crosshair is on the crystal, the way the attack key would. Holding the use key then
 * places the next one as soon as the last has gone off, so right click alone keeps the chain going.
 */
public class CrystalMacro extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> attackDelay = sgGeneral.add(new IntSetting.Builder()
        .name("attack-delay")
        .description("Ticks between the crystal appearing under your crosshair and the hit.")
        .defaultValue(1)
        .range(0, 40)
        .sliderRange(0, 20)
        .build()
    );

    private final Setting<Integer> randomDelay = sgGeneral.add(new IntSetting.Builder()
        .name("random-delay")
        .description("Up to this many extra ticks are added to the delay, picked at random each time.")
        .defaultValue(0)
        .range(0, 20)
        .sliderRange(0, 10)
        .build()
    );

    private final Setting<Boolean> ownOnly = sgGeneral.add(new BoolSetting.Builder()
        .name("only-my-crystals")
        .description("Only hit a crystal standing where you just placed one. Off hits any crystal under your crosshair shortly after you place.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> window = sgGeneral.add(new IntSetting.Builder()
        .name("give-up-after")
        .description("Ticks after placing during which the module still waits for the crystal to be under your crosshair.")
        .defaultValue(15)
        .range(3, 100)
        .sliderRange(3, 60)
        .build()
    );

    private final SettingGroup sgObsidian = settings.createGroup("Obsidian");

    private final Setting<Boolean> autoObsidian = sgObsidian.add(new BoolSetting.Builder()
        .name("auto-obsidian")
        .description("When you right click with a crystal on a block that cannot hold one, place obsidian from your hotbar there first, then the crystal on top.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> obsidianDelay = sgObsidian.add(new IntSetting.Builder()
        .name("obsidian-delay")
        .description("Ticks between your click and the obsidian being placed.")
        .defaultValue(1)
        .range(0, 40)
        .sliderRange(0, 20)
        .visible(autoObsidian::get)
        .build()
    );

    private final Setting<Integer> crystalDelay = sgObsidian.add(new IntSetting.Builder()
        .name("crystal-delay")
        .description("Ticks between the obsidian and the crystal placed on it.")
        .defaultValue(1)
        .range(0, 40)
        .sliderRange(0, 20)
        .visible(autoObsidian::get)
        .build()
    );

    private enum Base {
        None,
        /** Waiting to put obsidian against the block that was clicked. */
        Obsidian,
        /** Obsidian is down; waiting to put the crystal on it. */
        Crystal
    }

    private Base base = Base.None;
    /** Where the obsidian may end up: the clicked block if it gets replaced (grass, snow), otherwise the block beside the clicked face. */
    private BlockPos clicked, beside;
    private int baseWait, baseBlocked, crystalSlot;
    /** True while this module performs a click, so its own clicks are not mistaken for the player's. */
    private boolean clicking;

    /** Where the placed crystal stands: the block above the one that was clicked. */
    private BlockPos placedAt;
    private int armed;
    private int wait = -1;
    private EndCrystal target;

    public CrystalMacro() {
        super(AiCoderTab.EXPLOSIVES, "crystal-macro", "Hits the end crystal you just placed, after a delay you choose. Hold right click to keep placing; the module adds the left clicks.");
    }

    @Override
    public void onActivate() {
        reset();
    }

    private void reset() {
        base = Base.None;
        placedAt = null;
        armed = 0;
        wait = -1;
        target = null;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void onInteractBlock(InteractBlockEvent event) {
        if (clicking || event.isCancelled() || mc.player == null || mc.level == null) return;
        if (!mc.player.getItemInHand(event.hand).is(Items.END_CRYSTAL)) return;

        BlockPos pos = event.result.getBlockPos();
        if (holdsCrystals(pos)) {
            placedAt = pos.above();
            armed = window.get();
            return;
        }

        // Not a block a crystal can stand on: lay obsidian first, if asked to
        if (!autoObsidian.get() || base != Base.None || event.hand != InteractionHand.MAIN_HAND) return;
        if (mc.player.isShiftKeyDown() || findObsidian() < 0) return;

        clicked = pos.immutable();
        beside = clicked.relative(event.result.getDirection());
        crystalSlot = mc.player.getInventory().getSelectedSlot();
        base = Base.Obsidian;
        baseWait = obsidianDelay.get();
        baseBlocked = 0;
    }

    private boolean holdsCrystals(BlockPos pos) {
        BlockState state = mc.level.getBlockState(pos);
        return state.is(Blocks.OBSIDIAN) || state.is(Blocks.BEDROCK);
    }

    private int findObsidian() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getItem(i).is(Items.OBSIDIAN)) return i;
        }
        return -1;
    }

    /** One right click on the block under the crosshair, if that block is {@code pos}. */
    private boolean clickBlock(BlockPos pos) {
        if (!(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK || !hit.getBlockPos().equals(pos)) return false;

        clicking = true;
        try {
            Clicks.useOnBlock(hit);
        } finally {
            clicking = false;
        }
        return true;
    }

    /** The obsidian steps. Returns true while they are still running. */
    private boolean tickBase() {
        if (base == Base.None) return false;

        if (baseWait > 0) {
            baseWait--;
            return true;
        }

        if (base == Base.Obsidian) {
            int slot = findObsidian();
            if (slot < 0 || mc.player.isUsingItem()) {
                base = Base.None;
                return false;
            }

            // Switch, click and switch back within the tick, so a held use key cannot add a second block
            mc.player.getInventory().setSelectedSlot(slot);
            boolean done = clickBlock(clicked);
            mc.player.getInventory().setSelectedSlot(crystalSlot);

            if (done) {
                base = Base.Crystal;
                baseWait = crystalDelay.get();
                baseBlocked = 0;
            } else if (++baseBlocked > 10) {
                base = Base.None;
            }
            return true;
        }

        // Crystal: find where the obsidian landed and click it once the crosshair is on it
        BlockPos obsidian = mc.level.getBlockState(beside).is(Blocks.OBSIDIAN) ? beside
            : mc.level.getBlockState(clicked).is(Blocks.OBSIDIAN) ? clicked : null;

        if (obsidian != null && mc.player.getMainHandItem().is(Items.END_CRYSTAL) && clickBlock(obsidian)) {
            placedAt = obsidian.above();
            armed = window.get();
            base = Base.None;
            return false;
        }

        if (++baseBlocked > 20) base = Base.None;
        return true;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (armed <= 0 && base == Base.None) return;
        if (mc.player == null || mc.level == null || mc.gameMode == null || mc.gui.screen() != null) {
            reset();
            return;
        }

        if (tickBase() || armed <= 0) return;
        armed--;

        EndCrystal crystal = underCrosshair();
        if (crystal == null) {
            // Looked away: start the delay over when the crosshair comes back
            wait = -1;
            target = null;
            return;
        }

        if (crystal != target) {
            target = crystal;
            wait = attackDelay.get() + (randomDelay.get() > 0 ? ThreadLocalRandom.current().nextInt(randomDelay.get() + 1) : 0);
        }

        if (wait > 0) {
            wait--;
            // Keep waiting even if the window would run out during the delay
            armed = Math.max(armed, 1);
            return;
        }

        if (mc.player.isUsingItem() || mc.gameMode.isDestroying()) return;

        Clicks.attack(crystal);
        wait = -1;
        target = null;
    }

    private EndCrystal underCrosshair() {
        if (!(mc.hitResult instanceof EntityHitResult hit) || !(hit.getEntity() instanceof EndCrystal crystal)) return null;
        if (!crystal.isAlive()) return null;
        if (ownOnly.get() && (placedAt == null || !crystal.blockPosition().equals(placedAt))) return null;
        return crystal;
    }
}
