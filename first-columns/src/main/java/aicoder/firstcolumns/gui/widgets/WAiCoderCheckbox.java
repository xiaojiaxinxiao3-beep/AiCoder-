package aicoder.firstcolumns.gui.widgets;

import aicoder.firstcolumns.gui.AiCoderGuiTheme;
import aicoder.firstcolumns.gui.render.Anim;
import aicoder.firstcolumns.gui.render.Palette;
import aicoder.firstcolumns.gui.render.Sdf;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import net.minecraft.util.Mth;

/** 18x18 rounded check box, filled green to cyan and glowing when checked. */
public class WAiCoderCheckbox extends WCheckbox implements MeteorWidget {
    private double animProgress;

    public WAiCoderCheckbox(boolean checked) {
        super(checked);
        animProgress = checked ? 1 : 0;
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        double u = AiCoderGuiTheme.of(theme).u(1);

        animProgress += (checked ? 1 : -1) * delta * 10;
        animProgress = Mth.clamp(animProgress, 0, 1);

        // Meteor's own size is kept so setting rows still line up, the box is drawn centred in it
        double s = Math.min(18 * u, Math.min(width, height));
        double bx = Math.round(x + (width - s) / 2);
        double by = Math.round(y + (height - s) / 2);
        double radius = 5 * u;
        double a = animProgress;

        if (a < 1) {
            Sdf.box(bx, by, s, s, radius)
                .fill(Palette.rgba(16, 24, 44, 0.35 * (1 - a)))
                .stroke(Math.max(1, u), Palette.alpha(mouseOver ? Palette.withAlpha(Palette.ACCENT_MID, 0.7) : Palette.CHECK_BORDER, 1 - a))
                .draw();
        }

        if (a > 0) {
            Sdf.box(bx, by, s, s, radius)
                .gradient(Palette.alpha(Palette.ACCENT_1, a), Palette.alpha(Palette.ACCENT_MID, a), 45)
                .glow((8 + Anim.pulse() * 12) * u, Palette.withAlpha(Palette.ACCENT_MID, 0.8 * a))
                .draw();

            Sdf.check(bx, by, s, Palette.alpha(Palette.CHECK_MARK, a));
        }
    }
}
