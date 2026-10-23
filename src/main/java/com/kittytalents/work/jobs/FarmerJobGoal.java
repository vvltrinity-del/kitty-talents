package com.kittytalents.work.jobs;

import com.kittytalents.entity.TalentedCat;
import com.kittytalents.talent.Talents;
import com.kittytalents.work.AbstractCatJobGoal;
import com.kittytalents.work.AreaCursor;
import com.kittytalents.work.CatJob;
import com.kittytalents.work.WorkInventoryHelper;
import com.kittytalents.work.WorkerFakePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.IPlantable;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

public class FarmerJobGoal extends AbstractCatJobGoal {
    public FarmerJobGoal(TalentedCat cat) {
        super(cat, CatJob.FARMER);
    }

    @Override
    protected int getBaseTaskDurationTicks() {
        return 20;
    }

    @Override
    protected Predicate<ItemStack> getKeepInPackPredicate() {
        return FarmerJobGoal::isSeedOrBonemeal;
    }

    public static boolean isSeedOrBonemeal(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.is(Items.BONE_MEAL)) return true;
        if (stack.getItem() instanceof BlockItem bi) {
            Block b = bi.getBlock();
            return b instanceof CropBlock || b instanceof NetherWartBlock || b instanceof StemBlock || b instanceof CocoaBlock;
        }
        return false;
    }

    @Override
    @Nullable
    protected BlockPos findNextTask(int scanBudget) {
        Level level = cat.level();
        // Guard against crop-eating rabbits in the work area
        AABB box = AABB.of(getWorkArea());
        List<Rabbit> rabbits = level.getEntitiesOfClass(Rabbit.class, box);
        if (!rabbits.isEmpty() && cat.getTarget() == null) {
            cat.setTarget(rabbits.get(0));
        }

        int greenPaw = cat.getTalentLevel(Talents.GREEN_PAW);
        AreaCursor cursor = getCursor();
        for (int i = 0; i < scanBudget; i++) {
            BlockPos pos = cursor.next();
            if (isBlacklisted(pos) || !level.isLoaded(pos)) continue;
            if (isActionableFarmPos(level, pos, greenPaw)) {
                return pos;
            }
        }
        return null;
    }

    private boolean isActionableFarmPos(Level level, BlockPos pos, int greenPaw) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();

        // 1. Mature CropBlock
        if (block instanceof CropBlock crop && crop.isMaxAge(state)) {
            return true;
        }
        // 2. Mature NetherWart
        if (block instanceof NetherWartBlock && state.getValue(NetherWartBlock.AGE) >= 3) {
            return true;
        }
        // 3. Mature Cocoa
        if (block instanceof CocoaBlock && state.getValue(CocoaBlock.AGE) >= 2) {
            return true;
        }
        // 4. Mature Sweet Berry Bush
        if (block instanceof SweetBerryBushBlock && state.getValue(SweetBerryBushBlock.AGE) >= 2) {
            return true;
        }
        // 5. Sugar Cane / Cactus upper segments (keep bottom block intact)
        if (block instanceof SugarCaneBlock || block instanceof CactusBlock) {
            BlockState below = level.getBlockState(pos.below());
            BlockState twoBelow = level.getBlockState(pos.below(2));
            if (below.is(block) && !twoBelow.is(block)) {
                return true;
            }
        }
        // 6. Melon / Pumpkin
        if (block instanceof StemGrownBlock) {
            return true;
        }
        // 7. Empty farmland with air above -> plant seed if available
        if (state.is(Blocks.FARMLAND) && level.getBlockState(pos.above()).isAir()) {
            return hasSeedsAvailable(false);
        }
        // 8. Bonemeal immature crop at GREEN_PAW >= 3
        if (greenPaw >= 3 && block instanceof CropBlock crop && !crop.isMaxAge(state)) {
            return hasBonemealAvailable();
        }
        // 9. Till dirt/grass near water at GREEN_PAW >= 5
        if (greenPaw >= 5 && (state.is(Blocks.DIRT) || state.is(Blocks.GRASS_BLOCK)) && level.getBlockState(pos.above()).isAir()) {
            return isNearWater(level, pos);
        }
        return false;
    }

    private boolean isNearWater(Level level, BlockPos dirtPos) {
        for (BlockPos p : BlockPos.betweenClosed(dirtPos.offset(-4, 0, -4), dirtPos.offset(4, 1, 4))) {
            if (level.getFluidState(p).is(FluidTags.WATER)) return true;
        }
        return false;
    }

    private boolean hasSeedsAvailable(boolean netherWart) {
        Predicate<ItemStack> pred = s -> {
            if (s.getItem() instanceof BlockItem bi) {
                return netherWart ? bi.getBlock() instanceof NetherWartBlock : bi.getBlock() instanceof CropBlock;
            }
            return false;
        };
        for (int i = 0; i < cat.getCatPack().getContainerSize(); i++) {
            if (pred.test(cat.getCatPack().getItem(i))) return true;
        }
        return false;
    }

    private boolean hasBonemealAvailable() {
        for (int i = 0; i < cat.getCatPack().getContainerSize(); i++) {
            if (cat.getCatPack().getItem(i).is(Items.BONE_MEAL)) return true;
        }
        return false;
    }

    @Override
    protected boolean isTaskStillValid(BlockPos pos) {
        if (!cat.level().isLoaded(pos)) return false;
        return isActionableFarmPos(cat.level(), pos, cat.getTalentLevel(Talents.GREEN_PAW));
    }

    @Override
    protected boolean executeTask(BlockPos pos) {
        if (!(cat.level() instanceof ServerLevel sl)) return false;
        BlockState state = sl.getBlockState(pos);
        Block block = state.getBlock();
        int greenPaw = cat.getTalentLevel(Talents.GREEN_PAW);

        // Case A: Mature CropBlock
        if (block instanceof CropBlock crop && crop.isMaxAge(state)) {
            List<ItemStack> drops = WorkerFakePlayer.breakBlockAndCollectDrops(sl, pos, cat.getOwnerUUID(), new ItemStack(Items.IRON_HOE));
            if (drops.isEmpty()) return false;
            boolean replanted = false;
            Iterator<ItemStack> it = drops.iterator();
            while (it.hasNext()) {
                ItemStack drop = it.next();
                if (!replanted && drop.getItem() instanceof BlockItem bi && bi.getBlock() == block) {
                    drop.shrink(1);
                    if (drop.isEmpty()) it.remove();
                    WorkerFakePlayer.placeBlock(sl, pos, crop.getStateForAge(0), cat.getOwnerUUID());
                    replanted = true;
                }
            }
            if (!replanted) {
                ItemStack seed = WorkInventoryHelper.extractMatchingSupply(cat, s -> s.getItem() instanceof BlockItem bi && bi.getBlock() instanceof CropBlock, 1);
                if (!seed.isEmpty() && seed.getItem() instanceof BlockItem bi && bi.getBlock() instanceof CropBlock newCrop) {
                    WorkerFakePlayer.placeBlock(sl, pos, newCrop.getStateForAge(0), cat.getOwnerUUID());
                }
            }
            for (ItemStack d : drops) {
                if (!d.isEmpty() && cat.getJobData().getFilter().matches(d)) {
                    WorkInventoryHelper.storeProducedItem(cat, d);
                }
            }
            cat.playSound(SoundEvents.CROP_BREAK, 0.8F, 1.1F);
            return true;
        }

        // Case B: Mature NetherWart
        if (block instanceof NetherWartBlock && state.getValue(NetherWartBlock.AGE) >= 3) {
            List<ItemStack> drops = WorkerFakePlayer.breakBlockAndCollectDrops(sl, pos, cat.getOwnerUUID(), ItemStack.EMPTY);
            if (drops.isEmpty()) return false;
            boolean replanted = false;
            for (ItemStack d : drops) {
                if (!replanted && d.is(Items.NETHER_WART)) {
                    d.shrink(1);
                    WorkerFakePlayer.placeBlock(sl, pos, Blocks.NETHER_WART.defaultBlockState(), cat.getOwnerUUID());
                    replanted = true;
                }
                if (!d.isEmpty() && cat.getJobData().getFilter().matches(d)) {
                    WorkInventoryHelper.storeProducedItem(cat, d);
                }
            }
            return true;
        }

        // Case C: Mature Cocoa
        if (block instanceof CocoaBlock && state.getValue(CocoaBlock.AGE) >= 2) {
            List<ItemStack> drops = WorkerFakePlayer.breakBlockAndCollectDrops(sl, pos, cat.getOwnerUUID(), ItemStack.EMPTY);
            if (drops.isEmpty()) return false;
            boolean replanted = false;
            for (ItemStack d : drops) {
                if (!replanted && d.is(Items.COCOA_BEANS)) {
                    d.shrink(1);
                    WorkerFakePlayer.placeBlock(sl, pos, state.setValue(CocoaBlock.AGE, 0), cat.getOwnerUUID());
                    replanted = true;
                }
                if (!d.isEmpty() && cat.getJobData().getFilter().matches(d)) {
                    WorkInventoryHelper.storeProducedItem(cat, d);
                }
            }
            return true;
        }

        // Case D: Sweet Berry Bush (gentle harvest back to age 1)
        if (block instanceof SweetBerryBushBlock && state.getValue(SweetBerryBushBlock.AGE) >= 2) {
            int age = state.getValue(SweetBerryBushBlock.AGE);
            int count = (age == 3 ? 2 : 1) + sl.random.nextInt(2);
            if (WorkerFakePlayer.placeBlock(sl, pos, state.setValue(SweetBerryBushBlock.AGE, 1), cat.getOwnerUUID())) {
                ItemStack berries = new ItemStack(Items.SWEET_BERRIES, count);
                if (cat.getJobData().getFilter().matches(berries)) {
                    WorkInventoryHelper.storeProducedItem(cat, berries);
                }
                cat.playSound(SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, 0.8F, 1.0F);
                return true;
            }
            return false;
        }

        // Case E: Sugar Cane / Cactus column top-down
        if (block instanceof SugarCaneBlock || block instanceof CactusBlock) {
            BlockPos top = pos;
            while (sl.getBlockState(top.above()).is(block)) {
                top = top.above();
            }
            boolean brokeAny = false;
            while (top.getY() >= pos.getY() && sl.getBlockState(top).is(block)) {
                List<ItemStack> drops = WorkerFakePlayer.breakBlockAndCollectDrops(sl, top, cat.getOwnerUUID(), ItemStack.EMPTY);
                for (ItemStack d : drops) {
                    if (cat.getJobData().getFilter().matches(d)) {
                        WorkInventoryHelper.storeProducedItem(cat, d);
                    }
                }
                brokeAny = true;
                top = top.below();
            }
            return brokeAny;
        }

        // Case F: Melon / Pumpkin
        if (block instanceof StemGrownBlock) {
            List<ItemStack> drops = WorkerFakePlayer.breakBlockAndCollectDrops(sl, pos, cat.getOwnerUUID(), new ItemStack(Items.IRON_AXE));
            for (ItemStack d : drops) {
                if (cat.getJobData().getFilter().matches(d)) {
                    WorkInventoryHelper.storeProducedItem(cat, d);
                }
            }
            return !drops.isEmpty();
        }

        // Case G: Empty Farmland -> Plant seeds
        if (state.is(Blocks.FARMLAND) && sl.getBlockState(pos.above()).isAir()) {
            ItemStack seed = WorkInventoryHelper.extractMatchingSupply(cat, s -> s.getItem() instanceof BlockItem bi && bi.getBlock() instanceof CropBlock, 1);
            if (!seed.isEmpty() && seed.getItem() instanceof BlockItem bi && bi.getBlock() instanceof CropBlock crop) {
                if (WorkerFakePlayer.placeBlock(sl, pos.above(), crop.getStateForAge(0), cat.getOwnerUUID())) {
                    cat.playSound(SoundEvents.CROP_PLANTED, 0.8F, 1.0F);
                    return true;
                } else {
                    WorkInventoryHelper.storeProducedItem(cat, seed);
                }
            }
            return false;
        }

        // Case H: Bonemeal immature crop (GREEN_PAW >= 3)
        if (greenPaw >= 3 && block instanceof CropBlock crop && !crop.isMaxAge(state)) {
            ItemStack bm = WorkInventoryHelper.extractMatchingSupply(cat, s -> s.is(Items.BONE_MEAL), 1);
            if (!bm.isEmpty()) {
                crop.growCrops(sl, pos, state);
                sl.levelEvent(1505, pos, 0);
                return true;
            }
            return false;
        }

        // Case I: Till dirt/grass near water (GREEN_PAW >= 5)
        if (greenPaw >= 5 && (state.is(Blocks.DIRT) || state.is(Blocks.GRASS_BLOCK)) && sl.getBlockState(pos.above()).isAir() && isNearWater(sl, pos)) {
            if (WorkerFakePlayer.placeBlock(sl, pos, Blocks.FARMLAND.defaultBlockState(), cat.getOwnerUUID())) {
                cat.playSound(SoundEvents.HOE_TILL, 0.8F, 1.0F);
                return true;
            }
        }

        return false;
    }
}
