package net.example.jrtameall.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.vit.jurassicreborn.common.entities.DinosaurEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * M1: while a player rides a land dinosaur, force canDinoSwim() to true
 * inside travel() so the JR water-sink flail branch is skipped and the
 * vanilla water travel runs instead (no sink spiral when riding into water).
 */
@Mixin(DinosaurEntity.class)
public abstract class DinosaurRidingTravelMixin extends PathfinderMob {

    protected DinosaurRidingTravelMixin(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    @ModifyExpressionValue(method = "travel(Lnet/minecraft/world/phys/Vec3;)V",
            at = @At(value = "INVOKE",
                     target = "Lnet/vit/jurassicreborn/common/entities/DinosaurEntity;canDinoSwim()Z"))
    private boolean jr_tame_all$neutralizeWaterSink(boolean original) {
        return this.jr_tame_all$riddenByPlayer() || original;
    }

    private boolean jr_tame_all$riddenByPlayer() {
        return this.getFirstPassenger() instanceof Player;
    }
}
