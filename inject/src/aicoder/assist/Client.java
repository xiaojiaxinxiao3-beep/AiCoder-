package aicoder.assist;

import aicoder.assist.api.EventHandler;
import aicoder.assist.api.Events;
import aicoder.assist.api.Module;
import aicoder.assist.api.Setting;
import aicoder.assist.api.SettingGroup;
import aicoder.assist.aurora.AuroraScreen;
import aicoder.assist.modules.AnchorMacro;
import aicoder.assist.modules.CrosshairDrift;
import aicoder.assist.modules.CrystalMacro;
import aicoder.assist.modules.LeftClicker;
import aicoder.assist.modules.MaceDive;
import aicoder.assist.modules.MaceSwap;
import aicoder.assist.modules.RightClicker;
import aicoder.assist.modules.SpearLunge;
import aicoder.assist.modules.WindPearl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/** AiCoder Assist itself: the modules, the events that reach them, the key that opens the menu, and the saved settings. */
public final class Client {
    public static final String NAME = "AiCoder Assist";
    public static final Minecraft mc = Minecraft.getInstance();
    public static final List<Module> modules = new ArrayList<>();

    private record Handler(Module module, Method method, int priority) {
    }

    private static final Map<Class<?>, List<Handler>> handlers = new HashMap<>();
    private static final Events.TickEvent.Pre TICK = new Events.TickEvent.Pre();
    /** Which of the two aurora layouts the menu shows: "columns" or "window". */
    public static String layout = "columns";
    /** Drifting aurora, breathing highlights and fades. */
    public static boolean animations = true;

    private static final int MENU_KEY = GLFW.GLFW_KEY_RIGHT_SHIFT;
    private static boolean menuRequested;
    private static boolean started;

    private Client() {
    }

    /** Called once, from the first tick after injection, on the game's own thread. */
    private static void start() {
        add(new AnchorMacro());
        add(new CrystalMacro());
        add(new CrosshairDrift());
        add(new LeftClicker());
        add(new MaceDive());
        add(new MaceSwap());
        add(new RightClicker());
        add(new SpearLunge());
        add(new WindPearl());
        load();
        System.out.println("[" + NAME + "] ready, " + modules.size() + " modules. Right Shift opens the menu.");
    }

    private static void add(Module module) {
        modules.add(module);

        for (Method method : module.getClass().getDeclaredMethods()) {
            EventHandler mark = method.getAnnotation(EventHandler.class);
            if (mark == null || method.getParameterCount() != 1) continue;

            method.setAccessible(true);
            List<Handler> list = handlers.computeIfAbsent(method.getParameterTypes()[0], k -> new ArrayList<>());
            list.add(new Handler(module, method, mark.priority()));
            list.sort(Comparator.comparingInt(Handler::priority).reversed());
        }
    }

    /** Hands an event to every module that is switched on and listens for it. */
    public static void post(Object event) {
        List<Handler> list = handlers.get(event.getClass());
        if (list == null) return;

        for (Handler handler : list) {
            if (!handler.module.isActive()) continue;
            try {
                handler.method.invoke(handler.module, event);
            } catch (Throwable t) {
                // A module that fails is switched off rather than left to fail on every tick
                t.printStackTrace();
                handler.module.setActive(false);
                chat(handler.module.title, "Stopped after an error: " + (t.getCause() != null ? t.getCause() : t), 0xFFFF6F8A);
            }
        }
    }

    /**
     * From the game's keyboard handler. The menu itself opens on the next tick, outside the key handling.
     * Module binds only act while no screen is open, so typing in chat does not switch modules; a release
     * always counts, so a held module cannot get stuck on when a screen opens over it.
     */
    static void key(int key, boolean pressed) {
        if (pressed && key == MENU_KEY) menuRequested = true;

        for (Module module : modules) {
            if (module.bind != key) continue;

            if (module.holdBind) {
                if (!pressed) module.setActive(false);
                else if (mc.screen == null) module.setActive(true);
            } else if (pressed && mc.screen == null) {
                module.toggle();
            }
        }
    }

    public static boolean isMenuKey(int key) {
        return key == MENU_KEY;
    }

    static void tick() {
        if (!started) {
            started = true;
            start();
        }

        if (menuRequested) {
            menuRequested = false;
            if (mc.screen == null || mc.screen instanceof TitleScreen) mc.setScreen(new AuroraScreen());
        }

        post(TICK);
    }

    public static void chat(String from, String message, int color) {
        if (mc.gui == null || mc.level == null) return;

        MutableComponent text = Component.literal("[" + from + "] ").withStyle(Style.EMPTY.withColor(color & 0xFFFFFF));
        text.append(Component.literal(message).withStyle(Style.EMPTY.withColor(0xEEFAFF)));
        mc.gui.getChat().addMessage(text);
    }

    // Saved settings: one plain text file in the game folder

    private static File file() {
        return new File(mc.gameDirectory, "aicoder-assist.properties");
    }

    public static void save() {
        Properties out = new Properties();
        out.setProperty("ui.layout", layout);
        out.setProperty("ui.animations", String.valueOf(animations));
        for (Module module : modules) {
            // A module that is only on because its key is held right now is saved as off
            out.setProperty(module.name + ".enabled", String.valueOf(module.isActive() && !(module.holdBind && module.bind >= 0)));
            out.setProperty(module.name + ".bind", String.valueOf(module.bind));
            out.setProperty(module.name + ".bind-mode", module.holdBind ? "hold" : "toggle");
            for (SettingGroup group : module.settings.groups) {
                for (Setting<?> setting : group.settings) out.setProperty(module.name + "." + setting.name, setting.save());
            }
        }

        try (FileOutputStream stream = new FileOutputStream(file())) {
            out.store(stream, NAME);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void load() {
        File file = file();
        if (!file.exists()) return;

        Properties in = new Properties();
        try (FileInputStream stream = new FileInputStream(file)) {
            in.load(stream);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        layout = "window".equals(in.getProperty("ui.layout")) ? "window" : "columns";
        animations = !"false".equals(in.getProperty("ui.animations"));

        for (Module module : modules) {
            for (SettingGroup group : module.settings.groups) {
                for (Setting<?> setting : group.settings) {
                    String text = in.getProperty(module.name + "." + setting.name);
                    if (text == null) continue;
                    try {
                        setting.load(text);
                    } catch (RuntimeException ignored) {
                        // A value that no longer parses keeps its default
                    }
                }
            }
            try {
                module.bind = Integer.parseInt(in.getProperty(module.name + ".bind", "-1").trim());
            } catch (NumberFormatException ignored) {
                module.bind = -1;
            }
            module.holdBind = "hold".equals(in.getProperty(module.name + ".bind-mode"));
            if (Boolean.parseBoolean(in.getProperty(module.name + ".enabled"))) module.setActive(true);
        }
    }
}
