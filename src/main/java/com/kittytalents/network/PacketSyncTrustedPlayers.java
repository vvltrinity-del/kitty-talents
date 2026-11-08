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

    public PacketSyncTrustedPlayers(int entityId, Map<UUID, String> players) {
        this.entityId = entityId;
        this.players = new LinkedHashMap<>(players);
    }

    public PacketSyncTrustedPlayers(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        int size = buf.readVarInt();
        this.players = new LinkedHashMap<>();
        for (int i = 0; i < size; i++) {
            players.put(buf.readUUID(), buf.readUtf(64));
        }
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeVarInt(players.size());
        players.forEach((id, name) -> {
            buf.writeUUID(id);
            buf.writeUtf(name, 64);
        });
    }

    @Override
    protected void handleClient() {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandlers.handleSyncTrustedPlayers(entityId, players));
    }
}
