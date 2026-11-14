package dev.zorynth.api;

import java.nio.file.Path;
import java.util.List;

/**
 * Compile-only stand-in for sdk/zorynth-plugin-api-1.jar, which was not in the template archive.
 * Every signature here was read from the template's compiled ExamplePlugin classes or its README;
 * nothing else is declared, so the plugin cannot depend on anything unconfirmed.
 * Never packaged into the plugin jar. Replace with the real SDK jar when you have it.
 */
public final class PluginApi {
    private PluginApi() {}

    public interface Plugin {
        void onLoad(Context context);
        default void onShutdown() {}
    }

    public enum Category { CLIENT }
    public enum Layer { FOREGROUND, BACKGROUND }
    public enum InputKind { CLICK }

    public record ModuleSpec(String id, String name, Category category, boolean enabled) {}
    public record HudSpec(String id, String name, boolean visible, double x, double y, int width, int height) {}
    public record Theme(int primary, int text, int secondary) {}
    public record UiView(int width, int height) {}
    public record ClientView(boolean connected, String playerName, int fps) {}
    public record InputEvent(InputKind kind, int button, double x, double y) {}

    public interface Value<T> { T get(); }

    public interface Module {
        default void tick(ClientView client) {}
        default String displayData() { return null; }
        default void onDisconnect() {}
    }

    public interface ModuleHandle {
        boolean enabled();
        void setEnabled(boolean enabled);
        Value<String> text(String id, String name, String value, int maxLength);
        Value<Boolean> bool(String id, String name, boolean value);
        Value<Integer> color(String id, String name, int argb);
        Value<Double> number(String id, String name, double value, double min, double max, double step);
        Value<String> choice(String id, String name, String value, String... options);
    }

    public interface Canvas {
        int width();
        int height();
        void fill(int x, int y, int width, int height, int argb);
        void text(String text, int x, int y, int argb, boolean shadow);
    }

    public interface Hud { void render(Canvas canvas, ClientView client, boolean editing); }
    public interface HudTransform { List<String> lines(ClientView client, List<String> original); }

    public interface Ui {
        void render(Canvas canvas, UiView view);
        default boolean input(UiView view, InputEvent event) { return false; }
    }

    public interface Context {
        ModuleHandle module(ModuleSpec spec, Module module);
        void hud(HudSpec spec, Hud hud);
        void theme(String moduleId, int priority, Theme theme);
        void decorateHud(String hudId, String moduleId, int priority, HudTransform transform);
        void ui(String id, String moduleId, Layer layer, int priority, Ui ui);
        Path dataDirectory();
    }
}
