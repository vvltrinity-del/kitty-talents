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

import java.lang.reflect.Method;
import java.util.function.Supplier;

public class TacZCompat {
    private static Boolean taczLoaded = null;
    private static Method fromLivingEntityMethod = null;
    private static Method shootMethod = null;
    private static Method drawMethod = null;
    private static Method getIGunOrNullMethod = null;
    private static Method getCurrentAmmoCountMethod = null;
    private static Method setCurrentAmmoCountMethod = null;
    private static Method getIAmmoOrNullMethod = null;
    private static Method isAmmoOfGunMethod = null;

    public static boolean isLoaded() {
        if (taczLoaded == null) {
            taczLoaded = ModList.get().isLoaded("tacz");
            if (taczLoaded) {
                initReflection();
            }
        }
        return taczLoaded;
    }

    private static void initReflection() {
        try {
            Class<?> igunOperatorClass = Class.forName("com.tacz.guns.api.entity.IGunOperator");
            fromLivingEntityMethod = igunOperatorClass.getMethod("fromLivingEntity", LivingEntity.class);
            shootMethod = igunOperatorClass.getMethod("shoot", Supplier.class, Supplier.class);
            drawMethod = igunOperatorClass.getMethod("draw", Supplier.class);

            Class<?> igunClass = Class.forName("com.tacz.guns.api.item.IGun");
            getIGunOrNullMethod = igunClass.getMethod("getIGunOrNull", ItemStack.class);
            getCurrentAmmoCountMethod = igunClass.getMethod("getCurrentAmmoCount", ItemStack.class);
            setCurrentAmmoCountMethod = igunClass.getMethod("setCurrentAmmoCount", ItemStack.class, int.class);

            Class<?> iAmmoClass = Class.forName("com.tacz.guns.api.item.IAmmo");
            getIAmmoOrNullMethod = iAmmoClass.getMethod("getIAmmoOrNull", ItemStack.class);
            isAmmoOfGunMethod = iAmmoClass.getMethod("isAmmoOfGun", ItemStack.class, ItemStack.class);
        } catch (Throwable ignored) {
            // Reflection fallback will use direct NBT access
        }
    }

