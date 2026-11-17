package aicoder.firstcolumns.gui.widgets;

import aicoder.firstcolumns.gui.AiCoderGuiTheme;
import aicoder.firstcolumns.gui.render.Palette;
import aicoder.firstcolumns.gui.render.Sdf;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.widgets.input.WSlider;

/** Slider with a dark rounded track, a green to blue fill and a glowing white round handle. */
public class WAiCoderSlider extends WSlider implements MeteorWidget {
    public WAiCoderSlider(double value, double min, double max) {
        super(value, min, max);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        double u = AiCoderGuiTheme.of(theme).u(1);

        double handle = handleSize();
        double valueWidth = valueWidth();

        double trackH = 7 * u;
        double trackX = x + handle / 2;
        double trackW = width - handle;
        double trackY = Math.round(y + height / 2 - trackH / 2);
        double radius = trackH / 2;

        // Track
        Sdf.box(trackX - radius, trackY, trackW + radius * 2, trackH, radius)
            .fill(Palette.SLIDER_TRACK)
            .stroke(Math.max(1, u * 0.8), Palette.SLIDER_BORDER)
            .draw();

        // Fill, the gradient ends where the value is on the green to blue ramp of the whole track
        if (valueWidth > 0) {
            double t = trackW > 0 ? valueWidth / trackW : 0;

            Sdf.box(trackX - radius, trackY, valueWidth + radius * 2, trackH, radius)
                .gradient(Palette.ACCENT_1, Palette.mix(Palette.ACCENT_1, Palette.ACCENT_2, t), 0)
                .glow(14 * u, Palette.withAlpha(Palette.ACCENT_2, 0.6))
                .draw();
        }

        // Handle
        double cx = trackX + valueWidth;
        double cy = trackY + trackH / 2;
        double knob = (dragging ? 8 : handleMouseOver ? 7.6 : 7) * u;

        Sdf.circle(cx, cy, knob).glow(22 * u, Palette.withAlpha(Palette.ACCENT_2, 0.7)).draw();
        Sdf.circle(cx, cy, knob).fill(Palette.WHITE).glow(12 * u, Palette.withAlpha(Palette.ACCENT_MID, 0.9)).draw();
    }
}
