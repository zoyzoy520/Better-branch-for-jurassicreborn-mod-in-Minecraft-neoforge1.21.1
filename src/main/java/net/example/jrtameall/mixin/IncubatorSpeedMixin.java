package net.example.jrtameall.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.vit.jurassicreborn.common.blocks.entities.incubator.IncubatorBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Incubator hatch speed x10: the vanilla tick adds +1 per egg per tick;
 * we add +9 more for every slot that would advance, so total is 10x faster.
 */
@Mixin(IncubatorBlockEntity.class)
public class IncubatorSpeedMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private static void jr_tame_all$boostIncubator(Level level, BlockPos pos, BlockState state,
                                                   IncubatorBlockEntity be, CallbackInfo ci) {
        if (level.isClientSide) {
            return;
        }
        MachineAccessors.Incubator acc = (MachineAccessors.Incubator) be;
        int[] times = acc.jr_tame_all$getEggIncubationTime();
        for (int slot : IncubatorBlockEntity.INPUTS) {
            ItemStack stack = acc.jr_tame_all$getItemHandler().getStackInSlot(slot);
            if (!stack.isEmpty() && be.canProcess(stack)) {
                int t = times[slot];
                if (t < IncubatorBlockEntity.PROCESS_TIME) {
                    times[slot] = Math.min(IncubatorBlockEntity.PROCESS_TIME - 1, t + 9);
                }
            }
        }
    }
}
