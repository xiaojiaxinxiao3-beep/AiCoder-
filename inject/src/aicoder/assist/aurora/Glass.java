package aicoder.assist.aurora;

import aicoder.assist.Client;
import aicoder.assist.gfx.Color;
import aicoder.assist.gfx.GuiRenderer;
import aicoder.assist.gfx.Wallpaper;

/**
 * The liquid glass look, with the same entry points as the aurora Chrome so the screen can draw
 * with either. Layers: tinted body over the background image, a sheen under the top edge, shade at
 * the bottom, a bright rim, and slow highlights drifting inside the large panes.
 */
final class Glass {
    private Glass() {
    }

    private static final Color WHITE = new Color(255, 255, 255, 255);
    private static final Color BLACK = new Color(0, 0, 0, 255);

    static final Color A = new Color(), B = new Color(), ACCENT = new Color(), ACCENT2 = new Color(), SOLID = new Color();
    static final Color FIELD = new Color(8, 12, 34, 120);
    static boolean tinted;
    static double opacity;

    /** Recomputes the palette from the STYLE settings, so edits show at once. */
    static void update() {
        Look look = Client.look;
        tinted = look.tinted.get();
        opacity = look.opacity.get();
        if (tinted) {
            A.set(look.r1.get(), look.g1.get(), look.b1.get(), 255);
            B.set(look.r2.get(), look.g2.get(), look.b2.get(), 255);
        } else {
            A.set(214, 228, 255, 255);
            B.set(190, 204, 240, 255);
        }
        blend(ACCENT, A, WHITE, tinted ? 0.52 : 0.6);
        blend(ACCENT2, B, WHITE, tinted ? 0.4 : 0.5);
        if (tinted) blend(SOLID, B, new Color(10, 14, 36, 255), 0.62);
        else SOLID.set(26, 32, 54, 255);
        SOLID.a = 246;
    }

    private static void blend(Color out, Color c1, Color c2, double t) {
        out.set((int) Math.round(c1.r + (c2.r - c1.r) * t), (int) Math.round(c1.g + (c2.g - c1.g) * t), (int) Math.round(c1.b + (c2.b - c1.b) * t), 255);
    }

    static void backdrop(GuiRenderer g, double width, double height) {
        g.quad(0, 0, width, height, Fx.rgba(4, 6, 18, Client.look.dim.get()));
    }

    static void panel(GuiRenderer g, double x, double y, double w, double h, double u, double corner, boolean solid) {
        if (w <= 0 || h <= 0) return;
        corner = Math.min(corner, Math.min(w, h) / 2);
        double t = Fx.hair(u);
        Fx.occlude(x, y, w, h);

        // Shadow underneath, then a halo in the glass colors
        Fx.glowRect(g, x + 10 * u, y + 14 * u, w - 20 * u, h - 20 * u, 30 * u, Fx.rgba(0, 0, 10, 0.28));
        if (tinted) {
            Fx.boxOutline(g, x - 3 * u, y - 3 * u, w + 6 * u, h + 6 * u, corner + 3 * u, 3 * u, Fx.a(A, 0.16), Fx.a(B, 0.16));
            Fx.boxOutline(g, x - 1.5 * u, y - 1.5 * u, w + 3 * u, h + 3 * u, corner + 1.5 * u, 1.5 * u, Fx.a(Fx.mix(A, B, 0.5), 0.3));
        }

        if (solid) {
            Fx.box(g, x, y, w, h, corner, SOLID);
        } else {
            // The image first, so everything after it tints and frames it
            Wallpaper.renderIn(g, x, y, w, h, corner);
            if (tinted) {
                Color m = Fx.a(Fx.mix(A, B, 0.5), opacity);
                Fx.box(g, x, y, w, h, corner, Fx.a(A, opacity), m, Fx.a(B, opacity), m);
            } else {
                Fx.box(g, x, y, w, h, corner, Fx.rgba(16, 22, 44, 0.3 * opacity + 0.12), Fx.rgba(8, 10, 28, 0.42 * opacity + 0.14));
                Fx.box(g, x, y, w, h, corner, Fx.rgba(255, 255, 255, 0.1), Fx.rgba(255, 255, 255, 0.03));
            }
            if (w > 300 * u && h > 200 * u) caustics(g, x, y, w, h, u, corner);
        }

        // Sheen under the top edge and shade pooling at the bottom
        double in = 2 * u;
        Fx.box(g, x + in, y + in, w - in * 2, h * 0.34, corner - in, Fx.a(WHITE, solid ? 0.1 : 0.2), Fx.a(WHITE, 0));
        Fx.box(g, x + in, y + h * 0.62, w - in * 2, h * 0.38 - in, corner - in, Fx.a(BLACK, 0), Fx.a(BLACK, 0.2));

        // Rim: bright where the light lands, with a second line just inside for thickness
        Fx.boxOutline(g, x, y, w, h, corner, Math.max(1, Math.round(1.4 * u)), Fx.a(WHITE, 0.7), Fx.a(WHITE, 0.2));
        Fx.boxOutline(g, x + 2.5 * u, y + 2.5 * u, w - 5 * u, h - 5 * u, corner - 2.5 * u, t, Fx.a(WHITE, 0.14), Fx.a(WHITE, 0.03));
    }

