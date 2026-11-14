package aicoder.zorynth;

import dev.zorynth.api.PluginApi.Canvas;

/**
 * Backdrops and frames drawn with the two things a Canvas has: rectangles and text.
 * Motion is sums of sines on a clock in seconds; t = 0 is the still frame.
 */
final class Backdrops {
    private Backdrops() {}

    private static final String HEX = "0123456789ABCDEF";

    static void render(Canvas g, Style s, double t, double strength) {
        int w = g.width(), h = g.height();
        if (w <= 0 || h <= 0) return;
        double o = strength;
        if (s.kind() == Style.Kind.AURORA) { aurora(g, w, h, t, o); return; }
        gradient(g, w, h, s.top(), s.bottom(), (int) (215 * o));
        switch (s.kind()) {
            case CYBER -> {
                glow(g, w, h, .22 + .05 * Math.sin(t * .31), .30 + .06 * Math.sin(t * .23), .34, 0x00F0FF, 70 * o);
                glow(g, w, h, .78 + .05 * Math.sin(t * .27 + 2), .68 + .05 * Math.sin(t * .19 + 1), .36, 0xFF2A6D, 70 * o);
                glow(g, w, h, .55 + .08 * Math.sin(t * .17 + 4), .18 + .05 * Math.sin(t * .29 + 3), .26, 0xB026FF, 60 * o);
                rain(g, w, h, t, o);
            }
            case GLASS -> {
                glow(g, w, h, .25 + .08 * Math.sin(t * .21), .32 + .08 * Math.sin(t * .17 + 1), .42, s.a(), 95 * o);
                glow(g, w, h, .74 + .07 * Math.sin(t * .19 + 2), .64 + .08 * Math.sin(t * .23 + 3), .44, s.b(), 95 * o);
                glow(g, w, h, .52 + .10 * Math.sin(t * .13 + 5), .50 + .10 * Math.sin(t * .11 + 4), .22, 0xFFFFFF, 26 * o);
            }
            case STRATA -> {
                int[] layers = {0x6E4A30, 0x8A5A38, 0x5A3C28, 0xA87048, 0x4A3020, 0x7C5234};
                waves(g, w, h, t, layers, .34, 7, 110 * o);
                particles(g, w, h, t, 40, 11, 0, 0, s.a(), 70 * o, 2);
            }
            case MAGMA -> {
                glow(g, w, h, .5, 1.08, .62, s.a(), 120 * o * (.8 + .2 * Math.sin(t * 1.3)));
                glow(g, w, h, .2 + .04 * Math.sin(t * .4), .95, .32, s.b(), 100 * o);
                glow(g, w, h, .82, .98 + .03 * Math.sin(t * .5 + 1), .30, 0xFFA030, 80 * o);
                particles(g, w, h, t, 70, 23, 0, -26, 0xFFB050, 190 * o, 2);
            }
            case GLACIER -> {
                glow(g, w, h, .3, .1, .6, s.a(), 70 * o);
                waves(g, w, h, t * .3, new int[]{0xCFE8FA, 0x9CCBEE, 0xE8F5FF}, .70, 5, 46 * o);
                particles(g, w, h, t, 90, 37, 6, 18, 0xFFFFFF, 170 * o, 2);
            }
            case ABYSS -> {
                for (int i = 0; i < 5; i++) {
                    double x = .12 + i * .19 + .03 * Math.sin(t * .2 + i * 1.7);
                    ray(g, w, h, x, .05 + .02 * (i % 2), s.a(), 30 * o * (.6 + .4 * Math.sin(t * .5 + i)));
                }
                glow(g, w, h, .5, -.1, .6, s.b(), 80 * o);
                particles(g, w, h, t, 60, 51, 0, -14, 0xBFF6F0, 130 * o, 3);
            }
            case TOPOGRAPHY -> contours(g, w, h, t, s.a(), s.b(), 80 * o);
            case ATMOSPHERE -> {
                glow(g, w, h, .3 + .06 * Math.sin(t * .15), .4, .4, s.a(), 60 * o);
                glow(g, w, h, .76 + .05 * Math.sin(t * .12 + 2), .6, .3, s.b(), 46 * o);
                lines(g, w, h, t, 9, s.a(), 70 * o, .05, .25, 1);
            }
            case SEISMOGRAPH -> {
                for (int i = 1; i < 6; i++) g.fill(0, h * i / 6, w, 1, argb(26 * o, 0xFBF7EF));
                trace(g, w, h, t, .30, s.a(), 200 * o, 1.0);
                trace(g, w, h, t + 7, .52, 0xFBF7EF, 120 * o, .5);
                trace(g, w, h, t + 13, .74, 0xFBF7EF, 120 * o, .35);
            }
            case GEODE -> {
                glow(g, w, h, .5, .5, .55, s.b(), 90 * o);
                glow(g, w, h, .5 + .04 * Math.sin(t * .3), .5 + .04 * Math.cos(t * .3), .25, 0xFFFFFF, 30 * o);
                sparkle(g, w, h, t, 80, 71, s.a(), 220 * o);
            }
            case ORBIT -> {
                sparkle(g, w, h, t, 140, 83, 0xFFFFFF, 200 * o);
                horizon(g, w, h, s.a(), 150 * o);
                orbit(g, w, h, t, s.b(), 220 * o);
            }
            case AMBER -> {
                glow(g, w, h, .35 + .05 * Math.sin(t * .1), .4, .5, s.a(), 80 * o);
                glow(g, w, h, .7, .7 + .05 * Math.sin(t * .13 + 1), .4, s.b(), 60 * o);
                particles(g, w, h, t, 34, 97, 0, -3, 0xFFE2A0, 110 * o, 4);
            }
            default -> {}
        }
    }

