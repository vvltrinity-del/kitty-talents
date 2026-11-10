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
            // Any zombie counts: tests run side by side and a 24-block seek can see a neighbouring test's dummy.
            h.assertTrue(aggressive.getTarget() instanceof Zombie,
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
        h.succeedWhen(() -> h.assertTrue(hunter.getTarget() == chicken, "HUNTING cat should go after prey, target=" + hunter.getTarget() + " mode=" + hunter.getCatMode() + " sit=" + hunter.isOrderedToSit()));
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

    /** Combat tab loadout reaches the server: owner can set a valid loadout; invalid ones and strangers are rejected. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testLoadoutPacketValidatesAndAuthorizes(GameTestHelper h) {
        TalentedCat c = cat(h, new BlockPos(2, 2, 2), CatMode.FOLLOW);
        var owner = net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "kt_owner"));
        var stranger = net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "kt_stranger"));
        BlockPos at = h.absolutePos(new BlockPos(2, 2, 3));
        owner.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        stranger.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        c.tame(owner);

        var fire = com.kittytalents.combat.FelineElement.FIRE;
        var role = c.getCatRole();
        var good = new com.kittytalents.core.component.CatLoadout(role, fire, "frost_nova", "venom_fang", "tidal_surge", "tempest_claws");
        h.assertTrue(com.kittytalents.network.PacketSetCatLoadout.apply(owner, c.getId(), good), "Owner's valid loadout was rejected");
        h.assertTrue("frost_nova".equals(c.getAbilitySlot1()) && "tempest_claws".equals(c.getAbilityUltimate())
                && c.getPrimaryElement() == fire, "Valid loadout was not applied on the server");

        var pounceSlot = new com.kittytalents.core.component.CatLoadout(role, fire, "pounce", "venom_fang", "tidal_surge", "tempest_claws");
        var ultInSkill = new com.kittytalents.core.component.CatLoadout(role, fire, "apex_predator", "venom_fang", "tidal_surge", "tempest_claws");
        var duplicate = new com.kittytalents.core.component.CatLoadout(role, fire, "venom_fang", "venom_fang", "tidal_surge", "tempest_claws");
        var skillAsUlt = new com.kittytalents.core.component.CatLoadout(role, fire, "frost_nova", "venom_fang", "tidal_surge", "feline_hiss");
        for (var bad : java.util.List.of(pounceSlot, ultInSkill, duplicate, skillAsUlt)) {
            h.assertFalse(com.kittytalents.network.PacketSetCatLoadout.apply(owner, c.getId(), bad), "Invalid loadout accepted: " + bad);
        }
        h.assertTrue("frost_nova".equals(c.getAbilitySlot1()), "Rejected loadout must not change the cat");

        var other = new com.kittytalents.core.component.CatLoadout(role, fire, "feline_dash", "feline_hiss", "healing_purr", "catnip_frenzy");
        h.assertFalse(com.kittytalents.network.PacketSetCatLoadout.apply(stranger, c.getId(), other), "A stranger changed the loadout");
        h.assertTrue("frost_nova".equals(c.getAbilitySlot1()), "Stranger's loadout must not apply");
        h.succeed();
    }

    /**
     * Stress (Phase 8): tamed cats in mixed modes around their owner, plus 10 hostiles, grown in steps 0 -> 10 -> 50
     * -> 100 inside one test (separate tests ran concurrently and shared the counter). After each step a 60-tick
     * warm-up, then 200 measured ticks of the cats' own tick time (TickCost, only cats spawned here). Logged per step;
     * fails if 100 cats cost more than 25 ms per tick (half the 50 ms budget).
     */
    @GameTest(template = "arena", timeoutTicks = 1200, batch = "stress")
    public static void testHundredCatsTickBudget(GameTestHelper h) {
        CatMode[] modes = {CatMode.FOLLOW, CatMode.GUARD, CatMode.WANDER, CatMode.AGGRESSIVE, CatMode.STAY,
                CatMode.HUNTING, CatMode.SUPPORT, CatMode.DOCILE, CatMode.WORK, CatMode.TACTICAL};
        var owner = KittyTalentGameTests.joinFakeOwner(h, new BlockPos(15, 2, 15));
        for (int i = 0; i < 10; i++) dummy(h, new BlockPos(2 + i * 3, 2, 30));
        var server = h.getLevel().getServer();
        var log = org.slf4j.LoggerFactory.getLogger("KittyTalents/Stress");
        int[] steps = {0, 10, 50, 100};
        int[] spawned = {0};
        long[] startTick = {0};
        double[] lastMs = {0};
        int t = 0;
        for (int step : steps) {
            h.runAfterDelay(t, () -> {
                for (int i = spawned[0]; i < step; i++) {
                    CatMode m = modes[i % modes.length];
                    TalentedCat c = cat(h, new BlockPos(1 + (i % 10) * 3, 2, 1 + (i / 10) * 3), m);
                    c.tame(owner);
                    c.setOrderedToSit(false);
                    c.setCatMode(m);
                    c.tickCostTracked = true;
                }
                spawned[0] = step;
            });
            h.runAfterDelay(t + 60, () -> {
                startTick[0] = server.getTickCount();
                com.kittytalents.util.TickCost.reset();
                com.kittytalents.util.TickCost.enabled = true;
            });
            h.runAfterDelay(t + 260, () -> {
                com.kittytalents.util.TickCost.enabled = false;
                double ticks = Math.max(1, server.getTickCount() - startTick[0]);
                double catMs = com.kittytalents.util.TickCost.nanos() / 1.0e6 / ticks;
                lastMs[0] = catMs;
                log.info("{} cats: {} ms/tick for the cats ({} us per cat), whole server {} ms/tick", step,
                        String.format("%.3f", catMs), String.format("%.1f", step == 0 ? 0 : catMs * 1000.0 / step),
                        String.format("%.2f", server.getAverageTickTime()));
            });
            t += 270;
        }
        h.runAfterDelay(t, () -> {
            KittyTalentGameTests.leave(h, owner);
            h.assertTrue(lastMs[0] < 25.0, "100 cats cost " + lastMs[0] + " ms per tick (budget 25 ms)");
            h.succeed();
        });
    }

    /** FOLLOW: catches up to a distant owner (previously untestable: fake players never resolved as owners). */
    @GameTest(template = "arena", timeoutTicks = 200)
    public static void testFollowCatchesUpToOwner(GameTestHelper h) {
        var owner = KittyTalentGameTests.joinFakeOwner(h, new BlockPos(20, 2, 16));
        TalentedCat c = cat(h, new BlockPos(4, 2, 16), CatMode.FOLLOW);
        c.tame(owner);
        c.setCatMode(CatMode.FOLLOW);
        h.succeedWhen(() -> {
            h.assertTrue(c.getOwner() == owner, "owner must resolve");
            h.assertTrue(c.distanceTo(owner) < 4.5F, "FOLLOW cat should reach its owner, distance " + c.distanceTo(owner));
            KittyTalentGameTests.leave(h, owner);
        });
    }

    /** FOLLOW: defends the owner against whatever attacked them. */
    @GameTest(template = "arena", timeoutTicks = 100)
    public static void testFollowDefendsOwner(GameTestHelper h) {
        var owner = KittyTalentGameTests.joinFakeOwner(h, new BlockPos(8, 2, 8));
        TalentedCat c = cat(h, new BlockPos(6, 2, 8), CatMode.FOLLOW);
        c.tame(owner);
        c.setCatMode(CatMode.FOLLOW);
        Zombie attacker = dummy(h, new BlockPos(12, 2, 8));
        Zombie bystander = dummy(h, new BlockPos(8, 2, 14));
        owner.setLastHurtByMob(attacker);
        h.succeedWhen(() -> {
            h.assertTrue(c.getTarget() == attacker, "FOLLOW cat should go after its owner's attacker, target=" + c.getTarget());
            h.assertTrue(bystander.isAlive(), "bystander untouched");
            KittyTalentGameTests.leave(h, owner);
        });
    }

    /** Playtest regression: on Return Home the cat must curl up on its bed and STAY lying (it used to flicker off). */
    @GameTest(template = "arena", timeoutTicks = 300)
    public static void testReturnHomeSleepsAndStaysAsleep(GameTestHelper h) {
        BlockPos bedRel = new BlockPos(8, 2, 8);
        h.setBlock(bedRel, com.kittytalents.registry.ModBlocks.CAT_BED.get());
        TalentedCat c = cat(h, new BlockPos(12, 2, 8), CatMode.FOLLOW);
        c.setHomeBedPos(h.absolutePos(bedRel));
        c.setCatMode(CatMode.RETURN_HOME);
        int[] lyingStreak = {0};
        h.onEachTick(() -> lyingStreak[0] = c.isLying() ? lyingStreak[0] + 1 : 0);
        h.succeedWhen(() -> h.assertTrue(lyingStreak[0] >= 60,
                "Cat should lie on its bed for 3 s straight (streak " + lyingStreak[0] + ", lying=" + c.isLying()
                        + ", sitOrder=" + c.isOrderedToSit() + ", dist=" + Math.sqrt(c.distanceToSqr(h.absoluteVec(bedRel.getCenter()))) + ")"));
    }

    /** Playtest request: creep up crouched on an unaware target, then pounce once in range. */
    @GameTest(template = "arena", timeoutTicks = 400, requiredSuccesses = 30, attempts = 30)
    public static void testStalksUnawareTargetThenPounces(GameTestHelper h) {
        TalentedCat c = cat(h, new BlockPos(2, 2, 16), CatMode.HUNTING);
        Zombie z = dummy(h, new BlockPos(18, 2, 16)); // 16 blocks: beyond pounce range
        c.setTarget(z);
        boolean[] stalkedFar = {false};
        java.util.ArrayDeque<String> trail = new java.util.ArrayDeque<>(); // recent state, printed on failure
        int[] tick = {0};
        h.onEachTick(() -> {
            if (c.getTarget() != z && z.isAlive()) c.setTarget(z);
            if (c.isCrouching() && c.distanceTo(z) > com.kittytalents.ai.goal.CatPounceGoal.maxRange(c) + 1) stalkedFar[0] = true;
            if (tick[0]++ % 20 == 0) {
                if (trail.size() >= 8) trail.removeFirst();
                trail.addLast("t" + tick[0] + " d=" + String.format("%.1f", c.distanceTo(z)) + " ground=" + c.onGround()
                        + " y=" + String.format("%.2f", c.getY() - h.absolutePos(BlockPos.ZERO).getY())
                        + " goals=" + c.goalSelector.getRunningGoals().map(w -> w.getGoal().getClass().getSimpleName()).toList());
            }
        });
        h.succeedWhen(() -> {
            h.assertTrue(stalkedFar[0], "Cat should creep up crouched while out of pounce range");
            // Usually a pounce; occasionally melee wins the race once in range (see OVERHAUL_PLAN follow-ups).
            h.assertTrue(c.getPouncesPerformed() > 0,
                    "Cat should pounce after the stalk: " + pounceBlocker(c) + " | " + trail);
        });
    }

    /** Playtest request: AI cats dodge on their own (it used to be Twine-only). */
    @GameTest(template = "arena", timeoutTicks = 60)
    public static void testDodgesIncomingArrow(GameTestHelper h) {
        TalentedCat c = cat(h, new BlockPos(8, 2, 8), CatMode.FOLLOW);
        c.setEnergy(100);
        int energy = c.getEnergy();
        var arrow = new net.minecraft.world.entity.projectile.Arrow(h.getLevel(), 0, 0, 0);
        net.minecraft.world.phys.Vec3 from = h.absoluteVec(new net.minecraft.world.phys.Vec3(8.5, 3.0, 13.5));
        arrow.setPos(from);
        arrow.setNoGravity(true);
        arrow.shoot(0, -0.1, -1, 0.6F, 0.0F); // slowly towards the cat
        h.getLevel().addFreshEntity(arrow);
        h.succeedWhen(() -> h.assertTrue(c.combat().dodgeEnd > 0 && c.getEnergy() < energy + 5,
                "Cat should dodge the incoming arrow (dodgeEnd=" + c.combat().dodgeEnd + ")"));
    }

    @SuppressWarnings("unused")
    private static boolean alive(LivingEntity e) { return e != null && e.isAlive(); }
}
