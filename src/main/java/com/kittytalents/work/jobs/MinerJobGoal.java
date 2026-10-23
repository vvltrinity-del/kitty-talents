package com.kittytalents.work.jobs;

import com.kittytalents.config.KittyTalentsConfig;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.talent.Talents;
import com.kittytalents.work.AbstractCatJobGoal;
import com.kittytalents.work.AreaCursor;
import com.kittytalents.work.CatJob;
import com.kittytalents.work.JobData;
import com.kittytalents.work.WorkInventoryHelper;
import com.kittytalents.work.WorkerFakePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

public class MinerJobGoal extends AbstractCatJobGoal {
    public MinerJobGoal(TalentedCat cat) {
        super(cat, CatJob.MINER);
    }

    @Override
    protected boolean TopDownScan() {
        return true; // Quarry top-down layer by layer
    }

    @Override
    protected int getBaseTaskDurationTicks() {
        return 22;
    }

    @Override
    protected double getInteractReachSqr() {
        return 9.0D;
    }

    @Override
    protected Predicate<ItemStack> getKeepInPackPredicate() {
        return s -> s.is(Items.TORCH);
    }

    @Override
    @Nullable
    protected BlockPos findNextTask(int scanBudget) {
        Level level = cat.level();
        JobData data = cat.getJobData();
        int moleRank = Math.max(1, cat.getTalentLevel(Talents.MOLE_CAT));

        if (data.isMinerTunnelMode()) {
            BlockPos origin = data.getStationPos() != null ? data.getStationPos() : cat.blockPosition();
            Direction dir = data.getTunnelDirection();
            int maxDist = KittyTalentsConfig.COMMON.maxStationRadius.get();
            for (int d = 1; d <= maxDist; d++) {
                BlockPos feet = origin.relative(dir, d);
                BlockPos head = feet.above();
                if (!isBlacklisted(head) && isMineable(level, head, moleRank)) return head;
                if (!isBlacklisted(feet) && isMineable(level, feet, moleRank)) return feet;
            }
            return null;
        }

        AreaCursor cursor = getCursor();
        for (int i = 0; i < scanBudget; i++) {
            BlockPos pos = cursor.next();
            if (isBlacklisted(pos) || !level.isLoaded(pos)) continue;
            if (data.getStationPos() != null && pos.equals(data.getStationPos())) continue;
            if (isMineable(level, pos, moleRank)) {
                return pos;
            }
        }
        return null;
    }

    public static boolean isMineable(Level level, BlockPos pos, int moleRank) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || !state.getFluidState().isEmpty()) return false;
        if (level.getBlockEntity(pos) != null) return false;
        if (state.getDestroySpeed(level, pos) < 0.0F) return false;

        // Avoid breaking adjacent to fluids to prevent flooding the quarry/tunnel
        for (Direction d : Direction.values()) {
            if (!level.getFluidState(pos.relative(d)).isEmpty()) return false;
        }

        Block b = state.getBlock();
        if (state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.BASE_STONE_NETHER) ||
            b == Blocks.COBBLESTONE || b == Blocks.COBBLED_DEEPSLATE || b == Blocks.GRAVEL) {
            return true;
        }
        return isAllowedOre(state, moleRank);
    }

    public static boolean isAllowedOre(BlockState state, int rank) {
        if (rank >= 1 && (state.is(BlockTags.COAL_ORES) || state.is(BlockTags.COPPER_ORES))) return true;
        if (rank >= 2 && (state.is(BlockTags.IRON_ORES) || state.is(BlockTags.LAPIS_ORES))) return true;
        if (rank >= 3 && (state.is(BlockTags.GOLD_ORES) || state.is(BlockTags.REDSTONE_ORES) || state.is(Blocks.NETHER_QUARTZ_ORE))) return true;
        if (rank >= 4 && (state.is(BlockTags.DIAMOND_ORES) || state.is(BlockTags.EMERALD_ORES))) return true;
        if (rank >= 5 && (state.is(Blocks.ANCIENT_DEBRIS) || state.is(Blocks.OBSIDIAN))) return true;
        return false;
    }

    @Override
    protected boolean isTaskStillValid(BlockPos pos) {
        if (!cat.level().isLoaded(pos)) return false;
        return isMineable(cat.level(), pos, Math.max(1, cat.getTalentLevel(Talents.MOLE_CAT)));
    }

    @Override
    protected boolean executeTask(BlockPos pos) {
        if (!(cat.level() instanceof ServerLevel sl)) return false;
        ItemStack pick = new ItemStack(Items.NETHERITE_PICKAXE);
        List<ItemStack> drops = WorkerFakePlayer.breakBlockAndCollectDrops(sl, pos, cat.getOwnerUUID(), pick);
        if (drops.isEmpty()) return false;

        for (ItemStack d : drops) {
            if (cat.getJobData().getFilter().matches(d)) {
                WorkInventoryHelper.storeProducedItem(cat, d);
            }
        }
        cat.incrementBlocksMined();
        cat.playSound(SoundEvents.STONE_BREAK, 0.85F, 1.0F);

        // Place torch if TORCH_KITTY > 0 and dark
        if (cat.getTalentLevel(Talents.TORCH_KITTY) > 0) {
            BlockPos feetPos = cat.blockPosition();
            if (sl.getBrightness(LightLayer.BLOCK, feetPos) < 8 &&
                sl.getBlockState(feetPos).isAir() &&
                Blocks.TORCH.defaultBlockState().canSurvive(sl, feetPos)) {
                ItemStack torch = WorkInventoryHelper.extractMatchingSupply(cat, s -> s.is(Items.TORCH), 1);
                if (!torch.isEmpty()) {
                    WorkerFakePlayer.placeBlock(sl, feetPos, Blocks.TORCH.defaultBlockState(), cat.getOwnerUUID());
                }
            }
        }
        return true;
    }
}
