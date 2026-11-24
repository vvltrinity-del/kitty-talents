package com.kittytalents.item;

import com.kittytalents.entity.TalentedCat;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CatCollarItem extends Item {
    public CatCollarItem(Properties properties) {
        super(properties);
    }

    public static DyeColor getColor(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains("CollarColor")) {
            return DyeColor.byName(stack.getTag().getString("CollarColor"), DyeColor.RED);
        }
        return DyeColor.RED;
    }

    public static void setColor(ItemStack stack, DyeColor color) {
        stack.getOrCreateTag().putString("CollarColor", color.getName());
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (target instanceof TalentedCat cat && com.kittytalents.core.perm.CatPermissions.can(player, cat, com.kittytalents.core.perm.CatAction.STYLE)) {
            DyeColor color = getColor(stack);
            cat.setCollarColor(color);
            cat.playSound(SoundEvents.ARMOR_EQUIP_LEATHER, 1.0F, 1.2F);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            if (!player.level().isClientSide) {
                player.displayClientMessage(Component.literal("Equipped " + color.getName() + " collar on " + cat.getName().getString() + "!").withStyle(ChatFormatting.GOLD), true);
            }
            return InteractionResult.sidedSuccess(player.level().isClientSide);
        }
        return super.interactLivingEntity(stack, player, target, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        DyeColor color = getColor(stack);
        tooltip.add(Component.literal("Color: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(color.getName().toUpperCase()).withStyle(ChatFormatting.YELLOW)));
        tooltip.add(Component.literal("Right-Click your cat to equip this collar.").withStyle(ChatFormatting.DARK_GRAY));
    }
}
