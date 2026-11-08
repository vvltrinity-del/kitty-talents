package com.kittytalents.client.renderer;

import com.kittytalents.client.model.KTCatModel;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.talent.Talents;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class TalentedCatPackLayer extends RenderLayer<TalentedCat, KTCatModel> {
    private static final ResourceLocation PACK_TEX = new ResourceLocation("kittytalents", "textures/models/accessories/cat_backpack.png");
    private final ModelPart packPart;

    public TalentedCatPackLayer(RenderLayerParent<TalentedCat, KTCatModel> parent) {
        super(parent);
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Saddlebags in BACK-anchor space (+Y toward the tail, +Z out of the back, X = flank side; the torso is
        // 4 wide and 6 deep). Strap and bedroll lie on the back; each pouch sits flush against its flank.
        root.addOrReplaceChild("pack", CubeListBuilder.create()
                .texOffs(0, 24).addBox(-2.2F, 1.5F, 0.0F, 4.4F, 2.5F, 0.4F)
                .texOffs(0, 0).addBox(-3.1F, 1.2F, -3.6F, 1.1F, 3.4F, 3.4F)
                .texOffs(0, 9).addBox(-3.25F, 1.0F, -0.8F, 1.25F, 3.8F, 1.2F)
                .texOffs(16, 0).addBox(2.0F, 1.2F, -3.6F, 1.1F, 3.4F, 3.4F)
                .texOffs(16, 9).addBox(2.0F, 1.0F, -0.8F, 1.25F, 3.8F, 1.2F)
                .texOffs(0, 16).addBox(-1.8F, 1.7F, 0.4F, 3.6F, 2.0F, 1.4F),
                PartPose.ZERO);

        ModelPart baked = root.bake(32, 32);
        this.packPart = baked.getChild("pack");
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, TalentedCat cat,
                       float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (cat.isInvisible()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.getCameraEntity() == cat && mc.options.getCameraType().isFirstPerson()) return;

        // Render backpack when cat has Kitty Pack talent or inventory storage
        if (cat.getTalentLevel(Talents.KITTY_PACK) > 0 || (cat.getCatPack() != null && cat.getPackSlotCount() > 0)) {
            poseStack.pushPose();
            this.getParentModel().translateToAnchor(KTCatModel.Anchor.BACK, poseStack);
            VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(PACK_TEX));
            this.packPart.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
            poseStack.popPose();
        }
    }
}
