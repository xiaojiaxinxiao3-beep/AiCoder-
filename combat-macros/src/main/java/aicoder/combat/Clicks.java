package aicoder.combat;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.phys.BlockHitResult;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * The three clicks the modules make, each written to follow what the vanilla client does for the same
 * mouse button: the same calls in the same order, so the server receives the same packets.
 */
final class Clicks {
    private Clicks() {
    }

    /** Use key on a block: note the held item's swing, click, swing if the click predicts one. */
    static void useOnBlock(BlockHitResult hit) {
        SwingAnimation swing = mc.player.getMainHandItem().getInteractAnimation();
        InteractionResult result = mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
        swingIfPredicted(result, swing);
    }

    /** Use key in the air, with the item in the main hand. Returns whether the item was used. */
    static boolean useItem() {
        SwingAnimation swing = mc.player.getMainHandItem().getInteractAnimation();
        InteractionResult result = mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
        swingIfPredicted(result, swing);
        return result instanceof InteractionResult.Success;
    }

    /** Attack key on an entity: attack, then swing. */
    static void attack(Entity entity) {
        SwingAnimation swing = mc.player.getMainHandItem().getAttackAnimation();
        mc.gameMode.attack(mc.player, entity);
        mc.player.swing(InteractionHand.MAIN_HAND, swing, false);
    }

    private static void swingIfPredicted(InteractionResult result, SwingAnimation swing) {
        if (result instanceof InteractionResult.Success success && success.swingSource() == InteractionResult.SwingSource.PREDICTED) {
            mc.player.swing(InteractionHand.MAIN_HAND, swing, false);
        }
    }
}
