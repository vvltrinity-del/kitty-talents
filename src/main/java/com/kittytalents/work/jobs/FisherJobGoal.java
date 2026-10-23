package com.kittytalents.work.jobs;

import com.kittytalents.entity.TalentedCat;
import com.kittytalents.talent.Talents;
import com.kittytalents.work.AbstractCatJobGoal;
import com.kittytalents.work.AreaCursor;
import com.kittytalents.work.CatJob;
import com.kittytalents.work.WorkInventoryHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class FisherJobGoal extends AbstractCatJobGoal {
    public FisherJobGoal(TalentedCat cat) {
        super(cat, CatJob.FISHER);
    }

    @Override
    protected int getBaseTaskDurationTicks() {
        return 60;
    }

    @Override
    protected double getInteractReachSqr() {
        return 12.25D; // 3.5 blocks from water edge
    }

    @Override
    @Nullable
    protected BlockPos findNextTask(int scanBudget) {
        AreaCursor cursor = getCursor();
        for (int i = 0; i < scanBudget; i++) {
            BlockPos pos = cursor.next();
            if (isBlacklisted(pos) || !cat.level().isLoaded(pos)) continue;
            if (cat.level().getFluidState(pos).is(FluidTags.WATER) && cat.level().getBlockState(pos.above()).isAir()) {
                return pos;
            }
        }
        return null;
    }

    @Override
    protected boolean isTaskStillValid(BlockPos pos) {
        return cat.level().isLoaded(pos) && cat.level().getFluidState(pos).is(FluidTags.WATER);
    }

    @Override
    protected boolean executeTask(BlockPos pos) {
        if (!(cat.level() instanceof ServerLevel sl)) return false;
        int fisherRank = Math.max(1, cat.getTalentLevel(Talents.FISHER_CAT));

        LootParams params = new LootParams.Builder(sl)
                .withParameter(LootContextParams.ORIGIN, pos.getCenter())
                .withParameter(LootContextParams.TOOL, new ItemStack(Items.FISHING_ROD))
                .withParameter(LootContextParams.THIS_ENTITY, cat)
                .withLuck(fisherRank)
                .create(LootContextParamSets.FISHING);

        LootTable table = sl.getServer().getLootData().getLootTable(BuiltInLootTables.FISHING);
        List<ItemStack> catches = table.getRandomItems(params);
        if (catches.isEmpty()) {
            catches = List.of(new ItemStack(Items.COD));
        }
        for (ItemStack c : catches) {
            if (cat.getJobData().getFilter().matches(c)) {
                WorkInventoryHelper.storeProducedItem(cat, c);
            }
        }
        sl.sendParticles(ParticleTypes.SPLASH, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, 8, 0.2D, 0.1D, 0.2D, 0.05D);
        cat.playSound(SoundEvents.FISHING_BOBBER_SPLASH, 0.8F, 1.2F);
        return true;
    }
}
