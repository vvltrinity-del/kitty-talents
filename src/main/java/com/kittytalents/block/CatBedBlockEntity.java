package com.kittytalents.block;

import com.kittytalents.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class CatBedBlockEntity extends BlockEntity {
    private DyeColor color = DyeColor.RED;
    @Nullable
    private UUID ownerUUID;
    @Nullable
    private UUID linkedCatUUID;

    public CatBedBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CAT_BED.get(), pos, state);
    }

    public DyeColor getColor() {
        return color;
    }

    public void setColor(DyeColor color) {
        this.color = color;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Nullable
    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public void setOwnerUUID(@Nullable UUID ownerUUID) {
        this.ownerUUID = ownerUUID;
        setChanged();
    }

    @Nullable
    public UUID getLinkedCatUUID() {
        return linkedCatUUID;
    }

    public void setLinkedCatUUID(@Nullable UUID linkedCatUUID) {
        this.linkedCatUUID = linkedCatUUID;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("Color", color.getName());
        if (ownerUUID != null) {
            tag.putUUID("OwnerUUID", ownerUUID);
        }
        if (linkedCatUUID != null) {
            tag.putUUID("LinkedCatUUID", linkedCatUUID);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Color")) {
            this.color = DyeColor.byName(tag.getString("Color"), DyeColor.RED);
        }
        if (tag.hasUUID("OwnerUUID")) {
            this.ownerUUID = tag.getUUID("OwnerUUID");
        }
        if (tag.hasUUID("LinkedCatUUID")) {
            this.linkedCatUUID = tag.getUUID("LinkedCatUUID");
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
