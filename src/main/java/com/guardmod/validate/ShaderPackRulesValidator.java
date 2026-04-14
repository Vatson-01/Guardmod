package com.guardmod.validate;

import com.guardmod.config.GuardCommonConfig;
import com.guardmod.model.ClientEnvironmentReport;
import com.guardmod.model.ClientShaderPackEntry;
import com.guardmod.model.ScannedShaderPack;
import com.guardmod.scanner.ShaderPackScanService;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ShaderPackRulesValidator {
    private ShaderPackRulesValidator() {
    }

    public static ValidationResult validateActiveShaderPack(ClientEnvironmentReport report) {
        if (report == null) {
            return ValidationResult.failure("Client report is missing", ValidationStage.REPORT);
        }

        String mode = normalizeMode(GuardCommonConfig.SHADERPACKS_MODE.get());
        if ("OFF".equals(mode)) {
            return ValidationResult.success();
        }

        ShaderPackPolicySnapshot snapshot = ShaderPackScanService.getSnapshot();
        if (snapshot == null || !snapshot.isReady()) {
            String reason = snapshot == null ? "Server shader pack policy is not ready" : snapshot.getStatusMessage();
            return ValidationResult.failure(reason, ValidationStage.RULES);
        }

        ClientShaderPackEntry active = report.getActiveShaderPack();
        if (active == null) {
            return ValidationResult.success();
        }

        String normalizedHash = normalizeHash(active.getContentHash());
        if (normalizedHash.isEmpty()) {
            return ValidationResult.failure("Shader pack content hash is missing: " + safe(active.getDisplayName()), ValidationStage.RULES);
        }

        Map<String, ScannedShaderPack> allowedByContentHash = snapshot.getAllowedPacksByContentHash();
        ScannedShaderPack expected = allowedByContentHash.get(normalizedHash);
        if (expected == null) {
            return ValidationResult.failure("Active shader pack is not allowed: " + safe(active.getDisplayName()), ValidationStage.RULES);
        }

        if (GuardCommonConfig.CHECK_SHADERPACK_RAW_HASH_TOO.get()) {
            String expectedRawHash = normalizeHash(expected.getRawFileHash());
            String actualRawHash = normalizeHash(active.getRawFileHash());
            if (!expectedRawHash.isEmpty() && !actualRawHash.isEmpty() && !expectedRawHash.equals(actualRawHash)) {
                return ValidationResult.failure("Shader pack raw hash mismatch: " + safe(active.getDisplayName()), ValidationStage.RULES);
            }
        }

        return ValidationResult.success();
    }

    public static List<ClientShaderPackEntry> collectUnknownInstalledShaderPacks(ClientEnvironmentReport report) {
        List<ClientShaderPackEntry> result = new ArrayList<ClientShaderPackEntry>();
        if (report == null) {
            return result;
        }

        ShaderPackPolicySnapshot snapshot = ShaderPackScanService.getSnapshot();
        if (snapshot == null || !snapshot.isReady()) {
            return result;
        }

        Map<String, ScannedShaderPack> allowedByContentHash = snapshot.getAllowedPacksByContentHash();
        Set<String> seen = new LinkedHashSet<String>();

        for (ClientShaderPackEntry entry : report.getInstalledShaderPacks()) {
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

    private static String normalizeMode(String value) {
        return safe(value).trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeHash(String value) {
        return safe(value).trim().toLowerCase(Locale.ROOT);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
