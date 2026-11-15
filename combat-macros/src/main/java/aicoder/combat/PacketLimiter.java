package aicoder.combat;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.game.GamePacketTypes;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A cap on how many packets of each kind you send per second. A packet over its cap is not sent at
 * all: it is dropped, not delayed, so whatever it stood for did not happen as far as the server knows.
 *
 * <p>There is one setting per gameplay packet (0 = no limit) and one for the total. Packets the
 * connection itself depends on (keep-alive, teleport confirmation, login and configuration) are
 * never limited and are not listed.
 */
public class PacketLimiter extends Module {
    /** Gameplay packets the connection or the signed chat chain depends on. Never limited, not listed. */
    private static final List<String> PROTECTED = List.of("accept_teleportation", "chunk_batch_received", "configuration_acknowledged",
        "chat", "chat_ack", "chat_command_signed", "chat_session_update");

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPackets = settings.createGroup("Per packet, per second (0 = no limit)");

    private final Setting<Integer> total = sgGeneral.add(new IntSetting.Builder()
        .name("total")
        .description("Cap on all listed packets together, per second. 0 = no limit.")
        .defaultValue(0)
        .range(0, 2000)
        .sliderRange(0, 500)
        .build()
    );

    private final Setting<Boolean> report = sgGeneral.add(new BoolSetting.Builder()
        .name("report")
        .description("Say in chat, at most once every five seconds, which packets were dropped and how many.")
        .defaultValue(true)
        .build()
    );

    /** By packet id, for example "move_player_pos" or "interact". */
    private final Map<String, Setting<Integer>> limits = new HashMap<>();
    /** Send times of the last second, per packet class and overall. */
    private final Map<String, long[]> sent = new HashMap<>();
    private final Map<String, int[]> cursor = new HashMap<>();
    private final Map<String, Integer> dropped = new HashMap<>();
    private long[] allSent = new long[2048];
    private int allCursor, reportTimer;

    public PacketLimiter() {
        super(AiCoderTab.UTILITY, "packet-limiter", "Caps how many of each gameplay packet you send per second. Packets over the cap are dropped.");

        // Every serverbound packet type of the play phase, read from the game's own list
        List<String> ids = new ArrayList<>();
        for (Field field : GamePacketTypes.class.getFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || !PacketType.class.isAssignableFrom(field.getType())) continue;
            try {
                PacketType<?> type = (PacketType<?>) field.get(null);
                if (type.flow() == PacketFlow.SERVERBOUND) ids.add(type.id().getPath());
            } catch (ReflectiveOperationException ignored) {
                // A type that cannot be read is simply not listed, and so never limited
            }
        }
        ids.sort(null);
        for (String id : ids) {
            String name = id.replace('_', '-');
            if (PROTECTED.contains(id) || settings.get(name) != null) continue;
            limits.put(id, sgPackets.add(new IntSetting.Builder()
                .name(name)
                .description("Most \"" + id + "\" packets per second. 0 = no limit.")
                .defaultValue(0)
                .range(0, 1000)
                .sliderRange(0, 100)
                .build()
            ));
        }
    }

    @Override
    public void onActivate() {
        sent.clear();
        cursor.clear();
        dropped.clear();
        allCursor = 0;
        java.util.Arrays.fill(allSent, 0);
    }

    /** Counts the entries of a ring that are less than a second old. */
    private static int recent(long[] ring, long now) {
        int n = 0;
        for (long t : ring) if (t != 0 && now - t < 1000) n++;
        return n;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void onSend(PacketEvent.Send event) {
        if (event.isCancelled()) return;
        String type = event.packet.type().id().getPath();
        Setting<Integer> limit = limits.get(type);
        if (limit == null) return;

        long now = System.currentTimeMillis();
        int cap = limit.get(), all = total.get();
        long[] ring = sent.computeIfAbsent(type, k -> new long[1024]);
        if ((cap > 0 && recent(ring, now) >= cap) || (all > 0 && recent(allSent, now) >= all)) {
            event.cancel();
            dropped.merge(limit.name, 1, Integer::sum);
            return;
        }
        int[] at = cursor.computeIfAbsent(type, k -> new int[1]);
        ring[at[0]++ % ring.length] = now;
        allSent[allCursor++ % allSent.length] = now;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (++reportTimer < 100) return;
        reportTimer = 0;
        if (!report.get() || dropped.isEmpty()) {
            dropped.clear();
            return;
        }
        StringBuilder text = new StringBuilder();
        dropped.forEach((name, count) -> {
            if (text.length() > 0) text.append(", ");
            text.append(name).append(" x").append(count);
        });
        info("Dropped in the last 5 s: %s", text);
        dropped.clear();
    }
}
