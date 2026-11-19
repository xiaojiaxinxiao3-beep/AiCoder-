package aicoder.combat;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * A TNT minecart set off by your own burning arrow. Look at a spot on the ground a few blocks ahead and
 * switch the module on: it half-draws a Flame bow, aims so the slow arrow will come down on that spot,
 * lets go, and in the same tick puts a rail and a TNT minecart there. The arrow lands on the cart and
 * the cart goes off. The arrow leaves first, as it has to: the rail and cart follow it in the same tick.
 *
 * <p>One round per activation. Everything is a key press: hotbar slots, the use key held and let go for
 * the bow, the use key on the block for the rail and the cart, and the real camera turns.
 * The blast is three or four blocks from you: it hurts you as well.
 */
public class CartBomb extends HintModule {
    private enum Stage { Draw, Done }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> draw = sgGeneral.add(new IntSetting.Builder()
        .name("draw")
        .description("How long the bow is drawn, in ticks. 3 is the least that shoots at all; less draw is a slower arrow. 20 is a full draw.")
        .defaultValue(4)
        .range(3, 20)
        .sliderRange(3, 20)
        .build()
    );

    private final Setting<Boolean> lookBack = sgGeneral.add(new BoolSetting.Builder()
        .name("look-back")
        .description("Turn the camera back to where you were looking afterwards.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> report = sgGeneral.add(new BoolSetting.Builder()
        .name("messages")
        .description("Say in chat why a round was not started or not finished.")
        .defaultValue(true)
        .build()
    );

    private Stage stage = Stage.Done;
    private BlockPos ground;
    private int bow, rail, cart, restSlot, ticks;
    private float restYaw, restPitch;
    private boolean useDown, railDown;

    public CartBomb() {
        super(AiCoderTab.EXPLOSIVES, "cart-bomb", "Look at the ground a few blocks ahead: shoots a half-drawn Flame bow at that spot and puts a rail and a TNT minecart there before the arrow lands, so the arrow sets the cart off.");
    }

    @Override
    public void onActivate() {
        stage = Stage.Done;
        if (mc.player == null || mc.level == null) return;

        bow = find("bow");
        rail = find("rail");
        cart = find("tnt_minecart");
        if (bow < 0 || cart < 0) {
            quit("Needs a bow and a TNT minecart in your hotbar.");
            return;
        }
        if (!(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK || hit.getDirection() != Direction.UP) {
            quit("Look at the top of a block within reach first.");
            return;
        }
        ground = hit.getBlockPos();
        // A rail that is already there, from a round before: the cart goes straight onto it
        railDown = isRail(ground);
        if (railDown) ground = ground.below();
        if (!railDown && !mc.level.getBlockState(ground.above()).isAir()) {
            quit("There is no room for a rail on that block.");
            return;
        }
        if (!railDown && rail < 0) {
            quit("Needs a rail in your hotbar.");
            return;
        }
        if (Double.isNaN(aimPitch())) {
            quit("That spot is too far for an arrow this weak. Draw longer or pick a nearer spot.");
            return;
        }

        restSlot = mc.player.getInventory().getSelectedSlot();
        restYaw = mc.player.getYRot();
        restPitch = mc.player.getXRot();
        ticks = 0;
        stage = Stage.Draw;

        // Bow out and the use key down: the game starts drawing in this tick
        mc.player.getInventory().setSelectedSlot(bow);
        setUse(true);
    }

    @Override
    public void onDeactivate() {
        setUse(false);
        stage = Stage.Done;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (stage != Stage.Draw) return;
        if (mc.player == null || mc.level == null || mc.gui.screen() != null) {
            toggle();
            return;
        }
        if (++ticks > 60) {
            quit("The bow would not draw. Is there an arrow?");
            return;
        }

        if (mc.player.getInventory().getSelectedSlot() != bow) mc.player.getInventory().setSelectedSlot(bow);
        setUse(true);

        // Hold the aim the arrow needs while drawing
        double pitch = aimPitch();
        if (Double.isNaN(pitch)) {
            quit("That spot is out of the arrow's range now.");
            return;
        }
        face(cartX(), cartZ());
        mc.player.setXRot((float) pitch);

        if (!mc.player.isUsingItem() || mc.player.getTicksUsingItem() < draw.get()) return;

        // Let go: the arrow is away. The rail and the cart follow in this same tick, in that order.
        setUse(false);
        mc.gameMode.releaseUsingItem(mc.player);

        lookAt(ground.getX() + 0.5, ground.getY() + 1.0, ground.getZ() + 0.5);
        Clicks.repick();
        if (!railDown) {
            if (!(mc.hitResult instanceof BlockHitResult top) || top.getType() != HitResult.Type.BLOCK || !top.getBlockPos().equals(ground)) {
                finish("The spot is not under the crosshair any more: no rail placed.");
                return;
            }
            mc.player.getInventory().setSelectedSlot(rail);
            Clicks.useOnBlock(top);
            Clicks.repick();
        }

        if (!(mc.hitResult instanceof BlockHitResult onRail) || onRail.getType() != HitResult.Type.BLOCK || !onRail.getBlockPos().equals(ground.above())) {
            finish("The rail did not go down: no cart placed.");
            return;
        }
        mc.player.getInventory().setSelectedSlot(cart);
        Clicks.useOnBlock(onRail);
        finish(null);
    }

    private void finish(String problem) {
        if (problem != null && report.get()) warning(problem);
        mc.player.getInventory().setSelectedSlot(restSlot);
        if (lookBack.get()) {
            mc.player.setYRot(restYaw);
            mc.player.setXRot(restPitch);
        }
        stage = Stage.Done;
        toggle();
    }

    private void quit(String why) {
        if (report.get()) warning(why);
        stage = Stage.Done;
        toggle();
    }

    private void setUse(boolean down) {
        if (useDown == down) return;
        useDown = down;
        mc.options.keyUse.setDown(down || Input.isPressed(mc.options.keyUse));
    }

    // Aim

    private double cartX() { return ground.getX() + 0.5; }
    private double cartZ() { return ground.getZ() + 0.5; }

    private void face(double x, double z) {
        mc.player.setYRot((float) Math.toDegrees(Math.atan2(-(x - mc.player.getX()), z - mc.player.getZ())));
    }

    private void lookAt(double x, double y, double z) {
        double dx = x - mc.player.getX(), dy = y - mc.player.getEyeY(), dz = z - mc.player.getZ();
        face(x, z);
        mc.player.setXRot((float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz))));
    }

