package aicoder.glasscolumns.gui;

import aicoder.glasscolumns.mixin.GuiRendererAccessor;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;

/**
 * Drawing primitives shared by all ten themes. Meteor's renderer only has quads, triangles, text and
 * textures, so rounded corners, circles and curves are assembled from triangles here.
 *
 * <p>Sizes in the mockups are "design pixels" (an 840x480 window); one design pixel is {@code unit}
 * framebuffer pixels, see {@link GlassGuiTheme#unit()}.
 */
public final class Fx {
    private Fx() {
    }

    /** Approximate em size, in framebuffer pixels, of Meteor text rendered at scale 1. */
    private static final double EM = 15;

    // Animation clock

    /** Position inside a looping period of {@code seconds}, 0..1. */
    public static double phase(double seconds) {
        long ms = (long) (seconds * 1000);
        return (System.currentTimeMillis() % ms) / (double) ms;
    }

    /** Irregular but perfectly looping motion: three sines at integer multiples of the loop frequency. */
    public static double flow(double p, double s) {
        final double tau = Math.PI * 2;
        return 0.54 * Math.sin(tau * (p + s))
            + 0.30 * Math.sin(tau * (2 * p + s * 1.7) + 1.1)
            + 0.16 * Math.sin(tau * (3 * p - s * 0.6) + 2.3);
    }

    public static double clamp01(double v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }

    /**
     * Whether text goes through Minecraft's own font instead of Meteor's custom font. Only the vanilla font
     * has CJK glyphs, and it is drawn by the game's GUI pass on top of all of Meteor's geometry.
     */
    public static boolean vanillaText() {
        return !Config.get().customFont.get();
    }

    // Scratch colors. The renderer copies color values into its mesh immediately, so these can be reused.

    private static final Color[] SCRATCH = new Color[64];
    private static int scratchI;

    static {
        for (int i = 0; i < SCRATCH.length; i++) SCRATCH[i] = new Color();
    }

    public static Color rgba(int r, int g, int b, double a) {
        Color c = SCRATCH[scratchI++ & 63];
        c.r = r;
        c.g = g;
        c.b = b;
        c.a = (int) Math.round(clamp01(a) * 255);
        return c;
    }

    /** {@code base} with its alpha multiplied by {@code alpha}. */
    public static Color a(Color base, double alpha) {
        return rgba(base.r, base.g, base.b, base.a / 255.0 * alpha);
    }

    public static Color mix(Color c1, Color c2, double t) {
        t = clamp01(t);
        Color c = SCRATCH[scratchI++ & 63];
        c.r = (int) Math.round(c1.r + (c2.r - c1.r) * t);
        c.g = (int) Math.round(c1.g + (c2.g - c1.g) * t);
        c.b = (int) Math.round(c1.b + (c2.b - c1.b) * t);
        c.a = (int) Math.round(c1.a + (c2.a - c1.a) * t);
        return c;
    }

    // Lines and triangles

    /** Triangle with the same winding as the renderer's quads, whatever order the points come in. */
    public static void tri(GuiRenderer r, double x1, double y1, double x2, double y2, double x3, double y3, Color c) {
        double cross = (x2 - x1) * (y3 - y1) - (y2 - y1) * (x3 - x1);
        if (cross > 0) r.triangle(x1, y1, x3, y3, x2, y2, c);
        else r.triangle(x1, y1, x2, y2, x3, y3, c);
    }

