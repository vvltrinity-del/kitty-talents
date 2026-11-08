package com.kittytalents.work.jobs;

import com.kittytalents.compat.tacz.TacZCompat;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.work.AbstractCatJobGoal;
import com.kittytalents.work.CatJob;
import com.kittytalents.work.JobData;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * GunsmithJobGoal: Crafts TacZ ammunition (Lv10+) and attachments (Lv25+) at the cat's bound Work Station.
 * Gun recipes are strictly forbidden and rejected.
 */
public class GunsmithJobGoal extends AbstractCatJobGoal {
    public GunsmithJobGoal(TalentedCat cat) {
        super(cat, CatJob.GUNSMITH);
    }

    @Override
    protected int getBaseTaskDurationTicks() {
        return 100;
    }

    @Override
    protected double getInteractReachSqr() {
        return 9.0D;
    }

    @Override
    @Nullable
    protected BlockPos findNextTask(int scanBudget) {
        if (!TacZCompat.isLoaded()) return null;
        JobData data = cat.getJobData();
        BlockPos stationPos = data.getStationPos();
        if (stationPos == null) return null;
        if (TacZCompat.canCraftSelectedGunsmithRecipe(cat)) {
            return stationPos;
        }
        return null;
    }

    @Override
    protected boolean isTaskStillValid(BlockPos pos) {
        return TacZCompat.isLoaded() && TacZCompat.canCraftSelectedGunsmithRecipe(cat);
    }

    @Override
    protected boolean executeTask(BlockPos pos) {
        if (!TacZCompat.isLoaded()) return false;
        return TacZCompat.craftSelectedGunsmithRecipe(cat);
    }
}