    /**
     * The pitch that drops the arrow onto the middle of the cart, or NaN when it cannot get there.
     * An arrow leaves a little below the eyes at three times the bow's power, and each tick moves, then
     * loses 1% of its speed and gains 0.05 downwards.
     */
    private double aimPitch() {
        double seconds = draw.get() / 20.0, power = Math.min(1, (seconds * seconds + seconds * 2) / 3), speed = power * 3;
        double dx = cartX() - mc.player.getX(), dz = cartZ() - mc.player.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz), startY = mc.player.getEyeY() - 0.1, targetY = ground.getY() + 1.35;

        // From looking straight at it, raise the aim until the arrow no longer falls short
        double direct = -Math.toDegrees(Math.atan2(targetY - startY, distance));
        for (double pitch = direct; pitch >= -50; pitch -= 0.25) {
            double y = heightAt(speed, pitch, distance, startY);
            if (!Double.isNaN(y) && y >= targetY) return pitch;
        }
        return Double.NaN;
    }

    private static double heightAt(double speed, double pitch, double distance, double startY) {
        double vx = speed * Math.cos(Math.toRadians(pitch)), vy = -speed * Math.sin(Math.toRadians(pitch));
        double x = 0, y = startY;
        for (int i = 0; i < 80; i++) {
            if (x + vx >= distance) return y + vy * (distance - x) / vx;
            x += vx;
            y += vy;
            vx *= 0.99;
            vy = vy * 0.99 - 0.05;
        }
        return Double.NaN;
    }

    private boolean isRail(BlockPos pos) {
        String id = BuiltInRegistries.BLOCK.getKey(mc.level.getBlockState(pos).getBlock()).getPath();
        return id.equals("rail") || id.endsWith("_rail");
    }

    /** The first hotbar slot whose item is named like this: "rail" also takes powered and detector rails. */
    private int find(String name) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
            if (id.equals(name) || name.equals("rail") && id.endsWith("_rail")) return i;
        }
        return -1;
    }
}
