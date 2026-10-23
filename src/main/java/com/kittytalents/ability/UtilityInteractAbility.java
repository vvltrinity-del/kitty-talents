package com.kittytalents.ability;

import com.kittytalents.combat.FelineElement;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.talent.Talents;
import net.minecraft.world.entity.player.Player;

/**
 * Utility interaction ability (R4, R8).
 * Handles manual torch placement and equipped tool usage.
 */
public class UtilityInteractAbility extends CatAbility {

    public UtilityInteractAbility() {
        super("utility_interact", "Utility Action", "Performs utility actions such as placing torches or using equipped tools.",
                10, 20, FelineElement.PHYSICAL, Category.UTILITY);
    }

    @Override
    protected boolean execute(TalentedCat cat, Player player) {
        if (cat.getTalentLevel(Talents.TORCH_KITTY) > 0) {
            cat.tryPlaceTorchManual();
            return true;
        } else if (cat.getTalentLevel(Talents.KITTY_TOOLS) > 0) {
            cat.tryUseEquippedTool();
            return true;
        }
        return false;
    }
}
