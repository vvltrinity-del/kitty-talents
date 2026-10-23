package com.kittytalents.entity.ai;

import com.kittytalents.compat.tacz.TacZCompat;
import com.kittytalents.entity.CatMode;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.talent.Talents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.List;

/**
 * R12: Centralized Target Selection & Priority Engine.
 * Eliminates contradictory targeting behavior by unifying all threat, mode, ally,
 * distance, ammunition, and danger evaluations into an authoritative scoring pipeline.
 */
public class CatCentralTargetGoal extends TargetGoal {
    private final TalentedCat cat;
    private LivingEntity selectedTarget;
    private float highestThreatScore;
    private int evalCooldown = 0;

    public CatCentralTargetGoal(TalentedCat cat) {
        super(cat, false);
        this.cat = cat;
        this.setFlags(EnumSet.of(Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        if (cat.isTwined() || cat.isIncapacitated() || cat.isOrderedToSit() || !cat.getCatMode().canAttack()) {
            if (cat.getTarget() != null) {
                cat.setTarget(null);
            }
            evalCooldown = 20;
            return false;
        }

        if (cat.getCatMode() == CatMode.RETREAT) {
            if (cat.getTarget() != null) {
                cat.setTarget(null);
            }
            evalCooldown = 20;
            return false;
        }

        if (evalCooldown > 0) {
            evalCooldown--;
            return false;
        }

        // Throttle distant or idle cats: if no player is within 48 blocks, scan every 30 ticks
        boolean distant = !cat.level().hasNearbyAlivePlayer(cat.getX(), cat.getY(), cat.getZ(), 48.0D);
        if (distant) {
            evalCooldown = 30;
        } else if (!cat.getCatMode().isCombatMode() && cat.getTarget() == null) {
            evalCooldown = 12;
        } else {
            evalCooldown = 5;
        }

        LivingEntity candidate = evaluateBestTarget();
        if (candidate != null && candidate != cat.getTarget()) {
            this.selectedTarget = candidate;
            return true;
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity current = cat.getTarget();
        if (current == null || !current.isAlive()) return false;
        if (cat.isTwined() || cat.isIncapacitated() || cat.isOrderedToSit() || !cat.getCatMode().canAttack()) return false;
        if (cat.getCatMode() == CatMode.RETREAT) return false;
        if (cat.distanceToSqr(current) > 900.0D) return false; // Beyond 30 blocks

        // Creeper danger check: if creeper is actively swelling/ignited, disengage melee
        if (current instanceof Creeper creeper && creeper.isIgnited()) {
            return false;
        }

        return true;
    }

    @Override
    public void start() {
        if (this.selectedTarget != null) {
            this.mob.setTarget(this.selectedTarget);
        }
        super.start();
    }

    @Override
    public void stop() {
        this.selectedTarget = null;
        this.highestThreatScore = 0.0F;
        super.stop();
    }

    /**
     * Evaluates all nearby entities and computes unified threat scores.
     */
    private LivingEntity evaluateBestTarget() {
        CatMode mode = cat.getCatMode();
        LivingEntity owner = cat.getOwner();
        double searchRadius = cat.hasEquippedGun() ? 24.0D : 16.0D;
        if (mode == CatMode.GUARD) searchRadius = 16.0D;
        if (mode == CatMode.AGGRESSIVE || mode == CatMode.BERSERKER) searchRadius = 24.0D;

        AABB searchArea = cat.getBoundingBox().inflate(searchRadius, 6.0D, searchRadius);
        List<LivingEntity> potentials = cat.level().getEntitiesOfClass(LivingEntity.class, searchArea,
                e -> e != cat && e != owner && e.isAlive() && !cat.isAlliedTo(e));

        LivingEntity best = null;
        float bestScore = 0.0F;

        // Current target hysteresis: bonus score to avoid jittering
        LivingEntity current = cat.getTarget();
        if (current != null && current.isAlive()) {
            best = current;
            bestScore = calculateScore(current, mode, owner) + 20.0F;
        }

        for (LivingEntity candidate : potentials) {
            if (candidate == current) continue;
            float score = calculateScore(candidate, mode, owner);
            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }

        this.highestThreatScore = bestScore;
        return bestScore > 10.0F ? best : null;
    }

    private float calculateScore(LivingEntity entity, CatMode mode, LivingEntity owner) {
        if (entity == null || !entity.isAlive()) return 0.0F;

        // Never target pets or trusted players
        if (entity instanceof Player player) {
            if (cat.isOwnedBy(player) || cat.isTrusted(player)) return 0.0F;
        }

        // Danger check: ignited/swelling creepers are suicidal to attack
        if (entity instanceof Creeper creeper) {
            if (creeper.isIgnited() || creeper.getSwellDir() > 0) {
                return 0.0F; // Abort attack
            }
            // Night Prowler scaring
            if (cat.getTalentLevel(Talents.NIGHT_PROWLER) > 0) {
                return 30.0F;
            }
        }

        float score = 0.0F;
        double distSq = cat.distanceToSqr(entity);

        // 1. Threat to Owner (highest priority)
        if (owner != null) {
            if (owner.getLastHurtByMob() == entity && (cat.tickCount - owner.getLastHurtByMobTimestamp()) < 100) {
                score += 120.0F;
            }
            if (owner.getLastHurtMob() == entity && (cat.tickCount - owner.getLastHurtMobTimestamp()) < 100) {
                score += 90.0F;
            }
        }

        // 2. Threat to Trusted Players
        for (Player trusted : cat.level().getEntitiesOfClass(Player.class, cat.getBoundingBox().inflate(16.0D),
                p -> cat.isTrusted(p.getUUID()) && p != owner)) {
            if (trusted.getLastHurtByMob() == entity && (cat.tickCount - trusted.getLastHurtByMobTimestamp()) < 100) {
                score += 85.0F;
                break;
            }
        }

        // 3. Direct threat to cat
        if (cat.getLastHurtByMob() == entity && (cat.tickCount - cat.getLastHurtByMobTimestamp()) < 80) {
            score += 75.0F;
        }

        // 4. Mode-specific targeting
        switch (mode) {
            case AGGRESSIVE -> {
                if (entity instanceof Monster) score += 60.0F;
                else if (entity instanceof Enemy) score += 40.0F;
            }
            case BERSERKER -> {
                if (entity instanceof Monster) score += 80.0F;
                else if (entity instanceof Enemy) score += 50.0F;
            }
            case GUARD -> {
                if (entity instanceof Monster && distSq <= 256.0D) {
                    score += 65.0F;
                }
            }
            case DEFENSIVE, PROTECT -> {
                // Only attacks enemies within 8 blocks of owner or actively targeting owner
                if (owner != null && entity.distanceToSqr(owner) < 64.0D) {
                    score += 50.0F;
                }
            }
            case HUNTING -> {
                if (isPestOrPrey(entity)) {
                    score += 70.0F;
                } else if (entity instanceof Monster) {
                    score += 35.0F;
                }
            }
            case TACTICAL -> {
                if (entity instanceof Monster) {
                    score += 50.0F;
                    // Prefer targets in line of sight at optimal ranged distance (8-20 blocks)
                    if (distSq >= 64.0D && distSq <= 400.0D) {
                        score += 25.0F;
                    }
                }
            }
            default -> {}
        }

        // 5. Pest Hunter Talent
        if (isPestOrPrey(entity) && (cat.getTalentLevel(Talents.PEST_HUNTER) > 0 || cat.getTalentLevel(Talents.MOUSER) > 0)) {
            score += 45.0F;
        }

        if (score <= 0.0F) return 0.0F;

        // 6. Distance factor: closer targets preferred
        float distFactor = (float) Math.max(0.0D, 30.0D - Math.sqrt(distSq));
        score += distFactor;

        // 7. Line of Sight bonus
        if (cat.getSensing().hasLineOfSight(entity)) {
            score += 15.0F;
        }

        // 8. Navigation validity: penalize target if over deep chasm/cliff
        if (!CatCliffSafetyHelper.isSafeToLeap(cat, entity.position())) {
            score -= 25.0F;
        }

        return Math.max(0.0F, score);
    }

    private boolean isPestOrPrey(LivingEntity entity) {
        return entity instanceof Bat || entity instanceof Rabbit ||
               entity.getType().toShortString().contains("spider") ||
               entity.getType().toShortString().contains("silverfish") ||
               entity.getType().toShortString().contains("endermite");
    }
}
