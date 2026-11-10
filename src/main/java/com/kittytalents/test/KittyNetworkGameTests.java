package com.kittytalents.test;

import com.kittytalents.KittyTalents;
import com.kittytalents.core.component.CatLoadout;
import com.kittytalents.core.perm.CatAction;
import com.kittytalents.core.perm.CatPermissions;
import com.kittytalents.entity.CatMode;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.entity.TwineManager;
import com.kittytalents.network.*;
import com.kittytalents.registry.ModEntities;
import com.kittytalents.talent.Talents;
import com.kittytalents.work.CatJob;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.DyeColor;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Phase 7: malicious-packet tests. Each packet's server handler ({@code apply}) is fed what a modified client could
 * send: someone else's cat, out-of-range ids and values, a far-away cat, an entity id that isn't a cat, and Twine
 * control without a session. Every refusal must leave the cat unchanged. Plus the Twine lifecycle edge cases: the
 * player's body must always be restored.
 */
@GameTestHolder(KittyTalents.MODID)
@PrefixGameTestTemplate(false)
public class KittyNetworkGameTests {

    private static TalentedCat cat(GameTestHelper h, BlockPos pos, ServerPlayer owner) {
        TalentedCat c = h.spawn(ModEntities.TALENTED_CAT.get(), pos);
        c.setCatMode(CatMode.STAY);
        c.tame(owner);
        return c;
    }

    private static FakePlayer join(GameTestHelper h, BlockPos rel) {
        return KittyTalentGameTests.joinFakeOwner(h, rel);
    }

    private static void leave(GameTestHelper h, ServerPlayer... players) {
        for (ServerPlayer p : players) {
            TwineManager.endForPlayer(p, null);
            KittyTalentGameTests.leave(h, p);
        }
    }

    // ------------------------------------------------------------------ matrix

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testPermissionMatrix(GameTestHelper h) {
        for (CatAction a : CatAction.values()) {
            h.assertTrue(CatPermissions.allows(CatPermissions.Role.OWNER, a), "Owner must be allowed " + a);
            h.assertFalse(CatPermissions.allows(CatPermissions.Role.STRANGER, a), "Stranger must never be allowed " + a);
        }
        for (CatAction a : new CatAction[]{CatAction.TRAIN, CatAction.RESPEC, CatAction.RENAME, CatAction.MANAGE_TRUST}) {
            h.assertFalse(CatPermissions.allows(CatPermissions.Role.TRUSTED, a), "Co-owners must not be allowed " + a);
        }
        for (CatAction a : new CatAction[]{CatAction.COMMAND, CatAction.STYLE, CatAction.EQUIP, CatAction.LOADOUT, CatAction.TWINE_START}) {
            h.assertTrue(CatPermissions.allows(CatPermissions.Role.TRUSTED, a), "Co-owners should be allowed " + a);
        }
        h.succeed();
    }

    // ------------------------------------------------------------------ wrong owner

