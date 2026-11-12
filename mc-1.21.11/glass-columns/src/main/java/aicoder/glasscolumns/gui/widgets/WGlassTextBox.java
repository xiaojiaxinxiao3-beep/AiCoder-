package aicoder.glasscolumns.gui.widgets;

import aicoder.glasscolumns.gui.Chrome;
import aicoder.glasscolumns.gui.GlassGuiTheme;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.widgets.input.WMeteorTextBox;
import meteordevelopment.meteorclient.gui.utils.CharFilter;
import meteordevelopment.meteorclient.gui.widgets.WWidget;

/** Meteor's text box with the theme's value field drawn behind it; editing behaviour is untouched. */
public class WGlassTextBox extends WMeteorTextBox {
    public WGlassTextBox(String text, String placeholder, CharFilter filter, Class<? extends Renderer> renderer) {
        super(text, placeholder, filter, renderer);
    }

    @Override
    public void renderBackground(GuiRenderer renderer, WWidget widget, boolean pressed, boolean mouseOver) {
        GlassGuiTheme glass = (GlassGuiTheme) theme;
        Chrome.field(renderer, glass.style, widget.x, widget.y, widget.width, widget.height, glass.unit(), focused, false);
    }
}
