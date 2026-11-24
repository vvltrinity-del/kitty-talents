package com.kittytalents.test;

import com.kittytalents.KittyTalents;
import com.kittytalents.block.KittyHabitatBlockEntity;
import com.kittytalents.core.living.HabitatModifiers;
import com.kittytalents.core.perm.TrustRole;
import com.kittytalents.entity.CatMode;
import com.kittytalents.entity.CatVariant;
import com.kittytalents.entity.ProductionTier;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.event.ForgeEvents;
import com.kittytalents.inventory.KittyHabitatMenu;
import com.kittytalents.recipe.HabitatProductionRecipe;
import com.kittytalents.registry.ModBlocks;
import com.kittytalents.registry.ModEntities;
import com.kittytalents.registry.ModItems;
import com.kittytalents.talent.Talents;
import com.kittytalents.util.CatFoodHelper;
import com.kittytalents.util.CatTeleport;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;

import static com.kittytalents.block.KittyHabitatBlockEntity.SLOT_CATALYST_IN;
import static com.kittytalents.test.KittyTalentGameTests.joinFakeOwner;
import static com.kittytalents.test.KittyTalentGameTests.leave;

/**
 * 5.1.1 fixes: morning gifts, followers through portals, Return Home across dimensions, co-owner item rules, Happy
 * Eater's meat rule, Habitat recipe time and extra outputs, and the Habitat's synced energy.
 */
@GameTestHolder(KittyTalents.MODID)
@PrefixGameTestTemplate(false)
public class KittyFiveOneOneGameTests {

    private static TalentedCat ownedCat(GameTestHelper h, Player owner, BlockPos pos) {
        TalentedCat c = h.spawn(ModEntities.TALENTED_CAT.get(), pos);
        c.tame(owner);
        c.setOrderedToSit(false);
        c.setCatMode(CatMode.STAY);
        return c;
    }

    private static int itemsNear(GameTestHelper h, Player p) {
        return h.getLevel().getEntitiesOfClass(ItemEntity.class, p.getBoundingBox().inflate(3.0D)).size();
    }

    private static KittyHabitatBlockEntity habitat(GameTestHelper h, BlockPos rel) {
        h.setBlock(rel, ModBlocks.KITTY_HABITAT.get().defaultBlockState());
        BlockPos abs = h.absolutePos(rel);
        HabitatModifiers.loaded(h.getLevel(), abs);
        return (KittyHabitatBlockEntity) h.getLevel().getBlockEntity(abs);
    }

