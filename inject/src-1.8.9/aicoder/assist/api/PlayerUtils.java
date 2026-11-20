package aicoder.assist.api;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;

public final class PlayerUtils {
    private PlayerUtils() {
    }

    /** Round trip time to the server in milliseconds; 0 in single player. */
    public static int getPing() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.getNetHandler() == null || mc.thePlayer == null) return 0;
        NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID());
        return info == null ? 0 : info.getResponseTime();
    }
}
