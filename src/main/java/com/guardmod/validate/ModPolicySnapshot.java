package com.guardmod.validate;

import com.guardmod.model.ExpectedModEntry;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ModPolicySnapshot {
    private final boolean ready;
    private final long scannedAtEpochMillis;
    private final String statusMessage;
    private final Map<String, ExpectedModEntry> requiredMods;
    private final Map<String, ExpectedModEntry> allowedOptionalMods;

    private ModPolicySnapshot(boolean ready,
                              long scannedAtEpochMillis,
                              String statusMessage,
                              Map<String, ExpectedModEntry> requiredMods,
                              Map<String, ExpectedModEntry> allowedOptionalMods) {
        this.ready = ready;
        this.scannedAtEpochMillis = scannedAtEpochMillis;
        this.statusMessage = statusMessage == null ? "" : statusMessage;
        this.requiredMods = requiredMods == null
                ? Collections.<String, ExpectedModEntry>emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<String, ExpectedModEntry>(requiredMods));
        this.allowedOptionalMods = allowedOptionalMods == null
                ? Collections.<String, ExpectedModEntry>emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<String, ExpectedModEntry>(allowedOptionalMods));
    }

    public static ModPolicySnapshot ready(Map<String, ExpectedModEntry> requiredMods,
                                          Map<String, ExpectedModEntry> allowedOptionalMods,
                                          String statusMessage) {
        return new ModPolicySnapshot(true, System.currentTimeMillis(), statusMessage, requiredMods, allowedOptionalMods);
    }

    public static ModPolicySnapshot failed(String statusMessage) {
        return new ModPolicySnapshot(false, System.currentTimeMillis(), statusMessage, null, null);
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

    public Map<String, ExpectedModEntry> getRequiredMods() {
        return requiredMods;
    }

    public Map<String, ExpectedModEntry> getAllowedOptionalMods() {
        return allowedOptionalMods;
    }
}
