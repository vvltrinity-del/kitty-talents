package com.kittytalents.network;

import com.kittytalents.entity.CatMode;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.work.CatJob;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketSetCatJob {
    private final int catEntityId;
    private final int jobOrdinal;
    private final boolean clockedIn;

    public PacketSetCatJob(int catEntityId, CatJob job, boolean clockedIn) {
        this.catEntityId = catEntityId;
        this.jobOrdinal = job != null ? job.ordinal() : 0;
        this.clockedIn = clockedIn;
    }

    public PacketSetCatJob(FriendlyByteBuf buf) {
        this.catEntityId = buf.readInt();
        this.jobOrdinal = buf.readVarInt();
        this.clockedIn = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(catEntityId);
        buf.writeVarInt(jobOrdinal);
        buf.writeBoolean(clockedIn);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            Entity entity = player.level().getEntity(catEntityId);
            if (entity instanceof TalentedCat cat && (cat.isOwnedBy(player) || cat.isTrusted(player))) {
                CatJob job = CatJob.byOrdinal(jobOrdinal);
                cat.setCatJob(job);
                cat.setClockedIn(clockedIn);
                if (job != CatJob.NONE && clockedIn) {
                    cat.setCatMode(CatMode.WORK);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
