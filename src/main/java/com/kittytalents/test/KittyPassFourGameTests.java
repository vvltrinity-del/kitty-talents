package com.kittytalents.test;

import com.kittytalents.KittyTalents;
import com.kittytalents.block.KittyHabitatBlockEntity;
import com.kittytalents.combat.CatRole;
import com.kittytalents.core.base.BaseAreas;
import com.kittytalents.core.base.BasePatrol;
import com.kittytalents.core.living.CatBond;
import com.kittytalents.core.living.CatTrait;
import com.kittytalents.core.living.HabitatModifiers;
import com.kittytalents.entity.CatBreedingManager;
import com.kittytalents.entity.CatMode;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.registry.ModBlocks;
import com.kittytalents.registry.ModEntities;
import com.kittytalents.registry.ModItems;
import com.kittytalents.util.CatFoodHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static com.kittytalents.test.KittyTalentGameTests.joinFakeOwner;
import static com.kittytalents.test.KittyTalentGameTests.leave;

/** 5.0 pass 4: new features. */
@GameTestHolder(KittyTalents.MODID)
@PrefixGameTestTemplate(false)
public class KittyPassFourGameTests {

    /** Chicken and Rice is the top meal (+50 hunger); Deep Fried Chicken drops to +27. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testChickenAndRiceIsTheTopMeal(GameTestHelper h) {
        int rice = CatFoodHelper.getFoodValues(new ItemStack(ModItems.CHICKEN_AND_RICE.get()), null).hunger;
        int chicken = CatFoodHelper.getFoodValues(new ItemStack(ModItems.DEEP_FRIED_CHICKEN.get()), null).hunger;
        h.assertTrue(rice == 50 && chicken == 27, "chicken and rice " + rice + ", chicken " + chicken);
        h.assertTrue(CatFoodHelper.isEdibleByCat(new ItemStack(ModItems.CHICKEN_AND_RICE.get()), null), "cats eat it");
        h.succeed();
    }

    private static KittyHabitatBlockEntity habitat(GameTestHelper h, BlockPos rel, CatTrait trait) {
        h.setBlock(rel, ModBlocks.KITTY_HABITAT.get().defaultBlockState());
        BlockPos abs = h.absolutePos(rel);
        KittyHabitatBlockEntity be = (KittyHabitatBlockEntity) h.getLevel().getBlockEntity(abs);
        HabitatModifiers.loaded(h.getLevel(), abs);
        if (trait != null) {
            TalentedCat c = h.spawn(ModEntities.TALENTED_CAT.get(), rel.above());
            c.setTrait(trait);
            be.setWorker(c); // houses it
        }
        return be;
    }

    /** Shy works faster alone; a neighbour takes that away; a Brave neighbour adds the foreman bonus. */
    @GameTest(template = "arena", timeoutTicks = 20)
    public static void testHabitatTraitsAndNeighbours(GameTestHelper h) {
        KittyHabitatBlockEntity shy = habitat(h, new BlockPos(5, 2, 5), CatTrait.SHY);
        BlockPos shyPos = h.absolutePos(new BlockPos(5, 2, 5));
        h.assertTrue(shy.workerTrait() == CatTrait.SHY, "housed trait read from the stored cat");
        float alone = HabitatModifiers.compute(h.getLevel(), shyPos, shy).speed();
        h.assertTrue(Math.abs(alone - 1.2F) < 0.001F, "Shy alone should be 1.2x, was " + alone);
        habitat(h, new BlockPos(8, 2, 5), CatTrait.BRAVE); // 3 blocks away: a neighbour and a foreman
        float withBrave = HabitatModifiers.compute(h.getLevel(), shyPos, shy).speed();
        h.assertTrue(Math.abs(withBrave - 1.05F) < 0.001F, "Shy with a Brave neighbour should be 1.05x, was " + withBrave);
        KittyHabitatBlockEntity lazy = habitat(h, new BlockPos(15, 2, 15), CatTrait.LAZY);
        var mods = HabitatModifiers.compute(h.getLevel(), h.absolutePos(new BlockPos(15, 2, 15)), lazy);
        h.assertTrue(mods.hunger() == 0 && Math.abs(mods.speed() - 0.9F) < 0.001F, "Lazy: no hunger, 0.9x speed");
        h.succeed();
    }

