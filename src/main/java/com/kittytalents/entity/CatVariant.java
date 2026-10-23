package com.kittytalents.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import com.kittytalents.registry.ModItems;

import java.util.Locale;
import java.util.function.Supplier;

public enum CatVariant {
    // Standard Companion Feline Variants
    TABBY("tabby", "Tabby", VariantRarity.COMMON, "Amber", 0xD4A373, () -> ItemStack.EMPTY, "Nimble Stalker"),
    TUXEDO("tuxedo", "Tuxedo", VariantRarity.COMMON, "Green", 0x333333, () -> ItemStack.EMPTY, "Classy Gentleman"),
    RED("red", "Red Tabby", VariantRarity.COMMON, "Amber", 0xE76F51, () -> ItemStack.EMPTY, "Fiery Spirit"),
    SIAMESE("siamese", "Siamese", VariantRarity.COMMON, "Sky Blue", 0xF4A261, () -> ItemStack.EMPTY, "Vocal Companion"),
    BRITISH_SHORTHAIR("british_shorthair", "British Shorthair", VariantRarity.COMMON, "Copper", 0x6C757D, () -> ItemStack.EMPTY, "Sturdy Guardian"),
    CALICO("calico", "Calico", VariantRarity.COMMON, "Green", 0xF77F00, () -> ItemStack.EMPTY, "Lucky Charm"),
    PERSIAN("persian", "Persian", VariantRarity.COMMON, "Heterochromia", 0xE9ECEF, () -> ItemStack.EMPTY, "Royal Aristocrat"),
    RAGDOLL("ragdoll", "Ragdoll", VariantRarity.COMMON, "Deep Blue", 0xDEE2E6, () -> ItemStack.EMPTY, "Gentle Soul"),
    WHITE("white", "Snow White", VariantRarity.COMMON, "Blue", 0xFFFFFF, () -> ItemStack.EMPTY, "Silent Prowler"),
    JELLIE("jellie", "Jellie", VariantRarity.COMMON, "Green", 0x8D99AE, () -> ItemStack.EMPTY, "Beloved Friend"),
    ALL_BLACK("all_black", "Midnight Black", VariantRarity.COMMON, "Golden", 0x1A1A1A, () -> ItemStack.EMPTY, "Shadow Stalker"),
    TIGER("tiger", "Bengal Tiger", VariantRarity.ADVANCED, "Golden", 0xD97706, () -> ItemStack.EMPTY, "Apex Predator"),
    SNOW_LEOPARD("snow_leopard", "Snow Leopard", VariantRarity.ADVANCED, "Bright Blue", 0xCBD5E1, () -> ItemStack.EMPTY, "Mountain Ghost"),
    PANTHER("panther", "Black Panther", VariantRarity.ADVANCED, "Bright Blue", 0x0F172A, () -> ItemStack.EMPTY, "Night Stalker"),
    VOID_ENDER("void_ender", "Void Ender", VariantRarity.RARE, "Ender Purple", 0x3B0764, () -> ItemStack.EMPTY, "Dimensional Walker"),
    GOLDEN_MANEKI("golden_maneki", "Maneki Neko", VariantRarity.RARE, "Emerald", 0xFACC15, () -> ItemStack.EMPTY, "Fortune Bringer"),
    GHOST_PHANTOM("ghost_phantom", "Ghost Phantom", VariantRarity.RARE, "Cyan Glow", 0x67E8F9, () -> ItemStack.EMPTY, "Ethereal Spirit"),

