package com.kittytalents.inventory;

import com.kittytalents.block.TreatOvenBlockEntity;
import com.kittytalents.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class TreatOvenMenu extends AbstractContainerMenu {
    private final TreatOvenBlockEntity oven;
    private final ContainerLevelAccess access;

    public TreatOvenMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, (TreatOvenBlockEntity) playerInventory.player.level().getBlockEntity(buf.readBlockPos()));
    }

    public TreatOvenMenu(int containerId, Inventory playerInventory, TreatOvenBlockEntity oven) {
        super(ModMenus.TREAT_OVEN_MENU.get(), containerId);
        this.oven = oven;
        this.access = ContainerLevelAccess.create(oven.getLevel(), oven.getBlockPos());

        // 3 Ingredient Slots (0..2)
        for (int i = 0; i < 3; i++) {
            this.addSlot(new SlotItemHandler(oven.getInventory(), i, 30 + i * 20, 32));
        }

        // Fuel Slot (3)
        this.addSlot(new SlotItemHandler(oven.getInventory(), TreatOvenBlockEntity.SLOT_FUEL, 50, 72));

        // Output Slot (4)
        this.addSlot(new SlotItemHandler(oven.getInventory(), TreatOvenBlockEntity.SLOT_OUTPUT, 134, 50) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return false;
            }
        });

        // Player Inventory (3 rows of 9)
        int startY = 108;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 16 + col * 18, startY + row * 18));
            }
        }

        // Hotbar (9 slots)
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 16 + col * 18, startY + 58));
        }
    }

    public TreatOvenBlockEntity getOven() {
        return oven;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            itemstack = stackInSlot.copy();

            if (index < TreatOvenBlockEntity.TOTAL_SLOTS) {
                if (!this.moveItemStackTo(stackInSlot, TreatOvenBlockEntity.TOTAL_SLOTS, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (ForgeHooks.getBurnTime(stackInSlot, null) > 0) {
                    if (!this.moveItemStackTo(stackInSlot, TreatOvenBlockEntity.SLOT_FUEL, TreatOvenBlockEntity.SLOT_FUEL + 1, false) &&
                        !this.moveItemStackTo(stackInSlot, 0, 3, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(stackInSlot, 0, 3, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stackInSlot.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, oven.getBlockState().getBlock());
    }
}
