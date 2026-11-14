package aicoder.combat;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.phys.HitResult;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Turns holding the left mouse button into repeated left clicks. A short press is still one click; once
 * the button has been down for a moment the module keeps clicking for you until you let go. Each click
 * goes through the game's own left-click handling, exactly as if the button had been pressed again.
 */
public class LeftClicker extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> startAfter = sgGeneral.add(new IntSetting.Builder()
        .name("start-after")
        .description("Ticks the button has to be held before the repeated clicks begin.")
        .defaultValue(4)
        .range(0, 40)
        .sliderRange(0, 20)
        .build()
    );

    private final Setting<Integer> interval = sgGeneral.add(new IntSetting.Builder()
        .name("interval")
        .description("Ticks from one click to the next. 1 is twenty clicks a second, 2 is ten.")
        .defaultValue(2)
        .range(1, 40)
        .sliderRange(1, 20)
        .build()
    );

    private final Setting<Integer> randomDelay = sgGeneral.add(new IntSetting.Builder()
        .name("random-delay")
        .description("Up to this many extra ticks are added between clicks, picked at random each time.")
        .defaultValue(0)
        .range(0, 20)
        .sliderRange(0, 10)
        .build()
    );

    private final Setting<Boolean> skipBlocks = sgGeneral.add(new BoolSetting.Builder()
        .name("not-on-blocks")
        .description("Do nothing while the crosshair is on a block, so holding the button still mines normally.")
        .defaultValue(true)
        .build()
    );

    private int held;
    private int wait;

    public LeftClicker() {
        super(AiCoderTab.MELEE, "left-clicker", "Hold the left mouse button and it turns into repeated left clicks, at the pace you set.");
    }

    @Override
    public void onActivate() {
        held = 0;
        wait = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.level == null || mc.gui.screen() != null || !Input.isPressed(mc.options.keyAttack)) {
            held = 0;
            wait = 0;
            return;
        }

        if (held++ < startAfter.get()) return;
        if (wait > 0) {
            wait--;
            return;
        }
        if (skipBlocks.get() && mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK) return;

        Utils.leftClick();
        // Meteor's click releases the key afterwards; it is still physically down, so put it back
        mc.options.keyAttack.setDown(true);
        wait = interval.get() - 1 + (randomDelay.get() > 0 ? ThreadLocalRandom.current().nextInt(randomDelay.get() + 1) : 0);
    }
}
