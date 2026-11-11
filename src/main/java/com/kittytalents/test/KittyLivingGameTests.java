package com.kittytalents.test;

import com.kittytalents.KittyTalents;
import com.kittytalents.ai.goal.CatRetreatGoal;
import com.kittytalents.ai.goal.CatTraitGoal;
import com.kittytalents.core.living.CatBond;
import com.kittytalents.core.living.CatTrait;
import com.kittytalents.core.living.GrowthStage;
import com.kittytalents.core.progression.CatProgression;
import com.kittytalents.entity.CatMode;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.entity.interact.CatInteractions;
import com.kittytalents.registry.ModEntities;
import com.kittytalents.registry.ModItems;
import com.kittytalents.talent.Talents;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static com.kittytalents.test.KittyTalentGameTests.joinFakeOwner;
import static com.kittytalents.test.KittyTalentGameTests.leave;

/** 4.0 Phase 6 "Living cats": kitten growth, personalities and bond. */
@GameTestHolder(KittyTalents.MODID)
@PrefixGameTestTemplate(false)
public class KittyLivingGameTests {

    private static TalentedCat cat(GameTestHelper h, BlockPos pos, CatTrait trait) {
        TalentedCat c = h.spawn(ModEntities.TALENTED_CAT.get(), pos);
        c.setCatMode(CatMode.FOLLOW);
        c.setTrait(trait);
        c.updateAttributes();
        return c;
    }

    private static CatTraitGoal habit(TalentedCat c) {
        for (var w : c.goalSelector.getAvailableGoals()) {
            if (w.getGoal() instanceof CatTraitGoal g) {
                g.setChanceForTests(1.0F);
                return g;
            }
        }
        throw new IllegalStateException("no CatTraitGoal on the cat");
    }

    private static void hold(FakePlayer p, ItemStack stack) {
        p.setItemInHand(InteractionHand.MAIN_HAND, stack);
    }

    // ---------------------------------------------------------------- growth

    /** Kittens pass through three small stages, food and treats speed growth up, and they end as full adults. */
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void testKittenGrowsThroughStages(GameTestHelper h) {
        h.assertTrue(GrowthStage.ofAge(-24000) == GrowthStage.NEWBORN && GrowthStage.ofAge(-12000) == GrowthStage.KITTEN
                && GrowthStage.ofAge(-4000) == GrowthStage.JUNIOR && GrowthStage.ofAge(0) == GrowthStage.ADULT, "stage thresholds");
        FakePlayer owner = joinFakeOwner(h, new BlockPos(3, 2, 2));
        TalentedCat adult = cat(h, new BlockPos(1, 2, 1), CatTrait.NONE);
        TalentedCat c = cat(h, new BlockPos(2, 2, 2), CatTrait.NONE);
        c.tame(owner);
        c.setAge(GrowthStage.BABY_AGE);
        h.runAfterDelay(25, () -> {
            try {
                h.assertTrue(c.getGrowthStage() == GrowthStage.NEWBORN, "a newborn should be synced as NEWBORN, got " + c.getGrowthStage());
                h.assertTrue(c.getScale() < 0.5F && c.getBbHeight() < adult.getBbHeight() * 0.5F, "newborns are small (scale " + c.getScale() + ")");
                h.assertTrue(c.getMaxHealth() < adult.getMaxHealth(), "kittens are weaker than adults");
                // Fish: 10% of what is left. Treat: 25%.
                c.setAge(GrowthStage.BABY_AGE); // it aged a little while we waited
                hold(owner, new ItemStack(Items.COD, 4));
                CatInteractions.handle(c, owner, InteractionHand.MAIN_HAND);
                h.assertTrue(c.getAge() == -21600, "one fish should skip 10% of the growing time, age " + c.getAge());
                h.assertTrue(c.feedGrowth(true) && c.getAge() == -16200, "a treat should skip 25%, age " + c.getAge());
                c.setAge(-9000);
                h.assertTrue(c.feedGrowth(false) && c.getGrowthStage() == GrowthStage.KITTEN, "KITTEN stage after feeding, got " + c.getGrowthStage());
                c.setAge(-1);
                c.feedGrowth(true);
                h.assertTrue(c.getAge() == 0 && c.getGrowthStage() == GrowthStage.ADULT && !c.isBaby(), "fully grown");
                h.assertTrue(Math.abs(c.getMaxHealth() - adult.getMaxHealth()) < 1e-4 && c.getScale() == 1.0F, "adult stats and size");
                h.assertFalse(c.feedGrowth(true), "adults don't grow");
                h.succeed();
            } finally {
                leave(h, owner);
            }
        });
    }

