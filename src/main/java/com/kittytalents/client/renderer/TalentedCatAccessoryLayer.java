package com.kittytalents.client.renderer;

import com.kittytalents.client.model.KTCatModel;
import com.kittytalents.client.model.KTCatModel.Anchor;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.item.CatAccessoryItem;
import com.kittytalents.registry.ModItems;
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
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class TalentedCatAccessoryLayer extends RenderLayer<TalentedCat, KTCatModel> {
    private static final ResourceLocation TOP_HAT_TEX = new ResourceLocation("kittytalents", "textures/models/accessories/top_hat.png");
    private static final ResourceLocation WITCH_HAT_TEX = new ResourceLocation("kittytalents", "textures/models/accessories/witch_hat.png");
    private static final ResourceLocation CROWN_TEX = new ResourceLocation("kittytalents", "textures/models/accessories/crown.png");
    private static final ResourceLocation HALO_TEX = new ResourceLocation("kittytalents", "textures/models/accessories/halo.png");
    private static final ResourceLocation GOGGLES_TEX = new ResourceLocation("kittytalents", "textures/models/accessories/feline_goggles.png");
    private static final ResourceLocation PHANTOM_MASK_TEX = new ResourceLocation("kittytalents", "textures/models/accessories/phantom_mask.png");
    private static final ResourceLocation BOWTIE_TEX = new ResourceLocation("kittytalents", "textures/models/accessories/bowtie.png");
    private static final ResourceLocation CAT_SCARF_TEX = new ResourceLocation("kittytalents", "textures/models/accessories/cat_scarf.png");
    private static final ResourceLocation HERO_CAPE_TEX = new ResourceLocation("kittytalents", "textures/models/accessories/hero_cape.png");

    private final ModelPart topHatPart;
    private final ModelPart witchHatPart;
    private final ModelPart crownPart;
    private final ModelPart haloPart;
    private final ModelPart gogglesPart;
    private final ModelPart phantomMaskPart;
    private final ModelPart bowtiePart;
    private final ModelPart catScarfPart;
    private final ModelPart heroCapePart;

    public TalentedCatAccessoryLayer(RenderLayerParent<TalentedCat, KTCatModel> parent) {
        super(parent);
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Geometry is authored in anchor space (see KTCatModel#createMesh): HEAD_TOP has the skull top at y = 0
        // and up is -Y; NECK/BACK use the body axes (+Y toward the tail, +Z out of the back, -Z toward the belly).

        // 1. TOP_HAT (HEAD_TOP): brim resting on the skull, tall crown, ribbon band
        root.addOrReplaceChild("top_hat", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3.5F, -0.8F, -3.5F, 7.0F, 0.8F, 7.0F)
                .texOffs(0, 8).addBox(-2.25F, -5.8F, -2.25F, 4.5F, 5.0F, 4.5F)
                .texOffs(0, 18).addBox(-2.35F, -1.8F, -2.35F, 4.7F, 1.0F, 4.7F),
                PartPose.ZERO);

        // 2. WITCH_HAT (HEAD_TOP): wide brim, stacked cone, bent tip
        root.addOrReplaceChild("witch_hat", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, -0.8F, -4.0F, 8.0F, 0.8F, 8.0F)
                .texOffs(0, 9).addBox(-2.5F, -3.3F, -2.5F, 5.0F, 2.5F, 5.0F)
                .texOffs(0, 17).addBox(-1.5F, -5.8F, -1.5F, 3.0F, 2.5F, 3.0F)
                .texOffs(0, 23).addBox(-0.75F, -7.8F, -0.2F, 1.5F, 2.0F, 1.5F),
                PartPose.ZERO);

        // 3. CROWN (HEAD_TOP): circlet hugging the top of the skull + 4 points
        root.addOrReplaceChild("crown", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.6F, -1.0F, -2.6F, 5.2F, 1.8F, 5.2F)
                .texOffs(0, 8).addBox(-0.6F, -2.6F, -2.7F, 1.2F, 1.8F, 0.6F)
                .texOffs(4, 8).addBox(-0.6F, -2.6F, 2.1F, 1.2F, 1.8F, 0.6F)
                .texOffs(8, 8).addBox(-2.7F, -2.6F, -0.6F, 0.6F, 1.8F, 1.2F)
                .texOffs(12, 8).addBox(2.1F, -2.6F, -0.6F, 0.6F, 1.8F, 1.2F),
                PartPose.ZERO);

        // 4. HALO (HEAD_TOP): square ring floating above the ears
        root.addOrReplaceChild("halo", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3.0F, -3.0F, -3.0F, 6.0F, 0.8F, 0.8F)
                .texOffs(0, 2).addBox(-3.0F, -3.0F, 2.2F, 6.0F, 0.8F, 0.8F)
                .texOffs(0, 4).addBox(-3.0F, -3.0F, -2.2F, 0.8F, 0.8F, 4.4F)
                .texOffs(0, 10).addBox(2.2F, -3.0F, -2.2F, 0.8F, 0.8F, 4.4F),
                PartPose.ZERO);

        // 5. FELINE_GOGGLES (head bone): lenses over the eyes + strap around the skull
        root.addOrReplaceChild("feline_goggles", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.3F, -0.9F, -3.6F, 1.9F, 1.9F, 1.2F)
                .texOffs(8, 0).addBox(0.4F, -0.9F, -3.6F, 1.9F, 1.9F, 1.2F)
                .texOffs(0, 4).addBox(-0.4F, -0.3F, -3.5F, 0.8F, 0.6F, 0.6F)
                .texOffs(0, 6).addBox(-2.9F, -0.5F, -3.0F, 0.6F, 0.8F, 5.4F)
                .texOffs(8, 6).addBox(2.3F, -0.5F, -3.0F, 0.6F, 0.8F, 5.4F)
                .texOffs(0, 12).addBox(-2.3F, -0.5F, 2.0F, 4.6F, 0.8F, 0.6F),
                PartPose.ZERO);

        // 6. PHANTOM_MASK (head bone): plate over the upper face
        root.addOrReplaceChild("phantom_mask", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.5F, -1.8F, -3.6F, 5.0F, 2.2F, 0.8F)
                .texOffs(0, 4).addBox(-1.0F, 0.4F, -4.6F, 2.0F, 1.2F, 0.8F)
                .texOffs(0, 7).addBox(-3.1F, -1.8F, -3.2F, 0.8F, 1.6F, 1.8F)
                .texOffs(6, 7).addBox(2.3F, -1.8F, -3.2F, 0.8F, 1.6F, 1.8F),
                PartPose.ZERO);

        // 7. BOWTIE (NECK): knot + two loops on the chest, just under the chin
        root.addOrReplaceChild("bowtie", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-0.5F, -0.7F, -1.6F, 1.0F, 0.8F, 1.0F)
                .texOffs(0, 2).addBox(-2.2F, -0.6F, -2.0F, 1.7F, 0.6F, 1.8F)
                .texOffs(6, 2).addBox(0.5F, -0.6F, -2.0F, 1.7F, 0.6F, 1.8F),
                PartPose.ZERO);

        // 8. CAT_SCARF (NECK): ring around the chest + tail hanging down the front
        root.addOrReplaceChild("cat_scarf", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.5F, -0.2F, -3.4F, 5.0F, 1.8F, 6.6F)
                .texOffs(0, 8).addBox(0.4F, -0.7F, -4.6F, 1.6F, 0.6F, 3.8F),
                PartPose.ZERO);

        // 9. HERO_CAPE (BACK): clasp across the shoulders, panel down the back, short drapes over both flanks
        root.addOrReplaceChild("hero_cape", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.4F, -0.6F, -0.4F, 4.8F, 1.0F, 1.0F)
                .texOffs(0, 2).addBox(-2.5F, 0.0F, 0.05F, 5.0F, 13.0F, 0.4F)
                .texOffs(12, 2).addBox(2.1F, 0.6F, -3.2F, 0.4F, 12.0F, 3.25F)
                .texOffs(12, 2).addBox(-2.5F, 0.6F, -3.2F, 0.4F, 12.0F, 3.25F),
                PartPose.ZERO);

        ModelPart baked = root.bake(32, 32);
        this.topHatPart = baked.getChild("top_hat");
        this.witchHatPart = baked.getChild("witch_hat");
        this.crownPart = baked.getChild("crown");
        this.haloPart = baked.getChild("halo");
        this.gogglesPart = baked.getChild("feline_goggles");
        this.phantomMaskPart = baked.getChild("phantom_mask");
        this.bowtiePart = baked.getChild("bowtie");
        this.catScarfPart = baked.getChild("cat_scarf");
        this.heroCapePart = baked.getChild("hero_cape");
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, TalentedCat cat, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (cat.isInvisible()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.getCameraEntity() == cat && mc.options.getCameraType().isFirstPerson()) return;

        // Functional accessory
        ItemStack accessory = cat.getEquippedAccessory();
        if (!accessory.isEmpty()) {
            renderCosmetic3D(poseStack, buffer, packedLight, cat, accessory);
        }

        // Up to 3 cosmetics
        if (cat.isCosmeticsVisible()) {
            for (int i = 0; i < 3; i++) {
                ItemStack cosmetic = cat.getCosmeticSlot(i);
                if (!cosmetic.isEmpty()) {
                    renderCosmetic3D(poseStack, buffer, packedLight, cat, cosmetic);
                }
            }
        }
    }

    private void renderCosmetic3D(PoseStack poseStack, MultiBufferSource buffer, int packedLight, TalentedCat cat, ItemStack stack) {
        poseStack.pushPose();

        KTCatModel model = this.getParentModel();
        if (stack.is(ModItems.TOP_HAT.get())) {
            model.translateToAnchor(Anchor.HEAD_TOP, poseStack);
            VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(TOP_HAT_TEX));
            this.topHatPart.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        } else if (stack.is(ModItems.WITCH_HAT.get())) {
            model.translateToAnchor(Anchor.HEAD_TOP, poseStack);
            VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(WITCH_HAT_TEX));
            this.witchHatPart.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        } else if (stack.is(ModItems.CROWN.get())) {
            model.translateToAnchor(Anchor.HEAD_TOP, poseStack);
            VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(CROWN_TEX));
            this.crownPart.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        } else if (stack.is(ModItems.HALO.get())) {
            model.translateToAnchor(Anchor.HEAD_TOP, poseStack);
            VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucentEmissive(HALO_TEX));
            this.haloPart.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        } else if (stack.is(ModItems.FELINE_GOGGLES.get())) {
            model.head.translateAndRotate(poseStack);
            VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(GOGGLES_TEX));
            this.gogglesPart.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        } else if (stack.is(ModItems.PHANTOM_MASK.get())) {
            model.head.translateAndRotate(poseStack);
            VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(PHANTOM_MASK_TEX));
            this.phantomMaskPart.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        } else if (stack.is(ModItems.BOWTIE.get())) {
            model.translateToAnchor(Anchor.NECK, poseStack);
            VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(BOWTIE_TEX));
            this.bowtiePart.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        } else if (stack.is(ModItems.CAT_SCARF.get())) {
            model.translateToAnchor(Anchor.NECK, poseStack);
            VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(CAT_SCARF_TEX));
            this.catScarfPart.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        } else if (stack.is(ModItems.HERO_CAPE.get())) {
            model.translateToAnchor(Anchor.BACK, poseStack);
            VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(HERO_CAPE_TEX));
            this.heroCapePart.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        } else if (stack.is(ModItems.ANGEL_WINGS.get())) {
            // Handled directly via TalentedCatWingLayer on the cat's back
        } else {
            // Generic / Modded items equipped in cosmetic slots render in 3D
            if (stack.getItem() instanceof CatAccessoryItem acc) {
                switch (acc.getAccessoryType()) {
                    case HAT -> {
                        model.translateToAnchor(Anchor.HEAD_TOP, poseStack);
                        poseStack.translate(0.0D, -0.15D, 0.0D);
                        poseStack.scale(0.55F, 0.55F, 0.55F);
                    }
                    case GLASSES -> {
                        model.head.translateAndRotate(poseStack);
                        poseStack.translate(0.0D, -0.05D, -0.28D);
                        poseStack.scale(0.4F, 0.4F, 0.4F);
                    }
                    case BOW -> {
                        model.translateToAnchor(Anchor.NECK, poseStack);
                        poseStack.translate(0.0D, -0.03D, -0.06D);
                        poseStack.scale(0.35F, 0.35F, 0.35F);
                    }
                    case CAPE -> {
                        model.translateToAnchor(Anchor.BACK, poseStack);
                        poseStack.translate(0.0D, 0.3D, 0.02D);
                        poseStack.scale(0.6F, 0.6F, 0.6F);
                    }
                }
            } else {
                model.translateToAnchor(Anchor.HEAD_TOP, poseStack);
                poseStack.translate(0.0D, -0.12D, 0.0D);
                poseStack.scale(0.5F, 0.5F, 0.5F);
            }

            Minecraft.getInstance().getItemRenderer().renderStatic(
                    stack,
                    ItemDisplayContext.FIXED,
                    packedLight,
                    OverlayTexture.NO_OVERLAY,
                    poseStack,
                    buffer,
                    cat.level(),
                    cat.getId()
            );
        }

        poseStack.popPose();
    }
}
