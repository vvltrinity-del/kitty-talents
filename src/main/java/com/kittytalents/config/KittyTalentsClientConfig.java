package com.kittytalents.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** Per-player display settings (config/kittytalents-client.toml). */
public final class KittyTalentsClientConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue SHOW_COMPANION_HUD;
    public static final ForgeConfigSpec.IntValue COMPANION_HUD_MAX;
    public static final ForgeConfigSpec.IntValue COMPANION_HUD_RANGE;
    public static final ForgeConfigSpec.BooleanValue SHOW_FRIEND_CATS;
    public static final ForgeConfigSpec.BooleanValue SHOW_TWINE_MARKERS;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("hud");
        SHOW_COMPANION_HUD = b.comment("Show small health cards for your cats that are following you (or hurt) nearby.")
                .define("showCompanionHud", true);
        COMPANION_HUD_MAX = b.comment("How many companion cards to show at most.")
                .defineInRange("companionHudMax", 3, 1, 6);
        COMPANION_HUD_RANGE = b.comment("Only cats within this many blocks get a card.")
                .defineInRange("companionHudRange", 32, 8, 128);
        SHOW_FRIEND_CATS = b.comment("Also show cards for cats of players you share a cat with (they co-own one of yours, or you co-own one of theirs).")
                .define("showFriendCats", false);
        SHOW_TWINE_MARKERS = b.comment("Show a small label over cats other players are Twined with, and where their body rests.")
                .define("showTwineMarkers", true);
        b.pop();
        SPEC = b.build();
    }

    private KittyTalentsClientConfig() {}
}
