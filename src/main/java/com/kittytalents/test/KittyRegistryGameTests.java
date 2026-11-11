package com.kittytalents.test;

import com.kittytalents.KittyTalents;
import com.kittytalents.api.KittyTalentsAPI;
import com.kittytalents.entity.CatMode;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.network.PacketChangeCatMode;
import com.kittytalents.network.PacketSetCatJob;
import com.kittytalents.registry.ModEntities;
import com.kittytalents.work.AbstractCatJobGoal;
import com.kittytalents.work.CatJob;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static com.kittytalents.test.KittyTalentGameTests.joinFakeOwner;
import static com.kittytalents.test.KittyTalentGameTests.leave;

/** 4.0 Phase 8: modes and jobs are registries. {@link TestAddon} adds one of each through the API only. */
@GameTestHolder(KittyTalents.MODID)
@PrefixGameTestTemplate(false)
public class KittyRegistryGameTests {

    private static final String[] BUILTIN_MODES = {"DOCILE", "STAY", "FOLLOW", "WANDER", "GUARD", "HUNTING", "AGGRESSIVE",
            "TACTICAL", "SUPPORT", "FETCH", "WORK", "RETURN_HOME"};
    private static final String[] BUILTIN_JOBS = {"none", "farmer", "lumberjack", "miner", "rancher", "courier", "sorter",
            "fisher", "collector", "gunsmith"};

    private static TalentedCat reload(GameTestHelper h, TalentedCat c, BlockPos at) {
        CompoundTag tag = c.saveWithoutId(new CompoundTag());
        tag.remove("UUID");
        TalentedCat loaded = h.spawn(ModEntities.TALENTED_CAT.get(), at);
        loaded.load(tag);
        return loaded;
    }

