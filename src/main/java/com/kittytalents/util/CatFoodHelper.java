package com.kittytalents.util;

import com.kittytalents.KittyTalents;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.item.CatFoodItem;
import com.kittytalents.registry.ModItems;
import com.kittytalents.talent.Talents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class CatFoodHelper {
    public static final TagKey<Item> CAT_FOOD = TagKey.create(Registries.ITEM, new ResourceLocation(KittyTalents.MODID, "cat_food"));
    public static final TagKey<Item> CAT_FISH = TagKey.create(Registries.ITEM, new ResourceLocation(KittyTalents.MODID, "cat_fish"));
    public static final TagKey<Item> CAT_MEAT = TagKey.create(Registries.ITEM, new ResourceLocation(KittyTalents.MODID, "cat_meat"));
    public static final TagKey<Item> CAT_PREMIUM_FOOD = TagKey.create(Registries.ITEM, new ResourceLocation(KittyTalents.MODID, "cat_premium_food"));

    public static class CatFoodValue {
        public final int hunger;
        public final float saturation;
        public final float heal;

        public CatFoodValue(int hunger, float saturation, float heal) {
            this.hunger = hunger;
            this.saturation = saturation;
            this.heal = heal;
        }
    }

    public static boolean isEdibleByCat(ItemStack stack, TalentedCat cat) {
        if (stack.isEmpty()) return false;

        // Custom mod foods
        if (stack.getItem() instanceof CatFoodItem) return true;
        if (stack.is(ModItems.DEEP_FRIED_CHICKEN.get())) return true;
        if (stack.is(Items.GOLDEN_CARROT)) return true;

        // Tag checks
        if (stack.is(CAT_PREMIUM_FOOD)) return true;
        if (stack.is(CAT_FISH)) return true;
        if (stack.is(CAT_FOOD)) return true;

        // Vanilla fish
        if (stack.is(Items.COD) || stack.is(Items.COOKED_COD) ||
            stack.is(Items.SALMON) || stack.is(Items.COOKED_SALMON) ||
            stack.is(Items.TROPICAL_FISH) || stack.is(Items.PUFFERFISH)) {
            return true;
        }

        // Meats: Happy Eater allows all meats; normal cats also enjoy chicken
        if (stack.is(CAT_MEAT) || stack.is(Items.CHICKEN) || stack.is(Items.COOKED_CHICKEN)) {
            if (cat == null || cat.getTalentLevel(Talents.HAPPY_EATER) > 0 || stack.is(Items.CHICKEN) || stack.is(Items.COOKED_CHICKEN)) {
                return true;
            }
        }

        return false;
    }

    public static CatFoodValue getFoodValues(ItemStack stack, TalentedCat cat) {
        if (stack.isEmpty()) return new CatFoodValue(0, 0, 0);

        int happyEater = cat != null ? cat.getTalentLevel(Talents.HAPPY_EATER) : 0;
        float bonusMult = 1.0F + (happyEater * 0.25F);

        // Deep Fried Chicken - 15 Nutrition, 15 Saturation
        if (stack.is(ModItems.DEEP_FRIED_CHICKEN.get())) {
            return new CatFoodValue((int)(50 * bonusMult), 15.0F, 12.0F * bonusMult);
        }

        // Golden Carrot - Premium food
        if (stack.is(Items.GOLDEN_CARROT)) {
            return new CatFoodValue((int)(40 * bonusMult), 14.4F, 10.0F * bonusMult);
        }

        // Custom CatFoodItem
        if (stack.getItem() instanceof CatFoodItem foodItem) {
            return new CatFoodValue((int)(foodItem.getHungerRestored() * bonusMult), 8.0F, foodItem.getHealAmount() * bonusMult);
        }

        // Vanilla Fish
        if (stack.is(Items.COOKED_SALMON) || stack.is(Items.COOKED_COD)) {
            return new CatFoodValue((int)(30 * bonusMult), 9.6F, 8.0F * bonusMult);
        }
        if (stack.is(Items.SALMON) || stack.is(Items.COD)) {
            return new CatFoodValue((int)(20 * bonusMult), 4.0F, 5.0F * bonusMult);
        }
        if (stack.is(Items.TROPICAL_FISH)) {
            return new CatFoodValue((int)(15 * bonusMult), 2.0F, 3.0F * bonusMult);
        }
        if (stack.is(Items.PUFFERFISH)) {
            return new CatFoodValue((int)(15 * bonusMult), 1.0F, 2.0F * bonusMult);
        }

        // Cooked Meats
        if (stack.is(Items.COOKED_CHICKEN) || stack.is(Items.COOKED_BEEF) || stack.is(Items.COOKED_PORKCHOP) || stack.is(Items.COOKED_MUTTON)) {
            return new CatFoodValue((int)(30 * bonusMult), 9.0F, 6.0F * bonusMult);
        }
        // Raw Meats
        if (stack.is(Items.CHICKEN) || stack.is(Items.BEEF) || stack.is(Items.PORKCHOP) || stack.is(Items.MUTTON) || stack.is(Items.RABBIT)) {
            return new CatFoodValue((int)(18 * bonusMult), 3.0F, 4.0F * bonusMult);
        }

        // Fallback for any vanilla edible food item
        FoodProperties props = stack.getItem().getFoodProperties(stack, null);
        if (props != null) {
            int nutrition = props.getNutrition();
            float sat = props.getSaturationModifier() * nutrition * 2.0F;
            return new CatFoodValue((int)(nutrition * 3 * bonusMult), sat, (nutrition / 2.0F) * bonusMult);
        }

        return new CatFoodValue(15, 2.0F, 3.0F);
    }
}
