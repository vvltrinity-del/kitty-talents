package com.kittytalents.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ProductivityUpgradeItem extends Item {
    public enum Tier {
        BASIC("Basic", 1, ChatFormatting.WHITE, Rarity.COMMON),
        IMPROVED("Improved", 2, ChatFormatting.GREEN, Rarity.UNCOMMON),
        ADVANCED("Advanced", 3, ChatFormatting.AQUA, Rarity.UNCOMMON),
        SUPERIOR("Superior", 4, ChatFormatting.GOLD, Rarity.RARE),
        MASTER("Master", 5, ChatFormatting.LIGHT_PURPLE, Rarity.RARE),
        DIRE("Dire", 6, ChatFormatting.DARK_PURPLE, Rarity.EPIC);

        public final String name;
        public final int level;
        public final ChatFormatting color;
        public final Rarity rarity;

        Tier(String name, int level, ChatFormatting color, Rarity rarity) {
            this.name = name;
            this.level = level;
            this.color = color;
            this.rarity = rarity;
        }
    }

    private final Tier tier;

    public ProductivityUpgradeItem(Properties properties, Tier tier) {
        super(properties.rarity(tier.rarity));
        this.tier = tier;
    }

    public Tier getTier() {
        return tier;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Tier: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(tier.name).withStyle(tier.color)));
        tooltip.add(Component.literal("Increases Kitty Habitat production speed & resource yield.").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("Stackable up to 4 upgrades per Habitat.").withStyle(ChatFormatting.BLUE));
    }
}
