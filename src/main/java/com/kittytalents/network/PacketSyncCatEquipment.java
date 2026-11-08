package com.kittytalents.network;

import com.kittytalents.client.network.ClientPacketHandlers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;


public class PacketSyncCatEquipment extends PacketBase {
    private final int entityId;
    private final ItemStack weapon;
    private final ItemStack tool;
    private final ItemStack helmet;
    private final ItemStack chest;
    private final ItemStack legs;
    private final ItemStack boots;
    private final ItemStack accessory;
    private final ItemStack cosmetic1;
    private final ItemStack cosmetic2;
    private final ItemStack cosmetic3;
    private final boolean cosmeticsVisible;

    public PacketSyncCatEquipment(int entityId, ItemStack weapon, ItemStack tool,
                                 ItemStack helmet, ItemStack chest, ItemStack legs, ItemStack boots,
                                 ItemStack accessory, ItemStack cosmetic1, ItemStack cosmetic2, ItemStack cosmetic3,
                                 boolean cosmeticsVisible) {
        this.entityId = entityId;
        this.weapon = weapon.copy();
        this.tool = tool.copy();
        this.helmet = helmet.copy();
        this.chest = chest.copy();
        this.legs = legs.copy();
        this.boots = boots.copy();
        this.accessory = accessory.copy();
        this.cosmetic1 = cosmetic1.copy();
        this.cosmetic2 = cosmetic2.copy();
        this.cosmetic3 = cosmetic3.copy();
        this.cosmeticsVisible = cosmeticsVisible;
    }

    public PacketSyncCatEquipment(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.weapon = buf.readItem();
        this.tool = buf.readItem();
        this.helmet = buf.readItem();
        this.chest = buf.readItem();
        this.legs = buf.readItem();
        this.boots = buf.readItem();
        this.accessory = buf.readItem();
        this.cosmetic1 = buf.readItem();
        this.cosmetic2 = buf.readItem();
        this.cosmetic3 = buf.readItem();
        this.cosmeticsVisible = buf.readBoolean();
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeItem(weapon);
        buf.writeItem(tool);
        buf.writeItem(helmet);
        buf.writeItem(chest);
        buf.writeItem(legs);
        buf.writeItem(boots);
        buf.writeItem(accessory);
        buf.writeItem(cosmetic1);
        buf.writeItem(cosmetic2);
        buf.writeItem(cosmetic3);
        buf.writeBoolean(cosmeticsVisible);
    }

    @Override
    protected void handleClient() {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandlers.handleSyncCatEquipment(entityId, weapon, tool, helmet, chest, legs, boots, accessory, cosmetic1, cosmetic2, cosmetic3, cosmeticsVisible));
    }
}
