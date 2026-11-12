package aicoder.glass.gui;

import aicoder.glass.gui.screens.GlassModulesScreen;
import aicoder.glass.gui.widgets.WGlassButton;
import aicoder.glass.gui.widgets.WGlassCheckbox;
import aicoder.glass.gui.widgets.WGlassDropdown;
import aicoder.glass.gui.widgets.WGlassModule;
import aicoder.glass.gui.widgets.WGlassSection;
import aicoder.glass.gui.widgets.WGlassSlider;
import aicoder.glass.gui.widgets.WGlassTextBox;
import aicoder.glass.gui.widgets.WGlassTooltip;
import aicoder.glass.gui.widgets.WGlassTopBar;
import aicoder.glass.gui.widgets.WGlassWindow;
import aicoder.glass.mixin.GuiThemeAccessor;
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
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.ProvidedStringSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Blur;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.gui.screen.Screen;

import static meteordevelopment.meteorclient.utils.Utils.getWindowHeight;
import static meteordevelopment.meteorclient.utils.Utils.getWindowWidth;

/**
 * Liquid glass theme: rounded panes tinted with two colors of the player's choosing, or left clear.
 * Registered under its own name, so Meteor's stock theme stays available in the GUI tab.
 */
public class GlassGuiTheme extends MeteorGuiTheme {
    public static final String NAME = "AiCoder Glass";

    /** Palette derived from the settings below, refreshed every frame. */
    public final Style style = new Style();

    private final SettingGroup sgGlass = settings.createGroup("Liquid Glass");

    private final SettingGroup sgWallpaper = settings.createGroup("Wallpaper");

    public final Setting<String> wallpaper = sgWallpaper.add(new ProvidedStringSetting.Builder()
        .name("wallpaper")
        .description("Picture shown inside the glass panes. Add your own PNG or JPG files to meteor-client/aicoder-wallpapers in the game folder, then reopen this tab.")
        .supplier(Wallpaper::names)
        .defaultValue(Wallpaper.DEFAULT)
        .build()
    );

    public final Setting<Double> wallpaperOpacity = sgWallpaper.add(new DoubleSetting.Builder()
        .name("wallpaper-opacity")
        .description("How strongly the wallpaper shows inside the glass.")
        .defaultValue(1)
        .range(0, 1)
        .sliderRange(0, 1)
        .build()
    );

    public final Setting<Boolean> tinted = sgGlass.add(new BoolSetting.Builder()
        .name("color")
        .description("Tint the glass with the two colors below. Off gives clear glass.")
        .defaultValue(true)
        .build()
    );

    public final Setting<SettingColor> colorA = sgGlass.add(new ColorSetting.Builder()
        .name("color-1")
        .description("Glass color at the top-left.")
        .defaultValue(new SettingColor(52, 132, 255))
        .visible(tinted::get)
        .build()
    );

    public final Setting<SettingColor> colorB = sgGlass.add(new ColorSetting.Builder()
        .name("color-2")
        .description("Glass color at the bottom-right.")
        .defaultValue(new SettingColor(142, 58, 232))
        .visible(tinted::get)
        .build()
    );

    public final Setting<Double> glassOpacity = sgGlass.add(new DoubleSetting.Builder()
        .name("glass-opacity")
        .description("How solid the glass is. Lower shows more of the game through it.")
        .defaultValue(0.5)
        .range(0.1, 1)
        .sliderRange(0.1, 1)
        .build()
    );

