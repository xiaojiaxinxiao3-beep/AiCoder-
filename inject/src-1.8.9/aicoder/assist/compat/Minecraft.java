package aicoder.assist.compat;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.MovingObjectPosition;

import java.io.File;

/**
 * The 1.8.9 game seen through the names the newer versions use. The modules and the menu are written
 * once, against the 1.21 names (mc.player.getX(), mc.options.keyUp.setDown(...)); here each of those is
 * answered by the 1.8.9 game. The fields are refreshed by {@link #sync} at the start of every tick, every
 * frame and every key event, so a null player or world reads as null here too.
 */
public final class Minecraft {
    private static final Minecraft INSTANCE = new Minecraft();
    private final net.minecraft.client.Minecraft real = net.minecraft.client.Minecraft.getMinecraft();

    public Player player;
    public Level level;
    /** The open screen (a GuiScreen) or null. */
    public Object screen;
    public HitResult hitResult;
    public Options options;
    public final File gameDirectory = real.mcDataDir;

    private final Player playerShim = new Player();
    private final Level levelShim = new Level();
    private final HitResult hitShim = new HitResult();

    private Minecraft() {
    }

    public static Minecraft getInstance() {
        return INSTANCE;
    }

    public net.minecraft.client.Minecraft real() {
        return real;
    }

    public void sync() {
        player = real.thePlayer == null ? null : playerShim;
        level = real.theWorld == null ? null : levelShim;
        screen = real.currentScreen;
        MovingObjectPosition over = real.objectMouseOver;
        hitResult = over == null ? null : hitShim;
        if (options == null || options.settings != real.gameSettings) options = new Options(real.gameSettings);
    }

    public void setScreen(Object screen) {
        real.displayGuiScreen((GuiScreen) screen);
        this.screen = real.currentScreen;
    }

    public int getFps() {
        return net.minecraft.client.Minecraft.getDebugFPS();
    }

    /** The key bindings, under their newer names. */
    public static final class Options {
        final GameSettings settings;
        public final Key keyUp, keyDown, keyLeft, keyRight, keyJump, keySprint, keyShift, keyUse, keyAttack;

        Options(GameSettings s) {
            settings = s;
            keyUp = new Key(s.keyBindForward);
            keyDown = new Key(s.keyBindBack);
            keyLeft = new Key(s.keyBindLeft);
            keyRight = new Key(s.keyBindRight);
            keyJump = new Key(s.keyBindJump);
            keySprint = new Key(s.keyBindSprint);
            keyShift = new Key(s.keyBindSneak);
            keyUse = new Key(s.keyBindUseItem);
            keyAttack = new Key(s.keyBindAttack);
        }
    }
}
