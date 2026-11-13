package com.kittytalents.entity;

import com.kittytalents.config.KittyTalentsConfig;
import com.kittytalents.talent.Talents;
import net.minecraft.world.phys.Vec3;

/**
 * Sky Feline flight, in one place (playtest fix). Called from TalentedCat#travel, which runs on the server and, for a
 * Twined cat, on the controlling client too, so prediction and server agree and there is no rubber-banding.
 *
 * <p>The old version stacked jump impulses while Space was held (slow climb), and the glide multiplied horizontal speed
 * by up to 1.25 every tick while falling, with no cap: the cat accelerated without limit, shot out of the area loaded
 * around the player's body and the Twine session dropped. AI cats were flung around the same way after any jump.</p>
 */
public final class SkyFlight {
    private SkyFlight() {}

    /** Horizontal air acceleration while Twined (terminal speed is about 11x this with air drag). */
    public static float airAcceleration(TalentedCat cat) {
        int rank = cat.getTalentLevel(Talents.SKY_FELINE);
        float a = 0.04F + 0.005F * rank;
        return cat.isSprinting() ? a * 1.35F : a;
    }

    private static double flightMult() {
        return KittyTalentsConfig.COMMON.flightSpeedMultiplier.get();
    }

    /** Twined flight: hold jump to climb at a steady rate, release to glide down slowly. */
    public static void twined(TalentedCat cat, boolean jumpHeld) {
        int rank = cat.getTalentLevel(Talents.SKY_FELINE);
        if (rank <= 0 || cat.isInWater() || cat.isInLava()) return;
        Vec3 m = cat.getDeltaMovement();
        if (jumpHeld) {
            double climb = (0.30D + 0.03D * rank) * flightMult();
            cat.setDeltaMovement(m.x, Math.max(m.y, climb), m.z);
            cat.hasImpulse = true;
            cat.resetFallDistance();
        } else if (!cat.onGround()) {
            float pitch = cat.getXRot();
            double sink = 0.10D - 0.01D * rank;
            if (pitch > 10.0F) {
                // Pitching down allows diving: up to 0.75D sink speed at 90 degrees down
                double divePitch = Math.sin(Math.toRadians(pitch));
                double diveSink = sink + (0.65D * divePitch);
                if (m.y < -diveSink) {
                    cat.setDeltaMovement(m.x, -diveSink, m.z);
                }
                // Forward momentum boost when diving
                Vec3 look = cat.getLookAngle();
                double diveForward = 0.04D * divePitch;
                cat.setDeltaMovement(cat.getDeltaMovement().add(look.x * diveForward, 0, look.z * diveForward));
            } else {
                if (m.y < -sink) cat.setDeltaMovement(m.x, -sink, m.z);
            }
            cat.resetFallDistance();
        }
    }

    /** AI cats: a gentle glide on real drops only (not jumps or pounce arcs). Never adds horizontal speed. */
    public static void aiGlide(TalentedCat cat) {
        int rank = cat.getTalentLevel(Talents.SKY_FELINE);
        if (rank <= 0 || cat.isTwined() || cat.onGround() || cat.isInWater() || cat.fallDistance < 3.0F) return;
        // Ridden: the rider's client drives the fall (TalentedCat#travel). A server-side glide on top pushed the
        // cat out of sync with its rider, who then sank halfway into it while falling (5.0 fix).
        if (cat.getControllingPassenger() != null) return;
        Vec3 m = cat.getDeltaMovement();
        double sink = 0.16D - 0.015D * rank;
        if (m.y < -sink) {
            cat.setDeltaMovement(m.x, -sink, m.z);
            cat.hasImpulse = true;
        }
    }
}
