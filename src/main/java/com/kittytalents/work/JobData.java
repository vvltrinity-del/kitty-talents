package com.kittytalents.work;

import com.kittytalents.entity.TalentedCat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class JobData {
    public static final int MAX_LINKS = 8;

    private CatJob job = CatJob.NONE;
    @Nullable
    private BlockPos stationPos = null;
    @Nullable
    private BoundingBox workArea = null;
    private final List<BlockPos> linkedInputs = new ArrayList<>();
    private final List<BlockPos> linkedOutputs = new ArrayList<>();
    private final ItemFilter filter = new ItemFilter();
    private boolean clockedIn = true;
    private long lastWorkTick = 0L;
    private int jobXp = 0;
    private String statusMessage = "Idle";
    private String selectedRecipeId = "";
    private int stockTarget = 64;
    private boolean minerTunnelMode = false;
    private Direction tunnelDirection = Direction.NORTH;

    public CatJob getJob() { return job; }
    public void setJob(CatJob job) { this.job = job != null ? job : CatJob.NONE; }

    @Nullable
    public BlockPos getStationPos() { return stationPos; }
    public void setStationPos(@Nullable BlockPos stationPos) { this.stationPos = stationPos; }

    public BoundingBox getEffectiveWorkArea(BlockPos fallbackOrigin, int defaultRadius) {
        if (workArea != null) return workArea;
        BlockPos center = stationPos != null ? stationPos : fallbackOrigin;
        int r = Math.max(2, defaultRadius);
        return new BoundingBox(
                center.getX() - r, center.getY() - 3, center.getZ() - r,
                center.getX() + r, center.getY() + 5, center.getZ() + r
        );
    }

    @Nullable
    public BoundingBox getCustomWorkArea() { return workArea; }

    public void setWorkArea(@Nullable BoundingBox workArea) {
        this.workArea = workArea;
    }

    public void setWorkAreaCorners(BlockPos a, BlockPos b) {
        this.workArea = BoundingBox.fromCorners(a, b);
    }

    public List<BlockPos> getLinkedInputs() { return linkedInputs; }
    public List<BlockPos> getLinkedOutputs() { return linkedOutputs; }

    public boolean addLinkedInput(BlockPos pos) {
        if (linkedInputs.contains(pos)) {
            linkedInputs.remove(pos);
            return false;
        }
        if (linkedInputs.size() >= MAX_LINKS) linkedInputs.remove(0);
        linkedInputs.add(pos.immutable());
        return true;
    }

    public boolean addLinkedOutput(BlockPos pos) {
        if (linkedOutputs.contains(pos)) {
            linkedOutputs.remove(pos);
            return false;
        }
        if (linkedOutputs.size() >= MAX_LINKS) linkedOutputs.remove(0);
        linkedOutputs.add(pos.immutable());
        return true;
    }

    public ItemFilter getFilter() { return filter; }

    public boolean isClockedIn() { return clockedIn; }
    public void setClockedIn(boolean clockedIn) { this.clockedIn = clockedIn; }

    public long getLastWorkTick() { return lastWorkTick; }
    public void setLastWorkTick(long lastWorkTick) { this.lastWorkTick = lastWorkTick; }

    public int getJobXp() { return jobXp; }
    public void setJobXp(int jobXp) { this.jobXp = Math.max(0, jobXp); }
    public void addJobXp(int amount) { this.jobXp = Math.max(0, this.jobXp + amount); }

    public String getStatusMessage() { return statusMessage; }
    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage != null ? statusMessage : "Idle";
    }

    public String getSelectedRecipeId() { return selectedRecipeId; }
    public void setSelectedRecipeId(String selectedRecipeId) {
        this.selectedRecipeId = selectedRecipeId != null ? selectedRecipeId : "";
    }

    public int getStockTarget() { return stockTarget; }
    public void setStockTarget(int stockTarget) { this.stockTarget = Math.max(1, Math.min(4096, stockTarget)); }

    public boolean isMinerTunnelMode() { return minerTunnelMode; }
    public void setMinerTunnelMode(boolean minerTunnelMode) { this.minerTunnelMode = minerTunnelMode; }

    public Direction getTunnelDirection() { return tunnelDirection; }
    public void setTunnelDirection(Direction dir) {
        this.tunnelDirection = (dir != null && dir.getAxis().isHorizontal()) ? dir : Direction.NORTH;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Job", job.getId());
        if (stationPos != null) {
            tag.put("StationPos", NbtUtils.writeBlockPos(stationPos));
        }
        if (workArea != null) {
            tag.putIntArray("WorkArea", new int[]{
                    workArea.minX(), workArea.minY(), workArea.minZ(),
                    workArea.maxX(), workArea.maxY(), workArea.maxZ()
            });
        }
        ListTag inList = new ListTag();
        for (BlockPos p : linkedInputs) inList.add(NbtUtils.writeBlockPos(p));
        tag.put("LinkedInputs", inList);

        ListTag outList = new ListTag();
        for (BlockPos p : linkedOutputs) outList.add(NbtUtils.writeBlockPos(p));
        tag.put("LinkedOutputs", outList);

        tag.put("Filter", filter.save());
        tag.putBoolean("ClockedIn", clockedIn);
        tag.putLong("LastWorkTick", lastWorkTick);
        tag.putInt("JobXp", jobXp);
        tag.putString("StatusMessage", statusMessage);
        tag.putString("SelectedRecipeId", selectedRecipeId);
        tag.putInt("StockTarget", stockTarget);
        tag.putBoolean("MinerTunnelMode", minerTunnelMode);
        tag.putString("TunnelDirection", tunnelDirection.getName());
        return tag;
    }

    public void load(CompoundTag tag) {
        if (tag == null) return;
        this.job = CatJob.fromId(tag.getString("Job"));
        this.stationPos = tag.contains("StationPos") ? NbtUtils.readBlockPos(tag.getCompound("StationPos")) : null;
        if (tag.contains("WorkArea")) {
            int[] arr = tag.getIntArray("WorkArea");
            if (arr.length == 6) {
                this.workArea = new BoundingBox(arr[0], arr[1], arr[2], arr[3], arr[4], arr[5]);
            }
        } else {
            this.workArea = null;
        }
        linkedInputs.clear();
        if (tag.contains("LinkedInputs", Tag.TAG_LIST)) {
            ListTag inList = tag.getList("LinkedInputs", Tag.TAG_COMPOUND);
            for (int i = 0; i < inList.size(); i++) {
                linkedInputs.add(NbtUtils.readBlockPos(inList.getCompound(i)));
            }
        }
        linkedOutputs.clear();
        if (tag.contains("LinkedOutputs", Tag.TAG_LIST)) {
            ListTag outList = tag.getList("LinkedOutputs", Tag.TAG_COMPOUND);
            for (int i = 0; i < outList.size(); i++) {
                linkedOutputs.add(NbtUtils.readBlockPos(outList.getCompound(i)));
            }
        }
        if (tag.contains("Filter", Tag.TAG_COMPOUND)) {
            filter.load(tag.getCompound("Filter"));
        }
        this.clockedIn = !tag.contains("ClockedIn") || tag.getBoolean("ClockedIn");
        this.lastWorkTick = tag.getLong("LastWorkTick");
        this.jobXp = tag.getInt("JobXp");
        this.statusMessage = tag.contains("StatusMessage") ? tag.getString("StatusMessage") : "Idle";
        this.selectedRecipeId = tag.getString("SelectedRecipeId");
        this.stockTarget = tag.contains("StockTarget") ? Math.max(1, tag.getInt("StockTarget")) : 64;
        this.minerTunnelMode = tag.getBoolean("MinerTunnelMode");
        Direction d = Direction.byName(tag.getString("TunnelDirection"));
        this.tunnelDirection = (d != null && d.getAxis().isHorizontal()) ? d : Direction.NORTH;
    }
}
