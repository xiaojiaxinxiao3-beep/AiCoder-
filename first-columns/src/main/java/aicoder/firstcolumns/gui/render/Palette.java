package aicoder.firstcolumns.gui.render;

import meteordevelopment.meteorclient.utils.render.color.Color;

/** Colours as packed ARGB ints, plus a few helpers. The neutrals come from the design spec, the accents from the theme's two colour settings. */
public final class Palette {
    /** The two user colours and their midpoint. The theme refreshes them every frame. */
    public static int ACCENT_1 = 0xFF2FE08F;
    public static int ACCENT_2 = 0xFF2F8CFF;
    public static int ACCENT_MID = 0xFF2FB6C7;

    public static final int WHITE = 0xFFFFFFFF;

    public static final int TEXT = 0xFFC7D6F5;
    public static final int TEXT_SECONDARY = 0xFF9FB6E6;
    public static final int TEXT_LABEL = 0xFFAEBBE0;
    public static final int TEXT_KEY = 0xFF7FF3FF;
    public static final int TEXT_FOOTER = 0xFF8AA0CF;
    public static final int TEXT_HEADING = 0xFF8EF0FF;

    public static final int DIVIDER = rgba(120, 180, 255, 0.13);
    public static final int EDGE = rgba(180, 230, 255, 0.25);
    public static final int HIGHLIGHT = rgba(255, 255, 255, 0.20);

    public static final int ROW = rgba(130, 160, 230, 0.05);
    public static final int ROW_BORDER = rgba(130, 160, 255, 0.10);
    public static final int ROW_HOVER = rgba(130, 170, 240, 0.10);

    public static final int TOGGLE_OFF = rgba(16, 24, 44, 0.5);
    public static final int TOGGLE_OFF_RING = rgba(160, 190, 255, 0.32);
    public static final int TOGGLE_DOT = 0xFFEAFFF6;

    public static final int CHECK_BORDER = rgba(130, 160, 255, 0.40);
    public static final int CHECK_MARK = 0xFF04040A;

    public static final int SLIDER_TRACK = 0xFF141A30;
    public static final int SLIDER_BORDER = rgba(130, 160, 255, 0.15);

    private Palette() {
    }

    public static void setAccents(int first, int second) {
        ACCENT_1 = first | 0xFF000000;
        ACCENT_2 = second | 0xFF000000;
        ACCENT_MID = mix(ACCENT_1, ACCENT_2, 0.5);
    }

    public static int rgba(int r, int g, int b, double a) {
        return (clamp((int) Math.round(a * 255)) << 24) | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
    }

    /** Replaces the alpha of a colour with its current alpha multiplied by {@code factor}. */
    public static int alpha(int argb, double factor) {
        int a = clamp((int) Math.round(((argb >>> 24) & 0xFF) * factor));
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    /** Replaces the alpha of a colour. */
    public static int withAlpha(int argb, double a) {
        return (clamp((int) Math.round(a * 255)) << 24) | (argb & 0x00FFFFFF);
    }

    public static int mix(int c1, int c2, double t) {
        t = Math.max(0, Math.min(1, t));

        int a = (int) Math.round(((c1 >>> 24) & 0xFF) + (((c2 >>> 24) & 0xFF) - ((c1 >>> 24) & 0xFF)) * t);
        int r = (int) Math.round(((c1 >> 16) & 0xFF) + (((c2 >> 16) & 0xFF) - ((c1 >> 16) & 0xFF)) * t);
        int g = (int) Math.round(((c1 >> 8) & 0xFF) + (((c2 >> 8) & 0xFF) - ((c1 >> 8) & 0xFF)) * t);
        int b = (int) Math.round((c1 & 0xFF) + ((c2 & 0xFF) - (c1 & 0xFF)) * t);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /** Samples an evenly spaced multi stop gradient, {@code t} is clamped to 0..1. */
    public static int gradient(double t, int... stops) {
        t = Math.max(0, Math.min(1, t));

        double p = t * (stops.length - 1);
        int i = Math.min(stops.length - 2, (int) Math.floor(p));

        return mix(stops[i], stops[i + 1], p - i);
    }

    /** Same as {@link #gradient} but wraps around instead of clamping. */
    public static int loop(double t, int... stops) {
        t -= Math.floor(t);

        double p = t * stops.length;
        int i = (int) Math.floor(p) % stops.length;

        return mix(stops[i], stops[(i + 1) % stops.length], p - Math.floor(p));
    }

    public static Color color(int argb) {
        return new Color((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, (argb >>> 24) & 0xFF);
    }

    /** Writes a packed colour into an existing Meteor colour. */
    public static void into(Color target, int argb) {
        target.set((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, (argb >>> 24) & 0xFF);
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }
}
