package com.kittytalents.client;

import com.kittytalents.client.gui.BasePinScreen;
import com.kittytalents.network.PacketSyncBaseAreas;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

import java.util.List;

/** Client copy of the player's base areas (5.0 Base Pin): border particles while holding the pin, and the menu. */
public final class ClientBaseAreas {
    private ClientBaseAreas() {}

    private static List<PacketSyncBaseAreas.Entry> entries = List.of();
    private static final DustParticleOptions BORDER = new DustParticleOptions(new Vector3f(1.0F, 0.75F, 0.3F), 1.0F);

    public static List<PacketSyncBaseAreas.Entry> entries() { return entries; }

    public static void receive(List<PacketSyncBaseAreas.Entry> list, boolean open) {
        entries = List.copyOf(list);
        Minecraft mc = Minecraft.getInstance();
        if (open) mc.setScreen(new BasePinScreen());
        else if (mc.screen instanceof BasePinScreen s) s.refresh();
    }

    /** Dust along the edges of the areas in this dimension near the player. */
    public static void drawBorders(Level level, Player player) {
        String dim = level.dimension().location().toString();
        for (PacketSyncBaseAreas.Entry e : entries) {
            var a = e.area();
            if (!a.dimension().equals(dim)) continue;
            double x0 = a.min().getX(), x1 = a.max().getX() + 1, z0 = a.min().getZ(), z1 = a.max().getZ() + 1;
            double y = player.getY() + 0.2;
            for (double x = x0; x <= x1; x += 1.0) { dot(level, player, x, y, z0); dot(level, player, x, y, z1); }
            for (double z = z0; z <= z1; z += 1.0) { dot(level, player, x0, y, z); dot(level, player, x1, y, z); }
        }
    }

    private static void dot(Level level, Player p, double x, double y, double z) {
        double dx = x - p.getX(), dz = z - p.getZ();
        if (dx * dx + dz * dz > 48 * 48) return;
        level.addParticle(BORDER, x, y, z, 0, 0, 0);
    }
}
