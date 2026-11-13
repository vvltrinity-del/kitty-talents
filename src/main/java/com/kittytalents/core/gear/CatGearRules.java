package com.kittytalents.core.gear;

import com.kittytalents.compat.tacz.TacZCompat;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.item.CatAccessoryItem;
import com.kittytalents.item.CatCollarItem;
import com.kittytalents.talent.Talents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TridentItem;

import javax.annotation.Nullable;

/**
 * What each cat gear slot accepts (4.0 Phase 5), shared by the drag-and-drop slots, the click packet and the tooltips.
 * Slot ids: 0 weapon, 1 helmet, 2 chest, 3 legs, 4 boots, 5 accessory, 6 tool, 7..9 cosmetics.
 */
public final class CatGearRules {
    private CatGearRules() {}

    public static final int WEAPON = 0, ACCESSORY = 5, TOOL = 6, COSMETIC_1 = 7, COUNT = 10;

    public static ItemStack get(TalentedCat cat, int slot) {
        return switch (slot) {
            case 0 -> cat.getEquippedWeapon();
            case 1, 2, 3, 4 -> cat.getArmorSlotItem(slot);
            case 5 -> cat.getEquippedAccessory();
            case 6 -> cat.getEquippedTool();
            case 7, 8, 9 -> cat.getCosmeticSlot(slot - 7);
            default -> ItemStack.EMPTY;
        };
    }

    public static void set(TalentedCat cat, int slot, ItemStack stack) {
        switch (slot) {
            case 0 -> cat.setEquippedWeapon(stack);
            case 1, 2, 3, 4 -> cat.setArmorSlotItem(slot, stack);
            case 5 -> cat.setEquippedAccessory(stack);
            case 6 -> cat.setEquippedTool(stack);
            case 7, 8, 9 -> cat.setCosmeticSlot(slot - 7, stack);
            default -> { }
        }
    }

    @Nullable
    private static ArmorItem.Type armorType(int slot) {
        return switch (slot) {
            case 1 -> ArmorItem.Type.HELMET;
            case 2 -> ArmorItem.Type.CHESTPLATE;
            case 3 -> ArmorItem.Type.LEGGINGS;
            case 4 -> ArmorItem.Type.BOOTS;
            default -> null;
        };
    }

    /** Item category only (no talent ranks). */
    public static boolean fitsType(int slot, ItemStack stack) {
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();
        return switch (slot) {
            case 0 -> TacZCompat.isGun(stack) || item instanceof SwordItem || item instanceof TridentItem;
            case 1, 2, 3, 4 -> item instanceof ArmorItem armor && armor.getType() == armorType(slot);
            case 5 -> item instanceof CatAccessoryItem;
            case 6 -> item instanceof DiggerItem || item instanceof ShearsItem || item instanceof FishingRodItem;
            case 7, 8, 9 -> item instanceof CatAccessoryItem || item instanceof CatCollarItem;
            default -> false;
        };
    }

    /**
     * The talent rank the item needs in this slot (0 = none). 5.0: basic gear (leather to iron, wooden to iron
     * swords, modded armor and weapons) needs no talent; Feline Armor and Cat Armory add power per rank instead of
     * permission. Only diamond (rank 1), netherite (rank 3) and guns (Cat Armory 1) stay gated.
     */
    public static int requiredRank(int slot, ItemStack stack) {
        Item item = stack.getItem();
        if (slot == WEAPON) {
            if (TacZCompat.isGun(stack)) return 1;
            if (item instanceof SwordItem sword) {
                if (sword.getTier() == Tiers.DIAMOND) return 1;
                if (sword.getTier() == Tiers.NETHERITE) return 3;
            }
            return 0;
        }
        if (slot >= 1 && slot <= 4 && item instanceof ArmorItem armor) {
            if (armor.getMaterial() == ArmorMaterials.DIAMOND) return 1;
            if (armor.getMaterial() == ArmorMaterials.NETHERITE) return 3;
        }
        return 0;
    }

    /** True if {@code stack} needs a Cat Armory rank (weapons) rather than a Cat Armor rank (armor). */
    private static boolean isWeaponSlot(int slot) {
        return slot == WEAPON;
    }

    /** Why the cat can't wear {@code stack} in {@code slot}, or null if it can. */
    @Nullable
    public static Component refusal(TalentedCat cat, int slot, ItemStack stack) {
        if (stack.isEmpty()) return null;
        if (!fitsType(slot, stack)) {
            return Component.translatable("message.kittytalents.gear.wrong_slot", stack.getHoverName(), slotName(slot));
        }
        int need = requiredRank(slot, stack);
        if (need > 0) {
            boolean weapon = isWeaponSlot(slot);
            int have = cat.getTalentLevel(weapon ? Talents.CAT_ARMORY : Talents.CAT_ARMOR);
            if (have < need) {
                return Component.translatable(weapon ? "message.kittytalents.need_higher_armory" : "message.kittytalents.need_higher_armor",
                        need, stack.getHoverName());
            }
        }
        return null;
    }

    public static Component slotName(int slot) {
        String key = switch (slot) {
            case 0 -> "weapon";
            case 1 -> "head";
            case 2 -> "chest";
            case 3 -> "legs";
            case 4 -> "boots";
            case 5 -> "accessory";
            case 6 -> "tool";
            default -> "cosmetic" + (slot - 6);
        };
        return Component.translatable("gui.kittytalents.screen.gear.slot." + key);
    }
}
