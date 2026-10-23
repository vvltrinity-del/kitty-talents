package com.kittytalents.event;

import com.kittytalents.KittyTalents;
import com.kittytalents.entity.CatBreedingManager;
import com.kittytalents.entity.ProductionTier;
import com.kittytalents.entity.TalentedCat;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the "Neko Night" gameplay world event.
 * During Neko Night:
 * 1. Night transition has a chance to trigger Neko Night (higher on full moon).
 * 2. Hostile monsters are buffed with Strength, Speed, and Resistance.
 * 3. Wild cats have a 25% chance to spawn as Nekomata and a 35% chance to spawn with elevated Production Tiers.
 * 4. Chat alerts and mystical sound cues are broadcast.
 * 5. Full manual controls available via /kittytalents nekonight start/stop/status.
 */
@Mod.EventBusSubscriber(modid = KittyTalents.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class NekoNightManager {

    private static final Set<ResourceKey<Level>> ACTIVE_DIMENSIONS = Collections.synchronizedSet(new HashSet<>());
    private static final Map<ResourceKey<Level>, Long> LAST_CHECKED_DAY = new ConcurrentHashMap<>();

    /**
     * Checks whether Neko Night is currently active in the given level.
     */
    public static boolean isNekoNight(@Nullable Level level) {
        if (level == null) return false;
        return ACTIVE_DIMENSIONS.contains(level.dimension());
    }

    /**
     * Starts Neko Night for the specified ServerLevel.
     */
    public static void startNekoNight(ServerLevel level) {
        ACTIVE_DIMENSIONS.add(level.dimension());

        // Broadcast announcement to all players in the level
        Component alert = Component.literal("§d§l[Neko Night]§r §eA mystical Neko Night has begun... The moon glows with feline spirit!§r");
        for (ServerPlayer player : level.players()) {
            player.sendSystemMessage(alert);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BEACON_ACTIVATE, SoundSource.AMBIENT, 1.0F, 1.2F);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.CAT_PURREOW, SoundSource.PLAYERS, 1.2F, 1.2F);
        }

        KittyTalents.LOGGER.info("Neko Night started in dimension: {}", level.dimension().location());
    }

    /**
     * Stops Neko Night for the specified ServerLevel.
     */
    public static void stopNekoNight(ServerLevel level) {
        if (ACTIVE_DIMENSIONS.remove(level.dimension())) {
            Component alert = Component.literal("§d§l[Neko Night]§r §bThe morning sun rises, and the spirits of Neko Night fade away...§r");
            for (ServerPlayer player : level.players()) {
                player.sendSystemMessage(alert);
            }
            KittyTalents.LOGGER.info("Neko Night stopped in dimension: {}", level.dimension().location());
        }
    }

    /**
     * Applies wild cat Neko Night bonus rolls:
     * - 25% chance to spawn as Nekomata (split twin tails)
     * - 35% chance to spawn with elevated Production Tier
     */
    public static void applyNekoNightBonuses(TalentedCat cat) {
        if (cat == null || !isNekoNight(cat.level())) return;

        // 25% chance for Nekomata mutation
        if (cat.getRandom().nextFloat() < 0.25F) {
            cat.setNekomata(true);
        }

        // 35% chance for elevated Production Tier
        if (cat.getRandom().nextFloat() < 0.35F) {
            ProductionTier[] elevated = new ProductionTier[] {
                    ProductionTier.IRON,
                    ProductionTier.GOLD,
                    ProductionTier.DIAMOND,
                    ProductionTier.EMERALD,
                    ProductionTier.COPPER,
                    ProductionTier.REDSTONE,
                    ProductionTier.LAPIS
            };
            ProductionTier chosen = elevated[cat.getRandom().nextInt(elevated.length)];
            cat.setProductionTier(chosen);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide()) return;
        if (!(event.level instanceof ServerLevel level)) return;

        // Check once per second (every 20 ticks)
        if (level.getGameTime() % 20L != 0L) return;

        long dayTime = level.getDayTime();
        long currentDay = dayTime / 24000L;
        long timeOfDay = dayTime % 24000L;

        // Night starts around 12500 - 13000 ticks
        Long lastDay = LAST_CHECKED_DAY.get(level.dimension());
        if (lastDay == null || lastDay != currentDay) {
            if (timeOfDay >= 12500L && timeOfDay <= 13500L) {
                LAST_CHECKED_DAY.put(level.dimension(), currentDay);

                // Moon phase 0 is full moon: 35% chance on full moon, 15% on normal nights
                int moonPhase = level.getMoonPhase();
                float triggerChance = (moonPhase == 0) ? 0.35F : 0.15F;

                if (level.getRandom().nextFloat() < triggerChance) {
                    startNekoNight(level);
                }
            }
        }

        // When night ends (morning at ~23500 / 0), end Neko Night
        if (isNekoNight(level) && (timeOfDay < 12500L || timeOfDay > 23500L)) {
            stopNekoNight(level);
        }
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk() || event.getLevel().isClientSide()) return;
        Level level = event.getLevel();

        if (event.getEntity() instanceof TalentedCat cat) {
            applyNekoNightBonuses(cat);
        } else if (isNekoNight(level) && event.getEntity() instanceof Monster monster) {
            // Buff hostile monsters during Neko Night
            monster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 12000, 0, false, true));
            monster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 12000, 0, false, true));
            monster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 12000, 0, false, true));
        }
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        Level level = event.getEntity().level();

        if (!isNekoNight(level)) return;

        Entity attacker = event.getSource().getEntity();

        // Hostiles deal 25% more damage during Neko Night
        if (attacker instanceof Enemy) {
            event.setAmount(event.getAmount() * 1.25F);
        }

        // Hostiles receive 15% less damage during Neko Night
        if (event.getEntity() instanceof Enemy) {
            event.setAmount(event.getAmount() * 0.85F);
        }
    }
}
