package aicoder.client.gui.screens;

import aicoder.client.gui.AiCoderGuiTheme;
import aicoder.client.gui.Cyber;
import aicoder.client.gui.widgets.WCyberModule;
import net.minecraft.util.Pair;
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
import meteordevelopment.meteorclient.utils.misc.input.KeyBinds;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.input.KeyInput;
import net.minecraft.nbt.NbtCompound;
import org.apache.commons.lang3.SystemUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_F;
import static org.lwjgl.glfw.GLFW.GLFW_MOD_CONTROL;
import static org.lwjgl.glfw.GLFW.GLFW_MOD_SUPER;
import static meteordevelopment.meteorclient.MeteorClient.mc;
import static meteordevelopment.meteorclient.utils.Utils.getWindowHeight;
import static meteordevelopment.meteorclient.utils.Utils.getWindowWidth;

/**
 * Modules tab as a single 840x480 window: categories | modules | settings of the selected module.
 *
 * <p>Clicking a module row selects it, clicking its hexagon (or right clicking the row) toggles it.
 * Ctrl+F opens a search field above the module list.
 */
public class CyberModulesScreen extends TabScreen {
    // Layout of the mockup, in design pixels
    private static final double WINDOW_W = 840;
    private static final double WINDOW_H = 480;
    private static final double HEADER_H = 46;
    private static final double FOOTER_H = 34;
    private static final double CATEGORIES_W = 170;
    private static final double MODULES_W = 280;
    private static final double TOP_RESERVED = 58;
    private static final double MARGIN = 14;

    private static final String KANA_TAGLINE = "サイバー・システム";
    private static final Color TAG_TEXT = new Color(159, 220, 255);
    private static final Color HAZARD_DARK = new Color(20, 4, 16);

    // Remembered while the game runs, so reopening the GUI lands where it was left
    private static Category lastCategory;
    private static boolean lastFavorites;
    private static Module lastModule;

    private final String versionTag;
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

    public CyberModulesScreen(GuiTheme theme) {
        super(theme, Tabs.get().getFirst());

        versionTag = "SYS v" + shortVersion(modVersion("aicoder-client")) + " · MC " + modVersion("minecraft");
        openKey = "[" + KeyBinds.OPEN_GUI.getBoundKeyLocalizedText().getString().toUpperCase(Locale.ROOT) + "]";
    }

    private static String modVersion(String id) {
        return FabricLoader.getInstance().getModContainer(id)
            .map(mod -> mod.getMetadata().getVersion().getFriendlyString())
            .orElse("?");
    }

