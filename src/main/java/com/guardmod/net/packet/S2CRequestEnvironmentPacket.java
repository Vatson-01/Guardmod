package com.guardmod.net.packet;

import com.guardmod.client.ClientPacketHandlers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class S2CRequestEnvironmentPacket {
    private final long requestId;
    private final long deadlineEpochMillis;

    public S2CRequestEnvironmentPacket(long requestId, long deadlineEpochMillis) {
        this.requestId = requestId;
        this.deadlineEpochMillis = deadlineEpochMillis;
    }

    public long getRequestId() {
        return requestId;
    }

    public long getDeadlineEpochMillis() {
        return deadlineEpochMillis;
    }

    public static void encode(S2CRequestEnvironmentPacket packet, FriendlyByteBuf buffer) {
        buffer.writeLong(packet.requestId);
        buffer.writeLong(packet.deadlineEpochMillis);
    }

    public static S2CRequestEnvironmentPacket decode(FriendlyByteBuf buffer) {
        return new S2CRequestEnvironmentPacket(buffer.readLong(), buffer.readLong());
    }

    public static void handle(S2CRequestEnvironmentPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandlers.handleEnvironmentRequest(packet)));
        context.setPacketHandled(true);
    }
}
