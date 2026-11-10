package com.kittytalents.network;

import com.kittytalents.core.CatDirectory;
import com.kittytalents.entity.TalentedCat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class PacketRequestConductList extends PacketBase {
    private final boolean defaultToBed;

    public PacketRequestConductList(boolean defaultToBed) {
        this.defaultToBed = defaultToBed;
    }

    public PacketRequestConductList(FriendlyByteBuf buf) {
        this.defaultToBed = buf.readBoolean();
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(defaultToBed);
    }

    @Override
    protected void handleServer(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        CatDirectory dir = CatDirectory.get(level);
        UUID ownerUUID = player.getUUID();

        // Ensure any loaded cats in current level are registered
        for (TalentedCat cat : level.getEntitiesOfClass(TalentedCat.class, player.getBoundingBox().inflate(128.0D))) {
            if (cat.isOwnedBy(player) || cat.isTrusted(ownerUUID)) {
                dir.updateCat(cat);
            }
        }

        List<CatDirectory.Entry> entries = dir.getCatsForOwner(ownerUUID);
        List<PacketSyncConductList.ConductCatDto> dtos = new ArrayList<>();
        Set<UUID> seen = new HashSet<>();

        for (CatDirectory.Entry e : entries) {
            seen.add(e.catUUID);
            dtos.add(new PacketSyncConductList.ConductCatDto(
                    e.catUUID,
                    e.name,
                    e.dimension,
                    e.pos,
                    e.bedPos,
                    e.bedDimension
            ));
        }

        // Also check if any trusted cat isn't in owner directory
        for (net.minecraft.world.entity.Entity en : level.getAllEntities()) {
            if (en instanceof TalentedCat cat && !seen.contains(cat.getUUID()) && (cat.isOwnedBy(player) || cat.isTrusted(ownerUUID))) {
                dtos.add(new PacketSyncConductList.ConductCatDto(
                        cat.getUUID(),
                        cat.getName().getString(),
                        cat.level().dimension().location().toString(),
                        cat.blockPosition(),
                        cat.getHomeBedPos(),
                        cat.getHomeBedDimension()
                ));
            }
        }

        PacketHandler.sendToPlayer(new PacketSyncConductList(dtos, defaultToBed), player);
    }
}