    private static String shortVersion(String version) {
        String[] parts = version.split("\\.");
        return parts.length >= 2 ? parts[0] + "." + parts[1] : version;
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
    public boolean keyPressed(KeyInput input) {
        if (locked) return false;

        boolean ctrl = SystemUtils.IS_OS_MAC
            ? input.modifiers() == GLFW_MOD_SUPER
            : input.modifiers() == GLFW_MOD_CONTROL;

        if (ctrl && input.key() == GLFW_KEY_F) {
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
                if (seen.add(p.getLeft())) list.add(p);
            }
            for (Module m : Modules.get().searchSettingTitles(search)) {
                if (list.size() >= limit * 2) break;
                if (seen.add(m)) list.add(new Pair<>(m, m.title));
            }
            return list;
        }

        List<Module> source = favorites ? favoriteModules() : (category != null ? modulesOf(category) : List.of());
        for (Module m : source) list.add(new Pair<>(m, m.title));
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
                Color c = focused ? Cyber.MAGENTA : Cyber.a(Cyber.CYAN_LINE, handleMouseOver ? 0.7 : 0.3);
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
            double u = frame.u;
            boolean selected = isSelected();
            Color accent = ((AiCoderGuiTheme) theme).accentColor.get();

            hover += delta * 8 * (mouseOver ? 1 : -1);
            hover = Math.max(0, Math.min(1, hover));

            double cut = 10 * u;
            if (selected) {
                Cyber.cutRect(renderer, x, y, width, height, 0, cut, 0, 0, Cyber.a(accent, 0.26), Cyber.a(accent, 0.03));
                Cyber.glowRect(renderer, x, y, 3 * u, height, 6 * u, Cyber.a(accent, 0.35));
                renderer.quad(x, y, 3 * u, height, accent);
            } else {
                Cyber.cutRect(renderer, x, y, width, height, 0, cut, 0, 0, Cyber.a(Cyber.SLATE, 0.07 + 0.08 * hover));
            }

            Cyber.TextBatch text = Cyber.text(renderer, u);
            double cy = y + height / 2;
            double nx = x + 14 * u;
            text.put(number, nx, Cyber.centerY(theme, cy, 9.5, u), 9.5, selected ? accent : Cyber.TEXT_FAINT, 1, Cyber.BOLD);

            double lx = nx + Cyber.textWidth(theme, number, 9.5, 1, u) + 10 * u;
            Color name = selected ? Cyber.WHITE : Cyber.mix(Cyber.TEXT_IDLE, Cyber.WHITE, hover * 0.6);
            text.put(label, lx, Cyber.centerY(theme, cy, 11.5, u), 11.5, name, 2, Cyber.BOLD);
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
            double u = frame.u;
            width = 0;
            height = Cyber.lineHeight(theme, 14, u);
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            double u = frame.u;
            Cyber.TextBatch text = Cyber.text(renderer, u);

            text.glow(name, x, y, 14, Cyber.WHITE, 3, Cyber.BOLD, Cyber.CYAN_LINE, 1.6);

            double nameW = Cyber.textWidth(theme, name, 14, 3, u);
            double subY = y + height - Cyber.lineHeight(theme, 10, u) - 1 * u;
            text.put("// SETTINGS", x + nameW + 8 * u, subY, 10, Cyber.CYAN, 2, Cyber.BOLD);
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
            height = Cyber.lineHeight(theme, 10, frame.u) + 8 * frame.u;
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            Cyber.text(renderer, frame.u).put(message, x, y + 4 * frame.u, 10, Cyber.TEXT_FAINT, 2, Cyber.BOLD);
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
            double themeUnit = ((AiCoderGuiTheme) theme).unit();
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
        }

        private void place(WPane pane, double px, double py) {
            pane.x = px;
            pane.y = py;
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
                WCyberModule row = new WCyberModule(entry.getLeft(), entry.getRight());
                row.unit = u;
                row.selected = () -> module;
                row.onSelect = CyberModulesScreen.this::selectModule;
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
            NbtCompound tag = new NbtCompound();
            tag.putString("name", m.name);

            NbtCompound settingsTag = m.settings.toTag();
            if (!settingsTag.isEmpty()) tag.put("settings", settingsTag);

            NbtUtils.toClipboard(tag);
        }

        private void pasteModule(Module m) {
            NbtCompound tag = NbtUtils.fromClipboard();
            if (tag == null || !tag.getString("name", "").equals(m.name)) return;

            Optional<NbtCompound> settings = tag.getCompound("settings");
            if (settings.isPresent()) m.settings.fromTag(settings.get());
            else m.settings.reset();

            fillSettings();
        }

        // Rendering

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            AiCoderGuiTheme cyber = (AiCoderGuiTheme) theme;
            double p = Cyber.phase();

