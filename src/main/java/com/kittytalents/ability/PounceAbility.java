package com.kittytalents.ability;

import com.kittytalents.combat.FelineCombatEngine;
import com.kittytalents.combat.FelineElement;
import com.kittytalents.entity.TalentedCat;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Signature Pounce Ability for Kitty Talents 3.0 (R4, R6).
 * Launches the cat in a true parabolic physics arc toward their target.
 */
public class PounceAbility extends CatAbility {

    public PounceAbility() {
        super("pounce", "Pounce", "Launches the cat forward in a physics leap, dealing AoE impact damage and slowing foes.",
                20, 30, FelineElement.PHYSICAL, Category.SKILL);
    }

    @Override
    public float getAiPriority(TalentedCat cat) {
        if (cat.getTarget() != null) {
            double distSq = cat.distanceToSqr(cat.getTarget());
            if (distSq >= 9.0 && distSq <= 64.0) return 2.0f;
        }
        return 0.5f;
    }

    @Override
    protected boolean execute(TalentedCat cat, Player player) {
        if (!cat.onGround() && !cat.isInWater()) return false;

        Vec3 targetPos = null;
        if (cat.getTarget() != null && cat.getTarget().isAlive()) {
            targetPos = cat.getTarget().position();
        } else if (player != null) {
            targetPos = player.getLookAngle().scale(8.0).add(cat.position());
        }

        return FelineCombatEngine.launchPounce(cat, targetPos);
    }
}
