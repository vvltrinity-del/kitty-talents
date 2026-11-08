package com.kittytalents.ai.goal;

import com.kittytalents.entity.CatMode;
import com.kittytalents.entity.TalentedCat;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;

/** Free roaming for WANDER, and searching for something to fight for HUNTING / AGGRESSIVE when idle. */
public class CatWanderGoal extends WaterAvoidingRandomStrollGoal {
    private final TalentedCat cat;

    public CatWanderGoal(TalentedCat cat) {
        super(cat, 1.0D);
        this.cat = cat;
    }

    private boolean allowed() {
        CatMode m = cat.getCatMode();
        return (m == CatMode.WANDER || m == CatMode.HUNTING || m == CatMode.AGGRESSIVE)
                && !cat.isTwined() && !cat.isIncapacitated() && !cat.isOrderedToSit() && cat.getTarget() == null;
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
