package com.guardmod.model;

public class ExpectedModEntry {
    private final String modId;
    private final String version;
    private final String displayName;
    private final String jarHash;
    private final String fileName;
    private final boolean required;

    public ExpectedModEntry(String modId, String version, String displayName, String jarHash, String fileName, boolean required) {
        this.modId = modId == null ? "" : modId;
        this.version = version == null ? "" : version;
        this.displayName = displayName == null ? "" : displayName;
        this.jarHash = jarHash == null ? "" : jarHash;
        this.fileName = fileName == null ? "" : fileName;
        this.required = required;
    }

    public String getModId() {
        return modId;
    }

    public String getVersion() {
        return version;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getJarHash() {
        return jarHash;
    }

    public String getFileName() {
        return fileName;
    }

    public boolean isRequired() {
        return required;
    }
}
