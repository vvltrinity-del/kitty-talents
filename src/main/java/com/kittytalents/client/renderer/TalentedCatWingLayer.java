package com.kittytalents.client.renderer;

import com.kittytalents.client.model.KTCatModel;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.registry.ModItems;
import com.kittytalents.talent.Talents;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ElytraModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class TalentedCatWingLayer extends RenderLayer<TalentedCat, KTCatModel> {
    private static final ResourceLocation ELYTRA_LOCATION = new ResourceLocation("textures/entity/elytra.png");
    private static final ResourceLocation ANGEL_WINGS_LOCATION = new ResourceLocation("kittytalents", "textures/models/accessories/angel_wings.png");
    private final ElytraModel<TalentedCat> elytraModel;

    public TalentedCatWingLayer(RenderLayerParent<TalentedCat, KTCatModel> parent, EntityModelSet modelSet) {
        super(parent);
        this.elytraModel = new ElytraModel<>(modelSet.bakeLayer(ModelLayers.ELYTRA));
    }

    private boolean isAngelWings(TalentedCat cat) {
        if (cat.getEquippedAccessory().is(ModItems.ANGEL_WINGS.get())) return true;
        if (cat.isCosmeticsVisible()) {
            for (int i = 0; i < 3; i++) {
                if (cat.getCosmeticSlot(i).is(ModItems.ANGEL_WINGS.get())) return true;
            }
        }
        return false;
    }

    private boolean shouldRender(TalentedCat cat) {
        if (cat.isInvisible()) return false;
        if (cat.getTalentLevel(Talents.SKY_FELINE) > 0) return true;
        if (cat.getArmorSlotItem(2).is(Items.ELYTRA)) return true;
        return isAngelWings(cat);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, TalentedCat cat, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getCameraEntity() == cat && mc.options.getCameraType().isFirstPerson()) return;

        if (!shouldRender(cat)) return;

        poseStack.pushPose();

        // Elytra space matches the BACK anchor: +Y runs down the spine, +Z points out of the back.
        this.getParentModel().translateToAnchor(KTCatModel.Anchor.BACK, poseStack);
        poseStack.scale(0.38F, 0.38F, 0.38F);

        this.getParentModel().copyPropertiesTo(this.elytraModel);
        this.elytraModel.setupAnim(cat, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        boolean isAirborne = !cat.onGround() || cat.isFallFlying() || (cat.getDeltaMovement().y > 0.05D);
        if (isAirborne) {
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(15.0F));
        }

        ResourceLocation tex = isAngelWings(cat) ? ANGEL_WINGS_LOCATION : ELYTRA_LOCATION;
        ItemStack chestItem = cat.getArmorSlotItem(2);
        boolean hasGlint = chestItem.is(Items.ELYTRA) && chestItem.hasFoil();

        VertexConsumer vertexConsumer = ItemRenderer.getArmorFoilBuffer(buffer, RenderType.armorCutoutNoCull(tex), false, hasGlint);
        this.elytraModel.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);

        poseStack.popPose();
    }
}
