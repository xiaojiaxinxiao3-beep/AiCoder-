package aicoder.earth.gui.widgets;

import aicoder.earth.gui.Chrome;
import aicoder.earth.gui.EarthGuiTheme;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.widgets.input.WMeteorTextBox;
import meteordevelopment.meteorclient.gui.utils.CharFilter;
import meteordevelopment.meteorclient.gui.widgets.WWidget;

/** Meteor's text box with the theme's value field drawn behind it; editing behaviour is untouched. */
public class WEarthTextBox extends WMeteorTextBox {
    public WEarthTextBox(String text, String placeholder, CharFilter filter, Class<? extends Renderer> renderer) {
        super(text, placeholder, filter, renderer);
    }

    @Override
    public void renderBackground(GuiRenderer renderer, WWidget widget, boolean pressed, boolean mouseOver) {
        EarthGuiTheme earth = (EarthGuiTheme) theme;
        Chrome.field(renderer, earth.style, widget.x, widget.y, widget.width, widget.height, earth.unit(), focused, false);
    }
}
