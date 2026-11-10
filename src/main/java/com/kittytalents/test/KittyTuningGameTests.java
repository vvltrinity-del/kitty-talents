package com.kittytalents.test;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.kittytalents.KittyTalents;
import com.kittytalents.ability.CatAbilities;
import com.kittytalents.ability.CatAbility;
import com.kittytalents.ai.goal.CatPounceGoal;
import com.kittytalents.combat.FelineStatusEffects;
import com.kittytalents.combat.FelineStatusType;
import com.kittytalents.core.tuning.Tuning;
import com.kittytalents.entity.CatMode;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.registry.ModEntities;
import com.kittytalents.talent.Talents;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Map;

/** 4.0 Phase 4: datapack tuning and the new Frost / Poison / Arcane abilities. */
@GameTestHolder(KittyTalents.MODID)
@PrefixGameTestTemplate(false)
public class KittyTuningGameTests {

    private static TalentedCat cat(GameTestHelper h, BlockPos pos) {
        TalentedCat c = h.spawn(ModEntities.TALENTED_CAT.get(), pos);
        c.setCatMode(CatMode.AGGRESSIVE);
        c.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(4.0D);
        c.setEnergy(100);
        return c;
    }

    /** A still mob that is not undead (poison works on it) and not freeze-immune. */
    private static Mob foe(GameTestHelper h, BlockPos pos) {
        Mob m = h.spawn(EntityType.VINDICATOR, pos);
        m.setNoAi(true);
        m.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        return m;
    }

    /** Runs {@code body} with the given tuning file applied (as the datapack loader would), then restores the old values. */
    private static void withTuning(String json, Runnable body) {
        Map<String, Double> before = Tuning.overrides();
        try {
            Map<ResourceLocation, JsonElement> files = Map.of(new ResourceLocation("testpack", "tuning"), JsonParser.parseString(json));
            Tuning.setOverrides(Tuning.parse(files));
            body.run();
        } finally {
            Tuning.setOverrides(before);
        }
    }

    private static String tooltip(String scope, String owner) {
        StringBuilder sb = new StringBuilder();
        for (Component c : Tuning.describeAll(scope, owner, java.util.Set.of())) sb.append(c.getString()).append('\n');
        return sb.toString();
    }

