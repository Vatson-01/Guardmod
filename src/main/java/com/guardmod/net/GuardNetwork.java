package com.guardmod.net;

import com.guardmod.GuardMod;
import com.guardmod.net.packet.C2SEnvironmentReportPacket;
import com.guardmod.net.packet.S2CRequestEnvironmentPacket;
import com.guardmod.net.packet.S2CValidationResultPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class GuardNetwork {
    private static final String PROTOCOL_VERSION = "3";
    private static final int PROTOCOL_VERSION_NUMBER = 3;

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(GuardMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static boolean initialized = false;
    private static int packetId = 0;

    private GuardNetwork() {
    }

    public static void init() {
        if (initialized) {
            return;
        }

        CHANNEL.messageBuilder(S2CRequestEnvironmentPacket.class, nextPacketId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CRequestEnvironmentPacket::encode)
                .decoder(S2CRequestEnvironmentPacket::decode)
                .consumerMainThread(S2CRequestEnvironmentPacket::handle)
                .add();

        CHANNEL.messageBuilder(C2SEnvironmentReportPacket.class, nextPacketId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SEnvironmentReportPacket::encode)
                .decoder(C2SEnvironmentReportPacket::decode)
                .consumerMainThread(C2SEnvironmentReportPacket::handle)
                .add();

        CHANNEL.messageBuilder(S2CValidationResultPacket.class, nextPacketId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CValidationResultPacket::encode)
                .decoder(S2CValidationResultPacket::decode)
                .consumerMainThread(S2CValidationResultPacket::handle)
                .add();

        initialized = true;
    }

    private static int nextPacketId() {
        return packetId++;
    }

    public static int getProtocolVersionNumber() {
        return PROTOCOL_VERSION_NUMBER;
    }

    public static void sendEnvironmentRequest(ServerPlayer player, long requestId, long deadlineEpochMillis) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new S2CRequestEnvironmentPacket(requestId, deadlineEpochMillis));
    }

    public static void sendValidationResult(ServerPlayer player, boolean success, String message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new S2CValidationResultPacket(success, message));
    }
}
