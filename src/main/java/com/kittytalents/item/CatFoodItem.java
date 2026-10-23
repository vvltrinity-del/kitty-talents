package com.kittytalents.item;

import com.kittytalents.entity.TalentedCat;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CatFoodItem extends Item {
    private final int catHungerRestore;
    private final float catHealthRestore;
    private final String foodDesc;

    public CatFoodItem(Properties properties, int catHunger, float catHealth, String desc) {
        super(properties.food(new FoodProperties.Builder().nutrition(catHunger / 5).saturationMod(0.6F).build()));
        this.catHungerRestore = catHunger;
        this.catHealthRestore = catHealth;
        this.foodDesc = desc;
    }

    public int getHungerRestored() {
        return catHungerRestore;
    }

    public float getHealAmount() {
        return catHealthRestore;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (target instanceof TalentedCat cat && cat.isOwnedBy(player) && !cat.isIncapacitated()) {
            if (cat.getHunger() < 120 || cat.getHealth() < cat.getMaxHealth()) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                cat.increaseHunger(catHungerRestore);
                cat.heal(catHealthRestore);
                cat.playSound(SoundEvents.CAT_EAT, 1.0F, 1.0F);
                cat.level().addParticle(ParticleTypes.HAPPY_VILLAGER, cat.getX(), cat.getY() + 0.5D, cat.getZ(), 0, 0.1D, 0);
                return InteractionResult.sidedSuccess(player.level().isClientSide);
            }
        }
        return super.interactLivingEntity(stack, player, target, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Restores +" + catHungerRestore + " Cat Hunger | +" + (int) catHealthRestore + " HP").withStyle(ChatFormatting.GREEN));
        if (foodDesc != null && !foodDesc.isEmpty()) {
            tooltip.add(Component.literal(foodDesc).withStyle(ChatFormatting.GRAY));
        }
    }
}
