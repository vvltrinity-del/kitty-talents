package com.kittytalents.core.progression;

import com.kittytalents.config.KittyTalentsConfig;
import com.kittytalents.entity.TalentedCat;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;

/**
 * Cat experience (Phase 4). Before this there was no XP at all: cats levelled only from treats, plus a 20% chance of a
 * whole free level per gun kill (with no cap check). Now every kill credited to the cat and every completed job task
 * grants XP; each level-up grants one skill point.
 */
public final class CatProgression {
    private CatProgression() {}

    /** XP needed to go from {@code level} to the next: 30 at level 1, 1,010 at level 99. */
    public static int xpForNextLevel(int level) {
        return 20 + level * 10;
    }

    /** XP for killing {@code victim}: hostiles by toughness, bosses x5, everything else 1. */
    public static int killXp(LivingEntity victim) {
        if (victim instanceof Player) return 5;
        int xp = victim instanceof Enemy ? Math.max(2, Math.round(victim.getMaxHealth() / 2.0F)) : 1;
        if (victim instanceof WitherBoss || victim instanceof EnderDragon || victim instanceof Warden || victim instanceof ElderGuardian) {
            xp *= 5;
        }
        return xp;
    }

    public static void onKill(TalentedCat cat, LivingEntity victim) {
        addXp(cat, killXp(victim));
    }

    /** XP for one completed job task. */
    public static void onWorkTask(TalentedCat cat) {
        addXp(cat, 1);
    }

    /**
     * Adds XP and resolves any level-ups. At the level cap XP is not accumulated (no hidden banked levels).
     * @return levels gained
     */
    public static int addXp(TalentedCat cat, int amount) {
        if (amount <= 0 || cat.level().isClientSide) return 0;
        // 4.0 Phase 6: Curious cats and well-bonded cats learn faster.
        double mult = (cat.getTrait() == com.kittytalents.core.living.CatTrait.CURIOUS ? com.kittytalents.core.living.CatTrait.CURIOUS_XP.get() : 1.0)
                * (cat.hasBondPerk(com.kittytalents.core.living.CatBond.Perk.QUICK_LEARNER) ? com.kittytalents.core.living.CatBond.XP_MULT.get() : 1.0);
        if (mult != 1.0) amount = (int) Math.max(1, Math.round(amount * mult));
        int cap = KittyTalentsConfig.COMMON.maxCatLevel.get();
        if (cat.getCatLevel() >= cap) {
            cat.setCatXp(0);
            return 0;
        }
        int xp = cat.getCatXp() + amount;
        int gained = 0;
        while (cat.getCatLevel() < cap && xp >= xpForNextLevel(cat.getCatLevel())) {
            xp -= xpForNextLevel(cat.getCatLevel());
            cat.setCatLevel(cat.getCatLevel() + 1);
            cat.setSkillPoints(cat.getSkillPoints() + 1);
            gained++;
        }
        cat.setCatXp(cat.getCatLevel() >= cap ? 0 : xp);
        if (gained > 0) celebrate(cat);
        return gained;
    }

    private static void celebrate(TalentedCat cat) {
        cat.playSound(SoundEvents.PLAYER_LEVELUP, 0.7F, 1.3F);
        if (cat.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, cat.getX(), cat.getY() + 0.6, cat.getZ(), 12, 0.4, 0.4, 0.4, 0.05);
        }
        if (cat.getOwner() instanceof Player owner) {
            owner.displayClientMessage(Component.translatable("message.kittytalents.level_up", cat.getName(), cat.getCatLevel())
                    .withStyle(ChatFormatting.GOLD), true);
        }
        cat.syncTalents();
    }
}
