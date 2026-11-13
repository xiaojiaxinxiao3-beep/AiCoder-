package aicoder.aurora.gui.widgets;

import aicoder.aurora.gui.AuroraGuiTheme;
import aicoder.aurora.gui.Chrome;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.widgets.input.WMeteorTextBox;
import meteordevelopment.meteorclient.gui.utils.CharFilter;
import meteordevelopment.meteorclient.gui.widgets.WWidget;

/** Meteor's text box with the theme's dark inset drawn behind it; editing behaviour is untouched. */
public class WAuroraTextBox extends WMeteorTextBox {
    public WAuroraTextBox(String text, String placeholder, CharFilter filter, Class<? extends Renderer> renderer) {
        super(text, placeholder, filter, renderer);
    }

    @Override
    public void renderBackground(GuiRenderer renderer, WWidget widget, boolean pressed, boolean mouseOver) {
        AuroraGuiTheme aurora = (AuroraGuiTheme) theme;
        Chrome.field(renderer, aurora.style, widget.x, widget.y, widget.width, widget.height, aurora.unit(), focused, false);
    }
}