    /** With no tuning pack every number is the 3.0 value. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testTuningDefaultsMatch30(GameTestHelper h) {
        h.assertTrue(Tuning.overrides().isEmpty(), "The test world should load no tuning overrides, got " + Tuning.overrides());
        for (Tuning.Key k : Tuning.all()) {
            h.assertTrue(k.get() == k.def, k.scope + "/" + k.owner + "/" + k.name + " is " + k.get() + ", default " + k.def);
        }
        h.assertTrue(Tuning.all().size() >= 150, "Expected the talents and abilities to declare their numbers, got " + Tuning.all().size());
        h.assertTrue(Talents.HUNTER_CAT.getMaxLevel() == 5 && Talents.NINE_LIVES.getMaxLevel() == 8, "max ranks");
        CatAbility nova = CatAbilities.FROST_NOVA;
        h.assertTrue(nova.getEnergyCost() == 35 && nova.getCooldownTicks() == 90, "Frost Nova cost/cooldown");
        TalentedCat c = cat(h, new BlockPos(2, 2, 2));
        h.assertTrue(CatPounceGoal.cooldownTicks(c) == 100 && CatPounceGoal.maxRange(c) == 8.0D, "pounce numbers at rank 0");
        double armor = c.getAttributeValue(Attributes.ARMOR);
        c.setTalentLevel(Talents.CAT_ARMOR, 2);
        c.updateAttributes();
        h.assertTrue(Math.abs(c.getAttributeValue(Attributes.ARMOR) - armor - 6.0D) < 1e-6, "Cat Armor rank 2 should add 6 armor");
        h.assertTrue(TalentedCat.costOfRank(Talents.POUNCE, 6) == 3 && TalentedCat.costOfRank(Talents.HUNTER_CAT, 3) == 2, "rank costs");
        h.succeed();
    }

    /** Changing one JSON value changes behaviour and the tooltip; bad values are clamped or ignored. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testTuningPackChangesBehaviourAndTooltip(GameTestHelper h) {
        TalentedCat c = cat(h, new BlockPos(2, 2, 2));
        c.setTalentLevel(Talents.CAT_ARMOR, 2);
        c.updateAttributes();
        double armorBefore = c.getAttributeValue(Attributes.ARMOR);
        String json = """
                { "talents": { "cat_armor": { "armor_per_rank": 5 }, "hunter_cat": { "max_rank": 7, "damage_per_rank": 999999 },
                               "pounce": { "not_a_value": 3 } },
                  "abilities": { "frost_nova": { "energy_cost": 5, "cooldown_ticks": 20 } } }
                """;
        withTuning(json, () -> {
            c.updateAttributes();
            h.assertTrue(Math.abs(c.getAttributeValue(Attributes.ARMOR) - armorBefore - 4.0D) < 1e-6,
                    "armor_per_rank 5 at rank 2 should add 4 more armor than the default 3");
            h.assertTrue(tooltip(Tuning.TALENTS, "cat_armor").contains("Armor per rank: 5"), "Tooltip should show the new value:\n" + tooltip(Tuning.TALENTS, "cat_armor"));
            h.assertTrue(Talents.HUNTER_CAT.getMaxLevel() == 7, "max_rank should apply");
            h.assertTrue(Tuning.overrides().get("talents/hunter_cat/damage_per_rank") == 25.0D, "Out-of-range value should clamp to 10x default");
            h.assertFalse(Tuning.overrides().containsKey("talents/pounce/not_a_value"), "Unknown names must be ignored");
            h.assertTrue(CatAbilities.FROST_NOVA.getEnergyCost() == 5, "Ability cost should apply");
            Mob z = foe(h, new BlockPos(4, 2, 2));
            c.setTarget(z);
            c.setEnergy(6);
            h.assertTrue(CatAbilities.FROST_NOVA.tryCast(c, null), "Frost Nova should cast for 5 Energy");
        });
        h.assertTrue(CatAbilities.FROST_NOVA.getEnergyCost() == 35 && Talents.HUNTER_CAT.getMaxLevel() == 5, "Removing the pack restores defaults");
        h.succeed();
    }

    /** Lowering a talent's max rank caps cats that already have more, and respec still refunds every rank they paid for. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testLoweredMaxRankCapsAndRefunds(GameTestHelper h) {
        TalentedCat c = cat(h, new BlockPos(2, 2, 2));
        c.setTalentLevel(Talents.HUNTER_CAT, 5);
        int points = c.getSkillPoints();
        withTuning("{ \"talents\": { \"hunter_cat\": { \"max_rank\": 3 } } }", () -> {
            h.assertTrue(c.getTalentLevel(Talents.HUNTER_CAT) == 3, "Rank should be capped at the new max");
            h.assertFalse(c.canTrainTalent(Talents.HUNTER_CAT), "A capped talent can't be trained");
            int refunded = c.respecAllTalents();
            h.assertTrue(refunded == 1 + 1 + 2 + 2 + 2, "Respec should refund all 5 paid ranks, got " + refunded);
        });
        h.assertTrue(c.getSkillPoints() == points + 8, "points");
        h.succeed();
    }

    // ---------------------------------------------------------------- new abilities

    private static void assertAiWants(GameTestHelper h, TalentedCat c, CatAbility a) {
        float p = a.getAiPriority(c);
        h.assertTrue(p > 1.0f, a.getId() + ": the AI should want to cast here (priority " + p + ", needs > 1.0)");
    }

    @GameTest(template = "arena", timeoutTicks = 40)
    public static void testIceLance(GameTestHelper h) {
        TalentedCat c = cat(h, new BlockPos(4, 2, 8));
        Mob z = foe(h, new BlockPos(11, 2, 8));
        c.setTarget(z);
        CatAbility a = CatAbilities.get("ice_lance");
        assertAiWants(h, c, a);
        float hp = z.getHealth();
        h.assertTrue(a.tryCast(c, null), "Ice Lance should cast at 7 blocks");
        h.assertTrue(z.getHealth() < hp && FelineStatusEffects.hasStatus(z, FelineStatusType.FREEZE), "target hurt and frozen");
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 40)
    public static void testBlizzard(GameTestHelper h) {
        TalentedCat c = cat(h, new BlockPos(8, 2, 8));
        List<Mob> foes = List.of(foe(h, new BlockPos(10, 2, 8)), foe(h, new BlockPos(6, 2, 8)), foe(h, new BlockPos(8, 2, 12)));
        Mob villager = h.spawn(EntityType.VILLAGER, new BlockPos(8, 2, 5));
        villager.setNoAi(true);
        c.setTarget(foes.get(0));
        CatAbility a = CatAbilities.get("blizzard");
        assertAiWants(h, c, a);
        h.assertTrue(a.tryCast(c, null), "Blizzard should cast");
        for (Mob m : foes) h.assertTrue(FelineStatusEffects.hasStatus(m, FelineStatusType.FREEZE), "every foe frozen");
        h.assertFalse(FelineStatusEffects.hasStatus(villager, FelineStatusType.FREEZE), "the villager must be spared");
        h.assertTrue(FelineStatusEffects.hasStatus(c, FelineStatusType.FORTIFIED), "the cat is Fortified");
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 40)
    public static void testToxicCloud(GameTestHelper h) {
        TalentedCat c = cat(h, new BlockPos(4, 2, 8));
        Mob a1 = foe(h, new BlockPos(9, 2, 8)), a2 = foe(h, new BlockPos(10, 2, 9));
        c.setTarget(a1);
        CatAbility a = CatAbilities.get("toxic_cloud");
        assertAiWants(h, c, a);
        h.assertTrue(a.tryCast(c, null), "Toxic Cloud should cast");
        h.assertTrue(FelineStatusEffects.hasStatus(a1, FelineStatusType.POISON) && FelineStatusEffects.hasStatus(a2, FelineStatusType.POISON),
                "both foes in the cloud should be poisoned");
        // Undead don't care about poison: the AI should hold it.
        Mob zombie = h.spawn(EntityType.ZOMBIE, new BlockPos(4, 2, 13));
        zombie.setNoAi(true);
        a1.discard();
        a2.discard();
        c.setTarget(zombie);
        h.assertTrue(a.getAiPriority(c) < 1.0f, "Toxic Cloud should not be picked against undead only");
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 40)
    public static void testPlagueBloom(GameTestHelper h) {
        TalentedCat c = cat(h, new BlockPos(8, 2, 8));
        List<Mob> foes = List.of(foe(h, new BlockPos(10, 2, 8)), foe(h, new BlockPos(6, 2, 8)), foe(h, new BlockPos(8, 2, 11)));
        c.setTarget(foes.get(0));
        c.setHealth(c.getMaxHealth() * 0.5F);
        float hp = c.getHealth();
        CatAbility a = CatAbilities.get("plague_bloom");
        assertAiWants(h, c, a);
        h.assertTrue(a.tryCast(c, null), "Plague Bloom should cast");
        for (Mob m : foes) {
            h.assertTrue(FelineStatusEffects.hasStatus(m, FelineStatusType.POISON) && FelineStatusEffects.hasStatus(m, FelineStatusType.VULNERABLE), "foe afflicted");
        }
        h.assertTrue(c.getHealth() > hp, "the cat should heal from the bloom");
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 40)
    public static void testArcaneBlink(GameTestHelper h) {
        TalentedCat c = cat(h, new BlockPos(3, 2, 8));
        Mob z = foe(h, new BlockPos(11, 2, 8));
        c.setTarget(z);
        CatAbility a = CatAbilities.get("arcane_blink");
        assertAiWants(h, c, a);
        float hp = z.getHealth();
        h.assertTrue(a.tryCast(c, null), "Arcane Blink should cast");
        h.assertTrue(c.distanceTo(z) < 2.5F, "the cat should blink next to the target, distance " + c.distanceTo(z));
        h.assertTrue(z.getHealth() < hp && FelineStatusEffects.hasStatus(z, FelineStatusType.MARKED), "target hurt and Marked");
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 40)
    public static void testStarfall(GameTestHelper h) {
        TalentedCat c = cat(h, new BlockPos(8, 2, 8));
        List<Mob> foes = List.of(foe(h, new BlockPos(12, 2, 8)), foe(h, new BlockPos(4, 2, 8)), foe(h, new BlockPos(8, 2, 13)));
        c.setTarget(foes.get(0));
        CatAbility a = CatAbilities.get("starfall");
        assertAiWants(h, c, a);
        h.assertTrue(a.tryCast(c, null), "Starfall should cast");
        for (Mob m : foes) {
            h.assertTrue(m.getHealth() < m.getMaxHealth() && FelineStatusEffects.hasStatus(m, FelineStatusType.VULNERABLE), "foe struck");
        }
        h.succeed();
    }

    /** The AI really casts a new ability on its own: a cat with only Ice Lance slotted, in a fight, uses it. */
    @GameTest(template = "arena", timeoutTicks = 200)
    public static void testAiCastsNewAbility(GameTestHelper h) {
        TalentedCat c = cat(h, new BlockPos(4, 2, 8));
        c.setAbilitySlot1("ice_lance");
        c.setAbilitySlot2("");
        c.setAbilitySlot3("");
        c.setAbilityUltimate("");
        Mob z = foe(h, new BlockPos(11, 2, 8));
        c.setTarget(z);
        z.hurt(h.getLevel().damageSources().generic(), 0.5F);
        h.succeedWhen(() -> h.assertTrue(c.getAbilityCooldown("ice_lance") > 0, "The AI never cast Ice Lance"));
    }
}
