package com.kittytalents.work;

import com.kittytalents.entity.TalentedCat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public final class WorkInventoryHelper {
    private WorkInventoryHelper() {}

    @Nullable
    public static IItemHandler getHandlerAt(Level level, @Nullable BlockPos pos, @Nullable Direction side) {
        if (level == null || pos == null || !level.isLoaded(pos)) return null;
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) return null;
        LazyOptional<IItemHandler> cap = be.getCapability(ForgeCapabilities.ITEM_HANDLER, side);
        if (cap.isPresent()) {
            return cap.orElse(null);
        }
        if (side != null) {
            return be.getCapability(ForgeCapabilities.ITEM_HANDLER, null).orElse(null);
        }
        return null;
    }

    public static ItemStack insertIntoHandler(@Nullable IItemHandler handler, ItemStack stack, boolean simulate) {
        if (handler == null || stack.isEmpty()) return stack;
        return ItemHandlerHelper.insertItemStacked(handler, stack, simulate);
    }

    public static ItemStack insertIntoPos(Level level, @Nullable BlockPos pos, ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || pos == null) return stack;
        // Try null side first (internal buffer access for Work Station), then UP/NORTH
        IItemHandler handler = getHandlerAt(level, pos, null);
        if (handler == null) handler = getHandlerAt(level, pos, Direction.UP);
        return insertIntoHandler(handler, stack, simulate);
    }

    public static boolean hasRoomInCatPack(TalentedCat cat, ItemStack probe) {
        SimpleContainer pack = cat.getCatPack();
        for (int i = 0; i < pack.getContainerSize(); i++) {
            ItemStack s = pack.getItem(i);
            if (s.isEmpty()) return true;
            if (!probe.isEmpty() && ItemStack.isSameItemSameTags(s, probe) && s.getCount() < s.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    public static boolean isCatPackNearlyFull(TalentedCat cat) {
        SimpleContainer pack = cat.getCatPack();
        int empty = 0;
        for (int i = 0; i < pack.getContainerSize(); i++) {
            if (pack.getItem(i).isEmpty()) empty++;
        }
        return empty <= 2;
    }

    /**
     * Inserts an item into the cat's pack first, then into the bound station or linked outputs,
     * and finally drops at the cat's feet if all buffers are full.
     */
    public static void storeProducedItem(TalentedCat cat, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        ItemStack rem = cat.getCatPack().addItem(stack.copy());
        if (rem.isEmpty()) return;

        JobData data = cat.getJobData();
        if (data.getStationPos() != null) {
            rem = insertIntoPos(cat.level(), data.getStationPos(), rem, false);
            if (rem.isEmpty()) return;
        }
        for (BlockPos outPos : data.getLinkedOutputs()) {
            rem = insertIntoPos(cat.level(), outPos, rem, false);
            if (rem.isEmpty()) return;
        }
        if (!rem.isEmpty() && !cat.level().isClientSide) {
            Containers.dropItemStack(cat.level(), cat.getX(), cat.getY(), cat.getZ(), rem);
        }
    }

    /**
     * Pulls an item matching predicate from the cat's pack, or from the bound station/linked inputs.
     */
    public static ItemStack extractMatchingSupply(TalentedCat cat, Predicate<ItemStack> predicate, int maxCount) {
        SimpleContainer pack = cat.getCatPack();
        for (int i = 0; i < pack.getContainerSize(); i++) {
            ItemStack s = pack.getItem(i);
            if (!s.isEmpty() && predicate.test(s)) {
                return pack.removeItem(i, Math.min(maxCount, s.getCount()));
            }
        }
        JobData data = cat.getJobData();
        if (data.getStationPos() != null) {
            ItemStack fromStation = extractFromPos(cat.level(), data.getStationPos(), predicate, maxCount);
            if (!fromStation.isEmpty()) return fromStation;
        }
        for (BlockPos inPos : data.getLinkedInputs()) {
            ItemStack fromInput = extractFromPos(cat.level(), inPos, predicate, maxCount);
            if (!fromInput.isEmpty()) return fromInput;
        }
        return ItemStack.EMPTY;
    }

    /**
     * Pulls up to maxCount items matching predicate from bound station or linked inputs into catPack.
     */
    public static int pullToCatPack(TalentedCat cat, Predicate<ItemStack> predicate, int maxCount) {
        if (maxCount <= 0) return 0;
        int pulledTotal = 0;
        JobData data = cat.getJobData();
        if (data.getStationPos() != null) {
            while (pulledTotal < maxCount) {
                ItemStack fromStation = extractFromPos(cat.level(), data.getStationPos(), predicate, maxCount - pulledTotal);
                if (fromStation.isEmpty()) break;
                pulledTotal += fromStation.getCount();
                ItemStack rem = cat.getCatPack().addItem(fromStation);
                if (!rem.isEmpty()) {
                    insertIntoPos(cat.level(), data.getStationPos(), rem, false);
                    pulledTotal -= rem.getCount();
                    break;
                }
            }
        }
        for (BlockPos inPos : data.getLinkedInputs()) {
            while (pulledTotal < maxCount) {
                ItemStack fromInput = extractFromPos(cat.level(), inPos, predicate, maxCount - pulledTotal);
                if (fromInput.isEmpty()) break;
                pulledTotal += fromInput.getCount();
                ItemStack rem = cat.getCatPack().addItem(fromInput);
                if (!rem.isEmpty()) {
                    insertIntoPos(cat.level(), inPos, rem, false);
                    pulledTotal -= rem.getCount();
                    break;
                }
            }
        }
        return pulledTotal;
    }

    public static ItemStack extractFromPos(Level level, BlockPos pos, Predicate<ItemStack> predicate, int maxCount) {
        IItemHandler handler = getHandlerAt(level, pos, null);
        if (handler == null) handler = getHandlerAt(level, pos, Direction.DOWN);
        if (handler == null) return ItemStack.EMPTY;
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack inSlot = handler.getStackInSlot(i);
            if (!inSlot.isEmpty() && predicate.test(inSlot)) {
                ItemStack extracted = handler.extractItem(i, maxCount, false);
                if (!extracted.isEmpty()) return extracted;
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * Deposits items from the cat's pack into the bound station buffer or linked outputs.
     * Returns true if at least one item was deposited or the pack had nothing to deposit.
     */
    public static boolean depositPackToStationOrOutputs(TalentedCat cat, Predicate<ItemStack> keepInPack) {
        SimpleContainer pack = cat.getCatPack();
        JobData data = cat.getJobData();
        boolean hadItems = false;
        boolean movedAny = false;

        for (int i = 0; i < pack.getContainerSize(); i++) {
            ItemStack s = pack.getItem(i);
            if (s.isEmpty()) continue;
            if (keepInPack != null && keepInPack.test(s)) continue;

            hadItems = true;
            ItemStack rem = s.copy();
            if (data.getStationPos() != null) {
                rem = insertIntoPos(cat.level(), data.getStationPos(), rem, false);
            }
            if (!rem.isEmpty()) {
                for (BlockPos out : data.getLinkedOutputs()) {
                    rem = insertIntoPos(cat.level(), out, rem, false);
                    if (rem.isEmpty()) break;
                }
            }
            if (rem.getCount() != s.getCount()) {
                movedAny = true;
                pack.setItem(i, rem);
            }
        }
        return !hadItems || movedAny;
    }
}
