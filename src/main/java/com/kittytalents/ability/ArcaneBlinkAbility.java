package com.kittytalents.ability;

import com.kittytalents.combat.FelineElement;
import com.kittytalents.combat.FelineStatusEffects;
import com.kittytalents.combat.FelineStatusType;
import com.kittytalents.core.tuning.Tuning;
import com.kittytalents.entity.TalentedCat;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Arcane Blink (4.0, Arcane skill): the cat blinks behind a foe up to 12 blocks away and strikes it, leaving it Marked. */
public class ArcaneBlinkAbility extends CatAbility {
    static final Tuning.Key DAMAGE = Tuning.ability("arcane_blink", "damage_mult", 1.8, "Damage x attack");
    static final Tuning.Key RANGE = Tuning.ability("arcane_blink", "range", 12.0, "Range");
    static final Tuning.Key MARK = Tuning.ability("arcane_blink", "marked_ticks", 100, "Marked ticks");

    public ArcaneBlinkAbility() {
        super("arcane_blink", "Arcane Blink", "Blinks behind a distant foe in a flash of starlight and strikes, marking it for the kill.",
                30, 80, FelineElement.ARCANE, Category.SKILL);
    }

    @Override
    public float getAiPriority(TalentedCat cat) {
        LivingEntity t = cat.getTarget();
        if (t == null || !t.isAlive() || cat.isPassenger() || cat.isVehicle()) return 0.3f;
        double d = cat.distanceTo(t);
        return d >= 4.0D && d <= RANGE.get() ? 1.7f : 0.6f;
    }

    @Override
    protected boolean execute(TalentedCat cat, Player player) {
        if (cat.isPassenger() || cat.isVehicle()) return false; // never yank a rider or a Twined body around
        LivingEntity t = AbilityTargets.target(cat, RANGE.get());
        if (t == null) return false;
        Vec3 spot = behind(cat, t);
        if (spot == null) return false;
        if (cat.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.PORTAL, cat.getX(), cat.getY() + 0.4, cat.getZ(), 24, 0.3, 0.3, 0.3, 0.3);
        }
        cat.teleportTo(spot.x, spot.y, spot.z);
        cat.getLookControl().setLookAt(t);
        cat.playSound(SoundEvents.ENDERMAN_TELEPORT, 0.8F, 1.6F);
        cat.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.2F, 1.4F);
        t.hurt(cat.damageSources().indirectMagic(cat, cat), (float) cat.getAttributeValue(Attributes.ATTACK_DAMAGE) * DAMAGE.f());
        FelineStatusEffects.applyStatus(t, FelineStatusType.MARKED, 1, MARK.i());
        if (cat.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.END_ROD, t.getX(), t.getY() + t.getBbHeight() * 0.6, t.getZ(), 16, 0.3, 0.4, 0.3, 0.05);
        }
        return true;
    }

    /** A free spot on solid ground behind the target (then either side, then in front), or null. */
    static Vec3 behind(TalentedCat cat, LivingEntity t) {
        Vec3 look = Vec3.directionFromRotation(0, t.getYRot()).normalize();
        Vec3 side = new Vec3(-look.z, 0, look.x);
        for (Vec3 off : new Vec3[]{look.scale(-1.5), side.scale(1.5), side.scale(-1.5), look.scale(1.5)}) {
            Vec3 p = t.position().add(off);
            AABB box = cat.getBoundingBox().move(p.subtract(cat.position()));
            if (cat.level().noCollision(cat, box) && !cat.level().noCollision(cat, box.move(0, -0.6, 0))) return p;
        }
        return null;
    }
}
