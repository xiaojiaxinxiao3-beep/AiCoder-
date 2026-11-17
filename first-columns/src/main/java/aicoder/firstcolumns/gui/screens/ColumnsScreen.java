package aicoder.firstcolumns.gui.screens;

import aicoder.firstcolumns.gui.AiCoderGuiTheme;
import aicoder.firstcolumns.gui.render.Anim;
import aicoder.firstcolumns.gui.render.Draw;
import aicoder.firstcolumns.gui.render.Palette;
import aicoder.firstcolumns.gui.render.Sdf;
import com.mojang.datafixers.util.Pair;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import meteordevelopment.meteorclient.gui.utils.Cell;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.gui.widgets.containers.WSection;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WWindow;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.mixin.KeyMappingAccessor;
import meteordevelopment.meteorclient.mixin.MinecraftAccessor;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.NbtUtils;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.meteorclient.utils.misc.input.KeyBinds;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.input.KeyEvent;
import org.apache.commons.lang3.SystemUtils;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static com.mojang.blaze3d.platform.InputConstants.*;
import static meteordevelopment.meteorclient.MeteorClient.mc;
import static meteordevelopment.meteorclient.utils.Utils.getWindowHeight;
import static meteordevelopment.meteorclient.utils.Utils.getWindowWidth;

/**
 * The modules tab with one glass column per category, side by side, as in Meteor's own screen: left click
 * a module to switch it, right click for its settings. The look is the first AiCoder theme's: the aurora
 * behind, the logo in the top left corner, glass panes, round switches.
 */
public class ColumnsScreen extends TabScreen {
    private static final String TITLE = "AiCoder client";

    // Design pixels
    private static final double TOP = 58, LEFT = 20, GAP = 12, COLUMN_WIDTH = 190, FOOT = 34;

    private static final Color C_KEY = Palette.color(Palette.TEXT_KEY);
    private static final Color C_FOOTER = Palette.color(Palette.TEXT_FOOTER);
    private static final Color[] C_LOGO = new Color[TITLE.length()];

    static {
        for (int i = 0; i < C_LOGO.length; i++) C_LOGO[i] = new Color();
    }

    private WCategoryController controller;
    private WWindow searchWindow;
    private WTextBox searchTextBox;

    public ColumnsScreen(GuiTheme theme) {
        super(theme, Tabs.get().getFirst());
    }

    @Override
    public void initWidgets() {
        controller = add(new WCategoryController()).widget();
    }

    @Override
    protected void init() {
        super.init();
        controller.refresh();
    }

    private double u(double designPixels) {
        return AiCoderGuiTheme.of(theme).u(designPixels);
    }

    private static String guiKey() {
        try {
            String name = Keybind.fromKey(((KeyMappingAccessor) KeyBinds.OPEN_GUI).meteor$getKey()).toString().toUpperCase(Locale.ROOT);
            return name.replace("RIGHT ", "R").replace("LEFT ", "L").replace(" ", "");
        } catch (Throwable t) {
            return "RSHIFT";
        }
    }

    // Columns

    private WWindow column(WContainer c, String title, String id) {
        WWindow w = theme.window(title);
        w.id = id;
        w.padding = 8 * AiCoderGuiTheme.UNIT;
        w.spacing = 0;

        c.add(w);
        w.view.scrollOnlyWhenMouseOver = true;
        w.view.hasScrollBar = false;
        w.view.spacing = 6 * AiCoderGuiTheme.UNIT;

        // Cell sizes are in Meteor units
        w.minWidth = COLUMN_WIDTH * AiCoderGuiTheme.UNIT;
        return w;
    }

    protected WWindow createCategory(WContainer c, Category category, List<Module> moduleList) {
        WWindow w = column(c, category.name, category.name);
        for (Module module : moduleList) w.add(theme.module(module)).expandX();
        return w;
    }

    // Search

    protected void createSearchW(WContainer w, String text) {
        if (text.isEmpty()) return;

        List<Pair<Module, String>> modules = Modules.get().searchTitles(text);
        if (!modules.isEmpty()) {
            WSection section = w.add(theme.section("Modules")).expandX().widget();
            section.spacing = 6 * AiCoderGuiTheme.UNIT;

            int count = 0;
            for (Pair<Module, String> p : modules) {
                if (count >= Config.get().moduleSearchCount.get()) break;
                section.add(theme.module(p.getFirst(), p.getSecond())).expandX();
                count++;
            }
        }

        Set<Module> settings = Modules.get().searchSettingTitles(text);
        if (!settings.isEmpty()) {
            WSection section = w.add(theme.section("Settings")).expandX().widget();
            section.spacing = 6 * AiCoderGuiTheme.UNIT;

            int count = 0;
            for (Module module : settings) {
                if (count >= Config.get().moduleSearchCount.get()) break;
                section.add(theme.module(module)).expandX();
                count++;
            }
        }
    }

