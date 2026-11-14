package aicoder.assist.aurora;

import aicoder.assist.Client;
import aicoder.assist.api.BoolSetting;
import aicoder.assist.api.Category;
import aicoder.assist.api.DoubleSetting;
import aicoder.assist.api.EnumSetting;
import aicoder.assist.api.IntSetting;
import aicoder.assist.api.Module;
import aicoder.assist.api.Setting;
import aicoder.assist.api.SettingGroup;
import aicoder.assist.gfx.Color;
import aicoder.assist.gfx.GuiRenderer;
import aicoder.assist.gfx.GuiTheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.lwjgl.glfw.GLFW;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The menu, in the two aurora glass layouts: one window with categories, modules and settings side by
 * side, or one pane per category. The drawing is the aurora theme's own (Fx, Chrome, Aurora); this class
 * only lays the pieces out and handles the mouse, which Meteor's widgets did before.
 *
 * <p>Everything is drawn in framebuffer pixels. {@code u} is the size of one design pixel: 1 on a
 * 1920x1080 window, smaller on smaller windows.
 */
public class AuroraScreen extends Screen {
    private static final GuiRenderer R = new GuiRenderer();
    private static final Style S = new Style();

    /** Something that can be clicked, registered while drawing. Later entries are on top. */
    private record Hit(double x, double y, double w, double h, Runnable left, Runnable right, Setting<?> slider, double trackX, double trackW) {
        boolean contains(double px, double py) {
            return px >= x && px < x + w && py >= y && py < y + h;
        }
    }

    private final List<Hit> hits = new ArrayList<>();
    /** Fade states (hover, selection, switches), by a key naming the thing that fades. */
    private static final Map<String, Double> fades = new HashMap<>();

    // Remembered while the game runs
    private static Category category = Category.ALL.getFirst();
    private static Module selected;
    private static Module popup;
    private static double modulesScroll, settingsScroll, popupScroll;
    private static final Map<Category, Double> columnScroll = new HashMap<>();

    private GuiTheme theme;
    private double u = 1, width, height, mx, my, delta;
    private Hit dragging;
    private double modulesMax, settingsMax, popupMax;
    private final Map<Category, Double> columnMax = new HashMap<>();
    private String tip;
    private double tipX, tipY;
    private Module card;
    private double cardX, cardY, cardW;

    public AuroraScreen() {
        super(Component.literal(Client.NAME));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        listening = null;
        Client.save();
        super.onClose();
    }

    private boolean columns() {
        return Client.layout.equals("columns");
    }

    // Frame

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        width = minecraft.getWindow().getWidth();
        height = minecraft.getWindow().getHeight();
        double guiScale = width / Math.max(1, super.width);
        mx = mouseX * guiScale;
        my = mouseY * guiScale;
        delta = Math.min(0.1, minecraft.getDeltaTracker().getRealtimeDeltaTicks() / 20.0);
        u = Math.max(0.6, Math.min(1, Math.min(width / 1920, height / 1080)));
        theme = R.theme;

        Fx.animate = Client.animations;
        hits.clear();
        tip = null;
        card = null;

        R.begin(graphics, guiScale);
        Fx.nextFrame();
        Aurora.frame(width, height, 0.88, true);

        R.scissorStart(0, 0, width, height);
        Aurora.render(R);
        R.scissorEnd();

        R.scissorStart(0, 0, width, height);
        if (columns()) renderColumns();
        else renderWindow();
        renderTopBar();
        R.scissorEnd();

        if (popup != null) {
            R.scissorStart(0, 0, width, height);
            renderPopup();
            R.scissorEnd();
        }
        if (card != null) {
            R.scissorStart(0, 0, width, height);
            renderCard();
            R.scissorEnd();
        }
        if (tip != null) {
            R.scissorStart(0, 0, width, height);
            renderTip();
            R.scissorEnd();
        }
        R.end();

