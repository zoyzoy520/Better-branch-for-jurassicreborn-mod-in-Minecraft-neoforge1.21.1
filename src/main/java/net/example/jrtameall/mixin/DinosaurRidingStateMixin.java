package net.example.jrtameall.mixin;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.vit.jurassicreborn.common.entities.DinosaurEntity;
import net.vit.jurassicreborn.common.entities.FlyingDinosaurEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * M2: riding state guards. While a player rides, SIT order requests, sleep
 * requests and new combat targets are suppressed, and the dinosaur is never
 * immobile. The FlyingDinosaurEntity variant additionally unblocks its
 * isImmobile override (which would freeze the flier mid-air).
 */
@Mixin(DinosaurEntity.class)
public abstract class DinosaurRidingStateMixin extends PathfinderMob {

    protected DinosaurRidingStateMixin(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    // NOTE: distinct name from DinosaurRidingTravelMixin's helper - both mixins
    // merge into DinosaurEntity, and a same-signature duplicate would be skipped.
    private boolean jr_tame_all$riddenByPlayerState() {
        return this.getControllingPassenger() instanceof Player;
    }

    /** Riding overrides SIT order; WANDER and other orders pass through. */
    @Inject(method = "setFieldOrder(Lnet/vit/jurassicreborn/common/entities/DinosaurEntity$Order;)V",
            at = @At("HEAD"), cancellable = true)
    private void jr_tame_all$guardSitOrder(DinosaurEntity.Order order, CallbackInfo ci) {
        if (order == DinosaurEntity.Order.SIT && jr_tame_all$riddenByPlayerState()) {
            ci.cancel();
        }
    }

    /** Riding cancels falling asleep, but a wake call (false) always passes. */
    @Inject(method = "setSleeping(Z)V", at = @At("HEAD"), cancellable = true)
    private void jr_tame_all$guardSleep(boolean sleeping, CallbackInfo ci) {
        if (sleeping && jr_tame_all$riddenByPlayerState()) {
            ci.cancel();
        }
    }

    /** Riding never counts as immobile (sleep/SIT animation locks off). */
    @Inject(method = "isImmobile()Z", at = @At("HEAD"), cancellable = true)
    private void jr_tame_all$ridingNotImmobile(CallbackInfoReturnable<Boolean> cir) {
        if (jr_tame_all$riddenByPlayerState()) {
            cir.setReturnValue(false);
        }
    }

    /** Pure mount mode: riding blocks acquiring new combat targets. */
    @Inject(method = "setTarget(Lnet/minecraft/world/entity/LivingEntity;)V",
            at = @At("HEAD"), cancellable = true)
    private void jr_tame_all$guardCombatTarget(LivingEntity target, CallbackInfo ci) {
        if (jr_tame_all$riddenByPlayerState()) {
            ci.cancel();
        }
    }

    @Mixin(FlyingDinosaurEntity.class)
    public abstract static class FlyingStateMixin extends PathfinderMob {

        @Shadow
        public boolean shouldLand;

        protected FlyingStateMixin(EntityType<? extends PathfinderMob> type, Level level) {
            super(type, level);
        }

        /** Flying override locks immobility on blocked animation too; riding unblocks it. */
        @Inject(method = "isImmobile()Z", at = @At("HEAD"), cancellable = true)
        private void jr_tame_all$ridingNotImmobile(CallbackInfoReturnable<Boolean> cir) {
            if (this.getControllingPassenger() instanceof Player) {
                cir.setReturnValue(false);
            }
        }
    }
}
