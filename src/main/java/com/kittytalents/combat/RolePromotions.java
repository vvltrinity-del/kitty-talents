package com.kittytalents.combat;

import com.kittytalents.KittyTalents;
import com.kittytalents.ai.CatTargeting;
import com.kittytalents.entity.TalentedCat;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Role mastery and promotions (5.0). A cat masters its role by reaching the maximum level; a mastered role can then
 * be promoted one tier. The promotion keeps the role's stat spread and adds one signature perk:
 * <pre>
 * DPS -> Hyper Carry        each kill +5% damage, up to +50%; lost when downed
 * Tank -> Crusader          taunts mobs that target the owner (8 blocks); reflects 20% of damage taken
 * Healer -> Guardian of Light  once every 5 min, stops the owner (32 blocks) dying: back to 40% health, healing burst
 * Mage -> Warlock           abilities heal the cat for 25% of their damage, but cost health instead of Energy
 * Scout -> Pathfinder       hostiles within 24 blocks glow; the owner nearby gets Speed
 * Bruiser -> Juggernaut     immune to knockback; hits knock foes down (Slowness III, 1 s)
 * Assassin -> Phantom Blade after a kill, invisible for 3 s and the next hit deals double damage
 * Support -> Commander      the owner's other cats within 16 blocks deal +10% damage and regain Energy
 * Controller -> Archon      hits slow every foe within 3 blocks of the target
 * Breaker -> Siegebreaker   hits leave foes Vulnerable and knock shields down
 * Mobility -> Windrunner    dodges cost half Energy; the owner nearby gets Speed
 * Hybrid -> Paragon         +5% damage, reflects 5%, slowly regains Energy
 * </pre>
 */
