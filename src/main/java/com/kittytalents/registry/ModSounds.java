package com.kittytalents.registry;

import com.kittytalents.KittyTalents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Sound Registry for Kitty Talents 3.0 (R32 SFX Polish).
 * Provides high-quality sound events for combat swipes, bites, pouncing, dodging,
 * active ability casts, Twining transitions, and affectionate companion interactions.
 */
public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, KittyTalents.MODID);

    // Combat SFX
    public static final RegistryObject<SoundEvent> CAT_CLAW_SWIPE = register("cat_claw_swipe");
    public static final RegistryObject<SoundEvent> CAT_CLAW_HIT = register("cat_claw_hit");
    public static final RegistryObject<SoundEvent> CAT_BITE = register("cat_bite");
    public static final RegistryObject<SoundEvent> CAT_POUNCE_LAUNCH = register("cat_pounce_launch");
    public static final RegistryObject<SoundEvent> CAT_POUNCE_IMPACT = register("cat_pounce_impact");
    public static final RegistryObject<SoundEvent> CAT_DODGE = register("cat_dodge");
    public static final RegistryObject<SoundEvent> CAT_PERFECT_DODGE = register("cat_perfect_dodge");

    // Ability & Magic SFX
    public static final RegistryObject<SoundEvent> ABILITY_CAST = register("ability_cast");
    public static final RegistryObject<SoundEvent> ABILITY_HISS = register("ability_hiss");
    public static final RegistryObject<SoundEvent> ABILITY_PURR = register("ability_purr");
    public static final RegistryObject<SoundEvent> ABILITY_ELEMENTAL = register("ability_elemental");
    public static final RegistryObject<SoundEvent> ABILITY_ULTIMATE = register("ability_ultimate");

    // Twine & Dimension SFX
    public static final RegistryObject<SoundEvent> TWINE_ENTER = register("twine_enter");
    public static final RegistryObject<SoundEvent> TWINE_EXIT = register("twine_exit");

    // Companion Interaction SFX
    public static final RegistryObject<SoundEvent> CAT_PURR_DEEP = register("cat_purr_deep");
    public static final RegistryObject<SoundEvent> CAT_GROOMING = register("cat_grooming");
    public static final RegistryObject<SoundEvent> CAT_PETTING = register("cat_petting");

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(KittyTalents.MODID, name)));
    }

    public static void register(IEventBus bus) {
        SOUND_EVENTS.register(bus);
    }
}
