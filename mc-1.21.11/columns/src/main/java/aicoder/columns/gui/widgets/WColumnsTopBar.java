package aicoder.columns.gui.widgets;

import aicoder.columns.gui.AiCoderGuiTheme;
import aicoder.columns.gui.Cyber;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.tabs.Tab;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.utils.Cell;
import meteordevelopment.meteorclient.gui.widgets.WTopBar;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPressable;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;
import static org.lwjgl.glfw.GLFW.glfwSetCursorPos;

import java.util.List;
import java.util.Locale;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static meteordevelopment.meteorclient.utils.Utils.getWindowWidth;

/**
 * Tab strip, moved to the top-right corner. Every tab screen has one, so it also draws the
 * {@code AICODER} label in the top-left corner.
 */
public class WColumnsTopBar extends WTopBar implements MeteorWidget {
    // Design pixels
    private static final double TOP = 10;
    private static final double RIGHT = 30;
    private static final double HEIGHT = 24;
    private static final double SLANT = 10;
    private static final double TAB_PAD = 11;
    private static final double FONT = 10;
    private static final double TRACKING = 2;

    private static final String KANA_TAGLINE = "サイバー・システム";

    private final String version;

    public WColumnsTopBar() {
        spacing = 3;

        String v = FabricLoader.getInstance().getModContainer("aicoder-columns")
            .map(mod -> mod.getMetadata().getVersion().getFriendlyString())
            .orElse("?");
        String[] parts = v.split("\\.");
        version = "v" + (parts.length >= 2 ? parts[0] + "." + parts[1] : v) + " // CLIENT";
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
    protected void onCalculateWidgetPositions() {
        // Meteor centers the strip; the design keeps it against the right edge
        x = Math.round(Math.max(0, getWindowWidth() - width - RIGHT * unit()));
        y = 0;
        super.onCalculateWidgetPositions();
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
        double slant = SLANT * u;
        Color bg = Cyber.rgba(20, 6, 30, 0.6);
        Cyber.occlude(x, by, width, bh);

        renderer.quad(x + slant, by, width - slant * 2, bh, bg);
        Cyber.tri(renderer, x, by + bh, x + slant, by, x + slant, by + bh, bg);
        Cyber.tri(renderer, x + width - slant, by, x + width, by, x + width - slant, by + bh, bg);
        renderer.quad(x + slant, by, width - slant, Cyber.hair(u), Cyber.a(Cyber.CYAN, 0.35));

        renderLabel(renderer, u);
    }

    /** {@code AICODER  v0.3 // CLIENT  サイバー・システム} in the top-left corner, with the logo's glitch bursts. */
    private void renderLabel(GuiRenderer renderer, double u) {
        Color accent = theme().accentColor.get();
        double p = Cyber.phase();
        double cy = (TOP + HEIGHT / 2) * u;
        double lx = 18 * u;

        // Leave the label out when the tab strip needs the room
        String logo = "AICODER";
        double logoW = Cyber.textWidth(theme, logo, 17, 5, u);
        double verW = Cyber.textWidth(theme, version, 10, 2, u);
        if (lx + logoW + 10 * u + verW > x - 12 * u) return;

        double logoY = Cyber.centerY(theme, cy, 17, u);
        double logoH = Cyber.lineHeight(theme, 17, u);

        double glitch = 0;
        for (double burst : new double[]{0.12, 0.47, 0.81}) {
            double d = Math.abs(p - burst);
            if (d < 0.025) glitch = 1 - d / 0.025;
        }
        boolean dimmed = glitch > 0.6 && frac(p * 331) < 0.5;

        Cyber.TextBatch text = Cyber.text(renderer, u);
        text.glow(logo, lx, logoY, 17, Cyber.a(Cyber.WHITE, dimmed ? 0.55 : 1), 5, Cyber.BOLD, Cyber.CYAN_LINE, 2);

        if (glitch > 0) {
            double jitter = Math.sin(p * 997) * glitch;
            double c1 = frac(p * 7919) * 0.60;
            double c2 = frac(p * 104729) * 0.40;
            slice(renderer, logo, lx + jitter * 5 * u, logoY, lx, logoW, logoH, c1, c2, accent, u);
            slice(renderer, logo, lx - jitter * 4 * u, logoY, lx, logoW, logoH, c2, c1, Cyber.CYAN, u);
        }

        double tx = lx + logoW + 10 * u - 5 * u;
        double subY = Cyber.centerY(theme, cy + 2 * u, 10, u);
        text.put(version, tx, subY, 10, accent, 2, Cyber.BOLD);
        tx += verW + 10 * u;

        if (Cyber.vanillaText() && tx + Cyber.textWidth(theme, KANA_TAGLINE, 10, 3, u) < x - 12 * u) {
            text.put(KANA_TAGLINE, tx, subY, 10, Cyber.a(Cyber.CYAN, 0.7), 3, Cyber.BOLD);
        }
    }

    private static double frac(double v) {
        return v - Math.floor(v);
    }

    /** One color layer of the glitch: the logo shifted sideways, showing only a horizontal band. */
    private void slice(GuiRenderer renderer, String logo, double tx, double ty, double baseX, double w, double h,
                       double insetTop, double insetBottom, Color color, double u) {
        double bandY = ty + h * insetTop;
        double bandH = h * (1 - insetTop - insetBottom);
        if (bandH < 1) return;

        renderer.scissorStart(baseX - 12 * u, bandY, w + 24 * u, bandH);
        Cyber.text(renderer, u).put(logo, tx, ty, 17, color, 5, Cyber.BOLD);
        renderer.scissorEnd();
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
            double u = unit();
            boolean selected = mc.currentScreen instanceof TabScreen tabScreen && tabScreen.tab == tab;

            Color c = selected ? Cyber.WHITE : (mouseOver || pressed ? Cyber.TEXT : Cyber.TEXT_IDLE);
            Cyber.text(renderer, u).put(label, x + TAB_PAD * u, Cyber.centerY(theme, y + height / 2, FONT, u), FONT, c, TRACKING, Cyber.BOLD);

            if (selected) {
                double lx = x + 9 * u;
                double lw = width - 18 * u;
                double ly = y + height - 4 * u;
                Color accent = theme().accentColor.get();
                Cyber.glowRect(renderer, lx, ly, lw, 2 * u, 5 * u, Cyber.a(accent, 0.35));
                renderer.quad(lx, ly, lw, 2 * u, accent);
            }
        }
    }
}
