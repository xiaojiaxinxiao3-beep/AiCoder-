package aicoder.columns.gui.widgets;

import aicoder.columns.gui.AiCoderGuiTheme;
import aicoder.columns.gui.Cyber;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.renderer.packer.GuiTexture;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.utils.render.color.Color;

public class WCyberButton extends WButton implements MeteorWidget {
    private static final Color ICON = new Color(111, 191, 220);

    public WCyberButton(String text, GuiTexture texture) {
        super(text, texture);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        double u = ((AiCoderGuiTheme) theme).unit();
        double pad = pad();
        boolean hot = mouseOver && !theme.disableHoverColor;

        Color outline = pressed ? theme().accentColor.get() : Cyber.a(Cyber.CYAN_LINE, hot ? 0.7 : 0.22);
        Color fill = pressed ? Cyber.a(theme().accentColor.get(), 0.22) : Cyber.a(Cyber.FIELD, hot ? 0.6 : 0.35);
        double cut = text != null ? Math.min(5 * u, height / 3) : 0;
        Cyber.box(renderer, x, y, width, height, 0, 0, cut, 0, Cyber.hair(u), fill, outline);

        if (text != null) {
            renderer.text(text, x + width / 2 - textWidth / 2, y + pad, hot || pressed ? Cyber.WHITE : theme().textColor.get(), false);
        } else {
            double ts = theme.textHeight();
            renderer.quad(x + width / 2 - ts / 2, y + pad, ts, ts, texture, hot || pressed ? Cyber.CYAN : ICON);
        }
    }
}
