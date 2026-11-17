package aicoder.firstcolumns.gui;

import aicoder.firstcolumns.gui.render.Palette;
import aicoder.firstcolumns.gui.render.Sdf;
import aicoder.firstcolumns.gui.screens.ColumnsScreen;
import aicoder.firstcolumns.gui.widgets.WAiCoderCheckbox;
import aicoder.firstcolumns.gui.widgets.WAiCoderModule;
import aicoder.firstcolumns.gui.widgets.WAiCoderSlider;
import aicoder.firstcolumns.gui.widgets.WAiCoderWindow;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WWindow;
import meteordevelopment.meteorclient.gui.widgets.input.WSlider;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;

/**
 * The AiCoder client look for Meteor's GUI.
 *
 * <p>It extends {@link MeteorGuiTheme} because Meteor's own widgets cast their theme to that class, and it
 * keeps the name "Meteor", so registering it replaces the stock theme.</p>
 */
public class AiCoderGuiTheme extends MeteorGuiTheme {
    /** Design pixels to Meteor GUI units. At scale 1 Meteor's text is about as tall as the 13px text of the design times this. */
    public static final double UNIT = 1.2;

    private static final String MARKER = "aicoder-palette";

    private final SettingGroup sgAiCoder = settings.createGroup("AiCoder");

    public final Setting<Boolean> glassColor = sgAiCoder.add(new BoolSetting.Builder()
        .name("glass-color")
        .description("Colours the glass with the two colours below. Off leaves clear glass that only blurs what is behind it.")
        .defaultValue(true)
        .build()
    );

    public final Setting<SettingColor> color1 = sgAiCoder.add(new ColorSetting.Builder()
        .name("color-1")
        .description("First colour of the glass, the flowing light, the switches and the sliders.")
        .defaultValue(new SettingColor(47, 224, 143))
        .build()
    );

    public final Setting<SettingColor> color2 = sgAiCoder.add(new ColorSetting.Builder()
        .name("color-2")
        .description("Second colour of the glass, the flowing light, the switches and the sliders.")
        .defaultValue(new SettingColor(47, 140, 255))
        .build()
    );

    public final Setting<Double> glassOpacity = sgAiCoder.add(new DoubleSetting.Builder()
        .name("glass-opacity")
        .description("How strongly the two colours fill the glass. 0 leaves only a faint tint.")
        .defaultValue(0.45)
        .range(0, 1)
        .sliderRange(0, 1)
        .visible(glassColor::get)
        .build()
    );

    public final Setting<Integer> blurStrength = sgAiCoder.add(new IntSetting.Builder()
        .name("blur-strength")
        .description("How strongly the glass blurs what is behind it.")
        .defaultValue(10)
        .range(1, 20)
        .sliderRange(1, 20)
        .build()
    );

    public final Setting<Boolean> aurora = sgAiCoder.add(new BoolSetting.Builder()
        .name("aurora")
        .description("Draws drifting aurora curtains in the two colours behind the modules window.")
        .defaultValue(true)
        .visible(glassColor::get)
        .build()
    );

    public AiCoderGuiTheme() {
        super();

        // Show the AiCoder settings first on the GUI tab instead of below all of Meteor's colours
        settings.groups.remove(sgAiCoder);
        settings.groups.add(0, sgAiCoder);

        applyPalette();
    }

    /** Design pixels to screen pixels. */
    public double u(double designPixels) {
        return scale(designPixels * UNIT);
    }

    public static AiCoderGuiTheme of(GuiTheme theme) {
        return (AiCoderGuiTheme) theme;
    }

    /** Runs before every GUI frame: hands the current colour and blur settings to the renderer. */
    @Override
    public void beforeRender() {
        super.beforeRender();

        int first = color1.get().getPacked(), second = color2.get().getPacked();

        Palette.setAccents(first, second);
        Sdf.setFlowColors(first, second);
        Sdf.setBlurStrength(blurStrength.get());
    }

