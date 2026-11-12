package aicoder.earth.gui.widgets;

import aicoder.earth.gui.EarthGuiTheme;
import aicoder.earth.gui.Toggles;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import net.minecraft.util.Mth;

/** The theme's switch glyph at checkbox size. Keeps Meteor's footprint so setting rows still align. */
public class WEarthCheckbox extends WCheckbox implements MeteorWidget {
    private double animProgress;
    private double hover;

    public WEarthCheckbox(boolean checked) {
        super(checked);
        animProgress = checked ? 1 : 0;
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        EarthGuiTheme earth = (EarthGuiTheme) theme;

        animProgress = Mth.clamp(animProgress + (checked ? 1 : -1) * delta * 12, 0, 1);
        hover = Mth.clamp(hover + (mouseOver ? 1 : -1) * delta * 8, 0, 1);

        double radius = Math.min(7.5 * earth.unit(), Math.min(width, height) / 2 - 1);
        Toggles.draw(renderer, earth.style, Math.round(x + width / 2), Math.round(y + height / 2), radius, animProgress, hover, 0);
    }
}
