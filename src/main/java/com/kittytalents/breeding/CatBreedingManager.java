package com.kittytalents.breeding;

import com.kittytalents.entity.CatVariant;
import com.kittytalents.entity.ProductionTier;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.registry.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Map;

/**
 * Kitty Talents 3.0 Production Breeding Engine.
 * Implements deterministic production tier evolution, strict identity/skin/production separation,
 * and 100% Nekomata offspring guarantee upon Spirit Catnip consumption.
 */
public class CatBreedingManager extends com.kittytalents.entity.CatBreedingManager {

    /**
     * Interface contract specified in PROJECT.md Interface Contract 3.
     * Strictly deterministic. Tier_N + Tier_N = Tier_{N+1}.
     */
    public static ProductionTier getEvolutionTier(ProductionTier parent1, ProductionTier parent2) {
        return getChildTier(parent1, parent2);
    }

    /**
     * Interface contract specified in PROJECT.md Interface Contract 3.
     * If either parent was fed Spirit Catnip, or the breeding item is Spirit Catnip,
     * the offspring is 100% guaranteed Nekomata (P = 1.0).
     */
    public static boolean isNekomataGuaranteed(TalentedCat parent1, TalentedCat parent2, ItemStack breedItem) {
        if (breedItem != null && !breedItem.isEmpty() && breedItem.is(ModItems.SPIRIT_CATNIP.get())) {
            return true;
        }
        if (parent1 != null && parent1.getPersistentData().getBoolean("SpiritCatnipUsed")) {
            return true;
        }
        if (parent2 != null && parent2.getPersistentData().getBoolean("SpiritCatnipUsed")) {
            return true;
        }
        return false;
    }

    /**
     * Deterministic production tier evolution:
     * Standard -> Coal -> Copper -> Iron -> Gold -> Lapis -> Redstone -> Emerald -> Diamond -> Netherite.
     * Two cats of the SAME tier strictly evolve to the NEXT tier up with ZERO random rolls.
     */
    public static ProductionTier getChildTier(ProductionTier parentA, ProductionTier parentB) {
        if (parentA == null && parentB == null) return ProductionTier.STANDARD;
        if (parentA == null) return parentB;
        if (parentB == null) return parentA;

        // Same tiers strictly advance to the next tier up
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

        // Consult registered breeding matrix for stepped cross-tiers
        ProductionTier matrixTier = BreedingMatrix.lookupTierEvolution(parentA, parentB);
        if (matrixTier != null) {
            return matrixTier;
        }

        // Deterministic fallback: higher tier is preserved
        return parentA.ordinal() > parentB.ordinal() ? parentA : parentB;
    }

    /**
     * Completely deterministic visual breed inheritance.
     * STRICT SEPARATION: Production tier evolution NEVER arbitrarily recolors the cat!
     * A purebred Panther remains a Panther regardless of whether it is Iron, Diamond, or Netherite tier.
     */
    public static CatVariant getChildVariant(CatVariant parentA, CatVariant parentB) {
        return getChildVariant(parentA, parentB, null);
    }

    /**
     * Determines child breed variant while preserving strict separation between
     * feline identity and production tier.
     */
    public static CatVariant getChildVariant(CatVariant parentA, CatVariant parentB, ProductionTier childTier) {
        if (parentA == null && parentB == null) return CatVariant.TABBY;
        if (parentA == null) return parentB;
        if (parentB == null) return parentA;

        // Purebred parents of the same breed always yield identical child breed!
        // Preserves breed identity independent of production progression.
        if (parentA == parentB) {
            return parentA;
        }

        // Cross-breed recipe lookup
        CatVariant crossOutcome = BreedingMatrix.lookupBreedOutcome(parentA, parentB);
        if (crossOutcome != null) {
            return crossOutcome;
        }

        // Deterministic fallback: higher rarity variant wins, or canonical ordinal
        if (parentA.getRarity().ordinal() > parentB.getRarity().ordinal()) {
            return parentA;
        } else if (parentB.getRarity().ordinal() > parentA.getRarity().ordinal()) {
            return parentB;
        }
        return parentA.ordinal() < parentB.ordinal() ? parentA : parentB;
    }

    /**
     * Offspring mutation calculation:
     * - 100% Nekomata guarantee when Spirit Catnip is used (P = 1.0).
     * - Zero hidden random rolls.
     */
    public static MutationType calculateMutation(TalentedCat parentA, TalentedCat parentB, boolean usedSpiritCatnip) {
        if (usedSpiritCatnip) {
            return MutationType.NEKOMATA;
        }
        if (parentA != null && parentA.getPersistentData().getBoolean("SpiritCatnipUsed")) {
            return MutationType.NEKOMATA;
        }
        if (parentB != null && parentB.getPersistentData().getBoolean("SpiritCatnipUsed")) {
            return MutationType.NEKOMATA;
        }
        return MutationType.NONE;
    }

    /**
     * Checks whether an item can trigger breeding.
     * Normal fish NEVER triggers breeding; only Breeding Catnip or Spirit Catnip.
     */
    public static boolean isBreedingItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return stack.is(ModItems.BREEDING_CATNIP.get()) || stack.is(ModItems.SPIRIT_CATNIP.get());
    }

    /**
     * Validates that standard fish items cannot activate breeding.
     */
    public static boolean isFishItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return stack.is(Items.COD) || stack.is(Items.COOKED_COD) ||
               stack.is(Items.SALMON) || stack.is(Items.COOKED_SALMON) ||
               stack.is(Items.TROPICAL_FISH) || stack.is(Items.PUFFERFISH);
    }

    public static Map<String, ProductionTier> getAllTierRecipes() {
        return BreedingMatrix.getAllTierEvolutions();
    }
}
