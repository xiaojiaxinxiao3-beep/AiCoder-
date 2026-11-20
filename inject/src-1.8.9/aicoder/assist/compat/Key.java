package aicoder.assist.compat;

import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/** One key binding of the game. */
public final class Key {
    final KeyBinding binding;

    Key(KeyBinding binding) {
        this.binding = binding;
    }

    /** What the game believes about this key: held or not. */
    public void setDown(boolean down) {
        KeyBinding.setKeyBindState(binding.getKeyCode(), down);
    }

    /** One more press, handled by the game the way a real press is, later in the same tick. */
    public void click() {
        KeyBinding.onTick(binding.getKeyCode());
    }

    /** Whether the key or mouse button is physically held, whatever the game currently thinks of it. */
    public boolean physical() {
        int code = binding.getKeyCode();
        if (code == 0) return false;
        // The game stores mouse buttons as button - 100
        if (code < 0) return Mouse.isButtonDown(code + 100);
        return code < Keyboard.KEYBOARD_SIZE && Keyboard.isKeyDown(code);
    }
}
