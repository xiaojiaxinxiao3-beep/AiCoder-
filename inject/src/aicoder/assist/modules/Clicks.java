package aicoder.assist.modules;

import aicoder.assist.api.*;
import aicoder.assist.api.Module;
import aicoder.assist.api.Events.*;

import static aicoder.assist.Client.mc;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;


/**
 * The three clicks the modules make, each written to follow what the vanilla client does for the same
 * mouse button: the same calls in the same order, so the server receives the same packets.
 */
final class Clicks {
    private Clicks() {
    }

    /** Use key on a block: note the held item's swing, click, swing if the click predicts one. */
    static void useOnBlock(BlockHitResult hit) {
        InteractionResult result = mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
        swingIfPredicted(result);
    }

    /** Use key in the air, with the item in the main hand. Returns whether the item was used. */
    static boolean useItem() {
        InteractionResult result = mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
        swingIfPredicted(result);
        return result instanceof InteractionResult.Success;
    }

    /** Attack key on an entity: attack, then swing. */
    static void attack(Entity entity) {
        mc.gameMode.attack(mc.player, entity);
        mc.player.swing(InteractionHand.MAIN_HAND);
    }

    private static void swingIfPredicted(InteractionResult result) {
        if (result instanceof InteractionResult.Success success && success.swingSource() == InteractionResult.SwingSource.CLIENT) {
            mc.player.swing(InteractionHand.MAIN_HAND);
        }
    }
}
