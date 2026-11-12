package aicoder.glass.gui;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;

/**
 * The liquid glass look, built from layers: a tinted body, a bright rim that catches light at the top,
 * a soft sheen under the top edge, a shaded bottom, and (on windows) slow highlights drifting inside.
 */
public final class Chrome {
    private Chrome() {
    }

    private static final Color WHITE = new Color(255, 255, 255, 255);
    private static final Color BLACK = new Color(0, 0, 0, 255);

    /** Window body. {@code liquid} adds the drifting highlights. */
    public static void panel(GuiRenderer g, Style s, double x, double y, double w, double h, double u, boolean liquid) {
        double corner = Math.min(s.corner * u, Math.min(w, h) / 2);
        double t = Fx.hair(u);
        Fx.occlude(x, y, w, h);

        // Shadow underneath, then a halo in the glass colors
        Fx.glowRect(g, x + 10 * u, y + 14 * u, w - 20 * u, h - 20 * u, 30 * u, Fx.rgba(0, 0, 10, 0.28));
        if (s.tinted) {
            Color mid = Fx.mix(s.colorA, s.colorB, 0.5);
            Fx.boxOutline(g, x - 3 * u, y - 3 * u, w + 6 * u, h + 6 * u, corner + 3 * u, 3 * u, Fx.a(s.colorA, 0.16), Fx.a(s.colorB, 0.16));
            Fx.boxOutline(g, x - 1.5 * u, y - 1.5 * u, w + 3 * u, h + 3 * u, corner + 1.5 * u, 1.5 * u, Fx.a(mid, 0.3));
        }

        // Body: color A in the top-left running to color B in the bottom-right
        if (s.tinted) {
            Color a = Fx.a(s.colorA, s.opacity);
            Color b = Fx.a(s.colorB, s.opacity);
            Color m = Fx.a(Fx.mix(s.colorA, s.colorB, 0.5), s.opacity);
            Fx.box(g, x, y, w, h, corner, a, m, b, m);
        } else {
            // Clear glass: just enough milk and shade to read text over the blurred world
            Fx.box(g, x, y, w, h, corner, Fx.rgba(16, 22, 44, 0.3 * s.opacity + 0.12), Fx.rgba(8, 10, 28, 0.42 * s.opacity + 0.14));
            Fx.box(g, x, y, w, h, corner, Fx.rgba(255, 255, 255, 0.1), Fx.rgba(255, 255, 255, 0.03));
        }

        if (liquid) caustics(g, s, x, y, w, h, u, corner);

        // Sheen under the top edge and shade pooling at the bottom
        double in = 2 * u;
        Fx.box(g, x + in, y + in, w - in * 2, h * 0.34, corner - in, Fx.a(WHITE, 0.2), Fx.a(WHITE, 0));
        Fx.box(g, x + in, y + h * 0.62, w - in * 2, h * 0.38 - in, corner - in, Fx.a(BLACK, 0), Fx.a(BLACK, 0.2));

        // Rim: bright where the light lands, thin elsewhere, with a second line just inside for thickness
        Fx.boxOutline(g, x, y, w, h, corner, Math.max(1, Math.round(1.4 * u)), Fx.a(WHITE, 0.7), Fx.a(WHITE, 0.2));
        Fx.boxOutline(g, x + 2.5 * u, y + 2.5 * u, w - 5 * u, h - 5 * u, corner - 2.5 * u, t, Fx.a(WHITE, 0.14), Fx.a(WHITE, 0.03));
    }

    /** Slow blobs of light inside the glass, like liquid moving behind it. They loop every 14 seconds. */
    private static void caustics(GuiRenderer g, Style s, double x, double y, double w, double h, double u, double corner) {
        double p = Fx.phase(14);
        double margin = corner + 18 * u;

        for (int i = 0; i < 4; i++) {
            double seed = 0.17 + i * 0.23;
            double fx = 0.5 + 0.34 * Fx.flow(p, seed);
            double fy = 0.5 + 0.3 * Fx.flow(p, seed + 0.41);
            double cx = x + margin + (w - margin * 2) * Fx.clamp01(fx);
            double cy = y + margin + (h - margin * 2) * Fx.clamp01(fy);

            // Never larger than the distance to the nearest edge, so light cannot spill past the rounded rim
            double reach = Math.min(Math.min(cx - x, x + w - cx), Math.min(cy - y, y + h - cy)) - 2 * u;
            double radius = Math.min(reach, (90 + 40 * i) * u);
            if (radius <= 0) continue;

            Color c = i % 2 == 0 ? Fx.mix(s.colorA, WHITE, 0.55) : Fx.mix(s.colorB, WHITE, 0.45);
            Fx.halo(g, cx, cy, radius * 0.1, radius, new Color(c.r, c.g, c.b, 255), s.tinted ? 0.13 : 0.08);
        }
    }

