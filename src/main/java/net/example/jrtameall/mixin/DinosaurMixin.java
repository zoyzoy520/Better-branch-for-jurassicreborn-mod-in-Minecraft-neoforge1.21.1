package net.example.jrtameall.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.vit.jurassicreborn.common.entities.Dinosaurs.Dinosaur;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Unlocks imprinting (taming) + owner-defense for every Jurassic Reborn
 * species at once.
 *
 * Every species config class extends the abstract {@link Dinosaur} base and
 * inherits these getters (none overrides them, verified against the
 * jurassicreborn-1.3.44.jar bytecode), so overriding the return values here
 * covers all ~109 species including marine/flying/invertebrates.
 *
 * - isImprintable(): gates DinosaurEntity.setOwner(Player) - the only tame
 *   path is HatchedEggItem on hatch (skipped while the player is sneaking,
 *   which is preserved).
 * - shouldDefendOwner(): gates DefendOwnerAI + AssistOwnerAI registration in
 *   DinosaurEntity.registerGoals(); those AIs only act when the dinosaur has
 *   an owner and is ordered to follow.
 *
 * The target class belongs to a third-party mod and is never remapped by
 * NeoForge, so plain names/descriptors are the runtime names - no refmap.
 */
@Mixin(Dinosaur.class)
public class DinosaurMixin {

    @ModifyReturnValue(method = "isImprintable()Z", at = @At("RETURN"))
    private boolean jr_tame_all$unlockImprinting(boolean original) {
        return true;
    }

    @ModifyReturnValue(method = "shouldDefendOwner()Z", at = @At("RETURN"))
    private boolean jr_tame_all$unlockDefendOwner(boolean original) {
        return true;
    }
}