    /** Corner brackets, the theme's name and its readouts: the parts of the old header and footer bars. */
    static void frame(Canvas g, Style s, double t) {
        int w = g.width(), h = g.height();
        if (w < 120 || h < 80) return;
        int a = s.primary() & 0xFFFFFF, b = s.kind() == Style.Kind.CYBER ? 0xFF2A6D : s.b();
        double breathe = .7 + .3 * Math.sin(t * 1.6);
        int line = argb(230 * breathe, a), len = 18, m = 6;
        g.fill(m, m, len, 2, line); g.fill(m, m, 2, len, line);
        g.fill(w - m - len, m, len, 2, line); g.fill(w - m - 2, m, 2, len, line);
        g.fill(m, h - m - 2, len, 2, line); g.fill(m, h - m - len, 2, len, line);
        g.fill(w - m - len, h - m - 2, len, 2, line); g.fill(w - m - 2, h - m - len, 2, len, line);

        g.text("AICODER", m + 8, m + 7, s.text(), true);
        g.text("// " + s.tag(), m + 56, m + 7, argb(255, b), true);

        int y = h - m - 14, x = m + 8;
        if (s.kind() == Style.Kind.CYBER) {
            // Hazard strip: magenta and dark purple diagonals sliding slowly.
            int shift = (int) (t * 6) % 8;
            for (int i = 0; i < 60; i++) for (int j = 0; j < 8; j += 2)
                g.fill(x + i, y + j, 1, 2, ((i + j + shift) / 4) % 2 == 0 ? 0xE0FF2A6D : 0xE02A0A3A);
            x += 68;
            // Two scope lines at the right.
            for (int i = 0; i < 90; i++) {
                double p = i / 90.0 * Math.PI * 2;
                int y1 = (int) Math.round(4 * Math.sin(p * 2 + t * 3) + 2 * Math.sin(p * 5 - t * 2));
                int y2 = (int) Math.round(3 * Math.sin(p * 3 - t * 2.5) + 2 * Math.sin(p * 7 + t));
                g.fill(w - m - 100 + i, y + 4 + y1, 1, 1, 0xFF00F0FF);
                g.fill(w - m - 100 + i, y + 4 + y2, 1, 1, 0xFFFF2A6D);
            }
        }
        g.text(s.footer(), x, y, s.secondary(), true);
    }

    // ---- pieces ----

    private static void gradient(Canvas g, int w, int h, int top, int bottom, int alpha) {
        int bands = 24;
        for (int i = 0; i < bands; i++) {
            int y0 = h * i / bands, y1 = h * (i + 1) / bands;
            g.fill(0, y0, w, y1 - y0, argb(alpha, mix(top, bottom, i / (bands - 1.0))));
        }
    }

