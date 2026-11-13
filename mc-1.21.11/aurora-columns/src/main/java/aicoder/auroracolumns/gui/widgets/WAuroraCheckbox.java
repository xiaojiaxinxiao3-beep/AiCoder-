package aicoder.auroracolumns.gui.widgets;

import aicoder.auroracolumns.gui.AuroraGuiTheme;
import aicoder.auroracolumns.gui.Chrome;
import aicoder.auroracolumns.gui.Fx;
import aicoder.auroracolumns.gui.Style;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.utils.render.color.Color;

/** Rounded well that fills with green-to-teal light and a dark tick. Keeps Meteor's footprint so setting rows still align. */
public class WAuroraCheckbox extends WCheckbox implements MeteorWidget {
    private double animProgress;

    public WAuroraCheckbox(boolean checked) {
        super(checked);
        animProgress = checked ? 1 : 0;
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        AuroraGuiTheme aurora = (AuroraGuiTheme) theme;
        Style s = aurora.style;
        double u = aurora.unit();

        animProgress = Fx.step(animProgress, checked, delta, 10);

        double size = Math.round(Math.min(24 * u, Math.min(width, height) - 2));
        double bx = Math.round(x + (width - size) / 2);
        double by = Math.round(y + (height - size) / 2);
        double corner = size / 3;

        Fx.box(renderer, bx, by, size, size, corner, s.field);
        Fx.boxOutline(renderer, bx, by, size, size, corner, Fx.hair(u), Fx.rgba(255, 255, 255, mouseOver ? 0.45 : 0.2));

        if (animProgress > 0) {
            double a = animProgress;
            Fx.glowRect(renderer, bx + corner, by + corner, size - corner * 2, size - corner * 2, corner + 6 * u, Fx.a(s.green, 0.32 * a));
            Fx.box(renderer, bx, by, size, size, corner, Fx.a(s.green, a), Fx.a(Fx.mix(s.green, s.teal, 0.5), a), Fx.a(s.teal, a), Fx.a(Fx.mix(s.green, s.teal, 0.5), a));
            renderer.quad(bx + corner, by + Fx.hair(u), size - corner * 2, Fx.hair(u), Fx.rgba(255, 255, 255, 0.7 * a));

            Color mark = Fx.a(s.dark, a);
            double cx = bx + size / 2, cy = by + size / 2, k = size * 0.5, lt = Math.max(1.5, size * 0.1);
            Fx.line(renderer, cx - k * 0.46, cy + k * 0.02, cx - k * 0.12, cy + k * 0.36, lt, mark);
            Fx.line(renderer, cx - k * 0.12, cy + k * 0.36, cx + k * 0.48, cy - k * 0.32, lt, mark);
        }
    }
}
