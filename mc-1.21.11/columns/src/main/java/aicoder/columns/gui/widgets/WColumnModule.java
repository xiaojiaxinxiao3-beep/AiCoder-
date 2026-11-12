package aicoder.columns.gui.widgets;

import aicoder.columns.gui.AiCoderGuiTheme;
import aicoder.columns.gui.Cyber;
import aicoder.columns.gui.Sounds;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WView;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPressable;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;
import static meteordevelopment.meteorclient.MeteorClient.mc;
import static meteordevelopment.meteorclient.utils.Utils.getWindowHeight;
import static meteordevelopment.meteorclient.utils.Utils.getWindowWidth;

/**
 * One line in a column. Left click toggles the module (with a blip), right click opens its settings.
 * Off is plain grey text; on is white with a cyan bar and diamond; hovering adds a magenta frame,
 * a flowing underline and a detail card beside the row.
 */
public class WColumnModule extends WPressable implements MeteorWidget {
    private static final double HEIGHT = 19;
    private static final double FONT = 10.5;
    private static final double TIP_WIDTH = 230;
    private static final double TIP_DELAY = 0.25;

    private static final Color OFF_TEXT = new Color(122, 139, 163);
    private static final Color TIP_TEXT = new Color(207, 224, 240);

    private final Module module;
    private final String title;
    private final String id;
    private final double seed;

    private double active;
    private double hover;
    private double hoverTime;
    private List<String> wrapped;
    private double wrappedUnit;

    public WColumnModule(Module module, String title) {
        this.module = module;
        this.title = title;

        int hash = module.name.hashCode();
        this.id = String.format("MOD-0x%02X", hash & 0xFF);
        this.seed = ((hash >>> 8) & 0xFF) / 255.0;
        this.active = module.isActive() ? 1 : 0;
    }

    private double unit() {
        return ((AiCoderGuiTheme) theme).unit();
    }

    @Override
    protected void onCalculateSize() {
        double u = unit();
        width = (12 + 22) * u + Cyber.textWidth(theme, title, FONT, 0.2, u);
        height = Math.round(HEIGHT * u);
    }

    @Override
    protected void onPressed(int button) {
        if (button == GLFW_MOUSE_BUTTON_LEFT) {
            module.toggle();
            Sounds.toggle((AiCoderGuiTheme) theme, module.isActive());
        } else if (button == GLFW_MOUSE_BUTTON_RIGHT) {
            mc.setScreen(theme.moduleScreen(module));
        }
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        double u = unit();
        double p = Cyber.phase();
        Color accent = theme().accentColor.get();

        // A row scrolled under the column's edge still reports the mouse over it
        WView view = getView();
        boolean hovered = mouseOver && (view == null || view.mouseOver);

        active = MathHelper.clamp(active + delta * 8 * (module.isActive() ? 1 : -1), 0, 1);
        hover = MathHelper.clamp(hover + delta * 10 * (hovered ? 1 : -1), 0, 1);
        hoverTime = hovered ? hoverTime + delta : 0;

        if (active > 0) {
            // Cyan wash fading out to the right, a bar on the left, a diamond on the right; both breathe
            double breath = 0.6 + 0.4 * Cyber.breathe(p, seed);
            renderer.quad(x, y, width * 0.75, height, Cyber.a(Cyber.CYAN, 0.16 * active), Cyber.a(Cyber.CYAN, 0));

            double bx = x, bw = 2 * u, by = y + 3 * u, bh = height - 6 * u;
            Cyber.glowRect(renderer, bx, by, bw, bh, 4 * u, Cyber.a(Cyber.CYAN, 0.3 * breath * active));
            renderer.quad(bx, by, bw, bh, Cyber.a(Cyber.CYAN, breath * active));

            double dx = x + width - 11 * u, dy = y + height / 2, d = 2.9 * u;
            for (int i = 3; i >= 1; i--) diamond(renderer, dx, dy, d + i * 1.6 * u, Cyber.a(Cyber.CYAN, 0.1 * breath * active));
            diamond(renderer, dx, dy, d, Cyber.a(Cyber.CYAN, breath * active));
        }

        if (hover > 0) {
            double t = Cyber.hair(u);
            renderer.quad(x, y, width, height, Cyber.a(accent, 0.14 * hover));
            Cyber.cutOutline(renderer, x, y, width, height, 0, 0, 0, 0, t, Cyber.a(accent, 0.85 * hover));

            // Underline: magenta and cyan chasing each other to the right
            double lh = 2 * u;
            int steps = 12;
            for (int i = 0; i < steps; i++) {
                double f0 = (double) i / steps, f1 = (double) (i + 1) / steps;
                renderer.quad(x + width * f0, y + height - lh, width / steps + 0.5, lh,
                    Cyber.a(chase(accent, f0, p), hover), Cyber.a(chase(accent, f1, p), hover));
            }
        }

        Color text = hover > 0.5 ? Cyber.PURE_WHITE : Cyber.mix(OFF_TEXT, Cyber.WHITE, active);
        Cyber.text(renderer, u).put(title, x + 12 * u, Cyber.centerY(theme, y + height / 2.0, FONT, u), FONT, text, 0.2, 0);

        if (hoverTime >= TIP_DELAY) renderTip(renderer, u, accent);
    }

