package aicoder.client.gui.widgets;

import aicoder.client.gui.AiCoderGuiTheme;
import aicoder.client.gui.Cyber;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.widgets.WTooltip;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.renderer.text.VanillaTextRenderer;

public class WCyberTooltip extends WTooltip implements MeteorWidget {
    public WCyberTooltip(String text) {
        super(text);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        double u = ((AiCoderGuiTheme) theme).unit();
        double cut = Math.min(5 * u, height / 3);

        // Vanilla text is drawn by the game's own GUI pass, so the plate has to go through it as well
        if (TextRenderer.get() == VanillaTextRenderer.INSTANCE) {
            renderer.fill(this, Cyber.rgba(8, 6, 24, 0.94));
            return;
        }

        // Opaque enough to read over the world without the Blur module
        Cyber.box(renderer, x, y, width, height, cut, 0, cut, 0, Cyber.hair(u),
            Cyber.rgba(8, 6, 24, 0.94), Cyber.a(Cyber.CYAN_LINE, 0.45));
    }
}
