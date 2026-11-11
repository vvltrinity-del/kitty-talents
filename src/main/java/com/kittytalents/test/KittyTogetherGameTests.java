package com.kittytalents.test;

import com.kittytalents.KittyTalents;
import com.kittytalents.core.perm.CatAction;
import com.kittytalents.core.perm.CatPermissions;
import com.kittytalents.core.perm.TrustRole;
import com.kittytalents.entity.CatMode;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.entity.TwineManager;
import com.kittytalents.entity.WhistleCommand;
import com.kittytalents.entity.interact.CatInteractions;
import com.kittytalents.inventory.CatTalentMenu;
import com.kittytalents.network.PacketChangeCatMode;
import com.kittytalents.network.PacketChangeCatSkin;
import com.kittytalents.network.PacketEquipCatItem;
import com.kittytalents.network.PacketManageTrust;
import com.kittytalents.network.PacketOpenCatPack;
import com.kittytalents.network.PacketTwineAction;
import com.kittytalents.network.PacketWhistleCommand;
import com.kittytalents.registry.ModEntities;
import com.kittytalents.talent.Talents;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static com.kittytalents.test.KittyTalentGameTests.joinFakeOwner;

/** 4.0 Phase 7 "Playing together": co-owner roles and the whistle's scope. */
@GameTestHolder(KittyTalents.MODID)
@PrefixGameTestTemplate(false)
public class KittyTogetherGameTests {

    private static void leave(GameTestHelper h, FakePlayer... players) {
        for (FakePlayer p : players) {
            TwineManager.endForPlayer(p, null);
            p.stopRiding();
            KittyTalentGameTests.leave(h, p);
        }
    }

    private static TalentedCat cat(GameTestHelper h, FakePlayer owner, FakePlayer friend, TrustRole role) {
        TalentedCat c = h.spawn(ModEntities.TALENTED_CAT.get(), new BlockPos(2, 2, 2));
        c.tame(owner);
        c.setCatMode(CatMode.STAY);
        c.addTrustedPlayer(friend.getUUID(), friend.getGameProfile().getName());
        c.setTrustRole(friend.getUUID(), role);
        return c;
    }