    /** A kitten gets a parent's personality and rank 1 in one talent a parent trained. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testKittenInheritsTraitAndATalent(GameTestHelper h) {
        TalentedCat mum = cat(h, new BlockPos(2, 2, 2), CatTrait.BRAVE);
        TalentedCat dad = cat(h, new BlockPos(3, 2, 2), CatTrait.BRAVE);
        mum.setTalentLevel(Talents.HUNTER_CAT, 4);
        dad.setTalentLevel(Talents.HUNTER_CAT, 2);
        int brave = 0;
        for (int i = 0; i < 40; i++) {
            TalentedCat kit = (TalentedCat) mum.getBreedOffspring(h.getLevel(), dad);
            h.assertTrue(kit != null && kit.getTrait() != CatTrait.NONE, "every kitten has a personality");
            h.assertTrue(kit.getTalentLevel(Talents.HUNTER_CAT) == 1, "kitten should inherit rank 1 of the parents' talent");
            h.assertTrue(kit.getGrowthStage() == GrowthStage.NEWBORN, "kittens are born as newborns");
            if (kit.getTrait() == CatTrait.BRAVE) brave++;
            kit.discard();
        }
        h.assertTrue(brave >= 24, "about 80% (+ lucky rolls) of kittens should take the parents' trait, got " + brave + "/40");
        h.succeed();
    }

    /** A cat saved before 4.0 (no Trait / Bond keys) loads as an adult with a rolled personality and zero bond. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testOldSaveGetsTraitAndZeroBond(GameTestHelper h) {
        TalentedCat source = cat(h, new BlockPos(2, 2, 2), CatTrait.NONE);
        var tag = source.saveWithoutId(new net.minecraft.nbt.CompoundTag());
        h.assertTrue("none".equals(tag.getString("Trait")) && tag.contains("Bond"), "4.0 saves carry Trait and Bond");
        tag.remove("Trait");
        tag.remove("Bond");
        tag.remove("UUID");
        TalentedCat loaded = h.spawn(ModEntities.TALENTED_CAT.get(), new BlockPos(4, 2, 4));
        loaded.load(tag);
        h.assertTrue(loaded.getTrait() != CatTrait.NONE, "a 3.0 cat should get a rolled personality");
        h.assertTrue(loaded.getBondPoints() == 0 && loaded.getBondLevel() == 0, "bond starts at zero");
        h.assertTrue(loaded.getGrowthStage() == GrowthStage.ADULT && !loaded.isBaby(), "3.0 cats are adults");
        // and it is kept, not re-rolled, on the next save and load
        CatTrait rolled = loaded.getTrait();
        loaded.setBondPoints(80);
        var again = loaded.saveWithoutId(new net.minecraft.nbt.CompoundTag());
        again.remove("UUID");
        TalentedCat third = h.spawn(ModEntities.TALENTED_CAT.get(), new BlockPos(1, 2, 4));
        third.load(again);
        h.assertTrue(third.getTrait() == rolled && third.getBondPoints() == 80, "trait and bond survive save and load");
        h.succeed();
    }

    // ---------------------------------------------------------------- personalities: bonus and quirk numbers

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testTraitBonusesAndQuirks(GameTestHelper h) {
        TalentedCat plain = cat(h, new BlockPos(1, 2, 1), CatTrait.NONE);
        double dmg = plain.getAttributeValue(Attributes.ATTACK_DAMAGE), speed = plain.getAttributeValue(Attributes.MOVEMENT_SPEED);

        TalentedCat brave = cat(h, new BlockPos(2, 2, 1), CatTrait.BRAVE);
        h.assertTrue(Math.abs(brave.getAttributeValue(Attributes.ATTACK_DAMAGE) - dmg * 1.10) < 1e-6, "Brave: +10% damage");
        // Brave never retreats; an ordinary cat in the same spot does.
        Zombie z = h.spawn(EntityType.ZOMBIE, new BlockPos(4, 2, 4));
        z.setNoAi(true);
        for (TalentedCat c : new TalentedCat[]{plain, brave}) {
            c.setHealth(c.getMaxHealth() * 0.1F);
            c.setTarget(z);
        }
        h.assertTrue(new CatRetreatGoal(plain).canUse(), "a hurt ordinary cat should want to retreat");
        h.assertFalse(new CatRetreatGoal(brave).canUse(), "a Brave cat must not retreat");

        TalentedCat lazy = cat(h, new BlockPos(3, 2, 1), CatTrait.LAZY);
        h.assertTrue(Math.abs(lazy.getAttributeValue(Attributes.MOVEMENT_SPEED) - speed * 0.90) < 1e-6, "Lazy: 10% slower");

        TalentedCat curious = cat(h, new BlockPos(1, 2, 3), CatTrait.CURIOUS);
        int need = CatProgression.xpForNextLevel(plain.getCatLevel());
        int give = Math.max(1, Math.min(20, need / 3));
        CatProgression.addXp(plain, give);
        CatProgression.addXp(curious, give);
        h.assertTrue(curious.getCatXp() == Math.round(give * 1.15) && plain.getCatXp() == give,
                "Curious: +15% XP (" + plain.getCatXp() + " vs " + curious.getCatXp() + ")");

        TalentedCat shy = cat(h, new BlockPos(2, 2, 3), CatTrait.SHY);
        TalentedCat target = cat(h, new BlockPos(3, 2, 3), CatTrait.NONE);
        var arrowAt = h.getLevel().damageSources().arrow(new net.minecraft.world.entity.projectile.Arrow(h.getLevel(), 0, 0, 0), z);
        float a = target.getHealth(), b = shy.getHealth();
        target.hurt(arrowAt, 4.0F);
        shy.hurt(arrowAt, 4.0F);
        float lostPlain = a - target.getHealth(), lostShy = b - shy.getHealth();
        h.assertTrue(lostPlain > 0 && Math.abs(lostShy - lostPlain * 0.85F) < 0.05F, "Shy: 15% less projectile damage (" + lostPlain + " vs " + lostShy + ")");

        TalentedCat greedy = cat(h, new BlockPos(1, 2, 5), CatTrait.GREEDY);
        h.assertTrue(greedy.foodValue(25) == 38 && plain.foodValue(25) == 25, "Greedy: food restores 50% more");
        h.succeed();
    }

    /** Hunger and Energy over time: Greedy and Playful get hungry faster; Playful recovers Energy faster; Lazy heals at rest. */
    @GameTest(template = "empty", timeoutTicks = 500)
    public static void testTraitRatesOverTime(GameTestHelper h) {
        int rate = com.kittytalents.config.KittyTalentsConfig.COMMON.hungerTickRate.get();
        TalentedCat plain = cat(h, new BlockPos(1, 2, 1), CatTrait.NONE);
        TalentedCat playful = cat(h, new BlockPos(3, 2, 1), CatTrait.PLAYFUL);
        TalentedCat lazy = cat(h, new BlockPos(1, 2, 3), CatTrait.LAZY);
        TalentedCat plainRest = cat(h, new BlockPos(3, 2, 3), CatTrait.NONE);
        for (TalentedCat c : new TalentedCat[]{plain, playful}) {
            c.setCatMode(CatMode.STAY);
            c.setEnergy(0);
        }
        for (TalentedCat c : new TalentedCat[]{lazy, plainRest}) {
            c.setOrderedToSit(true);
            c.setHealth(c.getMaxHealth() - 10.0F);
        }
        float restStart = plainRest.getHealth();
        h.runAfterDelay(110, () -> {
            h.assertTrue(playful.getEnergy() > plain.getEnergy() || plain.getEnergy() >= plain.getMaxEnergy(),
                    "Playful should recover Energy faster (" + plain.getEnergy() + " vs " + playful.getEnergy() + ")");
        });
        h.runAfterDelay(260, () -> {
            float lazyGain = lazy.getHealth() - (lazy.getMaxHealth() - 10.0F), plainGain = plainRest.getHealth() - restStart;
            h.assertTrue(lazyGain >= 1.0F && lazyGain > plainGain, "Lazy should heal while resting (lazy +" + lazyGain + ", ordinary +" + plainGain + ")");
            h.succeed();
        });
        h.assertTrue(rate > 0, "hunger rate configured");
    }

