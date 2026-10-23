package com.kittytalents.item;

import com.kittytalents.entity.TalentedCat;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class FeatherToyItem extends Item {
    public FeatherToyItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (target instanceof TalentedCat cat && cat.isOwnedBy(player) && !cat.isIncapacitated()) {
            cat.playSound(SoundEvents.CAT_PURREOW, 1.0F, 1.3F);
            cat.level().addParticle(ParticleTypes.HAPPY_VILLAGER, cat.getX(), cat.getY() + 0.5D, cat.getZ(), 0, 0.1D, 0);
            cat.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 1));
            cat.increaseHunger(5);
            return InteractionResult.sidedSuccess(player.level().isClientSide);
        }
        return super.interactLivingEntity(stack, player, target, hand);
    }
}
