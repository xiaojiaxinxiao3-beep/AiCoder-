package aicoder.firstcolumns.gui.widgets;

import aicoder.firstcolumns.gui.AiCoderGuiTheme;
import aicoder.firstcolumns.gui.render.Palette;
import aicoder.firstcolumns.gui.render.Sdf;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WWindow;

/** Meteor's draggable window (settings screens, config tabs and so on) as a rounded pane of tinted glass. */
public class WAiCoderWindow extends WWindow implements MeteorWidget {
    private final double seed = Math.random();

    public WAiCoderWindow(WWidget icon, String title) {
        super(icon, title);
    }

    @Override
    protected WHeader header(WWidget icon) {
        return new WAiCoderHeader(icon);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        AiCoderGuiTheme theme = AiCoderGuiTheme.of(this.theme);
        double u = theme.u(1);

        double h = header.height + (height - header.height) * animProgress;
        double radius = Math.min(14 * u, h / 2);

        // These windows hold dense text, so their dark base is stronger than the modules window's
        Shapes.glass(theme, x, y, width, h, radius, u * 0.6, 1.6, seed);

        if (animProgress > 0) {
            Sdf.box(x + radius * 0.5, y + header.height, width - radius, Math.max(1, Math.round(u)), 0)
                .fill(Palette.alpha(Palette.DIVIDER, animProgress))
                .draw();
        }
    }

    private class WAiCoderHeader extends WHeader {
        public WAiCoderHeader(WWidget icon) {
            super(icon);
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            // The glass of the window is the background of the header
        }
    }
}
