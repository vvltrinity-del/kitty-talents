package com.kittytalents.compat.playerrevive;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;
import java.lang.reflect.Method;

public class PlayerReviveCompat {
    private static Boolean loaded = null;
    private static Method isBleedingMethod = null;
    private static Method reviveMethod = null;

    public static boolean isLoaded() {
        if (loaded == null) {
            loaded = ModList.get().isLoaded("playerrevive");
            if (loaded) {
                try {
                    Class<?> clazz = Class.forName("team.creative.playerrevive.server.PlayerReviveServer");
                    isBleedingMethod = clazz.getMethod("isBleeding", Player.class);
                    reviveMethod = clazz.getMethod("revive", Player.class);
                } catch (Exception e) {
                    loaded = false;
                }
            }
        }
        return loaded;
    }

    public static boolean isBleeding(Player player) {
        if (player == null) return false;
        if (!isLoaded()) {
            return player.getPersistentData().getBoolean("playerrevive:bleeding");
        }
        try {
            return (boolean) isBleedingMethod.invoke(null, player);
        } catch (Exception e) {
            return player.getPersistentData().getBoolean("playerrevive:bleeding");
        }
    }

    public static boolean revive(Player player) {
        if (player == null) return false;
        boolean success = false;
        if (isLoaded() && reviveMethod != null) {
            try {
                reviveMethod.invoke(null, player);
                success = true;
            } catch (Exception e) {
                // Ignore fallback to generic revive
            }
        }
        player.setForcedPose(null);
        player.getPersistentData().remove("playerrevive:bleeding");
        if (player.getHealth() < 4.0F) {
            player.setHealth(10.0F);
        }
        return success || true;
    }
}
