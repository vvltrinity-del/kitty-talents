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

    /**
     * Container jobs (Sorter, Courier): the cat must really stand at the block with a clear line to it. Without this a
     * cat on the surface counted as "arrived" at a chest a few blocks underground and worked through the floor.
     */
    protected boolean needsClearReach() {
        return false;
    }

    /** True if nothing solid is between the cat and {@code pos} (the block itself, or its other chest half, may be hit). */
    protected boolean hasClearLineTo(BlockPos pos) {
        net.minecraft.world.phys.Vec3 from = cat.getEyePosition();
        net.minecraft.world.phys.Vec3 to = net.minecraft.world.phys.Vec3.atCenterOf(pos);
        net.minecraft.world.phys.BlockHitResult hit = cat.level().clip(new net.minecraft.world.level.ClipContext(from, to,
                net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, cat));
        if (hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS) return true;
        BlockPos at = hit.getBlockPos();
        return at.equals(pos) || (at.distManhattan(pos) == 1 && cat.level().getBlockState(at).is(cat.level().getBlockState(pos).getBlock()));
    }

    /** Within reach of {@code pos}, and (for container jobs) actually able to get at it. */
    protected boolean canWorkOn(BlockPos pos, double reachSqr) {
        double d = cat.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
        return d <= reachSqr && (!needsClearReach() || hasClearLineTo(pos));
    }

    /**
     * Where to walk to work on {@code pos}: for container jobs, a free spot with a floor right beside the block (so the
     * path leads down into the cellar the chest is in, not to the grass above it); otherwise the block itself.
     */
    protected BlockPos walkTarget(BlockPos pos) {
        if (!needsClearReach()) return pos;
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            for (int dy = 0; dy >= -1; dy--) {
                BlockPos spot = pos.relative(dir).above(dy);
                var level = cat.level();
                if (!level.getBlockState(spot).getCollisionShape(level, spot).isEmpty()) continue;
                if (level.getBlockState(spot.below()).getCollisionShape(level, spot.below()).isEmpty()) continue;
                double d = cat.distanceToSqr(spot.getX() + 0.5D, spot.getY(), spot.getZ() + 0.5D);
                if (d < bestDist) {
                    bestDist = d;
                    best = spot;
                }
            }
        }
        return best != null ? best : pos;
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
                boolean arrived = canWorkOn(currentTargetPos, getInteractReachSqr());
                if (arrived) {
                    cat.getNavigation().stop();
                    state = WorkState.WORKING;
                    stateTimer = 0;
                    taskDurationTicks = getEffectiveTaskDurationTicks();
                    cat.setWorkStatus("Working (" + targetJob.getDisplayName() + ")");
                    return;
                }

                if (cat.getNavigation().isDone()) { // stopped without being able to work on it
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
                    BlockPos walkTo = walkTarget(currentTargetPos);
                    boolean started = cat.getNavigation().moveTo(
                            walkTo.getX() + 0.5D,
                            walkTo.getY(),
                            walkTo.getZ() + 0.5D,
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
                // Walk right up to the deposit container (a spot beside it, with a clear line), not just near it:
                // within 3 blocks through a floor or wall does not count. If it truly cannot get closer, it gives up
                // being picky after a few seconds rather than standing there forever.
                if (depositPos != null) {
                    boolean near = cat.distanceToSqr(depositPos.getX() + 0.5D, depositPos.getY() + 0.5D, depositPos.getZ() + 0.5D) <= 9.0D;
                    boolean clear = near && hasClearLineTo(depositPos);
                    if (!clear && !(near && depositStuckTicks > 100)) {
                        cat.setWorkStatus("Returning to deposit");
                        if (cat.getNavigation().isDone()) depositStuckTicks++;
                        if (--navCooldown <= 0) {
                            navCooldown = 10;
                            BlockPos walkTo = depositWalkTarget(depositPos);
                            cat.getNavigation().moveTo(walkTo.getX() + 0.5D, walkTo.getY(), walkTo.getZ() + 0.5D, 1.2D);
                        }
                        return;
                    }
                }
                depositStuckTicks = 0;
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

    private int depositStuckTicks;

    private BlockPos depositWalkTarget(BlockPos pos) {
        for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            BlockPos spot = pos.relative(dir);
            var level = cat.level();
            if (level.getBlockState(spot).getCollisionShape(level, spot).isEmpty()
                    && !level.getBlockState(spot.below()).getCollisionShape(level, spot.below()).isEmpty()) return spot;
        }
        return pos;
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
