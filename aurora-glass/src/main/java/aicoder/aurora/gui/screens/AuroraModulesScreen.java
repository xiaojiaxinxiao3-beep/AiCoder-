package aicoder.aurora.gui.screens;

import aicoder.aurora.gui.Chrome;
import aicoder.aurora.gui.AuroraGuiTheme;
import aicoder.aurora.gui.Fx;
import aicoder.aurora.gui.Style;
import aicoder.aurora.gui.widgets.WAuroraModule;
import com.mojang.datafixers.util.Pair;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.meteor.ModuleBindChangedEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
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
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.nbt.CompoundTag;
import org.apache.commons.lang3.SystemUtils;
import org.jspecify.annotations.NonNull;

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
 * Modules tab as a single 1260x780 pane of glass: categories | modules | settings of the selected module.
 *
 * <p>Clicking a module row selects it, clicking its switch (or right clicking the row) toggles it.
 * Ctrl+F opens a search field above the module list.
 */
public class AuroraModulesScreen extends TabScreen {
    // Layout of the mockup, in design pixels
    private static final double WINDOW_W = 1260;
    private static final double WINDOW_H = 780;
    private static final double HEADER_H = 74;
    private static final double FOOTER_H = 52;
    private static final double CATEGORIES_W = 250;
    private static final double MODULES_W = 400;
    private static final double TOP_RESERVED = 86;
    private static final double MARGIN = 20;

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

    public AuroraModulesScreen(GuiTheme theme) {
        super(theme, Tabs.get().getFirst());

        aurora().ensureBlur();
        openKey = "[" + KeyBinds.OPEN_GUI.getTranslatedKeyMessage().getString().toUpperCase(Locale.ROOT) + "]";
    }

    private AuroraGuiTheme aurora() {
        return (AuroraGuiTheme) theme;
    }

    private Style style() {
        return aurora().style;
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
            return Math.max(2, Math.round(3 * frame.u));
        }

        @Override
        protected double handleX() {
            // Clear of the column divider
            return x + width - handleWidth() - Math.round(5 * frame.u);
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
        private final String label;
        private final String count;
        private double hover, lit;

        WCategory(Category target, boolean isFavorites, int size) {
            this.target = target;
            this.isFavorites = isFavorites;
            this.label = (isFavorites ? "Favorites" : target.name).toUpperCase(Locale.ROOT);
            this.count = String.format("%02d", size);
        }

        private boolean isSelected() {
            if (searching && !search.isEmpty()) return false;
            return isFavorites ? favorites : (!favorites && target.equals(category));
        }

        @Override
        protected void onCalculateSize() {
            width = 0;
            height = Math.round(58 * frame.u);
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

            hover = Fx.step(hover, mouseOver, delta, 8.3);
            lit = Fx.step(lit, selected, delta, 8.3);

            double corner = s.plateCorner * u;
            Chrome.plate(renderer, x, y, width, height, u, corner, selected ? 0 : hover);
            Chrome.lit(renderer, x, y, width, height, u, corner, 0.30, 0.20, 0.26, Fx.rgba(255, 255, 255, 0.42), 0.22, lit);
            if (lit > 0) renderer.quad(x + corner, y + Fx.hair(u), width - corner * 2, Fx.hair(u), Fx.rgba(255, 255, 255, 0.4 * lit));

            double cy = y + height / 2;
            double bx = x + 24 * u;
            Chrome.bead(renderer, s, bx, cy, 4 * u, lit, 1);

            Fx.TextBatch text = Fx.text(renderer, u);
            text.put(label, bx + 18 * u, Fx.centerY(theme, cy, 15, u), 15, Fx.mix(s.dim, s.title, Math.max(lit, hover * 0.7)), 2.6, 0);
            text.put(count, x + width - 20 * u, Fx.centerY(theme, cy, 11.5, u), 11.5, s.faint, 0, Fx.RIGHT);
        }
    }

    /** {@code ANCHOR AURA · SETTINGS} */
    private class WSettingsTitle extends WWidget {
        private final String name;

        WSettingsTitle(String name) {
            this.name = name.toUpperCase(Locale.ROOT);
        }

