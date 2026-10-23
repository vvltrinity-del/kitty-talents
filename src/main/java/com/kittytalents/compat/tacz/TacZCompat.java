package com.kittytalents.compat.tacz;

import com.kittytalents.entity.TalentedCat;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Collections;
import java.util.List;

/**
 * Classloading-safe facade for TacZ integration.
 * Delegates to {@link TacZImpl}, {@link TacZGunsmith}, and {@link TacZEvents} only when "tacz" is loaded.
 */
public class TacZCompat {
    private static Boolean taczLoaded = null;

    public static boolean isLoaded() {
        if (taczLoaded == null) {
            taczLoaded = ModList.get().isLoaded("tacz");
            if (taczLoaded) {
                try {
                    TacZEvents.register();
                } catch (Throwable ignored) {}
            }
        }
        return taczLoaded;
    }

    public static boolean isGun(ItemStack stack) {
        if (stack.isEmpty() || !isLoaded()) return false;
        try {
            if (TacZImpl.isGun(stack)) return true;
        } catch (Throwable ignored) {}
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id != null && id.getNamespace().equals("tacz")) {
            if (stack.hasTag() && stack.getTag().contains("GunId")) return true;
            String path = id.getPath().toLowerCase();
            return path.contains("gun") || path.contains("rifle") || path.contains("pistol") ||
                   path.contains("shotgun") || path.contains("smg") || path.contains("sniper");
        }
        return false;
    }

    public static boolean isAmmo(ItemStack stack) {
        if (stack.isEmpty() || !isLoaded()) return false;
        try {
            if (TacZImpl.isAmmo(stack)) return true;
        } catch (Throwable ignored) {}
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id != null && id.getNamespace().equals("tacz")) {
            String path = id.getPath().toLowerCase();
            return path.contains("ammo") || path.contains("bullet") || path.contains("box") || path.contains("round");
        }
        return false;
    }

    public static boolean isAttachment(ItemStack stack) {
        if (stack.isEmpty() || !isLoaded()) return false;
        try {
            return TacZImpl.isAttachment(stack);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean isMatchingAmmo(ItemStack gun, ItemStack ammo) {
        if (!isGun(gun) || ammo.isEmpty()) return false;
        try {
            return TacZImpl.isMatchingAmmo(gun, ammo);
        } catch (Throwable ignored) {
            return isAmmo(ammo);
        }
    }

    public static void onGunEquipped(TalentedCat cat, ItemStack gun) {
        if (!isLoaded()) return;
        try {
            TacZImpl.onGunEquipped(cat, gun);
        } catch (Throwable ignored) {}
    }

    public static boolean isReloading(TalentedCat cat) {
        if (!isLoaded()) return false;
        try {
            return TacZImpl.isReloading(cat);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static void setAiming(TalentedCat cat, boolean aiming) {
        if (!isLoaded()) return;
        try {
            TacZImpl.setAiming(cat, aiming);
        } catch (Throwable ignored) {}
    }

    public static boolean triggerReload(TalentedCat cat, ItemStack gun) {
        if (!isLoaded()) return false;
        try {
            return TacZImpl.triggerReload(cat, gun);
        } catch (Throwable ignored) {
            return tryReloadFromPack(cat, gun);
        }
    }

    public static boolean hasAmmoInPack(TalentedCat cat, ItemStack gun) {
        if (!isLoaded()) return false;
        try {
            if (TacZImpl.hasAmmoInCatInventories(cat, gun)) return true;
        } catch (Throwable ignored) {}
        for (int i = 0; i < cat.getAmmoPouch().getContainerSize(); i++) {
            ItemStack s = cat.getAmmoPouch().getItem(i);
            if (!s.isEmpty() && isMatchingAmmo(gun, s)) return true;
        }
        for (int i = 0; i < cat.getCatPack().getContainerSize(); i++) {
            ItemStack s = cat.getCatPack().getItem(i);
            if (!s.isEmpty() && isMatchingAmmo(gun, s)) return true;
        }
        return false;
    }

    public static boolean consumeAmmoFromPack(TalentedCat cat, ItemStack gun) {
        if (!isLoaded()) return false;
        try {
            if (TacZImpl.consumeOneAmmoFromCatInventories(cat, gun)) return true;
        } catch (Throwable ignored) {}
        if (consumeFallbackFromContainer(cat.getAmmoPouch(), gun)) return true;
        return consumeFallbackFromContainer(cat.getCatPack(), gun);
    }

    private static boolean consumeFallbackFromContainer(SimpleContainer container, ItemStack gun) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty() && isMatchingAmmo(gun, stack)) {
                if (stack.hasTag() && stack.getTag().contains("AmmoCount")) {
                    int boxCount = stack.getTag().getInt("AmmoCount");
                    if (boxCount > 0) {
                        stack.getTag().putInt("AmmoCount", boxCount - 1);
                        return true;
                    }
                } else {
                    stack.shrink(1);
                    if (stack.isEmpty()) container.setItem(i, ItemStack.EMPTY);
                    return true;
                }
            }
        }
        return false;
    }

    public static int getAmmoCount(ItemStack gun) {
        if (!isLoaded() || gun.isEmpty()) return 0;
        try {
            return TacZImpl.getAmmoCount(gun);
        } catch (Throwable ignored) {}
        if (gun.hasTag() && gun.getTag().contains("GunCurrentAmmoCount")) {
            return gun.getTag().getInt("GunCurrentAmmoCount");
        }
        return 0;
    }

    public static int getMaxAmmoCount(ItemStack gun) {
        if (!isLoaded() || gun.isEmpty()) return 30;
        try {
            return TacZImpl.getMaxAmmoCount(gun);
        } catch (Throwable ignored) {
            return 30;
        }
    }

    public static void setAmmoCount(ItemStack gun, int count) {
        if (!isLoaded() || gun.isEmpty()) return;
        try {
            TacZImpl.setAmmoCount(gun, count);
            return;
        } catch (Throwable ignored) {}
        gun.getOrCreateTag().putInt("GunCurrentAmmoCount", count);
        gun.getOrCreateTag().putBoolean("HasBulletInBarrel", count > 0);
    }

    public static int getShootCooldownTicks(ItemStack gun) {
        if (!isLoaded() || gun.isEmpty()) return 8;
        try {
            return TacZImpl.getShootCooldownTicks(gun);
        } catch (Throwable ignored) {
            return 8;
        }
    }

    public static boolean tryReloadFromPack(TalentedCat cat, ItemStack gun) {
        if (!hasAmmoInPack(cat, gun)) return false;
        int current = getAmmoCount(gun);
        int max = getMaxAmmoCount(gun);
        int needed = max - current;
        int loaded = 0;
        while (loaded < needed && consumeAmmoFromPack(cat, gun)) {
            loaded++;
        }
        if (loaded > 0) {
            setAmmoCount(gun, current + loaded);
            cat.playSound(SoundEvents.IRON_DOOR_CLOSE, 0.7F, 1.6F);
            return true;
        }
        return false;
    }

    public static boolean shootGunAtTarget(TalentedCat cat, LivingEntity target) {
        if (!isLoaded()) return false;
        try {
            return TacZImpl.shootGunAtTarget(cat, target);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean shootGunTwined(TalentedCat cat, Player player) {
        if (!isLoaded()) return false;
        try {
            return TacZImpl.shootGunTwined(cat, player);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean executeShot(TalentedCat cat, ItemStack gun, float pitch, float yaw, LivingEntity target, boolean isCreative) {
        return executeFallbackShot(cat, gun, pitch, yaw, target, isCreative);
    }

    public static boolean executeFallbackShot(TalentedCat cat, ItemStack gun, float pitch, float yaw, LivingEntity target, boolean isCreative) {
        if (!isCreative) {
            int ammo = getAmmoCount(gun);
            if (ammo > 0) {
                setAmmoCount(gun, ammo - 1);
            } else if (!consumeAmmoFromPack(cat, gun)) {
                cat.playSound(SoundEvents.DISPENSER_FAIL, 0.8F, 1.6F);
                return false;
            }
        }
        cat.setLastShotTick(cat.tickCount);
        Vec3 look = cat.getLookAngle();
        if (look.lengthSqr() > 0.001D) {
            cat.push(-look.x * 0.06D, 0.02D, -look.z * 0.06D);
        }
        if (cat.level() instanceof ServerLevel sl) {
            Vec3 muzzle = cat.getEyePosition().add(cat.getLookAngle().scale(0.5D));
            sl.sendParticles(ParticleTypes.FLAME, muzzle.x, muzzle.y, muzzle.z, 3, 0.05, 0.05, 0.05, 0.02);
            sl.sendParticles(ParticleTypes.SMOKE, muzzle.x, muzzle.y, muzzle.z, 5, 0.1, 0.1, 0.1, 0.05);
        }
        cat.level().playSound(null, cat.getX(), cat.getY(), cat.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.NEUTRAL, 0.9F, 1.8F);
        if (target != null && target.isAlive()) {
            float baseGunDamage = 12.0F + (cat.getTalentLevel(com.kittytalents.talent.Talents.CAT_ARMORY) * 3.5F);
            target.hurt(cat.damageSources().mobAttack(cat), baseGunDamage);
            target.invulnerableTime = 0;
        }
        return true;
    }

    public static boolean handleEmptyAmmoSwapOrStow(TalentedCat cat) {
        if (!isLoaded()) return false;
        ItemStack gun = cat.getEquippedWeapon();
        if (!isGun(gun)) return false;

        if (getAmmoCount(gun) > 0 || hasAmmoInPack(cat, gun)) {
            return false;
        }

        SimpleContainer pack = cat.getCatPack();
        int meleeSlot = -1;
        for (int i = 0; i < pack.getContainerSize(); i++) {
            ItemStack s = pack.getItem(i);
            if (!s.isEmpty() && (s.getItem() instanceof net.minecraft.world.item.SwordItem ||
                                 s.getItem() instanceof net.minecraft.world.item.AxeItem ||
                                 s.getItem() instanceof net.minecraft.world.item.TieredItem)) {
                meleeSlot = i;
                break;
            }
        }

        if (meleeSlot >= 0) {
            ItemStack melee = pack.getItem(meleeSlot).copy();
            pack.setItem(meleeSlot, gun.copy());
            cat.setEquippedWeapon(melee);
            cat.playSound(SoundEvents.ARMOR_EQUIP_IRON, 0.8F, 1.2F);
            return true;
        } else {
            ItemStack gunCopy = gun.copy();
            cat.setEquippedWeapon(ItemStack.EMPTY);
            ItemStack remainder = pack.addItem(gunCopy);
            if (!remainder.isEmpty()) {
                cat.spawnAtLocation(remainder);
            }
            cat.playSound(SoundEvents.ARMOR_EQUIP_LEATHER, 0.7F, 1.0F);
            return true;
        }
    }

    public static boolean tryReEquipFirearmIfAmmoAvailable(TalentedCat cat) {
        if (!isLoaded()) return false;
        if (isGun(cat.getEquippedWeapon())) return false;

        SimpleContainer pack = cat.getCatPack();
        for (int i = 0; i < pack.getContainerSize(); i++) {
            ItemStack s = pack.getItem(i);
            if (isGun(s) && hasAmmoInPack(cat, s)) {
                ItemStack oldWeapon = cat.getEquippedWeapon();
                ItemStack gunToEquip = s.copy();
                pack.setItem(i, oldWeapon.copy());
                cat.setEquippedWeapon(gunToEquip);
                cat.playSound(SoundEvents.ARMOR_EQUIP_IRON, 0.8F, 1.5F);
                return true;
            }
        }
        return false;
    }

    public static boolean canCraftSelectedGunsmithRecipe(TalentedCat cat) {
        if (!isLoaded()) return false;
        try {
            return TacZGunsmith.canCraftSelectedRecipe(cat);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean craftSelectedGunsmithRecipe(TalentedCat cat) {
        if (!isLoaded()) return false;
        try {
            return TacZGunsmith.craftSelectedRecipe(cat);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static List<ResourceLocation> getAvailableGunsmithRecipes(int catLevel) {
        if (!isLoaded()) return Collections.emptyList();
        try {
            return TacZGunsmith.getAvailableRecipes(catLevel);
        } catch (Throwable ignored) {
            return Collections.emptyList();
        }
    }
}
