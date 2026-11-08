package com.kittytalents.core.component;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

/**
 * Everything a cat wears or carries. Pure storage with no side effects: the entity layers attribute updates,
 * TacZ hooks and network sync on top. NBT keys are unchanged from 2.0.
 */
public final class CatEquipment {

    public enum Slot {
        WEAPON("EquippedWeapon"), TOOL("EquippedTool"),
        HELMET("EquippedHelmet"), CHEST("EquippedChest"), LEGS("EquippedLegs"), BOOTS("EquippedBoots"),
        ACCESSORY("EquippedAccessory");

        final String nbtKey;
        Slot(String nbtKey) { this.nbtKey = nbtKey; }

        /** Legacy armor index used across the codebase: 1 = helmet .. 4 = boots. */
        public static Slot armor(int index) {
            return switch (index) {
                case 1 -> HELMET;
                case 2 -> CHEST;
                case 3 -> LEGS;
                case 4 -> BOOTS;
                default -> null;
            };
        }
    }

    public static final int COSMETIC_SLOTS = 3;
    public static final int PACK_SIZE = 27;
    public static final int AMMO_POUCH_SIZE = 9;

    private final ItemStack[] slots = new ItemStack[Slot.values().length];
    private final ItemStack[] cosmetics = new ItemStack[COSMETIC_SLOTS];
    private ItemStack carriedToy = ItemStack.EMPTY;
    private final SimpleContainer pack = new SimpleContainer(PACK_SIZE);
    private final SimpleContainer ammoPouch = new SimpleContainer(AMMO_POUCH_SIZE);

    public CatEquipment() {
        java.util.Arrays.fill(slots, ItemStack.EMPTY);
        java.util.Arrays.fill(cosmetics, ItemStack.EMPTY);
    }

    public ItemStack get(Slot slot) { return slot == null ? ItemStack.EMPTY : slots[slot.ordinal()]; }

    /** Stores a defensive copy. */
    public void set(Slot slot, ItemStack stack) {
        if (slot != null) slots[slot.ordinal()] = copyOrEmpty(stack);
    }

    public ItemStack getCosmetic(int index) {
        return index >= 0 && index < COSMETIC_SLOTS ? cosmetics[index] : ItemStack.EMPTY;
    }

    /** @return false if the index is out of range */
    public boolean setCosmetic(int index, ItemStack stack) {
        if (index < 0 || index >= COSMETIC_SLOTS) return false;
        cosmetics[index] = copyOrEmpty(stack);
        return true;
    }

    public ItemStack getCarriedToy() { return carriedToy; }
    public void setCarriedToy(ItemStack toy) { carriedToy = copyOrEmpty(toy); }

    public SimpleContainer pack() { return pack; }
    public SimpleContainer ammoPouch() { return ammoPouch; }

    private static ItemStack copyOrEmpty(ItemStack stack) {
        return stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
    }

    public void save(CompoundTag tag) {
        for (Slot s : Slot.values()) {
            ItemStack stack = get(s);
            if (!stack.isEmpty()) tag.put(s.nbtKey, stack.save(new CompoundTag()));
        }
        for (int i = 0; i < COSMETIC_SLOTS; i++) {
            if (!cosmetics[i].isEmpty()) tag.put("CosmeticSlot" + i, cosmetics[i].save(new CompoundTag()));
        }
        if (!carriedToy.isEmpty()) tag.put("CarriedToy", carriedToy.save(new CompoundTag()));
        tag.put("CatPack", saveContainer(pack));
        tag.put("AmmoPouch", saveContainer(ammoPouch));
    }

    public void load(CompoundTag tag) {
        for (Slot s : Slot.values()) {
            slots[s.ordinal()] = tag.contains(s.nbtKey) ? ItemStack.of(tag.getCompound(s.nbtKey)) : ItemStack.EMPTY;
        }
        for (int i = 0; i < COSMETIC_SLOTS; i++) {
            cosmetics[i] = tag.contains("CosmeticSlot" + i) ? ItemStack.of(tag.getCompound("CosmeticSlot" + i)) : ItemStack.EMPTY;
        }
        carriedToy = tag.contains("CarriedToy") ? ItemStack.of(tag.getCompound("CarriedToy")) : ItemStack.EMPTY;
        if (tag.contains("CatPack", Tag.TAG_LIST)) loadContainer(pack, tag.getList("CatPack", Tag.TAG_COMPOUND));
        if (tag.contains("AmmoPouch", Tag.TAG_LIST)) loadContainer(ammoPouch, tag.getList("AmmoPouch", Tag.TAG_COMPOUND));
    }

    private static ListTag saveContainer(SimpleContainer container) {
        ListTag list = new ListTag();
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putByte("Slot", (byte) i);
                stack.save(itemTag);
                list.add(itemTag);
            }
        }
        return list;
    }

    private static void loadContainer(SimpleContainer container, ListTag list) {
        container.clearContent();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag itemTag = list.getCompound(i);
            int slot = itemTag.getByte("Slot") & 255;
            if (slot < container.getContainerSize()) container.setItem(slot, ItemStack.of(itemTag));
        }
    }
}
