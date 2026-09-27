package se.filip.bigwalkcontrols.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.EquipmentSlot;

import se.filip.bigwalkcontrols.client.BigWalkControlsClient;

// Keep only the shoulder/arm pieces for the arms that are currently active

@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerMixin {

    @Inject(method = "setPartVisibility", at = @At("TAIL"))
    private void bigwalkcontrols$limitFirstPersonArmor(
            HumanoidModel<?> model,
            EquipmentSlot slot,
            CallbackInfo ci) {

        if (!BigWalkControlsClient.isRenderingOwnBody()) {
            return;
        }

        model.setAllVisible(false);

        if (slot == EquipmentSlot.CHEST) {
            model.leftArm.visible = BigWalkControlsClient.shouldRenderLeftFirstPersonArm();

            model.rightArm.visible = BigWalkControlsClient.shouldRenderRightFirstPersonArm();
        }
    }
}
