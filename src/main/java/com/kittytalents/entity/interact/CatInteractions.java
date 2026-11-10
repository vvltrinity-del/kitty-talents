package com.kittytalents.entity.interact;

import com.kittytalents.config.KittyTalentsConfig;
import com.kittytalents.core.component.CatCombatState;
import com.kittytalents.core.component.CatEquipment;
import com.kittytalents.core.component.CatStatistics;
import com.kittytalents.core.component.CatTalentLevels;
import com.kittytalents.core.component.CatTrust;
import com.kittytalents.core.persist.CatSchema;
import com.kittytalents.entity.ai.*;
import com.kittytalents.inventory.CatPackMenu;
import com.kittytalents.inventory.CatTalentMenu;
import com.kittytalents.item.CatTreatItem;
import com.kittytalents.item.WarmMilkItem;
import com.kittytalents.network.PacketHandler;
import com.kittytalents.network.PacketSyncCatEquipment;
import com.kittytalents.registry.ModEntities;
import com.kittytalents.registry.ModItems;
import com.kittytalents.talent.Talent;
import com.kittytalents.talent.Talents;
import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import com.kittytalents.entity.*;

/**
 * Right-click interactions with a {@link TalentedCat} (taming, feeding, treats, items, petting, menus).
 * Extracted from TalentedCat.mobInteract in Phase 2; Phase 7 routes the permission checks through CatPermissions.
 */
public final class CatInteractions {
    private CatInteractions() {}

    /**
     * @return the interaction result, or {@code null} if no Kitty Talents interaction applies and vanilla
     *         {@code Cat#mobInteract} should handle it.
     */
    @javax.annotation.Nullable
    public static InteractionResult handle(TalentedCat cat, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);

        // Spirit Catnip must reach its own item logic (primes the guaranteed Nekomata). It also counts as breeding
        // food, so without this the vanilla Cat#mobInteract fallback consumed it as plain breeding catnip first.
        if (held.getItem() instanceof com.kittytalents.item.SpiritCatnipItem spirit) {
            InteractionResult r = spirit.interactLivingEntity(held, player, cat, hand);
            if (r.consumesAction()) return r;
        }

        // Taming untamed Talented Cat
        if (!cat.isTame() || cat.getOwnerUUID() == null) {
            if (held.is(ModItems.CATNIP_TREAT.get())) {
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                if (!cat.level().isClientSide) {
                    cat.tame(player);
                    cat.rollTraitIfNone();
                    cat.setOrderedToSit(true);
                    cat.level().broadcastEntityEvent(cat, (byte) 7); // Heart particles
                    player.sendSystemMessage(Component.literal("You tamed " + cat.getName().getString() + "!").withStyle(ChatFormatting.GREEN));
                }
                cat.playSound(SoundEvents.CAT_PURREOW, 1.0F, 1.0F);
                return InteractionResult.sidedSuccess(cat.level().isClientSide);
            } else if (held.is(Items.COD) || held.is(Items.SALMON)) {
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                if (!cat.level().isClientSide) {
                    if (cat.getRandom().nextInt(3) == 0) {
                        cat.tame(player);
                    cat.rollTraitIfNone();
                        cat.setOrderedToSit(true);
                        cat.level().broadcastEntityEvent(cat, (byte) 7); // Heart particles
                        player.sendSystemMessage(Component.literal("You tamed " + cat.getName().getString() + "!").withStyle(ChatFormatting.GREEN));
                    } else {
                        cat.level().broadcastEntityEvent(cat, (byte) 6); // Smoke particles
                    }
                }
                cat.playSound(SoundEvents.CAT_EAT, 0.8F, 1.0F);
                return InteractionResult.sidedSuccess(cat.level().isClientSide);
            } else if (held.is(ModItems.CATNIP.get())) {
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                if (!cat.level().isClientSide) {
                    if (cat.getRandom().nextInt(2) == 0) {
                        cat.tame(player);
                    cat.rollTraitIfNone();
                        cat.setOrderedToSit(true);
                        cat.level().broadcastEntityEvent(cat, (byte) 7); // Heart particles
                        player.sendSystemMessage(Component.literal("You tamed " + cat.getName().getString() + " with Catnip!").withStyle(ChatFormatting.GREEN));
                    } else {
                        cat.level().broadcastEntityEvent(cat, (byte) 6); // Smoke particles
                    }
                }
                cat.playSound(SoundEvents.CAT_PURREOW, 0.9F, 1.1F);
                return InteractionResult.sidedSuccess(cat.level().isClientSide);
            }
        }

