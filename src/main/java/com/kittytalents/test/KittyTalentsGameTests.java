package com.kittytalents.test;

import com.kittytalents.KittyTalents;
import com.kittytalents.block.KittyHabitatBlockEntity;
import com.kittytalents.block.KittyWorkStationBlockEntity;
import com.kittytalents.block.TreatOvenBlockEntity;
import com.kittytalents.entity.CatMode;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.registry.ModBlocks;
import com.kittytalents.registry.ModEntities;
import com.kittytalents.talent.Talents;
import com.kittytalents.work.CatJob;
import com.kittytalents.work.WorkInventoryHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(KittyTalents.MODID)
@PrefixGameTestTemplate(false)
public class KittyTalentsGameTests {

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void testWorkerJobAssignmentAndClockIn(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        TalentedCat cat = helper.spawn(ModEntities.TALENTED_CAT.get(), pos);
        cat.setCatLevel(25);
        cat.setTalentLevel(Talents.GREEN_PAW, 3);
        cat.setCatJob(CatJob.FARMER);
        cat.setClockedIn(true);

        helper.assertTrue(cat.getCatJob() == CatJob.FARMER, "Cat job should be FARMER");
        helper.assertTrue(cat.isClockedIn(), "Cat should be clocked in");
        helper.assertTrue(cat.getCatMode() == CatMode.WORK, "Cat mode should switch to WORK on clock-in");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void testHabitatStoreAndReleaseCat(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, ModBlocks.KITTY_HABITAT.get());
        KittyHabitatBlockEntity habitat = (KittyHabitatBlockEntity) helper.getBlockEntity(pos);
        helper.assertTrue(habitat != null, "Habitat block entity should exist");

        TalentedCat cat = helper.spawn(ModEntities.TALENTED_CAT.get(), pos.above());
        cat.setCatLevel(30);

        boolean stored = habitat.houseWorker(cat);
        helper.assertTrue(stored && habitat.hasStoredCat(), "Cat should be stored inside Habitat NBT");
        helper.assertTrue(habitat.getAssignedCatLevel() == 30, "Habitat should preserve cat level");

        TalentedCat released = habitat.releaseHousedCat(helper.absolutePos(pos.above()));
        helper.assertTrue(released != null && !habitat.hasStoredCat(), "Cat should be released from Habitat");
        helper.assertTrue(released.getCatLevel() == 30, "Released cat should preserve level 30");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void testWorkStationBufferAndCapabilities(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, ModBlocks.KITTY_WORK_STATION.get());
        KittyWorkStationBlockEntity station = (KittyWorkStationBlockEntity) helper.getBlockEntity(pos);
        helper.assertTrue(station != null, "Work Station block entity should exist");

        ItemStack rem = WorkInventoryHelper.insertIntoPos(helper.getLevel(), helper.absolutePos(pos), new ItemStack(Items.WHEAT, 16), false);
        helper.assertTrue(rem.isEmpty(), "Work Station output buffer should accept 16 wheat");
        helper.assertTrue(station.getInventory().getStackInSlot(0).getCount() == 16, "Slot 0 should contain 16 wheat");
        helper.assertTrue(station.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent(), "Work Station should expose ITEM_HANDLER capability");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void testTreatOvenEnergyAndCapabilities(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, ModBlocks.TREAT_OVEN.get());
        TreatOvenBlockEntity oven = (TreatOvenBlockEntity) helper.getBlockEntity(pos);
        helper.assertTrue(oven != null, "Treat Oven block entity should exist");
        helper.assertTrue(oven.getCapability(ForgeCapabilities.ENERGY).isPresent(), "Treat Oven should expose ENERGY capability");
        helper.assertTrue(oven.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent(), "Treat Oven should expose ITEM_HANDLER capability");
        helper.succeed();
    }
}
