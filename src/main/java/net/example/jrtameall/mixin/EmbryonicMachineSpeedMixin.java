package net.example.jrtameall.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.vit.jurassicreborn.common.blocks.entities.Embryoncis.EmbryonicMachine.EmbryonicMachineBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Embryonic machine gestation speed x10: the vanilla tick adds +1 processTime
 * per tick; we add +9 more when actively processing, so total is 10x faster.
 */
@Mixin(EmbryonicMachineBlockEntity.class)
public class EmbryonicMachineSpeedMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private static void jr_tame_all$boostEmbryonic(Level level, BlockPos pos, BlockState state,
                                                   EmbryonicMachineBlockEntity be, CallbackInfo ci) {
        if (level.isClientSide) {
            return;
        }
        MachineAccessors.Embryonic acc = (MachineAccessors.Embryonic) be;
        if (acc.jr_tame_all$getProcessTime() >= EmbryonicMachineBlockEntity.STACK_PROCESS_TIME) {
            return;
        }
        if (be.canProcess(new ItemStack[0])) {
            int boosted = Math.min(EmbryonicMachineBlockEntity.STACK_PROCESS_TIME - 1,
                    acc.jr_tame_all$getProcessTime() + 9);
            acc.jr_tame_all$setProcessTime(boosted);
        }
    }
}
