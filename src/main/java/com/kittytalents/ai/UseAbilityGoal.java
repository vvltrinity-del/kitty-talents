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

    private boolean evaluateAbilities() {
        CatAbility best = null;
        float highestPriority = 0;

        String[] slots = {cat.getAbilitySlot1(), cat.getAbilitySlot2(), cat.getAbilitySlot3(), cat.getAbilityUltimate()};
        for (String id : slots) {
            if (id != null && !id.isEmpty()) {
                CatAbility abil = CatAbilities.get(id);
                if (abil != null && cat.getEnergy() >= abil.getEnergyCost() && (cat.getAbilityCooldown(id) + abil.getCooldownTicks()) <= cat.level().getGameTime()) {
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
