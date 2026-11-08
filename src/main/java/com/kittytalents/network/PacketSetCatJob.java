package com.kittytalents.network;

import com.kittytalents.core.perm.CatAction;
import com.kittytalents.entity.CatMode;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.work.CatJob;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/** Client -> server: set a cat's job and clock-in state. */
public class PacketSetCatJob extends PacketBase {
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

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(catEntityId);
        buf.writeVarInt(jobOrdinal);
        buf.writeBoolean(clockedIn);
    }

    @Override
    protected void handleServer(ServerPlayer sender) {
        apply(sender, catEntityId, jobOrdinal, clockedIn);
    }

    /** @return true if applied. Unknown job ordinals are rejected (they used to silently become NONE). */
    public static boolean apply(ServerPlayer sender, int catEntityId, int jobOrdinal, boolean clockedIn) {
        if (jobOrdinal < 0 || jobOrdinal >= CatJob.values().length) return false;
        TalentedCat cat = authorize(sender, catEntityId, CatAction.SET_JOB);
        if (cat == null) return false;
        CatJob job = CatJob.values()[jobOrdinal];
        cat.setCatJob(job);
        cat.setClockedIn(clockedIn);
        if (job != CatJob.NONE && clockedIn) cat.setCatMode(CatMode.WORK);
        return true;
    }
}
