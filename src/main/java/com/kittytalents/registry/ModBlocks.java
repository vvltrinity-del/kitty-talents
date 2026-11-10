package com.kittytalents.registry;

import com.kittytalents.KittyTalents;
import com.kittytalents.block.CatFoodBowlBlock;
import com.kittytalents.block.CatScratcherBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, KittyTalents.MODID);

    public static final RegistryObject<Block> CAT_FOOD_BOWL = BLOCKS.register("cat_food_bowl",
            () -> new CatFoodBowlBlock(BlockBehaviour.Properties.of()
                    .strength(0.5F)
                    .sound(SoundType.WOOD)
                    .noOcclusion()));

    public static final RegistryObject<Block> CAT_SCRATCHER = BLOCKS.register("cat_scratcher",
            () -> new CatScratcherBlock(BlockBehaviour.Properties.of()
                    .strength(1.0F)
                    .sound(SoundType.WOOD)
                    .noOcclusion()));

    public static final RegistryObject<Block> CAT_BED = BLOCKS.register("cat_bed",
            () -> new com.kittytalents.block.CatBedBlock(BlockBehaviour.Properties.of()
                    .strength(0.8F)
                    .sound(SoundType.WOOL)
                    .noOcclusion()));

    public static final RegistryObject<Block> CAT_BATH = BLOCKS.register("cat_bath",
            () -> new com.kittytalents.block.CatBathBlock(BlockBehaviour.Properties.of()
                    .strength(1.2F)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final RegistryObject<Block> CAT_RICE_MILL = BLOCKS.register("cat_rice_mill",
            () -> new com.kittytalents.block.CatRiceMillBlock(BlockBehaviour.Properties.of()
                    .strength(1.5F)
                    .sound(SoundType.STONE)
                    .noOcclusion()));

    public static final RegistryObject<Block> KITTY_HABITAT = BLOCKS.register("kitty_habitat",
            () -> new com.kittytalents.block.KittyHabitatBlock(BlockBehaviour.Properties.of()
                    .strength(2.0F)
                    .sound(SoundType.WOOD)
                    .noOcclusion()));

    public static final RegistryObject<Block> FEEDING_STATION = BLOCKS.register("feeding_station",
            () -> new com.kittytalents.block.KittyFeedingStationBlock(BlockBehaviour.Properties.of()
                    .strength(1.0F)
                    .sound(SoundType.WOOD)
                    .noOcclusion()));

    public static final RegistryObject<Block> KITTEN_INCUBATOR = BLOCKS.register("kitten_incubator",
            () -> new com.kittytalents.block.KittenIncubatorBlock(BlockBehaviour.Properties.of()
                    .strength(1.5F)
                    .sound(SoundType.GLASS)
                    .noOcclusion()));

    public static final RegistryObject<Block> KITTY_WORK_STATION = BLOCKS.register("kitty_work_station",
            () -> new com.kittytalents.block.KittyWorkStationBlock(BlockBehaviour.Properties.of()
                    .strength(2.0F)
                    .sound(SoundType.WOOD)
                    .noOcclusion()));

    public static final RegistryObject<Block> TREAT_OVEN = BLOCKS.register("treat_oven",
            () -> new com.kittytalents.block.TreatOvenBlock(BlockBehaviour.Properties.of()
                    .strength(2.0F)
                    .sound(SoundType.STONE)
                    .noOcclusion()));

    /** 4.0: two-block perch cats rest on (see CatTreeBlock / CatPerchGoal). */
    public static final RegistryObject<Block> CAT_TREE = BLOCKS.register("cat_tree",
            () -> new com.kittytalents.block.CatTreeBlock(BlockBehaviour.Properties.of()
                    .strength(1.2F)
                    .sound(SoundType.WOOL)
                    .noOcclusion()));
}
