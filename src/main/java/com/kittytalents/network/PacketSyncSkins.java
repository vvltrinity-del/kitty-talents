package com.kittytalents.network;

import com.kittytalents.skin.CatSkins;
import net.minecraft.network.FriendlyByteBuf;

import java.util.List;

/** Server -> client: the server's skin list (datapack skins), sent on join and after /reload. */
public class PacketSyncSkins extends PacketBase {
    private final List<CatSkins.Skin> skins;

    public PacketSyncSkins() {
        this.skins = CatSkins.all();
    }

    public PacketSyncSkins(FriendlyByteBuf buf) {
        this.skins = CatSkins.read(buf);
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeCollection(skins, (b, s) -> s.write(b));
    }

    @Override
    protected void handleClient() {
        CatSkins.set(skins);
    }
}
