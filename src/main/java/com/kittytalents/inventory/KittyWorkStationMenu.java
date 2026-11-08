package com.kittytalents.inventory;

import com.kittytalents.block.KittyWorkStationBlockEntity;
import com.kittytalents.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

public class KittyWorkStationMenu extends AbstractContainerMenu {
    private final KittyWorkStationBlockEntity station;
    private final ContainerLevelAccess access;

    public KittyWorkStationMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, (KittyWorkStationBlockEntity) playerInventory.player.level().getBlockEntity(buf.readBlockPos()));
    }

    public KittyWorkStationMenu(int containerId, Inventory playerInventory, KittyWorkStationBlockEntity station) {
        super(ModMenus.KITTY_WORK_STATION_MENU.get(), containerId);
        this.station = station;
        this.access = ContainerLevelAccess.create(station.getLevel(), station.getBlockPos());

        // 18 Output Buffer Slots (0..17): 2 rows of 9 at y = 52, 70
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 9; col++) {
                int idx = row * 9 + col;
                this.addSlot(new SlotItemHandler(station.getInventory(), idx, 18 + col * 18, 53 + row * 18));
            }
        }

        // 4 Supply Input Slots (18..21): y = 98
        for (int i = 0; i < 4; i++) {
            this.addSlot(new SlotItemHandler(station.getInventory(), 18 + i, 18 + i * 18, 105));
        }

        // 2 Upgrade Slots (22..23): y = 98, right side
        for (int i = 0; i < 2; i++) {
            this.addSlot(new SlotItemHandler(station.getInventory(), 22 + i, 144 + i * 18, 105));
        }

        // Player Inventory (3 rows of 9)
        int startY = 146;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 18 + col * 18, startY + row * 18));
            }
        }

        // Hotbar (9 slots)
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 18 + col * 18, startY + 58));
        }
    }

    public KittyWorkStationBlockEntity getStation() {
        return station;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            itemstack = stackInSlot.copy();

            if (index < KittyWorkStationBlockEntity.TOTAL_SLOTS) {
                if (!this.moveItemStackTo(stackInSlot, KittyWorkStationBlockEntity.TOTAL_SLOTS, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Try upgrade slots (22..23) then supply slots (18..21) then buffer (0..17)
                if (!this.moveItemStackTo(stackInSlot, 22, 24, false) &&
                    !this.moveItemStackTo(stackInSlot, 18, 22, false) &&
                    !this.moveItemStackTo(stackInSlot, 0, 18, false)) {
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
        return stillValid(this.access, player, station.getBlockState().getBlock());
    }
}
