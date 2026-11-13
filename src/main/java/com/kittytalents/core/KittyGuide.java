package com.kittytalents.core;

import com.kittytalents.KittyTalents;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * The guided first ten minutes (5.0): a "Kitty Talents" advancement tab that walks a new player through one system
 * at a time (tame, Follow, first talent, bowl or bed, a job, a combat role, Twine). Steps that vanilla can detect
 * (taming, placing a block) are plain advancement criteria; the rest are granted here from the matching action.
 */
public final class KittyGuide {
    private KittyGuide() {}

    public static final String FOLLOW = "follow", TALENT = "talent", JOB = "job", ROLE = "role", TWINE = "twine";

    /** Grants the guide step {@code step} to {@code player} (no-op if done or missing). */
    public static void award(ServerPlayer player, String step) {
        if (player == null || player.getServer() == null) return;
        Advancement adv = player.getServer().getAdvancements().getAdvancement(new ResourceLocation(KittyTalents.MODID, "guide/" + step));
        if (adv == null) return;
        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(adv);
        if (progress.isDone()) return;
        for (String criterion : progress.getRemainingCriteria()) player.getAdvancements().award(adv, criterion);
    }
}
