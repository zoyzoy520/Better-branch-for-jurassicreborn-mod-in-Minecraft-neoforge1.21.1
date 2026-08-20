package net.example.jrtameall.mixin;

import com.github.alexthe666.citadel.client.model.container.TabulaModelContainer;
import net.vit.jurassicreborn.common.entities.Dinosaurs.Dinosaur;
import net.vit.jurassicreborn.common.entities.EntityUtils.GrowthStage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Invoker for Dinosaur.getModelContainer (private). The tabula model
 * containers are client-only data, used by the riding seat math.
 */
@Mixin(Dinosaur.class)
public interface DinosaurModelAccessor {

    @Invoker("getModelContainer")
    TabulaModelContainer jr_tame_all$getModelContainer(GrowthStage stage);
}
