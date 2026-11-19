package aicoder.combat;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.AbstractContainerScreenAccessor;
import meteordevelopment.meteorclient.mixin.KeyMappingAccessor;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Items;
import org.lwjgl.sdl.SDLMouse;

/**
 * Keeps a totem in the off hand the way a player does it by hand, only faster: when the off hand has no
 * totem, the module presses the inventory key, moves the mouse onto a totem, presses the swap-with-off-hand
 * key, and closes the inventory again.
 *
 * <p>The real cursor moves and the real inventory screen opens, so you see it happen and cannot do
 * anything else for those few ticks. The delay setting is the number of ticks between the steps.
 */
public class OffhandTotem extends HintModule {
    private enum Stage { Idle, Open, Hover, Close }

    /** Size of the inventory window, and how far the recipe book pushes it aside when it is open. */
    private static final int WIDTH = 176, HEIGHT = 166;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Ticks between the steps: open, move the mouse, swap, close. 0 is as fast as the game allows.")
        .defaultValue(1)
        .range(0, 20)
        .sliderRange(0, 10)
        .build()
    );

    private final Setting<Integer> health = sgGeneral.add(new IntSetting.Builder()
        .name("below-health")
        .description("Only refill when your health is at or below this many half hearts. 20 always refills.")
        .defaultValue(20)
        .range(1, 20)
        .sliderRange(1, 20)
        .build()
    );

    private final Setting<Boolean> close = sgGeneral.add(new BoolSetting.Builder()
        .name("close-after")
        .description("Close the inventory again afterwards.")
        .defaultValue(true)
        .build()
    );

    private Stage stage = Stage.Idle;
    private int wait, tries, rest;
    private Slot wanted;

    public OffhandTotem() {
        super(AiCoderTab.UTILITY, "offhand-totem", "When your off hand has no totem: opens the inventory, moves the mouse onto one, swaps it into the off hand and closes the inventory.");
    }

    @Override
    public void onActivate() {
        stage = Stage.Idle;
        rest = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.level == null || !mc.player.isAlive()) {
            stage = Stage.Idle;
            return;
        }
        if (wait > 0) {
            wait--;
            return;
        }

        switch (stage) {
            case Idle -> {
                if (rest > 0) { rest--; return; }
                if (mc.gui.screen() != null || mc.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) return;
                if (mc.player.getHealth() > health.get() || !hasTotem()) return;

                // The inventory key, exactly as if it had been pressed
                KeyMapping.click(((KeyMappingAccessor) mc.options.keyInventory).meteor$getKey());
                stage = Stage.Open;
                tries = 0;
                wait = delay.get();
            }
            case Open -> {
                if (!(mc.gui.screen() instanceof AbstractContainerScreen<?> screen) || screen.getMenu().slots.size() != 46) {
                    // The inventory did not come up, or it is the creative one, which is laid out differently
                    if (++tries > 5) {
                        if (mc.gui.screen() != null) warning("Only works with the survival inventory, not in creative mode.");
                        finish();
                        rest = 100;
                    }
                    return;
                }
                wanted = null;
                for (Slot slot : screen.getMenu().slots) {
                    // 9..44 are the main inventory and the hotbar
                    if (slot.index >= 9 && slot.index <= 44 && slot.hasItem() && slot.getItem().is(Items.TOTEM_OF_UNDYING)) { wanted = slot; break; }
                }
                if (wanted == null) { finish(); return; }
                tries = 0;
                point(screen);
                stage = Stage.Hover;
                // The game notices the slot under the mouse when it draws the next frame
                wait = Math.max(1, delay.get());
            }
            case Hover -> {
                if (!(mc.gui.screen() instanceof AbstractContainerScreen<?> screen)) { stage = Stage.Idle; return; }

                if (((AbstractContainerScreenAccessor) screen).meteor$getHoveredSlot() != wanted) {
                    // Not on it yet: a slow frame, or the window sits elsewhere because the recipe book is open.
                    // Every second look tries the other place.
                    if (++tries > 9) { warning("Could not get the mouse onto the totem."); finish(); rest = 40; return; }
                    point(screen);
                    wait = 1;
                    return;
                }
                // The swap-with-off-hand key over the hovered slot
                int code = ((KeyMappingAccessor) mc.options.keySwapOffhand).meteor$getKey().getValue();
                screen.keyPressed(new KeyEvent(code, 0, 0));
                stage = Stage.Close;
                wait = delay.get();
            }
            case Close -> finish();
        }
    }

    /** Puts the cursor on the middle of the wanted slot. Tries 2-3, 6-7 assume the recipe book is open. */
    private void point(AbstractContainerScreen<?> screen) {
        int left = tries / 2 % 2 == 0 ? (screen.width - WIDTH) / 2 : 177 + (screen.width - WIDTH - 200) / 2;
        int top = (screen.height - HEIGHT) / 2;
        double toWindow = mc.getWindow().getScreenWidth() / (double) screen.width;
        double mouseX = (left + wanted.x + 8) * toWindow, mouseY = (top + wanted.y + 8) * toWindow;
        SDLMouse.SDL_WarpMouseInWindow(mc.getWindow().handle(), (float) mouseX, (float) mouseY);
        // Twice, one pixel apart: on some systems the window library swallows the mouse-move event of the
        // last place it put the cursor itself, and the game would never hear of the move
        mouseX += 1;
        SDLMouse.SDL_WarpMouseInWindow(mc.getWindow().handle(), (float) mouseX, (float) mouseY);
    }

    private void finish() {
        if (close.get() && mc.gui.screen() instanceof AbstractContainerScreen<?> screen) screen.onClose();
        stage = Stage.Idle;
        rest = 4;
    }

    private boolean hasTotem() {
        for (int i = 0; i < 36; i++) {
            if (mc.player.getInventory().getItem(i).is(Items.TOTEM_OF_UNDYING)) return true;
        }
        return false;
    }
}
