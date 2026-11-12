package aicoder.earth.gui;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;

/** Style-aware pieces that several widgets share: the window panel and the small input field. */
public final class Chrome {
    private Chrome() {
    }

    /** Window body: shadow, gradient fill in the style's corner shape, hairline outline. */
    public static void panel(GuiRenderer g, Style s, double x, double y, double w, double h, double u) {
        double corner = s.corner * u;
        Fx.occlude(x, y, w, h);

        if (s.paperShadow) {
            double d = 6 * u;
            Fx.box(g, x + d, y + d, w, h, corner, Fx.rgba(60, 40, 30, 0.22));
        } else {
            Fx.glowRect(g, x + 8 * u, y + 10 * u, w - 16 * u, h - 14 * u, 26 * u, Fx.rgba(0, 0, 0, s.light ? 0.12 : 0.3));
        }

        Fx.box(g, x, y, w, h, corner, s.windowTop, s.windowBottom);
        Fx.boxOutline(g, x, y, w, h, corner, Fx.hair(u), s.outline);
    }

    /** Value box, button or dropdown body. */
    public static void field(GuiRenderer g, Style s, double x, double y, double w, double h, double u, boolean hot, boolean pressed) {
        double corner = Math.min(Math.abs(s.fieldCorner) * u, h / 2) * Math.signum(s.fieldCorner);
        Color fill = pressed ? Fx.mix(s.field, s.accent, 0.3) : s.field;
        Color line = pressed ? s.accent : (hot ? Fx.a(s.accent, 0.85) : s.fieldLine);

        Fx.box(g, x, y, w, h, corner, fill);
        Fx.boxOutline(g, x, y, w, h, corner, Fx.hair(u), line);
    }

    /** Opaque version of the window color, for popups that must stay readable over anything. */
    public static Color solid(Style s) {
        return Fx.rgba(s.windowBottom.r, s.windowBottom.g, s.windowBottom.b, 0.97);
    }
}
