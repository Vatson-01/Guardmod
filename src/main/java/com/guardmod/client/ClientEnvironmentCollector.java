package com.guardmod.client;

import com.guardmod.model.ClientEnvironmentReport;
import com.guardmod.model.ClientModEntry;
import com.guardmod.net.GuardNetwork;
import com.guardmod.net.packet.C2SEnvironmentReportPacket;
import com.guardmod.client.ClientShaderPackCollector;
import com.guardmod.net.packet.S2CRequestEnvironmentPacket;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModInfo;

import java.util.List;

public final class ClientEnvironmentCollector {
    private ClientEnvironmentCollector() {
    }

    public static void handleEnvironmentRequest(S2CRequestEnvironmentPacket packet) {
        List<ClientModEntry> loadedMods = ClientModCollector.collectLoadedMods();
        ClientResourcePackCollector.Result resourcePackResult = ClientResourcePackCollector.collectActiveResourcePacks();
        ClientShaderPackCollector.Result shaderPackResult = ClientShaderPackCollector.collectShaderPackState();
        String guardModVersion = resolveGuardModVersion();

        ClientEnvironmentReport report = new ClientEnvironmentReport(
                GuardNetwork.getProtocolVersionNumber(),
                guardModVersion,
                loadedMods,
                resourcePackResult.getActivePacks(),
                resourcePackResult.getOrder(),
                ClientResourcePackCollector.collectInstalledExternalResourcePacks(),
                shaderPackResult.getRuntime(),
                shaderPackResult.getActiveShaderPack(),
                shaderPackResult.getInstalledShaderPacks()
        );

        GuardNetwork.CHANNEL.sendToServer(new C2SEnvironmentReportPacket(packet.getRequestId(), report));
    }

    private static String resolveGuardModVersion() {
        for (IModInfo modInfo : ModList.get().getMods()) {
            if ("guardmod".equalsIgnoreCase(modInfo.getModId())) {
                return modInfo.getVersion() == null ? "" : modInfo.getVersion().toString();
            }
        }
        return "";
    }

    public static void sendReactiveEnvironmentReport() {
        sendReactiveEnvironmentReport(ClientResourcePackCollector.collectActiveResourcePacks());
    }

    public static void sendReactiveEnvironmentReport(ClientResourcePackCollector.Result resourcePackResult) {
        List<ClientModEntry> loadedMods = ClientModCollector.collectLoadedMods();
        String guardModVersion = resolveGuardModVersion();

        ClientResourcePackCollector.Result safeResult =
                resourcePackResult == null ? ClientResourcePackCollector.Result.empty() : resourcePackResult;

        ClientShaderPackCollector.Result shaderPackResult = ClientShaderPackCollector.collectShaderPackState();

        ClientEnvironmentReport report = new ClientEnvironmentReport(
                GuardNetwork.getProtocolVersionNumber(),
                guardModVersion,
                loadedMods,
                safeResult.getActivePacks(),
                safeResult.getOrder(),
                ClientResourcePackCollector.collectInstalledExternalResourcePacks(),
                shaderPackResult.getRuntime(),
                shaderPackResult.getActiveShaderPack(),
                shaderPackResult.getInstalledShaderPacks()
        );
        GuardNetwork.CHANNEL.sendToServer(new C2SEnvironmentReportPacket(0L, report));
    }
}