    // Productive Feline Variants
    MINT("mint", "Mint Cat", VariantRarity.COMMON, "Mint Green", 0x86EFAC, () -> new ItemStack(ModItems.CATNIP.get()), "Botanist"),
    FISH("fish", "Fisher Cat", VariantRarity.COMMON, "Sea Blue", 0x38BDF8, () -> new ItemStack(Items.COD), "Angler"),
    CROP("crop", "Harvester Cat", VariantRarity.COMMON, "Golden Wheat", 0xFDE047, () -> new ItemStack(Items.WHEAT), "Harvester"),
    WOOD("wood", "Lumberjack Cat", VariantRarity.COMMON, "Bark Brown", 0x78350F, () -> new ItemStack(Items.OAK_LOG), "Lumberjack"),
    COAL("coal", "Coal Cat", VariantRarity.COMMON, "Soot Black", 0x27272A, () -> new ItemStack(Items.COAL), "Coal Miner"),
    COPPER("copper", "Copper Cat", VariantRarity.COMMON, "Copper Orange", 0xEA580C, () -> new ItemStack(Items.RAW_COPPER), "Copper Excavator"),
    IRON("iron", "Iron Cat", VariantRarity.ADVANCED, "Silver Gray", 0x94A3B8, () -> new ItemStack(Items.RAW_IRON), "Iron Prospector"),
    GOLD("gold", "Gold Cat", VariantRarity.ADVANCED, "Gilded Gold", 0xFBBF24, () -> new ItemStack(Items.RAW_GOLD), "Gold Miner"),
    REDSTONE("redstone", "Redstone Cat", VariantRarity.ADVANCED, "Crimson Red", 0xEF4444, () -> new ItemStack(Items.REDSTONE), "Redstone Engineer"),
    LAPIS("lapis", "Lapis Cat", VariantRarity.ADVANCED, "Lapis Blue", 0x2563EB, () -> new ItemStack(Items.LAPIS_LAZULI), "Gem Seeker"),
    QUARTZ("quartz", "Quartz Cat", VariantRarity.ADVANCED, "Nether Quartz", 0xF8FAFC, () -> new ItemStack(Items.QUARTZ), "Quartz Delver"),
    EMERALD("emerald", "Emerald Cat", VariantRarity.RARE, "Emerald Green", 0x10B981, () -> new ItemStack(Items.EMERALD), "Jewel Merchant"),
    DIAMOND("diamond", "Diamond Cat", VariantRarity.RARE, "Diamond Cyan", 0x06B6D4, () -> new ItemStack(Items.DIAMOND), "Diamond Excavator"),
    NETHERITE("netherite", "Netherite Cat", VariantRarity.LEGENDARY, "Netherite Dark", 0x475569, () -> new ItemStack(Items.NETHERITE_SCRAP), "Netherite Forger"),
    STEEL("steel", "Steel Cat", VariantRarity.RARE, "Refined Steel", 0x64748B, () -> new ItemStack(Items.IRON_INGOT), "Foundry Cat"),
    INDUSTRIAL("industrial", "Industrial Cat", VariantRarity.RARE, "Bronze Alloy", 0xB45309, () -> new ItemStack(Items.COPPER_INGOT), "Factory Worker"),
    TREASURE("treasure", "Treasure Cat", VariantRarity.LEGENDARY, "Royal Gilded", 0xFFD700, () -> new ItemStack(Items.GOLD_INGOT), "Royal Collector");

    public enum VariantRarity {
        COMMON("Common", 0xFFFFFF),
        ADVANCED("Advanced", 0x55FF55),
        RARE("Rare", 0x55FFFF),
        LEGENDARY("Legendary", 0xFFAA00);

        private final String displayName;
        private final int color;

        VariantRarity(String displayName, int color) {
            this.displayName = displayName;
            this.color = color;
        }

        public String getDisplayName() { return displayName; }
        public int getColor() { return color; }
    }

    private final String id;
    private final String displayName;
    private final VariantRarity rarity;
    private final String eyeColor;
    private final int hueColor;
    private final Supplier<ItemStack> productionSupplier;
    private final String traitDescription;

    CatVariant(String id, String displayName, VariantRarity rarity, String eyeColor, int hueColor,
               Supplier<ItemStack> productionSupplier, String traitDescription) {
        this.id = id;
        this.displayName = displayName;
        this.rarity = rarity;
        this.eyeColor = eyeColor;
        this.hueColor = hueColor;
        this.productionSupplier = productionSupplier;
        this.traitDescription = traitDescription;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public VariantRarity getRarity() { return rarity; }
    public String getEyeColor() { return eyeColor; }
    public int getHueColor() { return hueColor; }
    public String getTraitDescription() { return traitDescription; }

    public boolean isProductive() {
        return !productionSupplier.get().isEmpty();
    }

    public ItemStack getProductionResult() {
        ItemStack stack = productionSupplier.get();
        return stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
    }

    public static CatVariant fromId(String id) {
        if (id == null || id.isEmpty()) return TABBY;
        for (CatVariant v : values()) {
            if (v.id.equalsIgnoreCase(id)) return v;
        }
        return TABBY;
    }

    public static CatVariant fromVanillaCatVariant(net.minecraft.world.entity.animal.CatVariant vanilla) {
        if (vanilla == null) return TABBY;
        net.minecraft.resources.ResourceLocation key = net.minecraft.core.registries.BuiltInRegistries.CAT_VARIANT.getKey(vanilla);
        if (key != null) {
            String path = key.getPath();
            if (path.contains("all_black") || path.equals("black")) return ALL_BLACK;
            if (path.contains("red")) return RED;
            if (path.contains("siamese")) return SIAMESE;
            if (path.contains("british")) return BRITISH_SHORTHAIR;
            if (path.contains("calico")) return CALICO;
            if (path.contains("persian")) return PERSIAN;
            if (path.contains("ragdoll")) return RAGDOLL;
            if (path.contains("white")) return WHITE;
            if (path.contains("jellie")) return JELLIE;
        }
        return TABBY;
    }
}