    /** A stranger (not owner, not co-owner) can't change anything about someone else's cat. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testStrangerPacketsAreRefused(GameTestHelper h) {
        FakePlayer owner = join(h, new BlockPos(3, 2, 2));
        FakePlayer stranger = join(h, new BlockPos(2, 2, 3));
        try {
            stranger.getAbilities().instabuild = true; // so item costs can't be the reason for a refusal
            TalentedCat c = cat(h, new BlockPos(2, 2, 2), owner);
            c.setCustomName(Component.literal("Mittens"));
            int id = c.getId();
            CatMode mode = c.getCatMode();
            String skin = c.getSkin();
            DyeColor collar = c.getCollarColor();
            boolean female = c.isFemale();
            boolean cosmetics = c.isCosmeticsVisible();
            int points = c.getSkillPoints();
            String slot1 = c.getAbilitySlot1();

            h.assertFalse(PacketChangeCatMode.apply(stranger, id, CatMode.AGGRESSIVE.ordinal()), "mode");
            h.assertFalse(PacketChangeCatMode.apply(stranger, id, PacketChangeCatMode.NEXT), "mode next");
            h.assertFalse(PacketChangeCatSkin.apply(stranger, id, "kittytalents:siamese"), "skin");
            h.assertFalse(PacketChangeCollarColor.apply(stranger, id, (collar.getId() + 1) % 16), "collar");
            h.assertFalse(PacketChangeCatGender.apply(stranger, id, !female), "gender");
            h.assertFalse(PacketRenameCat.apply(stranger, id, "Stolen"), "rename");
            h.assertFalse(PacketSetCatJob.apply(stranger, id, CatJob.MINER.ordinal(), true), "job");
            h.assertFalse(PacketEquipCatItem.apply(stranger, id, PacketEquipCatItem.TOGGLE_COSMETICS, false), "cosmetics toggle");
            h.assertFalse(PacketEquipCatItem.apply(stranger, id, 0, true), "unequip weapon");
            h.assertFalse(PacketTrainTalent.apply(stranger, id, Talents.POUNCE.getId()), "train");
            h.assertFalse(PacketRespecTalents.apply(stranger, id), "respec");
            h.assertFalse(PacketOpenCatPack.apply(stranger, id), "open pack");
            h.assertFalse(PacketManageTrust.apply(stranger, id, PacketManageTrust.ADD, stranger.getGameProfile().getName()), "trust self");
            h.assertFalse(PacketManageTrust.apply(stranger, id, PacketManageTrust.REMOVE, owner.getUUID().toString()), "trust remove");
            h.assertFalse(PacketTwineAction.apply(stranger, id, PacketTwineAction.ActionType.TWINE_START, 0), "twine start");
            var loadout = new CatLoadout(c.getCatRole(), c.getPrimaryElement(), "feline_dash", "feline_hiss", "healing_purr", "catnip_frenzy");
            h.assertFalse(PacketSetCatLoadout.apply(stranger, id, loadout), "loadout");
            h.assertTrue(PacketWhistleCommand.apply(stranger, com.kittytalents.entity.WhistleCommand.AGGRESSIVE.ordinal()) == 0,
                    "A stranger's whistle must not reach someone else's cat");
            h.assertTrue(PacketTeamCommand.apply(stranger, PacketTeamCommand.CommandType.COME_TO_ME).isEmpty(),
                    "A stranger's team command must not reach someone else's cat");

            h.assertTrue(c.getCatMode() == mode && c.getSkin().equals(skin) && c.getCollarColor() == collar
                    && c.isFemale() == female && c.isCosmeticsVisible() == cosmetics && c.getSkillPoints() == points
                    && c.getCatJob() == CatJob.NONE && "Mittens".equals(c.getCustomName().getString())
                    && java.util.Objects.equals(slot1, c.getAbilitySlot1()) && !c.isTwined() && c.getTrustedPlayers().isEmpty(),
                    "A refused packet changed the cat");

            // Sanity: the owner can do the same things, so the refusals above are about permission.
            h.assertTrue(PacketChangeCatMode.apply(owner, id, CatMode.GUARD.ordinal()) && c.getCatMode() == CatMode.GUARD, "owner mode");
            h.assertTrue(PacketRenameCat.apply(owner, id, "Biscuit") && "Biscuit".equals(c.getCustomName().getString()), "owner rename");
            h.assertTrue(PacketEquipCatItem.apply(owner, id, PacketEquipCatItem.TOGGLE_COSMETICS, false) && c.isCosmeticsVisible() != cosmetics, "owner toggle");
            h.assertTrue(PacketTrainTalent.apply(owner, id, Talents.POUNCE.getId()) && c.getTalentLevel(Talents.POUNCE) == 1, "owner train");
            h.succeed();
        } finally {
            leave(h, owner, stranger);
        }
    }

    /** Co-owners get the shared actions but not the owner-only ones; they can leave, not kick other co-owners. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testCoOwnerPermissions(GameTestHelper h) {
        FakePlayer owner = join(h, new BlockPos(3, 2, 2));
        FakePlayer trusted = join(h, new BlockPos(2, 2, 3));
        FakePlayer other = join(h, new BlockPos(1, 2, 2));
        try {
            TalentedCat c = cat(h, new BlockPos(2, 2, 2), owner);
            c.addTrustedPlayer(trusted.getUUID(), trusted.getGameProfile().getName());
            c.addTrustedPlayer(other.getUUID(), other.getGameProfile().getName());
            int id = c.getId();
            int points = c.getSkillPoints();

            h.assertTrue(PacketChangeCatMode.apply(trusted, id, CatMode.FOLLOW.ordinal()) && c.getCatMode() == CatMode.FOLLOW, "co-owner mode");
            h.assertTrue(PacketChangeCatSkin.apply(trusted, id, "kittytalents:siamese") && c.getSkin().equals("kittytalents:siamese"), "co-owner skin");
            h.assertTrue(PacketSetCatJob.apply(trusted, id, CatJob.FARMER.ordinal(), false) && c.getCatJob() == CatJob.FARMER, "co-owner job");

            h.assertFalse(PacketRenameCat.apply(trusted, id, "Mine now"), "co-owner rename");
            h.assertFalse(PacketTrainTalent.apply(trusted, id, Talents.POUNCE.getId()), "co-owner train");
            h.assertTrue(c.getSkillPoints() == points && c.getTalentLevel(Talents.POUNCE) == 0, "co-owner spent points");
            trusted.getAbilities().instabuild = true;
            h.assertFalse(PacketRespecTalents.apply(trusted, id), "co-owner respec");
            h.assertFalse(PacketManageTrust.apply(trusted, id, PacketManageTrust.REMOVE, other.getUUID().toString()), "co-owner kicked another");
            h.assertTrue(c.isTrusted(other), "the other co-owner must still be trusted");

            h.assertTrue(PacketManageTrust.apply(trusted, id, PacketManageTrust.REMOVE, trusted.getUUID().toString()), "co-owner leaving");
            h.assertFalse(c.isTrusted(trusted), "co-owner should be gone after leaving");
            h.assertFalse(PacketChangeCatMode.apply(trusted, id, CatMode.GUARD.ordinal()), "ex-co-owner still has access");

            // Co-owners must be online real players: a FakePlayer name is never accepted.
            h.assertFalse(PacketManageTrust.apply(owner, id, PacketManageTrust.ADD, trusted.getGameProfile().getName()), "FakePlayer added as co-owner");
            h.assertTrue(PacketManageTrust.apply(owner, id, PacketManageTrust.REMOVE, other.getUUID().toString()) && !c.isTrusted(other), "owner remove");
            h.succeed();
        } finally {
            leave(h, owner, trusted, other);
        }
    }

    // ------------------------------------------------------------------ out-of-range values

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testOutOfRangeValuesAreRefused(GameTestHelper h) {
        FakePlayer owner = join(h, new BlockPos(3, 2, 2));
        try {
            TalentedCat c = cat(h, new BlockPos(2, 2, 2), owner);
            int id = c.getId();
            CatMode mode = c.getCatMode();
            String skin = c.getSkin();
            DyeColor collar = c.getCollarColor();
            int points = c.getSkillPoints();

            for (int bad : new int[]{-2, CatMode.values().length, 999, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
                h.assertFalse(PacketChangeCatMode.apply(owner, id, bad), "mode " + bad);
            }
            for (String bad : new String[]{"kittytalents:not_a_skin", "3", "minecraft:tabby", "Bad Id!", "<up>"}) {
                h.assertFalse(PacketChangeCatSkin.apply(owner, id, bad), "skin " + bad);
            }
            for (int bad : new int[]{-1, 16, 255}) h.assertFalse(PacketChangeCollarColor.apply(owner, id, bad), "collar " + bad);
            for (int bad : new int[]{-1, CatJob.values().length, 4096}) h.assertFalse(PacketSetCatJob.apply(owner, id, bad, true), "job " + bad);
            for (int bad : new int[]{-1, 11, 64}) h.assertFalse(PacketEquipCatItem.apply(owner, id, bad, false), "slot " + bad);
            h.assertFalse(PacketTrainTalent.apply(owner, id, "not_a_talent"), "unknown talent");
            h.assertFalse(PacketTrainTalent.apply(owner, id, ""), "empty talent");
            h.assertFalse(PacketManageTrust.apply(owner, id, 7, "x"), "trust action 7");
            h.assertFalse(PacketManageTrust.apply(owner, id, PacketManageTrust.REMOVE, "not-a-uuid"), "trust bad uuid");
            h.assertFalse(PacketManageTrust.apply(owner, id, PacketManageTrust.ADD, "has space"), "trust bad name");
            h.assertTrue(PacketWhistleCommand.apply(owner, -1) == -1 && PacketWhistleCommand.apply(owner, 9999) == -1, "whistle id");
            h.assertFalse(PacketRenameCat.apply(owner, id, ""), "empty name");
            h.assertFalse(PacketRenameCat.apply(owner, id, "§§  \u0007"), "name of only codes");

            h.assertTrue(c.getCatMode() == mode && c.getSkin().equals(skin) && c.getCollarColor() == collar
                    && c.getSkillPoints() == points && c.getCatJob() == CatJob.NONE, "A refused value changed the cat");

            // Cleaned, not refused: formatting codes and control characters are stripped, length is capped.
            h.assertTrue(PacketRenameCat.apply(owner, id, "  §cBob\u0000  "), "rename with codes");
            h.assertTrue("Bob".equals(c.getCustomName().getString()), "name should be cleaned, got " + c.getCustomName().getString());
            h.assertTrue(PacketRenameCat.apply(owner, id, "x".repeat(60)), "long rename");
            h.assertTrue(c.getCustomName().getString().length() == PacketRenameCat.MAX_NAME, "name should be capped at 32");
            h.assertTrue(PacketChangeCatMode.apply(owner, id, PacketChangeCatMode.NEXT) && c.getCatMode() == mode.next(), "NEXT still works");
            h.succeed();
        } finally {
            leave(h, owner);
        }
    }

    // ------------------------------------------------------------------ far away

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testFarAwayCatIsRefused(GameTestHelper h) {
        FakePlayer owner = join(h, new BlockPos(3, 2, 2));
        try {
            TalentedCat c = cat(h, new BlockPos(2, 2, 2), owner);
            int id = c.getId();
            owner.moveTo(c.getX() + 100.0D, c.getY(), c.getZ());
            CatMode mode = c.getCatMode();

            h.assertTrue(CatPermissions.check(owner, id, CatAction.STYLE).denial() == CatPermissions.Denial.TOO_FAR, "expected TOO_FAR");
            h.assertFalse(PacketChangeCatMode.apply(owner, id, CatMode.GUARD.ordinal()), "mode from 100 blocks");
            h.assertFalse(PacketRenameCat.apply(owner, id, "Far"), "rename from 100 blocks");
            h.assertFalse(PacketChangeCatSkin.apply(owner, id, "kittytalents:red"), "skin from 100 blocks");
            h.assertFalse(PacketEquipCatItem.apply(owner, id, PacketEquipCatItem.TOGGLE_COSMETICS, false), "equip from 100 blocks");
            h.assertFalse(PacketTwineAction.apply(owner, id, PacketTwineAction.ActionType.TWINE_START, 0), "twine from 100 blocks");
            var loadout = new CatLoadout(c.getCatRole(), c.getPrimaryElement(), "feline_dash", "feline_hiss", "healing_purr", "catnip_frenzy");
            h.assertFalse(PacketSetCatLoadout.apply(owner, id, loadout), "loadout from 100 blocks");
            h.assertTrue(PacketWhistleCommand.apply(owner, com.kittytalents.entity.WhistleCommand.GUARD.ordinal()) == 0, "whistle beyond 64 blocks");
            h.assertTrue(c.getCatMode() == mode && !c.isTwined(), "A far-away request changed the cat");

            owner.moveTo(c.getX() + 20.0D, c.getY(), c.getZ()); // inside the 32-block screen range, outside Twine's 16
            h.assertTrue(PacketChangeCatSkin.apply(owner, id, "kittytalents:red"), "skin from 20 blocks should work");
            h.assertFalse(PacketTwineAction.apply(owner, id, PacketTwineAction.ActionType.TWINE_START, 0), "twine from 20 blocks");
            h.succeed();
        } finally {
            leave(h, owner);
        }
    }

    // ------------------------------------------------------------------ non-cat entity ids

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testNonCatEntityIdIsRefused(GameTestHelper h) {
        FakePlayer owner = join(h, new BlockPos(3, 2, 2));
        try {
            Pig pig = h.spawn(EntityType.PIG, new BlockPos(2, 2, 2));
            pig.setNoAi(true);
            int pigId = pig.getId();
            int missing = Integer.MAX_VALUE;

            h.assertTrue(CatPermissions.check(owner, pigId, CatAction.COMMAND).denial() == CatPermissions.Denial.NOT_A_CAT, "expected NOT_A_CAT");
            h.assertTrue(CatPermissions.check(owner, missing, CatAction.COMMAND).denial() == CatPermissions.Denial.NO_ENTITY, "expected NO_ENTITY");
            h.assertTrue(CatPermissions.check(owner, owner.getId(), CatAction.COMMAND).denial() == CatPermissions.Denial.NOT_A_CAT, "a player is not a cat");
            for (int id : new int[]{pigId, missing, owner.getId()}) {
                h.assertFalse(PacketChangeCatMode.apply(owner, id, 0), "mode on " + id);
                h.assertFalse(PacketRenameCat.apply(owner, id, "Pig"), "rename on " + id);
                h.assertFalse(PacketEquipCatItem.apply(owner, id, 0, false), "equip on " + id);
                h.assertFalse(PacketTrainTalent.apply(owner, id, Talents.POUNCE.getId()), "train on " + id);
                h.assertFalse(PacketOpenCatPack.apply(owner, id), "pack on " + id);
                h.assertFalse(PacketTwineAction.apply(owner, id, PacketTwineAction.ActionType.TWINE_START, 0), "twine on " + id);
                h.assertFalse(PacketTwineAction.apply(owner, id, PacketTwineAction.ActionType.DODGE, 1), "dodge on " + id);
            }
            h.assertFalse(pig.hasCustomName(), "the pig was renamed");
            h.assertFalse(TwineManager.isPlayerTwined(owner.getUUID()), "twined with a non-cat");
            h.succeed();
        } finally {
            leave(h, owner);
        }
    }

    // ------------------------------------------------------------------ Twine control

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testTwineControlNeedsTheSession(GameTestHelper h) {
        FakePlayer owner = join(h, new BlockPos(3, 2, 2));
        FakePlayer trusted = join(h, new BlockPos(2, 2, 3));
        try {
            TalentedCat c = cat(h, new BlockPos(2, 2, 2), owner);
            c.addTrustedPlayer(trusted.getUUID(), trusted.getGameProfile().getName());
            int id = c.getId();

            h.assertFalse(input(id, 0.0F).apply(owner), "input without a session");
            h.assertFalse(PacketTwineAction.apply(owner, id, PacketTwineAction.ActionType.DODGE, 1), "dodge without a session");

            h.assertTrue(PacketTwineAction.apply(owner, id, PacketTwineAction.ActionType.TWINE_START, 0), "owner twine start");
            h.assertTrue(c.isTwinedWith(owner) && id == c.getId() && c.getUUID().equals(TwineManager.getCatOf(owner.getUUID())), "session not registered");
            h.assertTrue(owner.isInvisible() && owner.isInvulnerable() && TwineManager.isBodyHidden(owner.getUUID()), "body should be hidden");

            h.assertFalse(input(id, 0.0F).apply(trusted), "co-owner drove someone else's session");
            h.assertFalse(PacketTwineAction.apply(trusted, id, PacketTwineAction.ActionType.TWINE_START, 0), "second player took over a twined cat");
            h.assertFalse(PacketTwineAction.apply(owner, id, PacketTwineAction.ActionType.DODGE, 7), "dodge direction 7");
            h.assertFalse(PacketTwineAction.apply(owner, id, PacketTwineAction.ActionType.TRAVERSAL_ACTION, 99), "traversal 99");
            h.assertFalse(PacketTwineAction.apply(owner, id, PacketTwineAction.ActionType.AIM_GUN, Float.NaN), "NaN action data");
            h.assertFalse(input(id, Float.NaN).apply(owner), "NaN input");
            h.assertTrue(input(id, 45.0F).apply(owner), "owner input in session");
            h.assertTrue(Math.abs(c.getYRot() - 45.0F) < 0.01F, "input should turn the cat");
            h.assertTrue(new PacketTwineInput(id, 9.0F, -9.0F, false, false, false, false, false, false, false, 0.0F, 500.0F).apply(owner)
                    && c.getTwineXxa() == 1.0F && c.getTwineZza() == -1.0F && c.getXRot() == 90.0F, "input values should be clamped");

            h.assertTrue(PacketTwineAction.apply(owner, id, PacketTwineAction.ActionType.TWINE_STOP, 0), "stop");
            assertRestored(h, owner, c, "after TWINE_STOP");
            h.succeed();
        } finally {
            leave(h, owner, trusted);
        }
    }

    private static PacketTwineInput input(int catId, float yaw) {
        return new PacketTwineInput(catId, 0.0F, 0.0F, false, false, false, false, false, false, false, yaw, 0.0F);
    }

    private static void assertRestored(GameTestHelper h, ServerPlayer p, TalentedCat c, String when) {
        h.assertFalse(TwineManager.isPlayerTwined(p.getUUID()), "player still twined " + when);
        h.assertFalse(c.isTwined(), "cat still twined " + when);
        h.assertFalse(p.isInvisible() || p.isInvulnerable() || p.isNoGravity() || p.noPhysics, "body not restored " + when);
        h.assertFalse(TwineManager.isBodyHidden(p.getUUID()) || p.getPersistentData().contains(TwineManager.BODY_TAG), "body record left " + when);
    }

    private static TalentedCat twine(GameTestHelper h, ServerPlayer p, BlockPos pos) {
        TalentedCat c = cat(h, pos, p);
        h.assertTrue(PacketTwineAction.apply(p, c.getId(), PacketTwineAction.ActionType.TWINE_START, 0), "twine start failed");
        h.assertTrue(p.isInvisible() && TwineManager.isPlayerTwined(p.getUUID()), "twine did not start");
        return c;
    }

    // ------------------------------------------------------------------ Twine edge cases

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testTwineEndsWhenCatIsRemovedOrDies(GameTestHelper h) {
        FakePlayer p = join(h, new BlockPos(3, 2, 2));
        try {
            TalentedCat a = twine(h, p, new BlockPos(2, 2, 2));
            a.discard(); // chunk unload, dimension change and habitats all go through Entity#remove
            assertRestored(h, p, a, "after the cat was removed");

            TalentedCat b = twine(h, p, new BlockPos(4, 2, 4));
            b.die(b.damageSources().genericKill());
            assertRestored(h, p, b, "after the cat died");
            h.succeed();
        } finally {
            leave(h, p);
        }
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testTwineEndsOnPlayerDeathLogoutAndDimensionChange(GameTestHelper h) {
        FakePlayer p = join(h, new BlockPos(3, 2, 2));
        try {
            var bus = net.minecraftforge.common.MinecraftForge.EVENT_BUS;
            TalentedCat a = twine(h, p, new BlockPos(2, 2, 2));
            bus.post(new net.minecraftforge.event.entity.living.LivingDeathEvent(p, p.damageSources().genericKill()));
            assertRestored(h, p, a, "after the player died");

            TalentedCat b = twine(h, p, new BlockPos(4, 2, 2));
            bus.post(new net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(p));
            assertRestored(h, p, b, "after logout");

            TalentedCat c = twine(h, p, new BlockPos(2, 2, 4));
            bus.post(new net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent(p,
                    net.minecraft.world.level.Level.OVERWORLD, net.minecraft.world.level.Level.NETHER));
            assertRestored(h, p, c, "after a dimension change");
            h.succeed();
        } finally {
            leave(h, p);
        }
    }

    /** Switching cats keeps one session; a late end call for the old cat must not end the new one. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testSwitchingCatsKeepsOneSession(GameTestHelper h) {
        FakePlayer p = join(h, new BlockPos(3, 2, 2));
        try {
            TalentedCat a = twine(h, p, new BlockPos(2, 2, 2));
            TalentedCat b = cat(h, new BlockPos(4, 2, 2), p);
            h.assertTrue(PacketTwineAction.apply(p, b.getId(), PacketTwineAction.ActionType.TWINE_START, 0), "switch to b");
            h.assertFalse(a.isTwined(), "old cat still twined");
            h.assertTrue(b.isTwinedWith(p) && b.getUUID().equals(TwineManager.getCatOf(p.getUUID())), "new session not active");
            TwineManager.endForCat(a, null); // stale end for the old cat
            h.assertTrue(TwineManager.isPlayerTwined(p.getUUID()) && p.isInvisible(), "stale end call ended the new session");
            TwineManager.endForPlayer(p, null);
            assertRestored(h, p, b, "after ending the switched session");
            h.succeed();
        } finally {
            leave(h, p);
        }
    }

    /** Crash safety: body flags saved in player data are restored on the next login. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testBodyFlagsRestoredOnLogin(GameTestHelper h) {
        FakePlayer p = join(h, new BlockPos(3, 2, 2));
        try {
            TwineManager.hideDormantBody(p); // what a session does, then the server "crashes" before ending it
            h.assertTrue(p.isInvisible() && p.isInvulnerable() && p.getPersistentData().contains(TwineManager.BODY_TAG), "body should be hidden");
            TwineManager.restoreOnLogin(p);
            h.assertFalse(p.isInvisible() || p.isInvulnerable() || p.isNoGravity() || p.noPhysics, "login did not restore the body");
            h.assertFalse(p.getPersistentData().contains(TwineManager.BODY_TAG) || TwineManager.isBodyHidden(p.getUUID()), "body record left after login");
            h.succeed();
        } finally {
            leave(h, p);
        }
    }

    /** Removing a co-owner who is twined with the cat ends their session. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testRemovedCoOwnerLosesTwine(GameTestHelper h) {
        FakePlayer owner = join(h, new BlockPos(3, 2, 2));
        FakePlayer trusted = join(h, new BlockPos(2, 2, 3));
        try {
            TalentedCat c = cat(h, new BlockPos(2, 2, 2), owner);
            c.addTrustedPlayer(trusted.getUUID(), trusted.getGameProfile().getName());
            h.assertTrue(PacketTwineAction.apply(trusted, c.getId(), PacketTwineAction.ActionType.TWINE_START, 0), "co-owner twine");
            h.assertTrue(PacketManageTrust.apply(owner, c.getId(), PacketManageTrust.REMOVE, trusted.getUUID().toString()), "remove co-owner");
            assertRestored(h, trusted, c, "after the co-owner was removed");
            h.succeed();
        } finally {
            leave(h, owner, trusted);
        }
    }
}
