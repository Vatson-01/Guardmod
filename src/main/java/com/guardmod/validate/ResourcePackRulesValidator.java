package com.guardmod.validate;

import com.guardmod.config.GuardCommonConfig;
import com.guardmod.model.ClientEnvironmentReport;
import com.guardmod.model.ClientResourcePackEntry;
import com.guardmod.model.ScannedResourcePack;
import com.guardmod.scanner.ResourcePackScanService;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ResourcePackRulesValidator {
    private ResourcePackRulesValidator() {
    }

    public static ValidationResult validateActiveResourcePacks(ClientEnvironmentReport report) {
        if (report == null) {
            return ValidationResult.failure("Client report is missing", ValidationStage.REPORT);
        }

        String mode = normalizeMode(GuardCommonConfig.RESOURCEPACKS_MODE.get());
        if ("OFF".equals(mode)) {
            return ValidationResult.success();
        }

        ResourcePackPolicySnapshot snapshot = ResourcePackScanService.getSnapshot();
        if (snapshot == null || !snapshot.isReady()) {
            String reason = snapshot == null ? "Server resource pack policy is not ready" : snapshot.getStatusMessage();
            return ValidationResult.failure(reason, ValidationStage.RULES);
        }

        List<ClientResourcePackEntry> activePacks = report.getActiveResourcePacks();
        if (activePacks.isEmpty()) {
            if (GuardCommonConfig.REQUIRE_AT_LEAST_ONE_ALLOWED_RESOURCEPACK.get()) {
                return ValidationResult.failure("At least one allowed resource pack must be active", ValidationStage.RULES);
            }
            return ValidationResult.success();
        }

        Map<String, ScannedResourcePack> allowedByContentHash = snapshot.getAllowedPacksByContentHash();
        for (ClientResourcePackEntry entry : activePacks) {
            String normalizedHash = normalizeHash(entry.getContentHash());
            if (normalizedHash.isEmpty()) {
                return ValidationResult.failure("Resource pack content hash is missing: " + safe(entry.getDisplayName()), ValidationStage.RULES);
            }

            ScannedResourcePack expected = allowedByContentHash.get(normalizedHash);
            if (expected == null) {
                return ValidationResult.failure("Active resource pack is not allowed: " + safe(entry.getDisplayName()), ValidationStage.RULES);
            }

            if (GuardCommonConfig.CHECK_RESOURCEPACK_RAW_HASH_TOO.get()) {
                String expectedRawHash = normalizeHash(expected.getRawFileHash());
                String actualRawHash = normalizeHash(entry.getRawFileHash());
                if (!expectedRawHash.isEmpty() && !actualRawHash.isEmpty() && !expectedRawHash.equals(actualRawHash)) {
                    return ValidationResult.failure("Resource pack raw hash mismatch: " + safe(entry.getDisplayName()), ValidationStage.RULES);
                }
            }
        }

        return ValidationResult.success();
    }

    private static String normalizeMode(String value) {
        return safe(value).trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeHash(String value) {
        return safe(value).trim().toLowerCase(Locale.ROOT);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public static java.util.List<ClientResourcePackEntry> collectUnknownInstalledResourcePacks(ClientEnvironmentReport report) {
        java.util.List<ClientResourcePackEntry> result = new java.util.ArrayList<ClientResourcePackEntry>();
        if (report == null) {
            return result;
        }

        ResourcePackPolicySnapshot snapshot = ResourcePackScanService.getSnapshot();
        if (snapshot == null || !snapshot.isReady()) {
            return result;
        }

        Map<String, ScannedResourcePack> allowedByContentHash = snapshot.getAllowedPacksByContentHash();
        java.util.Set<String> seen = new java.util.LinkedHashSet<String>();

        for (ClientResourcePackEntry entry : report.getInstalledResourcePacks()) {
            String normalizedHash = normalizeHash(entry.getContentHash());
            String key = normalizedHash + "|" + safe(entry.getDisplayName());

            if (seen.contains(key)) {
                continue;
            }
            seen.add(key);

            if (normalizedHash.isEmpty() || !allowedByContentHash.containsKey(normalizedHash)) {
                result.add(entry);
            }
        }

        return result;
    }
}
