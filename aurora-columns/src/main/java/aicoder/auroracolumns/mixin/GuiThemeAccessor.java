package aicoder.auroracolumns.mixin;

import meteordevelopment.meteorclient.gui.GuiTheme;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * MeteorGuiTheme hard-codes its name to "Meteor". The ten themes have to extend it (Meteor's widgets cast
 * their theme to that class) and still be told apart, so each one renames itself through this accessor.
 */
@Mixin(value = GuiTheme.class, remap = false)
public interface GuiThemeAccessor {
    @Mutable
    @Accessor("name")
    void auroracol$setName(String name);
}
