package aicoder.firstcolumns.gui.render;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;

/** Text helpers. Shapes are drawn by the shader, see {@link Sdf}. */
public final class Draw {
    private Draw() {
    }

    /** Draws text with extra spacing between the characters and returns its width. */
    public static double spacedText(GuiRenderer r, GuiTheme theme, String text, double x, double y, double spacing, Color color, boolean title) {
        double start = x;

        for (int i = 0; i < text.length(); i++) {
            String ch = text.substring(i, i + 1);
            if (!ch.equals(" ")) r.text(ch, x, y, color, title);
            x += theme.textWidth(ch, 1, title) + spacing;
        }

        return x - spacing - start;
    }

    public static double spacedWidth(GuiTheme theme, String text, double spacing, boolean title) {
        double w = 0;
        for (int i = 0; i < text.length(); i++) w += theme.textWidth(text.substring(i, i + 1), 1, title) + spacing;
        return text.isEmpty() ? 0 : w - spacing;
    }
}
