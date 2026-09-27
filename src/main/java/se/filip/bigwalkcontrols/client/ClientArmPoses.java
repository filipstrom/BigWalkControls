package se.filip.bigwalkcontrols.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.client.IArmPoseTransformer;

public class ClientArmPoses {

    public static final EnumProxy<HumanoidModel.ArmPose> ARM_UP = new EnumProxy<>(
            HumanoidModel.ArmPose.class,
            false,
            (IArmPoseTransformer) ClientArmPoses::applyArmUp);
    public static final EnumProxy<HumanoidModel.ArmPose> ARM_FORWARD = new EnumProxy<>(
            HumanoidModel.ArmPose.class,
            false,
            (IArmPoseTransformer) ClientArmPoses::applyArmForward);
    public static final EnumProxy<HumanoidModel.ArmPose> ARM_SIDE = new EnumProxy<>(
            HumanoidModel.ArmPose.class,
            false,
            (IArmPoseTransformer) ClientArmPoses::applyArmSide);

    private static void applyArmUp(
            HumanoidModel<?> model,
            LivingEntity entity,
            HumanoidArm arm) {
        ModelPart armPart;

        if (arm == HumanoidArm.LEFT) {
            armPart = model.leftArm;
            armPart.zRot = 0.25f;
        } else {
            armPart = model.rightArm;
            armPart.zRot = -0.25f;
        }

        armPart.xRot = (float) Math.toRadians(-180);
        armPart.yRot = 0.0f;
    }

    private static void applyArmForward(
            HumanoidModel<?> model,
            LivingEntity entity,
            HumanoidArm arm) {
        ModelPart armPart;

        if (arm == HumanoidArm.LEFT) {
            armPart = model.leftArm;
        } else {
            armPart = model.rightArm;
        }

        // Armen pekar dit huvudet tittar
        armPart.xRot = model.head.xRot - (float) Math.PI / 2.0f;
        armPart.yRot = model.head.yRot;
        armPart.zRot = 0.0f;
    }

    private static void applyArmSide(
            HumanoidModel<?> model,
            LivingEntity entity,
            HumanoidArm arm) {
        ModelPart armPart = arm == HumanoidArm.LEFT ? model.leftArm : model.rightArm;

        armPart.xRot = 0.0f;
        armPart.yRot = 0.0f;

        if (arm == HumanoidArm.LEFT) {
            armPart.zRot = (float) Math.toRadians(-90);
        } else {
            armPart.zRot = (float) Math.toRadians(90);
        }
    }

}