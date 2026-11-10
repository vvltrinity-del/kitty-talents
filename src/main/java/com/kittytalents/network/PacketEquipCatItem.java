package com.kittytalents.network;

import com.kittytalents.core.perm.CatAction;
import com.kittytalents.entity.TalentedCat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

public class PacketEquipCatItem extends PacketBase {
    private final int entityId;
    private final int slotId; // 0 = weapon, 1 = helmet, 2 = chest, 3 = legs, 4 = boots, 5 = acc, 6 = tool, 7..9 = cos, 10 = toggle
    private final boolean isShiftClick;

    public PacketEquipCatItem(int entityId, int slotId, boolean isShiftClick) {
        this.entityId = entityId;
        this.slotId = slotId;
        this.isShiftClick = isShiftClick;
    }

    public PacketEquipCatItem(int entityId, int slotId) {
        this(entityId, slotId, false);
    }

    public PacketEquipCatItem(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.slotId = buf.readInt();
        this.isShiftClick = buf.readableBytes() > 0 && buf.readBoolean();
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeInt(slotId);
        buf.writeBoolean(isShiftClick);
    }

    /** Slot ids 0..9 are gear slots, 10 toggles cosmetic visibility. */
    public static final int TOGGLE_COSMETICS = 10;

    @Override
    protected void handleServer(ServerPlayer sender) {
        apply(sender, entityId, slotId, isShiftClick);
    }

    /** Validate -> authorize (EQUIP) -> apply. @return true if the click was handled. */
    public static boolean apply(ServerPlayer sender, int entityId, int slotId, boolean isShiftClick) {
        if (slotId < 0 || slotId > TOGGLE_COSMETICS) return false;
        TalentedCat cat = authorize(sender, entityId, CatAction.EQUIP);
        if (cat == null) return false;
        if (slotId == TOGGLE_COSMETICS) {
            cat.setCosmeticsVisible(!cat.isCosmeticsVisible());
            sender.playSound(SoundEvents.UI_BUTTON_CLICK.get(), 0.6F, 1.2F);
            cat.syncEquipment();
            return true;
        }
        handleSlotInteraction(sender, cat, slotId, isShiftClick);
        return true;
    }

    /** Server-side slot click. Public for GameTests. */
    public static void handleSlotInteraction(ServerPlayer sender, TalentedCat cat, int slotId, boolean isShiftClick) {
        ItemStack currentEquipped = com.kittytalents.core.gear.CatGearRules.get(cat, slotId);

        ItemStack carried = sender.containerMenu.getCarried();

        if (isShiftClick) {
            // Shift-click: unequip directly into player's personal inventory
            if (!currentEquipped.isEmpty()) {
                setSlotItem(cat, slotId, ItemStack.EMPTY);
                if (!sender.getInventory().add(currentEquipped)) {
                    sender.drop(currentEquipped, false);
                }
                sender.playSound(SoundEvents.ITEM_PICKUP, 0.7F, 1.2F);
            }
        } else {
            // Normal click:
            // If cursor is holding an item: try to equip carried item
            if (!carried.isEmpty() && isValidItemForSlot(cat, slotId, carried, sender)) {
                ItemStack toEquip = carried.split(1);
                setSlotItem(cat, slotId, toEquip);
                // Put old equipped item onto cursor (or into inventory if empty)
                if (!currentEquipped.isEmpty()) {
                    if (carried.isEmpty()) {
                        sender.containerMenu.setCarried(currentEquipped);
                    } else {
                        if (!sender.getInventory().add(currentEquipped)) {
                            sender.drop(currentEquipped, false);
                        }
                    }
                }
                sender.playSound(SoundEvents.ARMOR_EQUIP_GENERIC, 1.0F, 1.0F);
            } else if (carried.isEmpty() && !currentEquipped.isEmpty()) {
                // Unequip straight into the player's inventory. (The cat screen has no inventory slots, so an item
                // put on the cursor could not be placed anywhere until the menu closed.)
                setSlotItem(cat, slotId, ItemStack.EMPTY);
                if (!sender.getInventory().add(currentEquipped)) {
                    sender.drop(currentEquipped, false);
                }
                sender.playSound(SoundEvents.ITEM_PICKUP, 0.7F, 1.2F);
            } else if (carried.isEmpty() && currentEquipped.isEmpty()) {
                // Click-to-equip: take the first item from the player's inventory that fits this slot.
                equipFromInventory(sender, cat, slotId);
            }
        }

        sender.containerMenu.broadcastChanges();
        cat.updateAttributes();
        cat.syncEquipment();
    }

    /** Hotbar first, then main inventory, then offhand. Reports rank requirements or "nothing fits". */
    private static void equipFromInventory(ServerPlayer sender, TalentedCat cat, int slotId) {
        net.minecraft.world.entity.player.Inventory inv = sender.getInventory();
        ItemStack rankBlocked = ItemStack.EMPTY;
        int size = inv.items.size() + inv.offhand.size();
        for (int i = 0; i < size; i++) {
            ItemStack stack = i < inv.items.size() ? inv.items.get(i) : inv.offhand.get(i - inv.items.size());
            if (stack.isEmpty() || !fitsSlotType(slotId, stack)) continue;
            if (isValidItemForSlot(cat, slotId, stack, null)) {
                setSlotItem(cat, slotId, stack.split(1));
                inv.setChanged();
                sender.playSound(SoundEvents.ARMOR_EQUIP_GENERIC, 1.0F, 1.0F);
                return;
            }
            if (rankBlocked.isEmpty()) rankBlocked = stack;
        }
        if (!rankBlocked.isEmpty()) {
            isValidItemForSlot(cat, slotId, rankBlocked, sender); // sends the "need higher rank" message
        } else {
            sender.sendSystemMessage(Component.translatable("message.kittytalents.no_item_for_slot"));
            sender.playSound(SoundEvents.VILLAGER_NO, 0.6F, 1.0F);
        }
    }

    /** Item category check only (no talent-rank gating), used to find candidates. */
    private static boolean fitsSlotType(int slotId, ItemStack stack) {
        return com.kittytalents.core.gear.CatGearRules.fitsType(slotId, stack);
    }

    private static void setSlotItem(TalentedCat cat, int slotId, ItemStack stack) {
        com.kittytalents.core.gear.CatGearRules.set(cat, slotId, stack);
    }

    /** Rules live in CatGearRules; a rank refusal is reported to {@code sender} (null = a quiet check). */
    private static boolean isValidItemForSlot(TalentedCat cat, int slotId, ItemStack stack, @javax.annotation.Nullable ServerPlayer sender) {
        if (stack.isEmpty() || !fitsSlotType(slotId, stack)) return false;
        Component why = com.kittytalents.core.gear.CatGearRules.refusal(cat, slotId, stack);
        if (why == null) return true;
        if (sender != null) {
            sender.sendSystemMessage(why);
            sender.playSound(SoundEvents.VILLAGER_NO, 1.0F, 1.0F);
        }
        return false;
    }
}