    /** The permission matrix per role: Rider rides, Handler also commands and Twines, Full also changes gear and style. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testCoOwnerRoleMatrix(GameTestHelper h) {
        FakePlayer owner = joinFakeOwner(h, new BlockPos(3, 2, 2)), friend = joinFakeOwner(h, new BlockPos(1, 2, 2));
        try {
            for (TrustRole role : TrustRole.values()) {
                TalentedCat c = cat(h, owner, friend, role);
                for (CatAction a : CatAction.values()) {
                    boolean expected = a.minRole() != null && role.atLeast(a.minRole());
                    h.assertTrue(CatPermissions.can(friend, c, a) == expected, role + " / " + a + " should be " + expected);
                    h.assertTrue(CatPermissions.can(owner, c, a), "the owner may always " + a);
                }
                c.discard();
            }
            h.assertTrue(CatAction.RIDE.minRole() == TrustRole.RIDER && CatAction.COMMAND.minRole() == TrustRole.HANDLER
                    && CatAction.EQUIP.minRole() == TrustRole.FULL && CatAction.TRAIN.minRole() == null, "role floor per action");
            h.succeed();
        } finally {
            leave(h, owner, friend);
        }
    }

    /** The same rules through the real entry points: packets, the gear menu, the whistle and right-clicks. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testRolesThroughPacketsAndClicks(GameTestHelper h) {
        FakePlayer owner = joinFakeOwner(h, new BlockPos(3, 2, 2)), friend = joinFakeOwner(h, new BlockPos(1, 2, 2));
        try {
            // Rider: rides, nothing else.
            TalentedCat c = cat(h, owner, friend, TrustRole.RIDER);
            c.setTalentLevel(Talents.CAT_MOUNT, 1);
            int id = c.getId();
            h.assertFalse(PacketChangeCatMode.apply(friend, id, CatMode.GUARD.ordinal()), "a Rider changed the mode");
            h.assertFalse(PacketTwineAction.apply(friend, id, PacketTwineAction.ActionType.TWINE_START, 0), "a Rider Twined");
            h.assertFalse(PacketChangeCatSkin.apply(friend, id, "kittytalents:siamese"), "a Rider changed the skin");
            h.assertFalse(PacketOpenCatPack.apply(friend, id), "a Rider opened the pack");
            h.assertTrue(PacketWhistleCommand.apply(friend, WhistleCommand.FOLLOW.ordinal()) == 0, "a Rider's whistle reached the cat");
            friend.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BLUE_DYE));
            var collar = c.getCollarColor();
            CatInteractions.handle(c, friend, InteractionHand.MAIN_HAND);
            h.assertTrue(c.getCollarColor() == collar && friend.getMainHandItem().getCount() == 1, "a Rider dyed the collar");
            friend.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            CatInteractions.handle(c, friend, InteractionHand.MAIN_HAND);
            h.assertTrue(friend.getVehicle() == c, "a Rider should be able to ride");
            friend.stopRiding();

            // Handler: commands and Twine, but no gear.
            c.setTrustRole(friend.getUUID(), TrustRole.HANDLER);
            h.assertTrue(PacketChangeCatMode.apply(friend, id, CatMode.GUARD.ordinal()) && c.getCatMode() == CatMode.GUARD, "a Handler may change the mode");
            h.assertTrue(PacketWhistleCommand.apply(friend, WhistleCommand.FOLLOW.ordinal()) == 1, "a Handler's whistle reaches the cat");
            h.assertFalse(PacketEquipCatItem.apply(friend, id, PacketEquipCatItem.TOGGLE_COSMETICS, false), "a Handler toggled cosmetics");
            CatTalentMenu menu = new CatTalentMenu(1, friend.getInventory(), c);
            h.assertFalse(menu.canEquip() || menu.getSlot(1).mayPlace(new ItemStack(Items.LEATHER_HELMET)), "a Handler used a gear slot");
            h.assertTrue(PacketTwineAction.apply(friend, id, PacketTwineAction.ActionType.TWINE_START, 0) && c.isTwinedWith(friend), "a Handler may Twine");

            // Demoted mid-Twine: the session ends.
            h.assertTrue(PacketManageTrust.apply(owner, id, PacketManageTrust.SET_ROLE, friend.getUUID() + "|RIDER"), "owner sets a role");
            h.assertTrue(!c.isTwined() && c.getTrustRole(friend.getUUID()) == TrustRole.RIDER, "demotion ends the Twine session");

            // Full: gear too. Only the owner can set roles.
            h.assertTrue(PacketManageTrust.apply(owner, id, PacketManageTrust.SET_ROLE, friend.getUUID() + "|FULL"), "owner sets Full");
            h.assertTrue(PacketEquipCatItem.apply(friend, id, PacketEquipCatItem.TOGGLE_COSMETICS, false), "a Full co-owner may change gear");
            h.assertFalse(PacketManageTrust.apply(friend, id, PacketManageTrust.SET_ROLE, friend.getUUID() + "|FULL"), "a co-owner set a role");
            for (String bad : new String[]{friend.getUUID() + "|OWNER", friend.getUUID() + "|", "x|FULL", "FULL", owner.getUUID() + "|RIDER"}) {
                h.assertFalse(PacketManageTrust.apply(owner, id, PacketManageTrust.SET_ROLE, bad), "bad role payload accepted: " + bad);
            }
            h.succeed();
        } finally {
            leave(h, owner, friend);
        }
    }

    /** Roles are saved; a co-owner saved before 4.0 (no role) is Full, as before. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testRolesSaveAndOldCoOwnersAreFull(GameTestHelper h) {
        FakePlayer owner = joinFakeOwner(h, new BlockPos(3, 2, 2)), friend = joinFakeOwner(h, new BlockPos(1, 2, 2));
        try {
            TalentedCat c = cat(h, owner, friend, TrustRole.RIDER);
            var tag = c.saveWithoutId(new net.minecraft.nbt.CompoundTag());
            tag.remove("UUID");
            TalentedCat loaded = h.spawn(ModEntities.TALENTED_CAT.get(), new BlockPos(4, 2, 4));
            loaded.load(tag);
            h.assertTrue(loaded.getTrustRole(friend.getUUID()) == TrustRole.RIDER, "the role should survive save and load");
            var list = tag.getList("TrustedPlayers", net.minecraft.nbt.Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) list.getCompound(i).remove("Role"); // a 3.0 save
            TalentedCat old = h.spawn(ModEntities.TALENTED_CAT.get(), new BlockPos(1, 2, 4));
            old.load(tag);
            h.assertTrue(old.isTrusted(friend.getUUID()) && old.getTrustRole(friend.getUUID()) == TrustRole.FULL, "3.0 co-owners are Full");
            h.succeed();
        } finally {
            leave(h, owner, friend);
        }
    }

    /** Whistle scope: "mine only" leaves out cats the player only co-owns. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testWhistleScope(GameTestHelper h) {
        FakePlayer me = joinFakeOwner(h, new BlockPos(3, 2, 2)), friend = joinFakeOwner(h, new BlockPos(1, 2, 2));
        try {
            TalentedCat mine = h.spawn(ModEntities.TALENTED_CAT.get(), new BlockPos(2, 2, 3));
            mine.tame(me);
            mine.setCatMode(CatMode.STAY);
            TalentedCat shared = cat(h, friend, me, TrustRole.FULL);
            h.assertTrue(PacketWhistleCommand.apply(me, WhistleCommand.GUARD.ordinal(), true) == 1, "mine only: one cat");
            h.assertTrue(mine.getCatMode() == CatMode.GUARD && shared.getCatMode() == CatMode.STAY, "the co-owned cat must not react");
            h.assertTrue(PacketWhistleCommand.apply(me, WhistleCommand.GUARD.ordinal(), false) == 2 && shared.getCatMode() == CatMode.GUARD,
                    "mine + co-owned: both cats");
            h.succeed();
        } finally {
            leave(h, me, friend);
        }
    }
}