    public static boolean isGun(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (!isLoaded()) return false;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id != null && id.getNamespace().equals("tacz")) {
            if (stack.hasTag() && stack.getTag().contains("GunId")) return true;
            String path = id.getPath().toLowerCase();
            if (path.contains("gun") || path.contains("rifle") || path.contains("pistol") ||
                path.contains("shotgun") || path.contains("smg") || path.contains("sniper")) {
                return true;
            }
        }
        if (getIGunOrNullMethod != null) {
            try {
                return getIGunOrNullMethod.invoke(null, stack) != null;
            } catch (Throwable ignored) {}
        }
        return false;
    }

    public static boolean isAmmo(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (!isLoaded()) return false;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id != null && id.getNamespace().equals("tacz")) {
            String path = id.getPath().toLowerCase();
            if (path.contains("ammo") || path.contains("bullet") || path.contains("box") || path.contains("round")) {
                return true;
            }
        }
        if (getIAmmoOrNullMethod != null) {
            try {
                return getIAmmoOrNullMethod.invoke(null, stack) != null;
            } catch (Throwable ignored) {}
        }
        return false;
    }

    public static boolean isMatchingAmmo(ItemStack gun, ItemStack ammo) {
        if (!isGun(gun) || ammo.isEmpty()) return false;
        ResourceLocation ammoId = ForgeRegistries.ITEMS.getKey(ammo.getItem());
        if (ammoId == null || !ammoId.getNamespace().equals("tacz")) return false;

        if (getIAmmoOrNullMethod != null && isAmmoOfGunMethod != null) {
            try {
                Object ammoObj = getIAmmoOrNullMethod.invoke(null, ammo);
                if (ammoObj != null) {
                    Object res = isAmmoOfGunMethod.invoke(ammoObj, gun, ammo);
                    if (res instanceof Boolean b && b) return true;
                }
            } catch (Throwable ignored) {}
        }

        return true;
    }

    public static boolean hasAmmoInPack(TalentedCat cat, ItemStack gun) {
        SimpleContainer pack = cat.getCatPack();
        for (int i = 0; i < pack.getContainerSize(); i++) {
            ItemStack stack = pack.getItem(i);
            if (!stack.isEmpty() && isMatchingAmmo(gun, stack)) {
                return true;
            }
        }
        return false;
    }

    public static boolean consumeAmmoFromPack(TalentedCat cat, ItemStack gun) {
        SimpleContainer pack = cat.getCatPack();
        for (int i = 0; i < pack.getContainerSize(); i++) {
            ItemStack stack = pack.getItem(i);
            if (!stack.isEmpty() && isMatchingAmmo(gun, stack)) {
                if (stack.hasTag() && stack.getTag().contains("AmmoCount")) {
                    int boxCount = stack.getTag().getInt("AmmoCount");
                    if (boxCount > 0) {
                        stack.getTag().putInt("AmmoCount", boxCount - 1);
                        return true;
                    }
                } else {
                    stack.shrink(1);
                    if (stack.isEmpty()) {
                        pack.setItem(i, ItemStack.EMPTY);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    public static int getAmmoCount(ItemStack gun) {
        if (getCurrentAmmoCountMethod != null && getIGunOrNullMethod != null) {
            try {
                Object iGun = getIGunOrNullMethod.invoke(null, gun);
                if (iGun != null) {
                    return (int) getCurrentAmmoCountMethod.invoke(iGun, gun);
                }
            } catch (Throwable ignored) {}
        }
        if (gun.hasTag() && gun.getTag().contains("GunCurrentAmmoCount")) {
            return gun.getTag().getInt("GunCurrentAmmoCount");
        }
        return 0;
    }

    public static void setAmmoCount(ItemStack gun, int count) {
        if (setCurrentAmmoCountMethod != null && getIGunOrNullMethod != null) {
            try {
                Object iGun = getIGunOrNullMethod.invoke(null, gun);
                if (iGun != null) {
                    setCurrentAmmoCountMethod.invoke(iGun, gun, count);
                    return;
                }
            } catch (Throwable ignored) {}
        }
        gun.getOrCreateTag().putInt("GunCurrentAmmoCount", count);
        gun.getOrCreateTag().putBoolean("HasBulletInBarrel", count > 0);
    }

    public static int getShootCooldownTicks(ItemStack gun) {
        if (gun.hasTag() && gun.getTag().contains("GunId")) {
            String gunId = gun.getTag().getString("GunId").toLowerCase();
            if (gunId.contains("sniper") || gunId.contains("shotgun")) return 18;
            if (gunId.contains("smg") || gunId.contains("pistol")) return 6;
            if (gunId.contains("rifle")) return 8;
        }
        return 8;
    }

    public static boolean tryReloadFromPack(TalentedCat cat, ItemStack gun) {
        if (!hasAmmoInPack(cat, gun)) return false;
        int current = getAmmoCount(gun);
        int max = 30;
        if (gun.hasTag() && gun.getTag().contains("MaxAmmo")) {
            max = gun.getTag().getInt("MaxAmmo");
        }
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
        ItemStack gun = cat.getEquippedWeapon();
        if (!isGun(gun)) return false;

        // Try reload from catPack if gun magazine is empty
        if (getAmmoCount(gun) <= 0) {
            if (!tryReloadFromPack(cat, gun)) {
                if (cat.tickCount % 20 == 0) {
                    cat.playSound(SoundEvents.DISPENSER_FAIL, 0.8F, 1.6F);
                }
                return false;
            }
        }

        // Aim at target
        Vec3 catEye = cat.getEyePosition();
        Vec3 targetEye = target.getEyePosition();
        Vec3 diff = targetEye.subtract(catEye);
        if (diff.lengthSqr() < 0.001D) return false;

        Vec3 dir = diff.normalize();
        double dX = dir.x;
        double dY = dir.y;
        double dZ = dir.z;
        double dXZ = Math.sqrt(dX * dX + dZ * dZ);
        float pitch = (float) (-(Math.atan2(dY, dXZ) * (180.0D / Math.PI)));
        float yaw = (float) (Math.atan2(dZ, dX) * (180.0D / Math.PI)) - 90.0F;

        cat.setXRot(pitch);
        cat.setYHeadRot(yaw);
        cat.setYRot(yaw);

        return executeShot(cat, gun, pitch, yaw, target, false);
    }

    public static boolean shootGunTwined(TalentedCat cat, Player player) {
        if (!isLoaded()) return false;
        ItemStack gun = cat.getEquippedWeapon();
        if (!isGun(gun)) return false;

        boolean isCreative = player.getAbilities().instabuild;
        if (!isCreative && getAmmoCount(gun) <= 0 && !hasAmmoInPack(cat, gun)) {
            cat.playSound(SoundEvents.DISPENSER_FAIL, 0.8F, 1.6F);
            return false;
        }

        if (!isCreative && getAmmoCount(gun) <= 0) {
            tryReloadFromPack(cat, gun);
        }

        float pitch = player.getXRot();
        float yaw = player.getYRot();
        cat.setXRot(pitch);
        cat.setYHeadRot(yaw);
        cat.setYRot(yaw);

        // Raycast to find target along player look vector
        Vec3 eye = cat.getEyePosition();
        Vec3 look = cat.getLookAngle();
        Vec3 end = eye.add(look.scale(64.0D));
        LivingEntity hitTarget = null;
        for (LivingEntity e : cat.level().getEntitiesOfClass(LivingEntity.class, cat.getBoundingBox().inflate(64.0D),
                entity -> entity != cat && entity != player && !cat.isAlliedTo(entity))) {
            if (e.getBoundingBox().inflate(0.6D).clip(eye, end).isPresent()) {
                hitTarget = e;
                break;
            }
        }

        return executeShot(cat, gun, pitch, yaw, hitTarget, isCreative);
    }

    public static boolean executeShot(TalentedCat cat, ItemStack gun, float pitch, float yaw, LivingEntity target, boolean isCreative) {
        if (!isCreative) {
            int ammo = getAmmoCount(gun);
            if (ammo > 0) {
                setAmmoCount(gun, ammo - 1);
            } else {
                boolean consumed = consumeAmmoFromPack(cat, gun);
                if (!consumed) {
                    cat.playSound(SoundEvents.DISPENSER_FAIL, 0.8F, 1.6F);
                    return false;
                }
            }
        }

        // Recoil pushback
        Vec3 look = cat.getLookAngle();
        if (look.lengthSqr() > 0.001D) {
            cat.push(-look.x * 0.06D, 0.02D, -look.z * 0.06D);
        }

        // Muzzle flash particle & gunshot sound
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

    /**
     * When out of ammo for the equipped firearm:
     * 1. Searches catPack for a melee weapon (Sword/Axe) and equips it.
     * 2. If no melee weapon exists, unbinds/stows the empty gun so the cat fights with natural claws.
     */
    public static boolean handleEmptyAmmoSwapOrStow(TalentedCat cat) {
        if (!isLoaded()) return false;
        ItemStack gun = cat.getEquippedWeapon();
        if (!isGun(gun)) return false;

        // Check if truly empty
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
            // Stow gun into pack if empty slot exists
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

    /**
     * If cat has stowed a firearm and later receives ammo in pack, re-equips the firearm.
     */
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
}
