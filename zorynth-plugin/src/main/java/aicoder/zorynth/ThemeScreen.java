package aicoder.zorynth;

import dev.zorynth.api.PluginApi;
import dev.zorynth.api.PluginApi.Canvas;
import dev.zorynth.api.PluginApi.ModuleControl;
import dev.zorynth.api.PluginApi.SettingControl;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;

/**
 * The whole modules screen for one theme, in two layouts.
 * WINDOW: one window, categories | modules | settings. Click a row to select it, click its switch
 * or right-click the row to toggle. COLUMNS: one panel per category. Left click toggles, right click
 * opens the settings. Typing filters by name in both.
 */
final class ThemeScreen implements PluginApi.Ui {
    interface Actions { void theme(int step); void original(); }

    private record Hit(int x, int y, int w, int h, Runnable left, Runnable right, DoubleConsumer drag) {
        boolean has(double mx, double my) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    }
    private record Zone(int x, int y, int w, int h, IntConsumer scroll) {}

    private static final int ROW = 20;

    private final Style s;
    private final PluginApi.UiHost host;
    private final PluginApi.Value<String> layout;
    private final PluginApi.Value<Boolean> backdrop, animations;
    private final PluginApi.Value<Double> strength;
    private final Actions actions;

    private final List<Hit> hits = new ArrayList<>();
    private final List<Zone> zones = new ArrayList<>();
    private final Map<String, Integer> columnScroll = new HashMap<>();
    private int clipX, clipY, clipW = Integer.MAX_VALUE, clipH = Integer.MAX_VALUE;

    private String category, search = "", edit = "";
    private ModuleControl selected, popup, binding;
    private SettingControl editing;
    private DoubleConsumer dragging;
    private int moduleScroll, settingScroll;
    private double t;

    ThemeScreen(Style s, PluginApi.UiHost host, PluginApi.Value<String> layout, PluginApi.Value<Boolean> backdrop,
                PluginApi.Value<Boolean> animations, PluginApi.Value<Double> strength, Actions actions) {
        this.s = s; this.host = host; this.layout = layout; this.backdrop = backdrop;
        this.animations = animations; this.strength = strength; this.actions = actions;
    }

    // ---- render ----

    @Override public void render(Canvas c, PluginApi.UiView view) {
        hits.clear(); zones.clear();
        t = animations.get() ? (System.currentTimeMillis() % 3_600_000L) / 1000.0 : 0;
        int w = view.width(), h = view.height();
        mouseX = view.mouseX(); mouseY = view.mouseY();
        if (backdrop.get()) Backdrops.render(c, s, t, strength.get());
        else c.fill(0, 0, w, h, 0x90000000);
        List<ModuleControl> modules = host.modules();
        List<String> categories = new ArrayList<>();
        for (ModuleControl m : modules) if (!categories.contains(m.category())) categories.add(m.category());
        if (category == null || !categories.contains(category)) category = categories.isEmpty() ? "" : categories.get(0);
        boolean columns = layout.get().equals("Columns");
        if (columns) columns(c, w, h, modules, categories); else window(c, w, h, modules, categories);
        topBar(c, w, columns);
        if (columns && popup != null) popup(c, w, h);
    }

    private void topBar(Canvas c, int w, boolean columns) {
        String name = s.name().replace("AiCoder ", "");
        int nameW = Math.max(74, c.textWidth(name) + 12);
        int[] widths = {c.textWidth("WINDOW") + 12, c.textWidth("COLUMNS") + 12, 14, nameW, 14, c.textWidth("ORIGINAL UI") + 12};
        int total = 5 * 4 + 8;
        for (int v : widths) total += v;
        int x = Math.max(4, (w - total) / 2), y = 6;
        x = button(c, "WINDOW", x, y, widths[0], !columns, () -> { layout.set("Window"); popup = null; host.save(); }) + 4;
        x = button(c, "COLUMNS", x, y, widths[1], columns, () -> { layout.set("Columns"); host.save(); }) + 12;
        x = button(c, "<", x, y, widths[2], false, () -> actions.theme(-1)) + 4;
        Draw.framed(c, x, y, nameW, 16, s.rowCorner(), Draw.opaque(s.field(), 0xC0), s.fieldLine());
        c.text(name, x + (nameW - c.textWidth(name)) / 2, y + 4, s.accent(), false);
        x += nameW + 4;
        x = button(c, ">", x, y, widths[4], false, () -> actions.theme(1)) + 12;
        button(c, "ORIGINAL UI", x, y, widths[5], false, actions::original);
    }

