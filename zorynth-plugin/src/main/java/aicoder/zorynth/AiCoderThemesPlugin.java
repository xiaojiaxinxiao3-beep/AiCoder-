package aicoder.zorynth;

import dev.zorynth.api.PluginApi;
import java.util.ArrayList;
import java.util.List;

/**
 * The AiCoder Meteor themes as a Zorynth plugin (API 1).
 * One module per theme. Turning a module on applies its colours (context.theme) and draws its
 * animated backdrop and corner frame (context.ui). Only one theme stays on at a time.
 */
public final class AiCoderThemesPlugin implements PluginApi.Plugin {
    private record Entry(Style style, PluginApi.ModuleHandle module, PluginApi.Value<Boolean> backdrop,
                         PluginApi.Value<Boolean> frame, PluginApi.Value<Boolean> animations,
                         PluginApi.Value<Double> strength) {}

    private final List<Entry> entries = new ArrayList<>();
    private boolean[] wasOn;

    @Override public void onLoad(PluginApi.Context context) {
        PluginApi.Layer back = backLayer();
        int priority = 10;
        for (Style style : Style.ALL) {
            var module = context.module(new PluginApi.ModuleSpec(style.id(), style.name(), PluginApi.Category.CLIENT, false),
                new PluginApi.Module() {
                    @Override public void tick(PluginApi.ClientView client) { sync(); }
                    @Override public String displayData() { return style.tag(); }
                });
            Entry entry = new Entry(style, module,
                module.bool("backdrop", "Backdrop", true),
                module.bool("frame", "Frame", true),
                module.bool("animations", "Animations", true),
                module.number("strength", "Strength", .8, .2, 1, .05));
            entries.add(entry);
            context.theme(style.id(), priority, new PluginApi.Theme(style.primary(), style.text(), style.secondary()));
            if (back != null) context.ui(style.id() + "_backdrop", style.id(), back, priority, new PluginApi.Ui() {
                @Override public void render(PluginApi.Canvas canvas, PluginApi.UiView view) {
                    sync();
                    if (entry.backdrop().get()) Backdrops.render(canvas, style, time(entry), entry.strength().get());
                }
                @Override public boolean input(PluginApi.UiView view, PluginApi.InputEvent event) { return false; }
            });
            context.ui(style.id() + "_frame", style.id(), PluginApi.Layer.FOREGROUND, priority, new PluginApi.Ui() {
                @Override public void render(PluginApi.Canvas canvas, PluginApi.UiView view) {
                    sync();
                    if (entry.frame().get()) Backdrops.frame(canvas, style, time(entry));
                }
                @Override public boolean input(PluginApi.UiView view, PluginApi.InputEvent event) { return false; }
            });
            priority++;
        }
        wasOn = new boolean[entries.size()];
    }

    /** The layer that is not FOREGROUND, looked up by value so no unconfirmed constant name is compiled in. */
    private static PluginApi.Layer backLayer() {
        for (PluginApi.Layer layer : PluginApi.Layer.values()) if (layer != PluginApi.Layer.FOREGROUND) return layer;
        return null;
    }

    private static double time(Entry entry) {
        return entry.animations().get() ? (System.currentTimeMillis() % 3_600_000L) / 1000.0 : 0;
    }

    /** When a theme has just been switched on, switch the others off. */
    private void sync() {
        int fresh = -1;
        for (int i = 0; i < entries.size(); i++) {
            boolean on = entries.get(i).module().enabled();
            if (on && !wasOn[i]) fresh = i;
            wasOn[i] = on;
        }
        if (fresh < 0) return;
        for (int i = 0; i < entries.size(); i++) {
            if (i == fresh || !wasOn[i]) continue;
            entries.get(i).module().setEnabled(false);
            wasOn[i] = false;
        }
    }
}
