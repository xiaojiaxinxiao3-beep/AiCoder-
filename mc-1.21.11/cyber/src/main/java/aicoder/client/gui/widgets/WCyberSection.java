package aicoder.client.gui.widgets;

import aicoder.client.gui.AiCoderGuiTheme;
import aicoder.client.gui.Cyber;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WSection;

import java.util.Locale;

/** Collapsible group headed by {@code GENERAL ──── [-]}. */
public class WCyberSection extends WSection {
    private static final double FONT = 9.5;
    private static final double TRACKING = 2.5;
    private static final double GAP = 8;

    public WCyberSection(String title, boolean expanded, WWidget headerWidget) {
        super(title, expanded, headerWidget);
    }

    @Override
    protected WHeader createHeader() {
        return new WCyberHeader(title);
    }

    private class WCyberHeader extends WHeader {
        private final String label;
        private double labelWidth, toggleWidth;

        private WCyberHeader(String title) {
            super(title);
            this.label = title == null ? "" : title.toUpperCase(Locale.ROOT);
        }

        private double unit() {
            return ((AiCoderGuiTheme) theme).unit();
        }

        @Override
        public void init() {
            if (headerWidget != null) add(headerWidget).expandCellX().right();
        }

        @Override
        protected void onCalculateSize() {
            super.onCalculateSize();

            double u = unit();
            labelWidth = Cyber.textWidth(theme, label, FONT, TRACKING, u);
            toggleWidth = Cyber.textWidth(theme, "[+]", FONT, TRACKING, u);

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
            double u = unit();
            double ty = Cyber.centerY(theme, y + height / 2, FONT, u);

            Cyber.TextBatch text = Cyber.text(renderer, u);
            text.put(label, x, ty, FONT, Cyber.CYAN, TRACKING, Cyber.BOLD);
            text.put(expanded ? "[-]" : "[+]", x + width, ty, FONT, mouseOver ? Cyber.CYAN : Cyber.TEXT_FAINT, TRACKING, Cyber.RIGHT | Cyber.BOLD);

            double lineStart = x + labelWidth + GAP * u;
            double lineEnd = x + width - toggleWidth - GAP * u;
            if (headerWidget != null) lineEnd = Math.min(lineEnd, headerWidget.x - GAP * u);

            if (lineEnd > lineStart) {
                renderer.quad(lineStart, Math.round(y + height / 2), lineEnd - lineStart, Cyber.hair(u),
                    Cyber.a(Cyber.CYAN, 0.5), Cyber.a(Cyber.MAGENTA, 0.12));
            }
        }
    }
}
