package com.kittytalents.item;

import com.kittytalents.registry.ModItems;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;

public class CatnipBucketItem extends Item {
    public CatnipBucketItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity entity, InteractionHand hand) {
        if ((entity instanceof Cow || entity instanceof MushroomCow) && !entity.isBaby()) {
            player.playSound(SoundEvents.COW_MILK, 1.0F, 1.0F);
            if (!player.level().isClientSide) {
                ItemStack milk = new ItemStack(ModItems.CATNIP_MILK_BUCKET.get());
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                if (stack.isEmpty()) {
                    player.setItemInHand(hand, milk);
                } else {
                    if (!player.getInventory().add(milk)) {
                        player.drop(milk, false);
                    }
                }
            }
            return InteractionResult.sidedSuccess(player.level().isClientSide);
        }
        return super.interactLivingEntity(stack, player, entity, hand);
    }
}
