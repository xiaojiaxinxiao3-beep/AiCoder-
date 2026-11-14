package aicoder.combat;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Turns your view slowly towards a nearby player, as if a hand were nudging the mouse. It really turns
 * the camera (nothing is faked towards the server), it only acts inside a cone around where you already
 * look, and by default it lets go once the crosshair is on the target.
 */
public class CrosshairDrift extends Module {
    public enum Part {
        Head,
        Body,
        Nearest
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgTargets = settings.createGroup("Targets");

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .description("How close a player has to be before the crosshair starts drifting to them.")
        .defaultValue(5)
        .range(1, 64)
        .sliderRange(1, 16)
        .build()
    );

    private final Setting<Double> cone = sgGeneral.add(new DoubleSetting.Builder()
        .name("cone")
        .description("Only players within this many degrees of your crosshair are picked. 360 means anywhere around you.")
        .defaultValue(90)
        .range(5, 360)
        .sliderRange(5, 360)
        .build()
    );

    private final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
        .name("speed")
        .description("Fastest the view turns, in degrees per second.")
        .defaultValue(30)
        .range(1, 1440)
        .sliderRange(5, 360)
        .build()
    );

    private final Setting<Double> softness = sgGeneral.add(new DoubleSetting.Builder()
        .name("softness")
        .description("Slows the drift down as the crosshair gets close. 0 moves at full speed until it arrives.")
        .defaultValue(0.6)
        .range(0, 1)
        .sliderRange(0, 1)
        .build()
    );

    private final Setting<Part> part = sgGeneral.add(new EnumSetting.Builder<Part>()
        .name("aim-at")
        .description("Head, the middle of the body, or whichever height on the target is nearest to your crosshair.")
        .defaultValue(Part.Nearest)
        .build()
    );

    private final Setting<Boolean> stopOnTarget = sgGeneral.add(new BoolSetting.Builder()
        .name("stop-on-target")
        .description("Stop nudging once the crosshair is on the target, so you keep full control of fine aim.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> vertical = sgGeneral.add(new BoolSetting.Builder()
        .name("vertical")
        .description("Also drift up and down. Off only turns left and right.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> friends = sgTargets.add(new BoolSetting.Builder()
        .name("ignore-friends")
        .description("Never drift towards players on your Meteor friends list.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> walls = sgTargets.add(new BoolSetting.Builder()
        .name("visible-only")
        .description("Only drift towards players you can actually see.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> mobs = sgTargets.add(new BoolSetting.Builder()
        .name("mobs")
        .description("Also drift towards mobs. Meant for trying the module out in a single player world.")
        .defaultValue(false)
        .build()
    );

    public CrosshairDrift() {
        super(Categories.Combat, "crosshair-drift", "When a player is within the range you set, your crosshair slowly drifts towards them.");
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (mc.player == null || mc.level == null || mc.gui.screen() != null) return;

        Vec3 eye = mc.player.getEyePosition(event.tickDelta);
        Vec3 look = mc.player.getViewVector(event.tickDelta);

        Entity target = null;
        Vec3 targetPoint = null;
        double best = cone.get() / 2;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!valid(entity)) continue;

            Vec3 point = aimPoint(entity, eye, look, event.tickDelta);
            if (point.distanceTo(eye) > range.get()) continue;

            double angle = Math.toDegrees(Math.acos(Mth.clamp(point.subtract(eye).normalize().dot(look), -1, 1)));
            if (angle <= best) {
                best = angle;
                target = entity;
                targetPoint = point;
            }
        }

        if (target == null) return;
        if (stopOnTarget.get() && mc.hitResult instanceof EntityHitResult hit && hit.getEntity() == target) return;

        Vec3 d = targetPoint.subtract(eye);
        double wantYaw = Math.toDegrees(Math.atan2(-d.x, d.z));
        double wantPitch = -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));

        double dYaw = Mth.wrapDegrees(wantYaw - mc.player.getYRot());
        double dPitch = vertical.get() ? wantPitch - mc.player.getXRot() : 0;
        double off = Math.sqrt(dYaw * dYaw + dPitch * dPitch);
        if (off < 0.05) return;

        // Full speed far away, easing off inside the last few degrees
        double ease = softness.get() > 0 ? Math.min(1, off / (30 * softness.get())) : 1;
        double step = Math.min(off, speed.get() * Math.max(0.15, ease) * Math.min(event.frameTime, 0.1));

        mc.player.setYRot((float) (mc.player.getYRot() + dYaw / off * step));
        mc.player.setXRot((float) Mth.clamp(mc.player.getXRot() + dPitch / off * step, -90, 90));
    }

    private boolean valid(Entity entity) {
        if (entity == mc.player || !entity.isAlive() || !(entity instanceof LivingEntity)) return false;

        if (entity instanceof Player player) {
            if (player.isSpectator()) return false;
            if (friends.get() && Friends.get().isFriend(player)) return false;
        } else if (!mobs.get()) {
            return false;
        }

        return !walls.get() || PlayerUtils.canSeeEntity(entity);
    }

    private Vec3 aimPoint(Entity entity, Vec3 eye, Vec3 look, float tickDelta) {
        Vec3 pos = entity.getPosition(tickDelta);
        AABB box = entity.getBoundingBox();
        double height = box.getYsize();

        double y = switch (part.get()) {
            case Head -> pos.y + entity.getEyeHeight();
            case Body -> pos.y + height * 0.5;
            case Nearest -> {
                // Height at which the line of sight passes the target, kept inside its hitbox
                double flat = Math.sqrt((pos.x - eye.x) * (pos.x - eye.x) + (pos.z - eye.z) * (pos.z - eye.z));
                double lookFlat = Math.sqrt(look.x * look.x + look.z * look.z);
                double at = lookFlat > 1e-4 ? eye.y + look.y / lookFlat * flat : pos.y + height * 0.5;
                yield Mth.clamp(at, pos.y + height * 0.15, pos.y + height * 0.9);
            }
        };

        return new Vec3(pos.x, y, pos.z);
    }
}