    private int button(Canvas c, String label, int x, int y, int width, boolean active, Runnable action) {
        Draw.framed(c, x, y, width, 16, s.rowCorner(), active ? Draw.opaque(s.plateSelected(), 0xD0) : Draw.opaque(s.field(), 0xC0), active ? s.accent() : s.fieldLine());
        c.text(label, x + (width - c.textWidth(label)) / 2, y + 4, active ? (s.light() ? s.text() : 0xFFFFFFFF) : s.light() ? s.text() : s.dim(), false);
        hit(x, y, width, 16, action, null, null);
        return x + width;
    }

    // ---- window layout ----

    private void window(Canvas c, int w, int h, List<ModuleControl> modules, List<String> categories) {
        int ww = Math.min(840, w - 24), wh = Math.min(470, h - 44);
        int x = (w - ww) / 2, y = Math.max(28, 28 + (h - 34 - wh) / 2);
        Draw.panel(c, s, x, y, ww, wh, t);

        // Header
        int pad = Math.max(8, Math.abs(s.corner()));
        c.text("AICODER", x + pad + 2, y + 9, s.light() ? s.text() : 0xFFFFFFFF, !s.light());
        c.text("// " + s.tag(), x + pad + 8 + c.textWidth("AICODER"), y + 9, s.accent2(), false);
        String find = search.isEmpty() ? "TYPE TO SEARCH" : "SEARCH: " + search + (blink() ? "_" : "");
        c.text(find, x + ww - pad - 2 - c.textWidth(find), y + 9, search.isEmpty() ? s.faint() : s.accent(), false);
        c.fill(x + 6, y + 25, ww - 12, 1, Draw.alpha(s.accent(), 70));
        if (s.kind() == Style.Kind.CYBER) for (int i = x + 6; i < x + ww - 6; i += 12) c.fill(i, y + 26, 1, 2, Draw.alpha(s.accent(), 110));

        // Footer
        int fy = y + wh - 17;
        c.fill(x + 6, fy, ww - 12, 1, Draw.alpha(s.accent(), 40));
        int fx = x + pad + 2;
        if (s.kind() == Style.Kind.CYBER) { hazard(c, fx, fy + 5); fx += 46; }
        c.text(Draw.fit(c, s.footer(), ww - 150), fx, fy + 5, s.dim(), false);
        int on = 0;
        for (ModuleControl m : modules) if (m.enabled()) on++;
        String count = on + " / " + modules.size() + " ON";
        c.text(count, x + ww - pad - 2 - c.textWidth(count), fy + 5, s.accent(), false);

        int top = y + 30, height = fy - top - 3;
        int cw = Math.max(84, Math.min(170, ww / 5)), mw = Math.max(120, Math.min(280, ww * 34 / 100));
        int cx = x + 6, mx = cx + cw + 6, sx = mx + mw + 6, sw = x + ww - 6 - sx;

        // Categories
        for (int i = 0; i < categories.size(); i++) {
            String name = categories.get(i);
            int ry = top + i * (ROW + 2);
            if (ry + ROW > top + height) break;
            boolean active = name.equals(category) && search.isEmpty();
            Draw.box(c, cx, ry, cw, ROW, s.rowCorner(), active ? s.plateSelected() : s.plate());
            if (active) c.fill(cx, ry + 3, 2, ROW - 6, s.accent2());
            c.text(String.format("%02d", i + 1), cx + 7, ry + 6, active ? s.accent2() : s.faint(), false);
            c.text(Draw.fit(c, name.toUpperCase(Locale.ROOT), cw - 30), cx + 24, ry + 6, active ? (s.light() ? s.text() : 0xFFFFFFFF) : s.dim(), false);
            hit(cx, ry, cw, ROW, () -> { category = name; search = ""; moduleScroll = 0; }, null, null);
        }
        c.fill(mx - 4, top, 1, height, Draw.alpha(s.accent(), 34));
        c.fill(sx - 4, top, 1, height, Draw.alpha(s.accent(), 34));

        // Modules
        List<ModuleControl> shown = filter(modules, search.isEmpty() ? category : null);
        int step = ROW + 4;
        moduleScroll = clamp(moduleScroll, 0, Math.max(0, shown.size() * step - height));
        zones.add(new Zone(mx, top, mw, height, d -> moduleScroll -= d * step));
        clip(c, mx, top, mw, height);
        for (int i = 0; i < shown.size(); i++) {
            ModuleControl m = shown.get(i);
            int ry = top + i * step - moduleScroll;
            if (ry + step < top || ry > top + height) continue;
            boolean active = selected != null && selected.id().equals(m.id());
            Draw.box(c, mx, ry, mw, ROW + 2, s.rowCorner(), active ? s.plateSelected() : s.plate());
            if (active) {
                c.fill(mx, ry + 3, 2, ROW - 4, s.accent2());
                if (s.kind() == Style.Kind.CYBER) flow(c, mx + 3, ry + ROW, mw - 10);
            }
            c.text(Draw.fit(c, m.name(), mw - 34), mx + 8, ry + 7, m.enabled() ? (s.light() ? s.text() : 0xFFFFFFFF) : s.light() ? s.dim() : s.text(), false);
            Draw.toggle(c, s, mx + mw - 13, ry + 11, m.enabled(), t);
            hit(mx, ry, mw, ROW + 2, () -> select(m), () -> toggle(m), null);
            hit(mx + mw - 24, ry, 24, ROW + 2, () -> toggle(m), () -> toggle(m), null);
        }
        unclip(c);
        scrollbar(c, mx + mw + 1, top, height, shown.size() * step, moduleScroll);

        // Settings
        if (selected == null) {
            c.text("Select a module", sx + 8, top + 8, s.dim(), false);
            c.text("Left click: select   Right click or switch: toggle", sx + 8, top + 22, s.faint(), false);
        } else settings(c, selected, sx, top, sw, height);
    }

