package com.kittytalents.work.jobs;

import com.kittytalents.entity.TalentedCat;
import com.kittytalents.work.AbstractCatJobGoal;
import com.kittytalents.work.AreaCursor;
import com.kittytalents.work.CatJob;
import com.kittytalents.work.WorkInventoryHelper;
import com.kittytalents.work.WorkerFakePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

public class LumberjackJobGoal extends AbstractCatJobGoal {
    private static final int MAX_TREE_LOGS = 160;

    public LumberjackJobGoal(TalentedCat cat) {
        super(cat, CatJob.LUMBERJACK);
    }

    @Override
    protected int getBaseTaskDurationTicks() {
        return 30;
    }

    @Override
    protected double getInteractReachSqr() {
        return 9.0D; // 3 blocks reach for tree trunks
    }

    @Override
    protected Predicate<ItemStack> getKeepInPackPredicate() {
        return s -> s.is(ItemTags.SAPLINGS);
    }

    @Override
    @Nullable
    protected BlockPos findNextTask(int scanBudget) {
        Level level = cat.level();
        AreaCursor cursor = getCursor();
        for (int i = 0; i < scanBudget; i++) {
            BlockPos pos = cursor.next();
            if (isBlacklisted(pos) || !level.isLoaded(pos)) continue;
            BlockState state = level.getBlockState(pos);
            if (state.is(BlockTags.LOGS) && isNaturalTreeBaseOrTrunk(level, pos)) {
                return pos;
            }
            // Also plant saplings on empty dirt where a tree can grow
            if (isPlantableSoil(state) && level.getBlockState(pos.above()).isAir() && hasSaplingInPack() && isTreeSpacingClear(level, pos.above())) {
                return pos;
            }
        }
        return null;
    }