    private static void diamond(GuiRenderer renderer, double cx, double cy, double r, Color c) {
        Cyber.tri(renderer, cx - r, cy, cx, cy - r, cx + r, cy, c);
        Cyber.tri(renderer, cx - r, cy, cx + r, cy, cx, cy + r, c);
    }

    /** Color of the travelling underline at horizontal fraction {@code f}; two full cycles per loop. */
    private static Color chase(Color accent, double f, double p) {
        return Cyber.mix(accent, Cyber.CYAN, 0.5 - 0.5 * Math.cos(Math.PI * 2 * (f - 2 * p)));
    }

    /** Detail card beside the row: name, description, id and state. Drawn after everything else so no column covers it. */
    private void renderTip(GuiRenderer renderer, double u, Color accent) {
        double pad = 10 * u;
        double w = TIP_WIDTH * u;
        double inner = w - pad * 2;

        if (wrapped == null || wrappedUnit != u) {
            wrapped = wrap(module.description, inner, u);
            wrappedUnit = u;
        }

        double nameH = Cyber.lineHeight(theme, 10.5, u);
        double lineH = Cyber.lineHeight(theme, 10, u) * 1.12;
        double metaH = Cyber.lineHeight(theme, 8.5, u);
        double h = 8 * u + nameH + 5 * u + wrapped.size() * lineH + 4 * u + metaH + 8 * u;

        // To the right of the row when it fits, otherwise to the left; kept on screen
        double tx = x + width + 8 * u;
        if (tx + w > getWindowWidth()) tx = x - 8 * u - w;
        tx = Math.max(0, tx);
        double ty = MathHelper.clamp(y - 4 * u, 0, Math.max(0, getWindowHeight() - h));

        double fx = tx, fy = ty;
        String name = title.toUpperCase(Locale.ROOT);
        String meta = id + " · " + (module.isActive() ? "ENABLED" : "DISABLED");
        List<String> lines = wrapped;

        renderer.absolutePost(() -> {
            renderer.scissorStart(0, 0, getWindowWidth(), getWindowHeight());
            WCyberTooltip.tip(renderer, fx, fy, w, h, u, accent);

            Cyber.TextBatch text = Cyber.text(renderer, u);
            double cy = fy + 8 * u;
            text.put(name, fx + pad, cy, 10.5, Cyber.PURE_WHITE, 1.5, Cyber.BOLD);
            cy += nameH + 5 * u;
            for (String line : lines) {
                text.put(line, fx + pad, cy, 10, TIP_TEXT);
                cy += lineH;
            }
            text.put(meta, fx + pad, cy + 4 * u, 8.5, Cyber.CYAN, 1, Cyber.BOLD);
            renderer.scissorEnd();
        });
    }

    private List<String> wrap(String text, double maxWidth, double u) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();

        for (String word : text.split("\\s+")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (!line.isEmpty() && Cyber.textWidth(theme, candidate, 10, 0, u) > maxWidth) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (!line.isEmpty()) lines.add(line.toString());
        return lines;
    }
}