    /** Player report: leaving the bed partway through the night gave gifts every time. Only a night skip does, once a day. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testMorningGiftsOnlyAfterTheNightPasses(GameTestHelper h) {
        var fp = joinFakeOwner(h, new BlockPos(3, 2, 2));
        try {
            TalentedCat c = ownedCat(h, fp, new BlockPos(2, 2, 2));
            c.setTalentLevel(Talents.MOUSER, 1);
            for (int i = 0; i < 3; i++) ForgeEvents.onPlayerWakeUp(new PlayerWakeUpEvent(fp, false, true)); // "Leave Bed"
            ForgeEvents.onPlayerWakeUp(new PlayerWakeUpEvent(fp, true, true)); // woken early
            h.assertTrue(itemsNear(h, fp) == 0, "leaving the bed must not gift, got " + itemsNear(h, fp));
            ForgeEvents.onPlayerWakeUp(new PlayerWakeUpEvent(fp, false, false)); // the night passed
            h.assertTrue(itemsNear(h, fp) == 1, "one gift after the night, got " + itemsNear(h, fp));
            ForgeEvents.onPlayerWakeUp(new PlayerWakeUpEvent(fp, false, false));
            h.assertTrue(itemsNear(h, fp) == 1, "a second wake the same day must not gift again, got " + itemsNear(h, fp));
            fp.getPersistentData().putLong(ForgeEvents.GIFT_DAY_KEY, h.getLevel().getDayTime() / 24000L - 1); // last gifts: yesterday
            ForgeEvents.onPlayerWakeUp(new PlayerWakeUpEvent(fp, false, false));
            h.assertTrue(itemsNear(h, fp) == 2, "the next morning gifts again, got " + itemsNear(h, fp));
            h.succeed();
        } finally {
            leave(h, fp);
        }
    }

    /** Support cats come through a portal with the owner; Guard and Aggressive cats (not following) stay put. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testPortalFollowersFollowTheirMode(GameTestHelper h) {
        var fp = joinFakeOwner(h, new BlockPos(3, 2, 2));
        try {
            TalentedCat support = ownedCat(h, fp, new BlockPos(2, 2, 2));
            support.setCatMode(CatMode.SUPPORT);
            TalentedCat guard = ownedCat(h, fp, new BlockPos(2, 2, 4));
            guard.setCatMode(CatMode.GUARD);
            TalentedCat aggressive = ownedCat(h, fp, new BlockPos(4, 2, 4));
            aggressive.setCatMode(CatMode.AGGRESSIVE);
            var nether = h.getLevel().getServer().getLevel(Level.NETHER);
            h.assertTrue(nether != null, "GameTest server needs a Nether");
            var moved = ForgeEvents.bringFollowers(fp, h.getLevel(), nether);
            h.assertTrue(moved.size() == 1 && support.isRemoved(), "only the Support cat should come along, moved " + moved.size());
            h.assertTrue(!guard.isRemoved() && !aggressive.isRemoved(), "Guard and Aggressive cats stay at their posts");
            moved.forEach(Entity::discard);
            h.succeed();
        } finally {
            leave(h, fp);
        }
    }

    /** The whistle's Return Home goes to the bed's dimension (it used to use the bed's coordinates in the cat's own). */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testReturnHomeCrossesDimensions(GameTestHelper h) {
        var fp = joinFakeOwner(h, new BlockPos(3, 2, 2));
        try {
            TalentedCat c = ownedCat(h, fp, new BlockPos(2, 2, 2));
            BlockPos here = h.absolutePos(new BlockPos(5, 2, 5));
            c.setHomeBedPos(here);
            c.setHomeBedDimension(h.getLevel().dimension().location().toString());
            h.assertTrue(CatTeleport.isHomeBedHere(c), "the bed is in this dimension");
            h.assertTrue(CatTeleport.sendHome(c, 1.0D) == c && c.blockPosition().closerThan(here, 2.0D),
                    "same-dimension Return Home should land on the bed: " + c.blockPosition());
            BlockPos netherBed = new BlockPos(here.getX(), 70, here.getZ());
            c.setHomeBedPos(netherBed);
            c.setHomeBedDimension("minecraft:the_nether");
            h.assertFalse(CatTeleport.isHomeBedHere(c), "the bed is in another dimension");
            TalentedCat moved = CatTeleport.sendHome(c, 1.0D);
            h.assertTrue(moved != null && moved.level().dimension() == Level.NETHER && moved.blockPosition().closerThan(netherBed, 2.0D),
                    "Return Home should cross to the bed's dimension: " + (moved == null ? "null" : moved.level().dimension().location() + " " + moved.blockPosition()));
            h.assertTrue(c.isRemoved(), "the cat should have left this dimension");
            moved.discard();
            h.succeed();
        } finally {
            leave(h, fp);
        }
    }

    private static boolean use(Player p, TalentedCat c, Item item) {
        ItemStack stack = new ItemStack(item);
        p.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return item.interactLivingEntity(stack, p, c, InteractionHand.MAIN_HAND).consumesAction();
    }

