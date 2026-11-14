package aicoder.assist;

import aicoder.assist.api.Events;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;

/**
 * What the patched game methods call. Arguments arrive as Object so the inserted calls need no knowledge
 * of the game's class names. Nothing here may throw: it runs inside the game's own methods.
 */
public final class Hooks {
    private static final Events.DoAttackEvent ATTACK = new Events.DoAttackEvent();
    private static final Events.InteractBlockEvent USE_BLOCK = new Events.InteractBlockEvent();
    private static final Events.InteractItemEvent USE_ITEM = new Events.InteractItemEvent();
    private static final Events.EntityAddedEvent ENTITY = new Events.EntityAddedEvent();
    private static final Events.Render3DEvent FRAME = new Events.Render3DEvent();
    private static long lastFrame;

    private Hooks() {
    }

    public static void tick() {
        try {
            Client.tick();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static void attack() {
        try {
            Client.post(ATTACK);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static void useBlock(Object hand, Object hit) {
        try {
            USE_BLOCK.hand = (InteractionHand) hand;
            USE_BLOCK.result = (BlockHitResult) hit;
            Client.post(USE_BLOCK);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static void useItem(Object hand) {
        try {
            USE_ITEM.hand = (InteractionHand) hand;
            Client.post(USE_ITEM);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static void entityAdded(Object entity) {
        try {
            ENTITY.entity = (Entity) entity;
            Client.post(ENTITY);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /** A key went down, up or repeated. {@code action} is GLFW's: 1 is a press, 0 a release. */
    public static void key(int action, Object event) {
        try {
            if (action == 0 || action == 1) Client.key(((KeyEvent) event).key(), action == 1);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static void frame() {
        try {
            long now = System.nanoTime();
            FRAME.frameTime = lastFrame == 0 ? 0 : Math.min(0.25, (now - lastFrame) / 1e9);
            lastFrame = now;
            FRAME.tickDelta = Client.mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
            Client.post(FRAME);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
