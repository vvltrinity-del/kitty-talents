package com.kittytalents.item;

import com.kittytalents.entity.TalentedCat;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GroomingShearsItem extends Item {
    public GroomingShearsItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (target instanceof TalentedCat cat && com.kittytalents.core.perm.CatPermissions.can(player, cat, com.kittytalents.core.perm.CatAction.STYLE)) {
            if (!player.level().isClientSide) {
                // Safely return removed items to owner
                // Collars are consumed on use like dyes, so they are not refunded.
                // Safely return removed accessories to owner
                ItemStack droppedAcc = cat.getEquippedAccessory();
                if (!droppedAcc.isEmpty()) {
                    if (!player.getInventory().add(droppedAcc.copy())) {
                        player.drop(droppedAcc.copy(), false);
                    }
                }

                cat.setCollarColor(DyeColor.RED);
                cat.setEquippedAccessory(ItemStack.EMPTY);
                cat.playSound(SoundEvents.SHEEP_SHEAR, 1.0F, 1.0F);
                player.displayClientMessage(Component.literal("Groomed " + cat.getName().getString() + " and retrieved equipment.").withStyle(ChatFormatting.YELLOW), true);
            }
            return InteractionResult.sidedSuccess(player.level().isClientSide);
        }
        return super.interactLivingEntity(stack, player, target, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Used to safely unequip accessories and groom cats.").withStyle(ChatFormatting.GRAY));
    }
}
