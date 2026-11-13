package aicoder.auroracolumns.gui.widgets;

import aicoder.auroracolumns.gui.Fx;
import aicoder.auroracolumns.gui.AuroraGuiTheme;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.widgets.pressable.WTriangle;

/** Small collapse caret: points down when open, turns sideways as the window folds. */
public class WAuroraTriangle extends WTriangle implements MeteorWidget {
    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        double u = ((AuroraGuiTheme) theme).unit();
        double cx = x + width / 2, cy = y + height / 2;
        double a = 5 * u;
        double rad = Math.toRadians(rotation);
        double cos = Math.cos(rad), sin = Math.sin(rad);

        // Down-pointing triangle, rotated about its centre
        double[][] p = {{-a, -a * 0.55}, {a, -a * 0.55}, {0, a * 0.75}};
        double[] px = new double[3], py = new double[3];
        for (int i = 0; i < 3; i++) {
            px[i] = cx + p[i][0] * cos - p[i][1] * sin;
            py[i] = cy + p[i][0] * sin + p[i][1] * cos;
        }

        Fx.tri(renderer, px[0], py[0], px[1], py[1], px[2], py[2], Fx.rgba(255, 255, 255, mouseOver ? 1 : 0.7));
    }
}
