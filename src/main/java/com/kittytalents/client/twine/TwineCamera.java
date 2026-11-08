package com.kittytalents.client.twine;

import com.kittytalents.entity.TalentedCat;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * The Twine camera: the view moves into the cat, and mouse look turns the cat instead of the player.
 * <p>
 * Mouse input still lands on the local player's rotation, so every frame the player's rotation is read as a delta,
 * added to the continuous (unwrapped) {@link #yaw} and clamped {@link #pitch}, and reset to (0, 0). Keeping the cat's
 * yRot and yRotO in step stops Camera.setup() spinning across -180/+180 or rubber-banding.
 */
@Mod.EventBusSubscriber(modid = "kittytalents", value = Dist.CLIENT)
public final class TwineCamera {
    private static float yaw = 0.0F;
    private static float pitch = 0.0F;
    private static boolean anglesInitialized = false;
    private static boolean firstPersonCompatRegistered = false;

    private TwineCamera() {}

    static float yaw() {
        return yaw;
    }

    static float pitch() {
        return pitch;
    }

    static void reset() {
        anglesInitialized = false;
    }

    /** Points the camera at the cat (once per session takes the cat's angles as the start). */
    static void bind(Minecraft mc, TalentedCat cat) {
        if (!anglesInitialized) {
            yaw = cat.getYRot();
            pitch = Mth.clamp(cat.getXRot(), -89.0F, 89.0F);
            syncCatRotation(cat, yaw, pitch);
            anglesInitialized = true;
        }
        if (mc.getCameraEntity() != cat) mc.setCameraEntity(cat);
    }

    static void syncCatRotation(TalentedCat cat, float y, float p) {
        cat.setYRot(y);
        cat.yRotO = y;
        cat.setXRot(p);
        cat.xRotO = p;
        cat.setYHeadRot(y);
        cat.yHeadRotO = y;
        cat.setYBodyRot(y);
        cat.yBodyRotO = y;
    }

    /** The player's rotation is the mouse-delta accumulator: keep it at (0, 0) and the body facing where it was. */
    static void resetPlayerMouseBaseline(Minecraft mc) {
        if (mc.player == null) return;
        mc.player.setYRot(0.0F);
        mc.player.yRotO = 0.0F;
        mc.player.setXRot(0.0F);
        mc.player.xRotO = 0.0F;
        mc.player.yHeadRot = ClientTwineState.dormantHeadYaw;
        mc.player.yHeadRotO = ClientTwineState.dormantHeadYaw;
        mc.player.yBodyRot = ClientTwineState.dormantBodyYaw;
        mc.player.yBodyRotO = ClientTwineState.dormantBodyYaw;
    }

    /** Session over: camera back to the player, facing the way they faced before twining. */
    static void restorePlayerView(Minecraft mc) {
        anglesInitialized = false;
        if (mc.player == null) return;
        mc.setCameraEntity(mc.player);
        mc.player.setYRot(ClientTwineState.dormantYaw);
        mc.player.yRotO = ClientTwineState.dormantYaw;
        mc.player.setXRot(ClientTwineState.dormantPitch);
        mc.player.xRotO = ClientTwineState.dormantPitch;
        mc.player.yHeadRot = ClientTwineState.dormantHeadYaw;
        mc.player.yHeadRotO = ClientTwineState.dormantHeadYaw;
        mc.player.yBodyRot = ClientTwineState.dormantBodyYaw;
        mc.player.yBodyRotO = ClientTwineState.dormantBodyYaw;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (!ClientTwineState.isTwined() || mc.player == null) return;
        TalentedCat cat = ClientTwineState.activeCat(mc);
        if (cat == null) return;

        bind(mc, cat);
        float yawDelta = mc.player.getYRot();
        float pitchDelta = mc.player.getXRot();
        // Ignore anything that isn't a plausible mouse delta (e.g. a server packet setting an absolute angle).
        if (Float.isFinite(yawDelta) && Float.isFinite(pitchDelta)
                && Math.abs(yawDelta) < 120.0F && Math.abs(pitchDelta) < 120.0F
                && (Math.abs(yawDelta) > 1.0E-5F || Math.abs(pitchDelta) > 1.0E-5F)) {
            yaw += yawDelta;
            pitch = Mth.clamp(pitch + pitchDelta, -89.0F, 89.0F);
        }
        syncCatRotation(cat, yaw, pitch);
        resetPlayerMouseBaseline(mc);

        event.setYaw(yaw);
        event.setPitch(pitch);
        event.setRoll(0.0F);
    }

    /**
     * If FirstPersonModel (tr7zw.firstperson) is installed, register an ActivationHandler that turns off its body
     * offset and forced player render while twined.
     */
    static void registerFirstPersonModelGuard() {
        if (firstPersonCompatRegistered) return;
        firstPersonCompatRegistered = true;
        try {
            Class<?> apiClass = Class.forName("dev.tr7zw.firstperson.api.FirstPersonAPI");
            Class<?> handlerInterface = Class.forName("dev.tr7zw.firstperson.api.ActivationHandler");
            Object proxy = Proxy.newProxyInstance(handlerInterface.getClassLoader(), new Class<?>[]{handlerInterface},
                    (Object obj, Method method, Object[] args) -> "preventFirstperson".equals(method.getName()) && ClientTwineState.isTwined());
            Method registerMethod = apiClass.getMethod("registerPlayerHandler", Object.class);
            registerMethod.invoke(null, proxy);
        } catch (Throwable ignored) {
            // firstperson-forge not installed
        }
    }
}
