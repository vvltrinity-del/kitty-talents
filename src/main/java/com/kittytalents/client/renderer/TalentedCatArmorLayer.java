package com.kittytalents.client.renderer;

import com.kittytalents.entity.TalentedCat;
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

public class TalentedCatArmorLayer extends RenderLayer<TalentedCat, TalentedCatModel> {
    private final TalentedCatArmorModel outerArmorModel;
    private final TalentedCatArmorModel innerArmorModel;

    public TalentedCatArmorLayer(RenderLayerParent<TalentedCat, TalentedCatModel> parent) {
        super(parent);
        MeshDefinition outerMesh = TalentedCatArmorModel.createArmorMesh(new CubeDeformation(0.22F));
        ModelPart outerRoot = outerMesh.getRoot().bake(64, 32);
        this.outerArmorModel = new TalentedCatArmorModel(outerRoot);

        MeshDefinition innerMesh = TalentedCatArmorModel.createArmorMesh(new CubeDeformation(0.12F));
        ModelPart innerRoot = innerMesh.getRoot().bake(64, 32);
        this.innerArmorModel = new TalentedCatArmorModel(innerRoot);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, TalentedCat cat, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (cat.isInvisible()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.getCameraEntity() == cat && mc.options.getCameraType().isFirstPerson()) return;

        // Slot 1: Helmet
        renderArmorPiece(poseStack, buffer, packedLight, cat, cat.getArmorSlotItem(1), EquipmentSlot.HEAD, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch);
        // Slot 2: Chestplate / Battle Harness
        renderArmorPiece(poseStack, buffer, packedLight, cat, cat.getArmorSlotItem(2), EquipmentSlot.CHEST, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch);
        // Slot 3: Leggings / Flank Guards
        renderArmorPiece(poseStack, buffer, packedLight, cat, cat.getArmorSlotItem(3), EquipmentSlot.LEGS, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch);
        // Slot 4: Boots / Paw Guards
        renderArmorPiece(poseStack, buffer, packedLight, cat, cat.getArmorSlotItem(4), EquipmentSlot.FEET, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch);
    }

    private void renderArmorPiece(PoseStack poseStack, MultiBufferSource buffer, int packedLight, TalentedCat cat, ItemStack stack, EquipmentSlot slot, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (stack.isEmpty()) return;

        // Elytra on chest is handled by TalentedCatWingLayer with authentic wing folding & flaring
        if (slot == EquipmentSlot.CHEST && stack.is(Items.ELYTRA)) return;

        if (stack.getItem() instanceof ArmorItem armorItem) {
            TalentedCatArmorModel model = (slot == EquipmentSlot.LEGS) ? this.innerArmorModel : this.outerArmorModel;
            this.getParentModel().copyPropertiesTo(model);
            // CRITICAL: Call prepareMobModel so the armor model adopts the cat's sitting/sneaking bone transforms!
            model.prepareMobModel(cat, limbSwing, limbSwingAmount, partialTicks);
            model.setupAnim(cat, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            model.copyBonesFrom(this.getParentModel());
            model.setSlotVisible(slot);

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
            this.getParentModel().getHeadPart().translateAndRotate(poseStack);
            poseStack.translate(0.0D, -0.32D, 0.0D);
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

    private void renderModel(PoseStack poseStack, MultiBufferSource buffer, int packedLight, boolean hasFoil, TalentedCatArmorModel model, float red, float green, float blue, ResourceLocation texture) {
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
