package net.example.jrtameall.mixin;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.vit.jurassicreborn.common.entities.DinosaurEntity;
import net.vit.jurassicreborn.common.entities.FlyingDinosaurEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * M4: flight riding. FlyingDinosaurEntity.travel ignores ridden input while
 * airborne (the air branch just moves deltaMovement and damps by 0.91, with
 * no moveRelative), so the air branch is taken over entirely while a player
 * rides. Ground and water keep their original behavior: ground falls through
 * to DinosaurEntity.travel (pig-style walking, driven by the DinosaurRidingMixin
 * overrides), water uses the vanilla water branch which already consumes
 * input.y (pitch dive/climb).
 *
 * Horizontal acceleration comes from DinosaurRidingMixin.tickRidden (0.025
 * accel, 0.10 on jump, carrot required); this handler feeds the pitch
 * component through moveRelative and clamps vertical speed so a full-pitch
 * dive cannot accelerate without bound.
 */
@Mixin(FlyingDinosaurEntity.class)
public abstract class FlyingRidingMixin extends PathfinderMob {

    /** Max vertical speed in b/t while ridden (M5 tuning target). */
    private static final double MAX_RIDE_CLIMB_SPEED = 0.8D;
    /** M4 feedback: default climb/dive throttled to 0.25x (0.5x of the previous 0.5x); holding jump lifts it to 1.5x. */
    private static final double CLIMB_THROTTLE = 0.25D;
    private static final double CLIMB_BOOST = 1.5D;

    protected FlyingRidingMixin(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    @Shadow
    protected abstract void updateCustomFlightAnimation();

    /** Declared only on FlyingDinosaurEntity itself (not on PathfinderMob). */
    @Shadow
    public abstract boolean isTouchingGround();

    /**
     * M5: no flight animation while sitting. updateCustomFlightAnimation
     * advances walkAnimation (the wing-flap driver) from horizontal movement;
     * it is called at the end of every travel() branch, including when a
     * sitting flier idles. Cancelling it while SIT keeps the flier's wing
     * animation frozen instead of flapping in a seated pose.
     *
     * getOrder is inherited from DinosaurEntity and @Shadow would fail (shadow
     * methods must be declared on the target class itself), so call it through
     * a direct cast instead - the runtime instance is always a DinosaurEntity.
     */
    @Inject(method = "updateCustomFlightAnimation()V", at = @At("HEAD"), cancellable = true)
    private void jr_tame_all$noFlightAnimWhileSitting(CallbackInfo ci) {
        if (((DinosaurEntity) (Object) this).getOrder() == DinosaurEntity.Order.SIT) {
            ci.cancel();
        }
    }

    /**
     * M5: a sitting flier must never take off. JR's AIStartFlying goal
     * (random takeoff after ~11s on the ground) does not check the SIT order,
     * so a commanded-sitting flier would fly up and hover. startTakeOff is the
     * single entry point for every takeoff (AI goal and otherwise), so
     * cancelling it while SIT keeps the flier seated. (Ride takeoff uses the
     * jump hop in tickRidden, not this method.)
     */
    @Inject(method = "startTakeOff()V", at = @At("HEAD"), cancellable = true)
    private void jr_tame_all$noTakeoffWhileSitting(CallbackInfo ci) {
        if (((DinosaurEntity) (Object) this).getOrder() == DinosaurEntity.Order.SIT) {
            ci.cancel();
        }
    }

    @Inject(method = "travel(Lnet/minecraft/world/phys/Vec3;)V", at = @At("HEAD"), cancellable = true)
    private void jr_tame_all$rideAirTravel(Vec3 travelVector, CallbackInfo ci) {
        // Only take over while actually ridden and airborne. Unridden flight,
        // ground walking and water swimming keep the original branches.
        if (!(this.getControllingPassenger() instanceof Player)) {
            return;
        }
        if (this.isTouchingGround() || this.isInWater()) {
            return;
        }
        // Air branch takeover: the ride input (with pitch y) is the only
        // driver. Vertical speed is clamped so full-pitch dives/climbs settle
        // at a bounded speed instead of converging on unbounded velocity.
        // M4 feedback: default clamp is 0.5x the original; holding jump lifts
        // it to 1.5x. LocalPlayer reference is client-only, but on the server
        // this handler always returns at the passenger check above (server AI
        // travel never has a player passenger), so the class link never fires.
        double maxClimb = this.getControllingPassenger() instanceof net.minecraft.client.player.LocalPlayer local
                && local.input.jumping ? MAX_RIDE_CLIMB_SPEED * CLIMB_BOOST : MAX_RIDE_CLIMB_SPEED * CLIMB_THROTTLE;
        this.moveRelative(this.getSpeed(), travelVector);
        this.move(MoverType.SELF, this.getDeltaMovement());
        Vec3 d = this.getDeltaMovement();
        this.setDeltaMovement(new Vec3(d.x, Mth.clamp(d.y, -maxClimb, maxClimb), d.z));
        // Flap animation (the original travel calls this at the end of every
        // branch, including the air branch we just replaced).
        this.updateCustomFlightAnimation();
        ci.cancel();
    }
}
