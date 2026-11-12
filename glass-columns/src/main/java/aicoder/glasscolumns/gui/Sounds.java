package aicoder.glasscolumns.gui;

import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/** The addon's own UI sounds, defined in {@code assets/aicoder-glass-columns/sounds.json}. */
public final class Sounds {
    private Sounds() {
    }

    private static final SoundEvent DROP_ON = SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("aicoder-glass-columns", "drop_on"));
    private static final SoundEvent DROP_OFF = SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("aicoder-glass-columns", "drop_off"));

    /** A water drop on glass: rising for on, falling for off. Does nothing when the theme has the sound turned off. */
    public static void toggle(GlassGuiTheme theme, boolean nowActive) {
        if (!theme.toggleSound.get()) return;

        float volume = theme.soundVolume.get().floatValue();
        if (volume <= 0) return;

        mc.getSoundManager().play(SimpleSoundInstance.forUI(nowActive ? DROP_ON : DROP_OFF, 1f, volume));
    }
}
