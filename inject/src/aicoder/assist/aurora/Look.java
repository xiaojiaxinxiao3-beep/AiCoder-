package aicoder.assist.aurora;

import aicoder.assist.api.BoolSetting;
import aicoder.assist.api.DoubleSetting;
import aicoder.assist.api.EnumSetting;
import aicoder.assist.api.IntSetting;
import aicoder.assist.api.Module;
import aicoder.assist.api.Setting;
import aicoder.assist.api.SettingGroup;

/**
 * How the menu looks. Not a module in the list: it only borrows the settings machinery, so the same
 * popup, sliders and saving work for it. Opened with STYLE in the top bar.
 */
public class Look extends Module {
    public enum Skin { Aurora, Glass }

    private final SettingGroup sgLook = settings.createGroup("Look");
    private final SettingGroup sgGlass = settings.createGroup("Glass");
    private final SettingGroup sgImage = settings.createGroup("Background image");

    public final Setting<Skin> skin = sgLook.add(new EnumSetting.Builder<Skin>()
        .name("skin")
        .description("Aurora glass or liquid glass. Each has both layouts.")
        .defaultValue(Skin.Aurora)
        .build()
    );

    public final Setting<Boolean> tinted = sgGlass.add(new BoolSetting.Builder()
        .name("tinted")
        .description("Color the glass with the two colors below. Off: clear glass.")
        .defaultValue(true)
        .visible(this::glass)
        .build()
    );

    public final Setting<Integer> r1 = channel("color-1-red", "Top-left glass color: red.", 52);
    public final Setting<Integer> g1 = channel("color-1-green", "Top-left glass color: green.", 132);
    public final Setting<Integer> b1 = channel("color-1-blue", "Top-left glass color: blue.", 255);
    public final Setting<Integer> r2 = channel("color-2-red", "Bottom-right glass color: red.", 142);
    public final Setting<Integer> g2 = channel("color-2-green", "Bottom-right glass color: green.", 58);
    public final Setting<Integer> b2 = channel("color-2-blue", "Bottom-right glass color: blue.", 232);

    public final Setting<Double> opacity = sgGlass.add(new DoubleSetting.Builder()
        .name("glass-opacity")
        .description("How solid the glass is.")
        .defaultValue(0.5)
        .range(0.1, 1)
        .sliderRange(0.1, 1)
        .visible(this::glass)
        .build()
    );

    public final Setting<Double> dim = sgGlass.add(new DoubleSetting.Builder()
        .name("backdrop-dim")
        .description("How much the world behind the menu is darkened.")
        .defaultValue(0.35)
        .range(0, 1)
        .sliderRange(0, 1)
        .visible(this::glass)
        .build()
    );

    public final Setting<Double> imageOpacity = sgImage.add(new DoubleSetting.Builder()
        .name("image-opacity")
        .description("How strongly the background image shows inside the glass.")
        .defaultValue(1.0)
        .range(0, 1)
        .sliderRange(0, 1)
        .visible(this::glass)
        .build()
    );

    public Look() {
        super(null, "style", "Aurora or liquid glass, the glass colors, and the image behind the glass.");
        // Not a real module: nothing to say in chat, so no chat-hints row
        settings.groups.removeIf(group -> group.name.equals("General"));
    }

    private Setting<Integer> channel(String name, String description, int value) {
        return sgGlass.add(new IntSetting.Builder()
            .name(name)
            .description(description)
            .defaultValue(value)
            .range(0, 255)
            .sliderRange(0, 255)
            .visible(() -> glass() && tinted.get())
            .build()
        );
    }

    public boolean glass() {
        return skin.get() == Skin.Glass;
    }
}
