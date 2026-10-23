package com.kittytalents.item;

import com.kittytalents.entity.TalentedCat;
import com.kittytalents.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class EmptyLocatorOrbItem extends Item {
    public EmptyLocatorOrbItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (target instanceof TalentedCat cat && cat.isOwnedBy(player)) {
            if (!player.level().isClientSide) {
                ItemStack boundOrb = new ItemStack(ModItems.FELINE_LOCATOR_ORB.get());
                FelineLocatorOrbItem.bindCat(boundOrb, cat);

                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }

                if (!player.getInventory().add(boundOrb)) {
                    player.drop(boundOrb, false);
                }

                player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F, 1.2F);
                player.displayClientMessage(Component.literal("Bound Locator Orb to " + cat.getName().getString() + "!").withStyle(ChatFormatting.LIGHT_PURPLE), true);
            }
            return InteractionResult.sidedSuccess(player.level().isClientSide);
        }
        return super.interactLivingEntity(stack, player, target, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Right-click a Talented Cat to bind a locator tracker.").withStyle(ChatFormatting.GRAY));
    }
}
