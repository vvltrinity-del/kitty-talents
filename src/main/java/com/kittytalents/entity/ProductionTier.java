package com.kittytalents.entity;

import net.minecraft.ChatFormatting;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Locale;
import java.util.function.Supplier;

public enum ProductionTier {
    STANDARD("standard", "Standard", 0xFFFFFF, ChatFormatting.WHITE, () -> new ItemStack(Items.STRING), 160),
    IRON("iron", "Iron", 0xE6DAC8, ChatFormatting.GRAY, () -> new ItemStack(Items.RAW_IRON), 240),
    COAL("coal", "Coal", 0x374151, ChatFormatting.DARK_GRAY, () -> new ItemStack(Items.COAL), 180),
    COPPER("copper", "Copper", 0xD97706, ChatFormatting.GOLD, () -> new ItemStack(Items.RAW_COPPER), 200),
    GOLD("gold", "Gold", 0xFACC15, ChatFormatting.YELLOW, () -> new ItemStack(Items.RAW_GOLD), 280),
    LAPIS("lapis", "Lapis", 0x1E40AF, ChatFormatting.BLUE, () -> new ItemStack(Items.LAPIS_LAZULI, 2), 260),
    REDSTONE("redstone", "Redstone", 0xDC2626, ChatFormatting.RED, () -> new ItemStack(Items.REDSTONE, 2), 260),
    EMERALD("emerald", "Emerald", 0x16A34A, ChatFormatting.GREEN, () -> new ItemStack(Items.EMERALD), 340),
    DIAMOND("diamond", "Diamond", 0x38BDF8, ChatFormatting.AQUA, () -> new ItemStack(Items.DIAMOND), 420),
    NETHERITE("netherite", "Netherite", 0x581C87, ChatFormatting.DARK_PURPLE, () -> new ItemStack(Items.NETHERITE_SCRAP), 600);

    private final String id;
    private final String displayName;
    private final int colorValue;
    private final ChatFormatting formatting;
    private final Supplier<ItemStack> productionSupplier;
    private final int baseIntervalTicks;

    ProductionTier(String id, String displayName, int colorValue, ChatFormatting formatting, Supplier<ItemStack> productionSupplier, int baseIntervalTicks) {
        this.id = id;
        this.displayName = displayName;
        this.colorValue = colorValue;
        this.formatting = formatting;
        this.productionSupplier = productionSupplier;
        this.baseIntervalTicks = baseIntervalTicks;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public int getColorValue() { return colorValue; }
    public ChatFormatting getFormatting() { return formatting; }
    public int getBaseIntervalTicks() { return baseIntervalTicks; }

    public ItemStack getProductionResult() {
        return productionSupplier.get().copy();
    }

    public static ProductionTier fromId(String id) {
        if (id == null) return STANDARD;
        String lower = id.toLowerCase(Locale.ROOT);
        for (ProductionTier tier : values()) {
            if (tier.id.equals(lower) || tier.name().toLowerCase(Locale.ROOT).equals(lower)) {
                return tier;
            }
        }
        return STANDARD;
    }
}
