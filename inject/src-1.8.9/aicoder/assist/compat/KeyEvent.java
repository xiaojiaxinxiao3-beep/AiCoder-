package aicoder.assist.compat;

/** A key press or release; the key is numbered the way the newer versions number keys (GLFW). */
public final class KeyEvent {
    private final int key;

    public KeyEvent(int key) {
        this.key = key;
    }

    public int key() {
        return key;
    }
}
