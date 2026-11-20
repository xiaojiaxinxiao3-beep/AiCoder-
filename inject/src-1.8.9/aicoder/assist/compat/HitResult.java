package aicoder.assist.compat;

import net.minecraft.util.MovingObjectPosition;

/** What the crosshair is on. */
public final class HitResult {
    public enum Type { MISS, BLOCK, ENTITY }

    public MovingObjectPosition real() {
        return net.minecraft.client.Minecraft.getMinecraft().objectMouseOver;
    }

    public Type getType() {
        MovingObjectPosition over = real();
        if (over == null || over.typeOfHit == null) return Type.MISS;
        switch (over.typeOfHit) {
            case BLOCK: return Type.BLOCK;
            case ENTITY: return Type.ENTITY;
            default: return Type.MISS;
        }
    }
}
