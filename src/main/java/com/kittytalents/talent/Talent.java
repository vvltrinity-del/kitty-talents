package com.kittytalents.talent;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fml.ModList;

import java.util.function.Supplier;

/**
 * Feline Talent definition for Kitty Talents 3.0 (R8).
 */
public class Talent {
    private final String id;
    private final int maxLevel;
    private final Supplier<ItemStack> iconSupplier;
    private final boolean requiresDireTreat;
    private final boolean standard;
    private final TalentCategory category;
    private final String requiredModId;

    public Talent(String id, int maxLevel, Supplier<ItemStack> iconSupplier) {
        this(id, maxLevel, iconSupplier, false, false, TalentCategory.UTILITY, null);
    }

    public Talent(String id, int maxLevel, Supplier<ItemStack> iconSupplier, boolean requiresDireTreat) {
        this(id, maxLevel, iconSupplier, requiresDireTreat, false, requiresDireTreat ? TalentCategory.DIRE : TalentCategory.UTILITY, null);
    }

    public Talent(String id, int maxLevel, Supplier<ItemStack> iconSupplier, boolean requiresDireTreat, boolean standard) {
        this(id, maxLevel, iconSupplier, requiresDireTreat, standard, requiresDireTreat ? TalentCategory.DIRE : TalentCategory.UTILITY, null);
    }

    public Talent(String id, int maxLevel, Supplier<ItemStack> iconSupplier, boolean requiresDireTreat, boolean standard, TalentCategory category) {
        this(id, maxLevel, iconSupplier, requiresDireTreat, standard, category, null);
    }

    public Talent(String id, int maxLevel, Supplier<ItemStack> iconSupplier, boolean requiresDireTreat, boolean standard, TalentCategory category, String requiredModId) {
        this.id = id;
        this.maxLevel = maxLevel;
        this.iconSupplier = iconSupplier;
        this.requiresDireTreat = requiresDireTreat;
        this.standard = standard;
        this.category = category != null ? category : (requiresDireTreat ? TalentCategory.DIRE : TalentCategory.UTILITY);
        this.requiredModId = requiredModId;
    }

    public boolean isStandard() {
        return standard;
    }

    public String getId() {
        return id;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public boolean requiresDireTreat() {
        return requiresDireTreat;
    }

    public boolean isDireTalent() {
        return requiresDireTreat;
    }

    public TalentCategory getCategory() {
        return category;
    }

    public String getRequiredModId() {
        return requiredModId;
    }

    public boolean isAvailable() {
        if (requiredModId == null || requiredModId.isEmpty()) return true;
        try {
            return ModList.get() != null && ModList.get().isLoaded(requiredModId);
        } catch (Throwable ignored) {
            return true;
        }
    }

    public Component getName() {
        return Component.translatable("talent.kittytalents." + id);
    }

    public String getDisplayName() {
        return getName().getString();
    }

    public Component getDescription() {
        return Component.translatable("talent.kittytalents." + id + ".desc");
    }

    public ItemStack getIcon() {
        return iconSupplier != null ? iconSupplier.get() : new ItemStack(Items.COD);
    }
}
