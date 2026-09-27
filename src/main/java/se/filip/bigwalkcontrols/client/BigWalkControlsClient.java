package se.filip.bigwalkcontrols.client;

import java.util.List;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.common.util.Lazy;

import se.filip.bigwalkcontrols.BigWalkControls;
import se.filip.bigwalkcontrols.network.ArmNetwork;

@EventBusSubscriber(modid = BigWalkControls.MODID, value = Dist.CLIENT)
public class BigWalkControlsClient {

        private static boolean emoteMode = false;

        private static final int RESYNC_INTERVAL_TICKS = 20;
        private static int syncTicks = 0;

        private static ArmNetwork.ArmState lastLeft = ArmNetwork.ArmState.NORMAL;
        private static ArmNetwork.ArmState lastRight = ArmNetwork.ArmState.NORMAL;

        // Flag for seeing if you are rendering your own body
        private static boolean renderingOwnBody = false;

        // Saved model old visibility when changing it to not render for fp-view
        private static boolean oldHeadVisible;
        private static boolean oldHatVisible;
        private static boolean oldBodyVisible;
        private static boolean oldJacketVisible;
        private static boolean oldLeftLegVisible;
        private static boolean oldRightLegVisible;
        private static boolean oldLeftPantsVisible;
        private static boolean oldRightPantsVisible;
        private static boolean oldLeftArmVisible;
        private static boolean oldRightArmVisible;
        private static boolean oldLeftSleeveVisible;
        private static boolean oldRightSleeveVisible;

        // ------------------------------------------------------------
        // KEY MAPPINGS
        // ------------------------------------------------------------

        public static final Lazy<KeyMapping> EMOTE_MODE_KEY = Lazy.of(() -> new KeyMapping(
                        "key.bigwalkcontrols.emote_mode",
                        InputConstants.Type.KEYSYM,
                        GLFW.GLFW_KEY_TAB,
                        "key.categories.bigwalkcontrols"));

        public static final Lazy<KeyMapping> LEFT_HAND_UPP = Lazy.of(() -> new KeyMapping(
                        "key.bigwalkcontrols.left_hand_up",
                        InputConstants.Type.KEYSYM,
                        GLFW.GLFW_KEY_Q,
                        "key.categories.bigwalkcontrols"));

        public static final Lazy<KeyMapping> RIGHT_HAND_UPP = Lazy.of(() -> new KeyMapping(
                        "key.bigwalkcontrols.right_hand_up",
                        InputConstants.Type.KEYSYM,
                        GLFW.GLFW_KEY_E,
                        "key.categories.bigwalkcontrols"));

        public static final Lazy<KeyMapping> LEFT_HAND_FORWARD = Lazy.of(() -> new KeyMapping(
                        "key.bigwalkcontrols.left_hand_forward",
                        InputConstants.Type.MOUSE,
                        GLFW.GLFW_MOUSE_BUTTON_LEFT,
                        "key.categories.bigwalkcontrols"));

        public static final Lazy<KeyMapping> RIGHT_HAND_FORWARD = Lazy.of(() -> new KeyMapping(
                        "key.bigwalkcontrols.right_hand_forward",
                        InputConstants.Type.MOUSE,
                        GLFW.GLFW_MOUSE_BUTTON_RIGHT,
                        "key.categories.bigwalkcontrols"));

        private static List<KeyMapping> getEmoteKeys() {
                return List.of(
                                LEFT_HAND_UPP.get(),
                                RIGHT_HAND_UPP.get(),
                                LEFT_HAND_FORWARD.get(),
                                RIGHT_HAND_FORWARD.get());
        }

        // ------------------------------------------------------------
        // ARM STATE
        // ------------------------------------------------------------

        private static ArmNetwork.ArmState getArmState(
                        KeyMapping up,
                        KeyMapping forward) {

                boolean isUp = up.isDown();
                boolean isForward = forward.isDown();

                if (isUp && isForward) {
                        return ArmNetwork.ArmState.SIDE;
                }

                if (isUp) {
                        return ArmNetwork.ArmState.UP;
                }

                if (isForward) {
                        return ArmNetwork.ArmState.FORWARD;
                }

                return ArmNetwork.ArmState.NORMAL;
        }

