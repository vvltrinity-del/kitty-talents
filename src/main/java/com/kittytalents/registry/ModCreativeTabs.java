package com.kittytalents.registry;

import com.kittytalents.KittyTalents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, KittyTalents.MODID);

    public static final RegistryObject<CreativeModeTab> KITTY_TALENTS_TAB = CREATIVE_MODE_TABS.register("kitty_talents_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.CATNIP_TREAT.get()))
                    .title(Component.translatable("itemGroup.kittytalents"))
                    .displayItems((parameters, output) -> {
                        // Progression & Treats
                        output.accept(ModItems.CATNIP.get());
                        output.accept(ModItems.CATNIP_TREAT.get());
                        output.accept(ModItems.SUPER_CATNIP_TREAT.get());
                        output.accept(ModItems.MASTER_CATNIP_TREAT.get());
                        output.accept(ModItems.DIRE_TREAT.get());
                        output.accept(ModItems.AMNESIA_TREAT.get());
                        output.accept(ModItems.CATNIP_RESPEC_ELIXIR.get());

                        // Tools & Utility
                        output.accept(ModItems.KITTY_WHISTLE.get());
                        output.accept(ModItems.KITTY_TRACKER.get());
                        output.accept(ModItems.KITTY_WORK_CLIPBOARD.get());
                        output.accept(ModItems.VARIANT_CHART.get());
                        output.accept(ModItems.CAT_CARRIER.get());
                        output.accept(ModItems.CAT_LASER_POINTER.get());
                        output.accept(ModItems.TREAT_BAG.get());
                        output.accept(ModItems.GROOMING_SHEARS.get());
                        output.accept(ModItems.WARM_MILK.get());
                        output.accept(ModItems.CAT_COLLAR.get());
                        output.accept(ModItems.CATNIP_BUCKET.get());
                        output.accept(ModItems.CATNIP_MILK_BUCKET.get());

                        // Culinary Foods
                        output.accept(ModItems.CATNIP_COOKIE.get());
                        output.accept(ModItems.SALMON_SUSHI.get());
                        output.accept(ModItems.TUNA_BOWL.get());
                        output.accept(ModItems.PREMIUM_FEAST.get());
                        output.accept(ModItems.DEEP_FRIED_CHICKEN.get());
                        output.accept(ModItems.RICE_GRAIN.get());
                        output.accept(ModItems.RICE_BOWL.get());

                        // Wearable Accessories & Cosmetics
                        output.accept(ModItems.WITCH_HAT.get());
                        output.accept(ModItems.TOP_HAT.get());
                        output.accept(ModItems.BOWTIE.get());
                        output.accept(ModItems.HERO_CAPE.get());
                        output.accept(ModItems.FELINE_GOGGLES.get());
                        output.accept(ModItems.CROWN.get());
                        output.accept(ModItems.HALO.get());
                        output.accept(ModItems.ANGEL_WINGS.get());
                        output.accept(ModItems.CAT_SCARF.get());
                        output.accept(ModItems.PHANTOM_MASK.get());

                        // Toys
                        output.accept(ModItems.YARN_BALL.get());
                        output.accept(ModItems.FEATHER_TOY.get());
                        output.accept(ModItems.TOY_MOUSE.get());
                        output.accept(ModItems.CAT_BALL.get());
                        output.accept(ModItems.TOY_FISH.get());
                        output.accept(ModItems.FRISBEE.get());

                        // Locator, Breeding & Care
                        output.accept(ModItems.EMPTY_LOCATOR_ORB.get());
                        output.accept(ModItems.FELINE_LOCATOR_ORB.get());
                        output.accept(ModItems.BREEDING_CATNIP.get());
                        output.accept(ModItems.SPIRIT_CATNIP.get());
                        output.accept(ModItems.FELINE_WRAP.get());
                        output.accept(ModItems.HEALING_TREAT.get());
                        output.accept(ModItems.GENDER_RIBBON.get());

                        // Productive Cats System & Upgrades
                        output.accept(ModItems.RESOURCE_CATALYST.get());
                        output.accept(ModItems.CATNIP_CATALYST.get());
                        output.accept(ModItems.ORE_ESSENCE.get());
                        output.accept(ModItems.UPGRADE_PRODUCTIVITY_1.get());
                        output.accept(ModItems.UPGRADE_PRODUCTIVITY_2.get());
                        output.accept(ModItems.UPGRADE_PRODUCTIVITY_3.get());
                        output.accept(ModItems.UPGRADE_PRODUCTIVITY_4.get());
                        output.accept(ModItems.UPGRADE_PRODUCTIVITY_5.get());
                        output.accept(ModItems.UPGRADE_PRODUCTIVITY_6.get());
                        output.accept(ModItems.COAL_CAT_TREAT.get());
                        output.accept(ModItems.COPPER_CAT_TREAT.get());
                        output.accept(ModItems.IRON_CAT_TREAT.get());
                        output.accept(ModItems.GOLD_CAT_TREAT.get());
                        output.accept(ModItems.LAPIS_CAT_TREAT.get());
                        output.accept(ModItems.REDSTONE_CAT_TREAT.get());
                        output.accept(ModItems.EMERALD_CAT_TREAT.get());
                        output.accept(ModItems.DIAMOND_CAT_TREAT.get());
                        output.accept(ModItems.NETHERITE_CAT_TREAT.get());

                        // Furniture & Blocks
                        output.accept(ModItems.CAT_BED.get());
                        output.accept(ModItems.CAT_FOOD_BOWL.get());
                        output.accept(ModItems.CAT_SCRATCHER.get());
                        output.accept(ModItems.CAT_BATH.get());
                        output.accept(ModItems.CAT_RICE_MILL.get());
                        output.accept(ModItems.KITTY_HABITAT.get());
                        output.accept(ModItems.FEEDING_STATION.get());
                        output.accept(ModItems.KITTEN_INCUBATOR.get());

                        // Spawn Egg
                        output.accept(ModItems.TALENTED_CAT_SPAWN_EGG.get());
                    })
                    .build());
}
