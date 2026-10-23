package com.kittytalents.client.renderer;

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

public class TalentedCatPackLayer extends RenderLayer<TalentedCat, TalentedCatModel> {
    private static final ResourceLocation PACK_TEX = new ResourceLocation("kittytalents", "textures/models/accessories/cat_backpack.png");
    private final ModelPart packPart;

    public TalentedCatPackLayer(RenderLayerParent<TalentedCat, TalentedCatModel> parent) {
        super(parent);
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Saddlebag backpack fitted to feline torso:
        // Transverse leather harness strap across the back, twin side pouches on the flanks, and rolled canvas bedroll
        root.addOrReplaceChild("pack", CubeListBuilder.create()
                // Top harness strap across the back
                .texOffs(0, 24).addBox(-2.2F, 5.0F, -1.85F, 4.4F, 2.5F, 0.4F)
                // Left flank saddlebag pouch
                .texOffs(0, 0).addBox(-3.25F, 5.0F, -6.5F, 1.25F, 4.5F, 4.2F)
                // Left pouch flap & buckle
                .texOffs(0, 9).addBox(-3.35F, 4.8F, -6.2F, 1.35F, 2.0F, 3.6F)
                // Right flank saddlebag pouch
                .texOffs(16, 0).addBox(2.0F, 5.0F, -6.5F, 1.25F, 4.5F, 4.2F)
                // Right pouch flap & buckle
                .texOffs(16, 9).addBox(2.0F, 4.8F, -6.2F, 1.35F, 2.0F, 3.6F)
                // Rolled canvas bedroll on top harness
                .texOffs(0, 16).addBox(-1.8F, 5.2F, -1.45F, 3.6F, 2.0F, 1.6F),
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
            this.getParentModel().getBodyPart().translateAndRotate(poseStack);
            VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(PACK_TEX));
            this.packPart.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
            poseStack.popPose();
        }
    }
}
