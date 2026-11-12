package aicoder.glasscolumns.gui.widgets;

import aicoder.glasscolumns.gui.Chrome;
import aicoder.glasscolumns.gui.Fx;
import aicoder.glasscolumns.gui.GlassGuiTheme;
import aicoder.glasscolumns.gui.Sounds;
import aicoder.glasscolumns.gui.Style;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WView;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPressable;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

import static com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT;
import static com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT;
import static meteordevelopment.meteorclient.MeteorClient.mc;
import static meteordevelopment.meteorclient.utils.Utils.getWindowHeight;
import static meteordevelopment.meteorclient.utils.Utils.getWindowWidth;

/**
 * One line in a column. Left click toggles the module (with a water-drop sound), right click opens its
 * settings. Off is plain text; on is a lit capsule of the two glass colors with a droplet at the right;
 * hovering lifts a clear capsule under the row and shows a detail card beside it.
 */
public class WGlassModule extends WPressable implements MeteorWidget {
    private static final double HEIGHT = 21;
    private static final double FONT = 10.5;
    private static final double TIP_WIDTH = 230;
    private static final double TIP_DELAY = 0.25;

    private final Module module;
    private final String title;
    private final double seed;

    private double active;
    private double hover;
    private double hoverTime;
    private List<String> wrapped;
    private double wrappedUnit;

    public WGlassModule(Module module, String title) {
        this.module = module;
        this.title = title;
        this.seed = ((module.name.hashCode() >>> 8) & 0xFF) / 255.0;
        this.active = module.isActive() ? 1 : 0;
    }

    private GlassGuiTheme glass() {
        return (GlassGuiTheme) theme;
    }

    @Override
    protected void onCalculateSize() {
        double u = glass().unit();
        width = (16 + 26) * u + Fx.textWidth(theme, title, FONT, 0.2, u);
        height = Math.round(HEIGHT * u);
    }

    @Override
    protected void onPressed(int button) {
        if (button == MOUSE_BUTTON_LEFT) {
            module.toggle();
            Sounds.toggle(glass(), module.isActive());
        } else if (button == MOUSE_BUTTON_RIGHT) {
            mc.gui.setScreen(theme.moduleScreen(module));
        }
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        Style s = glass().style;
        double u = glass().unit();

        // A row scrolled under the column's edge still reports the mouse over it
        WView view = getView();
        boolean hovered = mouseOver && (view == null || view.mouseOver);

        active = Mth.clamp(active + delta * 8 * (module.isActive() ? 1 : -1), 0, 1);
        hover = Mth.clamp(hover + delta * 10 * (hovered ? 1 : -1), 0, 1);
        hoverTime = hovered ? hoverTime + delta : 0;

        // Capsule, inset from the column's edges
        double px = x + 5 * u, pw = width - 10 * u;
        double py = y + 1 * u, ph = height - 2 * u;
        double corner = ph / 2;

        if (active > 0) {
            Fx.box(renderer, px, py, pw, ph, corner, Fx.a(s.colorA, 0.55 * active), Fx.a(s.colorB, 0.5 * active), Fx.a(s.colorB, 0.55 * active), Fx.a(s.colorA, 0.5 * active));
            Fx.box(renderer, px + u, py + u, pw - 2 * u, ph * 0.5, corner - u, Fx.rgba(255, 255, 255, 0.2 * active), Fx.rgba(255, 255, 255, 0));
            Fx.boxOutline(renderer, px, py, pw, ph, corner, Fx.hair(u), Fx.rgba(255, 255, 255, 0.5 * active), Fx.rgba(255, 255, 255, 0.12 * active));
        }
        if (hover > 0) {
            Fx.box(renderer, px, py, pw, ph, corner, Fx.rgba(255, 255, 255, 0.13 * hover), Fx.rgba(255, 255, 255, 0.05 * hover));
            Fx.boxOutline(renderer, px, py, pw, ph, corner, Fx.hair(u), Fx.rgba(255, 255, 255, 0.45 * hover), Fx.rgba(255, 255, 255, 0.1 * hover));
        }

        Color text = Fx.mix(s.dim, s.title, Math.max(active, hover * 0.7));
        Fx.text(renderer, u).put(title, x + 16 * u, Fx.centerY(theme, y + height / 2.0, FONT, u), FONT, text, 0.2, 0);

        if (active > 0) {
            double r = 4.2 * u * active;
            double cx = x + width - 16 * u, cy = y + height / 2.0;
            double pulse = 0.6 + 0.4 * Fx.flow(Fx.phase(4), 0.3 + seed);
            Fx.glowDot(renderer, cx, cy, r, 4 * u * pulse, s.accent, active);
            Fx.circle(renderer, cx, cy, r, Fx.a(s.accent, active));
            Fx.circle(renderer, cx - r * 0.25, cy - r * 0.3, r * 0.4, Fx.rgba(255, 255, 255, 0.9 * active));
        }

        if (hoverTime >= TIP_DELAY) renderTip(renderer, s, u);
    }

