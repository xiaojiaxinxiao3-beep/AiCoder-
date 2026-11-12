package aicoder.glass.gui.widgets;

import aicoder.glass.gui.GlassGuiTheme;
import aicoder.glass.gui.Fx;
import aicoder.glass.gui.Style;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.widgets.WTooltip;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.renderer.text.VanillaTextRenderer;

public class WGlassTooltip extends WTooltip implements MeteorWidget {
    public WGlassTooltip(String text) {
        super(text);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        Style s = ((GlassGuiTheme) theme).style;
        double u = ((GlassGuiTheme) theme).unit();

        // Vanilla text is drawn by the game's own GUI pass, so the plate has to go through it as well
        if (TextRenderer.get() == VanillaTextRenderer.INSTANCE) {
            renderer.quad(this, s.solid);
            return;
        }

        double corner = Math.min(s.fieldCorner * u, height / 3);
        Fx.box(renderer, x, y, width, height, corner, s.solid);
        Fx.boxOutline(renderer, x, y, width, height, corner, Fx.hair(u), Fx.rgba(255, 255, 255, 0.55), Fx.rgba(255, 255, 255, 0.15));
    }
}
