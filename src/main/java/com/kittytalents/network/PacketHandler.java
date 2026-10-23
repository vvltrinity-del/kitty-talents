package com.kittytalents.network;

import com.kittytalents.KittyTalents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class PacketHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(KittyTalents.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    public static void register() {
        INSTANCE.messageBuilder(PacketTrainTalent.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(PacketTrainTalent::new)
                .encoder(PacketTrainTalent::toBytes)
                .consumerMainThread(PacketTrainTalent::handle)
                .add();

        INSTANCE.messageBuilder(PacketChangeCatMode.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(PacketChangeCatMode::new)
                .encoder(PacketChangeCatMode::toBytes)
                .consumerMainThread(PacketChangeCatMode::handle)
                .add();

        INSTANCE.messageBuilder(PacketOpenCatPack.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(PacketOpenCatPack::new)
                .encoder(PacketOpenCatPack::toBytes)
                .consumerMainThread(PacketOpenCatPack::handle)
                .add();

        INSTANCE.messageBuilder(PacketRenameCat.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(PacketRenameCat::new)
                .encoder(PacketRenameCat::toBytes)
                .consumerMainThread(PacketRenameCat::handle)
                .add();

        INSTANCE.messageBuilder(PacketChangeCatSkin.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(PacketChangeCatSkin::new)
                .encoder(PacketChangeCatSkin::toBytes)
                .consumerMainThread(PacketChangeCatSkin::handle)
                .add();

        INSTANCE.messageBuilder(PacketRespecTalents.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(PacketRespecTalents::new)
                .encoder(PacketRespecTalents::toBytes)
                .consumerMainThread(PacketRespecTalents::handle)
                .add();

        INSTANCE.messageBuilder(PacketEquipCatItem.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(PacketEquipCatItem::new)
                .encoder(PacketEquipCatItem::toBytes)
                .consumerMainThread(PacketEquipCatItem::handle)
                .add();

        INSTANCE.messageBuilder(PacketManageTrust.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(PacketManageTrust::new)
                .encoder(PacketManageTrust::toBytes)
                .consumerMainThread(PacketManageTrust::handle)
                .add();

        INSTANCE.messageBuilder(PacketSyncTrustedPlayers.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(PacketSyncTrustedPlayers::new)
                .encoder(PacketSyncTrustedPlayers::toBytes)
                .consumerMainThread(PacketSyncTrustedPlayers::handle)
                .add();

        INSTANCE.messageBuilder(PacketSyncCatEquipment.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(PacketSyncCatEquipment::new)
                .encoder(PacketSyncCatEquipment::toBytes)
                .consumerMainThread(PacketSyncCatEquipment::handle)
                .add();

        INSTANCE.messageBuilder(PacketWhistleCommand.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(PacketWhistleCommand::new)
                .encoder(PacketWhistleCommand::toBytes)
                .consumerMainThread(PacketWhistleCommand::handle)
                .add();

        INSTANCE.messageBuilder(PacketChangeCatGender.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(PacketChangeCatGender::new)
                .encoder(PacketChangeCatGender::toBytes)
                .consumerMainThread(PacketChangeCatGender::handle)
                .add();

        INSTANCE.messageBuilder(PacketChangeCollarColor.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(PacketChangeCollarColor::new)
                .encoder(PacketChangeCollarColor::toBytes)
                .consumerMainThread(PacketChangeCollarColor::handle)
                .add();

        INSTANCE.messageBuilder(PacketTwineAction.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(PacketTwineAction::new)
                .encoder(PacketTwineAction::toBytes)
                .consumerMainThread(PacketTwineAction::handle)
                .add();

        INSTANCE.messageBuilder(PacketSyncTwine.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(PacketSyncTwine::new)
                .encoder(PacketSyncTwine::toBytes)
                .consumerMainThread(PacketSyncTwine::handle)
                .add();

        INSTANCE.messageBuilder(PacketTwineInput.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(PacketTwineInput::new)
                .encoder(PacketTwineInput::toBytes)
                .consumerMainThread(PacketTwineInput::handle)
                .add();

        INSTANCE.messageBuilder(PacketSyncTalents.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(PacketSyncTalents::new)
                .encoder(PacketSyncTalents::toBytes)
                .consumerMainThread(PacketSyncTalents::handle)
                .add();

        INSTANCE.messageBuilder(PacketTeamCommand.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(PacketTeamCommand::new)
                .encoder(PacketTeamCommand::toBytes)
                .consumerMainThread(PacketTeamCommand::handle)
                .add();

        INSTANCE.messageBuilder(PacketSetCatJob.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(PacketSetCatJob::new)
                .encoder(PacketSetCatJob::toBytes)
                .consumerMainThread(PacketSetCatJob::handle)
                .add();
    }

    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        INSTANCE.sendTo(message, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }
}
