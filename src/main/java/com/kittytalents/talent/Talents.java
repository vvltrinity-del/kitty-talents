package com.kittytalents.talent;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.*;

/**
 * Registry and catalog of all feline talents for Kitty Talents (R8 & 2.0 Automation/TacZ).
 */
public class Talents {
    private static final Map<String, Talent> TALENTS = new LinkedHashMap<>();

    // Standard RPG Feline Talents (NINE_LIVES, FISHER_CAT, MOUSER, PILLOW_CAT are standard built-in defaults)
    public static final Talent NINE_LIVES = register(new Talent("nine_lives", 5, () -> new ItemStack(Items.TOTEM_OF_UNDYING), false, true, TalentCategory.DEFENSE));
    public static final Talent POUNCE = register(new Talent("pounce", 8, () -> new ItemStack(Items.IRON_SWORD), false, false, TalentCategory.COMBAT));
    public static final Talent NIGHT_PROWLER = register(new Talent("night_prowler", 8, () -> new ItemStack(Items.ENDER_EYE), false, false, TalentCategory.UTILITY));
    public static final Talent KITTY_PACK = register(new Talent("kitty_pack", 5, () -> new ItemStack(Items.CHEST), false, false, TalentCategory.UTILITY));
    public static final Talent LUCKY_PURR = register(new Talent("lucky_purr", 8, () -> new ItemStack(Items.EXPERIENCE_BOTTLE), false, false, TalentCategory.SUPPORT));
    public static final Talent MOUSER = register(new Talent("mouser", 8, () -> new ItemStack(Items.RABBIT_FOOT), false, true, TalentCategory.UTILITY));
    public static final Talent FISHER_CAT = register(new Talent("fisher_cat", 8, () -> new ItemStack(Items.FISHING_ROD), false, true, TalentCategory.UTILITY));
    public static final Talent FELINE_AGILITY = register(new Talent("feline_agility", 8, () -> new ItemStack(Items.FEATHER), false, false, TalentCategory.MOBILITY));
    public static final Talent PILLOW_CAT = register(new Talent("pillow_cat", 8, () -> new ItemStack(Items.RED_BED), false, true, TalentCategory.SUPPORT));
    public static final Talent WHISKER_RADAR = register(new Talent("whisker_radar", 8, () -> new ItemStack(Items.COMPASS), false, false, TalentCategory.UTILITY));
    public static final Talent CAT_MOUNT = register(new Talent("cat_mount", 5, () -> new ItemStack(Items.SADDLE), false, false, TalentCategory.MOBILITY));
    public static final Talent CAT_ARMORY = register(new Talent("cat_armory", 5, () -> new ItemStack(Items.DIAMOND_SWORD), false, false, TalentCategory.COMBAT));
    public static final Talent CAT_ARMOR = register(new Talent("cat_armor", 5, () -> new ItemStack(Items.DIAMOND_CHESTPLATE), false, false, TalentCategory.DEFENSE));

    public static final Talent POUNCE_STRIKE = POUNCE;
    public static final Talent FELINE_REFLEXES = register(new Talent("feline_reflexes", 5, () -> new ItemStack(Items.SHIELD), false, false, TalentCategory.DEFENSE));
    public static final Talent CRITICAL_CLAWS = register(new Talent("critical_claws", 5, () -> new ItemStack(Items.IRON_SWORD), false, false, TalentCategory.COMBAT));

