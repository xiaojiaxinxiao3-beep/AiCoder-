package aicoder.auroracolumns.gui.widgets;

import aicoder.auroracolumns.gui.AuroraGuiTheme;
import aicoder.auroracolumns.gui.Chrome;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.widgets.WTooltip;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.renderer.text.VanillaTextRenderer;

/** The one opaque surface: dark enough that nothing underneath shows through, with the same rim as the glass. */
public class WAuroraTooltip extends WTooltip implements MeteorWidget {
    public WAuroraTooltip(String text) {
        super(text);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        AuroraGuiTheme aurora = (AuroraGuiTheme) theme;

        // Vanilla text is drawn by the game's own GUI pass, so the plate has to go through it as well
        if (TextRenderer.get() == VanillaTextRenderer.INSTANCE) {
            renderer.quad(this, aurora.style.solid);
            return;
        }

        Chrome.panel(renderer, x, y, width, height, aurora.unit(), Math.min(12 * aurora.unit(), height / 3), true);
    }
}
