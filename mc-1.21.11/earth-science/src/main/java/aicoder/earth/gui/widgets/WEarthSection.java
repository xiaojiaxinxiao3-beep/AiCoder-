package aicoder.earth.gui.widgets;

import aicoder.earth.gui.EarthGuiTheme;
import aicoder.earth.gui.Fx;
import aicoder.earth.gui.Style;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WSection;

import java.util.Locale;

/** Collapsible group headed by {@code GENERAL ──── [-]}. */
public class WEarthSection extends WSection {
    private static final double FONT = 9.5;
    private static final double TRACKING = 2.5;
    private static final double GAP = 8;

    public WEarthSection(String title, boolean expanded, WWidget headerWidget) {
        super(title, expanded, headerWidget);
    }

    @Override
    protected WHeader createHeader() {
        return new WEarthHeader(title);
    }

    private class WEarthHeader extends WHeader {
        private final String label;
        private double labelWidth, toggleWidth;

        private WEarthHeader(String title) {
            super(title);
            this.label = title == null ? "" : title.toUpperCase(Locale.ROOT);
        }

        private double unit() {
            return ((EarthGuiTheme) theme).unit();
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
            toggleWidth = Fx.textWidth(theme, "[+]", FONT, TRACKING, u);

            width += labelWidth + toggleWidth + GAP * 4 * u;
            height = Math.max(height, Math.round(theme.textHeight() * 1.15));
        }

        @Override
        protected void onCalculateWidgetPositions() {
            // Keep the optional header widget clear of the [+] toggle on the right
            double reserved = toggleWidth + GAP * unit();
            width -= reserved;
            super.onCalculateWidgetPositions();
            width += reserved;
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            Style s = ((EarthGuiTheme) theme).style;
            double u = unit();
            double ty = Fx.centerY(theme, y + height / 2, FONT, u);

            Fx.TextBatch text = Fx.text(renderer, u);
            text.put(label, x, ty, FONT, s.accent, TRACKING, Fx.BOLD);
            text.put(expanded ? "[-]" : "[+]", x + width, ty, FONT, mouseOver ? s.title : s.accent, TRACKING, Fx.RIGHT | Fx.BOLD);

            double lineStart = x + labelWidth + GAP * u;
            double lineEnd = x + width - toggleWidth - GAP * u;
            if (headerWidget != null) lineEnd = Math.min(lineEnd, headerWidget.x - GAP * u);

            if (lineEnd > lineStart) {
                renderer.quad(lineStart, Math.round(y + height / 2), lineEnd - lineStart, Fx.hair(u), Fx.a(s.outline, 0.8));
            }
        }
    }
}
