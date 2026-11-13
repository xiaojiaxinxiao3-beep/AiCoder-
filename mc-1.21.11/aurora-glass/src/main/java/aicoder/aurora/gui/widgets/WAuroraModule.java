package aicoder.aurora.gui.widgets;

import aicoder.aurora.gui.AuroraGuiTheme;
import aicoder.aurora.gui.Chrome;
import aicoder.aurora.gui.Fx;
import aicoder.aurora.gui.Style;
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
 * Module row: name over a small id, and a capsule switch at the right.
 *
 * <p>On its own it behaves like Meteor's row: left click toggles, right click opens the module screen.
 * When {@link #onSelect} is set (the modules screen does this) the row becomes selectable instead:
 * clicking the row selects it, clicking the switch or right clicking toggles the module.
 */
public class WAuroraModule extends WPressable implements MeteorWidget {
    private static final double HEIGHT = 66;
    private static final double PAD = 20;
    private static final double NAME_FONT = 16.5;
    private static final double ID_FONT = 10.5;
    private static final double SWITCH_W = 44;
    private static final double SWITCH_H = 24;

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

    public WAuroraModule(Module module, String title) {
        this.module = module;
        this.title = title;
        this.tooltip = module.description;

        int hash = module.name.hashCode();
        this.id = String.format("MOD-0x%02X", hash & 0xFF);
        this.seed = ((hash >>> 8) & 0xFF) / 255.0;
        this.active = module.isActive() ? 1 : 0;
    }

    private double unit() {
        return unit > 0 ? unit : ((AuroraGuiTheme) theme).unit();
    }

    @Override
    protected void onCalculateSize() {
        double u = unit();
        width = Fx.textWidth(theme, title, NAME_FONT, 0.4, u) + (PAD * 2 + 14 + SWITCH_W) * u;
        height = Math.round(HEIGHT * u);
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

        boolean onSwitch = clickX >= x + width - (PAD + SWITCH_W + 8) * unit();
        if (button == GLFW_MOUSE_BUTTON_RIGHT || onSwitch) module.toggle();
        else onSelect.accept(module);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        Style s = ((AuroraGuiTheme) theme).style;
        double u = unit();
        boolean isSelected = selected != null && selected.get() == module;

        // 160ms for the switch, 120ms for the hover
        active = Fx.step(active, module.isActive(), delta, 6.25);
        hover = Fx.step(hover, mouseOver, delta, 8.3);

        // Switched-on rows breathe between 0.7 and 1 every 2.4 seconds, each on its own beat
        double pulse = 0.85 + 0.15 * Math.sin(Math.PI * 2 * (Fx.phase(2.4) + seed));
        double corner = s.plateCorner * u;

        if (isSelected) {
            Fx.boxOutline(renderer, x - 5 * u, y - 5 * u, width + 10 * u, height + 10 * u, corner + 5 * u, 5 * u, Fx.a(s.green, 0.14));
        }

        Chrome.plate(renderer, x, y, width, height, u, corner, isSelected ? 0 : hover);
        Chrome.lit(renderer, x, y, width, height, u, corner, 0.22, 0.10, 0.14, Fx.a(s.mint, 0.42), 0.16, active * pulse);

        if (isSelected) {
            Fx.boxOutline(renderer, x, y, width, height, corner, Math.max(1, Math.round(1.6 * u)), Fx.rgba(255, 255, 255, 0.75));
        }

        // Name and id
        Fx.TextBatch text = Fx.text(renderer, u);
        double nameH = Fx.lineHeight(theme, NAME_FONT, u);
        double idH = Fx.lineHeight(theme, ID_FONT, u);
        double gap = 3 * u;
        double top = y + (height - nameH - gap - idH) / 2;
        double tx = x + PAD * u;

        text.put(title, tx, top, NAME_FONT, s.title, 0.4, 0);
        text.put(id, tx, top + nameH + gap, ID_FONT, s.faint, 1.8, 0);

        // Switch
        double sw = SWITCH_W * u, sh = Math.round(SWITCH_H * u);
        Chrome.toggle(renderer, Math.round(x + width - PAD * u - sw), Math.round(y + (height - sh) / 2), sw, sh, u, active, hover, pulse);
    }
}
