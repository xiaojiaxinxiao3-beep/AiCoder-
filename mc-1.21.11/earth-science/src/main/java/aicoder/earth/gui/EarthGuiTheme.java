package aicoder.earth.gui;

import aicoder.earth.gui.screens.EarthModulesScreen;
import aicoder.earth.gui.widgets.WEarthButton;
import aicoder.earth.gui.widgets.WEarthCheckbox;
import aicoder.earth.gui.widgets.WEarthDropdown;
import aicoder.earth.gui.widgets.WEarthModule;
import aicoder.earth.gui.widgets.WEarthSection;
import aicoder.earth.gui.widgets.WEarthSlider;
import aicoder.earth.gui.widgets.WEarthTextBox;
import aicoder.earth.gui.widgets.WEarthTooltip;
import aicoder.earth.gui.widgets.WEarthTopBar;
import aicoder.earth.gui.widgets.WEarthWindow;
import aicoder.earth.mixin.GuiThemeAccessor;
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
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.gui.screen.Screen;

/**
 * One of the ten earth-science themes. All ten are instances of this class with a different {@link Style};
 * each is registered under its own name, so Meteor lists them in the GUI tab and saves them separately.
 */
public class EarthGuiTheme extends MeteorGuiTheme {
    public final Style style;

    private final SettingGroup sgEarth = settings.createGroup("Earth Science");

    public final Setting<Double> backdropOpacity = sgEarth.add(new DoubleSetting.Builder()
        .name("backdrop-opacity")
        .description("How much of the game the scene behind the windows hides. 0 turns the scene off.")
        .defaultValue(0.92)
        .range(0, 1)
        .sliderRange(0, 1)
        .build()
    );

    private boolean backdropPending;

    public EarthGuiTheme(Style style) {
        super();
        this.style = style;

        // MeteorGuiTheme always calls itself "Meteor"
        ((GuiThemeAccessor) this).earth$setName(style.name);

        Style s = style;
        color("accent-color", s.accent);
        color("checkbox-color", s.accent);
        color("plus-color", s.accent);
        color("minus-color", s.accent2);
        color("favorite-color", s.accent);

        color("text-color", s.text);
        color("text-secondary-text-color", s.dim);
        color("text-highlight-color", alpha(s.accent, 90));
        color("title-text-color", s.title);
        color("logged-in-text-color", s.accent);
        color("placeholder-color", alpha(s.dim, 140));

        color("background-color", s.windowTop);
        color("hovered-background-color", Fx.mix(s.windowTop, s.accent, 0.12));
        color("pressed-background-color", Fx.mix(s.windowTop, s.accent, 0.25));
        color("module-background-color", s.plateSelected);

        color("outline-color", s.fieldLine);
        color("hovered-outline-color", alpha(s.accent, 200));
        color("pressed-outline-color", s.accent);

        color("separator-text-color", s.accent);
        color("separator-center-color", alpha(s.accent, 140));
        color("separator-edges-color", alpha(s.accent, 30));

        color("Scrollbar-color", alpha(s.accent, 80));
        color("hovered-Scrollbar-color", alpha(s.accent, 140));
        color("pressed-Scrollbar-color", s.accent);

        color("slider-handle-color", s.knob);
        color("hovered-slider-handle-color", s.knob);
        color("pressed-slider-handle-color", s.knob);
        color("slider-left-color", s.slideTo);
        color("slider-right-color", alpha(s.slideTo, 44));
    }

    private static Color alpha(Color c, int a) {
        return new Color(c.r, c.g, c.b, a);
    }

    /** Makes the given color both the current value and the default, so "Reset Colors" returns to this palette. */
    @SuppressWarnings("unchecked")
    private void color(String name, Color c) {
        Setting<SettingColor> setting = (Setting<SettingColor>) settings.get(name);
        if (setting == null) return;

        setting.getDefaultValue().set(c.r, c.g, c.b, c.a);
        setting.reset();
    }

    @Override
    public void beforeRender() {
        super.beforeRender();
        backdropPending = true;
    }

    /**
     * Paints the full-screen scene once per frame. The tab strip, windows and the modules frame all call
     * this first thing, so whichever of them a screen draws first puts the scene behind everything else.
     */
    public void backdrop(GuiRenderer renderer) {
        if (!backdropPending) return;

        backdropPending = false;
        Backdrops.render(renderer, style, unit(), backdropOpacity.get());
    }

    /** Framebuffer pixels per design pixel. The mockups are drawn at two thirds of Meteor's native widget size. */
    public double unit() {
        return scale(1.5);
    }

    // Widgets

    @Override
    public WWindow window(WWidget icon, String title) {
        return w(new WEarthWindow(icon, title));
    }

    @Override
    protected WButton button(String text, GuiTexture texture) {
        return w(new WEarthButton(text, texture));
    }

    @Override
    public WCheckbox checkbox(boolean checked) {
        return w(new WEarthCheckbox(checked));
    }

    @Override
    public WSlider slider(double value, double min, double max) {
        return w(new WEarthSlider(value, min, max));
    }

    @Override
    public WTextBox textBox(String text, String placeholder, CharFilter filter, Class<? extends WTextBox.Renderer> renderer) {
        return w(new WEarthTextBox(text, placeholder, filter, renderer));
    }

    @Override
    public <T> WDropdown<T> dropdown(T[] values, T value) {
        return w(new WEarthDropdown<>(values, value));
    }

    @Override
    public WTooltip tooltip(String text) {
        return w(new WEarthTooltip(text));
    }

    @Override
    public WSection section(String title, boolean expanded, WWidget headerWidget) {
        return w(new WEarthSection(title, expanded, headerWidget));
    }

    @Override
    public WWidget module(Module module, String title) {
        return w(new WEarthModule(module, title));
    }

    @Override
    public WTopBar topBar() {
        return w(new WEarthTopBar());
    }

    // Screens

    @Override
    public TabScreen modulesScreen() {
        return new EarthModulesScreen(this);
    }

    @Override
    public boolean isModulesScreen(Screen screen) {
        return screen instanceof EarthModulesScreen;
    }
}
