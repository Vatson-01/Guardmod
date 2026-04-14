package com.guardmod.client;

import com.guardmod.net.packet.S2CRequestEnvironmentPacket;
import com.guardmod.net.packet.S2CValidationResultPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class ClientPacketHandlers {
    private ClientPacketHandlers() {
    }

    public static void handleEnvironmentRequest(S2CRequestEnvironmentPacket packet) {
        ClientEnvironmentCollector.handleEnvironmentRequest(packet);
    }

    public static void handleValidationResult(S2CValidationResultPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && packet.getMessage() != null && !packet.getMessage().isEmpty()) {
            minecraft.player.displayClientMessage(Component.literal(packet.getMessage()), true);
        }
    }
}
