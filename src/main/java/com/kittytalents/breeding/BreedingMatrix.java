package com.kittytalents.breeding;

import com.kittytalents.entity.CatVariant;
import com.kittytalents.entity.ProductionTier;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Immutable breeding matrix mapping deterministic parent pairings to offspring outcomes.
 * Strictly guarantees ZERO hidden random outcomes for production tier evolution.
 */
public final class BreedingMatrix {

    private static final Map<String, ProductionTier> TIER_EVOLUTION_MAP = new HashMap<>();
    private static final Map<String, CatVariant> BREED_CROSSING_MAP = new HashMap<>();

    static {
        // === DETERMINISTIC PRODUCTION TIER PROGRESSION ===
        // Standard -> Coal -> Copper -> Iron -> Gold -> Lapis -> Redstone -> Emerald -> Diamond -> Netherite
        registerTierEvolution(ProductionTier.STANDARD, ProductionTier.STANDARD, ProductionTier.COAL);
        registerTierEvolution(ProductionTier.COAL, ProductionTier.COAL, ProductionTier.COPPER);
        registerTierEvolution(ProductionTier.COPPER, ProductionTier.COPPER, ProductionTier.IRON);
        registerTierEvolution(ProductionTier.IRON, ProductionTier.IRON, ProductionTier.GOLD);
        registerTierEvolution(ProductionTier.GOLD, ProductionTier.GOLD, ProductionTier.LAPIS);
        registerTierEvolution(ProductionTier.LAPIS, ProductionTier.LAPIS, ProductionTier.REDSTONE);
        registerTierEvolution(ProductionTier.REDSTONE, ProductionTier.REDSTONE, ProductionTier.EMERALD);
        registerTierEvolution(ProductionTier.EMERALD, ProductionTier.EMERALD, ProductionTier.DIAMOND);
        registerTierEvolution(ProductionTier.DIAMOND, ProductionTier.DIAMOND, ProductionTier.NETHERITE);
        registerTierEvolution(ProductionTier.NETHERITE, ProductionTier.NETHERITE, ProductionTier.NETHERITE);

        // Cross-Tier deterministic stepped progressions
        registerTierEvolution(ProductionTier.STANDARD, ProductionTier.COAL, ProductionTier.COAL);
        registerTierEvolution(ProductionTier.COAL, ProductionTier.COPPER, ProductionTier.COPPER);
        registerTierEvolution(ProductionTier.COPPER, ProductionTier.IRON, ProductionTier.IRON);
        registerTierEvolution(ProductionTier.IRON, ProductionTier.GOLD, ProductionTier.GOLD);
        registerTierEvolution(ProductionTier.GOLD, ProductionTier.LAPIS, ProductionTier.LAPIS);
        registerTierEvolution(ProductionTier.LAPIS, ProductionTier.REDSTONE, ProductionTier.REDSTONE);
        registerTierEvolution(ProductionTier.REDSTONE, ProductionTier.EMERALD, ProductionTier.EMERALD);
        registerTierEvolution(ProductionTier.EMERALD, ProductionTier.DIAMOND, ProductionTier.DIAMOND);
        registerTierEvolution(ProductionTier.DIAMOND, ProductionTier.NETHERITE, ProductionTier.NETHERITE);

        // === DETERMINISTIC BREED CROSSING RECIPES ===
        // Nature & Harvest Breeds
        registerBreed(CatVariant.TABBY, CatVariant.CALICO, CatVariant.CROP);
        registerBreed(CatVariant.SIAMESE, CatVariant.JELLIE, CatVariant.FISH);
        registerBreed(CatVariant.BRITISH_SHORTHAIR, CatVariant.TUXEDO, CatVariant.WOOD);
        registerBreed(CatVariant.PERSIAN, CatVariant.RAGDOLL, CatVariant.MINT);

        // Tier 1 Big Cats & Companion Crosses
        registerBreed(CatVariant.TABBY, CatVariant.ALL_BLACK, CatVariant.PANTHER);
        registerBreed(CatVariant.WHITE, CatVariant.TABBY, CatVariant.SNOW_LEOPARD);
        registerBreed(CatVariant.RED, CatVariant.ALL_BLACK, CatVariant.TIGER);
        registerBreed(CatVariant.TUXEDO, CatVariant.CALICO, CatVariant.BRITISH_SHORTHAIR);
        registerBreed(CatVariant.SIAMESE, CatVariant.WHITE, CatVariant.RAGDOLL);
        registerBreed(CatVariant.PERSIAN, CatVariant.JELLIE, CatVariant.CALICO);

        // Advanced Mythical Crosses
        registerBreed(CatVariant.PANTHER, CatVariant.ALL_BLACK, CatVariant.VOID_ENDER);
        registerBreed(CatVariant.TIGER, CatVariant.RED, CatVariant.GOLDEN_MANEKI);
        registerBreed(CatVariant.SNOW_LEOPARD, CatVariant.WHITE, CatVariant.GHOST_PHANTOM);

        // Productive Crossbreeding
        registerBreed(CatVariant.ALL_BLACK, CatVariant.TUXEDO, CatVariant.COAL);
        registerBreed(CatVariant.MINT, CatVariant.COAL, CatVariant.COPPER);
        registerBreed(CatVariant.COPPER, CatVariant.COAL, CatVariant.IRON);
        registerBreed(CatVariant.IRON, CatVariant.RED, CatVariant.REDSTONE);
        registerBreed(CatVariant.IRON, CatVariant.SIAMESE, CatVariant.LAPIS);
        registerBreed(CatVariant.IRON, CatVariant.GOLDEN_MANEKI, CatVariant.GOLD);
        registerBreed(CatVariant.LAPIS, CatVariant.WHITE, CatVariant.QUARTZ);
        registerBreed(CatVariant.GOLD, CatVariant.CALICO, CatVariant.EMERALD);
        registerBreed(CatVariant.QUARTZ, CatVariant.LAPIS, CatVariant.DIAMOND);

        // Alloys & Legendary Productives
        registerBreed(CatVariant.IRON, CatVariant.COAL, CatVariant.STEEL);
        registerBreed(CatVariant.COPPER, CatVariant.IRON, CatVariant.INDUSTRIAL);
        registerBreed(CatVariant.GOLD, CatVariant.EMERALD, CatVariant.TREASURE);
        registerBreed(CatVariant.DIAMOND, CatVariant.VOID_ENDER, CatVariant.NETHERITE);
        registerBreed(CatVariant.TREASURE, CatVariant.NETHERITE, CatVariant.TREASURE);
    }

