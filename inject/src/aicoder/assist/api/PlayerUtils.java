package aicoder.assist.api;

import aicoder.assist.Client;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.Entity;

public final class PlayerUtils {
    private PlayerUtils() {
    }

    /** Round trip time to the server in milliseconds; 0 in single player. */
    public static int getPing() {
        if (Client.mc.getConnection() == null || Client.mc.player == null) return 0;
        PlayerInfo info = Client.mc.getConnection().getPlayerInfo(Client.mc.player.getUUID());
        return info == null ? 0 : info.getLatency();
    }

    public static boolean canSeeEntity(Entity entity) {
        return Client.mc.player != null && Client.mc.player.hasLineOfSight(entity);
    }
}
