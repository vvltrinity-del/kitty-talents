package com.kittytalents.registry;

import com.kittytalents.KittyTalents;
import com.kittytalents.block.CatFoodBowlBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, KittyTalents.MODID);

    public static final RegistryObject<BlockEntityType<CatFoodBowlBlockEntity>> CAT_FOOD_BOWL =
            BLOCK_ENTITIES.register("cat_food_bowl",
                    () -> BlockEntityType.Builder.of(CatFoodBowlBlockEntity::new, ModBlocks.CAT_FOOD_BOWL.get())
                            .build(null));

    public static final RegistryObject<BlockEntityType<com.kittytalents.block.CatBedBlockEntity>> CAT_BED =
            BLOCK_ENTITIES.register("cat_bed",
                    () -> BlockEntityType.Builder.of(com.kittytalents.block.CatBedBlockEntity::new, ModBlocks.CAT_BED.get())
                            .build(null));

    public static final RegistryObject<BlockEntityType<com.kittytalents.block.CatBathBlockEntity>> CAT_BATH =
            BLOCK_ENTITIES.register("cat_bath",
                    () -> BlockEntityType.Builder.of(com.kittytalents.block.CatBathBlockEntity::new, ModBlocks.CAT_BATH.get())
                            .build(null));

    public static final RegistryObject<BlockEntityType<com.kittytalents.block.CatRiceMillBlockEntity>> CAT_RICE_MILL =
            BLOCK_ENTITIES.register("cat_rice_mill",
                    () -> BlockEntityType.Builder.of(com.kittytalents.block.CatRiceMillBlockEntity::new, ModBlocks.CAT_RICE_MILL.get())
                            .build(null));

    public static final RegistryObject<BlockEntityType<com.kittytalents.block.KittyHabitatBlockEntity>> KITTY_HABITAT =
            BLOCK_ENTITIES.register("kitty_habitat",
                    () -> BlockEntityType.Builder.of(com.kittytalents.block.KittyHabitatBlockEntity::new, ModBlocks.KITTY_HABITAT.get())
                            .build(null));

    public static final RegistryObject<BlockEntityType<com.kittytalents.block.KittyFeedingStationBlockEntity>> FEEDING_STATION =
            BLOCK_ENTITIES.register("feeding_station",
                    () -> BlockEntityType.Builder.of(com.kittytalents.block.KittyFeedingStationBlockEntity::new, ModBlocks.FEEDING_STATION.get())
                            .build(null));

    public static final RegistryObject<BlockEntityType<com.kittytalents.block.KittenIncubatorBlockEntity>> KITTEN_INCUBATOR =
            BLOCK_ENTITIES.register("kitten_incubator",
                    () -> BlockEntityType.Builder.of(com.kittytalents.block.KittenIncubatorBlockEntity::new, ModBlocks.KITTEN_INCUBATOR.get())
                            .build(null));

    public static final RegistryObject<BlockEntityType<com.kittytalents.block.KittyWorkStationBlockEntity>> KITTY_WORK_STATION =
            BLOCK_ENTITIES.register("kitty_work_station",
                    () -> BlockEntityType.Builder.of(com.kittytalents.block.KittyWorkStationBlockEntity::new, ModBlocks.KITTY_WORK_STATION.get())
                            .build(null));

    public static final RegistryObject<BlockEntityType<com.kittytalents.block.TreatOvenBlockEntity>> TREAT_OVEN =
            BLOCK_ENTITIES.register("treat_oven",
                    () -> BlockEntityType.Builder.of(com.kittytalents.block.TreatOvenBlockEntity::new, ModBlocks.TREAT_OVEN.get())
                            .build(null));
}
