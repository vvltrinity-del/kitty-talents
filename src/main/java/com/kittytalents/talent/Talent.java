package com.kittytalents.talent;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

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

    public Talent(String id, int maxLevel, Supplier<ItemStack> iconSupplier) {
        this(id, maxLevel, iconSupplier, false, false, TalentCategory.UTILITY);
    }

    public Talent(String id, int maxLevel, Supplier<ItemStack> iconSupplier, boolean requiresDireTreat) {
        this(id, maxLevel, iconSupplier, requiresDireTreat, false, requiresDireTreat ? TalentCategory.DIRE : TalentCategory.UTILITY);
    }

    public Talent(String id, int maxLevel, Supplier<ItemStack> iconSupplier, boolean requiresDireTreat, boolean standard) {
        this(id, maxLevel, iconSupplier, requiresDireTreat, standard, requiresDireTreat ? TalentCategory.DIRE : TalentCategory.UTILITY);
    }

    public Talent(String id, int maxLevel, Supplier<ItemStack> iconSupplier, boolean requiresDireTreat, boolean standard, TalentCategory category) {
        this.id = id;
        this.maxLevel = maxLevel;
        this.iconSupplier = iconSupplier;
        this.requiresDireTreat = requiresDireTreat;
        this.standard = standard;
        this.category = category != null ? category : (requiresDireTreat ? TalentCategory.DIRE : TalentCategory.UTILITY);
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