    // Feline Equivalents of Doggy Talents Next Catalog
    public static final Talent HAPPY_EATER = register(new Talent("happy_eater", 5, () -> new ItemStack(Items.COOKED_BEEF), false, false, TalentCategory.SUPPORT));
    public static final Talent AQUA_KITTY = register(new Talent("aqua_kitty", 5, () -> new ItemStack(Items.PRISMARINE_SHARD), false, false, TalentCategory.MOBILITY));
    public static final Talent AQUA_CAT = AQUA_KITTY;
    public static final Talent PEST_HUNTER = register(new Talent("pest_hunter", 5, () -> new ItemStack(Items.SPIDER_EYE), false, false, TalentCategory.COMBAT));
    public static final Talent TORCH_KITTY = register(new Talent("torch_kitty", 5, () -> new ItemStack(Items.TORCH), false, false, TalentCategory.UTILITY));
    public static final Talent HOME_WHISKERS = register(new Talent("home_whiskers", 5, () -> new ItemStack(Items.RECOVERY_COMPASS), false, false, TalentCategory.UTILITY));
    public static final Talent PURR_MEDIC = register(new Talent("purr_medic", 5, () -> new ItemStack(Items.GLISTERING_MELON_SLICE), false, false, TalentCategory.SUPPORT));
    public static final Talent FELINE_BERSERKER = register(new Talent("feline_berserker", 5, () -> new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), false, false, TalentCategory.COMBAT));
    public static final Talent FELINE_HISS = register(new Talent("feline_hiss", 5, () -> new ItemStack(Items.FIRE_CHARGE), false, false, TalentCategory.COMBAT));

    public static final Talent SHADOW_PELT = register(new Talent("shadow_pelt", 5, () -> new ItemStack(Items.PHANTOM_MEMBRANE), false, false, TalentCategory.UTILITY));
    public static final Talent GUARDIAN_CAT = register(new Talent("guardian_cat", 5, () -> new ItemStack(Items.SHIELD), false, false, TalentCategory.DEFENSE));
    public static final Talent HUNTER_CAT = register(new Talent("hunter_cat", 5, () -> new ItemStack(Items.BOW), false, false, TalentCategory.COMBAT));
    public static final Talent INFERNO_CAT = register(new Talent("inferno_cat", 5, () -> new ItemStack(Items.MAGMA_CREAM), false, false, TalentCategory.COMBAT));
    public static final Talent VENOM_CLAWS = register(new Talent("venom_claws", 5, () -> new ItemStack(Items.FERMENTED_SPIDER_EYE), false, false, TalentCategory.COMBAT));
    public static final Talent RESCUE_PURR = register(new Talent("rescue_purr", 5, () -> new ItemStack(Items.GOLDEN_APPLE), false, false, TalentCategory.SUPPORT));
    public static final Talent HERDING_KITTY = register(new Talent("herding_kitty", 5, () -> new ItemStack(Items.WHEAT), false, false, TalentCategory.UTILITY));
    public static final Talent GALE_PURR = register(new Talent("gale_purr", 5, () -> new ItemStack(Items.FEATHER), false, false, TalentCategory.COMBAT));
    public static final Talent CHARM_WHISKERS = register(new Talent("charm_whiskers", 5, () -> new ItemStack(Items.EMERALD), false, false, TalentCategory.SUPPORT));
    public static final Talent WATER_PELT = register(new Talent("water_pelt", 5, () -> new ItemStack(Items.WATER_BUCKET), false, false, TalentCategory.DEFENSE));
    public static final Talent KITTY_TOOLS = register(new Talent("kitty_tools", 5, () -> new ItemStack(Items.IRON_PICKAXE), false, false, TalentCategory.UTILITY));
    public static final Talent CUSHION_PAWS = register(new Talent("cushion_paws", 5, () -> new ItemStack(Items.SLIME_BALL), false, false, TalentCategory.MOBILITY));
    public static final Talent PAW_RETRIEVER = register(new Talent("paw_retriever", 5, () -> new ItemStack(Items.LEAD), false, false, TalentCategory.UTILITY));
    public static final Talent FIREPROOF_PAWS = register(new Talent("fireproof_paws", 5, () -> new ItemStack(Items.MAGMA_BLOCK), false, false, TalentCategory.DEFENSE));
    public static final Talent SNIFFER_WHISKERS = register(new Talent("sniffer_whiskers", 5, () -> new ItemStack(Items.SNIFFER_EGG), false, false, TalentCategory.UTILITY));
    public static final Talent GATE_PURR = register(new Talent("gate_purr", 5, () -> new ItemStack(Items.OAK_FENCE_GATE), false, false, TalentCategory.UTILITY));

    // 2.0 Automation & Worker Talents
    public static final Talent GREEN_PAW = register(new Talent("green_paw", 5, () -> new ItemStack(Items.WHEAT_SEEDS), false, false, TalentCategory.UTILITY));
    public static final Talent TIMBER_CLAWS = register(new Talent("timber_claws", 5, () -> new ItemStack(Items.IRON_AXE), false, false, TalentCategory.UTILITY));
    public static final Talent LOGISTICS_WHISKERS = register(new Talent("logistics_whiskers", 5, () -> new ItemStack(Items.HOPPER), false, false, TalentCategory.UTILITY));
    public static final Talent WORK_ETHIC = register(new Talent("work_ethic", 5, () -> new ItemStack(Items.CLOCK), false, false, TalentCategory.UTILITY));

    // 2.0 TacZ Gunnery Talents (hidden when TacZ is absent)
    public static final Talent MARKSMAN = register(new Talent("marksman", 5, () -> new ItemStack(Items.SPYGLASS), false, false, TalentCategory.COMBAT, "tacz"));
    public static final Talent QUICK_RELOAD = register(new Talent("quick_reload", 5, () -> new ItemStack(Items.CROSSBOW), false, false, TalentCategory.COMBAT, "tacz"));
    public static final Talent AMMO_SCAVENGER = register(new Talent("ammo_scavenger", 5, () -> new ItemStack(Items.GUNPOWDER), false, false, TalentCategory.UTILITY, "tacz"));

    // End-Game Talents requiring Dire Treats:
    public static final Talent SKY_FELINE = register(new Talent("sky_feline", 5, () -> new ItemStack(Items.ELYTRA), true, false, TalentCategory.DIRE));
    public static final Talent MOLE_CAT = register(new Talent("mole_cat", 5, () -> new ItemStack(Items.DIAMOND_PICKAXE), true, false, TalentCategory.DIRE));
    public static final Talent FRENZY_FOCUS = register(new Talent("frenzy_focus", 5, () -> new ItemStack(Items.BLAZE_POWDER), true, false, TalentCategory.DIRE));
    public static final Talent ALCHEMY_KITTY = register(new Talent("alchemy_kitty", 5, () -> new ItemStack(Items.BREWING_STAND), true, false, TalentCategory.DIRE));
    public static final Talent CATNIP_CATASTROPHE = register(new Talent("catnip_catastrophe", 5, () -> new ItemStack(Items.TNT), true, false, TalentCategory.DIRE));

    public static Talent register(Talent talent) {
        TALENTS.put(talent.getId(), talent);
        return talent;
    }

    public static Talent get(String id) {
        if (id == null) return null;
        return TALENTS.get(id);
    }

    public static Collection<Talent> getAll() {
        return Collections.unmodifiableCollection(TALENTS.values());
    }

    public static List<Talent> getByCategory(TalentCategory category) {
        List<Talent> list = new ArrayList<>();
        for (Talent t : TALENTS.values()) {
            if (t.getCategory() == category && t.isAvailable()) list.add(t);
        }
        return Collections.unmodifiableList(list);
    }
}
