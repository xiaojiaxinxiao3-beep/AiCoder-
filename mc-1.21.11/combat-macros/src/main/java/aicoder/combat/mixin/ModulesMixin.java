package aicoder.combat.mixin;

import aicoder.combat.AiCoderTab;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits the module categories between two tabs. Every modules screen, whatever the theme, asks
 * {@code Modules.loopCategories()} which columns to show: on the AiCoder tab it gets only this addon's
 * categories, on the Modules tab everything else, and anywhere else the full list as usual.
 */
@Mixin(value = Modules.class, remap = false)
public abstract class ModulesMixin {
    @Shadow
    @Final
    private static List<Category> CATEGORIES;

    @Inject(method = "loopCategories", at = @At("HEAD"), cancellable = true)
    private static void aicombat$splitCategories(CallbackInfoReturnable<Iterable<Category>> cir) {
        AiCoderTab.View view = AiCoderTab.view();
        if (view == AiCoderTab.View.Everything) return;

        List<Category> shown = new ArrayList<>();
        for (Category category : CATEGORIES) {
            if (AiCoderTab.owns(category) == (view == AiCoderTab.View.Ours)) shown.add(category);
        }
        cir.setReturnValue(shown);
    }
}
