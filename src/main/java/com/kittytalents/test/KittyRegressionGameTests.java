package com.kittytalents.test;

import com.kittytalents.KittyTalents;
import com.kittytalents.block.CatFoodBowlBlockEntity;
import com.kittytalents.entity.CatMode;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.entity.WhistleCommand;
import com.kittytalents.entity.interact.CatInteractions;
import com.kittytalents.network.PacketChangeCatSkin;
import com.kittytalents.network.PacketChangeCollarColor;
import com.kittytalents.network.PacketRenameCat;
import com.kittytalents.network.PacketWhistleCommand;
import com.kittytalents.registry.ModBlocks;
import com.kittytalents.registry.ModEntities;
import com.kittytalents.registry.ModItems;
import com.kittytalents.talent.Talents;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static com.kittytalents.test.KittyTalentGameTests.joinFakeOwner;
import static com.kittytalents.test.KittyTalentGameTests.leave;

/**
 * Phase 8 R36 regression list: the 1.0/2.0 features that had no test yet (taming, naming, collars, skins, feeding,
 * food bowl, revival, mounting, weapons, whistle, statistics, breeding), driven through the same entry points the game
 * uses (CatInteractions#handle for right-clicks, packet apply() for screen actions).
 */
@GameTestHolder(KittyTalents.MODID)
@PrefixGameTestTemplate(false)
public class KittyRegressionGameTests {

    private static TalentedCat ownedCat(GameTestHelper h, net.minecraft.world.entity.player.Player owner, BlockPos pos) {
        TalentedCat c = h.spawn(ModEntities.TALENTED_CAT.get(), pos);
        c.tame(owner);
        c.setCatMode(CatMode.STAY);
        return c;
    }

