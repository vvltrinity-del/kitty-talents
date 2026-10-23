package com.kittytalents.item;

import com.kittytalents.entity.TalentedCat;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class FelineWrapItem extends Item {
    public FelineWrapItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (target instanceof TalentedCat cat && (cat.isOwnedBy(player) || cat.isTrusted(player))) {
            if (cat.getHealth() < cat.getMaxHealth() || cat.hasEffect(MobEffects.POISON) || cat.hasEffect(MobEffects.WITHER)) {
                if (!player.level().isClientSide) {
                    cat.heal(10.0F);
                    cat.removeEffect(MobEffects.POISON);
                    cat.removeEffect(MobEffects.WITHER);
                    cat.playSound(SoundEvents.ARMOR_EQUIP_LEATHER, 1.0F, 1.2F);
                    cat.playSound(SoundEvents.CAT_PURREOW, 0.9F, 1.3F);

                    if (cat.level() instanceof ServerLevel serverLevel) {
                        for (int i = 0; i < 6; i++) {
                            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                                    cat.getX() + (serverLevel.random.nextDouble() - 0.5D),
                                    cat.getY() + 0.5D,
                                    cat.getZ() + (serverLevel.random.nextDouble() - 0.5D),
                                    1, 0, 0, 0, 0);
                        }
                    }

                    if (!player.getAbilities().instabuild) stack.shrink(1);
                    player.displayClientMessage(Component.literal("Tended wounds on " + cat.getName().getString() + "!").withStyle(ChatFormatting.GREEN), true);
                }
                return InteractionResult.sidedSuccess(player.level().isClientSide);
            }
        }
        return super.interactLivingEntity(stack, player, target, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Sterile herbal feline bandage wrap.").withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.literal("Heals 10 HP and cleanses Poison & Wither.").withStyle(ChatFormatting.GRAY));
    }
}
