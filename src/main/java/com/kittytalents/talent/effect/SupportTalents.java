package com.kittytalents.talent.effect;

import com.kittytalents.entity.TalentedCat;
import com.kittytalents.talent.Talents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SaplingBlock;

import java.util.List;

/** Support, social and work talents. */
final class SupportTalents {
    private SupportTalents() {}

    static void register() {
        TalentEngine.register(Talents.PURR_MEDIC, new PurrMedic());
        TalentEngine.register(Talents.LUCKY_PURR, new LuckyPurr());
        TalentEngine.register(Talents.RESCUE_PURR, new RescuePurr());
        TalentEngine.register(Talents.HAPPY_EATER, new HappyEater());
        TalentEngine.register(Talents.CHARM_WHISKERS, new CharmWhiskers());
        TalentEngine.register(Talents.HERDING_KITTY, new HerdingKitty());
        TalentEngine.register(Talents.GREEN_PAW, new GreenPaw());
        TalentEngine.register(Talents.TIMBER_CLAWS, new TimberClaws());
        TalentEngine.register(Talents.LOGISTICS_WHISKERS, new LogisticsWhiskers());
    }

    /** Regenerates a hurt owner every 5 s (Absorption from rank 3). Mastery "Field Medic": also heals trusted players and the owner's pets. */
    static final class PurrMedic implements TalentEffect {
        private static final com.kittytalents.core.tuning.Tuning.Key RANGE =
                com.kittytalents.core.tuning.Tuning.talent("purr_medic", "range", 8.0, "Range");
        private static final com.kittytalents.core.tuning.Tuning.Key REGEN =
                com.kittytalents.core.tuning.Tuning.talent("purr_medic", "regen_ticks", 80, "Regeneration ticks");
        private static final com.kittytalents.core.tuning.Tuning.Key REGEN_PER =
                com.kittytalents.core.tuning.Tuning.talent("purr_medic", "regen_ticks_per_rank", 20, "Regeneration ticks per rank");
        @Override public boolean worksWhileResting() { return true; }
        @Override public int tickInterval() { return 100; }
        @Override public void onTick(TalentContext ctx) {
            boolean purred = false;
            Player o = ctx.ownerWithin(RANGE.get());
            if (o != null && o.getHealth() < o.getMaxHealth()) {
                o.addEffect(new MobEffectInstance(MobEffects.REGENERATION, REGEN.i() + ctx.rank * REGEN_PER.i(), 1, false, false, true));
                if (ctx.rank >= 3) o.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 120, 0, false, false, true));
                purred = true;
            }
            if (ctx.mastered()) {
                List<LivingEntity> patients = ctx.cat.level().getEntitiesOfClass(LivingEntity.class, ctx.cat.getBoundingBox().inflate(RANGE.get()),
                        e -> e != o && e.getHealth() < e.getMaxHealth() && (
                                (e instanceof Player p && ctx.cat.isTrusted(p.getUUID()))
                                || (e instanceof TamableAnimal pet && pet.isTame() && ctx.cat.getOwnerUUID() != null
                                    && ctx.cat.getOwnerUUID().equals(pet.getOwnerUUID()))));
                for (LivingEntity e : patients) {
                    e.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 1, false, false, true));
                    purred = true;
                }
            }
            if (purred) ctx.cat.playSound(SoundEvents.CAT_PURR, 1.0F, 1.0F);
        }
    }

    /** Luck to nearby players (Regeneration from rank 3). Mastery "Fortune's Favorite": Luck +1 level and Regeneration II. */
    static final class LuckyPurr implements TalentEffect {
        private static final com.kittytalents.core.tuning.Tuning.Key R =
                com.kittytalents.core.tuning.Tuning.talent("lucky_purr", "radius", 6.0, "Radius");
        private static final com.kittytalents.core.tuning.Tuning.Key R_PER =
                com.kittytalents.core.tuning.Tuning.talent("lucky_purr", "radius_per_rank", 2.0, "Radius per rank");
        @Override public boolean worksWhileResting() { return true; }
        @Override public int tickInterval() { return 100; }
        @Override public void onTick(TalentContext ctx) {
            for (Player p : ctx.cat.level().getEntitiesOfClass(Player.class, ctx.cat.getBoundingBox().inflate(R.get() + ctx.rank * R_PER.get()))) {
                p.addEffect(new MobEffectInstance(MobEffects.LUCK, 200, ctx.rank - 1 + (ctx.mastered() ? 1 : 0), false, false, true));
                if (ctx.rank >= 3) p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, ctx.mastered() ? 1 : 0, false, false, true));
            }
        }
    }

    /** Teleports to and heals an owner below 30% health. Mastery "Guardian Angel": also Resistance IV for 5 s. */
    static final class RescuePurr implements TalentEffect {
        private static final com.kittytalents.core.tuning.Tuning.Key HP =
                com.kittytalents.core.tuning.Tuning.talent("rescue_purr", "trigger_below_health", 0.3, "Rescue below owner health");
        private static final com.kittytalents.core.tuning.Tuning.Key CD =
                com.kittytalents.core.tuning.Tuning.talent("rescue_purr", "cooldown_ticks", 600, "Cooldown ticks");
        private static final com.kittytalents.core.tuning.Tuning.Key CD_PER =
                com.kittytalents.core.tuning.Tuning.talent("rescue_purr", "cooldown_ticks_per_rank", 60, "Cooldown cut per rank");
        private static final com.kittytalents.core.tuning.Tuning.Key HEAL =
                com.kittytalents.core.tuning.Tuning.talent("rescue_purr", "heal", 4.0, "Heal");
        private static final com.kittytalents.core.tuning.Tuning.Key HEAL_PER =
                com.kittytalents.core.tuning.Tuning.talent("rescue_purr", "heal_per_rank", 2.0, "Heal per rank");
        @Override public int tickInterval() { return 10; }
        @Override public void onTick(TalentContext ctx) {
            Player o = ctx.ownerWithin(32.0D);
            if (o == null) return;
            boolean bleeding = com.kittytalents.compat.playerrevive.PlayerReviveCompat.isBleeding(o);
            if (!bleeding && (o.getHealth() > o.getMaxHealth() * HP.f() || !ctx.ready("rescue"))) return;
            ctx.cooldown("rescue", Math.max(20, CD.i() - ctx.rank * CD_PER.i()));
            ctx.cat.teleportTo(o.getX(), o.getY(), o.getZ());
            if (bleeding) {
                com.kittytalents.compat.playerrevive.PlayerReviveCompat.revive(o);
            }
            o.heal(HEAL.f() + ctx.rank * HEAL_PER.f());
            o.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 1));
            o.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 1));
            if (ctx.mastered()) o.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 3));
            ctx.cat.playSound(SoundEvents.BEACON_ACTIVATE, 1.0F, 1.6F);
            ctx.cat.addHealingProvided(6 + ctx.rank * 2);
        }
    }

    /** Slower hunger drain (in TalentedCat / food helper). Mastery "Bottomless Belly": hunger never falls below half. */
    static final class HappyEater implements TalentEffect {
        private static final com.kittytalents.core.tuning.Tuning.Key FLOOR =
                com.kittytalents.core.tuning.Tuning.talent("happy_eater", "mastery_min_hunger", 60, "Mastery: hunger never below");
        @Override public boolean worksWhileResting() { return true; }
        @Override public int tickInterval() { return 100; }
        @Override public void onTick(TalentContext ctx) {
            if (ctx.mastered() && ctx.cat.getHunger() < FLOOR.i()) ctx.cat.setHunger(FLOOR.i());
        }
    }

    /** Hits may Weaken and Slow; near villagers the owner gets Hero of the Village. Mastery "Village Darling": villagers nearby restock trades. */
    static final class CharmWhiskers implements TalentEffect {
        private static final com.kittytalents.core.tuning.Tuning.Key CHANCE =
                com.kittytalents.core.tuning.Tuning.talent("charm_whiskers", "charm_chance_per_rank", 0.15, "Charm chance per rank");
        private static final com.kittytalents.core.tuning.Tuning.Key R =
                com.kittytalents.core.tuning.Tuning.talent("charm_whiskers", "villager_radius", 8.0, "Villager radius");
        private static final com.kittytalents.core.tuning.Tuning.Key R_PER =
                com.kittytalents.core.tuning.Tuning.talent("charm_whiskers", "villager_radius_per_rank", 2.0, "Villager radius per rank");
        @Override public boolean worksWhileResting() { return true; }
        @Override public void onHitDealt(TalentContext ctx, LivingEntity t, float dmg) {
            if (ctx.cat.getRandom().nextFloat() < CHANCE.f() * ctx.rank) {
                t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, Math.max(0, ctx.rank - 1)));
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
            }
        }
        @Override public int tickInterval() { return 40; }
        @Override public void onTick(TalentContext ctx) {
            double r = R.get() + ctx.rank * R_PER.get();
            List<Villager> villagers = ctx.cat.level().getEntitiesOfClass(Villager.class, ctx.cat.getBoundingBox().inflate(r), LivingEntity::isAlive);
            if (villagers.isEmpty()) return;
            int amp = Math.min(4, Math.max(0, ctx.rank - 1));
            Player o = ctx.ownerWithin(r);
            if (o != null) o.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, 120, amp, false, false, true));
            if (ctx.mastered() && ctx.ready("restock")) {
                ctx.cooldown("restock", 1200);
                villagers.forEach(Villager::restock);
                if (ctx.cat.level() instanceof ServerLevel sl) {
                    for (Villager v : villagers) sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, v.getX(), v.getY() + 1.2, v.getZ(), 4, 0.3, 0.3, 0.3, 0.02);
                }
            }
        }
    }

    /** Draws nearby livestock towards the cat. Mastery "Shepherd": baby animals nearby grow up faster. */
    static final class HerdingKitty implements TalentEffect {
        private static final com.kittytalents.core.tuning.Tuning.Key R =
                com.kittytalents.core.tuning.Tuning.talent("herding_kitty", "radius", 6.0, "Radius");
        private static final com.kittytalents.core.tuning.Tuning.Key R_PER =
                com.kittytalents.core.tuning.Tuning.talent("herding_kitty", "radius_per_rank", 2.0, "Radius per rank");
        @Override public int tickInterval() { return 40; }
        @Override public void onTick(TalentContext ctx) {
            for (Animal a : ctx.cat.level().getEntitiesOfClass(Animal.class, ctx.cat.getBoundingBox().inflate(R.get() + ctx.rank * R_PER.get()),
                    a -> !(a instanceof Cat) && !(a instanceof TalentedCat))) {
                if (a.getTarget() == null && ctx.cat.getRandom().nextFloat() < 0.3F) a.getNavigation().moveTo(ctx.cat, 1.0D);
                if (ctx.mastered() && a.isBaby()) a.ageUp(30);
            }
        }
    }

    /** Unlocks the Farmer job (see FarmerJobGoal). Mastery "Harvest Moon": crops near the cat grow faster. */
    static final class GreenPaw implements TalentEffect {
        @Override public boolean worksWhileResting() { return true; }
        @Override public int tickInterval() { return 40; }
        @Override public void onTick(TalentContext ctx) {
            if (ctx.mastered()) TalentHelpers.growPlants(ctx.cat, 4, 6, s -> s.getBlock() instanceof CropBlock);
        }
    }

    /** Unlocks the Lumberjack job. Mastery "Forest Friend": saplings near the cat grow faster. */
    static final class TimberClaws implements TalentEffect {
        @Override public boolean worksWhileResting() { return true; }
        @Override public int tickInterval() { return 40; }
        @Override public void onTick(TalentContext ctx) {
            if (ctx.mastered()) TalentHelpers.growPlants(ctx.cat, 5, 6, s -> s.getBlock() instanceof SaplingBlock);
        }
    }

    /** Unlocks Courier (Sorter at rank 3). Mastery "Express Delivery": +20% move speed while working. */
    static final class LogisticsWhiskers implements TalentEffect {
        private static final com.kittytalents.core.tuning.Tuning.Key SPEED =
                com.kittytalents.core.tuning.Tuning.talent("logistics_whiskers", "mastery_work_speed_mult", 1.2, "Mastery: work speed x");
        @Override public void applyAttributes(TalentContext ctx, AttributeTotals t) {
            if (ctx.mastered() && ctx.cat.getCatMode() == com.kittytalents.entity.CatMode.WORK) t.movementSpeed *= SPEED.get();
        }
    }
}
