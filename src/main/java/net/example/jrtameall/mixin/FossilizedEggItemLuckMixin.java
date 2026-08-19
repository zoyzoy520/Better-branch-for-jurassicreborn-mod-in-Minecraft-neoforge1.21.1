package net.example.jrtameall.mixin;

import net.vit.jurassicreborn.common.items.Fossils.FossilizedEggItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Random;

/**
 * Fossilized-egg soft-tissue extraction success x5.
 *
 * Vanilla roll: random.nextInt(3), soft tissue when roll == 0 (1/3).
 * 5x would exceed 100%, so clamped to always succeed (roll forced to 0).
 * The second nextInt call (picking a random amber dinosaur) is untouched
 * via the ordinal discriminator.
 */
@Mixin(FossilizedEggItem.class)
public class FossilizedEggItemLuckMixin {

    @Redirect(method = "getGroundItem",
              at = @At(value = "INVOKE", target = "Ljava/util/Random;nextInt(I)I", ordinal = 0))
    private int jr_tame_all$boostSoftTissueChance(Random random, int bound) {
        return 0; // always soft tissue
    }
}