    // ---- columns layout ----

    private void columns(Canvas c, int w, int h, List<ModuleControl> modules, List<String> categories) {
        int n = Math.max(1, categories.size()), gap = 6, avail = w - 16;
        int cw = clamp((avail - (n - 1) * gap) / n, 96, 150);
        int perRow = Math.max(1, Math.min(n, (avail + gap) / (cw + gap)));
        int rows = (n + perRow - 1) / perRow, top = 30, availH = h - top - 22;
        int rowH = (availH - (rows - 1) * gap) / rows;
        for (int i = 0; i < categories.size(); i++) {
            String name = categories.get(i);
            List<ModuleControl> list = filter(modules, name);
            int inRow = Math.min(perRow, n - i / perRow * perRow);
            int x0 = (w - (inRow * cw + (inRow - 1) * gap)) / 2 + i % perRow * (cw + gap);
            int y0 = top + i / perRow * (rowH + gap);
            int content = list.size() * 17, height = Math.min(rowH, 24 + content + 4);
            Draw.panel(c, s, x0, y0, cw, height, t + i);
            c.text(String.format("%02d", i + 1), x0 + 7, y0 + 7, s.accent2(), false);
            c.text(Draw.fit(c, name.toUpperCase(Locale.ROOT), cw - 30), x0 + 23, y0 + 7, s.light() ? s.text() : 0xFFFFFFFF, false);
            c.fill(x0 + 5, y0 + 19, cw - 10, 1, Draw.alpha(s.accent(), 80));
            int listTop = y0 + 22, listH = height - 26;
            int scroll = clamp(columnScroll.getOrDefault(name, 0), 0, Math.max(0, content - listH));
            columnScroll.put(name, scroll);
            zones.add(new Zone(x0, y0, cw, height, d -> columnScroll.merge(name, -d * 17, Integer::sum)));
            clip(c, x0, listTop, cw, listH);
            for (int k = 0; k < list.size(); k++) {
                ModuleControl m = list.get(k);
                int ry = listTop + k * 17 - scroll;
                if (ry + 17 < listTop || ry > listTop + listH) continue;
                boolean hover = popup == null && hovered(x0 + 3, ry, cw - 6, 16);
                if (m.enabled()) {
                    Draw.box(c, x0 + 3, ry, cw - 6, 16, s.rowCorner(), s.plateSelected());
                    c.fill(x0 + 3, ry + 3, 2, 10, s.accent());
                } else if (hover) Draw.box(c, x0 + 3, ry, cw - 6, 16, s.rowCorner(), s.plate());
                c.text(Draw.fit(c, m.name(), cw - 18), x0 + 9, ry + 4, m.enabled() ? (s.light() ? s.text() : 0xFFFFFFFF) : hover ? s.text() : s.dim(), false);
                hit(x0 + 3, ry, cw - 6, 16, () -> toggle(m), () -> { popup = m; settingScroll = 0; editing = null; binding = null; }, null);
            }
            unclip(c);
        }
        String foot = search.isEmpty() ? s.footer() : "SEARCH: " + search + (blink() ? "_" : "");
        c.text(foot, 10, h - 14, search.isEmpty() ? s.dim() : s.accent(), true);
        String help = "LMB toggle   RMB settings   type to search";
        c.text(help, w - 10 - c.textWidth(help), h - 14, s.faint(), true);
    }

