package aicoder.auroracolumns.gui;

import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/** The addon's own UI sounds, defined in {@code assets/aicoder-aurora-columns/sounds.json}. */
public final class Sounds {
    private Sounds() {
    }

    private static final SoundEvent DROP_ON = SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("aicoder-aurora-columns", "drop_on"));
    private static final SoundEvent DROP_OFF = SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("aicoder-aurora-columns", "drop_off"));

    /** A water drop on glass: rising for on, falling for off. Does nothing when the theme has the sound turned off. */
    public static void toggle(AuroraGuiTheme theme, boolean nowActive) {
        if (!theme.toggleSound.get()) return;

        float volume = theme.soundVolume.get().floatValue();
        if (volume <= 0) return;

        mc.getSoundManager().play(SimpleSoundInstance.forUI(nowActive ? DROP_ON : DROP_OFF, 1f, volume));
    }

    private static final SoundEvent HOVER = SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("aicoder-aurora-columns", "hover"));

    /** Shortest gap between two hover ticks, so sweeping the pointer down a column does not turn into a buzz. */
    private static final long HOVER_GAP_MS = 45;
    private static long lastHover;

    /**
     * A light tap on glass as the pointer enters a row. {@code tone} (0..1) is fixed per module and nudges
     * the pitch, so each row has its own note and running down a column sounds like a glass chime.
     */
    public static void hover(AuroraGuiTheme theme, double tone) {
        if (!theme.hoverSound.get()) return;

        float volume = theme.soundVolume.get().floatValue() * 0.45f;
        if (volume <= 0) return;

        long now = System.currentTimeMillis();
        if (now - lastHover < HOVER_GAP_MS) return;
        lastHover = now;

        float pitch = (float) (0.84 + 0.42 * Math.max(0, Math.min(1, tone)));
        mc.getSoundManager().play(SimpleSoundInstance.forUI(HOVER, pitch, volume));
    }
}
