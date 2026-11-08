package com.kittytalents.network;

import com.kittytalents.core.perm.CatAction;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.talent.Talent;
import com.kittytalents.talent.Talents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/** Client -> server: spend skill points on one rank of a talent (owner only). */
public class PacketTrainTalent extends PacketBase {
    private final int entityId;
    private final String talentId;

    public PacketTrainTalent(int entityId, String talentId) {
        this.entityId = entityId;
        this.talentId = talentId == null ? "" : talentId;
    }

    public PacketTrainTalent(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.talentId = buf.readUtf(64);
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeUtf(talentId, 64);
    }

    @Override
    protected void handleServer(ServerPlayer sender) {
        apply(sender, entityId, talentId);
    }

    /**
     * @return true if a rank was trained. Unknown talents, maxed talents and missing points are refused, and the
     * sender's talent view is resynced so the screen shows the real state.
     */
    public static boolean apply(ServerPlayer sender, int entityId, String talentId) {
        TalentedCat cat = authorize(sender, entityId, CatAction.TRAIN);
        if (cat == null) return false;
        Talent talent = Talents.get(talentId);
        // Dire talents need a Dire-awakened cat (one Dire Treat, checked in canTrainTalent); ranks cost only skill points.
        boolean trained = talent != null && cat.canTrainTalent(talent) && cat.trainTalent(talent);
        cat.syncTalents(sender);
        return trained;
    }
}
