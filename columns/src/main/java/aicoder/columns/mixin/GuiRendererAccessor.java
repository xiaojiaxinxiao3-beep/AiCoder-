package aicoder.columns.mixin;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exposes the graphics handle the theme needs to draw text at sizes Meteor's renderer does not offer. */
@Mixin(value = GuiRenderer.class, remap = false)
public interface GuiRendererAccessor {
    @Accessor("graphics")
    GuiGraphicsExtractor columns$getGraphics();
}
