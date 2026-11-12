package com.kittytalents.ability;

import com.kittytalents.combat.FelineElement;

import java.util.*;

/**
 * Registry and catalog of all active abilities, ultimates, and utility skills in Kitty Talents 3.0 (R4).
 */
public class CatAbilities {
    private static final Map<String, CatAbility> REGISTRY = new LinkedHashMap<>();

    // Physical & Signature Skills
    public static final CatAbility FELINE_HISS = register(new FelineHissAbility());
    public static final CatAbility FELINE_DASH = register(new FelineDashAbility());
    public static final CatAbility FORTRESS_GUARD = register(new FortressGuardAbility());
    public static final CatAbility UTILITY_INTERACT = register(new UtilityInteractAbility());

    // Elemental Skills
    public static final CatAbility HEALING_PURR = register(new HealingPurrAbility());
    public static final CatAbility SHADOW_STRIKE = register(new ShadowStrikeAbility());
    public static final CatAbility VOLT_DASH = register(new VoltDashAbility());
    public static final CatAbility INFERNO_SPIN = register(new InfernoSpinAbility());
    public static final CatAbility FROST_NOVA = register(new FrostNovaAbility());
    public static final CatAbility TIDAL_SURGE = register(new TidalSurgeAbility());
    public static final CatAbility VENOM_FANG = register(new VenomFangAbility());
    // 4.0: Frost, Poison and Arcane had the fewest options
    public static final CatAbility ICE_LANCE = register(new IceLanceAbility());
    public static final CatAbility TOXIC_CLOUD = register(new ToxicCloudAbility());
    public static final CatAbility ARCANE_BLINK = register(new ArcaneBlinkAbility());

    // Ultimates
    public static final CatAbility CATNIP_FRENZY = register(new CatnipFrenzyUltimate());
    public static final CatAbility SOUL_FLAME_BARRAGE = register(new SoulFlameBarrageUltimate());
    public static final CatAbility APEX_PREDATOR = register(new ApexPredatorUltimate());
    public static final CatAbility CELESTIAL_PURR = register(new CelestialPurrUltimate());
    public static final CatAbility TEMPEST_CLAWS = register(new TempestClawsUltimate());
    public static final CatAbility BLIZZARD = register(new BlizzardUltimate());
    public static final CatAbility PLAGUE_BLOOM = register(new PlagueBloomUltimate());
    public static final CatAbility STARFALL = register(new StarfallUltimate());

    public static CatAbility register(CatAbility ability) {
        REGISTRY.put(ability.getId(), ability);
        return ability;
    }

    public static CatAbility get(String id) {
        if (id == null || id.trim().isEmpty()) return null;
        return REGISTRY.get(id.trim().toLowerCase());
    }

    public static Collection<CatAbility> getAll() {
        return Collections.unmodifiableCollection(REGISTRY.values());
    }

    public static List<CatAbility> getSkills() {
        List<CatAbility> list = new ArrayList<>();
        for (CatAbility a : REGISTRY.values()) {
            if (a.getCategory() == CatAbility.Category.SKILL) list.add(a);
        }
        return Collections.unmodifiableList(list);
    }

    public static List<CatAbility> getUltimates() {
        List<CatAbility> list = new ArrayList<>();
        for (CatAbility a : REGISTRY.values()) {
            if (a.getCategory() == CatAbility.Category.ULTIMATE) list.add(a);
        }
        return Collections.unmodifiableList(list);
    }

    /**
     * Recommends a default signature ability matching the cat's primary element.
     */
    public static CatAbility getDefaultAbilityForElement(FelineElement element) {
        if (element == null) return FELINE_DASH;
        return switch (element) {
            case FIRE -> INFERNO_SPIN;
            case FROST -> FROST_NOVA;
            case WATER -> TIDAL_SURGE;
            case ELECTRIC -> VOLT_DASH;
            case SHADOW -> SHADOW_STRIKE;
            case POISON -> VENOM_FANG;
            case NATURE -> HEALING_PURR;
            case ARCANE -> ARCANE_BLINK;
            case PHYSICAL -> FELINE_DASH;
        };
    }
}
