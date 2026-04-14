package com.guardmod.validate;

import com.guardmod.config.GuardCommonConfig;
import com.guardmod.model.ClientEnvironmentReport;
import com.guardmod.model.ClientModEntry;
import com.guardmod.model.ExpectedModEntry;
import com.guardmod.scanner.ModScanService;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ModRulesValidator {
    private static final Set<String> ALWAYS_ALLOWED_BUILTINS = new HashSet<String>(Arrays.asList(
            "minecraft",
            "forge",
            "mcp",
            "fml",
            "javafml",
            "lowcodefml"
    ));

    private ModRulesValidator() {
    }

    public static ValidationResult validateBootstrapRules(ClientEnvironmentReport report) {
        if (report == null) {
            return ValidationResult.failure("Client report is missing", ValidationStage.REPORT);
        }

        String mode = normalizeMode(GuardCommonConfig.MODS_MODE.get());
        if ("OFF".equals(mode)) {
            return ValidationResult.success();
        }

        ModPolicySnapshot snapshot = ModScanService.getSnapshot();
        if (snapshot == null || !snapshot.isReady()) {
            String reason = snapshot == null ? "Server mod policy is not ready" : snapshot.getStatusMessage();
            return ValidationResult.failure(reason, ValidationStage.RULES);
        }

        Map<String, ClientModEntry> clientModsById = indexClientMods(report);

        for (ExpectedModEntry expected : snapshot.getRequiredMods().values()) {
            ClientModEntry clientEntry = clientModsById.get(expected.getModId());
            if (clientEntry == null) {
                String displayName = safe(expected.getDisplayName());
                String suffix = displayName.isEmpty() ? "" : " (" + displayName + ")";
                return ValidationResult.failure("Missing required mod: " + expected.getModId() + suffix, ValidationStage.RULES);
            }

            ValidationResult versionOrHashResult = validateVersionAndHash(expected, clientEntry);
            if (!versionOrHashResult.isSuccess()) {
                return versionOrHashResult;
            }
        }

        boolean allowUnknownClientMods = GuardCommonConfig.ALLOW_UNKNOWN_CLIENT_MODS.get() || "LENIENT".equals(mode);
        if (allowUnknownClientMods) {
            return ValidationResult.success();
        }

        Set<String> ignoredClientModIds = getIgnoredClientModIds();

        for (ClientModEntry clientEntry : clientModsById.values()) {
            String modId = normalize(clientEntry.getModId());
            if (modId.isEmpty() || ALWAYS_ALLOWED_BUILTINS.contains(modId) || ignoredClientModIds.contains(modId)) {
                continue;
            }

            ExpectedModEntry requiredEntry = snapshot.getRequiredMods().get(modId);
            if (requiredEntry != null) {
                continue;
            }

            ExpectedModEntry allowedOptionalEntry = snapshot.getAllowedOptionalMods().get(modId);
            if (allowedOptionalEntry != null) {
                ValidationResult versionOrHashResult = validateVersionAndHash(allowedOptionalEntry, clientEntry);
                if (!versionOrHashResult.isSuccess()) {
                    return versionOrHashResult;
                }
                continue;
            }

            return ValidationResult.failure("Unknown client mod is not allowed: " + modId, ValidationStage.RULES);
        }

        return ValidationResult.success();
    }

    private static ValidationResult validateVersionAndHash(ExpectedModEntry expected, ClientModEntry actual) {
        if (GuardCommonConfig.ENFORCE_EXACT_VERSION.get()) {
            String expectedVersion = safe(expected.getVersion());
            String actualVersion = safe(actual.getVersion());
            if (!expectedVersion.isEmpty() && !expectedVersion.equals(actualVersion)) {
                return ValidationResult.failure(
                        "Version mismatch for mod " + expected.getModId() + ": expected " + expectedVersion + ", got " + actualVersion,
                        ValidationStage.RULES
                );
            }
        }

        if (GuardCommonConfig.ENFORCE_EXACT_HASH.get()) {
            String expectedHash = safe(expected.getJarHash());
            String actualHash = safe(actual.getJarHash());
            if (!expectedHash.isEmpty() && !actualHash.isEmpty() && !expectedHash.equalsIgnoreCase(actualHash)) {
                return ValidationResult.failure(
                        "Hash mismatch for mod " + expected.getModId(),
                        ValidationStage.RULES
                );
            }
        }

        return ValidationResult.success();
    }

    private static Map<String, ClientModEntry> indexClientMods(ClientEnvironmentReport report) {
        Map<String, ClientModEntry> result = new HashMap<String, ClientModEntry>();
        for (ClientModEntry entry : report.getMods()) {
            if (entry == null) {
                continue;
            }

            String modId = normalize(entry.getModId());
            if (!modId.isEmpty()) {
                result.put(modId, entry);
            }
        }
        return result;
    }

    private static String normalizeMode(String value) {
        return safe(value).trim().toUpperCase(Locale.ROOT);
    }

    private static String normalize(String value) {
        return safe(value).trim().toLowerCase(Locale.ROOT);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
    private static Set<String> getIgnoredClientModIds() {
        Set<String> result = new HashSet<String>();

        String raw = safe(GuardCommonConfig.IGNORED_CLIENT_MOD_IDS.get());
        if (raw.isEmpty()) {
            return result;
        }

        for (String part : raw.split(",")) {
            String normalized = normalize(part);
            if (!normalized.isEmpty()) {
                result.add(normalized);
            }
        }

        return result;
    }
}