        if (dragging != null) slide(dragging);
    }

    private double fade(String key, boolean up, double speed) {
        double value = Fx.step(fades.getOrDefault(key, up && !Fx.animate ? 1.0 : 0.0), up, delta, speed);
        fades.put(key, value);
        return value;
    }

    private boolean over(double x, double y, double w, double h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private void hit(double x, double y, double w, double h, Runnable left, Runnable right) {
        hits.add(new Hit(x, y, w, h, left, right, null, 0, 0));
    }

    private static double pulse(Module module) {
        double seed = ((module.name.hashCode() >>> 8) & 0xFF) / 255.0;
        return 0.85 + 0.15 * Math.sin(Math.PI * 2 * (Fx.phase(2.4) + seed));
    }

    private static String id(Module module) {
        return String.format("MOD-0x%02X", module.name.hashCode() & 0xFF);
    }

    private static List<Module> modulesOf(Category c) {
        List<Module> list = new ArrayList<>();
        for (Module module : Client.modules) if (module.category.equals(c)) list.add(module);
        return list;
    }

    // Top bar: the two layouts, and the motion switch

    private void renderTopBar() {
        String[] labels = {"WINDOW", "COLUMNS", Client.animations ? "MOTION ON" : "MOTION OFF"};
        double font = 13.5, tracking = 2.6, pad = 20 * u, inset = 6 * u;
        double barH = 46 * u, top = 22 * u;

        double[] widths = new double[labels.length];
        double total = inset * 2;
        for (int i = 0; i < labels.length; i++) {
            widths[i] = Math.round(Fx.textWidth(theme, labels[i], font, tracking, u) + pad * 2);
            total += widths[i];
        }

        double x = columns() ? width - total - 34 * u : Math.round((width - total) / 2);
        Chrome.panel(R, x, top, total, barH, u, barH / 2, false);

        Fx.TextBatch text = Fx.text(R, u);
        double tx = x + inset;
        for (int i = 0; i < labels.length; i++) {
            boolean on = i == 0 ? !columns() : i == 1 && columns();
            boolean hover = over(tx, top, widths[i], barH);
            double h = fade("tab" + i, hover, 8.3);
            double cx = tx + 2 * u, cw = widths[i] - 4 * u, cy = top + inset, ch = barH - inset * 2;

            if (on) {
                Fx.glowRect(R, cx + ch / 2, cy + ch / 2, cw - ch, 0, ch / 2 + 10 * u, Fx.a(S.green, 0.3));
                Chrome.gradient(R, cx, cy, cw, ch, ch / 2, 1, 1, 1, 159, 208, 255);
                R.quad(cx + ch / 2, cy + Fx.hair(u), cw - ch, Fx.hair(u), Fx.rgba(255, 255, 255, 0.6));
            } else if (h > 0) {
                Chrome.plate(R, cx, cy, cw, ch, u, ch / 2, h);
            }

            Color c = on ? S.dark : Fx.mix(S.dim, S.title, h);
            text.put(labels[i], tx + pad + tracking * u / 2, Fx.centerY(theme, top + barH / 2, font, u), font, c, tracking, on ? Fx.BOLD : 0);

            int index = i;
            hit(tx, top, widths[i], barH, () -> {
                if (index == 0) Client.layout = "window";
                else if (index == 1) Client.layout = "columns";
                else Client.animations = !Client.animations;
                popup = null;
            }, null);
            tx += widths[i];
        }

        if (columns()) {
            // AICODER Assist in the top-left corner
            String word = "AICODER";
            double size = 25, lx = 34 * u, ty = Fx.centerY(theme, top + barH / 2 - 8 * u, size, u);
            for (int i = 0; i < word.length(); i++) {
                String ch = String.valueOf(word.charAt(i));
                double f = (double) i / (word.length() - 1);
                text.put(ch, lx, ty, size, f < 0.5 ? Fx.mix(S.green, S.teal, f * 2) : Fx.mix(S.teal, S.violet, f * 2 - 1), 0, Fx.BOLD);
                lx += Fx.textWidth(theme, ch, size, 6, u);
            }
            text.put(" Assist", lx, ty, size, S.title, 6, Fx.BOLD);
            text.put("AURORA GLASS · COLUMNS", 34 * u, ty + Fx.lineHeight(theme, size, u) + 4 * u, 11, S.faint, 2.4, 0);
        }
    }

    // Layout: columns

    private void renderColumns() {
        double x = 34 * u, top = 110 * u, columnW = 252 * u, header = Math.round(54 * u), row = Math.round(34 * u);
        double maxH = Math.max(120 * u, height - top - 30 * u);
        int index = 1;

        for (Category c : Category.ALL) {
            List<Module> list = modulesOf(c);
            double content = 7 * u + list.size() * row + 8 * u;
            double h = Math.min(maxH, header + content);
            double scrollMax = Math.max(0, header + content - h);
            columnMax.put(c, scrollMax);
            double scroll = Math.max(0, Math.min(scrollMax, columnScroll.getOrDefault(c, 0.0)));
            columnScroll.put(c, scroll);

            Chrome.panel(R, x, top, columnW, h, u, S.columnCorner * u, false);
            hits.add(new Hit(x, top, columnW, h, null, null, null, 0, 0));
            R.quad(x + 2 * u, top + header - Fx.hair(u), columnW - 4 * u, Fx.hair(u), Fx.rgba(255, 255, 255, 0.14));

            Fx.TextBatch text = Fx.text(R, u);
            double cy = top + header / 2;
            double tx = x + 18 * u;
            text.put(String.format("%02d", index++), tx, Fx.centerY(theme, cy + u, 10.5, u), 10.5, S.faint, 1, 0);
            tx += Fx.textWidth(theme, "00", 10.5, 1, u) + 10 * u;
            text.put(c.name().toUpperCase(Locale.ROOT), tx, Fx.centerY(theme, cy, 14, u), 14, S.title, 3.4, Fx.BOLD);

            String count = Integer.toString(list.size());
            double pillW = Fx.textWidth(theme, count, 11, 0, u) + 18 * u, pillH = 20 * u, px = x + columnW - 18 * u - pillW;
            Fx.box(R, px, cy - pillH / 2, pillW, pillH, pillH / 2, Fx.rgba(255, 255, 255, 0.10));
            text.put(count, px + pillW / 2, Fx.centerY(theme, cy, 11, u), 11, S.dim, 0, Fx.CENTER);

            R.scissorStart(x, top + header, columnW, h - header - 4 * u);
            double y = top + header + 7 * u - scroll;
            for (Module module : list) {
                boolean inView = my >= top + header && my < top + h;
                boolean hover = inView && popup == null && over(x, y, columnW, row);
                columnRow(module, x, y, columnW, row, hover);
                if (popup == null) hit(x, Math.max(y, top + header), columnW, row, module::toggle, () -> openPopup(module));
                y += row;
            }
            R.scissorEnd();

            x += columnW + 14 * u;
        }

        double right = width - 40 * u, lineH = Fx.lineHeight(theme, 12.5, u) * 1.5, hy = height - 26 * u - lineH * 2;
        Fx.TextBatch hint = Fx.text(R, u);
        hint.put("Left click · Toggle module", right, hy, 12.5, S.faint, 0, Fx.RIGHT);
        hint.put("Right click · Open module settings", right, hy + lineH, 12.5, S.faint, 0, Fx.RIGHT);
    }

    private void columnRow(Module module, double x, double y, double w, double h, boolean hover) {
        double active = fade("on:" + module.name, module.isActive(), 6.25);
        double hv = fade("hover:" + module.name, hover, 8.3);
        double pulse = pulse(module);

        double px = x + 8 * u, pw = w - 16 * u, py = y + 1.5 * u, ph = h - 3 * u, corner = 11 * u;
        Chrome.lit(R, px, py, pw, ph, u, corner, 0.30, 0.14, 0.20, Fx.a(S.mint, 0.42), 0.18, active * pulse);
        if (hv > 0) {
            Fx.box(R, px, py, pw, ph, corner, Fx.rgba(255, 255, 255, 0.11 * hv));
            Fx.boxOutline(R, px, py, pw, ph, corner, Fx.hair(u), Fx.rgba(255, 255, 255, 0.4 * hv));
        }

        Fx.text(R, u).put(module.title, x + 21 * u, Fx.centerY(theme, y + h / 2, 14, u), 14, Fx.mix(S.dim, S.title, Math.max(active, hv)), 0, 0);
        if (active > 0) Chrome.bead(R, S, x + w - 24.5 * u, y + h / 2, 3.5 * u * active, active, pulse);

        if (hover) {
            card = module;
            cardX = x + w + 6 * u;
            cardY = y - 8 * u;
            cardW = w;
        }
    }

    /** Detail card beside the hovered row: name, description and state. */
    private void renderCard() {
        double padX = 18 * u, padY = 16 * u, w = 300 * u;
        List<String> lines = wrap(card.description, w - padX * 2, 13);
        double nameH = Fx.lineHeight(theme, 15, u), lineH = Fx.lineHeight(theme, 13, u) * 1.25, metaH = Fx.lineHeight(theme, 11, u);
        double h = padY + nameH + 6 * u + lines.size() * lineH + 8 * u + metaH + padY;

        double x = cardX;
        if (x + w > width) x = cardX - 6 * u - cardW - 6 * u - w;
        x = Math.round(Math.max(0, x));
        double y = Math.round(Math.max(0, Math.min(cardY, height - h)));

        Chrome.panel(R, x, y, w, h, u, 18 * u, true);
        Fx.TextBatch text = Fx.text(R, u);
        double cy = y + padY;
        text.put(card.title.toUpperCase(Locale.ROOT), x + padX, cy, 15, S.title, 2.4, Fx.BOLD);
        cy += nameH + 6 * u;
        for (String line : lines) {
            text.put(line, x + padX, cy, 13, S.dim);
            cy += lineH;
        }
        text.put(id(card) + " · " + (card.isActive() ? "ENABLED" : "DISABLED"), x + padX, cy + 8 * u, 11, S.green, 1.4, 0);
    }

    private void openPopup(Module module) {
        popup = module;
        popupScroll = 0;
    }

    /** Settings of one module in a pane of their own, as Meteor's module screen shows them. */
    private void renderPopup() {
        R.quad(0, 0, width, height, Fx.rgba(2, 6, 18, 0.45));
        hit(0, 0, width, height, () -> popup = null, () -> popup = null);

        double w = Math.min(width - 40 * u, 620 * u), pad = 34 * u;
        double content = settingsHeight(popup, w - pad * 2);
        double h = Math.min(height - 120 * u, content + 52 * u);
        double x = Math.round((width - w) / 2), y = Math.round(Math.max(90 * u, (height - h) / 2));

        Chrome.panel(R, x, y, w, h, u, S.corner * u, false);
        hits.add(new Hit(x, y, w, h, null, null, null, 0, 0));

        popupMax = Math.max(0, content + 52 * u - h);
        popupScroll = Math.max(0, Math.min(popupMax, popupScroll));
        R.scissorStart(x, y + 4 * u, w, h - 8 * u);
        settings(popup, x + pad, y + 26 * u - popupScroll, w - pad * 2, y, y + h);
        R.scissorEnd();
    }

    // Layout: single window

    private void renderWindow() {
        double uu = Math.min(u, Math.min(width / (1260 + 40), (height - 86 * u) / (780 + 20)));
        double saved = u;
        u = Math.max(0.5, uu);

        double ww = Math.round(1260 * u), wh = Math.round(780 * u);
        double wx = Math.round((width - ww) / 2), wy = Math.round(Math.max(86 * saved, (height - wh) / 2));
        double header = Math.round(75 * u), footer = Math.round(52 * u);
        double bodyY = wy + header, bodyH = wh - header - footer;
        double catsW = Math.round(250 * u), modsW = Math.round(400 * u);
        double modsX = wx + catsW, setX = modsX + modsW, setW = ww - catsW - modsW;

        Chrome.panel(R, wx, wy, ww, wh, u, S.corner * u, false);
        hits.add(new Hit(wx, wy, ww, wh, null, null, null, 0, 0));

        // Header
        Fx.TextBatch text = Fx.text(R, u);
        double hy = wy + 74 * u / 2, ty = Fx.centerY(theme, hy, 26, u), x = wx + 34 * u;
        text.put("AiCoder", x, ty, 26, S.title, 5, 0);
        x += Fx.textWidth(theme, "AiCoder ", 26, 5, u);
        String word = "Assist";
        Fx.glowRect(R, x + 6 * u, hy, Fx.textWidth(theme, word, 26, 5, u) - 12 * u, 0, 16 * u, Fx.a(S.green, 0.13));
        for (int i = 0; i < word.length(); i++) {
            String ch = String.valueOf(word.charAt(i));
            double f = (double) i / (word.length() - 1);
            text.put(ch, x, ty, 26, f < 0.5 ? Fx.mix(S.green, S.teal, f * 2) : Fx.mix(S.teal, S.violet, f * 2 - 1), 0, Fx.BOLD);
            x += Fx.textWidth(theme, ch, 26, 5, u);
        }
        double right = wx + ww - 34 * u;
        right = tag(text, right, hy, Client.modules.size() + " MODULES") - 10 * u;
        tag(text, right, hy, "AURORA GLASS");

        // Dividers
        double t = Fx.hair(u);
        Color line = Fx.rgba(255, 255, 255, 0.14);
        R.quad(wx + 2 * u, bodyY - t, ww - 4 * u, t, line);
        R.quad(wx + 2 * u, bodyY + bodyH, ww - 4 * u, t, line);
        R.quad(modsX - t, bodyY, t, bodyH, line);
        R.quad(setX - t, bodyY, t, bodyH, line);

        // Categories
        double cy = bodyY + 22 * u;
        for (Category c : Category.ALL) {
            double cx = wx + 18 * u, cw = catsW - 36 * u, ch = Math.round(58 * u);
            boolean on = c.equals(category), hover = over(cx, cy, cw, ch);
            double lit = fade("cat:" + c.name(), on, 8.3), hv = fade("cathover:" + c.name(), hover, 8.3);
            double corner = S.plateCorner * u;

            Chrome.plate(R, cx, cy, cw, ch, u, corner, on ? 0 : hv);
            Chrome.lit(R, cx, cy, cw, ch, u, corner, 0.30, 0.20, 0.26, Fx.rgba(255, 255, 255, 0.42), 0.22, lit);
            if (lit > 0) R.quad(cx + corner, cy + t, cw - corner * 2, t, Fx.rgba(255, 255, 255, 0.4 * lit));
            Chrome.bead(R, S, cx + 24 * u, cy + ch / 2, 4 * u, lit, 1);

            text.put(c.name().toUpperCase(Locale.ROOT), cx + 42 * u, Fx.centerY(theme, cy + ch / 2, 15, u), 15, Fx.mix(S.dim, S.title, Math.max(lit, hv * 0.7)), 2.6, 0);
            text.put(String.format("%02d", modulesOf(c).size()), cx + cw - 20 * u, Fx.centerY(theme, cy + ch / 2, 11.5, u), 11.5, S.faint, 0, Fx.RIGHT);

            hit(cx, cy, cw, ch, () -> {
                category = c;
                modulesScroll = 0;
            }, null);
            cy += ch + 12 * u;
        }

        // Modules
        List<Module> list = modulesOf(category);
        if (selected == null && !list.isEmpty()) selected = list.getFirst();
        double rowH = Math.round(66 * u), gap = 11 * u;
        modulesMax = Math.max(0, 22 * u + list.size() * (rowH + gap) + 11 * u - bodyH);
        modulesScroll = Math.max(0, Math.min(modulesMax, modulesScroll));

        R.scissorStart(modsX, bodyY, modsW - t, bodyH);
        double my0 = bodyY + 22 * u - modulesScroll;
        for (Module module : list) {
            double rx = modsX + 18 * u, rw = modsW - 34 * u;
            boolean inView = my >= bodyY && my < bodyY + bodyH;
            windowRow(module, rx, my0, rw, rowH, inView && over(rx, my0, rw, rowH));

            double switchX = rx + rw - (20 + 44 + 8) * u;
            if (my0 + rowH > bodyY && my0 < bodyY + bodyH) {
                double top = Math.max(my0, bodyY), bottom = Math.min(my0 + rowH, bodyY + bodyH);
                hit(rx, top, rw, bottom - top, () -> {
                    if (mx >= switchX) module.toggle();
                    else {
                        selected = module;
                        settingsScroll = 0;
                    }
                }, module::toggle);
            }
            my0 += rowH + gap;
        }
        R.scissorEnd();

        // Settings of the selected module
        if (selected != null) {
            double pad = 34 * u;
            double content = settingsHeight(selected, setW - pad * 2) + 42 * u;
            settingsMax = Math.max(0, content - bodyH);
            settingsScroll = Math.max(0, Math.min(settingsMax, settingsScroll));
            R.scissorStart(setX, bodyY, setW - 2 * u, bodyH);
            settings(selected, setX + pad, bodyY + 26 * u - settingsScroll, setW - pad * 2, bodyY, bodyY + bodyH);
            R.scissorEnd();
        }

        // Footer
        Fx.TextBatch foot = Fx.text(R, u);
        double fy = bodyY + bodyH + (wy + wh - bodyY - bodyH) / 2, fty = Fx.centerY(theme, fy, 12.5, u);
        int active = 0;
        for (Module module : Client.modules) if (module.isActive()) active++;
        double fx = wx + 34 * u;
        fx = readout(foot, fx, fty, "[RIGHT SHIFT]", true);
        fx = readout(foot, fx, fty, " GUI", false) + 26 * u;
        fx = readout(foot, fx, fty, minecraft.getFps() + " FPS", false) + 26 * u;
        fx = readout(foot, fx, fty, aicoder.assist.api.PlayerUtils.getPing() + " ms", false) + 26 * u;
        fx = readout(foot, fx, fty, "ACTIVE ", false);
        readout(foot, fx, fty, Integer.toString(active), true);

        double[] rest = {9, 15, 21, 17, 12, 19, 8, 14, 10};
        double p = Fx.phase(6), bw = 5 * u, bg = 4 * u, base = fy + 11 * u, bright = wx + ww - 34 * u;
        for (int i = 0; i < rest.length; i++) {
            double sway = Fx.animate ? 0.75 + 0.25 * Fx.flow(p, i * 0.137) : 1;
            double bh = Math.max(4, rest[i] * sway) * u, bx = bright - (rest.length - i) * (bw + bg) + bg;
            Fx.box(R, bx, base - bh, bw, bh / 2, 0, S.violet, S.teal);
            Fx.box(R, bx, base - bh / 2, bw, bh / 2, 0, S.teal, S.green);
        }

        u = saved;
    }

    private double tag(Fx.TextBatch text, double right, double cy, String label) {
        double w = Fx.textWidth(theme, label, 12, 0.6, u) + 24 * u, h = 28 * u, x = right - w;
        Fx.box(R, x, cy - h / 2, w, h, h / 2, Fx.rgba(255, 255, 255, 0.07));
        Fx.boxOutline(R, x, cy - h / 2, w, h, h / 2, Fx.hair(u), Fx.rgba(255, 255, 255, 0.14));
        text.put(label, x + 12 * u, Fx.centerY(theme, cy, 12, u), 12, S.dim, 0.6, 0);
        return x;
    }

    private double readout(Fx.TextBatch text, double x, double y, String value, boolean highlight) {
        text.put(value, x, y, 12.5, highlight ? S.green : S.dim, 1.4, 0);
        return x + Fx.textWidth(theme, value, 12.5, 1.4, u);
    }

    private void windowRow(Module module, double x, double y, double w, double h, boolean hover) {
        boolean isSelected = module == selected;
        double active = fade("on:" + module.name, module.isActive(), 6.25);
        double hv = fade("hover:" + module.name, hover, 8.3);
        double pulse = pulse(module), corner = S.plateCorner * u;

        if (isSelected) Fx.boxOutline(R, x - 5 * u, y - 5 * u, w + 10 * u, h + 10 * u, corner + 5 * u, 5 * u, Fx.a(S.green, 0.14));
        Chrome.plate(R, x, y, w, h, u, corner, isSelected ? 0 : hv);
        Chrome.lit(R, x, y, w, h, u, corner, 0.22, 0.10, 0.14, Fx.a(S.mint, 0.42), 0.16, active * pulse);
        if (isSelected) Fx.boxOutline(R, x, y, w, h, corner, Math.max(1, Math.round(1.6 * u)), Fx.rgba(255, 255, 255, 0.75));

        Fx.TextBatch text = Fx.text(R, u);
        double nameH = Fx.lineHeight(theme, 16.5, u), idH = Fx.lineHeight(theme, 10.5, u), gap = 3 * u;
        double top = y + (h - nameH - gap - idH) / 2;
        text.put(module.title, x + 20 * u, top, 16.5, S.title, 0.4, 0);
        text.put(id(module), x + 20 * u, top + nameH + gap, 10.5, S.faint, 1.8, 0);

        double sw = 44 * u, sh = Math.round(24 * u);
        Chrome.toggle(R, Math.round(x + w - 20 * u - sw), Math.round(y + (h - sh) / 2), sw, sh, u, active, hv, pulse);
    }

    // Settings, shared by the window layout and the popup

    private static final double ROW = 41, GROUP = 44;

    /** The module whose bind row is waiting for a key press. */
    private Module listening;

    private double settingsHeight(Module module, double w) {
        double h = Fx.lineHeight(theme, 19, u) + 12 * u + wrap(module.description, w, 14).size() * Fx.lineHeight(theme, 14, u) * 1.3 + 8 * u;
        for (SettingGroup group : module.settings.groups) {
            int visible = 0;
            for (Setting<?> setting : group.settings) if (setting.isVisible()) visible++;
            if (visible > 0) h += GROUP * u + visible * ROW * u;
        }
        // The bind group: key and mode
        return h + GROUP * u + 2 * ROW * u;
    }

    private void settings(Module module, double x, double y, double w, double clipTop, double clipBottom) {
        Fx.TextBatch text = Fx.text(R, u);
        String name = module.title.toUpperCase(Locale.ROOT);
        text.put(name, x, y, 19, S.title, 4, Fx.BOLD);
        text.put("· SETTINGS", x + Fx.textWidth(theme, name, 19, 4, u) + 12 * u, y, 19, S.faint, 4, 0);
        y += Fx.lineHeight(theme, 19, u) + 12 * u;

        double lineH = Fx.lineHeight(theme, 14, u) * 1.3;
        for (String line : wrap(module.description, w, 14)) {
            text.put(line, x, y, 14, S.dim);
            y += lineH;
        }
        y += 8 * u;

        for (SettingGroup group : module.settings.groups) {
            boolean any = false;
            for (Setting<?> setting : group.settings) any |= setting.isVisible();
            if (!any) continue;

            // Group title, then a hairline running from green through violet to nothing
            String label = group.name.toUpperCase(Locale.ROOT);
            double gy = y + GROUP * u - 16 * u;
            text.put(label, x, Fx.centerY(theme, gy, 12, u), 12, S.green, 3.4, Fx.BOLD);
            double lx = x + Fx.textWidth(theme, label, 12, 3.4, u) + 14 * u, half = (x + w - lx) / 2;
            if (half > 0) {
                R.quad(lx, gy, half, Fx.hair(u), Fx.a(S.green, 0.55), Fx.a(S.violet, 0.25));
                R.quad(lx + half, gy, half, Fx.hair(u), Fx.a(S.violet, 0.25), Fx.a(S.violet, 0));
            }
            y += GROUP * u;

            for (Setting<?> setting : group.settings) {
                if (!setting.isVisible()) continue;
                boolean visible = y + ROW * u > clipTop && y < clipBottom;
                if (visible) settingRow(text, module, setting, x, y, w, clipTop, clipBottom);
                y += ROW * u;
            }
        }

        // Bind: which key switches the module, and whether it is held or pressed once
        double gy = y + GROUP * u - 16 * u;
        text.put("BIND", x, Fx.centerY(theme, gy, 12, u), 12, S.green, 3.4, Fx.BOLD);
        double lx = x + Fx.textWidth(theme, "BIND", 12, 3.4, u) + 14 * u, half = (x + w - lx) / 2;
        if (half > 0) {
            R.quad(lx, gy, half, Fx.hair(u), Fx.a(S.green, 0.55), Fx.a(S.violet, 0.25));
            R.quad(lx + half, gy, half, Fx.hair(u), Fx.a(S.violet, 0.25), Fx.a(S.violet, 0));
        }
        y += GROUP * u;

        String key = listening == module ? "PRESS A KEY" : module.bind < 0 ? "NONE" : keyName(module.bind);
        choiceRow(text, "Key", key, "The key that switches this module. Click, then press a key. Esc or right click clears it.", x, y, w, clipTop, clipBottom,
            listening == module, () -> listening = listening == module ? null : module, () -> {
                module.bind = -1;
                listening = null;
            });
        y += ROW * u;
        choiceRow(text, "Mode", module.holdBind ? "HOLD" : "TOGGLE", "Toggle: each press switches the module on or off. Hold: it is on only while the key is down.", x, y, w, clipTop, clipBottom,
            false, () -> module.holdBind = !module.holdBind, () -> module.holdBind = !module.holdBind);
    }

    private static String keyName(int key) {
        return InputConstants.Type.KEYSYM.getOrCreate(key).getDisplayName().getString().toUpperCase(Locale.ROOT);
    }

    /** A labelled value in a dark field that changes when clicked; used by the bind rows. */
    private void choiceRow(Fx.TextBatch text, String label, String value, String help, double x, double y, double w, double clipTop, double clipBottom,
                           boolean lit, Runnable left, Runnable right) {
        double h = ROW * u, cy = y + h / 2;
        if (y + h <= clipTop || y >= clipBottom) return;

        boolean inClip = my >= clipTop && my < clipBottom;
        text.put(label, x, Fx.centerY(theme, cy, 14.5, u), 14.5, S.text);
        if (inClip && over(x, y, 170 * u, h)) {
            tip = help;
            tipX = mx + 14 * u;
            tipY = my + 18 * u;
        }

        double wx = x + Math.min(190 * u, w * 0.38), fw = Math.min(190 * u, x + w - wx), fh = 30 * u, fy = cy - fh / 2;
        boolean hot = lit || (inClip && over(wx, fy, fw, fh));
        Chrome.field(R, S, wx, fy, fw, fh, u, hot, lit);
        text.put(value, wx + 11 * u, Fx.centerY(theme, cy, 13, u), 13, lit ? S.green : S.title, 1, 0);

        double top = Math.max(y, clipTop), bottom = Math.min(y + h, clipBottom);
        hit(wx, top, fw, bottom - top, left, right);
    }

    private void settingRow(Fx.TextBatch text, Module module, Setting<?> setting, double x, double y, double w, double clipTop, double clipBottom) {
        double h = ROW * u, cy = y + h / 2;
        boolean inClip = my >= clipTop && my < clipBottom;
        boolean hover = inClip && over(x, y, w, h);
        String key = module.name + "." + setting.name;

        text.put(setting.title, x, Fx.centerY(theme, cy, 14.5, u), 14.5, S.text);
        if (hover && over(x, y, 170 * u, h)) {
            tip = setting.description;
            tipX = mx + 14 * u;
            tipY = my + 18 * u;
        }

        double wx = x + Math.min(190 * u, w * 0.38);
        double resetR = 13 * u, resetX = x + w - resetR;
        double top = Math.max(y, clipTop), bottom = Math.min(y + h, clipBottom);
        if (bottom <= top) return;

        if (setting instanceof BoolSetting bool) {
            double size = Math.round(24 * u), bx = wx, by = Math.round(cy - size / 2), corner = size / 3;
            double on = fade("check:" + key, bool.get(), 10);
            Fx.box(R, bx, by, size, size, corner, S.field);
            Fx.boxOutline(R, bx, by, size, size, corner, Fx.hair(u), Fx.rgba(255, 255, 255, over(bx, by, size, size) ? 0.45 : 0.2));
            if (on > 0) {
                Fx.glowRect(R, bx + corner, by + corner, size - corner * 2, size - corner * 2, corner + 6 * u, Fx.a(S.green, 0.32 * on));
                Color mid = Fx.mix(S.green, S.teal, 0.5);
                Fx.box(R, bx, by, size, size, corner, Fx.a(S.green, on), Fx.a(mid, on), Fx.a(S.teal, on), Fx.a(mid, on));
                R.quad(bx + corner, by + Fx.hair(u), size - corner * 2, Fx.hair(u), Fx.rgba(255, 255, 255, 0.7 * on));
                Color mark = Fx.a(S.dark, on);
                double mcx = bx + size / 2, mcy = by + size / 2, k = size * 0.5, lt = Math.max(1.5, size * 0.1);
                Fx.line(R, mcx - k * 0.46, mcy + k * 0.02, mcx - k * 0.12, mcy + k * 0.36, lt, mark);
                Fx.line(R, mcx - k * 0.12, mcy + k * 0.36, mcx + k * 0.48, mcy - k * 0.32, lt, mark);
            }
            hit(bx - 4 * u, top, size + 8 * u, bottom - top, () -> bool.set(!bool.get()), null);
        } else if (setting instanceof EnumSetting<?> choice) {
            double fw = Math.min(190 * u, resetX - resetR - 14 * u - wx), fh = 30 * u, fy = cy - fh / 2;
            boolean hot = over(wx, fy, fw, fh) && inClip;
            Chrome.field(R, S, wx, fy, fw, fh, u, hot, false);
            text.put(choice.get().name(), wx + 11 * u, Fx.centerY(theme, cy, 13, u), 13, S.title, 1, 0);
            double cw = 9 * u, cx = wx + fw - 16 * u;
            Fx.tri(R, cx - cw / 2, cy - cw / 4, cx + cw / 2, cy - cw / 4, cx, cy + cw / 3, hot ? S.title : S.dim);
            hit(wx, top, fw, bottom - top, () -> choice.cycle(1), () -> choice.cycle(-1));
        } else {
            // Value box, then the slider
            double boxW = 96 * u, fh = 30 * u, fy = cy - fh / 2;
            Chrome.field(R, S, wx, fy, boxW, fh, u, false, false);
            String value = setting instanceof DoubleSetting d ? String.format(Locale.ROOT, "%.2f", d.get()) : String.valueOf(setting.get());
            text.put(value, wx + 11 * u, Fx.centerY(theme, cy, 12.5, u), 12.5, S.title, 0.4, 0);

            double knob = 10 * u, trackX = wx + boxW + 14 * u + knob, trackW = resetX - resetR - 14 * u - knob - trackX;
            if (trackW > 20 * u) {
                double f = fraction(setting), th = Math.max(4, Math.round(8 * u)), tyy = Math.round(cy - th / 2), filled = trackW * f;
                boolean hot = (dragging != null && dragging.slider == setting) || (inClip && over(trackX - knob, y, trackW + knob * 2, h));
                Fx.box(R, trackX - th / 2, tyy, trackW + th, th, th / 2, Fx.rgba(3, 10, 26, 0.5));
                Fx.boxOutline(R, trackX - th / 2, tyy, trackW + th, th, th / 2, Fx.hair(u), Fx.rgba(255, 255, 255, 0.14));
                if (filled > 0) {
                    Fx.glowRect(R, trackX, tyy + th / 2, filled, 0, th / 2 + 7 * u, Fx.a(S.green, 0.3));
                    Chrome.gradient(R, trackX - th / 2, tyy, filled + th, th, th / 2, 1, 1, 1);
                }
                double kr = (hot ? 11 : 10) * u, kx = trackX + filled, ky = tyy + th / 2;
                Fx.circle(R, kx, ky, kr + 4 * u, Fx.rgba(255, 255, 255, hot ? 0.2 : 0.12));
                Fx.circle(R, kx, ky + 2 * u, kr, Fx.rgba(0, 0, 0, 0.3));
                Fx.circle(R, kx, ky, kr, Fx.rgba(147, 169, 194, 1));
                Fx.circle(R, kx - kr * 0.06, ky - kr * 0.08, kr * 0.88, Fx.rgba(207, 226, 242, 1));
                Fx.circle(R, kx - kr * 0.2, ky - kr * 0.24, kr * 0.52, Fx.rgba(255, 255, 255, 1));
                hits.add(new Hit(trackX - knob, top, trackW + knob * 2, bottom - top, null, null, setting, trackX, trackW));
            }
        }

        // Reset: a thin ring with a turning arrow
        boolean overReset = inClip && over(resetX - resetR, cy - resetR, resetR * 2, resetR * 2);
        double rh = fade("reset:" + key, overReset, 8.3), rt = Math.max(1, Math.round(1.4 * u));
        if (rh > 0) Fx.circle(R, resetX, cy, resetR, Fx.rgba(255, 255, 255, 0.1 * rh));
        Fx.ring(R, resetX, cy, resetR - rt / 2, rt, Fx.mix(Fx.rgba(255, 255, 255, 0.3), S.green, rh * 0.8));
        Color arrow = Fx.mix(S.dim, S.title, rh);
        double ar = resetR * 0.42;
        Fx.ring(R, resetX, cy, ar, Math.max(1, 1.3 * u), arrow);
        Fx.tri(R, resetX + ar - 3 * u, cy - ar * 0.2, resetX + ar + 3 * u, cy - ar * 0.2, resetX + ar, cy + ar * 0.5, arrow);
        hit(resetX - resetR, Math.max(cy - resetR, clipTop), resetR * 2, resetR * 2, setting::reset, null);
    }

    private static double fraction(Setting<?> setting) {
        if (setting instanceof IntSetting i) return i.max > i.min ? (i.get() - i.min) / (double) (i.max - i.min) : 0;
        if (setting instanceof DoubleSetting d) return d.max > d.min ? (d.get() - d.min) / (d.max - d.min) : 0;
        return 0;
    }

    private void slide(Hit hit) {
        double f = Math.max(0, Math.min(1, (mx - hit.trackX) / hit.trackW));
        if (hit.slider instanceof IntSetting i) i.set((int) Math.round(i.min + f * (i.max - i.min)));
        else if (hit.slider instanceof DoubleSetting d) d.set(Math.round((d.min + f * (d.max - d.min)) * 100) / 100.0);
    }

    private void renderTip() {
        double padX = 14 * u, padY = 9 * u, maxW = 420 * u;
        List<String> lines = wrap(tip, maxW, 13);
        double w = 0, lineH = Fx.lineHeight(theme, 13, u) * 1.2;
        for (String line : lines) w = Math.max(w, Fx.textWidth(theme, line, 13, 0, u));
        w += padX * 2;
        double h = lines.size() * lineH + padY * 2;
        double x = Math.round(Math.max(0, Math.min(tipX, width - w))), y = Math.round(Math.max(0, Math.min(tipY, height - h)));

        Chrome.panel(R, x, y, w, h, u, 12 * u, true);
        Fx.TextBatch text = Fx.text(R, u);
        double ty = y + padY;
        for (String line : lines) {
            text.put(line, x + padX, ty, 13, S.text);
            ty += lineH;
        }
    }

    private List<String> wrap(String text, double maxWidth, double size) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split("\\s+")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (!line.isEmpty() && Fx.textWidth(theme, candidate, size, 0, u) > maxWidth) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (!line.isEmpty()) lines.add(line.toString());
        return lines;
    }

    // Input

    private double scale() {
        return width / Math.max(1, super.width);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        double px = click.x() * scale(), py = click.y() * scale();
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit hit = hits.get(i);
            if (!hit.contains(px, py)) continue;

            if (hit.slider != null && click.button() == 0) {
                dragging = hit;
                mx = px;
                slide(hit);
            } else if (click.button() == 0 && hit.left != null) {
                hit.left.run();
            } else if (click.button() == 1 && hit.right != null) {
                hit.right.run();
            }
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (listening != null) {
            int key = event.key();
            // Esc clears; the menu's own key cannot be taken
            if (key == GLFW.GLFW_KEY_ESCAPE) listening.bind = -1;
            else if (!Client.isMenuKey(key)) listening.bind = key;
            listening = null;
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        dragging = null;
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        double step = -vertical * 60 * u;
        if (popup != null) {
            popupScroll = Math.max(0, Math.min(popupMax, popupScroll + step));
        } else if (columns()) {
            double x = 34 * u, px = mouseX * scale();
            for (Category c : Category.ALL) {
                if (px >= x && px < x + 252 * u) {
                    columnScroll.put(c, Math.max(0, Math.min(columnMax.getOrDefault(c, 0.0), columnScroll.getOrDefault(c, 0.0) + step)));
                }
                x += 266 * u;
            }
        } else if (mouseX * scale() > width / 2) {
            settingsScroll = Math.max(0, Math.min(settingsMax, settingsScroll + step));
        } else {
            modulesScroll = Math.max(0, Math.min(modulesMax, modulesScroll + step));
        }
        return true;
    }
}
