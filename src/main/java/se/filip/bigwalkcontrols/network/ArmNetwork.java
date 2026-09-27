package se.filip.bigwalkcontrols.network;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import io.netty.buffer.ByteBuf;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import se.filip.bigwalkcontrols.BigWalkControls;

public final class ArmNetwork {

        public enum ArmState {
                NORMAL,
                UP,
                FORWARD,
                SIDE;

                public static ArmState fromId(int id) {
                        ArmState[] values = values();

                        if (id < 0 || id >= values.length) {
                                return NORMAL;
                        }

                        return values[id];
                }
        }

        public record ArmPair(
                        ArmState left,
                        ArmState right) {
        }

        private static final ArmPair NORMAL_STATE = new ArmPair(
                        ArmState.NORMAL,
                        ArmState.NORMAL);

        private static final Map<UUID, ArmPair> PLAYER_STATES = new ConcurrentHashMap<>();

        // ------------------------------------------------------------
        // CLIENT -> SERVER
        // ------------------------------------------------------------

        public record ArmStateToServer(
                        int left,
                        int right) implements CustomPacketPayload {

                public static final Type<ArmStateToServer> TYPE = new Type<>(
                                ResourceLocation.fromNamespaceAndPath(
                                                BigWalkControls.MODID,
                                                "arm_state_to_server"));

                public static final StreamCodec<ByteBuf, ArmStateToServer> STREAM_CODEC = StreamCodec.composite(
                                ByteBufCodecs.VAR_INT,
                                ArmStateToServer::left,

                                ByteBufCodecs.VAR_INT,
                                ArmStateToServer::right,

                                ArmStateToServer::new);

                @Override
                public Type<? extends CustomPacketPayload> type() {
                        return TYPE;
                }
        }

        // ------------------------------------------------------------
        // SERVER -> CLIENT
        // ------------------------------------------------------------

        public record PlayerArmState(
                        UUID playerId,
                        int left,
                        int right) implements CustomPacketPayload {

                public static final Type<PlayerArmState> TYPE = new Type<>(
                                ResourceLocation.fromNamespaceAndPath(
                                                BigWalkControls.MODID,
                                                "player_arm_state"));

                public static final StreamCodec<ByteBuf, PlayerArmState> STREAM_CODEC = StreamCodec.composite(
                                UUIDUtil.STREAM_CODEC,
                                PlayerArmState::playerId,

                                ByteBufCodecs.VAR_INT,
                                PlayerArmState::left,

                                ByteBufCodecs.VAR_INT,
                                PlayerArmState::right,

                                PlayerArmState::new);

                @Override
                public Type<? extends CustomPacketPayload> type() {
                        return TYPE;
                }
        }

        // ------------------------------------------------------------
        // REGISTRATION
        // ------------------------------------------------------------

        public static void register(
                        RegisterPayloadHandlersEvent event) {

                PayloadRegistrar registrar = event.registrar("1");

                registrar.playToServer(
                                ArmStateToServer.TYPE,
                                ArmStateToServer.STREAM_CODEC,
                                ArmNetwork::handleServer);

                registrar.playToClient(
                                PlayerArmState.TYPE,
                                PlayerArmState.STREAM_CODEC,
                                ArmNetwork::handleClient);
        }

        // ------------------------------------------------------------
        // SERVER
        // ------------------------------------------------------------

        private static void handleServer(
                        ArmStateToServer payload,
                        IPayloadContext context) {

                ServerPlayer player = (ServerPlayer) context.player();

                ArmState left = ArmState.fromId(payload.left());

                ArmState right = ArmState.fromId(payload.right());

                PlayerArmState outgoing = new PlayerArmState(
                                player.getUUID(),
                                left.ordinal(),
                                right.ordinal());

                PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                                player,
                                outgoing);
        }

        // ------------------------------------------------------------
        // CLIENT
        // ------------------------------------------------------------

        private static void handleClient(
                        PlayerArmState payload,
                        IPayloadContext context) {

                ArmState left = ArmState.fromId(payload.left());

                ArmState right = ArmState.fromId(payload.right());

                if (left == ArmState.NORMAL
                                && right == ArmState.NORMAL) {

                        PLAYER_STATES.remove(
                                        payload.playerId());

                        return;
                }

                PLAYER_STATES.put(
                                payload.playerId(),
                                new ArmPair(left, right));
        }

        // ------------------------------------------------------------
        // PUBLIC API
        // ------------------------------------------------------------

        public static void send(
                        ArmState left,
                        ArmState right) {

                PacketDistributor.sendToServer(
                                new ArmStateToServer(
                                                left.ordinal(),
                                                right.ordinal()));
        }

        public static ArmPair getState(
                        UUID playerId) {

                return PLAYER_STATES.getOrDefault(
                                playerId,
                                NORMAL_STATE);
        }
}