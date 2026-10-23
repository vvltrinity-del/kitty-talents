package com.kittytalents.client.renderer;

import com.kittytalents.entity.TalentedCat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class TalentedCatWeaponLayer extends RenderLayer<TalentedCat, TalentedCatModel> {
    public TalentedCatWeaponLayer(RenderLayerParent<TalentedCat, TalentedCatModel> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, TalentedCat cat, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (cat.isInvisible()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.getCameraEntity() == cat && mc.options.getCameraType().isFirstPerson()) return;

        // Render melee weapon in mouth (TacZ guns are rendered on the back harness by TalentedCatGunHarnessLayer)
        ItemStack weapon = cat.getEquippedWeapon();
        if (!weapon.isEmpty() && !com.kittytalents.compat.tacz.TacZCompat.isGun(weapon)) {
            poseStack.pushPose();
            this.getParentModel().getHeadPart().translateAndRotate(poseStack);
            poseStack.translate(0.0D, 0.15D, -0.4D);
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
            poseStack.scale(0.6F, 0.6F, 0.6F);

            Minecraft.getInstance().getItemRenderer().renderStatic(
                    weapon,
                    ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                    packedLight,
                    net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                    poseStack,
                    buffer,
                    cat.level(),
                    cat.getId()
            );

            poseStack.popPose();
        }

        // Render tool: if no weapon in mouth; otherwise holstered on cat's side
        ItemStack tool = cat.getEquippedTool();
        if (!tool.isEmpty()) {
            poseStack.pushPose();
            if (weapon.isEmpty()) {
                this.getParentModel().getHeadPart().translateAndRotate(poseStack);
                poseStack.translate(0.0D, 0.15D, -0.4D);
                poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
                poseStack.scale(0.6F, 0.6F, 0.6F);
            } else {
                this.getParentModel().getBodyPart().translateAndRotate(poseStack);
                poseStack.translate(0.18D, 0.1D, 0.0D);
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
                poseStack.mulPose(Axis.XP.rotationDegrees(-45.0F));
                poseStack.scale(0.5F, 0.5F, 0.5F);
            }

            Minecraft.getInstance().getItemRenderer().renderStatic(
                    tool,
                    ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                    packedLight,
                    net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                    poseStack,
                    buffer,
                    cat.level(),
                    cat.getId()
            );

            poseStack.popPose();
        }
    }
}
