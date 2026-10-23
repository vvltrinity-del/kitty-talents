package com.kittytalents.compat.tacz;

import com.kittytalents.entity.TalentedCat;
import com.kittytalents.talent.Talents;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.entity.ReloadState;
import com.tacz.guns.api.entity.ShootResult;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAmmoBox;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.resource.index.CommonGunIndex;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * Typed TacZ 1.1.8 implementation. Loaded ONLY when ModList.get().isLoaded("tacz") is true.
 */
public class TacZImpl {

    public static boolean isGun(ItemStack stack) {
        return !stack.isEmpty() && IGun.getIGunOrNull(stack) != null;
    }

    public static boolean isAmmo(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return IAmmo.getIAmmoOrNull(stack) != null || stack.getItem() instanceof IAmmoBox;
    }

    public static boolean isAttachment(ItemStack stack) {
        return !stack.isEmpty() && IAttachment.getIAttachmentOrNull(stack) != null;
    }

    public static boolean isMatchingAmmo(ItemStack gun, ItemStack ammo) {
        if (!isGun(gun) || ammo.isEmpty()) return false;
        IAmmo iAmmo = IAmmo.getIAmmoOrNull(ammo);
        if (iAmmo != null) {
            return iAmmo.isAmmoOfGun(gun, ammo);
        }
        if (ammo.getItem() instanceof IAmmoBox ammoBox) {
            return ammoBox.isAmmoBoxOfGun(gun, ammo);
        }
        return false;
    }

    public static int getAmmoCount(ItemStack gun) {
        IGun iGun = IGun.getIGunOrNull(gun);
        if (iGun == null) return 0;
        return iGun.getCurrentAmmoCount(gun) + (iGun.hasBulletInBarrel(gun) ? 1 : 0);
    }

    public static void setAmmoCount(ItemStack gun, int count) {
        IGun iGun = IGun.getIGunOrNull(gun);
        if (iGun != null) {
            iGun.setCurrentAmmoCount(gun, Math.max(0, count));
            iGun.setBulletInBarrel(gun, count > 0);
        }
    }

    public static int getMaxAmmoCount(ItemStack gun) {
        IGun iGun = IGun.getIGunOrNull(gun);
        if (iGun == null) return 30;
        ResourceLocation gunId = iGun.getGunId(gun);
        Optional<CommonGunIndex> opt = TimelessAPI.getCommonGunIndex(gunId);
        if (opt.isPresent() && opt.get().getGunData() != null) {
            return Math.max(1, opt.get().getGunData().getAmmoAmount());
        }
        return 30;
    }

    public static int getShootCooldownTicks(ItemStack gun) {
        IGun iGun = IGun.getIGunOrNull(gun);
        if (iGun != null) {
            int rpm = iGun.getRPM(gun);
            if (rpm > 0) {
                return Math.max(1, Math.round(1200.0F / rpm));
            }
        }
        return 8;
    }

    public static void onGunEquipped(TalentedCat cat, ItemStack gun) {
        if (cat.level().isClientSide || !isGun(gun)) return;
        IGunOperator op = IGunOperator.fromLivingEntity(cat);
        op.draw(cat::getEquippedWeapon);
    }

    public static void ensureGunDrawn(TalentedCat cat) {
        if (cat.level().isClientSide) return;
        IGunOperator op = IGunOperator.fromLivingEntity(cat);
        if (op.getDataHolder().currentGunItem == null || op.getDataHolder().currentGunItem.get() != cat.getEquippedWeapon()) {
            op.draw(cat::getEquippedWeapon);
        }
    }

    public static boolean isReloading(TalentedCat cat) {
        IGunOperator op = IGunOperator.fromLivingEntity(cat);
        ReloadState state = op.getSynReloadState();
        return state != null && state.getStateType().isReloading();
    }

    public static void setAiming(TalentedCat cat, boolean aiming) {
        IGunOperator op = IGunOperator.fromLivingEntity(cat);
        op.aim(aiming);
    }

    public static boolean triggerReload(TalentedCat cat, ItemStack gun) {
        if (!isGun(gun)) return false;
        ensureGunDrawn(cat);
        IGunOperator op = IGunOperator.fromLivingEntity(cat);
        if (isReloading(cat)) return true;

        // Trigger native TacZ reload (which reads from cat's ForgeCapabilities.ITEM_HANDLER -> CatAmmoAndPackItemHandler)
        op.reload();

        // Also support QUICK_RELOAD / instant fallback if native reload did not start
        int quickReload = cat.getTalentLevel(Talents.QUICK_RELOAD);
        if (!isReloading(cat) || quickReload >= 3) {
            return TacZCompat.tryReloadFromPack(cat, gun);
        }
        return true;
    }

