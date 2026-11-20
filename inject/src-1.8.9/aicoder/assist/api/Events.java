package aicoder.assist.api;

/** The moments in the game that modules can react to. Each is raised from a patched game method, see Hooks. */
public final class Events {
    private Events() {
    }

    /** Start of every client tick. */
    public static final class TickEvent {
        public static final class Pre {
        }
    }

    /** Once per rendered frame. */
    public static final class Render3DEvent {
        /** Seconds since the previous frame. */
        public double frameTime;
        /** How far the game is between two ticks, 0..1. */
        public float tickDelta;
    }
}
