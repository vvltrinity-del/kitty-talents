package com.kittytalents.ai.goal;

import com.kittytalents.ai.CatTargeting;
import com.kittytalents.entity.TalentedCat;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * The ONLY target selector (Phase 3). Replaces the old central goal plus nine vanilla/custom target goals that used
 * to overwrite each other. All scoring lives in {@link CatTargeting}.
 */
public class CatTargetGoal extends Goal {
    private final TalentedCat cat;
    private int evalCooldown = 0;
    private LivingEntity pending;

    public CatTargetGoal(TalentedCat cat) {
        this.cat = cat;
        this.setFlags(EnumSet.of(Flag.TARGET));
    }

    private boolean blocked() {
        return cat.isTwined() || cat.isIncapacitated() || cat.isOrderedToSit() || cat.isRetreating()
                || !cat.getCatMode().canAttack();
    }

    @Override
    public boolean canUse() {
        if (blocked()) {
            if (cat.getTarget() != null) cat.setTarget(null);
            return false;
        }
        if (evalCooldown > 0) {
            evalCooldown--;
            return false;
        }
        // Re-scan quickly when fighting or near players, slowly when idle and alone.
        boolean playerNear = cat.level().hasNearbyAlivePlayer(cat.getX(), cat.getY(), cat.getZ(), 48.0D);
        evalCooldown = !playerNear ? 15 : (cat.getTarget() != null || cat.getCatMode().isCombatMode() ? 3 : 6);

        pending = CatTargeting.findBestTarget(cat);
        if (pending == null && cat.getTarget() != null && !isStillValid(cat.getTarget())) {
            cat.setTarget(null);
        }
        return pending != null && pending != cat.getTarget();
    }

    @Override
    public void start() {
        if (pending != null) cat.setTarget(pending);
        pending = null;
    }

    @Override
    public boolean canContinueToUse() {
        return false; // one-shot: canUse() re-evaluates on its own cadence while the target persists on the mob
    }

    /** Drop targets that died, became friendly, or ran far out of range. */
    private boolean isStillValid(LivingEntity t) {
        if (!t.isAlive() || CatTargeting.isFriendly(cat, t)) return false;
        double r = CatTargeting.scanRadius(cat) + 8.0D;
        return cat.distanceToSqr(t) <= r * r;
    }

    @Override
    public void tick() {}
}
