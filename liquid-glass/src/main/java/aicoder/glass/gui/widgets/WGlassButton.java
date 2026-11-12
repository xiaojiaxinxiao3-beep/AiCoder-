package aicoder.glass.gui.widgets;

import aicoder.glass.gui.Chrome;
import aicoder.glass.gui.GlassGuiTheme;
import aicoder.glass.gui.Style;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.renderer.packer.GuiTexture;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;

public class WGlassButton extends WButton implements MeteorWidget {
    public WGlassButton(String text, GuiTexture texture) {
        super(text, texture);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        GlassGuiTheme glass = (GlassGuiTheme) theme;
        Style s = glass.style;
        double pad = pad();
        boolean hot = mouseOver && !theme.disableHoverColor;

        Chrome.field(renderer, s, x, y, width, height, glass.unit(), hot, pressed);

        if (text != null) {
            renderer.text(text, x + width / 2 - textWidth / 2, y + pad, hot || pressed ? s.title : theme().textColor.get(), false);
        } else {
            double ts = theme.textHeight();
            renderer.quad(x + width / 2 - ts / 2, y + pad, ts, ts, texture, hot || pressed ? s.accent : s.dim);
        }
    }
}
