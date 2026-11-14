package aicoder.combat;

import meteordevelopment.meteorclient.events.entity.player.InteractBlockEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.Hand;
import net.minecraft.item.Items;
import net.minecraft.world.World;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.Properties;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Finishes a respawn anchor the player placed by hand: select the glowstone, right click the anchor to
 * charge it, select the previous slot again, right click to set it off.
 *
 * <p>Every step is what the keyboard and mouse would do. Slots change through the hotbar selection, and a
 * click only happens while the crosshair is on the anchor, using the game's own hit result. Nothing is sent
 * that the vanilla client would not send for the same key presses, and the view is never turned.
 */
public class AnchorMacro extends Module {
    private enum Stage {
        Idle,
        /** The place click went out; waiting to see the anchor where it should appear. */
        Placed,
        SelectGlowstone,
        Charge,
        SelectBack,
        Explode
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgDelay = settings.createGroup("Delay");

    private final Setting<Boolean> explode = sgGeneral.add(new BoolSetting.Builder()
        .name("explode")
        .description("Set the anchor off after charging it. Off only adds the glowstone.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> skipSneaking = sgGeneral.add(new BoolSetting.Builder()
        .name("skip-when-sneaking")
        .description("Do nothing while you hold sneak, so you can still place anchors without setting them off.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> switchDelay = sgDelay.add(new IntSetting.Builder()
        .name("switch-delay")
        .description("Ticks to wait before each hotbar switch (after placing, and after charging).")
        .defaultValue(1)
        .range(0, 40)
        .sliderRange(0, 20)
        .build()
    );

    private final Setting<Integer> clickDelay = sgDelay.add(new IntSetting.Builder()
        .name("click-delay")
        .description("Ticks to wait between switching the hotbar slot and the right click that follows.")
        .defaultValue(1)
        .range(0, 40)
        .sliderRange(0, 20)
        .build()
    );

    private final Setting<Integer> randomDelay = sgDelay.add(new IntSetting.Builder()
        .name("random-delay")
        .description("Up to this many extra ticks are added to every wait, picked at random each time.")
        .defaultValue(0)
        .range(0, 20)
        .sliderRange(0, 10)
        .build()
    );

    private final Setting<Integer> giveUp = sgDelay.add(new IntSetting.Builder()
        .name("give-up-after")
        .description("Ticks a step may stay blocked (crosshair off the anchor, glowstone gone) before the sequence is dropped.")
        .defaultValue(20)
        .range(5, 200)
        .sliderRange(5, 100)
        .build()
    );

    private Stage stage = Stage.Idle;
    private BlockPos anchor;
    /** Where the anchor may appear: the clicked block if it was replaceable, otherwise the block beside the clicked face. */
    private BlockPos candidateA, candidateB;
    private int wait;
    private int blocked;
    private int returnSlot;
    /** True while this module performs a click, so its own clicks do not start a new sequence. */
    private boolean clicking;

    public AnchorMacro() {
        super(Categories.Combat, "anchor-macro", "After you place a respawn anchor by hand, charges it with one glowstone from your hotbar and sets it off. Plain key presses and clicks only.");
    }

    @Override
    public void onActivate() {
        reset();
    }

    @Override
    public void onDeactivate() {
        reset();
    }

    private void reset() {
        stage = Stage.Idle;
        anchor = null;
        candidateA = candidateB = null;
        wait = 0;
        blocked = 0;
    }

    private int delay(int base) {
        int extra = randomDelay.get();
        return base + (extra > 0 ? ThreadLocalRandom.current().nextInt(extra + 1) : 0);
    }

    /** The player right clicked a block. If the anchor is in their main hand, this is the placement to follow up on. */
    @EventHandler(priority = EventPriority.LOWEST)
    private void onInteractBlock(InteractBlockEvent event) {
        if (clicking || event.isCancelled() || stage != Stage.Idle) return;
        if (mc.player == null || mc.world == null) return;
        if (event.hand != Hand.MAIN_HAND || !mc.player.getMainHandStack().isOf(Items.RESPAWN_ANCHOR)) return;
        if (skipSneaking.get() && mc.player.isSneaking()) return;

        // Anchors only explode outside the Nether; there, clicking a charged one would set your spawn instead
        if (mc.world.getRegistryKey() == World.NETHER) return;
        if (findGlowstone() < 0) return;

        candidateA = event.result.getBlockPos().toImmutable();
        candidateB = candidateA.offset(event.result.getSide());
        returnSlot = mc.player.getInventory().getSelectedSlot();
        stage = Stage.Placed;
        blocked = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (stage == Stage.Idle) return;
        if (mc.player == null || mc.world == null || mc.interactionManager == null) {
            reset();
            return;
        }

        // A screen takes the keyboard and mouse away, exactly as it would from the player
        if (mc.currentScreen != null) {
            reset();
            return;
        }

        if (wait > 0) {
            wait--;
            return;
        }

        switch (stage) {
            case Placed -> {
                if (isAnchor(candidateB)) anchor = candidateB;
                else if (isAnchor(candidateA)) anchor = candidateA;

                if (anchor == null) {
                    // The click did not place anything (no room, or it opened a block instead)
                    if (++blocked > 3) reset();
                    return;
                }
                advance(Stage.SelectGlowstone, switchDelay.get());
            }

            case SelectGlowstone -> {
                if (!isAnchor(anchor)) {
                    reset();
                    return;
                }
                if (charges() > 0) {
                    // Already charged by someone else: go straight to setting it off
                    advance(explode.get() ? Stage.Explode : Stage.Idle, clickDelay.get());
                    return;
                }

                int slot = findGlowstone();
                if (slot < 0) {
                    reset();
                    return;
                }
                mc.player.getInventory().setSelectedSlot(slot);
                advance(Stage.Charge, clickDelay.get());
            }

            case Charge -> {
                if (!isAnchor(anchor)) {
                    reset();
                    return;
                }
                if (charges() > 0) {
                    advance(explode.get() ? Stage.SelectBack : Stage.Idle, switchDelay.get());
                    if (stage == Stage.Idle) restoreSlot();
                    return;
                }
                if (!mc.player.getMainHandStack().isOf(Items.GLOWSTONE) || !click()) stall();
            }

            case SelectBack -> {
                restoreSlot();
                advance(Stage.Explode, clickDelay.get());
            }

            case Explode -> {
                if (!isAnchor(anchor)) {
                    // Gone: it went off
                    reset();
                    return;
                }
                // Glowstone in either hand would add a charge instead of setting it off
                if (mc.player.getMainHandStack().isOf(Items.GLOWSTONE) || mc.player.getOffHandStack().isOf(Items.GLOWSTONE)) {
                    if (mc.player.getMainHandStack().isOf(Items.GLOWSTONE)) {
                        int other = findOther();
                        if (other >= 0) mc.player.getInventory().setSelectedSlot(other);
                    }
                    stall();
                    return;
                }
                if (charges() == 0 || !click()) stall();
                else {
                    // One click is all a player would make; the block disappears when the server answers
                    stage = Stage.Idle;
                    anchor = null;
                }
            }

            default -> reset();
        }
    }

    private void advance(Stage next, int base) {
        stage = next;
        wait = delay(base);
        blocked = 0;
        if (next == Stage.Idle) anchor = null;
    }

    /** A step could not run this tick. Try again next tick, up to the give-up limit. */
    private void stall() {
        if (++blocked > giveUp.get()) {
            restoreSlot();
            reset();
        }
    }

    private void restoreSlot() {
        if (mc.player.getMainHandStack().isOf(Items.GLOWSTONE) && returnSlot >= 0 && returnSlot < 9) {
            mc.player.getInventory().setSelectedSlot(returnSlot);
        }
    }

    private boolean isAnchor(BlockPos pos) {
        return pos != null && mc.world.getBlockState(pos).isOf(Blocks.RESPAWN_ANCHOR);
    }

    private int charges() {
        BlockState state = mc.world.getBlockState(anchor);
        return state.isOf(Blocks.RESPAWN_ANCHOR) ? state.get(Properties.CHARGES) : 0;
    }

    private int findGlowstone() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(Items.GLOWSTONE)) return i;
        }
        return -1;
    }

    /** A hotbar slot that does not hold glowstone, preferring the one the anchor was placed from. */
    private int findOther() {
        if (returnSlot >= 0 && returnSlot < 9 && !mc.player.getInventory().getStack(returnSlot).isOf(Items.GLOWSTONE)) return returnSlot;
        for (int i = 0; i < 9; i++) {
            if (!mc.player.getInventory().getStack(i).isOf(Items.GLOWSTONE)) return i;
        }
        return -1;
    }

    /**
     * One right click, the way the use key does it: only when the crosshair is on the anchor, with the
     * game's own hit result, followed by the arm swing. Returns false when the crosshair is elsewhere.
     */
    private boolean click() {
        if (mc.player.isSneaking() || mc.player.isUsingItem()) return false;
        if (!(mc.crosshairTarget instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return false;
        if (!hit.getBlockPos().equals(anchor)) return false;

        clicking = true;
        try {
            Clicks.useOnBlock(hit);
        } finally {
            clicking = false;
        }
        return true;
    }
}
