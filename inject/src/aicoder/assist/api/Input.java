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

    public static boolean isKeyPressed(int key) {
        return GLFW.glfwGetKey(Client.mc.getWindow().handle(), key) == GLFW.GLFW_PRESS;
    }

    public static boolean isButtonPressed(int button) {
        return GLFW.glfwGetMouseButton(Client.mc.getWindow().handle(), button) == GLFW.GLFW_PRESS;
    }

    /** Puts the mouse cursor at a place in the game window, in window pixels. The game hears of it as a mouse move. */
    public static void moveCursor(double x, double y) {
        GLFW.glfwSetCursorPos(Client.mc.getWindow().handle(), x, y);
    }

    private static java.lang.reflect.Field hovered;

    /** The slot the mouse is over in an inventory screen, or null. */
    public static net.minecraft.world.inventory.Slot hoveredSlot(net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> screen) {
        try {
            if (hovered == null) {
                hovered = net.minecraft.client.gui.screens.inventory.AbstractContainerScreen.class.getDeclaredField(aicoder.assist.Names.shared().get("AbstractContainerScreen.hoveredSlot"));
                hovered.setAccessible(true);
            }
            return (net.minecraft.world.inventory.Slot) hovered.get(screen);
        } catch (ReflectiveOperationException e) {
            return null;
        }
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
