package se.filip.bigwalkcontrols.client;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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

    // Fix for sneaking changes direction of forward arms (smoothing)
    private static final Map<UUID, Float> SNEAK_ANGLES = new ConcurrentHashMap<>();

    private static final float SNEAK_TARGET = (float) Math.toRadians(20);

    private static final float SNEAK_SMOOTHING = 0.35f;

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

        // The arm points where the head looks
        // and changes direction when sneaking
        float sneakAngle = getSneakAngle(entity);

        armPart.xRot = model.head.xRot
                - (float) Math.PI / 2.0f
                - sneakAngle;
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

    public static void updateSneakAngle(LivingEntity entity) {

        float current = SNEAK_ANGLES.getOrDefault(
                entity.getUUID(),
                0.0f);

        float target = entity.isShiftKeyDown()
                ? SNEAK_TARGET
                : 0.0f;

        current += (target - current)
                * SNEAK_SMOOTHING;

        // Snap very tiny differences to the target.
        if (Math.abs(target - current) < 0.001f) {
            current = target;
        }

        SNEAK_ANGLES.put(
                entity.getUUID(),
                current);
    }

    private static float getSneakAngle(
            LivingEntity entity) {

        return SNEAK_ANGLES.getOrDefault(
                entity.getUUID(),
                0.0f);
    }

}