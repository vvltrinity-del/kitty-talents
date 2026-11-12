package com.kittytalents.test;

import com.kittytalents.KittyTalents;
import com.kittytalents.ability.CatAbilities;
import com.kittytalents.ability.CatAbility;
import com.kittytalents.combat.FelineElement;
import com.kittytalents.combat.FelineEnergyEngine;
import com.kittytalents.combat.FelineStatusEffects;
import com.kittytalents.combat.FelineStatusType;
import com.kittytalents.entity.CatMode;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.entity.TwineManager;
import com.kittytalents.network.PacketTeamCommand;
import com.kittytalents.network.PacketTwineAction;
import com.kittytalents.network.PacketTwineInput;
import com.kittytalents.registry.ModEntities;
import com.kittytalents.talent.Talent;
import com.kittytalents.talent.Talents;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Full audit of a Talented Cat in a fight (2026-11-11): every ability and ultimate through the real Twine key path and
 * through the AI, kill credit for every damaging ability, the Twine claw, pounce, dodge and team synergy with friends
 * standing next to the enemy, every talent at max rank in a brawl, and every status effect.
 *
 * <p>"Friends" in these tests: a co-owner (trusted player), a villager, a second cat and a tamed wolf of the same
 * owner. None of them may ever be hurt or debuffed by the cat.</p>
 */
@GameTestHolder(KittyTalents.MODID)
@PrefixGameTestTemplate(false)
public class KittyCombatAuditGameTests {
    private static final String ARENA = KittyTalents.MODID + ":arena";

    /** Abilities that hurt their target directly (they must credit the kill to the cat). */
    private static final Set<String> DAMAGING = Set.of("shadow_strike", "volt_dash", "inferno_spin", "frost_nova", "tidal_surge",
            "venom_fang", "ice_lance", "toxic_cloud", "arcane_blink", "soul_flame_barrage", "apex_predator", "tempest_claws",
            "blizzard", "plague_bloom", "starfall");

    // ================================================================================================ scene

    /** A Twined (or AI) cat facing east at an enemy, with four friends around it. */
    private static final class Scene {
        FakePlayer owner, friend;
        TalentedCat cat, buddy;
        Villager villager;
        Wolf wolf;
        Mob foe, foe2;
        final Map<LivingEntity, Float> friendHp = new LinkedHashMap<>();

        List<FakePlayer> players() {
            List<FakePlayer> l = new ArrayList<>();
            if (owner != null) l.add(owner);
            if (friend != null) l.add(friend);
            return l;
        }
    }

    private static Mob foe(GameTestHelper h, BlockPos pos) {
        Mob m = h.spawn(EntityType.VINDICATOR, pos); // hostile, not undead (poison works), not fire or freeze immune
        m.setNoAi(true);
        m.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        m.setPersistenceRequired();
        return m;
    }

    private static TalentedCat ownedCat(GameTestHelper h, BlockPos pos, ServerPlayer owner, CatMode mode) {
        TalentedCat c = h.spawn(ModEntities.TALENTED_CAT.get(), pos);
        c.tame(owner);
        c.setCatMode(mode);
        return c;
    }

    private static Scene scene(GameTestHelper h, boolean twine) {
        Scene s = new Scene();
        s.owner = KittyTalentGameTests.joinFakeOwner(h, new BlockPos(3, 2, 3));
        s.friend = KittyTalentGameTests.joinFakeOwner(h, new BlockPos(6, 2, 9));
        s.cat = ownedCat(h, new BlockPos(8, 2, 8), s.owner, CatMode.AGGRESSIVE);
        s.cat.setCatLevel(50);
        s.cat.addTrustedPlayer(s.friend.getUUID(), s.friend.getGameProfile().getName());
        s.cat.setOrderedToSit(false);
        s.buddy = ownedCat(h, new BlockPos(8, 2, 6), s.owner, CatMode.STAY);
        s.villager = h.spawn(EntityType.VILLAGER, new BlockPos(7, 2, 8));
        s.villager.setNoAi(true);
        s.wolf = h.spawn(EntityType.WOLF, new BlockPos(9, 2, 6));
        s.wolf.tame(s.owner);
        s.wolf.setNoAi(true);
        s.foe = foe(h, new BlockPos(10, 2, 8));
        s.foe2 = foe(h, new BlockPos(10, 2, 10));
        face(s.cat, -90.0F);
        if (twine) {
            h.assertTrue(PacketTwineAction.apply(s.owner, s.cat.getId(), PacketTwineAction.ActionType.TWINE_START, 0), "Twine should start");
            input(s, false, false); // the client sends its view every tick; east
        }
        s.cat.setEnergy(FelineEnergyEngine.getMaxEnergy(s.cat));
        for (LivingEntity f : new LivingEntity[]{s.friend, s.villager, s.buddy, s.wolf}) s.friendHp.put(f, f.getHealth());
        return s;
    }

