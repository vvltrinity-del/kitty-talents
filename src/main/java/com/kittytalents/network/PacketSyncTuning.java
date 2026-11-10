package com.kittytalents.network;

import com.kittytalents.core.tuning.Tuning;
import net.minecraft.network.FriendlyByteBuf;

import java.util.Map;

/** Server -> client: datapack tuning overrides, so tooltips and client-side predictions use the server's numbers. */
public class PacketSyncTuning extends PacketBase {
    private final Map<String, Double> values;

    public PacketSyncTuning() {
        this.values = Tuning.overrides();
    }

    public PacketSyncTuning(FriendlyByteBuf buf) {
        this.values = Tuning.read(buf);
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        Tuning.write(buf, values);
    }

    @Override
    protected void handleClient() {
        Tuning.setOverrides(values);
    }
}
