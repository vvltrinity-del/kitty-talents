package com.kittytalents.client.model;

import com.kittytalents.KittyTalents;
import com.kittytalents.client.anim.CatAnimator;
import com.kittytalents.entity.TalentedCat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Kitty Talents' own cat rig. Same proportions and texture layout as the vanilla cat (so every vanilla and
 * custom 64x32 cat skin still fits), but with a real bone hierarchy:
 * <pre>
 * root
 * ├── head ── head_top (anchor), mouth (anchor)
 * ├── body ── back (anchor, shoulders), neck (anchor, chest front)
 * ├── tail_root (anchor) ── tail ── tail_tip
 * │                     └── tail_b ── tail_b_tip     (Nekomata second tail, hidden otherwise)
 * └── 4 legs
 * </pre>
 * Layers attach to {@link Anchor}s instead of hand-tuned offsets, so they follow every pose. Poses come from
 * {@link CatAnimator}. Armor and the collar reuse this skeleton with inflated cubes ({@link #createMesh}).
 */
public class KTCatModel extends EntityModel<TalentedCat> {
    public static final ModelLayerLocation MAIN = layer("main");
    public static final ModelLayerLocation COLLAR = layer("collar");
    public static final ModelLayerLocation ARMOR_OUTER = layer("armor_outer");
    public static final ModelLayerLocation ARMOR_INNER = layer("armor_inner");

    /** Named attachment points. Each is an empty bone; its parent chain gives the full transform. */
    public enum Anchor { HEAD_TOP, MOUTH, BACK, NECK, TAIL_ROOT }

    public final ModelPart root;
    public final ModelPart head;
    public final ModelPart body;
    public final ModelPart tailRoot;
    public final ModelPart tail;
    public final ModelPart tailTip;
    public final ModelPart tailB;
    public final ModelPart tailBTip;
    public final ModelPart leftHindLeg;
    public final ModelPart rightHindLeg;
    public final ModelPart leftFrontLeg;
    public final ModelPart rightFrontLeg;
    private final Map<Anchor, List<ModelPart>> anchorChains = new EnumMap<>(Anchor.class);

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(new ResourceLocation(KittyTalents.MODID, "cat"), name);
    }

    public KTCatModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.tailRoot = root.getChild("tail_root");
        this.tail = tailRoot.getChild("tail");
        this.tailTip = tail.getChild("tail_tip");
        this.tailB = tailRoot.getChild("tail_b");
        this.tailBTip = tailB.getChild("tail_b_tip");
        this.leftHindLeg = root.getChild("left_hind_leg");
        this.rightHindLeg = root.getChild("right_hind_leg");
        this.leftFrontLeg = root.getChild("left_front_leg");
        this.rightFrontLeg = root.getChild("right_front_leg");
        anchorChains.put(Anchor.HEAD_TOP, List.of(head, head.getChild("head_top")));
        anchorChains.put(Anchor.MOUTH, List.of(head, head.getChild("mouth")));
        anchorChains.put(Anchor.BACK, List.of(body, body.getChild("back")));
        anchorChains.put(Anchor.NECK, List.of(body, body.getChild("neck")));
        anchorChains.put(Anchor.TAIL_ROOT, List.of(tailRoot));
    }

    /** Which cubes the mesh carries. Anchors and bones are always present so poses can be copied 1:1. */
    public enum Shell { CAT, ARMOR }

    public static LayerDefinition createCatLayer() {
        return LayerDefinition.create(createMesh(CubeDeformation.NONE, Shell.CAT), 64, 32);
    }

    public static LayerDefinition createCollarLayer() {
        return LayerDefinition.create(createMesh(new CubeDeformation(0.01F), Shell.CAT), 64, 32);
    }

    public static LayerDefinition createArmorLayer(float inflate) {
        return LayerDefinition.create(createMesh(new CubeDeformation(inflate), Shell.ARMOR), 64, 32);
    }

    /**
     * Vanilla cat geometry and UVs (OcelotModel#createBodyMesh), re-parented. For {@link Shell#ARMOR} the head
     * loses nose and ears (a helmet is a shell around the skull) and the tails get no cubes.
     */
    public static MeshDefinition createMesh(CubeDeformation def, Shell shell) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        boolean cat = shell == Shell.CAT;

        CubeListBuilder headCubes = CubeListBuilder.create().addBox("main", -2.5F, -2.0F, -3.0F, 5.0F, 4.0F, 5.0F, def);
        if (cat) {
            headCubes.addBox("nose", -1.5F, -0.001F, -4.0F, 3, 2, 2, def, 0, 24)
                    .addBox("ear1", -2.0F, -3.0F, 0.0F, 1, 1, 2, def, 0, 10)
                    .addBox("ear2", 1.0F, -3.0F, 0.0F, 1, 1, 2, def, 6, 10);
        }
        PartDefinition head = root.addOrReplaceChild("head", headCubes, PartPose.offset(0.0F, 15.0F, -9.0F));
        // Top of the skull, centred between the ears.
        head.addOrReplaceChild("head_top", CubeListBuilder.create(), PartPose.offset(0.0F, -2.0F, -0.5F));
        // Under the front of the muzzle, where a held item's grip sits.
        head.addOrReplaceChild("mouth", CubeListBuilder.create(), PartPose.offset(0.0F, 1.6F, -3.2F));

        // Body box runs along local +Y (3 = chest, 19 = rump); local -Z is the belly (-8), -2 is the back.
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(20, 0)
                        .addBox(-2.0F, 3.0F, -8.0F, 4.0F, 16.0F, 6.0F, def),
                PartPose.offsetAndRotation(0.0F, 12.0F, -10.0F, (float) Math.PI / 2F, 0.0F, 0.0F));
        // Top of the shoulders. Local axes: +Y toward the tail, +Z out of the back, +X the cat's left.
        body.addOrReplaceChild("back", CubeListBuilder.create(), PartPose.offset(0.0F, 4.0F, -2.0F));
        // Centre of the chest front, just under the chin.
        body.addOrReplaceChild("neck", CubeListBuilder.create(), PartPose.offset(0.0F, 3.0F, -5.0F));

        PartDefinition tailRoot = root.addOrReplaceChild("tail_root", CubeListBuilder.create(), PartPose.offset(0.0F, 15.0F, 8.0F));
        CubeListBuilder tailCube = cat ? CubeListBuilder.create().texOffs(0, 15).addBox(-0.5F, 0.0F, 0.0F, 1.0F, 8.0F, 1.0F, def) : CubeListBuilder.create();
        CubeListBuilder tipCube = cat ? CubeListBuilder.create().texOffs(4, 15).addBox(-0.5F, 0.0F, 0.0F, 1.0F, 8.0F, 1.0F, new CubeDeformation(-0.02F)) : CubeListBuilder.create();
        PartDefinition tail = tailRoot.addOrReplaceChild("tail", tailCube, PartPose.rotation(0.9F, 0.0F, 0.0F));
        tail.addOrReplaceChild("tail_tip", tipCube, PartPose.offsetAndRotation(0.0F, 8.0F, 0.0F, 0.83F, 0.0F, 0.0F));
        PartDefinition tailB = tailRoot.addOrReplaceChild("tail_b", tailCube, PartPose.rotation(0.9F, 0.0F, 0.0F));
        tailB.addOrReplaceChild("tail_b_tip", tipCube, PartPose.offsetAndRotation(0.0F, 8.0F, 0.0F, 0.83F, 0.0F, 0.0F));

        CubeListBuilder hind = CubeListBuilder.create().texOffs(8, 13).addBox(-1.0F, 0.0F, 1.0F, 2.0F, 6.0F, 2.0F, def);
        root.addOrReplaceChild("left_hind_leg", hind, PartPose.offset(1.1F, 18.0F, 5.0F));
        root.addOrReplaceChild("right_hind_leg", hind, PartPose.offset(-1.1F, 18.0F, 5.0F));
        CubeListBuilder front = CubeListBuilder.create().texOffs(40, 0).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 10.0F, 2.0F, def);
        root.addOrReplaceChild("left_front_leg", front, PartPose.offset(1.2F, 14.1F, -5.0F));
        root.addOrReplaceChild("right_front_leg", front, PartPose.offset(-1.2F, 14.1F, -5.0F));
        return mesh;
    }

    @Override
    public void prepareMobModel(TalentedCat cat, float limbSwing, float limbSwingAmount, float partialTick) {
        CatAnimator.prepare(this, cat, partialTick);
    }

    @Override
    public void setupAnim(TalentedCat cat, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        CatAnimator.animate(this, cat, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int light, int overlay, float r, float g, float b, float a) {
        root.render(poseStack, buffer, light, overlay, r, g, b, a);
    }

    /** Moves the pose stack into the anchor's space (1 unit = 1 block; the anchor's local axes, see {@link #createMesh}). */
    public void translateToAnchor(Anchor anchor, PoseStack poseStack) {
        for (ModelPart part : anchorChains.get(anchor)) part.translateAndRotate(poseStack);
    }

    /** Copies every bone's pose (position, rotation, visibility) onto a model built from the same skeleton. */
    public void copyPoseTo(KTCatModel other) {
        other.attackTime = this.attackTime;
        other.riding = this.riding;
        other.young = this.young;
        copy(head, other.head);
        copy(body, other.body);
        copy(tailRoot, other.tailRoot);
        copy(tail, other.tail);
        copy(tailTip, other.tailTip);
        copy(tailB, other.tailB);
        copy(tailBTip, other.tailBTip);
        copy(leftHindLeg, other.leftHindLeg);
        copy(rightHindLeg, other.rightHindLeg);
        copy(leftFrontLeg, other.leftFrontLeg);
        copy(rightFrontLeg, other.rightFrontLeg);
    }

    private static void copy(ModelPart from, ModelPart to) {
        to.copyFrom(from);
        to.visible = from.visible;
    }

    /** Shows only the bones in {@code parts} (and their children); used by armor slots. */
    public void showOnly(ModelPart... parts) {
        for (ModelPart p : new ModelPart[]{head, body, tailRoot, leftHindLeg, rightHindLeg, leftFrontLeg, rightFrontLeg}) p.visible = false;
        for (ModelPart p : parts) p.visible = true;
    }

    // Compatibility accessors for layers that still address bones directly.
    public ModelPart getHeadPart() { return head; }
    public ModelPart getBodyPart() { return body; }
}
