package com.kittytalents.compat.curios;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

import java.util.Optional;

/**
 * Optional Curios API integration.
 * Allows Kitty Whistle, Tracker, Feline Locator Orb, Treat Bag, and Work Clipboard
 * to be equipped in Curios charm/belt slots and discovered hands-free.
 */
public class CuriosCompat {
    private static Boolean loaded = null;

    public static boolean isLoaded() {
        if (loaded == null) {
            loaded = ModList.get().isLoaded("curios");
        }
        return loaded;
    }

    public static void init() {
        isLoaded();
    }

    public static ItemStack findEquippedCurio(Player player, Item item) {
        if (!isLoaded() || player == null || item == null) return ItemStack.EMPTY;
        try {
            return Internal.findCurio(player, item);
        } catch (Throwable ignored) {
            return ItemStack.EMPTY;
        }
    }

    private static class Internal {
        static ItemStack findCurio(Player player, Item item) {
            Optional<SlotResult> res = CuriosApi.getCuriosHelper().findFirstCurio(player, item);
            return res.map(SlotResult::stack).orElse(ItemStack.EMPTY);
        }
    }
}
