package com.kittytalents.talent.effect;

import com.kittytalents.registry.ModItems;
import com.kittytalents.talent.Talents;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Containers;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/** Utility / exploration talents. */
final class UtilityTalents {
    private UtilityTalents() {}

    static void register() {
        TalentEngine.register(Talents.NIGHT_PROWLER, new NightProwler());
        TalentEngine.register(Talents.KITTY_PACK, new KittyPack());
        TalentEngine.register(Talents.MOUSER, new Mouser());
        TalentEngine.register(Talents.FISHER_CAT, new FisherCat());
        TalentEngine.register(Talents.PILLOW_CAT, new PillowCat());
        TalentEngine.register(Talents.WHISKER_RADAR, new WhiskerRadar());
        TalentEngine.register(Talents.PAW_RETRIEVER, new PawRetriever());
        TalentEngine.register(Talents.SNIFFER_WHISKERS, new SnifferWhiskers());
        TalentEngine.register(Talents.GATE_PURR, new GatePurr());
        TalentEngine.register(Talents.KITTY_TOOLS, new KittyTools());
        TalentEngine.register(Talents.AQUA_KITTY, new AquaKitty());
    }

    /** Repels creepers and phantoms (CatCreeperWardenGoal). Mastery "Cat's Eyes": the owner nearby gets Night Vision. */
    static final class NightProwler implements TalentEffect {
        @Override public boolean worksWhileResting() { return true; }
        @Override public int tickInterval() { return 100; }
        @Override public void onTick(TalentContext ctx) {
            Player o = ctx.mastered() ? ctx.ownerWithin(16.0D) : null;
            if (o != null) TalentHelpers.aura(o, MobEffects.NIGHT_VISION, 400, 0);
        }
    }

    /** More pack slots (TalentedCat#getPackSlotCount). Mastery "Bottomless Pack": items the cat walks over go into the pack. */
    static final class KittyPack implements TalentEffect {
        @Override public int tickInterval() { return 10; }
        @Override public void onTick(TalentContext ctx) {
            if (ctx.mastered()) TalentHelpers.collectItems(ctx.cat, 1.5D);
        }
    }

    /** Hunts pests (CatTargeting). Mastery "Exterminator": pests die in one hit. */
    static final class Mouser implements TalentEffect {
        @Override public float modifyOutgoingDamage(TalentContext ctx, LivingEntity t, float dmg) {
            return ctx.mastered() && com.kittytalents.ai.CatTargeting.isPest(t) ? Math.max(dmg, t.getHealth() + 1.0F) : dmg;
        }
    }

    /** Fishing yields (Fisher job, bath, habitat). Mastery "Catch of the Day": a cooked fish appears in the pack every 5 minutes. */
    static final class FisherCat implements TalentEffect {
        @Override public boolean worksWhileResting() { return true; }
        @Override public int tickInterval() { return 6000; }
        @Override public void onTick(TalentContext ctx) {
            if (ctx.mastered()) ctx.cat.getCatPack().addItem(new ItemStack(Items.COOKED_COD));
        }
    }

    /** Comfort/rest bonuses (CatComfortGoal). Mastery "Dream Weaver": a sleeping owner nearby regenerates. */
    static final class PillowCat implements TalentEffect {
        @Override public boolean worksWhileResting() { return true; }
        @Override public int tickInterval() { return 40; }
        @Override public void onTick(TalentContext ctx) {
            Player o = ctx.mastered() ? ctx.ownerWithin(8.0D) : null;
            if (o != null && o.isSleeping()) TalentHelpers.aura(o, MobEffects.REGENERATION, 100, 1);
        }
    }

    /** Warns of nearby monsters. Mastery "Sixth Sense": hostiles in range glow so the owner can see them through walls. */
    static final class WhiskerRadar implements TalentEffect {
        @Override public boolean worksWhileResting() { return true; }
        @Override public int tickInterval() { return 160; }
        @Override public void onTick(TalentContext ctx) {
            double r = 8.0D + ctx.rank * 4.0D;
            var foes = ctx.cat.level().getEntitiesOfClass(LivingEntity.class, ctx.cat.getBoundingBox().inflate(r),
                    e -> e instanceof Enemy && e.isAlive());
            if (foes.isEmpty()) return;
            ctx.cat.playSound(SoundEvents.CAT_BEG_FOR_FOOD, 0.8F, 1.2F);
            if (ctx.mastered()) foes.forEach(e -> TalentHelpers.aura(e, MobEffects.GLOWING, 200, 0));
        }
    }

    /**
     * Pulls loose items towards the cat. Mastery "Magnet Paws": items that reach the cat go straight into its pack.
     * (Fixed: the retrieved-items statistic used to count every pull tick of every item, not actual retrievals.)
     */
    static final class PawRetriever implements TalentEffect {
        @Override public int tickInterval() { return 10; }
        @Override public void onTick(TalentContext ctx) {
            for (ItemEntity item : ctx.cat.level().getEntitiesOfClass(ItemEntity.class, ctx.cat.getBoundingBox().inflate(4.0D + ctx.rank * 2.0D),
                    i -> i.isAlive() && !i.hasPickUpDelay())) {
                Vec3 diff = ctx.cat.position().subtract(item.position());
                if (diff.lengthSqr() > 1.0E-4D) {
                    Vec3 v = diff.normalize().scale(0.35D);
                    item.setDeltaMovement(v.x, 0.1D, v.z);
                }
            }
            if (ctx.mastered()) {
                int got = TalentHelpers.collectItems(ctx.cat, 1.5D);
                for (int i = 0; i < got; i++) ctx.cat.incrementItemsRetrieved();
            }
        }
    }

