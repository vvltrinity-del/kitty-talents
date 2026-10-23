package com.kittytalents.registry;

import com.kittytalents.KittyTalents;
import com.kittytalents.entity.TalentedCat;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, KittyTalents.MODID);

    public static final RegistryObject<EntityType<TalentedCat>> TALENTED_CAT = ENTITIES.register("talented_cat",
            () -> EntityType.Builder.of(TalentedCat::new, MobCategory.CREATURE)
                    .sized(0.6F, 0.7F)
                    .clientTrackingRange(10)
                    .build("talented_cat"));

    public static final RegistryObject<EntityType<com.kittytalents.entity.ThrownToyEntity>> THROWN_TOY = ENTITIES.register("thrown_toy",
            () -> EntityType.Builder.<com.kittytalents.entity.ThrownToyEntity>of(com.kittytalents.entity.ThrownToyEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("thrown_toy"));

    public static final RegistryObject<EntityType<com.kittytalents.entity.FelineHissProjectileEntity>> FELINE_HISS_PROJECTILE = ENTITIES.register("feline_hiss_projectile",
            () -> EntityType.Builder.<com.kittytalents.entity.FelineHissProjectileEntity>of(com.kittytalents.entity.FelineHissProjectileEntity::new, MobCategory.MISC)
                    .sized(0.3F, 0.3F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("feline_hiss_projectile"));
}
