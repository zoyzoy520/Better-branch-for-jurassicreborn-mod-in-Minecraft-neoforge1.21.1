package net.example.jrtameall.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.vit.jurassicreborn.common.entities.DinosaurEntity;
import net.vit.jurassicreborn.common.entities.FlyingDinosaurEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Peace rules between dinosaurs.
 *
 * DinosaurEntity.setTarget is the single funnel every targeting path flows
 * through (prey goals, the AGGRESSIVE player-hunt goal, HurtByTargetGoal
 * retaliation, RespondToAttackEntityAI, Defend/AssistOwnerAI), so cancelling
 * forbidden targets here covers all of them:
 *
 * Rule 1 - tamed dinosaurs never attack players (any player, not just the
 *          owner) nor other tamed dinosaurs (any owner).
 * Rule 2 - pterosaurs (FlyingDinosaurEntity) never attack each other, wild
 *          or tamed.
 * Rule 3 - marine creatures (Dinosaur.isMarineCreature()) never attack each
 *          other, wild or tamed.
 */
@Mixin(DinosaurEntity.class)
public class PeaceTargetMixin {

    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void jr_tame_all$filterPeaceTargets(LivingEntity target, CallbackInfo ci) {
        if (target == null) {
            return;
        }
        DinosaurEntity self = (DinosaurEntity) (Object) this;

        // Rule 1: tamed dinosaurs never target players or other tamed dinosaurs.
        if (self.getOwner() != null) {
            if (target instanceof Player
                    || (target instanceof DinosaurEntity other && other.getOwner() != null)) {
                ci.cancel();
                return;
            }
        }

        // Rule 2: pterosaurs never target other pterosaurs.
        if (self instanceof FlyingDinosaurEntity && target instanceof FlyingDinosaurEntity) {
            ci.cancel();
            return;
        }

        // Rule 3: marine creatures never target other marine creatures.
        if (self.getDinosaur().isMarineCreature()
                && target instanceof DinosaurEntity other
                && other.getDinosaur().isMarineCreature()) {
            ci.cancel();
        }
    }
}
