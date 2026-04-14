package com.guardmod.net.packet;

import com.guardmod.client.ClientPacketHandlers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class S2CValidationResultPacket {
    private final boolean success;
    private final String message;

    public S2CValidationResultPacket(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public static void encode(S2CValidationResultPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.success);
        buffer.writeUtf(packet.message == null ? "" : packet.message, 32767);
    }

    public static S2CValidationResultPacket decode(FriendlyByteBuf buffer) {
        return new S2CValidationResultPacket(buffer.readBoolean(), buffer.readUtf(32767));
    }

    public static void handle(S2CValidationResultPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandlers.handleValidationResult(packet)));
        context.setPacketHandled(true);
    }
}