    private void popup(Canvas c, int w, int h) {
        c.fill(0, 0, w, h, 0x8C000000);
        hit(0, 0, w, h, () -> { popup = null; editing = null; binding = null; }, () -> {}, null);
        zones.add(new Zone(0, 0, w, h, d -> {}));
        int pw = Math.min(340, w - 30), ph = Math.min(380, h - 50), x = (w - pw) / 2, y = (h - ph) / 2 + 8;
        Draw.panel(c, s, x, y, pw, ph, t);
        hit(x, y, pw, ph, () -> {}, () -> {}, null);
        settings(c, popup, x + 6, y + 6, pw - 12, ph - 12);
    }

    // ---- settings ----

    private void settings(Canvas c, ModuleControl m, int x, int y, int w, int h) {
        c.text(Draw.fit(c, m.name().toUpperCase(Locale.ROOT), w - 80), x + 6, y + 4, s.light() ? s.text() : 0xFFFFFFFF, !s.light());
        c.text("// SETTINGS", x + 12 + c.textWidth(Draw.fit(c, m.name().toUpperCase(Locale.ROOT), w - 80)), y + 4, s.accent(), false);
        c.text(m.category() + (m.alwaysOn() ? "   always on" : m.enabled() ? "   ON" : "   OFF"), x + 6, y + 16, s.dim(), false);
        int top = y + 30, height = h - 30, cy = top - settingScroll, start = cy;
        zones.add(new Zone(x, top, w, height, d -> settingScroll -= d * ROW));
        clip(c, x, top, w, height);
        String page = "";
        for (SettingControl setting : m.settings()) {
            if (!setting.visible()) continue;
            var info = setting.info();
            String p = info.page() == null ? "" : info.page();
            if (!p.equals(page)) { page = p; if (!p.isBlank()) { header(c, p, x, cy, w); cy += 16; } }
            if (cy + ROW >= top && cy <= top + height) row(c, setting, x, cy, w);
            cy += ROW;
        }
        header(c, "BIND", x, cy, w);
        cy += 16;
        int fx = x + w * 45 / 100, fw = x + w - 6 - fx;
        c.text("Key", x + 6, cy + 6, s.text(), false);
        boolean waiting = binding != null && binding.id().equals(m.id());
        field(c, fx, cy + 2, fw, waiting ? "PRESS A KEY" : keyName(m.bindKey()), waiting);
        hit(fx, cy + 2, fw, 16, () -> { binding = m; editing = null; }, () -> { m.bind(-1, m.bindMode()); binding = null; host.save(); }, null);
        cy += ROW;
        c.text("Mode", x + 6, cy + 6, s.text(), false);
        field(c, fx, cy + 2, fw, String.valueOf(m.bindMode()).toUpperCase(Locale.ROOT), false);
        Runnable flip = () -> { m.bind(m.bindKey(), otherMode(m.bindMode())); host.save(); };
        hit(fx, cy + 2, fw, 16, flip, flip, null);
        cy += ROW + 4;
        unclip(c);
        int content = cy - start;
        settingScroll = clamp(settingScroll, 0, Math.max(0, content - height));
        scrollbar(c, x + w - 2, top, height, content, settingScroll);
    }

