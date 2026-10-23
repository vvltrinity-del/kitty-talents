package com.kittytalents.item;

import com.kittytalents.entity.TalentedCat;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AmnesiaTreatItem extends Item {
    public AmnesiaTreatItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (target instanceof TalentedCat cat && cat.isOwnedBy(player)) {
            if (!player.level().isClientSide) {
                cat.resetStatistics();
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                cat.playSound(SoundEvents.PLAYER_BURP, 1.0F, 1.0F);
                player.sendSystemMessage(Component.literal("Reset companion statistics for " + cat.getName().getString() + "!").withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            return InteractionResult.sidedSuccess(player.level().isClientSide);
        }
        return super.interactLivingEntity(stack, player, target, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Clears all combat and activity statistics for a Talented Cat.").withStyle(ChatFormatting.GRAY));
    }
}
