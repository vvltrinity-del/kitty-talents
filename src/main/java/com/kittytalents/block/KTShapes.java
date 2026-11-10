package com.kittytalents.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

/**
 * Hitboxes for the 3D block models (4.0 Phase 2). Shapes are authored for a block facing NORTH, the same way the
 * models are (tools/asset_gen/blocks_3d.py), and rotated here for the other horizontal facings.
 */
public final class KTShapes {
    private KTShapes() {}

    /** Union of boxes in 1/16 block units (Block.box). Each box is {x0, y0, z0, x1, y1, z1}. */
    public static VoxelShape of(double[]... boxes) {
        VoxelShape s = Shapes.empty();
        for (double[] b : boxes) {
            s = Shapes.joinUnoptimized(s, Block.box(b[0], b[1], b[2], b[3], b[4], b[5]), BooleanOp.OR);
        }
        return s.optimize();
    }

    /** The NORTH shape rotated to every horizontal facing. */
    public static Map<Direction, VoxelShape> byFacing(VoxelShape north) {
        Map<Direction, VoxelShape> map = new EnumMap<>(Direction.class);
        map.put(Direction.NORTH, north);
        map.put(Direction.EAST, rotateClockwise(north));
        map.put(Direction.SOUTH, rotateClockwise(map.get(Direction.EAST)));
        map.put(Direction.WEST, rotateClockwise(map.get(Direction.SOUTH)));
        return map;
    }

    /** Rotates a shape 90 degrees clockwise seen from above (north -> east), matching blockstate y=90. */
    public static VoxelShape rotateClockwise(VoxelShape shape) {
        VoxelShape[] out = { Shapes.empty() };
        shape.forAllBoxes((x0, y0, z0, x1, y1, z1) ->
                out[0] = Shapes.joinUnoptimized(out[0], Shapes.box(1 - z1, y0, x0, 1 - z0, y1, x1), BooleanOp.OR));
        return out[0].optimize();
    }
}
