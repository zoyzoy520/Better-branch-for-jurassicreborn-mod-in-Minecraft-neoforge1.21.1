package net.example.jrtameall.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.vit.jurassicreborn.common.entities.DinosaurEntity;

/**
 * Force-tames a wild dinosaur on right-click. The dinosaur keeps its current
 * growth stage (setOwner only assigns the owner, nothing else). No crafting
 * recipe exists - obtainable from the vanilla Tools creative tab only.
 */
public class TamingStickItem extends Item {

    public TamingStickItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand usedHand) {
        if (player.level().isClientSide) {
            return InteractionResult.PASS;
        }
        if (target instanceof DinosaurEntity dino && dino.getOwner() == null) {
            dino.setOwner(player);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
