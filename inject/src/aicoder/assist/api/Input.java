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

    private static java.lang.reflect.Method onMove;

    /**
     * Puts the mouse cursor at a place in the game window, in window pixels. The system cursor is moved,
     * and the game's own mouse-move handler is called with the same place: whether the window library
     * reports a move it made itself differs from system to system.
     */
    public static void moveCursor(double x, double y) {
        long window = Client.mc.getWindow().handle();
        GLFW.glfwSetCursorPos(window, x, y);
        try {
            if (onMove == null) {
                onMove = net.minecraft.client.MouseHandler.class.getDeclaredMethod(aicoder.assist.Names.shared().get("MouseHandler.onMove"), long.class, double.class, double.class);
                onMove.setAccessible(true);
            }
            onMove.invoke(Client.mc.mouseHandler, window, x, y);
        } catch (ReflectiveOperationException e) {
            // The system cursor has still been moved
        }
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
