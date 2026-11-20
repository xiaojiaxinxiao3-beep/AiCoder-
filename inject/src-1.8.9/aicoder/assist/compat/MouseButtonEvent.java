package aicoder.assist.compat;

/** A mouse button going down or up, at a place in the game's GUI units. */
public final class MouseButtonEvent {
    private final double x, y;
    private final int button;

    public MouseButtonEvent(double x, double y, int button) {
        this.x = x;
        this.y = y;
        this.button = button;
    }

    public double x() { return x; }
    public double y() { return y; }
    public int button() { return button; }
}
