package aicoder.combat;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.KeyMappingAccessor;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.item.Items;

/**
 * One wind charge jump per activation: look straight down, throw a wind charge at your feet and jump,
 * look back where you were. The module switches itself off when you land.
 *
 * <p>How high the burst sends you depends on when the charge is thrown relative to the jump. Throwing one tick before the jump
 * key measured highest: the burst then reaches you just as the jump takes off.
 * With auto-tune on, each jump tries the next timing, measures how high you actually got, and once
 * every timing has been tried it keeps the best one in throw-timing and turns auto-tune off.
 *
 * <p>Everything is a key press: the real camera turns, the jump key and the use key are pressed.
 */
public class WindJump extends HintModule {
    private enum Stage { Jump, Throw, LateJump, Restore, Flight }

    /** Timings auto-tune tries: FIRST, FIRST + 1, ... ticks after the jump key. */
    private static final int TIMINGS = 8, FIRST = -3;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> throwDelay = sgGeneral.add(new IntSetting.Builder()
        .name("throw-timing")
        .description("Ticks between pressing jump and throwing the wind charge. 0 does both in the same tick; below 0 throws first and jumps that many ticks later. Auto-tune fills this in.")
        .defaultValue(-1)
        .range(-3, 10)
        .sliderRange(-3, 10)
        .build()
    );

    private final Setting<Boolean> autoTune = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-tune")
        .description("Try a different timing on each jump, measure the height, then keep the best one.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> lookBack = sgGeneral.add(new BoolSetting.Builder()
        .name("look-back")
        .description("Turn the camera back to where you were looking after the throw.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> report = sgGeneral.add(new BoolSetting.Builder()
        .name("report-height")
        .description("Say in chat how high each jump went.")
        .defaultValue(true)
        .build()
    );

    private final double[] measured = new double[TIMINGS];
    private int tried;

    private Stage stage;
    private int wait, ticks, delay, restSlot, chargeSlot;
    private float pitch;
    private double startY, peakY;
    private boolean pressedJump;

    public WindJump() {
        super(AiCoderTab.UTILITY, "wind-jump", "Jumps and throws a wind charge at your feet for you, and works out the timing that sends you highest.");
    }

    @Override
    public void onActivate() {
        if (mc.player == null) return;
        chargeSlot = findCharge();
        if (chargeSlot < 0) {
            warning("No wind charge in your hotbar.");
            toggle();
            return;
        }
        if (!mc.player.onGround()) {
            warning("Stand on the ground first.");
            toggle();
            return;
        }
        delay = autoTune.get() ? FIRST + tried % TIMINGS : throwDelay.get();
        restSlot = mc.player.getInventory().getSelectedSlot();
        pitch = mc.player.getXRot();
        startY = peakY = mc.player.getY();
        stage = Stage.Jump;
        ticks = 0;
    }

    @Override
    public void onDeactivate() {
        releaseJump();
        stage = null;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.level == null || stage == null) return;
        releaseJump();
        ticks++;
        peakY = Math.max(peakY, mc.player.getY());

        switch (stage) {
            case Jump -> {
                mc.player.setXRot(90);
                if (delay < 0) {
                    // Throw first, jump a little later
                    throwCharge();
                    wait = -delay;
                    stage = Stage.LateJump;
                    return;
                }
                mc.options.keyJump.setDown(true);
                pressedJump = true;
                wait = delay;
                stage = Stage.Throw;
                if (wait == 0) throwCharge();
            }
            case LateJump -> {
                if (mc.player.getInventory().getSelectedSlot() == chargeSlot) mc.player.getInventory().setSelectedSlot(restSlot);
                if (--wait <= 0) {
                    mc.options.keyJump.setDown(true);
                    pressedJump = true;
                    if (lookBack.get()) mc.player.setXRot(pitch);
                    stage = Stage.Flight;
                } else mc.player.setXRot(90);
            }
            case Throw -> {
                mc.player.setXRot(90);
                if (--wait <= 0) throwCharge();
            }
            case Restore -> {
                if (mc.player.getInventory().getSelectedSlot() == chargeSlot) mc.player.getInventory().setSelectedSlot(restSlot);
                if (lookBack.get()) mc.player.setXRot(pitch);
                stage = Stage.Flight;
            }
            case Flight -> {
                boolean landed = mc.player.onGround() && ticks > Math.abs(delay) + 6;
                if (landed || ticks > 200) finish();
            }
        }
    }

    /** Selects the charge and presses use; the game handles the press later in this same tick. */
    private void throwCharge() {
        mc.player.getInventory().setSelectedSlot(chargeSlot);
        KeyMapping.click(((KeyMappingAccessor) mc.options.keyUse).meteor$getKey());
        stage = Stage.Restore;
    }

    private void finish() {
        double height = peakY - startY;
        if (report.get()) info("Timing %d: %.1f blocks.", delay, height);
        if (autoTune.get()) {
            measured[delay - FIRST] = height;
            tried++;
            if (tried >= TIMINGS) {
                int best = 0;
                for (int i = 1; i < TIMINGS; i++) if (measured[i] > measured[best]) best = i;
                throwDelay.set(best + FIRST);
                autoTune.set(false);
                tried = 0;
                info("Best timing: %d ticks (%.1f blocks). Saved, auto-tune is off now.", best + FIRST, measured[best]);
            }
        }
        toggle();
    }

    private void releaseJump() {
        if (!pressedJump) return;
        pressedJump = false;
        // Leave the key as the keyboard has it, in case the player is holding jump too
        mc.options.keyJump.setDown(Input.isPressed(mc.options.keyJump));
    }

    private int findCharge() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getItem(i).is(Items.WIND_CHARGE)) return i;
        }
        return -1;
    }
}
