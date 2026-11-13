package aicoder.aurora.gui.widgets;

import aicoder.aurora.gui.AuroraGuiTheme;
import aicoder.aurora.gui.Chrome;
import aicoder.aurora.gui.Fx;
import aicoder.aurora.gui.Style;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.utils.Cell;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.gui.widgets.containers.WWindow;

import java.util.Locale;

/**
 * A pane of glass with a title row. Category columns get a running number and a module count from the
 * modules screen; any other window (including columns built by another addon's screen) shows just its title.
 */
public class WAuroraWindow extends WWindow implements MeteorWidget {
    private static final double HEADER = 54;
    private static final double FONT = 14;
    private static final double TRACKING = 3.4;
    private static final double SMALL = 11;

    /** Set by the modules screen for category columns: slightly tighter corners. */
    public boolean column;
    /** Running number shown before the title; 0 hides it. */
    public int index;
    /** Module count shown in a pill at the right of the title; negative hides it. */
    public int count = -1;

    private final String label;

    public WAuroraWindow(WWidget icon, String title) {
        super(icon, title);
        this.label = title == null ? "" : title.toUpperCase(Locale.ROOT);
    }

    private AuroraGuiTheme aurora() {
        return (AuroraGuiTheme) theme;
    }

    @Override
    public void init() {
        super.init();

        // The tab strip is taller than Meteor's, so tall windows need a little more headroom
        view.maxHeight -= aurora().unit() * 70;
    }

    @Override
    protected WHeader header(WWidget icon) {
        return new WAuroraHeader(icon);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        AuroraGuiTheme aurora = aurora();
        aurora.backdrop(renderer);

        boolean open = expanded || animProgress > 0;
        double u = aurora.unit();
        Chrome.panel(renderer, x, y, width, open ? height : header.height, u, (column ? aurora.style.columnCorner : aurora.style.corner) * u, false);
    }

    private class WAuroraHeader extends WHeader {
        private double countWidth, indexWidth;

        private WAuroraHeader(WWidget icon) {
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

            double u = aurora().unit();
            countWidth = count >= 0 ? Fx.textWidth(theme, Integer.toString(count), SMALL, 0, u) + 18 * u : 0;
            indexWidth = index > 0 ? Fx.textWidth(theme, "00", 10.5, 1, u) + 10 * u : 0;

            double needed = 18 * u + indexWidth + Fx.textWidth(theme, label, FONT, TRACKING, u) + 10 * u + countWidth + 34 * u;
            width = Math.max(width, needed);
            height = Math.round(HEADER * u);
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            Style s = aurora().style;
            double u = aurora().unit();
            double t = Fx.hair(u);
            double cy = y + height / 2;

            if (expanded || animProgress > 0) {
                renderer.quad(x + 2 * u, y + height - t, width - 4 * u, t, Fx.rgba(255, 255, 255, 0.14));
            }

            Fx.TextBatch text = Fx.text(renderer, u);
            double tx = x + 18 * u;
            if (index > 0) {
                text.put(String.format("%02d", index), tx, Fx.centerY(theme, cy + u, 10.5, u), 10.5, s.faint, 1, 0);
                tx += indexWidth;
            }
            text.put(label, tx, Fx.centerY(theme, cy, FONT, u), FONT, s.title, TRACKING, Fx.BOLD);

            if (count >= 0) {
                double ph = 20 * u;
                double px = x + width - 36 * u - countWidth;
                Fx.box(renderer, px, cy - ph / 2, countWidth, ph, ph / 2, Fx.rgba(255, 255, 255, 0.10));
                text.put(Integer.toString(count), px + countWidth / 2, Fx.centerY(theme, cy, SMALL, u), SMALL, s.dim, 0, Fx.CENTER);
            }
        }
    }
}
