package net.example.jrtameall.mixin;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.vit.jurassicreborn.common.entities.DinosaurEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * DISABLED (not registered in jr_tame_all.mixins.json) - kept for reference.
 *
 * Attempted M3 water-sprint fix by replacing the water-branch moveRelative
 * argument in LivingEntity.travel. Instrumentation probes showed
 * LivingEntity.travel is NEVER on the ridden dinosaur's path (a ridden
 * megalodon's travel never reaches it - JR's marine travel handles movement
 * itself), so this injection point can never fire.
 *
 * The working fix lives in DinosaurRidingMixin.tickRidden: extra forward
 * acceleration on deltaMovement while the local player holds jump.
 */
@Mixin(LivingEntity.class)
public abstract class WaterRidingTravelMixin extends LivingEntity {

    private static final float WATER_RIDE_ACCEL = 0.013F;
    private static final float WATER_RIDE_SPRINT_ACCEL = 0.033F;

    protected WaterRidingTravelMixin(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
    }

    /** Probe: is LivingEntity.travel even on the ridden dinosaur's path? */
    @Inject(method = "travel(Lnet/minecraft/world/phys/Vec3;)V", at = @At("HEAD"))
    private void jr_tame_all$waterTravelHead(CallbackInfo ci) {
        if (this.tickCount % 20 == 0) {
            System.out.println("[JRWATER] HEAD tick=" + this.tickCount
                    + " inWater=" + this.isInWater()
                    + " controlled=" + this.isControlledByLocalInstance()
                    + " dino=" + ((Object) this instanceof DinosaurEntity)
                    + " rider=" + (this.getControllingPassenger() instanceof Player));
        }
    }

    /** First moveRelative call in travel() is the water branch; the lava branch is the second. */
    @ModifyArg(method = "travel(Lnet/minecraft/world/phys/Vec3;)V",
               at = @At(value = "INVOKE",
                        target = "Lnet/minecraft/world/entity/LivingEntity;moveRelative(FLnet/minecraft/world/phys/Vec3;)V",
                        ordinal = 0),
               index = 0)
    private float jr_tame_all$riddenWaterAcceleration(float speed) {
        boolean dino = (Object) this instanceof DinosaurEntity;
        Player passenger = this.getControllingPassenger() instanceof Player player ? player : null;
        boolean ridden = passenger != null;
        boolean jump = ridden && passenger instanceof net.minecraft.client.player.LocalPlayer local
                && local.input.jumping;
        if (this.tickCount % 20 == 0) {
            System.out.println("[JRWATER] tick=" + this.tickCount + " dino=" + dino
                    + " ridden=" + ridden + " jump=" + jump
                    + " orig=" + speed + " ret=" + (dino && ridden ? (jump ? WATER_RIDE_SPRINT_ACCEL : WATER_RIDE_ACCEL) : speed));
        }
        if (dino && ridden) {
            return jump ? WATER_RIDE_SPRINT_ACCEL : WATER_RIDE_ACCEL;
        }
        return speed;
    }
}
