package aicoder.assist.gui;

import aicoder.assist.Client;
import aicoder.assist.api.BoolSetting;
import aicoder.assist.api.Category;
import aicoder.assist.api.DoubleSetting;
import aicoder.assist.api.EnumSetting;
import aicoder.assist.api.IntSetting;
import aicoder.assist.api.Module;
import aicoder.assist.api.Setting;
import aicoder.assist.api.SettingGroup;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The menu: one column per category. Left click switches a module on or off, right click opens its
 * settings underneath it. Sliders drag, switches and choices click. Closing the menu saves everything.
 */
public class ClickGui extends Screen {
    private static final int COLUMN = 132, GAP = 8, HEADER = 20, ROW = 15, SETTING = 14, TOP = 34, LEFT = 14;

    // Aurora palette
    private static final int GREEN = 0xFF4DFFB8, TEAL = 0xFF2FE6E0, VIOLET = 0xFF8F7BFF;
    private static final int INK = 0xFFEEFAFF, DIM = 0xFFA9BDCB, FAINT = 0xFF6F8494;
    private static final int PANEL = 0xB40A1630, PANEL_LIGHT = 0x28FFFFFF, FIELD = 0x8C030A1A;

    /** Modules whose settings are open. Kept between openings of the menu. */
    private static final Set<Module> open = new HashSet<>();

    /** A clickable or draggable area laid out by the last render. */
    private record Area(int x, int y, int w, int h, Module module, Setting<?> setting) {
        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    private final List<Area> areas = new ArrayList<>();
    private Setting<?> dragging;
    private Area dragArea;

    public ClickGui() {
        super(Component.literal(Client.NAME));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        Client.save();
        super.onClose();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        areas.clear();

        g.drawString(font, "AICODER", LEFT, 12, GREEN);
        g.drawString(font, "Assist", LEFT + font.width("AICODER") + 5, 12, INK);

        String tip = null;
        int x = LEFT;
        int index = 1;

        for (Category category : Category.ALL) {
            List<Module> list = new ArrayList<>();
            for (Module module : Client.modules) if (module.category.equals(category)) list.add(module);

            int height = HEADER + 4;
            for (Module module : list) height += ROW + (open.contains(module) ? settingsHeight(module) : 0);
            height += 4;

            panel(g, x, TOP, COLUMN, height);
            g.drawString(font, String.format("%02d", index++), x + 8, TOP + 6, FAINT);
            g.drawString(font, category.name().toUpperCase(Locale.ROOT), x + 24, TOP + 6, INK);
            String count = Integer.toString(list.size());
            g.drawString(font, count, x + COLUMN - 8 - font.width(count), TOP + 6, DIM);
            g.fill(x + 1, TOP + HEADER, x + COLUMN - 1, TOP + HEADER + 1, 0x24FFFFFF);

            int y = TOP + HEADER + 4;
            for (Module module : list) {
                boolean hover = mouseX >= x + 4 && mouseX < x + COLUMN - 4 && mouseY >= y && mouseY < y + ROW;
                areas.add(new Area(x + 4, y, COLUMN - 8, ROW, module, null));

                if (module.isActive()) {
                    gradient(g, x + 4, y, COLUMN - 8, ROW - 1, 0x4C4DFFB8, 0x242FE6E0, 0x338F7BFF);
                    outline(g, x + 4, y, COLUMN - 8, ROW - 1, 0x6BA0FFE1);
                    g.fill(x + COLUMN - 13, y + 5, x + COLUMN - 9, y + 9, GREEN);
                } else if (hover) {
                    g.fill(x + 4, y, x + COLUMN - 4, y + ROW - 1, 0x1CFFFFFF);
                    outline(g, x + 4, y, COLUMN - 8, ROW - 1, 0x66FFFFFF);
                }

                g.drawString(font, module.title, x + 10, y + 3, module.isActive() || hover ? INK : DIM);
                if (hover) tip = module.description;
                y += ROW;

                if (open.contains(module)) {
                    for (SettingGroup group : module.settings.groups) {
                        for (Setting<?> setting : group.settings) {
                            if (!setting.isVisible()) continue;
                            boolean over = mouseX >= x + 8 && mouseX < x + COLUMN - 8 && mouseY >= y && mouseY < y + SETTING;
                            Area area = new Area(x + 8, y, COLUMN - 16, SETTING, module, setting);
                            areas.add(area);
                            setting(g, area, setting, over);
                            if (over) tip = setting.description;
                            y += SETTING;
                        }
                    }
                    y += 3;
                }
            }

            x += COLUMN + GAP;
        }

        if (dragging != null && dragArea != null) slide(dragging, dragArea, mouseX);

        String hint = tip != null ? tip : "Left click: on / off      Right click: settings      Esc: close";
        int width = font.width(hint);
        int tx = Math.max(6, (this.width - width) / 2);
        g.fill(tx - 6, this.height - 22, tx + width + 6, this.height - 6, 0xD00C1A36);
        outline(g, tx - 6, this.height - 22, width + 12, 16, 0x40FFFFFF);
        g.drawString(font, hint, tx, this.height - 18, tip != null ? INK : FAINT);
    }

    private int settingsHeight(Module module) {
        int height = 3;
        for (SettingGroup group : module.settings.groups) {
            for (Setting<?> setting : group.settings) if (setting.isVisible()) height += SETTING;
        }
        return height;
    }

