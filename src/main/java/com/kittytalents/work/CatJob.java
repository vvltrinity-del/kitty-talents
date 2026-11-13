package com.kittytalents.work;

import com.kittytalents.compat.tacz.TacZCompat;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.talent.Talent;
import com.kittytalents.talent.Talents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.ai.goal.Goal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The jobs a cat can do in WORK mode. Saved by {@link #getId()}.
 *
 * <p>4.0 Phase 8: this is a registry, not an enum. Addons add jobs with
 * {@code KittyTalentsAPI.registerJob(CatJob.builder("mymod_beekeeper", "Beekeeper")...build())}; the job's goal
 * (usually an {@link AbstractCatJobGoal}) is created for every cat from {@link #createGoal}. {@link #values()} lists
 * the built-in jobs in their fixed order, then addon jobs sorted by id, so {@link #ordinal()} matches on both sides.
 */
public final class CatJob {
    private static final List<CatJob> REGISTERED = new ArrayList<>();
    private static volatile CatJob[] snapshot = new CatJob[0];

    public static final CatJob NONE = register(new CatJob("NONE", true, "none", "Unemployed", "No active work assignment.", null, 0, 1, () -> new ItemStack(Items.BARRIER), false, null));
    public static final CatJob FARMER = register(new CatJob("FARMER", true, "farmer", "Farmer", "Harvests mature crops, replants seeds, bonemeals, and guards farmland.", Talents.GREEN_PAW, 1, 1, () -> new ItemStack(Items.WHEAT), false, com.kittytalents.work.jobs.FarmerJobGoal::new));
    public static final CatJob LUMBERJACK = register(new CatJob("LUMBERJACK", true, "lumberjack", "Lumberjack", "Fells natural trees top-down, collects logs, and replants saplings.", Talents.TIMBER_CLAWS, 1, 1, () -> new ItemStack(Items.IRON_AXE), false, com.kittytalents.work.jobs.LumberjackJobGoal::new));
    public static final CatJob MINER = register(new CatJob("MINER", true, "miner", "Miner", "Quarries stone and ores or digs 1x2 tunnels while placing torches.", Talents.KITTY_TOOLS, 1, 1, () -> new ItemStack(Items.DIAMOND_PICKAXE), false, com.kittytalents.work.jobs.MinerJobGoal::new));
    public static final CatJob RANCHER = register(new CatJob("RANCHER", true, "rancher", "Rancher", "Breeds livestock, shears sheep, and gathers animal products.", null, 0, 15, () -> new ItemStack(Items.SHEARS), false, com.kittytalents.work.jobs.RancherJobGoal::new));
    public static final CatJob COURIER = register(new CatJob("COURIER", true, "courier", "Courier", "Transfers filtered items between linked input and output containers.", Talents.LOGISTICS_WHISKERS, 1, 1, () -> new ItemStack(Items.CHEST_MINECART), false, com.kittytalents.work.jobs.CourierJobGoal::new));
    public static final CatJob SORTER = register(new CatJob("SORTER", true, "sorter", "Sorter", "Sorts items from input chests into matching categorized storage chests.", Talents.LOGISTICS_WHISKERS, 3, 10, () -> new ItemStack(Items.HOPPER), false, com.kittytalents.work.jobs.SorterJobGoal::new));
    public static final CatJob FISHER = register(new CatJob("FISHER", true, "fisher", "Fisher", "Fishes in open water within the work area and deposits catches.", Talents.FISHER_CAT, 1, 1, () -> new ItemStack(Items.FISHING_ROD), false, com.kittytalents.work.jobs.FisherJobGoal::new));
    public static final CatJob COLLECTOR = register(new CatJob("COLLECTOR", true, "collector", "Collector", "Vacuums dropped items in the work area into the work station buffer.", null, 0, 5, () -> new ItemStack(Items.ENDER_PEARL), false, com.kittytalents.work.jobs.CollectorJobGoal::new));
    public static final CatJob GUNSMITH = register(new CatJob("GUNSMITH", true, "gunsmith", "Gunsmith", "Crafts TacZ ammunition and attachments from raw materials at a station.", Talents.CAT_ARMORY, 2, 10, () -> new ItemStack(Items.GUNPOWDER), true, com.kittytalents.work.jobs.GunsmithJobGoal::new));

    private final String name;
    private final boolean builtin;
    private final String id;
    private final String displayName;
    private final String description;
    @Nullable
    private final Talent requiredTalent;
    private final int minTalentRank;
    private final int minCatLevel;
    private final Supplier<ItemStack> iconSupplier;
    private final boolean requiresTacZ;
    @Nullable
    private final Function<TalentedCat, Goal> goalFactory;

    private CatJob(String name, boolean builtin, String id, String displayName, String description, @Nullable Talent requiredTalent,
           int minTalentRank, int minCatLevel, Supplier<ItemStack> iconSupplier, boolean requiresTacZ,
           @Nullable Function<TalentedCat, Goal> goalFactory) {
        this.name = name;
        this.builtin = builtin;
        this.goalFactory = goalFactory;
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.requiredTalent = requiredTalent;
        this.minTalentRank = minTalentRank;
        this.minCatLevel = minCatLevel;
        this.iconSupplier = iconSupplier;
        this.requiresTacZ = requiresTacZ;
    }

    /**
     * Adds a job to the registry. Addons go through {@code KittyTalentsAPI.registerJob}.
     *
     * @throws IllegalArgumentException if a job with this id already exists
     */
    public static synchronized CatJob register(CatJob job) {
        for (CatJob j : REGISTERED) {
            if (j.id.equals(job.id) || j.name.equals(job.name)) throw new IllegalArgumentException("Kitty Talents: a job with id '" + job.id + "' already exists");
        }
        REGISTERED.add(job);
        List<CatJob> sorted = new ArrayList<>(REGISTERED);
        sorted.sort(Comparator.<CatJob, Boolean>comparing(j -> !j.builtin).thenComparing(j -> j.builtin ? "" : j.id));
        snapshot = sorted.toArray(new CatJob[0]);
        return job;
    }

    /** Every registered job: built-ins in their fixed order (NONE first), then addon jobs by id. */
    public static CatJob[] values() { return snapshot.clone(); }

    /** Position in {@link #values()}. Packets and synced data only; saves use {@link #getId()}. */
    public int ordinal() {
        CatJob[] all = snapshot;
        for (int i = 0; i < all.length; i++) if (all[i] == this) return i;
        return 0;
    }

    public String name() { return name; }
    public boolean isBuiltin() { return builtin; }
    @Override public String toString() { return name; }

    /** The goal that does this job, made once per cat when its AI is built; null for a job without one (NONE). */
    @Nullable
    public Goal createGoal(TalentedCat cat) { return goalFactory != null ? goalFactory.apply(cat) : null; }

    /** Starts an addon job. {@code id} is your mod id plus a name ("mymod_beekeeper"). */
    public static Builder builder(String id, String displayName) { return new Builder(id, displayName); }

    /** Describes an addon job. Defaults: no talent needed, cat level 1, a paper icon. */
    public static final class Builder {
        private final String id, displayName;
        private String description = "";
        @Nullable private Talent requiredTalent;
        private int minTalentRank, minCatLevel = 1;
        private Supplier<ItemStack> icon = () -> new ItemStack(Items.PAPER);
        private boolean requiresTacZ;
        @Nullable private Function<TalentedCat, Goal> goalFactory;

        private Builder(String id, String displayName) {
            if (id == null || !id.matches("[a-z0-9_]+")) throw new IllegalArgumentException("Kitty Talents: job ids use a-z, 0-9 and _: " + id);
            this.id = id;
            this.displayName = displayName;
        }

        public Builder description(String description) { this.description = description; return this; }
        /** The cat needs this talent at this rank to do the job. */
        public Builder requiresTalent(Talent talent, int minRank) { this.requiredTalent = talent; this.minTalentRank = minRank; return this; }
        public Builder minCatLevel(int level) { this.minCatLevel = level; return this; }
        public Builder icon(Supplier<ItemStack> icon) { this.icon = icon; return this; }
        public Builder requiresTacZ() { this.requiresTacZ = true; return this; }
        /** The goal that does the work, created once per cat. Extend {@link AbstractCatJobGoal}. */
        public Builder goal(Function<TalentedCat, Goal> factory) { this.goalFactory = factory; return this; }

        public CatJob build() {
            if (goalFactory == null) throw new IllegalArgumentException("Kitty Talents: job '" + id + "' needs a goal");
            return new CatJob(id.toUpperCase(Locale.ROOT), false, id, displayName, description, requiredTalent, minTalentRank,
                    minCatLevel, icon, requiresTacZ, goalFactory);
        }
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
        for (CatJob job : snapshot) {
            if (job.id.equals(lower) || job.name.toLowerCase(Locale.ROOT).equals(lower)) {
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
