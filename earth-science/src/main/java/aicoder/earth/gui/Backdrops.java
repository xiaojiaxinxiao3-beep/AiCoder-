package aicoder.earth.gui;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;

import java.util.Random;

import static meteordevelopment.meteorclient.utils.Utils.getWindowHeight;
import static meteordevelopment.meteorclient.utils.Utils.getWindowWidth;

/** The full-screen scene behind the windows, one per theme. All motion is slow and loops seamlessly. */
public final class Backdrops {
    private Backdrops() {
    }

    /** Fixed noise table, so every frame and every launch draws the same scene. */
    private static final double[] RND = new double[1024];

    static {
        Random random = new Random(0xEA47);
        for (int i = 0; i < RND.length; i++) RND[i] = random.nextDouble();
    }

    private static double rnd(int i) {
        return RND[i & 1023];
    }

    private static final Color[] STRATA_BANDS = {
        new Color(224, 192, 142), new Color(206, 152, 98), new Color(178, 114, 72), new Color(152, 98, 66),
        new Color(124, 84, 62), new Color(146, 108, 82), new Color(100, 68, 52), new Color(82, 56, 44), new Color(60, 42, 34)
    };

    private static final Color[] SHARDS = {
        new Color(150, 70, 210), new Color(196, 120, 250), new Color(120, 50, 180), new Color(222, 160, 255)
    };

    /**
     * @param opacity how much of the game the scene hides, 0..1
     */
    public static void render(GuiRenderer g, Style style, double unit, double opacity) {
        double w = getWindowWidth();
        double h = getWindowHeight();

        Fx.nextFrame();
        if (opacity <= 0) return;

        // Own scissor, so the scene is flushed to the screen before any window is drawn over it
        g.scissorStart(0, 0, w, h);
        switch (style.backdrop) {
            case STRATA -> strata(g, w, h, unit, opacity);
            case MAGMA -> magma(g, w, h, unit, opacity);
            case GLACIER -> glacier(g, w, h, unit, opacity);
            case ABYSS -> abyss(g, w, h, unit, opacity);
            case TOPOGRAPHY -> topography(g, w, h, unit, opacity);
            case ATMOSPHERE -> atmosphere(g, w, h, unit, opacity);
            case SEISMOGRAPH -> seismograph(g, w, h, unit, opacity);
            case GEODE -> geode(g, w, h, unit, opacity);
            case ORBIT -> orbit(g, w, h, unit, opacity);
            case AMBER -> amber(g, w, h, unit, opacity);
        }
        g.scissorEnd();
    }

    private static void base(GuiRenderer g, double w, double h, int r1, int g1, int b1, int r2, int g2, int b2, double o) {
        Color top = Fx.rgba(r1, g1, b1, o);
        Color bottom = Fx.rgba(r2, g2, b2, o);
        g.quad(0, 0, w, h, top, top, bottom, bottom);
    }

    /** Fills everything below the curve {@code y(x)} sampled at two neighbouring columns. */
    private static void column(GuiRenderer g, double x, double step, double y0, double y1, double h, Color c) {
        double low = Math.max(y0, y1);
        g.quad(x, low, step, h - low, c);
        if (y0 < y1) Fx.tri(g, x, y0, x, y1, x + step, y1, c);
        else if (y1 < y0) Fx.tri(g, x + step, y1, x + step, y0, x, y0, c);
    }

    // 01 Strata: stacked sediment layers with gently undulating boundaries

    private static void strata(GuiRenderer g, double w, double h, double u, double o) {
        double p = Fx.phase(24);
        double tau = Math.PI * 2;
        double step = Math.max(12, w / 96);

        base(g, w, h, 236, 216, 172, 236, 216, 172, o);

        for (int i = 0; i < STRATA_BANDS.length; i++) {
            Color c = Fx.a(STRATA_BANDS[i], o);
            double baseline = h * (0.07 + i * 0.105);
            double amp = (14 + 10 * rnd(i)) * u;
            double k1 = 1 + (i % 3) * 0.5;
            double k2 = 2.5 + (i % 2);

            double prev = 0;
            for (double x = 0; x <= w; x += step) {
                double f = x / w;
                double y = baseline + amp * (0.6 * Math.sin(tau * (f * k1 + p) + i * 1.7) + 0.4 * Math.sin(tau * (f * k2 - p) + i * 0.9));
                if (x > 0) column(g, x - step, step, prev, y, h, c);
                prev = y;
            }
        }
    }

    // 02 Magma: glowing fissures running towards the viewer, embers rising

