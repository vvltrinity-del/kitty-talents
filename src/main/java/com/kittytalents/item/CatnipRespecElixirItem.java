package com.kittytalents.item;

import com.kittytalents.entity.TalentedCat;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;

public class CatnipRespecElixirItem extends Item {
    public CatnipRespecElixirItem(Properties properties) {
        super(properties.rarity(Rarity.UNCOMMON).stacksTo(16));
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity interactionTarget, InteractionHand hand) {
        if (interactionTarget instanceof TalentedCat cat && com.kittytalents.core.perm.CatPermissions.can(player, cat, com.kittytalents.core.perm.CatAction.RESPEC)) {
            Level level = cat.level();
            if (!level.isClientSide) {
                int refunded = cat.respecTalents();
                player.sendSystemMessage(Component.translatable("message.kittytalents.respec_success", cat.getName(), refunded));
                level.playSound(null, cat.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 1.2F);

                if (level instanceof ServerLevel serverLevel) {
                    for (int i = 0; i < 20; i++) {
                        serverLevel.sendParticles(ParticleTypes.ENCHANT, cat.getX(), cat.getY() + 0.5D, cat.getZ(), 1,
                                (serverLevel.random.nextDouble() - 0.5D) * 0.8D, 0.5D, (serverLevel.random.nextDouble() - 0.5D) * 0.8D, 0.1D);
                    }
                }

                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.interactLivingEntity(stack, player, interactionTarget, hand);
    }
}