    private boolean isPlantableSoil(BlockState state) {
        return state.is(BlockTags.DIRT) || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.PODZOL);
    }

    private boolean isTreeSpacingClear(Level level, BlockPos saplingPos) {
        // Only auto-plant on soil if not directly adjacent to another log/sapling
        for (BlockPos p : BlockPos.betweenClosed(saplingPos.offset(-2, 0, -2), saplingPos.offset(2, 0, 2))) {
            if (p.equals(saplingPos)) continue;
            BlockState s = level.getBlockState(p);
            if (s.is(BlockTags.LOGS) || s.is(BlockTags.SAPLINGS)) return false;
        }
        return false; // Only replant where a tree was just felled (handled directly in executeTask)
    }

    private boolean hasSaplingInPack() {
        for (int i = 0; i < cat.getCatPack().getContainerSize(); i++) {
            if (cat.getCatPack().getItem(i).is(ItemTags.SAPLINGS)) return true;
        }
        return false;
    }

    private boolean isNaturalTreeBaseOrTrunk(Level level, BlockPos startPos) {
        // Flood-fill logs up to 48 to check if any log touches natural leaves
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        queue.add(startPos);
        visited.add(startPos);
        int leavesFound = 0;

        while (!queue.isEmpty() && visited.size() < 64) {
            BlockPos cur = queue.poll();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        BlockPos next = cur.offset(dx, dy, dz);
                        BlockState ns = level.getBlockState(next);
                        if (ns.is(BlockTags.LEAVES)) {
                            leavesFound++;
                            if (leavesFound >= 3) return true;
                        } else if (ns.is(BlockTags.LOGS) && visited.add(next)) {
                            queue.add(next);
                        }
                    }
                }
            }
        }
        return leavesFound >= 3;
    }

    @Override
    protected boolean isTaskStillValid(BlockPos pos) {
        if (!cat.level().isLoaded(pos)) return false;
        return cat.level().getBlockState(pos).is(BlockTags.LOGS);
    }

    @Override
    protected boolean executeTask(BlockPos pos) {
        if (!(cat.level() instanceof ServerLevel sl)) return false;
        BlockState startState = sl.getBlockState(pos);
        if (!startState.is(BlockTags.LOGS)) return false;

        // Flood-fill all connected logs up to MAX_TREE_LOGS
        Set<BlockPos> logs = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        queue.add(pos.immutable());
        logs.add(pos.immutable());

        while (!queue.isEmpty() && logs.size() < MAX_TREE_LOGS) {
            BlockPos cur = queue.poll();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        BlockPos next = cur.offset(dx, dy, dz).immutable();
                        if (!logs.contains(next) && sl.getBlockState(next).is(BlockTags.LOGS)) {
                            logs.add(next);
                            queue.add(next);
                        }
                    }
                }
            }
        }

        // Sort logs top-down (highest Y first)
        List<BlockPos> sortedLogs = new ArrayList<>(logs);
        sortedLogs.sort((a, b) -> Integer.compare(b.getY(), a.getY()));

        BlockPos lowestBasePos = null;
        Block felledLogBlock = startState.getBlock();
        ItemStack axe = new ItemStack(Items.IRON_AXE);
        int felledCount = 0;

        for (BlockPos logPos : sortedLogs) {
            BlockState below = sl.getBlockState(logPos.below());
            if (isPlantableSoil(below)) {
                lowestBasePos = logPos;
            }
            List<ItemStack> drops = WorkerFakePlayer.breakBlockAndCollectDrops(sl, logPos, cat.getOwnerUUID(), axe);
            for (ItemStack d : drops) {
                if (cat.getJobData().getFilter().matches(d)) {
                    WorkInventoryHelper.storeProducedItem(cat, d);
                }
            }
            felledCount++;
        }

        // Collect nearby sapling/stick/apple drops around the tree base
        AABB dropBox = new AABB(pos).inflate(5.0D, 8.0D, 5.0D);
        for (ItemEntity ie : sl.getEntitiesOfClass(ItemEntity.class, dropBox)) {
            if (ie.isAlive() && !ie.getItem().isEmpty()) {
                ItemStack item = ie.getItem();
                if (item.is(ItemTags.LOGS) || item.is(ItemTags.SAPLINGS) || item.is(Items.STICK) || item.is(Items.APPLE)) {
                    WorkInventoryHelper.storeProducedItem(cat, item.copy());
                    ie.discard();
                }
            }
        }

        // Replant sapling at the base
        if (lowestBasePos != null && sl.getBlockState(lowestBasePos).isAir()) {
            Block matchingSapling = getMatchingSapling(felledLogBlock);
            ItemStack saplingStack = WorkInventoryHelper.extractMatchingSupply(
                    cat,
                    s -> (matchingSapling != null && s.is(matchingSapling.asItem())) || s.is(ItemTags.SAPLINGS),
                    1
            );
            Block toPlant = matchingSapling;
            if (!saplingStack.isEmpty() && saplingStack.getItem() instanceof BlockItem bi) {
                toPlant = bi.getBlock();
            } else if (saplingStack.isEmpty() && matchingSapling != null) {
                // Default sapling replant from felled tree canopy
                toPlant = matchingSapling;
            }
            if (toPlant != null) {
                WorkerFakePlayer.placeBlock(sl, lowestBasePos, toPlant.defaultBlockState(), cat.getOwnerUUID());
            }
        }

        if (felledCount > 0) {
            cat.playSound(SoundEvents.WOOD_BREAK, 0.9F, 1.0F);
            return true;
        }
        return false;
    }

    private static Block getMatchingSapling(Block logBlock) {
        if (logBlock == Blocks.SPRUCE_LOG || logBlock == Blocks.SPRUCE_WOOD) return Blocks.SPRUCE_SAPLING;
        if (logBlock == Blocks.BIRCH_LOG || logBlock == Blocks.BIRCH_WOOD) return Blocks.BIRCH_SAPLING;
        if (logBlock == Blocks.JUNGLE_LOG || logBlock == Blocks.JUNGLE_WOOD) return Blocks.JUNGLE_SAPLING;
        if (logBlock == Blocks.ACACIA_LOG || logBlock == Blocks.ACACIA_WOOD) return Blocks.ACACIA_SAPLING;
        if (logBlock == Blocks.DARK_OAK_LOG || logBlock == Blocks.DARK_OAK_WOOD) return Blocks.DARK_OAK_SAPLING;
        if (logBlock == Blocks.CHERRY_LOG || logBlock == Blocks.CHERRY_WOOD) return Blocks.CHERRY_SAPLING;
        if (logBlock == Blocks.MANGROVE_LOG || logBlock == Blocks.MANGROVE_WOOD) return Blocks.MANGROVE_PROPAGULE;
        return Blocks.OAK_SAPLING;
    }
}