    /** Captain: needs bond 10; promoting a second cat makes the first step down. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testCaptainNeedsBondAndIsOnePerOwner(GameTestHelper h) {
        var fp = joinFakeOwner(h, new BlockPos(3, 2, 3));
        try {
            TalentedCat a = h.spawn(ModEntities.TALENTED_CAT.get(), new BlockPos(1, 2, 1));
            TalentedCat b = h.spawn(ModEntities.TALENTED_CAT.get(), new BlockPos(2, 2, 1));
            a.tame(fp);
            b.tame(fp);
            h.assertFalse(a.promoteToCaptain(), "bond 0 can't be Captain");
            a.setBondPoints(CatBond.pointsFor(TalentedCat.CAPTAIN_BOND));
            b.setBondPoints(CatBond.pointsFor(TalentedCat.CAPTAIN_BOND));
            h.assertTrue(a.promoteToCaptain() && a.isCaptain(), "bond 10 promotes");
            h.assertTrue(b.promoteToCaptain() && b.isCaptain(), "second cat promoted");
            h.assertFalse(a.isCaptain(), "the first Captain steps down");
            b.demoteCaptain();
            h.succeed();
        } finally {
            leave(h, fp);
        }
    }

    /** Base areas: add two touching areas and merge them; a guard post puts the cat in Guard mode anchored there. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testBaseAreasAndGuardPost(GameTestHelper h) {
        var fp = joinFakeOwner(h, new BlockPos(3, 2, 3));
        try {
            BaseAreas bases = BaseAreas.get(h.getLevel().getServer());
            String dim = h.getLevel().dimension().location().toString();
            BlockPos o = h.absolutePos(BlockPos.ZERO);
            BaseAreas.Area a = bases.add(fp.getUUID(), dim, o, o.offset(4, 0, 4));
            BaseAreas.Area b = bases.add(fp.getUUID(), dim, o.offset(5, 0, 0), o.offset(8, 0, 4));
            h.assertTrue(a != null && b != null, "areas added");
            h.assertTrue(bases.merge(fp.getUUID(), a.id()) == 1, "touching areas merge");
            var merged = bases.of(fp.getUUID()).stream().filter(x -> x.id() == a.id()).findFirst().orElseThrow();
            h.assertTrue(merged.sizeX() == 9 && merged.sizeZ() == 5, "merged size " + merged.sizeX() + "x" + merged.sizeZ());
            TalentedCat c = h.spawn(ModEntities.TALENTED_CAT.get(), new BlockPos(2, 2, 2));
            c.tame(fp);
            BlockPos post = h.absolutePos(new BlockPos(4, 1, 4));
            c.setGuardPost(post);
            h.assertTrue(c.getCatMode() == CatMode.GUARD && post.equals(c.getModeAnchor()), "guard post anchors a Guard cat");
            bases.delete(fp.getUUID(), a.id());
            h.succeed();
        } finally {
            leave(h, fp);
        }
    }

    /** A Guard cat without a post in a base area patrols: its anchor moves to a border waypoint. */
    @GameTest(template = "arena", timeoutTicks = 20)
    public static void testGuardCatPatrolsItsBase(GameTestHelper h) {
        var fp = joinFakeOwner(h, new BlockPos(15, 2, 15));
        try {
            BaseAreas bases = BaseAreas.get(h.getLevel().getServer());
            String dim = h.getLevel().dimension().location().toString();
            BaseAreas.Area area = bases.add(fp.getUUID(), dim, h.absolutePos(new BlockPos(2, 1, 2)), h.absolutePos(new BlockPos(20, 3, 20)));
            TalentedCat c = h.spawn(ModEntities.TALENTED_CAT.get(), new BlockPos(10, 2, 10));
            c.tame(fp);
            c.setOrderedToSit(false);
            c.setCatMode(CatMode.GUARD);
            BasePatrol.tick(c);
            BlockPos anchor = c.getModeAnchor();
            boolean onBorder = anchor != null && (anchor.getX() == area.min().getX() + 1 || anchor.getX() == area.max().getX() - 1
                    || anchor.getZ() == area.min().getZ() + 1 || anchor.getZ() == area.max().getZ() - 1);
            h.assertTrue(onBorder, "patrol anchor should be on the border, was " + anchor);
            bases.delete(fp.getUUID(), area.id());
            h.succeed();
        } finally {
            leave(h, fp);
        }
    }

    /** Promotion needs the max level, follows the role, and a changed role is no longer promoted. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testRolePromotionNeedsMaxLevel(GameTestHelper h) {
        TalentedCat c = h.spawn(ModEntities.TALENTED_CAT.get(), new BlockPos(2, 2, 2));
        c.setCatRole(CatRole.DPS);
        h.assertFalse(c.promoteRole(), "a low-level cat can't be promoted");
        c.setCatLevel(com.kittytalents.config.KittyTalentsConfig.COMMON.maxCatLevel.get());
        h.assertTrue(c.promoteRole() && c.isPromoted(), "a max-level cat is promoted");
        c.setCatRole(CatRole.TANK);
        h.assertFalse(c.isPromoted(), "the promotion belongs to the DPS role");
        c.setCatRole(CatRole.DPS);
        h.assertTrue(c.isPromoted(), "back in DPS it is promoted again");
        h.succeed();
    }

    /** A Nekomata grows wisps over time; other cats don't. */
    @GameTest(template = "empty", timeoutTicks = 260)
    public static void testNekomataGrowsWisps(GameTestHelper h) {
        TalentedCat neko = h.spawn(ModEntities.TALENTED_CAT.get(), new BlockPos(2, 2, 2));
        neko.setMutation(CatBreedingManager.MutationType.NEKOMATA);
        TalentedCat plain = h.spawn(ModEntities.TALENTED_CAT.get(), new BlockPos(4, 2, 2));
        h.succeedWhen(() -> {
            h.assertTrue(neko.getWisps() >= 1, "the Nekomata should have a wisp");
            h.assertTrue(plain.getWisps() == 0, "a plain cat has none");
        });
    }
}
