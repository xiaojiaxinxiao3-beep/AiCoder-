package aicoder.zorynth;

import dev.zorynth.api.PluginApi.Canvas;

/** Shapes shared by every theme: boxes with the theme's corner style, the window panel, the switch glyphs. */
final class Draw {
    private Draw() {}

    /** corner > 0: rounded; corner < 0: top-left and bottom-right cut at 45 degrees; 0: square. */
    static void box(Canvas c, int x, int y, int w, int h, int corner, int argb) {
        if (w <= 0 || h <= 0 || argb >>> 24 == 0) return;
        int limit = Math.min(w, h) / 2;
        if (corner > 0 && limit > 0) c.roundedRect(x, y, w, h, Math.min(corner, limit), argb);
        else if (corner < 0 && limit > 0) {
            int k = Math.min(-corner, limit);
            c.polygon(new double[]{x + k, y, x + w, y, x + w, y + h - k, x + w - k, y + h, x, y + h, x, y + k}, argb);
        } else c.fill(x, y, w, h, argb);
    }

    /** Outline as the gap between two boxes, so it follows the corner style. */
    static void framed(Canvas c, int x, int y, int w, int h, int corner, int fill, int line) {
        box(c, x - 1, y - 1, w + 2, h + 2, corner == 0 ? 0 : corner > 0 ? corner + 1 : corner - 1, line);
        box(c, x, y, w, h, corner, fill);
    }

    static void panel(Canvas c, Style s, int x, int y, int w, int h, double t) {
        int corner = s.corner();
        if (s.light()) box(c, x + 3, y + 4, w, h, corner, 0x38000000);
        box(c, x - 1, y - 1, w + 2, h + 2, corner == 0 ? 0 : corner > 0 ? corner + 1 : corner - 1, s.outline());
        box(c, x, y, w, h, corner, opaque(mix(s.winTop(), s.winBottom(), .5), s.light() ? 0xF4 : 0xE6));
        int inset = Math.abs(corner) + 2;
        if (!s.light() && w > inset * 2 && h > 40) {
            // Sheen from the top edge, and for the glass themes a slow tint drifting across.
            c.gradient(x + inset, y + 1, w - inset * 2, Math.min(60, h / 3), 0x16FFFFFF, 0x00FFFFFF, false);
            if (s.kind() == Style.Kind.GLASS || s.kind() == Style.Kind.AURORA) {
                int band = w / 3, bx = x + inset + (int) ((w - inset * 2 - band) * (.5 + .5 * Math.sin(t * .35)));
                c.gradient(bx, y + inset, band / 2, h - inset * 2, alpha(s.accent(), 0), alpha(s.accent(), 26), true);
                c.gradient(bx + band / 2, y + inset, band / 2, h - inset * 2, alpha(s.accent2(), 26), alpha(s.accent2(), 0), true);
            }
        }
        if (s.kind() == Style.Kind.CYBER) {
            // Brighter runs on the top-left edge and bottom-right corner, and the corner brackets outside.
            int k = -corner;
            c.fill(x + k, y - 1, Math.min(120, w / 4), 1, 0xFFFF2A6D);
            c.fill(x + w - k - Math.min(90, w / 5), y + h, Math.min(90, w / 5), 1, 0xFFFF2A6D);
            int a = alpha(0xFF00F0FF, (int) (170 + 80 * Math.sin(t * 1.6)));
            c.fill(x + w + 3, y - 5, 2, 14, a); c.fill(x + w - 9, y - 5, 14, 2, a);
            c.fill(x - 5, y + h - 9, 2, 14, a); c.fill(x - 5, y + h + 3, 14, 2, a);
            c.line(x - 5, y + k + 2, x + k + 2, y - 5, 1, a);
            c.line(x + w - k - 2, y + h + 5, x + w + 5, y + h - k - 2, 1, a);
        }
    }

