package com.guardmod.model;

import net.minecraft.network.FriendlyByteBuf;

public class ClientModEntry {
    private final String modId;
    private final String version;
    private final String jarHash;
    private final String fileName;

    public ClientModEntry(String modId, String version, String jarHash, String fileName) {
        this.modId = modId;
        this.version = version;
        this.jarHash = jarHash;
        this.fileName = fileName;
    }

    public String getModId() {
        return modId;
    }

    public String getVersion() {
        return version;
    }

    public String getJarHash() {
        return jarHash;
    }

    public String getFileName() {
        return fileName;
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(modId == null ? "" : modId, 256);
        buffer.writeUtf(version == null ? "" : version, 256);
        buffer.writeUtf(jarHash == null ? "" : jarHash, 512);
        buffer.writeUtf(fileName == null ? "" : fileName, 512);
    }

    public static ClientModEntry decode(FriendlyByteBuf buffer) {
        return new ClientModEntry(
                buffer.readUtf(256),
                buffer.readUtf(256),
                buffer.readUtf(512),
                buffer.readUtf(512)
        );
    }
}
