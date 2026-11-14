package aicoder.assist.modules;

import aicoder.assist.api.*;
import aicoder.assist.api.Module;
import aicoder.assist.api.Events.*;

import static aicoder.assist.Client.mc;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/**
 * Chains spear lunges while a key is held. Each round: jump, select the spear, left click (the jab, which
 * with the Lunge enchantment throws you forward), and select the previous slot again on the next tick.
 *
 * <p>Switching away straight after the jab is what lets the next one come quickly: the spear is not what
 * you are holding while its cooldown would run. Lunge costs hunger and stops working below three hunger
 * bars, so the module only starts with at least that much and stops when it runs out, or when the key is
 * released. Every step is a key press: jump key, hotbar slot, attack key.
 */
public class SpearLunge extends Module {
    public enum HoldKey {
        R(InputConstants.KEY_R, false),
        V(InputConstants.KEY_V, false),
        G(InputConstants.KEY_G, false),
        C(InputConstants.KEY_C, false),
        X(InputConstants.KEY_X, false),
        Z(InputConstants.KEY_Z, false),
        B(InputConstants.KEY_B, false),
        LeftAlt(InputConstants.KEY_LALT, false),
        Mouse4(3, true),
        Mouse5(4, true);

        final int code;
        final boolean mouse;

        HoldKey(int code, boolean mouse) {
            this.code = code;
            this.mouse = mouse;
        }
    }

    private enum Stage {
        Idle,
        /** Jump pressed; waiting to leave the ground before the jab. */
        Rise,
        /** The spear is selected and the click has gone out; select the previous slot again. */
        Back
    }

    /** Three hunger bars. Lunge does nothing below this. */
    private static final int MIN_FOOD = 6;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<HoldKey> holdKey = sgGeneral.add(new EnumSetting.Builder<HoldKey>()
        .name("hold-key")
        .description("The module works only while this key is held, and stops the moment you let go.")
        .defaultValue(HoldKey.R)
        .build()
    );

    private final Setting<Boolean> jump = sgGeneral.add(new BoolSetting.Builder()
        .name("jump")
        .description("Jump before each lunge when you are on the ground.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> jumpLead = sgGeneral.add(new IntSetting.Builder()
        .name("jump-lead")
        .description("Ticks between pressing jump and the jab.")
        .defaultValue(2)
        .range(0, 20)
        .sliderRange(0, 10)
        .visible(jump::get)
        .build()
    );

    private final Setting<Integer> repeatDelay = sgGeneral.add(new IntSetting.Builder()
        .name("repeat-delay")
        .description("Ticks to wait after one lunge before starting the next.")
        .defaultValue(8)
        .range(0, 60)
        .sliderRange(0, 30)
        .build()
    );

    private Stage stage = Stage.Idle;
    private int wait;
    private int restSlot = -1;
    private int spearSlot = -1;
    private boolean pressedJump;

    public SpearLunge() {
        super(Category.MELEE, "spear-lunge", "Hold a key to chain spear lunges: jump, switch to the spear, jab, and switch straight back, until you let go or run low on hunger.");
    }

    @Override
    public void onActivate() {
        stage = Stage.Idle;
        wait = 0;
        restSlot = -1;
    }

    @Override
    public void onDeactivate() {
        stop();
    }

    private boolean keyHeld() {
        HoldKey key = holdKey.get();
        return key.mouse ? Input.isButtonPressed(key.code) : Input.isKeyPressed(key.code);
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.level == null) return;

        releaseJump();

        // The switch back always happens, whatever else has changed since the jab
        if (stage == Stage.Back) {
            restore();
            stage = Stage.Idle;
            wait = repeatDelay.get();
            return;
        }

        if (mc.screen != null || !keyHeld() || mc.player.getFoodData().getFoodLevel() < MIN_FOOD) {
            stop();
            return;
        }

        if (wait > 0) {
            wait--;
            return;
        }

        if (stage == Stage.Idle) {
            spearSlot = findSpear();
            if (spearSlot < 0) return;

            // Where to go back to: the slot in hand, or if that is the spear itself, any other slot
            int current = mc.player.getInventory().getSelectedSlot();
            restSlot = current != spearSlot ? current : otherSlot(spearSlot);

            if (jump.get() && mc.player.onGround()) {
                mc.options.keyJump.setDown(true);
                pressedJump = true;
                stage = Stage.Rise;
                wait = jumpLead.get();
                if (wait > 0) return;
            }
            jab();
            return;
        }

        if (stage == Stage.Rise) jab();
    }

    /** Selects the spear and presses attack; the game handles the press later in this same tick. */
    private void jab() {
        mc.player.getInventory().setSelectedSlot(spearSlot);
        KeyMapping.click(Input.keyOf(mc.options.keyAttack));
        stage = Stage.Back;
    }

    private void restore() {
        if (restSlot >= 0 && restSlot < 9 && mc.player.getInventory().getSelectedSlot() == spearSlot) {
            mc.player.getInventory().setSelectedSlot(restSlot);
        }
    }

    private void releaseJump() {
        if (!pressedJump) return;
        pressedJump = false;
        // Leave the key as the keyboard has it, in case the player is holding jump too
        mc.options.keyJump.setDown(Input.isPressed(mc.options.keyJump));
    }

    private void stop() {
        if (mc.player != null && stage == Stage.Back) restore();
        releaseJump();
        stage = Stage.Idle;
        wait = 0;
    }

    private int findSpear() {
        for (int i = 0; i < 9; i++) {
            if (isSpear(mc.player.getInventory().getItem(i))) return i;
        }
        return -1;
    }

    private int otherSlot(int spear) {
        for (int i = 0; i < 9; i++) {
            if (i != spear && !isSpear(mc.player.getInventory().getItem(i))) return i;
        }
        return (spear + 1) % 9;
    }

    private boolean isSpear(ItemStack stack) {
        return !stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().endsWith("_spear");
    }
}
