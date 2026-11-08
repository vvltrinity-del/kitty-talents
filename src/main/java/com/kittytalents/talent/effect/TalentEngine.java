package com.kittytalents.talent.effect;

import com.kittytalents.KittyTalents;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.talent.Talent;
import com.kittytalents.talent.Talents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry of talent effects and the single dispatcher for their hooks (Phase 4).
 * TalentedCat calls in from tick / doHurtTarget / hurt / updateAttributes; owner and kill hooks come from events here.
 */
@Mod.EventBusSubscriber(modid = KittyTalents.MODID)
public final class TalentEngine {
    private static final Map<Talent, TalentEffect> EFFECTS = new LinkedHashMap<>();

    private TalentEngine() {}

    public static void register(Talent talent, TalentEffect effect) {
        EFFECTS.put(talent, effect);
    }

    static {
        TalentEffects.registerAll();
    }

    public static TalentEffect get(Talent talent) {
        return EFFECTS.get(talent);
    }

    /** Mastery = max rank. Standard talents (free baseline rank) must actually be trained up to max. */
    public static boolean isMastered(TalentedCat cat, Talent talent) {
        if (cat == null || talent == null) return false;
        if (talent.isStandard()) {
            return cat.talents().getOrDefault(talent.getId(), 0) >= talent.getMaxLevel();
        }
        return cat.getTalentLevel(talent) >= talent.getMaxLevel();
    }

    // ---------------------------------------------------------------------------------------------------------
    // Dispatch from TalentedCat
    // ---------------------------------------------------------------------------------------------------------

    public static void tick(TalentedCat cat) {
        long time = cat.level().getGameTime();
        for (Map.Entry<Talent, TalentEffect> e : EFFECTS.entrySet()) {
            int interval = e.getValue().tickInterval();
            if (interval <= 0) continue;
            // Stagger by entity id so 100 cats don't all scan on the same tick.
            if ((time + cat.getId()) % interval != 0) continue;
            int rank = cat.getTalentLevel(e.getKey());
            if (rank > 0) e.getValue().onTick(new TalentContext(cat, e.getKey(), rank));
        }
    }

    public static float modifyOutgoing(TalentedCat cat, LivingEntity target, float damage) {
        for (Map.Entry<Talent, TalentEffect> e : EFFECTS.entrySet()) {
            int rank = cat.getTalentLevel(e.getKey());
            if (rank > 0) damage = e.getValue().modifyOutgoingDamage(new TalentContext(cat, e.getKey(), rank), target, damage);
        }
        return damage;
    }

    public static void onHitDealt(TalentedCat cat, LivingEntity target, float damage) {
        for (Map.Entry<Talent, TalentEffect> e : EFFECTS.entrySet()) {
            int rank = cat.getTalentLevel(e.getKey());
            if (rank > 0) e.getValue().onHitDealt(new TalentContext(cat, e.getKey(), rank), target, damage);
        }
    }

    public static float modifyIncoming(TalentedCat cat, DamageSource source, float amount) {
        for (Map.Entry<Talent, TalentEffect> e : EFFECTS.entrySet()) {
            int rank = cat.getTalentLevel(e.getKey());
            if (rank <= 0) continue;
            amount = e.getValue().modifyIncomingDamage(new TalentContext(cat, e.getKey(), rank), source, amount);
            if (amount <= 0.0F) return 0.0F;
        }
        return amount;
    }

    public static void applyAttributes(TalentedCat cat, AttributeTotals totals) {
        for (Map.Entry<Talent, TalentEffect> e : EFFECTS.entrySet()) {
            int rank = cat.getTalentLevel(e.getKey());
            if (rank > 0) e.getValue().applyAttributes(new TalentContext(cat, e.getKey(), rank), totals);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Events: kills (any source) and owner protection
    // ---------------------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getSource().getEntity() instanceof TalentedCat cat) || cat == event.getEntity()) return;
        LivingEntity victim = event.getEntity();
        com.kittytalents.core.progression.CatProgression.onKill(cat, victim);
        for (Map.Entry<Talent, TalentEffect> e : EFFECTS.entrySet()) {
            int rank = cat.getTalentLevel(e.getKey());
            if (rank > 0) e.getValue().onKill(new TalentContext(cat, e.getKey(), rank), victim);
        }
    }

    /** Hunter Cat: each rank gives 10% to double an animal's drops; mastered, always (and for hostiles too). */
    @SubscribeEvent
    public static void onDrops(net.minecraftforge.event.entity.living.LivingDropsEvent event) {
        if (!(event.getSource().getEntity() instanceof TalentedCat cat) || cat.level().isClientSide) return;
        int rank = cat.getTalentLevel(Talents.HUNTER_CAT);
        if (rank <= 0) return;
        boolean mastered = isMastered(cat, Talents.HUNTER_CAT);
        boolean animal = event.getEntity() instanceof net.minecraft.world.entity.animal.Animal;
        if (!animal && !mastered) return;
        if (!mastered && cat.getRandom().nextFloat() >= rank * 0.10F) return;
        List<net.minecraft.world.entity.item.ItemEntity> extra = new java.util.ArrayList<>();
        for (var drop : event.getDrops()) {
            extra.add(new net.minecraft.world.entity.item.ItemEntity(drop.level(), drop.getX(), drop.getY(), drop.getZ(), drop.getItem().copy()));
        }
        event.getDrops().addAll(extra);
    }

    private static List<TalentedCat> catsGuarding(Player owner) {
        return owner.level().getEntitiesOfClass(TalentedCat.class, owner.getBoundingBox().inflate(16.0D),
                c -> c.isAlive() && !c.isIncapacitated() && owner.getUUID().equals(c.getOwnerUUID()));
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onOwnerHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player owner) || owner.level().isClientSide) return;
        float amount = event.getAmount();
        for (TalentedCat cat : catsGuarding(owner)) {
            for (Map.Entry<Talent, TalentEffect> e : EFFECTS.entrySet()) {
                int rank = cat.getTalentLevel(e.getKey());
                if (rank > 0) amount = e.getValue().onOwnerHurt(new TalentContext(cat, e.getKey(), rank), owner, event.getSource(), amount);
            }
        }
        event.setAmount(Math.max(0.0F, amount));
    }

    @SubscribeEvent
    public static void onOwnerFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof Player owner) || owner.level().isClientSide) return;
        float mult = event.getDamageMultiplier();
        for (TalentedCat cat : catsGuarding(owner)) {
            for (Map.Entry<Talent, TalentEffect> e : EFFECTS.entrySet()) {
                int rank = cat.getTalentLevel(e.getKey());
                if (rank > 0) mult *= e.getValue().onOwnerFall(new TalentContext(cat, e.getKey(), rank), owner, event.getDistance());
            }
        }
        event.setDamageMultiplier(mult);
    }

    /** Talents whose gameplay lives elsewhere (jobs, goals, items) still get a registry entry so audits see them. */
    public static boolean hasEffect(Talent talent) {
        return EFFECTS.containsKey(talent) || talent == Talents.POUNCE;
    }
}
