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

    /** Cats never pick fish as a target, even in Hunting mode (they used to leave a fight to swim after cod). */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testHuntingCatIgnoresFish(GameTestHelper h) {
        var fp = joinFakeOwner(h, new BlockPos(1, 2, 1));
        try {
            TalentedCat c = h.spawn(ModEntities.TALENTED_CAT.get(), new BlockPos(3, 2, 3));
            c.tame(fp);
            c.setOrderedToSit(false);
            c.setCatMode(CatMode.HUNTING);
            h.spawn(net.minecraft.world.entity.EntityType.COD, new BlockPos(4, 2, 3));
            h.assertTrue(com.kittytalents.ai.CatTargeting.findBestTarget(c) == null, "a cod should not be a target");
            var rabbit = h.spawn(net.minecraft.world.entity.EntityType.RABBIT, new BlockPos(3, 2, 4));
            h.assertTrue(com.kittytalents.ai.CatTargeting.findBestTarget(c) == rabbit, "the rabbit is still prey");
            h.succeed();
        } finally {
            leave(h, fp);
        }
    }
}
