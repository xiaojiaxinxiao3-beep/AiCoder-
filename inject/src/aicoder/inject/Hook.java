package aicoder.inject;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** What the patched game methods call. Must never throw: an exception here would crash the game's own code. */
public final class Hook {
    private static Method getInstance, getFps, drawString;
    private static Field font;
    private static int ticks;
    private static boolean broken;

    static void init(Names names) throws Exception {
        Class<?> minecraft = Class.forName(names.get("Minecraft").replace('/', '.'));
        Class<?> graphics = Class.forName(names.get("GuiGraphics").replace('/', '.'));
        Class<?> fontClass = Class.forName(names.get("Font").replace('/', '.'));

        getInstance = minecraft.getMethod(names.get("Minecraft.getInstance"));
        getFps = minecraft.getMethod(names.get("Minecraft.getFps"));
        font = minecraft.getField(names.get("Minecraft.font"));
        drawString = graphics.getMethod(names.get("GuiGraphics.drawString"), fontClass, String.class, int.class, int.class, int.class);
    }

    public static void tick() {
        ticks++;
    }

    /** Runs at the end of every screen and of the in-game HUD, with the game's drawing context. */
    public static void overlay(Object graphics) {
        if (broken) return;
        try {
            Object mc = getInstance.invoke(null);
            String text = "AiCoder injected  |  tick " + ticks + "  |  " + getFps.invoke(mc) + " FPS";
            drawString.invoke(graphics, font.get(mc), text, 6, 6, 0xFF4DFFB8);
        } catch (Throwable t) {
            broken = true;
            t.printStackTrace();
        }
    }
}
