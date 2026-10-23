package com.kittytalents.item;

import com.kittytalents.registry.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Item representation for the Cat Food Bowl block.
 * Holds 5 stacks of feline food (fish, meats, sushi, catnip cookies, deep fried chicken).
 */
public class ItemFoodBowl extends BlockItem {

    public ItemFoodBowl(Block block, Properties properties) {
        super(block, properties);
    }

    public ItemFoodBowl(Properties properties) {
        super(ModBlocks.CAT_FOOD_BOWL.get(), properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Capacity: Exactly 5 Food Stacks").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("Nearby hungry or injured cats navigate here to eat.").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Accepts fish, meats, sushi, cookies, and Deep Fried Chicken.").withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.literal("Sneak + Right-Click block with food for instant feeding.").withStyle(ChatFormatting.DARK_AQUA));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
