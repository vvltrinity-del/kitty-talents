package com.kittytalents.test;

import com.kittytalents.KittyTalents;
import com.kittytalents.block.KittyHabitatBlockEntity;
import com.kittytalents.core.living.HabitatModifiers;
import com.kittytalents.entity.CatVariant;
import com.kittytalents.entity.ProductionTier;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.inventory.KittyHabitatMenu;
import com.kittytalents.registry.ModBlocks;
import com.kittytalents.registry.ModEntities;
import com.kittytalents.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;

import static com.kittytalents.block.KittyHabitatBlockEntity.SLOT_CATALYST_IN;
import static com.kittytalents.block.KittyHabitatBlockEntity.SLOT_FOOD_IN;
import static com.kittytalents.test.KittyTalentGameTests.joinFakeOwner;
import static com.kittytalents.test.KittyTalentGameTests.leave;

/** 5.1: the Habitat Smelter Upgrade, and the Habitat's supply slots (food for any worker, catalysts only). */
@GameTestHolder(KittyTalents.MODID)
@PrefixGameTestTemplate(false)
public class KittySmelterSupplyGameTests {

    private static KittyHabitatBlockEntity habitat(GameTestHelper h, BlockPos rel) {
        h.setBlock(rel, ModBlocks.KITTY_HABITAT.get().defaultBlockState());
        BlockPos abs = h.absolutePos(rel);
        HabitatModifiers.loaded(h.getLevel(), abs);
        return (KittyHabitatBlockEntity) h.getLevel().getBlockEntity(abs);
    }

    private static void expect(GameTestHelper h, ItemStack got, Item item, int count, String what) {
        h.assertTrue(got.is(item) && got.getCount() == count, what + ": expected " + count + " " + item + ", got " + got);
    }

    /** The Smelter Upgrade fits the four upgrade slots and nothing else, and doesn't stack. */
    @GameTest(template = "arena", timeoutTicks = 20)
    public static void testSmelterGoesInUpgradeSlotsOnly(GameTestHelper h) {
        var inv = habitat(h, new BlockPos(2, 2, 2)).getInventory();
        ItemStack smelter = new ItemStack(ModItems.UPGRADE_SMELTER.get());
        h.assertTrue(inv.isItemValid(0, smelter) && inv.isItemValid(3, smelter), "upgrade slots take it");
        h.assertFalse(inv.isItemValid(4, smelter) || inv.isItemValid(SLOT_FOOD_IN, smelter) || inv.isItemValid(SLOT_CATALYST_IN, smelter),
                "output, food and catalyst slots don't");
        h.assertTrue(smelter.getMaxStackSize() == 1, "one per slot");
        h.succeed();
    }

    /** Raw ores and ores become what a furnace makes of them, count kept; everything else is left alone. */
    @GameTest(template = "arena", timeoutTicks = 20)
    public static void testSmeltedTurnsRawOresIntoIngotsOnly(GameTestHelper h) {
        var lvl = h.getLevel();
        expect(h, KittyHabitatBlockEntity.smelted(lvl, new ItemStack(Items.RAW_IRON, 3)), Items.IRON_INGOT, 3, "raw iron");
        expect(h, KittyHabitatBlockEntity.smelted(lvl, new ItemStack(Items.RAW_GOLD, 2)), Items.GOLD_INGOT, 2, "raw gold");
        expect(h, KittyHabitatBlockEntity.smelted(lvl, new ItemStack(Items.RAW_COPPER, 5)), Items.COPPER_INGOT, 5, "raw copper");
        expect(h, KittyHabitatBlockEntity.smelted(lvl, new ItemStack(Items.RAW_IRON, 64)), Items.IRON_INGOT, 64, "a full stack");
        expect(h, KittyHabitatBlockEntity.smelted(lvl, new ItemStack(Items.IRON_ORE)), Items.IRON_INGOT, 1, "an ore block");
        expect(h, KittyHabitatBlockEntity.smelted(lvl, new ItemStack(Items.DIAMOND, 2)), Items.DIAMOND, 2, "diamonds stay");
        expect(h, KittyHabitatBlockEntity.smelted(lvl, new ItemStack(Items.COAL, 4)), Items.COAL, 4, "coal stays");
        expect(h, KittyHabitatBlockEntity.smelted(lvl, new ItemStack(Items.SAND)), Items.SAND, 1, "sand is smeltable but not an ore");
        expect(h, KittyHabitatBlockEntity.smelted(lvl, new ItemStack(Items.NETHERITE_SCRAP)), Items.NETHERITE_SCRAP, 1, "netherite scrap stays");
        h.succeed();
    }

    /** An Iron-tier worker: with a Smelter Upgrade the Habitat fills with iron ingots, without one with raw iron. */
    @GameTest(template = "arena", timeoutTicks = 200)
    public static void testHabitatWithSmelterMakesIngots(GameTestHelper h) {
        var smelt = habitat(h, new BlockPos(2, 2, 2));
        var plain = habitat(h, new BlockPos(5, 2, 2));
        for (var be : List.of(smelt, plain)) {
            be.assignWorker(UUID.randomUUID(), "Tester", CatVariant.IRON, ProductionTier.IRON, 10, false, "");
            for (int s = 1; s <= 3; s++) be.getInventory().setStackInSlot(s, new ItemStack(ModItems.UPGRADE_PRODUCTIVITY_6.get()));
        }
        smelt.getInventory().setStackInSlot(0, new ItemStack(ModItems.UPGRADE_SMELTER.get()));
        h.succeedWhen(() -> {
            ItemStack a = smelt.getInventory().getStackInSlot(4), b = plain.getInventory().getStackInSlot(4);
            h.assertTrue(a.is(Items.IRON_INGOT), "the smelter Habitat should make iron ingots, has " + a);
            h.assertTrue(b.is(Items.RAW_IRON), "the plain Habitat should make raw iron, has " + b);
            for (int i = 4; i <= 7; i++) {
                h.assertFalse(smelt.getInventory().getStackInSlot(i).is(Items.RAW_IRON), "no raw iron with a smelter");
            }
        });
    }