    private static void registerTierEvolution(ProductionTier parentA, ProductionTier parentB, ProductionTier outcome) {
        TIER_EVOLUTION_MAP.put(getTierPairKey(parentA, parentB), outcome);
    }

    private static void registerBreed(CatVariant parentA, CatVariant parentB, CatVariant outcome) {
        BREED_CROSSING_MAP.put(getBreedPairKey(parentA, parentB), outcome);
    }

    public static String getTierPairKey(ProductionTier a, ProductionTier b) {
        if (a == null) a = ProductionTier.STANDARD;
        if (b == null) b = ProductionTier.STANDARD;
        return a.ordinal() <= b.ordinal() ? a.getId() + "+" + b.getId() : b.getId() + "+" + a.getId();
    }

    public static String getBreedPairKey(CatVariant a, CatVariant b) {
        if (a == null) a = CatVariant.TABBY;
        if (b == null) b = CatVariant.TABBY;
        return a.ordinal() <= b.ordinal() ? a.getId() + "+" + b.getId() : b.getId() + "+" + a.getId();
    }

    public static ProductionTier lookupTierEvolution(ProductionTier parentA, ProductionTier parentB) {
        String key = getTierPairKey(parentA, parentB);
        return TIER_EVOLUTION_MAP.get(key);
    }

    public static CatVariant lookupBreedOutcome(CatVariant parentA, CatVariant parentB) {
        String key = getBreedPairKey(parentA, parentB);
        return BREED_CROSSING_MAP.get(key);
    }

    public static Map<String, ProductionTier> getAllTierEvolutions() {
        return Collections.unmodifiableMap(TIER_EVOLUTION_MAP);
    }

    public static Map<String, CatVariant> getAllBreedOutcomes() {
        return Collections.unmodifiableMap(BREED_CROSSING_MAP);
    }

    private BreedingMatrix() {}
}
