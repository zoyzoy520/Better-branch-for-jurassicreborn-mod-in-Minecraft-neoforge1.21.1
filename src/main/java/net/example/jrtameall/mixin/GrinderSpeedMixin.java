package net.example.jrtameall.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.vit.jurassicreborn.common.blocks.entities.grinder.FossilGrinderBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fossil grinder speed x5: the vanilla tick adds +1 grindTime per tick;
 * we add +4 more when actively grinding, so total progress is 5x faster.
 */
@Mixin(FossilGrinderBlockEntity.class)
public class GrinderSpeedMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private static void jr_tame_all$boostGrinder(Level level, BlockPos pos, BlockState state,
                                                 FossilGrinderBlockEntity be, CallbackInfo ci) {
        if (level.isClientSide) {
            return;
        }
        MachineAccessors.Grinder acc = (MachineAccessors.Grinder) be;
        if (acc.jr_tame_all$getGrindTime() >= FossilGrinderBlockEntity.PROCESS_TIME) {
            return;
        }
        if (be.hasInputs() && be.canProcess(new ItemStack[0])) {
            int boosted = Math.min(FossilGrinderBlockEntity.PROCESS_TIME - 1,
                    acc.jr_tame_all$getGrindTime() + 4);
            acc.jr_tame_all$setGrindTime(boosted);
        }
    }
}