    private static void hold(net.minecraft.world.entity.player.Player p, ItemStack stack) {
        p.setItemInHand(InteractionHand.MAIN_HAND, stack);
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testTamingWithCatnipTreat(GameTestHelper h) {
        var fp = joinFakeOwner(h, new BlockPos(3, 2, 2));
        try {
            TalentedCat c = h.spawn(ModEntities.TALENTED_CAT.get(), new BlockPos(2, 2, 2));
            h.assertTrue(!c.isTame(), "Fresh cat should be wild");
            hold(fp, new ItemStack(ModItems.CATNIP_TREAT.get(), 2));
            CatInteractions.handle(c, fp, InteractionHand.MAIN_HAND);
            h.assertTrue(c.isTame() && fp.getUUID().equals(c.getOwnerUUID()), "Catnip Treat should tame the cat to the player");
            h.assertTrue(fp.getMainHandItem().getCount() == 1, "One treat should be used");
            h.assertTrue(c.getSkillPoints() >= c.startingSkillPoints(), "A newly tamed cat gets its starting points");
            h.succeed();
        } finally {
            leave(h, fp);
        }
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testNamingCollarAndSkinFromScreenAndDye(GameTestHelper h) {
        var fp = joinFakeOwner(h, new BlockPos(3, 2, 2));
        try {
            TalentedCat c = ownedCat(h, fp, new BlockPos(2, 2, 2));
            h.assertTrue(PacketRenameCat.apply(fp, c.getId(), "§cWhiskers"), "Owner rename should apply");
            String name = c.getCustomName() == null ? "" : c.getCustomName().getString();
            h.assertTrue(name.equals("Whiskers"), "Name should drop formatting codes, got '" + name + "'");

            h.assertTrue(PacketChangeCollarColor.apply(fp, c.getId(), DyeColor.LIME.getId()), "Collar packet should apply");
            h.assertTrue(c.getCollarColor() == DyeColor.LIME, "Collar should be lime");
            hold(fp, new ItemStack(Items.BLUE_DYE));
            CatInteractions.handle(c, fp, InteractionHand.MAIN_HAND);
            h.assertTrue(c.getCollarColor() == DyeColor.BLUE && fp.getMainHandItem().isEmpty(), "Dye should recolor the collar and be used");

            h.assertTrue(PacketChangeCatSkin.apply(fp, c.getId(), 3), "Skin packet should apply");
            h.assertTrue(c.getSkinId() == 3, "Skin should be 3, got " + c.getSkinId());
            h.succeed();
        } finally {
            leave(h, fp);
        }
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testFeedingRestoresHungerAndHealth(GameTestHelper h) {
        var fp = joinFakeOwner(h, new BlockPos(3, 2, 2));
        try {
            TalentedCat c = ownedCat(h, fp, new BlockPos(2, 2, 2));
            c.setHunger(50);
            c.setHealth(c.getMaxHealth() - 8.0F);
            float hp = c.getHealth();
            hold(fp, new ItemStack(Items.COOKED_COD, 3));
            CatInteractions.handle(c, fp, InteractionHand.MAIN_HAND);
            h.assertTrue(c.getHunger() == 75, "Fish should add 25 hunger, have " + c.getHunger());
            h.assertTrue(c.getHealth() > hp, "Fish should heal");
            h.assertTrue(fp.getMainHandItem().getCount() == 2, "One fish should be eaten");
            h.succeed();
        } finally {
            leave(h, fp);
        }
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void testHungryCatEatsFromFoodBowl(GameTestHelper h) {
        BlockPos bowlPos = new BlockPos(5, 2, 5);
        h.setBlock(bowlPos, ModBlocks.CAT_FOOD_BOWL.get());
        CatFoodBowlBlockEntity bowl = (CatFoodBowlBlockEntity) h.getBlockEntity(bowlPos);
        h.assertTrue(bowl != null, "Bowl block entity should exist");
        bowl.getInventory().insertItem(0, new ItemStack(Items.COOKED_SALMON, 8), false);
        // A real owner: vanilla's sit goal keeps a tamed cat with no owner sitting forever.
        var fp = joinFakeOwner(h, new BlockPos(1, 2, 6));
        TalentedCat c = ownedCat(h, fp, new BlockPos(2, 2, 2));
        c.setCatMode(CatMode.WANDER);
        c.setOrderedToSit(false);
        c.setHunger(30);
        h.succeedWhen(() -> {
            h.assertTrue(c.getHunger() > 30, "Cat should eat from the bowl (hunger " + c.getHunger() + ")");
            h.assertTrue(bowl.getTotalFoodCount() < 8, "Bowl should lose food");
            leave(h, fp);
        });
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testGoldenAppleRevivesDownedCat(GameTestHelper h) {
        var fp = joinFakeOwner(h, new BlockPos(3, 2, 2));
        try {
            TalentedCat c = ownedCat(h, fp, new BlockPos(2, 2, 2));
            c.setHealth(1.0F);
            c.setIncapacitated(true);
            hold(fp, new ItemStack(ModItems.CATNIP.get()));
            CatInteractions.handle(c, fp, InteractionHand.MAIN_HAND);
            h.assertTrue(c.isIncapacitated(), "The wrong item must not revive");
            hold(fp, new ItemStack(Items.GOLDEN_APPLE));
            CatInteractions.handle(c, fp, InteractionHand.MAIN_HAND);
            h.assertTrue(!c.isIncapacitated() && c.getHealth() > 1.0F, "Golden apple should revive and heal");
            h.succeed();
        } finally {
            leave(h, fp);
        }
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testCatMountTalentLetsOwnerRide(GameTestHelper h) {
        var fp = joinFakeOwner(h, new BlockPos(3, 2, 2));
        try {
            TalentedCat c = ownedCat(h, fp, new BlockPos(2, 2, 2));
            hold(fp, ItemStack.EMPTY);
            CatInteractions.handle(c, fp, InteractionHand.MAIN_HAND);
            h.assertTrue(!fp.isPassenger(), "Without Cat Mount the owner must not ride");
            c.setTalentLevel(Talents.CAT_MOUNT, 1);
            CatInteractions.handle(c, fp, InteractionHand.MAIN_HAND);
            h.assertTrue(fp.getVehicle() == c, "With Cat Mount the owner should ride the cat");
            h.assertTrue(c.getCatScale() > 1.0F, "A ridden mount cat grows");
            fp.stopRiding();
            h.assertTrue(c.getCatScale() == 1.0F, "Back to normal size once dismounted");
            h.succeed();
        } finally {
            leave(h, fp);
        }
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testRightClickSwordEquipsWeapon(GameTestHelper h) {
        var fp = joinFakeOwner(h, new BlockPos(3, 2, 2));
        try {
            TalentedCat c = ownedCat(h, fp, new BlockPos(2, 2, 2));
            hold(fp, new ItemStack(Items.IRON_SWORD));
            CatInteractions.handle(c, fp, InteractionHand.MAIN_HAND);
            h.assertTrue(c.getEquippedWeapon().is(Items.IRON_SWORD), "Right-clicking with a sword should equip it");
            h.succeed();
        } finally {
            leave(h, fp);
        }
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testWhistleOnlyCommandsYourCats(GameTestHelper h) {
        var fp = joinFakeOwner(h, new BlockPos(3, 2, 2));
        var other = joinFakeOwner(h, new BlockPos(6, 2, 6));
        try {
            TalentedCat mine = ownedCat(h, fp, new BlockPos(2, 2, 2));
            TalentedCat theirs = ownedCat(h, other, new BlockPos(4, 2, 4));
            mine.setOrderedToSit(false);
            theirs.setOrderedToSit(false);
            int n = PacketWhistleCommand.apply(fp, WhistleCommand.STAY.ordinal());
            h.assertTrue(n >= 1 && mine.isOrderedToSit(), "Whistle STAY should sit my cat");
            h.assertTrue(!theirs.isOrderedToSit(), "Whistle must not command someone else's cat");
            h.succeed();
        } finally {
            leave(h, fp);
            leave(h, other);
        }
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testKillStatisticsAreRecorded(GameTestHelper h) {
        TalentedCat c = h.spawn(ModEntities.TALENTED_CAT.get(), new BlockPos(2, 2, 2));
        c.setTame(true);
        c.setCatMode(CatMode.STAY);
        Zombie z = h.spawn(EntityType.ZOMBIE, new BlockPos(3, 2, 2));
        z.setHealth(1.0F);
        int kills = c.getTotalKills(), hostile = c.getHostileKills();
        c.doHurtTarget(z);
        h.assertTrue(!z.isAlive(), "Zombie on 1 HP should die");
        h.assertTrue(c.getTotalKills() == kills + 1 && c.getHostileKills() == hostile + 1, "Kill counters should rise");
        h.assertTrue(c.getDamageDealt() > 0, "Damage dealt should be recorded");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testBreedingTwoCatsGivesTamedTalentedKitten(GameTestHelper h) {
        var fp = joinFakeOwner(h, new BlockPos(3, 2, 2));
        try {
            TalentedCat a = ownedCat(h, fp, new BlockPos(2, 2, 2));
            TalentedCat b = ownedCat(h, fp, new BlockPos(2, 2, 4));
            var child = a.getBreedOffspring(h.getLevel(), b);
            h.assertTrue(child instanceof TalentedCat, "Offspring should be a Talented Cat");
            TalentedCat kitten = (TalentedCat) child;
            h.assertTrue(kitten.isTame() && fp.getUUID().equals(kitten.getOwnerUUID()), "Kitten should belong to the parents' owner");
            h.assertTrue(kitten.getProductionTier() != null && kitten.getCatVariant() != null, "Kitten needs a tier and variant");
            h.succeed();
        } finally {
            leave(h, fp);
        }
    }

    /** API marker goal added by {@link #addMarker}; never runs. */
    static final class ApiMarkerGoal extends net.minecraft.world.entity.ai.goal.Goal {
        @Override public boolean canUse() { return false; }
    }

    /** Test-only RegisterCatGoalsEvent listener (the way an addon would use the API). */
    @net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = KittyTalents.MODID)
    public static final class ApiListener {
        @net.minecraftforge.eventbus.api.SubscribeEvent
        public static void addMarker(com.kittytalents.api.event.RegisterCatGoalsEvent event) {
            // GameTest runs only: this class ships in the jar, real games must not get the marker goal.
            if (System.getProperty("forge.enabledGameTestNamespaces") == null) return;
            event.addGoal(com.kittytalents.api.CatGoalBand.NEEDS, new ApiMarkerGoal());
        }
    }

    /** Addon goals arrive once per cat, in the requested band, and are not duplicated when the cat is reloaded. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testAddonGoalsRegisterOncePerCat(GameTestHelper h) {
        TalentedCat c = h.spawn(ModEntities.TALENTED_CAT.get(), new BlockPos(2, 2, 2));
        java.util.function.Supplier<java.util.List<Integer>> markers = () -> c.goalSelector.getAvailableGoals().stream()
                .filter(w -> w.getGoal() instanceof ApiMarkerGoal).map(w -> w.getPriority()).toList();
        h.assertTrue(markers.get().equals(java.util.List.of(com.kittytalents.api.CatGoalBand.NEEDS.priority())),
                "Expected one marker goal in NEEDS, got " + markers.get());
        // Reload the same entity into the level (what a chunk reload does): no extra goals.
        net.minecraft.nbt.CompoundTag tag = c.saveWithoutId(new net.minecraft.nbt.CompoundTag());
        c.load(tag);
        h.assertTrue(markers.get().size() == 1, "Reloading must not add the goal again: " + markers.get());
        h.succeed();
    }
}
