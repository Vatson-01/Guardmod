package com.guardmod.validate;

import com.guardmod.model.ScannedShaderPack;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ShaderPackPolicySnapshot {
    private final boolean ready;
    private final Map<String, ScannedShaderPack> allowedPacksByContentHash;
    private final String statusMessage;

    private ShaderPackPolicySnapshot(boolean ready,
                                     Map<String, ScannedShaderPack> allowedPacksByContentHash,
                                     String statusMessage) {
        this.ready = ready;
        this.allowedPacksByContentHash = allowedPacksByContentHash == null
                ? Collections.<String, ScannedShaderPack>emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<String, ScannedShaderPack>(allowedPacksByContentHash));
        this.statusMessage = statusMessage == null ? "" : statusMessage;
    }

    public static ShaderPackPolicySnapshot ready(Map<String, ScannedShaderPack> allowedPacksByContentHash,
                                                 String statusMessage) {
        return new ShaderPackPolicySnapshot(true, allowedPacksByContentHash, statusMessage);
    }

    public static ShaderPackPolicySnapshot failed(String statusMessage) {
        return new ShaderPackPolicySnapshot(false, Collections.<String, ScannedShaderPack>emptyMap(), statusMessage);
    }

    public boolean isReady() {
        return ready;
    }

    public Map<String, ScannedShaderPack> getAllowedPacksByContentHash() {
        return allowedPacksByContentHash;
    }

    public String getStatusMessage() {
        return statusMessage;
    }
}
