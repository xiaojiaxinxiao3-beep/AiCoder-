package aicoder.glass.gui.widgets;

import aicoder.glass.gui.GlassGuiTheme;
import aicoder.glass.gui.Chrome;
import aicoder.glass.gui.Fx;
import aicoder.glass.gui.Style;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPressable;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.Mth;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT;
import static com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT;
import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * Module row: a pane of glass with a round droplet switch.
 *
 * <p>On its own it behaves like Meteor's row: left click toggles, right click opens the module screen.
 * When {@link #onSelect} is set (the modules screen does this) the row becomes selectable instead:
 * clicking the row selects it, clicking the switch or right clicking toggles the module.
 */
public class WGlassModule extends WPressable implements MeteorWidget {
    private static final double HEIGHT = 44;
    private static final double NAME_FONT = 12.5;
    private static final double ID_FONT = 8.5;
    private static final double SWITCH_RADIUS = 10.5;
    private static final double SWITCH_INSET = 26;

    private final Module module;
    private final String title;
    private final String id;
    private final double seed;

    /** Design-pixel scale override; 0 means "use the theme's". */
    public double unit;
    public Supplier<Module> selected;
    public Consumer<Module> onSelect;

    private double active;
    private double hover;
    private double clickX;

    public WGlassModule(Module module, String title) {
        this.module = module;
        this.title = title;
        this.tooltip = module.description;

        int hash = module.name.hashCode();
        this.id = String.format("MOD-0x%02X", hash & 0xFF);
        this.seed = ((hash >>> 8) & 0xFF) / 255.0;
        this.active = module.isActive() ? 1 : 0;
    }

    private double unit() {
        return unit > 0 ? unit : ((GlassGuiTheme) theme).unit();
    }

    @Override
    protected void onCalculateSize() {
        double u = unit();
        width = Fx.textWidth(theme, title, NAME_FONT, 0.6, u) + (16 + 14 + SWITCH_INSET + SWITCH_RADIUS) * u;
        height = HEIGHT * u;
    }

    @Override
    public boolean onMouseClicked(MouseButtonEvent click, boolean doubled) {
        clickX = click.x();
        return super.onMouseClicked(click, doubled);
    }

    @Override
    protected void onPressed(int button) {
        if (onSelect == null) {
            if (button == MOUSE_BUTTON_LEFT) module.toggle();
            else if (button == MOUSE_BUTTON_RIGHT) mc.gui.setScreen(theme.moduleScreen(module));
            return;
        }

        boolean onSwitch = clickX >= x + width - (SWITCH_INSET + SWITCH_RADIUS + 6) * unit();
        if (button == MOUSE_BUTTON_RIGHT || onSwitch) module.toggle();
        else onSelect.accept(module);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        Style s = ((GlassGuiTheme) theme).style;
        double u = unit();
        boolean isSelected = selected != null && selected.get() == module;

        active = Mth.clamp(active + delta * 6 * (module.isActive() ? 1 : -1), 0, 1);
        hover = Mth.clamp(hover + delta * 8 * (mouseOver ? 1 : -1), 0, 1);

        Chrome.plate(renderer, s, x, y, width, height, u, isSelected ? 1 : 0, hover);

        // Name and id
        Fx.TextBatch text = Fx.text(renderer, u);
        double nameH = Fx.lineHeight(theme, NAME_FONT, u);
        double idH = Fx.lineHeight(theme, ID_FONT, u);
        double gap = 1 * u;
        double top = y + (height - nameH - gap - idH) / 2;
        double tx = x + 16 * u;

        Color name = Fx.mix(s.dim, s.title, Math.max(active, isSelected ? 1 : hover * 0.6));
        text.put(title, tx, top, NAME_FONT, name, 0.6, Fx.BOLD);
        text.put(id, tx, top + nameH + gap, ID_FONT, isSelected ? s.accent : s.faint, 1.2, 0);

        // Switch
        Chrome.droplet(renderer, s, Math.round(x + width - SWITCH_INSET * u), Math.round(y + height / 2), SWITCH_RADIUS * u, active, hover, seed);
    }
}
