package aicoder.columns.gui;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.utils.render.color.Color;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared palette, animation clock and drawing primitives for the cyberpunk theme.
 *
 * <p>Sizes in the design are expressed in "design pixels" (the 840x480 window of the mockup).
 * One design pixel is {@code unit} framebuffer pixels; see {@link AiCoderGuiTheme#unit()}.
 */
public final class Cyber {
    private Cyber() {
    }

    // Palette (magenta + cyan only, purple is reserved for the backdrop lights)

    public static final Color CYAN = new Color(0, 240, 255);
    public static final Color CYAN_LINE = new Color(0, 229, 255);
    public static final Color MAGENTA = new Color(255, 42, 109);
    public static final Color PURPLE = new Color(176, 38, 255);
    public static final Color WHITE = new Color(234, 246, 255);
    public static final Color PURE_WHITE = new Color(255, 255, 255);
    public static final Color DIM = new Color(111, 138, 166);
    public static final Color TEXT = new Color(183, 201, 220);
    public static final Color TEXT_IDLE = new Color(127, 155, 184);
    public static final Color TEXT_FAINT = new Color(79, 106, 134);
    public static final Color FIELD = new Color(0, 30, 50);
    public static final Color SLATE = new Color(60, 110, 160);

    /** Alpha of the 1px separator lines, {@code rgba(0,229,255,.22)}. */
    public static final double LINE_ALPHA = 0.22;

    /** Approximate em size, in framebuffer pixels, of Meteor text rendered at scale 1. */
    private static final double EM = 15;

    // Animation clock

    /** Position inside the 4 second loop, 0..1. */
    public static double phase() {
        return (System.currentTimeMillis() % 4000L) / 4000.0;
    }

    /** Irregular but perfectly looping motion: three sines at integer multiples of the loop frequency. */
    public static double flow(double p, double s) {
        final double tau = Math.PI * 2;
        return 0.54 * Math.sin(tau * (p + s))
            + 0.30 * Math.sin(tau * (2 * p + s * 1.7) + 1.1)
            + 0.16 * Math.sin(tau * (3 * p - s * 0.6) + 2.3);
    }

    /** Smooth 0..1 pulse derived from {@link #flow}. */
    public static double breathe(double p, double s) {
        return clamp01(0.5 + 0.5 * flow(p, s));
    }

    public static double clamp01(double v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }

    /**
     * Whether text goes through Minecraft's own font instead of Meteor's custom font.
     *
     * <p>Two things follow from it. Only the vanilla font can draw katakana, because Meteor's custom fonts pack
     * just Latin, Greek and Cyrillic. And vanilla text is drawn by the game's GUI pass after all of Meteor's
     * geometry, so it always ends up on top of the windows.
     */
    public static boolean vanillaText() {
        return !Config.get().customFont.get();
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

    private static Color bilinear(Color tl, Color tr, Color br, Color bl, double fx, double fy) {
        fx = clamp01(fx);
        fy = clamp01(fy);
        Color c = SCRATCH[scratchI++ & 63];
        c.r = (int) Math.round(lerp(lerp(tl.r, tr.r, fx), lerp(bl.r, br.r, fx), fy));
        c.g = (int) Math.round(lerp(lerp(tl.g, tr.g, fx), lerp(bl.g, br.g, fx), fy));
        c.b = (int) Math.round(lerp(lerp(tl.b, tr.b, fx), lerp(bl.b, br.b, fx), fy));
        c.a = (int) Math.round(lerp(lerp(tl.a, tr.a, fx), lerp(bl.a, br.a, fx), fy));
        return c;
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    // Primitives

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

    /**
     * Filled rectangle with 45 degree cut corners and a four-corner gradient.
     * Cuts that share a horizontal edge must be equal or zero.
     */
    public static void cutRect(GuiRenderer r, double x, double y, double w, double h,
                               double tl, double tr, double br, double bl,
                               Color cTL, Color cTR, Color cBR, Color cBL) {
        if (w <= 0 || h <= 0) return;

        double h1 = Math.max(tl, tr);
        double h2 = Math.max(bl, br);
        double f1 = h1 / h;
        double f2 = 1 - h2 / h;

        if (h1 > 0) {
            double fl = tl / w;
            double fr = 1 - tr / w;
            r.quad(x + tl, y, w - tl - tr, h1,
                bilinear(cTL, cTR, cBR, cBL, fl, 0), bilinear(cTL, cTR, cBR, cBL, fr, 0),
                bilinear(cTL, cTR, cBR, cBL, fr, f1), bilinear(cTL, cTR, cBR, cBL, fl, f1));
            if (tl > 0) tri(r, x, y + tl, x + tl, y, x + tl, y + tl, bilinear(cTL, cTR, cBR, cBL, fl * 0.6, f1 * 0.6));
            if (tr > 0) tri(r, x + w - tr, y, x + w, y + tr, x + w - tr, y + tr, bilinear(cTL, cTR, cBR, cBL, 1 - (1 - fr) * 0.6, f1 * 0.6));
        }

        r.quad(x, y + h1, w, h - h1 - h2,
            bilinear(cTL, cTR, cBR, cBL, 0, f1), bilinear(cTL, cTR, cBR, cBL, 1, f1),
            bilinear(cTL, cTR, cBR, cBL, 1, f2), bilinear(cTL, cTR, cBR, cBL, 0, f2));

        if (h2 > 0) {
            double fl = bl / w;
            double fr = 1 - br / w;
            double yb = y + h - h2;
            r.quad(x + bl, yb, w - bl - br, h2,
                bilinear(cTL, cTR, cBR, cBL, fl, f2), bilinear(cTL, cTR, cBR, cBL, fr, f2),
                bilinear(cTL, cTR, cBR, cBL, fr, 1), bilinear(cTL, cTR, cBR, cBL, fl, 1));
            if (bl > 0) tri(r, x, yb, x + bl, yb, x + bl, y + h, bilinear(cTL, cTR, cBR, cBL, fl * 0.6, 1 - (1 - f2) * 0.6));
            if (br > 0) tri(r, x + w - br, yb, x + w, yb, x + w - br, y + h, bilinear(cTL, cTR, cBR, cBL, 1 - (1 - fr) * 0.6, 1 - (1 - f2) * 0.6));
        }
    }

    public static void cutRect(GuiRenderer r, double x, double y, double w, double h,
                               double tl, double tr, double br, double bl, Color cLeft, Color cRight) {
        cutRect(r, x, y, w, h, tl, tr, br, bl, cLeft, cRight, cRight, cLeft);
    }

    public static void cutRect(GuiRenderer r, double x, double y, double w, double h,
                               double tl, double tr, double br, double bl, Color c) {
        cutRect(r, x, y, w, h, tl, tr, br, bl, c, c, c, c);
    }

    /** Outline matching {@link #cutRect}. */
    public static void cutOutline(GuiRenderer r, double x, double y, double w, double h,
                                  double tl, double tr, double br, double bl, double t, Color c) {
        r.quad(x + tl, y, w - tl - tr, t, c);
        r.quad(x + bl, y + h - t, w - bl - br, t, c);
        r.quad(x, y + tl, t, h - tl - bl, c);
        r.quad(x + w - t, y + tr, t, h - tr - br, c);

        double d = t / 2;
        double lt = t * 1.25;
        if (tl > 0) line(r, x + d, y + tl, x + tl, y + d, lt, c);
        if (tr > 0) line(r, x + w - tr, y + d, x + w - d, y + tr, lt, c);
        if (br > 0) line(r, x + w - d, y + h - br, x + w - br, y + h - d, lt, c);
        if (bl > 0) line(r, x + bl, y + h - d, x + d, y + h - bl, lt, c);
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

    private static final double[] HEX_X = new double[7];
    private static final double[] HEX_Y = new double[7];

    static {
        for (int i = 0; i < 7; i++) {
            double ang = Math.PI / 3 * i - Math.PI / 2;
            HEX_X[i] = Math.cos(ang);
            HEX_Y[i] = Math.sin(ang);
        }
    }

    /** Pointy-top hexagon. */
    public static void hexagon(GuiRenderer r, double cx, double cy, double radius, Color c) {
        for (int i = 0; i < 6; i++) {
            tri(r, cx, cy, cx + HEX_X[i] * radius, cy + HEX_Y[i] * radius, cx + HEX_X[i + 1] * radius, cy + HEX_Y[i + 1] * radius, c);
        }
    }

    public static void hexagonOutline(GuiRenderer r, double cx, double cy, double radius, double t, Color c) {
        double in = radius - t;
        for (int i = 0; i < 6; i++) {
            double ax = cx + HEX_X[i] * radius, ay = cy + HEX_Y[i] * radius;
            double bx = cx + HEX_X[i + 1] * radius, by = cy + HEX_Y[i + 1] * radius;
            double cx2 = cx + HEX_X[i + 1] * in, cy2 = cy + HEX_Y[i + 1] * in;
            double dx = cx + HEX_X[i] * in, dy = cy + HEX_Y[i] * in;
            tri(r, ax, ay, bx, by, cx2, cy2, c);
            tri(r, ax, ay, cx2, cy2, dx, dy, c);
        }
    }

    /** Thin box used by inputs: translucent field, 1px line, optional cut corners. */
    public static void box(GuiRenderer r, double x, double y, double w, double h,
                           double tl, double tr, double br, double bl, double t, Color fill, Color outline) {
        cutRect(r, x, y, w, h, tl, tr, br, bl, fill);
        cutOutline(r, x, y, w, h, tl, tr, br, bl, t, outline);
    }

    /** 1px in the design, never thinner than one real pixel. */
    public static double hair(double unit) {
        return Math.max(1, Math.round(unit));
    }

    // Window chrome

    /**
     * The glass panel every window shares: cut top-left / bottom-right corners, magenta hairline,
     * two brighter glowing runs and the breathing corner brackets.
     */
    public static void panel(GuiRenderer r, double x, double y, double w, double h, double unit, Color glass, Color accent, boolean brackets) {
        double cut = Math.min(16 * unit, Math.min(w, h) / 4);
        double t = hair(unit);
        double p = phase();

        occlude(x, y, w, h);

        Color deep = rgba(6, 8, 26, Math.min(1, glass.a / 255.0 * 1.16));
        Color mid = mix(glass, deep, 0.5);
        cutRect(r, x, y, w, h, cut, 0, cut, 0, glass, mid, deep, mid);

        cutOutline(r, x, y, w, h, cut, 0, cut, 0, t, a(accent, 0.45));

        // Brighter run along the left of the top edge
        double run = Math.min(136 * unit, w * 0.3);
        glowLine(r, x + cut, y + t / 2, x + cut + run, y + t / 2, 1.6 * unit, accent, 5 * unit, 1);

        // Brighter run wrapping the bottom-right corner
        double runV = Math.min(142 * unit, h * 0.35);
        double runH = Math.min(132 * unit, w * 0.3);
        double rx = x + w - t / 2;
        double by = y + h - t / 2;
        glowLine(r, rx, y + h - cut - runV, rx, y + h - cut, 1.6 * unit, CYAN_LINE, 5 * unit, 0.85);
        glowLine(r, rx, y + h - cut, x + w - cut, by, 1.6 * unit, CYAN_LINE, 5 * unit, 0.85);
        glowLine(r, x + w - cut, by, x + w - cut - runH, by, 1.6 * unit, CYAN_LINE, 5 * unit, 0.85);

        if (!brackets) return;

        // Corner brackets, 6px outside the window
        double o = 6 * unit;
        double arm = Math.min(28 * unit, Math.min(w, h) * 0.2);
        double bt = 2 * unit;
        double g = 5 * unit;

        double a1 = 0.55 + 0.4 * flow(p, 0.15);
        glowLine(r, x + w + o - arm, y - o, x + w + o + bt / 2, y - o, bt, CYAN, g, a1);
        glowLine(r, x + w + o, y - o, x + w + o, y - o + arm, bt, CYAN, g, a1);

        double a2 = 0.55 + 0.4 * flow(p, 0.38);
        glowLine(r, x - o, y + h + o - arm, x - o, y + h + o + bt / 2, bt, CYAN, g, a2);
        glowLine(r, x - o, y + h + o, x - o + arm, y + h + o, bt, CYAN, g, a2);

        double d = cut - 4 * unit;
        double a3 = 0.55 + 0.4 * flow(p, 0.61);
        glowLine(r, x - o, y + d, x + d, y - o, bt, CYAN, g, a3);

        double a4 = 0.55 + 0.4 * flow(p, 0.84);
        glowLine(r, x + w + o, y + h - d, x + w - d, y + h + o, bt, CYAN, g, a4);
    }

    /** Header underline: a cyan line that fades to the right plus a ruler of ticks every 12px. */
    public static void headerRule(GuiRenderer r, double x, double y, double w, double tickLeft, double tickRight, double unit) {
        double t = hair(unit);
        Color bright = a(CYAN, 1);
        Color soft = a(CYAN_LINE, 0.18);
        Color none = a(CYAN_LINE, 0);
        r.quad(x, y, w * 0.3, t, bright, soft);
        r.quad(x + w * 0.3, y, w * 0.4, t, soft, soft);
        r.quad(x + w * 0.7, y, w * 0.3, t, soft, none);

        Color tick = a(CYAN_LINE, 0.35);
        double step = 12 * unit;
        double tickH = Math.round(5 * unit);
        for (double tx = x + tickLeft; tx <= x + w - tickRight; tx += step) {
            r.quad(Math.round(tx), y, t, tickH, tick);
        }
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
         * @param flags   {@link Cyber#CENTER}, {@link Cyber#RIGHT}, {@link Cyber#BOLD}
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

        /** Text with a halo, standing in for a CSS text-shadow. */
        public TextBatch glow(String text, double x, double y, double size, Color c, double spacing, int flags, Color glow, double radius) {
            // The vanilla font snaps to whole pixels, which turns a halo into a smear
            if (vanillaText()) return put(text, x, y, size, c, spacing, flags);

            double d = radius * unit;
            Color g = a(glow, 0.07);
            for (int i = 0; i < 8; i++) {
                double ang = Math.PI / 4 * i;
                put(text, x + Math.cos(ang) * d, y + Math.sin(ang) * d, size, g, spacing, flags);
            }
            return put(text, x, y, size, c, spacing, flags);
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


            for (int i = 0; i < count; i++) {
                Op first = ops.get(i);
                if (first.done) continue;

                tr.begin(first.scale);
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
