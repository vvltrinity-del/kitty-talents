package com.kittytalents.item;

import com.kittytalents.entity.TalentedCat;
import com.kittytalents.registry.ModItems;
import com.kittytalents.talent.Talents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class CatnipMilkBucketItem extends Item {
    public CatnipMilkBucketItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity interactionTarget, InteractionHand hand) {
        if (interactionTarget instanceof TalentedCat cat && cat.isOwnedBy(player)) {
            Level level = cat.level();
            if (!level.isClientSide) {
                if (!cat.canConsumeFrenzyMilk()) {
                    player.sendSystemMessage(Component.translatable("message.kittytalents.frenzy_cooldown"));
                    return InteractionResult.FAIL;
                }

                int frenzyLevel = cat.getTalentLevel(Talents.FRENZY_FOCUS);
                int durationTicks = 3600 + (frenzyLevel * 1200); // 3 to 6 minutes!

                cat.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, durationTicks, 2));
                cat.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, durationTicks, 3));
                cat.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, durationTicks, 4)); // +20 absorption hearts!
                cat.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, durationTicks, 2)); // Haste
                cat.addEffect(new MobEffectInstance(MobEffects.REGENERATION, durationTicks, 2));
                cat.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, durationTicks, 2));
                cat.addEffect(new MobEffectInstance(MobEffects.JUMP, durationTicks, 2));
                cat.addEffect(new MobEffectInstance(MobEffects.GLOWING, durationTicks, 0));

                cat.triggerFrenzy(durationTicks);
                cat.heal(20.0F);
                cat.increaseHunger(120);

                level.playSound(null, cat.blockPosition(), SoundEvents.CAT_PURREOW, SoundSource.PLAYERS, 1.2F, 1.5F);
                level.playSound(null, cat.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.8F, 1.6F);

                if (level instanceof ServerLevel serverLevel) {
                    for (int i = 0; i < 28; i++) {
                        serverLevel.sendParticles(
                                ParticleTypes.SOUL_FIRE_FLAME,
                                cat.getX() + (serverLevel.random.nextDouble() - 0.5D),
                                cat.getY() + 0.3D + (serverLevel.random.nextDouble() - 0.5D),
                                cat.getZ() + (serverLevel.random.nextDouble() - 0.5D),
                                1, 0, 0.1D, 0, 0.05D
                        );
                    }
                }

                player.sendSystemMessage(Component.translatable("message.kittytalents.cat_frenzy", cat.getName()));

                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                    ItemStack emptyBucket = new ItemStack(ModItems.CATNIP_BUCKET.get());
                    if (!player.getInventory().add(emptyBucket)) {
                        player.drop(emptyBucket, false);
                    }
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.interactLivingEntity(stack, player, interactionTarget, hand);
    }
}
