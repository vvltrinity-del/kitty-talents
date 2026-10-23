package com.kittytalents.item;

import com.kittytalents.entity.TalentedCat;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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

public class GenderRibbonItem extends Item {
    public GenderRibbonItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (target instanceof TalentedCat cat && (cat.isOwnedBy(player) || cat.isTrusted(player))) {
            if (!player.level().isClientSide) {
                boolean isCurrentlyFemale = cat.isFemale();
                cat.setFemale(!isCurrentlyFemale);
                String newGender = !isCurrentlyFemale ? "Female (♀)" : "Male (♂)";
                ChatFormatting color = !isCurrentlyFemale ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.AQUA;

                cat.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 1.4F);
                if (cat.level() instanceof ServerLevel serverLevel) {
                    for (int i = 0; i < 8; i++) {
                        serverLevel.sendParticles(ParticleTypes.END_ROD,
                                cat.getX() + (serverLevel.random.nextDouble() - 0.5D),
                                cat.getY() + 0.4D,
                                cat.getZ() + (serverLevel.random.nextDouble() - 0.5D),
                                1, 0, 0.05D, 0, 0.02D);
                    }
                }

                player.displayClientMessage(Component.literal("Assigned " + cat.getName().getString() + "'s gender to: ").withStyle(ChatFormatting.GOLD)
                        .append(Component.literal(newGender).withStyle(color)), true);

                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            }
            return InteractionResult.sidedSuccess(player.level().isClientSide);
        }
        return super.interactLivingEntity(stack, player, target, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Silk ribbon of biological identity.").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.literal("Right-click a Talented Cat to toggle gender (Male ♂ / Female ♀).").withStyle(ChatFormatting.GRAY));
    }
}
