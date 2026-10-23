package com.kittytalents.compat.tacz;

import com.kittytalents.config.KittyTalentsConfig;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.talent.Talents;
import com.tacz.guns.api.event.common.EntityHurtByGunEvent;
import com.tacz.guns.api.event.common.EntityKillByGunEvent;
import com.tacz.guns.api.event.common.GunShootEvent;
import com.tacz.guns.api.item.IGun;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Objects;

/**
 * Forge event listeners for TacZ gun combat events.
 * Registered only when ModList.get().isLoaded("tacz") is true.
 */
public class TacZEvents {
    private static boolean registered = false;

    public static void register() {
        if (!registered) {
            registered = true;
            MinecraftForge.EVENT_BUS.register(new TacZEvents());
        }
    }

    @SubscribeEvent
    public void onEntityHurtByGunPre(EntityHurtByGunEvent.Pre event) {
        if (event.getLogicalSide().isClient()) return;

        LivingEntity attacker = event.getAttacker();
        Entity hurtEntity = event.getHurtEntity();
        if (hurtEntity == null) return;

        boolean friendlyFireAllowed = KittyTalentsConfig.COMMON.friendlyFire.get();

        // 1. Friendly-fire protection when a TalentedCat is the shooter
        if (attacker instanceof TalentedCat cat) {
            if (!friendlyFireAllowed) {
                if (hurtEntity == cat.getOwner()) {
                    event.setCanceled(true);
                    return;
                }
                if (hurtEntity instanceof Player p && (cat.isOwnedBy(p) || cat.isTrusted(p))) {
                    event.setCanceled(true);
                    return;
                }
                if (hurtEntity instanceof TalentedCat otherCat &&
                        cat.getOwnerUUID() != null &&
                        Objects.equals(cat.getOwnerUUID(), otherCat.getOwnerUUID())) {
                    event.setCanceled(true);
                    return;
                }
                if (cat.isAlliedTo(hurtEntity)) {
                    event.setCanceled(true);
                    return;
                }
            }

            // 2. Outgoing gun damage scaling for TalentedCat shooter
            double configMult = KittyTalentsConfig.COMMON.catGunDamageMultiplier.get();
            int armory = cat.getTalentLevel(Talents.CAT_ARMORY);
            int marksman = cat.getTalentLevel(Talents.MARKSMAN);
            double totalMult = configMult * (1.0D + 0.12D * armory) * (1.0D + 0.08D * marksman);
            event.setBaseAmount((float) (event.getBaseAmount() * totalMult));

            if (marksman >= 3) {
                event.setHeadshotMultiplier(event.getHeadshotMultiplier() + 0.25F);
            }
        }

        // 3. Friendly-fire protection & ballistic damage reduction when a TalentedCat is hit
        if (hurtEntity instanceof TalentedCat hitCat) {
            if (!friendlyFireAllowed && attacker instanceof Player p && (hitCat.isOwnedBy(p) || hitCat.isTrusted(p))) {
                event.setCanceled(true);
                return;
            }

            int catArmorLvl = hitCat.getTalentLevel(Talents.CAT_ARMOR);
            int armorDefense = 0;
            for (int i = 1; i <= 4; i++) {
                ItemStack piece = hitCat.getArmorSlotItem(i);
                if (piece.getItem() instanceof ArmorItem ai) {
                    armorDefense += ai.getDefense();
                }
            }
            double reduction = (catArmorLvl * 0.08D) + (armorDefense * 0.015D);
            double cap = KittyTalentsConfig.COMMON.catBallisticReductionCap.get();
            double clampedReduction = Math.min(cap, Math.max(0.0D, reduction));
            if (clampedReduction > 0.0D) {
                event.setBaseAmount((float) (event.getBaseAmount() * (1.0D - clampedReduction)));
            }
        }
    }

    @SubscribeEvent
    public void onGunShoot(GunShootEvent event) {
        if (event.getLogicalSide().isClient()) return;
        if (event.getShooter() instanceof TalentedCat cat) {
            cat.setLastShotTick(cat.tickCount);

            // AMMO_SCAVENGER: 7% chance per rank to refund the round into the magazine
            int scavenger = cat.getTalentLevel(Talents.AMMO_SCAVENGER);
            if (scavenger > 0 && cat.getRandom().nextFloat() < (0.07F * scavenger)) {
                ItemStack gun = event.getGunItemStack();
                IGun iGun = IGun.getIGunOrNull(gun);
                if (iGun != null) {
                    iGun.setCurrentAmmoCount(gun, iGun.getCurrentAmmoCount(gun) + 1);
                }
            }
        }
    }

    @SubscribeEvent
    public void onEntityKillByGun(EntityKillByGunEvent event) {
        if (event.getLogicalSide().isClient()) return;
        if (event.getAttacker() instanceof TalentedCat cat) {
            // Award XP chance on gun kill
            if (cat.getRandom().nextInt(5) == 0) {
                cat.setCatLevel(cat.getCatLevel() + 1);
                cat.setSkillPoints(cat.getSkillPoints() + 1);
            }

            // AMMO_SCAVENGER Rank 3+: 20% chance to scavenge extra rounds into ammoPouch
            int scavenger = cat.getTalentLevel(Talents.AMMO_SCAVENGER);
            if (scavenger >= 3 && cat.getRandom().nextFloat() < 0.20F) {
                ItemStack gun = cat.getEquippedWeapon();
                for (int i = 0; i < cat.getAmmoPouch().getContainerSize(); i++) {
                    ItemStack s = cat.getAmmoPouch().getItem(i);
                    if (!s.isEmpty() && TacZImpl.isMatchingAmmo(gun, s) && s.getCount() < s.getMaxStackSize()) {
                        int bonus = 2 + cat.getRandom().nextInt(5);
                        s.setCount(Math.min(s.getMaxStackSize(), s.getCount() + bonus));
                        break;
                    }
                }
            }
        }
    }
}