    /** Digs up small finds from dirt every minute. Mastery "Treasure Nose": finds can be gold, emeralds or diamonds. */
    static final class SnifferWhiskers implements TalentEffect {
        @Override public int tickInterval() { return 1200; }
        @Override public void onTick(TalentContext ctx) {
            if (!ctx.cat.onGround() || !ctx.cat.level().getBlockState(ctx.cat.blockPosition().below()).is(BlockTags.DIRT)) return;
            ctx.cat.playSound(SoundEvents.SNIFFER_DIGGING, 0.8F, 1.2F);
            ItemStack[] common = {new ItemStack(Items.WHEAT_SEEDS, 2), new ItemStack(Items.PUMPKIN_SEEDS), new ItemStack(Items.MELON_SEEDS),
                    new ItemStack(Items.BEETROOT_SEEDS), new ItemStack(Items.BONE), new ItemStack(Items.IRON_NUGGET, 2), new ItemStack(ModItems.CATNIP.get())};
            ItemStack[] rare = {new ItemStack(Items.GOLD_INGOT), new ItemStack(Items.EMERALD), new ItemStack(Items.DIAMOND)};
            ItemStack reward = ctx.mastered() && ctx.cat.getRandom().nextFloat() < 0.25F
                    ? rare[ctx.cat.getRandom().nextInt(rare.length)]
                    : common[ctx.cat.getRandom().nextInt(common.length)];
            Containers.dropItemStack(ctx.cat.level(), ctx.cat.getX(), ctx.cat.getY(), ctx.cat.getZ(), reward.copy());
        }
    }

    /**
     * Opens fence gates for the owner and closes them again once the owner has passed. Mastery "Doorman": doors too.
     * (Fixed: it used to open every nearby gate permanently, whether or not the owner was around.)
     */
    static final class GatePurr implements TalentEffect {
        private final Map<Object, Set<BlockPos>> opened = new WeakHashMap<>();

        @Override public int tickInterval() { return 10; }
        @Override public void onTick(TalentContext ctx) {
            Set<BlockPos> mine = opened.computeIfAbsent(ctx.cat, k -> new HashSet<>());
            Player o = ctx.ownerWithin(4.0D);
            var level = ctx.cat.level();
            if (o != null) {
                BlockPos c = ctx.cat.blockPosition();
                for (BlockPos p : BlockPos.betweenClosed(c.offset(-2, -1, -2), c.offset(2, 1, 2))) {
                    BlockState s = level.getBlockState(p);
                    boolean opener = s.getBlock() instanceof FenceGateBlock || (ctx.mastered() && s.getBlock() instanceof DoorBlock
                            && s.getValue(DoorBlock.HALF) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER);
                    if (opener && s.hasProperty(BlockStateProperties.OPEN) && !s.getValue(BlockStateProperties.OPEN)
                            && o.distanceToSqr(p.getX() + 0.5, p.getY(), p.getZ() + 0.5) < 9.0D) {
                        level.setBlock(p, s.setValue(BlockStateProperties.OPEN, true), 10);
                        mine.add(p.immutable());
                        ctx.cat.playSound(SoundEvents.FENCE_GATE_OPEN, 0.8F, 1.2F);
                    }
                }
            }
            // Close what we opened once nobody is standing in it and the owner moved on.
            mine.removeIf(p -> {
                if (o != null && o.distanceToSqr(p.getX() + 0.5, p.getY(), p.getZ() + 0.5) < 9.0D) return false;
                BlockState s = level.getBlockState(p);
                if (s.hasProperty(BlockStateProperties.OPEN) && s.getValue(BlockStateProperties.OPEN)) {
                    level.setBlock(p, s.setValue(BlockStateProperties.OPEN, false), 10);
                }
                return true;
            });
        }
    }

    /** Tool use (UtilityInteractAbility). Mastery "Master Craftscat": the equipped tool repairs itself. */
    static final class KittyTools implements TalentEffect {
        @Override public boolean worksWhileResting() { return true; }
        @Override public int tickInterval() { return 40; }
        @Override public void onTick(TalentContext ctx) {
            if (ctx.mastered()) TalentHelpers.repair(ctx.cat.getEquippedTool(), 1);
        }
    }

    /**
     * Water breathing and faster swimming. Mastery "Tidecaller": the owner nearby swims with Dolphin's Grace.
     * (Fixed: the swim boost used to be applied twice per tick by two separate code paths, compounding.)
     */
    static final class AquaKitty implements TalentEffect {
        @Override public boolean worksWhileResting() { return true; }
        @Override public int tickInterval() { return 1; }
        @Override public void onTick(TalentContext ctx) {
            if (!ctx.cat.isInWater()) return;
            ctx.cat.setAirSupply(ctx.cat.getMaxAirSupply());
            Vec3 mov = ctx.cat.getDeltaMovement();
            if (mov.horizontalDistanceSqr() > 0.001D) {
                // Boost, but clamp to a top swim speed: a per-tick multiplier alone outpaces water drag and accelerates forever.
                double cap = 0.22D + ctx.rank * 0.03D;
                Vec3 boosted = new Vec3(mov.x * 1.15D, 0, mov.z * 1.15D);
                double h = boosted.horizontalDistance();
                if (h > cap) boosted = boosted.scale(cap / h);
                ctx.cat.setDeltaMovement(boosted.x, mov.y, boosted.z);
            }
            if (ctx.mastered() && ctx.cat.tickCount % 20 == 0) {
                Player o = ctx.ownerWithin(16.0D);
                if (o != null && o.isInWater()) {
                    TalentHelpers.aura(o, MobEffects.DOLPHINS_GRACE, 60, 0);
                    TalentHelpers.aura(o, MobEffects.WATER_BREATHING, 60, 0);
                }
            }
        }
    }
}