    /** Soft round light: a coarse grid of cells whose alpha falls off with distance. */
    private static void glow(Canvas g, int w, int h, double cx, double cy, double radius, int rgb, double alpha) {
        int cell = Math.max(4, w / 48);
        double px = cx * w, py = cy * h, r = radius * w;
        int x0 = Math.max(0, (int) ((px - r) / cell)), x1 = Math.min((w - 1) / cell, (int) ((px + r) / cell));
        int y0 = Math.max(0, (int) ((py - r) / cell)), y1 = Math.min((h - 1) / cell, (int) ((py + r) / cell));
        for (int j = y0; j <= y1; j++) for (int i = x0; i <= x1; i++) {
            double dx = (i + .5) * cell - px, dy = (j + .5) * cell - py;
            double f = 1 - (dx * dx + dy * dy) / (r * r);
            if (f <= 0) continue;
            int al = (int) (alpha * f * f);
            if (al > 1) g.fill(i * cell, j * cell, cell, cell, argb(al, rgb));
        }
    }

    /** Hex data rain in cyan and magenta, fading at both ends. */
    private static void rain(Canvas g, int w, int h, double t, double o) {
        int columns = Math.max(8, w / 26);
        for (int i = 0; i < columns; i++) {
            double speed = 16 + 30 * hash(i, 1), length = 6 + (int) (8 * hash(i, 2));
            int x = (int) ((i + hash(i, 3) * .6) * w / columns);
            double head = (hash(i, 4) * (h + 200) + t * speed) % (h + length * 10) - 20;
            int rgb = hash(i, 5) < .55 ? 0x00F0FF : 0xFF2A6D;
            for (int k = 0; k < length; k++) {
                int y = (int) (head - k * 10);
                if (y < -10 || y > h) continue;
                double fade = (1 - k / length) * Math.min(1, Math.min(y / 60.0, (h - y) / 60.0));
                if (fade <= .05) continue;
                int ch = (int) (hash(i * 31 + k, 6 + (int) (t * 3 + hash(i, k) * 9)) * 16) & 15;
                g.text(HEX.substring(ch, ch + 1), x, y, argb(140 * o * fade, k == 0 ? 0xFFFFFF : rgb), false);
            }
        }
    }

    /** Aurora night sky: sky colour is a function of position, sampled on a grid; three ribbons add light. */
    private static void aurora(Canvas g, int w, int h, double t, double o) {
        int cols = 48, rows = Math.max(12, (int) Math.round(cols * (double) h / w));
        int[] ribbon = {0x4DFFB8, 0x2FE6E0, 0x8F7BFF, 0xFF6FD8};
        for (int j = 0; j < rows; j++) for (int i = 0; i < cols; i++) {
            double u = (i + .5) / cols, v = (j + .5) / rows;
            double r = lerp(6, 2, v), gr = lerp(18, 4, v), b = lerp(38, 14, v);
            for (int k = 0; k < 3; k++) {
                double c = .26 + .15 * k + .07 * Math.sin(u * (3.2 + k) + t * (.20 + .05 * k) + k * 2.1)
                    + .03 * Math.sin(u * 9 - t * .33 + k);
                double width = .09 + .03 * Math.sin(u * 5 + t * .15 + k);
                double d = (v - c) / width;
                double f = Math.exp(-d * d) * (.55 + .45 * Math.sin(u * 14 + t * .6 + k * 1.3) * Math.sin(u * 3.1 - t * .2));
                if (v > c) f *= Math.exp(-(v - c) * 3);
                if (f < .01) continue;
                int col = mix(ribbon[k], ribbon[k + 1], .5 + .5 * Math.sin(u * 4 + t * .1 + k));
                r = screen(r, (col >> 16 & 255) * f); gr = screen(gr, (col >> 8 & 255) * f); b = screen(b, (col & 255) * f);
            }
            double vignette = 1 - .45 * ((u - .5) * (u - .5) + (v - .5) * (v - .5)) * 2;
            int x0 = w * i / cols, y0 = h * j / rows;
            g.fill(x0, y0, w * (i + 1) / cols - x0, h * (j + 1) / rows - y0,
                argb(235 * o, clamp(r * vignette) << 16 | clamp(gr * vignette) << 8 | clamp(b * vignette)));
        }
        sparkle(g, w, h * 3 / 5, t, 90, 131, 0xFFFFFF, 190 * o);
    }

