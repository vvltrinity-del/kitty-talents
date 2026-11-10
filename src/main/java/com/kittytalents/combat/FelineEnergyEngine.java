package com.kittytalents.combat;

import com.kittytalents.entity.TalentedCat;
import com.kittytalents.talent.Talents;
import net.minecraft.nbt.CompoundTag;

/**
 * Feline Energy engine for Kitty Talents 3.0 (R5).
 * Governs energy pools, regeneration engines, out-of-combat recovery,
 * combat refunds, and usage across AI and Twining.
 */
public class FelineEnergyEngine {

    public static final String KEY_ENERGY = "KT_Energy";
    public static final String KEY_MAX_ENERGY = "KT_MaxEnergy";
    public static final String KEY_OOC_TICKS = "KT_OocTicks";

    /**
     * Calculates the maximum energy capacity for a cat based on level, talents, and role.
     */
    public static int getMaxEnergy(TalentedCat cat) {
        if (cat == null) return 100;
        int max = 100;

        // Level scaling: +2 Max Energy per level
        max += Math.min(100, cat.getCatLevel() * 2);

        // Talent bonuses
        int agility = cat.getTalentLevel(Talents.FELINE_AGILITY);
        max += agility * 10;

        int frenzy = cat.getTalentLevel(Talents.FRENZY_FOCUS);
        max += frenzy * 15;

        // Role bonuses
        CatRole role = cat.getCatRole();
        if (role == CatRole.MAGE) {
            max += 30;
        } else if (role == CatRole.SUPPORT || role == CatRole.HEALER) {
            max += 20;
        } else if (role == CatRole.MOBILITY) {
            max += 15;
        }

        return max;
    }

    /**
     * Gets current energy, bounded by max energy.
     */
    public static int getEnergy(TalentedCat cat) {
        if (cat == null) return 0;
        int current = cat.getEnergy();
        int max = getMaxEnergy(cat);
        if (current > max) {
            current = max;
            cat.setEnergy(current);
        }
        return current;
    }

    /**
     * Consumes an exact amount of energy. Returns true if successful.
     */
    public static boolean consumeEnergy(TalentedCat cat, int amount) {
        if (cat == null || amount <= 0) return true;
        
        // Mobility role consumes 25% less energy
        if (cat.getCatRole() == CatRole.MOBILITY) {
            amount = Math.max(1, (int) (amount * 0.75f));
        }

        int current = getEnergy(cat);
        if (current >= amount) {
            cat.setEnergy(current - amount);
            // Reset out of combat counter
            cat.resetOutOfCombatTicks();
            return true;
        }
        return false;
    }

    /**
     * Restores energy to the cat.
     */
    public static void restoreEnergy(TalentedCat cat, int amount) {
        if (cat == null || amount <= 0) return;
        int max = getMaxEnergy(cat);
        int current = getEnergy(cat);
        cat.setEnergy(Math.min(max, current + amount));
    }

    /**
     * Called every server tick from LivingEvent.LivingTickEvent to regenerate energy.
     */
    public static void tick(TalentedCat cat) {
        if (cat == null || cat.level().isClientSide || !cat.isAlive() || cat.isIncapacitated()) {
            return;
        }

        cat.incrementOutOfCombatTicks();
        int oocTicks = cat.getOutOfCombatTicks();

        int max = getMaxEnergy(cat);
        int current = cat.getEnergy();
        if (current >= max) {
            if (current > max) cat.setEnergy(max);
            return;
        }

        // Determine regen interval based on state
        boolean isResting = cat.isOrderedToSit() || cat.isSleeping();
        boolean isOutOfCombat = oocTicks > 60; // 3 seconds without taking/dealing damage

        int regenInterval = 10; // Default: every 0.5s (+1 energy = 2 energy/sec)
        int regenAmount = 1;

        if (isResting) {
            regenAmount = 3; // +6 energy/sec while resting
        } else if (isOutOfCombat) {
            regenAmount = 2; // +4 energy/sec out of combat
        }

        // Role multiplier
        float roleMult = cat.getCatRole().getEnergyRegenMultiplier();
        if (roleMult > 1.2f && cat.tickCount % 20 == 0) {
            regenAmount += 1;
        }

        // Talent: Purr Medic gives extra energy recovery
        int purrMedic = cat.getTalentLevel(Talents.PURR_MEDIC);
        if (purrMedic > 0 && cat.tickCount % (30 - purrMedic * 4) == 0) {
            regenAmount += 1;
        }

        // Talent: Happy Eater bonus when well fed
        int happyEater = cat.getTalentLevel(Talents.HAPPY_EATER);
        if (happyEater > 0 && cat.getHunger() > 80 && cat.tickCount % 20 == 0) {
            regenAmount += 1;
        }

        // 4.0 Phase 6: Playful cats recover Energy faster (an extra point on a share of the regen ticks).
        if (cat.getTrait() == com.kittytalents.core.living.CatTrait.PLAYFUL) {
            double extra = (com.kittytalents.core.living.CatTrait.PLAYFUL_ENERGY.get() - 1.0) * regenAmount;
            int every = extra > 0 ? (int) Math.max(1, Math.round(1.0 / Math.min(1.0, extra))) : 0;
            if (every > 0 && (cat.tickCount / regenInterval) % every == 0) regenAmount += (int) Math.max(1, Math.floor(extra));
        }

        if (cat.tickCount % regenInterval == 0) {
            cat.setEnergy(Math.min(max, current + regenAmount));
        }
    }
}
