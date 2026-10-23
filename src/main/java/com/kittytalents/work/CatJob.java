package com.kittytalents.work;

import com.kittytalents.compat.TacZCompat;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.talent.Talent;
import com.kittytalents.talent.Talents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.function.Supplier;

public enum CatJob {
    NONE("none", "Unemployed", "No active work assignment.", null, 0, 1, () -> new ItemStack(Items.BARRIER), false),
    FARMER("farmer", "Farmer", "Harvests mature crops, replants seeds, bonemeals, and guards farmland.", Talents.GREEN_PAW, 1, 1, () -> new ItemStack(Items.WHEAT), false),
    LUMBERJACK("lumberjack", "Lumberjack", "Fells natural trees top-down, collects logs, and replants saplings.", Talents.TIMBER_CLAWS, 1, 1, () -> new ItemStack(Items.IRON_AXE), false),
    MINER("miner", "Miner", "Quarries stone and ores or digs 1x2 tunnels while placing torches.", Talents.MOLE_CAT, 1, 1, () -> new ItemStack(Items.DIAMOND_PICKAXE), false),
    RANCHER("rancher", "Rancher", "Breeds livestock, shears sheep, and gathers animal products.", null, 0, 15, () -> new ItemStack(Items.SHEARS), false),
    COURIER("courier", "Courier", "Transfers filtered items between linked input and output containers.", Talents.LOGISTICS_WHISKERS, 1, 1, () -> new ItemStack(Items.CHEST_MINECART), false),
    SORTER("sorter", "Sorter", "Sorts items from input chests into matching categorized storage chests.", Talents.LOGISTICS_WHISKERS, 3, 10, () -> new ItemStack(Items.HOPPER), false),
    FISHER("fisher", "Fisher", "Fishes in open water within the work area and deposits catches.", Talents.FISHER_CAT, 1, 1, () -> new ItemStack(Items.FISHING_ROD), false),
    COLLECTOR("collector", "Collector", "Vacuums dropped items in the work area into the work station buffer.", null, 0, 5, () -> new ItemStack(Items.ENDER_PEARL), false),
    GUNSMITH("gunsmith", "Gunsmith", "Crafts TacZ ammunition and attachments from raw materials at a station.", Talents.CAT_ARMORY, 2, 10, () -> new ItemStack(Items.GUNPOWDER), true);

    private final String id;
    private final String displayName;
    private final String description;
    @Nullable
    private final Talent requiredTalent;
    private final int minTalentRank;
    private final int minCatLevel;
    private final Supplier<ItemStack> iconSupplier;
    private final boolean requiresTacZ;

    CatJob(String id, String displayName, String description, @Nullable Talent requiredTalent,
           int minTalentRank, int minCatLevel, Supplier<ItemStack> iconSupplier, boolean requiresTacZ) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.requiredTalent = requiredTalent;
        this.minTalentRank = minTalentRank;
        this.minCatLevel = minCatLevel;
        this.iconSupplier = iconSupplier;
        this.requiresTacZ = requiresTacZ;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    @Nullable
    public Talent getRequiredTalent() { return requiredTalent; }
    public int getMinTalentRank() { return minTalentRank; }
    public int getMinCatLevel() { return minCatLevel; }
    public ItemStack getIcon() { return iconSupplier.get(); }
    public boolean requiresTacZ() { return requiresTacZ; }

    public Component getTitleComponent() {
        return Component.translatable("job.kittytalents." + id);
    }

    public boolean isAvailable() {
        if (requiresTacZ && !TacZCompat.isLoaded()) return false;
        return true;
    }

    public boolean canCatPerform(TalentedCat cat) {
        if (this == NONE) return true;
        if (!isAvailable()) return false;
        if (cat.getCatLevel() < minCatLevel) return false;
        if (requiredTalent != null && cat.getTalentLevel(requiredTalent) < minTalentRank) return false;
        return true;
    }

    public static CatJob fromId(String id) {
        if (id == null || id.isEmpty()) return NONE;
        String lower = id.toLowerCase(Locale.ROOT);
        for (CatJob job : values()) {
            if (job.id.equals(lower) || job.name().toLowerCase(Locale.ROOT).equals(lower)) {
                return job;
            }
        }
        return NONE;
    }

    public static CatJob byOrdinal(int ord) {
        CatJob[] vals = values();
        if (ord < 0 || ord >= vals.length) return NONE;
        return vals[ord];
    }
}
