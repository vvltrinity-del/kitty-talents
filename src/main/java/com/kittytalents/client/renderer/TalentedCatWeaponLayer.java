package com.kittytalents.client.renderer;

import com.kittytalents.client.model.KTCatModel;
import com.kittytalents.entity.TalentedCat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Melee weapon (or the tool, when no weapon is equipped) held crosswise in the mouth, grip between the teeth and
 * the blade/head out to the cat's left. When both are equipped the tool rides holstered on the back.
 * TacZ guns are drawn by {@link TalentedCatGunHarnessLayer}.
 */
public class TalentedCatWeaponLayer extends RenderLayer<TalentedCat, KTCatModel> {
    /** Blocks along the item axis from the sprite centre to roughly the top of the grip. */
    private static final double GRIP_SHIFT = 0.16D;

    public TalentedCatWeaponLayer(RenderLayerParent<TalentedCat, KTCatModel> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, TalentedCat cat, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (cat.isInvisible()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.getCameraEntity() == cat && mc.options.getCameraType().isFirstPerson()) return;

        ItemStack weapon = cat.getEquippedWeapon();
        boolean weaponInMouth = !weapon.isEmpty() && !com.kittytalents.compat.tacz.TacZCompat.isGun(weapon);
        if (weaponInMouth) renderInMouth(poseStack, buffer, packedLight, cat, weapon);

        ItemStack tool = cat.getEquippedTool();
        if (tool.isEmpty()) return;
        if (!weaponInMouth) {
            renderInMouth(poseStack, buffer, packedLight, cat, tool);
        } else {
            poseStack.pushPose();
            this.getParentModel().translateToAnchor(KTCatModel.Anchor.BACK, poseStack);
            // Lying flat on the back, handle toward the tail, head toward the shoulders.
            poseStack.translate(0.0D, 6.0D / 16.0D, 0.6D / 16.0D);
            poseStack.mulPose(Axis.ZP.rotationDegrees(45.0F));
            poseStack.scale(0.5F, 0.5F, 0.5F);
            renderItem(poseStack, buffer, packedLight, cat, tool);
            poseStack.popPose();
        }
    }

    private void renderInMouth(PoseStack poseStack, MultiBufferSource buffer, int packedLight, TalentedCat cat, ItemStack stack) {
        poseStack.pushPose();
        this.getParentModel().translateToAnchor(KTCatModel.Anchor.MOUTH, poseStack);
        // Item sprites run diagonally, grip bottom-left to tip top-right (FIXED mirrors them about Y).
        // Slide the item toward its tip so the grip, not the middle, lands in the jaws; then turn the
        // diagonal horizontal so the blade/head sticks out to one side of the muzzle.
        poseStack.translate(GRIP_SHIFT, 0.0D, 0.0D);
        poseStack.mulPose(Axis.ZP.rotationDegrees(-135.0F));
        poseStack.scale(0.55F, 0.55F, 0.55F);
        renderItem(poseStack, buffer, packedLight, cat, stack);
        poseStack.popPose();
    }

    private void renderItem(PoseStack poseStack, MultiBufferSource buffer, int packedLight, TalentedCat cat, ItemStack stack) {
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, packedLight,
                OverlayTexture.NO_OVERLAY, poseStack, buffer, cat.level(), cat.getId());
    }
}
