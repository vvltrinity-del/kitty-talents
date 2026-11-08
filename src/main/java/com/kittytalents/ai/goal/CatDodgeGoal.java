package com.kittytalents.ai.goal;

import com.kittytalents.ai.CatTargeting;
import com.kittytalents.combat.FelineCombatEngine;
import com.kittytalents.combat.FelineCombatEngine.DodgeDirection;
import com.kittytalents.entity.TalentedCat;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * AI dodging (playtest request; previously only Twine could dodge). Every few ticks the cat checks for an incoming
 * projectile, a creeper about to explode, or a hostile closing in for a swing, and dodges away from it through the same
 * FelineCombatEngine#performDodge Twine uses (same Energy cost, i-frames and perfect-dodge window).
 * Claims no control flags: the dodge is an instant impulse, it never blocks movement goals.
 */
public class CatDodgeGoal extends Goal {
    private static final int COOLDOWN_TICKS = 50;
    /** Chance per check to sidestep a melee attacker in reach (a dodge every swing would be unbeatable). */
    private static final float MELEE_DODGE_CHANCE = 0.25F;

    private final TalentedCat cat;
    private long readyAt;

    public CatDodgeGoal(TalentedCat cat) {
        this.cat = cat;
        this.setFlags(EnumSet.noneOf(Goal.Flag.class));
    }

    @Override
    public boolean canUse() {
        if (cat.isTwined() || cat.isIncapacitated() || cat.isOrderedToSit() || !cat.getCatMode().canAttack()) return false;
        // (GoalSelector already throttles how often canUse runs.)
        if (cat.level().getGameTime() < readyAt || !cat.onGround()) return false;
        Vec3 threat = findThreat();
        if (threat == null) return false;
        dodgeAwayFrom(threat);
        return false; // one-shot: nothing to run
    }

    /** Position of the most urgent threat, or null. */
    private Vec3 findThreat() {
        // 1) A projectile flying at the cat.
        for (Projectile p : cat.level().getEntitiesOfClass(Projectile.class, cat.getBoundingBox().inflate(6.0D),
                p -> p.isAlive() && !(p.getOwner() instanceof LivingEntity o && CatTargeting.isFriendly(cat, o)))) {
            Vec3 v = p.getDeltaMovement();
            Vec3 toCat = cat.position().add(0, cat.getBbHeight() / 2, 0).subtract(p.position());
            if (v.lengthSqr() > 0.09D && toCat.lengthSqr() > 1.0E-4D && v.normalize().dot(toCat.normalize()) > 0.85D) {
                return p.position();
            }
        }
        // 2) A creeper about to blow up nearby.
        for (Creeper c : cat.level().getEntitiesOfClass(Creeper.class, cat.getBoundingBox().inflate(4.0D),
                c -> c.isAlive() && c.getSwellDir() > 0)) {
            return c.position();
        }
        // 3) A hostile in swing range that is after the cat.
        for (Mob m : cat.level().getEntitiesOfClass(Mob.class, cat.getBoundingBox().inflate(2.2D),
                m -> m.isAlive() && m.getTarget() == cat)) {
            if (cat.getRandom().nextFloat() < MELEE_DODGE_CHANCE) return m.position();
        }
        return null;
    }

    private void dodgeAwayFrom(Vec3 threat) {
        // Face the threat, then hop backward from it (sideways for projectiles, to step out of the line of fire).
        Vec3 d = threat.subtract(cat.position());
        if (d.horizontalDistanceSqr() < 1.0E-4D) return;
        float yawToThreat = (float) (Math.atan2(d.z, d.x) * (180.0D / Math.PI)) - 90.0F;
        cat.setYRot(yawToThreat);
        cat.setYBodyRot(yawToThreat);
        boolean projectile = !cat.level().getEntitiesOfClass(Entity.class, new net.minecraft.world.phys.AABB(threat, threat).inflate(0.5D),
                e -> e instanceof Projectile).isEmpty();
        DodgeDirection dir = projectile ? (cat.getRandom().nextBoolean() ? DodgeDirection.LEFT : DodgeDirection.RIGHT)
                : DodgeDirection.BACKWARD;
        if (FelineCombatEngine.performDodge(cat, dir)) {
            readyAt = cat.level().getGameTime() + COOLDOWN_TICKS;
        }
    }
}
