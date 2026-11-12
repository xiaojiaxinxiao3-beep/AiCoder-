package aicoder.earth.gui;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;

/** The on/off glyph of each theme. Module rows draw it large, checkboxes draw the same glyph small. */
public final class Toggles {
    private Toggles() {
    }

    private static final Color WHITE = new Color(255, 255, 255, 255);

    /**
     * @param radius half the glyph's size in framebuffer pixels
     * @param on     0 (off) to 1 (on), animated by the caller
     * @param hover  0..1, brightens the off state
     * @param seed   per-widget offset so neighbouring glows do not pulse in step
     */
    public static void draw(GuiRenderer g, Style s, double cx, double cy, double radius, double on, double hover, double seed) {
        double t = Math.max(1, radius * 0.12);
        double off = 1 - on;
        Color edge = Fx.a(s.light ? s.dim : s.text, (0.38 + 0.4 * hover) * off);
        boolean glow = !s.light;
        double pulse = 0.6 + 0.4 * Fx.flow(Fx.phase(4), 0.3 + seed);

        switch (s.toggle) {
            case STRATA -> {
                double w = radius * 0.92;
                double c = radius * 0.24;
                if (off > 0) Fx.boxOutline(g, cx - w, cy - w, w * 2, w * 2, c, t, edge);
                if (on > 0) {
                    Fx.box(g, cx - w, cy - w, w * 2, w * 2, c, Fx.a(s.title, on));
                    double band = w * 2 / 5;
                    Color[] layers = {s.accent, s.accent2, s.accent};
                    for (int i = 0; i < 3; i++) {
                        g.quad(cx - w + t * 1.5, cy - w + band * (i + 1) - band * 0.1, w * 2 - t * 3, band * 0.62, Fx.a(layers[i], on));
                    }
                }
            }
            case EMBER -> {
                if (off > 0) Fx.ring(g, cx, cy, radius * 0.86, t, edge);
                if (on > 0) {
                    Fx.glowDot(g, cx, cy, radius * 0.7, radius * (0.7 + 0.5 * pulse), s.accent, on);
                    Fx.circle(g, cx, cy, radius * 0.72, Fx.a(s.accent2, on));
                    Fx.circle(g, cx, cy, radius * 0.5, Fx.a(s.accent, on));
                    Fx.circle(g, cx, cy, radius * 0.24, Fx.rgba(255, 196, 120, on));
                }
            }
            case DIAMOND -> {
                if (off > 0) Fx.polygonOutline(g, cx, cy, radius * 0.95, 4, t, edge);
                if (on > 0) {
                    Fx.polygon(g, cx, cy, radius * 1.25, 4, Fx.a(s.accent, 0.16 * on));
                    Fx.polygon(g, cx, cy, radius * 0.95, 4, Fx.a(s.accent, on));
                    Fx.polygon(g, cx, cy, radius * 0.34, 4, Fx.a(WHITE, on));
                }
            }
            case SPHERE -> {
                if (off > 0) {
                    Fx.ring(g, cx, cy, radius * 0.86, t, edge);
                    Fx.circle(g, cx - radius * 0.3, cy - radius * 0.3, radius * 0.12, edge);
                }
                if (on > 0) {
                    Fx.glowDot(g, cx, cy, radius * 0.8, radius * (0.4 + 0.4 * pulse), s.accent, on);
                    Fx.circle(g, cx, cy, radius * 0.86, Fx.a(s.accent, on));
                    Fx.circle(g, cx - radius * 0.3, cy - radius * 0.3, radius * 0.2, Fx.a(WHITE, 0.9 * on));
                }
            }
            case TARGET -> {
                Color c = Fx.mix(Fx.a(s.dim, 0.75), s.accent, on);
                double r = radius * 0.78;
                Fx.ring(g, cx, cy, r, t * 1.2, c);
                if (on > 0) {
                    Fx.circle(g, cx, cy, r, Fx.a(s.accent, on));
                    Fx.circle(g, cx, cy, radius * 0.2, Fx.a(s.windowTop, on));
                }
                double tick = radius * 0.3;
                g.quad(cx - t * 0.6, cy - r - tick, t * 1.2, tick, c);
                g.quad(cx - t * 0.6, cy + r, t * 1.2, tick, c);
                g.quad(cx - r - tick, cy - t * 0.6, tick, t * 1.2, c);
                g.quad(cx + r, cy - t * 0.6, tick, t * 1.2, c);
            }
            case RADIO -> {
                if (off > 0) Fx.ring(g, cx, cy, radius * 0.84, t * 1.2, edge);
                if (on > 0) {
                    Fx.glowDot(g, cx, cy, radius * 0.8, radius * (0.4 + 0.4 * pulse), s.accent, on);
                    Fx.circle(g, cx, cy, radius * 0.84, Fx.a(s.accent, on));
                    Fx.circle(g, cx, cy, radius * 0.3, Fx.a(WHITE, on));
                }
            }
            case CHECK -> {
                double w = radius * 0.8;
                double c = radius * 0.16;
                if (off > 0) Fx.boxOutline(g, cx - w, cy - w, w * 2, w * 2, c, t * 1.2, edge);
                if (on > 0) {
                    Fx.box(g, cx - w, cy - w, w * 2, w * 2, c, Fx.a(s.accent, on));
                    Color mark = Fx.a(WHITE, on);
                    double lt = radius * 0.24;
                    Fx.line(g, cx - w * 0.52, cy + w * 0.02, cx - w * 0.12, cy + w * 0.42, lt, mark);
                    Fx.line(g, cx - w * 0.12, cy + w * 0.42, cx + w * 0.56, cy - w * 0.36, lt, mark);
                }
            }
            case HEXAGON -> {
                if (off > 0) Fx.polygonOutline(g, cx, cy, radius * 0.95, 6, t, edge);
                if (on > 0) {
                    for (int i = 3; i >= 1; i--) {
                        Fx.polygon(g, cx, cy, radius * (0.95 + (0.12 + 0.1 * pulse) * i), 6, Fx.a(s.accent, 0.1 * on));
                    }
                    Fx.polygon(g, cx, cy, radius * 0.95, 6, Fx.a(s.accent, on));
                    Fx.polygon(g, cx, cy, radius * 0.34, 6, Fx.a(WHITE, on));
                }
            }
            case PLANET -> {
                double tilt = -0.42;
                Color body = Fx.mix(Fx.a(s.dim, 0.6), s.accent, on);
                if (on > 0) {
                    if (glow) Fx.glowDot(g, cx, cy, radius * 0.6, radius * (0.3 + 0.3 * pulse), s.accent, on);
                    Fx.circle(g, cx, cy, radius * 0.6, Fx.a(s.accent, on));
                }
                if (off > 0) Fx.ring(g, cx, cy, radius * 0.6, t, Fx.a(body, off));
                Fx.ellipseRing(g, cx, cy, radius * 1.05, radius * 0.42, tilt, t * 1.2, body);
            }
            case BEAD -> {
                if (off > 0) Fx.ellipseRing(g, cx, cy, radius * 0.98, radius * 0.76, 0, t, Fx.a(s.dim, (0.4 + 0.4 * hover) * off));
                if (on > 0) {
                    Fx.ellipse(g, cx, cy, radius * (1.2 + 0.12 * pulse), radius * (0.98 + 0.12 * pulse), 0, Fx.a(s.accent, 0.16 * on));
                    Fx.ellipse(g, cx, cy, radius * 0.98, radius * 0.76, 0, Fx.a(s.accent2, on));
                    Fx.ellipse(g, cx, cy - radius * 0.06, radius * 0.86, radius * 0.62, 0, Fx.a(s.accent, on));
                    Fx.ellipse(g, cx - radius * 0.3, cy - radius * 0.26, radius * 0.24, radius * 0.14, 0, Fx.rgba(255, 244, 214, 0.9 * on));
                }
            }
        }
    }
}
