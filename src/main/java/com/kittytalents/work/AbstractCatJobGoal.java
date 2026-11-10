package com.kittytalents.work;

import com.kittytalents.config.KittyTalentsConfig;
import com.kittytalents.entity.CatMode;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.talent.Talents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Shared state machine for all CatJob worker goals:
 * IDLE -> FIND_TASK -> PATHING -> WORKING -> DEPOSIT -> STALLED -> REST
 */
public abstract class AbstractCatJobGoal extends Goal {
    public enum WorkState {
        IDLE, FIND_TASK, PATHING, WORKING, DEPOSIT, STALLED, REST
    }

    protected final TalentedCat cat;
    protected final CatJob targetJob;
    protected WorkState state = WorkState.IDLE;
    @Nullable
    protected BlockPos currentTargetPos = null;
    protected int stateTimer = 0;
    protected int navCooldown = 0;
    protected int pathFailCount = 0;
    protected int idleBackoffTicks = 0;
    protected int taskDurationTicks = 20;
    protected final Map<BlockPos, Long> blacklistedPositions = new HashMap<>();
    @Nullable
    protected AreaCursor areaCursor = null;
    @Nullable
    private BoundingBox lastCursorBox = null;

    protected AbstractCatJobGoal(TalentedCat cat, CatJob targetJob) {
        this.cat = cat;
        this.targetJob = targetJob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    protected boolean TopDownScan() {
        return false;
    }

    protected int getBaseTaskDurationTicks() {
        return 25;
    }

    protected int getEffectiveTaskDurationTicks() {
        int base = getBaseTaskDurationTicks();
        int ethic = cat.getTalentLevel(Talents.WORK_ETHIC);
        double mult = Math.max(0.35D, 1.0D - (0.08D * ethic));
        return Math.max(4, (int) Math.round(base * mult));
    }

    protected double getInteractReachSqr() {
        return 12.25D; // 3.5 blocks reach
    }

    @Nullable
    protected Predicate<ItemStack> getKeepInPackPredicate() {
        return null;
    }

    protected boolean shouldDeposit() {
        JobData data = cat.getJobData();
        if (data.getStationPos() == null && data.getLinkedOutputs().isEmpty()) return false;
        return WorkInventoryHelper.isCatPackNearlyFull(cat);
    }

    @Nullable
    private BlockPos anchoredWorkOrigin = null;

    public BoundingBox getWorkArea() {
        int defaultRadius = 8;
        if (cat.getJobData().getStationPos() != null) {
            return cat.getJobData().getEffectiveWorkArea(cat.getJobData().getStationPos(), defaultRadius);
        }
        if (cat.getJobData().getCustomWorkArea() != null) {
            BoundingBox custom = cat.getJobData().getCustomWorkArea();
            if (custom.maxY() - custom.minY() <= 1) {
                return new BoundingBox(custom.minX(), custom.minY() - 2, custom.minZ(), custom.maxX(), custom.maxY() + 4, custom.maxZ());
            }
            return custom;
        }
        if (anchoredWorkOrigin == null || cat.distanceToSqr(anchoredWorkOrigin.getX(), anchoredWorkOrigin.getY(), anchoredWorkOrigin.getZ()) > 1024.0D) {
            anchoredWorkOrigin = cat.blockPosition();
        }
        return cat.getJobData().getEffectiveWorkArea(anchoredWorkOrigin, defaultRadius);
    }

    protected AreaCursor getCursor() {
        BoundingBox box = getWorkArea();
        if (areaCursor == null || lastCursorBox == null || !lastCursorBox.equals(box)) {
            lastCursorBox = box;
            areaCursor = new AreaCursor(box, TopDownScan());
        }
        return areaCursor;
    }

    protected boolean isBlacklisted(BlockPos pos) {
        if (blacklistedPositions.isEmpty()) return false;
        long now = cat.level().getGameTime();
        Long until = blacklistedPositions.get(pos);
        if (until == null) return false;
        if (now >= until) {
            blacklistedPositions.remove(pos);
            return false;
        }
        return true;
    }

    protected void blacklistPosition(BlockPos pos, int durationTicks) {
        if (pos == null) return;
        long now = cat.level().getGameTime();
        if (blacklistedPositions.size() > 64) {
            Iterator<Map.Entry<BlockPos, Long>> it = blacklistedPositions.entrySet().iterator();
            while (it.hasNext()) {
                if (now >= it.next().getValue()) it.remove();
            }
        }
        blacklistedPositions.put(pos.immutable(), now + durationTicks);
    }

    @Override
    public boolean canUse() {
        if (cat.level().isClientSide) return false;
        if (cat.isTwined() || cat.isIncapacitated() || cat.isOrderedToSit() || cat.isSleeping()) return false;
        if (cat.getCatMode() != CatMode.WORK) return false;
        JobData data = cat.getJobData();
        if (data.getJob() != targetJob || !data.isClockedIn()) return false;
        if (!targetJob.canCatPerform(cat)) {
            cat.setWorkStatus("Unqualified for " + targetJob.getDisplayName());
            return false;
        }
        if (cat.getHunger() <= 20) {
            cat.setWorkStatus("Too Hungry (<20)");
            return false;
        } else if (cat.getWorkStatus().contains("Too Hungry")) {
            cat.setWorkStatus("Idle");
        }
        if (idleBackoffTicks > 0) {
            idleBackoffTicks--;
            return false;
        }
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (cat.level().isClientSide) return false;
        if (cat.isTwined() || cat.isIncapacitated() || cat.isOrderedToSit() || cat.isSleeping()) return false;
        if (cat.getCatMode() != CatMode.WORK) return false;
        JobData data = cat.getJobData();
        return data.getJob() == targetJob && data.isClockedIn() && cat.getHunger() > 15;
    }

    @Override
    public void start() {
        state = WorkState.FIND_TASK;
        stateTimer = 0;
        navCooldown = 0;
        pathFailCount = 0;
        cat.setWorkStatus("Scanning (" + targetJob.getDisplayName() + ")");
    }

    @Override
    public void stop() {
        currentTargetPos = null;
        state = WorkState.IDLE;
    }

    @Override
    public void tick() {
        JobData data = cat.getJobData();

        if (cat.getHunger() < 30) {
            state = WorkState.REST;
            cat.setWorkStatus("Resting / Hungry");
            idleBackoffTicks = 40;
            return;
        }

        switch (state) {
            case IDLE, REST -> {
                state = WorkState.FIND_TASK;
            }
            case FIND_TASK -> {
                if (shouldDeposit()) {
                    state = WorkState.DEPOSIT;
                    return;
                }
                int budget = Math.max(256, KittyTalentsConfig.COMMON.workScanBlocksPerTick.get() * 4);
                BlockPos found = findNextTask(budget);
                if (found != null) {
                    currentTargetPos = found.immutable();
                    pathFailCount = 0;
                    navCooldown = 0;
                    state = WorkState.PATHING;
                    cat.setWorkStatus("Moving to task (" + targetJob.getDisplayName() + ")");
                } else if (getCursor().checkAndResetWrapped()) {
                    cat.setWorkStatus("Idle - No task found");
                    idleBackoffTicks = 60;
                    state = WorkState.IDLE;
                } else {
                    cat.setWorkStatus("Scanning (" + targetJob.getDisplayName() + ")");
                }
            }
            case PATHING -> {
                if (currentTargetPos == null || !isTaskStillValid(currentTargetPos)) {
                    currentTargetPos = null;
                    state = WorkState.FIND_TASK;
                    return;
                }
                double distSqr = cat.distanceToSqr(
                        currentTargetPos.getX() + 0.5D,
                        currentTargetPos.getY() + 0.5D,
                        currentTargetPos.getZ() + 0.5D
                );
                if (distSqr <= getInteractReachSqr()) {
                    cat.getNavigation().stop();
                    state = WorkState.WORKING;
                    stateTimer = 0;
                    taskDurationTicks = getEffectiveTaskDurationTicks();
                    cat.setWorkStatus("Working (" + targetJob.getDisplayName() + ")");
                    return;
                }

                if (cat.getNavigation().isDone() && distSqr > getInteractReachSqr()) {
                    pathFailCount++;
                    if (pathFailCount >= 3) {
                        blacklistPosition(currentTargetPos, 600);
                        currentTargetPos = null;
                        state = WorkState.FIND_TASK;
                        cat.setWorkStatus("Cannot reach target");
                        return;
                    }
                }

                if (--navCooldown <= 0) {
                    navCooldown = 10;
                    boolean started = cat.getNavigation().moveTo(
                            currentTargetPos.getX() + 0.5D,
                            currentTargetPos.getY(),
                            currentTargetPos.getZ() + 0.5D,
                            1.15D
                    );
                    net.minecraft.world.level.pathfinder.Path p = cat.getNavigation().getPath();
                    if (!started || (p != null && !p.canReach() && p.getDistToTarget() > 3.0F)) {
                        pathFailCount++;
                        if (pathFailCount >= 3) {
                            blacklistPosition(currentTargetPos, 1200);
                            currentTargetPos = null;
                            state = WorkState.FIND_TASK;
                            cat.setWorkStatus("Cannot reach target");
                            return;
                        }
                    }
                }
            }
            case WORKING -> {
                if (currentTargetPos == null || !isTaskStillValid(currentTargetPos)) {
                    currentTargetPos = null;
                    state = WorkState.FIND_TASK;
                    return;
                }
                cat.getLookControl().setLookAt(
                        currentTargetPos.getX() + 0.5D,
                        currentTargetPos.getY() + 0.5D,
                        currentTargetPos.getZ() + 0.5D,
                        30.0F, 30.0F
                );
                stateTimer++;
                if (stateTimer >= taskDurationTicks) {
                    boolean completed = executeTask(currentTargetPos);
                    if (completed) {
                        onTaskCompleted();
                    } else {
                        blacklistPosition(currentTargetPos, 400);
                    }
                    currentTargetPos = null;
                    if (shouldDeposit()) {
                        state = WorkState.DEPOSIT;
                    } else {
                        state = WorkState.FIND_TASK;
                    }
                }
            }
            case DEPOSIT -> {
                BlockPos depositPos = data.getStationPos();
                if (depositPos == null && !data.getLinkedOutputs().isEmpty()) {
                    depositPos = data.getLinkedOutputs().get(0);
                }
                if (depositPos != null && cat.distanceToSqr(depositPos.getX() + 0.5D, depositPos.getY() + 0.5D, depositPos.getZ() + 0.5D) > 9.0D) {
                    cat.setWorkStatus("Returning to deposit");
                    if (--navCooldown <= 0) {
                        navCooldown = 10;
                        cat.getNavigation().moveTo(depositPos.getX() + 0.5D, depositPos.getY(), depositPos.getZ() + 0.5D, 1.2D);
                    }
                    return;
                }
                boolean ok = WorkInventoryHelper.depositPackToStationOrOutputs(cat, getKeepInPackPredicate());
                if (ok) {
                    cat.setWorkStatus("Deposited items");
                    state = WorkState.FIND_TASK;
                } else {
                    cat.setWorkStatus("Stalled: Output Full");
                    state = WorkState.STALLED;
                    stateTimer = 100;
                }
            }
            case STALLED -> {
                if (--stateTimer <= 0) {
                    state = WorkState.DEPOSIT;
                }
            }
        }
    }

    protected void onTaskCompleted() {
        JobData data = cat.getJobData();
        data.setLastWorkTick(cat.level().getGameTime());
        data.addJobXp(1);
        com.kittytalents.core.progression.CatProgression.onWorkTask(cat);
        // Work Ethic mastery "Tireless": working costs no hunger.
        if (com.kittytalents.talent.effect.TalentEngine.isMastered(cat, Talents.WORK_ETHIC)) return;
        // Hunger drain every 8 completed tasks, reduced by HAPPY_EATER and WORK_ETHIC
        int interval = 8 + cat.getTalentLevel(Talents.HAPPY_EATER) * 2 + cat.getTalentLevel(Talents.WORK_ETHIC);
        if (data.getJobXp() % interval == 0) {
            cat.setHunger(Math.max(0, cat.getHunger() - 1));
        }
    }

    @Nullable
    protected abstract BlockPos findNextTask(int scanBudget);

    protected abstract boolean isTaskStillValid(BlockPos pos);

    protected abstract boolean executeTask(BlockPos pos);
}
