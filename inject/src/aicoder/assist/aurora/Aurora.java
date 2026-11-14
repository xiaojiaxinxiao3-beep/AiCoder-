package aicoder.assist.aurora;

import aicoder.assist.gfx.GuiRenderer;
import aicoder.assist.gfx.Color;

/**
 * The night sky behind the GUI: three slow aurora ribbons, a curtain of faint vertical streaks, stars and a
 * vignette. Everything is a function of position in a 1920x1080 design space stretched over the screen, so
 * the glass panes can sample the same aurora (wider and more saturated) to look like they blur what is behind them.
 */
public final class Aurora {
    private Aurora() {
    }

    // Ribbons: centre x, centre y, half length, half thickness, tilt in degrees, drift period in seconds, drift distance, phase offset
    private static final double[][] RIBBONS = {
        {485, 225, 625, 165, -9, 18, 70, 0.00},
        {1270, 110, 750, 150, 7, 24, 80, 0.37},
        {985, 450, 825, 120, -3, 30, 90, 0.71},
    };

    // Color stops along each ribbon: position, r, g, b, alpha
    private static final double[][][] STOPS = {
        {{0.04, 77, 255, 184, 0}, {0.30, 77, 255, 184, 0.85}, {0.56, 47, 230, 224, 0.70}, {0.92, 47, 230, 224, 0}},
        {{0.06, 143, 123, 255, 0}, {0.34, 143, 123, 255, 0.75}, {0.60, 255, 111, 216, 0.50}, {0.90, 255, 111, 216, 0}},
        {{0.00, 47, 230, 224, 0}, {0.28, 47, 230, 224, 0.50}, {0.52, 77, 255, 184, 0.62}, {0.76, 143, 123, 255, 0.40}, {1.00, 143, 123, 255, 0}},
    };

    private static final double[] COS = new double[3], SIN = new double[3];
    private static final double[] shift = new double[3], gain = new double[3];

    private static final int STAR_COUNT = 420;
    private static final float[] STARS = new float[STAR_COUNT * 3];

    static {
        for (int i = 0; i < 3; i++) {
            double rad = Math.toRadians(RIBBONS[i][4]);
            COS[i] = Math.cos(rad);
            SIN[i] = Math.sin(rad);
            gain[i] = 1;
        }

        // Same generator and seed as the mockup, so the stars land where the design has them
        long s = 7;
        for (int i = 0; i < STAR_COUNT * 3; i++) {
            s = (s * 16807) % 2147483647L;
            STARS[i] = (float) (s / 2147483647.0);
        }
    }

    private static double sw = 1920, sh = 1080;
    private static double dim = 0.78;
    private static boolean translucent = true;

    /** Call once per frame before anything samples the sky. */
    public static void frame(double screenW, double screenH, double backdrop, boolean blur) {
        sw = Math.max(1, screenW);
        sh = Math.max(1, screenH);
        dim = backdrop;
        translucent = blur;

        for (int i = 0; i < 3; i++) {
            double p = Fx.phase(RIBBONS[i][5]) + RIBBONS[i][7];
            shift[i] = RIBBONS[i][6] * Math.sin(Math.PI * 2 * p);
            gain[i] = 0.9 + 0.1 * Math.sin(Math.PI * 2 * (p * 2 + 0.2));
        }
    }

    // Sampling. Results are left in these fields, 0..1.
    private static double cr, cg, cb, lit;

    /** Screens the ribbons onto cr/cg/cb. {@code spread} above 1 widens them, as a blur would. */
    private static void ribbons(double x, double y, double spread) {
        double keep = 1;

        for (int i = 0; i < 3; i++) {
            double[] r = RIBBONS[i];
            double dx = x - r[0] - shift[i], dy = y - r[1];
            double du = (dx * COS[i] + dy * SIN[i]) / (r[2] * (0.9 + 0.1 * spread));
            double dv = (-dx * SIN[i] + dy * COS[i]) / (r[3] * spread);
            if (du < -1 || du > 1 || dv < -2.6 || dv > 2.6) continue;

            double t = (du + 1) / 2;
            double[][] stops = STOPS[i];
            if (t <= stops[0][0] || t >= stops[stops.length - 1][0]) continue;

            int k = 1;
            while (k < stops.length - 1 && t > stops[k][0]) k++;
            double[] a = stops[k - 1], b = stops[k];
            double f = (t - a[0]) / (b[0] - a[0]);
            f = f * f * (3 - 2 * f);

            double alpha = (a[4] + (b[4] - a[4]) * f) * Math.exp(-0.85 * dv * dv) * gain[i];
            if (alpha <= 0.002) continue;

            cr = 1 - (1 - cr) * (1 - (a[1] + (b[1] - a[1]) * f) / 255 * alpha);
            cg = 1 - (1 - cg) * (1 - (a[2] + (b[2] - a[2]) * f) / 255 * alpha);
            cb = 1 - (1 - cb) * (1 - (a[3] + (b[3] - a[3]) * f) / 255 * alpha);
            keep *= 1 - alpha;
        }

        lit = 1 - keep;
    }

