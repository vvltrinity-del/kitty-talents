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

    /** Inferno Cat mastery "Phoenix": a rank-5 cat takes no fire damage of any kind (playtest report). */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testInfernoMasteryMakesCatFireImmune(GameTestHelper h) {
        TalentedCat c = h.spawn(ModEntities.TALENTED_CAT.get(), new BlockPos(2, 2, 2));
        c.setTame(true);
        c.setCatMode(CatMode.STAY);
        var ds = h.getLevel().damageSources();
        c.setTalentLevel(Talents.INFERNO_CAT, Talents.INFERNO_CAT.getMaxLevel() - 1);
        float hp = c.getHealth();
        c.hurt(ds.onFire(), 2.0F);
        h.assertTrue(c.getHealth() < hp, "Below max rank fire still hurts");
        c.setTalentLevel(Talents.INFERNO_CAT, Talents.INFERNO_CAT.getMaxLevel());
        StringBuilder hurtBy = new StringBuilder();
        for (var src : java.util.List.of(ds.onFire(), ds.inFire(), ds.lava(), ds.hotFloor())) {
            c.invulnerableTime = 0;
            float before = c.getHealth();
            c.hurt(src, 3.0F);
            if (c.getHealth() < before) hurtBy.append(src.getMsgId()).append(' ');
        }
        h.assertTrue(hurtBy.length() == 0, "Phoenix cat was hurt by: " + hurtBy);
        // Truly fire-immune: it doesn't catch fire either.
        h.assertTrue(c.fireImmune(), "Phoenix cat should be fire immune");
        c.setSecondsOnFire(5);
        h.runAfterDelay(5, () -> {
            h.assertTrue(!c.isOnFire(), "Phoenix cat should not stay on fire");
            h.succeed();
        });
    }

    /** Passive talents keep working while the cat sits (playtest: Flameward did nothing for a sitting cat). */
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void testPassiveTalentsWorkWhileSitting(GameTestHelper h) {
        var fp = joinFakeOwner(h, new BlockPos(3, 2, 2));
        TalentedCat c = ownedCat(h, fp, new BlockPos(2, 2, 2));
        c.setTalentLevel(Talents.FIREPROOF_PAWS, Talents.FIREPROOF_PAWS.getMaxLevel());
        c.setOrderedToSit(true);
        h.succeedWhen(() -> {
            h.assertTrue(c.isOrderedToSit(), "cat must stay sitting");
            h.assertTrue(fp.hasEffect(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE),
                    "Flameward should give the owner Fire Resistance while the cat sits");
            leave(h, fp);
        });
    }

    /** Playtest crash: following cats crossing a dimension ran the portal search and threw a NullPointerException. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testFollowingCatsComeAlongThroughDimensions(GameTestHelper h) {
        var fp = joinFakeOwner(h, new BlockPos(3, 2, 2));
        try {
            TalentedCat follower = ownedCat(h, fp, new BlockPos(2, 2, 2));
            follower.setOrderedToSit(false);
            follower.setCatMode(CatMode.FOLLOW);
            TalentedCat sitter = ownedCat(h, fp, new BlockPos(2, 2, 4)); // ordered to stay: must not come along
            sitter.setOrderedToSit(true);
            var nether = h.getLevel().getServer().getLevel(net.minecraft.world.level.Level.NETHER);
            h.assertTrue(nether != null, "GameTest server needs a Nether");
            var moved = com.kittytalents.event.ForgeEvents.bringFollowers(fp, h.getLevel(), nether);
            h.assertTrue(moved.size() == 1, "Exactly the following cat should travel, moved " + moved.size());
            h.assertTrue(follower.isRemoved() && sitter.isAlive() && !sitter.isRemoved(), "Follower left, sitter stayed");
            var arrived = moved.get(0);
            h.assertTrue(arrived instanceof TalentedCat && arrived.level() == nether
                    && arrived.distanceToSqr(fp.position()) < 1.0D, "Follower should arrive at the player's position in the Nether: "
                    + arrived.level().dimension().location() + " " + arrived.position());
            arrived.discard();
            h.succeed();
        } finally {
            leave(h, fp);
        }
    }

    // ---- Addon API (Phase 9): what an addon does with KittyTalentsAPI, end to end. ----
    private static com.kittytalents.talent.Talent apiTalent;
    private static final boolean[] apiCast = {false};

    private static synchronized com.kittytalents.talent.Talent apiTalent() {
        if (apiTalent == null) {
            apiTalent = com.kittytalents.api.KittyTalentsAPI.registerTalent(
                    new com.kittytalents.talent.Talent("kttest_iron_whiskers", 3, () -> new ItemStack(Items.IRON_INGOT), false, false,
                            com.kittytalents.talent.TalentCategory.DEFENSE),
                    new com.kittytalents.talent.effect.TalentEffect() {
                        @Override public float modifyIncomingDamage(com.kittytalents.talent.effect.TalentContext ctx,
                                net.minecraft.world.damagesource.DamageSource src, float amount) {
                            return amount / 2.0F; // halves all damage, any rank
                        }
                    });
            com.kittytalents.api.KittyTalentsAPI.registerAbility(new com.kittytalents.ability.CatAbility("kttest_purr_test", 0, 20) {
                @Override protected boolean execute(TalentedCat cat, net.minecraft.world.entity.player.Player player) {
                    apiCast[0] = true;
                    return true;
                }
            });
        }
        return apiTalent;
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testAddonTalentAndAbilityThroughApi(GameTestHelper h) {
        var t = apiTalent();
        var fp = joinFakeOwner(h, new BlockPos(3, 2, 2));
        try {
            TalentedCat c = ownedCat(h, fp, new BlockPos(2, 2, 2));
            h.assertTrue(com.kittytalents.talent.Talents.get(t.getId()) == t, "Addon talent should be in the registry");
            boolean threw = false;
            try {
                com.kittytalents.api.KittyTalentsAPI.registerTalent(new com.kittytalents.talent.Talent(t.getId(), 1,
                        () -> ItemStack.EMPTY), null);
            } catch (IllegalArgumentException e) { threw = true; }
            h.assertTrue(threw, "Registering a duplicate id must fail loudly");

            c.setSkillPoints(10);
            h.assertTrue(com.kittytalents.network.PacketTrainTalent.apply(fp, c.getId(), t.getId()), "Owner should train the addon talent");
            h.assertTrue(c.getTalentLevel(t) == 1, "Rank 1 after training, have " + c.getTalentLevel(t));
            float hp = c.getHealth();
            c.hurt(h.getLevel().damageSources().generic(), 4.0F);
            h.assertTrue(Math.abs((hp - c.getHealth()) - 2.0F) < 0.01F, "Addon effect should halve 4 damage to 2, took " + (hp - c.getHealth()));

            net.minecraft.nbt.CompoundTag tag = c.saveWithoutId(new net.minecraft.nbt.CompoundTag());
            tag.remove("UUID");
            TalentedCat copy = h.spawn(ModEntities.TALENTED_CAT.get(), new BlockPos(4, 2, 4));
            copy.load(tag);
            h.assertTrue(copy.getTalentLevel(t) == 1, "Addon talent rank should survive save/load");

            var ability = com.kittytalents.ability.CatAbilities.get("kttest_purr_test");
            h.assertTrue(ability != null && com.kittytalents.ability.CatAbilities.getSkills().contains(ability),
                    "Addon ability should be a selectable skill");
            apiCast[0] = false;
            ability.tryCast(c, fp);
            h.assertTrue(apiCast[0], "Addon ability should run when cast");
            h.succeed();
        } finally {
            leave(h, fp);
        }
    }

    /** Playtest: Sky Feline AI cats were flung around (glide multiplied horizontal speed every falling tick). */
    @GameTest(template = "arena", timeoutTicks = 60)
    public static void testSkyFelineGlideNeverSpeedsUp(GameTestHelper h) {
        TalentedCat c = h.spawn(ModEntities.TALENTED_CAT.get(), new BlockPos(16, 22, 16));
        c.setTame(true);
        c.setCatMode(CatMode.WANDER);
        c.setNoAi(true); // physics only
        c.setTalentLevel(Talents.SKY_FELINE, Talents.SKY_FELINE.getMaxLevel());
        c.setDeltaMovement(0.3D, -0.5D, 0.0D);
        c.fallDistance = 4.0F;
        double[] maxH = {0.3D};
        h.onEachTick(() -> maxH[0] = Math.max(maxH[0], c.getDeltaMovement().horizontalDistance()));
        h.runAfterDelay(30, () -> {
            h.assertTrue(maxH[0] <= 0.31D, "Glide must not add horizontal speed, peaked at " + maxH[0]);
            h.assertTrue(c.getDeltaMovement().y >= -0.2D || c.onGround(), "Glide should slow the fall, vy=" + c.getDeltaMovement().y);
            h.succeed();
        });
    }

    /** Playtest: Twined Sky Feline climbed slowly while Space was held, then shot off on release and untwined. */
    @GameTest(template = "arena", timeoutTicks = 100)
    public static void testTwinedSkyFelineClimbsAndGlides(GameTestHelper h) {
        var fp = joinFakeOwner(h, new BlockPos(16, 2, 18));
        TalentedCat c = ownedCat(h, fp, new BlockPos(16, 2, 16));
        c.setOrderedToSit(false);
        c.setTalentLevel(Talents.SKY_FELINE, Talents.SKY_FELINE.getMaxLevel());
        h.assertTrue(com.kittytalents.entity.TwineManager.start(fp, c), "Twine should start");
        double startY = c.getY();
        c.setTwineInput(0.0F, 1.0F); // flying forward
        c.setTwineJump(true);
        double[] peakH = {0};
        h.onEachTick(() -> peakH[0] = Math.max(peakH[0], c.getDeltaMovement().horizontalDistance()));
        h.runAfterDelay(20, () -> {
            h.assertTrue(c.getY() - startY > 5.0D, "Holding jump for 1 s should climb over 5 blocks, climbed " + (c.getY() - startY));
            c.setTwineJump(false);
        });
        h.runAfterDelay(50, () -> {
            h.assertTrue(c.isTwined(), "Still twined after releasing jump");
            h.assertTrue(!c.onGround() && c.getDeltaMovement().y > -0.15D, "Released: glide down slowly, vy=" + c.getDeltaMovement().y);
            h.assertTrue(peakH[0] < 1.2D, "Horizontal speed must stay sane, peaked at " + peakH[0]);
            com.kittytalents.entity.TwineManager.endForCat(c, null);
            leave(h, fp);
            h.succeed();
        });
    }

    // ---- 4.0 Phase 2: 3D blocks ----

    /** Every block keeps a solid hitbox in every facing (shapes are rotated from the north model). */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testBlockFacingAndShapes(GameTestHelper h) {
        var blocks = java.util.List.of(ModBlocks.CAT_FOOD_BOWL.get(), ModBlocks.CAT_BED.get(), ModBlocks.CAT_SCRATCHER.get(),
                ModBlocks.CAT_BATH.get(), ModBlocks.CAT_RICE_MILL.get(), ModBlocks.KITTY_HABITAT.get(), ModBlocks.FEEDING_STATION.get(),
                ModBlocks.KITTEN_INCUBATOR.get(), ModBlocks.KITTY_WORK_STATION.get(), ModBlocks.TREAT_OVEN.get());
        BlockPos pos = new BlockPos(3, 2, 3);
        for (var block : blocks) {
            for (var dir : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                var state = block.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, dir);
                h.setBlock(pos, state);
                var shape = h.getLevel().getBlockState(h.absolutePos(pos)).getShape(h.getLevel(), h.absolutePos(pos));
                h.assertTrue(!shape.isEmpty(), block + " facing " + dir + " has no hitbox");
            }
        }
        h.succeed();
    }

    /** The bowl's model shows how full it is. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testFoodBowlShowsFillLevel(GameTestHelper h) {
        BlockPos pos = new BlockPos(3, 2, 3);
        h.setBlock(pos, ModBlocks.CAT_FOOD_BOWL.get());
        CatFoodBowlBlockEntity bowl = (CatFoodBowlBlockEntity) h.getBlockEntity(pos);
        java.util.function.IntSupplier fill = () -> h.getLevel().getBlockState(h.absolutePos(pos)).getValue(com.kittytalents.block.CatFoodBowlBlock.FOOD);
        h.assertTrue(fill.getAsInt() == 0, "Empty bowl shows empty");
        bowl.getInventory().insertItem(0, new ItemStack(Items.COOKED_COD, 4), false);
        h.assertTrue(fill.getAsInt() == 1, "A little food shows level 1, got " + fill.getAsInt());
        bowl.getInventory().insertItem(1, new ItemStack(Items.COOKED_SALMON, 60), false);
        h.assertTrue(fill.getAsInt() == 3, "A lot of food shows level 3, got " + fill.getAsInt());
        bowl.getInventory().extractItem(0, 64, false);
        bowl.getInventory().extractItem(1, 64, false);
        h.assertTrue(fill.getAsInt() == 0, "Emptied bowl shows empty again");
        h.succeed();
    }

    /** Cat Tree: placed and broken as a pair, drops one item. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testCatTreeIsTwoBlocksTall(GameTestHelper h) {
        BlockPos lower = new BlockPos(3, 2, 3);
        var tree = ModBlocks.CAT_TREE.get().defaultBlockState();
        var HALF = com.kittytalents.block.CatTreeBlock.HALF;
        h.setBlock(lower, tree);
        h.setBlock(lower.above(), tree.setValue(HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER));
        h.assertBlockPresent(ModBlocks.CAT_TREE.get(), lower.above());
        h.getLevel().destroyBlock(h.absolutePos(lower), true);
        h.assertBlockNotPresent(ModBlocks.CAT_TREE.get(), lower.above());
        var dropped = h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(h.absolutePos(lower)).inflate(3), e -> e.getItem().is(ModItems.CAT_TREE.get()));
        int drops = dropped.stream().mapToInt(e -> e.getItem().getCount()).sum();
        dropped.forEach(net.minecraft.world.entity.Entity::discard); // FETCH cats in neighbouring tests chase loose items
        h.assertTrue(drops == 1, "Breaking the tree should drop exactly one item, dropped " + drops);
        h.succeed();
    }

    /** A wandering cat finds a Cat Tree, hops onto the perch and keeps watch (hostiles glow). */
    @GameTest(template = "arena", timeoutTicks = 400)
    public static void testCatPerchesOnCatTreeAndKeepsWatch(GameTestHelper h) {
        com.kittytalents.ai.goal.CatPerchGoal.setSearchChanceForTests(1);
        var fp = joinFakeOwner(h, new BlockPos(20, 2, 20));
        BlockPos lower = new BlockPos(16, 2, 16);
        var tree = ModBlocks.CAT_TREE.get().defaultBlockState();
        h.setBlock(lower, tree);
        h.setBlock(lower.above(), tree.setValue(com.kittytalents.block.CatTreeBlock.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER));
        TalentedCat c = ownedCat(h, fp, new BlockPos(10, 2, 16));
        c.setOrderedToSit(false);
        c.setCatMode(CatMode.WANDER);
        Zombie z = h.spawn(EntityType.ZOMBIE, new BlockPos(24, 2, 8));
        z.setNoAi(true);
        net.minecraft.world.phys.Vec3 perch = com.kittytalents.block.CatTreeBlock.perchOf(tree, h.absolutePos(lower));
        h.succeedWhen(() -> {
            h.assertTrue(c.distanceToSqr(perch) < 0.5D, "Cat should be on the perch, distance " + Math.sqrt(c.distanceToSqr(perch)));
            h.assertTrue(z.hasEffect(net.minecraft.world.effect.MobEffects.GLOWING), "A perched cat makes nearby hostiles glow");
            com.kittytalents.ai.goal.CatPerchGoal.setSearchChanceForTests(240);
            leave(h, fp);
        });
    }
}