        // Storing Gun Ammo into ammo pouch / backpack by right-clicking with ammo
        if (com.kittytalents.compat.tacz.TacZCompat.isAmmo(held) && (cat.isOwnedBy(player) || cat.isTrusted(player.getUUID()))) {
            ItemStack remaining = cat.equipment().ammoPouch().addItem(held.copy());
            if (!remaining.isEmpty() && cat.getPackSlotCount() > 0) {
                remaining = cat.equipment().pack().addItem(remaining);
            }
            int added = held.getCount() - remaining.getCount();
            if (added > 0) {
                if (!player.getAbilities().instabuild) {
                    held.setCount(remaining.getCount());
                }
                cat.playSound(SoundEvents.BUNDLE_INSERT, 0.8F, 1.2F);
                if (!cat.level().isClientSide) {
                    player.sendSystemMessage(Component.literal("Stored " + added + " ammo in " + cat.getName().getString() + "'s ammo pouch!").withStyle(ChatFormatting.AQUA));
                }
                return InteractionResult.sidedSuccess(cat.level().isClientSide);
            }
        }

        // Direct equip by right-clicking with gun or weapon
        if ((cat.isOwnedBy(player) || cat.isTrusted(player.getUUID())) && (com.kittytalents.compat.tacz.TacZCompat.isGun(held) || held.getItem() instanceof net.minecraft.world.item.SwordItem)) {
            if (cat.tryAutoEquip(held, player)) {
                return InteractionResult.sidedSuccess(cat.level().isClientSide);
            }
        }

        // Right-clicking with Stick or Laser Pointer opens companion talent screen
        if ((cat.isOwnedBy(player) || cat.isTrusted(player.getUUID())) && (held.is(Items.STICK) || held.is(ModItems.CAT_LASER_POINTER.get()))) {
            if (player instanceof ServerPlayer serverPlayer) {
                cat.openTalentScreen(serverPlayer);
            }
            return InteractionResult.sidedSuccess(cat.level().isClientSide);
        }

