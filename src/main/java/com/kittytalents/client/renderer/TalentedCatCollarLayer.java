package com.kittytalents.client.renderer;

import com.kittytalents.client.model.KTCatModel;
import com.kittytalents.entity.TalentedCat;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;

/** Vanilla collar texture on a slightly inflated copy of the cat rig, posed by copying bones (no second animation pass). */
public class TalentedCatCollarLayer extends RenderLayer<TalentedCat, KTCatModel> {
    private static final ResourceLocation CAT_COLLAR_LOCATION = new ResourceLocation("textures/entity/cat/cat_collar.png");
    private final KTCatModel collarModel;

    public TalentedCatCollarLayer(RenderLayerParent<TalentedCat, KTCatModel> parent, EntityModelSet modelSet) {
        super(parent);
        this.collarModel = new KTCatModel(modelSet.bakeLayer(KTCatModel.COLLAR));
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, TalentedCat cat, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.getCameraEntity() == cat && mc.options.getCameraType().isFirstPerson()) return;
        if (!cat.isTame() || cat.isInvisible()) return;

        float[] color = cat.getCollarColor().getTextureDiffuseColors();
        this.getParentModel().copyPoseTo(this.collarModel);
        this.collarModel.renderToBuffer(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(CAT_COLLAR_LOCATION)), packedLight,
                LivingEntityRenderer.getOverlayCoords(cat, 0.0F), color[0], color[1], color[2], 1.0F);
    }
}
