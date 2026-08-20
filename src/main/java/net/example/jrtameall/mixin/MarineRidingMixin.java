package net.example.jrtameall.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.vit.jurassicreborn.common.entities.AmphibianDinosaurEntity;
import net.vit.jurassicreborn.common.entities.CrocodileDinosaurEntity;
import net.vit.jurassicreborn.common.entities.DinosaurEntity;
import net.vit.jurassicreborn.common.entities.PenguinDinosaurEntity;
import net.vit.jurassicreborn.common.entities.SwimmingDinosaurEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * M3: aquatic riding. While a player rides, the marine travel branches that
 * would fight the rider are neutralized:
 *  - Swimming/Amphibian/Penguin: isCarcass() forced true inside travel(),
 *    skipping the JR server-side damped-sink branch and falling through to
 *    the vanilla water travel, so pitch (input.y) dives and climbs work on
 *    both sides with no rubber-banding.
 *  - Crocodile: travel() is overridden in CrocodileDinosaurEntity without an
 *    isCarcass call, but while ridden it has no target (M2 clears it), so it
 *    falls to super.travel() -> Amphibian.travel, which the Amphibian mixin
 *    above already neutralizes. It additionally returns early while basking;
 *    the isBaskingNow() call is forced false so a basking croc moves when
 *    ridden.
 */
@Mixin(SwimmingDinosaurEntity.class)
public abstract class MarineRidingMixin extends PathfinderMob {

    protected MarineRidingMixin(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    private static boolean jr_tame_all$ridden(PathfinderMob self) {
        return self.getFirstPassenger() instanceof Player;
    }

    // NOTE: javac resolves inherited-method invokes in subclass bodies to the
    // SUBCLASS owner (e.g. SwimmingDinosaurEntity.isCarcass), not DinosaurEntity.
    @ModifyExpressionValue(method = "travel(Lnet/minecraft/world/phys/Vec3;)V",
            at = @At(value = "INVOKE",
                     target = "Lnet/vit/jurassicreborn/common/entities/SwimmingDinosaurEntity;isCarcass()Z"))
    private boolean jr_tame_all$ridingSkipsDamp(boolean original) {
        return jr_tame_all$ridden(this) || original;
    }

    @Mixin(AmphibianDinosaurEntity.class)
    public abstract static class AmphibianMixin extends PathfinderMob {

        protected AmphibianMixin(EntityType<? extends PathfinderMob> type, Level level) {
            super(type, level);
        }

        @ModifyExpressionValue(method = "travel(Lnet/minecraft/world/phys/Vec3;)V",
                at = @At(value = "INVOKE",
                         target = "Lnet/vit/jurassicreborn/common/entities/AmphibianDinosaurEntity;isCarcass()Z"))
        private boolean jr_tame_all$ridingSkipsDamp(boolean original) {
            return jr_tame_all$ridden(this) || original;
        }
    }

    @Mixin(PenguinDinosaurEntity.class)
    public abstract static class PenguinMixin extends PathfinderMob {

        protected PenguinMixin(EntityType<? extends PathfinderMob> type, Level level) {
            super(type, level);
        }

        @ModifyExpressionValue(method = "travel(Lnet/minecraft/world/phys/Vec3;)V",
                at = @At(value = "INVOKE",
                         target = "Lnet/vit/jurassicreborn/common/entities/PenguinDinosaurEntity;isCarcass()Z"))
        private boolean jr_tame_all$ridingSkipsDamp(boolean original) {
            return jr_tame_all$ridden(this) || original;
        }
    }

    @Mixin(CrocodileDinosaurEntity.class)
    public abstract static class CrocodileMixin extends PathfinderMob {

        protected CrocodileMixin(EntityType<? extends PathfinderMob> type, Level level) {
            super(type, level);
        }

        @ModifyExpressionValue(method = "travel(Lnet/minecraft/world/phys/Vec3;)V",
                at = @At(value = "INVOKE",
                         target = "Lnet/vit/jurassicreborn/common/entities/CrocodileDinosaurEntity;isBaskingNow()Z"))
        private boolean jr_tame_all$ridingNotBasking(boolean original) {
            return jr_tame_all$ridden(this) ? false : original;
        }
    }
}