            cyber.backdrop(renderer);
            Cyber.panel(renderer, wx, wy, ww, wh, u, cyber.backgroundColor.get(), cyber.accentColor.get(), true);
            renderLattice(renderer, p);
            renderHeader(renderer, cyber, p);
            renderDividers(renderer);
            renderFooter(renderer, cyber, p);
        }

        /** Magenta dot grid, only visible through a soft spotlight that wanders around the window. */
        private void renderLattice(GuiRenderer renderer, double p) {
            double pitch = 14 * u;
            double dot = Math.max(1, Math.round(1.6 * u));
            double cut = 16 * u;

            double gx = ww * (0.50 + 0.32 * Cyber.flow(p, 0.21));
            double gy = wh * (0.48 + 0.26 * Cyber.flow(p, 0.63));

            // The CSS mask is a farthest-corner radial gradient
            double rx = Math.max(gx, ww - gx);
            double ry = Math.max(gy, wh - gy);
            double radius = Math.sqrt(rx * rx + ry * ry);
            double reach = radius * 0.62;

            int x0 = (int) Math.max(0, Math.floor((gx - reach) / pitch));
            int x1 = (int) Math.min(Math.floor((ww - dot) / pitch), Math.ceil((gx + reach) / pitch));
            int y0 = (int) Math.max(0, Math.floor((gy - reach) / pitch));
            int y1 = (int) Math.min(Math.floor((wh - dot) / pitch), Math.ceil((gy + reach) / pitch));

            for (int iy = y0; iy <= y1; iy++) {
                for (int ix = x0; ix <= x1; ix++) {
                    double dx = (ix + 0.5) * pitch;
                    double dy = (iy + 0.5) * pitch;

                    // Stay out of the cut corners
                    if (dx + dy < cut + pitch || (ww - dx) + (wh - dy) < cut + pitch) continue;

                    double d = Math.sqrt((dx - gx) * (dx - gx) + (dy - gy) * (dy - gy)) / radius;
                    double mask = d < 0.30 ? 1 - d / 0.30 * 0.65 : (d < 0.62 ? 0.35 * (1 - (d - 0.30) / 0.32) : 0);
                    if (mask <= 0.02) continue;

                    renderer.quad(Math.round(wx + dx - dot / 2), Math.round(wy + dy - dot / 2), dot, dot,
                        Cyber.rgba(255, 42, 109, 0.55 * 0.38 * mask));
                }
            }
        }

        private void renderHeader(GuiRenderer renderer, AiCoderGuiTheme cyber, double p) {
            Color accent = cyber.accentColor.get();
            double cy = wy + HEADER_H * u / 2;
            double gap = 12 * u;

            Cyber.TextBatch text = Cyber.text(renderer, u);

            // Logo, with the glitch bursts
            String logo = "AICODER";
            double logoX = wx + 30 * u;
            double logoY = Cyber.centerY(theme, cy, 18, u);
            double logoW = Cyber.textWidth(theme, logo, 18, 6, u);
            double logoH = Cyber.lineHeight(theme, 18, u);

            double glitch = glitchAmount(p);
            boolean dimmed = glitch > 0.6 && frac(p * 331) < 0.5;
            text.glow(logo, logoX, logoY, 18, Cyber.a(Cyber.WHITE, dimmed ? 0.55 : 1), 6, Cyber.BOLD, Cyber.CYAN_LINE, 2);

            if (glitch > 0) {
                double jitter = Math.sin(p * 997) * glitch;
                double c1 = frac(p * 7919) * 0.60;
                double c2 = frac(p * 104729) * 0.40;

                glitchSlice(renderer, logo, logoX + jitter * 6 * u, logoY, logoX, logoW, logoH, c1, c2, accent);
                glitchSlice(renderer, logo, logoX - jitter * 5 * u, logoY, logoX, logoW, logoH, c2, c1, Cyber.CYAN);
            }

            double x = logoX + logoW + gap - 6 * u;

            String sub = "// CLIENT";
            text.put(sub, x, Cyber.centerY(theme, cy, 11, u), 11, Cyber.a(accent, 0.85), 2, Cyber.BOLD);
            x += Cyber.textWidth(theme, sub, 11, 2, u) + gap;

            if (Cyber.vanillaText()) {
                x += 4 * u;
                text.put(KANA_TAGLINE, x, Cyber.centerY(theme, cy, 10, u), 10, Cyber.a(accent, 0.75), 3, Cyber.BOLD);
                x += Cyber.textWidth(theme, KANA_TAGLINE, 10, 3, u) + gap;
            }

            // Version tag
            double tagW = Cyber.textWidth(theme, versionTag, 9.5, 1.5, u) - 1.5 * u + 16 * u;
            double tagH = Math.round(18 * u);
            double tagY = Math.round(cy - tagH / 2);
            double tagCut = 5 * u;
            Cyber.box(renderer, x, tagY, tagW, tagH, tagCut, 0, tagCut, 0, Cyber.hair(u),
                Cyber.a(Cyber.CYAN_LINE, 0.06), Cyber.a(Cyber.CYAN_LINE, 0.35));
            text.put(versionTag, x + 8 * u, Cyber.centerY(theme, cy, 9.5, u), 9.5, TAG_TEXT, 1.5, Cyber.BOLD);

            // Link status, laid out from the right edge
            double right = wx + ww - 22 * u;
            double statusY = Cyber.centerY(theme, cy, 9.5, u);

            String stable = "STABLE";
            double stableW = Cyber.textWidth(theme, stable, 9.5, 2, u);
            text.put(stable, right - stableW, statusY, 9.5, Cyber.DIM, 2, Cyber.BOLD);
            right -= stableW + 8 * u;

            for (int i = 4; i >= 0; i--) {
                double bh = (4 + i * 2) * u;
                double bx = right - 3 * u;
                boolean lit = i < 4;
                if (lit) Cyber.glowRect(renderer, bx, cy + 6 * u - bh, 3 * u, bh, 3 * u, Cyber.a(Cyber.CYAN, 0.25));
                renderer.quad(bx, cy + 6 * u - bh, 3 * u, bh, lit ? Cyber.CYAN : Cyber.a(Cyber.CYAN_LINE, 0.2));
                right -= 5 * u;
            }
            right -= 6 * u;

            String link = "LINK";
            double linkW = Cyber.textWidth(theme, link, 9.5, 2, u);
            text.put(link, right - linkW, statusY, 9.5, Cyber.DIM, 2, Cyber.BOLD);
            right -= linkW + 8 * u;

            String rec = "REC";
            double recW = Cyber.textWidth(theme, rec, 9.5, 2, u);
            text.put(rec, right - recW, statusY, 9.5, accent, 2, Cyber.BOLD);
            double dotX = right - recW - 9 * u;
            Cyber.hexagon(renderer, dotX, cy, 5.5 * u, Cyber.a(accent, 0.22));
            Cyber.hexagon(renderer, dotX, cy, 3 * u, accent);

            Cyber.headerRule(renderer, wx, wy + Math.round(HEADER_H * u), ww, 30 * u, 22 * u, u);
        }

        /** 1 at the middle of each burst (12%, 47% and 81% of the loop, about 0.2s each), 0 otherwise. */
        private double glitchAmount(double p) {
            double g = 0;
            for (double burst : new double[]{0.12, 0.47, 0.81}) {
                double d = Math.abs(p - burst);
                if (d < 0.025) g = 1 - d / 0.025;
            }
            return g;
        }

        private double frac(double v) {
            return v - Math.floor(v);
        }

        /** One color layer of the glitch: the logo shifted sideways, showing only a horizontal band. */
        private void glitchSlice(GuiRenderer renderer, String logo, double x, double y, double baseX, double w, double h,
                                 double insetTop, double insetBottom, Color color) {
            double bandY = y + h * insetTop;
            double bandH = h * (1 - insetTop - insetBottom);
            if (bandH < 1) return;

            renderer.scissorStart(baseX - 12 * u, bandY, w + 24 * u, bandH);
            Cyber.text(renderer, u).put(logo, x, y, 18, color, 6, Cyber.BOLD);
            renderer.scissorEnd();
        }

        private void renderDividers(GuiRenderer renderer) {
            double t = Cyber.hair(u);
            Color line = Cyber.a(Cyber.CYAN_LINE, Cyber.LINE_ALPHA);
            double top = categoriesPane.y;
            double h = categoriesPane.fixedH;

            renderer.quad(modulesPane.x - t, top, t, h, line);
            renderer.quad(settingsPane.x - t, top, t, h, line);
            renderer.quad(wx, top + h, ww - 16 * u, t, line);
        }

        private void renderFooter(GuiRenderer renderer, AiCoderGuiTheme cyber, double p) {
            Color accent = cyber.accentColor.get();
            double top = categoriesPane.y + categoriesPane.fixedH;
            double cy = top + (wy + wh - top) / 2;
            double x = wx + 26 * u;

            // Hazard strip: diagonal stripes sliding sideways
            double hw = Math.round(60 * u);
            double hh = Math.round(10 * u);
            double hy = Math.round(cy - hh / 2);
            double period = 17 * u;
            double shift = frac(p * 3) * period;

            renderer.scissorStart(x, hy, hw, hh);
            renderer.quad(x, hy, hw, hh, Cyber.a(HAZARD_DARK, 0.85));
            for (double sx = x - period * 2 + shift; sx < x + hw; sx += period) {
                Color c = Cyber.a(accent, 0.85);
                Cyber.tri(renderer, sx, hy + hh, sx + period / 2, hy + hh, sx + period / 2 + hh, hy, c);
                Cyber.tri(renderer, sx, hy + hh, sx + period / 2 + hh, hy, sx + hh, hy, c);
            }
            renderer.scissorEnd();
            x += hw + 16 * u;

            // Readouts
            Cyber.TextBatch text = Cyber.text(renderer, u);
            double ty = Cyber.centerY(theme, cy, 10, u);

            Runtime runtime = Runtime.getRuntime();
            long used = runtime.totalMemory() - runtime.freeMemory();
            int memory = (int) Math.round(used * 100.0 / runtime.maxMemory());
            int ping = mc.player != null ? PlayerUtils.getPing() : 0;

            x = readout(text, x, ty, openKey, true);
            x = readout(text, x, ty, " GUI", false) + 16 * u;
            x = readout(text, x, ty, Integer.toString(mc.getCurrentFps()), true);
            x = readout(text, x, ty, " FPS", false) + 16 * u;
            x = readout(text, x, ty, Integer.toString(ping), true);
            x = readout(text, x, ty, " ms", false) + 16 * u;
            x = readout(text, x, ty, "MEM ", false);
            readout(text, x, ty, memory + "%", true);

            // Oscilloscope traces
            double waveW = 170 * u;
            double waveX = wx + ww - 20 * u - waveW;
            double tau = Math.PI * 2;
            double px = 0, py1 = 0, py2 = 0;

            for (int i = 0; i <= 85; i++) {
                double f = i / 85.0;
                double sx = waveX + f * waveW;
                double y1 = cy + 5 * u * (0.55 * Math.sin(tau * (p + f * 1.4)) + 0.3 * Math.sin(tau * (2 * p + f * 3.2) + 1) + 0.15 * Math.sin(tau * (3 * p - f * 2.1) + 2));
                double y2 = cy + 3.5 * u * (0.6 * Math.sin(tau * (-p + f * 2.2) + 0.7) + 0.4 * Math.sin(tau * (2 * p + f * 1.1) + 2.4));

                if (i > 0) {
                    Cyber.line(renderer, px, py2, sx, y2, Math.max(1, u), Cyber.a(accent, 0.7));
                    Cyber.line(renderer, px, py1, sx, y1, Math.max(1, 1.4 * u), Cyber.CYAN_LINE);
                }

                px = sx;
                py1 = y1;
                py2 = y2;
            }
        }

        private double readout(Cyber.TextBatch text, double x, double y, String value, boolean highlight) {
            text.put(value, x, y, 10, highlight ? Cyber.CYAN : Cyber.DIM, 1.5, Cyber.BOLD);
            return x + Cyber.textWidth(theme, value, 10, 1.5, u);
        }
    }
}