    // Palette

    public void applyPalette() {
        accentColor.set(setting(Palette.ACCENT_MID));
        checkboxColor.set(setting(Palette.ACCENT_1));
        plusColor.set(setting(Palette.ACCENT_1));
        minusColor.set(setting(Palette.ACCENT_2));
        favoriteColor.set(setting(Palette.ACCENT_MID));

        textColor.set(setting(Palette.TEXT));
        textSecondaryColor.set(setting(Palette.TEXT_SECONDARY));
        textHighlightColor.set(setting(Palette.rgba(47, 140, 255, 0.40)));
        titleTextColor.set(setting(Palette.WHITE));
        loggedInColor.set(setting(Palette.ACCENT_1));
        placeholderColor.set(setting(Palette.rgba(199, 214, 245, 0.25)));

        color("background-color", Palette.rgba(16, 24, 44, 0.50));
        color("hovered-background-color", Palette.rgba(24, 40, 70, 0.58));
        color("pressed-background-color", Palette.rgba(30, 60, 96, 0.66));
        moduleBackground.set(setting(Palette.rgba(47, 140, 255, 0.16)));

        color("outline-color", Palette.rgba(130, 160, 255, 0.22));
        color("hovered-outline-color", Palette.rgba(34, 211, 238, 0.45));
        color("pressed-outline-color", Palette.rgba(47, 224, 143, 0.60));

        separatorText.set(setting(Palette.TEXT_HEADING));
        separatorCenter.set(setting(Palette.rgba(120, 180, 255, 0.45)));
        separatorEdges.set(setting(Palette.rgba(120, 180, 255, 0.05)));

        color("Scrollbar-color", Palette.rgba(120, 180, 255, 0.22));
        color("hovered-Scrollbar-color", Palette.rgba(34, 211, 238, 0.45));
        color("pressed-Scrollbar-color", Palette.rgba(47, 224, 143, 0.60));

        color("slider-handle-color", Palette.WHITE);
        color("hovered-slider-handle-color", Palette.WHITE);
        color("pressed-slider-handle-color", 0xFFEAFFF6);
        sliderLeft.set(setting(Palette.ACCENT_1));
        sliderRight.set(setting(Palette.SLIDER_TRACK));
    }

    private static SettingColor setting(int argb) {
        return new SettingColor((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, (argb >>> 24) & 0xFF);
    }

    /** The three state colours of the Meteor theme keep their settings private, so those are looked up by name. */
    @SuppressWarnings("unchecked")
    private void color(String name, int argb) {
        Setting<?> setting = settings.get(name);
        if (setting != null && setting.get() instanceof SettingColor) ((Setting<SettingColor>) setting).set(setting(argb));
    }

    // Widgets

    @Override
    public WWindow window(WWidget icon, String title) {
        return w(new WAiCoderWindow(icon, title));
    }

    @Override
    public WCheckbox checkbox(boolean checked) {
        return w(new WAiCoderCheckbox(checked));
    }

    @Override
    public WSlider slider(double value, double min, double max) {
        return w(new WAiCoderSlider(value, min, max));
    }

    @Override
    public WWidget module(Module module, String title) {
        return w(new WAiCoderModule(module, title, null));
    }

    // Screens

    @Override
    public TabScreen modulesScreen() {
        return new ColumnsScreen(this);
    }

    @Override
    public boolean isModulesScreen(Screen screen) {
        return screen instanceof ColumnsScreen || super.isModulesScreen(screen);
    }

    // Saving / Loading

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = super.toTag();
        tag.putBoolean(MARKER, true);
        return tag;
    }

    @Override
    public GuiTheme fromTag(CompoundTag tag) {
        super.fromTag(tag);

        // A theme file saved by the stock Meteor theme holds its purple palette, replace that once
        if (!tag.getBooleanOr(MARKER, false)) applyPalette();

        return this;
    }
}