    protected WWindow createSearch(WContainer c) {
        WWindow w = column(c, "Search", "search");
        searchWindow = w;
        w.view.maxHeight -= 20;

        WVerticalList l = theme.verticalList();

        WTextBox text = w.add(theme.textBox("")).minWidth(140).expandX().widget();
        text.setFocused(true);
        searchTextBox = text;
        text.action = () -> {
            l.clear();
            createSearchW(l, text.get());
        };

        w.add(l).expandX();
        createSearchW(l, text.get());

        return w;
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent input) {
        if (locked) return false;

        boolean ctrl = SystemUtils.IS_OS_MAC
            ? Input.modsMatch(input.modifiers(), MOD_SUPER)
            : Input.modsMatch(input.modifiers(), MOD_CONTROL);

        if (ctrl && input.key() == KEY_F) {
            if (searchWindow != null) searchWindow.setExpanded(true);
            if (searchTextBox != null) {
                searchTextBox.setFocused(true);
                searchTextBox.setCursorMax();
            }

            return true;
        }

        return super.keyPressed(input);
    }

    // Favorites

    protected Cell<WWindow> createFavorites(WContainer c) {
        boolean hasFavorites = Modules.get().getAll().stream().anyMatch(module -> module.favorite);
        if (!hasFavorites) return null;

        WWindow w = column(c, "Favorites", "favorites");
        @SuppressWarnings("unchecked")
        Cell<WWindow> cell = (Cell<WWindow>) c.cells.getLast();

        createFavoritesW(w);
        return cell;
    }

    protected boolean createFavoritesW(WWindow w) {
        List<Module> modules = new ArrayList<>();
        for (Module module : Modules.get().getAll()) {
            if (module.favorite) modules.add(module);
        }

        modules.sort((o1, o2) -> String.CASE_INSENSITIVE_ORDER.compare(o1.name, o2.name));
        for (Module module : modules) w.add(theme.module(module)).expandX();

        return !modules.isEmpty();
    }

    @Override
    public boolean toClipboard() {
        return NbtUtils.toClipboard(Modules.get());
    }

    @Override
    public boolean fromClipboard() {
        return NbtUtils.fromClipboard(Modules.get());
    }

    @Override
    public void reload() {
    }

    // The whole screen: background, logo, status line, and where the columns go

    protected class WCategoryController extends WContainer {
        public final List<WWindow> windows = new ArrayList<>();
        private Cell<WWindow> favorites;

        private String key = "RSHIFT";
        private double keyTimer;

        @Override
        public void init() {
            List<Module> moduleList = new ArrayList<>();
            for (Category category : Modules.loopCategories()) {
                for (Module module : Modules.get().getGroup(category)) {
                    if (!Config.get().hiddenModules.get().contains(module)) moduleList.add(module);
                }

                if (!moduleList.isEmpty()) {
                    windows.add(createCategory(this, category, moduleList));
                    moduleList.clear();
                }
            }

            windows.add(createSearch(this));

            refresh();
        }

        protected void refresh() {
            if (favorites == null) {
                favorites = createFavorites(this);
                if (favorites != null) windows.add(favorites.widget());
            } else {
                favorites.widget().clear();

                if (!createFavoritesW(favorites.widget())) {
                    remove(favorites);
                    windows.remove(favorites.widget());
                    favorites = null;
                }
            }
        }

        /** Width of the widest column the last time they were laid out, to know how many fit in a row. */
        private double columnWidth;

        private int perRow() {
            double w = columnWidth > 0 ? columnWidth : u(COLUMN_WIDTH + 16);
            return Math.max(1, (int) ((getWindowWidth() - u(LEFT) * 2 + u(GAP)) / (w + u(GAP))));
        }

        @Override
        public void calculateSize() {
            // Everything stays on screen: when the columns do not fit side by side they go in two or more
            // rows, each row getting its share of the height, and a long column scrolls
            // Measured first, so the number that fit in a row comes from their real width
            super.calculateSize();
            double widest = 0;
            for (Cell<?> cell : cells) widest = Math.max(widest, cell.widget().width);
            if (widest > 0) columnWidth = widest;

            int rows = Math.max(1, (int) Math.ceil(cells.size() / (double) perRow()));
            double room = (getWindowHeight() - u(TOP) - u(FOOT) - u(GAP) * (rows - 1)) / rows - u(56);
            for (WWindow w : windows) w.view.maxHeight = Math.max(u(70), room);

            super.calculateSize();
        }

        @Override
        protected void onCalculateSize() {
            width = getWindowWidth();
            height = getWindowHeight();
        }

