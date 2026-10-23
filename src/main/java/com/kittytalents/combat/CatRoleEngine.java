package com.kittytalents.combat;

import com.kittytalents.entity.TalentedCat;
import com.kittytalents.talent.Talents;

/**
 * Evaluates a TalentedCat's build (talents, abilities, attributes)
 * and dynamically calculates their emergent combat role (R3).
 */
public class CatRoleEngine {

    /**
     * Calculates the most fitting CatRole based on the cat's talent investments.
     */
    public static CatRole calculateRole(TalentedCat cat) {
        if (cat == null) return CatRole.NONE;

        int tankScore = 0;
        int assassinScore = 0;
        int bruiserScore = 0;
        int dpsScore = 0;
        int healerScore = 0;
        int supportScore = 0;
        int controllerScore = 0;
        int breakerScore = 0;
        int scoutScore = 0;
        int mobilityScore = 0;
        int mageScore = 0;

        // Tank Talents: Guardian Cat, Cat Armor, Nine Lives, Pillow Cat, Water Pelt
        tankScore += cat.getTalentLevel(Talents.GUARDIAN_CAT) * 4;
        tankScore += cat.getTalentLevel(Talents.CAT_ARMOR) * 3;
        tankScore += cat.getTalentLevel(Talents.NINE_LIVES) * 2;
        tankScore += cat.getTalentLevel(Talents.PILLOW_CAT) * 2;
        tankScore += cat.getTalentLevel(Talents.WATER_PELT) * 2;

        // Assassin Talents: Critical Claws, Night Prowler, Shadow Pelt, Pounce
        assassinScore += cat.getTalentLevel(Talents.CRITICAL_CLAWS) * 4;
        assassinScore += cat.getTalentLevel(Talents.NIGHT_PROWLER) * 3;
        assassinScore += cat.getTalentLevel(Talents.SHADOW_PELT) * 3;
        assassinScore += cat.getTalentLevel(Talents.POUNCE) * 2;

        // Bruiser Talents: Cat Armory, Feline Berserker, Frenzy Focus
        bruiserScore += cat.getTalentLevel(Talents.CAT_ARMORY) * 4;
        bruiserScore += cat.getTalentLevel(Talents.FELINE_BERSERKER) * 4;
        bruiserScore += cat.getTalentLevel(Talents.FRENZY_FOCUS) * 3;

        // DPS Talents: Hunter Cat, Pest Hunter, Inferno Cat, Venom Claws
        dpsScore += cat.getTalentLevel(Talents.HUNTER_CAT) * 3;
        dpsScore += cat.getTalentLevel(Talents.PEST_HUNTER) * 3;
        dpsScore += cat.getTalentLevel(Talents.INFERNO_CAT) * 4;
        dpsScore += cat.getTalentLevel(Talents.VENOM_CLAWS) * 3;

        // Healer Talents: Purr Medic, Rescue Purr
        healerScore += cat.getTalentLevel(Talents.PURR_MEDIC) * 5;
        healerScore += cat.getTalentLevel(Talents.RESCUE_PURR) * 4;

        // Support Talents: Lucky Purr, Charm Whiskers, Happy Eater
        supportScore += cat.getTalentLevel(Talents.LUCKY_PURR) * 4;
        supportScore += cat.getTalentLevel(Talents.CHARM_WHISKERS) * 3;
        supportScore += cat.getTalentLevel(Talents.HAPPY_EATER) * 3;

        // Controller Talents: Feline Hiss, Gale Purr, Catnip Catastrophe
        controllerScore += cat.getTalentLevel(Talents.FELINE_HISS) * 4;
        controllerScore += cat.getTalentLevel(Talents.GALE_PURR) * 3;
        controllerScore += cat.getTalentLevel(Talents.CATNIP_CATASTROPHE) * 4;

        // Breaker Talents: Mole Cat, Kitty Tools
        breakerScore += cat.getTalentLevel(Talents.MOLE_CAT) * 5;
        breakerScore += cat.getTalentLevel(Talents.KITTY_TOOLS) * 3;

        // Scout Talents: Whisker Radar, Home Whiskers, Sniffer Whiskers, Paw Retriever
        scoutScore += cat.getTalentLevel(Talents.WHISKER_RADAR) * 4;
        scoutScore += cat.getTalentLevel(Talents.HOME_WHISKERS) * 3;
        scoutScore += cat.getTalentLevel(Talents.SNIFFER_WHISKERS) * 3;
        scoutScore += cat.getTalentLevel(Talents.PAW_RETRIEVER) * 3;

        // Mobility Talents: Feline Agility, Cushion Paws, Sky Feline, Feline Reflexes
        mobilityScore += cat.getTalentLevel(Talents.FELINE_AGILITY) * 4;
        mobilityScore += cat.getTalentLevel(Talents.CUSHION_PAWS) * 3;
        mobilityScore += cat.getTalentLevel(Talents.SKY_FELINE) * 4;
        mobilityScore += cat.getTalentLevel(Talents.FELINE_REFLEXES) * 3;

        // Mage / Elemental Talents: Aqua Kitty, Alchemy Kitty, Fireproof Paws
        mageScore += cat.getTalentLevel(Talents.AQUA_KITTY) * 4;
        mageScore += cat.getTalentLevel(Talents.ALCHEMY_KITTY) * 4;
        mageScore += cat.getTalentLevel(Talents.FIREPROOF_PAWS) * 2;

        int maxScore = 0;
        CatRole bestRole = CatRole.NONE;

        int[] scores = {tankScore, assassinScore, bruiserScore, dpsScore, healerScore,
                        supportScore, controllerScore, breakerScore, scoutScore,
                        mobilityScore, mageScore};
        CatRole[] roles = {CatRole.TANK, CatRole.ASSASSIN, CatRole.BRUISER, CatRole.DPS,
                           CatRole.HEALER, CatRole.SUPPORT, CatRole.CONTROLLER, CatRole.BREAKER,
                           CatRole.SCOUT, CatRole.MOBILITY, CatRole.MAGE};

        int activeCategories = 0;
        for (int i = 0; i < scores.length; i++) {
            if (scores[i] >= 6) {
                activeCategories++;
            }
            if (scores[i] > maxScore) {
                maxScore = scores[i];
                bestRole = roles[i];
            }
        }

        if (maxScore < 4) {
            return CatRole.NONE;
        }

        // If the cat has strong investments in 3 or more distinct roles with no single runaway score, they are a Hybrid
        if (activeCategories >= 3 && maxScore <= 15) {
            return CatRole.HYBRID;
        }

        return bestRole;
    }

    /**
     * Updates and sets the cat's role dynamically if the cat has NONE or if requested.
     */
    public static void autoUpdateRole(TalentedCat cat) {
        if (cat == null) return;
        CatRole computed = calculateRole(cat);
        if (computed != CatRole.NONE && cat.getCatRole() != computed) {
            cat.setCatRole(computed);
        }
    }
}
