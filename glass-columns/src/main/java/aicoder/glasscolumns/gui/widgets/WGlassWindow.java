package aicoder.glasscolumns.gui.widgets;

import aicoder.glasscolumns.gui.Chrome;
import aicoder.glasscolumns.gui.Fx;
import aicoder.glasscolumns.gui.GlassGuiTheme;
import aicoder.glasscolumns.gui.Style;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.utils.Cell;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.gui.widgets.containers.WWindow;

import java.util.Locale;

/**
 * A pane of liquid glass with a title bar. Module categories use it as a narrow column (tighter corners,
 * a count pill, no drifting highlights); every other window gets the full treatment.
 */
public class WGlassWindow extends WWindow implements MeteorWidget {
    private static final double HEADER = 32;
    private static final double FONT = 11;
    private static final double TRACKING = 2;
    private static final double SMALL = 8.5;
    private static final double COLUMN_RADIUS = 14;

    /** Set by the modules screen for category columns. */
    public boolean column;
    /** Module count shown in a pill at the right of the title; negative hides it. */
    public int count = -1;

    private final String label;

    public WGlassWindow(WWidget icon, String title) {
        super(icon, title);
        this.label = title.toUpperCase(Locale.ROOT);
    }

    private GlassGuiTheme glass() {
        return (GlassGuiTheme) theme;
    }

    @Override
    public void init() {
        super.init();

        // The tab strip sits above the windows, so tall ones need a little more headroom
        view.maxHeight -= theme.scale(48);
    }

    @Override
    protected WHeader header(WWidget icon) {
        return new WGlassHeader(icon);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        GlassGuiTheme glass = glass();
        glass.backdrop(renderer);

        boolean open = expanded || animProgress > 0;
        double h = open ? height : header.height;
        boolean liquid = !column && open && glass.liquid.get();
        Chrome.panel(renderer, glass.style, x, y, width, h, glass.unit(), liquid, column ? COLUMN_RADIUS : glass.style.corner);
    }

    private class WGlassHeader extends WHeader {
        private double countWidth;

        private WGlassHeader(WWidget icon) {
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

            double u = glass().unit();
            countWidth = count >= 0 ? Fx.textWidth(theme, Integer.toString(count), SMALL, 0.5, u) + 12 * u : 0;

            double needed = 14 * u + Fx.textWidth(theme, label, FONT, TRACKING, u) + 10 * u + countWidth + 24 * u;
            width = Math.max(width, needed);
            height = Math.round(HEADER * u);
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            Style s = glass().style;
            double u = glass().unit();
            double t = Fx.hair(u);
            double cy = y + height / 2;

            if (expanded || animProgress > 0) {
                renderer.quad(x + 4 * u, y + height - t, width / 2 - 4 * u, t, Fx.rgba(255, 255, 255, 0.04), Fx.rgba(255, 255, 255, 0.22));
                renderer.quad(x + width / 2, y + height - t, width / 2 - 4 * u, t, Fx.rgba(255, 255, 255, 0.22), Fx.rgba(255, 255, 255, 0.04));
            }

            Fx.TextBatch text = Fx.text(renderer, u);
            text.put(label, x + 14 * u, Fx.centerY(theme, cy, FONT, u), FONT, s.title, TRACKING, Fx.BOLD);

            if (count >= 0) {
                // Count in a small pill of darker glass
                double ph = 14 * u;
                double px = x + width - 24 * u - countWidth;
                Fx.box(renderer, px, cy - ph / 2, countWidth, ph, ph / 2, Fx.rgba(8, 12, 34, 0.42));
                Fx.boxOutline(renderer, px, cy - ph / 2, countWidth, ph, ph / 2, t, Fx.rgba(0, 0, 0, 0.25), Fx.rgba(255, 255, 255, 0.28));
                text.put(Integer.toString(count), px + countWidth / 2 + 0.25 * u, Fx.centerY(theme, cy, SMALL, u), SMALL, s.accent, 0.5, Fx.CENTER | Fx.BOLD);
            }
        }
    }
}
