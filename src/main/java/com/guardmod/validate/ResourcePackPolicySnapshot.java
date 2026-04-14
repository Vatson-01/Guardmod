package com.guardmod.validate;

import com.guardmod.model.ScannedResourcePack;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ResourcePackPolicySnapshot {
    private final boolean ready;
    private final long scannedAtEpochMillis;
    private final String statusMessage;
    private final Map<String, ScannedResourcePack> allowedPacksByContentHash;

    private ResourcePackPolicySnapshot(boolean ready,
                                       long scannedAtEpochMillis,
                                       String statusMessage,
                                       Map<String, ScannedResourcePack> allowedPacksByContentHash) {
        this.ready = ready;
        this.scannedAtEpochMillis = scannedAtEpochMillis;
        this.statusMessage = statusMessage == null ? "" : statusMessage;
        this.allowedPacksByContentHash = allowedPacksByContentHash == null
                ? Collections.<String, ScannedResourcePack>emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<String, ScannedResourcePack>(allowedPacksByContentHash));
    }

    public static ResourcePackPolicySnapshot ready(Map<String, ScannedResourcePack> allowedPacksByContentHash,
                                                   String statusMessage) {
        return new ResourcePackPolicySnapshot(true, System.currentTimeMillis(), statusMessage, allowedPacksByContentHash);
    }

    public static ResourcePackPolicySnapshot failed(String statusMessage) {
        return new ResourcePackPolicySnapshot(false, System.currentTimeMillis(), statusMessage, null);
    }

    public boolean isReady() {
        return ready;
    }

    public long getScannedAtEpochMillis() {
        return scannedAtEpochMillis;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public Map<String, ScannedResourcePack> getAllowedPacksByContentHash() {
        return allowedPacksByContentHash;
    }
}
