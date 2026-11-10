package com.kittytalents.work.jobs;

import com.kittytalents.entity.TalentedCat;
import com.kittytalents.talent.Talents;
import com.kittytalents.work.AbstractCatJobGoal;
import com.kittytalents.work.CatJob;
import com.kittytalents.work.ItemFilter;
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

public class CourierJobGoal extends AbstractCatJobGoal {
    private boolean carryingToDestination = false;

    public CourierJobGoal(TalentedCat cat) {
        super(cat, CatJob.COURIER);
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

    public int getCarryCapacity() {
        int rank = Math.max(1, cat.getTalentLevel(Talents.KITTY_PACK));
        return Math.max(16, 8 * rank);
    }

    private List<BlockPos> getSources() {
        JobData data = cat.getJobData();
        if (!data.getLinkedInputs().isEmpty()) return data.getLinkedInputs();
        List<BlockPos> fallback = new ArrayList<>();
        if (data.getStationPos() != null) fallback.add(data.getStationPos());
        return fallback;
    }

    private List<BlockPos> getDestinations() {
        JobData data = cat.getJobData();
        if (!data.getLinkedOutputs().isEmpty()) return data.getLinkedOutputs();
        List<BlockPos> fallback = new ArrayList<>();
        if (data.getStationPos() != null) fallback.add(data.getStationPos());
        return fallback;
    }

    @Override
    @Nullable
    protected BlockPos findNextTask(int scanBudget) {
        ItemFilter filter = cat.getJobData().getFilter();

        // Check if cat already holds matching items to deliver
        boolean hasCargo = false;
        for (int i = 0; i < cat.getCatPack().getContainerSize(); i++) {
            ItemStack s = cat.getCatPack().getItem(i);
            if (!s.isEmpty() && filter.matches(s)) {
                hasCargo = true;
                break;
            }
        }

        if (hasCargo) {
            for (BlockPos dest : getDestinations()) {
                IItemHandler handler = WorkInventoryHelper.getHandlerAt(cat.level(), dest, Direction.UP);
                if (handler != null) {
                    carryingToDestination = true;
                    return dest;
                }
            }
        }

        // Otherwise find a source container with matching items
        for (BlockPos src : getSources()) {
            IItemHandler handler = WorkInventoryHelper.getHandlerAt(cat.level(), src, Direction.DOWN);
            if (handler == null) continue;
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack inSlot = handler.getStackInSlot(i);
                if (!inSlot.isEmpty() && filter.matches(inSlot) && !handler.extractItem(i, 1, true).isEmpty()) {
                    carryingToDestination = false;
                    return src;
                }
            }
        }
        return null;
    }

    @Override
    protected boolean isTaskStillValid(BlockPos pos) {
        return WorkInventoryHelper.getHandlerAt(cat.level(), pos, null) != null;
    }

    @Override
    protected boolean executeTask(BlockPos pos) {
        ItemFilter filter = cat.getJobData().getFilter();

        if (!carryingToDestination) {
            IItemHandler handler = WorkInventoryHelper.getHandlerAt(cat.level(), pos, Direction.DOWN);
            if (handler == null) return false;
            int remainingBudget = getCarryCapacity();
            boolean extractedAny = false;
            for (int i = 0; i < handler.getSlots() && remainingBudget > 0; i++) {
                ItemStack inSlot = handler.getStackInSlot(i);
                if (!inSlot.isEmpty() && filter.matches(inSlot)) {
                    ItemStack sim = handler.extractItem(i, remainingBudget, true);
                    if (!sim.isEmpty() && WorkInventoryHelper.hasRoomInCatPack(cat, sim)) {
                        ItemStack ext = handler.extractItem(i, remainingBudget, false);
                        remainingBudget -= ext.getCount();
                        ItemStack leftover = cat.getCatPack().addItem(ext);
                        if (!leftover.isEmpty()) {
                            WorkInventoryHelper.insertIntoHandler(handler, leftover, false);
                        }
                        extractedAny = true;
                    }
                }
            }
            if (extractedAny) {
                cat.playSound(SoundEvents.ITEM_PICKUP, 0.6F, 1.2F);
                carryingToDestination = true;
            }
            return extractedAny;
        } else {
            IItemHandler destHandler = WorkInventoryHelper.getHandlerAt(cat.level(), pos, Direction.UP);
            if (destHandler == null) return false;
            boolean insertedAny = false;
            for (int i = 0; i < cat.getCatPack().getContainerSize(); i++) {
                ItemStack s = cat.getCatPack().getItem(i);
                if (!s.isEmpty() && filter.matches(s)) {
                    ItemStack rem = WorkInventoryHelper.insertIntoHandler(destHandler, s.copy(), false);
                    if (rem.getCount() != s.getCount()) {
                        cat.getCatPack().setItem(i, rem);
                        insertedAny = true;
                    }
                }
            }
            if (insertedAny) {
                cat.playSound(SoundEvents.BUNDLE_DROP_CONTENTS, 0.7F, 1.1F);
            }
            carryingToDestination = false;
            return insertedAny;
        }
    }
}
