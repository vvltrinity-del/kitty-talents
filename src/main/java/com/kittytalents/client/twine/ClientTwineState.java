package com.kittytalents.client.twine;

import com.kittytalents.entity.TalentedCat;
import com.kittytalents.network.PacketHandler;
import com.kittytalents.network.PacketTwineAction;
import com.kittytalents.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;

/**
 * Client-side Twine state: which cat (if any) this client is twined with, and the player's orientation from before
 * the session so it can be put back. Renderers and HUDs read this; {@link TwineCamera} and {@link TwineInput} drive it.
 * <p>
 * A session starts and ends only on the server's word ({@link #handleSync}), except for the local safety exits in
 * {@link #endLocally}: the world closed, the cat is gone or downed, or the cat stopped reporting itself as twined.
 */
public final class ClientTwineState {
    /** Ticks to wait for the cat's synced "twined" flag before deciding the server ended the session. */
    static final int DESYNC_GRACE_TICKS = 40;

    private static int activeCatId = -1;
    private static int desyncTicks = 0;

    // Player orientation from before the session, restored when it ends.
    static float dormantYaw, dormantPitch, dormantHeadYaw, dormantBodyYaw;

    private ClientTwineState() {}

    public static boolean isTwined() {
        return activeCatId != -1;
    }

    public static int activeCatId() {
        return activeCatId;
    }

    /** Where the player's sleeping body is (the Twine leash centre). */
    public static net.minecraft.world.phys.Vec3 bodyPosition() {
        var p = Minecraft.getInstance().player;
        return p != null ? p.position() : null;
    }

    public static boolean isTwinedWith(int entityId) {
        return activeCatId != -1 && activeCatId == entityId;
    }

    /** The twined cat if it is loaded on this client, otherwise null. */
    @Nullable
    public static TalentedCat activeCat(Minecraft mc) {
        if (activeCatId == -1 || mc.level == null) return null;
        Entity e = mc.level.getEntity(activeCatId);
        return e instanceof TalentedCat cat ? cat : null;
    }

    /** Server said a session started ({@code twined}) or ended. {@code catEntityId == -1} ends whatever is active. */
    public static void handleSync(int catEntityId, boolean twined) {
        TwineCamera.registerFirstPersonModelGuard();
        Minecraft mc = Minecraft.getInstance();
        if (twined) {
            if (mc.player != null && !isTwined()) {
                dormantYaw = Mth.wrapDegrees(mc.player.getYRot());
                dormantPitch = Mth.clamp(mc.player.getXRot(), -90.0F, 90.0F);
                dormantHeadYaw = Mth.wrapDegrees(mc.player.getYHeadRot());
                dormantBodyYaw = Mth.wrapDegrees(mc.player.yBodyRot);
            }
            activeCatId = catEntityId;
            desyncTicks = 0;
            TwineCamera.reset();
            TalentedCat cat = activeCat(mc);
            if (cat != null) {
                TwineCamera.bind(mc, cat);
                cat.setMaxUpStep(1.0F);
                cat.playSound(ModSounds.TWINE_ENTER.get(), 1.0F, 1.0F);
            }
            if (mc.player != null) TwineCamera.resetPlayerMouseBaseline(mc);
        } else if (activeCatId != -1 && (catEntityId == -1 || catEntityId == activeCatId)) {
            TalentedCat cat = activeCat(mc);
            if (cat != null) {
                cat.setMaxUpStep(0.6F);
                cat.setTwineInput(0.0F, 0.0F);
                cat.playSound(ModSounds.TWINE_EXIT.get(), 0.9F, 1.0F);
            }
            end(mc);
        }
    }

    /** Ask the server to end the session. The client view switches back when the server confirms. */
    public static void requestUntwine() {
        if (activeCatId != -1) {
            PacketHandler.sendToServer(new PacketTwineAction(activeCatId, PacketTwineAction.ActionType.TWINE_STOP, 0.0F));
        }
    }

    /**
     * Ends the session on this client without waiting for the server (the server is told too, when connected).
     * Used when the cat vanished, was downed, or the connection/world is gone. Always gives the camera back.
     */
    public static void endLocally(Minecraft mc, boolean tellServer) {
        if (activeCatId == -1) return;
        if (tellServer && mc.getConnection() != null) requestUntwine();
        end(mc);
    }

    /**
     * Per-tick safety check, called by {@link TwineInput} before handling input. Returns the cat to drive this tick,
     * or null after ending the session locally.
     */
    @Nullable
    static TalentedCat validate(Minecraft mc) {
        if (activeCatId == -1) return null;
        if (mc.level == null || mc.player == null) {
            end(mc);
            return null;
        }
        TalentedCat cat = activeCat(mc);
        if (cat == null || cat.isRemoved() || !cat.isAlive() || cat.isIncapacitated() || !mc.player.isAlive()) {
            endLocally(mc, true);
            return null;
        }
        // The cat's synced flag says no session: the end packet was lost or arrived out of order.
        desyncTicks = cat.isTwined() ? 0 : desyncTicks + 1;
        if (desyncTicks > DESYNC_GRACE_TICKS) {
            endLocally(mc, true);
            return null;
        }
        return cat;
    }

    private static void end(Minecraft mc) {
        activeCatId = -1;
        desyncTicks = 0;
        TwineInput.reset();
        TwineCamera.restorePlayerView(mc);
    }
}
