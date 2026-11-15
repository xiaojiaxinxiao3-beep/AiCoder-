package aicoder.assist.aurora;

import aicoder.assist.Client;
import aicoder.assist.gfx.Color;
import aicoder.assist.gfx.GuiRenderer;
import aicoder.assist.gfx.Wallpaper;

/** Sends each drawing call to the aurora Chrome or to Glass, whichever STYLE has selected. */
final class Ui {
    private Ui() {
    }

    private static final Style AURORA = new Style();

    static boolean glass() {
        return Client.look.glass();
    }

    /** Once per frame: the backdrop, and the palette the screen's own text and lines use. */
    static void frame(GuiRenderer g, Style s, double width, double height) {
        if (glass()) {
            Glass.update();
            Wallpaper.frame(width, height, Client.look.imageOpacity.get());
            copy(s.green, Glass.ACCENT); copy(s.teal, Fx.mix(Glass.A, Glass.ACCENT, 0.5)); copy(s.violet, Glass.ACCENT2); copy(s.pink, Glass.B);
            copy(s.mint, Fx.mix(Glass.ACCENT, new Color(255, 255, 255, 255), 0.5));
            s.dark.set(255, 255, 255, 255);
            s.title.set(246, 249, 255, 255); s.text.set(228, 235, 255, 255); s.dim.set(226, 234, 255, 232); s.faint.set(206, 218, 248, 168);
            copy(s.field, Glass.FIELD); copy(s.solid, Glass.SOLID);
            g.scissorStart(0, 0, width, height);
            Glass.backdrop(g, width, height);
            g.scissorEnd();
        } else {
            copy(s.green, AURORA.green); copy(s.teal, AURORA.teal); copy(s.violet, AURORA.violet); copy(s.pink, AURORA.pink); copy(s.mint, AURORA.mint);
            copy(s.dark, AURORA.dark); copy(s.title, AURORA.title); copy(s.text, AURORA.text); copy(s.dim, AURORA.dim); copy(s.faint, AURORA.faint);
            copy(s.field, AURORA.field); copy(s.solid, AURORA.solid);
            Aurora.frame(width, height, 0.88, true);
            g.scissorStart(0, 0, width, height);
            Aurora.render(g);
            g.scissorEnd();
        }
    }

    private static void copy(Color to, Color from) {
        to.set(from.r, from.g, from.b, from.a);
    }

    static void panel(GuiRenderer g, double x, double y, double w, double h, double u, double corner, boolean solid) {
        if (glass()) Glass.panel(g, x, y, w, h, u, corner, solid); else Chrome.panel(g, x, y, w, h, u, corner, solid);
    }

    static void plate(GuiRenderer g, double x, double y, double w, double h, double u, double corner, double hover) {
        if (glass()) Glass.plate(g, x, y, w, h, u, corner, hover); else Chrome.plate(g, x, y, w, h, u, corner, hover);
    }

    static void lit(GuiRenderer g, double x, double y, double w, double h, double u, double corner, double a1, double a2, double a3, Color border, double glow, double k) {
        if (glass()) Glass.lit(g, x, y, w, h, u, corner, a1, a2, a3, border, glow, k); else Chrome.lit(g, x, y, w, h, u, corner, a1, a2, a3, border, glow, k);
    }

    static void gradient(GuiRenderer g, double x, double y, double w, double h, double corner, double a1, double a2, double a3) {
        if (glass()) Glass.gradient(g, x, y, w, h, corner, a1, a2, a3); else Chrome.gradient(g, x, y, w, h, corner, a1, a2, a3);
    }

    static void gradient(GuiRenderer g, double x, double y, double w, double h, double corner, double a1, double a2, double a3, int endR, int endG, int endB) {
        if (glass()) Glass.gradient(g, x, y, w, h, corner, a1, a2, a3); else Chrome.gradient(g, x, y, w, h, corner, a1, a2, a3, endR, endG, endB);
    }

    static void field(GuiRenderer g, Style s, double x, double y, double w, double h, double u, boolean hot, boolean pressed) {
        if (glass()) Glass.field(g, x, y, w, h, u, s.fieldCorner * u, hot, pressed); else Chrome.field(g, s, x, y, w, h, u, hot, pressed);
    }

    static void toggle(GuiRenderer g, double x, double y, double w, double h, double u, double on, double hover, double pulse) {
        if (glass()) Glass.toggle(g, x, y, w, h, u, on, hover, pulse); else Chrome.toggle(g, x, y, w, h, u, on, hover, pulse);
    }

    static void bead(GuiRenderer g, Style s, double cx, double cy, double radius, double on, double pulse) {
        if (glass()) Glass.bead(g, cx, cy, radius, on, pulse); else Chrome.bead(g, s, cx, cy, radius, on, pulse);
    }
}
