package aicoder.assist.aurora;

import aicoder.assist.gfx.GuiRenderer;
import aicoder.assist.gfx.Color;

/**
 * The liquid glass surface shared by every pane: the aurora seen through it, a white sheen that is clearest
 * in the middle, a highlight under the top edge and a rim whose gradient slowly travels around the pane.
 */
public final class Chrome {
    private Chrome() {
    }

    private static final Color WHITE = new Color(255, 255, 255, 255);

    // Rim gradient: position, r, g, b, alpha
    private static final double[][] RIM_STOPS = {
        {0.00, 255, 255, 255, 0.85},
        {0.22, 255, 255, 255, 0.14},
        {0.48, 77, 255, 184, 0.42},
        {0.72, 143, 123, 255, 0.42},
        {1.00, 255, 255, 255, 0.55},
    };

    private static double rimX, rimY, rimDx, rimDy, rimLen;

    private static final Fx.Paint RIM = (px, py) -> {
        double t = Fx.clamp01(((px - rimX) * rimDx + (py - rimY) * rimDy) / rimLen + 0.5);
        int k = 1;
        while (k < RIM_STOPS.length - 1 && t > RIM_STOPS[k][0]) k++;
        double[] a = RIM_STOPS[k - 1], b = RIM_STOPS[k];
        double f = (t - a[0]) / (b[0] - a[0]);
        return Fx.rgba(
            (int) Math.round(a[1] + (b[1] - a[1]) * f),
            (int) Math.round(a[2] + (b[2] - a[2]) * f),
            (int) Math.round(a[3] + (b[3] - a[3]) * f),
            a[4] + (b[4] - a[4]) * f
        );
    };

    /** Points the rim gradient across the given box; the direction turns once every 12 seconds. */
    private static void aimRim(double x, double y, double w, double h) {
        double angle = Math.toRadians(140 + 360 * Fx.phase(12));
        rimDx = Math.sin(angle);
        rimDy = -Math.cos(angle);
        rimX = x + w / 2;
        rimY = y + h / 2;
        rimLen = Math.max(1, Math.abs(w * rimDx) + Math.abs(h * rimDy));
    }

    // Left-to-right aurora gradient: green, teal at 55%, then a third color

    private static double gradX, gradW, gradA1, gradA2, gradA3;
    private static int gradR = 143, gradG = 123, gradB = 255;

    private static final Fx.Paint GRADIENT = (px, py) -> {
        double t = Fx.clamp01((px - gradX) / gradW);
        if (t < 0.55) {
            double f = t / 0.55;
            return Fx.rgba((int) Math.round(77 - 30 * f), (int) Math.round(255 - 25 * f), (int) Math.round(184 + 40 * f), gradA1 + (gradA2 - gradA1) * f);
        }
        double f = (t - 0.55) / 0.45;
        return Fx.rgba((int) Math.round(47 + (gradR - 47) * f), (int) Math.round(230 + (gradG - 230) * f), (int) Math.round(224 + (gradB - 224) * f), gradA2 + (gradA3 - gradA2) * f);
    };

    /** Rounded box filled with the aurora gradient at the three given opacities. */
    public static void gradient(GuiRenderer g, double x, double y, double w, double h, double corner, double a1, double a2, double a3) {
        gradient(g, x, y, w, h, corner, a1, a2, a3, 143, 123, 255);
    }

    public static void gradient(GuiRenderer g, double x, double y, double w, double h, double corner, double a1, double a2, double a3, int endR, int endG, int endB) {
        if (w <= 0 || h <= 0) return;
        gradX = x;
        gradW = w;
        gradA1 = a1;
        gradA2 = a2;
        gradA3 = a3;
        gradR = endR;
        gradG = endG;
        gradB = endB;
        Fx.boxPaint(g, x, y, w, h, corner, Math.max(4, w * 0.55), GRADIENT);
    }

    /** A pane of glass. {@code solid} gives the opaque body used by tooltips and popups. */
    public static void panel(GuiRenderer g, double x, double y, double w, double h, double u, double corner, boolean solid) {
        if (w <= 0 || h <= 0) return;
        corner = Math.min(corner, Math.min(w, h) / 2);
        Fx.occlude(x, y, w, h);

        // Shadow: a dark pool under the pane, reaching out below it
        double sx = x + 16 * u, sy = y + 30 * u, sw = w - 32 * u, sh = h - 32 * u;
        if (sw > 0 && sh > 0) {
            Color shade = Fx.rgba(0, 6, 20, solid ? 0.4 : 0.3);
            g.quad(sx, sy, sw, sh, shade);
            Fx.glowRect(g, sx, sy, sw, sh, 44 * u, shade);
        }

        if (solid) {
            Fx.box(g, x, y, w, h, corner, Fx.rgba(20, 44, 78, 0.95), Fx.rgba(8, 18, 42, 0.97));
        } else {
            Fx.boxPaint(g, x, y, w, h, corner, 60 * u, Aurora.GLASS);
            Fx.box(g, x, y, w, h, corner, Fx.a(WHITE, 0.16), Fx.a(WHITE, 0.05), Fx.a(WHITE, 0.10), Fx.a(WHITE, 0.035));
        }

        // Highlight pooling under the top edge, strongest at the left
        Fx.box(g, x + u, y + u, w - 2 * u, Math.min(h * 0.5, 240 * u), corner - u, Fx.a(WHITE, 0.15), Fx.a(WHITE, 0.05), Fx.a(WHITE, 0), Fx.a(WHITE, 0));

        double rim = Math.max(1, Math.round(1.4 * u));
        double line = Math.max(1, Math.round(1.6 * u));
        double in = corner * 0.8;
        if (w - in * 2 > 0) {
            g.quad(x + in, y + rim, (w - in * 2) / 2, line, Fx.a(WHITE, 0.04), Fx.a(WHITE, 0.28));
            g.quad(x + w / 2, y + rim, (w - in * 2) / 2, line, Fx.a(WHITE, 0.28), Fx.a(WHITE, 0.04));
        }

        Fx.boxOutline(g, x + rim, y + rim, w - rim * 2, h - rim * 2, corner - rim, Fx.hair(u), Fx.a(WHITE, 0.10), Fx.a(WHITE, 0.06));
        aimRim(x, y, w, h);
        Fx.outlinePaint(g, x, y, w, h, corner, rim, 40 * u, RIM);
    }

