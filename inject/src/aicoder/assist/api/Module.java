package aicoder.assist.api;

import aicoder.assist.Client;
import net.minecraft.client.Minecraft;

/** Base of every module. The shape follows Meteor's Module, so the same module source builds for both. */
public abstract class Module {
    protected final Minecraft mc = Client.mc;

    public final Category category;
    public final String name;
    public final String title;
    public final String description;
    public final Settings settings = new Settings();

    /** Key that switches this module, as a GLFW key code; -1 for none. */
    public int bind = -1;
    /** With a bind: true keeps the module on only while the key is held, false flips it on each press. */
    public boolean holdBind;

    private boolean active;

    /** Off: the module writes nothing to chat. */
    private final Setting<Boolean> chatHints = settings.getDefaultGroup().add(new BoolSetting.Builder()
        .name("chat-hints")
        .description("Lets this module write to chat (what it did, or why it could not). Turn off to keep chat clear.")
        .defaultValue(true)
        .build()
    );

    protected Module(Category category, String name, String description) {
        this.category = category;
        this.name = name;
        this.title = titleOf(name);
        this.description = description;
    }

    /** "anchor-macro" becomes "Anchor Macro". */
    static String titleOf(String name) {
        StringBuilder out = new StringBuilder();
        for (String word : name.split("-")) {
            if (word.isEmpty()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return out.toString();
    }

    public boolean isActive() {
        return active;
    }

    public void toggle() {
        setActive(!active);
    }

    public void setActive(boolean value) {
        if (active == value) return;
        if (value && aicoder.assist.license.License.locked()) {
            Client.chat(title, "Locked: " + (aicoder.assist.license.License.state().equals("checking") ? "the licence is still being checked." : "no valid licence."), 0xFFFFC857);
            return;
        }
        active = value;
        try {
            if (value) onActivate();
            else onDeactivate();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public void onActivate() {
    }

    public void onDeactivate() {
    }

    protected void info(String message, Object... args) {
        if (chatHints.get()) Client.chat(title, String.format(message, args), 0xFF4DFFB8);
    }

    protected void warning(String message, Object... args) {
        if (chatHints.get()) Client.chat(title, String.format(message, args), 0xFFFFC857);
    }

    protected void error(String message, Object... args) {
        if (chatHints.get()) Client.chat(title, String.format(message, args), 0xFFFF6F8A);
    }
}