    private void header(Canvas c, String label, int x, int y, int w) {
        String text = label.toUpperCase(Locale.ROOT);
        c.text(text, x + 6, y + 5, s.accent(), false);
        int from = x + 12 + c.textWidth(text);
        c.fill(from, y + 9, Math.max(0, x + w - 8 - from), 1, Draw.alpha(s.accent(), 90));
    }

    private void row(Canvas c, SettingControl setting, int x, int y, int w) {
        var info = setting.info();
        int fx = x + w * 45 / 100, fw = x + w - 6 - fx;
        c.text(Draw.fit(c, info.name(), fx - x - 10), x + 6, y + 6, s.text(), false);
        Object value = setting.get();
        boolean active = editing != null && editing.info().id().equals(info.id());
        switch (info.kind()) {
            case BOOLEAN -> {
                boolean on = Boolean.TRUE.equals(value);
                Draw.toggle(c, s, x + w - 14, y + 10, on, t);
                Runnable flip = () -> { setting.set(!on); host.save(); };
                hit(x, y, w, ROW, flip, flip, null);
            }
            case NUMBER -> {
                double v = value instanceof Number n ? n.doubleValue() : 0, min = info.min(), max = info.max(), step = info.step();
                int vw = Math.min(46, fw / 2);
                field(c, fx, y + 2, vw, format(v, step), false);
                if (max > min) {
                    int sx = fx + vw + 6, sw = x + w - 8 - sx;
                    slider(c, sx, y, sw, (v - min) / (max - min));
                    DoubleConsumer drag = px -> {
                        double f = Math.max(0, Math.min(1, (px - sx) / Math.max(1, sw))), next = min + f * (max - min);
                        if (step > 0) next = min + Math.round((next - min) / step) * step;
                        setting.set(Math.max(min, Math.min(max, next)));
                    };
                    hit(sx - 3, y, sw + 6, ROW, null, null, drag);
                }
                double inc = step > 0 ? step : 1;
                hit(fx, y + 2, vw, 16, () -> { setting.set(bound(v + inc, min, max)); host.save(); }, () -> { setting.set(bound(v - inc, min, max)); host.save(); }, null);
            }
            case CHOICE -> {
                field(c, fx, y + 2, fw, String.valueOf(value), false);
                c.text("v", fx + fw - 9, y + 5, s.accent(), false);
                List<String> choices = info.choices();
                if (!choices.isEmpty()) {
                    int at = choices.indexOf(String.valueOf(value)), n = choices.size();
                    hit(fx, y + 2, fw, 16, () -> { setting.set(choices.get((at + 1) % n)); host.save(); }, () -> { setting.set(choices.get((at - 1 + n * 2) % n)); host.save(); }, null);
                }
            }
            case COLOR -> {
                int argb = value instanceof Number n ? n.intValue() : 0;
                Draw.framed(c, fx, y + 3, 14, 14, Math.min(3, Math.abs(s.rowCorner())), argb | 0xFF000000, s.fieldLine());
                field(c, fx + 20, y + 2, fw - 20, active ? edit + "_" : String.format("#%08X", argb), active);
                hit(fx, y + 2, fw, 16, () -> begin(setting, String.format("%08X", argb)), null, null);
            }
            case KEY -> {
                field(c, fx, y + 2, fw, active ? "PRESS A KEY" : keyName(value instanceof Number n ? n.intValue() : -1), active);
                hit(fx, y + 2, fw, 16, () -> begin(setting, ""), () -> { setting.set(-1); host.save(); }, null);
            }
            default -> {
                String shown = value instanceof List<?> list ? String.join(", ", list.stream().map(Object::toString).toList()) : String.valueOf(value);
                field(c, fx, y + 2, fw, active ? edit + "_" : shown, active);
                hit(fx, y + 2, fw, 16, () -> begin(setting, shown), null, null);
            }
        }
    }

