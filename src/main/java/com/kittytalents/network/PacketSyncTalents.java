package com.kittytalents.network;

import com.kittytalents.client.network.ClientPacketHandlers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import java.util.HashMap;
import java.util.Map;

public class PacketSyncTalents extends PacketBase {
    private final int entityId;
    private final int skillPoints;
    private final Map<String, Integer> talentMap;

    public PacketSyncTalents(int entityId, int skillPoints, Map<String, Integer> talentMap) {
        this.entityId = entityId;
        this.skillPoints = skillPoints;
        this.talentMap = talentMap != null ? talentMap : new HashMap<>();
    }

    public PacketSyncTalents(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.skillPoints = buf.readInt();
        int size = buf.readInt();
        this.talentMap = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            String key = buf.readUtf(64);
            int lvl = buf.readInt();
            this.talentMap.put(key, lvl);
        }
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeInt(skillPoints);
        buf.writeInt(talentMap.size());
        for (Map.Entry<String, Integer> entry : talentMap.entrySet()) {
            buf.writeUtf(entry.getKey());
            buf.writeInt(entry.getValue());
        }
    }

    @Override
    protected void handleClient() {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandlers.handleSyncTalents(entityId, skillPoints, talentMap));
    }
}
