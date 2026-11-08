package com.kittytalents.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** Per-player display settings (config/kittytalents-client.toml). */
public final class KittyTalentsClientConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue SHOW_COMPANION_HUD;
    public static final ForgeConfigSpec.IntValue COMPANION_HUD_MAX;
    public static final ForgeConfigSpec.IntValue COMPANION_HUD_RANGE;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("hud");
        SHOW_COMPANION_HUD = b.comment("Show small health cards for your cats that are following you (or hurt) nearby.")
                .define("showCompanionHud", true);
        COMPANION_HUD_MAX = b.comment("How many companion cards to show at most.")
                .defineInRange("companionHudMax", 3, 1, 6);
        COMPANION_HUD_RANGE = b.comment("Only cats within this many blocks get a card.")
                .defineInRange("companionHudRange", 32, 8, 128);
        b.pop();
        SPEC = b.build();
    }

    private KittyTalentsClientConfig() {}
}
