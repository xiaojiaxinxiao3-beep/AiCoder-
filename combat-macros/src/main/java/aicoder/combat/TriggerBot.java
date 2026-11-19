package aicoder.combat;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.KeyMappingAccessor;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;

/**
 * Presses attack for you while your crosshair rests on someone. It never turns the camera and never
 * picks a target: it only looks at what the game says is under the crosshair this tick, and if that
 * is a player within range and the cooldown has passed, it presses the attack key.
 *
 * <p>Cooldown: Auto waits until the held weapon's attack charge is full (so a sword, an axe and a
 * mace each get their own rhythm); Custom waits a fixed number of ticks between presses.
 */
public class TriggerBot extends HintModule {
    public enum Cooldown { Auto, Custom }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgTargets = settings.createGroup("Targets");

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .description("Only attack when the point under the crosshair is this close. The game's own reach still applies.")
        .defaultValue(3.0)
        .range(1, 6)
        .sliderRange(1, 6)
        .build()
    );

    private final Setting<Cooldown> cooldown = sgGeneral.add(new EnumSetting.Builder<Cooldown>()
        .name("cooldown")
        .description("Auto: wait for the held weapon's attack charge. Custom: wait a fixed number of ticks.")
        .defaultValue(Cooldown.Auto)
        .build()
    );

    private final Setting<Integer> charge = sgGeneral.add(new IntSetting.Builder()
        .name("charge")
        .description("How full the attack charge must be, in percent, before attacking.")
        .defaultValue(100)
        .range(50, 100)
        .sliderRange(50, 100)
        .visible(() -> cooldown.get() == Cooldown.Auto)
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Ticks between attacks. 20 ticks are one second.")
        .defaultValue(10)
        .range(1, 60)
        .sliderRange(1, 40)
        .visible(() -> cooldown.get() == Cooldown.Custom)
        .build()
    );

    private final Setting<Boolean> pauseWhileUsing = sgGeneral.add(new BoolSetting.Builder()
        .name("pause-while-using")
        .description("Do not attack while you are eating, drinking, blocking or drawing a bow.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> ignoreFriends = sgTargets.add(new BoolSetting.Builder()
        .name("ignore-friends")
        .description("Never attack players on your friends list.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> mobs = sgTargets.add(new BoolSetting.Builder()
        .name("mobs")
        .description("Also attack mobs and animals under the crosshair, not only players.")
        .defaultValue(false)
        .build()
    );

    private int wait;

    public TriggerBot() {
        super(AiCoderTab.MELEE, "trigger-bot", "Attacks for you when your crosshair is on a player within range, timed by the weapon's cooldown or by your own delay.");
    }

    @Override
    public void onActivate() {
        wait = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (wait > 0) wait--;
        if (mc.player == null || mc.level == null || mc.gui.screen() != null) return;
        if (!(mc.hitResult instanceof EntityHitResult hit)) return;

        Entity target = hit.getEntity();
        if (!accept(target)) return;
        if (mc.player.getEyePosition().distanceTo(hit.getLocation()) > range.get()) return;
        if (pauseWhileUsing.get() && mc.player.isUsingItem()) return;

        if (cooldown.get() == Cooldown.Auto) {
            if (mc.player.getAttackStrengthScale(0.5f) < charge.get() / 100f) return;
        } else {
            if (wait > 0) return;
            wait = delay.get();
        }

        // One more press of the attack key; the game handles it later in this same tick
        KeyMapping.click(((KeyMappingAccessor) mc.options.keyAttack).meteor$getKey());
    }

    private boolean accept(Entity target) {
        if (target instanceof Player player) {
            if (ignoreFriends.get() && Friends.get().isFriend(player)) return false;
            return player.isAlive();
        }
        return mobs.get() && target instanceof LivingEntity living && living.isAlive();
    }
}
