package aicoder.firstcolumns.gui.widgets;

import aicoder.firstcolumns.gui.AiCoderGuiTheme;
import aicoder.firstcolumns.gui.render.Anim;
import aicoder.firstcolumns.gui.render.Palette;
import aicoder.firstcolumns.gui.render.Sdf;

/** The pieces of the design that several widgets share, composed from shader shapes. */
public final class Shapes {
    /** Translucent dark blue under the glass so text stays readable over bright scenery. */
    public static final int GLASS_BASE = 0xFF060E22;

    private Shapes() {
    }

    /**
     * A window of frosted glass styled by the theme's settings. With the glass colour switched off it is a
     * clear pane that only blurs what is behind it, with a faint white hairline so its edge can still be seen.
     *
     * @param darkness how much of the dark base to use, more for windows that hold dense text
     */
    public static void glass(AiCoderGuiTheme theme, double x, double y, double w, double h, double radius, double u, double darkness, double seed) {
        if (!theme.glassColor.get()) {
            Sdf.glass(x, y, w, h, radius, u, 0, 0, 0, Palette.rgba(255, 255, 255, 0.22), 0, false, seed);
            return;
        }

        double opacity = theme.glassOpacity.get();

        Sdf.glass(x, y, w, h, radius, u,
            Palette.withAlpha(GLASS_BASE, Math.min(0.85, 0.62 * opacity * darkness)),
            opacity, 0.10,
            Palette.rgba(255, 255, 255, 0.55), 1, true, seed);
    }

    /** Round on / off switch. {@code on} is 0..1 so it can be animated. */
    public static void toggle(double cx, double cy, double radius, double u, double on) {
        if (on < 1) {
            Sdf.circle(cx, cy, radius)
                .fill(Palette.alpha(Palette.TOGGLE_OFF, 1 - on))
                .stroke(2 * u, Palette.alpha(Palette.TOGGLE_OFF_RING, 1 - on))
                .draw();
        }

        if (on > 0) {
            double pulse = Anim.pulse();

            // Wide blue halo, then the breathing green glow around the green to cyan disc
            Sdf.circle(cx, cy, radius).glow(22 * u, Palette.withAlpha(Palette.ACCENT_2, 0.30 * on)).draw();
            Sdf.circle(cx, cy, radius)
                .radial(Palette.alpha(Palette.ACCENT_1, on), Palette.alpha(Palette.ACCENT_MID, on))
                .glow((7 + pulse * 13) * u, Palette.withAlpha(Palette.ACCENT_1, (0.45 + pulse * 0.35) * on))
                .draw();

            Sdf.circle(cx, cy, 4 * u * on)
                .fill(Palette.alpha(Palette.TOGGLE_DOT, on))
                .glow(8 * u, Palette.rgba(255, 255, 255, 0.55 * on))
                .draw();
        }
    }

    /** Card behind a list row. */
    public static void row(double x, double y, double w, double h, double radius, double u, boolean hovered) {
        Sdf.box(x, y, w, h, radius)
            .fill(hovered ? Palette.ROW_HOVER : Palette.ROW)
            .stroke(Math.max(1, u), Palette.ROW_BORDER)
            .draw();
    }
}
