package com.kittytalents.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CatAccessoryItem extends Item {
    public enum AccessoryType {
        HAT("Hat"),
        BOW("Bow / Collar Charm"),
        CAPE("Cape / Cloak"),
        GLASSES("Goggles / Glasses");

        private final String displayName;

        AccessoryType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private final AccessoryType accessoryType;

    public CatAccessoryItem(Properties properties, AccessoryType type) {
        super(properties);
        this.accessoryType = type;
    }

    /** 4.0: the item shows its 3D accessory model (client/renderer/AccessoryItemRenderer). */
    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return com.kittytalents.client.renderer.AccessoryItemRenderer.get();
            }
        });
    }

    public AccessoryType getAccessoryType() {
        return accessoryType;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Slot: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(accessoryType.getDisplayName()).withStyle(ChatFormatting.GOLD)));
        tooltip.add(Component.literal("Can be equipped on Talented Cats via the companion menu.").withStyle(ChatFormatting.DARK_GRAY));
    }
}
