package aicoder.assist.compat;

import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/**
 * A 1.8.9 screen that hears about the mouse and the keyboard the way a screen of the newer versions
 * does, so the menu is written once.
 */
public abstract class Screen extends GuiScreen {
    protected final Minecraft minecraft = Minecraft.getInstance();
    private long lastFrame;
    private double frameSeconds;

    protected Screen(Component title) {
    }

    public boolean isPauseScreen() {
        return true;
    }

    public void onClose() {
        mc.displayGuiScreen(null);
        if (mc.currentScreen == null) mc.setIngameFocus();
    }

    public abstract void render(GuiGraphics graphics, int mouseX, int mouseY, float partial);

    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) { return false; }
    public boolean mouseReleased(MouseButtonEvent click) { return false; }
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) { return false; }
    public boolean keyPressed(KeyEvent event) { return false; }
    public boolean keyReleased(KeyEvent event) { return false; }

    /** Seconds the last frame took, at most a tenth. */
    protected double frameSeconds() {
        return frameSeconds;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return isPauseScreen();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partial) {
        long now = System.nanoTime();
        frameSeconds = lastFrame == 0 ? 0 : Math.min(0.1, (now - lastFrame) / 1e9);
        lastFrame = now;
        render(null, mouseX, mouseY, partial);
    }

    @Override
    protected void mouseClicked(int x, int y, int button) {
        mouseClicked(new MouseButtonEvent(x, y, button), false);
    }

    @Override
    protected void mouseReleased(int x, int y, int button) {
        if (button >= 0) mouseReleased(new MouseButtonEvent(x, y, button));
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            double x = Mouse.getEventX() * (double) width / mc.displayWidth;
            double y = height - Mouse.getEventY() * (double) height / mc.displayHeight - 1;
            mouseScrolled(x, y, 0, wheel / 120.0);
        }
    }

    @Override
    public void handleKeyboardInput() {
        int key = Keys.glfw(Keyboard.getEventKey());
        if (Keyboard.getEventKeyState()) {
            if (!keyPressed(new KeyEvent(key)) && key == 256) onClose();
        } else {
            keyReleased(new KeyEvent(key));
        }
        // The game's own handling of every key event; the menu key is heard there
        mc.dispatchKeypresses();
    }
}