    /** User report: raw fish and raw chicken go in the food slot, by click and by shift-click; junk stays out. */
    @GameTest(template = "arena", timeoutTicks = 20)
    public static void testFoodSlotTakesRawFishAndChicken(GameTestHelper h) {
        var be = habitat(h, new BlockPos(2, 2, 2));
        var fp = joinFakeOwner(h, new BlockPos(4, 2, 4));
        try {
            var menu = new KittyHabitatMenu(0, fp.getInventory(), be);
            menu.setCarried(new ItemStack(Items.COD, 3));
            menu.clicked(SLOT_FOOD_IN, 0, ClickType.PICKUP, fp);
            expect(h, be.getInventory().getStackInSlot(SLOT_FOOD_IN), Items.COD, 3, "clicked raw cod into the food slot");
            menu.clicked(SLOT_FOOD_IN, 0, ClickType.PICKUP, fp); // take it back out
            menu.setCarried(ItemStack.EMPTY);
            int hotbar0 = 10 + 27;
            fp.getInventory().setItem(0, new ItemStack(Items.CHICKEN, 5));
            menu.quickMoveStack(fp, hotbar0);
            expect(h, be.getInventory().getStackInSlot(SLOT_FOOD_IN), Items.CHICKEN, 5, "shift-clicked raw chicken");
            fp.getInventory().setItem(1, new ItemStack(Items.COBBLESTONE, 8));
            menu.quickMoveStack(fp, hotbar0 + 1);
            h.assertTrue(be.getInventory().getStackInSlot(SLOT_CATALYST_IN).isEmpty(), "cobblestone must not land in the catalyst slot");
            h.succeed();
        } finally {
            leave(h, fp);
        }
    }

    /** The catalyst slot takes the catalysts and refuses everything else (it used to take anything). */
    @GameTest(template = "arena", timeoutTicks = 20)
    public static void testCatalystSlotTakesOnlyCatalysts(GameTestHelper h) {
        var inv = habitat(h, new BlockPos(2, 2, 2)).getInventory();
        h.assertTrue(inv.isItemValid(SLOT_CATALYST_IN, new ItemStack(ModItems.RESOURCE_CATALYST.get())), "Resource Catalyst");
        h.assertTrue(inv.isItemValid(SLOT_CATALYST_IN, new ItemStack(ModItems.CATNIP_CATALYST.get())), "Catnip Catalyst");
        h.assertFalse(inv.isItemValid(SLOT_CATALYST_IN, new ItemStack(Items.COBBLESTONE)), "cobblestone is no catalyst");
        h.assertFalse(inv.isItemValid(SLOT_CATALYST_IN, new ItemStack(Items.CHICKEN)), "food belongs in the food slot");
        h.succeed();
    }

    /** A housed cat eats from the food slot as soon as a whole meal fits (it used to wait until below 50 hunger). */
    @GameTest(template = "arena", timeoutTicks = 100)
    public static void testHousedCatEatsFromSupplies(GameTestHelper h) {
        var be = habitat(h, new BlockPos(2, 2, 2));
        TalentedCat c = h.spawn(ModEntities.TALENTED_CAT.get(), new BlockPos(2, 3, 2));
        c.setHunger(60);
        be.setWorker(c); // houses it, keeping its 60 hunger
        be.getInventory().setStackInSlot(SLOT_FOOD_IN, new ItemStack(Items.COOKED_COD, 5));
        h.succeedWhen(() -> {
            h.assertTrue(be.getHousedHunger() == 120, "two cooked cod should fill it to 120, hunger " + be.getHousedHunger());
            expect(h, be.getInventory().getStackInSlot(SLOT_FOOD_IN), Items.COOKED_COD, 3, "two eaten, then it stops");
        });
    }

    /** A linked worker starving outside the Habitat gets fed from the food slot and goes back to work. */
    @GameTest(template = "arena", timeoutTicks = 100)
    public static void testLinkedStarvingWorkerGetsFed(GameTestHelper h) {
        var be = habitat(h, new BlockPos(2, 2, 2));
        TalentedCat c = h.spawn(ModEntities.TALENTED_CAT.get(), new BlockPos(6, 2, 6));
        c.setHunger(10);
        be.assignWorker(c.getUUID(), "Linked", c.getCatVariant(), c.getProductionTier(), c.getCatLevel(), c.isFemale(), "");
        be.getInventory().setStackInSlot(SLOT_FOOD_IN, new ItemStack(Items.CHICKEN, 8));
        h.succeedWhen(() -> {
            h.assertTrue(c.getHunger() > 20, "the linked worker should have eaten, hunger " + c.getHunger());
            h.assertTrue(be.getStatus() == KittyHabitatBlockEntity.Status.WORKING, "back to work, status " + be.getStatus());
            h.assertTrue(be.getInventory().getStackInSlot(SLOT_FOOD_IN).getCount() < 8, "chicken taken from the slot");
        });
    }
}