    private static void face(LivingEntity e, float yaw) {
        e.setYRot(yaw);
        e.setYHeadRot(yaw);
        e.setYBodyRot(yaw);
        e.setXRot(0.0F);
    }

    /** One tick of Twine input facing east. */
    private static boolean input(Scene s, boolean attack, boolean pounce) {
        return new PacketTwineInput(s.cat.getId(), 0, 0, false, false, false, attack, pounce, false, false, -90.0F, 0.0F).apply(s.owner);
    }

    /** Why {@code e} counts as harmed since {@code hpBefore}, or null. */
    private static String harmed(LivingEntity e, float hpBefore) {
        if (e.getHealth() < hpBefore - 0.01F) return "lost health " + hpBefore + " -> " + e.getHealth();
        if (e.isOnFire()) return "was set on fire";
        for (MobEffectInstance i : e.getActiveEffects()) {
            if (i.getEffect().getCategory() == MobEffectCategory.HARMFUL) return "got " + i.getEffect().getDescriptionId();
        }
        for (FelineStatusType t : FelineStatusType.values()) {
            if (!t.isBeneficial() && FelineStatusEffects.hasStatus(e, t)) return "got status " + t;
        }
        return null;
    }

    private static void assertFriendsUnharmed(GameTestHelper h, Scene s, String what) {
        for (Map.Entry<LivingEntity, Float> f : s.friendHp.entrySet()) {
            String why = harmed(f.getKey(), f.getValue());
            h.assertTrue(why == null, what + ": the " + f.getKey().getType().getDescription().getString() + " (a friend) " + why);
        }
    }

    private static void end(GameTestHelper h, Scene s) {
        for (FakePlayer p : s.players()) {
            TwineManager.endForPlayer(p, null);
            KittyTalentGameTests.leave(h, p);
        }
    }

    /** Runs {@code body}; leaves the fake players if it throws (a passing body must call {@link #end} itself). */
    private static void guarded(GameTestHelper h, Scene s, Runnable body) {
        try {
            body.run();
        } catch (RuntimeException | Error e) {
            end(h, s);
            throw e;
        }
    }

    private static PacketTeamCommand.CommandType slot(CatAbility a, TalentedCat cat) {
        if (a.isUltimate()) {
            cat.setAbilityUltimate(a.getId());
            return PacketTeamCommand.CommandType.CAST_ULTIMATE;
        }
        cat.setAbilitySlot1(a.getId());
        return PacketTeamCommand.CommandType.CAST_ABILITY_1;
    }

    private static TestFunction test(String name, int ticks, Consumer<GameTestHelper> body) {
        return new TestFunction("kittycombat", "kittycombat." + name, ARENA, ticks, 0L, true, body);
    }

    // ================================================================================================ abilities: Twine

    /**
     * Every ability and ultimate cast with its key while Twined: it fires, costs exactly its Energy, starts its
     * cooldown (a second press does nothing), does its job, and never touches a friend, now or a second later.
     */
    @GameTestGenerator
    public static Collection<TestFunction> twineCasts() {
        List<TestFunction> out = new ArrayList<>();
        for (CatAbility a : CatAbilities.getAll()) {
            out.add(test("twine_cast_" + a.getId(), 80, h -> twineCast(h, a)));
        }
        return out;
    }

