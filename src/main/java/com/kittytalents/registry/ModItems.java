package com.kittytalents.registry;

import com.kittytalents.KittyTalents;
import com.kittytalents.item.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, KittyTalents.MODID);

    // Treat Progression
    public static final RegistryObject<Item> CATNIP = ITEMS.register("catnip",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CATNIP_TREAT = ITEMS.register("catnip_treat",
            () -> new CatTreatItem(new Item.Properties(), 1, 20));

    public static final RegistryObject<Item> SUPER_CATNIP_TREAT = ITEMS.register("super_catnip_treat",
            () -> new CatTreatItem(new Item.Properties().rarity(Rarity.UNCOMMON), 21, 40));

    public static final RegistryObject<Item> MASTER_CATNIP_TREAT = ITEMS.register("master_catnip_treat",
            () -> new CatTreatItem(new Item.Properties().rarity(Rarity.RARE), 41, 60));

    public static final RegistryObject<Item> DIRE_TREAT = ITEMS.register("dire_treat",
            () -> new CatTreatItem(new Item.Properties().rarity(Rarity.EPIC), 61, 100));

    public static final RegistryObject<Item> AMNESIA_TREAT = ITEMS.register("amnesia_treat",
            () -> new AmnesiaTreatItem(new Item.Properties().rarity(Rarity.RARE)));

    // Tools & Utility
    public static final RegistryObject<Item> PRODUCER_STICK = ITEMS.register("producer_stick",
            () -> new com.kittytalents.item.ProducerStickItem(new Item.Properties()));

    public static final RegistryObject<Item> KITTY_WHISTLE = ITEMS.register("kitty_whistle",
            () -> new KittyWhistleItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> KITTY_TRACKER = ITEMS.register("kitty_tracker",
            () -> new KittyTrackerItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> CAT_CARRIER = ITEMS.register("cat_carrier",
            () -> new CatCarrierItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> CAT_LASER_POINTER = ITEMS.register("cat_laser_pointer",
            () -> new CatLaserPointerItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> WARM_MILK = ITEMS.register("warm_milk",
            () -> new WarmMilkItem(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> CAT_COLLAR = ITEMS.register("cat_collar",
            () -> new CatCollarItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> GROOMING_SHEARS = ITEMS.register("grooming_shears",
            () -> new GroomingShearsItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> TREAT_BAG = ITEMS.register("treat_bag",
            () -> new CatnipTreatBagItem(new Item.Properties().stacksTo(1)));

    // Toys
    public static final RegistryObject<Item> YARN_BALL = ITEMS.register("yarn_ball",
            () -> new YarnBallItem(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> FEATHER_TOY = ITEMS.register("feather_toy",
            () -> new FeatherToyItem(new Item.Properties().stacksTo(1)));

    // Culinary & Nutrition
    public static final RegistryObject<Item> CATNIP_COOKIE = ITEMS.register("catnip_cookie",
            () -> new CatFoodItem(new Item.Properties(), 20, 2.0F, "A tasty biscuit baked with fresh catnip."));

    public static final RegistryObject<Item> SALMON_SUSHI = ITEMS.register("salmon_sushi",
            () -> new CatFoodItem(new Item.Properties(), 40, 5.0F, "Delicate salmon sushi roll beloved by cats."));

    public static final RegistryObject<Item> TUNA_BOWL = ITEMS.register("tuna_bowl",
            () -> new CatFoodItem(new Item.Properties(), 60, 8.0F, "Hearty bowl of fresh tuna chunks in broth."));

    public static final RegistryObject<Item> PREMIUM_FEAST = ITEMS.register("premium_feast",
            () -> new CatFoodItem(new Item.Properties().rarity(Rarity.EPIC), 100, 15.0F, "The ultimate gourmet feast for champion battle cats."));

    public static final RegistryObject<Item> DEEP_FRIED_CHICKEN = ITEMS.register("deep_fried_chicken",
            () -> new DeepFriedChickenItem(new Item.Properties()));
    /** 5.0: marks bases and gives cats guard posts. */
    public static final RegistryObject<Item> BASE_PIN = ITEMS.register("base_pin",
            () -> new com.kittytalents.item.BasePinItem(new Item.Properties()));
    /** 5.0: the top meal (Deep Fried Chicken + Steamed Rice Bowl). */
    public static final RegistryObject<Item> CHICKEN_AND_RICE = ITEMS.register("chicken_and_rice",
            () -> new DeepFriedChickenItem(new Item.Properties(), DeepFriedChickenItem.CHICKEN_AND_RICE));

    // Milk Buckets
    public static final RegistryObject<Item> CATNIP_BUCKET = ITEMS.register("catnip_bucket",
            () -> new CatnipBucketItem(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> CATNIP_MILK_BUCKET = ITEMS.register("catnip_milk_bucket",
            () -> new CatnipMilkBucketItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    public static final RegistryObject<Item> CATNIP_RESPEC_ELIXIR = ITEMS.register("catnip_respec_elixir",
            () -> new CatnipRespecElixirItem(new Item.Properties()));

    // Wearable Accessories
    public static final RegistryObject<Item> WITCH_HAT = ITEMS.register("witch_hat",
            () -> new CatAccessoryItem(new Item.Properties().stacksTo(1), CatAccessoryItem.AccessoryType.HAT));

    public static final RegistryObject<Item> TOP_HAT = ITEMS.register("top_hat",
            () -> new CatAccessoryItem(new Item.Properties().stacksTo(1), CatAccessoryItem.AccessoryType.HAT));

    public static final RegistryObject<Item> BOWTIE = ITEMS.register("bowtie",
            () -> new CatAccessoryItem(new Item.Properties().stacksTo(1), CatAccessoryItem.AccessoryType.BOW));

    public static final RegistryObject<Item> HERO_CAPE = ITEMS.register("hero_cape",
            () -> new CatAccessoryItem(new Item.Properties().stacksTo(1), CatAccessoryItem.AccessoryType.CAPE));

    public static final RegistryObject<Item> FELINE_GOGGLES = ITEMS.register("feline_goggles",
            () -> new CatAccessoryItem(new Item.Properties().stacksTo(1), CatAccessoryItem.AccessoryType.GLASSES));

    public static final RegistryObject<Item> CROWN = ITEMS.register("crown",
            () -> new CatAccessoryItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), CatAccessoryItem.AccessoryType.HAT));

    public static final RegistryObject<Item> HALO = ITEMS.register("halo",
            () -> new CatAccessoryItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC), CatAccessoryItem.AccessoryType.HAT));

    public static final RegistryObject<Item> ANGEL_WINGS = ITEMS.register("angel_wings",
            () -> new CatAccessoryItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC), CatAccessoryItem.AccessoryType.CAPE));

    public static final RegistryObject<Item> CAT_SCARF = ITEMS.register("cat_scarf",
            () -> new CatAccessoryItem(new Item.Properties().stacksTo(1), CatAccessoryItem.AccessoryType.BOW));

    public static final RegistryObject<Item> PHANTOM_MASK = ITEMS.register("phantom_mask",
            () -> new CatAccessoryItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), CatAccessoryItem.AccessoryType.GLASSES));

    // Additional Toys & Interactive Items
    public static final RegistryObject<Item> TOY_MOUSE = ITEMS.register("toy_mouse",
            () -> new YarnBallItem(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> CAT_BALL = ITEMS.register("cat_ball",
            () -> new YarnBallItem(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> TOY_FISH = ITEMS.register("toy_fish",
            () -> new YarnBallItem(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> FRISBEE = ITEMS.register("frisbee",
            () -> new YarnBallItem(new Item.Properties().stacksTo(16)));

    // Locator & Care Items
    public static final RegistryObject<Item> EMPTY_LOCATOR_ORB = ITEMS.register("empty_locator_orb",
            () -> new EmptyLocatorOrbItem(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> FELINE_LOCATOR_ORB = ITEMS.register("feline_locator_orb",
            () -> new FelineLocatorOrbItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    public static final RegistryObject<Item> BREEDING_CATNIP = ITEMS.register("breeding_catnip",
            () -> new BreedingCatnipItem(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> SPIRIT_CATNIP = ITEMS.register("spirit_catnip",
            () -> new SpiritCatnipItem(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> FELINE_WRAP = ITEMS.register("feline_wrap",
            () -> new FelineWrapItem(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> HEALING_TREAT = ITEMS.register("healing_treat",
            () -> new HealingTreatItem(new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> GENDER_RIBBON = ITEMS.register("gender_ribbon",
            () -> new GenderRibbonItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> KITTY_WORK_CLIPBOARD = ITEMS.register("kitty_work_clipboard",
            () -> new com.kittytalents.item.KittyWorkClipboardItem(new Item.Properties()));

    public static final RegistryObject<Item> VARIANT_CHART = ITEMS.register("variant_chart",
            () -> new com.kittytalents.item.VariantChartItem(new Item.Properties()));

    // Culinary & Crafting Items
    public static final RegistryObject<Item> RICE_GRAIN = ITEMS.register("rice_grain",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> RICE_BOWL = ITEMS.register("rice_bowl",
            () -> new Item(new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(6).saturationMod(0.6F).build()).stacksTo(16)));

    // Productive Cats System Items & Upgrades
    public static final RegistryObject<Item> RESOURCE_CATALYST = ITEMS.register("resource_catalyst",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> CATNIP_CATALYST = ITEMS.register("catnip_catalyst",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> ORE_ESSENCE = ITEMS.register("ore_essence",
            () -> new Item(new Item.Properties().rarity(Rarity.RARE)));

    public static final RegistryObject<Item> UPGRADE_PRODUCTIVITY_1 = ITEMS.register("upgrade_productivity_1",
            () -> new ProductivityUpgradeItem(new Item.Properties(), ProductivityUpgradeItem.Tier.BASIC));

    public static final RegistryObject<Item> UPGRADE_PRODUCTIVITY_2 = ITEMS.register("upgrade_productivity_2",
            () -> new ProductivityUpgradeItem(new Item.Properties(), ProductivityUpgradeItem.Tier.IMPROVED));

    public static final RegistryObject<Item> UPGRADE_PRODUCTIVITY_3 = ITEMS.register("upgrade_productivity_3",
            () -> new ProductivityUpgradeItem(new Item.Properties(), ProductivityUpgradeItem.Tier.ADVANCED));

    public static final RegistryObject<Item> UPGRADE_PRODUCTIVITY_4 = ITEMS.register("upgrade_productivity_4",
            () -> new ProductivityUpgradeItem(new Item.Properties(), ProductivityUpgradeItem.Tier.SUPERIOR));

    public static final RegistryObject<Item> UPGRADE_PRODUCTIVITY_5 = ITEMS.register("upgrade_productivity_5",
            () -> new ProductivityUpgradeItem(new Item.Properties(), ProductivityUpgradeItem.Tier.MASTER));

    public static final RegistryObject<Item> UPGRADE_PRODUCTIVITY_6 = ITEMS.register("upgrade_productivity_6",
            () -> new ProductivityUpgradeItem(new Item.Properties(), ProductivityUpgradeItem.Tier.DIRE));

    /** 5.1: Habitat upgrade that smelts the raw ores a worker produces (one is enough, so it doesn't stack). */
    public static final RegistryObject<Item> UPGRADE_SMELTER = ITEMS.register("upgrade_smelter",
            () -> new SmelterUpgradeItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    // Craftable Production Tier Upgrade Treats
    public static final RegistryObject<Item> COAL_CAT_TREAT = ITEMS.register("coal_cat_treat",
            () -> new TierUpgradeTreatItem(new Item.Properties(), com.kittytalents.entity.ProductionTier.STANDARD, com.kittytalents.entity.ProductionTier.COAL, Rarity.COMMON));

    public static final RegistryObject<Item> COPPER_CAT_TREAT = ITEMS.register("copper_cat_treat",
            () -> new TierUpgradeTreatItem(new Item.Properties(), com.kittytalents.entity.ProductionTier.COAL, com.kittytalents.entity.ProductionTier.COPPER, Rarity.COMMON));

    public static final RegistryObject<Item> IRON_CAT_TREAT = ITEMS.register("iron_cat_treat",
            () -> new TierUpgradeTreatItem(new Item.Properties(), com.kittytalents.entity.ProductionTier.COPPER, com.kittytalents.entity.ProductionTier.IRON, Rarity.UNCOMMON));

    public static final RegistryObject<Item> GOLD_CAT_TREAT = ITEMS.register("gold_cat_treat",
            () -> new TierUpgradeTreatItem(new Item.Properties(), com.kittytalents.entity.ProductionTier.IRON, com.kittytalents.entity.ProductionTier.GOLD, Rarity.UNCOMMON));

    public static final RegistryObject<Item> LAPIS_CAT_TREAT = ITEMS.register("lapis_cat_treat",
            () -> new TierUpgradeTreatItem(new Item.Properties(), com.kittytalents.entity.ProductionTier.GOLD, com.kittytalents.entity.ProductionTier.LAPIS, Rarity.UNCOMMON));

    public static final RegistryObject<Item> REDSTONE_CAT_TREAT = ITEMS.register("redstone_cat_treat",
            () -> new TierUpgradeTreatItem(new Item.Properties(), com.kittytalents.entity.ProductionTier.LAPIS, com.kittytalents.entity.ProductionTier.REDSTONE, Rarity.UNCOMMON));

    public static final RegistryObject<Item> EMERALD_CAT_TREAT = ITEMS.register("emerald_cat_treat",
            () -> new TierUpgradeTreatItem(new Item.Properties(), com.kittytalents.entity.ProductionTier.REDSTONE, com.kittytalents.entity.ProductionTier.EMERALD, Rarity.RARE));

    public static final RegistryObject<Item> DIAMOND_CAT_TREAT = ITEMS.register("diamond_cat_treat",
            () -> new TierUpgradeTreatItem(new Item.Properties(), com.kittytalents.entity.ProductionTier.EMERALD, com.kittytalents.entity.ProductionTier.DIAMOND, Rarity.RARE));

    public static final RegistryObject<Item> NETHERITE_CAT_TREAT = ITEMS.register("netherite_cat_treat",
            () -> new TierUpgradeTreatItem(new Item.Properties(), com.kittytalents.entity.ProductionTier.DIAMOND, com.kittytalents.entity.ProductionTier.NETHERITE, Rarity.EPIC));

    // Blocks
    /** Placed as a pair (upper half too), like a door. */
    public static final RegistryObject<Item> CAT_TREE = ITEMS.register("cat_tree",
            () -> new net.minecraft.world.item.DoubleHighBlockItem(ModBlocks.CAT_TREE.get(), new Item.Properties()));

    public static final RegistryObject<Item> CAT_FOOD_BOWL = ITEMS.register("cat_food_bowl",
            () -> new ItemFoodBowl(ModBlocks.CAT_FOOD_BOWL.get(), new Item.Properties()));

    public static final RegistryObject<Item> CAT_SCRATCHER = ITEMS.register("cat_scratcher",
            () -> new BlockItem(ModBlocks.CAT_SCRATCHER.get(), new Item.Properties()));

    public static final RegistryObject<Item> CAT_BED = ITEMS.register("cat_bed",
            () -> new BlockItem(ModBlocks.CAT_BED.get(), new Item.Properties()));

    public static final RegistryObject<Item> CAT_BATH = ITEMS.register("cat_bath",
            () -> new BlockItem(ModBlocks.CAT_BATH.get(), new Item.Properties()));

    public static final RegistryObject<Item> CAT_RICE_MILL = ITEMS.register("cat_rice_mill",
            () -> new BlockItem(ModBlocks.CAT_RICE_MILL.get(), new Item.Properties()));

    public static final RegistryObject<Item> KITTY_HABITAT = ITEMS.register("kitty_habitat",
            () -> new BlockItem(ModBlocks.KITTY_HABITAT.get(), new Item.Properties()));

    public static final RegistryObject<Item> FEEDING_STATION = ITEMS.register("feeding_station",
            () -> new BlockItem(ModBlocks.FEEDING_STATION.get(), new Item.Properties()));

    public static final RegistryObject<Item> KITTY_WORK_STATION = ITEMS.register("kitty_work_station",
            () -> new BlockItem(ModBlocks.KITTY_WORK_STATION.get(), new Item.Properties()));

    public static final RegistryObject<Item> TREAT_OVEN = ITEMS.register("treat_oven",
            () -> new BlockItem(ModBlocks.TREAT_OVEN.get(), new Item.Properties()));

    public static final RegistryObject<Item> KITTEN_INCUBATOR = ITEMS.register("kitten_incubator",
            () -> new BlockItem(ModBlocks.KITTEN_INCUBATOR.get(), new Item.Properties()));

    // Spawn Egg
    public static final RegistryObject<Item> TALENTED_CAT_SPAWN_EGG = ITEMS.register("talented_cat_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.TALENTED_CAT, 0xefd3a2, 0xd07c42, new Item.Properties()));
}
