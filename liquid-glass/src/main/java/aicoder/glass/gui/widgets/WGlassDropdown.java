package aicoder.glass.gui.widgets;

import aicoder.glass.gui.Chrome;
import aicoder.glass.gui.GlassGuiTheme;
import aicoder.glass.gui.Fx;
import aicoder.glass.gui.Style;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.widgets.input.WDropdown;
import meteordevelopment.meteorclient.utils.render.color.Color;

public class WGlassDropdown<T> extends WDropdown<T> implements MeteorWidget {
    public WGlassDropdown(T[] values, T value) {
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
        GlassGuiTheme glass = (GlassGuiTheme) theme;
        Style s = glass.style;
        double pad = pad();
        double ts = theme.textHeight();
        boolean hot = (mouseOver && !theme.disableHoverColor) || expanded;

        Chrome.field(renderer, s, x, y, width, height, glass.unit(), hot, false);
        renderer.text(get().toString(), x + pad, y + pad, s.title, false);


        // Caret
        double cw = ts * 0.42;
        double cx = x + pad + maxValueWidth + pad + ts / 2;
        double cy = y + height / 2;
        Fx.tri(renderer, cx - cw / 2, cy - cw / 4, cx + cw / 2, cy - cw / 4, cx, cy + cw / 3, s.title);
    }

    private static class WRoot extends WDropdownRoot implements MeteorWidget {
        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            GlassGuiTheme glass = (GlassGuiTheme) theme;
            double t = Fx.hair(glass.unit());
            Color line = Fx.rgba(255, 255, 255, 0.35);

            renderer.quad(x, y, width, height, glass.style.solid);
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
            Style s = ((GlassGuiTheme) theme).style;
            boolean current = value.equals(WGlassDropdown.this.value);

            if (mouseOver || pressed) renderer.quad(x, y, width, height, Fx.a(s.colorA, 0.45), Fx.a(s.colorB, 0.45));
            renderer.text(value.toString(), x + pad(), y + pad(), mouseOver || current ? s.title : theme().textColor.get(), false);
        }
    }
}
