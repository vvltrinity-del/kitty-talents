package com.kittytalents.work;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

public class ItemFilter {
    public static final int SLOTS = 9;
    private boolean whitelist = true;
    private final NonNullList<ItemStack> ghostSlots = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private String tagFilter = "";

    public boolean isWhitelist() { return whitelist; }
    public void setWhitelist(boolean whitelist) { this.whitelist = whitelist; }

    public NonNullList<ItemStack> getGhostSlots() { return ghostSlots; }

    public ItemStack getGhostSlot(int index) {
        if (index < 0 || index >= SLOTS) return ItemStack.EMPTY;
        return ghostSlots.get(index);
    }

    public void setGhostSlot(int index, ItemStack stack) {
        if (index >= 0 && index < SLOTS) {
            if (stack == null || stack.isEmpty()) {
                ghostSlots.set(index, ItemStack.EMPTY);
            } else {
                ItemStack copy = stack.copy();
                copy.setCount(1);
                ghostSlots.set(index, copy);
            }
        }
    }

    public String getTagFilter() { return tagFilter; }
    public void setTagFilter(String tagFilter) {
        this.tagFilter = tagFilter != null ? tagFilter.trim().toLowerCase(Locale.ROOT) : "";
    }

    public boolean isEmptyFilter() {
        if (!tagFilter.isEmpty()) return false;
        for (ItemStack s : ghostSlots) {
            if (!s.isEmpty()) return false;
        }
        return true;
    }

    public boolean matches(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (isEmptyFilter()) {
            // An empty filter allows everything
            return true;
        }

        boolean found = false;
        for (ItemStack ghost : ghostSlots) {
            if (!ghost.isEmpty() && ItemStack.isSameItem(ghost, stack)) {
                found = true;
                break;
            }
        }

        if (!found && !tagFilter.isEmpty()) {
            String target = tagFilter.startsWith("#") ? tagFilter.substring(1) : tagFilter;
            found = stack.getTags().anyMatch(tagKey -> {
                ResourceLocation loc = tagKey.location();
                return loc.toString().equals(target) || loc.getPath().equals(target) || loc.getPath().contains(target);
            });
        }

        return whitelist == found;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("Whitelist", whitelist);
        tag.putString("TagFilter", tagFilter);
        ListTag list = new ListTag();
        for (int i = 0; i < SLOTS; i++) {
            ItemStack s = ghostSlots.get(i);
            if (!s.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putByte("Slot", (byte) i);
                s.save(itemTag);
                list.add(itemTag);
            }
        }
        tag.put("GhostSlots", list);
        return tag;
    }

    public void load(CompoundTag tag) {
        if (tag == null) return;
        this.whitelist = !tag.contains("Whitelist") || tag.getBoolean("Whitelist");
        this.tagFilter = tag.getString("TagFilter");
        for (int i = 0; i < SLOTS; i++) ghostSlots.set(i, ItemStack.EMPTY);
        if (tag.contains("GhostSlots", Tag.TAG_LIST)) {
            ListTag list = tag.getList("GhostSlots", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag itemTag = list.getCompound(i);
                int slot = itemTag.getByte("Slot") & 255;
                if (slot < SLOTS) {
                    ghostSlots.set(slot, ItemStack.of(itemTag));
                }
            }
        }
    }
}