    public static boolean shootGunAtTarget(TalentedCat cat, LivingEntity target) {
        ItemStack gun = cat.getEquippedWeapon();
        if (!isGun(gun)) return false;
        ensureGunDrawn(cat);

        IGunOperator op = IGunOperator.fromLivingEntity(cat);
        if (isReloading(cat)) return false;

        if (getAmmoCount(gun) <= 0) {
            if (!triggerReload(cat, gun)) {
                if (cat.tickCount % 20 == 0) {
                    cat.playSound(SoundEvents.DISPENSER_FAIL, 0.8F, 1.6F);
                }
            }
            return false;
        }

        int marksman = cat.getTalentLevel(Talents.MARKSMAN);
        if (marksman > 0) {
            op.aim(true);
        }

        // Aim at target chest/head
        Vec3 catEye = cat.getEyePosition();
        Vec3 targetAim = marksman >= 3 ? target.getEyePosition() : target.position().add(0, target.getBbHeight() * 0.65D, 0);
        Vec3 diff = targetAim.subtract(catEye);
        if (diff.lengthSqr() < 0.001D) return false;

        Vec3 dir = diff.normalize();
        double dXZ = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
        float basePitch = (float) (-(Math.atan2(dir.y, dXZ) * (180.0D / Math.PI)));
        float baseYaw = (float) (Math.atan2(dir.z, dir.x) * (180.0D / Math.PI)) - 90.0F;

        // Apply slight angular jitter reduced by MARKSMAN (15% per rank)
        float spreadScale = Math.max(0.10F, 1.0F - 0.15F * marksman);
        float jitterPitch = (cat.getRandom().nextFloat() - 0.5F) * 2.5F * spreadScale;
        float jitterYaw = (cat.getRandom().nextFloat() - 0.5F) * 2.5F * spreadScale;
        final float finalPitch = basePitch + jitterPitch;
        final float finalYaw = baseYaw + jitterYaw;

        cat.setXRot(finalPitch);
        cat.setYHeadRot(finalYaw);
        cat.setYRot(finalYaw);

        ShootResult result = op.shoot(() -> finalPitch, () -> finalYaw);
        return handleShootResult(cat, op, gun, result, finalPitch, finalYaw, target, false);
    }

    public static boolean shootGunTwined(TalentedCat cat, Player player) {
        ItemStack gun = cat.getEquippedWeapon();
        if (!isGun(gun)) return false;
        ensureGunDrawn(cat);

        IGunOperator op = IGunOperator.fromLivingEntity(cat);
        if (isReloading(cat)) return false;

        boolean isCreative = player.getAbilities().instabuild;
        if (!isCreative && getAmmoCount(gun) <= 0) {
            if (!triggerReload(cat, gun)) {
                cat.playSound(SoundEvents.DISPENSER_FAIL, 0.8F, 1.6F);
            }
            return false;
        }

        final float pitch = player.getXRot();
        final float yaw = player.getYRot();
        cat.setXRot(pitch);
        cat.setYHeadRot(yaw);
        cat.setYRot(yaw);

        ShootResult result = op.shoot(() -> pitch, () -> yaw);
        return handleShootResult(cat, op, gun, result, pitch, yaw, null, isCreative);
    }

    private static boolean handleShootResult(TalentedCat cat, IGunOperator op, ItemStack gun, ShootResult result,
                                             float pitch, float yaw, LivingEntity fallbackTarget, boolean isCreative) {
        if (result == ShootResult.SUCCESS) {
            cat.setLastShotTick(cat.tickCount);
            return true;
        }
        if (result == ShootResult.NEED_BOLT) {
            op.bolt();
            return false;
        }
        if (result == ShootResult.NO_AMMO) {
            triggerReload(cat, gun);
            return false;
        }
        if (result == ShootResult.NOT_DRAW || result == ShootResult.NOT_GUN) {
            op.draw(cat::getEquippedWeapon);
            // Fallback shot on the draw tick so responsiveness is immediate
            return TacZCompat.executeFallbackShot(cat, gun, pitch, yaw, fallbackTarget, isCreative);
        }
        if (result == ShootResult.COOL_DOWN) {
            return false;
        }
        return false;
    }

    public static boolean hasAmmoInCatInventories(TalentedCat cat, ItemStack gun) {
        SimpleContainer pouch = cat.getAmmoPouch();
        for (int i = 0; i < pouch.getContainerSize(); i++) {
            ItemStack s = pouch.getItem(i);
            if (!s.isEmpty() && isMatchingAmmo(gun, s)) return true;
        }
        SimpleContainer pack = cat.getCatPack();
        for (int i = 0; i < pack.getContainerSize(); i++) {
            ItemStack s = pack.getItem(i);
            if (!s.isEmpty() && isMatchingAmmo(gun, s)) return true;
        }
        return false;
    }

    public static boolean consumeOneAmmoFromCatInventories(TalentedCat cat, ItemStack gun) {
        if (consumeFromContainer(cat.getAmmoPouch(), gun)) return true;
        return consumeFromContainer(cat.getCatPack(), gun);
    }

    private static boolean consumeFromContainer(SimpleContainer container, ItemStack gun) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty() && isMatchingAmmo(gun, stack)) {
                if (stack.getItem() instanceof IAmmoBox box) {
                    if (box.isAllTypeCreative(stack) || box.isCreative(stack)) return true;
                    int count = box.getAmmoCount(stack);
                    if (count > 0) {
                        box.setAmmoCount(stack, count - 1);
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
}
