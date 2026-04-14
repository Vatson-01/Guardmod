package com.guardmod.model;

import java.nio.file.Path;

public class ScannedResourcePack {
    private final Path path;
    private final String name;
    private final String sourceType;
    private final long size;
    private final boolean hasPackMcmeta;
    private final String rawFileHash;
    private final String contentHash;

    public ScannedResourcePack(Path path,
                               String name,
                               String sourceType,
                               long size,
                               boolean hasPackMcmeta,
                               String rawFileHash,
                               String contentHash) {
        this.path = path;
        this.name = name == null ? "" : name;
        this.sourceType = sourceType == null ? "" : sourceType;
        this.size = size;
        this.hasPackMcmeta = hasPackMcmeta;
        this.rawFileHash = rawFileHash == null ? "" : rawFileHash;
        this.contentHash = contentHash == null ? "" : contentHash;
    }

    public Path getPath() {
        return path;
    }

    public String getName() {
        return name;
    }

    public String getSourceType() {
        return sourceType;
    }

    public long getSize() {
        return size;
    }

    public boolean hasPackMcmeta() {
        return hasPackMcmeta;
    }

    public String getRawFileHash() {
        return rawFileHash;
    }

    public String getContentHash() {
        return contentHash;
    }
}
