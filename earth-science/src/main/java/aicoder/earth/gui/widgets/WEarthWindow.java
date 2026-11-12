package aicoder.earth.gui.widgets;

import aicoder.earth.gui.Chrome;
import aicoder.earth.gui.EarthGuiTheme;
import aicoder.earth.gui.Fx;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WWindow;

/** Window used by every screen except the modules screen, which draws its own frame. */
public class WEarthWindow extends WWindow implements MeteorWidget {
    public WEarthWindow(WWidget icon, String title) {
        super(icon, title);
    }

    private EarthGuiTheme earth() {
        return (EarthGuiTheme) theme;
    }

    @Override
    public void init() {
        super.init();

        // The tab strip is taller than Meteor's, so tall windows need a little more headroom
        view.maxHeight -= theme.scale(64);
    }

    @Override
    protected WHeader header(WWidget icon) {
        return new WEarthHeader(icon);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        EarthGuiTheme earth = earth();
        earth.backdrop(renderer);

        boolean open = expanded || animProgress > 0;
        Chrome.panel(renderer, earth.style, x, y, width, open ? height : header.height, earth.unit());
    }

    private class WEarthHeader extends WHeader {
        private WEarthHeader(WWidget icon) {
            super(icon);
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            if (!expanded && animProgress == 0) return;

            double u = earth().unit();
            double t = Fx.hair(u);
            renderer.quad(x + t, y + height - t, width - t * 2, t, Fx.a(earth().style.outline, 0.7));
        }
    }
}
