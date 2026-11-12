package aicoder.earth.gui.screens;

import aicoder.earth.EarthAddon;
import aicoder.earth.gui.Chrome;
import aicoder.earth.gui.EarthGuiTheme;
import aicoder.earth.gui.Fx;
import aicoder.earth.gui.Style;
import aicoder.earth.gui.widgets.WEarthModule;
import com.mojang.datafixers.util.Pair;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.meteor.ModuleBindChangedEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import meteordevelopment.meteorclient.gui.utils.Cell;
import meteordevelopment.meteorclient.gui.widgets.WKeybind;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WSection;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WView;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WFavorite;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPressable;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.misc.NbtUtils;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.meteorclient.utils.misc.input.KeyBinds;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.nbt.CompoundTag;
import org.apache.commons.lang3.SystemUtils;
import org.jspecify.annotations.NonNull;

import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import static com.mojang.blaze3d.platform.InputConstants.KEY_F;
import static com.mojang.blaze3d.platform.InputConstants.MOD_CONTROL;
import static com.mojang.blaze3d.platform.InputConstants.MOD_SUPER;
import static meteordevelopment.meteorclient.MeteorClient.mc;
import static meteordevelopment.meteorclient.utils.Utils.getWindowHeight;
import static meteordevelopment.meteorclient.utils.Utils.getWindowWidth;

/**
 * Modules tab as a single 840x480 window: categories | modules | settings of the selected module.
 * The layout is the same in all ten themes; colors, corners and glyphs come from the theme's {@link Style}.
 *
 * <p>Clicking a module row selects it, clicking its switch (or right clicking the row) toggles it.
 * Ctrl+F opens a search field above the module list. The arrows in the header step through the themes.
 */
public class EarthModulesScreen extends TabScreen {
    // Layout of the mockup, in design pixels
    private static final double WINDOW_W = 840;
    private static final double WINDOW_H = 480;
    private static final double HEADER_H = 46;
    private static final double FOOTER_H = 34;
    private static final double CATEGORIES_W = 170;
    private static final double MODULES_W = 280;
    private static final double TOP_RESERVED = 58;
    private static final double MARGIN = 14;
    private static final double COUNTER_W = 46;
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss");

    // Remembered while the game runs, so reopening the GUI lands where it was left
    private static Category lastCategory;
    private static boolean lastFavorites;
    private static Module lastModule;

    private final String openKey;

    private WFrame frame;
    private WContainer settingsContainer;
    private WKeybind keybind;
    private WTextBox searchBox;

    private Category category = lastCategory;
    private boolean favorites = lastFavorites;
    private Module module = lastModule;
    private boolean searching;
    private String search = "";

    public EarthModulesScreen(GuiTheme theme) {
        super(theme, Tabs.get().getFirst());

        openKey = "[" + KeyBinds.OPEN_GUI.getTranslatedKeyMessage().getString().toUpperCase(Locale.ROOT) + "]";
    }

    private EarthGuiTheme earth() {
        return (EarthGuiTheme) theme;
    }

    private Style style() {
        return earth().style;
    }

    /** Selects the theme {@code step} places along and reopens this tab in it. */
    private void switchTheme(int step) {
        List<EarthGuiTheme> themes = EarthAddon.THEMES;
        int index = themes.indexOf(earth());
        if (index < 0 || themes.size() < 2) return;

        EarthGuiTheme next = themes.get(Math.floorMod(index + step, themes.size()));
        GuiThemes.select(next.name);

        mc.gui.setScreen(null);
        tab.openScreen(GuiThemes.get());
    }

    @Override
    public void initWidgets() {
        frame = add(new WFrame()).widget();
    }

    @Override
    public void reload() {
        if (frame != null) frame.rebuild();
    }

    @Override
    public void tick() {
        super.tick();

        if (module != null) module.settings.tick(settingsContainer, theme);
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent input) {
        if (locked) return false;

        boolean ctrl = SystemUtils.IS_OS_MAC
            ? Input.modsMatch(input.modifiers(), MOD_SUPER)
            : Input.modsMatch(input.modifiers(), MOD_CONTROL);

        if (ctrl && input.key() == KEY_F) {
            setSearching(!searching || !searchBox.isFocused());
            return true;
        }

        return super.keyPressed(input);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return !Modules.get().isBinding();
    }