    /** Slow blobs of light inside the glass, like liquid moving behind it. They loop every 14 seconds. */
    private static void caustics(GuiRenderer g, double x, double y, double w, double h, double u, double corner) {
        double p = Fx.phase(14);
        double margin = corner + 18 * u;
        for (int i = 0; i < 4; i++) {
            double seed = 0.17 + i * 0.23;
            double cx = x + margin + (w - margin * 2) * Fx.clamp01(0.5 + 0.34 * Fx.flow(p, seed));
            double cy = y + margin + (h - margin * 2) * Fx.clamp01(0.5 + 0.3 * Fx.flow(p, seed + 0.41));
            double reach = Math.min(Math.min(cx - x, x + w - cx), Math.min(cy - y, y + h - cy)) - 2 * u;
            double radius = Math.min(reach, (90 + 40 * i) * u);
            if (radius <= 0) continue;
            Color c = i % 2 == 0 ? Fx.mix(A, WHITE, 0.55) : Fx.mix(B, WHITE, 0.45);
            Fx.halo(g, cx, cy, radius * 0.1, radius, new Color(c.r, c.g, c.b, 255), tinted ? 0.13 : 0.08);
        }
    }

    static void plate(GuiRenderer g, double x, double y, double w, double h, double u, double corner, double hover) {
        corner = Math.min(corner, h / 2);
        Fx.box(g, x, y, w, h, corner, Fx.a(WHITE, 0.1 + 0.07 * hover), Fx.a(WHITE, 0.03 + 0.04 * hover));
        Fx.boxOutline(g, x, y, w, h, corner, Fx.hair(u), Fx.a(WHITE, 0.3 + 0.2 * hover), Fx.a(WHITE, 0.07));
    }

    static void lit(GuiRenderer g, double x, double y, double w, double h, double u, double corner, double a1, double a2, double a3, Color border, double glow, double k) {
        if (k <= 0) return;
        corner = Math.min(corner, h / 2);
        Fx.boxOutline(g, x - 2 * u, y - 2 * u, w + 4 * u, h + 4 * u, corner + 2 * u, 2 * u, Fx.a(Fx.mix(A, B, 0.5), 0.22 * k));
        Fx.box(g, x, y, w, h, corner, Fx.a(A, 0.5 * k), Fx.a(B, 0.42 * k), Fx.a(B, 0.5 * k), Fx.a(A, 0.42 * k));
        Fx.boxOutline(g, x, y, w, h, corner, Fx.hair(u), Fx.a(WHITE, 0.64 * k), Fx.a(WHITE, 0.19 * k));
    }

    static void gradient(GuiRenderer g, double x, double y, double w, double h, double corner, double a1, double a2, double a3) {
        Fx.box(g, x, y, w, h, corner, Fx.a(ACCENT, a1), Fx.a(Fx.mix(A, B, 0.5), a2), Fx.a(ACCENT2, a3), Fx.a(Fx.mix(A, B, 0.5), a2));
    }

    static void field(GuiRenderer g, double x, double y, double w, double h, double u, double corner, boolean hot, boolean pressed) {
        corner = Math.min(corner, h / 2);
        Fx.box(g, x, y, w, h, corner, pressed ? Fx.mix(FIELD, ACCENT, 0.3) : FIELD);
        Fx.boxOutline(g, x, y, w, h, corner, Fx.hair(u),
            hot || pressed ? Fx.a(ACCENT, 0.9) : Fx.a(BLACK, 0.3),
            hot || pressed ? Fx.a(ACCENT, 0.9) : Fx.a(WHITE, 0.3));
    }

    /** Capsule switch: an empty groove when off, the two glass colors with a droplet knob when on. */
    static void toggle(GuiRenderer g, double x, double y, double w, double h, double u, double on, double hover, double pulse) {
        double r = h / 2, cy = y + r;
        Fx.box(g, x, y, w, h, r, Fx.rgba(8, 12, 34, 0.5));
        Fx.boxOutline(g, x, y, w, h, r, Fx.hair(u), Fx.a(WHITE, 0.2 + 0.25 * hover));
        if (on > 0) {
            Fx.glowRect(g, x + r, cy, w - r * 2, 0, r + 8 * u, Fx.a(ACCENT, 0.3 * on * pulse));
            Fx.box(g, x, y, w, h, r, Fx.a(A, 0.95 * on), Fx.a(B, 0.95 * on), Fx.a(B, 0.95 * on), Fx.a(A, 0.95 * on));
            Fx.boxOutline(g, x, y, w, h, r, Fx.hair(u), Fx.a(WHITE, 0.6 * on), Fx.a(WHITE, 0.2 * on));
        }
        double kr = r - 3 * u, kx = x + 3 * u + kr + (w - 6 * u - kr * 2) * on;
        Fx.circle(g, kx, cy + 1.5 * u, kr + u, Fx.rgba(0, 0, 0, 0.28));
        Fx.circle(g, kx, cy, kr, Fx.rgba(206, 216, 240, 1));
        Fx.circle(g, kx - kr * 0.08, cy - kr * 0.1, kr * 0.86, Fx.rgba(255, 255, 255, 1));
        Fx.ellipse(g, kx - kr * 0.3, cy - kr * 0.38, kr * 0.26, kr * 0.15, -0.5, Fx.a(ACCENT, 0.5));
    }

    static void bead(GuiRenderer g, double cx, double cy, double radius, double on, double pulse) {
        if (on < 1) Fx.circle(g, cx, cy, radius, Fx.a(WHITE, 0.28 * (1 - on)));
        if (on > 0) {
            Fx.glowDot(g, cx, cy, radius, radius * 1.6 * pulse, ACCENT, on);
            Fx.circle(g, cx, cy, radius, Fx.a(ACCENT, on));
            Fx.circle(g, cx - radius * 0.25, cy - radius * 0.3, radius * 0.35, Fx.a(WHITE, 0.9 * on));
        }
    }
}