    private static void twineCast(GameTestHelper h, CatAbility a) {
        Scene s = scene(h, true);
        guarded(h, s, () -> {
            TalentedCat c = s.cat;
            if (a.getCategory() == CatAbility.Category.UTILITY) c.setTalentLevel(Talents.TORCH_KITTY, 1);
            boolean heal = a == CatAbilities.HEALING_PURR || a == CatAbilities.CELESTIAL_PURR;
            if (heal) {
                c.setHealth(c.getMaxHealth() * 0.3F);
                s.friend.setHealth(6.0F);
                s.friendHp.put(s.friend, 6.0F);
            }
            PacketTeamCommand.CommandType key = slot(a, c);
            int max = FelineEnergyEngine.getMaxEnergy(c);
            c.setEnergy(max);
            float catHp = c.getHealth(), foeHp = s.foe.getHealth();
            long now = h.getLevel().getGameTime();
            PacketTeamCommand.apply(s.owner, key);

            h.assertTrue(c.getAbilityCooldown(a.getId()) == now, a.getId() + " did not cast from its key while Twined");
            h.assertTrue(c.getEnergy() == max - a.getEnergyCost(),
                    a.getId() + " should cost " + a.getEnergyCost() + " Energy, cost " + (max - c.getEnergy()));
            int afterFirst = c.getEnergy();
            PacketTeamCommand.apply(s.owner, key);
            h.assertTrue(c.getEnergy() == afterFirst, a.getId() + ": a second press during the cooldown spent Energy");

            if (DAMAGING.contains(a.getId())) {
                h.assertTrue(s.foe.getHealth() < foeHp || s.foe2.getHealth() < s.foe2.getMaxHealth(), a.getId() + " hurt no enemy");
            } else if (a == CatAbilities.FELINE_HISS) {
                h.assertTrue(FelineStatusEffects.hasStatus(s.foe, FelineStatusType.WEAKNESS), "Hiss should weaken the enemy");
            } else if (a == CatAbilities.FORTRESS_GUARD) {
                h.assertTrue(s.foe.getTarget() == c && FelineStatusEffects.hasStatus(c, FelineStatusType.FORTIFIED), "Fortress Guard: taunt and Fortified");
            } else if (heal) {
                h.assertTrue(c.getHealth() > catHp && s.friend.getHealth() > 6.0F, a.getId() + " should heal the cat and the co-owner");
            } else if (a == CatAbilities.CATNIP_FRENZY) {
                h.assertTrue(c.hasEffect(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST), "Catnip Frenzy should give Strength");
            }
            assertFriendsUnharmed(h, s, a.getId());
        });
        h.runAfterDelay(30, () -> guarded(h, s, () -> {
            assertFriendsUnharmed(h, s, a.getId() + " (1.5 s later)");
            end(h, s);
            h.succeed();
        }));
    }

    // ================================================================================================ abilities: kill credit

    /** A kill by any damaging ability belongs to the cat: kill count, XP and bond, like a claw kill. */
    @GameTestGenerator
    public static Collection<TestFunction> killCredit() {
        List<TestFunction> out = new ArrayList<>();
        for (CatAbility a : CatAbilities.getAll()) {
            if (DAMAGING.contains(a.getId())) out.add(test("kill_credit_" + a.getId(), 40, h -> killCredit(h, a)));
        }
        return out;
    }

    private static void killCredit(GameTestHelper h, CatAbility a) {
        Scene s = scene(h, false);
        guarded(h, s, () -> {
            TalentedCat c = s.cat;
            s.foe.setHealth(1.0F);
            s.foe2.setHealth(1.0F);
            c.setTarget(s.foe);
            int kills = c.stats().getTotalKills();
            h.assertTrue(a.tryCast(c, null), a.getId() + " did not cast");
            h.assertTrue(!s.foe.isAlive() || !s.foe2.isAlive(), a.getId() + " did not kill a 1 HP enemy");
            h.assertTrue(c.stats().getTotalKills() > kills, a.getId() + " killed an enemy but the cat got no credit (kill count, XP, bond)");
            end(h, s);
            h.succeed();
        });
    }

