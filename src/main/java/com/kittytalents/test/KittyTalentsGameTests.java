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
        BlockPos pos = new BlockPos(2, 2, 2);
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
        BlockPos pos = new BlockPos(2, 2, 2);
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
        BlockPos pos = new BlockPos(2, 2, 2);
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
        BlockPos pos = new BlockPos(2, 2, 2);
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
        TalentedCat cat = helper.spawn(ModEntities.TALENTED_CAT.get(), new BlockPos(2, 2, 2));
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
        TalentedCat cat = helper.spawn(ModEntities.TALENTED_CAT.get(), new BlockPos(2, 2, 2));
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
        TalentedCat source = helper.spawn(ModEntities.TALENTED_CAT.get(), new BlockPos(2, 2, 2));
        source.setCatLevel(37);
        source.setTalentLevel(Talents.POUNCE, 3);
        net.minecraft.nbt.CompoundTag tag = source.saveWithoutId(new net.minecraft.nbt.CompoundTag());

        // Rewrite the tag into the legacy (schema 0) shape.
        tag.remove(com.kittytalents.core.persist.CatSchema.KEY_SCHEMA);
        tag.remove(com.kittytalents.core.persist.CatSchema.KEY_MODE_NAME);
        tag.remove(com.kittytalents.core.persist.CatSchema.KEY_ABILITY_COOLDOWNS);
        tag.putInt("CatMode", 8); // PATROL in the historic enum order
        tag.remove(com.kittytalents.core.persist.CatSchema.KEY_SKIN);
        tag.putInt("SkinId", 12); // 3.0 skin number: Snow Leopard
        net.minecraft.nbt.CompoundTag forge = tag.getCompound("ForgeData");
        forge.putLong("cooldown_pounce", 4321L);
        forge.putBoolean("SpiritCatnipUsed", true);
        forge.putInt("KT_ComboStep", 3);
        tag.put("ForgeData", forge);
        tag.remove("UUID"); // avoid two live entities sharing one UUID

        TalentedCat loaded = helper.spawn(ModEntities.TALENTED_CAT.get(), new BlockPos(4, 2, 4));
        loaded.load(tag);

        helper.assertTrue(loaded.getCatMode() == CatMode.GUARD, "Legacy ordinal 8 (PATROL) should load as GUARD after the mode merge, got " + loaded.getCatMode());
        helper.assertTrue(loaded.getCatLevel() == 37, "Level must survive migration");
        helper.assertTrue("kittytalents:snow_leopard".equals(loaded.getSkin()), "Legacy SkinId 12 should load as snow_leopard, got " + loaded.getSkin());
        helper.assertTrue(loaded.getTalentLevel(Talents.POUNCE) == 3, "Talents must survive migration");
        helper.assertTrue(loaded.getAbilityCooldown("pounce") == 4321L, "Ability cooldown should move out of ForgeData");
        helper.assertTrue(loaded.isSpiritCatnipPrimed(), "Spirit Catnip flag should move out of ForgeData");
        helper.assertFalse(loaded.getPersistentData().contains("cooldown_pounce"), "Legacy cooldown key should be gone");
        helper.assertFalse(loaded.getPersistentData().contains("KT_ComboStep"), "Transient combat key should be gone");

        net.minecraft.nbt.CompoundTag resaved = loaded.saveWithoutId(new net.minecraft.nbt.CompoundTag());
        helper.assertTrue(resaved.getInt(com.kittytalents.core.persist.CatSchema.KEY_SCHEMA) == com.kittytalents.core.persist.CatSchema.CURRENT,
                "Re-save should stamp the current schema version");
        helper.assertTrue("GUARD".equals(resaved.getString(com.kittytalents.core.persist.CatSchema.KEY_MODE_NAME)), "Re-save should store mode by name");
        helper.succeed();
    }

    /** R37: a heavily progressed cat survives save + reload with every component intact. */
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void testFullCatSaveReloadRoundTrip(GameTestHelper helper) {
        TalentedCat cat = helper.spawn(ModEntities.TALENTED_CAT.get(), new BlockPos(2, 2, 2));
        long now = helper.getLevel().getGameTime();
        cat.setCatLevel(88);
        cat.setSkillPoints(7);
        cat.setTalentLevel(Talents.POUNCE, 4);
        cat.setTalentLevel(Talents.MOUSER, 0); // explicit 0 on a standard talent must stay 0, not fall back to the free baseline
        cat.setEquippedWeapon(new ItemStack(Items.DIAMOND_SWORD));
        cat.setEquippedTool(new ItemStack(Items.TORCH, 12));
        cat.setArmorSlotItem(1, new ItemStack(Items.IRON_HELMET));
        cat.setArmorSlotItem(4, new ItemStack(Items.GOLDEN_BOOTS));
        cat.setCosmeticSlot(2, new ItemStack(Items.RED_BANNER));
        cat.setCarriedToy(new ItemStack(Items.STRING));
        cat.getCatPack().setItem(5, new ItemStack(Items.COOKED_SALMON, 9));
        cat.getAmmoPouch().setItem(0, new ItemStack(Items.ARROW, 32));
        java.util.UUID friend = java.util.UUID.randomUUID();
        cat.addTrustedPlayer(friend, "Friend");
        cat.stats().recordKill(true);
        cat.stats().addDistance(123.5);
        cat.setCatRole(com.kittytalents.combat.CatRole.values()[cat.getCatRole().ordinal() == 0 ? 1 : 0]);
        cat.setAbilitySlot2("venom_fang");
        cat.setAbilityCooldown("venom_fang", now - 5);
        cat.setSpiritCatnipPrimed(true);
        cat.setCatMode(CatMode.GUARD);

        net.minecraft.nbt.CompoundTag tag = cat.saveWithoutId(new net.minecraft.nbt.CompoundTag());
        tag.remove("UUID");
        TalentedCat copy = helper.spawn(ModEntities.TALENTED_CAT.get(), new BlockPos(4, 2, 4));
        copy.load(tag);

        helper.assertTrue(copy.getCatLevel() == 88 && copy.getSkillPoints() == 7, "Level/points lost");
        helper.assertTrue(copy.getTalentLevel(Talents.POUNCE) == 4, "Talent rank lost");
        helper.assertTrue(copy.getTalentLevel(Talents.MOUSER) == 1, "Standard talent set to 0 should read as its minimum (1), not the free baseline");
        helper.assertTrue(ItemStack.matches(copy.getEquippedWeapon(), cat.getEquippedWeapon()), "Weapon lost");
        helper.assertTrue(ItemStack.matches(copy.getEquippedTool(), cat.getEquippedTool()), "Tool lost");
        helper.assertTrue(ItemStack.matches(copy.getArmorSlotItem(1), cat.getArmorSlotItem(1)), "Helmet lost");
        helper.assertTrue(copy.getArmorSlotItem(2).isEmpty(), "Empty chest slot should stay empty");
        helper.assertTrue(ItemStack.matches(copy.getArmorSlotItem(4), cat.getArmorSlotItem(4)), "Boots lost");
        helper.assertTrue(ItemStack.matches(copy.getCosmeticSlot(2), cat.getCosmeticSlot(2)), "Cosmetic slot 3 lost");
        helper.assertTrue(ItemStack.matches(copy.getCarriedToy(), cat.getCarriedToy()), "Carried toy lost");
        helper.assertTrue(ItemStack.matches(copy.getCatPack().getItem(5), cat.getCatPack().getItem(5)), "Pack item lost");
        helper.assertTrue(ItemStack.matches(copy.getAmmoPouch().getItem(0), cat.getAmmoPouch().getItem(0)), "Ammo lost");
        helper.assertTrue(copy.isTrusted(friend), "Trust lost");
        helper.assertTrue(copy.getTotalKills() == 1 && copy.getHostileKills() == 1, "Kill stats lost");
        helper.assertTrue(copy.getDistanceTraveled() == 123.5, "Distance stat lost");
        helper.assertTrue(copy.getCatRole() == cat.getCatRole(), "Role lost");
        helper.assertTrue("venom_fang".equals(copy.getAbilitySlot2()), "Ability slot lost");
        helper.assertTrue(copy.getAbilityCooldown("venom_fang") == now - 5, "Ability cooldown lost");
        helper.assertTrue(copy.isSpiritCatnipPrimed(), "Spirit Catnip flag lost");
        helper.assertTrue(copy.getCatMode() == CatMode.GUARD, "Mode lost");
        helper.succeed();
    }

    /** Respec must refund only ranks actually paid for; free standard-talent ranks used to mint points. */
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void testRespecRefundsOnlyPaidRanks(GameTestHelper helper) {
        TalentedCat cat = helper.spawn(ModEntities.TALENTED_CAT.get(), new BlockPos(2, 2, 2));
        cat.setSkillPoints(10);
        cat.trainTalent(Talents.POUNCE); // rank 1, max 8 -> costs 1
        cat.trainTalent(Talents.POUNCE); // rank 2 -> costs 1
        helper.assertTrue(cat.getSkillPoints() == 8, "Training two Pounce ranks should cost 2, have " + cat.getSkillPoints());

        int refunded = cat.respecAllTalents();
        helper.assertTrue(refunded == 2, "Respec should refund exactly the 2 paid points, refunded " + refunded);
        helper.assertTrue(cat.getSkillPoints() == 10, "Points should be back to 10, have " + cat.getSkillPoints());

        int again = cat.respecAllTalents();
        helper.assertTrue(again == 0, "A second respec must refund nothing, refunded " + again);
        helper.succeed();
    }

    /** Two statuses where one expires first used to throw ConcurrentModificationException in the tick (server crash). */
    @GameTest(template = "empty", timeoutTicks = 80)
    public static void testStatusExpiryWithMultipleStatusesDoesNotCrash(GameTestHelper helper) {
        // Not undead: undead are immune to Poison and Bleed. Statuses enforce a 20-tick minimum duration.
        var zombie = helper.spawn(net.minecraft.world.entity.EntityType.PIG, new BlockPos(2, 2, 2));
        zombie.setNoAi(true);
        zombie.setInvulnerable(true); // only the status bookkeeping is under test, not the damage
        com.kittytalents.combat.FelineStatusEffects.applyStatus(zombie, com.kittytalents.combat.FelineStatusType.POISON, 1, 20);
        com.kittytalents.combat.FelineStatusEffects.applyStatus(zombie, com.kittytalents.combat.FelineStatusType.BLEED, 1, 60);
        helper.runAfterDelay(35, () -> {
            helper.assertTrue(com.kittytalents.combat.FelineStatusEffects.getStatusPower(zombie, com.kittytalents.combat.FelineStatusType.POISON) == 0,
                    "Poison should have expired");
            helper.assertTrue(com.kittytalents.combat.FelineStatusEffects.getStatusPower(zombie, com.kittytalents.combat.FelineStatusType.BLEED) > 0,
                    "Bleed should still be active");
            helper.succeed();
        });
    }

    /** 4.0 Phase 1: Status effects moved to capability; a zombie with a status saves no Kitty Talents data once expired. */
    @GameTest(template = "empty", timeoutTicks = 60)
    public static void testZombieSavesNoDataAfterStatusExpires(GameTestHelper helper) {
        var zombie = helper.spawn(net.minecraft.world.entity.EntityType.ZOMBIE, new BlockPos(2, 2, 2));
        zombie.setNoAi(true);
        zombie.setInvulnerable(true);
        com.kittytalents.combat.FelineStatusEffects.applyStatus(zombie, com.kittytalents.combat.FelineStatusType.WEAKNESS, 1, 20);
        helper.assertTrue(com.kittytalents.combat.FelineStatusEffects.hasStatus(zombie, com.kittytalents.combat.FelineStatusType.WEAKNESS),
                "Zombie should have Weakness status");
        helper.runAfterDelay(30, () -> {
            helper.assertFalse(com.kittytalents.combat.FelineStatusEffects.hasStatus(zombie, com.kittytalents.combat.FelineStatusType.WEAKNESS),
                    "Weakness should have expired");
            helper.assertFalse(zombie.getPersistentData().contains("FelineStatuses"),
                    "Zombie persistent data should not contain FelineStatuses");
            net.minecraft.nbt.CompoundTag nbt = new net.minecraft.nbt.CompoundTag();
            zombie.saveWithoutId(nbt);
            if (nbt.contains("ForgeData")) {
                helper.assertFalse(nbt.getCompound("ForgeData").contains("FelineStatuses"),
                        "ForgeData should not have FelineStatuses");
            }
            if (nbt.contains("ForgeCaps")) {
                net.minecraft.nbt.CompoundTag caps = nbt.getCompound("ForgeCaps");
                if (caps.contains("kittytalents:feline_statuses")) {
                    helper.assertTrue(caps.getCompound("kittytalents:feline_statuses").isEmpty(),
                            "Status capability tag must be empty");
                }
            }
            helper.succeed();
        });
    }

    /** Twine: with no player input, the AI must not move, turn-to-target or attack on its own. */
    // 240 ticks: 60 twined + up to ~100 for the AI to re-scan after untwine (it scans every 30 ticks with no player nearby).
    @GameTest(template = "empty", timeoutTicks = 240)
    public static void testTwinedCatIgnoresAiWithoutInput(GameTestHelper helper) {
        TalentedCat cat = helper.spawn(ModEntities.TALENTED_CAT.get(), new BlockPos(2, 2, 2));
        cat.setCatMode(CatMode.AGGRESSIVE);
        var zombie = helper.spawn(net.minecraft.world.entity.EntityType.ZOMBIE, new BlockPos(5, 2, 5));
        zombie.setNoAi(true); // keep the provocation still so any cat movement is the cat's own AI

        var player = helper.makeMockPlayer();
        cat.setTwinedPlayer(player);
        cat.setTwineInput(0.0F, 0.0F);
        net.minecraft.world.phys.Vec3 start = cat.position();
        float startYaw = cat.getYRot();
        // The mock player is not in the server player list, so TalentedCat's safety check (correctly) ends the
        // session each tick. Re-link it every tick to stand in for a connected player.
        boolean[] twinePhase = {true};
        helper.onEachTick(() -> {
            if (twinePhase[0] && !cat.isTwined()) {
                cat.setTwinedPlayer(player);
                cat.setTwineInput(0.0F, 0.0F);
            }
        });

        helper.runAfterDelay(60, () -> {
            twinePhase[0] = false;
            helper.assertTrue(cat.getTarget() == null, "Twined cat picked its own target: " + cat.getTarget());
            double moved = cat.position().subtract(start).horizontalDistance();
            helper.assertTrue(moved < 0.25, "Twined cat moved " + moved + " blocks with no input");
            helper.assertTrue(Math.abs(net.minecraft.util.Mth.wrapDegrees(cat.getYRot() - startYaw)) < 1.0F,
                    "Twined cat turned on its own: " + startYaw + " -> " + cat.getYRot());

            // Untwine: AI control must come straight back.
            cat.setTwinedPlayer(null);
            cat.clearTwineState();
            helper.succeedWhen(() -> helper.assertTrue(cat.getTarget() == zombie, "AI should retarget after untwine"));
        });
    }

    /** Gear screen: clicking an empty slot equips from the player's inventory; clicking a filled slot returns it. */
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void testGearClickEquipsFromInventoryAndUnequipsToInventory(GameTestHelper helper) {
        TalentedCat cat = helper.spawn(ModEntities.TALENTED_CAT.get(), new BlockPos(2, 2, 2));
        // 5.0: iron armor needs no talent; diamond needs Feline Armor 1.
        net.minecraft.server.level.ServerPlayer player = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(helper.getLevel());
        player.getInventory().clearContent();
        player.getInventory().setItem(5, new ItemStack(Items.DIAMOND_HELMET)); // needs rank 1: must be skipped
        player.getInventory().setItem(9, new ItemStack(Items.IRON_HELMET));

        com.kittytalents.network.PacketEquipCatItem.handleSlotInteraction(player, cat, 1, false);
        helper.assertTrue(cat.getArmorSlotItem(1).is(Items.IRON_HELMET), "Click should equip the first helmet the cat can wear, got " + cat.getArmorSlotItem(1));
        helper.assertTrue(player.getInventory().getItem(9).isEmpty(), "Equipped helmet should leave the inventory");
        helper.assertTrue(player.getInventory().getItem(5).is(Items.DIAMOND_HELMET), "Too-high-rank helmet must stay in the inventory");

        com.kittytalents.network.PacketEquipCatItem.handleSlotInteraction(player, cat, 1, false);
        helper.assertTrue(cat.getArmorSlotItem(1).isEmpty(), "Second click should unequip");
        helper.assertTrue(player.containerMenu.getCarried().isEmpty(), "Unequipped item must not be stuck on the cursor");
        helper.assertTrue(player.getInventory().countItem(Items.IRON_HELMET) == 1, "Unequipped helmet should be back in the inventory");
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
        TalentedCat cat = helper.spawn(ModEntities.TALENTED_CAT.get(), new BlockPos(2, 2, 2));
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
        TalentedCat cat = helper.spawn(ModEntities.TALENTED_CAT.get(), new BlockPos(2, 2, 2));
        cat.setCatMode(CatMode.FOLLOW);
        cat.setAbilitySlot1("venom_fang");
        cat.setEnergy(100);
        // A pillager, not a zombie: undead are immune to Venom Fang's poison, so the AI rightly won't cast it on them.
        var zombie = helper.spawn(net.minecraft.world.entity.EntityType.PILLAGER, new BlockPos(4, 2, 2));
        zombie.setNoAi(true);
        helper.succeedWhen(() -> {
            if (cat.getTarget() != zombie) cat.setTarget(zombie);
            helper.assertTrue(anySlotOnCooldown(cat), "Cat in combat should have cast an ability");
        });
    }
}
