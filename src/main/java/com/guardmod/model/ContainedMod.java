package com.guardmod.model;

public class ContainedMod {
    private final String modId;
    private final String version;
    private final String displayName;

    public ContainedMod(String modId, String version, String displayName) {
        this.modId = modId == null ? "" : modId;
        this.version = version == null ? "" : version;
        this.displayName = displayName == null ? "" : displayName;
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
}
