package com.kittytalents.client.renderer;

import com.kittytalents.entity.TalentedCat;
import net.minecraft.client.model.CatModel;
import net.minecraft.client.model.geom.ModelPart;

public class TalentedCatModel extends CatModel<TalentedCat> {
    private final ModelPart tail1Part;
    private final ModelPart tail2Part;

    public TalentedCatModel(ModelPart root) {
        super(root);
        this.tail1Part = root.getChild("tail1");
        this.tail2Part = root.getChild("tail2");
    }

    public ModelPart getHeadPart() {
        return this.head;
    }

    public ModelPart getBodyPart() {
        return this.body;
    }

    public ModelPart getTail1() {
        return this.tail1Part;
    }

    public ModelPart getTail2() {
        return this.tail2Part;
    }

    public ModelPart getLeftFrontLeg() {
        return this.leftFrontLeg;
    }

    public ModelPart getRightFrontLeg() {
        return this.rightFrontLeg;
    }

    public ModelPart getLeftHindLeg() {
        return this.leftHindLeg;
    }

    public ModelPart getRightHindLeg() {
        return this.rightHindLeg;
    }

    public void renderSecondTail(com.mojang.blaze3d.vertex.PoseStack poseStack, com.mojang.blaze3d.vertex.VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        this.tail1Part.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        this.tail2Part.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
