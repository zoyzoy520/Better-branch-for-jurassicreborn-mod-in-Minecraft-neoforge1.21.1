package net.example.jrtameall.mixin;

import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import com.github.alexthe666.citadel.client.model.TabulaModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

/**
 * Accessor for TabulaModel.rootBoxes (protected). The riding seat height is
 * measured at render time (post-animator), so the model part tree is walked
 * from these roots each frame.
 */
@Mixin(TabulaModel.class)
public interface TabulaModelAccessor {

    @Accessor("rootBoxes")
    List<AdvancedModelBox> jr_tame_all$getRootBoxes();
}