    private static void magma(GuiRenderer g, double w, double h, double u, double o) {
        double p = Fx.phase(8);

        base(g, w, h, 8, 5, 5, 16, 7, 5, o);
        Color heat = Fx.rgba(150, 34, 6, 0.55 * o);
        Color none = Fx.rgba(150, 34, 6, 0);
        g.quad(0, h * 0.55, w, h * 0.45, none, none, heat, heat);

        double vx = w / 2, vy = h * 0.42;
        for (int i = 0; i < 10; i++) {
            double bx = w * (i + 0.2 + 0.6 * rnd(i + 40)) / 10;
            double reach = 0.2 + 0.25 * rnd(i + 60);
            double ex = bx + (vx - bx) * reach, ey = h + (vy - h) * reach;
            double pulse = 0.65 + 0.3 * Fx.flow(p, rnd(i + 80));
            Fx.glowLine(g, bx, h + 4 * u, ex, ey, 3 * u, Fx.rgba(255, 96, 28, 1), 10 * u, pulse * o);
        }

        for (int i = 0; i < 46; i++) {
            double rise = (rnd(i + 100) + p * (1 + i % 3)) % 1;
            double x = rnd(i + 200) * w + Fx.flow(p, rnd(i)) * 14 * u;
            double y = h - rise * h * 0.95;
            double fade = Math.sin(Math.PI * rise);
            Fx.circle(g, x, y, (1 + 1.6 * rnd(i + 300)) * u, Fx.rgba(255, 176, 70, 0.7 * fade * o));
        }
    }

    // 03 Glacier: pale sky over blue ice, with a mesh of fracture lines

    private static void glacier(GuiRenderer g, double w, double h, double u, double o) {
        double p = Fx.phase(20);

        base(g, w, h, 236, 247, 255, 66, 128, 178, o);

        int n = 34;
        double[] px = new double[n], py = new double[n];
        for (int i = 0; i < n; i++) {
            // Keep the shards mostly along the lower edge and the sides
            double edge = rnd(i + 20);
            px[i] = w * rnd(i) + Fx.flow(p, rnd(i + 5)) * 8 * u;
            py[i] = h * (i % 3 == 0 ? edge * 0.5 : 0.55 + 0.5 * edge) + Fx.flow(p, rnd(i + 9) + 0.4) * 6 * u;
        }

        Color line = Fx.rgba(255, 255, 255, 0.42 * o);
        double t = Fx.hair(u);
        for (int i = 0; i < n; i++) {
            for (int step : new int[]{1, 3, 7}) {
                int j = (i + step) % n;
                Fx.line(g, px[i], py[i], px[j], py[j], t, line);
            }
        }
    }

    // 04 Abyss: shafts of light from the surface, depth contours and drifting plankton

    private static void abyss(GuiRenderer g, double w, double h, double u, double o) {
        double p = Fx.phase(20);

        base(g, w, h, 16, 100, 146, 2, 14, 30, o);

        for (int i = 0; i < 7; i++) {
            double x = w * (0.08 + i * 0.14) + Fx.flow(p, rnd(i)) * 30 * u;
            double rw = (26 + 30 * rnd(i + 10)) * u;
            double slant = 70 * u;
            double len = h * (0.55 + 0.3 * rnd(i + 20));
            Color c = Fx.rgba(150, 220, 255, (0.05 + 0.03 * Fx.flow(p, rnd(i + 30))) * o);
            Fx.tri(g, x, 0, x + rw, 0, x + rw + slant, len, c);
            Fx.tri(g, x, 0, x + rw + slant, len, x + slant, len, c);
        }

        double t = Fx.hair(u);
        for (int k = 0; k < 3; k++) {
            double cx = w * (0.12 + 0.38 * k), cy = h * (0.78 + 0.12 * rnd(k + 50));
            for (int i = 1; i <= 6; i++) {
                Fx.contour(g, cx, cy, i * 46 * u, 0.7, k * 3 + i * 0.37, 0.16, p, t, Fx.rgba(60, 200, 200, 0.14 * o));
            }
        }

        for (int i = 0; i < 34; i++) {
            double rise = (rnd(i + 100) + p * (1 + i % 2)) % 1;
            double x = rnd(i + 200) * w + Fx.flow(p, rnd(i)) * 18 * u;
            double y = h * (1.02 - rise * 0.6);
            double fade = Math.sin(Math.PI * rise);
            double r = (1.2 + 2 * rnd(i + 300)) * u;
            Color c = Fx.rgba(60, 235, 205, 1);
            Fx.glowDot(g, x, y, r, r * 2.5, c, fade * o);
            Fx.circle(g, x, y, r, Fx.a(c, 0.85 * fade * o));
        }
    }

    // 05 Topography: a paper map with a survey grid, contour lines and a river

