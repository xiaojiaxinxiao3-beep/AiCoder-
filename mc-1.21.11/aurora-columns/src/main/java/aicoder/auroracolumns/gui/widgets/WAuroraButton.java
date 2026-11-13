package aicoder.auroracolumns.gui.widgets;

import aicoder.auroracolumns.gui.AuroraGuiTheme;
import aicoder.auroracolumns.gui.Chrome;
import aicoder.auroracolumns.gui.Fx;
import aicoder.auroracolumns.gui.Style;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.renderer.packer.GuiTexture;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;

/** Text buttons are dark insets; icon buttons (reset, copy, paste...) are a thin ring around the icon. */
public class WAuroraButton extends WButton implements MeteorWidget {
    private double hover;

    public WAuroraButton(String text, GuiTexture texture) {
        super(text, texture);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        AuroraGuiTheme aurora = (AuroraGuiTheme) theme;
        Style s = aurora.style;
        double u = aurora.unit();
        double pad = pad();
        boolean hot = mouseOver && !theme.disableHoverColor;

        hover = Fx.step(hover, hot, delta, 8.3);

        if (text != null) {
            Chrome.field(renderer, s, x, y, width, height, u, hot, pressed);
            renderer.text(text, x + width / 2 - textWidth / 2, y + pad, Fx.mix(s.dim, s.title, pressed ? 1 : 0.55 + 0.45 * hover), false);
            return;
        }

        double radius = Math.min(width, height) / 2;
        double cx = x + width / 2, cy = y + height / 2;
        double t = Math.max(1, Math.round(1.4 * u));
        if (pressed) Fx.circle(renderer, cx, cy, radius, Fx.a(s.teal, 0.3));
        else if (hover > 0) Fx.circle(renderer, cx, cy, radius, Fx.rgba(255, 255, 255, 0.1 * hover));
        Fx.ring(renderer, cx, cy, radius - t / 2, t, Fx.mix(Fx.rgba(255, 255, 255, 0.3), s.green, pressed ? 1 : hover * 0.8));

        double ts = theme.textHeight();
        renderer.quad(x + width / 2 - ts / 2, y + pad, ts, ts, texture, Fx.mix(s.dim, s.title, pressed ? 1 : hover));
    }
}
