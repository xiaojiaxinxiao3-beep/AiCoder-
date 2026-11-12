package aicoder.earth.gui.widgets;

import aicoder.earth.gui.EarthGuiTheme;
import aicoder.earth.gui.Fx;
import aicoder.earth.gui.Style;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.widgets.input.WSlider;
import meteordevelopment.meteorclient.utils.render.color.Color;

/** Either a row of cells or a thin gradient line with a round knob, depending on the theme. */
public class WEarthSlider extends WSlider implements MeteorWidget {
    private static final int SEGMENTS = 22;

    public WEarthSlider(double value, double min, double max) {
        super(value, min, max);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        EarthGuiTheme earth = (EarthGuiTheme) theme;
        Style s = earth.style;
        double u = earth.unit();

        // Same track the base class maps the mouse onto
        double inset = handleSize() / 2;
        double trackX = x + inset;
        double trackW = width - inset * 2;
        if (trackW <= 0) return;

        double fraction = max > min ? (value - min) / (max - min) : 0;
        Color from = s.slideFrom;
        Color to = theme().sliderLeft.get();
        Color empty = theme().sliderRight.get();

        if (s.segmented) {
            double gap = Math.max(1, Math.round(2 * u));
            int n = (int) Math.max(4, Math.min(SEGMENTS, Math.floor((trackW + gap) / (gap + 2 * u))));
            double cell = (trackW + gap) / n - gap;
            double h = Math.round(9 * u);
            double cy = Math.round(y + (height - h) / 2);
            int lit = (int) Math.max(1, Math.round(fraction * n));

            for (int i = 0; i < n; i++) {
                double cx = trackX + i * (cell + gap);
                if (i < lit) {
                    if (i == lit - 1 && !s.light) Fx.glowRect(renderer, cx, cy, cell, h, 4 * u, Fx.a(to, 0.35));
                    renderer.quad(cx, cy, cell, h, to);
                } else {
                    renderer.quad(cx, cy, cell, h, empty);
                }
            }
            return;
        }

        double th = Math.max(2, Math.round(3 * u));
        double ty = Math.round(y + (height - th) / 2);
        double filled = trackW * fraction;
        double cy = y + height / 2;

        Fx.box(renderer, trackX, ty, trackW, th, th / 2, empty);
        Fx.boxOutline(renderer, trackX, ty - 1, trackW, th + 2, th / 2, 1, Fx.a(s.fieldLine, 0.6));
        if (filled > 0) renderer.quad(trackX, ty, filled, th, from, to);

        double knob = (dragging ? 7 : 6) * u;
        double kx = trackX + filled;
        if (!s.light) Fx.glowDot(renderer, kx, cy, knob, 5 * u, to, 0.8);
        else Fx.circle(renderer, kx, cy + u, knob + u, Fx.rgba(0, 0, 0, 0.14));
        Fx.circle(renderer, kx, cy, knob, theme().sliderHandle.get(dragging, handleMouseOver));
    }
}
