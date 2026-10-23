package com.kittytalents.entity;

import java.util.*;

public class CatBreedingManager {
    private static final Map<String, CatVariant> BREEDING_RECIPES = new HashMap<>();

    static {
        // Nature & Harvest Breeds from Wild Companions
        register(CatVariant.TABBY, CatVariant.CALICO, CatVariant.CROP);
        register(CatVariant.SIAMESE, CatVariant.JELLIE, CatVariant.FISH);
        register(CatVariant.BRITISH_SHORTHAIR, CatVariant.TUXEDO, CatVariant.WOOD);
        register(CatVariant.PERSIAN, CatVariant.RAGDOLL, CatVariant.MINT);

        // Tier 1 (Common + Common -> Advanced Big Cats)
        register(CatVariant.TABBY, CatVariant.ALL_BLACK, CatVariant.PANTHER);
        register(CatVariant.WHITE, CatVariant.TABBY, CatVariant.SNOW_LEOPARD);
        register(CatVariant.RED, CatVariant.ALL_BLACK, CatVariant.TIGER);
        register(CatVariant.TUXEDO, CatVariant.CALICO, CatVariant.BRITISH_SHORTHAIR);
        register(CatVariant.SIAMESE, CatVariant.WHITE, CatVariant.RAGDOLL);
        register(CatVariant.PERSIAN, CatVariant.JELLIE, CatVariant.CALICO);

        // Advanced + Advanced -> Rare Mythical Cats
        register(CatVariant.PANTHER, CatVariant.ALL_BLACK, CatVariant.VOID_ENDER);
        register(CatVariant.TIGER, CatVariant.RED, CatVariant.GOLDEN_MANEKI);
        register(CatVariant.SNOW_LEOPARD, CatVariant.WHITE, CatVariant.GHOST_PHANTOM);

        // Productive Variants Crossbreeding (Common -> Advanced -> Rare -> Legendary)
        register(CatVariant.ALL_BLACK, CatVariant.TUXEDO, CatVariant.COAL);
        register(CatVariant.MINT, CatVariant.COAL, CatVariant.COPPER);
        register(CatVariant.COPPER, CatVariant.COAL, CatVariant.IRON);
        register(CatVariant.IRON, CatVariant.RED, CatVariant.REDSTONE);
        register(CatVariant.IRON, CatVariant.SIAMESE, CatVariant.LAPIS);
        register(CatVariant.IRON, CatVariant.GOLDEN_MANEKI, CatVariant.GOLD);
        register(CatVariant.LAPIS, CatVariant.WHITE, CatVariant.QUARTZ);
        register(CatVariant.GOLD, CatVariant.CALICO, CatVariant.EMERALD);
        register(CatVariant.QUARTZ, CatVariant.LAPIS, CatVariant.DIAMOND);

        // Special Alloys & Legendary Productives
        // Iron + Coal -> Steel Cat
        register(CatVariant.IRON, CatVariant.COAL, CatVariant.STEEL);
        // Copper + Iron -> Industrial Cat
        register(CatVariant.COPPER, CatVariant.IRON, CatVariant.INDUSTRIAL);
        // Gold + Emerald -> Treasure Cat
        register(CatVariant.GOLD, CatVariant.EMERALD, CatVariant.TREASURE);
        // Diamond + Void Ender -> Netherite Cat
        register(CatVariant.DIAMOND, CatVariant.VOID_ENDER, CatVariant.NETHERITE);
        // Treasure + Netherite -> Treasure Cat
        register(CatVariant.TREASURE, CatVariant.NETHERITE, CatVariant.TREASURE);
    }

    private static void register(CatVariant parentA, CatVariant parentB, CatVariant result) {
        BREEDING_RECIPES.put(getKey(parentA, parentB), result);
    }

    private static String getKey(CatVariant a, CatVariant b) {
        return a.ordinal() < b.ordinal() ? a.getId() + "+" + b.getId() : b.getId() + "+" + a.getId();
    }

    public static CatVariant getChildVariant(CatVariant parentA, CatVariant parentB) {
        return getChildVariant(parentA, parentB, null);
    }

    /**
     * Completely deterministic breeding outcome.
     * ZERO random numbers generated.
     */
    public static CatVariant getChildVariant(CatVariant parentA, CatVariant parentB, ProductionTier childTier) {
        if (parentA == null && parentB == null) return CatVariant.TABBY;
        if (parentA == null) return parentB;
        if (parentB == null) return parentA;

        String key = getKey(parentA, parentB);
        if (BREEDING_RECIPES.containsKey(key)) {
            return BREEDING_RECIPES.get(key);
        }

        // Purebred same parents yield identical child
        if (parentA == parentB) {
            // If child tier advanced to a resource tier, allow the cat to express that resource variant
            if (childTier != null && childTier != ProductionTier.STANDARD) {
                CatVariant tierVariant = getVariantForTier(childTier);
                if (tierVariant != null && parentA.getRarity() == CatVariant.VariantRarity.COMMON) {
                    return tierVariant;
                }
            }
            return parentA;
        }

        // If child tier advanced from breeding (e.g. into COAL, COPPER, IRON, etc.)
        if (childTier != null && childTier != ProductionTier.STANDARD) {
            CatVariant tierVariant = getVariantForTier(childTier);
            if (tierVariant != null) {
                return tierVariant;
            }
        }

        // Deterministic default based on higher rarity or canonical ordering
        if (parentA.getRarity().ordinal() > parentB.getRarity().ordinal()) {
            return parentA;
        } else if (parentB.getRarity().ordinal() > parentA.getRarity().ordinal()) {
            return parentB;
        }
        return parentA.ordinal() < parentB.ordinal() ? parentA : parentB;
    }

