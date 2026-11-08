package com.kittytalents.test;

import com.kittytalents.KittyTalents;
import com.kittytalents.ai.CatTargeting;
import com.kittytalents.entity.CatMode;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * Phase 3 AI behavior tests: one or more per mode, the default pounce, bystander safety and retreat.
 * Dummies have no AI and wear a helmet (no sun burning), so every behavior observed is the cat's own.
 */
@GameTestHolder(KittyTalents.MODID)
@PrefixGameTestTemplate(false)
public class KittyAiGameTests {

    private static Zombie dummy(GameTestHelper h, BlockPos pos) {
        Zombie z = h.spawn(EntityType.ZOMBIE, pos);
        z.setNoAi(true);
        z.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
        z.setPersistenceRequired();
        return z;
    }

    private static TalentedCat cat(GameTestHelper h, BlockPos pos, CatMode mode) {
        TalentedCat c = h.spawn(ModEntities.TALENTED_CAT.get(), pos);
        c.setCatMode(mode);
        return c;
    }

    @GameTest(template = "arena", timeoutTicks = 60)
    public static void testOnlyOneTargetSelectorGoal(GameTestHelper h) {
        TalentedCat c = cat(h, new BlockPos(4, 2, 4), CatMode.FOLLOW);
        h.assertTrue(c.targetSelector.getAvailableGoals().size() == 1,
                "Exactly one target goal expected, found " + c.targetSelector.getAvailableGoals().size());
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 60)
    public static void testDocileAndReturnHomeNeverFight(GameTestHelper h) {
        TalentedCat docile = cat(h, new BlockPos(4, 2, 4), CatMode.DOCILE);
        TalentedCat home = cat(h, new BlockPos(20, 2, 4), CatMode.RETURN_HOME);
        Zombie z1 = dummy(h, new BlockPos(6, 2, 4));
        Zombie z2 = dummy(h, new BlockPos(22, 2, 4));
        docile.setLastHurtByMob(z1);
        home.setLastHurtByMob(z2);
        h.runAfterDelay(40, () -> {
            h.assertTrue(docile.getTarget() == null, "DOCILE cat targeted " + docile.getTarget());
            h.assertTrue(home.getTarget() == null, "RETURN_HOME cat targeted " + home.getTarget());
            h.assertTrue(docile.isOrderedToSit(), "DOCILE cat should be sitting");
            h.succeed();
        });
    }

    /** Self-defense modes ignore an unprovoked hostile, then fight back once it attacks them. */
    @GameTest(template = "arena", timeoutTicks = 160)
    public static void testSelfDefenseModesOnlyFightBack(GameTestHelper h) {
        List<CatMode> modes = List.of(CatMode.STAY, CatMode.WANDER, CatMode.SUPPORT, CatMode.FETCH, CatMode.WORK);
        TalentedCat[] cats = new TalentedCat[modes.size()];
        Zombie[] zombies = new Zombie[modes.size()];
        for (int i = 0; i < modes.size(); i++) {
            cats[i] = cat(h, new BlockPos(2 + i * 6, 1, 4), modes.get(i));
            zombies[i] = dummy(h, new BlockPos(2 + i * 6, 1, 7));
        }
        h.runAfterDelay(40, () -> {
            for (int i = 0; i < cats.length; i++) {
                h.assertTrue(cats[i].getTarget() == null, modes.get(i) + " cat attacked an unprovoked hostile");
                cats[i].setLastHurtByMob(zombies[i]);
            }
            h.succeedWhen(() -> {
                for (int i = 0; i < cats.length; i++) {
                    h.assertTrue(cats[i].getTarget() == zombies[i], modes.get(i) + " cat did not fight back");
                }
            });
        });
    }

