package com.kittytalents.network;

import com.kittytalents.entity.TalentedCat;
import com.kittytalents.talent.Talents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketEquipCatItem {
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

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeInt(slotId);
        buf.writeBoolean(isShiftClick);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null) {
                Entity entity = sender.level().getEntity(entityId);
                if (slotId < 0 || slotId > 10) return;
                if (entity instanceof TalentedCat cat && (cat.isOwnedBy(sender) || cat.isTrusted(sender)) && cat.distanceToSqr(sender) <= 64.0D * 64.0D) {
                    if (slotId == 10) {
                        // Toggle Cosmetics Visibility
                        cat.setCosmeticsVisible(!cat.isCosmeticsVisible());
                        sender.playSound(SoundEvents.UI_BUTTON_CLICK.get(), 0.6F, 1.2F);
                        cat.syncEquipment();
                        return;
                    }

                    handleSlotInteraction(sender, cat, slotId, isShiftClick);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    /** Server-side slot click. Public for GameTests. */
    public static void handleSlotInteraction(ServerPlayer sender, TalentedCat cat, int slotId, boolean isShiftClick) {
        ItemStack currentEquipped = switch (slotId) {
            case 0 -> cat.getEquippedWeapon();
            case 1, 2, 3, 4 -> cat.getArmorSlotItem(slotId);
            case 5 -> cat.getEquippedAccessory();
            case 6 -> cat.getEquippedTool();
            case 7, 8, 9 -> cat.getCosmeticSlot(slotId - 7);
            default -> ItemStack.EMPTY;
        };

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
        net.minecraft.world.item.Item item = stack.getItem();
        return switch (slotId) {
            case 0 -> com.kittytalents.compat.tacz.TacZCompat.isGun(stack) || item instanceof SwordItem
                    || item instanceof net.minecraft.world.item.TridentItem;
            case 1, 2, 3, 4 -> item instanceof ArmorItem armor && armor.getType() == switch (slotId) {
                case 1 -> ArmorItem.Type.HELMET;
                case 2 -> ArmorItem.Type.CHESTPLATE;
                case 3 -> ArmorItem.Type.LEGGINGS;
                default -> ArmorItem.Type.BOOTS;
            };
            case 5 -> item instanceof com.kittytalents.item.CatAccessoryItem;
            case 6 -> item instanceof net.minecraft.world.item.DiggerItem || item instanceof net.minecraft.world.item.ShearsItem
                    || item instanceof net.minecraft.world.item.FishingRodItem;
            case 7, 8, 9 -> item instanceof com.kittytalents.item.CatAccessoryItem || item instanceof com.kittytalents.item.CatCollarItem;
            default -> false;
        };
    }

    private static void setSlotItem(TalentedCat cat, int slotId, ItemStack stack) {
        switch (slotId) {
            case 0 -> cat.setEquippedWeapon(stack);
            case 1, 2, 3, 4 -> cat.setArmorSlotItem(slotId, stack);
            case 5 -> cat.setEquippedAccessory(stack);
            case 6 -> cat.setEquippedTool(stack);
            case 7, 8, 9 -> cat.setCosmeticSlot(slotId - 7, stack);
        }
    }

    private static boolean isValidItemForSlot(TalentedCat cat, int slotId, ItemStack stack, ServerPlayer sender) {
        if (stack.isEmpty()) return false;
        switch (slotId) {
            case 0 -> {
                if (com.kittytalents.compat.tacz.TacZCompat.isGun(stack)) {
                    int armoryLvl = cat.getTalentLevel(Talents.CAT_ARMORY);
                    if (armoryLvl < 1) {
                        deny(sender, "message.kittytalents.need_higher_armory", 1, stack);
                        return false;
                    }
                    return true;
                }
                if (stack.getItem() instanceof SwordItem sword) {
                    int armoryLvl = cat.getTalentLevel(Talents.CAT_ARMORY);
                    int req = getRequiredArmoryLevel(sword);
                    if (armoryLvl < req) {
                        deny(sender, "message.kittytalents.need_higher_armory", req, stack);
                        return false;
                    }
                    return true;
                }
                return stack.getItem() instanceof net.minecraft.world.item.TridentItem;
            }
            case 1, 2, 3, 4 -> {
                ArmorItem.Type expected = switch (slotId) {
                    case 1 -> ArmorItem.Type.HELMET;
                    case 2 -> ArmorItem.Type.CHESTPLATE;
                    case 3 -> ArmorItem.Type.LEGGINGS;
                    case 4 -> ArmorItem.Type.BOOTS;
                    default -> null;
                };
                if (stack.getItem() instanceof ArmorItem armor && armor.getType() == expected) {
                    int armorLvl = cat.getTalentLevel(Talents.CAT_ARMOR);
                    int req = getRequiredArmorLevel(armor);
                    if (armorLvl < req) {
                        deny(sender, "message.kittytalents.need_higher_armor", req, stack);
                        return false;
                    }
                    return true;
                }
                return false;
            }
            case 5 -> {
                return stack.getItem() instanceof com.kittytalents.item.CatAccessoryItem;
            }
            case 6 -> {
                return stack.getItem() instanceof net.minecraft.world.item.DiggerItem ||
                       stack.getItem() instanceof net.minecraft.world.item.ShearsItem ||
                       stack.getItem() instanceof net.minecraft.world.item.FishingRodItem;
            }
            case 7, 8, 9 -> {
                return stack.getItem() instanceof com.kittytalents.item.CatAccessoryItem ||
                       stack.getItem() instanceof com.kittytalents.item.CatCollarItem;
            }
            default -> {
                return false;
            }
        }
    }

    /** Rank-requirement feedback; {@code sender == null} means a quiet check. */
    private static void deny(@javax.annotation.Nullable ServerPlayer sender, String key, int rank, ItemStack stack) {
        if (sender == null) return;
        sender.sendSystemMessage(Component.translatable(key, rank, stack.getHoverName()));
        sender.playSound(SoundEvents.VILLAGER_NO, 1.0F, 1.0F);
    }

    private static int getRequiredArmoryLevel(SwordItem sword) {
        if (sword.getTier() == Tiers.WOOD || sword.getTier() == Tiers.STONE) return 1;
        if (sword.getTier() == Tiers.IRON || sword.getTier() == Tiers.GOLD) return 2;
        if (sword.getTier() == Tiers.DIAMOND) return 3;
        if (sword.getTier() == Tiers.NETHERITE) return 4;
        return 1;
    }

    private static int getRequiredArmorLevel(ArmorItem armor) {
        if (armor.getMaterial() == ArmorMaterials.LEATHER || armor.getMaterial() == ArmorMaterials.GOLD) return 1;
        if (armor.getMaterial() == ArmorMaterials.CHAIN || armor.getMaterial() == ArmorMaterials.IRON) return 2;
        if (armor.getMaterial() == ArmorMaterials.DIAMOND) return 3;
        if (armor.getMaterial() == ArmorMaterials.NETHERITE) return 4;
        return 1;
    }
}
