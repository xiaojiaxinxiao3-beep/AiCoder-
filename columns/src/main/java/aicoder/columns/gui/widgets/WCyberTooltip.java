package aicoder.columns.gui.widgets;

import aicoder.columns.gui.AiCoderGuiTheme;
import aicoder.columns.gui.Cyber;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.widgets.WTooltip;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.renderer.text.VanillaTextRenderer;

/** Plain tooltip for everything except module rows, which draw their own richer one. */
public class WCyberTooltip extends WTooltip implements MeteorWidget {
    public WCyberTooltip(String text) {
        super(text);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        double u = ((AiCoderGuiTheme) theme).unit();

        // Vanilla text is drawn by the game's own GUI pass, so the plate has to go through it as well
        if (TextRenderer.get() == VanillaTextRenderer.INSTANCE) {
            renderer.fill(this, Cyber.rgba(12, 6, 26, 0.97));
            return;
        }

        tip(renderer, x, y, width, height, u, theme().accentColor.get());
    }

    /** Opaque plate with a cut top-left corner, a magenta hairline and a faint magenta halo. */
    static void tip(GuiRenderer renderer, double x, double y, double w, double h, double u, meteordevelopment.meteorclient.utils.render.color.Color accent) {
        double cut = Math.min(8 * u, h / 3);
        Cyber.glowRect(renderer, x, y, w, h, 12 * u, Cyber.a(accent, 0.16));
        Cyber.box(renderer, x, y, w, h, cut, 0, 0, 0, Cyber.hair(u), Cyber.rgba(12, 6, 26, 0.97), Cyber.a(accent, 0.6));
    }
}