    @GameTest(template = "arena", timeoutTicks = 100)
    public static void testAggressiveSeeksDistantHostileButFollowDoesNot(GameTestHelper h) {
        TalentedCat aggressive = cat(h, new BlockPos(2, 2, 2), CatMode.AGGRESSIVE);
        TalentedCat follow = cat(h, new BlockPos(29, 2, 2), CatMode.FOLLOW);
        Zombie far = dummy(h, new BlockPos(2, 2, 20));     // 18 blocks from the aggressive cat
        Zombie far2 = dummy(h, new BlockPos(29, 2, 20));   // 18 blocks from the follow cat
        h.runAfterDelay(60, () -> {
            h.assertTrue(aggressive.getTarget() == far || aggressive.getTarget() == far2,
                    "AGGRESSIVE cat should seek a distant hostile, target=" + aggressive.getTarget());
            h.assertTrue(follow.getTarget() == null, "FOLLOW cat must not hunt unprovoked hostiles, target=" + follow.getTarget());
            h.succeed();
        });
    }

    @GameTest(template = "arena", timeoutTicks = 60)
    public static void testGuardDefendsOnlyItsArea(GameTestHelper h) {
        TalentedCat guard = cat(h, new BlockPos(16, 2, 16), CatMode.GUARD);
        Zombie intruder = dummy(h, new BlockPos(20, 2, 16));
        List<net.minecraft.world.entity.player.Player> none = List.of();
        h.assertTrue(CatTargeting.score(guard, intruder, none) > CatTargeting.MIN_SCORE, "Intruder inside the guard area should score");
        guard.setModeAnchor(h.absolutePos(new BlockPos(16, 2, 16)).offset(0, 0, -40)); // post far away
        h.assertTrue(CatTargeting.score(guard, intruder, none) == 0.0F, "Hostile outside the guarded area must not score");
        guard.setModeAnchor(h.absolutePos(new BlockPos(16, 2, 16)));
        h.succeedWhen(() -> h.assertTrue(guard.getTarget() == intruder, "GUARD cat should engage the intruder"));
    }

    @GameTest(template = "arena", timeoutTicks = 80)
    public static void testHuntingTargetsPrey(GameTestHelper h) {
        TalentedCat hunter = cat(h, new BlockPos(4, 2, 4), CatMode.HUNTING);
        Mob chicken = h.spawn(EntityType.CHICKEN, new BlockPos(8, 2, 4));
        chicken.setNoAi(true);
        h.succeedWhen(() -> h.assertTrue(hunter.getTarget() == chicken, "HUNTING cat should go after prey"));
    }

    /** Pounce is every cat's default opener: no Pounce talent needed. */
    @GameTest(template = "arena", timeoutTicks = 160)
    public static void testEveryCatPouncesByDefault(GameTestHelper h) {
        TalentedCat c = cat(h, new BlockPos(4, 2, 16), CatMode.AGGRESSIVE);
        h.assertTrue(c.getTalentLevel(com.kittytalents.talent.Talents.POUNCE) == 0, "Test cat must have no Pounce talent");
        dummy(h, new BlockPos(10, 2, 16)); // 6 blocks: inside default pounce range
        h.succeedWhen(() -> h.assertTrue(c.getPouncesPerformed() > 0, "Cat with no Pounce talent never pounced: " + pounceBlocker(c)));
    }

    private static String pounceBlocker(TalentedCat c) {
        return c.goalSelector.getAvailableGoals().stream()
                .filter(w -> w.getGoal() instanceof com.kittytalents.ai.goal.CatPounceGoal)
                .map(w -> String.valueOf(((com.kittytalents.ai.goal.CatPounceGoal) w.getGoal()).whyNot()))
                .findFirst().orElse("CatPounceGoal not registered");
    }

    /** Pounce landing damage must not hit bystanders standing next to the target. */
    @GameTest(template = "arena", timeoutTicks = 200)
    public static void testPounceLandingSparesBystanders(GameTestHelper h) {
        TalentedCat c = cat(h, new BlockPos(4, 2, 16), CatMode.AGGRESSIVE);
        dummy(h, new BlockPos(10, 2, 16));
        Mob villager = h.spawn(EntityType.VILLAGER, new BlockPos(10, 2, 17));
        villager.setNoAi(true);
        Mob pig = h.spawn(EntityType.PIG, new BlockPos(11, 2, 16));
        pig.setNoAi(true);
        h.succeedWhen(() -> {
            h.assertTrue(c.getPouncesPerformed() > 0 && !c.combat().pounceActive, "waiting for a completed pounce");
            h.assertTrue(villager.getHealth() == villager.getMaxHealth(), "Pounce landing hurt a villager");
            h.assertTrue(pig.getHealth() == pig.getMaxHealth(), "Pounce landing hurt a pig");
        });
    }

