package aicoder.glasscolumns.gui.widgets;

import aicoder.glasscolumns.gui.Chrome;
import aicoder.glasscolumns.gui.Fx;
import aicoder.glasscolumns.gui.GlassGuiTheme;
import aicoder.glasscolumns.gui.Style;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.util.math.MathHelper;

/** Rounded well that fills with the glass colors and a white tick. Keeps Meteor's footprint so setting rows still align. */
public class WGlassCheckbox extends WCheckbox implements MeteorWidget {
    private double animProgress;

    public WGlassCheckbox(boolean checked) {
        super(checked);
        animProgress = checked ? 1 : 0;
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        GlassGuiTheme glass = (GlassGuiTheme) theme;
        Style s = glass.style;
        double u = glass.unit();

        animProgress = MathHelper.clamp(animProgress + (checked ? 1 : -1) * delta * 12, 0, 1);

        double size = Math.round(Math.min(16 * u, Math.min(width, height) - 2));
        double bx = Math.round(x + (width - size) / 2);
        double by = Math.round(y + (height - size) / 2);
        double corner = size * 0.28;

        Chrome.field(renderer, s, bx, by, size, size, u, mouseOver, pressed);

        if (animProgress > 0) {
            double a = animProgress;
            Fx.glowRect(renderer, bx + corner, by + corner, size - corner * 2, size - corner * 2, corner + 4 * u, Fx.a(s.accent2, 0.3 * a));
            Fx.box(renderer, bx, by, size, size, corner, Fx.a(s.colorA, a), Fx.a(s.colorB, a), Fx.a(s.colorB, a), Fx.a(s.colorA, a));
            Fx.box(renderer, bx + u, by + u, size - 2 * u, size * 0.5, corner - u, Fx.rgba(255, 255, 255, 0.3 * a), Fx.rgba(255, 255, 255, 0));

            Color mark = Fx.rgba(255, 255, 255, a);
            double cx = bx + size / 2, cy = by + size / 2, k = size * 0.5, lt = Math.max(1.5, size * 0.13);
            Fx.line(renderer, cx - k * 0.5, cy + k * 0.02, cx - k * 0.12, cy + k * 0.4, lt, mark);
            Fx.line(renderer, cx - k * 0.12, cy + k * 0.4, cx + k * 0.54, cy - k * 0.36, lt, mark);
        }
    }
}
