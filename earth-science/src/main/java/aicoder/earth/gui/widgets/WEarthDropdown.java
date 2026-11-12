package aicoder.earth.gui.widgets;

import aicoder.earth.gui.Chrome;
import aicoder.earth.gui.EarthGuiTheme;
import aicoder.earth.gui.Fx;
import aicoder.earth.gui.Style;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.widgets.input.WDropdown;
import meteordevelopment.meteorclient.utils.render.color.Color;

public class WEarthDropdown<T> extends WDropdown<T> implements MeteorWidget {
    public WEarthDropdown(T[] values, T value) {
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
        EarthGuiTheme earth = (EarthGuiTheme) theme;
        Style s = earth.style;
        double pad = pad();
        double ts = theme.textHeight();
        boolean hot = (mouseOver && !theme.disableHoverColor) || expanded;

        Chrome.field(renderer, s, x, y, width, height, earth.unit(), hot, false);
        renderer.text(get().toString(), x + pad, y + pad, s.title, false);

        // Caret
        double cw = ts * 0.42;
        double cx = x + pad + maxValueWidth + pad + ts / 2;
        double cy = y + height / 2;
        Fx.tri(renderer, cx - cw / 2, cy - cw / 4, cx + cw / 2, cy - cw / 4, cx, cy + cw / 3, s.accent);
    }

    private static class WRoot extends WDropdownRoot implements MeteorWidget {
        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            EarthGuiTheme earth = (EarthGuiTheme) theme;
            double t = Fx.hair(earth.unit());
            Color line = Fx.a(earth.style.accent, 0.8);

            renderer.quad(x, y, width, height, Chrome.solid(earth.style));
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
            Style s = ((EarthGuiTheme) theme).style;
            boolean current = value.equals(WEarthDropdown.this.value);

            if (mouseOver || pressed) renderer.quad(x, y, width, height, Fx.a(s.accent, 0.22));
            renderer.text(value.toString(), x + pad(), y + pad(), mouseOver || current ? s.title : theme().textColor.get(), false);
        }
    }
}
