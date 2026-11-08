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

        if (cat.isInSittingPose()) {
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
