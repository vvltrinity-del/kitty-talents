package com.kittytalents.work;

import com.kittytalents.compat.tacz.TacZCompat;
import com.kittytalents.entity.TalentedCat;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.NotNull;

/**
 * Exposes the cat's 9-slot ammoPouch (slots 0..8) followed by its 27-slot catPack (slots 9..35)
 * via ForgeCapabilities.ITEM_HANDLER so TacZ's native LivingEntity reload and ammo checks
 * seamlessly discover and consume ammunition from the cat.
 */
public class CatAmmoAndPackItemHandler implements IItemHandlerModifiable {
    private final TalentedCat cat;

    public CatAmmoAndPackItemHandler(TalentedCat cat) {
        this.cat = cat;
    }

    private SimpleContainer pouch() {
        return cat.getAmmoPouch();
    }

    private SimpleContainer pack() {
        return cat.getCatPack();
    }

    @Override
    public int getSlots() {
        return pouch().getContainerSize() + pack().getContainerSize();
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        int pSize = pouch().getContainerSize();
        if (slot < 0) return ItemStack.EMPTY;
        if (slot < pSize) return pouch().getItem(slot);
        int packIdx = slot - pSize;
        if (packIdx < pack().getContainerSize()) return pack().getItem(packIdx);
        return ItemStack.EMPTY;
    }

    @Override
    public void setStackInSlot(int slot, @NotNull ItemStack stack) {
        int pSize = pouch().getContainerSize();
        if (slot < 0) return;
        if (slot < pSize) {
            pouch().setItem(slot, stack);
            return;
        }
        int packIdx = slot - pSize;
        if (packIdx < pack().getContainerSize()) {
            pack().setItem(packIdx, stack);
        }
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        int pSize = pouch().getContainerSize();
        if (slot < 0 || slot >= getSlots()) return stack;
        if (slot < pSize && !TacZCompat.isAmmo(stack)) {
            return stack;
        }
        ItemStack existing = getStackInSlot(slot);
        int limit = Math.min(getSlotLimit(slot), stack.getMaxStackSize());
        if (!existing.isEmpty()) {
            if (!ItemStack.isSameItemSameTags(existing, stack)) return stack;
            limit -= existing.getCount();
        }
        if (limit <= 0) return stack;
        int toInsert = Math.min(stack.getCount(), limit);
        if (!simulate) {
            if (existing.isEmpty()) {
                ItemStack copy = stack.copy();
                copy.setCount(toInsert);
                setStackInSlot(slot, copy);
            } else {
                existing.grow(toInsert);
            }
        }
        if (toInsert == stack.getCount()) return ItemStack.EMPTY;
        ItemStack rem = stack.copy();
        rem.shrink(toInsert);
        return rem;
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount <= 0) return ItemStack.EMPTY;
        ItemStack existing = getStackInSlot(slot);
        if (existing.isEmpty()) return ItemStack.EMPTY;
        int toExtract = Math.min(amount, existing.getCount());
        if (simulate) {
            ItemStack copy = existing.copy();
            copy.setCount(toExtract);
            return copy;
        } else {
            int pSize = pouch().getContainerSize();
            if (slot < pSize) {
                return pouch().removeItem(slot, toExtract);
            } else {
                return pack().removeItem(slot - pSize, toExtract);
            }
        }
    }

    @Override
    public int getSlotLimit(int slot) {
        return 64;
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        if (slot < pouch().getContainerSize()) {
            return TacZCompat.isAmmo(stack);
        }
        return true;
    }
}
