package aicoder.combat;

import meteordevelopment.meteorclient.events.entity.player.InteractBlockEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.util.math.BlockPos;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.util.hit.EntityHitResult;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Hits the end crystal you just placed. Placing stays yours: this module only adds the left click, and
 * only while the crosshair is on the crystal, the way the attack key would. Holding the use key then
 * places the next one as soon as the last has gone off, so right click alone keeps the chain going.
 */
public class CrystalMacro extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> attackDelay = sgGeneral.add(new IntSetting.Builder()
        .name("attack-delay")
        .description("Ticks between the crystal appearing under your crosshair and the hit.")
        .defaultValue(1)
        .range(0, 40)
        .sliderRange(0, 20)
        .build()
    );

    private final Setting<Integer> randomDelay = sgGeneral.add(new IntSetting.Builder()
        .name("random-delay")
        .description("Up to this many extra ticks are added to the delay, picked at random each time.")
        .defaultValue(0)
        .range(0, 20)
        .sliderRange(0, 10)
        .build()
    );

    private final Setting<Boolean> ownOnly = sgGeneral.add(new BoolSetting.Builder()
        .name("only-my-crystals")
        .description("Only hit a crystal standing where you just placed one. Off hits any crystal under your crosshair shortly after you place.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> window = sgGeneral.add(new IntSetting.Builder()
        .name("give-up-after")
        .description("Ticks after placing during which the module still waits for the crystal to be under your crosshair.")
        .defaultValue(15)
        .range(3, 100)
        .sliderRange(3, 60)
        .build()
    );

    /** Where the placed crystal stands: the block above the one that was clicked. */
    private BlockPos placedAt;
    private int armed;
    private int wait = -1;
    private EndCrystalEntity target;

    public CrystalMacro() {
        super(Categories.Combat, "crystal-macro", "Hits the end crystal you just placed, after a delay you choose. Hold right click to keep placing; the module adds the left clicks.");
    }

    @Override
    public void onActivate() {
        reset();
    }

    private void reset() {
        placedAt = null;
        armed = 0;
        wait = -1;
        target = null;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void onInteractBlock(InteractBlockEvent event) {
        if (event.isCancelled() || mc.player == null) return;
        if (!mc.player.getStackInHand(event.hand).isOf(Items.END_CRYSTAL)) return;

        placedAt = event.result.getBlockPos().up();
        armed = window.get();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (armed <= 0) return;
        if (mc.player == null || mc.world == null || mc.interactionManager == null || mc.currentScreen != null) {
            reset();
            return;
        }
        armed--;

        EndCrystalEntity crystal = underCrosshair();
        if (crystal == null) {
            // Looked away: start the delay over when the crosshair comes back
            wait = -1;
            target = null;
            return;
        }

        if (crystal != target) {
            target = crystal;
            wait = attackDelay.get() + (randomDelay.get() > 0 ? ThreadLocalRandom.current().nextInt(randomDelay.get() + 1) : 0);
        }

        if (wait > 0) {
            wait--;
            // Keep waiting even if the window would run out during the delay
            armed = Math.max(armed, 1);
            return;
        }

        if (mc.player.isUsingItem() || mc.interactionManager.isBreakingBlock()) return;

        Clicks.attack(crystal);
        wait = -1;
        target = null;
    }

    private EndCrystalEntity underCrosshair() {
        if (!(mc.crosshairTarget instanceof EntityHitResult hit) || !(hit.getEntity() instanceof EndCrystalEntity crystal)) return null;
        if (!crystal.isAlive()) return null;
        if (ownOnly.get() && (placedAt == null || !crystal.getBlockPos().equals(placedAt))) return null;
        return crystal;
    }
}