    public final Setting<Boolean> liquid = sgGlass.add(new BoolSetting.Builder()
        .name("liquid-motion")
        .description("Soft highlights drifting slowly inside the glass.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Boolean> autoBlur = sgGlass.add(new BoolSetting.Builder()
        .name("auto-blur")
        .description("Turns on Meteor's Blur module when the GUI opens, so the game behind the glass is blurred.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Double> backdropDim = sgGlass.add(new DoubleSetting.Builder()
        .name("backdrop-dim")
        .description("How much the game is darkened behind the GUI.")
        .defaultValue(0.12)
        .range(0, 1)
        .sliderRange(0, 1)
        .build()
    );

    private boolean backdropPending;

    public GlassGuiTheme() {
        super();

        // MeteorGuiTheme always calls itself "Meteor"
        ((GuiThemeAccessor) this).glass$setName(NAME);

        // Colors used by the Meteor widgets this theme does not replace
        color("accent-color", 150, 190, 255, 255);
        color("checkbox-color", 150, 190, 255, 255);
        color("plus-color", 150, 190, 255, 255);
        color("minus-color", 255, 130, 170, 255);
        color("favorite-color", 255, 255, 255, 255);

        color("text-color", 228, 235, 255, 255);
        color("text-secondary-text-color", 196, 208, 242, 255);
        color("text-highlight-color", 150, 190, 255, 90);
        color("title-text-color", 246, 249, 255, 255);
        color("logged-in-text-color", 150, 220, 255, 255);
        color("placeholder-color", 196, 208, 242, 130);

        color("background-color", 20, 24, 56, 150);
        color("hovered-background-color", 40, 46, 90, 170);
        color("pressed-background-color", 60, 66, 120, 190);
        color("module-background-color", 255, 255, 255, 40);

        color("outline-color", 255, 255, 255, 46);
        color("hovered-outline-color", 255, 255, 255, 120);
        color("pressed-outline-color", 255, 255, 255, 200);

        color("separator-text-color", 228, 235, 255, 255);
        color("separator-center-color", 255, 255, 255, 110);
        color("separator-edges-color", 255, 255, 255, 20);

        color("Scrollbar-color", 255, 255, 255, 60);
        color("hovered-Scrollbar-color", 255, 255, 255, 110);
        color("pressed-Scrollbar-color", 255, 255, 255, 170);

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

    private void refresh() {
        style.update(colorA.get(), colorB.get(), tinted.get(), glassOpacity.get());
    }

    @Override
    public void beforeRender() {
        super.beforeRender();
        refresh();
        backdropPending = true;
    }

    /**
     * Runs once per frame before the first window is drawn (the tab strip, windows and the modules frame
     * all call it first thing): resets the occluder list, picks this frame's wallpaper and lays the optional dim over the game.
     */
    public void backdrop(GuiRenderer renderer) {
        if (!backdropPending) return;
        backdropPending = false;

        Fx.nextFrame();

        double w = getWindowWidth(), h = getWindowHeight();
        Wallpaper.frame(wallpaper.get(), wallpaperOpacity.get(), w, h);

        double dim = backdropDim.get();
        if (dim > 0) {
            // Own scissor, so the dim reaches the screen before any glass is drawn over it
            renderer.scissorStart(0, 0, w, h);
            renderer.quad(0, 0, w, h, Fx.rgba(4, 6, 18, dim));
            renderer.scissorEnd();
        }
    }

    /** Switches Meteor's Blur module on if the player wants the game blurred behind the glass. */
    public void ensureBlur() {
        Wallpaper.ensureFolder();

        if (!autoBlur.get()) return;

        Blur blur = Modules.get().get(Blur.class);
        if (blur != null && !blur.isActive()) blur.toggle();
    }

    /** Framebuffer pixels per design pixel. */
    public double unit() {
        return scale(1.5);
    }

    // Widgets

    @Override
    public WWindow window(WWidget icon, String title) {
        return w(new WGlassWindow(icon, title));
    }

    @Override
    protected WButton button(String text, GuiTexture texture) {
        return w(new WGlassButton(text, texture));
    }

    @Override
    public WCheckbox checkbox(boolean checked) {
        return w(new WGlassCheckbox(checked));
    }

    @Override
    public WSlider slider(double value, double min, double max) {
        return w(new WGlassSlider(value, min, max));
    }

    @Override
    public WTextBox textBox(String text, String placeholder, CharFilter filter, Class<? extends WTextBox.Renderer> renderer) {
        return w(new WGlassTextBox(text, placeholder, filter, renderer));
    }

    @Override
    public <T> WDropdown<T> dropdown(T[] values, T value) {
        return w(new WGlassDropdown<>(values, value));
    }

    @Override
    public WTooltip tooltip(String text) {
        return w(new WGlassTooltip(text));
    }

    @Override
    public WSection section(String title, boolean expanded, WWidget headerWidget) {
        return w(new WGlassSection(title, expanded, headerWidget));
    }

    @Override
    public WWidget module(Module module, String title) {
        return w(new WGlassModule(module, title));
    }

    @Override
    public WTopBar topBar() {
        return w(new WGlassTopBar());
    }

    // Screens

    @Override
    public TabScreen modulesScreen() {
        return new GlassModulesScreen(this);
    }

    @Override
    public boolean isModulesScreen(Screen screen) {
        return screen instanceof GlassModulesScreen;
    }
}
