package com.kittytalents.network;

import net.minecraft.network.FriendlyByteBuf;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server -> every client: the Twine sessions running right now, as player -> cat. Clients skip drawing those players'
 * dormant bodies completely (the vanilla invisibility flag alone still shows armor and held items) and can mark the
 * cat and the resting place for other players (client/twine/TwineMarkers).
 */
public class PacketSyncTwineBodies extends PacketBase {
    /** Client copies (read by client/twine). */
    public static final Set<UUID> CLIENT_HIDDEN = ConcurrentHashMap.newKeySet();
    public static final Map<UUID, UUID> CLIENT_CAT_TO_PLAYER = new ConcurrentHashMap<>();

    private final Map<UUID, UUID> sessions;

    public PacketSyncTwineBodies(Map<UUID, UUID> playerToCat) {
        this.sessions = new HashMap<>(playerToCat);
    }

    public PacketSyncTwineBodies(FriendlyByteBuf buf) {
        this.sessions = buf.readMap(FriendlyByteBuf::readUUID, FriendlyByteBuf::readUUID);
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeMap(sessions, FriendlyByteBuf::writeUUID, FriendlyByteBuf::writeUUID);
    }

    @Override
    protected void handleClient() {
        CLIENT_HIDDEN.clear();
        CLIENT_HIDDEN.addAll(sessions.keySet());
        CLIENT_CAT_TO_PLAYER.clear();
        sessions.forEach((player, cat) -> CLIENT_CAT_TO_PLAYER.put(cat, player));
    }
}
