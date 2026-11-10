package com.kittytalents.inventory;

import com.kittytalents.core.gear.CatGearRules;
import com.kittytalents.core.perm.CatAction;
import com.kittytalents.core.perm.CatPermissions;
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

/**
 * The companion screen's menu. Since 4.0 (Phase 5) it has real slots: the cat's 10 gear slots (menu indices 0..9,
 * same ids as {@link CatGearRules}) and the player's inventory (10..36 main, 37..45 hotbar), so items drag, shift-click
 * and sync like any container. Gear slots check {@link CatGearRules} and the EQUIP permission on both sides; the server's
 * copy decides. Slot positions only matter on the client, where the Gear tab lays them out ({@link #place}).
 */
public class CatTalentMenu extends AbstractContainerMenu {
    public static final int GEAR = CatGearRules.COUNT, INV_START = GEAR, HOTBAR_START = INV_START + 27, END = HOTBAR_START + 9;

    private final TalentedCat cat;
    private final Player player;
    private final Container gear;
    /** Client: slots are drawn and clickable only on the Gear tab. */
    private boolean slotsShown = true;

    public CatTalentMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, getCatFromBuf(playerInventory.player.level(), extraData));
    }

    public CatTalentMenu(int containerId, Inventory playerInventory, TalentedCat cat) {
        super(ModMenus.CAT_TALENT_MENU.get(), containerId);
        this.cat = cat;
        this.player = playerInventory.player;
        this.gear = new CatGearContainer(cat);
        for (int i = 0; i < GEAR; i++) addSlot(new GearSlot(this, i, 0, 0));
        for (int i = 0; i < 27; i++) addSlot(new InvSlot(this, playerInventory, 9 + i, 0, 0));
        for (int i = 0; i < 9; i++) addSlot(new InvSlot(this, playerInventory, i, 0, 0));
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

    public boolean canEquip() {
        return cat != null && CatPermissions.can(player, cat, CatAction.EQUIP);
    }

    public void setSlotsShown(boolean shown) {
        this.slotsShown = shown;
    }

    /** Client layout: replaces menu slot {@code index} with an identical slot at (x, y). Slot coordinates are final. */
    public void place(int index, int x, int y) {
        Slot old = slots.get(index);
        Slot moved = old instanceof GearSlot g ? new GearSlot(this, g.getContainerSlot(), x, y)
                : new InvSlot(this, (Inventory) old.container, old.getContainerSlot(), x, y);
        moved.index = index;
        slots.set(index, moved);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        if (index < GEAR) {
            if (!slot.mayPickup(player) || !moveItemStackTo(stack, INV_START, END, true)) return ItemStack.EMPTY;
        } else {
            // Into the first empty gear slot that accepts it (one item: gear slots hold one).
            boolean moved = false;
            for (int i = 0; i < GEAR && !moved; i++) {
                Slot g = slots.get(i);
                if (!g.hasItem() && g.mayPlace(stack)) {
                    g.setByPlayer(stack.split(1));
                    moved = true;
                }
            }
            if (!moved) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        // EMPTY stops vanilla's "repeat while it moved" loop: one shift-click moves one item (no filling all three
        // cosmetic slots from one stack).
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return cat != null && cat.isAlive() && cat.distanceToSqr(player) < 64.0D;
    }

    /** One of the cat's gear slots. */
    public static class GearSlot extends Slot {
        private final CatTalentMenu menu;

        GearSlot(CatTalentMenu menu, int gearSlot, int x, int y) {
            super(menu.gear, gearSlot, x, y);
            this.menu = menu;
        }

        public int gearSlot() {
            return getContainerSlot();
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return menu.cat != null && menu.canEquip() && CatGearRules.refusal(menu.cat, getContainerSlot(), stack) == null;
        }

        @Override
        public boolean mayPickup(Player player) {
            return menu.canEquip();
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public boolean isActive() {
            return menu.slotsShown;
        }
    }

    /** A player inventory slot that hides with the Gear tab. */
    public static class InvSlot extends Slot {
        private final CatTalentMenu menu;

        InvSlot(CatTalentMenu menu, Inventory inv, int invSlot, int x, int y) {
            super(inv, invSlot, x, y);
            this.menu = menu;
        }

        @Override
        public boolean isActive() {
            return menu.slotsShown;
        }
    }

    /** The cat's gear as a Container: reads and writes go straight to the cat; the server re-derives stats and syncs. */
    static final class CatGearContainer implements Container {
        private final TalentedCat cat;

        CatGearContainer(TalentedCat cat) {
            this.cat = cat;
        }

        @Override public int getContainerSize() { return GEAR; }
        @Override public boolean isEmpty() {
            for (int i = 0; i < GEAR; i++) if (!getItem(i).isEmpty()) return false;
            return true;
        }
        @Override public ItemStack getItem(int slot) { return cat == null ? ItemStack.EMPTY : CatGearRules.get(cat, slot); }
        @Override public ItemStack removeItem(int slot, int amount) {
            ItemStack cur = getItem(slot);
            if (cur.isEmpty() || amount <= 0) return ItemStack.EMPTY;
            ItemStack out = cur.copy();
            setItem(slot, ItemStack.EMPTY);
            return out;
        }
        @Override public ItemStack removeItemNoUpdate(int slot) { return removeItem(slot, 1); }
        @Override public void setItem(int slot, ItemStack stack) {
            if (cat == null) return;
            CatGearRules.set(cat, slot, stack);
            setChanged();
        }
        @Override public int getMaxStackSize() { return 1; }
        @Override public void setChanged() {
            if (cat != null && !cat.level().isClientSide) {
                cat.updateAttributes();
                cat.syncEquipment();
            }
        }
        @Override public boolean stillValid(Player player) { return cat != null && cat.isAlive(); }
        @Override public void clearContent() { }
    }
}
