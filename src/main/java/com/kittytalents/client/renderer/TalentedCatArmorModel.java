package com.kittytalents.client.renderer;

import com.kittytalents.entity.TalentedCat;
import net.minecraft.client.model.CatModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.EquipmentSlot;

/**
 * A deliberately restrained armor shell for the vanilla cat rig.
 *
 * Every piece is attached to an existing cat bone.  The old model attempted to
 * turn every player-armor slot into a complete 3D suit, which produced several
 * overlapping cubes around a very small entity.  This version uses only a
 * fitted helm, a thin dorsal carapace and small ankle cuffs.
 */
public class TalentedCatArmorModel extends CatModel<TalentedCat> {
    private final ModelPart carapace;
    private final ModelPart rumpPlate;
    private final ModelPart leftFrontCuff;
    private final ModelPart rightFrontCuff;
    private final ModelPart leftHindCuff;
    private final ModelPart rightHindCuff;

    public TalentedCatArmorModel(ModelPart root) {
        super(root);
        this.carapace = this.body.getChild("carapace");
        this.rumpPlate = this.body.getChild("rump_plate");
        this.leftFrontCuff = this.leftFrontLeg.getChild("cuff");
        this.rightFrontCuff = this.rightFrontLeg.getChild("cuff");
        this.leftHindCuff = this.leftHindLeg.getChild("cuff");
        this.rightHindCuff = this.rightHindLeg.getChild("cuff");
    }

    public static MeshDefinition createArmorMesh(CubeDeformation deformation) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Helm: a shallow skull cap.  It intentionally leaves the muzzle, eyes,
        // cheeks, and ears clear so the cat remains recognisable.
        root.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-2.12F, -2.13F, -2.12F, 4.24F, 1.15F, 4.05F, deformation)
                        .texOffs(0, 6).addBox(-2.12F, -1.68F, 1.72F, 4.24F, 0.95F, 0.35F, deformation),
                PartPose.offset(0.0F, 15.0F, -9.0F));

        // Chestplate: a thin fitted strip along the cat's spine, rather than a
        // cuboid that encloses the torso.  The body has the same transform as the
        // vanilla cat body, so it follows sitting, walking, and sleeping poses.
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0F, 12.0F, -10.0F, 1.5707964F, 0.0F, 0.0F));
        body.addOrReplaceChild("carapace", CubeListBuilder.create()
                        .texOffs(18, 0).addBox(-2.12F, 3.15F, -8.12F, 4.24F, 11.0F, 1.15F, deformation),
                PartPose.ZERO);
        body.addOrReplaceChild("rump_plate", CubeListBuilder.create()
                        .texOffs(18, 14).addBox(-2.12F, 14.15F, -8.12F, 4.24F, 4.0F, 1.15F, deformation),
                PartPose.ZERO);

        // Leg armor is intentionally limited to the lower leg/ankle.  It avoids
        // the stacked thigh and shoulder blocks that previously obscured the rig.
        PartDefinition leftHindLeg = root.addOrReplaceChild("left_hind_leg", CubeListBuilder.create(),
                PartPose.offset(1.1F, 18.0F, 5.0F));
        leftHindLeg.addOrReplaceChild("cuff", CubeListBuilder.create()
                        .texOffs(0, 12).addBox(-1.08F, 3.85F, -0.08F, 2.16F, 1.45F, 2.16F, deformation),
                PartPose.ZERO);
        PartDefinition rightHindLeg = root.addOrReplaceChild("right_hind_leg", CubeListBuilder.create(),
                PartPose.offset(-1.1F, 18.0F, 5.0F));
        rightHindLeg.addOrReplaceChild("cuff", CubeListBuilder.create()
                        .texOffs(0, 12).addBox(-1.08F, 3.85F, -0.08F, 2.16F, 1.45F, 2.16F, deformation),
                PartPose.ZERO);
        PartDefinition leftFrontLeg = root.addOrReplaceChild("left_front_leg", CubeListBuilder.create(),
                PartPose.offset(1.2F, 14.1F, -5.0F));
        leftFrontLeg.addOrReplaceChild("cuff", CubeListBuilder.create()
                        .texOffs(9, 12).addBox(-1.08F, 3.85F, -0.08F, 2.16F, 1.45F, 2.16F, deformation),
                PartPose.ZERO);
        PartDefinition rightFrontLeg = root.addOrReplaceChild("right_front_leg", CubeListBuilder.create(),
                PartPose.offset(-1.2F, 14.1F, -5.0F));
        rightFrontLeg.addOrReplaceChild("cuff", CubeListBuilder.create()
                        .texOffs(9, 12).addBox(-1.08F, 3.85F, -0.08F, 2.16F, 1.45F, 2.16F, deformation),
                PartPose.ZERO);

        // Required CatModel bones, deliberately left empty: no tail sleeves or
        // duplicate geometry are rendered for armor.
        root.addOrReplaceChild("tail1", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0F, 15.0F, 8.0F, 0.9F, 0.0F, 0.0F));
        root.addOrReplaceChild("tail2", CubeListBuilder.create(), PartPose.offset(0.0F, 20.0F, 14.0F));
        return mesh;
    }

    public void copyBonesFrom(TalentedCatModel parent) {
        this.head.copyFrom(parent.getHeadPart());
        this.body.copyFrom(parent.getBodyPart());
        this.leftFrontLeg.copyFrom(parent.getLeftFrontLeg());
        this.rightFrontLeg.copyFrom(parent.getRightFrontLeg());
        this.leftHindLeg.copyFrom(parent.getLeftHindLeg());
        this.rightHindLeg.copyFrom(parent.getRightHindLeg());
        this.tail1.copyFrom(parent.getTail1());
        this.tail2.copyFrom(parent.getTail2());
    }

    public void setSlotVisible(EquipmentSlot slot) {
        this.head.visible = slot == EquipmentSlot.HEAD;
        this.body.visible = slot == EquipmentSlot.CHEST || slot == EquipmentSlot.LEGS;
        this.carapace.visible = slot == EquipmentSlot.CHEST;
        this.rumpPlate.visible = slot == EquipmentSlot.LEGS;

        boolean feet = slot == EquipmentSlot.FEET;
        this.leftFrontLeg.visible = feet;
        this.rightFrontLeg.visible = feet;
        this.leftHindLeg.visible = feet;
        this.rightHindLeg.visible = feet;
        this.leftFrontCuff.visible = feet;
        this.rightFrontCuff.visible = feet;
        this.leftHindCuff.visible = feet;
        this.rightHindCuff.visible = feet;

        // Leggings use only the rear plate, avoiding a second full torso layer.
        this.tail1.visible = false;
        this.tail2.visible = false;
    }
}
