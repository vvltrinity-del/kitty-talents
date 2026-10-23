package com.kittytalents.item;

import com.kittytalents.entity.TalentedCat;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CatnipTreatBagItem extends Item {
    public CatnipTreatBagItem(Properties properties) {
        super(properties);
    }

    public int getStoredTreats(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains("StoredTreats")) {
            return stack.getTag().getInt("StoredTreats");
        }
        return 0;
    }

    public void setStoredTreats(ItemStack stack, int count) {
        stack.getOrCreateTag().putInt("StoredTreats", Math.max(0, Math.min(64 * 9, count)));
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (target instanceof TalentedCat cat && cat.isOwnedBy(player) && !cat.isIncapacitated()) {
            int treats = getStoredTreats(stack);
            if (treats > 0 && cat.getHunger() < 120) {
                setStoredTreats(stack, treats - 1);
                cat.increaseHunger(25);
                cat.heal(4.0F);
                cat.playSound(SoundEvents.CAT_EAT, 1.0F, 1.0F);
                player.displayClientMessage(Component.literal("Fed " + cat.getName().getString() + " from treat bag! (" + (treats - 1) + " left)").withStyle(ChatFormatting.GREEN), true);
                return InteractionResult.sidedSuccess(player.level().isClientSide);
            }
        }
        return super.interactLivingEntity(stack, player, target, hand);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        // Deposit treats from inventory into bag
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack invStack = player.getInventory().getItem(i);
            if (invStack.getItem() instanceof CatTreatItem) {
                count += invStack.getCount();
                invStack.setCount(0);
            }
        }
        if (count > 0) {
            setStoredTreats(held, getStoredTreats(held) + count);
            player.playSound(SoundEvents.ARMOR_EQUIP_LEATHER, 1.0F, 1.0F);
            player.displayClientMessage(Component.literal("Stored " + count + " treats in bag! Total: " + getStoredTreats(held)).withStyle(ChatFormatting.GOLD), true);
            return InteractionResultHolder.sidedSuccess(held, level.isClientSide);
        }
        return super.use(level, player, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        int treats = getStoredTreats(stack);
        tooltip.add(Component.literal("Stored Treats: " + treats + " / " + (64 * 9)).withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("Right-Click air: Deposit treats from inventory").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Right-Click cat: Feed treat from bag").withStyle(ChatFormatting.AQUA));
    }
}
