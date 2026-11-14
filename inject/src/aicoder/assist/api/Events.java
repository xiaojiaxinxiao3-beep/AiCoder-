package aicoder.assist.api;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;

/** The moments in the game that modules can react to. Each is raised from a patched game method, see Hooks. */
public final class Events {
    private Events() {
    }

    /** Start of every client tick. */
    public static final class TickEvent {
        public static final class Pre {
        }
    }

    /** The attack key was pressed, before the game looks at what is in the player's hand. */
    public static final class DoAttackEvent {
        public boolean isCancelled() {
            return false;
        }
    }

    /** The player is about to right click a block. */
    public static final class InteractBlockEvent {
        public InteractionHand hand;
        public BlockHitResult result;

        public boolean isCancelled() {
            return false;
        }
    }

    /** The player is about to use the item in their hand. */
    public static final class InteractItemEvent {
        public InteractionHand hand;
    }

    /** An entity appeared in the world. */
    public static final class EntityAddedEvent {
        public Entity entity;
    }

    /** Once per rendered frame. */
    public static final class Render3DEvent {
        /** Seconds since the previous frame. */
        public double frameTime;
        /** How far the game is between two ticks, 0..1. */
        public float tickDelta;
    }
}