    private static void topography(GuiRenderer g, double w, double h, double u, double o) {
        double p = Fx.phase(30);
        double t = Fx.hair(u);

        base(g, w, h, 240, 232, 210, 236, 226, 202, o);

        Color grid = Fx.rgba(150, 132, 104, 0.16 * o);
        for (double x = 0; x < w; x += 48 * u) g.quad(Math.round(x), 0, t, h, grid);
        for (double y = 0; y < h; y += 48 * u) g.quad(0, Math.round(y), w, t, grid);

        double[][] hills = {{0.12, 0.2}, {0.74, 0.12}, {0.82, 0.76}, {0.3, 0.95}};
        for (int k = 0; k < hills.length; k++) {
            for (int i = 1; i <= 8; i++) {
                boolean index = i % 4 == 0;
                Fx.contour(g, w * hills[k][0], h * hills[k][1], i * 34 * u, 0.82, k * 2.3 + i * 0.21, 0.2, p,
                    index ? t * 2 : t, Fx.rgba(176, 128, 92, (index ? 0.75 : 0.5) * o));
            }
        }

        double px = 0, py = 0;
        for (int i = 0; i <= 60; i++) {
            double f = i / 60.0;
            double x = f * w;
            double y = h * (0.52 + 0.1 * Math.sin(f * 7.3 + 1) + 0.04 * Math.sin(f * 19 + Math.PI * 2 * p));
            if (i > 0) Fx.line(g, px, py, x, y, 2 * u, Fx.rgba(92, 142, 204, 0.7 * o));
            px = x;
            py = y;
        }
    }

    // 06 Atmosphere: a weather chart at night, isobars slowly shifting

    private static void atmosphere(GuiRenderer g, double w, double h, double u, double o) {
        double p = Fx.phase(24);
        double t = Fx.hair(u);

        base(g, w, h, 10, 30, 58, 4, 14, 32, o);

        double[][] cells = {{0.3, 0.45}, {0.78, 0.25}, {0.62, 0.9}};
        for (int k = 0; k < cells.length; k++) {
            double cx = w * cells[k][0] + Fx.flow(p, k * 0.31) * 16 * u;
            double cy = h * cells[k][1] + Fx.flow(p, k * 0.31 + 0.5) * 10 * u;
            for (int i = 1; i <= 7; i++) {
                Fx.contour(g, cx, cy, i * 62 * u, 0.72, k * 1.9 + i * 0.17, 0.14, p, t, Fx.rgba(134, 184, 232, 0.2 * o));
            }
        }
    }

    // 07 Seismograph: graph paper with traces scrolling past

    private static void seismograph(GuiRenderer g, double w, double h, double u, double o) {
        double t = Fx.hair(u);

        base(g, w, h, 244, 239, 227, 244, 239, 227, o);

        Color fine = Fx.rgba(214, 150, 140, 0.2 * o);
        Color bold = Fx.rgba(214, 150, 140, 0.42 * o);
        int i = 0;
        for (double x = 0; x < w; x += 8 * u, i++) g.quad(Math.round(x), 0, t, h, i % 5 == 0 ? bold : fine);
        i = 0;
        for (double y = 0; y < h; y += 8 * u, i++) g.quad(0, Math.round(y), w, t, i % 5 == 0 ? bold : fine);

        // The paper feeds continuously; the trace is a function of position on the paper, so it never jumps
        double feed = (System.currentTimeMillis() % 3_600_000L) / 1000.0 * 40 * u;
        double step = Math.max(4, 3 * u);

        for (int row = 0; row < 8; row++) {
            double cy = h * (row + 0.55) / 8;
            Color c = row % 2 == 0 ? Fx.rgba(70, 60, 56, 0.8 * o) : Fx.rgba(192, 36, 28, 0.8 * o);
            double px = 0, py = 0;

            for (double x = 0; x <= w + step; x += step) {
                double s = (x + feed) / u;
                double burst = Math.pow(Math.max(0, Math.sin(s * 0.004 + row * 1.3)), 6);
                double y = cy + u * (1.2 * Math.sin(s * 0.9 + row) * Math.sin(s * 0.13 + row * 2)
                    + 9 * burst * Math.sin(s * 0.55 + row * 0.7) * Math.sin(s * 0.071));
                if (x > 0) Fx.line(g, px, py, x, y, Math.max(1, 1.1 * u), c);
                px = x;
                py = y;
            }
        }
    }

    // 08 Geode: crystals growing inwards from the rim of a cavity

