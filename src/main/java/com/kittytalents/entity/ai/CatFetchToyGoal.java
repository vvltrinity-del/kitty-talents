package com.kittytalents.entity.ai;

import com.kittytalents.entity.CatMode;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.entity.ThrownToyEntity;
import com.kittytalents.talent.Talents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.List;

public class CatFetchToyGoal extends Goal {
    private final TalentedCat cat;
    private Entity targetEntity;
    private int fetchCooldown = 0;
    private int navCooldown = 0;

    public CatFetchToyGoal(TalentedCat cat) {
        this.cat = cat;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (cat.isTwined() || cat.isIncapacitated() || cat.isOrderedToSit() || cat.isSleeping()) return false;
        if (cat.getCatMode() == CatMode.DOCILE || cat.getCatMode() == CatMode.STAY) return false;
        if (fetchCooldown > 0) {
            fetchCooldown--;
            return false;
        }

        if (!cat.getCarriedToy().isEmpty()) return true;

        // Fetch thrown toys
        List<ThrownToyEntity> toys = cat.level().getEntitiesOfClass(
                ThrownToyEntity.class,
                new AABB(cat.blockPosition()).inflate(16.0D)
        );
        if (!toys.isEmpty()) {
            targetEntity = toys.get(0);
            return true;
        }

        // In FETCH mode or with PAW_RETRIEVER talent, fetch dropped items
        if (cat.getCatMode() == CatMode.FETCH || cat.getTalentLevel(Talents.PAW_RETRIEVER) > 0) {
            List<ItemEntity> dropped = cat.level().getEntitiesOfClass(
                    ItemEntity.class,
                    new AABB(cat.blockPosition()).inflate(12.0D),
                    i -> i.isAlive() && !i.getItem().isEmpty()
            );
            if (!dropped.isEmpty()) {
                targetEntity = dropped.get(0);
                return true;
            }
        }

        return false;
    }

    @Override
    public void start() {
        navCooldown = 0;
        if (targetEntity != null && cat.getCarriedToy().isEmpty()) {
            cat.getNavigation().moveTo(targetEntity, 1.45D);
        }
    }

    @Override
    public void tick() {
        ItemStack carried = cat.getCarriedToy();
        if (!carried.isEmpty()) {
            // Return item to owner!
            if (cat.getOwner() instanceof Player owner) {
                navCooldown++;
                if (navCooldown >= 15 || cat.getNavigation().isDone()) {
                    navCooldown = 0;
                    cat.getNavigation().moveTo(owner, 1.35D);
                }
                if (cat.distanceToSqr(owner) < 6.0D) {
                    // Drop item at owner's feet
                    cat.level().addFreshEntity(new ItemEntity(cat.level(), cat.getX(), cat.getY(), cat.getZ(), carried.copy()));
                    cat.playSound(SoundEvents.CAT_PURREOW, 1.0F, 1.2F);
                    if (cat.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.HEART, cat.getX(), cat.getY() + 0.5D, cat.getZ(), 2, 0.1D, 0.1D, 0.1D, 0.02D);
                    }
                    cat.setCarriedToy(ItemStack.EMPTY);
                    fetchCooldown = 100;
                }
            } else {
                cat.level().addFreshEntity(new ItemEntity(cat.level(), cat.getX(), cat.getY(), cat.getZ(), carried.copy()));
                cat.setCarriedToy(ItemStack.EMPTY);
            }
            return;
        }

        if (targetEntity == null || !targetEntity.isAlive()) return;

        navCooldown++;
        if (navCooldown >= 15 || cat.getNavigation().isDone()) {
            navCooldown = 0;
            cat.getNavigation().moveTo(targetEntity, 1.45D);
        }

        if (cat.distanceToSqr(targetEntity) < 3.0D) {
            // Pounce and grab!
            cat.playSound(SoundEvents.FOX_BITE, 1.0F, 1.2F);
            cat.playSound(SoundEvents.CAT_PURR, 1.0F, 1.0F);
            if (cat.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, cat.getX(), cat.getY() + 0.3D, cat.getZ(), 3, 0.1D, 0.1D, 0.1D, 0.02D);
            }

            if (targetEntity instanceof ThrownToyEntity toy) {
                cat.setCarriedToy(toy.getItem().copy());
                toy.discard();
            } else if (targetEntity instanceof ItemEntity itemEntity) {
                cat.setCarriedToy(itemEntity.getItem().copy());
                itemEntity.discard();
            }
            targetEntity = null;
        }
    }

    @Override
    public boolean canContinueToUse() {
        if (cat.isTwined() || cat.isIncapacitated() || cat.isOrderedToSit() || cat.isSleeping()) return false;
        return !cat.getCarriedToy().isEmpty() || (targetEntity != null && targetEntity.isAlive());
    }

    @Override
    public void stop() {
        targetEntity = null;
        navCooldown = 0;
    }
}
