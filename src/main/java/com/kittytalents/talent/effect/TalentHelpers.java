package com.kittytalents.talent.effect;

import com.kittytalents.ai.CatTargeting;
import com.kittytalents.entity.TalentedCat;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.function.Predicate;

/** Small shared helpers for talent effects. */
final class TalentHelpers {
    private TalentHelpers() {}

    /** Ambient (no particles, no icon flicker) effect refresh, used for auras. */
    /** Night vision granted by cats lasts at least this long, so it never sits in the vanilla blink window. */
    public static final int NIGHT_VISION_TICKS = 400;

    static void aura(LivingEntity e, MobEffect effect, int duration, int amplifier) {
        // Night vision blinks in its last 200 ticks: keep a refreshed aura well above that (5.0 flicker fix).
        if (effect == net.minecraft.world.effect.MobEffects.NIGHT_VISION) duration = Math.max(duration, NIGHT_VISION_TICKS);
        e.addEffect(new MobEffectInstance(effect, duration, amplifier, true, false, true));
    }

    /** Repairs {@code amount} durability on a damaged stack ("free Mending"). */
    static void repair(ItemStack stack, int amount) {
        if (!stack.isEmpty() && stack.isDamageableItem() && stack.isDamaged()) {
            stack.setDamageValue(Math.max(0, stack.getDamageValue() - amount));
        }
    }

    /** Living things the cat may hurt within {@code radius} of {@code center}. */
    static List<LivingEntity> foesAround(TalentedCat cat, LivingEntity center, double radius) {
        return cat.level().getEntitiesOfClass(LivingEntity.class, center.getBoundingBox().inflate(radius),
                e -> e != center && CatTargeting.canHarm(cat, e));
    }

    /** Pull loose items within {@code radius} into the cat's pack. Returns the number of stacks fully collected. */
    static int collectItems(TalentedCat cat, double radius) {
        int collected = 0;
        for (ItemEntity item : cat.level().getEntitiesOfClass(ItemEntity.class, cat.getBoundingBox().inflate(radius),
                i -> i.isAlive() && !i.hasPickUpDelay())) {
            ItemStack remaining = cat.getCatPack().addItem(item.getItem().copy());
            if (remaining.isEmpty()) {
                item.discard();
                collected++;
            } else {
                item.setItem(remaining);
            }
        }
        return collected;
    }

    /** Give a few random growth ticks to plants matching {@code which} within {@code radius} blocks. */
    static void growPlants(TalentedCat cat, int radius, int attempts, Predicate<BlockState> which) {
        if (!(cat.level() instanceof ServerLevel level)) return;
        BlockPos base = cat.blockPosition();
        for (int i = 0; i < attempts; i++) {
            BlockPos p = base.offset(cat.getRandom().nextInt(radius * 2 + 1) - radius, cat.getRandom().nextInt(3) - 1,
                    cat.getRandom().nextInt(radius * 2 + 1) - radius);
            BlockState state = level.getBlockState(p);
            if (which.test(state) && state.getBlock() instanceof BonemealableBlock && state.isRandomlyTicking()) {
                state.randomTick(level, p, level.random);
            }
        }
    }

    static boolean isOwnerOrTrusted(TalentedCat cat, Player p) {
        return p.getUUID().equals(cat.getOwnerUUID()) || cat.isTrusted(p.getUUID());
    }
}
