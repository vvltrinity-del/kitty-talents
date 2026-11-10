package com.kittytalents.test;

import com.kittytalents.KittyTalents;
import com.kittytalents.entity.CatMode;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.entity.interact.CatInteractions;
import com.kittytalents.registry.ModEntities;
import com.kittytalents.talent.Talents;
import com.kittytalents.work.CatJob;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static com.kittytalents.test.KittyTalentGameTests.joinFakeOwner;
import static com.kittytalents.test.KittyTalentGameTests.leave;

/** Fixes from the 4.0 playtest (2026-11-10). */
@GameTestHolder(KittyTalents.MODID)
@PrefixGameTestTemplate(false)
public class KittyPlaytestFixGameTests {

    /**
     * A Sorter standing on the roof right above its output chest (within 3 blocks, but with a block in between) used
     * to count as "arrived" and deposit through the roof. It must walk down and stand beside the chest.
     */
    @GameTest(template = "arena", timeoutTicks = 400)
    public static void testSorterWalksToChestNotAboveIt(GameTestHelper h) {
        BlockPos chestRel = new BlockPos(10, 2, 8);
        h.setBlock(chestRel, Blocks.CHEST);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) h.setBlock(chestRel.offset(dx, 1, dz), Blocks.STONE); // a 3x3 roof over it
        }
        BlockPos chest = h.absolutePos(chestRel);
        TalentedCat c = h.spawn(ModEntities.TALENTED_CAT.get(), chestRel.above(2)); // on the roof, 1.5 blocks from the chest
        c.setTalentLevel(Talents.LOGISTICS_WHISKERS, 5);
        c.setTalentLevel(Talents.KITTY_PACK, 5);
        c.setCatLevel(15); // Sorter needs level 10
        c.setHunger(120);
        c.getCatPack().setItem(0, new ItemStack(Items.IRON_INGOT, 8));
        c.setCatJob(CatJob.SORTER);
        c.getJobData().addLinkedOutput(chest);
        c.setClockedIn(true);
        h.assertTrue(c.getCatMode() == CatMode.WORK, "clocked-in sorter should be in WORK mode");
        double roofY = c.getY();
        boolean[] depositedFromRoof = new boolean[1];
        h.onEachTick(() -> {
            if (h.getLevel().getBlockEntity(chest) instanceof ChestBlockEntity be && be.countItem(Items.IRON_INGOT) > 0
                    && c.getY() > roofY - 0.5D) depositedFromRoof[0] = true;
        });
        h.succeedWhen(() -> {
            h.assertFalse(depositedFromRoof[0], "the sorter deposited through the roof");
            h.assertTrue(h.getLevel().getBlockEntity(chest) instanceof ChestBlockEntity be && be.countItem(Items.IRON_INGOT) == 8,
                    "the iron should end up in the chest (cat at y " + c.getY() + ", status " + c.getWorkStatus() + ")");
            h.assertTrue(c.getY() < roofY - 0.5D, "the cat should have come down to the chest");
        });
    }

    /** Mounting makes a sitting cat stand up, and a ridden cat cannot be sat down. */
    @GameTest(template = "empty", timeoutTicks = 60)
    public static void testCannotRideASittingCat(GameTestHelper h) {
        FakePlayer p = joinFakeOwner(h, new BlockPos(3, 2, 2));
        TalentedCat c = h.spawn(ModEntities.TALENTED_CAT.get(), new BlockPos(2, 2, 2));
        c.tame(p);
        c.setTalentLevel(Talents.CAT_MOUNT, 1);
        c.setCatMode(CatMode.DOCILE);
        c.setOrderedToSit(true);
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        CatInteractions.handle(c, p, InteractionHand.MAIN_HAND);
        h.assertTrue(p.getVehicle() == c, "the owner should be riding");
        h.assertTrue(!c.isOrderedToSit() && !c.isInSittingPose() && c.getCatMode() == CatMode.FOLLOW,
                "a mounted cat must be standing (sit " + c.isOrderedToSit() + ", mode " + c.getCatMode() + ")");
        c.setOrderedToSit(true); // e.g. a whistle or the screen while riding
        h.runAfterDelay(5, () -> {
            try {
                h.assertTrue(!c.isOrderedToSit() && p.getVehicle() == c, "a ridden cat must not stay sat");
                h.succeed();
            } finally {
                p.stopRiding();
                leave(h, p);
            }
        });
    }
}