    /**
     * A small pane of glass: category and module rows, tab capsules.
     *
     * @param lit 0..1, how strongly the pane is filled with the glass colors (selection)
     */
    public static void plate(GuiRenderer g, Style s, double x, double y, double w, double h, double u, double lit, double hover) {
        double corner = Math.min(s.plateCorner * u, h / 2);
        double t = Fx.hair(u);

        if (lit > 0) {
            Color glow = Fx.mix(s.colorA, s.colorB, 0.5);
            Fx.boxOutline(g, x - 2 * u, y - 2 * u, w + 4 * u, h + 4 * u, corner + 2 * u, 2 * u, Fx.a(glow, 0.22 * lit));
            Fx.box(g, x, y, w, h, corner, Fx.a(s.colorA, 0.5 * lit), Fx.a(s.colorB, 0.42 * lit), Fx.a(s.colorB, 0.5 * lit), Fx.a(s.colorA, 0.42 * lit));
        }

        Fx.box(g, x, y, w, h, corner, Fx.a(WHITE, 0.1 + 0.07 * hover), Fx.a(WHITE, 0.03 + 0.04 * hover));
        Fx.boxOutline(g, x, y, w, h, corner, t, Fx.a(WHITE, 0.34 + 0.3 * lit), Fx.a(WHITE, 0.07 + 0.12 * lit));
    }

    /** Dark inset: value box, button or dropdown body. */
    public static void field(GuiRenderer g, Style s, double x, double y, double w, double h, double u, boolean hot, boolean pressed) {
        double corner = Math.min(s.fieldCorner * u, h / 2);
        Color fill = pressed ? Fx.mix(s.field, s.accent, 0.3) : s.field;

        Fx.box(g, x, y, w, h, corner, fill);
        // Lit from above, so the inset's lower edge is the bright one
        Fx.boxOutline(g, x, y, w, h, corner, Fx.hair(u),
            hot || pressed ? Fx.a(s.accent, 0.9) : Fx.a(BLACK, 0.3),
            hot || pressed ? Fx.a(s.accent, 0.9) : Fx.a(WHITE, 0.3));
    }

    /** The round switch: an empty well when off, a lit droplet when on. */
    public static void droplet(GuiRenderer g, Style s, double cx, double cy, double radius, double on, double hover, double seed) {
        double t = Math.max(1, radius * 0.1);

        Fx.circle(g, cx, cy, radius, Fx.rgba(8, 12, 34, 0.5));
        Fx.ring(g, cx, cy, radius - t / 2, t, Fx.a(WHITE, 0.2 + 0.25 * hover));

        if (on <= 0) return;

        double pulse = 0.6 + 0.4 * Fx.flow(Fx.phase(4), 0.3 + seed);
        Fx.glowDot(g, cx, cy, radius, radius * (0.5 + 0.4 * pulse), s.accent, on);
        Fx.circle(g, cx, cy, radius * 0.92 * on, Fx.a(s.colorB, on));
        Fx.circle(g, cx - radius * 0.08, cy - radius * 0.1, radius * 0.74 * on, Fx.a(Fx.mix(s.colorA, s.colorB, 0.4), on));
        Fx.circle(g, cx - radius * 0.16, cy - radius * 0.2, radius * 0.5 * on, Fx.a(s.accent, 0.85 * on));
        Fx.ellipse(g, cx - radius * 0.3, cy - radius * 0.38, radius * 0.26, radius * 0.15, -0.5, Fx.a(WHITE, 0.9 * on));
    }
}