    private void field(Canvas c, int x, int y, int w, String text, boolean active) {
        Draw.framed(c, x, y, w, 16, s.rowCorner(), s.field(), active ? s.accent() : s.fieldLine());
        c.text(Draw.fit(c, text, w - 10), x + 5, y + 4, active ? s.accent() : s.light() ? s.text() : s.accent(), false);
    }

    private void slider(Canvas c, int x, int y, int w, double f) {
        f = Math.max(0, Math.min(1, f));
        if (s.segmented()) {
            int n = Math.max(4, w / 5), last = (int) Math.round(f * n) - 1;
            for (int i = 0; i < n; i++) {
                int sx = x + i * w / n, sw = Math.max(1, (i + 1) * w / n - i * w / n - 2);
                int color = i > last ? Draw.alpha(s.fieldLine(), 70) : i == last ? (s.light() ? s.accent() : 0xFFFFFFFF) : Draw.mix(s.accent(), s.accent2(), i / (double) n);
                if (i == last && !s.light()) c.fill(sx - 1, y + 5, sw + 2, 10, Draw.alpha(s.accent2(), 90));
                c.fill(sx, y + 6, sw, 8, color);
            }
        } else {
            int k = x + (int) (f * w);
            c.fill(x, y + 9, w, 2, Draw.alpha(s.fieldLine(), 150));
            c.gradient(x, y + 9, Math.max(1, k - x), 2, s.accent2(), s.accent(), true);
            c.circle(k, y + 10, 5.5, Draw.alpha(s.accent(), 70));
            c.circle(k, y + 10, 3.5, s.light() ? s.accent() : 0xFFFFFFFF);
        }
    }

    private void scrollbar(Canvas c, int x, int y, int h, int content, int scroll) {
        if (content <= h || h <= 0) return;
        int bar = Math.max(12, h * h / content), at = y + (h - bar) * scroll / Math.max(1, content - h);
        c.fill(x, at, 2, bar, Draw.alpha(s.accent(), 150));
    }

    private void hazard(Canvas c, int x, int y) {
        int shift = (int) (t * 6) % 8;
        for (int i = 0; i < 40; i++) for (int j = 0; j < 8; j += 2)
            c.fill(x + i, y + j, 1, 2, ((i + j + shift) / 4) % 2 == 0 ? 0xE0FF2A6D : 0xE02A0A3A);
    }

    /** Selected row underline: magenta and cyan dashes moving right. */
    private void flow(Canvas c, int x, int y, int w) {
        int shift = (int) (t * 30) % 24;
        for (int i = -24; i < w; i += 12) {
            int from = Math.max(0, i + shift), to = Math.min(w, i + shift + 12);
            if (to > from) c.fill(x + from, y, to - from, 2, ((i + 24) / 12) % 2 == 0 ? 0xFFFF2A6D : 0xFF00F0FF);
        }
    }

    // ---- input ----