    /** Stacked wavy bands filling the lower part of the screen (rock layers, ice shelves). */
    private static void waves(Canvas g, int w, int h, double t, int[] colors, double from, int step, double alpha) {
        int n = colors.length * 2;
        for (int x = 0; x < w; x += step) {
            double u = (double) x / w;
            int prev = h;
            for (int k = n - 1; k >= 0; k--) {
                double base = from + (1 - from) * k / n;
                int y = (int) (h * (base + .03 * Math.sin(u * (4 + k) + k * 1.9 + t * .08) + .012 * Math.sin(u * 17 + k)));
                if (y < prev) { g.fill(x, y, step, prev - y, argb(alpha, colors[k % colors.length])); prev = y; }
            }
        }
    }

    /** Drifting specks: embers, snow, bubbles, dust. vx/vy in pixels per second. */
    private static void particles(Canvas g, int w, int h, double t, int count, int seed, double vx, double vy, int rgb, double alpha, int size) {
        for (int i = 0; i < count; i++) {
            double speed = .4 + hash(i, seed);
            double x = mod(hash(i, seed + 1) * w + t * vx * speed + 9 * Math.sin(t * .7 + i), w);
            double y = mod(hash(i, seed + 2) * h + t * vy * speed, h);
            double twinkle = .5 + .5 * Math.sin(t * (1 + hash(i, seed + 3) * 2) + i);
            int sz = 1 + (int) (hash(i, seed + 4) * size);
            g.fill((int) x, (int) y, sz, sz, argb(alpha * (.35 + .65 * twinkle), rgb));
        }
    }

    private static void sparkle(Canvas g, int w, int h, double t, int count, int seed, int rgb, double alpha) {
        for (int i = 0; i < count; i++) {
            int x = (int) (hash(i, seed) * w), y = (int) (hash(i, seed + 1) * h);
            double f = .5 + .5 * Math.sin(t * (.6 + 2 * hash(i, seed + 2)) + i * 2.4);
            int a = (int) (alpha * (.2 + .8 * f * f));
            g.fill(x, y, 1, 1, argb(a, rgb));
            if (hash(i, seed + 3) > .8) { g.fill(x - 1, y, 3, 1, argb(a / 2.0, rgb)); g.fill(x, y - 1, 1, 3, argb(a / 2.0, rgb)); }
        }
    }

    /** A light shaft from the surface, widening downwards. */
    private static void ray(Canvas g, int w, int h, double cx, double half, int rgb, double alpha) {
        int bands = 16;
        for (int j = 0; j < bands; j++) {
            double v = j / (double) bands, width = (half + v * .06) * w;
            g.fill((int) (cx * w - width / 2 + v * w * .05), h * j / bands, (int) width, h / bands + 1, argb(alpha * (1 - v), rgb));
        }
    }

    /** Flowing lines across the screen (isobars). */
    private static void lines(Canvas g, int w, int h, double t, int count, int rgb, double alpha, double amp, double speed, int thick) {
        for (int k = 0; k < count; k++) {
            double base = (k + .5) / count;
            for (int x = 0; x < w; x += 4) {
                double u = (double) x / w;
                double y = base + amp * Math.sin(u * 5 + k * .9 + t * speed) + amp * .4 * Math.sin(u * 11 - t * speed * .6 + k);
                g.fill(x, (int) (y * h), 4, thick, argb(alpha, rgb));
            }
        }
    }

    /** One seismograph trace: quiet noise with a burst travelling along it. */
    private static void trace(Canvas g, int w, int h, double t, double at, int rgb, double alpha, double gain) {
        int prev = (int) (at * h);
        double burst = mod(t * .12, 1.4) - .2;
        for (int x = 0; x < w; x++) {
            double u = (double) x / w, d = (u - burst) / .09;
            double env = .08 + Math.exp(-d * d) * (u > burst ? 1 : .5);
            double v = env * (Math.sin(u * 180 + t * 3) * .6 + Math.sin(u * 77 - t * 5) * .4 + Math.sin(u * 311) * .3);
            int y = (int) (at * h + v * gain * h * .09);
            g.fill(x, Math.min(y, prev), 1, Math.abs(y - prev) + 1, argb(alpha, rgb));
            prev = y;
        }
    }

