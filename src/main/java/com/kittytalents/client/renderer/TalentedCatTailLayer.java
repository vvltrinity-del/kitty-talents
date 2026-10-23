package com.kittytalents.client.renderer;

import com.kittytalents.entity.CatBreedingManager;
import com.kittytalents.entity.TalentedCat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class TalentedCatTailLayer extends RenderLayer<TalentedCat, TalentedCatModel> {
    private final TalentedCatModel secondTailModel;

    public TalentedCatTailLayer(RenderLayerParent<TalentedCat, TalentedCatModel> parent, EntityModelSet modelSet) {
        super(parent);
        this.secondTailModel = new TalentedCatModel(modelSet.bakeLayer(ModelLayers.CAT));
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, TalentedCat cat,
                       float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getCameraEntity() == cat && mc.options.getCameraType().isFirstPerson()) return;

        if (cat.getMutation() == CatBreedingManager.MutationType.NEKOMATA && !cat.isInvisible()) {
            // Synchronize model state, animation, and sitting/standing bone transforms
            this.getParentModel().copyPropertiesTo(this.secondTailModel);
            this.secondTailModel.prepareMobModel(cat, limbSwing, limbSwingAmount, partialTicks);
            this.secondTailModel.setupAnim(cat, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            this.secondTailModel.getTail1().copyFrom(this.getParentModel().getTail1());
            this.secondTailModel.getTail2().copyFrom(this.getParentModel().getTail2());

            ResourceLocation texture = this.getTextureLocation(cat);
            VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(texture));

            // Authoritative tail base pivot sampled directly from the cat's parent model
            float px = this.getParentModel().getTail1().x / 16.0F;
            float py = this.getParentModel().getTail1().y / 16.0F;
            float pz = this.getParentModel().getTail1().z / 16.0F;
            boolean isSitting = cat.isInSittingPose();

            float wag = (float) Math.sin(ageInTicks * 0.22D) * 5.0F;

            // Symmetrical Twin Tails (Left & Right) physically attached to rear pivot
            // Left Tail
            poseStack.pushPose();
            poseStack.translate(px, py, pz);
            poseStack.translate(0.025D, 0.0D, 0.0D); // Anchored slightly to left of tail root
            if (isSitting) {
                poseStack.mulPose(Axis.ZP.rotationDegrees(22.0F + wag * 0.4F));
                poseStack.mulPose(Axis.YP.rotationDegrees(12.0F));
            } else {
                poseStack.mulPose(Axis.YP.rotationDegrees(16.0F + wag));
            }
            poseStack.translate(-px, -py, -pz);
            this.secondTailModel.renderSecondTail(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
            poseStack.popPose();

            // Right Tail (Perfect symmetry)
            poseStack.pushPose();
            poseStack.translate(px, py, pz);
            poseStack.translate(-0.025D, 0.0D, 0.0D); // Anchored slightly to right of tail root
            if (isSitting) {
                poseStack.mulPose(Axis.ZP.rotationDegrees(-22.0F - wag * 0.4F));
                poseStack.mulPose(Axis.YP.rotationDegrees(-12.0F));
            } else {
                poseStack.mulPose(Axis.YP.rotationDegrees(-16.0F - wag));
            }
            poseStack.translate(-px, -py, -pz);
            this.secondTailModel.renderSecondTail(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
            poseStack.popPose();
        }
    }
}