    public static CatVariant getVariantForTier(ProductionTier tier) {
        if (tier == null) return null;
        return switch (tier) {
            case COAL -> CatVariant.COAL;
            case COPPER -> CatVariant.COPPER;
            case IRON -> CatVariant.IRON;
            case GOLD -> CatVariant.GOLD;
            case LAPIS -> CatVariant.LAPIS;
            case REDSTONE -> CatVariant.REDSTONE;
            case EMERALD -> CatVariant.EMERALD;
            case DIAMOND -> CatVariant.DIAMOND;
            case NETHERITE -> CatVariant.NETHERITE;
            default -> null;
        };
    }

    public static Map<String, CatVariant> getAllRecipes() {
        return Collections.unmodifiableMap(BREEDING_RECIPES);
    }

    private static final Map<String, ProductionTier> TIER_BREEDING_RECIPES = new HashMap<>();

    static {
        registerTier(ProductionTier.STANDARD, ProductionTier.COAL, ProductionTier.COAL);
        registerTier(ProductionTier.COAL, ProductionTier.COPPER, ProductionTier.COPPER);
        registerTier(ProductionTier.COPPER, ProductionTier.IRON, ProductionTier.IRON);
        registerTier(ProductionTier.IRON, ProductionTier.GOLD, ProductionTier.GOLD);
        registerTier(ProductionTier.GOLD, ProductionTier.LAPIS, ProductionTier.LAPIS);
        registerTier(ProductionTier.LAPIS, ProductionTier.REDSTONE, ProductionTier.REDSTONE);
        registerTier(ProductionTier.REDSTONE, ProductionTier.EMERALD, ProductionTier.EMERALD);
        registerTier(ProductionTier.EMERALD, ProductionTier.DIAMOND, ProductionTier.DIAMOND);
        registerTier(ProductionTier.DIAMOND, ProductionTier.NETHERITE, ProductionTier.NETHERITE);
    }

    private static void registerTier(ProductionTier parentA, ProductionTier parentB, ProductionTier result) {
        TIER_BREEDING_RECIPES.put(getTierKey(parentA, parentB), result);
    }

    private static String getTierKey(ProductionTier a, ProductionTier b) {
        return a.ordinal() < b.ordinal() ? a.getId() + "+" + b.getId() : b.getId() + "+" + a.getId();
    }

    /**
     * Determines child production tier:
     * - Two cats of the SAME tier always evolve to the NEXT tier up!
     * - Two STANDARD cats produce a COAL tier cat.
     * - Cross-tiers check specific alloy recipes or yield the higher tier.
     */
    public static ProductionTier getChildTier(ProductionTier parentA, ProductionTier parentB) {
        if (parentA == null && parentB == null) return ProductionTier.STANDARD;
        if (parentA == null) return parentB;
        if (parentB == null) return parentA;

        // Two cats of the SAME tier evolve to the NEXT tier up!
        if (parentA == parentB) {
            return switch (parentA) {
                case STANDARD -> ProductionTier.COAL;
                case COAL -> ProductionTier.COPPER;
                case COPPER -> ProductionTier.IRON;
                case IRON -> ProductionTier.GOLD;
                case GOLD -> ProductionTier.LAPIS;
                case LAPIS -> ProductionTier.REDSTONE;
                case REDSTONE -> ProductionTier.EMERALD;
                case EMERALD -> ProductionTier.DIAMOND;
                case DIAMOND, NETHERITE -> ProductionTier.NETHERITE;
            };
        }

        String key = getTierKey(parentA, parentB);
        if (TIER_BREEDING_RECIPES.containsKey(key)) {
            return TIER_BREEDING_RECIPES.get(key);
        }

        // Higher tier wins deterministically
        return parentA.ordinal() > parentB.ordinal() ? parentA : parentB;
    }

    public enum MutationType {
        NONE("none"),
        NEKOMATA("nekomata");

        private final String id;

        MutationType(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        public static MutationType fromString(String name) {
            if (name == null || name.isEmpty()) return NONE;
            for (MutationType type : values()) {
                if (type.name().equalsIgnoreCase(name) || type.id.equalsIgnoreCase(name)) {
                    return type;
                }
            }
            return NONE;
        }
    }

    /**
     * Calculates offspring mutation:
     * - 100% chance if Spirit Catnip was used as catalyst.
     * - 10% base chance when breeding two purebred cats (same visual variant).
     */
    public static MutationType calculateMutation(TalentedCat parentA, TalentedCat parentB, boolean usedSpiritCatnip) {
        if (usedSpiritCatnip) {
            return MutationType.NEKOMATA;
        }
        if (parentA != null && parentB != null) {
            // Purebred breeding: same variant
            if (parentA.getCatVariant() == parentB.getCatVariant()) {
                if (parentA.getRandom().nextFloat() < 0.10F) {
                    return MutationType.NEKOMATA;
                }
            }
        }
        return MutationType.NONE;
    }
}
