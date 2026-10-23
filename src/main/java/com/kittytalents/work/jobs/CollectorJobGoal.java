package com.kittytalents.work.jobs;

import com.kittytalents.entity.TalentedCat;
import com.kittytalents.work.AbstractCatJobGoal;
import com.kittytalents.work.CatJob;
import com.kittytalents.work.WorkInventoryHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CollectorJobGoal extends AbstractCatJobGoal {
    public CollectorJobGoal(TalentedCat cat) {
        super(cat, CatJob.COLLECTOR);
    }

    @Override
    protected int getBaseTaskDurationTicks() {
        return 6;
    }

    @Override
    protected double getInteractReachSqr() {
        return 6.25D;
    }

    @Override
    @Nullable
    protected BlockPos findNextTask(int scanBudget) {
        AABB box = AABB.of(getWorkArea());
        List<ItemEntity> items = cat.level().getEntitiesOfClass(ItemEntity.class, box, ie ->
                ie.isAlive() && !ie.getItem().isEmpty() && cat.getJobData().getFilter().matches(ie.getItem()));
        if (!items.isEmpty()) {
            return items.get(0).blockPosition();
        }
        return null;
    }

    @Override
    protected boolean isTaskStillValid(BlockPos pos) {
        return true;
    }

    @Override
    protected boolean executeTask(BlockPos pos) {
        AABB pickupBox = new AABB(pos).inflate(2.5D);
        List<ItemEntity> items = cat.level().getEntitiesOfClass(ItemEntity.class, pickupBox, ie ->
                ie.isAlive() && !ie.getItem().isEmpty() && cat.getJobData().getFilter().matches(ie.getItem()));
        if (items.isEmpty()) return false;
        for (ItemEntity ie : items) {
            WorkInventoryHelper.storeProducedItem(cat, ie.getItem().copy());
            ie.discard();
        }
        cat.playSound(SoundEvents.ITEM_PICKUP, 0.6F, 1.3F);
        return true;
    }
}
