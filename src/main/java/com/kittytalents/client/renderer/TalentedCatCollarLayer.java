package com.kittytalents.client.renderer;

import com.kittytalents.entity.TalentedCat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.CatModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;

public class TalentedCatCollarLayer extends RenderLayer<TalentedCat, TalentedCatModel> {
    private static final ResourceLocation CAT_COLLAR_LOCATION = new ResourceLocation("textures/entity/cat/cat_collar.png");
    private final CatModel<TalentedCat> collarModel;

    public TalentedCatCollarLayer(RenderLayerParent<TalentedCat, TalentedCatModel> parent, EntityModelSet modelSet) {
        super(parent);
        this.collarModel = new CatModel<>(modelSet.bakeLayer(ModelLayers.CAT_COLLAR));
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, TalentedCat cat, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.getCameraEntity() == cat && mc.options.getCameraType().isFirstPerson()) return;

        if (cat.isTame() && !cat.isInvisible()) {
            float[] color = cat.getCollarColor().getTextureDiffuseColors();
            coloredCutoutModelCopyLayerRender(
                    this.getParentModel(),
                    this.collarModel,
                    CAT_COLLAR_LOCATION,
                    poseStack,
                    buffer,
                    packedLight,
                    cat,
                    limbSwing,
                    limbSwingAmount,
                    ageInTicks,
                    netHeadYaw,
                    headPitch,
                    partialTicks,
                    color[0], color[1], color[2]
            );
        }
    }
}
