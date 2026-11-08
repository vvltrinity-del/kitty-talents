package com.kittytalents.item;

import com.kittytalents.entity.TalentedCat;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SpiritCatnipItem extends Item {
    public SpiritCatnipItem(Properties properties) {
        super(properties.rarity(Rarity.EPIC));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (target instanceof TalentedCat cat && cat.isOwnedBy(player)) {
            if (!cat.isInLove() && cat.getAge() == 0) {
                if (!player.level().isClientSide) {
                    cat.setInLove(player);
                    cat.setSpiritCatnipPrimed(true);
                    if (!player.getAbilities().instabuild) stack.shrink(1);
                    cat.playSound(SoundEvents.TOTEM_USE, 0.7F, 1.6F);
                    cat.playSound(SoundEvents.CAT_PURREOW, 1.0F, 1.2F);

                    if (cat.level() instanceof ServerLevel serverLevel) {
                        for (int i = 0; i < 15; i++) {
                            serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                                    cat.getX() + (serverLevel.random.nextDouble() - 0.5D),
                                    cat.getY() + 0.5D + (serverLevel.random.nextDouble() - 0.5D),
                                    cat.getZ() + (serverLevel.random.nextDouble() - 0.5D),
                                    1, 0, 0.05D, 0, 0.02D);
                            serverLevel.sendParticles(ParticleTypes.HEART,
                                    cat.getX() + (serverLevel.random.nextDouble() - 0.5D),
                                    cat.getY() + 0.6D + (serverLevel.random.nextDouble() - 0.5D),
                                    cat.getZ() + (serverLevel.random.nextDouble() - 0.5D),
                                    1, 0, 0, 0, 0);
                        }
                    }
                    player.displayClientMessage(Component.literal("✦ " + cat.getName().getString() + " is imbued with spiritual essence! Guaranteed Nekomata offspring! ✦").withStyle(ChatFormatting.AQUA), true);
                }
                return InteractionResult.sidedSuccess(player.level().isClientSide);
            }
        }
        return super.interactLivingEntity(stack, player, target, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Mystical spiritual catnip infused with ethereal soul essence.").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("Breeding Catalyst: Guarantees 100% Nekomata twin-tailed mutation on offspring.").withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
