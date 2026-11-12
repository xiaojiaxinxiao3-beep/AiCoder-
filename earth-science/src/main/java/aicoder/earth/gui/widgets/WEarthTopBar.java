package aicoder.earth.gui.widgets;

import aicoder.earth.gui.EarthGuiTheme;
import aicoder.earth.gui.Fx;
import aicoder.earth.gui.Style;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.tabs.Tab;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.utils.Cell;
import meteordevelopment.meteorclient.gui.widgets.WTopBar;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPressable;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.sdl.SDLMouse;

import java.util.List;
import java.util.Locale;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/** Tab strip: an outlined box in the theme's corner style with uppercase, letter-spaced tab names. */
public class WEarthTopBar extends WTopBar implements MeteorWidget {
    // Design pixels
    private static final double TOP = 16;
    private static final double HEIGHT = 26;
    private static final double SIDE = 10;
    private static final double TAB_PAD = 12;
    private static final double FONT = 10.5;
    private static final double TRACKING = 2.2;

    public WEarthTopBar() {
        spacing = 3;
    }

    private EarthGuiTheme earth() {
        return (EarthGuiTheme) theme;
    }

    @Override
    public void init() {
        // Cell paddings are in Meteor units, which are 1.5 design pixels
        List<Tab> tabs = Tabs.get();
        for (int i = 0; i < tabs.size(); i++) {
            Cell<?> cell = add(new WTab(tabs.get(i))).padTop(TOP * 1.5);
            if (i == 0) cell.padLeft(SIDE * 1.5);
            if (i == tabs.size() - 1) cell.padRight(SIDE * 1.5);
        }
    }

    @Override
    protected Color getButtonColor(boolean pressed, boolean hovered) {
        return theme().backgroundColor.get(pressed, hovered);
    }

    @Override
    protected Color getNameColor() {
        return theme().textColor.get();
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        EarthGuiTheme earth = earth();
        Style s = earth.style;
        double u = earth.unit();

        earth.backdrop(renderer);

        double by = y + TOP * u;
        double bh = height - TOP * u;
        double corner = Math.min(Math.abs(s.corner), 5) * u * Math.signum(s.corner);
        Fx.occlude(x, by, width, bh);

        Fx.box(renderer, x, by, width, bh, corner, s.bar);
        Fx.boxOutline(renderer, x, by, width, bh, corner, Fx.hair(u), s.barLine);
    }

    private class WTab extends WPressable {
        private final Tab tab;
        private final String label;

        private WTab(Tab tab) {
            this.tab = tab;
            this.label = tab.name.toUpperCase(Locale.ROOT);
        }

        @Override
        protected void onCalculateSize() {
            double u = earth().unit();
            width = Fx.textWidth(theme, label, FONT, TRACKING, u) + TAB_PAD * 2 * u;
            height = HEIGHT * u;
        }

        @Override
        protected void onPressed(int button) {
            Screen screen = mc.gui.screen();

            if (!(screen instanceof TabScreen tabScreen) || tabScreen.tab != tab) {
                double mouseX = mc.mouseHandler.xpos();
                double mouseY = mc.mouseHandler.ypos();

                tab.openScreen(theme);
                SDLMouse.SDL_WarpMouseInWindow(mc.getWindow().handle(), (float) mouseX, (float) mouseY);
            }
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            Style s = earth().style;
            double u = earth().unit();
            boolean selected = mc.gui.screen() instanceof TabScreen tabScreen && tabScreen.tab == tab;

            Color c = selected ? s.title : (mouseOver || pressed ? s.text : s.dim);
            Fx.text(renderer, u).put(label, x + TAB_PAD * u, Fx.centerY(theme, y + height / 2, FONT, u), FONT, c, TRACKING, Fx.BOLD);

            if (selected) {
                double lx = x + 10 * u;
                double lw = width - 20 * u;
                double ly = y + height - 4 * u;
                if (!s.light) Fx.glowRect(renderer, lx, ly, lw, 2 * u, 4 * u, Fx.a(s.accent, 0.3));
                renderer.quad(lx, ly, lw, 2 * u, s.accent);
            }
        }
    }
}
