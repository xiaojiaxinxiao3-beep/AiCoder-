package aicoder.combat;

import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.BlockHitResult;

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
        ActionResult result = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
        swingIfPredicted(result);
    }

    /** Use key in the air, with the item in the main hand. Returns whether the item was used. */
    static boolean useItem() {
        ActionResult result = mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        swingIfPredicted(result);
        return result instanceof ActionResult.Success;
    }

    /** Attack key on an entity: attack, then swing. */
    static void attack(Entity entity) {
        mc.interactionManager.attackEntity(mc.player, entity);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private static void swingIfPredicted(ActionResult result) {
        if (result instanceof ActionResult.Success success && success.swingSource() == ActionResult.SwingSource.CLIENT) {
            mc.player.swingHand(Hand.MAIN_HAND);
        }
    }
}