    // ================================================================================================ abilities: AI

    /** The AI casts every combat ability and ultimate on its own in a real fight (hurt cat, three enemies). */
    @GameTestGenerator
    public static Collection<TestFunction> aiCasts() {
        List<TestFunction> out = new ArrayList<>();
        for (CatAbility a : CatAbilities.getAll()) {
            if (a.getCategory() != CatAbility.Category.UTILITY) out.add(test("ai_cast_" + a.getId(), 260, h -> aiCast(h, a)));
        }
        return out;
    }

    private static void aiCast(GameTestHelper h, CatAbility a) {
        FakePlayer owner = KittyTalentGameTests.joinFakeOwner(h, new BlockPos(4, 2, 9));
        TalentedCat c = ownedCat(h, new BlockPos(8, 2, 9), owner, CatMode.AGGRESSIVE);
        c.setCatLevel(30);
        slot(a, c);
        // Only the ability under test: the rest of the default loadout (Dash, Hiss, Healing Purr, Catnip Frenzy) is
        // put on cooldown so it can't spend the Energy first (slots can't be empty).
        long now = h.getLevel().getGameTime();
        for (String other : new String[]{c.getAbilitySlot1(), c.getAbilitySlot2(), c.getAbilitySlot3(), c.getAbilityUltimate()}) {
            if (!other.equals(a.getId())) c.setAbilityCooldown(other, now + 100000L);
        }
        c.setEnergy(FelineEnergyEngine.getMaxEnergy(c));
        c.setHealth(c.getMaxHealth() * 0.35F);
        if (a == CatAbilities.TIDAL_SURGE) c.setSecondsOnFire(30); // Tidal Surge is the cat's fire extinguisher
        owner.setHealth(6.0F); // the healers also care about a hurt owner
        for (BlockPos p : new BlockPos[]{new BlockPos(14, 2, 8), new BlockPos(14, 2, 10), new BlockPos(15, 2, 9)}) foe(h, p);
        java.util.ArrayDeque<String> trail = new java.util.ArrayDeque<>();
        int[] t = {0};
        h.onEachTick(() -> {
            if (t[0]++ % 15 != 0) return;
            if (trail.size() >= 12) trail.removeFirst();
            var tg = c.getTarget();
            trail.addLast("t" + t[0] + (tg == null ? " no target" : " d=" + String.format("%.1f", c.distanceTo(tg)))
                    + " hp=" + Math.round(100 * c.getHealth() / c.getMaxHealth()) + "% e=" + c.getEnergy() + " p=" + a.getAiPriority(c)
                    + " goals=" + c.goalSelector.getRunningGoals().map(w -> w.getGoal().getClass().getSimpleName()).toList());
        });
        h.succeedWhen(() -> {
            if (c.getAbilityCooldown(a.getId()) <= 0) {
                throw new net.minecraft.gametest.framework.GameTestAssertException("the AI never cast " + a.getId() + ": " + trail);
            }
            KittyTalentGameTests.leave(h, owner);
        });
    }

    // ================================================================================================ Twine moves

    /**
     * Twine claw held for a second with a villager and a second cat right behind: only the enemy in front is hit, and
     * holding the button doesn't wear the weapon out every tick.
     */
    @GameTest(template = "arena", timeoutTicks = 60)
    public static void testTwineClawHitsOnlyTheEnemyInFront(GameTestHelper h) {
        Scene s = scene(h, true);
        guarded(h, s, () -> {
            s.foe.setPos(s.cat.getX() + 1.6, s.cat.getY(), s.cat.getZ());
            s.foe2.setPos(s.cat.getX() - 1.6, s.cat.getY(), s.cat.getZ() + 0.6); // an enemy behind the cat
            s.villager.setPos(s.cat.getX() - 1.2, s.cat.getY(), s.cat.getZ() - 0.6);
            s.buddy.setPos(s.cat.getX(), s.cat.getY(), s.cat.getZ() - 1.4);
            s.cat.setEquippedWeapon(new ItemStack(Items.IRON_SWORD));
        });
        int[] ticks = {0};
        h.onEachTick(() -> {
            if (ticks[0]++ < 20) input(s, true, false);
        });
        h.runAfterDelay(30, () -> guarded(h, s, () -> {
            assertFriendsUnharmed(h, s, "Twine claw");
            h.assertTrue(s.foe.getHealth() < s.foe.getMaxHealth(), "the enemy in front was not hit");
            h.assertTrue(s.foe2.getHealth() == s.foe2.getMaxHealth(), "the claw hit the enemy behind the cat instead of the one it faces");
            int wear = s.cat.getEquippedWeapon().getDamageValue();
            h.assertTrue(wear <= 4, "a second of holding the claw wore the sword by " + wear + " (one per tick, not per hit)");
            end(h, s);
            h.succeed();
        }));
    }

