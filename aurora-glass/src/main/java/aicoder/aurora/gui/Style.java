package aicoder.aurora.gui;

import meteordevelopment.meteorclient.utils.render.color.Color;

/** The aurora palette and corner sizes (design pixels) every widget draws with. */
public final class Style {
    public final double corner = 28, columnCorner = 24, plateCorner = 18, fieldCorner = 10;

    public final Color green = new Color(77, 255, 184, 255);
    public final Color teal = new Color(47, 230, 224, 255);
    public final Color violet = new Color(143, 123, 255, 255);
    public final Color pink = new Color(255, 111, 216, 255);
    /** Border of a switched-on row. */
    public final Color mint = new Color(160, 255, 225, 255);

    public final Color title = new Color(238, 250, 255, 255);
    public final Color text = new Color(238, 250, 255, 255);
    public final Color dim = new Color(226, 244, 255, 158);
    public final Color faint = new Color(226, 244, 255, 92);
    /** Text on top of a solid aurora fill. */
    public final Color dark = new Color(4, 35, 28, 255);

    /** Aliases used by widgets that only need "the accent". */
    public final Color accent = green, accent2 = teal;

    /** Dark inset used by value boxes, buttons and slider tracks. */
    public final Color field = new Color(3, 10, 26, 107);

    /** Body of popups that must stay readable over anything. */
    public final Color solid = new Color(12, 28, 58, 244);
}
