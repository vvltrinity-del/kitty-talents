package com.kittytalents.compat;

import com.kittytalents.entity.TalentedCat;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Convenience compatibility bridge forwarding to {@link com.kittytalents.compat.tacz.TacZCompat}.
 * Guarantees zero NoClassDefFoundError whether TacZ is present or absent.
 */
public class TacZCompat {

    public static boolean isLoaded() {
        return com.kittytalents.compat.tacz.TacZCompat.isLoaded();
    }

    public static boolean isGun(ItemStack stack) {
        return com.kittytalents.compat.tacz.TacZCompat.isGun(stack);
    }

    public static boolean isAmmo(ItemStack stack) {
        return com.kittytalents.compat.tacz.TacZCompat.isAmmo(stack);
    }

    public static boolean isMatchingAmmo(ItemStack gun, ItemStack ammo) {
        return com.kittytalents.compat.tacz.TacZCompat.isMatchingAmmo(gun, ammo);
    }

    public static boolean hasAmmoInPack(TalentedCat cat, ItemStack gun) {
        return com.kittytalents.compat.tacz.TacZCompat.hasAmmoInPack(cat, gun);
    }

    public static boolean consumeAmmoFromPack(TalentedCat cat, ItemStack gun) {
        return com.kittytalents.compat.tacz.TacZCompat.consumeAmmoFromPack(cat, gun);
    }

    public static int getAmmoCount(ItemStack gun) {
        return com.kittytalents.compat.tacz.TacZCompat.getAmmoCount(gun);
    }

    public static void setAmmoCount(ItemStack gun, int count) {
        com.kittytalents.compat.tacz.TacZCompat.setAmmoCount(gun, count);
    }

    public static int getShootCooldownTicks(ItemStack gun) {
        return com.kittytalents.compat.tacz.TacZCompat.getShootCooldownTicks(gun);
    }

    public static boolean tryReloadFromPack(TalentedCat cat, ItemStack gun) {
        return com.kittytalents.compat.tacz.TacZCompat.tryReloadFromPack(cat, gun);
    }

    public static boolean shootGunAtTarget(TalentedCat cat, LivingEntity target) {
        return com.kittytalents.compat.tacz.TacZCompat.shootGunAtTarget(cat, target);
    }

    public static boolean shootGunTwined(TalentedCat cat, Player player) {
        return com.kittytalents.compat.tacz.TacZCompat.shootGunTwined(cat, player);
    }

    public static boolean executeShot(TalentedCat cat, ItemStack gun, float pitch, float yaw, LivingEntity target, boolean isCreative) {
        return com.kittytalents.compat.tacz.TacZCompat.executeShot(cat, gun, pitch, yaw, target, isCreative);
    }

    public static boolean handleEmptyAmmoSwapOrStow(TalentedCat cat) {
        return com.kittytalents.compat.tacz.TacZCompat.handleEmptyAmmoSwapOrStow(cat);
    }

    public static boolean tryReEquipFirearmIfAmmoAvailable(TalentedCat cat) {
        return com.kittytalents.compat.tacz.TacZCompat.tryReEquipFirearmIfAmmoAvailable(cat);
    }
}