    @Override public boolean input(PluginApi.UiView view, PluginApi.InputEvent e) {
        switch (e.kind()) {
            case CLICK -> {
                boolean capturing = binding != null || editing != null;
                for (int i = hits.size() - 1; i >= 0; i--) {
                    Hit hit = hits.get(i);
                    if (!hit.has(e.x(), e.y())) continue;
                    if (capturing) { commit(); binding = null; }
                    if (hit.drag() != null && e.button() == 0) { dragging = hit.drag(); dragging.accept(e.x()); }
                    else if (e.button() == 0 && hit.left() != null) hit.left().run();
                    else if (e.button() == 1 && hit.right() != null) hit.right().run();
                    return true;
                }
                if (capturing) { commit(); binding = null; }
                return true;
            }
            case DRAG -> { if (dragging != null) dragging.accept(e.x()); return true; }
            case RELEASE -> { if (dragging != null) { dragging = null; host.save(); } return true; }
            case SCROLL -> {
                int d = (int) Math.signum(e.deltaY());
                for (int i = zones.size() - 1; i >= 0 && d != 0; i--) {
                    Zone z = zones.get(i);
                    if (view.mouseX() >= z.x() && view.mouseX() < z.x() + z.w() && view.mouseY() >= z.y() && view.mouseY() < z.y() + z.h()) { z.scroll().accept(d); break; }
                }
                return true;
            }
            case KEY -> {
                int key = e.key();
                if (binding != null) {
                    binding.bind(key == 259 || key == 261 ? -1 : key, binding.bindMode());
                    binding = null; host.save();
                    return true;
                }
                if (editing != null && editing.info().kind() == PluginApi.SettingKind.KEY) {
                    editing.set(key == 259 || key == 261 ? -1 : key);
                    editing = null; host.save();
                    return true;
                }
                if (key == 259) {
                    if (editing != null) edit = chop(edit); else { search = chop(search); moduleScroll = 0; }
                    return true;
                }
                if (key == 257 || key == 335) { if (editing != null) commit(); return true; }
                return editing != null;
            }
            case CHARACTER -> {
                if (binding != null) return true;
                String text = e.text() == null ? "" : e.text();
                if (editing != null) {
                    int max = editing.info().maximum() > 0 ? editing.info().maximum() : 4096;
                    if (editing.info().kind() != PluginApi.SettingKind.KEY && edit.length() + text.length() <= max) edit += text;
                } else if (search.length() < 40 && popup == null) { search += text; moduleScroll = 0; }
                return true;
            }
            default -> { return false; }
        }
    }

    @Override public void closed() {
        commit();
        hits.clear(); zones.clear();
        binding = null; dragging = null; popup = null;
        host.save();
    }

    private void begin(SettingControl setting, String text) {
        editing = setting; edit = text; binding = null;
    }

    /** Writes the text being edited back to its setting. Text that does not parse is dropped. */
    private void commit() {
        SettingControl setting = editing;
        editing = null;
        if (setting == null) return;
        try {
            switch (setting.info().kind()) {
                case TEXT -> setting.set(edit);
                case BLOCK_LIST -> setting.set(edit.isBlank() ? List.of() : List.of(edit.trim().split("[,; ]+")));
                case COLOR -> {
                    String hex = edit.replace("#", "").trim();
                    if (hex.length() == 6) hex = "FF" + hex;
                    if (hex.length() == 8) setting.set((int) Long.parseLong(hex, 16));
                }
                default -> {}
            }
            host.save();
        } catch (RuntimeException ignored) {
        }
    }

    // ---- helpers ----

    private void select(ModuleControl m) {
        if (selected == null || !selected.id().equals(m.id())) settingScroll = 0;
        selected = m; editing = null; binding = null;
    }

    private void toggle(ModuleControl m) {
        if (m.alwaysOn()) return;
        m.setEnabled(!m.enabled());
        host.save();
    }

