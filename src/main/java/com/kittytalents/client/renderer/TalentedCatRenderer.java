package com.kittytalents.client.renderer;

import com.kittytalents.KittyTalents;
import com.kittytalents.entity.TalentedCat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.CatModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class TalentedCatRenderer extends MobRenderer<TalentedCat, TalentedCatModel> {
    public TalentedCatRenderer(EntityRendererProvider.Context context) {
        super(context, new TalentedCatModel(context.bakeLayer(ModelLayers.CAT)), 0.4F);
        this.addLayer(new TalentedCatArmorLayer(this));
        this.addLayer(new TalentedCatCollarLayer(this, context.getModelSet()));
        this.addLayer(new TalentedCatWeaponLayer(this));
        this.addLayer(new TalentedCatGunHarnessLayer(this));
        this.addLayer(new TalentedCatPackLayer(this));
        this.addLayer(new TalentedCatWingLayer(this, context.getModelSet()));
        this.addLayer(new TalentedCatAccessoryLayer(this));
        this.addLayer(new TalentedCatTailLayer(this, context.getModelSet()));
    }

    private static final ResourceLocation[] VANILLA_SKINS = new ResourceLocation[] {
        new ResourceLocation("textures/entity/cat/tabby.png"),
        new ResourceLocation("textures/entity/cat/black.png"),
        new ResourceLocation("textures/entity/cat/red.png"),
        new ResourceLocation("textures/entity/cat/siamese.png"),
        new ResourceLocation("textures/entity/cat/british_shorthair.png"),
        new ResourceLocation("textures/entity/cat/calico.png"),
        new ResourceLocation("textures/entity/cat/persian.png"),
        new ResourceLocation("textures/entity/cat/ragdoll.png"),
        new ResourceLocation("textures/entity/cat/white.png"),
        new ResourceLocation("textures/entity/cat/jellie.png"),
        new ResourceLocation("textures/entity/cat/all_black.png")
    };

    @Override
    public ResourceLocation getTextureLocation(TalentedCat cat) {
        int skinId = cat.getSkinId();
        if (skinId >= 11 && skinId <= 16) {
            if (skinId == 16) {
                return new ResourceLocation(KittyTalents.MODID, "textures/entity/cat/panther.png");
            }
            return new ResourceLocation(KittyTalents.MODID, "textures/entity/cat/skin_" + skinId + ".png");
        }

        // If default cosmetic skin (skinId == 0), check for custom breed identity
        if (skinId == 0) {
            com.kittytalents.entity.CatVariant variant = cat.getCatVariant();
            if (variant == com.kittytalents.entity.CatVariant.PANTHER) {
                return new ResourceLocation(KittyTalents.MODID, "textures/entity/cat/panther.png");
            } else if (variant == com.kittytalents.entity.CatVariant.SNOW_LEOPARD) {
                return new ResourceLocation(KittyTalents.MODID, "textures/entity/cat/skin_12.png");
            } else if (variant == com.kittytalents.entity.CatVariant.TIGER) {
                return new ResourceLocation(KittyTalents.MODID, "textures/entity/cat/skin_11.png");
            } else if (variant == com.kittytalents.entity.CatVariant.VOID_ENDER) {
                return new ResourceLocation(KittyTalents.MODID, "textures/entity/cat/skin_13.png");
            } else if (variant == com.kittytalents.entity.CatVariant.GOLDEN_MANEKI) {
                return new ResourceLocation(KittyTalents.MODID, "textures/entity/cat/skin_14.png");
            } else if (variant == com.kittytalents.entity.CatVariant.GHOST_PHANTOM) {
                return new ResourceLocation(KittyTalents.MODID, "textures/entity/cat/skin_15.png");
            }
        }

        if (skinId >= 0 && skinId < VANILLA_SKINS.length) {
            return VANILLA_SKINS[skinId];
        }
        return cat.getResourceLocation();
    }

    @Override
    public void render(TalentedCat cat, float entityYaw, float partialTicks, PoseStack poseStack, net.minecraft.client.renderer.MultiBufferSource buffer, int packedLight) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        boolean isFirstPersonTwine = mc.options.getCameraType().isFirstPerson() &&
                (mc.getCameraEntity() == cat || (com.kittytalents.client.ClientTwineHandler.isTwined() &&
                        com.kittytalents.client.ClientTwineHandler.activeTwinedCatId == cat.getId()));
        if (isFirstPersonTwine) {
            return;
        }

        TalentedCatModel model = this.getModel();
        model.getHeadPart().visible = true;

        boolean isNekomata = cat.getMutation() == com.kittytalents.entity.CatBreedingManager.MutationType.NEKOMATA;
        if (isNekomata) {
            model.getTail1().visible = false;
            model.getTail2().visible = false;
        }

        super.render(cat, entityYaw, partialTicks, poseStack, buffer, packedLight);

        if (isNekomata) {
            model.getTail1().visible = true;
            model.getTail2().visible = true;
        }
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
        if (cat.isIncapacitated()) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
            poseStack.translate(0.1D, -0.2D, 0.0D);
        }
    }
}
