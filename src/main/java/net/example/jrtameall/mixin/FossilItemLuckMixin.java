package net.example.jrtameall.mixin;

import net.vit.jurassicreborn.common.items.Fossils.FossilItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Random;

/**
 * Fossil soft-tissue extraction success x5.
 *
 * Vanilla roll: random.nextInt(6), soft tissue when roll == 5 (1/6).
 * Boosted: 25/30 = 5/6 soft tissue, 3/30 bone meal, 2/30 flint.
 * "Fresh" fossils still always succeed (checked separately in vanilla code).
 */
@Mixin(FossilItem.class)
public class FossilItemLuckMixin {

    @Redirect(method = "getGroundItem",
              at = @At(value = "INVOKE", target = "Ljava/util/Random;nextInt(I)I"))
    private int jr_tame_all$boostSoftTissueChance(Random random, int bound) {
        int roll = random.nextInt(30);
        if (roll < 25) {
            return 5; // soft tissue (vanilla success value)
        }
        if (roll < 28) {
            return 0; // bone meal (vanilla roll < 3)
        }
        return 3;     // flint
    }
}
