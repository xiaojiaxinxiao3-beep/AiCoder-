package aicoder.assist.compat;

import org.lwjgl.input.Keyboard;

/**
 * Key numbers. 1.8.9 numbers keys the LWJGL 2 way; the menu, the games and the saved binds use the
 * numbers of the newer versions (GLFW), so the same settings file means the same keys in both.
 */
public final class Keys {
    private static final int[] TO_GLFW = new int[Keyboard.KEYBOARD_SIZE];
    private static final int[] TO_LWJGL = new int[400];

    private Keys() {
    }

    private static void pair(int lwjgl, int glfw) {
        TO_GLFW[lwjgl] = glfw;
        TO_LWJGL[glfw] = lwjgl;
    }

    static {
        String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        for (int i = 0; i < 26; i++) pair(Keyboard.getKeyIndex(String.valueOf(letters.charAt(i))), 65 + i);
        pair(Keyboard.KEY_0, 48);
        for (int i = 1; i <= 9; i++) pair(Keyboard.KEY_1 + i - 1, 48 + i);
        for (int i = 0; i < 10; i++) pair(Keyboard.KEY_F1 + i, 290 + i);
        pair(Keyboard.KEY_F11, 300);
        pair(Keyboard.KEY_F12, 301);
        pair(Keyboard.KEY_SPACE, 32);
        pair(Keyboard.KEY_APOSTROPHE, 39);
        pair(Keyboard.KEY_COMMA, 44);
        pair(Keyboard.KEY_MINUS, 45);
        pair(Keyboard.KEY_PERIOD, 46);
        pair(Keyboard.KEY_SLASH, 47);
        pair(Keyboard.KEY_SEMICOLON, 59);
        pair(Keyboard.KEY_EQUALS, 61);
        pair(Keyboard.KEY_LBRACKET, 91);
        pair(Keyboard.KEY_BACKSLASH, 92);
        pair(Keyboard.KEY_RBRACKET, 93);
        pair(Keyboard.KEY_GRAVE, 96);
        pair(Keyboard.KEY_ESCAPE, 256);
        pair(Keyboard.KEY_RETURN, 257);
        pair(Keyboard.KEY_TAB, 258);
        pair(Keyboard.KEY_BACK, 259);
        pair(Keyboard.KEY_INSERT, 260);
        pair(Keyboard.KEY_DELETE, 261);
        pair(Keyboard.KEY_RIGHT, 262);
        pair(Keyboard.KEY_LEFT, 263);
        pair(Keyboard.KEY_DOWN, 264);
        pair(Keyboard.KEY_UP, 265);
        pair(Keyboard.KEY_PRIOR, 266);
        pair(Keyboard.KEY_NEXT, 267);
        pair(Keyboard.KEY_HOME, 268);
        pair(Keyboard.KEY_END, 269);
        pair(Keyboard.KEY_CAPITAL, 280);
        pair(Keyboard.KEY_SCROLL, 281);
        pair(Keyboard.KEY_NUMLOCK, 282);
        pair(Keyboard.KEY_PAUSE, 284);
        for (int i = 0; i <= 9; i++) pair(Keyboard.getKeyIndex("NUMPAD" + i), 320 + i);
        pair(Keyboard.KEY_DECIMAL, 330);
        pair(Keyboard.KEY_DIVIDE, 331);
        pair(Keyboard.KEY_MULTIPLY, 332);
        pair(Keyboard.KEY_SUBTRACT, 333);
        pair(Keyboard.KEY_ADD, 334);
        pair(Keyboard.KEY_NUMPADENTER, 335);
        pair(Keyboard.KEY_LSHIFT, 340);
        pair(Keyboard.KEY_LCONTROL, 341);
        pair(Keyboard.KEY_LMENU, 342);
        pair(Keyboard.KEY_LMETA, 343);
        pair(Keyboard.KEY_RSHIFT, 344);
        pair(Keyboard.KEY_RCONTROL, 345);
        pair(Keyboard.KEY_RMENU, 346);
        pair(Keyboard.KEY_RMETA, 347);
    }

    /** A 1.8.9 key number as the newer versions number it, or -1 for a key they have no number for. */
    public static int glfw(int lwjgl) {
        int key = lwjgl >= 0 && lwjgl < TO_GLFW.length ? TO_GLFW[lwjgl] : 0;
        return key == 0 ? -1 : key;
    }

    public static String name(int glfw) {
        int key = glfw >= 0 && glfw < TO_LWJGL.length ? TO_LWJGL[glfw] : 0;
        String name = key == 0 ? null : Keyboard.getKeyName(key);
        return name == null ? "KEY " + glfw : name;
    }
}
