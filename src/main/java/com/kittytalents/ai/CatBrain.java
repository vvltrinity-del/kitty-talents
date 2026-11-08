package com.kittytalents.ai;

import com.kittytalents.ai.goal.*;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.entity.ai.*;
import com.kittytalents.work.jobs.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.player.Player;

/**
 * The ONE place a TalentedCat's AI is assembled (Phase 3). Called from TalentedCat#registerGoals on every construction.
 *
 * <h2>Priority bands (lower runs first; a goal can only take MOVE/LOOK/JUMP from a higher-numbered one)</h2>
 * <ol start="0">
 *   <li>{@link #SAFETY}: swimming, downed lock</li>
 *   <li>{@link #ORDERS}: explicit sit, low-health retreat</li>
 *   <li>{@link #COMBAT}: pounce opener, gun combat</li>
 *   <li>{@link #MELEE}: close combat; ability casting (claims no controls, runs alongside)</li>
 *   <li>{@link #COMMAND}: follow owner, hold anchor (stay/guard), return home, fetch</li>
 *   <li>{@link #WORK}: assigned jobs (WORK mode only)</li>
 *   <li>{@link #NEEDS}: food bowl, bed, scratcher, breeding</li>
 *   <li>{@link #IDLE}: wandering, grooming, torch placing, looking around</li>
 * </ol>
 * Goals with no control flags (backpack feeding, comfort, creeper warding, grooming look) never block anything.
 * Targeting is a single goal: {@link CatTargetGoal}.
 */
public final class CatBrain {
    public static final int SAFETY = 0;
    public static final int ORDERS = 1;
    public static final int COMBAT = 2;
    public static final int MELEE = 3;
    public static final int COMMAND = 4;
    public static final int WORK = 5;
    public static final int NEEDS = 6;
    public static final int IDLE = 7;

    private CatBrain() {}

    public static void install(TalentedCat cat) {
        GoalSelector g = cat.goalSelector;

        g.addGoal(SAFETY, new FloatGoal(cat));
        g.addGoal(SAFETY, new CatDownedSafetyGoal(cat));

        g.addGoal(ORDERS, new SitWhenOrderedToGoal(cat) {
            @Override public boolean canUse() { return super.canUse() && !cat.isTwined(); }
            @Override public boolean canContinueToUse() { return super.canContinueToUse() && !cat.isTwined(); }
        });
        g.addGoal(ORDERS, new CatRetreatGoal(cat));

        g.addGoal(COMBAT, new CatPounceGoal(cat));
        g.addGoal(COMBAT, new CatStalkGoal(cat));   // creep up on unaware targets until in pounce range
        g.addGoal(COMBAT, new CatDodgeGoal(cat));   // no control flags: instant dodge impulse
        g.addGoal(COMBAT, new CatGunAttackGoal(cat, 1.25D, 8.0F, 22.0F));

        g.addGoal(MELEE, new CatMeleeGoal(cat));
        g.addGoal(MELEE, new UseAbilityGoal(cat));

        g.addGoal(COMMAND, new CatFollowGoal(cat));
        g.addGoal(COMMAND, new CatHoldAnchorGoal(cat));
        g.addGoal(COMMAND, new CatReturnHomeGoal(cat));
        g.addGoal(COMMAND, new CatFetchToyGoal(cat));

        g.addGoal(WORK, new FarmerJobGoal(cat));
        g.addGoal(WORK, new LumberjackJobGoal(cat));
        g.addGoal(WORK, new MinerJobGoal(cat));
        g.addGoal(WORK, new RancherJobGoal(cat));
        g.addGoal(WORK, new CourierJobGoal(cat));
        g.addGoal(WORK, new SorterJobGoal(cat));
        g.addGoal(WORK, new FisherJobGoal(cat));
        g.addGoal(WORK, new CollectorJobGoal(cat));
        g.addGoal(WORK, new GunsmithJobGoal(cat));

        g.addGoal(NEEDS, new CatEatFromBowlGoal(cat));
        g.addGoal(NEEDS, new CatBackpackFeedGoal(cat));
        g.addGoal(NEEDS, new CatRestOnBedGoal(cat));
        g.addGoal(NEEDS, new CatUseScratcherGoal(cat));
        g.addGoal(NEEDS, new BreedGoal(cat, 1.0D));

        g.addGoal(IDLE, new CatWanderGoal(cat));
        g.addGoal(IDLE, new CatPlaceTorchGoal(cat));
        g.addGoal(IDLE, new CatGroomGoal(cat));
        g.addGoal(IDLE, new CatComfortGoal(cat));
        g.addGoal(IDLE, new CatCreeperWardenGoal(cat));
        g.addGoal(IDLE + 1, new LookAtPlayerGoal(cat, Player.class, 8.0F) {
            @Override public boolean canUse() { return super.canUse() && !cat.isTwined(); }
        });
        g.addGoal(IDLE + 1, new RandomLookAroundGoal(cat) {
            @Override public boolean canUse() { return super.canUse() && !cat.isTwined(); }
        });

        cat.targetSelector.addGoal(1, new CatTargetGoal(cat));

        // Addons (e.g. KittyTalentsCompat) add their goals into the same bands.
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new com.kittytalents.api.event.RegisterCatGoalsEvent(cat));
    }
}
