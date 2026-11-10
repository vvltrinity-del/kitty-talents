package com.kittytalents.ai.goal;

import com.kittytalents.entity.TalentedCat;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

/** Close combat (claws, bite, mouth weapon) after the pounce opener. Gun-wielding cats use CatGunAttackGoal. */
public class CatMeleeGoal extends MeleeAttackGoal {
    private final TalentedCat cat;

    public CatMeleeGoal(TalentedCat cat) {
        super(cat, 1.35D, true);
        this.cat = cat;
    }

    private boolean allowed() {
        return !cat.isTwined() && !cat.isIncapacitated() && !cat.isOrderedToSit() && !cat.isRetreating()
                && cat.getCatMode().canAttack() && !cat.hasEquippedGun();
    }

    @Override
    public boolean canUse() {
        if (!allowed()) return false;
        LivingEntity t = cat.getTarget();
        if (t != null && CatStalkGoal.isUnaware(cat, t)) {
            double dist = cat.distanceTo(t);
            // If the target has not noticed the cat and is beyond melee strike range (2.5m),
            // do not charge with melee; allow stalk and pounce to handle the opener.
            if (dist > 2.5D && dist < 24.0D) {
                return false;
            }
        }
        return super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        if (!allowed()) return false;
        LivingEntity t = cat.getTarget();
        if (t != null && CatStalkGoal.isUnaware(cat, t) && cat.distanceTo(t) > 2.5D) {
            return false;
        }
        return super.canContinueToUse();
    }
}
