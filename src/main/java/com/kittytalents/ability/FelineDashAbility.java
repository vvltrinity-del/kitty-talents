package com.kittytalents.ability;

import com.kittytalents.combat.FelineCombatEngine;
import com.kittytalents.combat.FelineElement;
import com.kittytalents.combat.FelineStatusEffects;
import com.kittytalents.combat.FelineStatusType;
import com.kittytalents.entity.TalentedCat;
import net.minecraft.world.entity.player.Player;

/**
 * Feline Dash active skill (R4, R6).
 * Swift burst evasion triggering a forward dodge with brief invulnerability.
 */
public class FelineDashAbility extends CatAbility {

    public FelineDashAbility() {
        super("feline_dash", "Feline Dash", "Performs an evasive forward leap with invulnerability frames, granting Haste.",
                15, 40, FelineElement.PHYSICAL, Category.SKILL);
    }

    @Override
    public float getAiPriority(TalentedCat cat) {
        if (cat.getHealth() < cat.getMaxHealth() * 0.4f) return 2.0f;
        return 0.4f;
    }

    @Override
    protected boolean execute(TalentedCat cat, Player player) {
        boolean success = FelineCombatEngine.performDodge(cat, FelineCombatEngine.DodgeDirection.FORWARD, false);
        if (success) {
            FelineStatusEffects.applyStatus(cat, FelineStatusType.HASTE, 1, 60);
        }
        return success;
    }
}
