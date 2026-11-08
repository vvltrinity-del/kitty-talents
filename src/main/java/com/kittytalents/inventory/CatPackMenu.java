package com.kittytalents.inventory;

import com.kittytalents.entity.TalentedCat;
import com.kittytalents.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class CatPackMenu extends AbstractContainerMenu {
    private final TalentedCat cat;
    private final Container catContainer;
    private final int numRows;

    public CatPackMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, getCatFromBuf(playerInventory.player.level(), extraData));
    }

    public CatPackMenu(int containerId, Inventory playerInventory, TalentedCat cat) {
        super(ModMenus.CAT_PACK_MENU.get(), containerId);
        this.cat = cat;
        this.catContainer = cat.getCatPack();
        this.numRows = Math.max(1, Math.min(3, cat.getPackSlotCount() / 9));

        // Cat inventory slots
        for (int r = 0; r < this.numRows; ++r) {
            for (int c = 0; c < 9; ++c) {
                this.addSlot(new Slot(this.catContainer, c + r * 9, 12 + c * 18, 31 + r * 18));
            }
        }

        // Player Inventory
        int startY = 31 + this.numRows * 18 + 26;
        for (int l = 0; l < 3; ++l) {
            for (int k = 0; k < 9; ++k) {
                this.addSlot(new Slot(playerInventory, k + l * 9 + 9, 12 + k * 18, startY + l * 18));
            }
        }

        // Player Hotbar
        for (int i1 = 0; i1 < 9; ++i1) {
            this.addSlot(new Slot(playerInventory, i1, 12 + i1 * 18, startY + 58));
        }
    }

    private static TalentedCat getCatFromBuf(Level level, FriendlyByteBuf buf) {
        if (buf != null) {
            int entityId = buf.readInt();
            Entity entity = level.getEntity(entityId);
            if (entity instanceof TalentedCat talentedCat) {
                return talentedCat;
            }
        }
        return null;
    }

    public TalentedCat getCat() {
        return cat;
    }

    public int getNumRows() {
        return numRows;
    }

    @Override
    public boolean stillValid(Player player) {
        return cat != null && cat.isAlive() && cat.distanceToSqr(player) < 64.0D;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        int catSlotTotal = this.numRows * 9;

        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            itemstack = slotStack.copy();

            if (index < catSlotTotal) {
                if (!this.moveItemStackTo(slotStack, catSlotTotal, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (cat != null && cat.tryAutoEquip(slotStack, player)) {
                    if (slotStack.isEmpty()) {
                        slot.setByPlayer(ItemStack.EMPTY);
                    } else {
                        slot.setChanged();
                    }
                    return itemstack;
                }

                if (!this.moveItemStackTo(slotStack, 0, catSlotTotal, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (slotStack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemstack;
    }
}