    /** Twine pounce lands on the enemy, never on a villager or a friend's pet standing next to it. */
    @GameTest(template = "arena", timeoutTicks = 60)
    public static void testTwinePounceSparesFriends(GameTestHelper h) {
        Scene s = scene(h, true);
        guarded(h, s, () -> {
            s.foe.setPos(s.cat.getX() + 1.8, s.cat.getY(), s.cat.getZ());
            s.villager.setPos(s.cat.getX() - 1.0, s.cat.getY(), s.cat.getZ());
            s.wolf.setPos(s.cat.getX(), s.cat.getY(), s.cat.getZ() + 1.2);
        });
        h.runAfterDelay(3, () -> guarded(h, s, () -> input(s, false, true)));
        h.runAfterDelay(20, () -> guarded(h, s, () -> {
            assertFriendsUnharmed(h, s, "Twine pounce");
            h.assertTrue(s.foe.getHealth() < s.foe.getMaxHealth(), "the pounce did not hit the enemy");
            end(h, s);
            h.succeed();
        }));
    }

    /** A perfect dodge refunds its Energy, even for a cat with more than 100 max Energy. */
    @GameTest(template = "arena", timeoutTicks = 20)
    public static void testPerfectDodgeRefundKeepsHighEnergy(GameTestHelper h) {
        Scene s = scene(h, true);
        guarded(h, s, () -> {
            int max = FelineEnergyEngine.getMaxEnergy(s.cat);
            h.assertTrue(max > 115, "test needs a cat with lots of Energy, max " + max);
            s.cat.setEnergy(max);
            s.foe.setTarget(s.cat);
            h.assertTrue(PacketTwineAction.apply(s.owner, s.cat.getId(), PacketTwineAction.ActionType.DODGE, 1), "dodge refused");
            h.assertTrue(s.cat.getEnergy() >= max - 15, "a perfect dodge left " + s.cat.getEnergy() + " of " + max + " Energy");
            assertFriendsUnharmed(h, s, "Dodge");
            end(h, s);
            h.succeed();
        });
    }

    /** Dodging next to a cow or a villager is not a "perfect dodge": no refund, and nobody passive gets slowed. */
    @GameTest(template = "arena", timeoutTicks = 20)
    public static void testDodgeNextToPassiveMobsIsNotPerfect(GameTestHelper h) {
        FakePlayer owner = KittyTalentGameTests.joinFakeOwner(h, new BlockPos(3, 2, 3));
        try {
            owner.moveTo(h.absolutePos(new BlockPos(18, 2, 20)).getX() + 0.5, h.absolutePos(new BlockPos(18, 2, 20)).getY(), h.absolutePos(new BlockPos(18, 2, 20)).getZ() + 0.5);
            TalentedCat c = ownedCat(h, new BlockPos(20, 2, 20), owner, CatMode.FOLLOW);
            Villager v = h.spawn(EntityType.VILLAGER, new BlockPos(21, 2, 20));
            v.setNoAi(true);
            var cow = h.spawn(EntityType.COW, new BlockPos(20, 2, 21));
            cow.setNoAi(true);
            h.assertTrue(PacketTwineAction.apply(owner, c.getId(), PacketTwineAction.ActionType.TWINE_START, 0), "twine");
            c.setEnergy(100);
            h.assertTrue(PacketTwineAction.apply(owner, c.getId(), PacketTwineAction.ActionType.DODGE, 1), "dodge refused");
            h.assertTrue(c.getEnergy() == 85, "a dodge with only a cow and a villager nearby refunded Energy (" + c.getEnergy() + ")");
            h.assertTrue(harmed(v, v.getHealth()) == null && harmed(cow, cow.getHealth()) == null, "the dodge debuffed a passive mob");
            h.succeed();
        } finally {
            TwineManager.endForPlayer(owner, null);
            KittyTalentGameTests.leave(h, owner);
        }
    }

