package aicoder.combat;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;

/**
 * Base of every module here: adds one switch, "chat-hints", that silences everything the module
 * would say in chat. The injected client has the same switch in its own Module class.
 */
public abstract class HintModule extends Module {
    private final Setting<Boolean> chatHints = settings.getDefaultGroup().add(new BoolSetting.Builder()
        .name("chat-hints")
        .description("Lets this module write to chat (what it did, or why it could not). Turn off to keep chat clear.")
        .defaultValue(true)
        .build()
    );

    protected HintModule(Category category, String name, String description) {
        super(category, name, description);
    }

    @Override
    public void info(String message, Object... args) {
        if (chatHints.get()) super.info(message, args);
    }

    @Override
    public void warning(String message, Object... args) {
        if (chatHints.get()) super.warning(message, args);
    }

    @Override
    public void error(String message, Object... args) {
        if (chatHints.get()) super.error(message, args);
    }
}
