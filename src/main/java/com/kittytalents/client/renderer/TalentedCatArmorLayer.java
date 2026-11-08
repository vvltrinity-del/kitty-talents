package com.kittytalents.client.renderer;

import com.kittytalents.client.model.KTCatModel;
import com.kittytalents.entity.TalentedCat;
import net.minecraft.client.model.geom.EntityModelSet;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import javax.annotation.Nullable;
import java.util.Locale;

public class TalentedCatArmorLayer extends RenderLayer<TalentedCat, KTCatModel> {
    /** Helmet, chest and front-leg guards: a shell 0.3 px out from the skin. */
    private final KTCatModel outerArmorModel;
    /** Hind-leg guards (the "leggings" slot) sit a little tighter so they never overlap the chest shell. */
    private final KTCatModel innerArmorModel;
    private final KTCatModel feetArmorModel;
    private final KTCatModel headArmorModel;

    public TalentedCatArmorLayer(RenderLayerParent<TalentedCat, KTCatModel> parent, EntityModelSet models) {
        super(parent);
        this.outerArmorModel = new KTCatModel(models.bakeLayer(KTCatModel.ARMOR_OUTER));
        this.innerArmorModel = new KTCatModel(models.bakeLayer(KTCatModel.ARMOR_INNER));
        this.feetArmorModel = new KTCatModel(models.bakeLayer(KTCatModel.ARMOR_FEET));
        this.headArmorModel = new KTCatModel(models.bakeLayer(KTCatModel.ARMOR_HEAD));
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, TalentedCat cat, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (cat.isInvisible()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.getCameraEntity() == cat && mc.options.getCameraType().isFirstPerson()) return;

        renderArmorPiece(poseStack, buffer, packedLight, cat, cat.getArmorSlotItem(1), EquipmentSlot.HEAD);
        renderArmorPiece(poseStack, buffer, packedLight, cat, cat.getArmorSlotItem(2), EquipmentSlot.CHEST);
        renderArmorPiece(poseStack, buffer, packedLight, cat, cat.getArmorSlotItem(3), EquipmentSlot.LEGS);
        renderArmorPiece(poseStack, buffer, packedLight, cat, cat.getArmorSlotItem(4), EquipmentSlot.FEET);
    }

