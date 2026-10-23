package com.kittytalents.inventory;

import com.kittytalents.block.KittyHabitatBlockEntity;
import com.kittytalents.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

public class KittyHabitatMenu extends AbstractContainerMenu {
    private final KittyHabitatBlockEntity habitat;
    private final ContainerLevelAccess access;

    public KittyHabitatMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, (KittyHabitatBlockEntity) playerInventory.player.level().getBlockEntity(buf.readBlockPos()));
    }

    public KittyHabitatMenu(int containerId, Inventory playerInventory, KittyHabitatBlockEntity habitat) {
        super(ModMenus.KITTY_HABITAT_MENU.get(), containerId);
        this.habitat = habitat;
        this.access = ContainerLevelAccess.create(habitat.getLevel(), habitat.getBlockPos());

        // 4 Upgrade Slots (0..3)
        for (int i = 0; i < 4; i++) {
            this.addSlot(new SlotItemHandler(habitat.getInventory(), i, 16 + i * 20, 94));
        }

        // 4 Output Slots (4..7)
        for (int i = 0; i < 4; i++) {
            this.addSlot(new SlotItemHandler(habitat.getInventory(), 4 + i, 108 + i * 20, 94));
        }

        // 2 Input Slots: 8 = Food Input, 9 = Catalyst Input
        this.addSlot(new SlotItemHandler(habitat.getInventory(), KittyHabitatBlockEntity.SLOT_FOOD_IN, 56, 66));
        this.addSlot(new SlotItemHandler(habitat.getInventory(), KittyHabitatBlockEntity.SLOT_CATALYST_IN, 76, 66));

        // Player Inventory (3 rows of 9)
        int startY = 126;
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

    public KittyHabitatBlockEntity getHabitat() {
        return habitat;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            itemstack = stackInSlot.copy();

            if (index < 10) { // Habitat slots (0..9)
                if (!this.moveItemStackTo(stackInSlot, 10, 46, true)) {
                    return ItemStack.EMPTY;
                }
            } else { // Player inventory
                if (!this.moveItemStackTo(stackInSlot, 0, 4, false) &&
                    !this.moveItemStackTo(stackInSlot, 8, 10, false)) {
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
        return stillValid(this.access, player, habitat.getBlockState().getBlock());
    }
}
