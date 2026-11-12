package aicoder.client.gui.widgets;

import aicoder.client.gui.AiCoderGuiTheme;
import aicoder.client.gui.Cyber;
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
 * Module row with a hexagon switch.
 *
 * <p>On its own it behaves like Meteor's row: left click toggles, right click opens the module screen.
 * When {@link #onSelect} is set (the modules screen does this) the row becomes selectable instead:
 * clicking the row selects it, clicking the hexagon or right clicking toggles the module.
 */
public class WCyberModule extends WPressable implements MeteorWidget {
    private static final double HEIGHT = 44;
    private static final double NAME_FONT = 12.5;
    private static final double ID_FONT = 8.5;
    private static final double HEX_RADIUS = 10.5;
    private static final double HEX_INSET = 25;

    private static final Color NAME_IDLE = new Color(169, 191, 214);
    private static final Color ID_IDLE = new Color(69, 96, 124);
    private static final Color HEX_EDGE_OFF = new Color(120, 170, 210);
    private static final Color HEX_FILL_OFF = new Color(10, 22, 38);
    private static final Color HEX_OUTER = new Color(0, 169, 214);
    private static final Color HEX_MID = new Color(63, 208, 234);
    private static final Color HEX_INNER = new Color(127, 247, 255);
    private static final Color HEX_EDGE_ON = new Color(159, 246, 255);
    private static final Color HEX_CORE = new Color(242, 254, 255);

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

    public WCyberModule(Module module, String title) {
        this.module = module;
        this.title = title;
        this.tooltip = module.description;

        int hash = module.name.hashCode();
        this.id = String.format("MOD-0x%02X", hash & 0xFF);
        this.seed = ((hash >>> 8) & 0xFF) / 255.0;
        this.active = module.isActive() ? 1 : 0;
    }

    private double unit() {
        return unit > 0 ? unit : ((AiCoderGuiTheme) theme).unit();
    }

    @Override
    protected void onCalculateSize() {
        double u = unit();
        width = Cyber.textWidth(theme, title, NAME_FONT, 0.6, u) + (16 + 14 + HEX_INSET + HEX_RADIUS) * u;
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

        boolean onSwitch = clickX >= x + width - (HEX_INSET + HEX_RADIUS + 6) * unit();
        if (button == GLFW_MOUSE_BUTTON_RIGHT || onSwitch) module.toggle();
        else onSelect.accept(module);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        double u = unit();
        double p = Cyber.phase();
        boolean on = module.isActive();
        boolean isSelected = selected != null && selected.get() == module;
        Color accent = theme().accentColor.get();

        active = MathHelper.clamp(active + delta * 6 * (on ? 1 : -1), 0, 1);
        hover = MathHelper.clamp(hover + delta * 8 * (mouseOver ? 1 : -1), 0, 1);

        // Plate
        double cut = 8 * u;
        if (isSelected) {
            Cyber.cutRect(renderer, x, y, width, height, 0, 0, cut, 0, Cyber.a(accent, 0.20), Cyber.a(Cyber.CYAN, 0.06));
        } else {
            Cyber.cutRect(renderer, x, y, width, height, 0, 0, cut, 0, Cyber.a(Cyber.SLATE, 0.06 + 0.08 * hover));
        }

        if (isSelected) {
            // Underline: magenta and cyan chasing each other to the right
            double lw = width - cut;
            double lh = 2 * u;
            double ly = y + height - lh;
            Cyber.glowRect(renderer, x, ly, lw, lh, 5 * u, Cyber.a(accent, 0.3));

            int steps = 16;
            for (int i = 0; i < steps; i++) {
                double f0 = (double) i / steps;
                double f1 = (double) (i + 1) / steps;
                renderer.quad(x + lw * f0, ly, lw / steps + 0.5, lh, chase(accent, f0, p), chase(accent, f1, p));
            }

            // Marker bar
            Cyber.glowRect(renderer, x, y, 3 * u, height, 6 * u, Cyber.a(accent, 0.35));
            renderer.quad(x, y, 3 * u, height, accent);
        }

        // Name and id
        Cyber.TextBatch text = Cyber.text(renderer, u);
        double nameH = Cyber.lineHeight(theme, NAME_FONT, u);
        double idH = Cyber.lineHeight(theme, ID_FONT, u);
        double gap = 1 * u;
        double top = y + (height - nameH - gap - idH) / 2;
        double tx = x + 16 * u;

        Color name = Cyber.mix(NAME_IDLE, Cyber.WHITE, Math.max(active, isSelected ? 1 : hover * 0.6));
        text.put(title, tx, top, NAME_FONT, name, 0.6, Cyber.BOLD);
        text.put(id, tx, top + nameH + gap, ID_FONT, isSelected ? accent : ID_IDLE, 1.2, 0);

        // Switch
        double cx = Math.round(x + width - HEX_INSET * u);
        double cy = Math.round(y + height / 2);
        double radius = HEX_RADIUS * u;

        if (active < 1) {
            double a = 1 - active;
            Cyber.hexagon(renderer, cx, cy, radius - 0.5 * u, Cyber.a(HEX_FILL_OFF, 0.65 * a));
            Cyber.hexagonOutline(renderer, cx, cy, radius - 0.5 * u, 1.2 * u, Cyber.a(HEX_EDGE_OFF, (0.45 + 0.4 * hover) * a));
        }

        if (active > 0) {
            // Breathing halo
            double halo = (3 + 4 * Cyber.breathe(p, 0.3 + seed)) * u;
            for (int i = 4; i >= 1; i--) {
                Cyber.hexagon(renderer, cx, cy, radius + halo * i / 2.2, Cyber.a(Cyber.CYAN, 0.085 * active));
            }

            // Radial fill, lit slightly above center
            Cyber.hexagon(renderer, cx, cy, radius, Cyber.a(HEX_OUTER, active));
            Cyber.hexagon(renderer, cx, cy - 0.5 * u, radius * 0.74, Cyber.a(HEX_MID, active));
            Cyber.hexagon(renderer, cx, cy - 1 * u, radius * 0.48, Cyber.a(HEX_INNER, active));
            Cyber.hexagonOutline(renderer, cx, cy, radius, 0.8 * u, Cyber.a(HEX_EDGE_ON, active));
            Cyber.hexagon(renderer, cx, cy, 3.6 * u, Cyber.a(HEX_CORE, active));
        }
    }

    /** Color of the travelling underline at horizontal fraction {@code f}; one full magenta-cyan cycle per width, two per loop. */
    private static Color chase(Color accent, double f, double p) {
        double t = 0.5 - 0.5 * Math.cos(Math.PI * 2 * (f - 2 * p));
        return Cyber.mix(accent, Cyber.CYAN, t);
    }
}