    // ---------------------------------------------------------------- personalities: visible habits

    @GameTest(template = "arena", timeoutTicks = 200)
    public static void testLazyCatNapsAndPlayfulCatHops(GameTestHelper h) {
        TalentedCat lazy = cat(h, new BlockPos(4, 2, 4), CatTrait.LAZY);
        TalentedCat playful = cat(h, new BlockPos(12, 2, 12), CatTrait.PLAYFUL);
        CatTraitGoal nap = habit(lazy), hop = habit(playful);
        double groundY = playful.getY();
        boolean[] seen = new boolean[2];
        h.onEachTick(() -> {
            if (nap.acting() == CatTrait.LAZY && lazy.isLying()) seen[0] = true;
            if (hop.acting() == CatTrait.PLAYFUL && playful.getY() > groundY + 0.2D) seen[1] = true;
        });
        h.succeedWhen(() -> h.assertTrue(seen[0] && seen[1], "lazy napped: " + seen[0] + ", playful hopped: " + seen[1]));
    }

    @GameTest(template = "arena", timeoutTicks = 300)
    public static void testCuriousCatInspectsDroppedItem(GameTestHelper h) {
        TalentedCat c = cat(h, new BlockPos(4, 2, 8), CatTrait.CURIOUS);
        BlockPos at = h.absolutePos(new BlockPos(10, 2, 8));
        ItemEntity item = new ItemEntity(h.getLevel(), at.getX() + 0.5, at.getY(), at.getZ() + 0.5, new ItemStack(Items.FEATHER));
        item.setNeverPickUp();
        item.setDeltaMovement(0, 0, 0);
        h.getLevel().addFreshEntity(item);
        habit(c);
        h.succeedWhen(() -> h.assertTrue(c.distanceTo(item) < 2.0F, "the Curious cat should walk to the item, distance " + c.distanceTo(item)));
    }

