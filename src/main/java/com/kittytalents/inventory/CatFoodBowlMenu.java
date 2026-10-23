package com.kittytalents.inventory;

import com.kittytalents.block.CatFoodBowlBlockEntity;
import com.kittytalents.registry.ModMenus;
import com.kittytalents.util.CatFoodHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

public class CatFoodBowlMenu extends AbstractContainerMenu {
    private final CatFoodBowlBlockEntity blockEntity;
    private final ContainerLevelAccess levelAccess;
    private final ContainerData data;

    public CatFoodBowlMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, getBlockEntity(playerInventory, extraData), new SimpleContainerData(3));
    }

    public CatFoodBowlMenu(int containerId, Inventory playerInventory, CatFoodBowlBlockEntity blockEntity, ContainerData data) {
        super(ModMenus.CAT_FOOD_BOWL_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        this.data = data;
        this.levelAccess = blockEntity != null ? ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()) : ContainerLevelAccess.NULL;

        addDataSlots(data);

        ItemStackHandler handler = blockEntity != null ? blockEntity.getInventory() : new ItemStackHandler(5);

        // 5 Food Bowl Slots centered horizontally
        int startX = 44;
        int slotY = 36;
        for (int i = 0; i < 5; i++) {
            this.addSlot(new SlotItemHandler(handler, i, startX + i * 18, slotY) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return CatFoodHelper.isEdibleByCat(stack, null);
                }
            });
        }

        // Player Inventory (3 rows x 9 columns)
        int playerInvY = 84;
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, playerInvY + row * 18));
            }
        }

        // Player Hotbar
        int hotbarY = 142;
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, hotbarY));
        }
    }

    private static CatFoodBowlBlockEntity getBlockEntity(Inventory playerInventory, FriendlyByteBuf extraData) {
        if (extraData != null) {
            BlockPos pos = extraData.readBlockPos();
            BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
            if (be instanceof CatFoodBowlBlockEntity bowl) {
                return bowl;
            }
        }
        return null;
    }

    public CatFoodBowlBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public int getNearbyCatsCount() {
        return this.data.get(0);
    }

    public int getTotalFoodCount() {
        return this.data.get(1);
    }

    public int getFeedCooldown() {
        return this.data.get(2);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            itemstack = stackInSlot.copy();

            // From Bowl (slots 0..4) to Player inventory (slots 5..40)
            if (index < 5) {
                if (!this.moveItemStackTo(stackInSlot, 5, 41, true)) {
                    return ItemStack.EMPTY;
                }
            }
            // From Player inventory to Bowl
            else {
                if (CatFoodHelper.isEdibleByCat(stackInSlot, null)) {
                    if (!this.moveItemStackTo(stackInSlot, 0, 5, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index < 32) { // Main player inv to hotbar
                    if (!this.moveItemStackTo(stackInSlot, 32, 41, false)) {
                        return ItemStack.EMPTY;
                    }
                } else { // Hotbar to main inv
                    if (!this.moveItemStackTo(stackInSlot, 5, 32, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (stackInSlot.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stackInSlot.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, stackInSlot);
        }

        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.levelAccess, player, com.kittytalents.registry.ModBlocks.CAT_FOOD_BOWL.get());
    }
}
