package com.kittytalents.core.base;

import com.kittytalents.core.living.CaptainRally;
import com.kittytalents.entity.CatMode;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Guard posts and patrols (5.0 Base Pin). A Guard cat with a guard post holds it (its mode anchor is the post).
 * A Guard cat without a post standing in one of its owner's base areas patrols: the owner's patrolling cats in that
 * area form teams of up to 3, and each team walks the area's border, moving on to the next waypoint every 10 s
 * (the patrol moves the cat's mode anchor; Guard's own goals walk there, defend and come back after a chase).
 * A Captain in a team rallies it while patrolling.
 */
public final class BasePatrol {
    private BasePatrol() {}

    public static final int TEAM_SIZE = 3, STEP_TICKS = 200;

    /** Server, every 40 ticks per Guard cat. */
    public static void tick(TalentedCat cat) {
        if (!(cat.level() instanceof ServerLevel sl) || cat.getOwnerUUID() == null || cat.getCatMode() != CatMode.GUARD) {
            stopRally(cat);
            return;
        }
        BlockPos post = cat.getGuardPost();
        if (post != null) {
            if (!post.equals(cat.getModeAnchor())) cat.setModeAnchor(post);
            stopRally(cat);
            return;
        }
        String dim = sl.dimension().location().toString();
        BaseAreas bases = BaseAreas.get(sl.getServer());
        BlockPos here = cat.getModeAnchor() != null ? cat.getModeAnchor() : cat.blockPosition();
        BaseAreas.Area area = bases.at(cat.getOwnerUUID(), dim, here);
        if (area == null) area = bases.at(cat.getOwnerUUID(), dim, cat.blockPosition());
        if (area == null) {
            stopRally(cat);
            return;
        }
        List<TalentedCat> patrol = patrollers(sl, cat, area);
        int index = patrol.indexOf(cat);
        if (index < 0) return;
        int teams = (patrol.size() + TEAM_SIZE - 1) / TEAM_SIZE;
        int team = index / TEAM_SIZE;
        List<BlockPos> points = waypoints(sl, area);
        int step = (int) (sl.getGameTime() / STEP_TICKS);
        int offset = team * points.size() / Math.max(1, teams);
        cat.setModeAnchor(points.get(Math.floorMod(step + offset, points.size())));
        // A Captain leading a patrol team rallies it.
        if (cat.isCaptain()) {
            CaptainRally.set(cat, true);
            cat.patrolRally = true;
        }
    }

    private static void stopRally(TalentedCat cat) {
        if (cat.patrolRally) {
            cat.patrolRally = false;
            CaptainRally.set(cat, false);
        }
    }

    /** The owner's Guard cats without a post in {@code area}, in a stable order (by UUID). */
    public static List<TalentedCat> patrollers(ServerLevel level, TalentedCat any, BaseAreas.Area area) {
        AABB box = new AABB(area.min().getX(), area.min().getY() - 8, area.min().getZ(), area.max().getX() + 1, area.max().getY() + 9, area.max().getZ() + 1).inflate(12);
        List<TalentedCat> list = new ArrayList<>(level.getEntities(ModEntities.TALENTED_CAT.get(), box,
                c -> any.getOwnerUUID().equals(c.getOwnerUUID()) && c.getCatMode() == CatMode.GUARD && c.getGuardPost() == null && !c.isIncapacitated()));
        list.sort(Comparator.comparing(TalentedCat::getUUID));
        return list;
    }

    /** Eight points around the border (corners and side middles), one block in, at ground height. */
    public static List<BlockPos> waypoints(ServerLevel level, BaseAreas.Area a) {
        int x0 = a.min().getX() + 1, x1 = Math.max(x0, a.max().getX() - 1);
        int z0 = a.min().getZ() + 1, z1 = Math.max(z0, a.max().getZ() - 1);
        int xm = (x0 + x1) / 2, zm = (z0 + z1) / 2;
        int[][] xz = {{x0, z0}, {xm, z0}, {x1, z0}, {x1, zm}, {x1, z1}, {xm, z1}, {x0, z1}, {x0, zm}};
        List<BlockPos> out = new ArrayList<>();
        for (int[] p : xz) {
            int y = level.hasChunkAt(new BlockPos(p[0], 0, p[1]))
                    ? level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, p[0], p[1]) : a.min().getY();
            y = Math.max(a.min().getY(), Math.min(a.max().getY() + 1, y));
            out.add(new BlockPos(p[0], y, p[1]));
        }
        return out;
    }

    /** Guard cats of {@code owner} in an area (for the Base Pin menu). */
    public static int guardsIn(ServerLevel level, java.util.UUID owner, BaseAreas.Area area) {
        AABB box = new AABB(area.min().getX(), area.min().getY() - 8, area.min().getZ(), area.max().getX() + 1, area.max().getY() + 9, area.max().getZ() + 1);
        return level.getEntities(ModEntities.TALENTED_CAT.get(), box, c -> owner.equals(c.getOwnerUUID()) && c.getCatMode() == CatMode.GUARD).size();
    }
}