        /**
         * Stops player from using arms while doing other stuff with arms
         * mostly when the arms are in a wierd position from the head
         */
        private static boolean canUseEmotePoses(Player player) {
                return player != null
                                && !player.isVisuallySwimming()
                                && !player.isFallFlying()
                                && !player.isSleeping();
        }

        private static ArmNetwork.ArmState getLocalLeftState() {
                Minecraft mc = Minecraft.getInstance();

                if (!emoteMode || !canUseEmotePoses(mc.player)) {
                        return ArmNetwork.ArmState.NORMAL;
                }

                return getArmState(
                                LEFT_HAND_UPP.get(),
                                LEFT_HAND_FORWARD.get());
        }

        private static ArmNetwork.ArmState getLocalRightState() {
                Minecraft mc = Minecraft.getInstance();

                if (!emoteMode || !canUseEmotePoses(mc.player)) {
                        return ArmNetwork.ArmState.NORMAL;
                }

                return getArmState(
                                RIGHT_HAND_UPP.get(),
                                RIGHT_HAND_FORWARD.get());
        }

        // ------------------------------------------------------------
        // CLIENT TICK + NETWORK SYNC
        // ------------------------------------------------------------

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {

                while (EMOTE_MODE_KEY.get().consumeClick()) {
                        emoteMode = !emoteMode;
                }

                Minecraft mc = Minecraft.getInstance();

                if (mc.player == null) {
                        return;
                }

                ArmNetwork.ArmState left = getLocalLeftState();
                ArmNetwork.ArmState right = getLocalRightState();

                syncTicks++;

                boolean changed = left != lastLeft
                                || right != lastRight;

                // While emote mode is active, occasionally resend the pose so a player
                // who has just entered tracking range receives the current state.
                boolean shouldResync = emoteMode
                                && syncTicks >= RESYNC_INTERVAL_TICKS;

                if (changed || shouldResync) {
                        ArmNetwork.send(left, right);

                        lastLeft = left;
                        lastRight = right;
                        syncTicks = 0;
                }
        }

        // ------------------------------------------------------------
        // BLOCK VANILLA/OTHER KEY MAPPINGS WHILE IN EMOTE MODE
        // ------------------------------------------------------------

        @SubscribeEvent
        public static void onClientTickPre(ClientTickEvent.Pre event) {

                if (!emoteMode) {
                        return;
                }

                Minecraft mc = Minecraft.getInstance();
                List<KeyMapping> emoteKeys = getEmoteKeys();

                for (KeyMapping other : mc.options.keyMappings) {

                        // Never suppress our own mappings
                        if (emoteKeys.contains(other)) {
                                continue;
                        }

                        for (KeyMapping ours : emoteKeys) {

                                if (other.getKey().equals(ours.getKey())) {

                                        while (other.consumeClick()) {
                                                // Consume all queued clicks from the conflicting mapping
                                        }

                                        other.setDown(false);
                                        break;
                                }
                        }
                }
        }

        // ------------------------------------------------------------
        // BLOCK ATTACK / USE
        // ------------------------------------------------------------

        @SubscribeEvent
        public static void onInteractionKey(
                        InputEvent.InteractionKeyMappingTriggered event) {

                if (!emoteMode) {
                        return;
                }

                for (KeyMapping ours : getEmoteKeys()) {

                        if (event.getKeyMapping().getKey().equals(ours.getKey())) {
                                event.setCanceled(true);
                                event.setSwingHand(false);
                                return;
                        }
                }
        }

        // ------------------------------------------------------------
        // HIDE VANILLA FIRST-PERSON HAND / ITEM
        // ------------------------------------------------------------

        @SubscribeEvent
        public static void onRenderArm(RenderArmEvent event) {

                if (emoteMode && canUseEmotePoses(Minecraft.getInstance().player)) {
                        event.setCanceled(true);
                }
        }