    /** The full-screen layer: night sky, ribbons and vignette, more opaque where the aurora or the vignette is. */
    public static final Fx.Paint SKY = (px, py) -> {
        double x = px / sw * 1920, y = py / sh * 1080;
        double t = y / 1080;

        if (t < 0.42) sky(2, 5, 16, 6, 18, 42, t / 0.42);
        else if (t < 0.72) sky(6, 18, 42, 11, 32, 56, (t - 0.42) / 0.30);
        else sky(11, 32, 56, 14, 42, 60, (t - 0.72) / 0.28);

        ribbons(x, y, 1);

        double ex = (x - 960) / 1300, ey = (y - 497) / 760;
        double v = Fx.clamp01((Math.sqrt(ex * ex + ey * ey) - 0.52) / 0.48) * 0.72;
        cr += (1 / 255.0 - cr) * v;
        cg += (3 / 255.0 - cg) * v;
        cb += (10 / 255.0 - cb) * v;

        double alpha = dim + (1 - dim) * Math.max(lit * 0.9, v);
        return Fx.rgba((int) Math.round(cr * 255), (int) Math.round(cg * 255), (int) Math.round(cb * 255), alpha);
    };

    private static void sky(double r1, double g1, double b1, double r2, double g2, double b2, double f) {
        cr = (r1 + (r2 - r1) * f) / 255;
        cg = (g1 + (g2 - g1) * f) / 255;
        cb = (b1 + (b2 - b1) * f) / 255;
    }

    /**
     * What a pane of glass shows of the sky behind it: the ribbons spread out and pushed in saturation, over
     * deep blue. With blur off it is nearly opaque, so the pane stays readable over the sharp game image.
     */
    public static final Fx.Paint GLASS = (px, py) -> {
        double x = px / sw * 1920, y = py / sh * 1080;
        cr = 12 / 255.0;
        cg = 29 / 255.0;
        cb = 58 / 255.0;
        ribbons(x, y, 1.7);

        double mean = (cr + cg + cb) / 3;
        double r = Fx.clamp01(mean + (cr - mean) * 1.45);
        double g = Fx.clamp01(mean + (cg - mean) * 1.45);
        double b = Fx.clamp01(mean + (cb - mean) * 1.45);

        double alpha = translucent ? 0.34 + 0.5 * lit : 0.62 + 0.3 * lit;
        return Fx.rgba((int) Math.round(r * 255), (int) Math.round(g * 255), (int) Math.round(b * 255), alpha);
    };

    /** Draws the sky over the whole screen. The caller wraps it in its own scissor so it lands under the GUI. */
    public static void render(GuiRenderer r) {
        double k = sw / 1920;

        Fx.boxPaint(r, 0, 0, sw, sh, 0, sw / 40, SKY);

        // Curtain: thin streaks hanging through the ribbons, sliding sideways very slowly
        double period = 66 * k;
        double slide = Fx.phase(60) * period;
        double y0 = sh * 0.02, y1 = sh * 0.18, y2 = sh * 0.46, y3 = sh * 0.66;
        for (double x = -period + slide; x < sw; x += period) {
            streak(r, x + 22 * k, 4 * k, y0, y1, y2, y3, 120, 255, 214, 0.04);
            streak(r, x + 61 * k, 5 * k, y0, y1, y2, y3, 170, 150, 255, 0.034);
        }

        // Stars over the upper part of the sky, left out where a pane sits so the glass reads as blurred
        double unit = Math.max(1, k);
        for (int i = 0; i < STAR_COUNT; i++) {
            double x = STARS[i * 3] * sw, y = STARS[i * 3 + 1] * sh * 0.59, z = STARS[i * 3 + 2];
            if (Fx.occluded(x, y)) continue;

            double size = Math.max(1, Math.round((1 + z * 2.2) * unit * 0.8));
            r.quad(Math.round(x), Math.round(y), size, size, Fx.rgba(255, 255, 255, (0.25 + z * 0.7) * (size > 1 ? 0.75 : 1)));
        }
    }

    private static void streak(GuiRenderer r, double x, double w, double y0, double y1, double y2, double y3, int cr, int cg, int cb, double alpha) {
        Color none = Fx.rgba(cr, cg, cb, 0);
        Color full = Fx.rgba(cr, cg, cb, alpha);
        r.quad(x, y0, w, y1 - y0, none, none, full, full);
        r.quad(x, y1, w, y2 - y1, full);
        r.quad(x, y2, w, y3 - y2, full, full, none, none);
    }
}
