package com.kittytalents.network;

import net.minecraft.network.FriendlyByteBuf;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server -> every client: which players' bodies are dormant (Twined) right now. Clients skip drawing those players
 * completely; the vanilla invisibility flag alone still shows armor and held items.
 */
public class PacketSyncTwineBodies extends PacketBase {
    /** Client copy of the dormant set (read by client/twine/TwineBodyHider). */
    public static final Set<UUID> CLIENT_HIDDEN = ConcurrentHashMap.newKeySet();

    private final Set<UUID> hidden;

    public PacketSyncTwineBodies(Collection<UUID> hidden) {
        this.hidden = new HashSet<>(hidden);
    }

    public PacketSyncTwineBodies(FriendlyByteBuf buf) {
        this.hidden = buf.readCollection(HashSet::new, FriendlyByteBuf::readUUID);
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeCollection(hidden, FriendlyByteBuf::writeUUID);
    }

    @Override
    protected void handleClient() {
        CLIENT_HIDDEN.clear();
        CLIENT_HIDDEN.addAll(hidden);
    }
}
