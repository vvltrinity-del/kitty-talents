package com.kittytalents.work.jobs;

import com.kittytalents.entity.TalentedCat;
import com.kittytalents.work.AbstractCatJobGoal;
import com.kittytalents.work.CatJob;
import com.kittytalents.work.JobData;
import com.kittytalents.work.WorkInventoryHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * SorterJobGoal: Pulls items from station or linkedInputs and deposits each stack into
 * a linkedOutput chest that already contains that item type, falling back to the last
 * linkedOutput chest as overflow.
 */
public class SorterJobGoal extends AbstractCatJobGoal {
    public SorterJobGoal(TalentedCat cat) {
        super(cat, CatJob.SORTER);
    }

    @Override
    protected int getBaseTaskDurationTicks() {
        return 12;
    }

    @Override
    protected boolean needsClearReach() {
        return true;
    }

    @Override
    protected double getInteractReachSqr() {
        return 9.0D;
    }

    private List<BlockPos> getSources() {
        JobData data = cat.getJobData();
        List<BlockPos> sources = new ArrayList<>(data.getLinkedInputs());
        if (data.getStationPos() != null && !sources.contains(data.getStationPos())) {
            sources.add(data.getStationPos());
        }
        return sources;
    }

    @Override
    @Nullable
    protected BlockPos findNextTask(int scanBudget) {
        JobData data = cat.getJobData();
        if (data.getLinkedOutputs().isEmpty()) return null;

        // If cat is holding items, find the matching destination chest
        for (int i = 0; i < cat.getCatPack().getContainerSize(); i++) {
            ItemStack held = cat.getCatPack().getItem(i);
            if (!held.isEmpty()) {
                BlockPos dest = findMatchingDestination(held);
                if (dest != null) return dest;
            }
        }

        // Otherwise pull from a source container
        for (BlockPos src : getSources()) {
            IItemHandler handler = WorkInventoryHelper.getHandlerAt(cat.level(), src, Direction.DOWN);
            if (handler == null) continue;
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack s = handler.getStackInSlot(i);
                if (!s.isEmpty() && data.getFilter().matches(s) && findMatchingDestination(s) != null) {
                    return src;
                }
            }
        }
        return null;
    }

    @Nullable
    public BlockPos findMatchingDestination(ItemStack stack) {
        List<BlockPos> outputs = cat.getJobData().getLinkedOutputs();
        if (outputs.isEmpty()) return null;

        // Pass 1: Chest that already has this item and has room
        for (BlockPos out : outputs) {
            IItemHandler handler = WorkInventoryHelper.getHandlerAt(cat.level(), out, Direction.UP);
            if (handler == null) continue;
            boolean hasMatching = false;
            for (int i = 0; i < handler.getSlots(); i++) {
                if (ItemStack.isSameItem(handler.getStackInSlot(i), stack)) {
                    hasMatching = true;
                    break;
                }
            }
            if (hasMatching && WorkInventoryHelper.insertIntoHandler(handler, stack, true).getCount() < stack.getCount()) {
                return out;
            }
        }

        // Pass 2: Last linked output acts as overflow chest
        BlockPos overflow = outputs.get(outputs.size() - 1);
        IItemHandler overflowHandler = WorkInventoryHelper.getHandlerAt(cat.level(), overflow, Direction.UP);
        if (overflowHandler != null && WorkInventoryHelper.insertIntoHandler(overflowHandler, stack, true).getCount() < stack.getCount()) {
            return overflow;
        }
        return null;
    }

    @Override
    protected boolean isTaskStillValid(BlockPos pos) {
        return WorkInventoryHelper.getHandlerAt(cat.level(), pos, null) != null;
    }

    @Override
    protected boolean executeTask(BlockPos pos) {
        JobData data = cat.getJobData();
        // If pos is a destination output chest, deposit matching items into it
        if (data.getLinkedOutputs().contains(pos)) {
            IItemHandler dest = WorkInventoryHelper.getHandlerAt(cat.level(), pos, Direction.UP);
            if (dest == null) return false;
            boolean moved = false;
            for (int i = 0; i < cat.getCatPack().getContainerSize(); i++) {
                ItemStack held = cat.getCatPack().getItem(i);
                if (!held.isEmpty() && pos.equals(findMatchingDestination(held))) {
                    ItemStack rem = WorkInventoryHelper.insertIntoHandler(dest, held.copy(), false);
                    if (rem.getCount() != held.getCount()) {
                        cat.getCatPack().setItem(i, rem);
                        moved = true;
                    }
                }
            }
            if (moved) cat.playSound(SoundEvents.BUNDLE_DROP_CONTENTS, 0.6F, 1.2F);
            return moved;
        }

        // Otherwise pos is a source container: extract 1 stack into catPack
        IItemHandler src = WorkInventoryHelper.getHandlerAt(cat.level(), pos, Direction.DOWN);
        if (src == null) return false;
        for (int i = 0; i < src.getSlots(); i++) {
            ItemStack inSlot = src.getStackInSlot(i);
            if (!inSlot.isEmpty() && data.getFilter().matches(inSlot) && findMatchingDestination(inSlot) != null) {
                ItemStack extracted = src.extractItem(i, inSlot.getMaxStackSize(), false);
                if (!extracted.isEmpty()) {
                    ItemStack rem = cat.getCatPack().addItem(extracted);
                    if (!rem.isEmpty()) {
                        WorkInventoryHelper.insertIntoHandler(src, rem, false);
                    }
                    cat.playSound(SoundEvents.ITEM_PICKUP, 0.6F, 1.1F);
                    return true;
                }
            }
        }
        return false;
    }
}
