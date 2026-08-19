package net.example.jrtameall.mixin;

// PLAN-B FALLBACK - NOT ACTIVE.
//
// Only enable this if DinosaurMixin's getter overrides ever prove fragile
// (e.g. a future Jurassic Reborn update inlines the field read). It redirects
// the isImprintable() call inside DinosaurEntity.setOwner(Player) so the tame
// gate sees "true" for every species.
//
// NOTE: @Inject at HEAD cannot bypass the early return in setOwner
// (cancelling at HEAD would abort the whole method), which is why the
// redirect-at-call-site form is used here instead.
//
// To activate:
//   1. Uncomment the class body below.
//   2. Add "DinosaurEntityMixin" to the "mixins" array in
//      jr_tame_all.mixins.json.

/*
package net.example.jrtameall.mixin;

import net.vit.jurassicreborn.common.entities.Dinosaurs.Dinosaur;
import net.vit.jurassicreborn.common.entities.DinosaurEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(DinosaurEntity.class)
public class DinosaurEntityMixin {

    @Redirect(
        method = "setOwner(Lnet/minecraft/world/entity/player/Player;)V",
        at = @At(value = "INVOKE",
                 target = "Lnet/vit/jurassicreborn/common/entities/Dinosaurs$Dinosaur;isImprintable()Z")
    )
    private boolean jr_tame_all$forceImprintable(Dinosaur dinosaur) {
        return true;
    }
}
*/