    /** The on/off glyph, centred on (cx, cy), about 12 px across. */
    static void toggle(Canvas c, Style s, int cx, int cy, boolean on, double t) {
        int acc = s.accent(), off = s.fieldLine(), white = s.light() ? 0xFFFFFFFF : 0xFFF4FBFF;
        double breathe = .6 + .4 * Math.sin(t * 2.2 + cx * .05);
        switch (s.toggle()) {
            case HEXAGON -> {
                if (on) {
                    c.polygon(ngon(cx, cy, 8.5, 6, -90), alpha(acc, (int) (70 * breathe)));
                    c.polygon(ngon(cx, cy, 6, 6, -90), acc);
                    c.polygon(ngon(cx, cy, 2.4, 6, -90), white);
                } else outline(c, ngon(cx, cy, 6, 6, -90), off);
            }
            case DIAMOND -> {
                if (on) { c.polygon(ngon(cx, cy, 8, 4, -90), alpha(acc, (int) (60 * breathe))); c.polygon(ngon(cx, cy, 6, 4, -90), acc); c.polygon(ngon(cx, cy, 2, 4, -90), white); }
                else outline(c, ngon(cx, cy, 6, 4, -90), off);
            }
            case EMBER -> {
                if (on) { c.polygon(ngon(cx, cy + 1, 9, 3, -90), alpha(acc, (int) (70 * breathe))); c.polygon(ngon(cx, cy + 1, 6.5, 3, -90), acc); c.polygon(ngon(cx, cy + 2, 2.6, 3, -90), 0xFFFFE2A0); }
                else outline(c, ngon(cx, cy + 1, 6.5, 3, -90), off);
            }
            case STRATA -> {
                for (int i = 0; i < 3; i++) c.fill(cx - 6 + i, cy - 5 + i * 4, 12 - i * 2, 2, on ? (i == 1 ? s.accent2() : acc) : off);
            }
            case CHECK -> {
                framed(c, cx - 5, cy - 5, 10, 10, 0, s.field(), on ? acc : off);
                if (on) { c.line(cx - 3, cy, cx - 1, cy + 3, 1.6, acc); c.line(cx - 1, cy + 3, cx + 4, cy - 3, 1.6, acc); }
            }
            case TARGET, RADIO -> {
                ring(c, cx, cy, 5.5, on ? acc : off);
                if (s.toggle() == Style.Toggle.TARGET) { c.fill(cx - 7, cy, 3, 1, on ? acc : off); c.fill(cx + 5, cy, 3, 1, on ? acc : off); c.fill(cx, cy - 7, 1, 3, on ? acc : off); c.fill(cx, cy + 5, 1, 3, on ? acc : off); }
                if (on) c.circle(cx + .5, cy + .5, 2.5, acc);
            }
            case PLANET -> {
                if (on) c.circle(cx + .5, cy + .5, 7, alpha(acc, (int) (50 * breathe)));
                c.circle(cx + .5, cy + .5, 4, on ? acc : off);
                c.line(cx - 7, cy + 3, cx + 8, cy - 2, 1, on ? s.accent2() : off);
            }
            default -> {
                if (on) {
                    c.circle(cx + .5, cy + .5, 8, alpha(acc, (int) (60 * breathe)));
                    c.circle(cx + .5, cy + .5, 5.5, acc);
                    c.circle(cx - 1.5, cy - 1.5, 1.6, alpha(white, 220));
                } else { c.circle(cx + .5, cy + .5, 5.5, off); c.circle(cx + .5, cy + .5, 4.5, opaque(s.field(), 0xFF)); }
            }
        }
    }

    static void ring(Canvas c, double cx, double cy, double r, int argb) {
        outline(c, ngon(cx + .5, cy + .5, r, 16, 0), argb);
    }

    static void outline(Canvas c, double[] p, int argb) {
        for (int i = 0; i < p.length; i += 2) {
            int j = (i + 2) % p.length;
            c.line(p[i], p[i + 1], p[j], p[j + 1], 1, argb);
        }
    }

    /** Regular polygon, clockwise on screen, first vertex at the given angle. */
    static double[] ngon(double cx, double cy, double r, int n, double startDegrees) {
        double[] p = new double[n * 2];
        for (int i = 0; i < n; i++) {
            double a = Math.toRadians(startDegrees + 360.0 * i / n);
            p[i * 2] = cx + Math.cos(a) * r;
            p[i * 2 + 1] = cy + Math.sin(a) * r;
        }
        return p;
    }

    static String fit(Canvas c, String text, int width) {
        if (width <= 0) return "";
        if (c.textWidth(text) <= width) return text;
        int n = text.length();
        while (n > 0 && c.textWidth(text.substring(0, n) + "..") > width) n--;
        return n == 0 ? "" : text.substring(0, n) + "..";
    }

    static int alpha(int argb, int alpha) {
        return Math.max(0, Math.min(255, alpha)) << 24 | argb & 0xFFFFFF;
    }

    static int opaque(int argb, int minAlpha) {
        return Math.max(argb >>> 24, minAlpha) << 24 | argb & 0xFFFFFF;
    }

    static int mix(int a, int b, double f) {
        f = Math.max(0, Math.min(1, f));
        int out = 0;
        for (int shift = 0; shift <= 24; shift += 8) {
            int x = a >>> shift & 255, y = b >>> shift & 255;
            out |= (int) Math.round(x + (y - x) * f) << shift;
        }
        return out;
    }
}