    /** Resting row or button: barely-there glass that brightens under the pointer. */
    public static void plate(GuiRenderer g, double x, double y, double w, double h, double u, double corner, double hover) {
        corner = Math.min(corner, h / 2);
        Fx.box(g, x, y, w, h, corner, Fx.a(WHITE, 0.045 + 0.065 * hover));
        Fx.boxOutline(g, x, y, w, h, corner, Fx.hair(u), Fx.a(WHITE, 0.10 + 0.30 * hover));
    }

    /**
     * Row lit by the aurora: teal light under it, the gradient at the given opacities and a border.
     *
     * @param k overall strength 0..1, for fading in and breathing
     */
    public static void lit(GuiRenderer g, double x, double y, double w, double h, double u, double corner, double a1, double a2, double a3, Color border, double glow, double k) {
        if (k <= 0) return;
        corner = Math.min(corner, h / 2);

        if (glow > 0) {
            double in = Math.min(corner, h / 2 - 1);
            Fx.glowRect(g, x + in, y + in + 3 * u, w - in * 2, h - in * 2, in + 10 * u, Fx.rgba(47, 230, 224, glow * k));
        }
        gradient(g, x, y, w, h, corner, a1 * k, a2 * k, a3 * k);
        Fx.boxOutline(g, x, y, w, h, corner, Fx.hair(u), Fx.a(border, k));
    }

    /** Dark inset: value box, button, dropdown or slider track. */
    public static void field(GuiRenderer g, Style s, double x, double y, double w, double h, double u, boolean hot, boolean pressed) {
        double corner = Math.min(s.fieldCorner * u, h / 2);

        Fx.box(g, x, y, w, h, corner, pressed ? Fx.rgba(20, 70, 80, 0.5) : s.field);
        if (hot || pressed) Fx.boxOutline(g, x, y, w, h, corner, Fx.hair(u), Fx.a(s.green, pressed ? 0.9 : 0.6));
        else Fx.boxOutline(g, x, y, w, h, corner, Fx.hair(u), Fx.rgba(0, 0, 0, 0.3), Fx.a(WHITE, 0.24));
    }

    /** Capsule switch: a dim groove with the knob at the left when off, the aurora gradient with the knob at the right when on. */
    public static void toggle(GuiRenderer g, double x, double y, double w, double h, double u, double on, double hover, double pulse) {
        double r = h / 2;
        double cy = y + r;

        if (on < 1) {
            Fx.box(g, x, y, w, h, r, Fx.rgba(0, 0, 0, 0.22 * (1 - on)), Fx.a(WHITE, 0.12 * (1 - on)));
            Fx.boxOutline(g, x, y, w, h, r, Fx.hair(u), Fx.a(WHITE, (0.22 + 0.2 * hover) * (1 - on)));
        }
        if (on > 0) {
            Fx.glowRect(g, x + r, cy, w - r * 2, 0, r + 8 * u, Fx.rgba(77, 255, 184, 0.34 * on * pulse));
            gradient(g, x, y, w, h, r, 0.9 * on, 0.9 * on, 0.9 * on);
            Fx.boxOutline(g, x, y, w, h, r, Fx.hair(u), Fx.a(WHITE, 0.5 * on));
        }

        double kr = r - 3 * u;
        double kx = x + 3 * u + kr + (w - 6 * u - kr * 2) * on;
        Fx.circle(g, kx, cy + 1.5 * u, kr + u, Fx.rgba(0, 0, 0, 0.28));
        Fx.circle(g, kx, cy, kr, Fx.rgba(190, 204, 222, 1));
        Fx.circle(g, kx - kr * 0.08, cy - kr * 0.1, kr * 0.86, Fx.rgba(255, 255, 255, 1));
    }

    /** Small glowing bead: the status dot of a category or of a switched-on module. */
    public static void bead(GuiRenderer g, Style s, double cx, double cy, double radius, double on, double pulse) {
        if (on < 1) Fx.circle(g, cx, cy, radius, Fx.a(WHITE, 0.28 * (1 - on)));
        if (on > 0) {
            Fx.glowDot(g, cx, cy, radius, radius * 1.6 * pulse, s.green, on);
            Fx.circle(g, cx, cy, radius, Fx.a(s.green, on));
        }
    }
}
