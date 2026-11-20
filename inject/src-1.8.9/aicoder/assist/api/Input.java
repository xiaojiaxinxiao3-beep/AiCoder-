package aicoder.assist.api;

import aicoder.assist.compat.Key;

public final class Input {
    private Input() {
    }

    /** The key or mouse button a control is bound to right now. */
    public static Key keyOf(Key mapping) {
        return mapping;
    }

    /** Whether the bound key or button is physically held, whatever the game currently thinks of it. */
    public static boolean isPressed(Key mapping) {
        return mapping.physical();
    }
}
