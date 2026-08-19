package net.example.jrtameall.mixin;

import net.vit.jurassicreborn.common.items.Fossils.PlantFossilItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Random;

/**
 * Plant-fossil extraction success x5.
 *
 * Vanilla roll: random.nextInt(4), prehistoric plant when roll == 3 (1/4).
 * 5x would exceed 100%, so clamped to always succeed (roll forced to 3).
 * The second nextInt call (picking a random plant) is untouched via the
 * ordinal discriminator.
 */
@Mixin(PlantFossilItem.class)
public class PlantFossilItemLuckMixin {

    @Redirect(method = "getGroundItem",
              at = @At(value = "INVOKE", target = "Ljava/util/Random;nextInt(I)I", ordinal = 0))
    private int jr_tame_all$boostPlantChance(Random random, int bound) {
        return 3; // always the plant-tissue branch
    }
}
