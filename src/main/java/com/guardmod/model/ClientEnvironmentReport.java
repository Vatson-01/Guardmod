package com.guardmod.model;

import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ClientEnvironmentReport {
    private final int protocolVersion;
    private final String clientModVersion;
    private final List<ClientModEntry> mods;
    private final List<ClientResourcePackEntry> activeResourcePacks;
    private final List<String> activeResourcePackOrder;
    private final List<ClientResourcePackEntry> installedResourcePacks;
    private final String shaderRuntime;
    private final ClientShaderPackEntry activeShaderPack;
    private final List<ClientShaderPackEntry> installedShaderPacks;

    public ClientEnvironmentReport(int protocolVersion,
                                   String clientModVersion,
                                   List<ClientModEntry> mods,
                                   List<ClientResourcePackEntry> activeResourcePacks,
                                   List<String> activeResourcePackOrder,
                                   List<ClientResourcePackEntry> installedResourcePacks,
                                   String shaderRuntime,
                                   ClientShaderPackEntry activeShaderPack,
                                   List<ClientShaderPackEntry> installedShaderPacks) {
        this.protocolVersion = protocolVersion;
        this.clientModVersion = clientModVersion;
        this.mods = mods == null ? Collections.<ClientModEntry>emptyList() : new ArrayList<ClientModEntry>(mods);
        this.activeResourcePacks = activeResourcePacks == null
                ? Collections.<ClientResourcePackEntry>emptyList()
                : new ArrayList<ClientResourcePackEntry>(activeResourcePacks);
        this.activeResourcePackOrder = activeResourcePackOrder == null
                ? Collections.<String>emptyList()
                : new ArrayList<String>(activeResourcePackOrder);
        this.installedResourcePacks = installedResourcePacks == null
                ? Collections.<ClientResourcePackEntry>emptyList()
                : new ArrayList<ClientResourcePackEntry>(installedResourcePacks);
        this.shaderRuntime = shaderRuntime == null ? "" : shaderRuntime;
        this.activeShaderPack = activeShaderPack;
        this.installedShaderPacks = installedShaderPacks == null
                ? Collections.<ClientShaderPackEntry>emptyList()
                : new ArrayList<ClientShaderPackEntry>(installedShaderPacks);
    }

    public int getProtocolVersion() {
        return protocolVersion;
    }

    public String getClientModVersion() {
        return clientModVersion;
    }

    public List<ClientModEntry> getMods() {
        return Collections.unmodifiableList(mods);
    }

    public List<ClientResourcePackEntry> getActiveResourcePacks() {
        return Collections.unmodifiableList(activeResourcePacks);
    }

    public List<String> getActiveResourcePackOrder() {
        return Collections.unmodifiableList(activeResourcePackOrder);
    }
    public List<ClientResourcePackEntry> getInstalledResourcePacks() {
        return Collections.unmodifiableList(installedResourcePacks);
    }
    public String getShaderRuntime() {
        return shaderRuntime;
    }

    public ClientShaderPackEntry getActiveShaderPack() {
        return activeShaderPack;
    }

    public List<ClientShaderPackEntry> getInstalledShaderPacks() {
        return Collections.unmodifiableList(installedShaderPacks);
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(protocolVersion);
        buffer.writeUtf(clientModVersion == null ? "" : clientModVersion, 256);

        buffer.writeInt(mods.size());
        for (ClientModEntry mod : mods) {
            mod.encode(buffer);
        }

        buffer.writeInt(activeResourcePacks.size());
        for (ClientResourcePackEntry pack : activeResourcePacks) {
            pack.encode(buffer);
        }

        buffer.writeInt(activeResourcePackOrder.size());
        for (String entry : activeResourcePackOrder) {
            buffer.writeUtf(entry == null ? "" : entry, 512);
        }

        buffer.writeInt(installedResourcePacks.size());
        for (ClientResourcePackEntry pack : installedResourcePacks) {
            pack.encode(buffer);
        }

        buffer.writeUtf(shaderRuntime == null ? "" : shaderRuntime, 64);
        buffer.writeBoolean(activeShaderPack != null);
        if (activeShaderPack != null) {
            activeShaderPack.encode(buffer);
        }

        buffer.writeInt(installedShaderPacks.size());
        for (ClientShaderPackEntry pack : installedShaderPacks) {
            pack.encode(buffer);
        }
    }

    public static ClientEnvironmentReport decode(FriendlyByteBuf buffer) {
        int protocolVersion = buffer.readInt();
        String clientModVersion = buffer.readUtf(256);

        int modCount = buffer.readInt();
        List<ClientModEntry> mods = new ArrayList<ClientModEntry>(modCount);
        for (int i = 0; i < modCount; i++) {
            mods.add(ClientModEntry.decode(buffer));
        }

        int resourcePackCount = buffer.readInt();
        List<ClientResourcePackEntry> activeResourcePacks = new ArrayList<ClientResourcePackEntry>(resourcePackCount);
        for (int i = 0; i < resourcePackCount; i++) {
            activeResourcePacks.add(ClientResourcePackEntry.decode(buffer));
        }

        int orderCount = buffer.readInt();
        List<String> activeResourcePackOrder = new ArrayList<String>(orderCount);
        for (int i = 0; i < orderCount; i++) {
            activeResourcePackOrder.add(buffer.readUtf(512));
        }

        int installedCount = buffer.readInt();
        List<ClientResourcePackEntry> installedResourcePacks = new ArrayList<ClientResourcePackEntry>(installedCount);
        for (int i = 0; i < installedCount; i++) {
            installedResourcePacks.add(ClientResourcePackEntry.decode(buffer));
        }

        String shaderRuntime = buffer.readUtf(64);
        ClientShaderPackEntry activeShaderPack = buffer.readBoolean() ? ClientShaderPackEntry.decode(buffer) : null;

        int installedShaderCount = buffer.readInt();
        List<ClientShaderPackEntry> installedShaderPacks = new ArrayList<ClientShaderPackEntry>(installedShaderCount);
        for (int i = 0; i < installedShaderCount; i++) {
            installedShaderPacks.add(ClientShaderPackEntry.decode(buffer));
        }

        return new ClientEnvironmentReport(
                protocolVersion,
                clientModVersion,
                mods,
                activeResourcePacks,
                activeResourcePackOrder,
                installedResourcePacks,
                shaderRuntime,
                activeShaderPack,
                installedShaderPacks
        );
    }
}