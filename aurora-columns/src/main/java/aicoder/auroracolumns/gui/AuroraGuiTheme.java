package aicoder.auroracolumns.gui;

import aicoder.auroracolumns.gui.screens.AuroraColumnsScreen;
import aicoder.auroracolumns.gui.widgets.WAuroraButton;
import aicoder.auroracolumns.gui.widgets.WAuroraCheckbox;
import aicoder.auroracolumns.gui.widgets.WAuroraDropdown;
import aicoder.auroracolumns.gui.widgets.WAuroraModule;
import aicoder.auroracolumns.gui.widgets.WAuroraSection;
import aicoder.auroracolumns.gui.widgets.WAuroraSlider;
import aicoder.auroracolumns.gui.widgets.WAuroraTextBox;
import aicoder.auroracolumns.gui.widgets.WAuroraTooltip;
import aicoder.auroracolumns.gui.widgets.WAuroraTopBar;
import aicoder.auroracolumns.gui.widgets.WAuroraTriangle;
import aicoder.auroracolumns.gui.widgets.WAuroraWindow;
import aicoder.auroracolumns.mixin.GuiThemeAccessor;
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
import meteordevelopment.meteorclient.gui.widgets.pressable.WTriangle;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Blur;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.gui.screens.Screen;

import static meteordevelopment.meteorclient.utils.Utils.getWindowHeight;
import static meteordevelopment.meteorclient.utils.Utils.getWindowWidth;

/**
 * Aurora glass theme in a column layout: one pane of liquid glass per module category, over a night sky
 * with slow aurora ribbons.
 * Registered under its own name, so Meteor's stock theme stays available in the GUI tab.
 */
public class AuroraGuiTheme extends MeteorGuiTheme {
    public static final String NAME = "AiCoder Aurora Columns";

    public final Style style = new Style();

    private final SettingGroup sgAurora = settings.createGroup("Aurora Glass");

