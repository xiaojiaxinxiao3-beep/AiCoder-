package aicoder.combat.mixin;

import meteordevelopment.meteorclient.gui.tabs.Tab;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * A theme's modules screen always says it belongs to the Modules tab. The AiCoder tab reuses that screen,
 * so it has to put its own name on it for the tab strip to highlight the right tab.
 */
@Mixin(value = TabScreen.class, remap = false)
public interface TabScreenAccessor {
    @Mutable
    @Accessor("tab")
    void aicombat$setTab(Tab tab);
}
