package aicoder.earth.gui.widgets;

import aicoder.earth.gui.Chrome;
import aicoder.earth.gui.EarthGuiTheme;
import aicoder.earth.gui.Fx;
import aicoder.earth.gui.Style;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.widgets.WTooltip;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.renderer.text.VanillaTextRenderer;

public class WEarthTooltip extends WTooltip implements MeteorWidget {
    public WEarthTooltip(String text) {
        super(text);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        Style s = ((EarthGuiTheme) theme).style;
        double u = ((EarthGuiTheme) theme).unit();

        // Vanilla text is drawn by the game's own GUI pass, so the plate has to go through it as well
        if (TextRenderer.get() == VanillaTextRenderer.INSTANCE) {
            renderer.fill(this, Chrome.solid(s));
            return;
        }

        double corner = Math.min(Math.abs(s.fieldCorner) * u, height / 3) * Math.signum(s.fieldCorner);
        Fx.box(renderer, x, y, width, height, corner, Chrome.solid(s));
        Fx.boxOutline(renderer, x, y, width, height, corner, Fx.hair(u), Fx.a(s.accent, 0.6));
    }
}
