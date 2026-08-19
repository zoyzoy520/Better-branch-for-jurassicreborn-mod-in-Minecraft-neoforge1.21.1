package net.example.jrtameall.mixin;

import net.vit.jurassicreborn.common.entities.DinosaurEntity;
import net.vit.jurassicreborn.common.entities.EntityUtils.MetabolismContainer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tamed dinosaurs stay fully fed and hydrated: energy and water never drop.
 *
 * update() is the per-tick metabolism tick: it drains energy/water by 1 when
 * the DINO_METABOLISM gamerule is on and the dinosaur is awake. For tamed
 * dinosaurs we top both values up to max every tick and skip the drain.
 * This also instantly refills activity costs (mating etc.) within a tick.
 */
@Mixin(MetabolismContainer.class)
public abstract class TamedMetabolismMixin {

    @Shadow
    @Final
    private DinosaurEntity dinosaur;

    @Shadow
    public abstract int getMaxEnergy();

    @Shadow
    public abstract int getMaxWater();

    @Shadow
    public abstract void setEnergy(int energy);

    @Shadow
    public abstract void setWater(int water);

    @Inject(method = "update", at = @At("HEAD"), cancellable = true)
    private void jr_tame_all$keepTamedFull(CallbackInfo ci) {
        if (this.dinosaur.getOwner() != null) {
            this.setEnergy(this.getMaxEnergy());
            this.setWater(this.getMaxWater());
            ci.cancel();
        }
    }
}
