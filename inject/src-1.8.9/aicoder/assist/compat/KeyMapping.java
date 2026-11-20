package aicoder.assist.compat;

/** The newer versions' name for a key binding; only the static click is used. */
public final class KeyMapping {
    private KeyMapping() {
    }

    public static void click(Key key) {
        key.click();
    }
}