        @Override
        protected void onCalculateSize() {
            width = 0;
            height = Fx.lineHeight(theme, 19, frame.u);
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            Style s = style();
            double u = frame.u;
            Fx.TextBatch text = Fx.text(renderer, u);

            text.put(name, x, y, 19, s.title, 4, Fx.BOLD);
            text.put("\u00B7 SETTINGS", x + Fx.textWidth(theme, name, 19, 4, u) + 12 * u, y, 19, s.faint, 4, 0);
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
            height = Fx.lineHeight(theme, 12, frame.u) + 12 * frame.u;
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            Fx.text(renderer, frame.u).put(message, x, y + 6 * frame.u, 12, style().faint, 3, 0);
        }
    }

    /** The window itself: chrome, header, footer and the three columns. */
    private class WFrame extends WContainer {
        /** Framebuffer pixels per design pixel; shrinks below the theme's unit when the game window is too small. */
        double u;
        double wx, wy, ww, wh;

        private WPane categoriesPane, modulesPane, settingsPane;
        private WVerticalList moduleList;
        private double builtUnit;

        @Override
        public void init() {
            frame = this;

            categoriesPane = super.add(new WPane()).widget();
            modulesPane = super.add(new WPane()).widget();
            settingsPane = super.add(new WPane()).widget();

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
            double themeUnit = aurora().unit();
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
            categoriesPane.padLeft = categoriesPane.padRight = 18 * u;

            modulesPane.fixedW = Math.round(MODULES_W * u);
            modulesPane.fixedH = bodyH;
            modulesPane.padLeft = 18 * u;
            modulesPane.padRight = 16 * u;

            settingsPane.fixedW = ww - categoriesPane.fixedW - modulesPane.fixedW;
            settingsPane.fixedH = bodyH;
            settingsPane.padLeft = settingsPane.padRight = 34 * u;
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
        }

        private void place(WWidget widget, double px, double py) {
            widget.x = Math.round(px);
            widget.y = Math.round(py);
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
            categoriesPane.spacing = mu(12);
            categoriesPane.space(22 - 12);

            for (Category c : categories) {
                categoriesPane.add(new WCategory(c, false, modulesOf(c).size())).expandX();
            }
            List<Module> favoriteList = favoriteModules();
            if (!favoriteList.isEmpty() || favorites) {
                categoriesPane.add(new WCategory(null, true, favoriteList.size())).expandX();
            }

            categoriesPane.space(22 - 12);
        }

        void fillModules() {
            modulesPane.clear();
            modulesPane.spacing = mu(11);
            modulesPane.space(22 - 11);

            searchBox = null;
            if (searching) {
                searchBox = modulesPane.add(theme.textBox(search, "Search modules and settings")).expandX().widget();
                searchBox.action = () -> {
                    search = searchBox.get();
                    fillModuleList();
                };
            }

            moduleList = modulesPane.add(theme.verticalList()).expandX().widget();
            moduleList.spacing = mu(11);
            fillModuleList();

            modulesPane.space(22 - 11);
        }

        private void fillModuleList() {
            moduleList.clear();

            List<Pair<Module, String>> modules = visibleModules();
            for (Pair<Module, String> entry : modules) {
                WAuroraModule row = new WAuroraModule(entry.getFirst(), entry.getSecond());
                row.unit = u;
                row.selected = () -> module;
                row.onSelect = AuroraModulesScreen.this::selectModule;
                moduleList.add(row).expandX();
            }

            if (modules.isEmpty()) {
                moduleList.add(new WHint(searching ? (search.isEmpty() ? "TYPE TO SEARCH" : "NO MATCH") : "NO MODULES")).expandX();
            }
        }

        void fillSettings() {
            settingsPane.clear();
            settingsPane.spacing = mu(10);
            settingsPane.space(26 - 10);

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

            settingsPane.space(16 - 10);
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
            AuroraGuiTheme aurora = aurora();
            Style s = aurora.style;

            aurora.backdrop(renderer);
            Chrome.panel(renderer, wx, wy, ww, wh, u, s.corner * u, false);
            renderHeader(renderer, s);
            renderDividers(renderer);
            renderFooter(renderer, s);
        }

