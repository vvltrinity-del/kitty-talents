package com.kittytalents.network;

import com.kittytalents.core.perm.CatAction;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.skin.CatSkins;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/** Client -> server: previous / next / an explicit skin id ("" = the breed's default look). */
public class PacketChangeCatSkin extends PacketBase {
    public static final String PREV = "<prev>";
    public static final String NEXT = "<next>";
    private static final int MAX_LEN = 256;

    private final int entityId;
    private final String target;

    public PacketChangeCatSkin(int entityId, boolean next) {
        this(entityId, next ? NEXT : PREV);
    }

    public PacketChangeCatSkin(int entityId, String target) {
        this.entityId = entityId;
        this.target = target;
    }

    public PacketChangeCatSkin(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.target = buf.readUtf(MAX_LEN);
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeUtf(target, MAX_LEN);
    }

    @Override
    protected void handleServer(ServerPlayer sender) {
        apply(sender, entityId, target);
    }

    /** @return true if the skin was changed. Ids the server has not loaded are refused. */
    public static boolean apply(ServerPlayer sender, int entityId, String target) {
        if (target == null) return false;
        boolean step = PREV.equals(target) || NEXT.equals(target);
        if (!step && !CatSkins.isValid(target)) return false;
        TalentedCat cat = authorize(sender, entityId, CatAction.STYLE);
        if (cat == null) return false;
        String skin = target;
        if (step) {
            List<String> cycle = new ArrayList<>();
            cycle.add(CatSkins.DEFAULT);
            CatSkins.all().forEach(sk -> cycle.add(sk.id().toString()));
            int i = Math.max(0, cycle.indexOf(cat.getSkin()));
            skin = cycle.get(Math.floorMod(i + (NEXT.equals(target) ? 1 : -1), cycle.size()));
        }
        cat.setSkin(skin);
        return true;
    }
}