    /** Contour map: cells where the height field crosses a level. Every fifth level is the second colour. */
    private static void contours(Canvas g, int w, int h, double t, int rgb, int rgb2, double alpha) {
        int cell = Math.max(3, w / 160);
        int cols = w / cell + 2, rows = h / cell + 2;
        int[] level = new int[cols];
        for (int j = 0; j < rows; j++) {
            int left = 0;
            for (int i = 0; i < cols; i++) {
                int l = (int) Math.floor(height((double) i * cell / w, (double) j * cell / w, t) * 7);
                if (j > 0 && i > 0 && (l != left || l != level[i]))
                    g.fill(i * cell, j * cell, cell, cell, argb(Math.floorMod(l, 5) == 0 ? alpha : alpha * .55, Math.floorMod(l, 5) == 0 ? rgb2 : rgb));
                left = l;
                level[i] = l;
            }
        }
    }

    private static double height(double u, double v, double t) {
        return Math.sin(u * 5.1 + t * .03) * Math.cos(v * 6.3 - t * .02) + .6 * Math.sin(u * 11 + v * 7 + 1.3)
            + .35 * Math.cos(u * 3 - v * 13 + t * .04);
    }

    /** The lit limb of a planet along the bottom edge. */
    private static void horizon(Canvas g, int w, int h, int rgb, double alpha) {
        double radius = w * 1.25, cy = h + radius - h * .16;
        for (int x = 0; x < w; x += 3) {
            double dx = x - w / 2.0;
            int y = (int) (cy - Math.sqrt(radius * radius - dx * dx));
            for (int k = 0; k < 5; k++) g.fill(x, y - k * 5, 3, 5, argb(alpha * .35 * (1 - k / 5.0) * (1 - k / 5.0), rgb));
            g.fill(x, y, 3, h - y, argb(alpha, mix(0x0A1E3C, 0x04080F, Math.min(1, (h - y) / (h * .2)))));
            g.fill(x, y, 3, 2, argb(alpha, rgb));
        }
    }

    /** A dotted orbit with one satellite going round. */
    private static void orbit(Canvas g, int w, int h, double t, int rgb, double alpha) {
        double cx = w * .5, cy = h * .46, rx = w * .36, ry = h * .16, tilt = -.22;
        for (int i = 0; i < 120; i++) {
            double a = i / 120.0 * Math.PI * 2;
            double x = Math.cos(a) * rx, y = Math.sin(a) * ry;
            g.fill((int) (cx + x * Math.cos(tilt) - y * Math.sin(tilt)), (int) (cy + x * Math.sin(tilt) + y * Math.cos(tilt)), 1, 1, argb(alpha * .35, 0xFFFFFF));
        }
        double a = t * .25, x = Math.cos(a) * rx, y = Math.sin(a) * ry;
        int px = (int) (cx + x * Math.cos(tilt) - y * Math.sin(tilt)), py = (int) (cy + x * Math.sin(tilt) + y * Math.cos(tilt));
        g.fill(px - 3, py - 3, 7, 7, argb(alpha * .25, rgb));
        g.fill(px - 1, py - 1, 3, 3, argb(alpha, rgb));
    }

    // ---- maths ----

    static int argb(double alpha, int rgb) {
        return clamp(alpha) << 24 | rgb & 0xFFFFFF;
    }

    private static int clamp(double v) {
        return v <= 0 ? 0 : v >= 255 ? 255 : (int) v;
    }

    private static int mix(int a, int b, double f) {
        f = Math.max(0, Math.min(1, f));
        return clamp(lerp(a >> 16 & 255, b >> 16 & 255, f)) << 16 | clamp(lerp(a >> 8 & 255, b >> 8 & 255, f)) << 8 | clamp(lerp(a & 255, b & 255, f));
    }

    private static double lerp(double a, double b, double f) {
        return a + (b - a) * f;
    }

    private static double screen(double a, double b) {
        return 255 - (255 - a) * (255 - Math.min(255, Math.max(0, b))) / 255;
    }

    private static double mod(double v, double m) {
        double r = v % m;
        return r < 0 ? r + m : r;
    }

    private static double hash(int i, int salt) {
        int x = i * 374761393 + salt * 668265263;
        x = (x ^ x >>> 13) * 1274126177;
        return ((x ^ x >>> 16) & 0xFFFFFF) / (double) 0x1000000;
    }
}