    public static void line(GuiRenderer r, double x1, double y1, double x2, double y2, double thickness, Color c) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len < 0.001) return;

        if (Math.abs(dy) < 0.001) {
            r.quad(Math.min(x1, x2), y1 - thickness / 2, len, thickness, c);
            return;
        }
        if (Math.abs(dx) < 0.001) {
            r.quad(x1 - thickness / 2, Math.min(y1, y2), thickness, len, c);
            return;
        }

        double nx = -dy / len * thickness / 2;
        double ny = dx / len * thickness / 2;
        tri(r, x1 + nx, y1 + ny, x1 - nx, y1 - ny, x2 - nx, y2 - ny, c);
        tri(r, x1 + nx, y1 + ny, x2 - nx, y2 - ny, x2 + nx, y2 + ny, c);
    }

    /** A line with a soft halo, approximated with wider translucent copies underneath. */
    public static void glowLine(GuiRenderer r, double x1, double y1, double x2, double y2, double thickness, Color c, double glow, double alpha) {
        for (int i = 3; i >= 1; i--) {
            line(r, x1, y1, x2, y2, thickness + glow * i * 2 / 3, a(c, alpha * 0.13));
        }
        line(r, x1, y1, x2, y2, thickness, a(c, alpha));
    }

    /** Soft glow around a rectangle: gradient skirts that fade from {@code c} to transparent. */
    public static void glowRect(GuiRenderer r, double x, double y, double w, double h, double spread, Color c) {
        Color z = a(c, 0);
        r.quad(x, y - spread, w, spread, z, z, c, c);
        r.quad(x, y + h, w, spread, c, c, z, z);
        r.quad(x - spread, y, spread, h, z, c, c, z);
        r.quad(x + w, y, spread, h, c, z, z, c);
        r.quad(x - spread, y - spread, spread, spread, z, z, c, z);
        r.quad(x + w, y - spread, spread, spread, z, z, z, c);
        r.quad(x + w, y + h, spread, spread, c, z, z, z);
        r.quad(x - spread, y + h, spread, spread, z, c, z, z);
    }

    /** 1px in the design, never thinner than one real pixel. */
    public static double hair(double unit) {
        return Math.max(1, Math.round(unit));
    }

    // Boxes. A positive corner is a radius, a negative one a 45 degree cut, zero a plain rectangle.

    private static final double[] INSET = new double[16];
    private static final double[] DROP = new double[16];

    /** Fills INSET/DROP with the corner profile, from the outer edge inwards, and returns the step count. */
    private static int profile(double corner, double w, double h) {
        double size = Math.min(Math.abs(corner), Math.min(w, h) / 2);
        if (size < 0.75) return 0;

        if (corner < 0) {
            INSET[0] = size;
            DROP[0] = 0;
            INSET[1] = 0;
            DROP[1] = size;
            return 1;
        }

        int n = (int) Math.max(3, Math.min(10, Math.ceil(size / 2.5)));
        for (int i = 0; i <= n; i++) {
            double ang = Math.PI / 2 * (1 - (double) i / n);
            INSET[i] = size * (1 - Math.cos(ang));
            DROP[i] = size * (1 - Math.sin(ang));
        }
        return n;
    }

    // Gradient state for the box being drawn. Kept outside the scratch ring, which a rounded box would overrun.
    private static final double[][] GRAD = new double[4][4];
    private static final Color G1 = new Color(), G2 = new Color(), G3 = new Color(), G4 = new Color();

    private static void grad(Color tl, Color tr, Color br, Color bl) {
        Color[] cs = {tl, tr, br, bl};
        for (int i = 0; i < 4; i++) {
            GRAD[i][0] = cs[i].r;
            GRAD[i][1] = cs[i].g;
            GRAD[i][2] = cs[i].b;
            GRAD[i][3] = cs[i].a;
        }
    }

    /** Bilinear sample of the current gradient into {@code out}; fx and fy run 0..1 across the box. */
    private static Color at(Color out, double fx, double fy) {
        fx = clamp01(fx);
        fy = clamp01(fy);
        out.r = (int) Math.round(sample(0, fx, fy));
        out.g = (int) Math.round(sample(1, fx, fy));
        out.b = (int) Math.round(sample(2, fx, fy));
        out.a = (int) Math.round(sample(3, fx, fy));
        return out;
    }

    private static double sample(int k, double fx, double fy) {
        double top = GRAD[0][k] + (GRAD[1][k] - GRAD[0][k]) * fx;
        double bottom = GRAD[3][k] + (GRAD[2][k] - GRAD[3][k]) * fx;
        return top + (bottom - top) * fy;
    }

    /** Filled box with a color in each corner (top-left, top-right, bottom-right, bottom-left). */
    public static void box(GuiRenderer r, double x, double y, double w, double h, double corner, Color tl, Color tr, Color br, Color bl) {
        if (w <= 0 || h <= 0) return;
        grad(tl, tr, br, bl);

        int n = profile(corner, w, h);
        if (n == 0) {
            r.quad(x, y, w, h, at(G1, 0, 0), at(G2, 1, 0), at(G3, 1, 1), at(G4, 0, 1));
            return;
        }

        double size = DROP[n];
        for (int i = 0; i < n; i++) {
            double in0 = INSET[i], in1 = INSET[i + 1];
            double d0 = DROP[i], d1 = DROP[i + 1];
            double fl = in0 / w, fr = 1 - in0 / w, fl1 = in1 / w, fr1 = 1 - in1 / w;

            // Top band
            double f0 = d0 / h, f1 = d1 / h;
            r.quad(x + in0, y + d0, w - in0 * 2, d1 - d0, at(G1, fl, f0), at(G2, fr, f0), at(G3, fr, f1), at(G4, fl, f1));
            tri(r, x + in0, y + d0, x + in1, y + d1, x + in0, y + d1, at(G1, fl1, f1));
            tri(r, x + w - in0, y + d0, x + w - in1, y + d1, x + w - in0, y + d1, at(G1, fr1, f1));

            // Bottom band
            double b0 = 1 - d0 / h, b1 = 1 - d1 / h;
            r.quad(x + in0, y + h - d1, w - in0 * 2, d1 - d0, at(G1, fl, b1), at(G2, fr, b1), at(G3, fr, b0), at(G4, fl, b0));
            tri(r, x + in0, y + h - d0, x + in1, y + h - d1, x + in0, y + h - d1, at(G1, fl1, b1));
            tri(r, x + w - in0, y + h - d0, x + w - in1, y + h - d1, x + w - in0, y + h - d1, at(G1, fr1, b1));
        }

        double m0 = size / h, m1 = 1 - size / h;
        r.quad(x, y + size, w, h - size * 2, at(G1, 0, m0), at(G2, 1, m0), at(G3, 1, m1), at(G4, 0, m1));
    }

    /** Filled box with a top-to-bottom gradient. */
    public static void box(GuiRenderer r, double x, double y, double w, double h, double corner, Color top, Color bottom) {
        box(r, x, y, w, h, corner, top, top, bottom, bottom);
    }

    public static void box(GuiRenderer r, double x, double y, double w, double h, double corner, Color c) {
        box(r, x, y, w, h, corner, c, c, c, c);
    }

    public static void boxOutline(GuiRenderer r, double x, double y, double w, double h, double corner, double t, Color c) {
        boxOutline(r, x, y, w, h, corner, t, c, c);
    }

    /**
     * Outline matching {@link #box}, fading from {@code top} to {@code bottom}. Drawn as a strip between the
     * outer shape and one inset by {@code t}, so translucent outlines have no bright joints.
     */
    public static void boxOutline(GuiRenderer r, double x, double y, double w, double h, double corner, double t, Color top, Color bottom) {
        if (w <= 0 || h <= 0) return;
        grad(top, top, bottom, bottom);

        int n = profile(corner, w, h);
        double size = n == 0 ? 0 : DROP[n];
        double edge = Math.max(size, t);
        double fe = edge / h;

        r.quad(x + edge, y, w - edge * 2, t, at(G1, 0, 0));
        r.quad(x + edge, y + h - t, w - edge * 2, t, at(G1, 0, 1));
        r.quad(x, y + edge, t, h - edge * 2, at(G1, 0, fe), at(G2, 0, fe), at(G3, 0, 1 - fe), at(G4, 0, 1 - fe));
        r.quad(x + w - t, y + edge, t, h - edge * 2, at(G1, 0, fe), at(G2, 0, fe), at(G3, 0, 1 - fe), at(G4, 0, 1 - fe));

        if (n == 0) {
            r.quad(x, y, t, t, at(G1, 0, 0));
            r.quad(x + w - t, y, t, t, at(G1, 0, 0));
            r.quad(x, y + h - t, t, t, at(G1, 0, 1));
            r.quad(x + w - t, y + h - t, t, t, at(G1, 0, 1));
            return;
        }

        // Inner profile: same shape, shrunk by t and pushed in by t
        boolean solid = size <= t;
        double k = corner < 0 ? 1 : (size - t) / size;
        double shift = t * 0.41;
        for (int i = 0; i < n; i++) {
            double ii0, id0, ii1, id1;
            if (solid) {
                ii0 = id0 = ii1 = id1 = size;
            } else if (corner < 0) {
                ii0 = size + shift;
                id0 = t;
                ii1 = t;
                id1 = size + shift;
            } else {
                ii0 = t + INSET[i] * k;
                id0 = t + DROP[i] * k;
                ii1 = t + INSET[i + 1] * k;
                id1 = t + DROP[i + 1] * k;
            }

            double f = (DROP[i] + DROP[i + 1]) / 2 / h;
            for (int c = 0; c < 4; c++) {
                boolean right = (c & 1) != 0, bottom2 = (c & 2) != 0;
                Color col = at(G1, 0, bottom2 ? 1 - f : f);
                double ax = right ? x + w - INSET[i] : x + INSET[i], ay = bottom2 ? y + h - DROP[i] : y + DROP[i];
                double bx = right ? x + w - INSET[i + 1] : x + INSET[i + 1], by = bottom2 ? y + h - DROP[i + 1] : y + DROP[i + 1];
                double cx = right ? x + w - ii1 : x + ii1, cy = bottom2 ? y + h - id1 : y + id1;
                double dx = right ? x + w - ii0 : x + ii0, dy = bottom2 ? y + h - id0 : y + id0;
                tri(r, ax, ay, bx, by, cx, cy, col);
                tri(r, ax, ay, cx, cy, dx, dy, col);
            }
        }
    }

    // Round shapes

    private static final int STEPS = 48;
    private static final double[] COS = new double[STEPS + 1];
    private static final double[] SIN = new double[STEPS + 1];

    static {
        for (int i = 0; i <= STEPS; i++) {
            double ang = Math.PI * 2 * i / STEPS;
            COS[i] = Math.cos(ang);
            SIN[i] = Math.sin(ang);
        }
    }

    private static int stride(double radius) {
        return radius < 7 ? 4 : (radius < 26 ? 2 : 1);
    }

    /** Filled ellipse, rotated by {@code rot} radians. */
    public static void ellipse(GuiRenderer r, double cx, double cy, double rx, double ry, double rot, Color c) {
        double cr = Math.cos(rot), sr = Math.sin(rot);
        int step = stride(Math.max(rx, ry));
        double px = cx + rx * cr, py = cy + rx * sr;

        for (int i = step; i <= STEPS; i += step) {
            double ex = COS[i] * rx, ey = SIN[i] * ry;
            double nx = cx + ex * cr - ey * sr, ny = cy + ex * sr + ey * cr;
            tri(r, cx, cy, px, py, nx, ny, c);
            px = nx;
            py = ny;
        }
    }

    /** Ellipse outline of thickness {@code t}, rotated by {@code rot} radians. Drawn as a strip, so it has no bright joints. */
    public static void ellipseRing(GuiRenderer r, double cx, double cy, double rx, double ry, double rot, double t, Color c) {
        double cr = Math.cos(rot), sr = Math.sin(rot);
        int step = stride(Math.max(rx, ry));
        double ox = rx + t / 2, oy = ry + t / 2;
        double ix = Math.max(0, rx - t / 2), iy = Math.max(0, ry - t / 2);

        double pox = cx + ox * cr, poy = cy + ox * sr;
        double pix = cx + ix * cr, piy = cy + ix * sr;

        for (int i = step; i <= STEPS; i += step) {
            double ex = COS[i] * ox, ey = SIN[i] * oy;
            double nox = cx + ex * cr - ey * sr, noy = cy + ex * sr + ey * cr;
            ex = COS[i] * ix;
            ey = SIN[i] * iy;
            double nix = cx + ex * cr - ey * sr, niy = cy + ex * sr + ey * cr;

            tri(r, pox, poy, nox, noy, nix, niy, c);
            tri(r, pox, poy, nix, niy, pix, piy, c);

            pox = nox;
            poy = noy;
            pix = nix;
            piy = niy;
        }
    }

    public static void circle(GuiRenderer r, double cx, double cy, double radius, Color c) {
        ellipse(r, cx, cy, radius, radius, 0, c);
    }

    public static void ring(GuiRenderer r, double cx, double cy, double radius, double t, Color c) {
        ellipseRing(r, cx, cy, radius, radius, 0, t, c);
    }

    /** A disc with a soft halo of {@code spread} around it. */
    public static void glowDot(GuiRenderer r, double cx, double cy, double radius, double spread, Color c, double alpha) {
        for (int i = 4; i >= 1; i--) {
            circle(r, cx, cy, radius + spread * i / 4, a(c, alpha * 0.11));
        }
    }

    /** A large, smooth glow for backdrops: many faint discs, so no single edge shows as a band. */
    public static void halo(GuiRenderer r, double cx, double cy, double inner, double outer, Color c, double alpha) {
        int steps = 18;
        for (int i = steps; i >= 1; i--) {
            circle(r, cx, cy, inner + (outer - inner) * i / steps, a(c, alpha / steps * 1.6));
        }
    }

    /** Regular polygon with {@code sides} corners, the first one pointing up. */
    public static void polygon(GuiRenderer r, double cx, double cy, double radius, int sides, Color c) {
        double px = cx, py = cy - radius;
        for (int i = 1; i <= sides; i++) {
            double ang = Math.PI * 2 * i / sides - Math.PI / 2;
            double nx = cx + Math.cos(ang) * radius, ny = cy + Math.sin(ang) * radius;
            tri(r, cx, cy, px, py, nx, ny, c);
            px = nx;
            py = ny;
        }
    }

    public static void polygonOutline(GuiRenderer r, double cx, double cy, double radius, int sides, double t, Color c) {
        double inner = Math.max(0, radius - t / Math.cos(Math.PI / sides));
        double px = 0, py = -1;

        for (int i = 1; i <= sides; i++) {
            double ang = Math.PI * 2 * i / sides - Math.PI / 2;
            double nx = Math.cos(ang), ny = Math.sin(ang);
            tri(r, cx + px * radius, cy + py * radius, cx + nx * radius, cy + ny * radius, cx + nx * inner, cy + ny * inner, c);
            tri(r, cx + px * radius, cy + py * radius, cx + nx * inner, cy + ny * inner, cx + px * inner, cy + py * inner, c);
            px = nx;
            py = ny;
        }
    }

    /**
     * A closed, slightly irregular curve such as a contour line or an isobar. {@code seed} picks the shape,
     * {@code drift} (0..1, looping) makes it breathe.
     */
    public static void contour(GuiRenderer r, double cx, double cy, double radius, double squash, double seed, double wobble, double drift, double t, Color c) {
        double tau = Math.PI * 2;
        double px = 0, py = 0;

        for (int i = 0; i <= STEPS; i++) {
            double ang = tau * i / STEPS;
            double k = 1 + wobble * (0.5 * Math.sin(2 * ang + seed * 7.1)
                + 0.3 * Math.sin(3 * ang + seed * 13.7 + tau * drift)
                + 0.2 * Math.sin(5 * ang + seed * 3.3 - tau * drift));
            double nx = cx + COS[i] * radius * k;
            double ny = cy + SIN[i] * radius * k * squash;

            if (i > 0) line(r, px, py, nx, ny, t, c);
            px = nx;
            py = ny;
        }
    }

    // Occluders: where windows were last frame, so vanilla-font backdrop text can stay out from under them

    private static List<double[]> occluders = new ArrayList<>();
    private static List<double[]> lastOccluders = new ArrayList<>();

    public static void occlude(double x, double y, double w, double h) {
        occluders.add(new double[]{x, y, x + w, y + h});
    }

    static void nextFrame() {
        List<double[]> swap = lastOccluders;
        lastOccluders = occluders;
        occluders = swap;
        occluders.clear();
    }

    static boolean occluded(double x, double y) {
        for (double[] o : lastOccluders) {
            if (x >= o[0] && x <= o[2] && y >= o[1] && y <= o[3]) return true;
        }
        return false;
    }

    // Text

    public static final int CENTER = 1;
    public static final int RIGHT = 2;
    public static final int BOLD = 4;

    public static double textScale(double size, double unit) {
        return size * unit / EM;
    }

    /** Width in framebuffer pixels of {@code text} at {@code size} design pixels with {@code spacing} design pixels of tracking. */
    public static double textWidth(GuiTheme theme, String text, double size, double spacing, double unit) {
        return theme.textRenderer().getWidth(text) * textScale(size, unit) + spacing * unit * text.length();
    }

    public static double lineHeight(GuiTheme theme, double size, double unit) {
        return theme.textRenderer().getHeight() * textScale(size, unit);
    }

    /** Top y that vertically centers a line of {@code size} text on {@code cy}. */
    public static double centerY(GuiTheme theme, double cy, double size, double unit) {
        return cy - lineHeight(theme, size, unit) / 2;
    }

    private static final List<TextBatch> BATCH_POOL = new ArrayList<>();
    private static final String[] ASCII = new String[128];

    static {
        for (int i = 0; i < ASCII.length; i++) ASCII[i] = String.valueOf((char) i);
    }

    private static String ch(char c) {
        return c < 128 ? ASCII[c] : String.valueOf(c);
    }

    /**
     * Starts a batch of free-size text. The batch is drawn when the scissor that is current right now ends,
     * on top of that scissor's geometry, so call this from inside the widget that owns the text.
     */
    public static TextBatch text(GuiRenderer renderer, double unit) {
        TextBatch batch = BATCH_POOL.isEmpty() ? new TextBatch() : BATCH_POOL.removeLast();
        batch.renderer = renderer;
        batch.unit = unit;
        renderer.post(batch);
        return batch;
    }

    public static final class TextBatch implements Runnable {
        private static final class Op {
            String text;
            double x, y, scale, spacing;
            int r, g, b, a, flags;
            boolean done;
        }

        private final List<Op> ops = new ArrayList<>();
        private final Color color = new Color();
        private int count;
        private GuiRenderer renderer;
        private double unit;

        public TextBatch put(String text, double x, double y, double size, Color c) {
            return put(text, x, y, size, c, 0, 0);
        }

        /**
         * @param size    font size in design pixels
         * @param spacing letter spacing in design pixels
         * @param flags   {@link Fx#CENTER}, {@link Fx#RIGHT}, {@link Fx#BOLD}
         */
        public TextBatch put(String text, double x, double y, double size, Color c, double spacing, int flags) {
            if (text == null || text.isEmpty() || c.a <= 0) return this;

            if (count == ops.size()) ops.add(new Op());
            Op op = ops.get(count++);
            op.text = text;
            op.x = x;
            op.y = y;
            op.scale = textScale(size, unit);
            op.spacing = spacing * unit;
            op.r = c.r;
            op.g = c.g;
            op.b = c.b;
            op.a = c.a;
            op.flags = flags;
            op.done = false;
            return this;
        }

        @Override
        public void run() {
            try {
                if (count > 0) flush();
            } finally {
                count = 0;
                renderer = null;
                BATCH_POOL.add(this);
            }
        }

        private void flush() {
            TextRenderer tr = renderer.theme.textRenderer();
            if (tr.isBuilding()) return;

            GuiGraphicsExtractor graphics = ((GuiRendererAccessor) renderer).glasscol$getGraphics();

            for (int i = 0; i < count; i++) {
                Op first = ops.get(i);
                if (first.done) continue;

                tr.begin(graphics, first.scale);
                for (int j = i; j < count; j++) {
                    Op op = ops.get(j);
                    if (!op.done && Math.abs(op.scale - first.scale) < 0.0005) {
                        draw(tr, op);
                        op.done = true;
                    }
                }
                tr.end();
            }
        }

        private void draw(TextRenderer tr, Op op) {
            color.r = op.r;
            color.g = op.g;
            color.b = op.b;
            color.a = op.a;

            String text = op.text;
            boolean bold = (op.flags & BOLD) != 0;
            // Vanilla: one font pixel, like Minecraft's own bold. Custom font: a slight thickening.
            double boldOffset = vanillaText() ? op.scale * 2 : Math.max(0.5, op.scale * 0.55);
            double x = op.x;

            if ((op.flags & (CENTER | RIGHT)) != 0) {
                double width = tr.getWidth(text) + op.spacing * text.length();
                x -= (op.flags & CENTER) != 0 ? width / 2 : width;
            }

            if (op.spacing == 0) {
                tr.render(text, x, op.y, color);
                if (bold) tr.render(text, x + boldOffset, op.y, color);
                return;
            }

            for (int i = 0; i < text.length(); i++) {
                String c = ch(text.charAt(i));
                if (text.charAt(i) != ' ') {
                    tr.render(c, x, op.y, color);
                    if (bold) tr.render(c, x + boldOffset, op.y, color);
                }
                x += tr.getWidth(c) + op.spacing;
            }
        }
    }
}
