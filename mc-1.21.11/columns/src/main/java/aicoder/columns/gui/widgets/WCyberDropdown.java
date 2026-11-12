package aicoder.columns.gui.widgets;

import aicoder.columns.gui.AiCoderGuiTheme;
import aicoder.columns.gui.Cyber;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.widgets.input.WDropdown;
import meteordevelopment.meteorclient.utils.render.color.Color;

public class WCyberDropdown<T> extends WDropdown<T> implements MeteorWidget {
    public WCyberDropdown(T[] values, T value) {
        super(values, value);
    }

    private double unit() {
        return ((AiCoderGuiTheme) theme).unit();
    }

    @Override
    protected WDropdownRoot createRootWidget() {
        return new WRoot();
    }

    @Override
    protected WDropdownValue createValueWidget() {
        return new WValue();
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        double u = unit();
        double pad = pad();
        double s = theme.textHeight();
        boolean hot = (mouseOver && !theme.disableHoverColor) || expanded;

        double cut = expanded ? 0 : Math.min(6 * u, height / 3);
        Cyber.box(renderer, x, y, width, height, 0, cut, 0, 0, Cyber.hair(u),
            Cyber.a(Cyber.FIELD, 0.55), Cyber.a(Cyber.CYAN_LINE, hot ? 0.8 : 0.32));

        renderer.text(get().toString(), x + pad, y + pad, Cyber.WHITE, false);

        // Caret
        double cw = s * 0.42;
        double cx = x + pad + maxValueWidth + pad + s / 2;
        double cy = y + height / 2;
        Cyber.tri(renderer, cx - cw / 2, cy - cw / 4, cx + cw / 2, cy - cw / 4, cx, cy + cw / 3, Cyber.CYAN);
    }

    private static class WRoot extends WDropdownRoot implements MeteorWidget {
        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            double u = ((AiCoderGuiTheme) theme).unit();
            double t = Cyber.hair(u);
            Color line = Cyber.a(Cyber.CYAN_LINE, 0.8);

            renderer.quad(x, y, width, height, Cyber.rgba(8, 6, 24, 0.96));
            renderer.quad(x, y + height - t, width, t, line);
            renderer.quad(x, y, t, height - t, line);
            renderer.quad(x + width - t, y, t, height - t, line);
        }
    }

    private class WValue extends WDropdownValue implements MeteorWidget {
        @Override
        protected void onCalculateSize() {
            double pad = pad();

            width = pad + theme.textWidth(value.toString()) + pad;
            height = pad + theme.textHeight() + pad;
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            double u = unit();
            boolean current = value.equals(WCyberDropdown.this.value);
            Color accent = theme().accentColor.get();

            if (mouseOver || pressed) {
                renderer.quad(x, y, width, height, Cyber.a(accent, 0.26), Cyber.a(accent, 0.03));
                renderer.quad(x, y, 2 * u, height, accent);
            }

            renderer.text(value.toString(), x + pad(), y + pad(), mouseOver || current ? Cyber.WHITE : theme().textColor.get(), false);
        }
    }
}
