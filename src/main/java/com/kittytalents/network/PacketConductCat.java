package com.kittytalents.network;

import com.kittytalents.core.CatDirectory;
import com.kittytalents.entity.TalentedCat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

public class PacketConductCat extends PacketBase {
    public enum Action {
        TO_SELF,
        TO_BED
    }

    private final UUID catUUID;
    private final Action action;

    public PacketConductCat(UUID catUUID, Action action) {
        this.catUUID = catUUID;
        this.action = action;
    }

    public PacketConductCat(FriendlyByteBuf buf) {
        this.catUUID = buf.readUUID();
        this.action = buf.readEnum(Action.class);
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(catUUID);
        buf.writeEnum(action);
    }

    @Override
    protected void handleServer(ServerPlayer player) {
        ServerLevel serverLevel = player.serverLevel();
        CatDirectory dir = CatDirectory.get(serverLevel);
        CatDirectory.Entry entry = dir.getEntry(catUUID);

        // Find or load cat
        Entity targetEntity = serverLevel.getEntity(catUUID);
        if (targetEntity == null && entry != null && entry.pos != null) {
            // Load chunk where cat was last recorded
            ChunkPos cp = new ChunkPos(entry.pos);
            serverLevel.getChunkSource().addRegionTicket(TicketType.PORTAL, cp, 2, entry.pos);
            targetEntity = serverLevel.getEntity(catUUID);
        }

        if (!(targetEntity instanceof TalentedCat cat)) {
            player.sendSystemMessage(Component.literal("§c[Producer Stick] Conduct rejected: Cat not found or loaded in current dimension! (Code: ERR_CAT_NOT_FOUND)"));
            return;
        }

        if (!cat.isOwnedBy(player) && !cat.isTrusted(player.getUUID())) {
            player.sendSystemMessage(Component.literal("§c[Producer Stick] Conduct rejected: You do not own or have trust with this cat! (Code: ERR_UNAUTHORIZED)"));
            return;
        }

        String playerDim = serverLevel.dimension().location().toString();
        String catDim = cat.level().dimension().location().toString();

        if (!playerDim.equals(catDim)) {
            player.sendSystemMessage(Component.literal("§c[Producer Stick] Conduct rejected: Cat is in another dimension (" + catDim + ")! (Code: ERR_DIFF_DIM)"));
            return;
        }

        if (action == Action.TO_SELF) {
            BlockPos safePos = findSafeSpotAround(serverLevel, player.blockPosition());
            if (safePos == null) {
                player.sendSystemMessage(Component.literal("§c[Producer Stick] Conduct rejected: No safe landing spot found in 9x3x9 area! (Code: ERR_NO_SAFE_SPOT)"));
                return;
            }

            cat.teleportTo(safePos.getX() + 0.5D, safePos.getY(), safePos.getZ() + 0.5D);
            cat.setOrderedToSit(false);
            cat.setTarget(null);
            cat.getNavigation().stop();
            cat.resetFallDistance();

            serverLevel.sendParticles(ParticleTypes.PORTAL, safePos.getX() + 0.5D, safePos.getY() + 0.5D, safePos.getZ() + 0.5D, 40, 0.4D, 0.4D, 0.4D, 0.2D);
            serverLevel.playSound(null, safePos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.2F);
            player.sendSystemMessage(Component.literal("§d[Producer Stick] Conducted §f" + cat.getName().getString() + "§d to your side! (Code: OK)"));
            dir.updateCat(cat);
        } else { // TO_BED
            BlockPos bedPos = cat.getHomeBedPos();
            if (bedPos == null) {
                player.sendSystemMessage(Component.literal("§c[Producer Stick] Conduct rejected: " + cat.getName().getString() + " has no assigned bed! (Code: ERR_NO_BED)"));
                return;
            }

            String bedDim = cat.getHomeBedDimension();
            if (!bedDim.isEmpty() && !bedDim.equals(playerDim)) {
                player.sendSystemMessage(Component.literal("§c[Producer Stick] Conduct rejected: Bed is in another dimension (" + bedDim + ")! (Code: ERR_BED_DIFF_DIM)"));
                return;
            }

            // Ensure chunk with bed is loaded
            ChunkPos bedChunk = new ChunkPos(bedPos);
            serverLevel.getChunkSource().addRegionTicket(TicketType.PORTAL, bedChunk, 2, bedPos);

            BlockState state = serverLevel.getBlockState(bedPos);
            if (!(state.getBlock() instanceof com.kittytalents.block.CatBedBlock) && !state.is(net.minecraft.tags.BlockTags.BEDS)) {
                player.sendSystemMessage(Component.literal("§c[Producer Stick] Conduct rejected: Bed at " + bedPos.getX() + ", " + bedPos.getY() + ", " + bedPos.getZ() + " is missing or obstructed! (Code: ERR_BED_MISSING)"));
                return;
            }

            cat.teleportTo(bedPos.getX() + 0.5D, bedPos.getY() + 0.5D, bedPos.getZ() + 0.5D);
            cat.setOrderedToSit(true);
            cat.setTarget(null);
            cat.getNavigation().stop();
            cat.resetFallDistance();

            serverLevel.sendParticles(ParticleTypes.PORTAL, bedPos.getX() + 0.5D, bedPos.getY() + 0.5D, bedPos.getZ() + 0.5D, 40, 0.4D, 0.4D, 0.4D, 0.2D);
            serverLevel.playSound(null, bedPos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
            player.sendSystemMessage(Component.literal("§d[Producer Stick] Conducted §f" + cat.getName().getString() + "§d back to their bed! (Code: OK)"));
            dir.updateCat(cat);
        }
    }

    private static BlockPos findSafeSpotAround(ServerLevel level, BlockPos origin) {
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;

        // 9x3x9 search: dx: -4..4, dy: -1..1, dz: -4..4
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos p = origin.offset(dx, dy, dz);
                    BlockState ground = level.getBlockState(p.below());
                    BlockState feet = level.getBlockState(p);
                    BlockState head = level.getBlockState(p.above());

                    if (ground.isSolid() && !ground.is(net.minecraft.world.level.block.Blocks.LAVA) && !ground.is(net.minecraft.world.level.block.Blocks.FIRE) && !ground.is(net.minecraft.world.level.block.Blocks.CACTUS)) {
                        if ((feet.isAir() || feet.canBeReplaced()) && (head.isAir() || head.canBeReplaced())) {
                            double dist = origin.distSqr(p);
                            if (dist < bestDist) {
                                bestDist = dist;
                                best = p;
                            }
                        }
                    }
                }
            }
        }
        return best;
    }
}
