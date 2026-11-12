package com.kittytalents.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * Box searches for a kind of block that skip whole 16x16x16 sections whose palette cannot contain it (5.0 performance
 * pass). Same results as walking {@code BlockPos.betweenClosed} over the box, but a cat looking for a bowl or a Cat
 * Tree now reads a handful of palettes instead of thousands of block states. Unloaded chunks are skipped.
 */
public final class BlockSearch {
    private BlockSearch() {}

    /** Calls {@code found} for every block in the box (inclusive) whose state matches, in no particular order. */
    public static void forEach(Level level, BlockPos min, BlockPos max, Predicate<BlockState> match, BiConsumer<BlockPos, BlockState> found) {
        int x0 = Math.min(min.getX(), max.getX()), x1 = Math.max(min.getX(), max.getX());
        int y0 = Math.max(Math.min(min.getY(), max.getY()), level.getMinBuildHeight());
        int y1 = Math.min(Math.max(min.getY(), max.getY()), level.getMaxBuildHeight() - 1);
        int z0 = Math.min(min.getZ(), max.getZ()), z1 = Math.max(min.getZ(), max.getZ());
        if (y0 > y1) return;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int cx = SectionPos.blockToSectionCoord(x0); cx <= SectionPos.blockToSectionCoord(x1); cx++) {
            for (int cz = SectionPos.blockToSectionCoord(z0); cz <= SectionPos.blockToSectionCoord(z1); cz++) {
                if (!level.hasChunk(cx, cz)) continue;
                LevelChunk chunk = level.getChunk(cx, cz);
                for (int sy = SectionPos.blockToSectionCoord(y0); sy <= SectionPos.blockToSectionCoord(y1); sy++) {
                    int index = level.getSectionIndexFromSectionY(sy);
                    if (index < 0 || index >= chunk.getSectionsCount()) continue;
                    LevelChunkSection section = chunk.getSection(index);
                    if (section.hasOnlyAir() || !section.maybeHas(match)) continue;
                    int bx0 = Math.max(x0, cx << 4), bx1 = Math.min(x1, (cx << 4) + 15);
                    int by0 = Math.max(y0, sy << 4), by1 = Math.min(y1, (sy << 4) + 15);
                    int bz0 = Math.max(z0, cz << 4), bz1 = Math.min(z1, (cz << 4) + 15);
                    for (int y = by0; y <= by1; y++) {
                        for (int z = bz0; z <= bz1; z++) {
                            for (int x = bx0; x <= bx1; x++) {
                                BlockState state = section.getBlockState(x & 15, y & 15, z & 15);
                                if (match.test(state)) found.accept(pos.set(x, y, z).immutable(), state);
                            }
                        }
                    }
                }
            }
        }
    }

    /** The matching block closest to {@code center} (squared block distance), or null. */
    public static BlockPos closest(Level level, BlockPos center, BlockPos min, BlockPos max, Predicate<BlockState> match, Predicate<BlockPos> accept) {
        BlockPos[] best = {null};
        double[] bestD = {Double.MAX_VALUE};
        forEach(level, min, max, match, (p, s) -> {
            double d = p.distSqr(center);
            if (d < bestD[0] && accept.test(p)) { bestD[0] = d; best[0] = p; }
        });
        return best[0];
    }
}
