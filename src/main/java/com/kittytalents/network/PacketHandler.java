package com.kittytalents.network;

import com.kittytalents.KittyTalents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Function;

/**
 * The mod's one network channel. Every packet extends {@link PacketBase}; the registration order below is the wire id,
 * so append new packets at the end. Bump {@link #PROTOCOL_VERSION} whenever a packet's format changes.
 */
public class PacketHandler {
    /** 2: Phase 7 (PacketBase port; PacketTwineAction always sends type + data). */
    private static final String PROTOCOL_VERSION = "2";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(KittyTalents.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    public static void register() {
        toServer(PacketTrainTalent.class, PacketTrainTalent::new);
        toServer(PacketChangeCatMode.class, PacketChangeCatMode::new);
        toServer(PacketOpenCatPack.class, PacketOpenCatPack::new);
        toServer(PacketRenameCat.class, PacketRenameCat::new);
        toServer(PacketChangeCatSkin.class, PacketChangeCatSkin::new);
        toServer(PacketRespecTalents.class, PacketRespecTalents::new);
        toServer(PacketEquipCatItem.class, PacketEquipCatItem::new);
        toServer(PacketManageTrust.class, PacketManageTrust::new);
        toClient(PacketSyncTrustedPlayers.class, PacketSyncTrustedPlayers::new);
        toServer(PacketSetCatLoadout.class, PacketSetCatLoadout::new);
        toClient(PacketSyncCatLoadout.class, PacketSyncCatLoadout::new);
        toClient(PacketSyncCatEquipment.class, PacketSyncCatEquipment::new);
        toServer(PacketWhistleCommand.class, PacketWhistleCommand::new);
        toServer(PacketChangeCatGender.class, PacketChangeCatGender::new);
        toServer(PacketChangeCollarColor.class, PacketChangeCollarColor::new);
        toServer(PacketTwineAction.class, PacketTwineAction::new);
        toClient(PacketSyncTwine.class, PacketSyncTwine::new);
        toServer(PacketTwineInput.class, PacketTwineInput::new);
        toClient(PacketSyncTalents.class, PacketSyncTalents::new);
        toServer(PacketTeamCommand.class, PacketTeamCommand::new);
        toServer(PacketSetCatJob.class, PacketSetCatJob::new);
        toServer(PacketRequestConductList.class, PacketRequestConductList::new);
        toClient(PacketSyncConductList.class, PacketSyncConductList::new);
        toServer(PacketConductCat.class, PacketConductCat::new);
    }

    private static <T extends PacketBase> void toServer(Class<T> type, Function<FriendlyByteBuf, T> decoder) {
        add(type, decoder, NetworkDirection.PLAY_TO_SERVER);
    }

    private static <T extends PacketBase> void toClient(Class<T> type, Function<FriendlyByteBuf, T> decoder) {
        add(type, decoder, NetworkDirection.PLAY_TO_CLIENT);
    }

    private static <T extends PacketBase> void add(Class<T> type, Function<FriendlyByteBuf, T> decoder, NetworkDirection dir) {
        INSTANCE.messageBuilder(type, packetId++, dir)
                .decoder(decoder)
                .encoder(PacketBase::toBytes)
                .consumerMainThread(PacketBase::handle)
                .add();
    }

    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        INSTANCE.sendTo(message, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }
}
