package aicoder.assist.gfx;

/** The little of Meteor's theme object that the drawing code asks for: where to get the text renderer. */
public class GuiTheme {
    private final TextRenderer text;

    GuiTheme(GuiRenderer renderer) {
        text = new TextRenderer(renderer);
    }

    public TextRenderer textRenderer() {
        return text;
    }
}
