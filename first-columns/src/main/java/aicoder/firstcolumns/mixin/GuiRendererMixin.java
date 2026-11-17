package aicoder.firstcolumns.mixin;

import aicoder.firstcolumns.gui.render.Sdf;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.renderer.Scissor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Ties the shader shape batch to Meteor's GUI batches: the shapes collected while a batch is open are
 * drawn right before Meteor draws that batch's own quads and text, under the same scissor.
 */
@Mixin(value = GuiRenderer.class, remap = false)
public abstract class GuiRendererMixin {
    @Inject(method = "beginRender", at = @At("HEAD"))
    private void aicoder$beginRender(CallbackInfo ci) {
        Sdf.begin();
    }

    @Inject(method = "endRender(Lmeteordevelopment/meteorclient/gui/renderer/Scissor;)V", at = @At("HEAD"))
    private void aicoder$endRender(Scissor scissor, CallbackInfo ci) {
        Sdf.flush(scissor);
    }

    @Inject(method = "setAlpha", at = @At("HEAD"))
    private void aicoder$setAlpha(double a, CallbackInfo ci) {
        Sdf.setAlpha(a);
    }
}
