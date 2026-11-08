package com.kittytalents.entity.ai;

import com.kittytalents.entity.TalentedCat;
import com.kittytalents.talent.Talents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;

public class CatPlaceTorchGoal extends Goal {
    private final TalentedCat cat;
    private BlockPos targetTorchPos = null;
    private BlockPos cachedNearbyTorch = null;
    private int cooldown = 60;
    private int placementProgress = 0;

    public CatPlaceTorchGoal(TalentedCat cat) {
        this.cat = cat;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (cat.isIncapacitated() || cat.isOrderedToSit() || cat.isSleeping() || cat.isPassenger() || cat.isTwined()) return false;
        int torchLvl = cat.getTalentLevel(Talents.TORCH_KITTY);
        if (torchLvl <= 0) return false;

        if (cooldown > 0) {
            cooldown--;
            return false;
        }

        // Check if cat has torches
        if (!hasTorches()) {
            this.cooldown = 40;
            return false;
        }

        // Check darkness: block light <= 6
        BlockPos catPos = cat.blockPosition();
        if (cat.level().getBrightness(LightLayer.BLOCK, catPos) > 6) {
            this.cooldown = 40;
            return false;
        }

        // Search for valid legal torch spot
        this.targetTorchPos = findValidTorchPos(torchLvl);
        if (this.targetTorchPos == null) {
            this.cooldown = 40;
            return false;
        }
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return targetTorchPos != null && !cat.isIncapacitated() && !cat.isOrderedToSit() && !cat.isSleeping() && !cat.isTwined() && placementProgress < 100;
    }

    @Override
    public void start() {
        this.placementProgress = 0;
        if (targetTorchPos != null) {
            cat.getNavigation().moveTo(targetTorchPos.getX() + 0.5D, targetTorchPos.getY(), targetTorchPos.getZ() + 0.5D, 1.25D);
        }
    }

    @Override
    public void stop() {
        this.targetTorchPos = null;
        this.placementProgress = 0;
        this.cachedNearbyTorch = null;
        int torchLvl = cat.getTalentLevel(Talents.TORCH_KITTY);
        this.cooldown = Math.max(60, 240 - torchLvl * 35);
    }

    @Override
    public void tick() {
        if (targetTorchPos == null) return;

        cat.getLookControl().setLookAt(targetTorchPos.getX() + 0.5D, targetTorchPos.getY() + 0.5D, targetTorchPos.getZ() + 0.5D, 30.0F, 30.0F);
        double distSq = cat.distanceToSqr(targetTorchPos.getX() + 0.5D, targetTorchPos.getY(), targetTorchPos.getZ() + 0.5D);

        if (distSq > 4.0D) {
            if (cat.getNavigation().isDone() && cat.tickCount % 10 == 0) {
                cat.getNavigation().moveTo(targetTorchPos.getX() + 0.5D, targetTorchPos.getY(), targetTorchPos.getZ() + 0.5D, 1.25D);
            }
        } else {
            cat.getNavigation().stop();
            placementProgress += 10;

            if (placementProgress >= 30) {
                // Verify position is still valid and place
                if (isValidPlacementLocation(targetTorchPos)) {
                    if (consumeOneTorch()) {
                        cat.level().setBlock(targetTorchPos, Blocks.TORCH.defaultBlockState(), 3);
                        cat.level().playSound(null, targetTorchPos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);

                        if (cat.level() instanceof ServerLevel serverLevel) {
                            serverLevel.sendParticles(ParticleTypes.FLAME,
                                    targetTorchPos.getX() + 0.5D, targetTorchPos.getY() + 0.7D, targetTorchPos.getZ() + 0.5D,
                                    5, 0.1D, 0.1D, 0.1D, 0.02D);
                        }
                    }
                }
                stop();
            }
        }
    }

    private boolean hasTorches() {
        // Check pack inventory
        SimpleContainer pack = cat.getCatPack();
        for (int i = 0; i < pack.getContainerSize(); i++) {
            ItemStack stack = pack.getItem(i);
            if (!stack.isEmpty() && isTorchItem(stack)) {
                return true;
            }
        }
        // Check tool slot
        ItemStack tool = cat.getEquippedTool();
        if (!tool.isEmpty() && isTorchItem(tool)) {
            return true;
        }
        return false;
    }

    private boolean consumeOneTorch() {
        if (com.kittytalents.talent.effect.TalentEngine.isMastered(cat, com.kittytalents.talent.Talents.TORCH_KITTY)) return true; // mastery: torches are free
        SimpleContainer pack = cat.getCatPack();
        for (int i = 0; i < pack.getContainerSize(); i++) {
            ItemStack stack = pack.getItem(i);
            if (!stack.isEmpty() && isTorchItem(stack)) {
                stack.shrink(1);
                return true;
            }
        }
        ItemStack tool = cat.getEquippedTool();
        if (!tool.isEmpty() && isTorchItem(tool)) {
            tool.shrink(1);
            cat.setEquippedTool(tool.isEmpty() ? ItemStack.EMPTY : tool);
            return true;
        }
        return false;
    }

    private boolean isTorchItem(ItemStack stack) {
        return stack.is(Items.TORCH) || stack.is(Items.SOUL_TORCH) || stack.is(Items.REDSTONE_TORCH);
    }

    private BlockPos findValidTorchPos(int torchLvl) {
        BlockPos center = cat.blockPosition();
        int radius = Math.min(6, 2 + torchLvl);
        int vertical = 2;

        BlockPos bestPos = null;
        double closestDist = Double.MAX_VALUE;

        for (int y = -vertical; y <= vertical; y++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    if (isValidPlacementLocation(pos)) {
                        // Ensure no torch within 4 blocks already; use cached nearby torch to eliminate nested scan
                        if (!hasTorchNear(pos, 4)) {
                            double dist = center.distSqr(pos);
                            if (dist < closestDist) {
                                closestDist = dist;
                                bestPos = pos.immutable();
                            }
                        }
                    }
                }
            }
        }
        return bestPos;
    }

    private boolean isValidPlacementLocation(BlockPos pos) {
        if (!cat.level().isEmptyBlock(pos)) return false;
        if (cat.level().getBrightness(LightLayer.BLOCK, pos) > 6) return false;
        BlockPos below = pos.below();
        BlockState belowState = cat.level().getBlockState(below);
        if (belowState.is(Blocks.LAVA) || belowState.is(Blocks.WATER)) return false;
        if (!belowState.isFaceSturdy(cat.level(), below, Direction.UP)) return false;
        if (!Blocks.TORCH.defaultBlockState().canSurvive(cat.level(), pos)) return false;
        return true;
    }

    private boolean hasTorchNear(BlockPos pos, int radius) {
        if (cachedNearbyTorch != null && pos.distSqr(cachedNearbyTorch) <= (radius * radius)) {
            BlockState cachedState = cat.level().getBlockState(cachedNearbyTorch);
            if (isTorchState(cachedState)) {
                return true;
            } else {
                cachedNearbyTorch = null;
            }
        }

        for (BlockPos nearby : BlockPos.betweenClosed(pos.offset(-radius, -1, -radius), pos.offset(radius, 1, radius))) {
            BlockState state = cat.level().getBlockState(nearby);
            if (isTorchState(state)) {
                cachedNearbyTorch = nearby.immutable();
                return true;
            }
        }
        return false;
    }

    private boolean isTorchState(BlockState state) {
        return state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH) || state.is(Blocks.SOUL_TORCH) || state.is(Blocks.SOUL_WALL_TORCH);
    }
}
