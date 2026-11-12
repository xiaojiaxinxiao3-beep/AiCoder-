package aicoder.columns.gui.widgets;

import aicoder.columns.gui.AiCoderGuiTheme;
import aicoder.columns.gui.Cyber;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.utils.Cell;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.gui.widgets.containers.WWindow;
import meteordevelopment.meteorclient.utils.render.color.Color;

import java.util.Locale;

/**
 * A column: tinted title bar, dark glass body with the bottom-right corner cut, and two cyan
 * brackets hugging the bottom corners. Used for module categories and for every other window.
 */
public class WColumnWindow extends WWindow implements MeteorWidget {
    private static final double HEADER = 30;
    private static final double FONT = 10.5;
    private static final double TRACKING = 2.2;
    private static final double SMALL = 8.5;

    /** Position among the category columns, shown as {@code 01}; 0 hides it. */
    public int number;
    /** Module count shown as {@code [21]}; negative hides it. */
    public int count = -1;
    /** Cyan title bar and outline instead of magenta, for the addon's own column. */
    public boolean cyan;

    private final String label;

    public WColumnWindow(WWidget icon, String title) {
        super(icon, title);
        this.label = title.toUpperCase(Locale.ROOT);
    }

    private double unit() {
        return ((AiCoderGuiTheme) theme).unit();
    }

    private Color tint() {
        return cyan ? Cyber.CYAN : theme().accentColor.get();
    }

    @Override
    public void init() {
        super.init();

        // The tab strip and logo sit above the windows, so tall ones need a little more headroom
        view.maxHeight -= theme.scale(40);
    }

    @Override
    protected WHeader header(WWidget icon) {
        return new WColumnHeader(icon);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        ((AiCoderGuiTheme) theme).backdrop(renderer);

        double u = unit();
        boolean open = expanded || animProgress > 0;
        double h = open ? height : header.height;
        double cut = Math.min(12 * u, h / 3);
        double t = Cyber.hair(u);
        Color tint = tint();

        Cyber.occlude(x, y, width, h);

        Color top = theme().backgroundColor.get();
        Color bottom = Cyber.rgba(8, 6, 22, Math.min(1, top.a / 255.0 * 1.13));
        Cyber.cutRect(renderer, x, y, width, h, 0, 0, cut, 0, top, top, bottom, bottom);
        Cyber.cutOutline(renderer, x, y, width, h, 0, 0, cut, 0, t, Cyber.a(tint, cyan ? 0.55 : 0.5));

        // While folding, the window is scissored to its own bounds, which would crop the brackets
        if (animProgress != 0 && animProgress != 1) return;

        double p = Cyber.phase();
        double seed = (x * 0.0131 + y * 0.0071) % 1;
        double bt = 2 * u, glow = 4 * u, o = 4 * u, arm = 14 * u;

        // Bottom-left: an L
        double a1 = 0.45 + 0.45 * Cyber.breathe(p, seed);
        Cyber.glowLine(renderer, x - o, y + h - arm, x - o, y + h + o + bt / 2, bt, Cyber.CYAN, glow, a1);
        Cyber.glowLine(renderer, x - o, y + h + o, x - o + arm + o, y + h + o, bt, Cyber.CYAN, glow, a1);

        // Bottom-right: a short stroke along the cut
        double a2 = 0.45 + 0.45 * Cyber.breathe(p, seed + 0.211);
        Cyber.glowLine(renderer, x + width - cut - 3 * u, y + h + o, x + width + 3 * u, y + h - cut - 2 * u, bt, Cyber.CYAN, glow, a2);
    }

    private class WColumnHeader extends WHeader {
        private double numberWidth, countWidth;

        private WColumnHeader(WWidget icon) {
            super(icon);
        }

        @Override
        public void init() {
            super.init();

            // Meteor adds its own centered title label; this header draws the title itself
            hideLabels(this);
        }

        private void hideLabels(WContainer container) {
            for (Cell<?> cell : container.cells) {
                if (cell.widget() instanceof WLabel) cell.widget().visible = false;
                else if (cell.widget() instanceof WContainer inner) hideLabels(inner);
            }
        }

        @Override
        protected void onCalculateSize() {
            super.onCalculateSize();

            double u = unit();
            numberWidth = number > 0 ? Cyber.textWidth(theme, "00", SMALL, 0.5, u) + 7 * u : 0;
            countWidth = count >= 0 ? Cyber.textWidth(theme, "[" + count + "]", SMALL, 0.5, u) + 7 * u : 0;

            double needed = 9 * u + numberWidth + Cyber.textWidth(theme, label, FONT, TRACKING, u) + 10 * u + countWidth + 22 * u;
            width = Math.max(width, needed);
            height = Math.round(HEADER * u);
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            double u = unit();
            double t = Cyber.hair(u);
            Color tint = tint();
            double cy = y + height / 2;

            renderer.quad(x + t, y + t, width - t * 2, height - t, Cyber.a(tint, cyan ? 0.35 : 0.42), Cyber.a(tint, cyan ? 0.08 : 0.10));
            if (expanded || animProgress > 0) renderer.quad(x + t, y + height - t, width - t * 2, t, Cyber.a(tint, cyan ? 0.8 : 0.75));

            Cyber.TextBatch text = Cyber.text(renderer, u);
            double tx = x + 9 * u;

            if (number > 0) {
                text.put(String.format("%02d", number), tx, Cyber.centerY(theme, cy, SMALL, u), SMALL, Cyber.rgba(255, 255, 255, 0.65), 0.5, Cyber.BOLD);
                tx += numberWidth;
            }

            text.glow(label, tx, Cyber.centerY(theme, cy, FONT, u), FONT, Cyber.PURE_WHITE, TRACKING, Cyber.BOLD, tint, 1.4);

            if (count >= 0) {
                text.put("[" + count + "]", x + width - 22 * u, Cyber.centerY(theme, cy, SMALL, u), SMALL, Cyber.a(Cyber.CYAN, 0.85), 0.5, Cyber.RIGHT | Cyber.BOLD);
            }
        }
    }
}
