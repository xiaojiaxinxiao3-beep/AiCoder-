package aicoder.assist.compat;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.ItemStack;

/** The player, under the newer names. */
public final class Player {
    private final Vec velocity = new Vec();
    private final Inventory inventory = new Inventory();
    private final Stack stack = new Stack();

    private static EntityPlayerSP p() {
        return net.minecraft.client.Minecraft.getMinecraft().thePlayer;
    }

    public double getX() { return p().posX; }
    public double getY() { return p().posY; }
    public double getZ() { return p().posZ; }
    public double getEyeY() { return p().posY + p().getEyeHeight(); }
    public float getXRot() { return p().rotationPitch; }
    public float getYRot() { return p().rotationYaw; }
    /**
     * 1.8.9 draws the camera between the previous tick's angle and the current one, by itself. The
     * modules already set the angle they want shown on every frame, so both are set: otherwise the game
     * blends a second time, from an angle of its own, and the picture swings back and forth each tick.
     */
    public void setXRot(float pitch) {
        p().rotationPitch = pitch;
        p().prevRotationPitch = pitch;
    }
    /**
     * In 1.8.9 the crosshair's line is worked out from the head's yaw, which the game copies from the
     * body's yaw only when the player is updated, later in the tick than the crosshair. Setting both keeps
     * a click made in the same tick on the spot the camera was turned to.
     */
    public void setYRot(float yaw) {
        p().rotationYaw = yaw;
        p().prevRotationYaw = yaw;
        p().rotationYawHead = yaw;
        p().prevRotationYawHead = yaw;
    }
    public boolean onGround() { return p().onGround; }
    public boolean isUsingItem() { return p().isUsingItem(); }

    public Vec getDeltaMovement() {
        velocity.x = p().motionX;
        velocity.y = p().motionY;
        velocity.z = p().motionZ;
        return velocity;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public Stack getMainHandItem() {
        stack.stack = p().inventory.getCurrentItem();
        return stack;
    }

    public static final class Vec {
        public double x, y, z;
    }

    /** An item stack; in 1.8.9 an empty slot is null, here it is a stack whose item is null. */
    public static final class Stack {
        ItemStack stack;

        public Object getItem() {
            return stack == null ? null : stack.getItem();
        }
    }

    public static final class Inventory {
        private final Stack stack = new Stack();

        public int getSelectedSlot() { return p().inventory.currentItem; }
        public void setSelectedSlot(int slot) { p().inventory.currentItem = slot; }

        public Stack getItem(int slot) {
            stack.stack = p().inventory.getStackInSlot(slot);
            return stack;
        }
    }
}
