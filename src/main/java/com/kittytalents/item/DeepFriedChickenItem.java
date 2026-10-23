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
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Deep Fried Chicken — Signature high-tier culinary item in Kitty Talents 3.0.
 * Restores 15 Nutrition and 15 Saturation for players.
 * Can be directly fed to Talented Cats (+50 Hunger, +15 Saturation, +12 HP) and placed into Food Bowls.
 */
public class DeepFriedChickenItem extends Item {
    public static final FoodProperties FOOD = new FoodProperties.Builder()
            .nutrition(15)
            .saturationMod(0.5F) // 15 * 0.5 * 2.0 = 15.0 Saturation
            .meat()
            .alwaysEat()
            .build();

    public DeepFriedChickenItem(Properties properties) {
        super(properties.food(FOOD).rarity(Rarity.RARE));
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (target instanceof TalentedCat cat && (cat.isOwnedBy(player) || cat.isTrusted(player.getUUID()))) {
            if (!cat.isIncapacitated()) {
                if (cat.getHunger() < 120 || cat.getHealth() < cat.getMaxHealth()) {
                    if (!player.level().isClientSide) {
                        if (!player.getAbilities().instabuild) {
                            stack.shrink(1);
                        }
                        // Restores hunger, saturation and health
                        cat.applyFood(50, 15.0F, 12.0F);
                        cat.playSound(SoundEvents.CAT_EAT, 1.0F, 1.0F);
                        cat.playSound(SoundEvents.CAT_PURREOW, 0.9F, 1.2F);

                        if (cat.level() instanceof ServerLevel serverLevel) {
                            for (int i = 0; i < 6; i++) {
                                serverLevel.sendParticles(ParticleTypes.HEART,
                                        cat.getX() + (serverLevel.random.nextDouble() - 0.5D) * 0.5D,
                                        cat.getY() + 0.5D + (serverLevel.random.nextDouble() - 0.5D) * 0.3D,
                                        cat.getZ() + (serverLevel.random.nextDouble() - 0.5D) * 0.5D,
                                        1, 0, 0, 0, 0);
                            }
                        }

                        player.displayClientMessage(Component.literal("Fed " + cat.getName().getString() + " delicious Deep Fried Chicken! (+50 Hunger, +15 Saturation, +12 HP)").withStyle(ChatFormatting.GOLD), true);
                    }
                    return InteractionResult.sidedSuccess(player.level().isClientSide);
                }
            }
        }
        return super.interactLivingEntity(stack, player, target, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Crispy, golden-brown deep-fried drumstick.").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("Nutrition: 15 | Saturation: 15.0").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("Fully usable by Players, Talented Cats, and in Food Bowls.").withStyle(ChatFormatting.GREEN));
    }
}
