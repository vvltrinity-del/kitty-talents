package com.kittytalents.client.renderer;

import com.kittytalents.compat.tacz.TacZCompat;
import com.kittytalents.client.model.KTCatModel;
import com.kittytalents.entity.TalentedCat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
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
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Renders an equipped TacZ firearm on a fitted 3D back-mounted tactical turret harness
 * directly above the cat's dorsal spine. Tracks head yaw/pitch, kicks back on firing,
 * and tilts sideways while reloading.
 */
public class TalentedCatGunHarnessLayer extends RenderLayer<TalentedCat, KTCatModel> {
    private static final ResourceLocation HARNESS_TEX = new ResourceLocation("kittytalents", "textures/models/accessories/cat_backpack.png");
    private final ModelPart harnessPart;

    public TalentedCatGunHarnessLayer(RenderLayerParent<TalentedCat, KTCatModel> parent) {
        super(parent);
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Fitted tactical harness in cat body bone space (body box is x=-2..2, y=3..19, z=-8..-2):
        // z = -2.0F is the dorsal spine; z = -8.0F is the belly.
        root.addOrReplaceChild("harness", CubeListBuilder.create()
                // Dorsal saddle plate across the upper back/shoulders
                .texOffs(0, 24).addBox(-2.2F, 4.2F, -2.15F, 4.4F, 4.2F, 0.55F)
                // Left flank girth strap
                .texOffs(0, 9).addBox(-2.3F, 4.8F, -7.2F, 0.45F, 2.2F, 5.2F)
                // Right flank girth strap
                .texOffs(16, 9).addBox(1.85F, 4.8F, -7.2F, 0.45F, 2.2F, 5.2F)
                // Low-profile central turret rail & gimbal mount on the dorsal plate
                .texOffs(0, 16).addBox(-0.9F, 4.8F, -1.65F, 1.8F, 3.0F, 0.85F),
                PartPose.ZERO);

        ModelPart baked = root.bake(32, 32);
        this.harnessPart = baked.getChild("harness");
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, TalentedCat cat,
                       float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (cat.isInvisible()) return;
        ItemStack weapon = cat.getEquippedWeapon();
        if (weapon.isEmpty() || !TacZCompat.isGun(weapon)) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.options.getCameraType().isFirstPerson() &&
                (mc.getCameraEntity() == cat || (com.kittytalents.client.ClientTwineHandler.isTwined() &&
                        com.kittytalents.client.ClientTwineHandler.activeTwinedCatId == cat.getId()))) {
            return;
        }

        poseStack.pushPose();
        ModelPart bodyPart = this.getParentModel().getBodyPart();
        bodyPart.translateAndRotate(poseStack);

        // 1. Render fitted 3D tactical harness & turret rail directly on the cat's torso
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(HARNESS_TEX));
        this.harnessPart.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);

        // 2. Translate in bodyPart local space to the top of the turret gimbal on the shoulder spine
        // (y = 6.3px = 0.394D along spine; z = -1.1px = -0.068D just above dorsal spine surface z = -2.0px)
        poseStack.translate(0.0D, 0.394D, -0.068D);

        // 3. Level the turret gimbal with the horizon (undoing bodyPart.xRot whether standing at 90° or sitting at 45°)
        // and flip 180° around Z to undo LivingEntityRenderer's (-1, -1, 1) scale so +Y is UP and -Z is FORWARD.
        poseStack.mulPose(Axis.XP.rotation(-bodyPart.xRot));
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));

        // 4. Track target/head yaw and pitch in upright turret space (+Y up, +X right, -Z forward)
        poseStack.mulPose(Axis.YP.rotationDegrees(-netHeadYaw * 0.65F));
        poseStack.mulPose(Axis.XP.rotationDegrees(-headPitch * 0.75F));

        // 5. Recoil kick animation for 5 ticks after firing
        int ticksSinceShot = cat.tickCount - cat.getLastShotTick();
        if (ticksSinceShot >= 0 && ticksSinceShot < 5) {
            float recoilProgress = 1.0F - ((ticksSinceShot + partialTicks) / 5.0F);
            recoilProgress = Math.max(0.0F, Math.min(1.0F, recoilProgress));
            poseStack.translate(0.0D, 0.0D, recoilProgress * 0.06D);
            poseStack.mulPose(Axis.XP.rotationDegrees(recoilProgress * 10.0F));
        }

        // 6. Reload tilt animation
        if (TacZCompat.isReloading(cat)) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(35.0F));
        }

        // Snug receiver offset so the gun body rests flush on the turret rail
        poseStack.translate(0.0D, -0.03D, 0.04D);
        poseStack.scale(0.50F, 0.50F, 0.50F);

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
