package aicoder.combat;

import aicoder.combat.mixin.TabScreenAccessor;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.tabs.Tab;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.tabs.builtin.ModulesTab;
import meteordevelopment.meteorclient.systems.modules.Category;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * A tab of its own for this addon's modules, next to Modules, Config and the rest. It opens the current
 * theme's ordinary modules screen, so it looks like the Modules tab in whatever theme is selected, but
 * lists only the categories below; those are in turn left out of the Modules tab.
 */
public class AiCoderTab extends Tab {
    public static final Category EXPLOSIVES = new Category("Explosives");
    public static final Category MELEE = new Category("Melee");
    public static final Category UTILITY = new Category("Utility");
    public static final List<Category> CATEGORIES = List.of(EXPLOSIVES, MELEE, UTILITY);

    public enum View {
        Everything,
        Ours,
        Others
    }

    private static AiCoderTab instance;
    /** True while this tab's screen is being constructed, before it has become the current screen. */
    private static boolean creating;

    public AiCoderTab() {
        super("AiCoder");
        instance = this;
    }

    @Override
    public TabScreen createScreen(GuiTheme theme) {
        creating = true;
        try {
            TabScreen screen = theme.modulesScreen();
            ((TabScreenAccessor) screen).aicombat$setTab(this);
            return screen;
        } finally {
            creating = false;
        }
    }

    @Override
    public boolean isScreen(Screen screen) {
        return screen instanceof TabScreen tabScreen && tabScreen.tab == this;
    }

    public static boolean owns(Category category) {
        return CATEGORIES.contains(category);
    }

    /** Which categories the screen asking right now should see. */
    public static View view() {
        if (instance == null) return View.Everything;
        if (creating) return View.Ours;

        if (mc != null && mc.gui.screen() instanceof TabScreen tabScreen) {
            if (tabScreen.tab == instance) return View.Ours;
            if (tabScreen.tab instanceof ModulesTab) return View.Others;
        }
        return View.Everything;
    }
}
