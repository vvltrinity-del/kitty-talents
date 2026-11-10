package com.kittytalents.item;

import com.kittytalents.network.PacketHandler;
import com.kittytalents.network.PacketRequestConductList;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ProducerStickItem extends Item {
    public ProducerStickItem(Properties properties) {
        super(properties.fireResistant().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (level.isClientSide) {
            boolean defaultToBed = player.isShiftKeyDown();
            PacketHandler.sendToServer(new PacketRequestConductList(defaultToBed));
        }
        return InteractionResultHolder.sidedSuccess(held, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("item.kittytalents.producer_stick.desc1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.kittytalents.producer_stick.desc2").withStyle(ChatFormatting.DARK_PURPLE));
    }
}