    @GameTest(template = "arena", timeoutTicks = 100)
    public static void testLowHealthRetreatExceptAggressive(GameTestHelper h) {
        TalentedCat follow = cat(h, new BlockPos(4, 2, 4), CatMode.FOLLOW);
        TalentedCat berserk = cat(h, new BlockPos(24, 2, 4), CatMode.AGGRESSIVE);
        Zombie z1 = dummy(h, new BlockPos(6, 2, 4));
        Zombie z2 = dummy(h, new BlockPos(26, 2, 4));
        for (TalentedCat c : List.of(follow, berserk)) c.setHealth(c.getMaxHealth() * 0.15F);
        follow.setLastHurtByMob(z1);
        berserk.setLastHurtByMob(z2);
        follow.setTarget(z1);
        berserk.setTarget(z2);
        // Energy 0 so neither cat heals itself out of the low-health window with an ability first.
        follow.setEnergy(0);
        berserk.setEnergy(0);
        boolean[] followRetreated = {false}, followDroppedTarget = {false}, berserkRetreated = {false};
        h.onEachTick(() -> {
            if (follow.isRetreating()) {
                followRetreated[0] = true;
                if (follow.getTarget() == null) followDroppedTarget[0] = true;
            }
            if (berserk.isRetreating()) berserkRetreated[0] = true;
        });
        h.runAfterDelay(40, () -> {
            h.assertTrue(followRetreated[0], "Badly hurt FOLLOW cat should retreat (health "
                    + follow.getHealth() + "/" + follow.getMaxHealth() + ", energy " + follow.getEnergy() + ")");
            h.assertTrue(followDroppedTarget[0], "Retreating cat should drop its target");
            h.assertFalse(berserkRetreated[0], "AGGRESSIVE cats never retreat");
            h.succeed();
        });
    }

    /** STAY returns to where it was ordered to stay. */
    @GameTest(template = "arena", timeoutTicks = 200)
    public static void testStayReturnsToItsAnchor(GameTestHelper h) {
        TalentedCat c = cat(h, new BlockPos(8, 2, 8), CatMode.STAY);
        BlockPos anchor = c.getModeAnchor();
        h.assertTrue(anchor != null && anchor.equals(h.absolutePos(new BlockPos(8, 2, 8))), "STAY should anchor where ordered, got " + anchor);
        BlockPos away = h.absolutePos(new BlockPos(16, 2, 8));
        c.teleportTo(away.getX() + 0.5, away.getY(), away.getZ() + 0.5);
        h.succeedWhen(() -> h.assertTrue(c.distanceToSqr(anchor.getX() + 0.5, anchor.getY(), anchor.getZ() + 0.5) < 9.0,
                "STAY cat should walk back to its anchor"));
    }

    /** Switching a clocked-in worker to another mode clocks it out (seen in playtest: BERSERKER + Clocked In). */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testLeavingWorkClocksOut(GameTestHelper h) {
        TalentedCat c = cat(h, new BlockPos(2, 2, 2), CatMode.FOLLOW);
        c.setCatJob(com.kittytalents.work.CatJob.FARMER);
        c.setClockedIn(true);
        h.assertTrue(c.getCatMode() == CatMode.WORK, "clocking in should switch to WORK");
        c.setCatMode(CatMode.AGGRESSIVE);
        h.assertFalse(c.isClockedIn(), "Leaving WORK must clock the cat out");
        h.succeed();
    }

    @SuppressWarnings("unused")
    private static boolean alive(LivingEntity e) { return e != null && e.isAlive(); }
}
