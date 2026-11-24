package com.kittytalents.util;

import com.kittytalents.entity.TalentedCat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;

import javax.annotation.Nullable;
import java.util.function.Function;

/**
 * Moving cats to a point, in any dimension (5.1.1). Used for followers through portals and the whistle's Return Home,
 * which used to teleport a cat to its bed's coordinates in whatever dimension the cat was in.
 */
public final class CatTeleport {
    private CatTeleport() {}

    /**
     * Moves {@code entity} to {@code pos} in {@code dest} and returns it as it now exists (a cross-dimension move makes a
     * copy). Uses an explicit teleporter: the default one runs the portal search, which throws for an entity that isn't
     * standing in a portal. Returns null if the move failed.
     */
    @Nullable
    public static Entity moveTo(Entity entity, ServerLevel dest, Vec3 pos) {
        if (entity.level() == dest) {
            entity.teleportTo(pos.x, pos.y, pos.z);
            return entity;
        }
        return entity.changeDimension(dest, new ITeleporter() {
            @Override
            public PortalInfo getPortalInfo(Entity e, ServerLevel to, Function<ServerLevel, PortalInfo> defaultPortalInfo) {
                return new PortalInfo(pos, Vec3.ZERO, e.getYRot(), e.getXRot());
            }

            @Override
            public boolean playTeleportSound(ServerPlayer p, ServerLevel src, ServerLevel to) {
                return false;
            }
        });
    }

    /** The dimension the cat's bed is in. Beds saved before dimensions were stored count as the cat's own. */
    public static ResourceKey<Level> homeDimension(TalentedCat cat) {
        String dim = cat.getHomeBedDimension();
        if (dim == null || dim.isEmpty()) return cat.level().dimension();
        ResourceLocation id = ResourceLocation.tryParse(dim);
        return id == null ? cat.level().dimension() : ResourceKey.create(Registries.DIMENSION, id);
    }

    /** True if the cat has a bed and it is in the dimension the cat is in now. Goals that walk to the bed need this. */
    public static boolean isHomeBedHere(TalentedCat cat) {
        return cat.getHomeBedPos() != null && homeDimension(cat).equals(cat.level().dimension());
    }

    /**
     * Teleports the cat onto its bed, crossing dimensions when the bed is in another one, and returns the cat as it now
     * exists. Null when it has no bed, the bed's dimension isn't loaded, or this isn't the server.
     */
    @Nullable
    public static TalentedCat sendHome(TalentedCat cat, double yOffset) {
        BlockPos bed = cat.getHomeBedPos();
        if (bed == null || !(cat.level() instanceof ServerLevel here)) return null;
        ServerLevel dest = here.getServer().getLevel(homeDimension(cat));
        if (dest == null) return null;
        Entity moved = moveTo(cat, dest, new Vec3(bed.getX() + 0.5D, bed.getY() + yOffset, bed.getZ() + 0.5D));
        return moved instanceof TalentedCat c ? c : null;
    }
}
