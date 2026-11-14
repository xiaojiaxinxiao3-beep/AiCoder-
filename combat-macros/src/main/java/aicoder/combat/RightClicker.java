package aicoder.combat;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.KeyMappingAccessor;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.KeyMapping;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Turns holding the right mouse button into repeated right clicks. A short press is still one click; once
 * the button has been down for a moment the module keeps clicking for you until you let go.
 *
 * <p>Each click is registered as one more press of the button, and the game then handles it like any
 * other press. The game looks at the mouse twenty times a second, so above 20 clicks per second several
 * presses are handed over at once and handled one after another within the same tick.
 */
public class RightClicker extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> cps = sgGeneral.add(new IntSetting.Builder()
        .name("cps")
        .description("Clicks per second while the button is held.")
        .defaultValue(10)
        .range(1, 100)
        .sliderRange(1, 100)
        .build()
    );

    private final Setting<Integer> variation = sgGeneral.add(new IntSetting.Builder()
        .name("variation")
        .description("Lets the pace wander up and down by this many percent, so the clicks are not perfectly even. 0 is a steady pace.")
        .defaultValue(0)
        .range(0, 50)
        .sliderRange(0, 50)
        .build()
    );

    private final Setting<Integer> startAfter = sgGeneral.add(new IntSetting.Builder()
        .name("start-after")
        .description("Ticks the button has to be held before the repeated clicks begin.")
        .defaultValue(4)
        .range(0, 40)
        .sliderRange(0, 20)
        .build()
    );

    private int held;
    /** Clicks owed: grows by cps/20 every tick, and each whole click in it is handed to the game. */
    private double owed;

    public RightClicker() {
        super(AiCoderTab.UTILITY, "right-clicker", "Hold the right mouse button and it turns into repeated right clicks, up to 100 a second.");
    }

    @Override
    public void onActivate() {
        held = 0;
        owed = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.level == null || mc.gui.screen() != null || !Input.isPressed(mc.options.keyUse)) {
            held = 0;
            owed = 0;
            return;
        }

        if (held++ < startAfter.get()) return;

        double pace = cps.get();
        if (variation.get() > 0) pace *= 1 + (ThreadLocalRandom.current().nextDouble() * 2 - 1) * variation.get() / 100.0;
        owed += pace / 20;

        while (owed >= 1) {
            owed -= 1;
            KeyMapping.click(((KeyMappingAccessor) mc.options.keyUse).meteor$getKey());
        }
    }
}
