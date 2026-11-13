package aicoder.auroracolumns.gui.widgets;

import aicoder.auroracolumns.gui.AuroraGuiTheme;
import aicoder.auroracolumns.gui.Chrome;
import aicoder.auroracolumns.gui.Fx;
import aicoder.auroracolumns.gui.Style;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.widgets.input.WSlider;

/** A dark groove filled with the aurora gradient, and a white bead for a handle. */
public class WAuroraSlider extends WSlider implements MeteorWidget {
    public WAuroraSlider(double value, double min, double max) {
        super(value, min, max);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        AuroraGuiTheme aurora = (AuroraGuiTheme) theme;
        Style s = aurora.style;
        double u = aurora.unit();

        // Same track the base class maps the mouse onto
        double inset = handleSize() / 2;
        double trackX = x + inset;
        double trackW = width - inset * 2;
        if (trackW <= 0) return;

        double fraction = max > min ? Fx.clamp01((value - min) / (max - min)) : 0;
        double th = Math.max(4, Math.round(8 * u));
        double ty = Math.round(y + (height - th) / 2);
        double filled = trackW * fraction;
        double cy = ty + th / 2;

        Fx.box(renderer, trackX - th / 2, ty, trackW + th, th, th / 2, Fx.rgba(3, 10, 26, 0.5));
        Fx.boxOutline(renderer, trackX - th / 2, ty, trackW + th, th, th / 2, Fx.hair(u), Fx.rgba(255, 255, 255, 0.14));

        if (filled > 0) {
            Fx.glowRect(renderer, trackX, cy, filled, 0, th / 2 + 7 * u, Fx.a(s.green, 0.3));
            Chrome.gradient(renderer, trackX - th / 2, ty, filled + th, th, th / 2, 1, 1, 1);
        }

        boolean hot = dragging || handleMouseOver;
        double knob = (hot ? 11 : 10) * u;
        double kx = trackX + filled;
        Fx.circle(renderer, kx, cy, knob + 4 * u, Fx.rgba(255, 255, 255, hot ? 0.2 : 0.12));
        Fx.circle(renderer, kx, cy + 2 * u, knob, Fx.rgba(0, 0, 0, 0.3));
        Fx.circle(renderer, kx, cy, knob, Fx.rgba(147, 169, 194, 1));
        Fx.circle(renderer, kx - knob * 0.06, cy - knob * 0.08, knob * 0.88, Fx.rgba(207, 226, 242, 1));
        Fx.circle(renderer, kx - knob * 0.2, cy - knob * 0.24, knob * 0.52, Fx.rgba(255, 255, 255, 1));
    }
}
