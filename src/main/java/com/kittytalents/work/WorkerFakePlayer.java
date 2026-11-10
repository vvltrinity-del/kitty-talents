package com.kittytalents.work;

import com.kittytalents.config.KittyTalentsConfig;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.level.BlockEvent;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Provides a Forge FakePlayer wrapper for worker cats so all block breaking and placement
 * respects claim/protection mods and server configuration.
 */
public final class WorkerFakePlayer {
    private static final UUID DEFAULT_WORKER_UUID =
            UUID.nameUUIDFromBytes("KittyTalentsWorker".getBytes(StandardCharsets.UTF_8));

    private WorkerFakePlayer() {}

    public static FakePlayer get(ServerLevel level, @Nullable UUID ownerUUID) {
        UUID id = ownerUUID != null ? ownerUUID : DEFAULT_WORKER_UUID;
        GameProfile profile = new GameProfile(id, "[KittyWorker]");
        return FakePlayerFactory.get(level, profile);
    }

    public static boolean canBreakBlock(ServerLevel level, BlockPos pos, @Nullable UUID ownerUUID) {
        if (!KittyTalentsConfig.COMMON.allowWorkerBlockBreaking.get()) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.getDestroySpeed(level, pos) < 0.0F) {
            return false;
        }
        // Crops, plants, and stems in farming areas are always permitted for worker cats
        if (state.getBlock() instanceof net.minecraft.world.level.block.CropBlock
                || state.is(net.minecraft.tags.BlockTags.CROPS)
                || state.is(net.minecraft.tags.BlockTags.MAINTAINS_FARMLAND)
                || state.getBlock() instanceof net.minecraft.world.level.block.StemGrownBlock
                || state.getBlock() instanceof net.minecraft.world.level.block.SugarCaneBlock
                || state.getBlock() instanceof net.minecraft.world.level.block.CactusBlock
                || state.getBlock() instanceof net.minecraft.world.level.block.SweetBerryBushBlock
                || state.getBlock() instanceof net.minecraft.world.level.block.NetherWartBlock
                || state.getBlock() instanceof net.minecraft.world.level.block.CocoaBlock) {
            return true;
        }
        for (net.minecraft.world.level.block.state.properties.Property<?> prop : state.getProperties()) {
            if (prop.getName().toLowerCase().contains("age")) return true;
        }
        FakePlayer fake = get(level, ownerUUID);
        fake.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(level, pos, state, fake);
        return !MinecraftForge.EVENT_BUS.post(event);
    }

    public static List<ItemStack> breakBlockAndCollectDrops(ServerLevel level, BlockPos pos, @Nullable UUID ownerUUID, ItemStack tool) {
        if (!canBreakBlock(level, pos, ownerUUID)) {
            return Collections.emptyList();
        }
        BlockState state = level.getBlockState(pos);
        BlockEntity be = level.getBlockEntity(pos);
        FakePlayer fake = get(level, ownerUUID);
        fake.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        fake.setItemInHand(InteractionHand.MAIN_HAND, tool.copy());
        List<ItemStack> drops = Block.getDrops(state, level, pos, be, fake, tool);
        if (drops.isEmpty()) {
            net.minecraft.world.level.storage.loot.LootParams.Builder builder = new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, net.minecraft.world.phys.Vec3.atCenterOf(pos))
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_STATE, state)
                    .withOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY, be)
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.TOOL, tool);
            drops = state.getBlock().getDrops(state, builder);
        }
        level.destroyBlock(pos, false, fake);
        return drops;
    }

    public static boolean placeBlock(ServerLevel level, BlockPos pos, BlockState newState, @Nullable UUID ownerUUID) {
        if (!KittyTalentsConfig.COMMON.allowWorkerBlockBreaking.get()) {
            return false;
        }
        // Crops and farmland replanting are always allowed for workers
        if (newState.getBlock() instanceof net.minecraft.world.level.block.CropBlock
                || newState.is(net.minecraft.tags.BlockTags.CROPS)
                || newState.is(net.minecraft.world.level.block.Blocks.FARMLAND)) {
            return level.setBlock(pos, newState, 3);
        }
        FakePlayer fake = get(level, ownerUUID);
        fake.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        BlockSnapshot snapshot = BlockSnapshot.create(level.dimension(), level, pos);
        BlockEvent.EntityPlaceEvent placeEvent = new BlockEvent.EntityPlaceEvent(snapshot, level.getBlockState(pos.below()), fake);
        if (MinecraftForge.EVENT_BUS.post(placeEvent)) {
            return false;
        }
        return level.setBlock(pos, newState, 3);
    }
}
