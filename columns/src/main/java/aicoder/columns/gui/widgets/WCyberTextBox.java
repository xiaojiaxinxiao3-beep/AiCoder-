package aicoder.columns.gui.widgets;

import aicoder.columns.gui.AiCoderGuiTheme;
import aicoder.columns.gui.Cyber;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.widgets.input.WMeteorTextBox;
import meteordevelopment.meteorclient.gui.utils.CharFilter;
import meteordevelopment.meteorclient.gui.widgets.WWidget;

/** Meteor's text box with the cut-corner value field from the design; editing behaviour is untouched. */
public class WCyberTextBox extends WMeteorTextBox {
    public WCyberTextBox(String text, String placeholder, CharFilter filter, Class<? extends Renderer> renderer) {
        super(text, placeholder, filter, renderer);
    }

    @Override
    public void renderBackground(GuiRenderer renderer, WWidget widget, boolean pressed, boolean mouseOver) {
        double u = ((AiCoderGuiTheme) theme).unit();
        double cut = Math.min(5 * u, widget.height / 3);

        Cyber.box(renderer, widget.x, widget.y, widget.width, widget.height, cut, 0, 0, 0, Cyber.hair(u),
            Cyber.a(Cyber.FIELD, 0.55), focused ? Cyber.a(Cyber.CYAN, 0.9) : Cyber.a(Cyber.CYAN_LINE, 0.32));
    }
}
