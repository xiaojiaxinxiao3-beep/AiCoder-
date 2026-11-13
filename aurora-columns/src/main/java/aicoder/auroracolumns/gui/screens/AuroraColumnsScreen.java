package aicoder.auroracolumns.gui.screens;

import com.mojang.datafixers.util.Pair;
import aicoder.auroracolumns.gui.AuroraGuiTheme;
import aicoder.auroracolumns.gui.Fx;
import aicoder.auroracolumns.gui.widgets.WAuroraWindow;
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
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.misc.NbtUtils;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import net.minecraft.client.input.KeyEvent;
import org.apache.commons.lang3.SystemUtils;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.mojang.blaze3d.platform.InputConstants.*;
import static meteordevelopment.meteorclient.utils.Utils.getWindowHeight;
import static meteordevelopment.meteorclient.utils.Utils.getWindowWidth;

/**
 * Modules tab: one column per category, side by side, as in Meteor's own screen. This is that screen with
 * the layout adjusted for the aurora columns: they start below the logo, sit 14px apart, share a minimum
 * width and show their module count. Windows that do not fit go under the shortest column.
 */
public class AuroraColumnsScreen extends TabScreen {
    // Design pixels
    private static final double TOP = 110;
    private static final double LEFT = 34;
    private static final double GAP = 14;
    private static final double COLUMN_WIDTH = 252;
    private static final double SEARCH_WIDTH = 224;


    private WCategoryController controller;
    private WWindow searchWindow;
    private WTextBox searchTextBox;

