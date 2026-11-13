package aicoder.auroracolumns.gui.widgets;

import aicoder.auroracolumns.gui.AuroraGuiTheme;
import aicoder.auroracolumns.gui.Fx;
import aicoder.auroracolumns.gui.Style;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WSection;
import meteordevelopment.meteorclient.utils.render.color.Color;

import java.util.Locale;

/** Collapsible group: a small green title, then a hairline running from green through violet to nothing, and a caret. */
public class WAuroraSection extends WSection {
    private static final double FONT = 12;
    private static final double TRACKING = 3.4;
    private static final double GAP = 14;

    public WAuroraSection(String title, boolean expanded, WWidget headerWidget) {
        super(title, expanded, headerWidget);
    }

    @Override
    protected WHeader createHeader() {
        return new WAuroraHeader(title);
    }

    private class WAuroraHeader extends WHeader {
        private final String label;
        private double labelWidth, caret;

        private WAuroraHeader(String title) {
            super(title);
            this.label = title == null ? "" : title.toUpperCase(Locale.ROOT);
        }

        private double unit() {
            return ((AuroraGuiTheme) theme).unit();
        }

        @Override
        public void init() {
            if (headerWidget != null) add(headerWidget).expandCellX().right();
        }

        @Override
        protected void onCalculateSize() {
            super.onCalculateSize();

            double u = unit();
            labelWidth = Fx.textWidth(theme, label, FONT, TRACKING, u);
            caret = 14 * u;

            width += labelWidth + caret + GAP * 3 * u;
            height = Math.max(height, Math.round(theme.textHeight() * 1.15));
        }

        @Override
        protected void onCalculateWidgetPositions() {
            // Keep the optional header widget clear of the caret on the right
            double reserved = caret + GAP * unit();
            width -= reserved;
            super.onCalculateWidgetPositions();
            width += reserved;
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            Style s = ((AuroraGuiTheme) theme).style;
            double u = unit();
            double cy = Math.round(y + height / 2);
            double t = Fx.hair(u);

            double right = x + width - caret - GAP * u;
            if (headerWidget != null) right = Math.min(right, headerWidget.x - GAP * u);

            Fx.text(renderer, u).put(label, x, Fx.centerY(theme, cy, FONT, u), FONT, mouseOver ? s.title : s.green, TRACKING, Fx.BOLD);

            double lx = x + labelWidth + GAP * u;
            if (right > lx) {
                double half = (right - lx) / 2;
                renderer.quad(lx, cy, half, t, Fx.a(s.green, 0.55), Fx.a(s.violet, 0.25));
                renderer.quad(lx + half, cy, half, t, Fx.a(s.violet, 0.25), Fx.a(s.violet, 0));
            }

            // Caret: down when open, sideways when folded
            double cx = x + width - caret / 2, a = 5 * u * (0.6 + 0.4 * animProgress);
            Color c = mouseOver ? s.title : s.dim;
            if (expanded) Fx.tri(renderer, cx - a, cy - a * 0.5, cx + a, cy - a * 0.5, cx, cy + a * 0.7, c);
            else Fx.tri(renderer, cx - a * 0.5, cy - a, cx - a * 0.5, cy + a, cx + a * 0.7, cy, c);
        }
    }
}
