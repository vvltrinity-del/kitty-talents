package com.kittytalents.event;

import com.kittytalents.KittyTalents;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.registry.ModEntities;
import com.kittytalents.registry.ModItems;
import com.kittytalents.talent.Talents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = KittyTalents.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeEvents {

    @SubscribeEvent
    public static void onCommandsRegister(net.minecraftforge.event.RegisterCommandsEvent event) {
        com.kittytalents.command.KittyTalentsCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        // Cow milking with Catnip Bucket
        if ((event.getTarget() instanceof net.minecraft.world.entity.animal.Cow || event.getTarget() instanceof net.minecraft.world.entity.animal.MushroomCow) &&
                !((net.minecraft.world.entity.animal.Animal) event.getTarget()).isBaby()) {
            Player player = event.getEntity();
            ItemStack held = player.getItemInHand(event.getHand());
            if (held.is(ModItems.CATNIP_BUCKET.get())) {
                player.playSound(SoundEvents.COW_MILK, 1.0F, 1.0F);
                ItemStack milk = net.minecraft.world.item.ItemUtils.createFilledResult(held, player, new ItemStack(ModItems.CATNIP_MILK_BUCKET.get()));
                player.setItemInHand(event.getHand(), milk);
                event.setCancellationResult(InteractionResult.sidedSuccess(player.level().isClientSide));
                event.setCanceled(true);
                return;
            }
        }

        if (event.getTarget() instanceof Cat cat && !(cat instanceof TalentedCat)) {
            Player player = event.getEntity();
            ItemStack held = player.getItemInHand(event.getHand());

            if (held.is(ModItems.CATNIP_TREAT.get())) {
                if (cat.isTame() && cat.isOwnedBy(player)) {
                    Level level = cat.level();
                    if (!level.isClientSide) {
                        TalentedCat talentedCat = ModEntities.TALENTED_CAT.get().create(level);
                        if (talentedCat != null) {
                            talentedCat.moveTo(cat.getX(), cat.getY(), cat.getZ(), cat.getYRot(), cat.getXRot());
                            talentedCat.tame(player);
                            talentedCat.setVariant(cat.getVariant());
                            talentedCat.setCatVariant(com.kittytalents.entity.CatVariant.fromVanillaCatVariant(cat.getVariant()));
                            talentedCat.setFemale(cat.getRandom().nextBoolean());
                            talentedCat.setCollarColor(cat.getCollarColor());
                            talentedCat.setHealth(cat.getHealth());
                            if (cat.hasCustomName()) {
                                talentedCat.setCustomName(cat.getCustomName());
                            }
                            talentedCat.setCatLevel(1);
                            talentedCat.setSkillPoints(1); // Bonus starter point!

                            level.addFreshEntity(talentedCat);
                            cat.discard();

                            if (!player.getAbilities().instabuild) {
                                held.shrink(1);
                            }

                            level.playSound(null, talentedCat.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.2F);
                            player.sendSystemMessage(Component.translatable("message.kittytalents.cat_trained", talentedCat.getName()));

                            if (level instanceof ServerLevel serverLevel) {
                                for (int i = 0; i < 12; i++) {
                                    serverLevel.sendParticles(
                                            ParticleTypes.HAPPY_VILLAGER,
                                            talentedCat.getX() + (serverLevel.random.nextDouble() - 0.5D),
                                            talentedCat.getY() + 0.5D + (serverLevel.random.nextDouble() - 0.5D),
                                            talentedCat.getZ() + (serverLevel.random.nextDouble() - 0.5D),
                                            1, 0, 0.1D, 0, 0.05D
                                    );
                                }
                            }
                        }
                    }
                    event.setCancellationResult(InteractionResult.SUCCESS);
                    event.setCanceled(true);
                } else if (!cat.isTame()) {
                    if (!cat.level().isClientSide) {
                        player.sendSystemMessage(Component.translatable("message.kittytalents.must_tame_first"));
                    }
                    event.setCancellationResult(InteractionResult.SUCCESS);
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerWakeUp(PlayerWakeUpEvent event) {
        Player player = event.getEntity();
        Level level = player.level();
        if (!level.isClientSide) {
            List<TalentedCat> cats = level.getEntitiesOfClass(
                    TalentedCat.class,
                    player.getBoundingBox().inflate(12.0D),
                    cat -> cat.isOwnedBy(player) && !cat.isIncapacitated() && cat.getTalentLevel(Talents.MOUSER) > 0
            );

            for (TalentedCat cat : cats) {
                int mouserLevel = cat.getTalentLevel(Talents.MOUSER);
                ItemStack gift;
                int roll = level.random.nextInt(100);
                if (roll < 35) {
                    gift = new ItemStack(Items.STRING, 1 + level.random.nextInt(2));
                } else if (roll < 65) {
                    gift = new ItemStack(Items.FEATHER, 1 + level.random.nextInt(3));
                } else if (roll < 85) {
                    gift = new ItemStack(Items.RAW_COPPER, 1);
                } else if (roll < 95 && mouserLevel >= 3) {
                    gift = new ItemStack(Items.RABBIT_FOOT, 1);
                } else if (mouserLevel >= 5) {
                    gift = new ItemStack(Items.EMERALD, 1);
                } else {
                    gift = new ItemStack(Items.COOKED_COD, 1);
                }

                // Addons may redirect the gift (e.g. into a backpack); cancelled means it was delivered elsewhere.
                var reward = new com.kittytalents.api.event.CatRewardEvent(cat, gift);
                boolean taken = net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(reward);
                gift = reward.getReward();
                if (!taken && !gift.isEmpty()) {
                    ItemEntity itemEntity = new ItemEntity(level, player.getX(), player.getY() + 0.5D, player.getZ(), gift);
                    level.addFreshEntity(itemEntity);
                }
                if (gift.isEmpty()) continue;
                player.sendSystemMessage(Component.translatable("message.kittytalents.morning_gift", cat.getName(), gift.getHoverName()));
            }
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!event.getLevel().isClientSide()) {
            if (event.getState().is(Blocks.GRASS) || event.getState().is(Blocks.TALL_GRASS) || event.getState().is(Blocks.FERN)) {
                if (event.getLevel().getRandom().nextFloat() < 0.15F) {
                    BlockPos pos = event.getPos();
                    ItemEntity itemEntity = new ItemEntity(
                            (Level) event.getLevel(),
                            pos.getX() + 0.5D,
                            pos.getY() + 0.5D,
                            pos.getZ() + 0.5D,
                            new ItemStack(ModItems.CATNIP.get())
                    );
                    event.getLevel().addFreshEntity(itemEntity);
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Twine failsafes & dormant-body protection
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void onPlayerLogin(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer sp) {
            com.kittytalents.entity.TwineManager.restoreOnLogin(sp); // body flags left by a crash mid-Twine
        }
    }

    @SubscribeEvent
    public static void onServerStopped(net.minecraftforge.event.server.ServerStoppedEvent event) {
        com.kittytalents.entity.TwineManager.clearAll(); // static maps must not leak into the next world
    }

    @SubscribeEvent
    public static void onPlayerLogout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer sp) {
            com.kittytalents.entity.TwineManager.endForPlayer(sp, null);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangeDim(net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer sp) {
            com.kittytalents.entity.TwineManager.endForPlayer(sp, "Dimension changed - untwined.");
        }
    }

    @SubscribeEvent
    public static void onPlayerDeath(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer sp) {
            com.kittytalents.entity.TwineManager.endForPlayer(sp, null);
        }
        if (!event.getEntity().level().isClientSide()) {
            net.minecraft.world.damagesource.DamageSource src = event.getSource();
            TalentedCat cat = null;
            if (src.getEntity() instanceof TalentedCat c) {
                cat = c;
            } else if (src.getDirectEntity() instanceof TalentedCat c) {
                cat = c;
            }
            if (cat != null) {
                boolean hostile = event.getEntity() instanceof net.minecraft.world.entity.monster.Monster;
                cat.stats().recordKill(hostile);
                cat.syncStats();
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(net.minecraftforge.event.TickEvent.PlayerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END || event.side.isClient()) return;
        if (!(event.player instanceof net.minecraft.server.level.ServerPlayer sp)) return;
        java.util.UUID catId = com.kittytalents.entity.TwineManager.getCatOf(sp.getUUID());
        if (catId == null) return;

        net.minecraft.world.entity.Entity e = ((ServerLevel) sp.level()).getEntity(catId);
        if (!(e instanceof TalentedCat cat) || !cat.isAlive()) {
            com.kittytalents.entity.TwineManager.endForPlayer(sp, "Your cat is out of range - untwined.");
            return;
        }

        // Failsafe 1: Player HP critically low (< 25%)
        if (sp.getHealth() <= sp.getMaxHealth() * 0.25F) {
            sp.level().playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
            com.kittytalents.entity.TwineManager.endForPlayer(sp, "Emergency Untwine! Player health critically low (< 25%)!");
            return;
        }

        // Failsafe 2: Cat incapacitated
        if (cat.isIncapacitated()) {
            com.kittytalents.entity.TwineManager.endForPlayer(sp, "Your cat was incapacitated - untwined!");
            return;
        }

        // Dormant body: it stays still and NEVER turns. The Twine camera reads the local player's rotation as mouse
        // input, so any server-side rotation of the player (lookAt, or a teleport with absolute angles) reaches the
        // client as a fake mouse delta and turns the camera on its own. (The old "bodyguard AI" called lookAt every
        // tick, which is exactly what made the camera drift.) The body is invulnerable while twined, so it needs no AI.
        sp.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);

        // Keep the body within chunk-loading range of the cat. Relative X/Y rotation = "leave the client's angles alone".
        if (sp.distanceToSqr(cat) > 1024.0D) { // > 32 blocks
            net.minecraft.world.phys.Vec3 look = cat.getLookAngle();
            sp.connection.teleport(cat.getX() - look.x * 2.0D, cat.getY(), cat.getZ() - look.z * 2.0D, 0.0F, 0.0F,
                    java.util.EnumSet.of(net.minecraft.world.entity.RelativeMovement.X_ROT, net.minecraft.world.entity.RelativeMovement.Y_ROT));
        }
    }

    @SubscribeEvent
    public static void onLivingDamage(net.minecraftforge.event.entity.living.LivingDamageEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer sp && com.kittytalents.entity.TwineManager.isPlayerTwined(sp.getUUID())) {
            float remainingHealth = sp.getHealth() - event.getAmount();
            if (remainingHealth <= sp.getMaxHealth() * 0.25F) {
                sp.level().playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
                com.kittytalents.entity.TwineManager.endForPlayer(sp, "Emergency Untwine! Player health critically low (< 25%)!");
            }
        }
        if (!event.getEntity().level().isClientSide()) {
            if (event.getSource().getEntity() instanceof TalentedCat cat) {
                cat.stats().addDamageDealt(event.getAmount());
                cat.syncStats();
            }
            // Damage taken is counted in TalentedCat#hurt, after armor and talent reductions.
        }
    }

    @SubscribeEvent
    public static void onLivingDrops(net.minecraftforge.event.entity.living.LivingDropsEvent event) {
        if (event.getSource().getEntity() instanceof TalentedCat cat) {
            if (cat.getMutation() == com.kittytalents.entity.CatBreedingManager.MutationType.NEKOMATA) {
                java.util.List<ItemEntity> extraDrops = new java.util.ArrayList<>();
                for (ItemEntity drop : event.getDrops()) {
                    ItemStack copy = drop.getItem().copy();
                    ItemEntity dup = new ItemEntity(cat.level(), drop.getX(), drop.getY(), drop.getZ(), copy);
                    dup.setDeltaMovement(drop.getDeltaMovement());
                    extraDrops.add(dup);
                }
                event.getDrops().addAll(extraDrops);
            }
        }
    }

    @SubscribeEvent
    public static void onStartTracking(net.minecraftforge.event.entity.player.PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof TalentedCat cat && event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            cat.syncTalents(player);
            cat.syncEquipment(player);
            cat.syncStats(player);
            com.kittytalents.network.PacketHandler.sendToPlayer(
                    new com.kittytalents.network.PacketSyncTrustedPlayers(cat.getId(), cat.getTrustedPlayers()),
                    player
            );
            if (cat.isTwinedWith(player)) {
                com.kittytalents.network.PacketHandler.sendToPlayer(
                        new com.kittytalents.network.PacketSyncTwine(cat.getId(), true),
                        player
                );
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)) return;
        net.minecraft.server.MinecraftServer server = player.getServer();
        if (server == null) return;
        ServerLevel fromLevel = server.getLevel(event.getFrom());
        ServerLevel toLevel = server.getLevel(event.getTo());
        if (fromLevel == null || toLevel == null) return;
        bringFollowers(player, fromLevel, toLevel);
    }

    /**
     * Brings the player's following cats along through a dimension change, placing them where the player arrived.
     * Uses an explicit teleporter: the default one runs the portal search, which crashes (NullPointerException on the
     * portal position) for an entity that isn't standing in the portal. Returns the cats as they now exist in the new level.
     */
    public static List<net.minecraft.world.entity.Entity> bringFollowers(net.minecraft.server.level.ServerPlayer player, ServerLevel fromLevel, ServerLevel toLevel) {
        // Collect first: changing dimension removes the cat from the level being iterated.
        List<TalentedCat> followers = new java.util.ArrayList<>();
        for (net.minecraft.world.entity.Entity entity : fromLevel.getAllEntities()) {
            if (entity instanceof TalentedCat cat && cat.isAlive() && !cat.isIncapacitated() && !cat.isOrderedToSit()
                    && !cat.isTwined() && !cat.isPassenger() && cat.isPrimaryOwner(player)) {
                com.kittytalents.entity.CatMode mode = cat.getCatMode();
                if (mode == com.kittytalents.entity.CatMode.FOLLOW || mode == com.kittytalents.entity.CatMode.GUARD
                        || mode == com.kittytalents.entity.CatMode.TACTICAL || mode == com.kittytalents.entity.CatMode.AGGRESSIVE) {
                    followers.add(cat);
                }
            }
        }
        net.minecraft.world.phys.Vec3 arrival = player.position();
        net.minecraftforge.common.util.ITeleporter toPlayer = new net.minecraftforge.common.util.ITeleporter() {
            @Override
            public net.minecraft.world.level.portal.PortalInfo getPortalInfo(net.minecraft.world.entity.Entity entity, ServerLevel dest,
                    java.util.function.Function<ServerLevel, net.minecraft.world.level.portal.PortalInfo> defaultPortalInfo) {
                return new net.minecraft.world.level.portal.PortalInfo(arrival, net.minecraft.world.phys.Vec3.ZERO, entity.getYRot(), entity.getXRot());
            }

            @Override
            public boolean playTeleportSound(net.minecraft.server.level.ServerPlayer p, ServerLevel src, ServerLevel dest) {
                return false;
            }
        };
        List<net.minecraft.world.entity.Entity> moved = new java.util.ArrayList<>();
        for (TalentedCat cat : followers) {
            net.minecraft.world.entity.Entity copy = cat.changeDimension(toLevel, toPlayer);
            if (copy != null) moved.add(copy);
        }
        return moved;
    }
}
