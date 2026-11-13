package com.kittytalents.entity;

import net.minecraft.ChatFormatting;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Locale;
import java.util.function.Supplier;

/**
 * Production tiers IN PROGRESSION ORDER (Standard -> Coal -> Copper -> Iron -> ... -> Netherite). Ordinal comparisons
 * and statStep() rely on this order. Saves use the id string, never the ordinal, so reordering is safe.
 * (Before 2026-11-08 IRON was listed second, which made Coal "outrank" Iron in every ordinal comparison.)
 */
public enum ProductionTier {
    STANDARD("standard", "Standard", 0xFFFFFF, ChatFormatting.WHITE, () -> new ItemStack(Items.STRING), 160),
    COAL("coal", "Coal", 0x374151, ChatFormatting.DARK_GRAY, () -> new ItemStack(Items.COAL), 180),
    COPPER("copper", "Copper", 0xD97706, ChatFormatting.GOLD, () -> new ItemStack(Items.RAW_COPPER), 200),
    IRON("iron", "Iron", 0xE6DAC8, ChatFormatting.GRAY, () -> new ItemStack(Items.RAW_IRON), 240),
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

    /**
     * Combat stat bonus per tier step (Standard 0 ... Netherite 9), so a higher-tier cat out-fights a lower one with the
     * same level and talents, and a level-100 cat still has tiers to climb. Netherite: +36 health, +9 armor,
     * +4.5 toughness, +6.75 attack damage.
     */
    public int statStep() { return ordinal(); }
    public double bonusHealth() { return statStep() * 4.0D; }
    public double bonusArmor() { return statStep() * 1.0D; }
    public double bonusToughness() { return statStep() * 0.5D; }
    public double bonusDamage() { return statStep() * 0.75D; }
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

    /** Ore tiers (the ones Mole Cat boosts): these Habitat cats can condense Ore Essence (5.0). */
    public boolean isMetal() {
        return this == COPPER || this == IRON || this == GOLD || this == DIAMOND || this == NETHERITE;
    }
}
