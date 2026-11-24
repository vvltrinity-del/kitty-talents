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
    private final net.minecraft.world.inventory.ContainerData data;

    /** Screen size; slot positions below are laid out for it (see KittyHabitatScreen). */
    public static final int WIDTH = 232, HEIGHT = 234;
    public static final int BUTTON_DISMISS = 0, BUTTON_PAUSE = 1;

    public KittyHabitatMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, (KittyHabitatBlockEntity) playerInventory.player.level().getBlockEntity(buf.readBlockPos()),
                new net.minecraft.world.inventory.SimpleContainerData(KittyHabitatBlockEntity.DATA_COUNT));
    }

    public KittyHabitatMenu(int containerId, Inventory playerInventory, KittyHabitatBlockEntity habitat) {
        this(containerId, playerInventory, habitat, habitat.dataAccess);
    }

    private KittyHabitatMenu(int containerId, Inventory playerInventory, KittyHabitatBlockEntity habitat,
                             net.minecraft.world.inventory.ContainerData data) {
        super(ModMenus.KITTY_HABITAT_MENU.get(), containerId);
        this.habitat = habitat;
        this.data = data;
        this.access = ContainerLevelAccess.create(habitat.getLevel(), habitat.getBlockPos());
        addDataSlots(data);

        // 4 Upgrade slots (0..3), left panel
        for (int i = 0; i < 4; i++) {
            this.addSlot(new SlotItemHandler(habitat.getInventory(), i, 14 + i * 20, 84));
        }
        // 4 Output slots (4..7), right panel
        for (int i = 0; i < 4; i++) {
            this.addSlot(new SlotItemHandler(habitat.getInventory(), 4 + i, 130 + i * 20, 103));
        }
        // Supplies: 8 = food, 9 = catalyst, left panel
        this.addSlot(new SlotItemHandler(habitat.getInventory(), KittyHabitatBlockEntity.SLOT_FOOD_IN, 14, 106));
        this.addSlot(new SlotItemHandler(habitat.getInventory(), KittyHabitatBlockEntity.SLOT_CATALYST_IN, 34, 106));

        // Player inventory and hotbar, centred
        int startX = (WIDTH - 162) / 2 + 1, startY = 147;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, startX + col * 18, startY + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, startX + col * 18, startY + 58));
        }
    }

    public KittyHabitatBlockEntity getHabitat() {
        return habitat;
    }

    // Live values (synced every tick while open, like a furnace).
    public int getProgress() { return data.get(0); }
    public int getMaxProgress() { return Math.max(1, data.get(1)); }
    public int getTotalCycles() { return data.get(2); }
    public boolean isPaused() { return data.get(3) != 0; }
    /** Stored FE (5.1.1), for the energy bar. */
    public int getEnergy() { return Math.max(0, data.get(5)); }
    public KittyHabitatBlockEntity.Status getStatus() {
        int i = data.get(4);
        var all = KittyHabitatBlockEntity.Status.values();
        return all[Math.max(0, Math.min(all.length - 1, i))];
    }

    /** Server side of the screen buttons (the screen used to change only its client copy, so nothing happened). */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_DISMISS) {
            habitat.dismissWorker();
            return true;
        }
        if (id == BUTTON_PAUSE) {
            habitat.setPaused(!habitat.isPaused());
            return true;
        }
        return false;
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