    @GameTest(template = "arena", timeoutTicks = 300)
    public static void testBraveCatStaresDownMonster(GameTestHelper h) {
        TalentedCat c = cat(h, new BlockPos(4, 2, 8), CatTrait.BRAVE);
        c.setCatMode(CatMode.DOCILE); // will not fight: only the habit can make it face the zombie
        c.setOrderedToSit(false);
        Zombie z = h.spawn(EntityType.ZOMBIE, new BlockPos(10, 2, 8));
        z.setNoAi(true);
        CatTraitGoal g = habit(c);
        h.succeedWhen(() -> h.assertTrue(g.acting() == CatTrait.BRAVE, "the Brave cat should stand and stare at the monster"));
    }

    @GameTest(template = "arena", timeoutTicks = 300)
    public static void testShyCatBacksAwayAndGreedyCatBegs(GameTestHelper h) {
        FakePlayer owner = joinFakeOwner(h, new BlockPos(13, 2, 13));
        FakePlayer stranger = joinFakeOwner(h, new BlockPos(4, 2, 4));
        TalentedCat shy = cat(h, new BlockPos(5, 2, 5), CatTrait.SHY);
        TalentedCat greedy = cat(h, new BlockPos(6, 2, 13), CatTrait.GREEDY);
        shy.setCatMode(CatMode.WANDER);
        greedy.tame(owner);
        greedy.setCatMode(CatMode.WANDER);
        hold(owner, new ItemStack(Items.COD));
        float shyStart = shy.distanceTo(stranger);
        boolean[] done = new boolean[1];
        h.onEachTick(() -> {
            if (!done[0] && shy.distanceTo(stranger) > Math.max(5.0F, shyStart + 2.5F) && greedy.distanceTo(owner) < 3.0F) {
                done[0] = true;
                leave(h, owner);
                leave(h, stranger);
                h.succeed();
            }
        });
        h.runAfterDelay(295, () -> {
            if (done[0]) return;
            float s = shy.distanceTo(stranger), g = greedy.distanceTo(owner);
            leave(h, owner);
            leave(h, stranger);
            h.fail("shy cat is " + s + " from the stranger (started " + shyStart + "), greedy cat is " + g + " from its owner with food");
        });
    }

