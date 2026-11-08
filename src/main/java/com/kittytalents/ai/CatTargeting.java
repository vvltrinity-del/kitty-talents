package com.kittytalents.ai;

import com.kittytalents.entity.CatMode;
import com.kittytalents.entity.CatMode.TargetPolicy;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.entity.ai.CatCliffSafetyHelper;
import com.kittytalents.talent.Talents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.UUID;

/**
 * The single source of truth for "who may this cat fight / hurt" (Phase 3, R12).
 *
 * <p>Used by target selection ({@link com.kittytalents.ai.goal.CatTargetGoal}), area damage (pounce landings,
 * ability AoEs) and anything else that must not hit friends. Server-side only.</p>
 */
public final class CatTargeting {
    private CatTargeting() {}

    /** Recent-attack window in ticks for "X attacked Y" checks. */
    public static final int THREAT_WINDOW = 100;
    /** Minimum score for a candidate to become a target. */
    public static final float MIN_SCORE = 10.0F;
    /** Score bonus that keeps the current target unless something is clearly more urgent. */
    public static final float HYSTERESIS = 20.0F;

    // ------------------------------------------------------------------------------------------------------------
    // Friend / foe
    // ------------------------------------------------------------------------------------------------------------

    /** Never attacked or hit by area damage: the cat, its owner, trusted players, their pets, and allies. */
    public static boolean isFriendly(TalentedCat cat, LivingEntity e) {
        if (e == null || e == cat) return true;
        if (e instanceof ArmorStand) return true;
        LivingEntity owner = cat.getOwner();
        if (e == owner) return true;
        if (e instanceof Player p) {
            if (cat.isTrusted(p.getUUID()) || p.isCreative() || p.isSpectator()) return true;
        }
        if (e instanceof TamableAnimal pet && pet.isTame()) {
            UUID petOwner = pet.getOwnerUUID();
            if (petOwner != null && (petOwner.equals(cat.getOwnerUUID()) || cat.isTrusted(petOwner))) return true;
        }
        return cat.isAlliedTo(e);
    }

    /** {@code attacker} hurt {@code victim} within the threat window (uses the VICTIM's own clock). */
    public static boolean recentlyHurtBy(LivingEntity victim, LivingEntity attacker) {
        return victim != null && attacker != null && victim.getLastHurtByMob() == attacker
                && victim.tickCount - victim.getLastHurtByMobTimestamp() < THREAT_WINDOW;
    }

    /** {@code victim} recently attacked {@code target} (uses the VICTIM's own clock). */
    public static boolean recentlyAttacked(LivingEntity victim, LivingEntity target) {
        return victim != null && target != null && victim.getLastHurtMob() == target
                && victim.tickCount - victim.getLastHurtMobTimestamp() < THREAT_WINDOW;
    }

    /** True for a mob actively hunting {@code who}. */
    public static boolean isTargeting(LivingEntity e, LivingEntity who) {
        return who != null && e instanceof Mob mob && mob.getTarget() == who;
    }

