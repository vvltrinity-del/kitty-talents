package com.kittytalents.network;

import com.kittytalents.client.network.ClientPacketHandlers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class PacketSyncTrustedPlayers extends PacketBase {
    private final int entityId;
    private final Map<UUID, String> players;
    private final Map<UUID, com.kittytalents.core.perm.TrustRole> roles = new LinkedHashMap<>();

    public PacketSyncTrustedPlayers(int entityId, Map<UUID, String> players, Map<UUID, com.kittytalents.core.perm.TrustRole> roles) {
        this.entityId = entityId;
        this.players = new LinkedHashMap<>(players);
        this.roles.putAll(roles);
    }

    public PacketSyncTrustedPlayers(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        int size = buf.readVarInt();
        this.players = new LinkedHashMap<>();
        for (int i = 0; i < size; i++) {
            UUID id = buf.readUUID();
            players.put(id, buf.readUtf(64));
            roles.put(id, buf.readEnum(com.kittytalents.core.perm.TrustRole.class));
        }
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeVarInt(players.size());
        players.forEach((id, name) -> {
            buf.writeUUID(id);
            buf.writeUtf(name, 64);
            buf.writeEnum(roles.getOrDefault(id, com.kittytalents.core.perm.TrustRole.FULL));
        });
    }

    @Override
    protected void handleClient() {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandlers.handleSyncTrustedPlayers(entityId, players, roles));
    }
}
