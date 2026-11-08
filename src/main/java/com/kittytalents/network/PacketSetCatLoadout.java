package com.kittytalents.network;

import com.kittytalents.core.component.CatLoadout;
import com.kittytalents.entity.TalentedCat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client -> server: change a cat's combat loadout (role, element, skills, ultimate) from the Combat tab.
 * Before this existed the Combat tab only changed the client's copy, so the server never used the choice.
 * Decode -> validate (CatLoadout#validate) -> authorize (owner or trusted, within 16 blocks) -> apply -> sync.
 */
public class PacketSetCatLoadout {
    private final int entityId;
    private final CatLoadout loadout;

    public PacketSetCatLoadout(int entityId, CatLoadout loadout) {
        this.entityId = entityId;
        this.loadout = loadout;
    }

    public PacketSetCatLoadout(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.loadout = CatLoadout.read(buf);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        loadout.write(buf);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> apply(ctx.get().getSender(), entityId, loadout));
        ctx.get().setPacketHandled(true);
    }

    /** Server-side handling. Public for GameTests. @return true if the loadout was applied. */
    public static boolean apply(ServerPlayer sender, int entityId, CatLoadout loadout) {
        if (sender == null || loadout == null) return false;
        Entity e = sender.level().getEntity(entityId);
        if (!(e instanceof TalentedCat cat) || cat.distanceToSqr(sender) > 16.0D * 16.0D) return false;
        if (!cat.isPrimaryOwner(sender) && !cat.isTrusted(sender)) return false;
        if (loadout.validate() != null) {
            cat.syncLoadout(sender); // reset the client's optimistic change to the real state
            return false;
        }
        loadout.applyTo(cat);
        cat.syncLoadout();
        return true;
    }
}
