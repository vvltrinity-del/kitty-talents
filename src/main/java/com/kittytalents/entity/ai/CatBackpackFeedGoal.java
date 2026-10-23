package com.kittytalents.entity.ai;

import com.kittytalents.entity.TalentedCat;
import com.kittytalents.util.CatFoodHelper;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;

/**
 * Feature 39: Backpack Auto-Feeding Goal.
 * Periodically inspects catPack inventory and automatically consumes food when hunger < 60% or health < 100%.
 */
public class CatBackpackFeedGoal extends Goal {
    private final TalentedCat cat;
    private int feedCooldown = 40;

    public CatBackpackFeedGoal(TalentedCat cat) {
        this.cat = cat;
    }

    @Override
    public boolean canUse() {
        if (cat.isTwined() || cat.isIncapacitated() || cat.isSleeping()) return false;
        if (feedCooldown > 0) {
            feedCooldown--;
            return false;
        }
        feedCooldown = 40; // Check every 2 seconds

        // Hunger < 60% (72/120) or injured
        boolean needsFood = cat.getHunger() < 72 || cat.getHealth() < cat.getMaxHealth();
        if (!needsFood) return false;

        SimpleContainer pack = cat.getCatPack();
        for (int i = 0; i < pack.getContainerSize(); i++) {
            ItemStack stack = pack.getItem(i);
            if (!stack.isEmpty() && CatFoodHelper.isEdibleByCat(stack, cat)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void start() {
        SimpleContainer pack = cat.getCatPack();
        for (int i = 0; i < pack.getContainerSize(); i++) {
            ItemStack stack = pack.getItem(i);
            if (!stack.isEmpty() && CatFoodHelper.isEdibleByCat(stack, cat)) {
                CatFoodHelper.CatFoodValue val = CatFoodHelper.getFoodValues(stack, cat);
                ItemStack consumedCopy = stack.copy();

                stack.shrink(1);
                if (stack.isEmpty()) {
                    pack.setItem(i, ItemStack.EMPTY);
                }

                cat.applyFood(val.hunger, val.saturation, val.heal);
                cat.playSound(SoundEvents.CAT_EAT, 0.7F, 1.0F);

                if (cat.level() instanceof ServerLevel sl) {
                    sl.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, consumedCopy),
                            cat.getX(), cat.getY() + 0.3D, cat.getZ(), 4, 0.15D, 0.1D, 0.15D, 0.05D);
                }
                break;
            }
        }
        feedCooldown = 100; // 5 seconds between meals
    }
}