    private List<ModuleControl> filter(List<ModuleControl> modules, String inCategory) {
        String needle = search.toLowerCase(Locale.ROOT);
        List<ModuleControl> out = new ArrayList<>();
        for (ModuleControl m : modules) {
            if (inCategory != null && !m.category().equals(inCategory)) continue;
            if (!needle.isEmpty() && !m.name().toLowerCase(Locale.ROOT).contains(needle)) continue;
            out.add(m);
        }
        return out;
    }

    private void hit(int x, int y, int w, int h, Runnable left, Runnable right, DoubleConsumer drag) {
        int x0 = Math.max(x, clipX), y0 = Math.max(y, clipY);
        long x1 = Math.min((long) x + w, (long) clipX + clipW), y1 = Math.min((long) y + h, (long) clipY + clipH);
        if (x1 > x0 && y1 > y0) hits.add(new Hit(x0, y0, (int) (x1 - x0), (int) (y1 - y0), left, right, drag));
    }

    private void clip(Canvas c, int x, int y, int w, int h) {
        clipX = x; clipY = y; clipW = Math.max(0, w); clipH = Math.max(0, h);
        c.clip(x, y, Math.max(0, w), Math.max(0, h));
    }

    private void unclip(Canvas c) {
        c.unclip();
        clipX = 0; clipY = 0; clipW = Integer.MAX_VALUE; clipH = Integer.MAX_VALUE;
    }

    private double mouseX, mouseY;

    private boolean hovered(int x, int y, int w, int h) {
        return mouseX >= Math.max(x, clipX) && mouseX < x + w && mouseY >= Math.max(y, clipY) && mouseY < Math.min((long) y + h, (long) clipY + clipH);
    }

    private boolean blink() {
        return System.currentTimeMillis() / 400 % 2 == 0;
    }

    private static String otherMode(String mode) {
        String m = mode == null ? "" : mode;
        boolean hold = m.equalsIgnoreCase("hold");
        String next = hold ? "Toggle" : "Hold";
        if (!m.isEmpty() && m.equals(m.toUpperCase(Locale.ROOT))) return next.toUpperCase(Locale.ROOT);
        if (!m.isEmpty() && m.equals(m.toLowerCase(Locale.ROOT))) return next.toLowerCase(Locale.ROOT);
        return next;
    }

    private static double bound(double v, double min, double max) {
        return max > min ? Math.max(min, Math.min(max, v)) : v;
    }

    private static String format(double v, double step) {
        if (step >= 1 && v == Math.rint(v)) return Long.toString((long) v);
        String text = String.format(Locale.ROOT, "%.2f", v);
        return text.contains(".") ? text.replaceAll("0+$", "").replaceAll("\\.$", "") : text;
    }

    private static String chop(String text) {
        return text.isEmpty() ? "" : text.substring(0, text.offsetByCodePoints(text.length(), -1));
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    /** GLFW key codes, which is what the template's example compares against (257 enter, 259 backspace). */
    static String keyName(int key) {
        if (key <= 0) return "NONE";
        if (key >= 65 && key <= 90 || key >= 48 && key <= 57) return String.valueOf((char) key);
        if (key >= 290 && key <= 314) return "F" + (key - 289);
        if (key >= 320 && key <= 329) return "NUM " + (key - 320);
        return switch (key) {
            case 32 -> "SPACE"; case 96 -> "`"; case 256 -> "ESC"; case 257 -> "ENTER"; case 258 -> "TAB";
            case 259 -> "BACKSPACE"; case 260 -> "INSERT"; case 261 -> "DELETE"; case 262 -> "RIGHT"; case 263 -> "LEFT";
            case 264 -> "DOWN"; case 265 -> "UP"; case 266 -> "PAGE UP"; case 267 -> "PAGE DOWN"; case 268 -> "HOME"; case 269 -> "END";
            case 280 -> "CAPS LOCK"; case 340 -> "L SHIFT"; case 341 -> "L CTRL"; case 342 -> "L ALT";
            case 344 -> "R SHIFT"; case 345 -> "R CTRL"; case 346 -> "R ALT";
            default -> "KEY " + key;
        };
    }
}
