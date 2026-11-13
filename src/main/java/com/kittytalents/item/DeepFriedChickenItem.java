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
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import java.util.List;

/**
 * Cooked meals players and Talented Cats can both eat, and that go into Food Bowls.
 * 5.0: Deep Fried Chicken is a mid-tier meal (8 nutrition, +27 cat hunger); the top meal is Chicken and Rice
 * (Deep Fried Chicken + Steamed Rice Bowl, 15 nutrition, +50 cat hunger), so the best food needs the Rice Mill line.
 */
public class DeepFriedChickenItem extends Item {
    /** Values per meal: player food, cat hunger/saturation/health, whether a bowl comes back, tooltip line. */
    public record Meal(FoodProperties food, int catHunger, float catSaturation, float catHeal, boolean bowl, String flavor) {}

    public static final Meal CHICKEN = new Meal(new FoodProperties.Builder().nutrition(8).saturationMod(0.6F).meat().alwaysEat().build(),
            27, 8.0F, 7.0F, false, "Crispy, golden-brown deep-fried drumstick.");
    public static final Meal CHICKEN_AND_RICE = new Meal(new FoodProperties.Builder().nutrition(15).saturationMod(0.5F).meat().alwaysEat().build(),
            50, 15.0F, 12.0F, true, "Deep fried chicken on steamed rice. The best meal a cat can get.");

    /** Kept for callers that read the player food of Deep Fried Chicken. */
    public static final FoodProperties FOOD = CHICKEN.food();

    private final Meal meal;

    public DeepFriedChickenItem(Properties properties) {
        this(properties, CHICKEN);
    }

    public DeepFriedChickenItem(Properties properties, Meal meal) {
        super((meal.bowl() ? properties.stacksTo(16) : properties).food(meal.food()).rarity(meal.bowl() ? Rarity.EPIC : Rarity.RARE));
        this.meal = meal;
    }

    public Meal meal() { return meal; }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity eater) {
        ItemStack rest = super.finishUsingItem(stack, level, eater);
        if (!meal.bowl()) return rest;
        if (eater instanceof Player p && p.getAbilities().instabuild) return rest;
        if (rest.isEmpty()) return new ItemStack(Items.BOWL);
        if (eater instanceof Player p && !p.getInventory().add(new ItemStack(Items.BOWL))) p.drop(new ItemStack(Items.BOWL), false);
        return rest;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (target instanceof TalentedCat cat && (cat.isOwnedBy(player) || cat.isTrusted(player.getUUID()))) {
            if (!cat.isIncapacitated()) {
                if (cat.getHunger() < 120 || cat.getHealth() < cat.getMaxHealth()) {
                    if (!player.level().isClientSide) {
                        String mealName = stack.getHoverName().getString();
                        if (!player.getAbilities().instabuild) {
                            stack.shrink(1);
                            if (meal.bowl() && !player.getInventory().add(new ItemStack(Items.BOWL))) player.drop(new ItemStack(Items.BOWL), false);
                        }
                        cat.applyFood(meal.catHunger(), meal.catSaturation(), meal.catHeal());
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

                        player.displayClientMessage(Component.literal("Fed " + cat.getName().getString() + " " + mealName
                                + "! (+" + meal.catHunger() + " Hunger, +" + (int) meal.catHeal() + " HP)").withStyle(ChatFormatting.GOLD), true);
                    }
                    return InteractionResult.sidedSuccess(player.level().isClientSide);
                }
            }
        }
        return super.interactLivingEntity(stack, player, target, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(meal.flavor()).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("Nutrition: " + meal.food().getNutrition() + " | Cat hunger: +" + meal.catHunger()).withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("For players, Talented Cats and Food Bowls.").withStyle(ChatFormatting.GREEN));
    }
}
