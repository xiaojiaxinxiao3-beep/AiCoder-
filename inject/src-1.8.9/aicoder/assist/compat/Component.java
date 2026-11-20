package aicoder.assist.compat;

/** A piece of text; only the title of the menu screen uses it. */
public final class Component {
    public final String text;

    private Component(String text) {
        this.text = text;
    }

    public static Component literal(String text) {
        return new Component(text);
    }
}
