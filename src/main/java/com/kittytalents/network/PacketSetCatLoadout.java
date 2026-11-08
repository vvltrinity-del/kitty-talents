package com.kittytalents.network;

import com.kittytalents.core.component.CatLoadout;
import com.kittytalents.core.perm.CatAction;
import com.kittytalents.core.perm.CatPermissions;
import com.kittytalents.entity.TalentedCat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/**
 * Client -> server: change a cat's combat loadout (role, element, skills, ultimate) from the Combat tab.
 * Decode -> validate (CatLoadout#validate) -> authorize (LOADOUT) -> apply -> sync.
 */
public class PacketSetCatLoadout extends PacketBase {
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

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        loadout.write(buf);
    }

    @Override
    protected void handleServer(ServerPlayer sender) {
        apply(sender, entityId, loadout);
    }

    /** Server-side handling. Public for GameTests. @return true if the loadout was applied. */
    public static boolean apply(ServerPlayer sender, int entityId, CatLoadout loadout) {
        if (sender == null || loadout == null) return false;
        CatPermissions.Access access = CatPermissions.check(sender, entityId, CatAction.LOADOUT);
        if (!access.allowed()) {
            explain(sender, access, CatAction.LOADOUT);
            if (access.cat() != null) access.cat().syncLoadout(sender); // undo any optimistic client change
            return false;
        }
        TalentedCat cat = access.cat();
        if (loadout.validate() != null) {
            cat.syncLoadout(sender); // reset the client's optimistic change to the real state
            return false;
        }
        loadout.applyTo(cat);
        cat.syncLoadout();
        return true;
    }
}
