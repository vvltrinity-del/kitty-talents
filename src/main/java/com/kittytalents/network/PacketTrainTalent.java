package com.kittytalents.network;

import com.kittytalents.entity.TalentedCat;
import com.kittytalents.registry.ModItems;
import com.kittytalents.talent.Talent;
import com.kittytalents.talent.Talents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketTrainTalent {
    private final int entityId;
    private final String talentId;

    public PacketTrainTalent(int entityId, String talentId) {
        this.entityId = entityId;
        this.talentId = talentId;
    }

    public PacketTrainTalent(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.talentId = buf.readUtf(64);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeUtf(talentId);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null) {
                Entity entity = sender.level().getEntity(entityId);
                if (entity instanceof TalentedCat cat && cat.isOwnedBy(sender) && cat.distanceToSqr(sender) <= 64.0D * 64.0D) {
                    Talent talent = Talents.get(talentId);
                    if (talent != null && cat.canTrainTalent(talent)) {
                        // Dire talents need a Dire-awakened cat (one Dire Treat, checked in canTrainTalent); ranks cost
                        // only skill points. Previously every Dire rank also consumed a Dire Treat (26 Nether Stars to max).
                        cat.trainTalent(talent);
                        cat.syncTalents(sender);
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