    /** Detail card beside the row: name, description and state. Drawn after everything else so no column covers it. */
    private void renderTip(GuiRenderer renderer, Style s, double u) {
        double pad = 12 * u;
        double w = TIP_WIDTH * u;
        double inner = w - pad * 2;

        if (wrapped == null || wrappedUnit != u) {
            wrapped = wrap(module.description, inner, u);
            wrappedUnit = u;
        }

        double nameH = Fx.lineHeight(theme, 11, u);
        double lineH = Fx.lineHeight(theme, 10, u) * 1.12;
        double metaH = Fx.lineHeight(theme, 8.5, u);
        double h = 10 * u + nameH + 5 * u + wrapped.size() * lineH + 5 * u + metaH + 10 * u;

        // To the right of the row when it fits, otherwise to the left; kept on screen
        double tx = x + width + 8 * u;
        if (tx + w > getWindowWidth()) tx = x - 8 * u - w;
        tx = Math.max(0, tx);
        double ty = Mth.clamp(y - 6 * u, 0, Math.max(0, getWindowHeight() - h));

        double fx = tx, fy = ty;
        boolean on = module.isActive();
        List<String> lines = wrapped;

        renderer.absolutePost(() -> {
            renderer.scissorStart(0, 0, getWindowWidth(), getWindowHeight());

            double corner = 12 * u;
            Fx.glowRect(renderer, fx + corner, fy + corner, w - corner * 2, h - corner * 2, corner + 14 * u, Fx.rgba(0, 0, 10, 0.3));
            Fx.box(renderer, fx, fy, w, h, corner, s.solid);
            Fx.box(renderer, fx + 2 * u, fy + 2 * u, w - 4 * u, h * 0.4, corner - 2 * u, Fx.rgba(255, 255, 255, 0.12), Fx.rgba(255, 255, 255, 0));
            Fx.boxOutline(renderer, fx, fy, w, h, corner, Math.max(1, Math.round(1.3 * u)), Fx.rgba(255, 255, 255, 0.65), Fx.rgba(255, 255, 255, 0.18));

            Fx.TextBatch text = Fx.text(renderer, u);
            double cy = fy + 10 * u;
            text.put(title, fx + pad, cy, 11, s.title, 0.6, Fx.BOLD);
            cy += nameH + 5 * u;
            for (String line : lines) {
                text.put(line, fx + pad, cy, 10, s.text);
                cy += lineH;
            }
            text.put(on ? "ON" : "OFF", fx + pad, cy + 5 * u, 8.5, on ? s.accent : s.faint, 1.5, Fx.BOLD);
            text.put("RIGHT CLICK · SETTINGS", fx + w - pad, cy + 5 * u, 8.5, s.faint, 1, Fx.RIGHT | Fx.BOLD);

            renderer.scissorEnd();
        });
    }

    private List<String> wrap(String text, double maxWidth, double u) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();

        for (String word : text.split("\\s+")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (!line.isEmpty() && Fx.textWidth(theme, candidate, 10, 0, u) > maxWidth) {
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