    @EventHandler
    private void onModuleBindChanged(ModuleBindChangedEvent event) {
        if (keybind != null) keybind.reset();
    }

    @Override
    public boolean toClipboard() {
        return NbtUtils.toClipboard(Modules.get());
    }

    @Override
    public boolean fromClipboard() {
        return NbtUtils.fromClipboard(Modules.get());
    }

    // State

    private List<Category> categories() {
        List<Category> list = new ArrayList<>();
        for (Category c : Modules.loopCategories()) {
            if (!modulesOf(c).isEmpty()) list.add(c);
        }
        return list;
    }

    private List<Module> modulesOf(Category c) {
        List<Module> list = new ArrayList<>();
        List<Module> hidden = Config.get().hiddenModules.get();
        for (Module m : Modules.get().getGroup(c)) {
            if (!hidden.contains(m)) list.add(m);
        }
        return list;
    }

    private List<Module> favoriteModules() {
        List<Module> list = new ArrayList<>();
        for (Module m : Modules.get().getAll()) {
            if (m.favorite) list.add(m);
        }
        list.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.name, b.name));
        return list;
    }

    /** Title override per module, used to show what a search matched. */
    private List<Pair<Module, String>> visibleModules() {
        List<Pair<Module, String>> list = new ArrayList<>();

        if (searching && !search.isEmpty()) {
            int limit = Config.get().moduleSearchCount.get();
            Set<Module> seen = new LinkedHashSet<>();

            for (Pair<Module, String> p : Modules.get().searchTitles(search)) {
                if (list.size() >= limit) break;
                if (seen.add(p.getFirst())) list.add(p);
            }
            for (Module m : Modules.get().searchSettingTitles(search)) {
                if (list.size() >= limit * 2) break;
                if (seen.add(m)) list.add(Pair.of(m, m.title));
            }
            return list;
        }

        List<Module> source = favorites ? favoriteModules() : (category != null ? modulesOf(category) : List.of());
        for (Module m : source) list.add(Pair.of(m, m.title));
        return list;
    }

    private void selectCategory(Category c, boolean fav) {
        category = c;
        favorites = fav;
        lastCategory = c;
        lastFavorites = fav;

        if (searching) setSearching(false);
        else frame.fillModules();
    }

    private void selectModule(Module m) {
        module = m;
        lastModule = m;
        frame.fillSettings();
    }

    private void setSearching(boolean value) {
        searching = value;
        if (!value) search = "";

        frame.fillModules();

        if (value && searchBox != null) {
            searchBox.setFocused(true);
            searchBox.setCursorMax();
        }
    }

    // Widgets

    /** Fixed-size scrolling column. Always scissored, so rows and their free-size text clip together. */
    private class WPane extends WView {
        double fixedW, fixedH;
        double padLeft, padRight;

        @Override
        public void init() {
        }

        /** Vertical padding has to be real content, otherwise the view cannot scroll all the way to the end. */
        void space(double designPx) {
            super.add(new WSpacer(designPx));
        }

        @Override
        protected double handleWidth() {
            return Math.max(2, Math.round(2 * frame.u));
        }

        @Override
        protected double handleX() {
            // Clear of the column divider
            return x + width - handleWidth() - Math.round(4 * frame.u);
        }

        @Override
        protected void onCalculateSize() {
            // Height stays at the content's own when it fits: the view's scroll math assumes that
            maxHeight = fixedH;
            super.onCalculateSize();
            width = fixedW;
        }

        @Override
        protected void onCalculateWidgetPositions() {
            double px = x, pw = width;
            x += padLeft;
            width -= padLeft + padRight;
            super.onCalculateWidgetPositions();
            x = px;
            width = pw;
        }

        @Override
        public boolean render(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            renderer.scissorStart(x, y, width, height);
            boolean result = super.render(renderer, mouseX, mouseY, delta);
            renderer.scissorEnd();
            return result;
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            if (canScroll && hasScrollBar) {
                Color c = Fx.a(style().accent, focused ? 1 : (handleMouseOver ? 0.7 : 0.35));
                renderer.quad(handleX(), handleY(), handleWidth(), handleHeight(), c);
            }
        }
    }

    private class WSpacer extends WWidget {
        private final double designPx;

        WSpacer(double designPx) {
            this.designPx = designPx;
        }

        @Override
        protected void onCalculateSize() {
            width = 0;
            height = Math.max(0, designPx * frame.u);
        }
    }

    private class WCategory extends WPressable {
        private final Category target;
        private final boolean isFavorites;
        private final String number;
        private final String label;
        private double hover;

        WCategory(Category target, boolean isFavorites, int index) {
            this.target = target;
            this.isFavorites = isFavorites;
            this.number = String.format("%02d", index);
            this.label = (isFavorites ? "Favorites" : target.name).toUpperCase(Locale.ROOT);
        }

        private boolean isSelected() {
            if (searching && !search.isEmpty()) return false;
            return isFavorites ? favorites : (!favorites && target.equals(category));
        }

        @Override
        protected void onCalculateSize() {
            width = 0;
            height = 38 * frame.u;
        }

        @Override
        protected void onPressed(int button) {
            selectCategory(target, isFavorites);
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            Style s = style();
            double u = frame.u;
            boolean selected = isSelected();

            hover += delta * 8 * (mouseOver ? 1 : -1);
            hover = Math.max(0, Math.min(1, hover));

            double corner = s.plateCorner * u;
            if (selected) {
                Fx.box(renderer, x, y, width, height, corner, s.plateSelected);

                // Marker bar, kept inside the plate's rounding
                double bw = 3 * u;
                double inset = Math.max(7 * u, Math.abs(corner) * 0.6);
                Fx.box(renderer, x + 1 * u, y + inset, bw, height - inset * 2, bw / 2, s.accent);
            } else {
                Fx.box(renderer, x, y, width, height, corner, Fx.a(s.plate, 1 + 1.2 * hover));
            }

            Fx.TextBatch text = Fx.text(renderer, u);
            double cy = y + height / 2;
            double nx = x + 14 * u;
            text.put(number, nx, Fx.centerY(theme, cy, 9.5, u), 9.5, selected ? s.dim : s.faint, 1, Fx.BOLD);

            double lx = nx + Fx.textWidth(theme, number, 9.5, 1, u) + 10 * u;
            Color name = selected ? s.title : Fx.mix(s.dim, s.title, hover * 0.6);
            text.put(label, lx, Fx.centerY(theme, cy, 11.5, u), 11.5, name, 2, Fx.BOLD);
        }
    }

    /** {@code AUTO ARMOR // SETTINGS} */
    private class WSettingsTitle extends WWidget {
        private final String name;

        WSettingsTitle(String name) {
            this.name = name.toUpperCase(Locale.ROOT);
        }

        @Override
        protected void onCalculateSize() {
            width = 0;
            height = Fx.lineHeight(theme, 14, frame.u);
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            Style s = style();
            double u = frame.u;
            Fx.TextBatch text = Fx.text(renderer, u);

            text.put(name, x, y, 14, s.title, 3, Fx.BOLD);

            double nameW = Fx.textWidth(theme, name, 14, 3, u);
            double subY = y + height - Fx.lineHeight(theme, 10, u) - 1 * u;
            text.put("// SETTINGS", x + nameW + 8 * u, subY, 10, s.accent, 2, Fx.BOLD);
        }
    }

    private class WHint extends WWidget {
        private final String message;

        WHint(String message) {
            this.message = message;
        }

        @Override
        protected void onCalculateSize() {
            width = 0;
            height = Fx.lineHeight(theme, 10, frame.u) + 8 * frame.u;
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            Fx.text(renderer, frame.u).put(message, x, y + 4 * frame.u, 10, style().faint, 2, Fx.BOLD);
        }
    }

    /** Header arrow that steps to the previous or next theme. */
    private class WArrow extends WPressable {
        private final int step;

        WArrow(int step) {
            this.step = step;
            this.tooltip = step < 0 ? "Previous theme" : "Next theme";
        }

        @Override
        protected void onCalculateSize() {
            width = 18 * frame.u;
            height = 18 * frame.u;
        }

        @Override
        protected void onPressed(int button) {
            switchTheme(step);
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            Style s = style();
            Chrome.field(renderer, s, x, y, width, height, frame.u, mouseOver, pressed);

            double cx = x + width / 2, cy = y + height / 2;
            double a = 3.5 * frame.u;
            Color c = mouseOver || pressed ? s.accent : s.dim;
            Fx.tri(renderer, cx - a * step * 0.6, cy - a, cx - a * step * 0.6, cy + a, cx + a * step * 0.8, cy, c);
        }
    }

    /** The window itself: chrome, header, footer and the three columns. */
    private class WFrame extends WContainer {
        /** Framebuffer pixels per design pixel; shrinks below the theme's unit when the game window is too small. */
        double u;
        double wx, wy, ww, wh;

        private WPane categoriesPane, modulesPane, settingsPane;
        private WArrow previousArrow, nextArrow;
        private WVerticalList moduleList;
        private double builtUnit;

        @Override
        public void init() {
            frame = this;

            categoriesPane = super.add(new WPane()).widget();
            modulesPane = super.add(new WPane()).widget();
            settingsPane = super.add(new WPane()).widget();

            previousArrow = super.add(new WArrow(-1)).widget();
            nextArrow = super.add(new WArrow(1)).widget();

            categoriesPane.hasScrollBar = false;

            layout();
            rebuild();
        }

        /** Meteor paddings and spacings are in Meteor units; this converts design pixels of this window into them. */
        private double mu(double designPx) {
            return designPx * u / theme.scale(1);
        }

        private void layout() {
            double winW = getWindowWidth();
            double winH = getWindowHeight();
            double themeUnit = earth().unit();
            double top = TOP_RESERVED * themeUnit;

            u = Math.min(themeUnit, Math.min(winW / (WINDOW_W + MARGIN * 2), (winH - top) / (WINDOW_H + MARGIN)));
            u = Math.max(0.5, u);

            ww = Math.round(WINDOW_W * u);
            wh = Math.round(WINDOW_H * u);
            wx = Math.round((winW - ww) / 2);
            wy = Math.round(Math.max(top, (winH - wh) / 2));

            double bodyH = wh - Math.round((HEADER_H + 1) * u) - Math.round(FOOTER_H * u);

            categoriesPane.fixedW = Math.round(CATEGORIES_W * u);
            categoriesPane.fixedH = bodyH;
            categoriesPane.padLeft = 14 * u;

            modulesPane.fixedW = Math.round(MODULES_W * u);
            modulesPane.fixedH = bodyH;
            modulesPane.padLeft = modulesPane.padRight = 14 * u;

            settingsPane.fixedW = ww - categoriesPane.fixedW - modulesPane.fixedW;
            settingsPane.fixedH = bodyH;
            settingsPane.padLeft = settingsPane.padRight = 18 * u;
        }

        @Override
        public void calculateSize() {
            layout();
            if (u != builtUnit) rebuild();

            super.calculateSize();
        }

        @Override
        protected void onCalculateSize() {
            width = getWindowWidth();
            height = getWindowHeight();
        }

        @Override
        protected void onCalculateWidgetPositions() {
            double bodyY = wy + Math.round((HEADER_H + 1) * u);

            place(categoriesPane, wx, bodyY);
            place(modulesPane, wx + categoriesPane.fixedW, bodyY);
            place(settingsPane, wx + categoriesPane.fixedW + modulesPane.fixedW, bodyY);

            // Theme switcher: [<] 03/10 [>] at the right end of the header. Its position must not depend on
            // the theme, so the arrows stay under the cursor while stepping through
            double ay = wy + Math.round((HEADER_H * u - nextArrow.height) / 2);
            place(nextArrow, wx + ww - 22 * u - nextArrow.width, ay);
            place(previousArrow, nextArrow.x - COUNTER_W * u - previousArrow.width, ay);
        }

        private void place(WWidget widget, double px, double py) {
            widget.x = Math.round(px);
            widget.y = Math.round(py);
        }

        private String counter() {
            return String.format("%02d/%02d", EarthAddon.THEMES.indexOf(earth()) + 1, EarthAddon.THEMES.size());
        }

        private double tagWidth() {
            return Fx.textWidth(theme, style().tag, 9.5, 1.5, u) - 1.5 * u + 20 * u;
        }

        // Content

        void rebuild() {
            builtUnit = u;

            List<Category> categories = categories();
            if (!favorites && (category == null || !categories.contains(category))) {
                category = categories.isEmpty() ? null : categories.getFirst();
            }
            if (module == null && category != null) {
                List<Module> first = modulesOf(category);
                if (!first.isEmpty()) module = first.getFirst();
            }

            fillCategories(categories);
            fillModules();
            fillSettings();
        }

        private void fillCategories(List<Category> categories) {
            categoriesPane.clear();
            categoriesPane.spacing = mu(8);
            categoriesPane.space(14 - 8);

            int index = 1;
            for (Category c : categories) {
                categoriesPane.add(new WCategory(c, false, index++)).expandX();
            }
            if (!favoriteModules().isEmpty() || favorites) {
                categoriesPane.add(new WCategory(null, true, index)).expandX();
            }

            categoriesPane.space(14 - 8);
        }

        void fillModules() {
            modulesPane.clear();
            modulesPane.spacing = mu(6);
            modulesPane.space(14 - 6);

            searchBox = null;
            if (searching) {
                searchBox = modulesPane.add(theme.textBox(search, "Search modules and settings")).expandX().widget();
                searchBox.action = () -> {
                    search = searchBox.get();
                    fillModuleList();
                };
            }

            moduleList = modulesPane.add(theme.verticalList()).expandX().widget();
            moduleList.spacing = mu(6);
            fillModuleList();

            modulesPane.space(14 - 6);
        }

        private void fillModuleList() {
            moduleList.clear();

            List<Pair<Module, String>> modules = visibleModules();
            for (Pair<Module, String> entry : modules) {
                WEarthModule row = new WEarthModule(entry.getFirst(), entry.getSecond());
                row.unit = u;
                row.selected = () -> module;
                row.onSelect = EarthModulesScreen.this::selectModule;
                moduleList.add(row).expandX();
            }

            if (modules.isEmpty()) {
                moduleList.add(new WHint(searching ? (search.isEmpty() ? "TYPE TO SEARCH" : "NO MATCH") : "NO MODULES")).expandX();
            }
        }

        void fillSettings() {
            settingsPane.clear();
            settingsPane.spacing = mu(9);
            settingsPane.space(16 - 9);

            settingsContainer = null;
            keybind = null;

            Module m = module;
            if (m == null) {
                settingsPane.add(new WHint("SELECT A MODULE")).expandX();
                return;
            }

            double contentW = settingsPane.fixedW - settingsPane.padLeft - settingsPane.padRight;

            // Title, favorite star on the right
            WHorizontalList head = settingsPane.add(theme.horizontalList()).expandX().widget();
            head.add(new WSettingsTitle(m.title)).expandX();
            WFavorite favorite = head.add(theme.favorite(m.favorite)).right().widget();
            favorite.tooltip = "Favorite";
            favorite.action = () -> {
                m.favorite = favorite.checked;
                fillCategories(categories());
                if (favorites) fillModules();
            };

            settingsPane.add(theme.label(m.description, contentW).color(theme.textSecondaryColor()));

            if (m.addon != null && m.addon != MeteorClient.ADDON) {
                WHorizontalList addon = settingsPane.add(theme.horizontalList()).expandX().widget();
                addon.add(theme.label("From: ").color(theme.textSecondaryColor()));
                addon.add(theme.label(m.addon.name).color(m.addon.color));
            }

            // Settings
            if (!m.settings.groups.isEmpty()) {
                settingsContainer = settingsPane.add(theme.verticalList()).expandX().widget();
                settingsContainer.add(theme.settings(m.settings)).expandX();
            }

            // Custom widget
            WWidget custom = m.getWidget(theme);
            if (custom != null) {
                settingsPane.add(theme.horizontalSeparator()).expandX();
                Cell<WWidget> cell = settingsPane.add(custom);
                if (custom instanceof WContainer) cell.expandX();
            }

            // Bind
            WSection section = settingsPane.add(theme.section("Bind", true)).expandX().widget();

            WHorizontalList bind = section.add(theme.horizontalList()).expandX().widget();
            bind.add(theme.label("Bind: "));
            keybind = bind.add(theme.keybind(m.keybind)).expandX().widget();
            keybind.actionOnSet = () -> Modules.get().setModuleToBind(m);
            WButton reset = bind.add(theme.button(GuiRenderer.RESET)).expandCellX().right().widget();
            reset.action = keybind::resetBind;
            reset.tooltip = "Reset";

            WHorizontalList onRelease = section.add(theme.horizontalList()).widget();
            onRelease.add(theme.label("Toggle on bind release: "));
            WCheckbox onReleaseBox = onRelease.add(theme.checkbox(m.toggleOnBindRelease)).widget();
            onReleaseBox.action = () -> m.toggleOnBindRelease = onReleaseBox.checked;

            WHorizontalList feedback = section.add(theme.horizontalList()).widget();
            feedback.add(theme.label("Chat Feedback: "));
            WCheckbox feedbackBox = feedback.add(theme.checkbox(m.chatFeedback)).widget();
            feedbackBox.action = () -> m.chatFeedback = feedbackBox.checked;

            // Config sharing
            WHorizontalList sharing = settingsPane.add(theme.horizontalList()).right().widget();
            WButton copy = sharing.add(theme.button(GuiRenderer.COPY)).widget();
            copy.action = () -> copyModule(m);
            copy.tooltip = "Copy config";
            WButton paste = sharing.add(theme.button(GuiRenderer.PASTE)).widget();
            paste.action = () -> pasteModule(m);
            paste.tooltip = "Paste config";

            settingsPane.space(10 - 9);
        }

        private void copyModule(Module m) {
            CompoundTag tag = new CompoundTag();
            tag.putString("name", m.name);

            CompoundTag settingsTag = m.settings.toTag();
            if (!settingsTag.isEmpty()) tag.put("settings", settingsTag);

            NbtUtils.toClipboard(tag);
        }

        private void pasteModule(Module m) {
            CompoundTag tag = NbtUtils.fromClipboard();
            if (tag == null || !tag.getStringOr("name", "").equals(m.name)) return;

            Optional<CompoundTag> settings = tag.getCompound("settings");
            if (settings.isPresent()) m.settings.fromTag(settings.get());
            else m.settings.reset();

            fillSettings();
        }

        // Rendering

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            EarthGuiTheme earth = earth();
            Style s = earth.style;

            earth.backdrop(renderer);
            Chrome.panel(renderer, s, wx, wy, ww, wh, u);
            renderHeader(renderer, s);
            renderDividers(renderer, s);
            renderFooter(renderer, s);
        }

        private void renderHeader(GuiRenderer renderer, Style s) {
            double cy = wy + HEADER_H * u / 2;
            double gap = 12 * u;
            Fx.TextBatch text = Fx.text(renderer, u);

            String logo = "AICODER";
            double x = wx + 26 * u;
            text.put(logo, x, Fx.centerY(theme, cy, 18, u), 18, s.title, 6, Fx.BOLD);
            x += Fx.textWidth(theme, logo, 18, 6, u) + gap - 6 * u;

            String sub = "// " + s.subtitle;
            text.put(sub, x, Fx.centerY(theme, cy, 11, u), 11, s.accent, 2, Fx.BOLD);
            x += Fx.textWidth(theme, sub, 11, 2, u) + gap;

            // Meteor's custom fonts have no CJK glyphs; the vanilla font does
            if (Fx.vanillaText()) {
                text.put(s.cjk, x, Fx.centerY(theme, cy, 10, u), 10, s.accent2, 3, Fx.BOLD);
            }

            // Tag at the right edge
            double tagW = tagWidth();
            double tagH = Math.round(20 * u);
            double tagX = previousArrow.x - 12 * u - tagW;
            double tagY = Math.round(cy - tagH / 2);
            Chrome.field(renderer, s, tagX, tagY, tagW, tagH, u, false, false);
            text.put(s.tag, tagX + 10 * u, Fx.centerY(theme, cy, 9.5, u), 9.5, s.dim, 1.5, Fx.BOLD);

            // Counter between the two arrows
            double mid = (previousArrow.x + previousArrow.width + nextArrow.x) / 2;
            text.put(counter(), mid + 0.75 * u, Fx.centerY(theme, cy, 9.5, u), 9.5, s.dim, 1.5, Fx.CENTER | Fx.BOLD);
        }

        private void renderDividers(GuiRenderer renderer, Style s) {
            double t = Fx.hair(u);
            Color line = Fx.a(s.outline, 0.75);
            double top = categoriesPane.y;
            double h = categoriesPane.fixedH;

            renderer.quad(wx + t, top - t, ww - t * 2, t, line);
            renderer.quad(modulesPane.x - t, top, t, h, line);
            renderer.quad(settingsPane.x - t, top, t, h, line);
            renderer.quad(wx + t, top + h, ww - t * 2, t, line);

            if (s.backdrop == Style.Backdrop.STRATA) {
                // A core sample down the left edge
                Color[] core = {s.accent, s.accent2, s.dim, s.faint, s.accent2};
                double bh = h / core.length;
                for (int i = 0; i < core.length; i++) {
                    renderer.quad(wx + t, top + bh * i, 4 * u, bh, Fx.a(core[i], 0.55));
                }
            } else if (s.backdrop == Style.Backdrop.SEISMOGRAPH) {
                // Time marks along the bottom edge of the chart
                double by = wy + wh - t;
                for (int i = 0; i * 12 * u < ww - 24 * u; i++) {
                    double tick = (i % 5 == 0 ? 6 : 3) * u;
                    renderer.quad(Math.round(wx + 12 * u + i * 12 * u), by - tick, t, tick, Fx.a(s.dim, 0.55));
                }
            }
        }

        private void renderFooter(GuiRenderer renderer, Style s) {
            double top = categoriesPane.y + categoriesPane.fixedH;
            double cy = top + (wy + wh - top) / 2;
            double ty = Fx.centerY(theme, cy, 10, u);
            Fx.TextBatch text = Fx.text(renderer, u);
            String[] r = s.readouts;

            double x = wx + 26 * u;
            x = readout(text, s, x, ty, openKey, true);
            x = readout(text, s, x, ty, " GUI", false) + 20 * u;
            x = readout(text, s, x, ty, r[0], false);
            x = readout(text, s, x, ty, r[1], true) + 20 * u;
            x = readout(text, s, x, ty, r[2], false);
            readout(text, s, x, ty, r[3], true);

            // The orbit theme shows the real time
            String last = s.backdrop == Style.Backdrop.ORBIT
                ? LocalTime.now(ZoneOffset.UTC).format(CLOCK)
                : r[5];
            double right = wx + ww - 26 * u;
            double lastW = Fx.textWidth(theme, last, 10, 1.5, u);
            double labelW = Fx.textWidth(theme, r[4], 10, 1.5, u);
            readout(text, s, right - lastW - labelW, ty, r[4], false);
            readout(text, s, right - lastW, ty, last, true);
        }

        private double readout(Fx.TextBatch text, Style s, double x, double y, String value, boolean highlight) {
            text.put(value, x, y, 10, highlight ? s.accent : s.dim, 1.5, Fx.BOLD);
            return x + Fx.textWidth(theme, value, 10, 1.5, u);
        }
    }
}
