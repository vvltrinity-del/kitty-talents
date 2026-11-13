package com.kittytalents.test;

import com.kittytalents.KittyTalents;
import com.kittytalents.block.CatTreeBlock;
import com.kittytalents.block.KittyHabitatBlockEntity;
import com.kittytalents.entity.CatMode;
import com.kittytalents.entity.SkyFlight;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.registry.ModBlocks;
import com.kittytalents.registry.ModEntities;
import com.kittytalents.registry.ModItems;
import com.kittytalents.talent.Talents;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static com.kittytalents.test.KittyTalentGameTests.joinFakeOwner;
import static com.kittytalents.test.KittyTalentGameTests.leave;

/** 5.0 pass 2 bug fixes. */
@GameTestHolder(KittyTalents.MODID)
@PrefixGameTestTemplate(false)
public class KittyFiveFixGameTests {

    /** Placing a Cat Tree the way the item does (only the lower half, then setPlacedBy) builds the top half too. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testCatTreePlacementBuildsTopHalf(GameTestHelper h) {
        BlockPos lower = new BlockPos(3, 2, 3);
        var state = ModBlocks.CAT_TREE.get().defaultBlockState();
        h.setBlock(lower, state);
        BlockPos abs = h.absolutePos(lower);
        state.getBlock().setPlacedBy(h.getLevel(), abs, state, null, new ItemStack(ModItems.CAT_TREE.get()));
        var top = h.getLevel().getBlockState(abs.above());
        h.assertTrue(top.is(ModBlocks.CAT_TREE.get()) && top.getValue(CatTreeBlock.HALF) == DoubleBlockHalf.UPPER,
                "the top half should be placed, found " + top);
        h.succeed();
    }

    /** A ridden Sky Feline's fall belongs to the rider: the server-side AI glide must not change its speed. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testRiddenSkyFelineIsNotAiGlided(GameTestHelper h) {
        var fp = joinFakeOwner(h, new BlockPos(3, 2, 3));
        try {
            TalentedCat c = h.spawn(ModEntities.TALENTED_CAT.get(), new BlockPos(2, 2, 2));
            c.tame(fp);
            c.setCatMode(CatMode.STAY);
            c.setTalentLevel(Talents.SKY_FELINE, 3);
            fp.startRiding(c, true);
            h.assertTrue(c.getControllingPassenger() == fp, "player should be riding");
            c.setOnGround(false);
            c.fallDistance = 5.0F;
            c.setDeltaMovement(0, -0.6D, 0);
            SkyFlight.aiGlide(c);
            h.assertTrue(c.getDeltaMovement().y == -0.6D, "ridden cat was glided by the AI: " + c.getDeltaMovement());
            fp.stopRiding();
            c.setOnGround(false);
            c.fallDistance = 5.0F;
            SkyFlight.aiGlide(c);
            h.assertTrue(c.getDeltaMovement().y > -0.6D, "an unridden falling cat should still glide");
            h.succeed();
        } finally {
            leave(h, fp);
        }
    }

    /** Reloading a Habitat's tag (the client does it on every status update) sets its energy, never adds to it. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testHabitatEnergyReloadDoesNotGrow(GameTestHelper h) {
        var be = new KittyHabitatBlockEntity(BlockPos.ZERO, ModBlocks.KITTY_HABITAT.get().defaultBlockState());
        be.getEnergyStorage().receiveEnergy(300, false);
        CompoundTag tag = be.saveWithoutMetadata();
        be.load(tag);
        be.load(tag);
        h.assertTrue(be.getEnergyStorage().getEnergyStored() == 300, "energy after two reloads: " + be.getEnergyStorage().getEnergyStored());
        h.succeed();
    }

    /** Cats never pick fish or bats as a target, even in Hunting mode (they used to leave a fight to swim after cod). */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testHuntingCatIgnoresFishAndBats(GameTestHelper h) {
        var fp = joinFakeOwner(h, new BlockPos(1, 2, 1));
        try {
            TalentedCat c = h.spawn(ModEntities.TALENTED_CAT.get(), new BlockPos(3, 2, 3));
            c.tame(fp);
            c.setOrderedToSit(false);
            c.setCatMode(CatMode.HUNTING);
            h.spawn(net.minecraft.world.entity.EntityType.COD, new BlockPos(4, 2, 3));
            h.assertTrue(com.kittytalents.ai.CatTargeting.findBestTarget(c) == null, "a cod should not be a target");
            var bat = h.spawn(net.minecraft.world.entity.EntityType.BAT, new BlockPos(2, 3, 3));
            bat.setNoAi(true);
            h.assertTrue(com.kittytalents.ai.CatTargeting.findBestTarget(c) == null, "a bat should not be a target");
            var rabbit = h.spawn(net.minecraft.world.entity.EntityType.RABBIT, new BlockPos(3, 2, 4));
            h.assertTrue(com.kittytalents.ai.CatTargeting.findBestTarget(c) == rabbit, "the rabbit is still prey");
            h.succeed();
        } finally {
            leave(h, fp);
        }
    }