    /** The built-in lists keep their order and numbers; addon entries come after them. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testBuiltinOrderIsUnchanged(GameTestHelper h) {
        CatMode[] modes = CatMode.values();
        for (int i = 0; i < BUILTIN_MODES.length; i++) {
            h.assertTrue(modes[i].name().equals(BUILTIN_MODES[i]) && modes[i].ordinal() == i && modes[i].isBuiltin(), "mode " + i + " should be " + BUILTIN_MODES[i]);
            h.assertTrue(CatMode.byId(i) == modes[i] && CatMode.fromSavedName(BUILTIN_MODES[i]) == modes[i], "lookup of " + BUILTIN_MODES[i]);
        }
        CatJob[] jobs = CatJob.values();
        for (int i = 0; i < BUILTIN_JOBS.length; i++) {
            h.assertTrue(jobs[i].getId().equals(BUILTIN_JOBS[i]) && jobs[i].ordinal() == i && jobs[i].isBuiltin(), "job " + i + " should be " + BUILTIN_JOBS[i]);
            h.assertTrue(CatJob.byOrdinal(i) == jobs[i] && CatJob.fromId(BUILTIN_JOBS[i]) == jobs[i], "lookup of " + BUILTIN_JOBS[i]);
        }
        h.assertTrue(TestAddon.SCOUT.ordinal() >= BUILTIN_MODES.length && !TestAddon.SCOUT.isBuiltin(), "addon modes come after the built-in ones");
        h.assertTrue(TestAddon.COUNTER.ordinal() >= BUILTIN_JOBS.length && !TestAddon.COUNTER.isBuiltin(), "addon jobs come after the built-in ones");
        h.assertTrue(CatMode.FOLLOW.followStartDistance() == 6.0D && CatMode.SUPPORT.followStopDistance() == 2.5D
                && CatMode.TACTICAL.followStartDistance() == 7.0D && CatMode.STAY.isStationary() && !CatMode.GUARD.isStationary(), "built-in mode data");

        // Every job except NONE gives each cat its goal; the nine built-in ones are the usual job goals.
        TalentedCat c = h.spawn(ModEntities.TALENTED_CAT.get(), new BlockPos(2, 2, 2));
        long jobGoals = c.goalSelector.getAvailableGoals().stream().filter(w -> w.getGoal() instanceof AbstractCatJobGoal).count();
        h.assertTrue(jobGoals == 9, "expected 9 built-in job goals on a cat, found " + jobGoals);
        h.assertTrue(CatJob.NONE.createGoal(c) == null, "NONE has no goal");
        h.succeed();
    }

    /** Bad registrations are refused instead of silently replacing something. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testRegistryRefusesDuplicatesAndBadIds(GameTestHelper h) {
        int modes = CatMode.values().length, jobs = CatJob.values().length;
        Runnable[] bad = {
                () -> KittyTalentsAPI.registerMode(CatMode.builder("follow").build()),
                () -> KittyTalentsAPI.registerMode(CatMode.builder("kttest_scout").build()),
                () -> KittyTalentsAPI.registerMode(CatMode.builder("patrol").build()), // a legacy save name
                () -> CatMode.builder("Bad Id"),
                () -> CatMode.builder("x").followsOwner().anchorRadius(4).build(),
                () -> KittyTalentsAPI.registerJob(CatJob.builder("farmer", "Farmer").goal(c -> null).build()),
                () -> KittyTalentsAPI.registerJob(CatJob.builder("kttest_counter", "Counter").goal(c -> null).build()),
                () -> CatJob.builder("no_goal", "No goal").build(),
                () -> CatJob.builder("Bad-Id", "Bad"),
        };
        for (int i = 0; i < bad.length; i++) {
            boolean refused = false;
            try {
                bad[i].run();
            } catch (IllegalArgumentException e) {
                refused = true;
            }
            h.assertTrue(refused, "bad registration " + i + " was accepted");
        }
        h.assertTrue(CatMode.values().length == modes && CatJob.values().length == jobs, "a refused registration changed the registry");
        h.succeed();
    }

    /** The addon mode: reachable by packet and by cycling, behaves per its data, saved by name. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testAddonMode(GameTestHelper h) {
        FakePlayer owner = joinFakeOwner(h, new BlockPos(3, 2, 2));
        try {
            CatMode scout = TestAddon.SCOUT;
            TalentedCat c = h.spawn(ModEntities.TALENTED_CAT.get(), new BlockPos(2, 2, 2));
            c.tame(owner);
            int id = c.getId();
            h.assertTrue(PacketChangeCatMode.apply(owner, id, scout.ordinal()) && c.getCatMode() == scout, "the addon mode is set by packet");
            h.assertTrue(scout.followsOwner() && !scout.usesAnchor() && scout.canAttack() && scout.isCombatMode() && !scout.retreatsWhenHurt()
                    && !scout.isStationary() && !scout.isWorkMode() && scout.followStartDistance() == 9.0D, "the mode's data");
            h.assertTrue(!c.isOrderedToSit(), "a moving mode stands the cat up");
            h.assertTrue(scout.getKey().endsWith(".kttest_scout") && scout.name().equals("KTTEST_SCOUT"), "names");

            // Cycling: the last built-in mode leads to it, and it wraps round to the first.
            CatMode[] all = CatMode.values();
            h.assertTrue(all[all.length - 1].next() == all[0] && all[0].previous() == all[all.length - 1], "the cycle wraps");
            h.assertTrue(scout.previous().next() == scout, "the addon mode is in the cycle");
            c.setCatMode(scout.previous());
            h.assertTrue(PacketChangeCatMode.apply(owner, id, PacketChangeCatMode.NEXT) && c.getCatMode() == scout, "NEXT reaches the addon mode");

            // Saved by name; a mode whose addon is gone falls back to Follow.
            TalentedCat loaded = reload(h, c, new BlockPos(4, 2, 4));
            h.assertTrue(loaded.getCatMode() == scout, "the addon mode survives save and load, got " + loaded.getCatMode());
            h.assertTrue(CatMode.fromSavedName("KTTEST_SCOUT") == scout && CatMode.fromSavedName("kttest_scout") == scout, "lookup by save name");
            h.assertTrue(CatMode.fromSavedName("REMOVEDMOD_MODE") == CatMode.FOLLOW, "an unknown mode loads as Follow");
            h.succeed();
        } finally {
            leave(h, owner);
        }
    }

    /** The addon job: requirement checked, its goal runs only while it is the cat's job, saved by id. */
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void testAddonJob(GameTestHelper h) {
        FakePlayer owner = joinFakeOwner(h, new BlockPos(3, 2, 2));
        CatJob counter = TestAddon.COUNTER;
        TalentedCat c = h.spawn(ModEntities.TALENTED_CAT.get(), new BlockPos(2, 2, 2));
        TalentedCat idle = h.spawn(ModEntities.TALENTED_CAT.get(), new BlockPos(2, 2, 4));
        try {
            c.tame(owner);
            idle.tame(owner);
            h.assertFalse(counter.canCatPerform(c), "a level 1 cat qualified for a level 5 job");
            c.setCatLevel(5);
            idle.setCatLevel(5);
            h.assertTrue(counter.canCatPerform(c), "a level 5 cat should qualify");
            h.assertTrue(counter.getTitleComponent().getString().contains("kttest_counter") || !counter.getDisplayName().isEmpty(), "title");

            h.assertTrue(PacketSetCatJob.apply(owner, c.getId(), counter.ordinal(), true), "the addon job is set by packet");
            h.assertTrue(c.getCatJob() == counter && c.isClockedIn() && c.getCatMode() == CatMode.WORK, "clocked in to the addon job");
            // The other cat is a Farmer: the addon goal must stay off for it.
            idle.setCatJob(CatJob.FARMER);
            idle.setClockedIn(true);

            TalentedCat loaded = reload(h, c, new BlockPos(4, 2, 4));
            h.assertTrue(loaded.getCatJob() == counter, "the addon job survives save and load, got " + loaded.getCatJob());
            loaded.discard();
            h.assertTrue(CatJob.fromId("removedmod_job") == CatJob.NONE, "an unknown job loads as unemployed");
        } catch (Throwable t) {
            leave(h, owner);
            throw t;
        }
        h.runAfterDelay(40, () -> {
            try {
                int worked = TestAddon.WORKED.getOrDefault(c.getUUID(), 0);
                h.assertTrue(worked >= 20, "the addon job's goal should have run, ticks: " + worked);
                h.assertTrue(!TestAddon.WORKED.containsKey(idle.getUUID()), "the addon goal ran for a Farmer");
                // Clocking out stops it.
                h.assertTrue(PacketSetCatJob.apply(owner, c.getId(), CatJob.NONE.ordinal(), false), "back to unemployed");
                int before = TestAddon.WORKED.getOrDefault(c.getUUID(), 0);
                h.runAfterDelay(20, () -> {
                    try {
                        int after = TestAddon.WORKED.getOrDefault(c.getUUID(), 0);
                        h.assertTrue(after - before <= 1, "the addon goal kept running after the job ended: " + (after - before));
                        h.succeed();
                    } finally {
                        leave(h, owner);
                    }
                });
            } catch (Throwable t) {
                leave(h, owner);
                throw t;
            }
        });
    }
}
