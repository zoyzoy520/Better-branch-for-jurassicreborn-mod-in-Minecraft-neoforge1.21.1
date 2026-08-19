package net.example.jrtameall.mixin;

import net.vit.jurassicreborn.common.blocks.wood.PetrifiedLogBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Random;

/**
 * Petrified-log tissue extraction success x5.
 *
 * Vanilla roll: random.nextInt(4), tissue when roll == 3 (1/4).
 * 5x would exceed 100%, so clamped to always succeed (roll forced to 3).
 * If the log has no tissue item, vanilla falls back to flint as before.
 */
@Mixin(PetrifiedLogBlock.class)
public class PetrifiedLogLuckMixin {

    @Redirect(method = "getGroundItem",
              at = @At(value = "INVOKE", target = "Ljava/util/Random;nextInt(I)I"))
    private int jr_tame_all$boostTissueChance(Random random, int bound) {
        return 3; // always the tissue branch
    }
}