        @SubscribeEvent
        public static void onRenderHand(RenderHandEvent event) {

                if (emoteMode && canUseEmotePoses(Minecraft.getInstance().player)) {
                        event.setCanceled(true);
                }
        }

        // ------------------------------------------------------------
        // HELPERS USED BY HumanoidArmorLayerMixin
        // ------------------------------------------------------------

        public static boolean isRenderingOwnBody() {
                return renderingOwnBody;
        }

        public static boolean shouldRenderLeftFirstPersonArm() {
                return renderingOwnBody
                                && getLocalLeftState() != ArmNetwork.ArmState.NORMAL;
        }

        public static boolean shouldRenderRightFirstPersonArm() {
                return renderingOwnBody
                                && getLocalRightState() != ArmNetwork.ArmState.NORMAL;
        }

        // ------------------------------------------------------------
        // PLAYER ARM POSES
        // ------------------------------------------------------------

        @SubscribeEvent
        public static void onRenderPlayer(RenderPlayerEvent.Pre event) {

                var model = event.getRenderer().getModel();

                ArmNetwork.ArmState left;
                ArmNetwork.ArmState right;

                if (event.getEntity() == Minecraft.getInstance().player) {
                        left = getLocalLeftState();
                        right = getLocalRightState();
                } else {
                        ArmNetwork.ArmPair state = ArmNetwork.getState(event.getEntity().getUUID());

                        left = state.left();
                        right = state.right();
                }

                switch (left) {
                        case UP ->
                                model.leftArmPose = ClientArmPoses.ARM_UP.getValue();

                        case FORWARD ->
                                model.leftArmPose = ClientArmPoses.ARM_FORWARD.getValue();

                        case SIDE ->
                                model.leftArmPose = ClientArmPoses.ARM_SIDE.getValue();

                        case NORMAL -> {
                                // Keep vanilla's current arm pose
                        }
                }

                switch (right) {
                        case UP ->
                                model.rightArmPose = ClientArmPoses.ARM_UP.getValue();

                        case FORWARD ->
                                model.rightArmPose = ClientArmPoses.ARM_FORWARD.getValue();

                        case SIDE ->
                                model.rightArmPose = ClientArmPoses.ARM_SIDE.getValue();

                        case NORMAL -> {
                                // Keep vanilla's current arm pose
                        }
                }

                // Hide the whole player except whichever emote arms are currently active

                if (renderingOwnBody
                                && event.getEntity() == Minecraft.getInstance().player) {

                        oldHeadVisible = model.head.visible;
                        oldHatVisible = model.hat.visible;
                        oldBodyVisible = model.body.visible;
                        oldJacketVisible = model.jacket.visible;

                        oldLeftLegVisible = model.leftLeg.visible;
                        oldRightLegVisible = model.rightLeg.visible;

                        oldLeftPantsVisible = model.leftPants.visible;
                        oldRightPantsVisible = model.rightPants.visible;

                        oldLeftArmVisible = model.leftArm.visible;
                        oldRightArmVisible = model.rightArm.visible;

                        oldLeftSleeveVisible = model.leftSleeve.visible;
                        oldRightSleeveVisible = model.rightSleeve.visible;

                        model.head.visible = false;
                        model.hat.visible = false;

                        model.body.visible = false;
                        model.jacket.visible = false;

                        model.leftLeg.visible = false;
                        model.rightLeg.visible = false;

                        model.leftPants.visible = false;
                        model.rightPants.visible = false;

                        boolean showLeft = left != ArmNetwork.ArmState.NORMAL;

                        boolean showRight = right != ArmNetwork.ArmState.NORMAL;

                        model.leftArm.visible = oldLeftArmVisible && showLeft;

                        model.leftSleeve.visible = oldLeftSleeveVisible && showLeft;

                        model.rightArm.visible = oldRightArmVisible && showRight;

                        model.rightSleeve.visible = oldRightSleeveVisible && showRight;
                }
        }

