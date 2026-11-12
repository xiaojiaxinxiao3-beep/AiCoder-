package aicoder.columns.gui;

import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/** The addon's own UI sounds, defined in {@code assets/aicoder-columns/sounds.json}. */
public final class Sounds {
    private Sounds() {
    }

    private static final SoundEvent TOGGLE_ON = SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("aicoder-columns", "toggle_on"));
    private static final SoundEvent TOGGLE_OFF = SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("aicoder-columns", "toggle_off"));

    /** Rising blip for on, falling blip for off. Does nothing when the theme has the sound turned off. */
    public static void toggle(AiCoderGuiTheme theme, boolean nowActive) {
        if (!theme.toggleSound.get()) return;

        float volume = theme.soundVolume.get().floatValue();
        if (volume <= 0) return;

        mc.getSoundManager().play(SimpleSoundInstance.forUI(nowActive ? TOGGLE_ON : TOGGLE_OFF, 1f, volume));
    }
}