    private void setting(GuiGraphics g, Area a, Setting<?> setting, boolean over) {
        int color = over ? INK : DIM;
        String label = fit(setting.title, a.w - 34);
        g.drawString(font, label, a.x, a.y + 1, color);

        if (setting instanceof BoolSetting bool) {
            int bx = a.x + a.w - 18, by = a.y + 2;
            if (bool.get()) gradient(g, bx, by, 18, 9, 0xE64DFFB8, 0xE62FE6E0, 0xE68F7BFF);
            else g.fill(bx, by, bx + 18, by + 9, FIELD);
            outline(g, bx, by, 18, 9, bool.get() ? 0x80FFFFFF : 0x38FFFFFF);
            int kx = bool.get() ? bx + 10 : bx + 1;
            g.fill(kx, by + 1, kx + 7, by + 8, 0xFFFFFFFF);
        } else if (setting instanceof EnumSetting<?> choice) {
            String value = choice.get().name();
            g.drawString(font, value, a.x + a.w - font.width(value), a.y + 1, GREEN);
        } else {
            String value = setting instanceof DoubleSetting d ? String.format(Locale.ROOT, "%.2f", d.get()) : String.valueOf(setting.get());
            g.drawString(font, value, a.x + a.w - font.width(value), a.y + 1, GREEN);

            // Track along the bottom of the row
            double f = fraction(setting);
            g.fill(a.x, a.y + SETTING - 3, a.x + a.w, a.y + SETTING - 1, FIELD);
            int filled = (int) Math.round(a.w * f);
            if (filled > 0) gradient(g, a.x, a.y + SETTING - 3, filled, 2, GREEN, TEAL, VIOLET);
        }
    }

    private String fit(String text, int width) {
        if (font.width(text) <= width) return text;
        while (text.length() > 1 && font.width(text + "..") > width) text = text.substring(0, text.length() - 1);
        return text + "..";
    }

    private static double fraction(Setting<?> setting) {
        if (setting instanceof IntSetting i) return i.max > i.min ? (i.get() - i.min) / (double) (i.max - i.min) : 0;
        if (setting instanceof DoubleSetting d) return d.max > d.min ? (d.get() - d.min) / (d.max - d.min) : 0;
        return 0;
    }

    private static void slide(Setting<?> setting, Area a, double mouseX) {
        double f = Math.max(0, Math.min(1, (mouseX - a.x) / (double) a.w));
        if (setting instanceof IntSetting i) i.set((int) Math.round(i.min + f * (i.max - i.min)));
        else if (setting instanceof DoubleSetting d) d.set(Math.round((d.min + f * (d.max - d.min)) * 100) / 100.0);
    }

    // Drawing

    /** Glass pane with clipped corners: a dark body, a light sheen at the top and a pale border. */
    private static void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x + 2, y, x + w - 2, y + h, PANEL);
        g.fill(x, y + 2, x + 2, y + h - 2, PANEL);
        g.fill(x + w - 2, y + 2, x + w, y + h - 2, PANEL);
        g.fill(x + 1, y + 1, x + 2, y + 2, PANEL);
        g.fill(x + w - 2, y + 1, x + w - 1, y + 2, PANEL);
        g.fill(x + 1, y + h - 2, x + 2, y + h - 1, PANEL);
        g.fill(x + w - 2, y + h - 2, x + w - 1, y + h - 1, PANEL);
        g.fillGradient(x + 2, y + 1, x + w - 2, y + Math.min(h, 40), PANEL_LIGHT, 0x00FFFFFF);

        // Border: white at the top-left running through green and violet, like the rim of the glass
        gradient(g, x + 2, y, w - 4, 1, 0xD0FFFFFF, 0x6C4DFFB8, 0x6C8F7BFF);
        gradient(g, x + 2, y + h - 1, w - 4, 1, 0x6C4DFFB8, 0x6C8F7BFF, 0x8CFFFFFF);
        g.fillGradient(x, y + 2, x + 1, y + h - 2, 0xD0FFFFFF, 0x6C4DFFB8);
        g.fillGradient(x + w - 1, y + 2, x + w, y + h - 2, 0x6C8F7BFF, 0x8CFFFFFF);
    }

    private static void outline(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y + 1, x + 1, y + h - 1, color);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    /** Left-to-right gradient through three colors, drawn as vertical slices. */
    private static void gradient(GuiGraphics g, int x, int y, int w, int h, int left, int middle, int right) {
        int step = Math.max(1, w / 24);
        for (int i = 0; i < w; i += step) {
            double f = w <= 1 ? 0 : i / (double) (w - 1);
            int color = f < 0.55 ? mix(left, middle, f / 0.55) : mix(middle, right, (f - 0.55) / 0.45);
            g.fill(x + i, y, Math.min(x + w, x + i + step), y + h, color);
        }
    }

    private static int mix(int a, int b, double t) {
        int out = 0;
        for (int shift = 0; shift <= 24; shift += 8) {
            int ca = (a >>> shift) & 0xFF, cb = (b >>> shift) & 0xFF;
            out |= ((int) Math.round(ca + (cb - ca) * t) & 0xFF) << shift;
        }
        return out;
    }

    // Input

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        for (Area area : areas) {
            if (!area.contains(click.x(), click.y())) continue;

            if (area.setting == null) {
                if (click.button() == 0) area.module.toggle();
                else if (click.button() == 1 && !open.remove(area.module)) open.add(area.module);
                return true;
            }

            Setting<?> setting = area.setting;
            if (click.button() == 1) {
                setting.reset();
            } else if (setting instanceof BoolSetting bool) {
                bool.set(!bool.get());
            } else if (setting instanceof EnumSetting<?> choice) {
                choice.cycle(1);
            } else {
                dragging = setting;
                dragArea = area;
                slide(setting, area, click.x());
            }
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        dragging = null;
        dragArea = null;
        return super.mouseReleased(click);
    }
}
