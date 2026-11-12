package aicoder.client.gui.widgets;

import aicoder.client.gui.AiCoderGuiTheme;
import aicoder.client.gui.Cyber;
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

/**
 * Tab strip: a slanted translucent bar with uppercase, letter-spaced tab names.
 */
public class WCyberTopBar extends WTopBar implements MeteorWidget {
    // Design pixels
    private static final double TOP = 16;
    private static final double HEIGHT = 26;
    private static final double SLANT = 10;
    private static final double TAB_PAD = 12;
    private static final double FONT = 10.5;
    private static final double TRACKING = 2.2;

    public WCyberTopBar() {
        spacing = 3;
    }

    private double unit() {
        return ((AiCoderGuiTheme) theme).unit();
    }

    @Override
    public void init() {
        // Cell paddings are in Meteor units, which are 1.5 design pixels
        List<Tab> tabs = Tabs.get();
        for (int i = 0; i < tabs.size(); i++) {
            Cell<?> cell = add(new WTab(tabs.get(i))).padTop(TOP * 1.5);
            if (i == 0) cell.padLeft(SLANT * 1.5);
            if (i == tabs.size() - 1) cell.padRight(SLANT * 1.5);
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
        double u = unit();

        ((AiCoderGuiTheme) theme).backdrop(renderer);

        double by = y + TOP * u;
        double bh = height - TOP * u;
        Cyber.occlude(x, by, width, bh);
        double slant = SLANT * u;
        Color bg = Cyber.rgba(6, 16, 30, 0.62);

        renderer.quad(x + slant, by, width - slant * 2, bh, bg);
        Cyber.tri(renderer, x, by + bh, x + slant, by, x + slant, by + bh, bg);
        Cyber.tri(renderer, x + width - slant, by, x + width, by, x + width - slant, by + bh, bg);
        renderer.quad(x + slant, by, width - slant, Cyber.hair(u), Cyber.a(Cyber.CYAN_LINE, 0.35));
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
            double u = unit();
            width = Cyber.textWidth(theme, label, FONT, TRACKING, u) + TAB_PAD * 2 * u;
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
            double u = unit();
            boolean selected = mc.gui.screen() instanceof TabScreen tabScreen && tabScreen.tab == tab;

            Cyber.TextBatch text = Cyber.text(renderer, u);
            double tx = x + TAB_PAD * u;
            double ty = Cyber.centerY(theme, y + height / 2, FONT, u);

            if (selected) {
                text.glow(label, tx, ty, FONT, Cyber.WHITE, TRACKING, Cyber.BOLD, Cyber.CYAN_LINE, 1.5);

                double lx = x + 10 * u;
                double lw = width - 20 * u;
                double ly = y + height - 4 * u;
                Color accent = theme().accentColor.get();
                Cyber.glowRect(renderer, lx, ly, lw, 2 * u, 5 * u, Cyber.a(accent, 0.35));
                renderer.quad(lx, ly, lw, 2 * u, accent);
            } else {
                text.put(label, tx, ty, FONT, mouseOver || pressed ? Cyber.TEXT : Cyber.TEXT_IDLE, TRACKING, Cyber.BOLD);
            }
        }
    }
}