    /** Items follow the co-owner roles: a Rider can't style or carry the cat; respec and stat reset are the owner's. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testCoOwnerItemsFollowTheRoles(GameTestHelper h) {
        FakePlayer owner = joinFakeOwner(h, new BlockPos(3, 2, 2)), friend = joinFakeOwner(h, new BlockPos(1, 2, 2));
        try {
            TalentedCat c = ownedCat(h, owner, new BlockPos(2, 2, 2));
            c.addTrustedPlayer(friend.getUUID(), friend.getGameProfile().getName());
            c.setTrustRole(friend.getUUID(), TrustRole.RIDER);
            h.assertFalse(use(friend, c, ModItems.CAT_COLLAR.get()), "a Rider put on a collar");
            h.assertFalse(use(friend, c, ModItems.CAT_CARRIER.get()), "a Rider picked the cat up");
            h.assertTrue(!c.isRemoved(), "the cat must still be here");
            c.setTrustRole(friend.getUUID(), TrustRole.FULL);
            h.assertTrue(use(friend, c, ModItems.CAT_COLLAR.get()), "a Full co-owner may put on a collar");
            h.assertFalse(use(friend, c, ModItems.CATNIP_RESPEC_ELIXIR.get()), "a co-owner used a Respec Elixir");
            h.assertFalse(use(friend, c, ModItems.AMNESIA_TREAT.get()), "a co-owner reset the statistics");
            h.assertTrue(use(owner, c, ModItems.AMNESIA_TREAT.get()), "the owner may reset the statistics");
            h.succeed();
        } finally {
            leave(h, owner);
            leave(h, friend);
        }
    }

    /** Happy Eater's rule applies: beef, pork and mutton need it; every cat eats chicken, fish and the meals. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testHappyEaterMeatRule(GameTestHelper h) {
        TalentedCat plain = h.spawn(ModEntities.TALENTED_CAT.get(), new BlockPos(2, 2, 2));
        TalentedCat eater = h.spawn(ModEntities.TALENTED_CAT.get(), new BlockPos(4, 2, 2));
        eater.setTalentLevel(Talents.HAPPY_EATER, 1);
        for (Item meat : new Item[]{Items.BEEF, Items.COOKED_PORKCHOP, Items.MUTTON}) {
            h.assertFalse(CatFoodHelper.isEdibleByCat(new ItemStack(meat), plain), "a cat without Happy Eater ate " + meat);
            h.assertTrue(CatFoodHelper.isEdibleByCat(new ItemStack(meat), eater), "a Happy Eater refused " + meat);
            h.assertTrue(CatFoodHelper.isEdibleByCat(new ItemStack(meat), null), "a bowl or Habitat slot should take " + meat);
        }
        for (Item food : new Item[]{Items.CHICKEN, Items.COOKED_CHICKEN, Items.COD, Items.COOKED_SALMON}) {
            h.assertTrue(CatFoodHelper.isEdibleByCat(new ItemStack(food), plain), "every cat eats " + food);
        }
        h.assertTrue(CatFoodHelper.isEdibleByCat(new ItemStack(ModItems.CHICKEN_AND_RICE.get()), plain), "every cat eats Chicken and Rice");
        h.succeed();
    }

    /** The catnip recipe's time is read, and extra outputs roll by their chance. */
    @GameTest(template = "arena", timeoutTicks = 20)
    public static void testHabitatRecipeTimeAndExtraRolls(GameTestHelper h) {
        var r = h.getLevel().getRecipeManager().byKey(new ResourceLocation(KittyTalents.MODID, "habitat_catnip_production"));
        h.assertTrue(r.isPresent() && r.get() instanceof HabitatProductionRecipe hr && hr.getDuration() == 200,
                "the catnip recipe should run for its 200 ticks");
        var always = new HabitatProductionRecipe.WeightedOutput(new ItemStack(Items.DIAMOND), 1.0F);
        var never = new HabitatProductionRecipe.WeightedOutput(new ItemStack(Items.EMERALD), 0.0F);
        List<ItemStack> rolled = KittyHabitatBlockEntity.rollExtras(List.of(always, never), h.getLevel().random);
        h.assertTrue(rolled.size() == 1 && rolled.get(0).is(Items.DIAMOND), "chance 1 always comes out, chance 0 never: " + rolled);
        h.succeed();
    }

    /** A catnip Habitat makes the recipe's 50% Catnip Treats as well as catnip (only the first output used to drop). */
    @GameTest(template = "arena", timeoutTicks = 1000)
    public static void testHabitatMakesRecipeExtras(GameTestHelper h) {
        var be = habitat(h, new BlockPos(2, 2, 2));
        be.assignWorker(UUID.randomUUID(), "Tester", CatVariant.TABBY, ProductionTier.STANDARD, 10, false, "");
        for (int s = 0; s <= 3; s++) be.getInventory().setStackInSlot(s, new ItemStack(ModItems.UPGRADE_PRODUCTIVITY_1.get()));
        be.getInventory().setStackInSlot(SLOT_CATALYST_IN, new ItemStack(ModItems.CATNIP_CATALYST.get()));
        be.getCapability(ForgeCapabilities.ENERGY).ifPresent(e -> {
            for (int i = 0; i < 50; i++) e.receiveEnergy(400, false);
        });
        h.succeedWhen(() -> {
            boolean catnip = false, treat = false;
            for (int i = 4; i <= 7; i++) {
                ItemStack s = be.getInventory().getStackInSlot(i);
                catnip |= s.is(ModItems.CATNIP.get());
                treat |= s.is(ModItems.CATNIP_TREAT.get());
            }
            h.assertTrue(catnip && treat, "the catnip recipe should make catnip and Catnip Treats (catnip " + catnip + ", treats " + treat + ")");
        });
    }

    /** The Habitat's stored FE reaches the screen (5.1.1 energy bar). */
    @GameTest(template = "arena", timeoutTicks = 20)
    public static void testHabitatEnergyIsSynced(GameTestHelper h) {
        var be = habitat(h, new BlockPos(2, 2, 2));
        h.assertTrue(be.dataAccess.getCount() == KittyHabitatBlockEntity.DATA_COUNT, "the synced value count");
        be.getEnergyStorage().receiveEnergy(400, false);
        be.getEnergyStorage().receiveEnergy(400, false);
        h.assertTrue(be.dataAccess.get(5) == 800, "stored FE should be synced, got " + be.dataAccess.get(5));
        var fp = joinFakeOwner(h, new BlockPos(4, 2, 4));
        try {
            var menu = new KittyHabitatMenu(0, fp.getInventory(), be);
            h.assertTrue(menu.getEnergy() == 800, "the menu should read it, got " + menu.getEnergy());
        } finally {
            leave(h, fp);
        }
        h.succeed();
    }
}
