package com.guardmod.net.packet;

import com.guardmod.model.ClientEnvironmentReport;
import com.guardmod.validate.ClientEnvironmentValidator;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class C2SEnvironmentReportPacket {
    private final long requestId;
    private final ClientEnvironmentReport report;

    public C2SEnvironmentReportPacket(long requestId, ClientEnvironmentReport report) {
        this.requestId = requestId;
        this.report = report;
    }

    public long getRequestId() {
        return requestId;
    }

    public ClientEnvironmentReport getReport() {
        return report;
    }

    public static void encode(C2SEnvironmentReportPacket packet, FriendlyByteBuf buffer) {
        buffer.writeLong(packet.requestId);
        packet.report.encode(buffer);
    }

    public static C2SEnvironmentReportPacket decode(FriendlyByteBuf buffer) {
        long requestId = buffer.readLong();
        ClientEnvironmentReport report = ClientEnvironmentReport.decode(buffer);
        return new C2SEnvironmentReportPacket(requestId, report);
    }

    public static void handle(C2SEnvironmentReportPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> ClientEnvironmentValidator.validateAndConsume(context.getSender(), packet));
        context.setPacketHandled(true);
    }
}
