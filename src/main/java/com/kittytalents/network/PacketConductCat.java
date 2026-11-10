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

        Entity targetEntity = serverLevel.getEntity(catUUID);
        if (targetEntity == null) {
            // Only the owner may wake up an unloaded cat's area (a co-owner can't be verified until it loads),
            // and the check happens BEFORE any chunk is loaded.
            if (entry == null || entry.pos == null || !player.getUUID().equals(entry.ownerUUID)) {
                player.sendSystemMessage(Component.literal("§c[Producer Stick] Conduct rejected: Cat not found or loaded in current dimension! (Code: ERR_CAT_NOT_FOUND)"));
                return;
            }
            if (!serverLevel.dimension().location().toString().equals(entry.dimension)) {
                player.sendSystemMessage(Component.literal("§c[Producer Stick] Conduct rejected: Cat is in another dimension (" + entry.dimension + ")! (Code: ERR_DIFF_DIM)"));
                return;
            }
            // Entities load a few ticks after their chunk: load the area and retry for up to 2 seconds.
            serverLevel.getChunkSource().addRegionTicket(TicketType.PORTAL, new ChunkPos(entry.pos), 2, entry.pos);
            Pending.add(player, catUUID, action, serverLevel.getGameTime() + 40);
            player.sendSystemMessage(Component.literal("§7[Producer Stick] Calling " + entry.name + " from afar..."));
            return;
        }
        if (!(targetEntity instanceof TalentedCat cat)) {
            player.sendSystemMessage(Component.literal("§c[Producer Stick] Conduct rejected: Cat not found or loaded in current dimension! (Code: ERR_CAT_NOT_FOUND)"));
            return;
        }
        conduct(player, cat, action);
    }

    /** Teleports a loaded cat after the permission and safety checks. */
    static void conduct(ServerPlayer player, TalentedCat cat, Action action) {
        ServerLevel serverLevel = player.serverLevel();
        CatDirectory dir = CatDirectory.get(serverLevel);
        if (!com.kittytalents.core.perm.CatPermissions.can(player, cat, com.kittytalents.core.perm.CatAction.COMMAND)) {
            player.sendSystemMessage(Component.literal("§c[Producer Stick] Conduct rejected: You do not own or have trust with this cat! (Code: ERR_UNAUTHORIZED)"));
            return;
        }
        if (cat.isTwined() || cat.isPassenger() || cat.isVehicle()) {
            player.sendSystemMessage(Component.literal("§c[Producer Stick] Conduct rejected: " + cat.getName().getString() + " is being twined or ridden! (Code: ERR_BUSY)"));
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

    /** Conduct requests waiting for a far-away cat's area to finish loading. */
    @net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = com.kittytalents.KittyTalents.MODID)
    static final class Pending {
        private record Req(UUID player, UUID cat, Action action, long deadline) {}
        private static final java.util.List<Req> QUEUE = new java.util.ArrayList<>();

        static void add(ServerPlayer player, UUID cat, Action action, long deadline) {
            QUEUE.removeIf(r -> r.player().equals(player.getUUID()) && r.cat().equals(cat));
            QUEUE.add(new Req(player.getUUID(), cat, action, deadline));
        }

        @net.minecraftforge.eventbus.api.SubscribeEvent
        public static void onServerTick(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
            if (event.phase != net.minecraftforge.event.TickEvent.Phase.END || QUEUE.isEmpty()) return;
            var server = event.getServer();
            java.util.Iterator<Req> it = QUEUE.iterator();
            while (it.hasNext()) {
                Req r = it.next();
                ServerPlayer player = server.getPlayerList().getPlayer(r.player());
                if (player == null) { it.remove(); continue; }
                ServerLevel level = player.serverLevel();
                if (level.getEntity(r.cat()) instanceof TalentedCat cat) {
                    it.remove();
                    conduct(player, cat, r.action());
                } else if (level.getGameTime() > r.deadline()) {
                    it.remove();
                    player.sendSystemMessage(Component.literal("§c[Producer Stick] Conduct rejected: Cat not found or loaded in current dimension! (Code: ERR_CAT_NOT_FOUND)"));
                }
            }
        }
    }
}
