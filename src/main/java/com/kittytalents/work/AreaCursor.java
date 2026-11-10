package com.kittytalents.work;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Budgeted 3D bounding-box cursor that scans at most N blocks per tick
 * without allocating full volume lists.
 */
public class AreaCursor {
    private int minX, minY, minZ;
    private int maxX, maxY, maxZ;
    private int curX, curY, curZ;
    private boolean topDown = false;
    private boolean wrappedLastStep = false;
    private boolean wrappedThisPass = false;

    public AreaCursor(BoundingBox box, boolean topDown) {
        setBounds(box, topDown);
    }

    public void setBounds(BoundingBox box, boolean topDown) {
        this.topDown = topDown;
        this.minX = Math.min(box.minX(), box.maxX());
        this.minY = Math.min(box.minY(), box.maxY());
        this.minZ = Math.min(box.minZ(), box.maxZ());
        this.maxX = Math.max(box.minX(), box.maxX());
        this.maxY = Math.max(box.minY(), box.maxY());
        this.maxZ = Math.max(box.minZ(), box.maxZ());
        reset();
    }

    public void reset() {
        this.curX = minX;
        this.curZ = minZ;
        this.curY = topDown ? maxY : minY;
        this.wrappedLastStep = false;
        this.wrappedThisPass = false;
    }

    public boolean didWrap() {
        return wrappedLastStep;
    }

    public boolean checkAndResetWrapped() {
        boolean w = wrappedThisPass;
        wrappedThisPass = false;
        return w;
    }

    public BlockPos next() {
        wrappedLastStep = false;
        BlockPos result = new BlockPos(curX, curY, curZ);
        curX++;
        if (curX > maxX) {
            curX = minX;
            curZ++;
            if (curZ > maxZ) {
                curZ = minZ;
                if (topDown) {
                    curY--;
                    if (curY < minY) {
                        curY = maxY;
                        wrappedLastStep = true;
                        wrappedThisPass = true;
                    }
                } else {
                    curY++;
                    if (curY > maxY) {
                        curY = minY;
                        wrappedLastStep = true;
                        wrappedThisPass = true;
                    }
                }
            }
        }
        return result;
    }
}