        @SubscribeEvent
        public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {

                if (!renderingOwnBody
                                || event.getEntity() != Minecraft.getInstance().player) {
                        return;
                }

                var model = event.getRenderer().getModel();

                model.head.visible = oldHeadVisible;
                model.hat.visible = oldHatVisible;

                model.body.visible = oldBodyVisible;
                model.jacket.visible = oldJacketVisible;

                model.leftLeg.visible = oldLeftLegVisible;
                model.rightLeg.visible = oldRightLegVisible;

                model.leftPants.visible = oldLeftPantsVisible;
                model.rightPants.visible = oldRightPantsVisible;

                model.leftArm.visible = oldLeftArmVisible;
                model.rightArm.visible = oldRightArmVisible;

                model.leftSleeve.visible = oldLeftSleeveVisible;
                model.rightSleeve.visible = oldRightSleeveVisible;
        }

        // ------------------------------------------------------------
        // EXTRA FIRST-PERSON SELF RENDER
        // ------------------------------------------------------------

        @SubscribeEvent
        public static void renderOwnBody(RenderLevelStageEvent event) {

                if (!emoteMode
                                || event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
                        return;
                }

                Minecraft mc = Minecraft.getInstance();
                ArmNetwork.ArmState left = getLocalLeftState();
                ArmNetwork.ArmState right = getLocalRightState();

                // If both arms are normal there is nothing for our extra render to draw.
                if (left == ArmNetwork.ArmState.NORMAL
                                && right == ArmNetwork.ArmState.NORMAL) {
                        return;
                }

                if (mc.player == null
                                || mc.level == null
                                || !mc.options.getCameraType().isFirstPerson()
                                || !canUseEmotePoses(mc.player)
                                || mc.player.isSpectator()) {
                        return;
                }

                var player = mc.player;
                var camera = event.getCamera();
                var poseStack = event.getPoseStack();

                if (poseStack == null) {
                        return;
                }

                float partialTick = event.getPartialTick()
                                .getGameTimeDeltaPartialTick(false);

                var dispatcher = mc.getEntityRenderDispatcher();
                var cameraPos = camera.getPosition();

                double playerX = Mth.lerp(
                                partialTick,
                                player.xOld,
                                player.getX());

                double playerY = Mth.lerp(
                                partialTick,
                                player.yOld,
                                player.getY());

                double playerZ = Mth.lerp(
                                partialTick,
                                player.zOld,
                                player.getZ());

                double x = playerX - cameraPos.x;
                double y = playerY - cameraPos.y;
                double z = playerZ - cameraPos.z;

                int light = dispatcher.getPackedLightCoords(
                                player,
                                partialTick);

                // Save the user's setting for the shadow so we can restore it when we are done
                boolean oldRenderShadow = mc.options.entityShadows().get();

                poseStack.pushPose();

                renderingOwnBody = true;
                dispatcher.setRenderShadow(false);

                try {
                        dispatcher.render(
                                        player,
                                        x,
                                        y,
                                        z,
                                        player.getYRot(),
                                        partialTick,
                                        poseStack,
                                        mc.renderBuffers().bufferSource(),
                                        light);

                } finally {
                        dispatcher.setRenderShadow(oldRenderShadow);
                        renderingOwnBody = false;
                        poseStack.popPose();
                }
        }

        // ------------------------------------------------------------
        // MOD BUS EVENTS
        // ------------------------------------------------------------

        @EventBusSubscriber(modid = BigWalkControls.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
        public static class ModEvents {

                @SubscribeEvent
                public static void registerBindings(
                                RegisterKeyMappingsEvent event) {

                        event.register(EMOTE_MODE_KEY.get());

                        event.register(LEFT_HAND_UPP.get());
                        event.register(RIGHT_HAND_UPP.get());

                        event.register(LEFT_HAND_FORWARD.get());
                        event.register(RIGHT_HAND_FORWARD.get());
                }
        }
}
