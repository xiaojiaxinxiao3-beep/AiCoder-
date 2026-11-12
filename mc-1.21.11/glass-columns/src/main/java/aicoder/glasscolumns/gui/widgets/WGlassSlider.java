package aicoder.glasscolumns.gui.widgets;

import aicoder.glasscolumns.gui.Fx;
import aicoder.glasscolumns.gui.GlassGuiTheme;
import aicoder.glasscolumns.gui.Style;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.widgets.input.WSlider;

/** A dark channel filled with the two glass colors, and a white knob that glows. */
public class WGlassSlider extends WSlider implements MeteorWidget {
    public WGlassSlider(double value, double min, double max) {
        super(value, min, max);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        GlassGuiTheme glass = (GlassGuiTheme) theme;
        Style s = glass.style;
        double u = glass.unit();

        // Same track the base class maps the mouse onto
        double inset = handleSize() / 2;
        double trackX = x + inset;
        double trackW = width - inset * 2;
        if (trackW <= 0) return;

        double fraction = max > min ? (value - min) / (max - min) : 0;
        double th = Math.max(3, Math.round(4 * u));
        double ty = Math.round(y + (height - th) / 2);
        double filled = trackW * fraction;
        double cy = y + height / 2;

        Fx.box(renderer, trackX - th / 2, ty, trackW + th, th, th / 2, Fx.rgba(6, 8, 28, 0.6));

        if (filled > 0) {
            Fx.glowRect(renderer, trackX, ty, filled, th, 5 * u, Fx.a(s.accent2, 0.3));
            Fx.box(renderer, trackX - th / 2, ty, filled + th, th, th / 2, s.colorA, s.colorB, s.colorB, s.colorA);
        }

        double knob = (dragging ? 7 : 6) * u;
        double kx = trackX + filled;
        Fx.glowDot(renderer, kx, cy, knob, 7 * u, s.accent, dragging || handleMouseOver ? 1 : 0.7);
        Fx.circle(renderer, kx, cy, knob, Fx.rgba(255, 255, 255, 1));
        Fx.circle(renderer, kx, cy + knob * 0.25, knob * 0.7, Fx.a(s.accent, 0.25));
    }
}
