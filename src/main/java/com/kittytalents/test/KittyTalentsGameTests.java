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

    // ---- Phase 1 hotfix regression tests ----

    /** Nine Lives revives once, then goes on cooldown: the next lethal hit downs the cat instead. */
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void testNineLivesHasCooldown(GameTestHelper helper) {
        TalentedCat cat = helper.spawn(ModEntities.TALENTED_CAT.get(), new BlockPos(2, 1, 2));
        cat.setTalentLevel(Talents.NINE_LIVES, 1);
        var dmg = helper.getLevel().damageSources().generic();

        cat.hurt(dmg, 1000.0F);
        helper.assertTrue(cat.isAlive() && !cat.isIncapacitated(), "First lethal hit should trigger the Nine Lives revive");
        helper.assertTrue(cat.getNineLivesReadyAt() > helper.getLevel().getGameTime(), "Nine Lives should now be recharging");

        cat.hurt(dmg, 1000.0F);
        helper.assertTrue(cat.isIncapacitated(), "Second lethal hit during recharge should down the cat, not revive it");
        helper.succeed();
    }

    /** Downed cats ignore normal damage but must still take void/kill damage. */
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void testDownedCatStillTakesVoidDamage(GameTestHelper helper) {
        TalentedCat cat = helper.spawn(ModEntities.TALENTED_CAT.get(), new BlockPos(2, 1, 2));
        cat.setIncapacitated(true);
        float before = cat.getHealth();

        cat.hurt(helper.getLevel().damageSources().generic(), 5.0F);
        helper.assertTrue(cat.getHealth() == before, "Downed cat should ignore normal damage");

        cat.hurt(helper.getLevel().damageSources().fellOutOfWorld(), 5.0F);
        helper.assertTrue(cat.getHealth() < before, "Downed cat must still take void damage");
        helper.succeed();
    }

    /** A 2.0-format save (mode ordinal, ForgeData cooldowns/flags) loads into a real cat with nothing lost. */
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void testLegacySaveMigratesOnLoad(GameTestHelper helper) {
        TalentedCat source = helper.spawn(ModEntities.TALENTED_CAT.get(), new BlockPos(2, 1, 2));
        source.setCatLevel(37);
        source.setTalentLevel(Talents.POUNCE, 3);
        net.minecraft.nbt.CompoundTag tag = source.saveWithoutId(new net.minecraft.nbt.CompoundTag());

        // Rewrite the tag into the legacy (schema 0) shape.
        tag.remove(com.kittytalents.core.persist.CatSchema.KEY_SCHEMA);
        tag.remove(com.kittytalents.core.persist.CatSchema.KEY_MODE_NAME);
        tag.remove(com.kittytalents.core.persist.CatSchema.KEY_ABILITY_COOLDOWNS);
        tag.putInt("CatMode", 8); // PATROL in the historic enum order
        net.minecraft.nbt.CompoundTag forge = tag.getCompound("ForgeData");
        forge.putLong("cooldown_pounce", 4321L);
        forge.putBoolean("SpiritCatnipUsed", true);
        forge.putInt("KT_ComboStep", 3);
        tag.put("ForgeData", forge);
        tag.remove("UUID"); // avoid two live entities sharing one UUID

        TalentedCat loaded = helper.spawn(ModEntities.TALENTED_CAT.get(), new BlockPos(4, 1, 4));
        loaded.load(tag);

        helper.assertTrue(loaded.getCatMode() == CatMode.PATROL, "Legacy mode ordinal 8 should load as PATROL, got " + loaded.getCatMode());
        helper.assertTrue(loaded.getCatLevel() == 37, "Level must survive migration");
        helper.assertTrue(loaded.getTalentLevel(Talents.POUNCE) == 3, "Talents must survive migration");
        helper.assertTrue(loaded.getAbilityCooldown("pounce") == 4321L, "Ability cooldown should move out of ForgeData");
        helper.assertTrue(loaded.isSpiritCatnipPrimed(), "Spirit Catnip flag should move out of ForgeData");
        helper.assertFalse(loaded.getPersistentData().contains("cooldown_pounce"), "Legacy cooldown key should be gone");
        helper.assertFalse(loaded.getPersistentData().contains("KT_ComboStep"), "Transient combat key should be gone");

        net.minecraft.nbt.CompoundTag resaved = loaded.saveWithoutId(new net.minecraft.nbt.CompoundTag());
        helper.assertTrue(resaved.getInt(com.kittytalents.core.persist.CatSchema.KEY_SCHEMA) == com.kittytalents.core.persist.CatSchema.CURRENT,
                "Re-save should stamp the current schema version");
        helper.assertTrue("PATROL".equals(resaved.getString(com.kittytalents.core.persist.CatSchema.KEY_MODE_NAME)), "Re-save should store mode by name");
        helper.succeed();
    }

    private static boolean anySlotOnCooldown(TalentedCat cat) {
        for (String id : new String[]{cat.getAbilitySlot1(), cat.getAbilitySlot2(), cat.getAbilitySlot3(), cat.getAbilityUltimate()}) {
            if (id != null && !id.isEmpty() && cat.getAbilityCooldown(id) > 0) return true;
        }
        return false;
    }

    /** An idle cat with full Energy must not waste abilities. */
    @GameTest(template = "empty", timeoutTicks = 60)
    public static void testIdleCatDoesNotCastAbilities(GameTestHelper helper) {
        TalentedCat cat = helper.spawn(ModEntities.TALENTED_CAT.get(), new BlockPos(2, 1, 2));
        cat.setCatMode(CatMode.FOLLOW);
        cat.setEnergy(100);
        helper.runAfterDelay(50, () -> {
            helper.assertFalse(anySlotOnCooldown(cat), "Idle cat should not cast any ability");
            helper.succeed();
        });
    }

    /** A cat fighting a target should autonomously cast an equipped ability. */
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void testCombatCatCastsAbility(GameTestHelper helper) {
        TalentedCat cat = helper.spawn(ModEntities.TALENTED_CAT.get(), new BlockPos(2, 1, 2));
        cat.setCatMode(CatMode.FOLLOW);
        cat.setAbilitySlot1("venom_fang");
        cat.setEnergy(100);
        var zombie = helper.spawn(net.minecraft.world.entity.EntityType.ZOMBIE, new BlockPos(4, 1, 2));
        helper.succeedWhen(() -> {
            if (cat.getTarget() != zombie) cat.setTarget(zombie);
            helper.assertTrue(anySlotOnCooldown(cat), "Cat in combat should have cast an ability");
        });
    }
}
