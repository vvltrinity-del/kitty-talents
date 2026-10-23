package com.kittytalents.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class KittyTalentsConfig {
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final Common COMMON;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        COMMON = new Common(builder);
        COMMON_SPEC = builder.build();
    }

    public static class Common {
        public final ForgeConfigSpec.IntValue maxCatLevel;
        public final ForgeConfigSpec.IntValue direLevelRequirement;
        public final ForgeConfigSpec.IntValue hungerTickRate;
        public final ForgeConfigSpec.DoubleValue mountSpeedMultiplier;
        public final ForgeConfigSpec.DoubleValue flightSpeedMultiplier;
        public final ForgeConfigSpec.IntValue whistleRadius;
        public final ForgeConfigSpec.BooleanValue friendlyFire;
        public final ForgeConfigSpec.BooleanValue immortalCats;

        public Common(ForgeConfigSpec.Builder builder) {
            builder.push("general");
            maxCatLevel = builder.comment("Maximum level a Talented Cat can achieve.")
                    .defineInRange("maxCatLevel", 100, 10, 200);
            direLevelRequirement = builder.comment("Minimum level required for Dire Treat awakening.")
                    .defineInRange("direLevelRequirement", 60, 20, 100);
            hungerTickRate = builder.comment("Ticks between hunger drain events (default 1200 ticks = 60s).")
                    .defineInRange("hungerTickRate", 1200, 200, 6000);
            mountSpeedMultiplier = builder.comment("Speed multiplier for Giant Kitty Mount.")
                    .defineInRange("mountSpeedMultiplier", 1.0D, 0.5D, 3.0D);
            flightSpeedMultiplier = builder.comment("Speed multiplier for Sky Feline flight.")
                    .defineInRange("flightSpeedMultiplier", 1.0D, 0.5D, 3.0D);
            whistleRadius = builder.comment("Command radius for the Kitty Whistle in blocks.")
                    .defineInRange("whistleRadius", 32, 8, 128);
            friendlyFire = builder.comment("Whether owners can accidentally damage their own cats.")
                    .define("friendlyFire", false);
            immortalCats = builder.comment("If true, lethal damage incapacitates cats instead of deleting them.")
                    .define("immortalCats", true);
            builder.pop();
        }
    }
}