    // ---------------------------------------------------------------- bond

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testBondLevelsGainsAndPerks(GameTestHelper h) {
        h.assertTrue(CatBond.pointsFor(1) == 25 && CatBond.pointsFor(3) == 150 && CatBond.pointsFor(10) == 1375, "level thresholds");
        h.assertTrue(CatBond.levelFor(24) == 0 && CatBond.levelFor(25) == 1 && CatBond.levelFor(149) == 2 && CatBond.levelFor(99999) == 10, "levelFor");
        FakePlayer owner = joinFakeOwner(h, new BlockPos(3, 2, 2));
        try {
            TalentedCat c = cat(h, new BlockPos(2, 2, 2), CatTrait.NONE);
            c.tame(owner);
            h.assertTrue(c.getBondPoints() == 0, "starts at zero");

            // Petting: points once, then a cooldown (no farming).
            owner.setShiftKeyDown(true);
            hold(owner, ItemStack.EMPTY);
            CatInteractions.handle(c, owner, InteractionHand.MAIN_HAND);
            CatInteractions.handle(c, owner, InteractionHand.MAIN_HAND);
            owner.setShiftKeyDown(false);
            h.assertTrue(c.getBondPoints() == 2, "petting gives 2 points once per cooldown, got " + c.getBondPoints());
            // Feeding
            c.setHunger(40);
            hold(owner, new ItemStack(Items.COD, 2));
            CatInteractions.handle(c, owner, InteractionHand.MAIN_HAND);
            h.assertTrue(c.getBondPoints() == 5, "feeding gives 3, got " + c.getBondPoints());

            // Play needs bond level 3.
            hold(owner, new ItemStack(Items.STRING));
            c.setEnergy(0);
            CatInteractions.handle(c, owner, InteractionHand.MAIN_HAND);
            h.assertTrue(c.getEnergy() == 0 && c.getBondPoints() == 5, "no play below bond level 3");
            c.setBondPoints(CatBond.pointsFor(3));
            CatInteractions.handle(c, owner, InteractionHand.MAIN_HAND);
            h.assertTrue(c.getEnergy() == 20 && c.getBondPoints() == CatBond.pointsFor(3) + 4, "play restores Energy and adds bond");
            h.assertTrue(owner.getMainHandItem().getCount() == 1, "the String is not used up");

            // Perks
            double hp = c.getAttributeBaseValue(Attributes.MAX_HEALTH), dmg = c.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);
            c.setBondPoints(CatBond.pointsFor(7));
            h.assertTrue(c.hasBondPerk(CatBond.Perk.KINDRED) && c.getAttributeBaseValue(Attributes.MAX_HEALTH) == hp + 2
                    && c.getAttributeBaseValue(Attributes.ATTACK_DAMAGE) == dmg + 1, "Kindred: +2 health, +1 damage");
            int need = CatProgression.xpForNextLevel(c.getCatLevel());
            int give = Math.max(1, Math.min(20, need / 3));
            int before = c.getCatXp();
            CatProgression.addXp(c, give);
            h.assertTrue(c.getCatXp() - before == Math.round(give * 1.05), "Quick Learner: +5% XP");
            h.assertTrue(c.bondDamageMultiplier() == 1.0F, "no Soulmates before level 10");
            c.setBondPoints(CatBond.pointsFor(10));
            h.assertTrue(Math.abs(c.bondDamageMultiplier() - 1.10F) < 1e-6, "Soulmates near the owner");
            c.setBondPoints(999999);
            h.assertTrue(c.getBondPoints() == CatBond.pointsFor(10), "bond is capped");
            h.succeed();
        } finally {
            leave(h, owner);
        }
    }

    /** Greedy and Playful cats get hungry faster: fewer ticks per hunger point. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testTraitHungerRates(GameTestHelper h) {
        TalentedCat plain = cat(h, new BlockPos(1, 2, 1), CatTrait.NONE);
        TalentedCat greedy = cat(h, new BlockPos(2, 2, 1), CatTrait.GREEDY);
        TalentedCat playful = cat(h, new BlockPos(3, 2, 1), CatTrait.PLAYFUL);
        h.assertTrue(plain.traitHungerRate(1200) == 1200, "ordinary cats keep the configured rate");
        h.assertTrue(greedy.traitHungerRate(1200) == 960, "Greedy: 25% faster, got " + greedy.traitHungerRate(1200));
        h.assertTrue(playful.traitHungerRate(1200) == 1091, "Playful: 10% faster, got " + playful.traitHungerRate(1200));
        h.succeed();
    }

    /** Bond grows by itself: a kill next to the owner, and a minute spent near the owner. Not when the owner is away. */
    @GameTest(template = "arena", timeoutTicks = 1400)
    public static void testBondFromKillsAndTimeTogether(GameTestHelper h) {
        FakePlayer owner = joinFakeOwner(h, new BlockPos(4, 2, 4));
        FakePlayer away = joinFakeOwner(h, new BlockPos(4, 2, 4));
        BlockPos far = h.absolutePos(new BlockPos(4, 2, 4)).offset(200, 0, 0);
        away.moveTo(far.getX(), far.getY(), far.getZ());
        TalentedCat near = cat(h, new BlockPos(5, 2, 5), CatTrait.NONE);
        TalentedCat lonely = cat(h, new BlockPos(7, 2, 7), CatTrait.NONE);
        near.tame(owner);
        lonely.tame(away);
        for (TalentedCat c : new TalentedCat[]{near, lonely}) c.setCatMode(CatMode.STAY);
        Zombie z = h.spawn(EntityType.ZOMBIE, new BlockPos(8, 2, 5));
        z.setNoAi(true);
        z.hurt(h.getLevel().damageSources().mobAttack(near), 1000.0F);
        h.assertTrue(!z.isAlive() && near.getBondPoints() == 2, "a kill next to the owner gives 2 bond, got " + near.getBondPoints());
        h.runAfterDelay(1320, () -> {
            int n = near.getBondPoints(), l = lonely.getBondPoints();
            leave(h, owner);
            leave(h, away);
            h.assertTrue(n >= 3, "a minute near the owner should add bond, got " + n);
            h.assertTrue(l == 0, "no bond while the owner is far away, got " + l);
            h.succeed();
        });
    }
}
