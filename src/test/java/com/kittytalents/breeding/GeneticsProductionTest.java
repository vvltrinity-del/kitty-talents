package com.kittytalents.breeding;

import com.kittytalents.entity.CatVariant;
import com.kittytalents.entity.ProductionTier;

/**
 * Pure verification suite for Genetics and Production logic.
 * Validates deterministic tier matrix, breed identity preservation, and zero random outcomes.
 */
public class GeneticsProductionTest {

    public static void main(String[] args) {
        testDeterministicTierProgression();
        testBreedPreservationOnTierEvolution();
        testCrossBreedDeterministicMatrix();
        System.out.println("ALL GENETICS & PRODUCTION TESTS PASSED SUCCESSFULLY.");
    }

    public static void testDeterministicTierProgression() {
        // Standard -> Coal -> Copper -> Iron -> Gold -> Lapis -> Redstone -> Emerald -> Diamond -> Netherite
        assertEqual(ProductionTier.COAL, CatBreedingManager.getEvolutionTier(ProductionTier.STANDARD, ProductionTier.STANDARD));
        assertEqual(ProductionTier.COPPER, CatBreedingManager.getEvolutionTier(ProductionTier.COAL, ProductionTier.COAL));
        assertEqual(ProductionTier.IRON, CatBreedingManager.getEvolutionTier(ProductionTier.COPPER, ProductionTier.COPPER));
        assertEqual(ProductionTier.GOLD, CatBreedingManager.getEvolutionTier(ProductionTier.IRON, ProductionTier.IRON));
        assertEqual(ProductionTier.LAPIS, CatBreedingManager.getEvolutionTier(ProductionTier.GOLD, ProductionTier.GOLD));
        assertEqual(ProductionTier.REDSTONE, CatBreedingManager.getEvolutionTier(ProductionTier.LAPIS, ProductionTier.LAPIS));
        assertEqual(ProductionTier.EMERALD, CatBreedingManager.getEvolutionTier(ProductionTier.REDSTONE, ProductionTier.REDSTONE));
        assertEqual(ProductionTier.DIAMOND, CatBreedingManager.getEvolutionTier(ProductionTier.EMERALD, ProductionTier.EMERALD));
        assertEqual(ProductionTier.NETHERITE, CatBreedingManager.getEvolutionTier(ProductionTier.DIAMOND, ProductionTier.DIAMOND));
        assertEqual(ProductionTier.NETHERITE, CatBreedingManager.getEvolutionTier(ProductionTier.NETHERITE, ProductionTier.NETHERITE));
    }

    public static void testBreedPreservationOnTierEvolution() {
        // Purebred Panther + Panther must produce Panther, NOT recolor to Coal or Iron variant!
        CatVariant purebredPanther = CatBreedingManager.getChildVariant(CatVariant.PANTHER, CatVariant.PANTHER, ProductionTier.DIAMOND);
        assertEqual(CatVariant.PANTHER, purebredPanther);

        CatVariant purebredSnowLeopard = CatBreedingManager.getChildVariant(CatVariant.SNOW_LEOPARD, CatVariant.SNOW_LEOPARD, ProductionTier.NETHERITE);
        assertEqual(CatVariant.SNOW_LEOPARD, purebredSnowLeopard);

        CatVariant purebredTiger = CatBreedingManager.getChildVariant(CatVariant.TIGER, CatVariant.TIGER, ProductionTier.IRON);
        assertEqual(CatVariant.TIGER, purebredTiger);
    }

    public static void testCrossBreedDeterministicMatrix() {
        // Tabby + All Black -> Panther
        CatVariant panther = CatBreedingManager.getChildVariant(CatVariant.TABBY, CatVariant.ALL_BLACK);
        assertEqual(CatVariant.PANTHER, panther);

        // White + Tabby -> Snow Leopard
        CatVariant snowLeopard = CatBreedingManager.getChildVariant(CatVariant.WHITE, CatVariant.TABBY);
        assertEqual(CatVariant.SNOW_LEOPARD, snowLeopard);

        // Red + All Black -> Tiger
        CatVariant tiger = CatBreedingManager.getChildVariant(CatVariant.RED, CatVariant.ALL_BLACK);
        assertEqual(CatVariant.TIGER, tiger);
    }

    private static void assertEqual(Object expected, Object actual) {
        if (expected == null && actual == null) return;
        if (expected != null && expected.equals(actual)) return;
        throw new AssertionError("Assertion Failed! Expected: " + expected + ", Actual: " + actual);
    }
}
