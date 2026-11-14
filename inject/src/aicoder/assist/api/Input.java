package aicoder.assist.api;

import aicoder.assist.Client;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class Input {
    private Input() {
    }

    /** The key or mouse button a control is bound to right now. */
    public static InputConstants.Key keyOf(KeyMapping mapping) {
        return InputConstants.getKey(mapping.saveString());
    }

    /** Whether the bound key or button is physically held, whatever the game currently thinks of it. */
    public static boolean isPressed(KeyMapping mapping) {
        InputConstants.Key key = keyOf(mapping);
        long window = Client.mc.getWindow().handle();
        int code = key.getValue();
        if (code < 0) return false;

        if (key.getType() == InputConstants.Type.MOUSE) return GLFW.glfwGetMouseButton(window, code) == GLFW.GLFW_PRESS;
        return GLFW.glfwGetKey(window, code) == GLFW.GLFW_PRESS;
    }
}
