package com.kittytalents.ai.goal;

import com.kittytalents.entity.TalentedCat;
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
        return allowed() && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return allowed() && super.canContinueToUse();
    }
}
