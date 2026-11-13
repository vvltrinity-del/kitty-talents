package com.kittytalents.test;

import com.kittytalents.KittyTalents;
import com.kittytalents.core.gear.CatGearRules;
import com.kittytalents.entity.CatMode;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.inventory.CatTalentMenu;
import com.kittytalents.registry.ModEntities;
import com.kittytalents.registry.ModItems;
import com.kittytalents.talent.Talents;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static com.kittytalents.test.KittyTalentGameTests.joinFakeOwner;
import static com.kittytalents.test.KittyTalentGameTests.leave;

/**
 * 4.0 Phase 5: the Gear tab's real slots. Clicks go through {@link CatTalentMenu#clicked} exactly as a client's
 * container clicks do on the server, so these cover drag (pick up + place), shift-click and plain click.
 */
@GameTestHolder(KittyTalents.MODID)
@PrefixGameTestTemplate(false)
public class KittyGearGameTests {

    private static TalentedCat ownedCat(GameTestHelper h, FakePlayer owner) {
        TalentedCat c = h.spawn(ModEntities.TALENTED_CAT.get(), new BlockPos(2, 2, 2));
        c.tame(owner);
        c.setCatMode(CatMode.STAY);
        return c;
    }

    private static CatTalentMenu open(FakePlayer p, TalentedCat c) {
        CatTalentMenu menu = new CatTalentMenu(1, p.getInventory(), c);
        p.containerMenu = menu;
        return menu;
    }

    /** Menu index of player inventory slot {@code inv} (0..8 hotbar, 9..35 main). */
    private static int menuIndex(int inv) {
        return inv < 9 ? CatTalentMenu.HOTBAR_START + inv : CatTalentMenu.INV_START + (inv - 9);
    }

    /** Drag: pick the stack up from the inventory slot, drop it on the gear slot. */
    private static void drag(CatTalentMenu m, FakePlayer p, int inv, int gearSlot) {
        m.clicked(menuIndex(inv), 0, ClickType.PICKUP, p);
        m.clicked(gearSlot, 0, ClickType.PICKUP, p);
    }

    private static void give(FakePlayer p, int inv, Item item, int count) {
        p.getInventory().setItem(inv, new ItemStack(item, count));
    }

