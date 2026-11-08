package com.kittytalents.test;

import com.kittytalents.KittyTalents;
import com.kittytalents.core.progression.CatProgression;
import com.kittytalents.entity.CatMode;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.registry.ModEntities;
import com.kittytalents.talent.Talent;
import com.kittytalents.talent.Talents;
import com.kittytalents.talent.effect.TalentEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Phase 4: talent hooks, masteries, XP progression and ability targeting. */
@GameTestHolder(KittyTalents.MODID)
@PrefixGameTestTemplate(false)
public class KittyTalentGameTests {

    private static TalentedCat cat(GameTestHelper h, BlockPos pos) {
        TalentedCat c = h.spawn(ModEntities.TALENTED_CAT.get(), pos);
        c.setCatMode(CatMode.STAY);
        return c;
    }

    private static void max(TalentedCat c, Talent t) {
        c.setTalentLevel(t, t.getMaxLevel());
    }

    // ---------------------------------------------------------------- XP

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testKillsGrantXpAndLevelUps(GameTestHelper h) {
        TalentedCat c = cat(h, new BlockPos(2, 2, 2));
        int level = c.getCatLevel(), points = c.getSkillPoints();
        Zombie z = h.spawn(EntityType.ZOMBIE, new BlockPos(4, 2, 2));
        int expected = CatProgression.killXp(z);
        z.hurt(c.damageSources().mobAttack(c), 1000.0F); // killed by the cat
        h.assertTrue(!z.isAlive(), "zombie should be dead");
        h.assertTrue(c.getCatXp() == expected && c.getCatLevel() == level,
                "Kill should give " + expected + " XP, have xp=" + c.getCatXp() + " level=" + c.getCatLevel());

        CatProgression.addXp(c, CatProgression.xpForNextLevel(c.getCatLevel())); // exactly one more level's worth
        h.assertTrue(c.getCatLevel() == level + 1, "Should have levelled up once, level=" + c.getCatLevel());
        h.assertTrue(c.getSkillPoints() == points + 1, "Level-up should grant one skill point");
        h.assertTrue(c.getCatXp() == expected, "Leftover XP should carry over, xp=" + c.getCatXp());
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testNoXpOrPointsBeyondLevelCap(GameTestHelper h) {
        TalentedCat c = cat(h, new BlockPos(2, 2, 2));
        int cap = com.kittytalents.config.KittyTalentsConfig.COMMON.maxCatLevel.get();
        c.setCatLevel(cap);
        int points = c.getSkillPoints();
        int gained = CatProgression.addXp(c, 1_000_000);
        h.assertTrue(gained == 0 && c.getCatLevel() == cap, "Capped cat must not level");
        h.assertTrue(c.getSkillPoints() == points && c.getCatXp() == 0, "Capped cat must not bank XP or points");
        h.succeed();
    }

    // ---------------------------------------------------------------- Mastery rules

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testMasteryNeedsMaxRankAndStandardTalentsMustBeTrained(GameTestHelper h) {
        TalentedCat c = cat(h, new BlockPos(2, 2, 2));
        h.assertFalse(TalentEngine.isMastered(c, Talents.NINE_LIVES), "Free baseline ranks must not count as mastery");
        h.assertTrue(c.getTalentLevel(Talents.NINE_LIVES) == 5, "Standard talents still start at their free rank 5");
        c.setTalentLevel(Talents.CAT_ARMOR, Talents.CAT_ARMOR.getMaxLevel() - 1);
        h.assertFalse(TalentEngine.isMastered(c, Talents.CAT_ARMOR), "One rank short is not mastery");
        max(c, Talents.CAT_ARMOR);
        max(c, Talents.NINE_LIVES);
        h.assertTrue(TalentEngine.isMastered(c, Talents.CAT_ARMOR) && TalentEngine.isMastered(c, Talents.NINE_LIVES), "Max rank = mastery");
        h.succeed();
    }

    /** The user's example: maxed Cat Armor = free Mending on worn armor. */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void testCatArmorMasteryRepairsArmor(GameTestHelper h) {
        TalentedCat master = cat(h, new BlockPos(2, 2, 2));
        TalentedCat novice = cat(h, new BlockPos(5, 2, 5));
        max(master, Talents.CAT_ARMOR);
        novice.setTalentLevel(Talents.CAT_ARMOR, 4);
        for (TalentedCat c : new TalentedCat[]{master, novice}) {
            ItemStack helm = new ItemStack(Items.IRON_HELMET);
            helm.setDamageValue(50);
            c.setArmorSlotItem(1, helm);
        }
        h.runAfterDelay(150, () -> {
            h.assertTrue(master.getArmorSlotItem(1).getDamageValue() < 50, "Mastered Cat Armor should repair the helmet");
            h.assertTrue(novice.getArmorSlotItem(1).getDamageValue() == 50, "Rank 4 must not repair");
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testNineLivesMasteryRevivesToFullHealth(GameTestHelper h) {
        TalentedCat c = cat(h, new BlockPos(2, 2, 2));
        max(c, Talents.NINE_LIVES);
        c.hurt(h.getLevel().damageSources().generic(), 10_000.0F);
        h.assertTrue(c.isAlive() && !c.isIncapacitated(), "Mastered Nine Lives should revive");
        h.assertTrue(c.getHealth() == c.getMaxHealth(), "Tenth Life revives to full health, got " + c.getHealth());
        h.assertTrue(c.hasEffect(MobEffects.DAMAGE_RESISTANCE), "Tenth Life grants Resistance");
        h.succeed();
    }

    // ---------------------------------------------------------------- Hooks

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testTalentAttributesAreIdempotent(GameTestHelper h) {
        TalentedCat c = cat(h, new BlockPos(2, 2, 2));
        c.setTalentLevel(Talents.CAT_ARMOR, 2);
        c.setTalentLevel(Talents.CAT_ARMORY, 3);
        c.setTalentLevel(Talents.FELINE_AGILITY, 4);
        c.updateAttributes();
        double armor = c.getAttributeBaseValue(Attributes.ARMOR);
        double dmg = c.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);
        double speed = c.getAttributeBaseValue(Attributes.MOVEMENT_SPEED);
        c.updateAttributes();
        c.updateAttributes();
        h.assertTrue(armor == 2.0 + 2 * 3.0, "Armor = base 2 + 3/rank, got " + armor);
        h.assertTrue(dmg == 4.0 + 3 * 2.5, "Damage = base 4 + 2.5/rank, got " + dmg);
        h.assertTrue(Math.abs(speed - (0.3 + 4 * 0.03)) < 1e-9, "Speed = base 0.3 + 0.03/rank, got " + speed);
        h.assertTrue(c.getAttributeBaseValue(Attributes.ARMOR) == armor && c.getAttributeBaseValue(Attributes.ATTACK_DAMAGE) == dmg,
                "Recomputing attributes must not stack");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testFireproofPawsBlocksFire(GameTestHelper h) {
        TalentedCat c = cat(h, new BlockPos(2, 2, 2));
        float before = c.getHealth();
        c.hurt(h.getLevel().damageSources().onFire(), 4.0F);
        h.assertTrue(c.getHealth() < before, "Without the talent fire hurts");
        c.setTalentLevel(Talents.FIREPROOF_PAWS, 1);
        c.invulnerableTime = 0;
        float beforeProtected = c.getHealth();
        c.hurt(h.getLevel().damageSources().onFire(), 4.0F);
        h.assertTrue(c.getHealth() == beforeProtected, "Fireproof Paws should block fire damage: " + beforeProtected + " -> " + c.getHealth());
        h.succeed();
    }

    /**
     * A fake player that is actually in the level, so cat.getOwner() resolves it. Call {@link #leave} when done.
     */
    static net.minecraftforge.common.util.FakePlayer joinFakeOwner(GameTestHelper h, BlockPos rel) {
        var fp = net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "kt_owner_" + System.nanoTime() % 100000));
        BlockPos at = h.absolutePos(rel);
        fp.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        h.getLevel().addNewPlayer(fp);
        return fp;
    }

    static void leave(GameTestHelper h, net.minecraft.server.level.ServerPlayer fp) {
        h.getLevel().removePlayerImmediately(fp, net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
    }

    /** User request: mastered Alchemy Kitty pulls the owner's debuffs onto the enemy it hits. */
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void testAlchemyMasteryMovesOwnerDebuffsToEnemy(GameTestHelper h) {
        var owner = joinFakeOwner(h, new BlockPos(3, 2, 2));
        try {
            TalentedCat c = cat(h, new BlockPos(2, 2, 2));
            c.tame(owner);
            h.assertTrue(c.getOwner() == owner, "Owner must resolve for this test");
            max(c, Talents.ALCHEMY_KITTY);
            owner.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.POISON, 200, 1));
            owner.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 0));
            owner.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 0)); // beneficial: must stay
            Mob pillager = h.spawn(EntityType.PILLAGER, new BlockPos(5, 2, 5));
            pillager.setNoAi(true);
            c.doHurtTarget(pillager);
            h.assertFalse(owner.hasEffect(MobEffects.POISON) || owner.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "Owner debuffs should be gone");
            h.assertTrue(owner.hasEffect(MobEffects.DAMAGE_BOOST), "Beneficial effects must stay on the owner");
            h.assertTrue(pillager.hasEffect(MobEffects.POISON) && pillager.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "Enemy should receive the debuffs");
            h.assertTrue(pillager.getEffect(MobEffects.POISON).getAmplifier() == 1, "Strength should carry over");
            h.succeed();
        } finally {
            leave(h, owner);
        }
    }

    /** Playtest regression: feeding Spirit Catnip by right-click must guarantee a Nekomata kitten. */
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void testSpiritCatnipBreedsNekomata(GameTestHelper h) {
        var owner = joinFakeOwner(h, new BlockPos(3, 2, 3));
        try {
            TalentedCat mom = cat(h, new BlockPos(2, 2, 2));
            TalentedCat dad = cat(h, new BlockPos(4, 2, 2));
            mom.tame(owner);
            dad.tame(owner);
            owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                    new ItemStack(com.kittytalents.registry.ModItems.SPIRIT_CATNIP.get()));
            mom.mobInteract(owner, net.minecraft.world.InteractionHand.MAIN_HAND);
            h.assertTrue(mom.isSpiritCatnipPrimed(), "Right-clicking with Spirit Catnip must prime the cat");
            h.assertTrue(mom.isInLove(), "Spirit Catnip should also put the cat in love");

            var kitten = mom.getBreedOffspring(h.getLevel(), dad);
            h.assertTrue(kitten instanceof TalentedCat k
                            && k.getMutation() == com.kittytalents.entity.CatBreedingManager.MutationType.NEKOMATA,
                    "Spirit Catnip offspring must be a Nekomata");
            h.assertFalse(mom.isSpiritCatnipPrimed(), "The prime is used up by breeding");
            h.succeed();
        } finally {
            leave(h, owner);
        }
    }

    /** Playtest request: each production tier is stronger than the last, Netherite strongest. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testHigherProductionTierIsStronger(GameTestHelper h) {
        var tiers = com.kittytalents.entity.ProductionTier.values();
        h.assertTrue(tiers[0] == com.kittytalents.entity.ProductionTier.STANDARD
                && tiers[1] == com.kittytalents.entity.ProductionTier.COAL
                && tiers[3] == com.kittytalents.entity.ProductionTier.IRON
                && tiers[tiers.length - 1] == com.kittytalents.entity.ProductionTier.NETHERITE, "Tiers must be in progression order");
        TalentedCat c = cat(h, new BlockPos(2, 2, 2));
        c.setCatLevel(100);
        double prevHp = -1, prevArmor = -1, prevDmg = -1;
        for (var tier : tiers) {
            c.setProductionTier(tier);
            double hp = c.getAttributeBaseValue(Attributes.MAX_HEALTH);
            double armor = c.getAttributeBaseValue(Attributes.ARMOR);
            double dmg = c.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);
            h.assertTrue(hp > prevHp && armor > prevArmor && dmg > prevDmg,
                    tier + " must beat the tier below it (hp " + hp + ", armor " + armor + ", dmg " + dmg + ")");
            prevHp = hp; prevArmor = armor; prevDmg = dmg;
        }
        h.succeed();
    }

    /** User request: a newly tamed cat can max any one non-Dire talent with its starting points, once. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testStartingPointsMaxAnyOneTalentOnce(GameTestHelper h) {
        var owner = joinFakeOwner(h, new BlockPos(3, 2, 3));
        try {
            TalentedCat c = cat(h, new BlockPos(2, 2, 2));
            int before = c.getSkillPoints();
            c.tame(owner);
            int pool = TalentedCat.startingSkillPoints();
            h.assertTrue(pool == 17, "Pool should cover an 8-rank talent (17 points), got " + pool);
            h.assertTrue(c.getSkillPoints() == before + pool, "Taming should grant the pool");
            for (Talent t : Talents.getAll()) {
                if (!t.requiresDireTreat()) h.assertTrue(TalentedCat.costToMax(t) <= pool, t.getId() + " can't be maxed with the pool");
            }
            while (c.trainTalent(Talents.POUNCE)) { }
            h.assertTrue(c.getTalentLevel(Talents.POUNCE) == Talents.POUNCE.getMaxLevel(), "The pool should max Pounce Training");

            c.tame(owner); // taming again must not grant again
            var tag = c.saveWithoutId(new net.minecraft.nbt.CompoundTag());
            tag.remove("UUID");
            TalentedCat reloaded = cat(h, new BlockPos(5, 2, 5));
            reloaded.load(tag);
            h.assertTrue(reloaded.getSkillPoints() == c.getSkillPoints() && c.getSkillPoints() == before,
                    "The pool is granted exactly once (points " + c.getSkillPoints() + ", reloaded " + reloaded.getSkillPoints() + ")");
            h.succeed();
        } finally {
            leave(h, owner);
        }
    }

    // ---------------------------------------------------------------- Abilities

    /** Frost Nova used to freeze everything around the cat, villagers included. */
    @GameTest(template = "arena", timeoutTicks = 40)
    public static void testAbilityAoeSparesVillagers(GameTestHelper h) {
        TalentedCat c = cat(h, new BlockPos(8, 2, 8));
        c.setCatMode(CatMode.AGGRESSIVE);
        Zombie z = h.spawn(EntityType.ZOMBIE, new BlockPos(10, 2, 8));
        z.setNoAi(true);
        z.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET)); // not leather: leather makes mobs freeze-immune
        Mob villager = h.spawn(EntityType.VILLAGER, new BlockPos(8, 2, 10));
        villager.setNoAi(true);
        c.setTarget(z);
        c.setEnergy(100);
        boolean cast = com.kittytalents.ability.CatAbilities.get("frost_nova").tryCast(c, null);
        h.assertTrue(cast, "Frost Nova should cast");
        var freeze = com.kittytalents.combat.FelineStatusType.FREEZE;
        h.assertTrue(com.kittytalents.combat.FelineStatusEffects.hasStatus(z, freeze), "The zombie should be frozen");
        h.assertFalse(com.kittytalents.combat.FelineStatusEffects.hasStatus(villager, freeze), "The villager must not be frozen");
        h.assertTrue(villager.getHealth() == villager.getMaxHealth(), "The villager must not be hurt");
        h.succeed();
    }
}