        /** {@code AiCoder client} with the second word in the aurora gradient, and two small tags at the right. */
        private void renderHeader(GuiRenderer renderer, Style s) {
            double cy = wy + HEADER_H * u / 2;
            double size = 26, tracking = 5;
            double ty = Fx.centerY(theme, cy, size, u);
            double x = wx + 34 * u;

            Fx.TextBatch text = Fx.text(renderer, u);
            text.put("AiCoder", x, ty, size, s.title, tracking, 0);
            x += Fx.textWidth(theme, "AiCoder ", size, tracking, u);

            // Soft green light behind the gradient word
            String word = "client";
            double wordW = Fx.textWidth(theme, word, size, tracking, u);
            Fx.glowRect(renderer, x + 6 * u, cy, wordW - 12 * u, 0, 16 * u, Fx.a(s.green, 0.13));

            for (int i = 0; i < word.length(); i++) {
                String c = String.valueOf(word.charAt(i));
                double f = (double) i / (word.length() - 1);
                Color color = f < 0.5 ? Fx.mix(s.green, s.teal, f * 2) : Fx.mix(s.teal, s.violet, f * 2 - 1);
                text.put(c, x, ty, size, color, 0, Fx.BOLD);
                x += Fx.textWidth(theme, c, size, tracking, u);
            }

            double right = wx + ww - 34 * u;
            right = tag(renderer, text, s, right, cy, Modules.get().getAll().size() + " MODULES") - 10 * u;
            tag(renderer, text, s, right, cy, "AURORA GLASS");
        }

        /** Small capsule label, right-aligned at {@code right}; returns its left edge. */
        private double tag(GuiRenderer renderer, Fx.TextBatch text, Style s, double right, double cy, String label) {
            double tw = Fx.textWidth(theme, label, 12, 0.6, u);
            double w = tw + 24 * u, h = 28 * u;
            double x = right - w;

            Fx.box(renderer, x, cy - h / 2, w, h, h / 2, Fx.rgba(255, 255, 255, 0.07));
            Fx.boxOutline(renderer, x, cy - h / 2, w, h, h / 2, Fx.hair(u), Fx.rgba(255, 255, 255, 0.14));
            text.put(label, x + 12 * u, Fx.centerY(theme, cy, 12, u), 12, s.dim, 0.6, 0);
            return x;
        }

        private void renderDividers(GuiRenderer renderer) {
            double t = Fx.hair(u);
            Color line = Fx.rgba(255, 255, 255, 0.14);
            double top = categoriesPane.y;
            double h = categoriesPane.fixedH;
            double in = 2 * u;

            renderer.quad(wx + in, top - t, ww - in * 2, t, line);
            renderer.quad(wx + in, top + h, ww - in * 2, t, line);
            renderer.quad(modulesPane.x - t, top, t, h, line);
            renderer.quad(settingsPane.x - t, top, t, h, line);
        }

        private void renderFooter(GuiRenderer renderer, Style s) {
            double top = categoriesPane.y + categoriesPane.fixedH;
            double cy = top + (wy + wh - top) / 2;
            double ty = Fx.centerY(theme, cy, 12.5, u);
            Fx.TextBatch text = Fx.text(renderer, u);

            int ping = mc.player != null ? PlayerUtils.getPing() : 0;
            int active = Modules.get().getActive().size();

            double x = wx + 34 * u;
            x = readout(text, s, x, ty, openKey, true);
            x = readout(text, s, x, ty, " GUI", false) + 26 * u;
            x = readout(text, s, x, ty, mc.getFps() + " FPS", false) + 26 * u;
            x = readout(text, s, x, ty, ping + " ms", false) + 26 * u;
            x = readout(text, s, x, ty, "ACTIVE ", false);
            readout(text, s, x, ty, Integer.toString(active), true);

            // Equalizer: nine small bars, violet at the top running to green at the bottom, swaying slowly
            double[] rest = {9, 15, 21, 17, 12, 19, 8, 14, 10};
            double p = Fx.phase(6);
            double bw = 5 * u, gap = 4 * u;
            double right = wx + ww - 34 * u;
            double base = cy + 11 * u;

            for (int i = 0; i < rest.length; i++) {
                double sway = Fx.animate ? 0.75 + 0.25 * Fx.flow(p, i * 0.137) : 1;
                double bh = Math.max(4, rest[i] * sway) * u;
                double bx = right - (rest.length - i) * (bw + gap) + gap;
                Fx.box(renderer, bx, base - bh, bw, bh / 2, 0, s.violet, s.teal);
                Fx.box(renderer, bx, base - bh / 2, bw, bh / 2, 0, s.teal, s.green);
            }
        }

        private double readout(Fx.TextBatch text, Style s, double x, double y, String value, boolean highlight) {
            text.put(value, x, y, 12.5, highlight ? s.green : s.dim, 1.4, 0);
            return x + Fx.textWidth(theme, value, 12.5, 1.4, u);
        }
    }
}
