package aicoder.combat;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.orbit.EventHandler;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Turns holding the right mouse button into repeated right clicks. A short press is still one click; once
 * the button has been down for a moment the module keeps clicking for you until you let go. Each click
 * goes through the game's own right-click handling, exactly as if the button had been pressed again.
 */
public class RightClicker extends Module {
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

    private int held;
    private int wait;

    public RightClicker() {
        super(Categories.Combat, "right-clicker", "Hold the right mouse button and it turns into repeated right clicks, at the pace you set.");
    }

    @Override
    public void onActivate() {
        held = 0;
        wait = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null || mc.currentScreen != null || !Input.isPressed(mc.options.useKey)) {
            held = 0;
            wait = 0;
            return;
        }

        if (held++ < startAfter.get()) return;
        if (wait > 0) {
            wait--;
            return;
        }

        Utils.rightClick();
        wait = interval.get() - 1 + (randomDelay.get() > 0 ? ThreadLocalRandom.current().nextInt(randomDelay.get() + 1) : 0);
    }
}
