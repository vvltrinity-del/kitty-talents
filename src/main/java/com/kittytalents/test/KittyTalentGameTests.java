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
