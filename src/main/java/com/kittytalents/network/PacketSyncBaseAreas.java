package com.kittytalents.network;

import com.kittytalents.core.base.BaseAreas;
import com.kittytalents.core.base.BasePatrol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/** Server -> client (5.0): the player's base areas with their guard counts; optionally opens the Base Pin menu. */
public class PacketSyncBaseAreas extends PacketBase {
    public record Entry(BaseAreas.Area area, int guards) {}

    private final List<Entry> entries;
    private final boolean open;

    public PacketSyncBaseAreas(List<Entry> entries, boolean open) {
        this.entries = entries;
        this.open = open;
    }

    public PacketSyncBaseAreas(FriendlyByteBuf buf) {
        int n = Math.min(buf.readVarInt(), BaseAreas.MAX_AREAS);
        entries = new ArrayList<>(n);
        for (int i = 0; i < n; i++) entries.add(new Entry(BaseAreas.Area.read(buf), buf.readVarInt()));
        open = buf.readBoolean();
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(entries.size());
        for (Entry e : entries) {
            e.area().write(buf);
            buf.writeVarInt(e.guards());
        }
        buf.writeBoolean(open);
    }

    @Override
    protected void handleClient() {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.kittytalents.client.ClientBaseAreas.receive(entries, open));
    }

    public static void send(ServerPlayer player, boolean open) {
        List<Entry> list = new ArrayList<>();
        for (BaseAreas.Area a : BaseAreas.get(player.server).of(player.getUUID())) {
            ServerLevel level = null;
            for (ServerLevel l : player.server.getAllLevels()) {
                if (l.dimension().location().toString().equals(a.dimension())) level = l;
            }
            list.add(new Entry(a, level == null ? 0 : BasePatrol.guardsIn(level, player.getUUID(), a)));
        }
        PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new PacketSyncBaseAreas(list, open));
    }
}
