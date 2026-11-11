package com.kittytalents.client.twine;

import com.kittytalents.config.KittyTalentsClientConfig;
import com.kittytalents.network.PacketSyncTwineBodies;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * What other players see of a Twine session (4.0 Phase 7): a small label over the Twined cat ("name is here") and
 * one where the player's body rests. You never see markers for your own session.
 */
public final class TwineMarkers {
    private TwineMarkers() {}

    public static boolean enabled() {
        return KittyTalentsClientConfig.SHOW_TWINE_MARKERS.get();
    }

    /** The name of the player Twined with this cat, if it is someone else. */
    @Nullable
    public static String twinerOf(UUID catId) {
        UUID player = PacketSyncTwineBodies.CLIENT_CAT_TO_PLAYER.get(catId);
        Minecraft mc = Minecraft.getInstance();
        if (player == null || mc.player == null || player.equals(mc.player.getUUID()) || mc.getConnection() == null) return null;
        PlayerInfo info = mc.getConnection().getPlayerInfo(player);
        return info != null ? info.getProfile().getName() : null;
    }

    /** A camera-facing label {@code height} blocks above the entity origin the pose stack is at. */
    public static void label(PoseStack pose, MultiBufferSource buffers, Component text, float height, int light) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        pose.pushPose();
        pose.translate(0.0F, height, 0.0F);
        pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        pose.scale(-0.02F, -0.02F, 0.02F);
        Matrix4f m = pose.last().pose();
        float x = -font.width(text) / 2.0F;
        int bg = (int) (mc.options.getBackgroundOpacity(0.25F) * 255.0F) << 24;
        font.drawInBatch(text, x, 0, 0xFF67E8F9, false, m, buffers, Font.DisplayMode.NORMAL, bg, light);
        pose.popPose();
    }
}
