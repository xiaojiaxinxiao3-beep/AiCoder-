package aicoder.client.gui.widgets;

import aicoder.client.gui.AiCoderGuiTheme;
import aicoder.client.gui.Cyber;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WWindow;

/** Glass window with cut corners, used by every screen except the modules screen (which draws its own frame). */
public class WCyberWindow extends WWindow implements MeteorWidget {
    public WCyberWindow(WWidget icon, String title) {
        super(icon, title);
    }

    private double unit() {
        return ((AiCoderGuiTheme) theme).unit();
    }

    @Override
    public void init() {
        super.init();

        // The tab strip is taller than Meteor's, so tall windows need a little more headroom
        view.maxHeight -= theme.scale(64);
    }

    @Override
    protected WHeader header(WWidget icon) {
        return new WCyberHeader(icon);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        ((AiCoderGuiTheme) theme).backdrop(renderer);

        boolean open = expanded || animProgress > 0;
        double h = open ? height : header.height;

        // While the window is folding it is scissored to its own bounds, which would crop the brackets
        boolean settled = animProgress == 0 || animProgress == 1;
        Cyber.panel(renderer, x, y, width, h, unit(), theme().backgroundColor.get(), theme().accentColor.get(), settled);
    }

    private class WCyberHeader extends WHeader {
        private WCyberHeader(WWidget icon) {
            super(icon);
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            if (!expanded && animProgress == 0) return;

            double u = unit();
            Cyber.headerRule(renderer, x, y + height - Cyber.hair(u), width, 16 * u, 8 * u, u);
        }
    }
}
