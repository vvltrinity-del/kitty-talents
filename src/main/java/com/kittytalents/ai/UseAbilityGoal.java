package com.kittytalents.ai;

import com.kittytalents.entity.TalentedCat;
import com.kittytalents.ability.CatAbility;
import com.kittytalents.ability.CatAbilities;
import net.minecraft.world.entity.ai.goal.Goal;
import java.util.EnumSet;

public class UseAbilityGoal extends Goal {
    private final TalentedCat cat;
    private int checkCooldown;

    public UseAbilityGoal(TalentedCat cat) {
        this.cat = cat;
        this.setFlags(EnumSet.of(Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (cat.isTwined() || cat.isIncapacitated()) return false;
        if (checkCooldown > 0) {
            checkCooldown--;
            return false;
        }
        checkCooldown = 10;
        return evaluateAbilities();
    }

    /** Minimum AI priority before an ability is worth spending Energy on. */
    private static final float MIN_PRIORITY = 1.0f;

    private boolean isInCombat() {
        net.minecraft.world.entity.LivingEntity target = cat.getTarget();
        if (target != null && target.isAlive()) return true;
        return cat.getLastHurtByMob() != null && cat.tickCount - cat.getLastHurtByMobTimestamp() < 100;
    }

    private boolean evaluateAbilities() {
        // Only spend Energy in real combat and in modes that are allowed to fight.
        if (!cat.getCatMode().canAttack() || !isInCombat()) return false;

        CatAbility best = null;
        float highestPriority = MIN_PRIORITY;

        String[] slots = {cat.getAbilitySlot1(), cat.getAbilitySlot2(), cat.getAbilitySlot3(), cat.getAbilityUltimate()};
        for (String id : slots) {
            if (id != null && !id.isEmpty()) {
                CatAbility abil = CatAbilities.get(id);
                if (abil != null && abil.getCategory() != CatAbility.Category.UTILITY && cat.getEnergy() >= abil.getEnergyCost() && (cat.getAbilityCooldown(id) + abil.getCooldownTicks()) <= cat.level().getGameTime()) {
                    float priority = abil.getAiPriority(cat);
                    if (priority > highestPriority) {
                        highestPriority = priority;
                        best = abil;
                    }
                }
            }
        }
        
        if (best != null) {
            best.tryCast(cat, null);
            return true;
        }
        return false;
    }
}
