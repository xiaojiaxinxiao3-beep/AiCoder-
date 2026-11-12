package aicoder.client.gui.widgets;

import aicoder.client.gui.AiCoderGuiTheme;
import aicoder.client.gui.Cyber;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.widgets.input.WSlider;
import meteordevelopment.meteorclient.utils.render.color.Color;

/** Segmented bar: filled cells run cyan to magenta, the leading cell is white with a magenta halo. */
public class WCyberSlider extends WSlider implements MeteorWidget {
    private static final int SEGMENTS = 22;

    public WCyberSlider(double value, double min, double max) {
        super(value, min, max);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        MeteorGuiTheme theme = theme();
        double u = ((AiCoderGuiTheme) theme).unit();

        // Same track the base class maps the mouse onto
        double inset = handleSize() / 2;
        double trackX = x + inset;
        double trackW = width - inset * 2;
        if (trackW <= 0) return;

        double gap = Math.max(1, Math.round(2 * u));
        int n = (int) Math.max(4, Math.min(SEGMENTS, Math.floor((trackW + gap) / (gap + 2 * u))));
        double cell = (trackW + gap) / n - gap;
        double h = Math.round(9 * u);
        double cy = Math.round(y + (height - h) / 2);

        double fraction = max > min ? (value - min) / (max - min) : 0;
        int lit = (int) Math.max(1, Math.round(fraction * n));

        Color top = theme.sliderLeft.get();
        Color bottom = theme.accentColor.get();
        Color empty = theme.sliderRight.get();
        Color head = theme.sliderHandle.get(dragging, handleMouseOver);

        for (int i = 0; i < n; i++) {
            double cx = trackX + i * (cell + gap);

            if (i < lit - 1) {
                Cyber.glowRect(renderer, cx, cy, cell, h, 3 * u, Cyber.a(bottom, 0.16));
                renderer.quad(cx, cy, cell, h, top, top, bottom, bottom);
            } else if (i == lit - 1) {
                Cyber.glowRect(renderer, cx, cy, cell, h, (dragging ? 7 : 5) * u, Cyber.a(bottom, 0.55));
                renderer.quad(cx, cy, cell, h, head);
            } else {
                renderer.quad(cx, cy, cell, h, empty);
            }
        }
    }
}