    public final Setting<Boolean> animations = sgAurora.add(new BoolSetting.Builder()
        .name("animations")
        .description("Drifting aurora, light travelling along the glass rims, breathing highlights and fades. Off shows everything at rest.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Boolean> blur = sgAurora.add(new BoolSetting.Builder()
        .name("blur")
        .description("Blurs the game behind the GUI with Meteor's Blur module. Turn off on slower machines; the glass then becomes more solid so it stays readable.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Double> backdrop = sgAurora.add(new DoubleSetting.Builder()
        .name("night-sky")
        .description("How strongly the night sky covers the game behind the GUI.")
        .defaultValue(0.88)
        .range(0, 1)
        .sliderRange(0, 1)
        .build()
    );

    public final Setting<Boolean> toggleSound = sgAurora.add(new BoolSetting.Builder()
        .name("toggle-sound")
        .description("Plays a water-drop sound when a module is switched on or off from the GUI.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Boolean> hoverSound = sgAurora.add(new BoolSetting.Builder()
        .name("hover-sound")
        .description("Plays a light tap on glass when the pointer moves onto a module.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Double> soundVolume = sgAurora.add(new DoubleSetting.Builder()
        .name("sound-volume")
        .description("Volume of the toggle and hover sounds.")
        .defaultValue(0.6)
        .range(0, 1)
        .sliderRange(0, 1)
        .visible(() -> toggleSound.get() || hoverSound.get())
        .build()
    );

    private boolean backdropPending;
    private double fit = 1;

    public AuroraGuiTheme() {
        super();

        // MeteorGuiTheme always calls itself "Meteor"
        ((GuiThemeAccessor) this).auroracol$setName(NAME);

        // Colors used by the Meteor widgets this theme does not replace
        color("accent-color", 77, 255, 184, 255);
        color("checkbox-color", 77, 255, 184, 255);
        color("plus-color", 77, 255, 184, 255);
        color("minus-color", 255, 111, 216, 255);
        color("favorite-color", 77, 255, 184, 255);

        color("text-color", 238, 250, 255, 255);
        color("text-secondary-text-color", 226, 244, 255, 158);
        color("text-highlight-color", 47, 230, 224, 90);
        color("title-text-color", 238, 250, 255, 255);
        color("logged-in-text-color", 77, 255, 184, 255);
        color("placeholder-color", 226, 244, 255, 92);

        color("background-color", 3, 10, 26, 107);
        color("hovered-background-color", 20, 50, 70, 140);
        color("pressed-background-color", 30, 80, 90, 170);
        color("module-background-color", 77, 255, 184, 50);

        color("outline-color", 255, 255, 255, 46);
        color("hovered-outline-color", 255, 255, 255, 110);
        color("pressed-outline-color", 77, 255, 184, 200);

        color("separator-text-color", 77, 255, 184, 255);
        color("separator-center-color", 77, 255, 184, 140);
        color("separator-edges-color", 143, 123, 255, 20);

        color("Scrollbar-color", 255, 255, 255, 50);
        color("hovered-Scrollbar-color", 255, 255, 255, 100);
        color("pressed-Scrollbar-color", 77, 255, 184, 170);

        refresh();
    }

    /** Makes the given color both the current value and the default, so "Reset Colors" returns to this palette. */
    @SuppressWarnings("unchecked")
    private void color(String name, int r, int g, int b, int a) {
        Setting<SettingColor> setting = (Setting<SettingColor>) settings.get(name);
        if (setting == null) return;

        setting.getDefaultValue().set(r, g, b, a);
        setting.reset();
    }

    /** Picks up the settings and the size of the game window for this frame. */
    private void refresh() {
        Fx.animate = animations.get();

        // Design pixels are real pixels at Meteor GUI scale 1 on a 1920x1080 screen; smaller windows scale everything down
        double s = super.scale(1);
        double w = getWindowWidth(), h = getWindowHeight();
        fit = w > 0 && h > 0 ? Math.max(0.6, Math.min(1, Math.min(w / (1920 * s), h / (1080 * s)))) : 1;

        Aurora.frame(w, h, backdrop.get(), blur.get());
    }

    @Override
    public void beforeRender() {
        super.beforeRender();
        refresh();
        backdropPending = true;
    }

    /**
     * Runs once per frame before the first pane is drawn (the tab strip, windows and the modules frame all
     * call it first thing): resets the occluder list and paints the night sky over the game.
     */
    public void backdrop(GuiRenderer renderer) {
        if (!backdropPending) return;
        backdropPending = false;

        Fx.nextFrame();

        // Own scissor, so the sky reaches the screen before any glass is drawn over it
        renderer.scissorStart(0, 0, getWindowWidth(), getWindowHeight());
        Aurora.render(renderer);
        renderer.scissorEnd();
    }

    /** Switches Meteor's Blur module on if the player wants the game blurred behind the glass. */
    public void ensureBlur() {
        if (!blur.get()) return;

        Blur module = Modules.get().get(Blur.class);
        if (module != null && !module.isActive()) module.toggle();
    }

    /** Meteor's own widgets and text shrink together with the theme's on small game windows. */
    @Override
    public double scale(double value) {
        return super.scale(value) * fit;
    }

    /** Framebuffer pixels per design pixel. */
    public double unit() {
        return scale(1);
    }

    /** Design pixels as Meteor layout units (paddings, spacings, minimum widths). */
    public double mu(double designPx) {
        return designPx;
    }

    /** Whether the tab strip sits in the top-right corner instead of the top centre. */
    public boolean topBarRight() {
        return true;
    }

    // Widgets

    @Override
    public WWindow window(WWidget icon, String title) {
        return w(new WAuroraWindow(icon, title));
    }

    @Override
    protected WButton button(String text, GuiTexture texture) {
        return w(new WAuroraButton(text, texture));
    }

    @Override
    public WCheckbox checkbox(boolean checked) {
        return w(new WAuroraCheckbox(checked));
    }

    @Override
    public WSlider slider(double value, double min, double max) {
        return w(new WAuroraSlider(value, min, max));
    }

    @Override
    public WTextBox textBox(String text, String placeholder, CharFilter filter, Class<? extends WTextBox.Renderer> renderer) {
        return w(new WAuroraTextBox(text, placeholder, filter, renderer));
    }

    @Override
    public <T> WDropdown<T> dropdown(T[] values, T value) {
        return w(new WAuroraDropdown<>(values, value));
    }

    @Override
    public WTooltip tooltip(String text) {
        return w(new WAuroraTooltip(text));
    }

    @Override
    public WSection section(String title, boolean expanded, WWidget headerWidget) {
        return w(new WAuroraSection(title, expanded, headerWidget));
    }

    @Override
    public WWidget module(Module module, String title) {
        return w(new WAuroraModule(module, title));
    }

    @Override
    public WTriangle triangle() {
        return w(new WAuroraTriangle());
    }

    @Override
    public WTopBar topBar() {
        return w(new WAuroraTopBar());
    }

    // Screens

    @Override
    public TabScreen modulesScreen() {
        return new AuroraColumnsScreen(this);
    }

    @Override
    public boolean isModulesScreen(Screen screen) {
        return screen instanceof AuroraColumnsScreen;
    }
}
