package aicoder.glasscolumns.gui;

import meteordevelopment.meteorclient.utils.render.color.Color;

/**
 * The palette every widget draws with. Unlike a fixed theme it is recomputed each frame from the two
 * colors the player picked (or from neutral glass when color is turned off), so edits apply live.
 */
public final class Style {
    private static final Color WHITE = new Color(255, 255, 255, 255);
    private static final Color INK = new Color(10, 14, 36, 255);

    /** Corners in design pixels. */
    public final double corner = 22, plateCorner = 11, fieldCorner = 6;

    /** False when the player turned color off: clear glass, neutral accents. */
    public boolean tinted;
    /** 0..1, how solid the glass body is. */
    public double opacity;

    /** The two glass colors, top-left and bottom-right. */
    public final Color colorA = new Color(), colorB = new Color();
    /** Light versions of the two colors, for text and small glowing details. */
    public final Color accent = new Color(), accent2 = new Color();

    public final Color title = new Color(246, 249, 255, 255);
    public final Color text = new Color(228, 235, 255, 255);
    public final Color dim = new Color(196, 208, 242, 255);
    public final Color faint = new Color(150, 166, 214, 255);

    /** Dark inset used by value boxes and buttons. */
    public final Color field = new Color(8, 12, 34, 120);
    public final Color fieldLine = new Color(255, 255, 255, 46);

    /** Opaque body for popups that must stay readable over anything. */
    public final Color solid = new Color();

    public void update(Color a, Color b, boolean tinted, double opacity) {
        this.tinted = tinted;
        this.opacity = opacity;

        if (tinted) {
            colorA.set(a.r, a.g, a.b, 255);
            colorB.set(b.r, b.g, b.b, 255);
        } else {
            colorA.set(214, 228, 255, 255);
            colorB.set(190, 204, 240, 255);
        }

        blend(accent, colorA, WHITE, tinted ? 0.52 : 0.6);
        blend(accent2, colorB, WHITE, tinted ? 0.4 : 0.5);

        if (tinted) blend(solid, colorB, INK, 0.62);
        else solid.set(26, 32, 54, 255);
        solid.a = 246;
    }

    private static void blend(Color out, Color c1, Color c2, double t) {
        out.set(
            (int) Math.round(c1.r + (c2.r - c1.r) * t),
            (int) Math.round(c1.g + (c2.g - c1.g) * t),
            (int) Math.round(c1.b + (c2.b - c1.b) * t),
            255
        );
    }
}
