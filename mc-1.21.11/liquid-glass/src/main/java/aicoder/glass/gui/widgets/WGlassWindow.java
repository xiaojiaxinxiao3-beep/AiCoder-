package aicoder.glass.gui.widgets;

import aicoder.glass.gui.Chrome;
import aicoder.glass.gui.GlassGuiTheme;
import aicoder.glass.gui.Fx;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WWindow;

/** Window used by every screen except the modules screen, which draws its own frame. */
public class WGlassWindow extends WWindow implements MeteorWidget {
    public WGlassWindow(WWidget icon, String title) {
        super(icon, title);
    }

    private GlassGuiTheme glass() {
        return (GlassGuiTheme) theme;
    }

    @Override
    public void init() {
        super.init();

        // The tab strip is taller than Meteor's, so tall windows need a little more headroom
        view.maxHeight -= theme.scale(64);
    }

    @Override
    protected WHeader header(WWidget icon) {
        return new WGlassHeader(icon);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        GlassGuiTheme glass = glass();
        glass.backdrop(renderer);

        boolean open = expanded || animProgress > 0;
        Chrome.panel(renderer, glass.style, x, y, width, open ? height : header.height, glass.unit(), open && glass.liquid.get());
    }

    private class WGlassHeader extends WHeader {
        private WGlassHeader(WWidget icon) {
            super(icon);
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            if (!expanded && animProgress == 0) return;

            double u = glass().unit();
            double t = Fx.hair(u);
            renderer.quad(x + 3 * u, y + height - t, width - 6 * u, t, Fx.rgba(255, 255, 255, 0.16));
        }
    }
}