        // Coal Stats Reset Protection (Sneak + Right-Click with Coal or Charcoal)
        if (player.isShiftKeyDown() && (cat.isPrimaryOwner(player) || player.getAbilities().instabuild) && (held.is(Items.COAL) || held.is(Items.CHARCOAL))) {
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
            cat.resetStatistics();
            cat.playSound(SoundEvents.FIRE_EXTINGUISH, 0.8F, 0.8F);
            if (cat.level() instanceof ServerLevel serverLevel) {
                for (int i = 0; i < 15; i++) {
                    serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, cat.getX(), cat.getY() + 0.3D, cat.getZ(),
                            1, (serverLevel.random.nextDouble() - 0.5D) * 0.3D, 0.1D, (serverLevel.random.nextDouble() - 0.5D) * 0.3D, 0.02D);
                }
            }
            player.displayClientMessage(Component.literal(cat.getName().getString() + "'s stats and memories have been wiped clean.").withStyle(ChatFormatting.GRAY), false);
            return InteractionResult.sidedSuccess(cat.level().isClientSide);
        }

        // Petting with sneak + bare hand!
        if (player.isShiftKeyDown() && cat.isOwnedBy(player) && held.isEmpty()) {
            cat.playSound(SoundEvents.CAT_PURR, 1.0F, 1.0F);
            cat.playSound(SoundEvents.CAT_PURREOW, 0.9F, 1.2F);
            if (cat.level().isClientSide) {
                for (int i = 0; i < 5; i++) {
                    cat.level().addParticle(ParticleTypes.HEART, cat.getRandomX(0.5D), cat.getY() + 0.4D, cat.getRandomZ(0.5D), 0, 0.05D, 0);
                }
            } else {
                cat.heal(2.0F);
                cat.increaseHunger(5);
                cat.incrementTimesPet();
                cat.syncStats();
                cat.gainBond("pet", com.kittytalents.core.living.CatBond.PET_POINTS.i());
                if (cat.hasBondPerk(com.kittytalents.core.living.CatBond.Perk.COMFORT)) {
                    player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.REGENERATION, 100, 0));
                }
                String[] petStyles = {"Head Scratch", "Chin Scratch", "Belly Rub", "Back Scratch"};
                String style = petStyles[cat.getRandom().nextInt(petStyles.length)];
                player.displayClientMessage(Component.literal("You gave " + cat.getName().getString() + " a loving " + style + "! ❤").withStyle(ChatFormatting.LIGHT_PURPLE), true);
            }
            return InteractionResult.sidedSuccess(cat.level().isClientSide);
        }

        // 4.0 Phase 6: play with String (bond level 3+): the cat hops after it and gets its Energy back.
        if (held.is(Items.STRING) && cat.isOwnedBy(player) && !cat.isIncapacitated()) {
            if (!cat.level().isClientSide) {
                if (!cat.hasBondPerk(com.kittytalents.core.living.CatBond.Perk.PLAY)) {
                    player.displayClientMessage(Component.translatable("message.kittytalents.play_needs_bond",
                            cat.getName(), com.kittytalents.core.living.CatBond.Perk.PLAY.level).withStyle(ChatFormatting.GRAY), true);
                } else if (cat.gainBond("play", com.kittytalents.core.living.CatBond.PLAY_POINTS.i())) {
                    cat.setOrderedToSit(false);
                    cat.getLookControl().setLookAt(player);
                    cat.setDeltaMovement(cat.getDeltaMovement().add(0, 0.42D, 0));
                    cat.hasImpulse = true;
                    cat.playSound(SoundEvents.CAT_AMBIENT, 1.0F, 1.5F);
                    com.kittytalents.combat.FelineEnergyEngine.restoreEnergy(cat, com.kittytalents.core.living.CatBond.PLAY_ENERGY.i());
                    cat.level().broadcastEntityEvent(cat, (byte) 7);
                } else {
                    player.displayClientMessage(Component.translatable("message.kittytalents.play_tired", cat.getName()).withStyle(ChatFormatting.GRAY), true);
                }
            }
            return InteractionResult.sidedSuccess(cat.level().isClientSide);
        }

        // Colorable Collar with Dyes
        if (held.getItem() instanceof DyeItem dye && cat.isOwnedBy(player)) {
            DyeColor dyeColor = dye.getDyeColor();
            if (dyeColor != cat.getCollarColor()) {
                cat.setCollarColor(dyeColor);
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                cat.playSound(SoundEvents.DYE_USE, 1.0F, 1.0F);
                return InteractionResult.sidedSuccess(cat.level().isClientSide);
            }
        }

        // Dire Treat Awakening
        if (held.is(ModItems.DIRE_TREAT.get()) && cat.isOwnedBy(player)) {
            if (!cat.isDireAwakened()) {
                int reqLvl = KittyTalentsConfig.COMMON.direLevelRequirement.get();
                if (cat.getCatLevel() >= reqLvl) {
                    if (!player.getAbilities().instabuild) {
                        held.shrink(1);
                    }
                    cat.setDireAwakened(true);
                    cat.increaseHunger(20);
                    cat.syncStats();
                    cat.playSound(SoundEvents.TOTEM_USE, 1.0F, 1.0F);
                    cat.playSound(SoundEvents.ENDER_DRAGON_GROWL, 0.5F, 1.8F);
                    if (cat.level() instanceof ServerLevel serverLevel) {
                        for (int i = 0; i < 30; i++) {
                            serverLevel.sendParticles(ParticleTypes.DRAGON_BREATH, cat.getX(), cat.getY() + 0.5D, cat.getZ(),
                                    1, (serverLevel.random.nextDouble() - 0.5D), 0.5D, (serverLevel.random.nextDouble() - 0.5D), 0.1D);
                        }
                    }
                    player.sendSystemMessage(Component.translatable("message.kittytalents.dire_awakened", cat.getName()));
                    return InteractionResult.sidedSuccess(cat.level().isClientSide);
                } else {
                    if (!cat.level().isClientSide) {
                        player.sendSystemMessage(Component.translatable("message.kittytalents.dire_level_req", reqLvl));
                    }
                    return InteractionResult.sidedSuccess(cat.level().isClientSide);
                }
            }
        }

        // Reviving with Warm Milk or Golden Apple
        if (cat.isIncapacitated()) {
            if (held.getItem() instanceof WarmMilkItem || held.is(Items.MILK_BUCKET) || held.is(Items.GOLDEN_APPLE)) {
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                    if (held.is(Items.MILK_BUCKET)) {
                        player.addItem(new ItemStack(Items.BUCKET));
                    }
                }
                cat.setIncapacitated(false);
                cat.heal(12.0F);
                cat.increaseHunger(50);
                cat.playSound(SoundEvents.PLAYER_BURP, 0.8F, 1.2F);
                cat.playSound(SoundEvents.CAT_PURREOW, 1.0F, 1.0F);
                return InteractionResult.sidedSuccess(cat.level().isClientSide);
            } else {
                if (!cat.level().isClientSide) {
                    player.sendSystemMessage(Component.translatable("message.kittytalents.needs_milk"));
                }
                return InteractionResult.sidedSuccess(cat.level().isClientSide);
            }
        }

        // Treats for leveling
        if (held.getItem() instanceof CatTreatItem treatItem && cat.isOwnedBy(player)) {
            // 4.0 Phase 6: a treat helps a kitten grow (and still levels it when it can).
            if (cat.isBaby() && !cat.level().isClientSide && cat.feedGrowth(true)) {
                cat.gainBond("feed", com.kittytalents.core.living.CatBond.FEED_POINTS.i());
                if (!treatItem.canLevelUp(cat)) {
                    if (!player.getAbilities().instabuild) held.shrink(1);
                    cat.playSound(SoundEvents.CAT_EAT, 0.8F, 1.3F);
                    return InteractionResult.SUCCESS;
                }
            }
            if (treatItem.canLevelUp(cat)) {
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                cat.setCatLevel(cat.getCatLevel() + 1);
                cat.setSkillPoints(cat.getSkillPoints() + 1);
                cat.increaseHunger(20);
                cat.heal(4.0F);
                cat.syncStats();
                cat.playSound(SoundEvents.PLAYER_LEVELUP, 0.7F, 1.3F);
                return InteractionResult.sidedSuccess(cat.level().isClientSide);
            } else {
                if (!cat.level().isClientSide) {
                    player.sendSystemMessage(Component.translatable("message.kittytalents.treat_too_low"));
                }
                return InteractionResult.sidedSuccess(cat.level().isClientSide);
            }
        }

        // Fish & meats for food & healing
        if (held.is(Items.COD) || held.is(Items.COOKED_COD) || held.is(Items.SALMON) || held.is(Items.COOKED_SALMON) ||
                (cat.getTalentLevel(Talents.HAPPY_EATER) > 0 && (held.is(Items.CHICKEN) || held.is(Items.COOKED_CHICKEN) || held.is(Items.BEEF) || held.is(Items.PORKCHOP)))) {
            if (cat.getHunger() < 120 || cat.getHealth() < cat.getMaxHealth() || cat.isBaby()) {
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                cat.increaseHunger(cat.foodValue(25));
                cat.heal(6.0F);
                if (!cat.level().isClientSide) {
                    cat.feedGrowth(false);
                    if (cat.isOwnedBy(player)) cat.gainBond("feed", com.kittytalents.core.living.CatBond.FEED_POINTS.i());
                }
                cat.syncStats();
                cat.playSound(SoundEvents.CAT_EAT, 0.8F, 1.0F);
                return InteractionResult.sidedSuccess(cat.level().isClientSide);
            }
        }

        // Mounting the cat
        if (cat.getTalentLevel(Talents.CAT_MOUNT) > 0 && (cat.isOwnedBy(player) || cat.isTrusted(player.getUUID()) || player.getAbilities().instabuild) && (held.isEmpty() || held.is(Items.SADDLE))) {
            if (!cat.level().isClientSide && !cat.isVehicle()) {
                player.startRiding(cat);
            }
            return InteractionResult.sidedSuccess(cat.level().isClientSide);
        }

        // Normal right click toggles sit/stand
        if ((cat.isOwnedBy(player) || cat.isTrusted(player.getUUID())) && held.isEmpty()) {
            cat.setCatMode(cat.getCatMode() == CatMode.DOCILE ? CatMode.FOLLOW : CatMode.DOCILE);
            return InteractionResult.sidedSuccess(cat.level().isClientSide);
        }

        return null;
    }
}
