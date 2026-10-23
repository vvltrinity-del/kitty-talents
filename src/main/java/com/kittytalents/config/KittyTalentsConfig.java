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

        // Work & Automation Config (2.0)
        public final ForgeConfigSpec.BooleanValue allowWorkerBlockBreaking;
        public final ForgeConfigSpec.IntValue workScanBlocksPerTick;
        public final ForgeConfigSpec.IntValue maxWorkersPerPlayer;
        public final ForgeConfigSpec.IntValue maxWorkersPerStation;
        public final ForgeConfigSpec.IntValue maxStationRadius;
        public final ForgeConfigSpec.BooleanValue workerChunkLoading;
        public final ForgeConfigSpec.IntValue habitatFePerTick;
        public final ForgeConfigSpec.BooleanValue rancherCulling;
        public final ForgeConfigSpec.IntValue rancherAnimalCap;

        // TacZ Integration Config (2.0)
        public final ForgeConfigSpec.BooleanValue catGunsEnabled;
        public final ForgeConfigSpec.DoubleValue catGunDamageMultiplier;
        public final ForgeConfigSpec.IntValue requireArmoryRankForGuns;
        public final ForgeConfigSpec.DoubleValue catBallisticReductionCap;

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

            builder.push("work");
            allowWorkerBlockBreaking = builder.comment("Whether worker cats can break and place blocks via FakePlayer.")
                    .define("allowWorkerBlockBreaking", true);
            workScanBlocksPerTick = builder.comment("Maximum blocks a worker cat scans per tick for jobs.")
                    .defineInRange("workScanBlocksPerTick", 64, 8, 512);
            maxWorkersPerPlayer = builder.comment("Maximum active working cats per player.")
                    .defineInRange("maxWorkersPerPlayer", 8, 1, 32);
            maxWorkersPerStation = builder.comment("Maximum cats that can be bound to a single Kitty Work Station.")
                    .defineInRange("maxWorkersPerStation", 2, 1, 8);
            maxStationRadius = builder.comment("Maximum allowed work area radius around a Kitty Work Station.")
                    .defineInRange("maxStationRadius", 32, 4, 64);
            workerChunkLoading = builder.comment("Whether clocked-in worker cats keep their station chunk loaded.")
                    .define("workerChunkLoading", false);
            habitatFePerTick = builder.comment("FE/t consumed by Kitty Habitat 2.0 for up to +100% speed boost.")
                    .defineInRange("habitatFePerTick", 20, 1, 500);
            rancherCulling = builder.comment("Whether Rancher cats cull surplus adult animals above rancherAnimalCap.")
                    .define("rancherCulling", false);
            rancherAnimalCap = builder.comment("Maximum animals per species in a Rancher work area.")
                    .defineInRange("rancherAnimalCap", 8, 2, 32);
            builder.pop();

            builder.push("tacz");
            catGunsEnabled = builder.comment("Whether cats can equip and fire TacZ firearms.")
                    .define("catGunsEnabled", true);
            catGunDamageMultiplier = builder.comment("Base multiplier applied to TacZ bullets fired by Talented Cats.")
                    .defineInRange("catGunDamageMultiplier", 1.0D, 0.1D, 10.0D);
            requireArmoryRankForGuns = builder.comment("Minimum Cat Armory talent rank required to equip and fire TacZ guns.")
                    .defineInRange("requireArmoryRankForGuns", 3, 0, 5);
            catBallisticReductionCap = builder.comment("Maximum fraction of incoming gunfire damage that Feline Armor can mitigate.")
                    .defineInRange("catBallisticReductionCap", 0.70D, 0.0D, 0.95D);
            builder.pop();
        }
    }
}
