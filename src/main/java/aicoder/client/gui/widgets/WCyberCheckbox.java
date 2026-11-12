package aicoder.client.gui.widgets;

import aicoder.client.gui.AiCoderGuiTheme;
import aicoder.client.gui.Cyber;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.util.Mth;

/** 14px hairline box; checked fills it with a glowing magenta square. Keeps Meteor's footprint so rows still align. */
public class WCyberCheckbox extends WCheckbox implements MeteorWidget {
    private double animProgress;

    public WCyberCheckbox(boolean checked) {
        super(checked);
        animProgress = checked ? 1 : 0;
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        double u = ((AiCoderGuiTheme) theme).unit();

        animProgress += (checked ? 1 : -1) * delta * 14;
        animProgress = Mth.clamp(animProgress, 0, 1);

        double s = Math.round(Math.min(14 * u, Math.min(width, height)));
        double bx = Math.round(x + (width - s) / 2);
        double by = Math.round(y + (height - s) / 2);

        Color outline = pressed ? theme().accentColor.get() : Cyber.a(Cyber.CYAN_LINE, mouseOver ? 0.85 : 0.45);
        Cyber.box(renderer, bx, by, s, s, 0, 0, 0, 0, Cyber.hair(u), Cyber.a(Cyber.FIELD, 0.35), outline);

        if (animProgress > 0) {
            Color fill = theme().checkboxColor.get();
            double is = (s - 6 * u) * animProgress;
            double ix = bx + (s - is) / 2;
            double iy = by + (s - is) / 2;

            Cyber.glowRect(renderer, ix, iy, is, is, 5 * u, Cyber.a(fill, 0.4 * animProgress));
            renderer.quad(ix, iy, is, is, fill);
        }
    }
}