    /**
     * May this cat's area damage (pounce landing, AoE abilities) hit {@code e}? Hostiles, the cat's current target,
     * and anything that is actively fighting the cat or its owner. Never friends or bystanders (villagers, animals).
     */
    public static boolean canHarm(TalentedCat cat, LivingEntity e) {
        if (e == null || !e.isAlive() || isFriendly(cat, e)) return false;
        if (e == cat.getTarget()) return true;
        LivingEntity owner = cat.getOwner();
        if (isTargeting(e, cat) || isTargeting(e, owner)) return true;
        if (recentlyHurtBy(cat, e) || recentlyHurtBy(owner, e)) return true;
        if (e instanceof NeutralMob neutral && !neutral.isAngryAt(cat) && (owner == null || !neutral.isAngryAt(owner))) {
            return false; // don't drag endermen / piglins / golems into a fight
        }
        return e instanceof Enemy;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Target selection
    // ------------------------------------------------------------------------------------------------------------

    public static boolean isPest(LivingEntity e) {
        return e instanceof Silverfish || e instanceof Endermite || e instanceof Spider;
    }

    public static boolean isPrey(LivingEntity e) {
        return e instanceof Rabbit || e instanceof Chicken || e instanceof AbstractFish || e instanceof Bat;
    }

    /** Scan radius for candidate targets under this cat's current mode. */
    public static double scanRadius(TalentedCat cat) {
        CatMode mode = cat.getCatMode();
        double r = switch (mode.targetPolicy()) {
            case NONE -> 0.0D;
            case SELF_DEFENSE -> 12.0D;
            case DEFEND_OWNER, AREA_DEFENSE -> 16.0D;
            case HUNT -> 16.0D;
            case SEEK_HOSTILES -> 24.0D;
        };
        if (cat.hasEquippedGun() && r > 0) r = Math.max(r, 24.0D);
        return r;
    }

    /** Best target for the cat right now, or null. Keeps the current target unless a candidate beats it clearly. */
    public static LivingEntity findBestTarget(TalentedCat cat) {
        double radius = scanRadius(cat);
        if (radius <= 0) return null;

        List<LivingEntity> candidates = cat.level().getEntitiesOfClass(LivingEntity.class,
                cat.getBoundingBox().inflate(radius, 6.0D, radius), e -> e.isAlive() && !isFriendly(cat, e));
        List<Player> trusted = cat.level().getEntitiesOfClass(Player.class, cat.getBoundingBox().inflate(16.0D),
                p -> cat.isTrusted(p.getUUID()));

        LivingEntity current = cat.getTarget();
        LivingEntity best = null;
        float bestScore = 0.0F;
        if (current != null && current.isAlive() && !isFriendly(cat, current)) {
            float s = score(cat, current, trusted);
            if (s > 0) {
                best = current;
                bestScore = s + HYSTERESIS;
            }
        }
        for (LivingEntity c : candidates) {
            if (c == current) continue;
            float s = score(cat, c, trusted);
            if (s > bestScore) {
                bestScore = s;
                best = c;
            }
        }
        return bestScore > MIN_SCORE ? best : null;
    }

    /** Threat/priority score of {@code e} for this cat (0 = not a valid target). */
    public static float score(TalentedCat cat, LivingEntity e, List<Player> nearbyTrusted) {
        if (e == null || !e.isAlive() || isFriendly(cat, e)) return 0.0F;
        CatMode mode = cat.getCatMode();
        TargetPolicy policy = mode.targetPolicy();
        if (policy == TargetPolicy.NONE) return 0.0F;

        // Melee-attacking a creeper is suicide; only a gun-wielding cat engages one.
        if (e instanceof Creeper creeper && (creeper.isIgnited() || creeper.getSwellDir() > 0 || !cat.hasEquippedGun())) {
            return 0.0F;
        }

        LivingEntity owner = cat.getOwner();
        double distSq = cat.distanceToSqr(e);
        float score = 0.0F;

        // --- Direct threats (every policy that fights) ---
        if (recentlyHurtBy(cat, e) || isTargeting(e, cat)) score += 75.0F;

        boolean ownerNear = owner != null && cat.distanceToSqr(owner) < 36.0D;
        if (owner != null && (policy != TargetPolicy.SELF_DEFENSE || ownerNear)) {
            if (recentlyHurtBy(owner, e)) score += 120.0F;
            if (recentlyAttacked(owner, e) && policy != TargetPolicy.SELF_DEFENSE) score += 90.0F;
            if (isTargeting(e, owner)) score += 80.0F;
        }
        if (policy != TargetPolicy.SELF_DEFENSE) {
            for (Player t : nearbyTrusted) {
                if (recentlyHurtBy(t, e) || isTargeting(e, t)) {
                    score += 85.0F;
                    break;
                }
            }
        }

        // --- Proactive targets by policy ---
        boolean hostile = e instanceof Enemy && !(e instanceof NeutralMob n && !n.isAngryAt(cat)
                && (owner == null || !n.isAngryAt(owner)));
        switch (policy) {
            case AREA_DEFENSE -> {
                BlockPos anchor = cat.getModeAnchor();
                double reach = mode.anchorRadius() + 6.0D;
                if (hostile && anchor != null && e.distanceToSqr(anchor.getX() + 0.5D, anchor.getY(), anchor.getZ() + 0.5D) <= reach * reach) {
                    score += 65.0F;
                }
            }
            case HUNT -> {
                if (isPrey(e) || isPest(e)) score += 70.0F;
                else if (hostile && distSq <= 144.0D) score += 35.0F;
            }
            case SEEK_HOSTILES -> {
                if (hostile) score += 60.0F;
            }
            default -> {}
        }

        // Pest control talents: pests are fair game in any mode that defends (not mere self-defense).
        if (isPest(e) && policy != TargetPolicy.SELF_DEFENSE
                && (cat.getTalentLevel(Talents.PEST_HUNTER) > 0 || cat.getTalentLevel(Talents.MOUSER) > 0)) {
            score += 45.0F;
        }

        if (score <= 0.0F) return 0.0F;

        score += (float) Math.max(0.0D, 30.0D - Math.sqrt(distSq));          // closer is better
        if (cat.getSensing().hasLineOfSight(e)) score += 15.0F;               // visible is better
        if (!CatCliffSafetyHelper.isSafeToLeap(cat, e.position())) score -= 25.0F; // don't chase over cliffs
        return Math.max(0.0F, score);
    }
}