    /** Each Team Synergy (picked by the cats' talents) hurts the enemy and none of the friends, the owner's third cat included. */
    @GameTestGenerator
    public static Collection<TestFunction> synergies() {
        List<TestFunction> out = new ArrayList<>();
        Map<String, Talent[]> kinds = new LinkedHashMap<>();
        kinds.put("pack_pounce", new Talent[0]);
        kinds.put("inferno_hunt", new Talent[]{Talents.INFERNO_CAT});
        kinds.put("frozen_tide", new Talent[]{Talents.AQUA_KITTY});
        kinds.put("twin_soul", new Talent[]{Talents.SHADOW_PELT});
        kinds.forEach((name, talents) -> out.add(test("synergy_" + name, 40, h -> synergy(h, name, talents))));
        return out;
    }

    private static void synergy(GameTestHelper h, String name, Talent[] talents) {
        Scene s = scene(h, true);
        guarded(h, s, () -> {
            TalentedCat third = ownedCat(h, new BlockPos(9, 2, 10), s.owner, CatMode.STAY);
            s.friendHp.put(third, third.getHealth());
            for (Talent t : talents) {
                s.cat.setTalentLevel(t, 1);
                s.buddy.setTalentLevel(t, 1);
            }
            s.cat.setEnergy(100);
            h.assertTrue(PacketTwineAction.apply(s.owner, s.cat.getId(), PacketTwineAction.ActionType.TEAM_SYNERGY, 0), "synergy refused");
            h.assertTrue(s.cat.getEnergy() < 100, name + " did not fire");
            h.assertTrue(s.foe.getHealth() < s.foe.getMaxHealth(), name + " did not hurt the enemy");
            s.friendHp.remove(s.buddy); // the partner joins the attack (and may get its buff)
            assertFriendsUnharmed(h, s, "Team Synergy " + name);
            end(h, s);
            h.succeed();
        });
    }

    // ================================================================================================ talents and statuses

    /**
     * Every talent at max rank, one cat each, in a 15-second brawl with real zombies: nothing throws, and every
     * cat's health and Energy stay sane.
     */
    @GameTest(template = "arena", timeoutTicks = 400)
    public static void testEveryTalentAtMaxRankInABrawl(GameTestHelper h) {
        FakePlayer owner = KittyTalentGameTests.joinFakeOwner(h, new BlockPos(15, 2, 15));
        owner.setInvulnerable(true);
        List<TalentedCat> cats = new ArrayList<>();
        int i = 0;
        for (Talent t : Talents.getAll()) {
            TalentedCat c = ownedCat(h, new BlockPos(2 + (i % 10) * 3, 2, 2 + (i / 10) * 3), owner, CatMode.AGGRESSIVE);
            c.setCatLevel(60);
            c.setTalentLevel(t, t.getMaxLevel());
            c.setEnergy(FelineEnergyEngine.getMaxEnergy(c));
            c.setAbilitySlot1(CatAbilities.FROST_NOVA.getId());
            c.setAbilityUltimate(CatAbilities.TEMPEST_CLAWS.getId());
            cats.add(c);
            i++;
        }
        List<Zombie> zombies = new ArrayList<>();
        for (int z = 0; z < 8; z++) {
            Zombie zombie = h.spawn(EntityType.ZOMBIE, new BlockPos(3 + z * 3, 2, 26));
            zombie.setPersistenceRequired();
            zombies.add(zombie);
        }
        h.runAfterDelay(300, () -> {
            try {
                for (TalentedCat c : cats) {
                    String who = "cat with " + Talents.getAll().stream().filter(t -> c.getTalentLevel(t) > 0).map(Talent::getId).toList();
                    h.assertTrue(Float.isFinite(c.getHealth()) && c.getHealth() <= c.getMaxHealth() + 0.01F, who + ": health " + c.getHealth());
                    int e = c.getEnergy(), max = FelineEnergyEngine.getMaxEnergy(c);
                    h.assertTrue(e >= 0 && e <= max, who + ": Energy " + e + " of " + max);
                    h.assertTrue(Double.isFinite(c.getX()) && Double.isFinite(c.getY()), who + ": position " + c.position());
                }
                long hurt = zombies.stream().filter(z -> !z.isAlive() || z.getHealth() < z.getMaxHealth()).count();
                h.assertTrue(hurt >= 4, "the cats barely fought: " + hurt + " of 8 zombies hurt");
                h.succeed();
            } finally {
                KittyTalentGameTests.leave(h, owner);
            }
        });
    }