    private static void geode(GuiRenderer g, double w, double h, double u, double o) {
        double p = Fx.phase(10);
        double cx = w / 2, cy = h / 2;

        base(g, w, h, 30, 8, 48, 20, 5, 34, o);
        Fx.halo(g, cx, cy, Math.min(w, h) * 0.1, Math.min(w, h) * 0.75, new Color(150, 70, 200), 0.55 * o);

        int n = 54;
        double reach = Math.hypot(w, h) / 2;
        for (int i = 0; i < n; i++) {
            double ang = Math.PI * 2 * (i + 0.8 * rnd(i)) / n;
            double r0 = reach * (0.52 + 0.2 * rnd(i + 60));
            double r1 = reach * 1.1;
            double hw = (12 + 30 * rnd(i + 120)) * u;
            double ca = Math.cos(ang), sa = Math.sin(ang);
            double nx = -sa * hw, ny = ca * hw;

            double shimmer = 0.5 + 0.25 * Fx.flow(p, rnd(i + 180));
            Color c = Fx.a(SHARDS[i % SHARDS.length], shimmer * o);

            // A pointed prism: narrow tip towards the centre, widening outwards
            double tx = cx + ca * r0, ty = cy + sa * r0;
            double mx = cx + ca * (r0 + hw * 1.4), my = cy + sa * (r0 + hw * 1.4);
            double ox = cx + ca * r1, oy = cy + sa * r1;
            Fx.tri(g, tx, ty, mx + nx, my + ny, mx - nx, my - ny, c);
            Fx.tri(g, mx + nx, my + ny, mx - nx, my - ny, ox - nx, oy - ny, c);
            Fx.tri(g, mx + nx, my + ny, ox - nx, oy - ny, ox + nx, oy + ny, c);
            Fx.line(g, tx, ty, ox + nx, oy + ny, Fx.hair(u), Fx.rgba(240, 210, 255, 0.35 * shimmer * o));
        }

        stars(g, w, h, u, o, 36, 400, false);
    }

    // 09 Orbit: stars, and the limb of the planet glowing along the bottom edge

    private static void orbit(GuiRenderer g, double w, double h, double u, double o) {
        base(g, w, h, 2, 4, 12, 3, 6, 18, o);
        stars(g, w, h, u, o, 170, 500, true);

        double radius = w * 1.25;
        double cx = w / 2;
        double cy = h - 64 * u + radius;
        double step = Math.max(12, w / 96);
        // Not a scratch color: it has to survive the glow calls inside the loop
        Color night = new Color(4, 10, 30, (int) Math.round(255 * o));

        double prev = 0;
        for (double x = 0; x <= w; x += step) {
            double dx = x - cx;
            double y = cy - Math.sqrt(Math.max(0, radius * radius - dx * dx));
            if (x > 0) {
                column(g, x - step, step, prev, y, h, night);
                Fx.glowLine(g, x - step, prev, x, y, 2 * u, Fx.rgba(96, 170, 255, 1), 26 * u, 0.9 * o);
            }
            prev = y;
        }
    }

    private static void stars(GuiRenderer g, double w, double h, double u, double o, int count, int salt, boolean gold) {
        double p = Fx.phase(6);
        for (int i = 0; i < count; i++) {
            double x = rnd(i + salt) * w, y = rnd(i + salt + 300) * h;
            double s = Math.max(1, Math.round((0.8 + 1.4 * rnd(i + salt + 77)) * u));
            double twinkle = 0.45 + 0.4 * Fx.flow(p, rnd(i + salt + 150));
            boolean warm = gold && i % 4 == 0;
            g.quad(Math.round(x), Math.round(y), s, s, warm ? Fx.rgba(255, 204, 128, twinkle * o) : Fx.rgba(255, 255, 255, twinkle * o));
        }
    }

    // 10 Amber: warm resin with trapped bubbles and old fracture lines

    private static void amber(GuiRenderer g, double w, double h, double u, double o) {
        double p = Fx.phase(18);

        Color a = Fx.rgba(128, 58, 10, o), b = Fx.rgba(92, 38, 8, o);
        g.quad(0, 0, w, h, a, b, a, b);
        Fx.halo(g, w * 0.45, h * 0.12, h * 0.1, h * 0.8, new Color(240, 150, 30), 0.7 * o);
        Fx.halo(g, w * 0.5, h * 1.02, h * 0.05, h * 0.55, new Color(230, 140, 28), 0.55 * o);

        double t = Fx.hair(u);
        Fx.contour(g, w * -0.05, h * 1.25, h * 0.75, 1, 0.4, 0.05, p, t * 2, Fx.rgba(96, 44, 8, 0.6 * o));
        Fx.contour(g, w * 0.95, h * -0.55, h * 0.8, 1, 1.7, 0.05, p, t * 2, Fx.rgba(96, 44, 8, 0.6 * o));
        Fx.contour(g, w * 1.2, h * 1.1, h * 0.5, 1, 2.9, 0.06, p, t * 1.5, Fx.rgba(96, 44, 8, 0.5 * o));

        for (int i = 0; i < 30; i++) {
            double x = rnd(i + 600) * w + Fx.flow(p, rnd(i + 20)) * 6 * u;
            double y = rnd(i + 700) * h + Fx.flow(p, rnd(i + 40) + 0.3) * 8 * u;
            double r = (3 + 9 * rnd(i + 800)) * u;
            Fx.ring(g, x, y, r, t, Fx.rgba(255, 214, 150, 0.55 * o));
        }
    }
}
