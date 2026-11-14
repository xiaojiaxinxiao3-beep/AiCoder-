package aicoder.combat;

import meteordevelopment.meteorclient.events.entity.player.DoAttackEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.registry.Registries;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.EntityHitResult;

/**
 * Swaps to the mace for the hit. When you left click someone while holding another weapon, the module
 * selects the mace in your hotbar just before the click goes through, and selects your weapon again a few
 * ticks later.
 *
 * <p>This is the same as pressing the mace's number key right before clicking. The server only applies a
 * newly held item's attributes and restarts the attack cooldown on its next tick, so the hit lands with
 * the mace as the weapon (its smash bonus and enchantments, and it takes the durability loss) but with the
 * attack damage and the charge of the weapon you were holding. The slot change is a real one: other
 * players see the mace in your hand for as long as it stays selected.
 */
public class MaceSwap extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgWhen = settings.createGroup("When");

    private final Setting<Integer> backDelay = sgGeneral.add(new IntSetting.Builder()
        .name("swap-back-delay")
        .description("Ticks the mace stays in your hand after the hit. At least 2 is needed for other players to see it.")
        .defaultValue(3)
        .range(1, 40)
        .sliderRange(1, 20)
        .build()
    );

    private final Setting<Boolean> swapBack = sgGeneral.add(new BoolSetting.Builder()
        .name("swap-back")
        .description("Select your weapon again afterwards. Off leaves the mace in your hand.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> weaponsOnly = sgWhen.add(new BoolSetting.Builder()
        .name("weapons-only")
        .description("Only swap when you hold a sword, axe, spear or trident. Off swaps from any item.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> playersOnly = sgWhen.add(new BoolSetting.Builder()
        .name("players-only")
        .description("Only swap when the target is another player. Off also swaps against mobs.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> ignoreFriends = sgWhen.add(new BoolSetting.Builder()
        .name("ignore-friends")
        .description("Do not swap when the target is on your Meteor friends list.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> chargedOnly = sgWhen.add(new BoolSetting.Builder()
        .name("charged-only")
        .description("Only swap when your attack is charged, since the charge is what carries over to the mace.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> minFall = sgWhen.add(new DoubleSetting.Builder()
        .name("min-fall")
        .description("Only swap after falling at least this many blocks, for the mace's smash. 0 swaps on every hit.")
        .defaultValue(0)
        .range(0, 50)
        .sliderRange(0, 20)
        .build()
    );

    private int returnSlot = -1;
    private int maceSlot = -1;
    private int wait;

    public MaceSwap() {
        super(AiCoderTab.MELEE, "mace-swap", "When you hit someone with another weapon and have a mace in your hotbar, switches to the mace for that hit and back again shortly after.");
    }

    @Override
    public void onActivate() {
        returnSlot = -1;
    }

    /** The attack key was pressed. Runs before the game looks at what is in your hand. */
    @EventHandler(priority = EventPriority.LOWEST)
    private void onAttack(DoAttackEvent event) {
        if (event.isCancelled() || mc.player == null || mc.world == null || returnSlot >= 0) return;
        if (!(mc.crosshairTarget instanceof EntityHitResult hit)) return;

        Entity target = hit.getEntity();
        if (!(target instanceof LivingEntity) || !target.isAlive()) return;
        if (target instanceof PlayerEntity player) {
            if (ignoreFriends.get() && Friends.get().isFriend(player)) return;
        } else if (playersOnly.get()) {
            return;
        }

        ItemStack held = mc.player.getMainHandStack();
        if (held.isOf(Items.MACE)) return;
        if (weaponsOnly.get() && !isWeapon(held)) return;

        int slot = findMace();
        if (slot < 0) return;

        if (chargedOnly.get() && mc.player.getAttackCooldownProgress(0.5f) < 0.9f) return;
        if (mc.player.fallDistance < minFall.get()) return;

        // A spear reaches further than a mace: leave the hit alone if the mace could not reach
        if (hit.getPos().distanceTo(mc.player.getEyePos()) > mc.player.getEntityInteractionRange()) return;

        returnSlot = mc.player.getInventory().getSelectedSlot();
        maceSlot = slot;
        wait = backDelay.get();
        mc.player.getInventory().setSelectedSlot(slot);
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (returnSlot < 0) return;
        if (mc.player == null) {
            returnSlot = -1;
            return;
        }

        if (wait-- > 0) return;

        // Only if the mace is still what is selected: the player may have picked something else meanwhile
        if (swapBack.get() && mc.player.getInventory().getSelectedSlot() == maceSlot) {
            mc.player.getInventory().setSelectedSlot(returnSlot);
        }
        returnSlot = -1;
    }

    private int findMace() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(Items.MACE)) return i;
        }
        return -1;
    }

    private boolean isWeapon(ItemStack stack) {
        // Registry name without the namespace, e.g. diamond_sword
        String name = Registries.ITEM.getId(stack.getItem()).getPath();
        return name.endsWith("_sword") || name.endsWith("_axe") || name.endsWith("_spear") || name.equals("trident");
    }
}
