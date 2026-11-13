package aicoder.auroracolumns.gui.widgets;

import aicoder.auroracolumns.gui.AuroraGuiTheme;
import aicoder.auroracolumns.gui.Chrome;
import aicoder.auroracolumns.gui.Fx;
import aicoder.auroracolumns.gui.Style;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.widgets.input.WDropdown;
import meteordevelopment.meteorclient.utils.render.color.Color;

public class WAuroraDropdown<T> extends WDropdown<T> implements MeteorWidget {
    public WAuroraDropdown(T[] values, T value) {
        super(values, value);
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
        AuroraGuiTheme aurora = (AuroraGuiTheme) theme;
        Style s = aurora.style;
        double pad = pad();
        double ts = theme.textHeight();
        boolean hot = (mouseOver && !theme.disableHoverColor) || expanded;

        Chrome.field(renderer, s, x, y, width, height, aurora.unit(), hot, false);
        renderer.text(get().toString(), x + pad, y + pad, s.title, false);

        // Caret
        double cw = ts * 0.42;
        double cx = x + pad + maxValueWidth + pad + ts / 2;
        double cy = y + height / 2;
        Fx.tri(renderer, cx - cw / 2, cy - cw / 4, cx + cw / 2, cy - cw / 4, cx, cy + cw / 3, hot ? s.title : s.dim);
    }

    private static class WRoot extends WDropdownRoot implements MeteorWidget {
        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            AuroraGuiTheme aurora = (AuroraGuiTheme) theme;
            double t = Fx.hair(aurora.unit());
            Color line = Fx.rgba(255, 255, 255, 0.3);

            renderer.quad(x, y, width, height, Fx.rgba(20, 44, 78, 0.97), Fx.rgba(20, 44, 78, 0.97), Fx.rgba(8, 18, 42, 0.98), Fx.rgba(8, 18, 42, 0.98));
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
            Style s = ((AuroraGuiTheme) theme).style;
            boolean current = value.equals(WAuroraDropdown.this.value);

            if (mouseOver || pressed) Chrome.gradient(renderer, x, y, width, height, 0, 0.36, 0.22, 0.3);
            renderer.text(value.toString(), x + pad(), y + pad(), mouseOver || current ? s.title : s.dim, false);
        }
    }
}