    /** A 4.0 cat with bought ranks in a reworked talent gets those points back on load (Feline Reflexes 3 = 4 points). */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testReworkedTalentIsRefundedOnLoad(GameTestHelper h) {
        TalentedCat old = h.spawn(ModEntities.TALENTED_CAT.get(), new BlockPos(2, 2, 2));
        old.setTalentLevel(Talents.FELINE_REFLEXES, 3);
        old.setSkillPoints(2);
        CompoundTag tag = old.saveWithoutId(new CompoundTag());
        tag.remove(com.kittytalents.talent.TalentRevisions.TAG); // what a 4.0 save looks like
        old.discard();
        TalentedCat c = h.spawn(ModEntities.TALENTED_CAT.get(), new BlockPos(3, 2, 2));
        c.load(tag);
        h.assertTrue(c.getTalentLevel(Talents.FELINE_REFLEXES) == 0, "reflexes should be reset, is " + c.getTalentLevel(Talents.FELINE_REFLEXES));
        h.assertTrue(c.getSkillPoints() == 6, "2 + 4 refunded points expected, got " + c.getSkillPoints());
        CompoundTag again = c.saveWithoutId(new CompoundTag());
        c.setTalentLevel(Talents.FELINE_REFLEXES, 2);
        again.put("Talents", c.saveWithoutId(new CompoundTag()).getCompound("Talents"));
        c.load(again);
        h.assertTrue(c.getTalentLevel(Talents.FELINE_REFLEXES) == 2, "a 5.0 save must not be refunded again");
        h.succeed();
    }

    /** With the default follower limit (4), a fifth cat told to follow sits down instead. */
    @GameTest(template = "arena", timeoutTicks = 40)
    public static void testFollowerLimitSitsTheFifthCat(GameTestHelper h) {
        var fp = joinFakeOwner(h, new BlockPos(15, 2, 15));
        TalentedCat[] cats = new TalentedCat[5];
        for (int i = 0; i < 5; i++) {
            cats[i] = h.spawn(ModEntities.TALENTED_CAT.get(), new BlockPos(10 + i * 2, 2, 10));
            cats[i].tame(fp);
            cats[i].setOrderedToSit(false);
            cats[i].setCatMode(CatMode.STAY);
        }
        h.runAfterDelay(2, () -> {
            try {
                for (int i = 0; i < 4; i++) cats[i].setCatMode(CatMode.FOLLOW);
                for (int i = 0; i < 4; i++) h.assertTrue(cats[i].getCatMode() == CatMode.FOLLOW, "cat " + i + " should follow");
                cats[4].setCatMode(CatMode.FOLLOW);
                h.assertTrue(cats[4].getCatMode() != CatMode.FOLLOW && cats[4].isOrderedToSit(), "the fifth cat should sit instead");
                cats[0].setOrderedToSit(true); // a sitting follower doesn't count
                cats[4].setOrderedToSit(false);
                cats[4].setCatMode(CatMode.FOLLOW);
                h.assertTrue(cats[4].getCatMode() == CatMode.FOLLOW, "with one follower sitting, the fifth may follow");
                h.succeed();
            } finally {
                leave(h, fp);
            }
        });
    }

    /** kittyTalentsCatDeath on: a downed cat bleeds out after 60 s (here: loaded with 5 ticks left). */
    @GameTest(template = "empty", timeoutTicks = 60, batch = "catdeath")
    public static void testDownedCatBleedsOutWithCatDeathRule(GameTestHelper h) {
        var rules = h.getLevel().getGameRules();
        rules.getRule(com.kittytalents.registry.ModGameRules.CAT_DEATH).set(true, h.getLevel().getServer());
        TalentedCat c = h.spawn(ModEntities.TALENTED_CAT.get(), new BlockPos(2, 2, 2));
        CompoundTag tag = c.saveWithoutId(new CompoundTag());
        tag.putBoolean("Incapacitated", true);
        tag.putInt("DownedTicks", com.kittytalents.registry.ModGameRules.bleedOutTicks(h.getLevel()) - 5);
        c.load(tag);
        h.assertTrue(c.isIncapacitated(), "cat should be downed");
        h.succeedWhen(() -> {
            h.assertTrue(!c.isAlive(), "the downed cat should have bled out");
            rules.getRule(com.kittytalents.registry.ModGameRules.CAT_DEATH).set(false, h.getLevel().getServer());
        });
    }
}
