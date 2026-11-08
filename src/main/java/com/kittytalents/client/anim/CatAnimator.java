package com.kittytalents.client.anim;

import com.kittytalents.client.model.KTCatModel;
import com.kittytalents.entity.CatBreedingManager;
import com.kittytalents.entity.TalentedCat;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Hand-written pose/animation driver for {@link KTCatModel} (no GeckoLib, per the 3.0 decision).
 * Base poses match the vanilla cat exactly (crouch, walk, sprint, sit, lie down, relax), translated to the
 * hierarchical tail; on top of that it adds an idle tail sway and the Nekomata twin-tail splay, which is a
 * mirror image around the tail root in every pose.
 *
 * <p>One-shot moves (Phase 5, R32) are layered on top of the base pose, blended in and out over 3 ticks:
 * pounce leap, dodge, ability cast and grooming, started by entity events from the server
 * ({@link TalentedCat#playAnimation}). Stalking uses the synced crouch pose and sleeping the vanilla lie-down.</p>
 */
public final class CatAnimator {
    private static final float PI = (float) Math.PI;
    private static final float TAIL_TIP_STAND = 1.7278761F;
    /** Nekomata: each tail turns this far out from the spine, mirrored. */
    private static final float TWIN_SPLAY = 0.38F;
    private static final float TWIN_SPLAY_SITTING = 0.62F;
    private static final float TWIN_BASE_OFFSET = 0.45F;

    private enum State { CROUCH, WALK, SPRINT, SIT }

    private CatAnimator() {}

    /** Per-model state carried from {@link #prepare} to {@link #animate} (one model instance renders one cat at a time). */
    private static final class Frame {
        State state = State.WALK;
        float lieDown, lieDownTail, relaxOne;
        float tailAbsX, tailTipAbsX;
    }

    private static final java.util.WeakHashMap<KTCatModel, Frame> FRAMES = new java.util.WeakHashMap<>();

    private static Frame frame(KTCatModel m) {
        return FRAMES.computeIfAbsent(m, k -> new Frame());
    }

    public static void prepare(KTCatModel m, TalentedCat cat, float partialTick) {
        Frame f = frame(m);
        f.lieDown = cat.getLieDownAmount(partialTick);
        f.lieDownTail = cat.getLieDownAmountTail(partialTick);
        f.relaxOne = cat.getRelaxStateOneAmount(partialTick);

        reset(m);
        float tailRootY = 15.0F, tailRootZ = 8.0F;
        f.tailAbsX = 0.9F;
        f.tailTipAbsX = TAIL_TIP_STAND;

        if (cat.isCrouching()) {
            m.body.y += 1.0F;
            m.head.y += 2.0F;
            tailRootY += 1.0F;
            f.tailAbsX = PI / 2F;
            f.tailTipAbsX = PI / 2F;
            f.state = State.CROUCH;
        } else if (cat.isSprinting()) {
            f.tailAbsX = PI / 2F;
            f.tailTipAbsX = PI / 2F;
            f.state = State.SPRINT;
        } else {
            f.state = State.WALK;
        }

        // Lying down (asleep) overrides sitting, as in vanilla: a resting cat is ordered to sit but curls up.
        if (cat.isInSittingPose() && !cat.isLying()) {
            m.body.xRot = PI / 4F;
            m.body.y += -4.0F;
            m.body.z += 5.0F;
            m.head.y += -3.3F;
            m.head.z += 1.0F;
            tailRootY += 8.0F;
            tailRootZ += -2.0F;
            f.tailAbsX = TAIL_TIP_STAND;
            f.tailTipAbsX = 2.670354F;
            m.leftFrontLeg.xRot = -0.15707964F;
            m.leftFrontLeg.y = 16.1F;
            m.leftFrontLeg.z = -7.0F;
            m.rightFrontLeg.xRot = -0.15707964F;
            m.rightFrontLeg.y = 16.1F;
            m.rightFrontLeg.z = -7.0F;
            m.leftHindLeg.xRot = -PI / 2F;
            m.leftHindLeg.y = 21.0F;
            m.leftHindLeg.z = 1.0F;
            m.rightHindLeg.xRot = -PI / 2F;
            m.rightHindLeg.y = 21.0F;
            m.rightHindLeg.z = 1.0F;
            f.state = State.SIT;
        }
        m.tailRoot.y = tailRootY;
        m.tailRoot.z = tailRootZ;
    }

    public static void animate(KTCatModel m, TalentedCat cat, float limbSwing, float amount, float age, float netHeadYaw, float headPitch) {
        Frame f = frame(m);
        m.head.xRot = headPitch * (PI / 180F);
        m.head.yRot = netHeadYaw * (PI / 180F);

        if (f.state != State.SIT) {
            m.body.xRot = PI / 2F;
            float w = limbSwing * 0.6662F;
            if (f.state == State.SPRINT) {
                m.leftHindLeg.xRot = Mth.cos(w) * amount;
                m.rightHindLeg.xRot = Mth.cos(w + 0.3F) * amount;
                m.leftFrontLeg.xRot = Mth.cos(w + PI + 0.3F) * amount;
                m.rightFrontLeg.xRot = Mth.cos(w + PI) * amount;
                f.tailTipAbsX = TAIL_TIP_STAND + (PI / 10F) * Mth.cos(limbSwing) * amount;
            } else {
                m.leftHindLeg.xRot = Mth.cos(w) * amount;
                m.rightHindLeg.xRot = Mth.cos(w + PI) * amount;
                m.leftFrontLeg.xRot = Mth.cos(w + PI) * amount;
                m.rightFrontLeg.xRot = Mth.cos(w) * amount;
                float swing = f.state == State.WALK ? PI / 4F : 0.47123894F;
                f.tailTipAbsX = TAIL_TIP_STAND + swing * Mth.cos(limbSwing) * amount;
            }
        }

        if (f.lieDown > 0.0F) {
            m.head.zRot = rotlerp(m.head.zRot, -1.2707963F, f.lieDown);
            m.head.yRot = rotlerp(m.head.yRot, 1.2707963F, f.lieDown);
            m.leftFrontLeg.xRot = -1.2707963F;
            m.rightFrontLeg.xRot = -0.47079635F;
            m.rightFrontLeg.zRot = -0.2F;
            m.rightFrontLeg.x = -0.2F;
            m.leftHindLeg.xRot = -0.4F;
            m.rightHindLeg.xRot = 0.5F;
            m.rightHindLeg.zRot = -0.5F;
            m.rightHindLeg.x = -0.3F;
            m.rightHindLeg.y = 20.0F;
            f.tailAbsX = rotlerp(f.tailAbsX, 0.8F, f.lieDownTail);
            f.tailTipAbsX = rotlerp(f.tailTipAbsX, -0.4F, f.lieDownTail);
        }
        if (f.relaxOne > 0.0F) {
            m.head.xRot = rotlerp(m.head.xRot, -0.58177644F, f.relaxOne);
        }

        oneShot(m, cat, f, age);

        // Hierarchical tail: the tip's rotation is relative to the base segment.
        boolean twin = cat.getMutation() == CatBreedingManager.MutationType.NEKOMATA;
        boolean sitting = f.state == State.SIT;
        float idle = (1.0F - Mth.clamp(amount * 4.0F, 0.0F, 1.0F)) * (1.0F - f.lieDown);
        m.tailRoot.yRot = Mth.sin(age * 0.09F) * 0.12F * idle;
        poseTail(m.tail, m.tailTip, f);
        poseTail(m.tailB, m.tailBTip, f);
        m.tailB.visible = twin;
        if (twin) {
            float splay = sitting ? TWIN_SPLAY_SITTING : TWIN_SPLAY;
            m.tail.x = TWIN_BASE_OFFSET;
            m.tail.yRot = splay;
            m.tailB.x = -TWIN_BASE_OFFSET;
            m.tailB.yRot = -splay;
        }
    }

    /** Duration in ticks of each one-shot animation. */
    public static int duration(byte anim) {
        return switch (anim) {
            case TalentedCat.ANIM_POUNCE -> 14;
            case TalentedCat.ANIM_DODGE -> 8;
            case TalentedCat.ANIM_CAST -> 12;
            case TalentedCat.ANIM_GROOM -> 80;
            default -> 0;
        };
    }

    /** 0..1 weight: ramps in over the first 3 ticks and out over the last 3. */
    private static float weight(float t, int dur) {
        if (t < 0 || t >= dur) return 0.0F;
        float in = Mth.clamp(t / 3.0F, 0.0F, 1.0F), out = Mth.clamp((dur - t) / 3.0F, 0.0F, 1.0F);
        float w = Math.min(in, out);
        return w * w * (3.0F - 2.0F * w); // smoothstep
    }

    private static float toward(float from, float to, float w) {
        return from + (to - from) * w;
    }

    private static void oneShot(KTCatModel m, TalentedCat cat, Frame f, float age) {
        byte anim = cat.getAnimType();
        int dur = duration(anim);
        float t = age - cat.getAnimStartTick();
        float w = weight(t, dur);
        if (w <= 0.0F) return;
        float p = Mth.clamp(t / dur, 0.0F, 1.0F); // progress through the move
        switch (anim) {
            case TalentedCat.ANIM_POUNCE -> {
                // Launch nose-up, stretched out mid-air, then tip forward to land.
                float pitch = Mth.lerp(p, -0.35F, 0.25F);
                if (f.state != State.SIT) m.body.xRot += pitch * w;
                m.head.xRot = toward(m.head.xRot, pitch, w);
                m.leftFrontLeg.xRot = toward(m.leftFrontLeg.xRot, -1.3F, w);
                m.rightFrontLeg.xRot = toward(m.rightFrontLeg.xRot, -1.3F, w);
                m.leftHindLeg.xRot = toward(m.leftHindLeg.xRot, 1.1F, w);
                m.rightHindLeg.xRot = toward(m.rightHindLeg.xRot, 1.1F, w);
                f.tailAbsX = toward(f.tailAbsX, PI / 2F, w);
                f.tailTipAbsX = toward(f.tailTipAbsX, PI / 2F, w);
            }
            case TalentedCat.ANIM_DODGE -> {
                // Legs tucked under, head ducked and tilted, tail whipping.
                m.leftFrontLeg.xRot = toward(m.leftFrontLeg.xRot, -0.7F, w);
                m.rightFrontLeg.xRot = toward(m.rightFrontLeg.xRot, -0.7F, w);
                m.leftHindLeg.xRot = toward(m.leftHindLeg.xRot, 0.7F, w);
                m.rightHindLeg.xRot = toward(m.rightHindLeg.xRot, 0.7F, w);
                m.head.xRot = toward(m.head.xRot, 0.35F, w);
                m.head.zRot = toward(m.head.zRot, 0.3F, w);
                m.tailRoot.yRot += Mth.sin(t * 1.4F) * 0.6F * w;
            }
            case TalentedCat.ANIM_CAST -> {
                // Head up, one paw raised, tail straight up.
                m.head.xRot = toward(m.head.xRot, -0.5F, w);
                m.rightFrontLeg.xRot = toward(m.rightFrontLeg.xRot, -1.4F, w);
                f.tailAbsX = toward(f.tailAbsX, 2.3F, w);
                f.tailTipAbsX = toward(f.tailTipAbsX, 2.9F, w);
            }
            case TalentedCat.ANIM_GROOM -> {
                // Classic cat groom: head turned round and down to lick the shoulder, one paw lifted slightly.
                float lick = Mth.sin(age * 0.6F) * 0.15F;
                m.head.yRot = toward(m.head.yRot, -1.0F, w);
                m.head.xRot = toward(m.head.xRot, 0.65F + lick, w);
                m.head.zRot = toward(m.head.zRot, -0.25F, w);
                m.rightFrontLeg.xRot = toward(m.rightFrontLeg.xRot, m.rightFrontLeg.xRot - 0.35F, w);
            }
            default -> {}
        }
    }

    private static void poseTail(ModelPart base, ModelPart tip, Frame f) {
        base.x = 0.0F;
        base.yRot = 0.0F;
        base.xRot = f.tailAbsX;
        tip.xRot = f.tailTipAbsX - f.tailAbsX;
    }

    private static void reset(KTCatModel m) {
        m.head.setPos(0.0F, 15.0F, -9.0F);
        m.head.setRotation(0.0F, 0.0F, 0.0F);
        m.body.setPos(0.0F, 12.0F, -10.0F);
        m.body.setRotation(PI / 2F, 0.0F, 0.0F);
        m.tailRoot.setPos(0.0F, 15.0F, 8.0F);
        m.tailRoot.setRotation(0.0F, 0.0F, 0.0F);
        m.leftFrontLeg.setPos(1.2F, 14.1F, -5.0F);
        m.leftFrontLeg.setRotation(0.0F, 0.0F, 0.0F);
        m.rightFrontLeg.setPos(-1.2F, 14.1F, -5.0F);
        m.rightFrontLeg.setRotation(0.0F, 0.0F, 0.0F);
        m.leftHindLeg.setPos(1.1F, 18.0F, 5.0F);
        m.leftHindLeg.setRotation(0.0F, 0.0F, 0.0F);
        m.rightHindLeg.setPos(-1.1F, 18.0F, 5.0F);
        m.rightHindLeg.setRotation(0.0F, 0.0F, 0.0F);
    }

    private static float rotlerp(float from, float to, float t) {
        float d = to - from;
        while (d < -PI) d += 2F * PI;
        while (d >= PI) d -= 2F * PI;
        return from + t * d;
    }
}
