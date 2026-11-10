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

    /** Feathered wing models (4.0 Phase 2b); loaded on first use, rebuilt with the layer on resource reload. */
    private com.kittytalents.client.model.AccessoryModel angelWings, skyWings;
    private boolean loaded;

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, TalentedCat cat, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getCameraEntity() == cat && mc.options.getCameraType().isFirstPerson()) return;
        if (!shouldRender(cat)) return;
        if (!loaded) {
            loaded = true;
            angelWings = com.kittytalents.client.model.AccessoryModel.load("angel_wings");
            skyWings = com.kittytalents.client.model.AccessoryModel.load("sky_feline_wings");
        }

        poseStack.pushPose();
        this.getParentModel().translateToAnchor(KTCatModel.Anchor.BACK, poseStack);
        com.kittytalents.client.model.AccessoryModel wings = isAngelWings(cat) ? angelWings
                : cat.getTalentLevel(Talents.SKY_FELINE) > 0 ? skyWings : null;
        if (wings != null) {
            wings.render(poseStack, buffer, packedLight, cat, limbSwingAmount, ageInTicks);
        } else {
            // A real Elytra in the chest slot keeps the vanilla elytra look.
            poseStack.scale(0.38F, 0.38F, 0.38F);
            this.getParentModel().copyPropertiesTo(this.elytraModel);
            this.elytraModel.setupAnim(cat, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            ItemStack chestItem = cat.getArmorSlotItem(2);
            VertexConsumer vc = ItemRenderer.getArmorFoilBuffer(buffer, RenderType.armorCutoutNoCull(VANILLA_ELYTRA), false, chestItem.hasFoil());
            this.elytraModel.renderToBuffer(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        }
        poseStack.popPose();
    }

    private static final ResourceLocation VANILLA_ELYTRA = new ResourceLocation("textures/entity/elytra.png");
}