    /** Every slot accepts its own kind of item and rejects the others, by drag. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testEachGearSlotAcceptsTheRightItems(GameTestHelper h) {
        FakePlayer p = joinFakeOwner(h, new BlockPos(3, 2, 2));
        try {
            TalentedCat c = ownedCat(h, p);
            c.setTalentLevel(Talents.CAT_ARMORY, 5);
            c.setTalentLevel(Talents.CAT_ARMOR, 5);
            CatTalentMenu m = open(p, c);
            Item[] right = {Items.IRON_SWORD, Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS,
                    ModItems.TOP_HAT.get(), Items.IRON_PICKAXE, ModItems.BOWTIE.get(), ModItems.CAT_COLLAR.get(), ModItems.CROWN.get()};
            for (int slot = 0; slot < CatGearRules.COUNT; slot++) {
                // A wrong item first (a stone block fits nowhere; a helmet fits only slot 1)
                Item wrong = slot == 1 ? Items.IRON_BOOTS : Items.IRON_HELMET;
                give(p, 0, wrong, 1);
                drag(m, p, 0, slot);
                h.assertTrue(CatGearRules.get(c, slot).isEmpty(), "slot " + slot + " accepted " + wrong);
                h.assertTrue(m.getCarried().is(wrong), "a refused item stays on the cursor (slot " + slot + ")");
                m.clicked(menuIndex(0), 0, ClickType.PICKUP, p); // put it back
                give(p, 0, right[slot], 1);
                drag(m, p, 0, slot);
                h.assertTrue(CatGearRules.get(c, slot).is(right[slot]), "slot " + slot + " should take " + right[slot]);
                h.assertTrue(m.getCarried().isEmpty(), "cursor empty after equipping slot " + slot);
            }
            h.assertTrue(c.getArmorValue() > 0, "equipped armor should count");
            h.succeed();
        } finally {
            leave(h, p);
        }
    }

    /** 5.0: basic gear needs no talent; diamond (rank 1) and netherite (rank 3) are gated by drag and shift-click alike. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testRankGatedGearIsRefusedEveryWay(GameTestHelper h) {
        FakePlayer p = joinFakeOwner(h, new BlockPos(3, 2, 2));
        try {
            TalentedCat c = ownedCat(h, p);
            CatTalentMenu m = open(p, c);
            h.assertTrue(CatGearRules.refusal(c, 2, new ItemStack(Items.IRON_CHESTPLATE)) == null, "any cat can wear iron armor");
            h.assertTrue(CatGearRules.refusal(c, 0, new ItemStack(Items.IRON_SWORD)) == null, "any cat can hold an iron sword");
            h.assertTrue(CatGearRules.refusal(c, 2, new ItemStack(Items.DIAMOND_CHESTPLATE)) != null, "rank 0 can't wear diamond");
            h.assertTrue(CatGearRules.refusal(c, 2, new ItemStack(Items.NETHERITE_CHESTPLATE)) != null, "rank 0 can't wear netherite");
            give(p, 0, Items.DIAMOND_CHESTPLATE, 1);
            drag(m, p, 0, 2);
            h.assertTrue(CatGearRules.get(c, 2).isEmpty(), "drag must refuse diamond armor at rank 0");
            m.clicked(menuIndex(0), 0, ClickType.PICKUP, p);
            m.clicked(menuIndex(0), 0, ClickType.QUICK_MOVE, p);
            h.assertTrue(CatGearRules.get(c, 2).isEmpty() && p.getInventory().getItem(0).is(Items.DIAMOND_CHESTPLATE), "shift-click must refuse it too");
            give(p, 1, Items.DIAMOND_SWORD, 1);
            m.clicked(menuIndex(1), 0, ClickType.QUICK_MOVE, p);
            h.assertTrue(CatGearRules.get(c, 0).isEmpty(), "diamond sword needs Cat Armory 1");
            c.setTalentLevel(Talents.CAT_ARMOR, 1);
            h.assertTrue(CatGearRules.refusal(c, 2, new ItemStack(Items.NETHERITE_CHESTPLATE)) != null, "netherite needs rank 3");
            m.clicked(menuIndex(0), 0, ClickType.QUICK_MOVE, p);
            h.assertTrue(CatGearRules.get(c, 2).is(Items.DIAMOND_CHESTPLATE), "at rank 1 shift-click equips it");
            h.succeed();
        } finally {
            leave(h, p);
        }
    }

    /** Shift-click moves items both ways; gear slots hold one item; swapping puts the old item on the cursor. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testShiftClickBothWaysAndSwap(GameTestHelper h) {
        FakePlayer p = joinFakeOwner(h, new BlockPos(3, 2, 2));
        try {
            TalentedCat c = ownedCat(h, p);
            c.setTalentLevel(Talents.CAT_ARMORY, 5);
            CatTalentMenu m = open(p, c);
            give(p, 4, ModItems.CAT_COLLAR.get(), 5);
            m.clicked(menuIndex(4), 0, ClickType.QUICK_MOVE, p);
            h.assertTrue(c.getCosmeticSlot(0).is(ModItems.CAT_COLLAR.get()) && c.getCosmeticSlot(0).getCount() == 1, "one collar into the first cosmetic slot");
            h.assertTrue(p.getInventory().getItem(4).getCount() == 4, "four collars stay, got " + p.getInventory().getItem(4).getCount());

            give(p, 0, Items.IRON_SWORD, 1);
            drag(m, p, 0, 0);
            give(p, 0, Items.GOLDEN_SWORD, 1);
            drag(m, p, 0, 0);
            h.assertTrue(c.getEquippedWeapon().is(Items.GOLDEN_SWORD) && m.getCarried().is(Items.IRON_SWORD), "swap puts the old sword on the cursor");
            m.clicked(menuIndex(0), 0, ClickType.PICKUP, p);

            m.clicked(0, 0, ClickType.QUICK_MOVE, p);
            h.assertTrue(c.getEquippedWeapon().isEmpty(), "shift-click on a gear slot unequips");
            h.assertTrue(p.getInventory().countItem(Items.GOLDEN_SWORD) == 1, "the sword goes to the player's inventory");
            h.succeed();
        } finally {
            leave(h, p);
        }
    }

    /** A player without EQUIP permission can't take or put gear through the menu. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testStrangerCannotUseGearSlots(GameTestHelper h) {
        FakePlayer owner = joinFakeOwner(h, new BlockPos(3, 2, 2));
        FakePlayer stranger = joinFakeOwner(h, new BlockPos(1, 2, 2));
        try {
            TalentedCat c = ownedCat(h, owner);
            c.setEquippedWeapon(new ItemStack(Items.WOODEN_SWORD));
            CatTalentMenu m = open(stranger, c);
            m.clicked(0, 0, ClickType.PICKUP, stranger);
            h.assertTrue(c.getEquippedWeapon().is(Items.WOODEN_SWORD) && m.getCarried().isEmpty(), "a stranger took the weapon");
            m.clicked(0, 0, ClickType.QUICK_MOVE, stranger);
            h.assertTrue(c.getEquippedWeapon().is(Items.WOODEN_SWORD), "a stranger shift-clicked the weapon away");
            give(stranger, 0, Items.IRON_HELMET, 1);
            m.clicked(menuIndex(0), 0, ClickType.QUICK_MOVE, stranger);
            h.assertTrue(c.getArmorSlotItem(1).isEmpty(), "a stranger equipped a helmet");
            h.succeed();
        } finally {
            leave(h, owner);
            leave(h, stranger);
        }
    }
}
