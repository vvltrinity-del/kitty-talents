package com.kittytalents.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Kitty Habitat upgrade (5.1): raw ores the worker produces come out smelted, the way a furnace would smelt them
 * (raw iron -> iron ingot), modded ores included. Takes one upgrade slot; no fuel. See
 * {@link com.kittytalents.block.KittyHabitatBlockEntity#smelted}.
 */
public class SmelterUpgradeItem extends Item {
    public SmelterUpgradeItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.kittytalents.upgrade_smelter.tooltip").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.kittytalents.upgrade_smelter.tooltip2").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("item.kittytalents.upgrade_smelter.tooltip3").withStyle(ChatFormatting.BLUE));
    }
}
