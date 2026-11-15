package aicoder.combat;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.KeyBindingAccessor;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.item.Items;

/**
 * One wind charge jump per activation: look straight down, jump, throw a wind charge at your feet,
 * look back where you were. The module switches itself off when you land.
 *
 * <p>How high the burst sends you depends on how many ticks after the jump the charge is thrown.
 * With auto-tune on, each jump tries the next timing, measures how high you actually got, and once
 * every timing has been tried it keeps the best one in throw-delay and turns auto-tune off.
 *
 * <p>Everything is a key press: the real camera turns, the jump key and the use key are pressed.
 */
public class WindJump extends Module {
    private enum Stage { Jump, Throw, Restore, Flight }

    /** Timings auto-tune tries, in ticks after the jump key. */
    private static final int TIMINGS = 8;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> throwDelay = sgGeneral.add(new IntSetting.Builder()
        .name("throw-delay")
        .description("Ticks between pressing jump and throwing the wind charge. Auto-tune fills this in.")
        .defaultValue(0)
        .range(0, 10)
        .sliderRange(0, 10)
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
        if (!mc.player.isOnGround()) {
            warning("Stand on the ground first.");
            toggle();
            return;
        }
        delay = autoTune.get() ? tried % TIMINGS : throwDelay.get();
        restSlot = mc.player.getInventory().getSelectedSlot();
        pitch = mc.player.getPitch();
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
        if (mc.player == null || mc.world == null || stage == null) return;
        releaseJump();
        ticks++;
        peakY = Math.max(peakY, mc.player.getY());

        switch (stage) {
            case Jump -> {
                mc.player.setPitch(90);
                mc.options.jumpKey.setPressed(true);
                pressedJump = true;
                wait = delay;
                stage = Stage.Throw;
                if (wait == 0) throwCharge();
            }
            case Throw -> {
                mc.player.setPitch(90);
                if (--wait <= 0) throwCharge();
            }
            case Restore -> {
                if (mc.player.getInventory().getSelectedSlot() == chargeSlot) mc.player.getInventory().setSelectedSlot(restSlot);
                if (lookBack.get()) mc.player.setPitch(pitch);
                stage = Stage.Flight;
            }
            case Flight -> {
                boolean landed = mc.player.isOnGround() && ticks > delay + 6;
                if (landed || ticks > 200) finish();
            }
        }
    }

    /** Selects the charge and presses use; the game handles the press later in this same tick. */
    private void throwCharge() {
        mc.player.getInventory().setSelectedSlot(chargeSlot);
        KeyBinding.onKeyPressed(((KeyBindingAccessor) mc.options.useKey).meteor$getKey());
        stage = Stage.Restore;
    }

    private void finish() {
        double height = peakY - startY;
        if (report.get()) info("Throw %d ticks after the jump: %.1f blocks.", delay, height);
        if (autoTune.get()) {
            measured[delay] = height;
            tried++;
            if (tried >= TIMINGS) {
                int best = 0;
                for (int i = 1; i < TIMINGS; i++) if (measured[i] > measured[best]) best = i;
                throwDelay.set(best);
                autoTune.set(false);
                tried = 0;
                info("Best timing: %d ticks (%.1f blocks). Saved, auto-tune is off now.", best, measured[best]);
            }
        }
        toggle();
    }

    private void releaseJump() {
        if (!pressedJump) return;
        pressedJump = false;
        // Leave the key as the keyboard has it, in case the player is holding jump too
        mc.options.jumpKey.setPressed(Input.isPressed(mc.options.jumpKey));
    }

    private int findCharge() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(Items.WIND_CHARGE)) return i;
        }
        return -1;
    }
}