@Mod.EventBusSubscriber(modid = KittyTalents.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RolePromotions {
    private RolePromotions() {}

    /** Transient per-cat combat state. */
    private static final class State {
        int carryStacks;
        boolean nextCrit;
        long guardianReadyAt;
    }

    private static final Map<TalentedCat, State> STATE = new WeakHashMap<>();

    private static State state(TalentedCat cat) {
        return STATE.computeIfAbsent(cat, c -> new State());
    }

    /** The promotion name key for a role ("promotion.kittytalents.dps"), shown in the Combat tab. */
    public static String key(CatRole role) {
        return "promotion.kittytalents." + role.name().toLowerCase();
    }

    private static boolean is(TalentedCat cat, CatRole role) {
        return cat.isPromoted() && cat.getCatRole() == role;
    }

    // ---------------------------------------------------------------- hooks called from other systems

    /** Warlock: pay an ability's Energy cost in health (1 health per 5 Energy). @return true if paid this way. */
    public static boolean payWithHealth(TalentedCat cat, int energy) {
        if (!is(cat, CatRole.MAGE)) return false;
        float cost = Math.max(1.0F, energy / 5.0F);
        if (cat.getHealth() <= cost + 1.0F) return false;
        cat.setHealth(cat.getHealth() - cost);
        return true;
    }

    /** Windrunner: dodges cost half Energy. */
    public static float dodgeCostMultiplier(TalentedCat cat) {
        return is(cat, CatRole.MOBILITY) ? 0.5F : 1.0F;
    }

    /** Server, every 20 ticks per cat. */
    public static void tick(TalentedCat cat) {
        if (!cat.isPromoted() || !(cat.level() instanceof ServerLevel sl)) return;
        State st = state(cat);
        if (cat.isIncapacitated()) st.carryStacks = 0;
        Player owner = cat.getOwner() instanceof Player p && p.level() == cat.level() ? p : null;
        switch (cat.getCatRole()) {
            case TANK -> {
                if (owner == null || cat.isIncapacitated()) break;
                for (Mob m : sl.getEntitiesOfClass(Mob.class, cat.getBoundingBox().inflate(8.0D), m -> m instanceof Enemy && m.getTarget() == owner)) {
                    m.setTarget(cat);
                }
            }
            case SCOUT -> {
                for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, cat.getBoundingBox().inflate(24.0D), e -> e instanceof Enemy && e.isAlive())) {
                    e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, true, false));
                }
                if (owner != null && owner.distanceToSqr(cat) < 16 * 16) owner.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 0, true, false, true));
            }
            case SUPPORT -> {
                if (cat.getOwnerUUID() == null) break;
                for (TalentedCat c : sl.getEntitiesOfClass(TalentedCat.class, cat.getBoundingBox().inflate(16.0D),
                        c -> c != cat && cat.getOwnerUUID().equals(c.getOwnerUUID()))) {
                    FelineEnergyEngine.restoreEnergy(c, 2);
                }
            }
            case MOBILITY -> {
                if (owner != null && owner.distanceToSqr(cat) < 6 * 6) owner.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 0, true, false, true));
            }
            case HYBRID -> FelineEnergyEngine.restoreEnergy(cat, 1);
            default -> {}
        }
    }

    // ---------------------------------------------------------------- events

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) return;
        DamageSource src = event.getSource();
        float amount = event.getAmount();

        if (src.getEntity() instanceof TalentedCat cat && cat != victim) {
            amount *= commanderBonus(cat);
            if (cat.isPromoted()) {
                State st = state(cat);
                switch (cat.getCatRole()) {
                    case DPS -> amount *= 1.0F + 0.05F * st.carryStacks;
                    case ASSASSIN -> {
                        if (st.nextCrit) {
                            amount *= 2.0F;
                            st.nextCrit = false;
                            cat.removeEffect(MobEffects.INVISIBILITY);
                        }
                    }
                    case HYBRID -> amount *= 1.05F;
                    case MAGE -> {
                        if (src.getDirectEntity() != cat) cat.heal(amount * 0.25F); // ability damage
                    }
                    case BREAKER -> {
                        FelineStatusEffects.applyStatus(victim, FelineStatusType.VULNERABLE, 2, 80);
                        if (victim instanceof Player p && p.isBlocking()) p.disableShield(true);
                    }
                    case CONTROLLER -> {
                        for (LivingEntity e : victim.level().getEntitiesOfClass(LivingEntity.class, victim.getBoundingBox().inflate(3.0D),
                                e -> e != victim && e.isAlive() && CatTargeting.canHarm(cat, e))) {
                            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
                        }
                    }
                    case BRUISER -> victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 2));
                    default -> {}
                }
            }
        }

        if (victim instanceof TalentedCat cat && cat.isPromoted() && src.getEntity() instanceof LivingEntity attacker
                && attacker != cat && CatTargeting.canHarm(cat, attacker)) {
            float reflect = cat.getCatRole() == CatRole.TANK ? 0.20F : cat.getCatRole() == CatRole.HYBRID ? 0.05F : 0;
            if (reflect > 0 && amount > 0.5F) attacker.hurt(cat.damageSources().thorns(cat), amount * reflect);
        }
        event.setAmount(amount);
    }

    /** +10% when one of the owner's Commanders (other than this cat) is within 16 blocks. */
    private static float commanderBonus(TalentedCat cat) {
        if (cat.getOwnerUUID() == null) return 1.0F;
        boolean near = !cat.level().getEntitiesOfClass(TalentedCat.class, cat.getBoundingBox().inflate(16.0D),
                c -> c != cat && c.isPromoted() && c.getCatRole() == CatRole.SUPPORT && cat.getOwnerUUID().equals(c.getOwnerUUID())).isEmpty();
        return near ? 1.10F : 1.0F;
    }

    @SubscribeEvent
    public static void onKnockback(LivingKnockBackEvent event) {
        if (event.getEntity() instanceof TalentedCat cat && cat.isPromoted() && cat.getCatRole() == CatRole.BRUISER) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead.level().isClientSide) return;
        if (event.getSource().getEntity() instanceof TalentedCat cat && cat.isPromoted()) {
            State st = state(cat);
            if (cat.getCatRole() == CatRole.DPS) st.carryStacks = Math.min(10, st.carryStacks + 1);
            if (cat.getCatRole() == CatRole.ASSASSIN) {
                st.nextCrit = true;
                cat.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 60, 0, false, false));
            }
        }
        if (dead instanceof Player owner && dead.level() instanceof ServerLevel sl) {
            for (TalentedCat cat : sl.getEntitiesOfClass(TalentedCat.class, owner.getBoundingBox().inflate(32.0D),
                    c -> c.isPromoted() && c.getCatRole() == CatRole.HEALER && !c.isIncapacitated() && c.isOwnedBy(owner))) {
                State st = state(cat);
                if (sl.getGameTime() < st.guardianReadyAt) continue;
                st.guardianReadyAt = sl.getGameTime() + 6000;
                event.setCanceled(true);
                owner.setHealth(owner.getMaxHealth() * 0.4F);
                owner.removeAllEffects();
                owner.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
                for (LivingEntity ally : sl.getEntitiesOfClass(LivingEntity.class, owner.getBoundingBox().inflate(8.0D),
                        e -> e instanceof Player || e instanceof TalentedCat)) {
                    ally.heal(6.0F);
                }
                sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, owner.getX(), owner.getY() + 1, owner.getZ(), 40, 0.6, 0.8, 0.6, 0.3);
                sl.playSound(null, owner.blockPosition(), SoundEvents.TOTEM_USE, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.2F);
                return;
            }
        }
    }

    /** For tests and the HUD. */
    public static int carryStacks(TalentedCat cat) { return state(cat).carryStacks; }

}
