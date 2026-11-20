package aicoder.assist;

import aicoder.assist.api.Events;
import aicoder.assist.compat.Keys;
import org.lwjgl.input.Keyboard;

import java.lang.reflect.Field;

/**
 * What the patched game methods call. Nothing here may throw: it runs inside the game's own methods.
 * This is the 1.8.9 set: the tick, every key event and every frame.
 */
public final class Hooks {
    private static final Events.Render3DEvent FRAME = new Events.Render3DEvent();
    private static long lastFrame;
    private static Field timer;

    private Hooks() {
    }

    public static void tick() {
        try {
            Client.mc.sync();
            Client.tick();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /** The game is about to look at the key event the keyboard has just handed it. */
    public static void key() {
        try {
            if (Keyboard.isRepeatEvent()) return;
            int key = Keys.glfw(Keyboard.getEventKey());
            if (key < 0) return;
            Client.mc.sync();
            Client.key(key, Keyboard.getEventKeyState());
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static void frame() {
        try {
            long now = System.nanoTime();
            FRAME.frameTime = lastFrame == 0 ? 0 : Math.min(0.25, (now - lastFrame) / 1e9);
            lastFrame = now;
            if (timer == null) {
                timer = net.minecraft.client.Minecraft.class.getDeclaredField(Names.shared().get("Minecraft.timer"));
                timer.setAccessible(true);
            }
            FRAME.tickDelta = ((net.minecraft.util.Timer) timer.get(Client.mc.real())).renderPartialTicks;
            Client.mc.sync();
            Client.post(FRAME);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
