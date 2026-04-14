package com.guardmod.model;

import net.minecraft.network.FriendlyByteBuf;

public class ClientShaderPackEntry {
    private final String displayName;
    private final String sourceType;
    private final String rawFileHash;
    private final String contentHash;

    public ClientShaderPackEntry(String displayName, String sourceType, String rawFileHash, String contentHash) {
        this.displayName = displayName == null ? "" : displayName;
        this.sourceType = sourceType == null ? "" : sourceType;
        this.rawFileHash = rawFileHash == null ? "" : rawFileHash;
        this.contentHash = contentHash == null ? "" : contentHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getSourceType() {
        return sourceType;
    }

    public String getRawFileHash() {
        return rawFileHash;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(displayName, 512);
        buffer.writeUtf(sourceType, 64);
        buffer.writeUtf(rawFileHash, 256);
        buffer.writeUtf(contentHash, 256);
    }

    public static ClientShaderPackEntry decode(FriendlyByteBuf buffer) {
        return new ClientShaderPackEntry(
                buffer.readUtf(512),
                buffer.readUtf(64),
                buffer.readUtf(256),
                buffer.readUtf(256)
        );
    }
}