    private void renderArmorPiece(PoseStack poseStack, MultiBufferSource buffer, int packedLight, TalentedCat cat, ItemStack stack, EquipmentSlot slot) {
        if (stack.isEmpty()) return;

        // Elytra on chest is handled by TalentedCatWingLayer with authentic wing folding & flaring
        if (slot == EquipmentSlot.CHEST && stack.is(Items.ELYTRA)) return;

        if (stack.getItem() instanceof ArmorItem) {
            KTCatModel model = switch (slot) {
                case LEGS -> this.innerArmorModel;
                case FEET -> this.feetArmorModel;
                case HEAD -> this.headArmorModel;
                default -> this.outerArmorModel;
            };
            this.getParentModel().copyPoseTo(model);
            switch (slot) {
                case HEAD -> model.showOnly(model.head);
                case CHEST -> model.showOnly(model.body);
                case LEGS -> model.showOnly(model.leftHindLeg, model.rightHindLeg);
                default -> model.showOnly(model.leftFrontLeg, model.rightFrontLeg);
            }

            boolean isLeather = isLeatherArmor(stack);
            ResourceLocation tex = getFelineArmorLocation(cat, stack, slot, null);

            if (isLeather) {
                int color = 0xA06540; // Default undyed leather brown
                if (stack.getItem() instanceof DyeableLeatherItem dyeable && dyeable.hasCustomColor(stack)) {
                    color = dyeable.getColor(stack);
                } else if (stack.getTag() != null && stack.getTag().contains("display", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
                    net.minecraft.nbt.CompoundTag display = stack.getTag().getCompound("display");
                    if (display.contains("color", net.minecraft.nbt.Tag.TAG_INT)) {
                        color = display.getInt("color");
                    }
                }
                float r = (float)(color >> 16 & 255) / 255.0F;
                float g = (float)(color >> 8 & 255) / 255.0F;
                float b = (float)(color & 255) / 255.0F;
                renderModel(poseStack, buffer, packedLight, stack.hasFoil(), model, r, g, b, tex);

                ResourceLocation overlayTex = getFelineArmorLocation(cat, stack, slot, "overlay");
                renderModel(poseStack, buffer, packedLight, false, model, 1.0F, 1.0F, 1.0F, overlayTex);
            } else {
                renderModel(poseStack, buffer, packedLight, stack.hasFoil(), model, 1.0F, 1.0F, 1.0F, tex);
            }
        } else if (slot == EquipmentSlot.HEAD && (stack.is(Items.CARVED_PUMPKIN) || stack.is(Items.SKELETON_SKULL) || stack.is(Items.WITHER_SKELETON_SKULL) || stack.is(Items.ZOMBIE_HEAD) || stack.is(Items.CREEPER_HEAD) || stack.is(Items.PIGLIN_HEAD))) {
            poseStack.pushPose();
            this.getParentModel().translateToAnchor(KTCatModel.Anchor.HEAD_TOP, poseStack);
            poseStack.translate(0.0D, -0.14D, 0.0D);
            poseStack.scale(0.55F, 0.55F, 0.55F);
            Minecraft.getInstance().getItemRenderer().renderStatic(
                    stack,
                    ItemDisplayContext.HEAD,
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

    private void renderModel(PoseStack poseStack, MultiBufferSource buffer, int packedLight, boolean hasFoil, KTCatModel model, float red, float green, float blue, ResourceLocation texture) {
        VertexConsumer vertexConsumer = ItemRenderer.getArmorFoilBuffer(buffer, RenderType.armorCutoutNoCull(texture), false, hasFoil);
        model.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, red, green, blue, 1.0F);
    }

    private ResourceLocation getFelineArmorLocation(TalentedCat cat, ItemStack stack, EquipmentSlot slot, @Nullable String type) {
        if (stack.getItem() instanceof ArmorItem armorItem) {
            String material = armorItem.getMaterial().getName().toLowerCase(Locale.ROOT);
            if (material.contains(":")) {
                material = material.substring(material.indexOf(':') + 1);
            }
            if ("overlay".equals(type) && material.contains("leather")) {
                return new ResourceLocation("kittytalents", "textures/models/armor/cat_armor_leather_overlay.png");
            }
            if (material.contains("netherite")) {
                return new ResourceLocation("kittytalents", "textures/models/armor/cat_armor_netherite.png");
            } else if (material.contains("diamond")) {
                return new ResourceLocation("kittytalents", "textures/models/armor/cat_armor_diamond.png");
            } else if (material.contains("gold")) {
                return new ResourceLocation("kittytalents", "textures/models/armor/cat_armor_golden.png");
            } else if (material.contains("iron")) {
                return new ResourceLocation("kittytalents", "textures/models/armor/cat_armor_iron.png");
            } else if (material.contains("chainmail")) {
                return new ResourceLocation("kittytalents", "textures/models/armor/cat_armor_chainmail.png");
            } else if (material.contains("turtle")) {
                return new ResourceLocation("kittytalents", "textures/models/armor/cat_armor_turtle.png");
            } else if (material.contains("leather")) {
                return new ResourceLocation("kittytalents", "textures/models/armor/cat_armor_leather.png");
            }
            return new ResourceLocation("kittytalents", "textures/models/armor/cat_armor_iron.png");
        }
        return new ResourceLocation("kittytalents", "textures/models/armor/cat_armor_iron.png");
    }

    private boolean isLeatherArmor(ItemStack stack) {
        if (stack.getItem() instanceof ArmorItem armorItem) {
            String mat = armorItem.getMaterial().getName().toLowerCase(Locale.ROOT);
            return mat.contains("leather");
        }
        return stack.getItem() instanceof DyeableLeatherItem;
    }
}
