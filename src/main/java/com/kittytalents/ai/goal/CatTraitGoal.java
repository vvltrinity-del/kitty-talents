package com.kittytalents.ai.goal;

import com.kittytalents.core.living.CatTrait;
import com.kittytalents.entity.CatMode;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.item.CatTreatItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

/**
 * Personality in idle time (4.0 Phase 6). One small, visible habit per {@link CatTrait}:
 * <ul>
 *   <li>Brave: stands and stares down the nearest monster.</li>
 *   <li>Lazy: flops down for a nap.</li>
 *   <li>Curious: trots over to inspect a dropped item.</li>
 *   <li>Playful: hops about on the spot.</li>
 *   <li>Shy: backs away from players it does not know.</li>
 *   <li>Greedy: runs to its owner and begs when they hold food.</li>
 * </ul>
 * IDLE band: anything more important (orders, combat, following, work) takes over at once.
 */
public class CatTraitGoal extends Goal {
    private final TalentedCat cat;
    private CatTrait acting = CatTrait.NONE;
    private LivingEntity focus;
    private ItemEntity item;
    private int ticks, duration;
    private long readyAt;
    /** Chance per check (every tick the goal is idle) to start a random habit; tests raise it. */
    private float chance = 0.004F;

    public CatTraitGoal(TalentedCat cat) {
        this.cat = cat;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    /** GameTests: make the random habits (nap, hop, inspect, stare) start right away. */
    public void setChanceForTests(float chance) {
        this.chance = chance;
        this.readyAt = 0;
    }

    private boolean free() {
        CatMode m = cat.getCatMode();
        return m != CatMode.WORK && m != CatMode.STAY && !cat.isTwined() && !cat.isIncapacitated() && !cat.isOrderedToSit()
                && !cat.isVehicle() && !cat.isPassenger() && cat.getTarget() == null;
    }

    private Player stranger() {
        double r = CatTrait.SHY_DISTANCE.get();
        // The level's player list, not an entity search: this runs on every canUse of every Shy cat.
        for (Player p : cat.level().players()) {
            if (!p.isSpectator() && p.isAlive() && cat.distanceToSqr(p) <= r * r && !cat.isOwnedBy(p) && !cat.isTrusted(p.getUUID())) return p;
        }
        return null;
    }

    private static boolean isCatFood(ItemStack stack) {
        return !stack.isEmpty() && (stack.getItem() instanceof CatTreatItem
                || stack.is(net.minecraft.tags.ItemTags.FISHES) || stack.is(net.minecraft.world.item.Items.COOKED_COD)
                || stack.is(net.minecraft.world.item.Items.COOKED_SALMON));
    }

    private Player ownerWithFood() {
        if (cat.getOwner() instanceof Player o && o.level() == cat.level() && cat.distanceToSqr(o) < 100.0D
                && (isCatFood(o.getMainHandItem()) || isCatFood(o.getOffhandItem()))) return o;
        return null;
    }

    @Override
    public boolean canUse() {
        CatTrait trait = cat.getTrait();
        if (trait == CatTrait.NONE || !free()) return false;
        // Reactions: no dice roll, they answer what is around the cat.
        if (trait == CatTrait.SHY) {
            focus = stranger();
            if (focus != null) return begin(trait, 60);
            return false;
        }
        if (trait == CatTrait.GREEDY) {
            focus = ownerWithFood();
            if (focus != null) return begin(trait, 100);
            return false;
        }
        if (cat.level().getGameTime() < readyAt || cat.getRandom().nextFloat() >= chance) return false;
        switch (trait) {
            case BRAVE -> {
                List<LivingEntity> foes = cat.level().getEntitiesOfClass(LivingEntity.class, cat.getBoundingBox().inflate(12.0D),
                        e -> e instanceof Enemy && e.isAlive());
                if (foes.isEmpty()) return false;
                foes.sort(java.util.Comparator.comparingDouble(cat::distanceToSqr));
                focus = foes.get(0);
                return begin(trait, 80);
            }
            case LAZY -> {
                return cat.onGround() && begin(trait, 240);
            }
            case CURIOUS -> {
                List<ItemEntity> items = cat.level().getEntitiesOfClass(ItemEntity.class, cat.getBoundingBox().inflate(8.0D, 3.0D, 8.0D), ItemEntity::isAlive);
                if (items.isEmpty()) return false;
                items.sort(java.util.Comparator.comparingDouble(cat::distanceToSqr));
                item = items.get(0);
                return begin(trait, 120);
            }
            case PLAYFUL -> {
                return cat.onGround() && begin(trait, 70);
            }
            default -> {
                return false;
            }
        }
    }

    private boolean begin(CatTrait trait, int ticksToRun) {
        acting = trait;
        duration = ticksToRun;
        return true;
    }

    @Override
    public void start() {
        ticks = 0;
        if (acting == CatTrait.LAZY) {
            cat.getNavigation().stop();
            cat.setLying(true);
        }
    }

    @Override
    public boolean canContinueToUse() {
        if (!free() || ticks >= duration || cat.getTrait() != acting) return false;
        return switch (acting) {
            case SHY -> focus != null && focus.isAlive() && cat.distanceToSqr(focus) < Math.pow(CatTrait.SHY_DISTANCE.get() + 2.0D, 2);
            case GREEDY -> ownerWithFood() == focus;
            case BRAVE -> focus != null && focus.isAlive();
            case CURIOUS -> item != null && item.isAlive();
            default -> true;
        };
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        ticks++;
        switch (acting) {
            case BRAVE -> {
                cat.getNavigation().stop();
                cat.getLookControl().setLookAt(focus, 30.0F, 30.0F);
                if (ticks == 1) cat.playSound(net.minecraft.sounds.SoundEvents.CAT_HISS, 0.6F, 0.9F);
            }
            case CURIOUS -> {
                cat.getLookControl().setLookAt(item, 30.0F, 30.0F);
                if (cat.distanceToSqr(item) > 2.25D) {
                    if (ticks % 10 == 1) cat.getNavigation().moveTo(item, 1.0D);
                } else {
                    cat.getNavigation().stop();
                }
            }
            case PLAYFUL -> {
                if (cat.onGround() && ticks % 14 == 1) {
                    cat.getJumpControl().jump();
                    cat.setYRot(cat.getYRot() + 70.0F);
                    cat.yBodyRot = cat.getYRot();
                    if (ticks == 1) cat.playSound(net.minecraft.sounds.SoundEvents.CAT_AMBIENT, 0.8F, 1.6F);
                }
            }
            case SHY -> {
                cat.getLookControl().setLookAt(focus, 30.0F, 30.0F);
                if (ticks % 10 == 1) {
                    Vec3 away = DefaultRandomPos.getPosAway(cat, 8, 4, focus.position());
                    if (away != null) cat.getNavigation().moveTo(away.x, away.y, away.z, 1.2D);
                }
            }
            case GREEDY -> {
                cat.getLookControl().setLookAt(focus, 30.0F, 30.0F);
                if (cat.distanceToSqr(focus) > 4.0D) {
                    if (ticks % 10 == 1) cat.getNavigation().moveTo(focus, 1.15D);
                } else {
                    cat.getNavigation().stop();
                    if (ticks % 40 == 5) cat.playSound(net.minecraft.sounds.SoundEvents.CAT_BEG_FOR_FOOD, 0.8F, 1.0F);
                }
            }
            default -> { }
        }
    }

    @Override
    public void stop() {
        if (acting == CatTrait.LAZY) cat.setLying(false);
        if (acting != CatTrait.SHY && acting != CatTrait.GREEDY) readyAt = cat.level().getGameTime() + 400;
        acting = CatTrait.NONE;
        focus = null;
        item = null;
    }

    /** The habit running right now (NONE when idle). */
    public CatTrait acting() {
        return acting;
    }
}
