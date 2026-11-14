package aicoder.zorynth;

import dev.zorynth.api.PluginApi;
import java.util.ArrayList;
import java.util.List;

/**
 * The AiCoder Meteor themes as a Zorynth plugin (API 1, UI extension 2).
 * One module per theme. Turning one on replaces the client's modules screen with that theme's
 * own screen (ThemeScreen) and applies its three colours to the HUD. Only one stays on at a time.
 * On a client without UI extension 2 the same modules fall back to colours, a backdrop layer and a frame layer.
 */
public final class AiCoderThemesPlugin implements PluginApi.Plugin {
    private record Entry(Style style, PluginApi.ModuleHandle module) {}

    private final List<Entry> entries = new ArrayList<>();
    private boolean[] wasOn;

    @Override public void onLoad(PluginApi.Context context) {
        boolean full = context.uiVersion() >= 2;
        PluginApi.UiHost host = full ? context.uiHost() : null;
        int priority = 100;
        for (Style style : Style.ALL) {
            int index = entries.size();
            var module = context.module(new PluginApi.ModuleSpec(style.id(), style.name(), PluginApi.Category.CLIENT, false),
                new PluginApi.Module() {
                    @Override public void onEnable() { sync(); }
                    @Override public void tick(PluginApi.ClientView client) { sync(); }
                    @Override public String displayData() { return style.tag(); }
                });
            entries.add(new Entry(style, module));
            var layout = module.choice("layout", "Layout", "Window", "Window", "Columns");
            var backdrop = module.bool("backdrop", "Backdrop", true);
            var animations = module.bool("animations", "Animations", true);
            var strength = module.number("strength", "Backdrop Strength", .8, .2, 1, .05);
            context.theme(style.id(), priority, new PluginApi.Theme(style.primary(), style.light() ? 0xFFF4F6FA : style.text(), style.light() ? 0xFFA8B0BC : style.secondary()));
            if (full) {
                ThemeScreen screen = new ThemeScreen(style, host, layout, backdrop, animations, strength, new ThemeScreen.Actions() {
                    @Override public void theme(int step) {
                        int next = Math.floorMod(index + step, entries.size());
                        entries.get(next).module().setEnabled(true);
                        module.setEnabled(false);
                        sync();
                        host.save();
                    }
                    @Override public void original() { module.setEnabled(false); sync(); host.save(); }
                });
                context.replaceScreen(style.id() + "_modules", style.id(), PluginApi.ScreenTarget.MODULES, priority, new PluginApi.Ui() {
                    @Override public void render(PluginApi.Canvas canvas, PluginApi.UiView view) { sync(); screen.render(canvas, view); }
                    @Override public boolean input(PluginApi.UiView view, PluginApi.InputEvent event) { return screen.input(view, event); }
                    @Override public void closed() { screen.closed(); }
                });
            } else {
                var frame = module.bool("frame", "Frame", true);
                PluginApi.Ui back = new PluginApi.Ui() {
                    @Override public void render(PluginApi.Canvas canvas, PluginApi.UiView view) {
                        sync();
                        if (backdrop.get()) Backdrops.render(canvas, style, time(animations), strength.get());
                    }
                };
                PluginApi.Ui front = new PluginApi.Ui() {
                    @Override public void render(PluginApi.Canvas canvas, PluginApi.UiView view) {
                        if (frame.get()) Backdrops.frame(canvas, style, time(animations));
                    }
                };
                context.ui(style.id() + "_backdrop", style.id(), PluginApi.Layer.BACKGROUND, priority, back);
                context.ui(style.id() + "_frame", style.id(), PluginApi.Layer.FOREGROUND, priority, front);
            }
            priority++;
        }
        wasOn = new boolean[entries.size()];
    }

    private static double time(PluginApi.Value<Boolean> animations) {
        return animations.get() ? (System.currentTimeMillis() % 3_600_000L) / 1000.0 : 0;
    }

    /** When a theme has just been switched on, switch the others off. */
    private void sync() {
        if (wasOn == null) return;
        int fresh = -1;
        for (int i = 0; i < entries.size(); i++) {
            boolean on = entries.get(i).module().enabled();
            if (on && !wasOn[i]) fresh = i;
            wasOn[i] = on;
        }
        if (fresh < 0) return;
        for (int i = 0; i < entries.size(); i++) {
            if (i == fresh || !wasOn[i]) continue;
            wasOn[i] = false;
            entries.get(i).module().setEnabled(false);
        }
    }
}
