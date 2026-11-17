package aicoder.firstcolumns.gui.render;

/**
 * Shared animation clock. Motion is built from a few stacked sine waves so it loops but reads as
 * irregular drifting, nothing spins and everything is slow.
 */
public final class Anim {
    private static final double TAU = Math.PI * 2;

    /** Length of one loop of the aurora and of the flowing borders, in seconds. */
    public static final double FLOW_PERIOD = 9;
    /** Length of one breath of the glows, in seconds. */
    public static final double PULSE_PERIOD = 3.6;

    private static final long START = System.nanoTime();

    private Anim() {
    }

    public static double seconds() {
        return (System.nanoTime() - START) / 1e9;
    }

    /** Position 0..1 inside the current flow loop. */
    public static double phase() {
        double p = seconds() / FLOW_PERIOD;
        return p - Math.floor(p);
    }

    /** Irregular wave in roughly -1..1, {@code seed} offsets it so different users do not move in sync. */
    public static double flow(double p, double seed) {
        return 0.54 * Math.sin(TAU * (p + seed))
            + 0.30 * Math.sin(TAU * (2 * p + seed * 1.7) + 1.1)
            + 0.16 * Math.sin(TAU * (3 * p - seed * 0.6) + 2.3);
    }

    public static double flow(double seed) {
        return flow(phase(), seed);
    }

    /** Slow breathing value in 0..1. */
    public static double pulse() {
        return 0.5 + 0.5 * Math.sin(TAU * seconds() / PULSE_PERIOD);
    }
}
