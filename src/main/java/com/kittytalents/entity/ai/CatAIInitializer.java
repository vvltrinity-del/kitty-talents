package com.kittytalents.entity.ai;

import com.kittytalents.entity.TalentedCat;
import com.kittytalents.work.jobs.*;

/**
 * Initializes and coordinates all Kitty Talents AI companion goals,
 * centralized targeting, feline behaviors, worker jobs, and safety overrides.
 */
public class CatAIInitializer {

    public static void initializeGoals(TalentedCat cat) {
        if (cat == null) return;
        if (cat.getPersistentData().getBoolean("kt_ai_initialized")) {
            return;
        }
        cat.getPersistentData().putBoolean("kt_ai_initialized", true);

        // Priority 0: Downed / Incapacitated Safety Guard (100% halts all downed pathing)
        cat.goalSelector.addGoal(0, new CatDownedSafetyGoal(cat));

        // Priority 3: Backpack Auto-Feeding
        cat.goalSelector.addGoal(3, new CatBackpackFeedGoal(cat));

        // Priority 3: Autonomous combat ability use (LOOK flag only, so it never blocks movement goals)
        cat.goalSelector.addGoal(3, new com.kittytalents.ai.UseAbilityGoal(cat));

        // Priority 4: Feline Stalking Behavior
        cat.goalSelector.addGoal(4, new CatStalkGoal(cat));

        // Priority 5: Multi-Mode Following, Escorting, and Bodyguarding
        cat.goalSelector.addGoal(5, new CatFollowAndEscortGoal(cat));

        // Priority 6: Territorial Guard, Patrol, Stay
        cat.goalSelector.addGoal(6, new CatPatrolAndGuardGoal(cat));

        // Priority 6: Return Home Navigation
        cat.goalSelector.addGoal(6, new CatReturnHomeGoal(cat));

        // Priority 6: Tactical / Emergency Retreat
        cat.goalSelector.addGoal(6, new CatRetreatGoal(cat));

        // Priority 7: Worker Job Goals (active in CatMode.WORK for the assigned CatJob)
        cat.goalSelector.addGoal(7, new FarmerJobGoal(cat));
        cat.goalSelector.addGoal(7, new LumberjackJobGoal(cat));
        cat.goalSelector.addGoal(7, new MinerJobGoal(cat));
        cat.goalSelector.addGoal(7, new RancherJobGoal(cat));
        cat.goalSelector.addGoal(7, new CourierJobGoal(cat));
        cat.goalSelector.addGoal(7, new SorterJobGoal(cat));
        cat.goalSelector.addGoal(7, new FisherJobGoal(cat));
        cat.goalSelector.addGoal(7, new CollectorJobGoal(cat));
        cat.goalSelector.addGoal(7, new GunsmithJobGoal(cat));

        // Priority 10: Feline Grooming & Wellbeing
        cat.goalSelector.addGoal(10, new CatGroomGoal(cat));

        // Target Selector Priority 1: Centralized Target Selection & Priority Engine (R12)
        cat.targetSelector.addGoal(1, new CatCentralTargetGoal(cat));
    }
}
