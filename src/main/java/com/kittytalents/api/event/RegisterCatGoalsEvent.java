package com.kittytalents.api.event;

import com.kittytalents.api.CatGoalBand;
import com.kittytalents.entity.TalentedCat;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraftforge.eventbus.api.Event;

/**
 * Fired on the Forge event bus once per Talented Cat, when its AI is built (both sides; check
 * {@code getCat().level().isClientSide()} if the goal is server-only). Add goals here rather than in
 * EntityJoinLevelEvent, which fires again on every chunk reload and dimension change and would stack duplicates.
 */
public class RegisterCatGoalsEvent extends Event {
    private final TalentedCat cat;

    public RegisterCatGoalsEvent(TalentedCat cat) {
        this.cat = cat;
    }

    public TalentedCat getCat() { return cat; }

    /** Adds a movement/behaviour goal in the given band. */
    public void addGoal(CatGoalBand band, Goal goal) {
        cat.goalSelector.addGoal(band.priority(), goal);
    }

    /** Adds a target-selection goal; it runs after the built-in target goal (priority 1). */
    public void addTargetGoal(Goal goal) {
        cat.targetSelector.addGoal(2, goal);
    }
}
