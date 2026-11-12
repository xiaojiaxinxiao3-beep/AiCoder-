package aicoder.glass.gui.widgets;

import aicoder.glass.gui.Chrome;
import aicoder.glass.gui.GlassGuiTheme;
import aicoder.glass.gui.Fx;
import aicoder.glass.gui.Style;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.tabs.Tab;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.utils.Cell;
import meteordevelopment.meteorclient.gui.widgets.WTopBar;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPressable;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.gui.screen.Screen;
import static org.lwjgl.glfw.GLFW.glfwSetCursorPos;

import java.util.List;
import java.util.Locale;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/** Tab strip: a pill of dark glass; the open tab sits in a lit capsule. */
public class WGlassTopBar extends WTopBar implements MeteorWidget {
    // Design pixels
    private static final double TOP = 16;
    private static final double HEIGHT = 26;
    private static final double SIDE = 8;
    private static final double TAB_PAD = 12;
    private static final double FONT = 10.5;
    private static final double TRACKING = 2.2;

    public WGlassTopBar() {
        spacing = 3;
    }

    private GlassGuiTheme glass() {
        return (GlassGuiTheme) theme;
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
        GlassGuiTheme glass = glass();
        Style s = glass.style;
        double u = glass.unit();

        glass.backdrop(renderer);

        double by = y + TOP * u;
        double bh = height - TOP * u;
        double corner = bh / 2;
        Fx.occlude(x, by, width, bh);

        // A pill of dark glass
        Fx.glowRect(renderer, x + corner, by + 4 * u, width - corner * 2, bh - 4 * u, 12 * u, Fx.rgba(0, 0, 10, 0.2));
        Fx.box(renderer, x, by, width, bh, corner, Fx.rgba(18, 24, 56, 0.42), Fx.rgba(8, 10, 30, 0.5));
        Fx.box(renderer, x + u, by + u, width - 2 * u, bh * 0.5, corner - u, Fx.rgba(255, 255, 255, 0.14), Fx.rgba(255, 255, 255, 0));
        Fx.boxOutline(renderer, x, by, width, bh, corner, Fx.hair(u), Fx.rgba(255, 255, 255, 0.5), Fx.rgba(255, 255, 255, 0.12));
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
            double u = glass().unit();
            width = Fx.textWidth(theme, label, FONT, TRACKING, u) + TAB_PAD * 2 * u;
            height = HEIGHT * u;
        }

        @Override
        protected void onPressed(int button) {
            Screen screen = mc.currentScreen;

            if (!(screen instanceof TabScreen tabScreen) || tabScreen.tab != tab) {
                double mouseX = mc.mouse.getX();
                double mouseY = mc.mouse.getY();

                tab.openScreen(theme);
                glfwSetCursorPos(mc.getWindow().getHandle(), mouseX, mouseY);
            }
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            Style s = glass().style;
            double u = glass().unit();
            boolean selected = mc.currentScreen instanceof TabScreen tabScreen && tabScreen.tab == tab;

            hover = Math.max(0, Math.min(1, hover + delta * 8 * (mouseOver ? 1 : -1)));

            if (selected || hover > 0) {
                double inset = 3 * u;
                Chrome.plate(renderer, s, x, y + inset, width, height - inset * 2, u, selected ? 1 : 0, selected ? 0 : hover);
            }

            Color c = selected ? s.title : (mouseOver || pressed ? s.text : s.dim);
            Fx.text(renderer, u).put(label, x + TAB_PAD * u, Fx.centerY(theme, y + height / 2, FONT, u), FONT, c, TRACKING, Fx.BOLD);
        }
    }
}
