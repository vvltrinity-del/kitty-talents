package com.kittytalents.client.renderer;

import com.kittytalents.KittyTalents;
import com.kittytalents.entity.TalentedCat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.kittytalents.client.model.KTCatModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class TalentedCatRenderer extends MobRenderer<TalentedCat, KTCatModel> {
    public TalentedCatRenderer(EntityRendererProvider.Context context) {
        super(context, new KTCatModel(context.bakeLayer(KTCatModel.MAIN)), 0.4F);
        this.addLayer(new TalentedCatArmorLayer(this, context.getModelSet()));
        this.addLayer(new TalentedCatCollarLayer(this, context.getModelSet()));
        this.addLayer(new TalentedCatWeaponLayer(this));
        this.addLayer(new TalentedCatGunHarnessLayer(this));
        this.addLayer(new TalentedCatPackLayer(this));
        this.addLayer(new TalentedCatWingLayer(this, context.getModelSet()));
        this.addLayer(new TalentedCatAccessoryLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(TalentedCat cat) {
        return com.kittytalents.client.skin.SkinRegistry.INSTANCE.resolve(cat);
    }

    @Override
    public void render(TalentedCat cat, float entityYaw, float partialTicks, PoseStack poseStack, net.minecraft.client.renderer.MultiBufferSource buffer, int packedLight) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        boolean isFirstPersonTwine = mc.options.getCameraType().isFirstPerson() &&
                (mc.getCameraEntity() == cat || com.kittytalents.client.twine.ClientTwineState.isTwinedWith(cat.getId()));
        if (isFirstPersonTwine) {
            return;
        }

        super.render(cat, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    protected void scale(TalentedCat cat, PoseStack poseStack, float partialTickTime) {
        float scale = cat.getCatScale();
        poseStack.scale(scale, scale, scale);
        super.scale(cat, poseStack, partialTickTime);
    }

    @Override
    protected void setupRotations(TalentedCat cat, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTicks) {
        super.setupRotations(cat, poseStack, ageInTicks, rotationYaw, partialTicks);
        // Curled up asleep: roll onto the side like the vanilla cat renderer does.
        float lie = cat.getLieDownAmount(partialTicks);
        if (lie > 0.0F && !cat.isIncapacitated()) {
            poseStack.translate(0.4F * lie, 0.15F * lie, 0.1F * lie);
            poseStack.mulPose(Axis.ZP.rotationDegrees(net.minecraft.util.Mth.rotLerp(lie, 0.0F, 90.0F)));
        }
        if (cat.isIncapacitated()) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
            poseStack.translate(0.1D, -0.2D, 0.0D);
        }
    }
}
