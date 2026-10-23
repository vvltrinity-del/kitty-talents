package com.kittytalents.work.jobs;

import com.kittytalents.config.KittyTalentsConfig;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.work.AbstractCatJobGoal;
import com.kittytalents.work.CatJob;
import com.kittytalents.work.WorkInventoryHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class RancherJobGoal extends AbstractCatJobGoal {
    @Nullable
    private Animal targetAnimal = null;

    public RancherJobGoal(TalentedCat cat) {
        super(cat, CatJob.RANCHER);
    }

    @Override
    protected int getBaseTaskDurationTicks() {
        return 25;
    }

    @Override
    protected double getInteractReachSqr() {
        return 9.0D;
    }

    @Override
    @Nullable
    protected BlockPos findNextTask(int scanBudget) {
        AABB box = AABB.of(getWorkArea());
        List<Animal> animals = cat.level().getEntitiesOfClass(Animal.class, box, a -> a != cat && !(a instanceof TalentedCat) && a.isAlive());

        // 1. Collect animal product drops (eggs, feathers, wool, leather, meat)
        List<ItemEntity> drops = cat.level().getEntitiesOfClass(ItemEntity.class, box, ItemEntity::isAlive);
        for (ItemEntity ie : drops) {
            if (cat.getJobData().getFilter().matches(ie.getItem())) {
                targetAnimal = null;
                return ie.blockPosition();
            }
        }

        // 2. Shear ready sheep
        for (Animal a : animals) {
            if (a instanceof Sheep sheep && sheep.readyForShearing()) {
                targetAnimal = sheep;
                return sheep.blockPosition();
            }
        }

        // Count species populations
        Map<EntityType<?>, List<Animal>> bySpecies = new HashMap<>();
        for (Animal a : animals) {
            bySpecies.computeIfAbsent(a.getType(), k -> new ArrayList<>()).add(a);
        }

        int cap = KittyTalentsConfig.COMMON.rancherAnimalCap.get();

        // 3. Breed animals below cap if feed is available
        for (Map.Entry<EntityType<?>, List<Animal>> entry : bySpecies.entrySet()) {
            List<Animal> list = entry.getValue();
            if (list.size() >= 2 && list.size() < cap) {
                for (Animal a : list) {
                    if (!a.isBaby() && a.getAge() == 0 && a.canFallInLove()) {
                        ItemStack feed = WorkInventoryHelper.extractMatchingSupply(cat, a::isFood, 1);
                        if (!feed.isEmpty()) {
                            // Put back until we reach the animal
                            WorkInventoryHelper.storeProducedItem(cat, feed);
                            targetAnimal = a;
                            return a.blockPosition();
                        }
                    }
                }
            }
        }

        // 4. Optional culling of surplus adult animals above cap
        if (KittyTalentsConfig.COMMON.rancherCulling.get()) {
            for (List<Animal> list : bySpecies.values()) {
                List<Animal> adults = list.stream().filter(a -> !a.isBaby()).toList();
                if (adults.size() > cap) {
                    targetAnimal = adults.get(0);
                    return targetAnimal.blockPosition();
                }
            }
        }

        return null;
    }

    @Override
    protected boolean isTaskStillValid(BlockPos pos) {
        if (targetAnimal != null) {
            return targetAnimal.isAlive();
        }
        return true;
    }

    @Override
    protected boolean executeTask(BlockPos pos) {
        if (!(cat.level() instanceof ServerLevel sl)) return false;

        // Pickup nearby drops first
        AABB pickupBox = new AABB(pos).inflate(2.5D);
        boolean pickedUp = false;
        for (ItemEntity ie : sl.getEntitiesOfClass(ItemEntity.class, pickupBox, ItemEntity::isAlive)) {
            if (cat.getJobData().getFilter().matches(ie.getItem())) {
                WorkInventoryHelper.storeProducedItem(cat, ie.getItem().copy());
                ie.discard();
                pickedUp = true;
            }
        }
        if (pickedUp && targetAnimal == null) return true;

        if (targetAnimal == null || !targetAnimal.isAlive()) return false;

        // Shear Sheep
        if (targetAnimal instanceof Sheep sheep && sheep.readyForShearing()) {
            List<ItemStack> woolDrops = sheep.onSheared(null, new ItemStack(Items.SHEARS), sl, sheep.blockPosition(), 0);
            sheep.setSheared(true);
            for (ItemStack w : woolDrops) {
                if (cat.getJobData().getFilter().matches(w)) {
                    WorkInventoryHelper.storeProducedItem(cat, w);
                }
            }
            sl.playSound(null, sheep, SoundEvents.SHEEP_SHEAR, SoundSource.PLAYERS, 1.0F, 1.0F);
            targetAnimal = null;
            return true;
        }

        // Breed Animal
        if (!targetAnimal.isBaby() && targetAnimal.getAge() == 0 && targetAnimal.canFallInLove()) {
            ItemStack feed = WorkInventoryHelper.extractMatchingSupply(cat, targetAnimal::isFood, 1);
            if (!feed.isEmpty()) {
                targetAnimal.setInLove(null);
                sl.playSound(null, targetAnimal, SoundEvents.GENERIC_EAT, SoundSource.NEUTRAL, 0.8F, 1.0F);
                targetAnimal = null;
                return true;
            }
        }

        // Cull surplus if enabled
        if (KittyTalentsConfig.COMMON.rancherCulling.get() && !targetAnimal.isBaby()) {
            targetAnimal.hurt(cat.damageSources().mobAttack(cat), 20.0F);
            targetAnimal = null;
            return true;
        }

        targetAnimal = null;
        return false;
    }
}