    public AuroraColumnsScreen(GuiTheme theme) {
        super(theme, Tabs.get().getFirst());

        ((AuroraGuiTheme) theme).ensureBlur();
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

    // Category

    private AuroraGuiTheme aurora() {
        return (AuroraGuiTheme) theme;
    }

    private double unit() {
        return aurora().unit();
    }

    /** Columns stop short of the bottom edge; taller ones scroll. */
    private double columnMaxHeight() {
        return Math.max(120 * unit(), getWindowHeight() - (TOP + 54 + 30) * unit());
    }

    /** Vertical breathing room inside a column; real content, so it scrolls with the list. */
    private class WGap extends WWidget {
        private final double designPx;

        WGap(double designPx) {
            this.designPx = designPx;
        }

        @Override
        protected void onCalculateSize() {
            width = 0;
            height = Math.round(designPx * unit());
        }
    }

    private WWindow column(WContainer c, String title, String id) {
        WWindow w = theme.window(title);
        w.id = id;
        w.padding = 0;
        w.spacing = 0;

        c.add(w);
        w.view.scrollOnlyWhenMouseOver = true;
        w.view.hasScrollBar = false;
        w.view.spacing = 0;

        w.view.maxHeight = columnMaxHeight();
        w.minWidth = aurora().mu(COLUMN_WIDTH);
        return w;
    }

    protected WWindow createCategory(WContainer c, Category category, List<Module> moduleList, int index) {
        WWindow w = column(c, category.name, category.name);

        if (w instanceof WAuroraWindow column) {
            column.column = true;
            column.index = index;
            column.count = moduleList.size();
        }

        w.add(new WGap(7));
        for (Module module : moduleList) {
            w.add(theme.module(module)).expandX();
        }
        w.add(new WGap(8));

        return w;
    }

    // Search

    protected void createSearchW(WContainer w, String text) {
        if (!text.isEmpty()) {
            // Titles
            List<Pair<Module, String>> modules = Modules.get().searchTitles(text);

            if (!modules.isEmpty()) {
                WSection section = w.add(theme.section("Modules")).expandX().widget();
                section.spacing = 0;

                int count = 0;
                for (Pair<Module, String> p : modules) {
                    if (count >= Config.get().moduleSearchCount.get() || count >= modules.size()) break;
                    section.add(theme.module(p.getFirst(), p.getSecond())).expandX();
                    count++;
                }
            }

            // Settings
            Set<Module> settings = Modules.get().searchSettingTitles(text);

            if (!settings.isEmpty()) {
                WSection section = w.add(theme.section("Settings")).expandX().widget();
                section.spacing = 0;

                int count = 0;
                for (Module module : settings) {
                    if (count >= Config.get().moduleSearchCount.get() || count >= settings.size()) break;
                    section.add(theme.module(module)).expandX();
                    count++;
                }
            }
        }
    }

    protected WWindow createSearch(WContainer c) {
        WWindow w = theme.window("Search");
        w.id = "search";
        searchWindow = w;

        c.add(w);
        w.view.scrollOnlyWhenMouseOver = true;
        w.view.hasScrollBar = false;
        w.view.maxHeight = columnMaxHeight();
        w.minWidth = aurora().mu(SEARCH_WIDTH);
        if (w instanceof WAuroraWindow column) column.column = true;

        WVerticalList l = theme.verticalList();

        WTextBox text = w.add(theme.textBox("", "Search modules")).minWidth(aurora().mu(SEARCH_WIDTH - 40)).expandX().widget();
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

        WWindow w = theme.window("Favorites");
        w.id = "favorites";
        w.padding = 0;
        w.spacing = 0;

        Cell<WWindow> cell = c.add(w);
        w.view.scrollOnlyWhenMouseOver = true;
        w.view.hasScrollBar = false;
        w.view.spacing = 0;
        w.view.maxHeight = columnMaxHeight();
        w.minWidth = aurora().mu(COLUMN_WIDTH);
        if (w instanceof WAuroraWindow column) column.column = true;

        createFavoritesW(w);
        return cell;
    }

    protected boolean createFavoritesW(WWindow w) {
        List<Module> modules = new ArrayList<>();

        for (Module module : Modules.get().getAll()) {
            if (module.favorite) {
                modules.add(module);
            }
        }

        modules.sort((o1, o2) -> String.CASE_INSENSITIVE_ORDER.compare(o1.name, o2.name));

        for (Module module : modules) {
            w.add(theme.module(module)).expandX();
        }

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

    // Stuff

    protected class WCategoryController extends WContainer {
        public final List<WWindow> windows = new ArrayList<>();
        private Cell<WWindow> favorites;

        @Override
        public void init() {
            List<Module> moduleList = new ArrayList<>();
            for (Category category : Modules.loopCategories()) {
                for (Module module : Modules.get().getGroup(category)) {
                    if (!Config.get().hiddenModules.get().contains(module)) {
                        moduleList.add(module);
                    }
                }

                // Ensure empty categories are not shown
                if (!moduleList.isEmpty()) {
                    windows.add(createCategory(this, category, moduleList, windows.size() + 1));
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

        @Override
        protected void onCalculateSize() {
            width = getWindowWidth();
            height = getWindowHeight();
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            // Two lines of help in the bottom-right corner
            double u = unit();
            double right = getWindowWidth() - 40 * u;
            double lineH = Fx.lineHeight(theme, 12.5, u) * 1.5;
            double y = getWindowHeight() - 26 * u - lineH * 2;

            Fx.TextBatch text = Fx.text(renderer, u);
            text.put("Left click \u00B7 Toggle module", right, y, 12.5, aurora().style.faint, 0, Fx.RIGHT);
            text.put("Right click \u00B7 Open module settings", right, y + lineH, 12.5, aurora().style.faint, 0, Fx.RIGHT);
        }

        @Override
        protected void onCalculateWidgetPositions() {
            double u = unit();
            double gap = GAP * u;
            double left = LEFT * u;
            double windowWidth = getWindowWidth();

            double top = TOP * u;
            double x = left;
            boolean firstRow = true;

            // Columns already placed in the first row: left edge, width and current bottom of each stack
            List<double[]> stacks = new ArrayList<>();

            // Left to right along the top. Whatever no longer fits goes under the shortest column so far,
            // which is where the search box and favorites usually land. A column the player has dragged
            // keeps its own saved position (WWindow applies that itself).
            for (Cell<?> cell : cells) {
                WWidget w = cell.widget();
                double wx, wy;

                if (firstRow && (stacks.isEmpty() || x + w.width <= windowWidth)) {
                    wx = x;
                    wy = top;
                    stacks.add(new double[]{wx, w.width, wy + w.height});
                    x += w.width + gap;
                } else {
                    firstRow = false;

                    double[] shortest = stacks.getFirst();
                    for (double[] stack : stacks) {
                        if (stack[2] < shortest[2]) shortest = stack;
                    }

                    wx = Math.max(0, Math.min(shortest[0], windowWidth - w.width));
                    wy = shortest[2] + gap * 2;
                    shortest[2] = wy + w.height;
                }

                cell.x = wx;
                cell.y = wy;
                cell.width = w.width;
                cell.height = w.height;
                w.x = wx;
                w.y = wy;
            }
        }
    }
}
