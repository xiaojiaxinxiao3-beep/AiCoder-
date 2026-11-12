package aicoder.client.gui;

import aicoder.client.gui.screens.CyberModulesScreen;
import aicoder.client.gui.widgets.WCyberButton;
import aicoder.client.gui.widgets.WCyberCheckbox;
import aicoder.client.gui.widgets.WCyberDropdown;
import aicoder.client.gui.widgets.WCyberModule;
import aicoder.client.gui.widgets.WCyberSection;
import aicoder.client.gui.widgets.WCyberSlider;
import aicoder.client.gui.widgets.WCyberTextBox;
import aicoder.client.gui.widgets.WCyberTooltip;
import aicoder.client.gui.widgets.WCyberTopBar;
import aicoder.client.gui.widgets.WCyberWindow;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.renderer.packer.GuiTexture;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.gui.utils.CharFilter;
import meteordevelopment.meteorclient.gui.widgets.WTooltip;
import meteordevelopment.meteorclient.gui.widgets.WTopBar;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WSection;
import meteordevelopment.meteorclient.gui.widgets.containers.WWindow;
import meteordevelopment.meteorclient.gui.widgets.input.WDropdown;
import meteordevelopment.meteorclient.gui.widgets.input.WSlider;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;

/**
 * Cyberpunk theme. It keeps Meteor's theme name, so registering it replaces the stock theme;
 * it extends {@link MeteorGuiTheme} because Meteor's own widgets cast their theme to that class.
 */
public class AiCoderGuiTheme extends MeteorGuiTheme {
    /** Marks theme files written by this theme, so colors saved by the stock theme are not loaded over the palette. */
    private static final String SAVE_MARKER = "aicoder";

    private final SettingGroup sgCyber = settings.createGroup("Cyber");

    public final Setting<Double> backdropDim = sgCyber.add(new DoubleSetting.Builder()
        .name("backdrop-dim")
        .description("How much the game is darkened behind the GUI.")
        .defaultValue(0.55)
        .range(0, 1)
        .sliderRange(0, 1)
        .build()
    );

    public final Setting<Boolean> dataRain = sgCyber.add(new BoolSetting.Builder()
        .name("data-rain")
        .description("Columns of falling characters behind the windows.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Boolean> lights = sgCyber.add(new BoolSetting.Builder()
        .name("lights")
        .description("Drifting cyan, magenta and purple lights behind the windows.")
        .defaultValue(true)
        .build()
    );

    public AiCoderGuiTheme() {
        super();

        // Accents: magenta for selection and emphasis, cyan for switches, headings and values
        color("accent-color", 255, 42, 109, 255);
        color("checkbox-color", 255, 42, 109, 255);
        color("plus-color", 0, 240, 255, 255);
        color("minus-color", 255, 42, 109, 255);
        color("favorite-color", 255, 42, 109, 255);

        color("text-color", 183, 201, 220, 255);
        color("text-secondary-text-color", 111, 138, 166, 255);
        color("text-highlight-color", 0, 240, 255, 90);
        color("title-text-color", 234, 246, 255, 255);
        color("logged-in-text-color", 0, 240, 255, 255);
        color("placeholder-color", 111, 138, 166, 130);

        // Glass. The normal background is also the top-left color of the window gradient.
        color("background-color", 28, 6, 40, 128);
        color("hovered-background-color", 44, 12, 62, 150);
        color("pressed-background-color", 70, 14, 60, 170);
        color("module-background-color", 255, 42, 109, 51);

        color("outline-color", 0, 229, 255, 82);
        color("hovered-outline-color", 0, 229, 255, 150);
        color("pressed-outline-color", 255, 42, 109, 200);

        color("separator-text-color", 0, 240, 255, 255);
        color("separator-center-color", 0, 240, 255, 128);
        color("separator-edges-color", 255, 42, 109, 31);

        color("Scrollbar-color", 0, 229, 255, 70);
        color("hovered-Scrollbar-color", 0, 229, 255, 130);
        color("pressed-Scrollbar-color", 255, 42, 109, 200);

        color("slider-handle-color", 255, 255, 255, 255);
        color("hovered-slider-handle-color", 255, 255, 255, 255);
        color("pressed-slider-handle-color", 255, 255, 255, 255);
        color("slider-left-color", 0, 240, 255, 255);
        color("slider-right-color", 70, 120, 170, 51);
    }

    /** Makes the given color both the current value and the default, so "Reset Colors" returns to this palette. */
    @SuppressWarnings("unchecked")
    private void color(String name, int r, int g, int b, int a) {
        Setting<SettingColor> setting = (Setting<SettingColor>) settings.get(name);
        if (setting == null) return;

        setting.getDefaultValue().set(r, g, b, a);
        setting.reset();
    }

    private boolean backdropPending;

    @Override
    public void beforeRender() {
        super.beforeRender();
        backdropPending = true;
    }

    /**
     * Paints the full-screen backdrop once per frame. The tab strip, windows and the modules frame all call
     * this first thing, so whichever of them a screen draws first puts the backdrop behind everything else.
     */
    public void backdrop(GuiRenderer renderer) {
        if (!backdropPending) return;

        backdropPending = false;
        Backdrop.render(renderer, this);
    }

    /** Framebuffer pixels per design pixel. The mockup is drawn at two thirds of Meteor's native widget size. */
    public double unit() {
        return scale(1.5);
    }

    // Widgets

    @Override
    public WWindow window(WWidget icon, String title) {
        return w(new WCyberWindow(icon, title));
    }

    @Override
    protected WButton button(String text, GuiTexture texture) {
        return w(new WCyberButton(text, texture));
    }

    @Override
    public WCheckbox checkbox(boolean checked) {
        return w(new WCyberCheckbox(checked));
    }

    @Override
    public WSlider slider(double value, double min, double max) {
        return w(new WCyberSlider(value, min, max));
    }

    @Override
    public WTextBox textBox(String text, String placeholder, CharFilter filter, Class<? extends WTextBox.Renderer> renderer) {
        return w(new WCyberTextBox(text, placeholder, filter, renderer));
    }

    @Override
    public <T> WDropdown<T> dropdown(T[] values, T value) {
        return w(new WCyberDropdown<>(values, value));
    }

    @Override
    public WTooltip tooltip(String text) {
        return w(new WCyberTooltip(text));
    }

    @Override
    public WSection section(String title, boolean expanded, WWidget headerWidget) {
        return w(new WCyberSection(title, expanded, headerWidget));
    }

    @Override
    public WWidget module(Module module, String title) {
        return w(new WCyberModule(module, title));
    }

    @Override
    public WTopBar topBar() {
        return w(new WCyberTopBar());
    }

    // Screens

    @Override
    public TabScreen modulesScreen() {
        return new CyberModulesScreen(this);
    }

    @Override
    public boolean isModulesScreen(Screen screen) {
        return screen instanceof CyberModulesScreen;
    }

    // Saving / Loading

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = super.toTag();
        tag.putBoolean(SAVE_MARKER, true);
        return tag;
    }

    @Override
    public GuiTheme fromTag(CompoundTag tag) {
        if (!tag.getBooleanOr(SAVE_MARKER, false)) return this;
        return super.fromTag(tag);
    }
}
