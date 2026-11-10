package com.kittytalents.talent.effect;

import com.kittytalents.talent.Talents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LightLayer;

/** Defensive talents (Nine Lives and Feline Reflexes live in TalentedCat#hurt / FelineCombatEngine). */
final class DefenseTalents {
    private DefenseTalents() {}

    static void register() {
        TalentEngine.register(Talents.FIREPROOF_PAWS, new FireproofPaws());
        TalentEngine.register(Talents.GUARDIAN_CAT, new GuardianCat());
        TalentEngine.register(Talents.CAT_ARMOR, new CatArmor());
        TalentEngine.register(Talents.WATER_PELT, new WaterPelt());
        TalentEngine.register(Talents.SHADOW_PELT, new ShadowPelt());
    }

    /** Immune to fire, lava and hot floors. Mastery "Flameward": the owner nearby gets Fire Resistance. */
    static final class FireproofPaws implements TalentEffect {
        @Override public boolean worksWhileResting() { return true; }
        @Override public float modifyIncomingDamage(TalentContext ctx, DamageSource src, float amt) {
            if (src.is(DamageTypeTags.IS_FIRE) || src.is(DamageTypes.HOT_FLOOR) || src.is(DamageTypes.LAVA)) {
                ctx.cat.clearFire();
                return 0.0F;
            }
            return amt;
        }
        @Override public int tickInterval() { return 40; }
        @Override public void onTick(TalentContext ctx) {
            Player o = ctx.mastered() ? ctx.ownerWithin(8.0D) : null;
            if (o != null) TalentHelpers.aura(o, MobEffects.FIRE_RESISTANCE, 100, 0);
        }
    }

    /** Takes 12%/rank less damage (min 40% taken). Mastery "Bulwark": absorbs 25% of the damage its owner takes. */
    static final class GuardianCat implements TalentEffect {
        private static final com.kittytalents.core.tuning.Tuning.Key PER =
                com.kittytalents.core.tuning.Tuning.talent("guardian_cat", "damage_reduction_per_rank", 0.12, "Damage reduction per rank");
        private static final com.kittytalents.core.tuning.Tuning.Key FLOOR =
                com.kittytalents.core.tuning.Tuning.talent("guardian_cat", "min_damage_taken", 0.4, "Min damage taken");
        private static final com.kittytalents.core.tuning.Tuning.Key SHARE =
                com.kittytalents.core.tuning.Tuning.talent("guardian_cat", "mastery_owner_share", 0.25, "Mastery: owner damage absorbed");
        @Override public float modifyIncomingDamage(TalentContext ctx, DamageSource src, float amt) {
            return amt > 1.0F ? amt * Math.max(FLOOR.f(), 1.0F - ctx.rank * PER.f()) : amt;
        }
        @Override public float onOwnerHurt(TalentContext ctx, Player owner, DamageSource src, float amt) {
            if (!ctx.mastered() || amt <= 1.0F || src.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return amt;
            if (ctx.cat.getHealth() < ctx.cat.getMaxHealth() * 0.3F) return amt; // don't sacrifice itself
            float shared = amt * SHARE.f();
            ctx.cat.hurt(ctx.cat.damageSources().generic(), shared);
            return amt - shared;
        }
    }

    /** +3 armor, +1.5 toughness, +15% knockback resistance per rank. Mastery "Living Armor": worn armor repairs itself (free Mending). */
    static final class CatArmor implements TalentEffect {
        private static final com.kittytalents.core.tuning.Tuning.Key ARMOR =
                com.kittytalents.core.tuning.Tuning.talent("cat_armor", "armor_per_rank", 3.0, "Armor per rank");
        private static final com.kittytalents.core.tuning.Tuning.Key TOUGH =
                com.kittytalents.core.tuning.Tuning.talent("cat_armor", "toughness_per_rank", 1.5, "Toughness per rank");
        private static final com.kittytalents.core.tuning.Tuning.Key KB =
                com.kittytalents.core.tuning.Tuning.talent("cat_armor", "knockback_resist_per_rank", 0.15, "Knockback resistance per rank");
        @Override public boolean worksWhileResting() { return true; }
        @Override public void applyAttributes(TalentContext ctx, AttributeTotals t) {
            t.armor += ctx.rank * ARMOR.get();
            t.armorToughness += ctx.rank * TOUGH.get();
            t.knockbackResistance = Math.min(0.75D, t.knockbackResistance + ctx.rank * KB.get());
        }
        @Override public int tickInterval() { return 40; }
        @Override public void onTick(TalentContext ctx) {
            if (!ctx.mastered()) return;
            for (int slot = 1; slot <= 4; slot++) TalentHelpers.repair(ctx.cat.getArmorSlotItem(slot), 1);
        }
    }

    /** Puts out a burning owner and gives water breathing. Mastery "Tideborn": owner gets Conduit Power in water near the cat. */
    static final class WaterPelt implements TalentEffect {
        @Override public boolean worksWhileResting() { return true; }
        @Override public int tickInterval() { return 10; }
        @Override public void onTick(TalentContext ctx) {
            Player o = ctx.ownerWithin(8.0D);
            if (o == null) return;
            if (o.isOnFire()) {
                o.clearFire();
                ctx.cat.level().playSound(null, o.getX(), o.getY(), o.getZ(), SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.PLAYERS, 0.7F, 1.0F);
            }
            if (o.isInWater() || ctx.cat.isInWater()) {
                TalentHelpers.aura(o, MobEffects.WATER_BREATHING, 60, 0);
                if (ctx.mastered()) TalentHelpers.aura(o, MobEffects.CONDUIT_POWER, 60, 0);
            }
        }
    }

    /** Invisible in darkness or while crouching. Mastery "Umbral Cloak": the owner nearby is also hidden in darkness. */
    static final class ShadowPelt implements TalentEffect {
        private static final com.kittytalents.core.tuning.Tuning.Key LIGHT =
                com.kittytalents.core.tuning.Tuning.talent("shadow_pelt", "max_light", 4, "Hidden at light level or below");
        @Override public boolean worksWhileResting() { return true; }
        @Override public int tickInterval() { return 20; }
        @Override public void onTick(TalentContext ctx) {
            int light = ctx.cat.level().getBrightness(LightLayer.BLOCK, ctx.cat.blockPosition());
            if (light <= LIGHT.i() || ctx.cat.isCrouching()) TalentHelpers.aura(ctx.cat, MobEffects.INVISIBILITY, 40, 0);
            Player o = ctx.mastered() ? ctx.ownerWithin(8.0D) : null;
            if (o != null && o.level().getBrightness(LightLayer.BLOCK, o.blockPosition()) <= LIGHT.i()) {
                TalentHelpers.aura(o, MobEffects.INVISIBILITY, 40, 0);
            }
        }
    }

}