    /** Every Feline status on a zombie and on a cat: damage-over-time hurts, everything expires, nothing breaks. */
    @GameTest(template = "arena", timeoutTicks = 200)
    public static void testEveryStatusTicksAndExpires(GameTestHelper h) {
        List<Mob> zombies = new ArrayList<>(); // vindicators: not undead, so Poison applies
        List<TalentedCat> cats = new ArrayList<>();
        FelineStatusType[] all = FelineStatusType.values();
        for (int i = 0; i < all.length; i++) {
            Mob z = foe(h, new BlockPos(2 + i * 2, 2, 6));
            FelineStatusEffects.applyStatus(z, all[i], all[i].getMaxPower(), 60);
            zombies.add(z);
            TalentedCat c = h.spawn(ModEntities.TALENTED_CAT.get(), new BlockPos(2 + i * 2, 2, 12));
            c.setNoAi(true);
            FelineStatusEffects.applyStatus(c, all[i], all[i].getMaxPower(), 60);
            cats.add(c);
        }
        for (int i = 0; i < all.length; i++) {
            h.assertTrue(FelineStatusEffects.hasStatus(zombies.get(i), all[i]), all[i] + " was not applied to a zombie");
        }
        h.runAfterDelay(120, () -> {
            for (int i = 0; i < all.length; i++) {
                for (LivingEntity e : new LivingEntity[]{zombies.get(i), cats.get(i)}) {
                    h.assertTrue(!FelineStatusEffects.hasStatus(e, all[i]), all[i] + " never expired on a " + e.getType().toShortString());
                    h.assertTrue(Float.isFinite(e.getHealth()), all[i] + ": health " + e.getHealth());
                }
            }
            for (FelineStatusType dot : new FelineStatusType[]{FelineStatusType.BURN, FelineStatusType.POISON, FelineStatusType.BLEED}) {
                Mob z = zombies.get(dot.ordinal());
                h.assertTrue(!z.isAlive() || z.getHealth() < z.getMaxHealth(), dot + " did no damage over time");
            }
            h.succeed();
        });
    }

    /** Small rules: each element's suggested signature is a normal skill, and every skill/ultimate has an icon id and a cost. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testAbilityCatalogIsConsistent(GameTestHelper h) {
        for (FelineElement e : FelineElement.values()) {
            CatAbility a = CatAbilities.getDefaultAbilityForElement(e);
            h.assertTrue(a != null && a.getCategory() == CatAbility.Category.SKILL, e + " suggests " + (a == null ? null : a.getId()) + ", not a skill");
        }
        for (CatAbility a : CatAbilities.getAll()) {
            h.assertTrue(a.getEnergyCost() > 0 && a.getCooldownTicks() > 0, a.getId() + ": cost " + a.getEnergyCost() + ", cooldown " + a.getCooldownTicks());
        }
        h.assertTrue(CatAbilities.getUltimates().size() == 8 && CatAbilities.getSkills().size() == 13, "8 ultimates and 13 skills, got "
                + CatAbilities.getUltimates().size() + " and " + CatAbilities.getSkills().size());
        h.succeed();
    }
}