        @Override
        protected void onCalculateWidgetPositions() {
            double gap = u(GAP);
            double left = u(LEFT);
            int perRow = perRow();

            double x = left, y = u(TOP), rowHeight = 0, widest = 0;
            int inRow = 0;

            // Left to right, then the next row. A column the player has dragged keeps its own saved
            // position (WWindow applies that itself).
            for (Cell<?> cell : cells) {
                WWidget w = cell.widget();

                if (inRow >= perRow) {
                    x = left;
                    y += rowHeight + gap;
                    rowHeight = 0;
                    inRow = 0;
                }

                cell.x = x;
                cell.y = y;
                cell.width = w.width;
                cell.height = w.height;
                w.x = x;
                w.y = y;

                x += w.width + gap;
                rowHeight = Math.max(rowHeight, w.height);
                widest = Math.max(widest, w.width);
                inRow++;
            }

            columnWidth = widest;
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            AiCoderGuiTheme theme = AiCoderGuiTheme.of(this.theme);
            double u = theme.u(1);

            if (theme.aurora.get() && theme.glassColor.get()) Sdf.aurora(getWindowWidth(), getWindowHeight(), 1);

            renderTitle(renderer, u);
            renderFooter(renderer, u, delta);
        }

        private void renderTitle(GuiRenderer renderer, double u) {
            double spacing = 3.2 * u;
            double textH = theme.textHeight(true);
            double tx = LEFT * u + 4 * u;
            double ty = Math.round((TOP * u - textH) / 2);
            double tw = Draw.spacedWidth(theme, TITLE, spacing, true);

            // Soft light behind the logo
            double pulse = Anim.pulse();
            Sdf.box(tx, ty + textH * 0.35, tw, textH * 0.3, textH * 0.15)
                .fill(Palette.withAlpha(Palette.ACCENT_MID, 0.10 + 0.05 * pulse))
                .glow(22 * u, Palette.withAlpha(Palette.ACCENT_MID, 0.22 + 0.10 * pulse))
                .draw();

            // The logo runs from the first colour through white to the second
            double cx = tx;
            for (int i = 0; i < TITLE.length(); i++) {
                Palette.into(C_LOGO[i], Palette.gradient((double) i / (C_LOGO.length - 1), Palette.ACCENT_1, Palette.WHITE, Palette.ACCENT_2));

                String ch = TITLE.substring(i, i + 1);
                if (!ch.equals(" ")) renderer.text(ch, cx, ty, C_LOGO[i], true);
                cx += theme.textWidth(ch, 1, true) + spacing;
            }
        }

        private void renderFooter(GuiRenderer renderer, double u, double delta) {
            keyTimer -= delta;
            if (keyTimer <= 0) {
                key = guiKey();
                keyTimer = 1;
            }

            double fy = getWindowHeight() - FOOT * u;
            double ty = Math.round(fy + (FOOT * u - theme.textHeight()) / 2);
            double tx = LEFT * u + 4 * u;
            double sp = 0.8 * u;

            tx += Draw.spacedText(renderer, theme, "[" + key + "]", tx, ty, sp, C_KEY, false) + 6 * u;
            tx += Draw.spacedText(renderer, theme, "GUI", tx, ty, sp, C_FOOTER, false) + 18 * u;

            tx += Draw.spacedText(renderer, theme, Integer.toString(MinecraftAccessor.meteor$getFps()), tx, ty, sp, C_KEY, false) + 6 * u;
            tx += Draw.spacedText(renderer, theme, "FPS", tx, ty, sp, C_FOOTER, false) + 18 * u;

            int ping = mc.player != null ? PlayerUtils.getPing() : 0;
            tx += Draw.spacedText(renderer, theme, Integer.toString(ping), tx, ty, sp, C_KEY, false) + 6 * u;
            tx += Draw.spacedText(renderer, theme, "ms", tx, ty, sp, C_FOOTER, false) + 26 * u;

            Draw.spacedText(renderer, theme, "LEFT CLICK  TOGGLE     RIGHT CLICK  SETTINGS", tx, ty, sp, C_FOOTER, false);

            // Equalizer in the bottom right corner
            int bars = 9;
            double bw = 4 * u, gap = 3 * u;
            double ex = getWindowWidth() - LEFT * u - (bars * bw + (bars - 1) * gap);
            double base = fy + (FOOT * u) / 2 + 10 * u;
            double p = Anim.seconds() / 4.2 * Math.PI * 2;

            for (int i = 0; i < bars; i++) {
                double h = (6 + 14 * Math.abs(Math.sin(p + i * 0.7))) * u;
                double bx = ex + i * (bw + gap);

                Sdf.box(bx, base - h, bw, h, 2 * u)
                    .gradient(Palette.ACCENT_2, Palette.ACCENT_1, 90)
                    .glow(6 * u, Palette.withAlpha(Palette.ACCENT_MID, 0.35))
                    .draw();
            }
        }
    }
}
