package aicoder.glasscolumns.gui.widgets;

import aicoder.glasscolumns.gui.Fx;
import aicoder.glasscolumns.gui.GlassGuiTheme;
import aicoder.glasscolumns.gui.Style;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WSection;
import meteordevelopment.meteorclient.utils.render.color.Color;

/** Collapsible group: the title centered on a hairline, a caret at the right that points down when open. */
public class WGlassSection extends WSection {
    private static final double FONT = 11;
    private static final double TRACKING = 0.8;
    private static final double GAP = 8;

    public WGlassSection(String title, boolean expanded, WWidget headerWidget) {
        super(title, expanded, headerWidget);
    }

    @Override
    protected WHeader createHeader() {
        return new WGlassHeader(title);
    }

    private class WGlassHeader extends WHeader {
        private final String label;
        private double labelWidth, caret;

        private WGlassHeader(String title) {
            super(title);
            this.label = title == null ? "" : title;
        }

        private double unit() {
            return ((GlassGuiTheme) theme).unit();
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
            caret = 10 * u;

            width += labelWidth + caret + GAP * 5 * u;
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
            Style s = ((GlassGuiTheme) theme).style;
            double u = unit();
            double cy = Math.round(y + height / 2);
            double t = Fx.hair(u);

            double right = x + width - caret - GAP * u;
            if (headerWidget != null) right = Math.min(right, headerWidget.x - GAP * u);
            double mid = (x + right) / 2;

            Fx.text(renderer, u).put(label, mid, Fx.centerY(theme, cy, FONT, u), FONT, mouseOver ? s.title : s.accent, TRACKING, Fx.CENTER);

            Color line = Fx.rgba(255, 255, 255, 0.3);
            Color none = Fx.rgba(255, 255, 255, 0.04);
            double half = labelWidth / 2 + GAP * 0.6 * u;
            if (mid - half > x) renderer.quad(x, cy, mid - half - x, t, none, line);
            if (right > mid + half) renderer.quad(mid + half, cy, right - mid - half, t, line, none);

            // Caret: down when open, sideways when folded
            double cx = x + width - caret / 2, a = 4 * u * (0.6 + 0.4 * animProgress);
            Color c = mouseOver ? s.title : s.text;
            if (expanded) Fx.tri(renderer, cx - a, cy - a * 0.5, cx + a, cy - a * 0.5, cx, cy + a * 0.7, c);
            else Fx.tri(renderer, cx - a * 0.5, cy - a, cx - a * 0.5, cy + a, cx + a * 0.7, cy, c);
        }
    }
}
