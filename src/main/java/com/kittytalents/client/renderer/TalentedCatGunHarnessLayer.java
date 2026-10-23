package com.kittytalents.client.renderer;

import com.kittytalents.compat.tacz.TacZCompat;
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
import net.minecraft.world.item.Items;

/**
 * Renders an equipped TacZ firearm on a back-mounted tactical turret harness above the cat's spine.
 * Tracks head yaw/pitch, kicks back on firing, and tilts sideways while reloading.
 */
public class TalentedCatGunHarnessLayer extends RenderLayer<TalentedCat, TalentedCatModel> {
    private static final ItemStack HARNESS_STRAP_ICON = new ItemStack(Items.SADDLE);

    public TalentedCatGunHarnessLayer(RenderLayerParent<TalentedCat, TalentedCatModel> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, TalentedCat cat,
                       float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (cat.isInvisible()) return;
        ItemStack weapon = cat.getEquippedWeapon();
        if (weapon.isEmpty() || !TacZCompat.isGun(weapon)) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.getCameraEntity() == cat && mc.options.getCameraType().isFirstPerson()) return;

        poseStack.pushPose();
        this.getParentModel().getBodyPart().translateAndRotate(poseStack);

        // Undo the 90-degree X rotation of the vanilla cat body part so +Y is down and -Z is forward
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));

        // 1. Render subtle tactical saddle/harness mount on the cat's back
        poseStack.pushPose();
        poseStack.translate(0.0D, -0.10D, 0.0D);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        poseStack.scale(0.32F, 0.32F, 0.25F);
        mc.getItemRenderer().renderStatic(
                HARNESS_STRAP_ICON,
                ItemDisplayContext.FIXED,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                buffer,
                cat.level(),
                cat.getId()
        );
        poseStack.popPose();

        // 2. Position turret mount above the spine
        poseStack.translate(0.0D, -0.22D, -0.05D);

        // Track target/head yaw and pitch
        poseStack.mulPose(Axis.YP.rotationDegrees(netHeadYaw * 0.65F));
        poseStack.mulPose(Axis.XP.rotationDegrees(headPitch * 0.75F));

        // Recoil kick animation for 5 ticks after firing
        int ticksSinceShot = cat.tickCount - cat.getLastShotTick();
        if (ticksSinceShot >= 0 && ticksSinceShot < 5) {
            float recoilProgress = 1.0F - ((ticksSinceShot + partialTicks) / 5.0F);
            recoilProgress = Math.max(0.0F, Math.min(1.0F, recoilProgress));
            poseStack.translate(0.0D, 0.0D, recoilProgress * 0.08D);
            poseStack.mulPose(Axis.XP.rotationDegrees(-recoilProgress * 12.0F));
        }

        // Reload tilt animation
        if (TacZCompat.isReloading(cat)) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(35.0F));
        }

        poseStack.scale(0.55F, 0.55F, 0.55F);

        mc.getItemRenderer().renderStatic(
                weapon,
                ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                buffer,
                cat.level(),
                cat.getId()
        );

        poseStack.popPose();
    }
}
