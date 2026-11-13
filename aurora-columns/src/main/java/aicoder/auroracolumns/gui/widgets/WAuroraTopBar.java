package aicoder.auroracolumns.gui.widgets;

import aicoder.auroracolumns.gui.AuroraGuiTheme;
import aicoder.auroracolumns.gui.Chrome;
import aicoder.auroracolumns.gui.Fx;
import aicoder.auroracolumns.gui.Style;
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
import static meteordevelopment.meteorclient.utils.Utils.getWindowWidth;

/**
 * Tab strip: a capsule of glass with one capsule button per tab from {@link Tabs#get()}, so tabs added by
 * other addons show up too. The open tab is filled with the aurora gradient.
 */
public class WAuroraTopBar extends WTopBar implements MeteorWidget {
    // Design pixels
    private static final double TOP = 22;
    private static final double HEIGHT = 46;
    private static final double INSET = 6;
    private static final double TAB_PAD = 20;
    private static final double FONT = 13.5;
    private static final double TRACKING = 2.6;
    private static final double RIGHT = 34;

    public WAuroraTopBar() {
        spacing = 0;
    }

    private AuroraGuiTheme aurora() {
        return (AuroraGuiTheme) theme;
    }

    @Override
    public void init() {
        AuroraGuiTheme aurora = aurora();

        List<Tab> tabs = Tabs.get();
        for (int i = 0; i < tabs.size(); i++) {
            Cell<?> cell = add(new WTab(tabs.get(i))).padTop(aurora.mu(TOP));
            if (i == 0) cell.padLeft(aurora.mu(INSET));
            if (i == tabs.size() - 1) cell.padRight(aurora.mu(INSET));
        }
    }

    @Override
    public void calculateWidgetPositions() {
        // The column layout keeps the strip in the top-right corner, leaving the left for the logo
        if (aurora().topBarRight()) x = Math.max(0, getWindowWidth() - width - Math.round(RIGHT * aurora().unit()));

        super.calculateWidgetPositions();
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
        AuroraGuiTheme aurora = aurora();
        double u = aurora.unit();

        aurora.backdrop(renderer);

        double by = y + TOP * u;
        double bh = height - TOP * u;
        Chrome.panel(renderer, x, by, width, bh, u, bh / 2, false);

        if (aurora.topBarRight()) renderLogo(renderer, aurora.style, u, by + bh / 2);
    }

    /** {@code AICODER client} in the top-left corner, with a line of small print under it. */
    private void renderLogo(GuiRenderer renderer, Style s, double u, double cy) {
        String word = "AICODER";
        double size = 25, tracking = 6;
        double lx = 34 * u;

        double total = Fx.textWidth(theme, word + " client", size, tracking, u);
        if (lx + total > x - 16 * u) return;

        double ty = Fx.centerY(theme, cy - 8 * u, size, u);
        Fx.TextBatch text = Fx.text(renderer, u);

        for (int i = 0; i < word.length(); i++) {
            String c = String.valueOf(word.charAt(i));
            double f = (double) i / (word.length() - 1);
            Color color = f < 0.5 ? Fx.mix(s.green, s.teal, f * 2) : Fx.mix(s.teal, s.violet, f * 2 - 1);
            text.put(c, lx, ty, size, color, 0, Fx.BOLD);
            lx += Fx.textWidth(theme, c, size, tracking, u);
        }
        text.put(" client", lx, ty, size, s.title, tracking, Fx.BOLD);
        text.put("AURORA GLASS · COLUMNS", 34 * u, ty + Fx.lineHeight(theme, size, u) + 4 * u, 11, s.faint, 2.4, 0);
    }

    private class WTab extends WPressable {
        private final Tab tab;
        private final String label;
        private double hover;

        private WTab(Tab tab) {
            this.tab = tab;
            this.label = tab.name.toUpperCase(Locale.ROOT);
        }

        @Override
        protected void onCalculateSize() {
            double u = aurora().unit();
            width = Math.round(Fx.textWidth(theme, label, FONT, TRACKING, u) + TAB_PAD * 2 * u);
            height = Math.round(HEIGHT * u);
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
            Style s = aurora().style;
            double u = aurora().unit();
            boolean selected = mc.gui.screen() instanceof TabScreen tabScreen && tabScreen.tab == tab;

            hover = Fx.step(hover, mouseOver, delta, 8.3);

            double inset = INSET * u;
            double cx = x + 2 * u, cw = width - 4 * u, cy = y + inset, ch = height - inset * 2;
            if (selected) {
                Fx.glowRect(renderer, cx + ch / 2, cy + ch / 2, cw - ch, 0, ch / 2 + 10 * u, Fx.a(s.green, 0.3));
                Chrome.gradient(renderer, cx, cy, cw, ch, ch / 2, 1, 1, 1, 159, 208, 255);
                renderer.quad(cx + ch / 2, cy + Fx.hair(u), cw - ch, Fx.hair(u), Fx.rgba(255, 255, 255, 0.6));
            } else if (hover > 0) {
                Chrome.plate(renderer, cx, cy, cw, ch, u, ch / 2, hover);
            }

            Color c = selected ? s.dark : Fx.mix(s.dim, s.title, hover);
            Fx.text(renderer, u).put(label, x + TAB_PAD * u + TRACKING * u / 2, Fx.centerY(theme, y + height / 2, FONT, u), FONT, c, TRACKING, selected ? Fx.BOLD : 0);
        }
    }
}
