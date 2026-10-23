package com.kittytalents.test;

import com.kittytalents.KittyTalents;
import com.kittytalents.entity.CatMode;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.registry.ModEntities;
import com.kittytalents.talent.Talents;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * GameTest test definitions for Kitty Talents.
 * Executed via Forge GameTest runner (runGameTestServer).
 */
@GameTestHolder(KittyTalents.MODID)
@PrefixGameTestTemplate(false)
public class KittyTalentsGameTests {

    public static final String EMPTY_TEMPLATE = "empty3x3x3";

    @GameTest(template = EMPTY_TEMPLATE)
    public static void testCatSpawnAndAttributes(GameTestHelper helper) {
        BlockPos spawnPos = new BlockPos(1, 1, 1);
        TalentedCat cat = helper.spawn(ModEntities.TALENTED_CAT.get(), spawnPos);

        helper.assertTrue(cat != null, "TalentedCat failed to spawn");
        helper.assertTrue(cat.getHealth() > 0.0F, "Cat spawned with non-positive health");
        helper.assertTrue(cat.getCatMode() != null, "Cat spawned with null CatMode");
        helper.assertTrue(cat.getEnergy() >= 0, "Cat energy is negative");
        helper.assertTrue(cat.getCatLevel() >= 1, "Cat level should start at >= 1");

        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void testTalentTraining(GameTestHelper helper) {
        BlockPos spawnPos = new BlockPos(1, 1, 1);
        TalentedCat cat = helper.spawn(ModEntities.TALENTED_CAT.get(), spawnPos);

        cat.setSkillPoints(10);
        helper.assertTrue(cat.getSkillPoints() == 10, "Skill points failed to set");

        cat.setTalentLevel(Talents.POUNCE, 1);
        helper.assertTrue(cat.getTalentLevel(Talents.POUNCE) == 1, "Pounce talent level was not updated to 1");

        cat.setTalentLevel(Talents.TORCH_KITTY, 2);
        helper.assertTrue(cat.getTalentLevel(Talents.TORCH_KITTY) == 2, "Torch Kitty talent level was not updated to 2");

        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void testModeTransitions(GameTestHelper helper) {
        BlockPos spawnPos = new BlockPos(1, 1, 1);
        TalentedCat cat = helper.spawn(ModEntities.TALENTED_CAT.get(), spawnPos);

        cat.setCatMode(CatMode.DOCILE);
        helper.assertTrue(cat.getCatMode() == CatMode.DOCILE, "Failed setting DOCILE mode");

        cat.setCatMode(CatMode.FOLLOW);
        helper.assertTrue(cat.getCatMode() == CatMode.FOLLOW, "Failed setting FOLLOW mode");

        cat.setCatMode(CatMode.GUARD);
        helper.assertTrue(cat.getCatMode() == CatMode.GUARD, "Failed setting GUARD mode");

        cat.setCatMode(CatMode.STAY);
        helper.assertTrue(cat.getCatMode() == CatMode.STAY, "Failed setting STAY mode");

        cat.setCatMode(CatMode.WANDER);
        helper.assertTrue(cat.getCatMode() == CatMode.WANDER, "Failed setting WANDER mode");

        cat.setCatMode(CatMode.AGGRESSIVE);
        helper.assertTrue(cat.getCatMode() == CatMode.AGGRESSIVE, "Failed setting AGGRESSIVE mode");

        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void testCatPackStorage(GameTestHelper helper) {
        BlockPos spawnPos = new BlockPos(1, 1, 1);
        TalentedCat cat = helper.spawn(ModEntities.TALENTED_CAT.get(), spawnPos);

        helper.assertTrue(cat.getCatPack() != null, "CatPack inventory is null");
        helper.assertTrue(cat.getCatPack().getContainerSize() == 27, "CatPack must have 27 slots");

        ItemStack torches = new ItemStack(Items.TORCH, 16);
        cat.getCatPack().setItem(0, torches);
        helper.assertTrue(cat.getCatPack().getItem(0).getItem() == Items.TORCH, "Torch item was not stored in slot 0");
        helper.assertTrue(cat.getCatPack().getItem(0).getCount() == 16, "Torch count was not stored correctly");

        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void testCreeperWardenPreconditions(GameTestHelper helper) {
        BlockPos spawnPos = new BlockPos(1, 1, 1);
        TalentedCat cat = helper.spawn(ModEntities.TALENTED_CAT.get(), spawnPos);
        cat.setTalentLevel(Talents.NIGHT_PROWLER, 1);

        Creeper creeper = helper.spawn(EntityType.CREEPER, new BlockPos(2, 1, 1));
        helper.assertTrue(creeper != null, "Creeper failed to spawn");
        helper.assertTrue(cat.getTalentLevel(Talents.NIGHT_PROWLER) > 0, "Night Prowler not active on cat");

        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void testFoxPouncePreconditions(GameTestHelper helper) {
        BlockPos spawnPos = new BlockPos(1, 1, 1);
        TalentedCat cat = helper.spawn(ModEntities.TALENTED_CAT.get(), spawnPos);
        cat.setTalentLevel(Talents.POUNCE, 1);
        cat.setCatMode(CatMode.AGGRESSIVE);

        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 1, 1));
        helper.assertTrue(zombie != null, "Zombie failed to spawn");
        helper.assertTrue(cat.getTalentLevel(Talents.POUNCE) > 0, "Pounce not active on cat");

        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void testMockPlayerInteraction(GameTestHelper helper) {
        BlockPos spawnPos = new BlockPos(1, 1, 1);
        TalentedCat cat = helper.spawn(ModEntities.TALENTED_CAT.get(), spawnPos);

        Player player = helper.makeMockPlayer();
        helper.assertTrue(player != null, "Mock player creation failed");

        cat.tame(player);
        helper.assertTrue(cat.isTame(), "Cat failed to tame to mock player");
        helper.assertTrue(player.getUUID().equals(cat.getOwnerUUID()), "Owner UUID does not match mock player");

        helper.succeed();
    }
}
