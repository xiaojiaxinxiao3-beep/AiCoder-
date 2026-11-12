package aicoder.earth.gui.widgets;

import aicoder.earth.gui.EarthGuiTheme;
import aicoder.earth.gui.Fx;
import aicoder.earth.gui.Style;
import aicoder.earth.gui.Toggles;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPressable;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.gui.Click;
import net.minecraft.util.math.MathHelper;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;
import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * Module row with the theme's switch glyph.
 *
 * <p>On its own it behaves like Meteor's row: left click toggles, right click opens the module screen.
 * When {@link #onSelect} is set (the modules screen does this) the row becomes selectable instead:
 * clicking the row selects it, clicking the switch or right clicking toggles the module.
 */
public class WEarthModule extends WPressable implements MeteorWidget {
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

    public WEarthModule(Module module, String title) {
        this.module = module;
        this.title = title;
        this.tooltip = module.description;

        int hash = module.name.hashCode();
        this.id = String.format("MOD-0x%02X", hash & 0xFF);
        this.seed = ((hash >>> 8) & 0xFF) / 255.0;
        this.active = module.isActive() ? 1 : 0;
    }

    private double unit() {
        return unit > 0 ? unit : ((EarthGuiTheme) theme).unit();
    }

    @Override
    protected void onCalculateSize() {
        double u = unit();
        width = Fx.textWidth(theme, title, NAME_FONT, 0.6, u) + (16 + 14 + SWITCH_INSET + SWITCH_RADIUS) * u;
        height = HEIGHT * u;
    }

    @Override
    public boolean onMouseClicked(Click click, boolean doubled) {
        clickX = click.x();
        return super.onMouseClicked(click, doubled);
    }

    @Override
    protected void onPressed(int button) {
        if (onSelect == null) {
            if (button == GLFW_MOUSE_BUTTON_LEFT) module.toggle();
            else if (button == GLFW_MOUSE_BUTTON_RIGHT) mc.setScreen(theme.moduleScreen(module));
            return;
        }

        boolean onSwitch = clickX >= x + width - (SWITCH_INSET + SWITCH_RADIUS + 6) * unit();
        if (button == GLFW_MOUSE_BUTTON_RIGHT || onSwitch) module.toggle();
        else onSelect.accept(module);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        Style s = ((EarthGuiTheme) theme).style;
        double u = unit();
        boolean isSelected = selected != null && selected.get() == module;

        active = MathHelper.clamp(active + delta * 6 * (module.isActive() ? 1 : -1), 0, 1);
        hover = MathHelper.clamp(hover + delta * 8 * (mouseOver ? 1 : -1), 0, 1);

        // Plate
        double corner = s.plateCorner * u;
        if (isSelected) {
            Fx.box(renderer, x, y, width, height, corner, s.plateSelected);
            Fx.boxOutline(renderer, x, y, width, height, corner, Math.max(1, Math.round(1.3 * u)), s.accent);
        } else {
            Fx.box(renderer, x, y, width, height, corner, Fx.a(s.plate, 1 + 1.2 * hover));
        }

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
        Toggles.draw(renderer, s, Math.round(x + width - SWITCH_INSET * u), Math.round(y + height / 2), SWITCH_RADIUS * u, active, hover, seed);
    }
}
